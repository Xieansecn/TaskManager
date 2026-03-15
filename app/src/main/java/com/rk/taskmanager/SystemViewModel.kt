package com.rk.taskmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rk.daemon_messages
import com.rk.send_daemon_messages
import com.rk.taskmanager.screens.cpu.CpuInfoReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SystemViewModel : ViewModel() {
    private val _temperature = MutableStateFlow("N/A")
    val temperature: StateFlow<String> = _temperature

    private val _uptime = MutableStateFlow("")
    val uptime: StateFlow<String> = _uptime

    private val _cpuInfo = MutableStateFlow<CpuInfoReader.CpuInfo?>(null)
    val cpuInfo: StateFlow<CpuInfoReader.CpuInfo?> = _cpuInfo

    init {
        // 1. 负责收集温度回报
        viewModelScope.launch(Dispatchers.IO) {
            daemon_messages.collect { message ->
                if (message.startsWith("CTEMP:")) {
                    val temp = message.removePrefix("CTEMP:").toIntOrNull()
                    if (temp != null && temp > 0) {
                        _temperature.value = temp.toString()
                    }
                }
            }
        }

        // 2. 负责发送心跳与读取文件
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                send_daemon_messages.emit("CTEMP_PING")
                _uptime.value = CpuInfoReader.getUptimeFormatted()
                _cpuInfo.value = CpuInfoReader.read() // 后台 IO 拉取
                delay(2000)
            }
        }
    }
}
