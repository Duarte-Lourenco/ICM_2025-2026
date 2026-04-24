package com.studio.vitalroute.ui.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Modelos
// ─────────────────────────────────────────────────────────────

enum class DeviceType { SMARTWATCH, FITNESS_BAND, HEART_RATE, GENERIC }

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int        = -70,
    val type: DeviceType = DeviceType.GENERIC,
    val isConnected: Boolean = false
)

data class BluetoothUiState(
    // Scan
    val isScanning: Boolean            = false,
    val discoveredDevices: List<BleDevice> = emptyList(),
    // Dispositivo conectado
    val connectedDevice: BleDevice?    = null,
    // Deteção de quedas
    val fallDetectionEnabled: Boolean  = true,
    val fallSensitivity: Float         = 0.6f,   // 0 = baixa … 1 = alta
    val sosDelaySecs: Int              = 15,
    // Countdown SOS
    val isSosCountdown: Boolean        = false,
    val sosCountdownRemaining: Int     = 0,
    val sosSent: Boolean               = false,
    // Feedback ao utilizador
    val lastEventLabel: String?        = null,
    // Estado do adaptador BT
    val bluetoothAvailable: Boolean    = false
)

// ─────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────

class BluetoothViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanJob: Job? = null
    private var sosJob:  Job? = null
    private var scanCallback: ScanCallback? = null

    // ── Inicialização ─────────────────────────────────────────

    fun init(context: Context) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = manager?.adapter
        _uiState.update { it.copy(bluetoothAvailable = bluetoothAdapter != null) }
    }

    // ── Scan BLE ──────────────────────────────────────────────

    fun startScan(context: Context) {
        if (_uiState.value.isScanning) return
        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList()) }

        scanJob = viewModelScope.launch {
            try {
                val scanner = bluetoothAdapter?.bluetoothLeScanner
                if (scanner == null) {
                    simulateScan(); return@launch
                }

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val cb = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val dev = BleDevice(
                            address = result.device.address,
                            name    = result.device.name ?: "Dispositivo Desconhecido",
                            rssi    = result.rssi,
                            type    = guessType(result.device.name)
                        )
                        _uiState.update { s ->
                            if (s.discoveredDevices.none { it.address == dev.address })
                                s.copy(discoveredDevices = s.discoveredDevices + dev)
                            else s
                        }
                    }
                }
                scanCallback = cb
                scanner.startScan(null, settings, cb)
                delay(10_000L)
                scanner.stopScan(cb)

            } catch (_: Exception) {
                simulateScan()
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun stopScan() {
        try {
            scanCallback?.let { bluetoothAdapter?.bluetoothLeScanner?.stopScan(it) }
        } catch (_: Exception) {}
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    // ── Dispositivos simulados (emulador / sem BT) ────────────

    private suspend fun simulateScan() {
        val fakes = listOf(
            BleDevice("AA:BB:CC:11:22:33", "VitalBand Pro",        rssi = -48, type = DeviceType.FITNESS_BAND),
            BleDevice("AA:BB:CC:11:22:44", "Garmin Forerunner 265",rssi = -57, type = DeviceType.SMARTWATCH),
            BleDevice("AA:BB:CC:11:22:55", "Polar H10",            rssi = -63, type = DeviceType.HEART_RATE),
            BleDevice("AA:BB:CC:11:22:66", "Wahoo TICKR X",        rssi = -71, type = DeviceType.HEART_RATE),
            BleDevice("AA:BB:CC:11:22:77", "Apple Watch Ultra 2",  rssi = -52, type = DeviceType.SMARTWATCH),
        )
        for (dev in fakes) {
            delay(700L)
            _uiState.update { it.copy(discoveredDevices = it.discoveredDevices + dev) }
        }
        delay(800L)
        _uiState.update { it.copy(isScanning = false) }
    }

    // ── Ligar / desligar ──────────────────────────────────────

    fun connect(device: BleDevice) {
        val connected = device.copy(isConnected = true)
        _uiState.update { s ->
            s.copy(
                connectedDevice   = connected,
                discoveredDevices = s.discoveredDevices.map {
                    if (it.address == device.address) connected else it.copy(isConnected = false)
                },
                lastEventLabel = "Ligado a ${device.name}"
            )
        }
    }

    fun disconnect() {
        val name = _uiState.value.connectedDevice?.name ?: "dispositivo"
        _uiState.update { s ->
            s.copy(
                connectedDevice   = null,
                discoveredDevices = s.discoveredDevices.map { it.copy(isConnected = false) },
                lastEventLabel    = "Desligado de $name"
            )
        }
    }

    // ── Definições de deteção de quedas ───────────────────────

    fun toggleFallDetection(enabled: Boolean) {
        _uiState.update { it.copy(fallDetectionEnabled = enabled) }
    }

    fun setFallSensitivity(value: Float) {
        _uiState.update { it.copy(fallSensitivity = value) }
    }

    fun setSosDelay(seconds: Int) {
        _uiState.update { it.copy(sosDelaySecs = seconds) }
    }

    fun dismissSosSent() {
        _uiState.update { it.copy(sosSent = false, lastEventLabel = null) }
    }

    // ── Simular queda (botão de teste) ────────────────────────

    fun simulateFall() {
        if (!_uiState.value.fallDetectionEnabled) return
        triggerSosCountdown()
    }

    // ── Countdown SOS ─────────────────────────────────────────

    private fun triggerSosCountdown() {
        sosJob?.cancel()
        val delay = _uiState.value.sosDelaySecs
        _uiState.update {
            it.copy(
                isSosCountdown        = true,
                sosCountdownRemaining = delay,
                sosSent               = false,
                lastEventLabel        = "Queda detetada!"
            )
        }
        sosJob = viewModelScope.launch {
            for (remaining in (delay - 1) downTo 0) {
                delay(1_000L)
                _uiState.update { it.copy(sosCountdownRemaining = remaining) }
            }
            // Countdown chegou a 0
            _uiState.update {
                it.copy(
                    isSosCountdown = false,
                    sosCountdownRemaining = 0,
                    sosSent        = true,
                    lastEventLabel = "SOS enviado aos contactos de emergência"
                )
            }
        }
    }

    fun cancelSos() {
        sosJob?.cancel()
        _uiState.update {
            it.copy(
                isSosCountdown        = false,
                sosCountdownRemaining = 0,
                lastEventLabel        = "SOS cancelado — estás bem!"
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun guessType(name: String?): DeviceType = when {
        name == null -> DeviceType.GENERIC
        name.contains("watch", ignoreCase = true) ||
        name.contains("garmin", ignoreCase = true) ||
        name.contains("suunto", ignoreCase = true) ||
        name.contains("apple", ignoreCase = true)  -> DeviceType.SMARTWATCH
        name.contains("band", ignoreCase = true) ||
        name.contains("vital", ignoreCase = true) ||
        name.contains("fitbit", ignoreCase = true) -> DeviceType.FITNESS_BAND
        name.contains("polar", ignoreCase = true) ||
        name.contains("wahoo", ignoreCase = true) ||
        name.contains("tickr", ignoreCase = true) ||
        name.contains("hrm", ignoreCase = true)    -> DeviceType.HEART_RATE
        else                                       -> DeviceType.GENERIC
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        sosJob?.cancel()
    }
}
