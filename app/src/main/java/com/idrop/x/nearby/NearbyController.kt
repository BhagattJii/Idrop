package com.idrop.x.nearby

import android.content.Context
import android.os.Environment
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class NearbyPeer(
    val endpointId: String,
    val name: String,
)

data class NearbyState(
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val peers: List<NearbyPeer> = emptyList(),
    val connected: List<NearbyPeer> = emptyList(),
    val transferProgress: Map<String, Float> = emptyMap(),
    val lastReceived: String? = null,
    val logs: List<String> = emptyList(),
)

class NearbyController(private val context: Context) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val _state = MutableStateFlow(NearbyState())
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    private val serviceId = "com.idrop.x.nearby"
    private var myName = "iDrop"

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.FILE -> {
                    val incoming = payload.asFile()?.asJavaFile()
                    if (incoming != null) {
                        val downloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                        val destination = File(downloads, "iDrop-${System.currentTimeMillis()}-${incoming.name}")
                        incoming.copyTo(destination, overwrite = true)
                        log("Received ${incoming.length() / 1024} KB -> ${destination.absolutePath}")
                        _state.value = _state.value.copy(lastReceived = destination.absolutePath)
                    }
                }

                Payload.Type.BYTES -> {
                    val text = payload.asBytes()?.toString(Charsets.UTF_8)
                    log("Received text: ${text.orEmpty()}")
                    _state.value = _state.value.copy(lastReceived = text)
                }

                Payload.Type.STREAM -> {
                    val input = payload.asStream()?.asInputStream()
                    if (input != null) {
                        val destination = File(context.cacheDir, "iDrop-stream-${System.currentTimeMillis()}.bin")
                        destination.outputStream().use { output -> input.copyTo(output) }
                        log("Received stream -> ${destination.absolutePath}")
                        _state.value = _state.value.copy(lastReceived = destination.absolutePath)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val total = update.totalBytes.coerceAtLeast(1L)
            val progress = update.bytesTransferred.toFloat() / total.toFloat()
            _state.value = _state.value.copy(
                transferProgress = _state.value.transferProgress + (endpointId to progress.coerceIn(0f, 1f)),
            )
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> log("Transfer complete (${update.bytesTransferred / 1024} KB)")
                PayloadTransferUpdate.Status.FAILURE -> log("Transfer failed")
            }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            log("Connection request from ${info.endpointName}")
            client.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { error -> log("Accept failed: ${error.message}") }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val peer = _state.value.peers.firstOrNull { it.endpointId == endpointId }
                    ?: NearbyPeer(endpointId = endpointId, name = endpointId)
                log("Connected to ${peer.name}")
                _state.value = _state.value.copy(
                    connected = (_state.value.connected + peer).distinctBy { it.endpointId },
                )
            } else {
                log("Connection failed: ${result.status.statusMessage ?: "unknown"}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            log("Disconnected $endpointId")
            _state.value = _state.value.copy(
                connected = _state.value.connected.filterNot { it.endpointId == endpointId },
                transferProgress = _state.value.transferProgress - endpointId,
            )
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val peer = NearbyPeer(endpointId = endpointId, name = info.endpointName)
            log("Found ${info.endpointName}")
            _state.value = _state.value.copy(
                peers = (_state.value.peers + peer).distinctBy { it.endpointId },
            )
        }

        override fun onEndpointLost(endpointId: String) {
            _state.value = _state.value.copy(
                peers = _state.value.peers.filterNot { it.endpointId == endpointId },
            )
        }
    }

    fun start(name: String) {
        myName = name
        if (!_state.value.isAdvertising) {
            val options = AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            client.startAdvertising(name, serviceId, connectionCallback, options)
                .addOnSuccessListener {
                    _state.value = _state.value.copy(isAdvertising = true)
                    log("Advertising as $name")
                }
                .addOnFailureListener { error -> log("Advertise failed: ${error.message}") }
        }

        if (!_state.value.isDiscovering) {
            val options = DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            client.startDiscovery(serviceId, discoveryCallback, options)
                .addOnSuccessListener {
                    _state.value = _state.value.copy(isDiscovering = true)
                    log("Discovering nearby iDrop devices")
                }
                .addOnFailureListener { error -> log("Discovery failed: ${error.message}") }
        }
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        _state.value = _state.value.copy(
            isAdvertising = false,
            isDiscovering = false,
            peers = emptyList(),
            connected = emptyList(),
            transferProgress = emptyMap(),
        )
        log("Stopped")
    }

    fun connect(peer: NearbyPeer) {
        client.requestConnection(myName, peer.endpointId, connectionCallback)
            .addOnFailureListener { error -> log("Request failed: ${error.message}") }
    }

    fun sendTestFile(peer: NearbyPeer) {
        val file = File(context.cacheDir, "iDrop-test-5mb.bin")
        if (!file.exists() || file.length() < FIVE_MB) {
            file.outputStream().use { output ->
                val chunk = ByteArray(64 * 1024)
                var remaining = FIVE_MB
                while (remaining > 0) {
                    val count = minOf(chunk.size, remaining)
                    output.write(chunk, 0, count)
                    remaining -= count
                }
            }
        }
        log("Sending 5 MB test file to ${peer.name}")
        client.sendPayload(peer.endpointId, Payload.fromFile(file))
            .addOnFailureListener { error -> log("Send failed: ${error.message}") }
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }

    private fun log(message: String) {
        _state.value = _state.value.copy(
            logs = (listOf(message) + _state.value.logs).take(250),
        )
    }

    private companion object {
        const val FIVE_MB = 5 * 1024 * 1024
    }
}
