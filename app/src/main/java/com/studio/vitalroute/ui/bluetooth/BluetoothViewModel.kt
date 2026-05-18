package com.studio.vitalroute.ui.bluetooth

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.SosContact
import com.studio.vitalroute.data.SosManager
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.ui.recording.RecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt


enum class DeviceType { SMARTWATCH, FITNESS_BAND, HEART_RATE, GENERIC }

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int            = -70,
    val type: DeviceType     = DeviceType.GENERIC,
    val isConnected: Boolean = false
)

data class BluetoothUiState(
    val isScanning: Boolean                = false,
    val discoveredDevices: List<BleDevice> = emptyList(),
    val connectedDevice: BleDevice?        = null,
    val gattStatus: GattConnectionStatus   = GattConnectionStatus.DISCONNECTED,
    val gattStatusLabel: String            = "",
    val fallDetectionEnabled: Boolean      = true,
    val fallSensitivity: Float             = 0.6f,
    val sosDelaySecs: Int                  = 15,
    val isSosCountdown: Boolean            = false,
    val sosCountdownRemaining: Int         = 0,
    val sosSent: Boolean                   = false,
    val lastEventLabel: String?            = null,
    val bluetoothAvailable: Boolean        = false,
    val isSimulatedMode: Boolean           = false,
    val phoneSensorActive: Boolean         = false,
    val phoneSensorAvailable: Boolean      = false
)


class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx        = application.applicationContext
    private val repository = FirestoreRepository()

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanJob: Job?  = null
    private var sosJob: Job?   = null
    private var scanCallback: ScanCallback? = null

    // gestor da ligacao gatt
    private var gattManager: BleGattManager? = null

    // cache de contactos para envio de sos offline
    private var cachedSosContacts: List<SosContact> = emptyList()

    // sensor do telemovel
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor?        = null
    private var phoneFallPhase                = PhoneFallPhase.NONE
    private var phoneLastFallTime             = 0L

    private val phoneSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            if (RecordingService.state.value.isRecording) return
            if (!_uiState.value.fallDetectionEnabled) return
            if (_uiState.value.isSosCountdown) return

            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            val magnitude = sqrt(x * x + y * y + z * z)

            val sensitivity = _uiState.value.fallSensitivity
            val freeFallThreshold = lerp(4.0f, 2.0f, sensitivity)
            val impactThreshold   = lerp(30f,  18f,  sensitivity)

            val now = System.currentTimeMillis()
            if (now - phoneLastFallTime < 10_000L) return

            when (phoneFallPhase) {
                PhoneFallPhase.NONE -> {
                    if (magnitude < freeFallThreshold) phoneFallPhase = PhoneFallPhase.FREE_FALL
                }
                PhoneFallPhase.FREE_FALL -> {
                    if (magnitude > impactThreshold) {
                        phoneLastFallTime = now
                        phoneFallPhase    = PhoneFallPhase.NONE
                        viewModelScope.launch { triggerSosCountdown() }
                    } else if (magnitude > freeFallThreshold * 2.5f) {
                        phoneFallPhase = PhoneFallPhase.NONE
                    }
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    // inicializacao

    fun init(context: Context) {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = btManager?.adapter
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        _uiState.update {
            it.copy(
                bluetoothAvailable   = bluetoothAdapter != null,
                phoneSensorAvailable = accelerometer != null
            )
        }
        loadSosContacts()
    }

    fun togglePhoneSensor(enable: Boolean) {
        if (enable) {
            accelerometer?.let { sensor ->
                sensorManager?.registerListener(
                    phoneSensorListener, sensor, SensorManager.SENSOR_DELAY_GAME
                )
            }
            phoneFallPhase    = PhoneFallPhase.NONE
            phoneLastFallTime = 0L
            _uiState.update { it.copy(phoneSensorActive = true,
                lastEventLabel = "Sensor do telemóvel ativo — a monitorizar quedas") }
        } else {
            sensorManager?.unregisterListener(phoneSensorListener)
            phoneFallPhase = PhoneFallPhase.NONE
            _uiState.update { it.copy(phoneSensorActive = false,
                lastEventLabel = "Sensor do telemóvel desativado") }
        }
    }

    private fun loadSosContacts() {
        viewModelScope.launch {
            repository.getContacts()
                .catch { /* offline */ }
                .collect { list ->
                    cachedSosContacts = list.map { c ->
                        SosContact(name = c.name, phone = c.phone, sosEnabled = c.sosEnabled)
                    }
                }
        }
    }

    // scan ble

    fun startScan(context: Context) {
        if (_uiState.value.isScanning) return
        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList()) }

        scanJob = viewModelScope.launch {
            try {
                val scanner = bluetoothAdapter?.bluetoothLeScanner
                if (scanner == null) { simulateScan(); return@launch }

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
        try { scanCallback?.let { bluetoothAdapter?.bluetoothLeScanner?.stopScan(it) } }
        catch (_: Exception) {}
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    private suspend fun simulateScan() {
        _uiState.update { it.copy(isSimulatedMode = true) }
        val fakes = listOf(
            BleDevice("AA:BB:CC:11:22:33", "VitalBand Pro",         rssi = -48, type = DeviceType.FITNESS_BAND),
            BleDevice("AA:BB:CC:11:22:44", "Garmin Forerunner 265", rssi = -57, type = DeviceType.SMARTWATCH),
            BleDevice("AA:BB:CC:11:22:55", "Polar H10",             rssi = -63, type = DeviceType.HEART_RATE),
            BleDevice("AA:BB:CC:11:22:66", "Wahoo TICKR X",         rssi = -71, type = DeviceType.HEART_RATE),
            BleDevice("AA:BB:CC:11:22:77", "Apple Watch Ultra 2",   rssi = -52, type = DeviceType.SMARTWATCH),
        )
        for (dev in fakes) {
            delay(700L)
            _uiState.update { it.copy(discoveredDevices = it.discoveredDevices + dev) }
        }
        delay(800L)
        _uiState.update { it.copy(isScanning = false) }
    }

    // ligar ao dispositivo via gatt real ou simulado

    fun connect(device: BleDevice) {
        gattManager?.close()

        val isSimulated = bluetoothAdapter == null || device.address.startsWith("AA:BB:CC")

        if (isSimulated) {
            connectSimulated(device)
            return
        }

        val btDevice = try {
            bluetoothAdapter!!.getRemoteDevice(device.address)
        } catch (_: Exception) {
            connectSimulated(device); return
        }

        gattManager = buildGattManager(device).also { it.sensitivity = _uiState.value.fallSensitivity }
        gattManager!!.connect(btDevice)
    }

    // constroi um blegattmanager com os callbacks padrao
    private fun buildGattManager(device: BleDevice): BleGattManager =
        BleGattManager(
            context = ctx,
            onFallDetected = {
                viewModelScope.launch {
                    if (_uiState.value.fallDetectionEnabled) triggerSosCountdown()
                }
            },
            onStatusChanged = { status, label ->
                viewModelScope.launch {
                    val isConnected    = status == GattConnectionStatus.CONNECTED
                    val isTerminal     = status == GattConnectionStatus.DISCONNECTED
                                     || status == GattConnectionStatus.ERROR
                    _uiState.update { s -> s.copy(
                        gattStatus      = status,
                        gattStatusLabel = label,
                        connectedDevice = when {
                            isConnected -> device.copy(isConnected = true)
                            isTerminal  -> null
                            else        -> s.connectedDevice
                        },
                        discoveredDevices = when {
                            isConnected -> s.discoveredDevices.map {
                                if (it.address == device.address) it.copy(isConnected = true)
                                else it.copy(isConnected = false)
                            }
                            isTerminal -> s.discoveredDevices.map { it.copy(isConnected = false) }
                            else       -> s.discoveredDevices
                        },
                        lastEventLabel = label
                    )}
                }
            }
        )

    // fallback emulador ou simulado usa simulacao interna do blegattmanager
    private fun connectSimulated(device: BleDevice) {
        gattManager = buildGattManager(device).also { it.sensitivity = _uiState.value.fallSensitivity }

        // blegattmanager deteta enderecos aa bb cc e entra em modo simulado
        // sem adaptador simulamos os estados manualmente
        val fakeDevice = try { bluetoothAdapter?.getRemoteDevice("AA:BB:CC:11:22:33") }
                         catch (_: Exception) { null }

        if (fakeDevice != null) {
            gattManager!!.connect(fakeDevice)
        } else {
            // sem adaptador bt gere estados diretamente
            viewModelScope.launch {
                _uiState.update { it.copy(
                    gattStatus      = GattConnectionStatus.CONNECTING,
                    gattStatusLabel = "A ligar a ${device.name}…",
                    lastEventLabel  = "A ligar a ${device.name}…"
                )}
                delay(800)
                _uiState.update { it.copy(
                    gattStatus      = GattConnectionStatus.SUBSCRIBING,
                    gattStatusLabel = "A configurar ${device.name}…"
                )}
                delay(1_000)
                _uiState.update { s -> s.copy(
                    gattStatus      = GattConnectionStatus.CONNECTED,
                    gattStatusLabel = "Ligado (modo simulado) — a monitorizar quedas",
                    connectedDevice = device.copy(isConnected = true),
                    discoveredDevices = s.discoveredDevices.map {
                        if (it.address == device.address) it.copy(isConnected = true)
                        else it.copy(isConnected = false)
                    },
                    lastEventLabel  = "Ligado a ${device.name}"
                )}
                // simula queda ao fim de 8 segundos
                delay(8_000)
                if (_uiState.value.connectedDevice?.address == device.address
                    && _uiState.value.fallDetectionEnabled
                    && !_uiState.value.isSosCountdown) {
                    triggerSosCountdown()
                }
            }
        }
    }

    // desligar

    fun disconnect() {
        val name = _uiState.value.connectedDevice?.name ?: "dispositivo"
        gattManager?.close()
        gattManager = null
        _uiState.update { s -> s.copy(
            connectedDevice   = null,
            gattStatus        = GattConnectionStatus.DISCONNECTED,
            gattStatusLabel   = "",
            discoveredDevices = s.discoveredDevices.map { it.copy(isConnected = false) },
            lastEventLabel    = "Desligado de $name"
        )}
    }

    // definicoes de detecao de quedas

    fun toggleFallDetection(enabled: Boolean) {
        _uiState.update { it.copy(fallDetectionEnabled = enabled) }
    }

    fun setFallSensitivity(value: Float) {
        _uiState.update { it.copy(fallSensitivity = value) }
        gattManager?.sensitivity = value
    }

    fun setSosDelay(seconds: Int) {
        _uiState.update { it.copy(sosDelaySecs = seconds) }
    }

    fun dismissSosSent() {
        _uiState.update { it.copy(sosSent = false, lastEventLabel = null) }
    }

    // simular queda botao de teste

    fun simulateFall() {
        if (!_uiState.value.fallDetectionEnabled) return
        triggerSosCountdown()
    }

    // countdown sos

    private fun triggerSosCountdown() {
        sosJob?.cancel()
        val delay = _uiState.value.sosDelaySecs
        _uiState.update { it.copy(
            isSosCountdown        = true,
            sosCountdownRemaining = delay,
            sosSent               = false,
            lastEventLabel        = "Queda detetada!"
        )}
        sosJob = viewModelScope.launch {
            for (remaining in (delay - 1) downTo 0) {
                delay(1_000L)
                _uiState.update { it.copy(sosCountdownRemaining = remaining) }
            }
            _uiState.update { it.copy(
                isSosCountdown        = false,
                sosCountdownRemaining = 0,
                sosSent               = true,
                lastEventLabel        = "SOS enviado!"
            )}
            launch(Dispatchers.IO) {
                SosManager.sendSos(
                    context       = ctx,
                    location      = null,
                    localContacts = cachedSosContacts
                )
            }
        }
    }

    fun cancelSos() {
        sosJob?.cancel()
        _uiState.update { it.copy(
            isSosCountdown        = false,
            sosCountdownRemaining = 0,
            lastEventLabel        = "SOS cancelado — estás bem!"
        )}
    }

    // cleanup

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        sosJob?.cancel()
        gattManager?.close()
        sensorManager?.unregisterListener(phoneSensorListener)
    }

    // helpers

    private enum class PhoneFallPhase { NONE, FREE_FALL }

    private fun guessType(name: String?): DeviceType = when {
        name == null -> DeviceType.GENERIC
        name.contains("watch",  ignoreCase = true) ||
        name.contains("garmin", ignoreCase = true) ||
        name.contains("suunto", ignoreCase = true) ||
        name.contains("apple",  ignoreCase = true) -> DeviceType.SMARTWATCH
        name.contains("band",   ignoreCase = true) ||
        name.contains("vital",  ignoreCase = true) ||
        name.contains("fitbit", ignoreCase = true) -> DeviceType.FITNESS_BAND
        name.contains("polar",  ignoreCase = true) ||
        name.contains("wahoo",  ignoreCase = true) ||
        name.contains("tickr",  ignoreCase = true) ||
        name.contains("hrm",    ignoreCase = true) -> DeviceType.HEART_RATE
        else                                       -> DeviceType.GENERIC
    }
}
