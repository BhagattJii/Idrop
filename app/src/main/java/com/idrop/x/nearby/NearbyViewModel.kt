package com.idrop.x.nearby

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = NearbyController(application)

    val state = controller.state

    private val _deviceName = MutableStateFlow("iDrop ${Build.MODEL}")
    val deviceName = _deviceName.asStateFlow()

    fun start() {
        if (!state.value.isAdvertising && !state.value.isDiscovering) {
            controller.start(_deviceName.value)
        }
    }

    fun stop() = controller.stop()

    fun connect(peer: NearbyPeer) = controller.connect(peer)

    fun sendTestFile(peer: NearbyPeer) = controller.sendTestFile(peer)

    fun clearLogs() = controller.clearLogs()

    override fun onCleared() {
        controller.stop()
        super.onCleared()
    }
}
