package com.impactomsn.app.calls

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.UUID
import java.util.ArrayDeque
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.impactomsn.app.SessionPrefs
import org.json.JSONObject

enum class CallPhase { Idle, Calling, Ringing, Connecting, Active }

data class CallState(
    val phase: CallPhase = CallPhase.Idle,
    val peer: String = "",
    val video: Boolean = false,
    val message: String? = null,
    val signalOnline: Boolean = false,
)

/**
 * Process-wide call controller: signalling (LISTEN/SIG) and one media session (JOIN token).
 * Protocol and behaviour mirror MSNCalls/app/core.py so PC<->Android, PC<->PC and Android<->Android all work.
 */
class CallManager private constructor(private val appContext: Context) {
    private val lock = Any()
    private val _state = MutableStateFlow(CallState())
    val state: StateFlow<CallState> = _state.asStateFlow()

    /** Video frames from the other side (JPEG bytes). */
    val remoteVideo = MutableSharedFlow<ByteArray>(replay = 1, extraBufferCapacity = 2)

    /** Our own camera preview (already rotated upright), null while the camera is off. */
    val localVideo = MutableStateFlow<Bitmap?>(null)

    private var signal: SignalThread? = null
    private var host = ""
    private var email = ""
    private var token = ""
    private var role = ""
    private var session: MediaSession? = null
    private var media: CallMedia? = null
    private var timeout: Thread? = null
    private data class PendingSignal(val line: String, val expiresAt: Long)

	// ---- lifecycle
    fun startIfSignedIn() {
        if (!SessionPrefs.canOpenAuthenticatedScreens(appContext)) return
        val snap = SessionPrefs.readLoginSnapshot(appContext)
        if (snap.useMockServer || snap.email.isBlank() || snap.password.isEmpty()) return
        connectWithCredentials(snap.email, snap.password, snap.resolvedServers.mainServer)
    }
	
    fun connectWithCredentials(emailArg: String, passwordArg: String, mainServerArg: String) {
        if (emailArg.isBlank() || passwordArg.isEmpty() || mainServerArg.isBlank()) return
        val targetEmail = emailArg.lowercase()
        synchronized(lock) {
            // Si ya está vivo y conectado con el mismo host y correo, no reinicies el socket
            if (signal?.alive == true && host == mainServerArg && email == targetEmail) {
                return
            }
            stopSignal()
            host = mainServerArg
            email = targetEmail
            signal = SignalThread(mainServerArg, targetEmail, passwordArg).also { it.start() }
        }
    }

    fun stop() {
        hangup()
        synchronized(lock) { stopSignal() }
    }

    private fun stopSignal() {
        signal?.close()
        signal = null
        _state.value = _state.value.copy(signalOnline = false)
    }

    // ---- user actions
    fun placeCall(peer: String, video: Boolean) {
        val p = peer.trim().lowercase()
        if (p.isBlank()) return
        val callToken: String
        synchronized(lock) {
            if (_state.value.phase != CallPhase.Idle) return
            callToken = UUID.randomUUID().toString().replace("-", "")
            token = callToken
            role = "caller"
            // Enter Calling immediately so the call screen and its controls are visible
            // while the signalling socket finishes authenticating.
            _state.value = CallState(
                CallPhase.Calling, p, video,
                message = if (signal?.authenticated == true) null else "Conectando con el servicio de llamadas…",
                signalOnline = signal?.authenticated == true,
            )
        }

        // A voice call can be started immediately after opening the chat, before the
        // process-wide signalling thread has received the server's LISTEN/OK response.
        // Previously this left the state Idle, hid every control and closed the screen.
        if (signal?.authenticated != true) startIfSignedIn()

        Thread({
            val deadline = System.currentTimeMillis() + 20_000L
            while (System.currentTimeMillis() < deadline) {
                val current = _state.value
                if (token != callToken || current.phase != CallPhase.Calling) return@Thread
                if (signal?.authenticated == true) {
                    val sent = signal?.send(p, "INVITE", callToken, if (video) "video" else "audio") == true
                    if (!sent) {
                        end("No se pudo enviar la invitación al servidor de llamadas")
                        return@Thread
                    }
                    Log.i("MSNCalls", "INVITE ${if (video) "video" else "audio"} enviado a $p")
                    armTimeout(callToken)
                    return@Thread
                }
                try { Thread.sleep(250) } catch (_: InterruptedException) { return@Thread }
            }
            if (token == callToken && _state.value.phase == CallPhase.Calling) {
                end("No se pudo conectar con el servicio de llamadas. Comprueba tu sesión e inténtalo de nuevo.")
            }
        }, "call-invite").also { it.isDaemon = true; it.start() }
    }

    fun accept() {
        val s: CallState
        synchronized(lock) {
            s = _state.value
            if (s.phase != CallPhase.Ringing) return
            _state.value = s.copy(phase = CallPhase.Connecting, message = null)
        }
        IncomingCallNotifier.cancel(appContext) // stop the ringtone as soon as the call is answered
        // Keep the acceptance and media setup in the same state transition. The
        // signal thread can briefly be reconnecting after an invite was delivered;
        // send() queues this control line until LISTEN is authenticated again.
        if (signal?.send(s.peer, "ACCEPT", token) != true) {
            end("No se pudo confirmar la llamada con el servidor")
            return
        }
        openSession()
    }

    fun decline() {
        val s = _state.value
        if (s.phase != CallPhase.Ringing) return
        signal?.send(s.peer, "DECLINE", token)
        end(null)
    }

    fun hangup() {
        val s = _state.value
        if (s.phase == CallPhase.Idle) return
        signal?.send(s.peer, "HANGUP", token)
        end(null)
    }

    fun setMuted(muted: Boolean) { media?.muted = muted }
    fun setCameraOn(on: Boolean) {
        media?.setCamera(on)
        session?.sendCtrl(JSONObject().put("video", on))
    }
    fun setSpeaker(on: Boolean) { media?.setSpeaker(on) }
    fun switchCamera() { media?.switchCamera() }

    // ---- signalling from the other side
    private fun onSig(sender: String, cmd: String, a: String, b: String) {
        val s = _state.value
        when {
            cmd == "INVITE" -> {
                if (s.phase != CallPhase.Idle) { signal?.send(sender, "DECLINE", a, "busy"); return }
                if (a.length !in 16..64) return
                synchronized(lock) {
                    token = a; role = "callee"
                    _state.value = CallState(CallPhase.Ringing, sender, b == "video", signalOnline = s.signalOnline)
                }
                IncomingCallNotifier.show(appContext, sender, b == "video")
                armTimeout(a)
            }
            cmd == "NOPEER" && s.peer == sender &&
                (s.phase == CallPhase.Calling || s.phase == CallPhase.Connecting) ->
                end(if (s.phase == CallPhase.Connecting)
                    "El otro dispositivo perdió la conexión de señalización"
                else "El contacto no esta disponible para llamadas")
            s.phase != CallPhase.Idle && s.peer == sender && token == a -> when (cmd) {
                "ACCEPT" -> if (role == "caller" && s.phase == CallPhase.Calling) {
                    _state.value = s.copy(phase = CallPhase.Connecting)
                    openSession()
                }
                "DECLINE" -> end("Llamada rechazada" + if (b == "busy") " (ocupado)" else "")
                "HANGUP" -> end("El contacto colgo")
            }
        }
    }

    private fun armTimeout(forToken: String) {
        timeout?.interrupt()
        timeout = Thread {
            try { Thread.sleep(45_000) } catch (_: InterruptedException) { return@Thread }
            val s = _state.value
            if (token == forToken && (s.phase == CallPhase.Calling || s.phase == CallPhase.Ringing)) {
                if (s.phase == CallPhase.Calling) signal?.send(s.peer, "HANGUP", forToken)
                end("Sin respuesta")
            }
        }.also { it.isDaemon = true; it.start() }
    }

    // ---- media
    private fun openSession() {
        val sess = MediaSession(host, token, ::onHello, ::onAudio, ::onVideo, ::onCtrl, ::onSessionClosed)
        session = sess
        sess.start()
    }

    private fun onHello() {
        val s = _state.value
        if (s.phase != CallPhase.Connecting) return
        _state.value = s.copy(phase = CallPhase.Active, message = null)
        val sess = session ?: return
        media = CallMedia(appContext, sess, s.video) { localVideo.value = it }.also { it.start() }
        IncomingCallNotifier.cancel(appContext)
    }

    private fun onAudio(data: ByteArray) { media?.pushAudio(data) }
    private fun onVideo(data: ByteArray) { remoteVideo.tryEmit(data) }
    private fun onCtrl(o: JSONObject) {
        if (o.has("video")) _state.value = _state.value.copy(message = if (o.optBoolean("video")) "El contacto activo su camara" else "El contacto apago su camara")
    }
    private fun onSessionClosed() { if (_state.value.phase != CallPhase.Idle) end("Llamada finalizada") }

    private fun end(text: String?) {
        synchronized(lock) {
            if (_state.value.phase == CallPhase.Idle) return
            timeout?.interrupt()
            media?.stop(); media = null
            localVideo.value = null
            remoteVideo.resetReplayCache()
            session?.close(false); session = null
            IncomingCallNotifier.cancel(appContext)
            _state.value = CallState(CallPhase.Idle, message = text ?: "Llamada finalizada", signalOnline = _state.value.signalOnline)
        }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    // ---- signalling socket
    private inner class SignalThread(val host: String, val email: String, val password: String) : Thread("call-signal") {
        @Volatile var alive = true
        @Volatile var authenticated = false
            private set
        @Volatile private var sock: Socket? = null
        private val wlock = Any()
        private val queueLock = Any()
        private val pendingSignals = ArrayDeque<PendingSignal>()
        private val maxPendingSignals = 64
        private val pendingSignalLifetimeMs = 30_000L
        private val pingIntervalMs = 10_000L
        private val pongTimeoutMs = 20_000L
        init { isDaemon = true }

		fun send(to: String, cmd: String, a: String = "-", b: String = "-"): Boolean {
            if (listOf(to, cmd, a, b).any { it.isEmpty() || it.any(Char::isWhitespace) }) return false
            if (!alive) return false
            val line = "SIG $to $cmd $a $b\n"
            
            // Intento directo de envío
            if (write(line)) {
                Log.i("MSNCalls", "Señal $cmd enviada con éxito al servidor")
                return true
            }

            // Si falló, la guardamos en la cola y activamos el mecanismo de recuperación
            synchronized(queueLock) {
                if (!alive) return false
                val now = System.currentTimeMillis()
                while (pendingSignals.isNotEmpty() && pendingSignals.peekFirst().expiresAt < now) {
                    pendingSignals.removeFirst()
                }
                if (pendingSignals.size >= maxPendingSignals) pendingSignals.removeFirst()
                pendingSignals.addLast(PendingSignal(line, now + pendingSignalLifetimeMs))
                Log.i("MSNCalls", "Señal $cmd puesta en cola (reintentando conexión...)")
            }
            
            // Intentar vaciar la cola inmediatamente por si el socket se acaba de restablecer
            flushPendingSignals()
            return true
        }

        private fun write(line: String): Boolean {
            // Capturamos el socket en una variable local para evitar que se vuelva null por concurrencia
            val s = sock ?: return false
            if (s.isClosed) return false
            return try {
                synchronized(wlock) {
                    val output = s.getOutputStream() ?: return false
                    output.write(line.toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                true
            } catch (ex: Exception) {
                Log.w("MSNCalls", "Fallo al escribir en el socket: ${ex.message}")
                // Si el socket falló, forzamos su cierre para que el hilo reconecte limpiamente de inmediato
                try {
                    s.close()
                } catch (_: Exception) {}
                sock = null
                false
            }
        }
		
        private fun flushPendingSignals() {
            synchronized(queueLock) {
                val now = System.currentTimeMillis()
                while (pendingSignals.isNotEmpty()) {
                    val pending = pendingSignals.peekFirst()
                    if (pending.expiresAt < now) {
                        pendingSignals.removeFirst()
                        continue
                    }
                    if (!authenticated || !write(pending.line)) return
                    Log.i("MSNCalls", "Señal pendiente enviada al recuperar la conexión")
                    pendingSignals.removeFirst()
                }
            }
        }

        fun close() {
            alive = false
            authenticated = false
            synchronized(queueLock) { pendingSignals.clear() }
            try { sock?.close() } catch (_: Exception) {}
            interrupt()
        }

        override fun run() {
            var delay = 1000L
            while (alive) {
                try {
                    session()
                    delay = 1000L
                } catch (ex: Exception) {
                    if (alive) { // Solo registrar si no fue un cierre provocado
                        Log.w("MSNCalls", "Error de señalización con $host:${CallProtocol.RELAY_PORT}", ex)
                        _state.value = _state.value.copy(
                            signalOnline = false,
                            message = "Error del servicio de llamadas: ${ex.javaClass.simpleName}"
                        )
                    }
                }
                authenticated = false
                sock = null
                _state.value = _state.value.copy(signalOnline = false)
                if (!alive) break
                try { sleep(delay) } catch (_: InterruptedException) { break }
                delay = minOf(delay * 2, 60_000L)
            }
        }

        private fun session() {
            val s = Socket()
            s.connect(InetSocketAddress(host, CallProtocol.RELAY_PORT), 10_000)
            s.soTimeout = 15_000
            sock = s
            write("LISTEN $email ${URLEncoder.encode(password, "UTF-8")}\n")
            val input: InputStream = s.getInputStream()
            val pending = StringBuilder()
            val chunk = ByteArray(4096)
            var lastPing = System.currentTimeMillis()
            var lastPong = lastPing
            var authed = false

            while (alive) {
                try {
                    val n = input.read(chunk)
                    if (n < 0) return
                    pending.append(String(chunk, 0, n, Charsets.UTF_8))
                } catch (_: SocketTimeoutException) {}

                while (true) {
                    val i = pending.indexOf("\n")
                    if (i < 0) break
                    val line = pending.substring(0, i).trim()
                    pending.delete(0, i + 1)
                    val p = line.split(Regex("\\s+"))
                    when {
                        line == "OK" && !authed -> {
                            authed = true
                            authenticated = true
                            _state.value = _state.value.copy(signalOnline = true, message = null)
                            Log.i("MSNCalls", "Canal de señalización autenticado correctamente")
                            flushPendingSignals()
                        }
                        line == "PONG" -> lastPong = System.currentTimeMillis()
                        line == "DENIED" -> {
                            authenticated = false
                            alive = false
                            _state.value = _state.value.copy(signalOnline = false, message = "Autenticación rechazada (DENIED).")
                            return
                        }
                        p.size == 5 && p[0] == "SIG" -> {
                            onSig(p[1].lowercase(), p[2].uppercase(), p[3], p[4])
                        }
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastPing > 15_000) {
                    if (!write("PING\n")) throw SocketTimeoutException("No se pudo enviar PING")
                    lastPing = now
                }
            }
        }
    }

    // ---- media connection (JOIN room)
    class MediaSession(
        private val host: String, private val token: String,
        private val onHello: () -> Unit, private val onAudio: (ByteArray) -> Unit, private val onVideo: (ByteArray) -> Unit,
        private val onCtrl: (JSONObject) -> Unit, private val onClosed: () -> Unit,
    ) {
        private val out = ArrayBlockingQueue<ByteArray>(400)
        @Volatile private var closed = false
        @Volatile private var peerHello = false
        private var sock: Socket? = null

        fun start() { Thread({ runLoop() }, "call-media").also { it.isDaemon = true }.start() }
        fun backlog() = out.size

        fun send(type: Int, payload: ByteArray) {
            if (closed) return
            val frame = CallProtocol.pack(type, payload)
            if (!out.offer(frame) && (type == CallProtocol.T_CTRL || type == CallProtocol.T_HELLO)) { out.poll(); out.offer(frame) }
        }

        fun sendCtrl(o: JSONObject) = send(CallProtocol.T_CTRL, o.toString().toByteArray())

        fun close(notify: Boolean = true) {
            if (closed) return
            closed = true
            try { sock?.close() } catch (_: Exception) {}
            if (notify) onClosed()
        }

        private fun runLoop() {
            val s = try { Socket().also { it.connect(InetSocketAddress(host, CallProtocol.RELAY_PORT), 10_000); it.soTimeout = 500; it.tcpNoDelay = true } }
            catch (ex: Exception) {
                Log.w("MSNCalls", "No se pudo abrir el canal multimedia en $host:${CallProtocol.RELAY_PORT}", ex)
                close(); return
            }
            sock = s
            try { s.getOutputStream().apply { write("JOIN $token\n".toByteArray()); flush() } }
            catch (ex: Exception) { Log.w("MSNCalls", "No se pudo entrar a la sala multimedia", ex); close(); return }
            Thread({
                while (!closed) {
                    val f = try { out.poll(500, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { return@Thread } ?: continue
                    try { s.getOutputStream().apply { write(f); flush() } } catch (_: Exception) { close(); return@Thread }
                }
            }, "call-media-writer").also { it.isDaemon = true }.start()

            val reader = CallProtocol.FrameReader()
            val buf = ByteArray(65536)
            val started = System.currentTimeMillis()
            var lastHello = 0L
            while (!closed) {
                if (!peerHello) {
                    if (System.currentTimeMillis() - started > 35_000) {
                        Log.w("MSNCalls", "Tiempo agotado esperando al otro participante en la sala")
                        close(); return
                    }
                    if (System.currentTimeMillis() - lastHello > 500) { send(CallProtocol.T_HELLO, CallProtocol.HELLO_JSON.toByteArray()); lastHello = System.currentTimeMillis() }
                }
                val n = try { s.getInputStream().read(buf) } catch (_: SocketTimeoutException) { continue } catch (_: Exception) { close(); return }
                if (n < 0) { close(); return }
                val frames = try { reader.feed(buf, n) } catch (_: Exception) { close(); return }
                for ((type, payload) in frames) {
                    try {
                        when (type) {
                            CallProtocol.T_HELLO -> {
                                val first = !peerHello
                                peerHello = true
                                send(CallProtocol.T_HELLO, CallProtocol.HELLO_JSON.toByteArray())
                                if (first) {
                                    Log.i("MSNCalls", "Canal multimedia establecido con el otro participante")
                                    onHello()
                                }
                            }
                            CallProtocol.T_AUDIO -> onAudio(payload)
                            CallProtocol.T_VIDEO -> onVideo(payload)
                            CallProtocol.T_CTRL -> onCtrl(JSONObject(String(payload)))
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    companion object {
        @Volatile private var instance: CallManager? = null
        private var pendingEmail: String = ""
        private var pendingPassword: String = ""
        private var pendingMainServer: String = ""

        fun provideCredentials(email: String, password: String, mainServer: String) {
            pendingEmail = email
            pendingPassword = password
            pendingMainServer = mainServer
            instance?.connectWithCredentials(email, password, mainServer)
        }

        fun stopIfCreated() { instance?.stop() }
        
        fun get(context: Context): CallManager =
            instance ?: synchronized(this) { 
                instance ?: CallManager(context.applicationContext).also { 
                    instance = it 
                    if (pendingEmail.isNotBlank()) {
                        it.connectWithCredentials(pendingEmail, pendingPassword, pendingMainServer)
                    }
                } 
            }
    }
}
