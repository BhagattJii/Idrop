package com.idrop.x.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.idrop.x.nearby.NearbyPeer
import com.idrop.x.nearby.NearbyPermissions
import com.idrop.x.nearby.NearbyState
import com.idrop.x.nearby.NearbyViewModel
import com.idrop.x.ui.theme.Aurora
import com.idrop.x.ui.theme.Cyan
import com.idrop.x.ui.theme.IdropTheme
import com.idrop.x.ui.theme.Ink
import com.idrop.x.ui.theme.Iris
import com.idrop.x.ui.theme.OutlineSoft
import com.idrop.x.ui.theme.Panel
import com.idrop.x.ui.theme.PanelRaised
import com.idrop.x.ui.theme.TextMuted
import com.idrop.x.ui.theme.TextPrimary
import com.idrop.x.ui.theme.Violet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import com.idrop.x.BuildConfig

private enum class IdropScreen {
    Home,
    Settings,
    Diagnostics,
}

@Composable
fun IdropApp(vm: NearbyViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val deviceName by vm.deviceName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var screen by rememberSaveable { mutableStateOf(IdropScreen.Home) }
    var permissionsGranted by remember { mutableStateOf(NearbyPermissions.hasAll(context)) }
    var popupPeer by remember { mutableStateOf<NearbyPeer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionsGranted = NearbyPermissions.hasAll(context)
        if (permissionsGranted) vm.start()
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) vm.start()
    }

    LaunchedEffect(state.peers, state.connected, permissionsGranted) {
        if (permissionsGranted && popupPeer == null) {
            popupPeer = state.peers.firstOrNull { peer ->
                state.connected.none { it.endpointId == peer.endpointId }
            }
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
                        onSendTest = vm::sendTestFile,
                        onSettings = { screen = IdropScreen.Settings },
                        onDiagnostics = { screen = IdropScreen.Diagnostics },
                    )

                    IdropScreen.Settings -> SettingsScreen(
                        state = state,
                        deviceName = deviceName,
                        onBack = { screen = IdropScreen.Home },
                        onStart = vm::start,
                        onStop = vm::stop,
                        onDiagnostics = { screen = IdropScreen.Diagnostics },
                    )

                    IdropScreen.Diagnostics -> DiagnosticsScreen(
                        state = state,
                        onBack = { screen = IdropScreen.Home },
                        onClear = vm::clearLogs,
                    )
                }
            }

            popupPeer?.let { peer ->
                Dialog(
                    onDismissRequest = { popupPeer = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    ProximityDialog(
                        peer = peer,
                        onConnect = {
                            vm.connect(peer)
                            popupPeer = null
                        },
                        onDismiss = { popupPeer = null },
                    )
                }
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
                colors = listOf(Color(0xFFA855F7).copy(alpha = 0.10f), Color.Transparent),
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
    onSendTest: (NearbyPeer) -> Unit,
    onSettings: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    val nearby = state.peers.filter { peer ->
        state.connected.none { it.endpointId == peer.endpointId }
    }
    val scanning = state.isAdvertising || state.isDiscovering

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
            onSettings = onSettings,
            onDiagnostics = onDiagnostics,
        )

        Spacer(Modifier.height(10.dp))

        RadarPulse(
            active = scanning && permissionsGranted,
            peerCount = nearby.size + state.connected.size,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        )

        Text(
            text = when {
                !permissionsGranted -> "Nearby access needed"
                !scanning -> "Ready when you are"
                nearby.isEmpty() && state.connected.isEmpty() -> "Looking for nearby devices"
                else -> "${nearby.size + state.connected.size} nearby"
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
                nearby.isEmpty() && state.connected.isEmpty() -> "Bring both phones close. No router, no pairing."
                else -> "Tap Connect. iDrop handles the rest automatically."
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
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = TextMuted)
                Spacer(Modifier.width(8.dp))
                Text("Stop discovery", color = TextMuted)
            }
        }

        if (state.connected.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionLabel("CONNECTED", state.connected.size)
            Spacer(Modifier.height(10.dp))
            state.connected.forEach { peer ->
                DeviceCard(
                    peer = peer,
                    connected = true,
                    progress = state.transferProgress[peer.endpointId],
                    onConnect = {},
                    onSendTest = { onSendTest(peer) },
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
                    progress = state.transferProgress[peer.endpointId],
                    onConnect = { onConnect(peer) },
                    onSendTest = {},
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
                "Diagnostics & logs",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun Header(onSettings: () -> Unit, onDiagnostics: () -> Unit) {
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
        IconButton(onClick = onDiagnostics) {
            Icon(Icons.Rounded.Info, contentDescription = "Diagnostics", tint = TextMuted)
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
                    if (scanning) "Keep both phones unlocked and close. This can take a few seconds."
                    else "Start discovery to see nearby iDrop devices.",
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
    progress: Float?,
    onConnect: () -> Unit,
    onSendTest: () -> Unit,
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
            Text(
                text = if (connected) "Connected securely" else "iDrop device nearby",
                style = MaterialTheme.typography.bodyMedium,
                color = if (connected) Cyan else TextMuted,
            )
            if (progress != null && progress < 1f) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
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
        }
        Spacer(Modifier.width(10.dp))
        if (connected) {
            Button(
                onClick = onSendTest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Iris.copy(alpha = 0.9f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("5 MB", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            OutlinedButton(
                onClick = onConnect,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Iris.copy(alpha = 0.55f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Connect", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }
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
private fun ProximityDialog(peer: NearbyPeer, onConnect: () -> Unit, onDismiss: () -> Unit) {
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
                text = "is right next to you and wants to connect.",
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
private fun SettingsScreen(
    state: NearbyState,
    deviceName: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    var sensitivity by remember { mutableFloatStateOf(0.6f) }

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
            Spacer(Modifier.height(14.dp))
            SettingRow(
                title = "Device name",
                subtitle = deviceName,
            )
            Spacer(Modifier.height(14.dp))
            SettingRow(
                title = "Proximity sensitivity",
                subtitle = if (sensitivity > 0.7f) "Close range" else if (sensitivity > 0.4f) "Balanced" else "Wide range",
            )
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Iris,
                    inactiveTrackColor = PanelRaised,
                ),
            )
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
                title = "Privacy",
                subtitle = "No account, no cloud, no tracking",
            )
        }

        Spacer(Modifier.height(16.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("About iDrop X", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                Text("Open diagnostics", color = Cyan)
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
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        BackHeader(title = "Diagnostics", onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Advertise", state.isAdvertising)
            StatusChip("Discover", state.isDiscovering)
            StatusChip("Peers", state.peers.isNotEmpty())
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Connected: ${state.connected.joinToString { it.name }.ifBlank { "none" }}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
        )
        if (state.lastReceived != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Last received: ${state.lastReceived}",
                style = MaterialTheme.typography.bodyMedium,
                color = Cyan,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(state.logs.joinToString("\n")))
                },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextMuted)
                Spacer(Modifier.width(6.dp))
                Text("Copy logs", color = TextPrimary)
            }
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft),
            ) {
                Text("Clear", color = TextMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (state.logs.isEmpty()) {
            GlassCard {
                Text(
                    "No events yet. Start discovery and bring another phone close.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
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
        }
        Spacer(Modifier.height(12.dp))
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
