package com.idrop.x.diagnostics

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.idrop.x.BuildConfig
import com.idrop.x.nearby.NearbyPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CheckStatus { Pass, Warn, Fail, Running, Pending }

data class SelfTestCheck(
    val title: String,
    val status: CheckStatus = CheckStatus.Pending,
    val detail: String = "",
)

data class SelfTestState(
    val running: Boolean = false,
    val finished: Boolean = false,
    val checks: List<SelfTestCheck> = emptyList(),
    val peerCount: Int = 0,
    val report: String = "",
)

class SelfTestRunner(private val context: Context) {
    suspend fun run(onUpdate: (SelfTestState) -> Unit): SelfTestState {
        val checks = mutableListOf(
            SelfTestCheck("Device & Android"),
            SelfTestCheck("Bluetooth LE"),
            SelfTestCheck("Wi-Fi radio"),
            SelfTestCheck("Location services"),
            SelfTestCheck("Google Play services"),
            SelfTestCheck("Nearby permissions"),
            SelfTestCheck("App storage"),
            SelfTestCheck("Downloads folder"),
        )

        fun publish() {
            onUpdate(SelfTestState(running = true, checks = checks.toList()))
        }

        suspend fun setCheck(index: Int, status: CheckStatus, detail: String) {
            checks[index] = checks[index].copy(status = status, detail = detail)
            publish()
            delay(170)
        }

        publish()

        val android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        setCheck(
            0,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) CheckStatus.Pass else CheckStatus.Fail,
            "${Build.MANUFACTURER} ${Build.MODEL} · $android",
        )

        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter
        val hasBle = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        when {
            !hasBle || adapter == null -> setCheck(
                1,
                CheckStatus.Fail,
                "This phone does not support Bluetooth LE.",
            )

            runCatching { adapter.isEnabled }.getOrDefault(false) -> setCheck(
                1,
                CheckStatus.Pass,
                "Bluetooth is on and BLE is supported.",
            )

            else -> setCheck(
                1,
                CheckStatus.Warn,
                "Bluetooth is off. Turn it on so nearby phones can find you.",
            )
        }

        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val hasWifi = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        val wifiOn = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
        when {
            !hasWifi -> setCheck(
                2,
                CheckStatus.Warn,
                "No Wi-Fi hardware reported. Transfers will rely on Bluetooth only.",
            )

            wifiOn -> setCheck(2, CheckStatus.Pass, "Wi-Fi is on. Fast local transfers are available.")
            else -> setCheck(
                2,
                CheckStatus.Warn,
                "Wi-Fi is off. Turn it on for the fastest direct transfer.",
            )
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            val locationManager = context.getSystemService(LocationManager::class.java)
            val locationOn = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    locationManager?.isLocationEnabled == true
                } else {
                    @Suppress("DEPRECATION")
                    locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                }
            }.getOrDefault(false)
            if (locationOn) {
                setCheck(3, CheckStatus.Pass, "Location services are on (required below Android 12).")
            } else {
                setCheck(
                    3,
                    CheckStatus.Warn,
                    "Turn on Location for nearby scanning on Android 11 and older.",
                )
            }
        } else {
            setCheck(3, CheckStatus.Pass, "Not required on Android 12 and newer.")
        }

        val playServices = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context.applicationContext)
        if (playServices == ConnectionResult.SUCCESS) {
            setCheck(4, CheckStatus.Pass, "Nearby engine is installed and available.")
        } else {
            setCheck(
                4,
                CheckStatus.Fail,
                "Google Play services missing or outdated (code $playServices). Nearby cannot start.",
            )
        }

        if (NearbyPermissions.hasAll(context)) {
            setCheck(5, CheckStatus.Pass, "Bluetooth, Wi-Fi and notification access granted.")
        } else {
            setCheck(
                5,
                CheckStatus.Fail,
                "Nearby permission missing. Open iDrop and tap “Allow nearby access”.",
            )
        }

        val storageOk = withContext(Dispatchers.IO) { testAppStorage() }
        if (storageOk) {
            setCheck(6, CheckStatus.Pass, "1 MB write + read verified in app storage.")
        } else {
            setCheck(6, CheckStatus.Fail, "App storage write test failed.")
        }

        val downloads = withContext(Dispatchers.IO) { testDownloadsFolder() }
        if (downloads.first) {
            setCheck(7, CheckStatus.Pass, downloads.second)
        } else {
            setCheck(7, CheckStatus.Fail, downloads.second)
        }

        val state = SelfTestState(
            running = false,
            finished = false,
            checks = checks.toList(),
        )
        onUpdate(state)
        return state
    }

    private fun testAppStorage(): Boolean {
        return runCatching {
            val payload = ByteArray(1024 * 1024) { index -> (index % 251).toByte() }
            val file = File(context.cacheDir, "idrop-selftest.bin")
            file.writeBytes(payload)
            val readBack = file.readBytes()
            val ok = readBack.contentEquals(payload)
            file.delete()
            ok
        }.getOrDefault(false)
    }

    private fun testDownloadsFolder(): Pair<Boolean, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return runCatching {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "iDrop-selftest.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/iDrop",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Pair(false, "Could not create Downloads/iDrop entry.")
                resolver.openOutputStream(uri)?.use { it.write("iDrop self-test".toByteArray()) }
                resolver.delete(uri, null, null)
                Pair(true, "Downloads/iDrop is writable. Received files are saved there.")
            }.getOrElse { error ->
                Pair(false, "Downloads/iDrop is not writable: ${error.message}")
            }
        }

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Pair(false, "Storage permission is missing on this Android version.")

        return runCatching {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "iDrop",
            )
            if (!dir.exists() && !dir.mkdirs()) {
                return Pair(false, "Could not create Downloads/iDrop.")
            }
            val file = File(dir, ".idrop-selftest")
            file.writeText("iDrop self-test")
            file.delete()
            Pair(true, "Downloads/iDrop is writable. Received files are saved there.")
        }.getOrElse { error ->
            Pair(false, "Downloads/iDrop is not writable: ${error.message}")
        }
    }
}

fun buildSelfTestReport(
    context: Context,
    checks: List<SelfTestCheck>,
    peerCount: Int,
    logs: List<String>,
): String {
    val passed = checks.count { it.status == CheckStatus.Pass }
    val failed = checks.count { it.status == CheckStatus.Fail }
    val warned = checks.count { it.status == CheckStatus.Warn }
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
    return buildString {
        appendLine("iDrop — self-test report")
        appendLine("Generated: $timestamp")
        appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Summary: $passed passed, $warned warnings, $failed failed")
        appendLine("Nearby peers seen: $peerCount")
        appendLine()
        appendLine("Checks:")
        checks.forEach { check ->
            appendLine("[${check.status.name.uppercase(Locale.US)}] ${check.title} — ${check.detail}")
        }
        appendLine()
        appendLine("Recent events:")
        if (logs.isEmpty()) {
            appendLine("  (no events)")
        } else {
            logs.take(30).forEach { line -> appendLine("  $line") }
        }
    }
}
