package com.impactomsn.app.calls

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

/** Microphone, speaker and camera for one call. Everything degrades gracefully when a permission is missing. */
class CallMedia(
    private val context: Context,
    private val session: CallManager.MediaSession,
    private val wantVideo: Boolean,
    private val onLocalFrame: (Bitmap?) -> Unit,
) {
    @Volatile var muted = false
    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var audioThread: Thread? = null
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var previousMode = AudioManager.MODE_NORMAL

    private var camThread: HandlerThread? = null
    private var camHandler: Handler? = null
    private var camera: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var facing = CameraCharacteristics.LENS_FACING_FRONT
    private var sensorRotation = 90
    @Volatile private var lastFrameAt = 0L

    private fun has(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun start() {
        running = true
        previousMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        startAudio()
        if (wantVideo) setCamera(true)
    }

    fun stop() {
        running = false
        setCamera(false)
        audioThread?.interrupt()
        try { record?.stop() } catch (_: Exception) {}
        record?.release(); record = null
        try { track?.stop() } catch (_: Exception) {}
        track?.release(); track = null
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = previousMode
    }

    // ---- audio
    @SuppressLint("MissingPermission")
    private fun startAudio() {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(CallProtocol.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(CallProtocol.FRAME_SAMPLES * 2 * 8)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        this.track = track
        if (!has(Manifest.permission.RECORD_AUDIO)) return
        val minBuf = AudioRecord.getMinBufferSize(CallProtocol.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, CallProtocol.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, CallProtocol.FRAME_SAMPLES * 2 * 4))
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return }
        record = rec
        rec.startRecording()
        audioThread = Thread({
            val pcm = ShortArray(CallProtocol.FRAME_SAMPLES)
            while (running) {
                var got = 0
                while (got < pcm.size && running) {
                    val n = rec.read(pcm, got, pcm.size - got)
                    if (n <= 0) return@Thread
                    got += n
                }
                if (!muted && got == pcm.size) session.send(CallProtocol.T_AUDIO, CallProtocol.ulawEncode(pcm))
            }
        }, "call-audio-in").also { it.isDaemon = true; it.start() }
    }

    fun pushAudio(ulaw: ByteArray) {
        val t = track ?: return
        val pcm = CallProtocol.ulawDecode(ulaw)
        t.write(pcm, 0, pcm.size, AudioTrack.WRITE_NON_BLOCKING)
    }

    @Suppress("DEPRECATION")
    fun setSpeaker(on: Boolean) { audioManager.isSpeakerphoneOn = on }

    // ---- camera
    fun setCamera(on: Boolean) {
        if (!on) { closeCamera(); onLocalFrame(null); return }
        if (!has(Manifest.permission.CAMERA)) return
        if (camera != null) return
        camThread = HandlerThread("call-camera").also { it.start() }
        camHandler = Handler(camThread!!.looper)
        openCamera()
    }

    fun switchCamera() {
        if (camera == null) return
        facing = if (facing == CameraCharacteristics.LENS_FACING_FRONT) CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT
        closeCamera()
        camThread = HandlerThread("call-camera").also { it.start() }
        camHandler = Handler(camThread!!.looper)
        openCamera()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            val cm = context.getSystemService(CameraManager::class.java)
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == facing }
                ?: cm.cameraIdList.firstOrNull() ?: return
            val ch = cm.getCameraCharacteristics(id)
            sensorRotation = ch.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            val sizes = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(ImageFormat.YUV_420_888) ?: return
            val size = sizes.filter { it.width >= 320 && it.height >= 240 }.minByOrNull { it.width * it.height }
                ?: sizes.maxByOrNull { it.width * it.height } ?: return
            val r = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
            reader = r
            r.setOnImageAvailableListener({ rd ->
                val img = rd.acquireLatestImage() ?: return@setOnImageAvailableListener
                try { handleImage(img) } catch (_: Exception) {} finally { img.close() }
            }, camHandler)
            cm.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    camera = device
                    @Suppress("DEPRECATION")
                    device.createCaptureSession(listOf(r.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            captureSession = s
                            val req = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(r.surface) }
                            try { s.setRepeatingRequest(req.build(), null, camHandler) } catch (_: Exception) {}
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) { closeCamera() }
                    }, camHandler)
                }
                override fun onDisconnected(device: CameraDevice) { device.close(); camera = null }
                override fun onError(device: CameraDevice, error: Int) { device.close(); camera = null }
            }, camHandler)
        } catch (_: Exception) {
            closeCamera()
        }
    }

    private fun closeCamera() {
        try { captureSession?.close() } catch (_: Exception) {}
        captureSession = null
        try { camera?.close() } catch (_: Exception) {}
        camera = null
        try { reader?.close() } catch (_: Exception) {}
        reader = null
        camThread?.quitSafely(); camThread = null; camHandler = null
    }

    private fun handleImage(img: Image) {
        val now = System.currentTimeMillis()
        if (now - lastFrameAt < 83 || session.backlog() > 20) return // ~12 fps, skip when the link is congested
        lastFrameAt = now
        val w = img.width
        val h = img.height
        val nv21 = toNv21(img, w, h)
        val jpeg = ByteArrayOutputStream().also { YuvImage(nv21, ImageFormat.NV21, w, h, null).compressToJpeg(Rect(0, 0, w, h), 70, it) }.toByteArray()
        val src = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return
        val swapped = sensorRotation == 90 || sensorRotation == 270
        val targetW = if (swapped) 240 else 320
        val targetH = if (swapped) 320 else 240
        val m = Matrix().apply {
            postRotate(sensorRotation.toFloat())
            val rw = if (swapped) h else w
            val rh = if (swapped) w else h
            postScale(targetW.toFloat() / rw, targetH.toFloat() / rh)
        }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (out !== src) src.recycle()
        val bytes = ByteArrayOutputStream().also { out.compress(Bitmap.CompressFormat.JPEG, 55, it) }.toByteArray()
        session.send(CallProtocol.T_VIDEO, bytes)
        onLocalFrame(out)
    }

    private fun toNv21(img: Image, w: Int, h: Int): ByteArray {
        val out = ByteArray(w * h * 3 / 2)
        val y = img.planes[0]
        val yb = y.buffer
        for (row in 0 until h) {
            for (col in 0 until w) out[row * w + col] = yb.get(row * y.rowStride + col * y.pixelStride)
        }
        val u = img.planes[1]; val v = img.planes[2]
        var pos = w * h
        for (row in 0 until h / 2) for (col in 0 until w / 2) {
            out[pos++] = v.buffer.get(row * v.rowStride + col * v.pixelStride)
            out[pos++] = u.buffer.get(row * u.rowStride + col * u.pixelStride)
        }
        return out
    }
}
