package com.idrop.x.nearby

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import com.idrop.x.util.formatBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class NearbyPeer(
    val endpointId: String,
    val name: String,
)

enum class TransferDirection { Incoming, Outgoing }

enum class TransferStatus { InProgress, Complete, Failed }

data class TransferRecord(
    val id: String,
    val peerName: String,
    val fileName: String,
    val direction: TransferDirection,
    val totalBytes: Long,
    val bytesTransferred: Long = 0L,
    val status: TransferStatus = TransferStatus.InProgress,
    val savedPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val progress: Float
        get() = if (totalBytes <= 0L) {
            0f
        } else {
            (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        }
}

data class NearbyState(
    val isAdvertising: Boolean = false,
    val isDiscovering: Boolean = false,
    val peers: List<NearbyPeer> = emptyList(),
    val connected: List<NearbyPeer> = emptyList(),
    val transfers: List<TransferRecord> = emptyList(),
    val receivedText: String? = null,
    val lastSavedPath: String? = null,
    val lastError: String? = null,
    val logs: List<String> = emptyList(),
)

private data class FileMeta(
    val name: String,
    val mime: String,
    val size: Long,
)

class NearbyController(private val context: Context) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val _state = MutableStateFlow(NearbyState())
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val outgoingPayloads = ConcurrentHashMap<Long, String>()
    private val incomingMeta = ConcurrentHashMap<String, MutableList<FileMeta>>()
    private val sequence = AtomicLong(0L)

    private val serviceId = "com.idrop.x.nearby.v1"
    private var myName = "iDrop"

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> handleBytesPayload(endpointId, payload)
                Payload.Type.FILE -> handleFilePayload(endpointId, payload)
                Payload.Type.STREAM -> log("Ignoring unsupported stream payload from ${peerName(endpointId)}")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            val recordId = outgoingPayloads[update.payloadId] ?: return
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    outgoingPayloads.remove(update.payloadId)
                    updateRecord(recordId) {
                        it.copy(
                            bytesTransferred = if (update.totalBytes > 0L) update.totalBytes else it.totalBytes,
                            totalBytes = if (update.totalBytes > 0L) update.totalBytes else it.totalBytes,
                            status = TransferStatus.Complete,
                        )
                    }
                    log("Sent ${recordName(recordId)} to ${peerName(endpointId)}")
                }

                PayloadTransferUpdate.Status.FAILURE -> {
                    outgoingPayloads.remove(update.payloadId)
                    updateRecord(recordId) { it.copy(status = TransferStatus.Failed) }
                    log("Transfer failed: ${recordName(recordId)}")
                }

                else -> {
                    updateRecord(recordId) {
                        it.copy(
                            bytesTransferred = update.bytesTransferred,
                            totalBytes = if (update.totalBytes > 0L) update.totalBytes else it.totalBytes,
                        )
                    }
                }
            }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val peer = NearbyPeer(endpointId = endpointId, name = info.endpointName)
            _state.update { current ->
                current.copy(peers = (current.peers + peer).distinctBy { it.endpointId })
            }
            log("Connection request from ${info.endpointName}")
            client.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { error ->
                    setError("Could not accept ${info.endpointName}: ${error.message}")
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val peer = _state.value.peers.firstOrNull { it.endpointId == endpointId }
                    ?: NearbyPeer(endpointId = endpointId, name = endpointId)
                _state.update { current ->
                    current.copy(
                        connected = (current.connected + peer).distinctBy { it.endpointId },
                        lastError = null,
                    )
                }
                log("Connected to ${peer.name}")
            } else {
                log("Connection failed: ${result.status.statusMessage ?: "unknown"}")
                setError("Connection failed: ${result.status.statusMessage ?: "unknown"}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            log("Disconnected ${peerName(endpointId)}")
            _state.update { current ->
                current.copy(
                    connected = current.connected.filterNot { it.endpointId == endpointId },
                )
            }
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val peer = NearbyPeer(endpointId = endpointId, name = info.endpointName)
            log("Found ${info.endpointName}")
            _state.update { current ->
                current.copy(peers = (current.peers + peer).distinctBy { it.endpointId })
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _state.update { current ->
                current.copy(peers = current.peers.filterNot { it.endpointId == endpointId })
            }
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
                    _state.update { it.copy(isAdvertising = true, lastError = null) }
                    log("Advertising as $name")
                }
                .addOnFailureListener { error ->
                    setError("Advertising failed: ${error.message}")
                }
        }

        if (!_state.value.isDiscovering) {
            val options = DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            client.startDiscovery(serviceId, discoveryCallback, options)
                .addOnSuccessListener {
                    _state.update { it.copy(isDiscovering = true) }
                    log("Discovering nearby iDrop devices")
                }
                .addOnFailureListener { error ->
                    setError("Discovery failed: ${error.message}")
                }
        }
    }

    fun rename(name: String) {
        myName = name
        if (_state.value.isAdvertising) {
            client.stopAdvertising()
            _state.update { it.copy(isAdvertising = false) }
            start(name)
        }
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        _state.update {
            it.copy(
                isAdvertising = false,
                isDiscovering = false,
                peers = emptyList(),
                connected = emptyList(),
            )
        }
        log("Stopped")
    }

    fun connect(peer: NearbyPeer) {
        log("Connecting to ${peer.name}")
        client.requestConnection(myName, peer.endpointId, connectionCallback)
            .addOnFailureListener { error -> setError("Connection request failed: ${error.message}") }
    }

    fun sendFiles(peer: NearbyPeer, uris: List<Uri>) {
        scope.launch {
            uris.forEach { uri ->
                runCatching { sendUri(peer, uri) }
                    .onFailure { setError("Could not send file: ${it.message}") }
            }
        }
    }

    fun sendText(peer: NearbyPeer, text: String) {
        if (text.isBlank()) return
        val message = text.trim()
        val bytes = (TEXT_PREFIX + message).toByteArray(Charsets.UTF_8)
        val payload = Payload.fromBytes(bytes)
        val record = TransferRecord(
            id = newId(),
            peerName = peer.name,
            fileName = "Text message",
            direction = TransferDirection.Outgoing,
            totalBytes = bytes.size.toLong(),
        )
        outgoingPayloads[payload.id] = record.id
        addRecord(record)
        client.sendPayload(peer.endpointId, payload)
            .addOnFailureListener { error ->
                outgoingPayloads.remove(payload.id)
                updateRecord(record.id) { it.copy(status = TransferStatus.Failed) }
                setError("Text send failed: ${error.message}")
            }
    }

    fun sendTestFile(peer: NearbyPeer) {
        scope.launch {
            runCatching {
                val file = File(context.cacheDir, "iDrop-test-5mb.bin")
                if (!file.exists() || file.length() < FIVE_MB) {
                    file.outputStream().use { output ->
                        val chunk = ByteArray(64 * 1024)
                        var remaining = FIVE_MB.toLong()
                        while (remaining > 0L) {
                            val count = minOf(chunk.size.toLong(), remaining).toInt()
                            output.write(chunk, 0, count)
                            remaining -= count.toLong()
                        }
                    }
                }
                sendLocalFile(peer, file, "iDrop-test-5MB.bin", "application/octet-stream")
            }.onFailure { setError("Test send failed: ${it.message}") }
        }
    }

    private fun sendUri(peer: NearbyPeer, uri: Uri) {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(uri) ?: "iDrop-file"
        val safeName = sanitizeName(displayName)
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val outDir = File(context.cacheDir, "outgoing").apply { mkdirs() }
        val temp = File(outDir, "${UUID.randomUUID()}-$safeName")
        resolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open selected file")
        sendLocalFile(peer, temp, safeName, mime)
    }

    private fun sendLocalFile(peer: NearbyPeer, file: File, displayName: String, mime: String) {
        val meta = JSONObject()
            .put("name", displayName)
            .put("mime", mime)
            .put("size", file.length())
            .toString()
        client.sendPayload(peer.endpointId, Payload.fromBytes((META_PREFIX + meta).toByteArray()))

        val payload = Payload.fromFile(file)
        val record = TransferRecord(
            id = newId(),
            peerName = peer.name,
            fileName = displayName,
            direction = TransferDirection.Outgoing,
            totalBytes = file.length(),
        )
        outgoingPayloads[payload.id] = record.id
        addRecord(record)
        log("Sending $displayName (${formatBytes(file.length())}) to ${peer.name}")
        client.sendPayload(peer.endpointId, payload)
            .addOnFailureListener { error ->
                outgoingPayloads.remove(payload.id)
                updateRecord(record.id) { it.copy(status = TransferStatus.Failed) }
                setError("Send failed: ${error.message}")
            }
    }

    private fun handleBytesPayload(endpointId: String, payload: Payload) {
        val text = payload.asBytes()?.toString(Charsets.UTF_8) ?: return
        if (text.startsWith(META_PREFIX)) {
            val json = runCatching { JSONObject(text.removePrefix(META_PREFIX)) }.getOrNull() ?: return
            val meta = FileMeta(
                name = json.optString("name", "iDrop-file"),
                mime = json.optString("mime", "application/octet-stream"),
                size = json.optLong("size", 0L),
            )
            val queue = incomingMeta.getOrPut(endpointId) { mutableListOf() }
            synchronized(queue) { queue.add(meta) }
            return
        }

        val message = if (text.startsWith(TEXT_PREFIX)) {
            text.removePrefix(TEXT_PREFIX)
        } else {
            text
        }
        val size = message.toByteArray(Charsets.UTF_8).size.toLong()
        val record = TransferRecord(
            id = newId(),
            peerName = peerName(endpointId),
            fileName = "Text message",
            direction = TransferDirection.Incoming,
            totalBytes = size,
            bytesTransferred = size,
            status = TransferStatus.Complete,
        )
        _state.update { current ->
            current.copy(
                transfers = (listOf(record) + current.transfers).take(100),
                receivedText = message,
            )
        }
        log("Received text from ${peerName(endpointId)}")
    }

    private fun handleFilePayload(endpointId: String, payload: Payload) {
        val incoming = payload.asFile()?.asJavaFile() ?: return
        val queue = incomingMeta[endpointId]
        val meta = if (queue == null) {
            null
        } else {
            synchronized(queue) { queue.removeFirstOrNull() }
        }
        val name = sanitizeName(meta?.name ?: incoming.name)
        val mime = meta?.mime ?: "application/octet-stream"
        scope.launch {
            runCatching { saveToDownloads(incoming, name, mime) }
                .onSuccess { savedPath ->
                    val size = incoming.length()
                    val record = TransferRecord(
                        id = newId(),
                        peerName = peerName(endpointId),
                        fileName = name,
                        direction = TransferDirection.Incoming,
                        totalBytes = size,
                        bytesTransferred = size,
                        status = TransferStatus.Complete,
                        savedPath = savedPath,
                    )
                    _state.update { current ->
                        current.copy(
                            transfers = (listOf(record) + current.transfers).take(100),
                            lastSavedPath = savedPath,
                        )
                    }
                    log("Saved $name (${formatBytes(size)}) to $savedPath")
                }
                .onFailure { error ->
                    setError("Could not save ${name}: ${error.message}")
                }
        }
    }

    private fun saveToDownloads(source: File, name: String, mime: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/iDrop",
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create download entry")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Cannot open download destination")
                val done = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
            return "Downloads/iDrop/$name"
        }

        @Suppress("DEPRECATION")
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "iDrop",
        )
        if (!dir.exists() && !dir.mkdirs()) error("Cannot create Downloads/iDrop folder")
        val destination = uniqueFile(dir, name)
        source.inputStream().use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
        return destination.absolutePath
    }

    private fun uniqueFile(directory: File, name: String): File {
        var candidate = File(directory, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$base ($index)$extension")
            index++
        }
        return candidate
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            }
        }.getOrNull()
    }

    private fun sanitizeName(raw: String): String {
        val cleaned = raw
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\\p{Cntrl}]"), "")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(120)
        return when {
            cleaned.isBlank() -> "iDrop-${System.currentTimeMillis()}"
            cleaned == "." || cleaned == ".." -> "iDrop-${System.currentTimeMillis()}"
            else -> cleaned
        }
    }

    private fun peerName(endpointId: String): String {
        val current = _state.value
        return current.connected.firstOrNull { it.endpointId == endpointId }?.name
            ?: current.peers.firstOrNull { it.endpointId == endpointId }?.name
            ?: "Nearby device"
    }

    private fun newId(): String = "${System.currentTimeMillis()}-${sequence.incrementAndGet()}"

    private fun addRecord(record: TransferRecord) {
        _state.update { current ->
            current.copy(transfers = (listOf(record) + current.transfers).take(100))
        }
    }

    private fun updateRecord(id: String, transform: (TransferRecord) -> TransferRecord) {
        _state.update { current ->
            current.copy(
                transfers = current.transfers.map { record ->
                    if (record.id == id) transform(record) else record
                },
            )
        }
    }

    private fun recordName(id: String): String =
        _state.value.transfers.firstOrNull { it.id == id }?.fileName ?: "file"

    fun clearReceivedText() {
        _state.update { it.copy(receivedText = null) }
    }

    fun clearLastSaved() {
        _state.update { it.copy(lastSavedPath = null) }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private fun setError(message: String) {
        _state.update { it.copy(lastError = message) }
        log(message)
    }

    private fun log(message: String) {
        _state.update { current ->
            current.copy(logs = (listOf(message) + current.logs).take(250))
        }
    }

    private companion object {
        const val FIVE_MB = 5 * 1024 * 1024
        const val META_PREFIX = "iDropMeta:"
        const val TEXT_PREFIX = "iDropText:"
    }
}
