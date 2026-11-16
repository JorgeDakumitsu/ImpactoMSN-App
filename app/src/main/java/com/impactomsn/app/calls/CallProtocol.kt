package com.impactomsn.app.calls

import java.nio.ByteBuffer

/**
 * Wire format shared with the PC companion (MSNCalls, Python) and the WLM server's callrelay.py.
 * Media frame = type(1) + length(4, big endian) + payload.
 */
object CallProtocol {
    const val RELAY_PORT = 5200
    const val T_HELLO = 1
    const val T_AUDIO = 2
    const val T_VIDEO = 3
    const val T_CTRL = 4
    const val MAX_FRAME = 2 * 1024 * 1024
    const val SAMPLE_RATE = 16000
    const val FRAME_SAMPLES = 320 // 20 ms
    const val HELLO_JSON = """{"v":1,"audio":"ulaw16k","video":"mjpeg","w":320,"h":240}"""

    fun pack(type: Int, payload: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(5 + payload.size)
        buf.put(type.toByte()).putInt(payload.size).put(payload)
        return buf.array()
    }

    private const val BIAS = 0x84
    private const val CLIP = 32635

    /** G.711 mu-law, same arithmetic as the Python implementation. */
    fun ulawEncode(pcm: ShortArray, count: Int = pcm.size): ByteArray {
        val out = ByteArray(count)
        for (i in 0 until count) {
            var x = pcm[i].toInt()
            val sign = (x shr 8) and 0x80
            if (sign != 0) x = -x
            if (x > CLIP) x = CLIP
            x += BIAS
            var exp = 7
            var mask = 0x4000
            while (exp > 0 && (x and mask) == 0) { exp--; mask = mask shr 1 }
            val mant = (x shr (exp + 3)) and 0x0F
            out[i] = (sign or (exp shl 4) or mant).inv().toByte()
        }
        return out
    }

    fun ulawDecode(data: ByteArray): ShortArray {
        val out = ShortArray(data.size)
        for (i in data.indices) {
            val u = data[i].toInt().inv() and 0xFF
            val sign = u and 0x80
            val exp = (u shr 4) and 0x07
            val mant = u and 0x0F
            val t = ((mant shl 3) + BIAS) shl exp
            out[i] = (if (sign != 0) BIAS - t else t - BIAS).toShort()
        }
        return out
    }

    /** Accumulates stream bytes and yields complete frames. */
    class FrameReader {
        private var pending = ByteArray(0)

        fun feed(data: ByteArray, len: Int = data.size): List<Pair<Int, ByteArray>> {
            pending += data.copyOfRange(0, len)
            val out = ArrayList<Pair<Int, ByteArray>>()
            var pos = 0
            while (pending.size - pos >= 5) {
                val type = pending[pos].toInt() and 0xFF
                val n = ByteBuffer.wrap(pending, pos + 1, 4).int
                if (n < 0 || n > MAX_FRAME) throw IllegalStateException("frame too large")
                if (pending.size - pos < 5 + n) break
                out.add(type to pending.copyOfRange(pos + 5, pos + 5 + n))
                pos += 5 + n
            }
            if (pos > 0) pending = pending.copyOfRange(pos, pending.size)
            return out
        }
    }
}
