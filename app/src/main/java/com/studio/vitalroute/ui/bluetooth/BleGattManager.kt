package com.studio.vitalroute.ui.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.UUID
import kotlin.math.sqrt


private object BleUuid {
    // gatt standard
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // environmental sensing service 0x181a mais free fall characteristic 0x2a79
    val ENV_SENSING_SERVICE: UUID = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")
    val FREE_FALL_CHAR: UUID      = UUID.fromString("00002a79-0000-1000-8000-00805f9b34fb")

    // acceleration 3d usado por alguns wearables
    val ACCEL_CHAR: UUID = UUID.fromString("00002713-0000-1000-8000-00805f9b34fb")

    // protocolo vitalroute custom para dispositivos vitalband nativos
    val VITALROUTE_SERVICE: UUID = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
    val VITALROUTE_FALL_CHAR: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    val VITALROUTE_ACCEL_CHAR: UUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")

    // polar h10 tickr servico de frequencia cardiaca
    val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")

    // qualquer servico desconhecido subscreve todas as caracteristicas notificaveis
}


enum class GattConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING_SERVICES,
    SUBSCRIBING,
    CONNECTED,           // ligado e a receber dados
    ERROR
}

//

@SuppressLint("MissingPermission")
class BleGattManager(
    private val context: Context,
    private val onFallDetected: () -> Unit,
    private val onStatusChanged: (GattConnectionStatus, String) -> Unit
) {
    private var gatt: BluetoothGatt? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // fila de operacoes gatt so uma por vez
    private val opQueue = LinkedList<() -> Unit>()
    private var opInProgress = false

    // algoritmo de queda
    private var fallPhase    = FallPhase.NONE
    private var lastFallTime = 0L
    var sensitivity          = 0.6f

    // simulacao quando o device address comeca por aabbcc
    private var simJob: Job? = null

    // ligar

    fun connect(device: BluetoothDevice) {
        onStatusChanged(GattConnectionStatus.CONNECTING, "A ligar a ${device.name ?: device.address}…")

        if (device.address.startsWith("AA:BB:CC")) {
            // dispositivo simulado nao faz gatt real
            startSimulation(device.name ?: "Dispositivo")
            return
        }

        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    // desligar

    fun disconnect() {
        simJob?.cancel()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        opQueue.clear()
        opInProgress = false
        onStatusChanged(GattConnectionStatus.DISCONNECTED, "Desligado")
    }

    // cleanup

    fun close() {
        disconnect()
        scope.cancel()
    }

    // callback gatt

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BleGatt", "Ligado — a descobrir serviços")
                    onStatusChanged(GattConnectionStatus.DISCOVERING_SERVICES, "A descobrir serviços…")
                    scope.launch(Dispatchers.Main) {
                        delay(600) // pequena espera recomendada pela Google antes do discoverServices
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        onStatusChanged(GattConnectionStatus.ERROR, "Ligação perdida (código $status)")
                    } else {
                        onStatusChanged(GattConnectionStatus.DISCONNECTED, "Desligado")
                    }
                    gatt.close()
                    this@BleGattManager.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onStatusChanged(GattConnectionStatus.ERROR, "Erro ao descobrir serviços")
                return
            }

            val chars = mutableListOf<BluetoothGattCharacteristic>()

            // prioridade 1 servico vitalroute custom
            gatt.getService(BleUuid.VITALROUTE_SERVICE)?.let { svc ->
                svc.getCharacteristic(BleUuid.VITALROUTE_FALL_CHAR)?.let  { chars += it }
                svc.getCharacteristic(BleUuid.VITALROUTE_ACCEL_CHAR)?.let { chars += it }
            }

            // prioridade 2 environmental sensing free fall standard
            gatt.getService(BleUuid.ENV_SENSING_SERVICE)?.let { svc ->
                svc.getCharacteristic(BleUuid.FREE_FALL_CHAR)?.let { chars += it }
                svc.getCharacteristic(BleUuid.ACCEL_CHAR)?.let    { chars += it }
            }

            // prioridade 3 todas as caracteristicas notificaveis de qualquer servico
            if (chars.isEmpty()) {
                gatt.services.forEach { svc ->
                    svc.characteristics.forEach { char ->
                        val props = char.properties
                        val isNotifiable = (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                        val isIndicatable = (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                        if (isNotifiable || isIndicatable) chars += char
                    }
                }
            }

            if (chars.isEmpty()) {
                // ligado mas sem caracteristicas uteis continua funcional ex polar h10 hr
                onStatusChanged(GattConnectionStatus.CONNECTED, "Ligado (sem dados de queda disponíveis)")
                return
            }

            onStatusChanged(GattConnectionStatus.SUBSCRIBING, "A ativar notificações (${chars.size} características)…")

            // encadeia subscricoes em fila gatt so permite uma operacao de cada vez
            chars.forEach { char -> enqueue { enableNotifications(gatt, char) } }
            nextOp()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            processCharacteristicData(characteristic.uuid, characteristic.value ?: return)
        }

        // api 33 ou superior
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            processCharacteristicData(characteristic.uuid, value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            opInProgress = false
            // quando todas as subscricoes estiverem completas anuncia que esta pronto
            if (opQueue.isEmpty()) {
                onStatusChanged(GattConnectionStatus.CONNECTED, "Ligado e a monitorizar quedas")
            }
            nextOp()
        }
    }

    // fila de operações gatt

    private fun enqueue(op: () -> Unit) {
        opQueue.add(op)
    }

    private fun nextOp() {
        if (opInProgress || opQueue.isEmpty()) return
        opInProgress = true
        opQueue.poll()?.invoke()
    }

    private fun enableNotifications(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
        val props = char.properties
        val notifyType = when {
            (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0   -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else -> { opInProgress = false; nextOp(); return }
        }

        gatt.setCharacteristicNotification(char, true)

        val cccd = char.getDescriptor(BleUuid.CCCD)
        if (cccd == null) { opInProgress = false; nextOp(); return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, notifyType)
        } else {
            @Suppress("DEPRECATION")
            cccd.value = notifyType
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(cccd)
        }
    }

    // processar dados recebidos

    private fun processCharacteristicData(uuid: UUID, value: ByteArray) {
        when {
            // queda direta vitalroute custom ou free fall standard byte diferente de 0 e queda
            uuid == BleUuid.VITALROUTE_FALL_CHAR || uuid == BleUuid.FREE_FALL_CHAR -> {
                if (value.isNotEmpty() && value[0].toInt() != 0) {
                    triggerFall()
                }
            }
            // dados de acelerometro 6 bytes 3 int16 x y z em m s2 vezes 100
            (uuid == BleUuid.VITALROUTE_ACCEL_CHAR || uuid == BleUuid.ACCEL_CHAR) && value.size >= 6 -> {
                val x = value.toInt16(0) / 100f
                val y = value.toInt16(2) / 100f
                val z = value.toInt16(4) / 100f
                processAcceleration(x, y, z)
            }
            // dados genericos com 6 bytes tenta interpretar como acelerometro
            value.size == 6 || value.size == 7 -> {
                try {
                    val x = value.toInt16(0) / 100f
                    val y = value.toInt16(2) / 100f
                    val z = value.toInt16(4) / 100f
                    val mag = sqrt(x * x + y * y + z * z)
                    // so processa se parecer acelerometro real magnitude entre 0 e 50 m s2
                    if (mag in 0f..50f) processAcceleration(x, y, z)
                } catch (_: Exception) {}
            }
        }
    }

    // algoritmo de queda (igual ao do recordingservice)

    private fun processAcceleration(x: Float, y: Float, z: Float) {
        val magnitude = sqrt(x * x + y * y + z * z)
        val freeFallThreshold = lerp(4.0f, 2.0f, sensitivity)
        val impactThreshold   = lerp(30f,  18f,  sensitivity)

        val now = System.currentTimeMillis()
        if (now - lastFallTime < 10_000L) return  // debounce 10s

        when (fallPhase) {
            FallPhase.NONE -> {
                if (magnitude < freeFallThreshold) fallPhase = FallPhase.FREE_FALL
            }
            FallPhase.FREE_FALL -> {
                if (magnitude > impactThreshold) {
                    lastFallTime = now
                    fallPhase = FallPhase.NONE
                    triggerFall()
                } else if (magnitude > freeFallThreshold * 2.5f) {
                    fallPhase = FallPhase.NONE
                }
            }
        }
    }

    private fun triggerFall() {
        scope.launch { onFallDetected() }
    }

    // simulação (para dispositivos falsos / emulador)
    //

    private fun startSimulation(deviceName: String) {
        onStatusChanged(GattConnectionStatus.SUBSCRIBING, "A configurar $deviceName…")
        simJob = scope.launch {
            delay(1_200)
            onStatusChanged(GattConnectionStatus.CONNECTED, "Ligado (modo simulado) — a monitorizar quedas")

            var t = 0
            while (true) {
                delay(50L)  // ~20 Hz
                t++

                // simula queda ao fim de 8 segundos t igual a 160 amostras vezes 50ms
                val x: Float
                val y: Float
                val z: Float

                if (t in 160..164) {
                    // fase queda livre magnitude aproximadamente 1 m s2
                    x = 0.2f; y = 0.8f; z = 0.3f
                } else if (t in 165..167) {
                    // fase impacto magnitude aproximadamente 35 m s2
                    x = 20f; y = 25f; z = 15f
                } else {
                    // movimento normal em ciclismo ou caminhada
                    val noise = (Math.random() * 2 - 1).toFloat() * 0.5f
                    x = 1.0f + noise
                    y = (9.8f + noise)  // gravidade dominante em Y
                    z = 0.5f + noise
                }
                processAcceleration(x, y, z)
            }
        }
    }

    // helpers

    private fun ByteArray.toInt16(offset: Int): Int {
        val lo = this[offset].toInt() and 0xFF
        val hi = this[offset + 1].toInt()   // signed
        return (hi shl 8) or lo
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}

private enum class FallPhase { NONE, FREE_FALL }
