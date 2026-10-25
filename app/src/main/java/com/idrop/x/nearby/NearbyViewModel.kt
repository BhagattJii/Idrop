package com.idrop.x.nearby

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idrop.x.diagnostics.CheckStatus
import com.idrop.x.diagnostics.SelfTestCheck
import com.idrop.x.diagnostics.SelfTestRunner
import com.idrop.x.diagnostics.SelfTestState
import com.idrop.x.diagnostics.buildSelfTestReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NearbyViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = NearbyController(application)

    val state = controller.state

    private val _deviceName = MutableStateFlow("iDrop ${Build.MODEL}")
    val deviceName = _deviceName.asStateFlow()

    private val _selfTest = MutableStateFlow(SelfTestState())
    val selfTest = _selfTest.asStateFlow()

    private var selfTestJob: Job? = null

    fun start() {
        if (!state.value.isAdvertising && !state.value.isDiscovering) {
            controller.start(_deviceName.value)
        }
    }

    fun stop() = controller.stop()

    fun rename(name: String) {
        val trimmed = name.trim().replace(Regex("\\s+"), " ").take(30).ifBlank { "iDrop" }
        _deviceName.value = trimmed
        controller.rename(trimmed)
    }

    fun connect(peer: NearbyPeer) = controller.connect(peer)

    fun sendFiles(peer: NearbyPeer, uris: List<android.net.Uri>) = controller.sendFiles(peer, uris)

    fun sendText(peer: NearbyPeer, text: String) = controller.sendText(peer, text)

    fun sendTestFile(peer: NearbyPeer) = controller.sendTestFile(peer)

    fun clearReceivedText() = controller.clearReceivedText()

    fun clearLastSaved() = controller.clearLastSaved()

    fun clearLogs() = controller.clearLogs()

    fun clearError() = controller.clearError()

    fun runSelfTest() {
        if (selfTestJob?.isActive == true) return
        selfTestJob = viewModelScope.launch {
            val app = getApplication<Application>()
            _selfTest.value = SelfTestState(running = true)
            if (NearbyPermissions.hasAll(app)) {
                controller.start(_deviceName.value)
            }

            val static = SelfTestRunner(app).run { snapshot ->
                _selfTest.value = snapshot
            }

            val liveCheck = SelfTestCheck(
                title = "Live peer discovery",
                status = CheckStatus.Running,
                detail = "Keep iDrop open on a second phone. Scanning for 12 seconds…",
            )
            var checks = static.checks + liveCheck
            _selfTest.value = SelfTestState(
                running = true,
                checks = checks,
            )

            delay(12_000)

            val current = controller.state.value
            val peerCount = (current.peers + current.connected)
                .distinctBy { it.endpointId }
                .size
            val live = liveCheck.copy(
                status = if (peerCount > 0) CheckStatus.Pass else CheckStatus.Warn,
                detail = if (peerCount > 0) {
                    "$peerCount nearby iDrop device${if (peerCount == 1) "" else "s"} found."
                } else {
                    "No second phone found. Install iDrop on another phone, keep both screens on, then run again."
                },
            )
            checks = static.checks + live
            val report = buildSelfTestReport(
                context = app,
                checks = checks,
                peerCount = peerCount,
                logs = current.logs,
            )
            _selfTest.value = SelfTestState(
                running = false,
                finished = true,
                checks = checks,
                peerCount = peerCount,
                report = report,
            )
        }
    }

    fun resetSelfTest() {
        selfTestJob?.cancel()
        selfTestJob = null
        _selfTest.value = SelfTestState()
    }

    override fun onCleared() {
        selfTestJob?.cancel()
        controller.shutdown()
        super.onCleared()
    }
}
