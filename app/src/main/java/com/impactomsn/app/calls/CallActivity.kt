package com.impactomsn.app.calls

import com.impactomsn.app.R

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.impactomsn.app.ui.theme.MsnAvatarFrame
import com.impactomsn.app.appContainer
import com.impactomsn.app.loadAvatarBitmap
import com.impactomsn.app.ui.theme.MsnPalette
import com.impactomsn.app.ui.theme.ImpactoMsnTheme
import com.impactomsn.app.ui.theme.msnWindowBackground
import androidx.compose.ui.res.stringResource

/** Full-screen call UI in the style of MSN Messenger 8.5 (light blue "Aero" panels) adapted to Android. */
class CallActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.impactomsn.app.LanguagePrefs.wrap(newBase))
    }
    private lateinit var manager: CallManager
    private var afterPermissions: (() -> Unit)? = null
    private var sawCall by mutableStateOf(false)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // The call still works without the camera (audio only) or without the microphone (listen only).
        afterPermissions?.invoke()
        afterPermissions = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        manager = CallManager.get(this)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handleIntent(intent)
        setContent {
            ImpactoMsnTheme {
                val state by manager.state.collectAsState()
                val contacts by appContainer.messengerRepository.contacts.collectAsState()
                val peerContact = remember(contacts, state.peer) {
                    contacts.firstOrNull { it.email.equals(state.peer, ignoreCase = true) }
                }
                val peerAvatar by produceState<Bitmap?>(null, peerContact?.avatarUri, state.video) {
                    value = if (state.video) null else peerContact?.avatarUri?.let { loadAvatarBitmap(it) }
                }
                val local by manager.localVideo.collectAsState()
                val remote by produceState<Bitmap?>(null) {
                    manager.remoteVideo.collect { jpeg ->
                        value = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) }
                    }
                }
                LaunchedEffect(state.phase) { if (state.phase != CallPhase.Idle) sawCall = true }
                LaunchedEffect(state.phase, sawCall) {
                    if (state.phase == CallPhase.Idle) {
                        delay(if (sawCall) 1500 else 2500)
                        if (manager.state.value.phase == CallPhase.Idle) finish()
                    }
                }
                CallScreen(
                    state = state,
                    remote = if (state.phase == CallPhase.Active) remote else null,
                    local = local,
                    peerAvatar = if (state.video) null else peerAvatar,
                    onAccept = { withPermissions(state.video) { manager.accept() } },
                    onDecline = { manager.decline() },
                    onHangup = { manager.hangup() },
                    onMute = { manager.setMuted(it) },
                    onCamera = { on -> withPermissions(true) { manager.setCameraOn(on) } },
                    onSpeaker = { manager.setSpeaker(it) },
                    onSwitchCamera = { manager.switchCamera() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        setIntent(Intent(this, CallActivity::class.java)) // consume: a rotation must not repeat the action
        when (action) {
            ACTION_ACCEPT -> withPermissions(manager.state.value.video) { manager.accept() }
            ACTION_DECLINE -> { manager.decline(); finish() }
            ACTION_OUTGOING -> {
                val peer = intent.getStringExtra(EXTRA_PEER) ?: return
                val video = intent.getBooleanExtra(EXTRA_VIDEO, false)
                withPermissions(video) { manager.placeCall(peer, video) }
            }
        }
    }

    private fun withPermissions(video: Boolean, then: () -> Unit) {
        val needed = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (video) add(Manifest.permission.CAMERA)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isEmpty()) then()
        else {
            afterPermissions = then
            permissions.launch(needed.toTypedArray())
        }
    }

    companion object {
        const val ACTION_ACCEPT = "com.impactomsn.app.calls.ACCEPT"
        const val ACTION_DECLINE = "com.impactomsn.app.calls.DECLINE"
        const val ACTION_OUTGOING = "com.impactomsn.app.calls.OUTGOING"
        private const val EXTRA_PEER = "peer"
        private const val EXTRA_VIDEO = "video"

        fun incomingIntent(context: Context, action: String?): Intent =
            Intent(context, CallActivity::class.java).setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun outgoingIntent(context: Context, peerEmail: String, video: Boolean): Intent =
            Intent(context, CallActivity::class.java).setAction(ACTION_OUTGOING)
                .putExtra(EXTRA_PEER, peerEmail).putExtra(EXTRA_VIDEO, video)
    }
}

@Composable
private fun CallScreen(
    state: CallState,
    remote: Bitmap?,
    local: Bitmap?,
    peerAvatar: Bitmap?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onHangup: () -> Unit,
    onMute: (Boolean) -> Unit,
    onCamera: (Boolean) -> Unit,
    onSpeaker: (Boolean) -> Unit,
    onSwitchCamera: () -> Unit,
) {
    var muted by remember { mutableStateOf(false) }
    var cameraOn by remember(state.video) { mutableStateOf(state.video) }
    var speaker by remember(state.video) { mutableStateOf(state.video) }
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.Active) {
            onSpeaker(speaker)
            seconds = 0
            while (true) { delay(1000); seconds++ }
        }
    }

    val status = when (state.phase) {
        CallPhase.Calling -> "Calling…"
        CallPhase.Ringing -> if (state.video) "Incoming video call" else "Incoming voice call"
        CallPhase.Connecting -> "Connecting…"
        CallPhase.Active -> "%d:%02d".format(seconds / 60, seconds % 60)
        CallPhase.Idle -> state.message ?: "Call ended"
    }
    val showVideo = state.phase == CallPhase.Active && remote != null

    Column(Modifier.fillMaxSize().msnWindowBackground().background(MsnPalette.Content.copy(alpha = 0f))) {
        // Title strip, like the blue window frame of the PC client.
        Column(
            Modifier.fillMaxWidth().background(MsnPalette.TitleBlue).statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text("MSN Messenger 8.5", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(
                state.peer.ifBlank { " " }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(status, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            if (showVideo) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(MsnPalette.Toolbar)
                        .border(2.dp, MsnPalette.GlowTop, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(remote!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MsnAvatarFrame(size = 168.dp) {
                        if (!state.video && peerAvatar != null) {
                            Image(
                                peerAvatar.asImageBitmap(), contentDescription = stringResource(R.string.call_contact_picture),
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(state.peer.firstOrNull()?.uppercase() ?: "?", color = MsnPalette.TitleBlue, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.video && state.phase == CallPhase.Active) {
                        Text(stringResource(R.string.call_waiting_video), color = MsnPalette.TitleBlueDeep, fontSize = 14.sp)
                    }
                    state.message?.takeIf { state.phase != CallPhase.Idle }?.let {
                        Text(it, color = MsnPalette.TitleBlueDeep, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            if (local != null && cameraOn && state.phase == CallPhase.Active) {
                Image(
                    local.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(width = 96.dp, height = 128.dp)
                        .clip(RoundedCornerShape(12.dp)).border(2.dp, Color.White, RoundedCornerShape(12.dp)),
                )
            }
        }

        // Toolbar panel with glass buttons.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Brush.verticalGradient(listOf(MsnPalette.GlowTop, MsnPalette.GlowBottom)))
                .navigationBarsPadding().padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            if (state.phase == CallPhase.Ringing) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallButton(Icons.Default.CallEnd, "Decline", Color(0xFFD33B2C), 76.dp, onDecline)
                    CallButton(Icons.Default.Call, "Answer", MsnPalette.Online, 76.dp, onAccept)
                }
            } else if (state.phase != CallPhase.Idle) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                    CallButton(if (muted) Icons.Default.MicOff else Icons.Default.Mic, if (muted) "Unmute" else "Mute", if (muted) MsnPalette.Offline else MsnPalette.TitleBlue) { muted = !muted; onMute(muted) }
                    CallButton(if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff, "Camera", if (cameraOn) MsnPalette.TitleBlue else MsnPalette.Offline) { cameraOn = !cameraOn; onCamera(cameraOn) }
                    CallButton(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", if (speaker) MsnPalette.TitleBlue else MsnPalette.Offline) { speaker = !speaker; onSpeaker(speaker) }
                    if (cameraOn) CallButton(Icons.Default.Cameraswitch, "Flip", MsnPalette.TitleBlue, onClick = onSwitchCamera)
                    CallButton(Icons.Default.CallEnd, "End", Color(0xFFD33B2C), onClick = onHangup)
                }
            } else {
                Spacer(Modifier.height(56.dp))
            }
        }
    }
}

@Composable
private fun CallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.85f).compositeOver(Color.White), color)))
                .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape),
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(size * 0.46f))
        }
        Text(label, color = MsnPalette.Ink, fontSize = 12.sp, maxLines = 1)
    }
}
