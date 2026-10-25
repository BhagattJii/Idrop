package com.idrop.x.ui

import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.idrop.x.BuildConfig
import com.idrop.x.diagnostics.CheckStatus
import com.idrop.x.diagnostics.SelfTestCheck
import com.idrop.x.diagnostics.SelfTestState
import com.idrop.x.nearby.BleStatus
import com.idrop.x.nearby.NearbyPeer
import com.idrop.x.nearby.NearbyPermissions
import com.idrop.x.nearby.NearbyState
import com.idrop.x.nearby.NearbyViewModel
import com.idrop.x.nearby.TransferDirection
import com.idrop.x.nearby.TransferRecord
import com.idrop.x.nearby.TransferStatus
import com.idrop.x.ui.theme.Amber
import com.idrop.x.ui.theme.Aurora
import com.idrop.x.ui.theme.Cyan
import com.idrop.x.ui.theme.IdropTheme
import com.idrop.x.ui.theme.Ink
import com.idrop.x.ui.theme.Iris
import com.idrop.x.ui.theme.Mint
import com.idrop.x.ui.theme.OutlineSoft
import com.idrop.x.ui.theme.Panel
import com.idrop.x.ui.theme.PanelRaised
import com.idrop.x.ui.theme.TextMuted
import com.idrop.x.ui.theme.TextPrimary
import com.idrop.x.ui.theme.Violet
import com.idrop.x.util.formatBytes
import com.idrop.x.util.formatRelativeTime
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class IdropScreen {
    Home,
    Transfers,
    Settings,
    Diagnostics,
}

@Composable
fun IdropApp(vm: NearbyViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val deviceName by vm.deviceName.collectAsStateWithLifecycle()
    val selfTest by vm.selfTest.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var screen by rememberSaveable { mutableStateOf(IdropScreen.Home) }
    var permissionsGranted by remember { mutableStateOf(NearbyPermissions.hasAll(context)) }
    var popupPeer by remember { mutableStateOf<NearbyPeer?>(null) }
    var popupDistance by remember { mutableStateOf<Float?>(null) }
    var dismissedPeerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var proximityTick by remember { mutableStateOf(0L) }

    var sendTarget by remember { mutableStateOf<NearbyPeer?>(null) }
    var showSendSheet by remember { mutableStateOf(false) }
    var textTarget by remember { mutableStateOf<NearbyPeer?>(null) }
    var textDraft by remember { mutableStateOf("") }
    var showTextDialog by remember { mutableStateOf(false) }
    var pickTarget by remember { mutableStateOf<NearbyPeer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsGranted = NearbyPermissions.hasAll(context)
        if (permissionsGranted) vm.start()
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val target = pickTarget
        if (target != null && uris.isNotEmpty()) vm.sendFiles(target, uris)
        pickTarget = null
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) vm.start()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000L)
            proximityTick++
        }
    }

    LaunchedEffect(state.peers) {
        val currentIds = state.peers.map { it.endpointId }.toSet()
        dismissedPeerIds = dismissedPeerIds.intersect(currentIds)
    }

    LaunchedEffect(
        state.peers,
        state.connected,
        state.beacons,
        state.bleStatus,
        permissionsGranted,
        screen,
        proximityTick,
    ) {
        if (!permissionsGranted || screen != IdropScreen.Home || popupPeer != null) {
            return@LaunchedEffect
        }
        val candidates = state.peers.filter { peer ->
            state.connected.none { it.endpointId == peer.endpointId } &&
                peer.endpointId !in dismissedPeerIds
        }
        if (candidates.isEmpty()) return@LaunchedEffect

        val now = System.currentTimeMillis()
        val fresh = state.beacons.values.filter { now - it.lastSeen <= 7_000L }

        fun distanceFor(peer: NearbyPeer): Float? {
            val id = peer.proximityId ?: return null
            return fresh.firstOrNull { it.id == id }?.distanceMeters
        }

        val close = candidates
            .mapNotNull { peer -> distanceFor(peer)?.let { peer to it } }
            .filter { it.second <= 2.5f }
            .minByOrNull { it.second }

        if (close != null) {
            popupPeer = close.first
            popupDistance = close.second
            return@LaunchedEffect
        }

        fun fallback() {
            val first = candidates.minByOrNull { it.discoveredAt }
            popupPeer = first
            popupDistance = null
        }

        when (state.bleStatus) {
            BleStatus.Starting, BleStatus.Off -> {
                val waited = candidates.minByOrNull { it.discoveredAt }
                if (waited != null && now - waited.discoveredAt > 8_000L) fallback()
            }

            BleStatus.Unavailable, BleStatus.Failed -> fallback()

            BleStatus.Active, BleStatus.ScanOnly -> {
                if (fresh.isEmpty()) {
                    val waited = candidates.minByOrNull { it.discoveredAt }
                    if (waited != null && now - waited.discoveredAt > 8_000L) fallback()
                }
            }
        }
    }

    LaunchedEffect(popupPeer) {
        if (popupPeer == null) return@LaunchedEffect
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

    IdropTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink),
        ) {
            AuroraBackground()

            Crossfade(
                targetState = screen,
                animationSpec = tween(240),
                label = "screen",
            ) { target ->
                when (target) {
                    IdropScreen.Home -> HomeScreen(
                        state = state,
                        permissionsGranted = permissionsGranted,
                        onRequestPermissions = { permissionLauncher.launch(NearbyPermissions.required()) },
                        onStart = vm::start,
                        onStop = vm::stop,
                        onConnect = vm::connect,
                        onSend = { peer ->
                            sendTarget = peer
                            showSendSheet = true
                        },
                        onDismissError = vm::clearError,
                        onHistory = { screen = IdropScreen.Transfers },
                        onSettings = { screen = IdropScreen.Settings },
                        onDiagnostics = { screen = IdropScreen.Diagnostics },
                    )

                    IdropScreen.Transfers -> TransfersScreen(
                        transfers = state.transfers,
                        onBack = { screen = IdropScreen.Home },
                    )

                    IdropScreen.Settings -> SettingsScreen(
                        state = state,
                        deviceName = deviceName,
                        onRename = vm::rename,
                        onBack = { screen = IdropScreen.Home },
                        onStart = vm::start,
                        onStop = vm::stop,
                        onDiagnostics = { screen = IdropScreen.Diagnostics },
                    )

                    IdropScreen.Diagnostics -> DiagnosticsScreen(
                        state = state,
                        selfTest = selfTest,
                        onBack = { screen = IdropScreen.Home },
                        onClear = vm::clearLogs,
                        onRunTest = vm::runSelfTest,
                        onResetTest = vm::resetSelfTest,
                    )
                }
            }

            popupPeer?.let { peer ->
                Dialog(
                    onDismissRequest = {
                        dismissedPeerIds = dismissedPeerIds + peer.endpointId
                        popupPeer = null
                        popupDistance = null
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    ProximityDialog(
                        peer = peer,
                        distanceMeters = popupDistance,
                        onConnect = {
                            dismissedPeerIds = dismissedPeerIds + peer.endpointId
                            vm.connect(peer)
                            popupPeer = null
                            popupDistance = null
                            screen = IdropScreen.Home
                        },
                        onDismiss = {
                            dismissedPeerIds = dismissedPeerIds + peer.endpointId
                            popupPeer = null
                            popupDistance = null
                        },
                    )
                }
            }

            if (showSendSheet) {
                sendTarget?.let { peer ->
                    SendSheet(
                        peer = peer,
                        onFiles = {
                            showSendSheet = false
                            pickTarget = peer
                            filePicker.launch(arrayOf("*/*"))
                        },
                        onPhotos = {
                            showSendSheet = false
                            pickTarget = peer
                            filePicker.launch(arrayOf("image/*"))
                        },
                        onText = {
                            showSendSheet = false
                            textTarget = peer
                            textDraft = ""
                            showTextDialog = true
                        },
                        onTestFile = {
                            showSendSheet = false
                            vm.sendTestFile(peer)
                        },
                        onDismiss = { showSendSheet = false },
                    )
                }
            }

            if (showTextDialog) {
                textTarget?.let { peer ->
                    TextComposeDialog(
                        peer = peer,
                        text = textDraft,
                        onTextChange = { textDraft = it.take(4000) },
                        onSend = {
                            vm.sendText(peer, textDraft)
                            showTextDialog = false
                            textDraft = ""
                        },
                        onDismiss = { showTextDialog = false },
                    )
                }
            }

            state.receivedText?.let { text ->
                ReceivedTextDialog(
                    text = text,
                    onDismiss = vm::clearReceivedText,
                )
            }

            state.lastSavedPath?.let { path ->
                SavedFileDialog(
                    path = path,
                    onDismiss = vm::clearLastSaved,
                )
            }
        }
    }
}

@Composable
private fun AuroraBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Ink, Color(0xFF0A1024), Ink),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Iris.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.18f),
                radius = size.width * 0.72f,
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.12f, size.height * 0.18f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Cyan.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.62f),
                radius = size.width * 0.66f,
            ),
            radius = size.width * 0.66f,
            center = Offset(size.width * 0.92f, size.height * 0.62f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Violet.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.98f),
                radius = size.width * 0.7f,
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.5f, size.height * 0.98f),
        )
    }
}

@Composable
private fun HomeScreen(
    state: NearbyState,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onConnect: (NearbyPeer) -> Unit,
    onSend: (NearbyPeer) -> Unit,
    onDismissError: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    val connected = state.connected
    val nearby = state.peers.filter { peer ->
        connected.none { it.endpointId == peer.endpointId }
    }
    val allNearby = (state.peers + connected).distinctBy { it.endpointId }
    val scanning = state.isAdvertising || state.isDiscovering
    val activeTransfers = state.transfers.filter { it.status == TransferStatus.InProgress }
    val now = System.currentTimeMillis()

    fun distanceFor(peer: NearbyPeer): Float? {
        val id = peer.proximityId ?: return null
        val beacon = state.beacons[id] ?: return null
        return if (now - beacon.lastSeen <= 7_000L) beacon.distanceMeters else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Header(
            onHistory = onHistory,
            onSettings = onSettings,
        )

        Spacer(Modifier.height(10.dp))

        RadarPulse(
            active = scanning && permissionsGranted,
            peerCount = allNearby.size,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        )

        Text(
            text = when {
                !permissionsGranted -> "Nearby access needed"
                !scanning -> "Ready when you are"
                allNearby.isEmpty() -> "Looking for nearby devices"
                else -> "${allNearby.size} nearby"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                !permissionsGranted -> "Allow Bluetooth + nearby Wi-Fi. No internet, no account."
                !scanning -> "Start discovery to find iDrop devices around you."
                allNearby.isEmpty() -> "Bring both phones close. No router, no pairing."
                else -> "Tap a device to connect, then send files or text."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(22.dp))

        if (!permissionsGranted) {
            PermissionCard(onRequestPermissions = onRequestPermissions)
        } else if (!scanning) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Iris),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Start nearby discovery", fontWeight = FontWeight.SemiBold)
            }
        } else {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, OutlineSoft),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = TextMuted)
                Spacer(Modifier.width(8.dp))
                Text("Stop discovery", color = TextMuted)
            }
        }

        state.lastError?.let { error ->
            Spacer(Modifier.height(12.dp))
            ErrorCard(message = error, onDismiss = onDismissError)
        }

        if (activeTransfers.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionLabel("TRANSFERRING", activeTransfers.size)
            Spacer(Modifier.height(10.dp))
            GlassCard {
                activeTransfers.take(3).forEachIndexed { index, transfer ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    ActiveTransferRow(transfer)
                }
            }
        }

        if (connected.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionLabel("CONNECTED", connected.size)
            Spacer(Modifier.height(10.dp))
            connected.forEach { peer ->
                val active = state.transfers.firstOrNull {
                    it.peerName == peer.name && it.status == TransferStatus.InProgress
                }
                DeviceCard(
                    peer = peer,
                    connected = true,
                    activeTransfer = active,
                    distanceMeters = null,
                    onConnect = {},
                    onSend = { onSend(peer) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
        SectionLabel("NEARBY", nearby.size)
        Spacer(Modifier.height(10.dp))

        if (nearby.isEmpty()) {
            EmptyNearby(scanning = scanning && permissionsGranted)
        } else {
            nearby.forEach { peer ->
                DeviceCard(
                    peer = peer,
                    connected = false,
                    activeTransfer = null,
                    distanceMeters = distanceFor(peer),
                    onConnect = { onConnect(peer) },
                    onSend = {},
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDiagnostics)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Self-test, logs & diagnostics",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Header(onHistory: () -> Unit, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Aurora)
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(25.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "iDrop",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            Text(
                "OFFLINE. INSTANT. YOURS.",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onHistory) {
            Icon(Icons.Rounded.History, contentDescription = "Transfers", tint = TextMuted)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextMuted)
        }
    }
}

@Composable
private fun RadarPulse(active: Boolean, peerCount: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 2800 else 5200,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 10.dp.toPx()

            for (index in 0..3) {
                val radius = maxRadius * (0.32f + index * 0.21f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            drawCircle(
                color = Iris.copy(alpha = 0.10f),
                radius = maxRadius,
                center = center,
            )

            val sweepRadius = maxRadius * (0.18f + phase * 0.82f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = 0.16f), Color.Transparent),
                    center = center,
                    radius = sweepRadius,
                ),
                radius = sweepRadius,
                center = center,
            )

            if (peerCount > 0) {
                val count = peerCount.coerceAtMost(8)
                val orbit = maxRadius * 0.62f
                repeat(count) { index ->
                    val angle = (index * (360.0 / count) + phase * 360.0) * PI / 180.0
                    val dot = Offset(
                        x = center.x + cos(angle).toFloat() * orbit,
                        y = center.y + sin(angle).toFloat() * orbit,
                    )
                    drawCircle(
                        color = Cyan.copy(alpha = 0.18f),
                        radius = 11.dp.toPx(),
                        center = dot,
                    )
                    drawCircle(
                        color = Cyan,
                        radius = 4.dp.toPx(),
                        center = dot,
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Iris.copy(alpha = 0.55f), Iris.copy(alpha = 0.10f), Color.Transparent),
                    center = center,
                    radius = maxRadius * 0.45f,
                ),
                radius = maxRadius * 0.45f,
                center = center,
            )
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Aurora)
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermissions: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Iris.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Nearby access", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Bluetooth scan + nearby Wi-Fi. Nothing leaves your phones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Iris),
        ) {
            Text("Allow nearby access")
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF3A1420).copy(alpha = 0.72f))
            .border(1.dp, Color(0xFFFF6B81).copy(alpha = 0.32f), shape)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            tint = Color(0xFFFF8A9B),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFFFC2CB),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EmptyNearby(scanning: Boolean) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PanelRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (scanning) "No devices yet" else "Discovery is off",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    if (scanning) {
                        "Keep both phones unlocked and close. This can take a few seconds."
                    } else {
                        "Start discovery to see nearby iDrop devices."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    peer: NearbyPeer,
    connected: Boolean,
    activeTransfer: TransferRecord?,
    distanceMeters: Float?,
    onConnect: () -> Unit,
    onSend: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = peer.name)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            if (activeTransfer != null) {
                Text(
                    text = "${activeTransfer.fileName} · ${(activeTransfer.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Cyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                ProgressBar(progress = activeTransfer.progress)
            } else {
                Text(
                    text = when {
                        connected -> "Connected securely"
                        distanceMeters != null -> "${formatDistance(distanceMeters)} · ready to connect"
                        else -> "iDrop device nearby"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connected) Cyan else TextMuted,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (connected) {
            Button(
                onClick = onSend,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Iris.copy(alpha = 0.9f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            OutlinedButton(
                onClick = onConnect,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Iris.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Connect", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActiveTransferRow(transfer: TransferRecord) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (transfer.direction == TransferDirection.Outgoing) Iris.copy(alpha = 0.18f) else Cyan.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (transfer.direction == TransferDirection.Outgoing) {
                    Icons.Rounded.ArrowUpward
                } else {
                    Icons.Rounded.ArrowDownward
                },
                contentDescription = null,
                tint = if (transfer.direction == TransferDirection.Outgoing) Iris else Cyan,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transfer.fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            ProgressBar(progress = transfer.progress)
            Spacer(Modifier.height(4.dp))
            Text(
                "${formatBytes(transfer.bytesTransferred)} of ${formatBytes(transfer.totalBytes)} · ${transfer.peerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, height: Int = 5) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.09f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Aurora),
        )
    }
}

@Composable
private fun Avatar(name: String) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "i"
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Iris.copy(alpha = 0.85f), Cyan.copy(alpha = 0.65f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SectionLabel(label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Cyan,
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (count > 0) Cyan else TextMuted.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun ProximityDialog(
    peer: NearbyPeer,
    distanceMeters: Float?,
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PanelRaised, Panel),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "NEARBY DEVICE",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan,
            )
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Iris.copy(alpha = 0.22f),
                                Violet.copy(alpha = 0.18f),
                                Cyan.copy(alpha = 0.16f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AvatarLarge(peer.name)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = peer.name,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (distanceMeters != null) {
                    "is about ${formatDistance(distanceMeters)} and ready to connect."
                } else {
                    "is nearby and ready to connect."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Direct, encrypted, no internet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Iris),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connect", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDismiss) {
                Text("Not now", color = TextMuted)
            }
        }
    }
}

@Composable
private fun AvatarLarge(name: String) {
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "i"
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Aurora)
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun SendSheet(
    peer: NearbyPeer,
    onFiles: () -> Unit,
    onPhotos: () -> Unit,
    onText: () -> Unit,
    onTestFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(Brush.verticalGradient(listOf(PanelRaised, Panel)))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SEND TO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            peer.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
                Spacer(Modifier.height(16.dp))
                SendOption(
                    icon = Icons.Rounded.Folder,
                    title = "Files",
                    subtitle = "Any file type · multiple selection",
                    onClick = onFiles,
                )
                Spacer(Modifier.height(8.dp))
                SendOption(
                    icon = Icons.Rounded.Image,
                    title = "Photos & videos",
                    subtitle = "Pick from your gallery",
                    onClick = onPhotos,
                )
                Spacer(Modifier.height(8.dp))
                SendOption(
                    icon = Icons.Rounded.TextFields,
                    title = "Text message",
                    subtitle = "Send a note or a link instantly",
                    onClick = onText,
                )
                Spacer(Modifier.height(8.dp))
                SendOption(
                    icon = Icons.Rounded.Speed,
                    title = "5 MB speed test",
                    subtitle = "Measure real transfer speed",
                    onClick = onTestFile,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SendOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Iris.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

@Composable
private fun TextComposeDialog(
    peer: NearbyPeer,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(PanelRaised)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .imePadding()
                .padding(22.dp),
        ) {
            Text("SEND TEXT", style = MaterialTheme.typography.labelSmall, color = Cyan)
            Spacer(Modifier.height(6.dp))
            Text(
                "To ${peer.name}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Type a message or paste a link…", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Cyan,
                    focusedBorderColor = Iris,
                    unfocusedBorderColor = OutlineSoft,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OutlineSoft),
                ) {
                    Text("Cancel", color = TextMuted)
                }
                Button(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Iris),
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun ReceivedTextDialog(text: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(PanelRaised, Panel)))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(22.dp),
        ) {
            Text("TEXT RECEIVED", style = MaterialTheme.typography.labelSmall, color = Cyan)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(text)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OutlineSoft),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextMuted)
                    Spacer(Modifier.width(6.dp))
                    Text("Copy", color = TextPrimary)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Iris),
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun SavedFileDialog(path: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(PanelRaised, Panel)))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Mint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Mint, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("File received", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Saved safely on this phone",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                path,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                color = Cyan,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(path)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OutlineSoft),
                ) {
                    Text("Copy path", color = TextPrimary)
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Iris),
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.025f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun TransfersScreen(transfers: List<TransferRecord>, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackHeader(title = "Transfers", onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text(
            "Everything stays on your devices. Received files are saved in Downloads/iDrop.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(16.dp))
        if (transfers.isEmpty()) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PanelRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("No transfers yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "Connect to a nearby phone and send your first file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(transfers) { transfer ->
                    TransferRow(transfer)
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun TransferRow(transfer: TransferRecord) {
    val shape = RoundedCornerShape(18.dp)
    val statusColor = when (transfer.status) {
        TransferStatus.Complete -> Mint
        TransferStatus.Failed -> Color(0xFFFF6B81)
        TransferStatus.InProgress -> Amber
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (transfer.direction == TransferDirection.Outgoing) Iris.copy(alpha = 0.16f) else Cyan.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (transfer.direction == TransferDirection.Outgoing) {
                    Icons.Rounded.ArrowUpward
                } else {
                    Icons.Rounded.ArrowDownward
                },
                contentDescription = null,
                tint = if (transfer.direction == TransferDirection.Outgoing) Iris else Cyan,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transfer.fileName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${if (transfer.direction == TransferDirection.Outgoing) "To" else "From"} ${transfer.peerName} · ${formatBytes(transfer.totalBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (transfer.status == TransferStatus.InProgress) {
                Spacer(Modifier.height(8.dp))
                ProgressBar(progress = transfer.progress)
            }
            transfer.savedPath?.let { path ->
                Spacer(Modifier.height(3.dp))
                Text(
                    path,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = Cyan.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = when (transfer.status) {
                    TransferStatus.Complete -> "Done"
                    TransferStatus.Failed -> "Failed"
                    TransferStatus.InProgress -> "${(transfer.progress * 100).toInt()}%"
                },
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                formatRelativeTime(transfer.timestamp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    state: NearbyState,
    deviceName: String,
    onRename: (String) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    var nameDraft by remember(deviceName) { mutableStateOf(deviceName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackHeader(title = "Settings", onBack = onBack)
        Spacer(Modifier.height(18.dp))

        GlassCard {
            SettingRow(
                title = "Nearby visibility",
                subtitle = if (state.isAdvertising) "Visible to nearby iDrop devices" else "Hidden",
                trailing = {
                    Switch(
                        checked = state.isAdvertising,
                        onCheckedChange = { enabled -> if (enabled) onStart() else onStop() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Iris,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = PanelRaised,
                        ),
                    )
                },
            )
            Spacer(Modifier.height(16.dp))
            Text("Device name", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                "Shown to nearby phones",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { if (it.length <= 30) nameDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Cyan,
                    focusedBorderColor = Iris,
                    unfocusedBorderColor = OutlineSoft,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { onRename(nameDraft) },
                    enabled = nameDraft.isNotBlank() && nameDraft.trim() != deviceName,
                ) {
                    Text("Save name", color = Cyan)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        GlassCard {
            SettingRow(
                title = "Theme",
                subtitle = "Aurora Dark • Material 3",
            )
            Spacer(Modifier.height(14.dp))
            SettingRow(
                title = "Transfer engine",
                subtitle = "Nearby Connections • encrypted P2P",
            )
            Spacer(Modifier.height(14.dp))
            SettingRow(
                title = "Received files",
                subtitle = "Downloads/iDrop on this phone",
            )
            Spacer(Modifier.height(14.dp))
            SettingRow(
                title = "Privacy",
                subtitle = "No account, no cloud, no tracking",
            )
        }

        Spacer(Modifier.height(16.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("About iDrop", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME} • MIT License\n" +
                    "Advanced offline sharing for Android.\n" +
                    "No ads. No subscriptions. No owner fees.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDiagnostics) {
                Text("Open self-test & diagnostics", color = Cyan)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    state: NearbyState,
    selfTest: SelfTestState,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onRunTest: () -> Unit,
    onResetTest: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val shareReport = {
        if (selfTest.report.isNotBlank()) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "iDrop self-test report")
                putExtra(Intent.EXTRA_TEXT, selfTest.report)
            }
            context.startActivity(Intent.createChooser(intent, "Share self-test report"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackHeader(title = "Diagnostics", onBack = onBack)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Advertise", state.isAdvertising)
            StatusChip("Discover", state.isDiscovering)
            StatusChip("Close-range", state.bleStatus == BleStatus.Active)
        }
        Spacer(Modifier.height(8.dp))
        val nearestBeacon = state.beacons.values.minByOrNull { it.distanceMeters }
        Text(
            text = "BLE proximity: ${state.bleStatus.name}" +
                (nearestBeacon?.let { " · nearest ${formatDistance(it.distanceMeters)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SelfTestCard(
                    selfTest = selfTest,
                    onRunTest = onRunTest,
                    onResetTest = onResetTest,
                    onShareReport = shareReport,
                )
            }
            item {
                Text(
                    "Connected: ${state.connected.joinToString { it.name }.ifBlank { "none" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
            state.lastSavedPath?.let { path ->
                item {
                    Text(
                        "Last saved: $path",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cyan,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { clipboard.setText(AnnotatedString(state.logs.joinToString("\n"))) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, OutlineSoft),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy logs", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onClear,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, OutlineSoft),
                    ) {
                        Text("Clear", color = TextMuted)
                    }
                }
            }
            item {
                Text(
                    "EVENT LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            if (state.logs.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            "No events yet. Start discovery and bring another phone close.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                }
            } else {
                items(state.logs) { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        ),
                        color = TextMuted,
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SelfTestCard(
    selfTest: SelfTestState,
    onRunTest: () -> Unit,
    onResetTest: () -> Unit,
    onShareReport: () -> Unit,
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Iris.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("One-tap self-test", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "Checks radios, permissions, storage and a second phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRunTest,
            enabled = !selfTest.running,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Iris),
        ) {
            if (selfTest.running) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text("Testing nearby radios…")
            } else {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (selfTest.finished) "Run self-test again" else "Run self-test")
            }
        }
        if (selfTest.checks.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            selfTest.checks.forEach { check ->
                SelfTestRow(check)
                Spacer(Modifier.height(9.dp))
            }
        }
        if (selfTest.finished) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShareReport,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Iris.copy(alpha = 0.55f)),
                ) {
                    Text("Share report", color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onResetTest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OutlineSoft),
                ) {
                    Text("Reset", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun SelfTestRow(check: SelfTestCheck) {
    val color = when (check.status) {
        CheckStatus.Pass -> Mint
        CheckStatus.Warn -> Amber
        CheckStatus.Fail -> Color(0xFFFF6B81)
        CheckStatus.Running -> Iris
        CheckStatus.Pending -> TextMuted
    }
    val label = when (check.status) {
        CheckStatus.Pass -> "PASS"
        CheckStatus.Warn -> "WARN"
        CheckStatus.Fail -> "FAIL"
        CheckStatus.Running -> "…"
        CheckStatus.Pending -> "WAIT"
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                check.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                check.detail,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun StatusChip(label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (active) Cyan else TextMuted.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (active) TextPrimary else TextMuted)
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
    }
}

private fun formatDistance(meters: Float): String {
    val rounded = (meters * 10f).roundToInt() / 10f
    return if (rounded < 1f) {
        "${(rounded * 100f).roundToInt()} cm away"
    } else {
        "$rounded m away"
    }
}
