package com.idrop.x.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.pow

enum class BleStatus {
    Off,
    Starting,
    Active,
    ScanOnly,
    Unavailable,
    Failed,
}

data class ProximityBeacon(
    val id: String,
    val rssi: Int,
    val distanceMeters: Float,
    val lastSeen: Long,
)

/**
 * Tiny BLE beacon layer used only to estimate proximity (about 0.5–3 m).
 * File transfer itself is handled by Nearby Connections; if BLE advertising or
 * scanning is unavailable, the app falls back to normal discovery popups.
 */
class BleProximity(
    private val context: Context,
    private val onEvent: (String) -> Unit,
) {
    private val _status = MutableStateFlow(BleStatus.Off)
    val status: StateFlow<BleStatus> = _status.asStateFlow()

    private val _beacons = MutableStateFlow<Map<String, ProximityBeacon>>(emptyMap())
    val beacons: StateFlow<Map<String, ProximityBeacon>> = _beacons.asStateFlow()

    private val smoothedRssi = mutableMapOf<String, Double>()
    private var running = false
    private var ownId = ""
    private var advertiseFailed = false
    private var scanFailed = false
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            advertiseFailed = false
            if (!scanFailed) _status.value = BleStatus.Active
            onEvent("proximity beacon advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            advertiseFailed = true
            onEvent("beacon advertise failed ($errorCode)")
            _status.value = if (running) BleStatus.ScanOnly else BleStatus.Starting
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            scanFailed = true
            running = false
            onEvent("proximity scan failed ($errorCode)")
            _status.value = BleStatus.Failed
        }
    }

    @SuppressLint("MissingPermission")
    fun start(id: String) {
        if (running) return
        ownId = id
        advertiseFailed = false
        scanFailed = false

        if (!hasPermissions()) {
            _status.value = BleStatus.Unavailable
            onEvent("proximity needs Bluetooth permission")
            return
        }

        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            _status.value = BleStatus.Unavailable
            onEvent("Bluetooth is off; using discovery fallback")
            return
        }

        val localScanner = adapter.bluetoothLeScanner
        if (localScanner == null) {
            _status.value = BleStatus.Unavailable
            onEvent("BLE scanner unavailable; using discovery fallback")
            return
        }

        _status.value = BleStatus.Starting
        scanner = localScanner
        advertiser = adapter.bluetoothLeAdvertiser

        var scanStarted = false
        var advertiseStarted = false

        advertiser?.let { bleAdvertiser ->
            advertiseStarted = runCatching {
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setConnectable(false)
                    .setTimeout(0)
                    .build()
                val data = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .addManufacturerData(COMPANY_ID, idToBytes(id))
                    .setIncludeDeviceName(false)
                    .build()
                bleAdvertiser.startAdvertising(settings, data, advertiseCallback)
                true
            }.getOrElse {
                onEvent("beacon advertise error: ${it.message}")
                false
            }
        }

        scanStarted = runCatching {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0L)
                .build()
            localScanner.startScan(listOf(filter), settings, scanCallback)
            true
        }.getOrElse {
            onEvent("proximity scan error: ${it.message}")
            false
        }

        running = scanStarted
        if (!advertiseStarted) advertiseFailed = true
        _status.value = when {
            scanFailed -> BleStatus.Failed
            !scanStarted -> BleStatus.Failed
            advertiseFailed -> BleStatus.ScanOnly
            else -> BleStatus.Active
        }
        if (scanStarted) {
            onEvent("proximity scanning started")
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        running = false
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        runCatching { scanner?.stopScan(scanCallback) }
        advertiser = null
        scanner = null
        smoothedRssi.clear()
        _beacons.value = emptyMap()
        _status.value = BleStatus.Off
    }

    private fun handleResult(result: ScanResult) {
        val data = result.scanRecord?.getManufacturerSpecificData(COMPANY_ID) ?: return
        if (data.size < ID_BYTES) return
        val id = runCatching { bytesToId(data) }.getOrNull() ?: return
        if (id == ownId) return

        val previous = smoothedRssi[id]
        val smoothed = if (previous == null) {
            result.rssi.toDouble()
        } else {
            previous * 0.65 + result.rssi * 0.35
        }
        smoothedRssi[id] = smoothed

        val distance = 10.0.pow((TX_POWER_AT_ONE_METER - smoothed) / (10.0 * PATH_LOSS))
            .toFloat()
            .coerceIn(0.1f, 60f)

        val now = System.currentTimeMillis()
        val updated = _beacons.value
            .filterValues { now - it.lastSeen < STALE_MS }
            .toMutableMap()
        updated[id] = ProximityBeacon(
            id = id,
            rssi = result.rssi,
            distanceMeters = distance,
            lastSeen = now,
        )
        _beacons.value = updated
    }

    private fun idToBytes(id: String): ByteArray {
        require(id.length == ID_BYTES * 2) { "Proximity id must be ${ID_BYTES * 2} hex chars" }
        return ByteArray(ID_BYTES) { index ->
            id.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun bytesToId(data: ByteArray): String = buildString(ID_BYTES * 2) {
        for (index in 0 until ID_BYTES) {
            val value = data[index].toInt() and 0xFF
            append(HEX[value ushr 4])
            append(HEX[value and 0x0F])
        }
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6f1c9a3e-6d5b-4f2a-9c1d-8b7f2e4a1c33")
        const val COMPANY_ID = 0xFFFF
        const val ID_BYTES = 3
        const val HEX = "0123456789ABCDEF"
        const val STALE_MS = 8_000L
        const val TX_POWER_AT_ONE_METER = -59.0
        const val PATH_LOSS = 2.4
    }
}
