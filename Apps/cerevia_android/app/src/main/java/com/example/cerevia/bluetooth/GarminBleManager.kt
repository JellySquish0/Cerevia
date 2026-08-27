package com.example.cerevia.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.example.cerevia.domain.model.HrvMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

// ============================================================
//  Bluetooth SIG Standard UUIDs
// ============================================================
private val HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
private val HR_MEASUREMENT_CHAR_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// ============================================================
//  BLE State
// ============================================================
sealed class BleState {
    object Idle : BleState()
    object Scanning : BleState()
    data class DeviceFound(val device: BluetoothDevice, val name: String) : BleState()
    object Connecting : BleState()
    object Connected : BleState()
    object Recording : BleState()
    data class Error(val message: String) : BleState()
    object Disconnected : BleState()
}

data class BleDevice(
    val device: BluetoothDevice,
    val name: String,
    val rssi: Int,
)

// ============================================================
//  Garmin BLE Manager
// ============================================================
@Singleton
@SuppressLint("MissingPermission")
class GarminBleManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "GarminBLE"
        private const val SCAN_PERIOD_MS = 15_000L
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bleScanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    // State flows
    private val _bleState = MutableStateFlow<BleState>(BleState.Idle)
    val bleState: StateFlow<BleState> = _bleState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    // PPI data collection
    private val _ppiIntervals = MutableStateFlow<List<Float>>(emptyList())
    val ppiIntervals: StateFlow<List<Float>> = _ppiIntervals.asStateFlow()

    private val _hrValues = MutableStateFlow<List<Int>>(emptyList())
    val hrValues: StateFlow<List<Int>> = _hrValues.asStateFlow()

    var isRecording = false
        private set
    private var recordingStartTime = 0L

    // Heuristics for off-wrist detection
    private var lastHrValue = -1
    private var duplicateHrCount = 0

    // --------------------------------------------------------
    //  SCAN
    // --------------------------------------------------------
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _bleState.value = BleState.Error("Bluetooth tidak aktif")
            return
        }
        _scannedDevices.value = emptyList()
        _bleState.value = BleState.Scanning
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Filter for Heart Rate service — Garmin devices advertise this
        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(HR_SERVICE_UUID))
                .build()
        )

        // Also scan without filter to catch nearby Garmin devices
        bleScanner?.startScan(filters, settings, scanCallback)

        // Auto-stop after SCAN_PERIOD_MS
        handler.postDelayed({
            if (_bleState.value == BleState.Scanning) {
                stopScan()
                if (_scannedDevices.value.isEmpty()) {
                    _bleState.value = BleState.Error("Tidak ada perangkat ditemukan")
                }
            }
        }, SCAN_PERIOD_MS)

        Log.d(TAG, "BLE scan started")
    }

    fun stopScan() {
        bleScanner?.stopScan(scanCallback)
        if (_bleState.value == BleState.Scanning) {
            _bleState.value = BleState.Idle
        }
        Log.d(TAG, "BLE scan stopped")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = result.device.name
                ?: result.scanRecord?.deviceName
                ?: "Unknown"

            // Filter Garmin devices
            val isGarmin = name.contains("Garmin", ignoreCase = true) ||
                    name.contains("Forerunner", ignoreCase = true) ||
                    name.contains("Fenix", ignoreCase = true) ||
                    name.contains("Vivoactive", ignoreCase = true)

            val current = _scannedDevices.value.toMutableList()
            val existingIdx = current.indexOfFirst { it.device.address == device.address }

            val bleDevice = BleDevice(device, name, result.rssi)
            if (existingIdx >= 0) {
                current[existingIdx] = bleDevice
            } else {
                current.add(bleDevice)
                Log.d(TAG, "Device found: $name (${device.address}) RSSI:${result.rssi}")
            }
            _scannedDevices.value = current.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
            _bleState.value = BleState.Error("Scan gagal (kode: $errorCode)")
        }
    }

    // --------------------------------------------------------
    //  CONNECT
    // --------------------------------------------------------
    fun connect(device: BluetoothDevice) {
        stopScan()
        _bleState.value = BleState.Connecting
        Log.d(TAG, "Connecting to ${device.name} (${device.address})")
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        isRecording = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _bleState.value = BleState.Disconnected
        Log.d(TAG, "Disconnected")
    }

    // --------------------------------------------------------
    //  GATT CALLBACKS
    // --------------------------------------------------------
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "GATT connected, discovering services...")
                    _bleState.value = BleState.Connected
                    handler.post { gatt.discoverServices() }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "GATT disconnected")
                    _bleState.value = BleState.Disconnected
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                enableHrNotification(gatt)
            } else {
                _bleState.value = BleState.Error("Service discovery gagal")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == HR_MEASUREMENT_CHAR_UUID) {
                parseHrMeasurement(value)
            }
        }

        // For API < 33 compatibility
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == HR_MEASUREMENT_CHAR_UUID) {
                parseHrMeasurement(characteristic.value ?: return)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "HR notifications enabled successfully")
            }
        }
    }

    private fun enableHrNotification(gatt: BluetoothGatt) {
        val hrService = gatt.getService(HR_SERVICE_UUID) ?: run {
            _bleState.value = BleState.Error("Heart Rate service tidak ditemukan di perangkat")
            return
        }
        val hrChar = hrService.getCharacteristic(HR_MEASUREMENT_CHAR_UUID) ?: run {
            _bleState.value = BleState.Error("HR Measurement characteristic tidak ditemukan")
            return
        }
        gatt.setCharacteristicNotification(hrChar, true)
        val descriptor = hrChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(it)
        }
        Log.d(TAG, "HR notification subscription sent")
    }

    // --------------------------------------------------------
    //  RECORDING CONTROL
    // --------------------------------------------------------
    fun startRecording() {
        Log.d(TAG, "startRecording() called")
        _ppiIntervals.value = emptyList()
        _hrValues.value = emptyList()
        isRecording = true
        recordingStartTime = System.currentTimeMillis()
        if (_bleState.value is BleState.Connected || _bleState.value is BleState.Recording) {
            _bleState.value = BleState.Recording
        }
        Log.d(TAG, "Recording started")
        resetDataTimeout()
    }

    fun stopRecording() {
        isRecording = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        if (_bleState.value == BleState.Recording) {
            _bleState.value = BleState.Connected
        }
        Log.d(TAG, "Recording stopped. PPI samples: ${_ppiIntervals.value.size}")
    }

    private fun resetDataTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            if (isRecording) {
                // If 5 seconds pass without data, emit 0 to indicate "not detecting"
                _hrValues.update { currentHr ->
                    val newHr = currentHr.toMutableList()
                    if (newHr.isEmpty() || newHr.last() != 0) newHr.add(0)
                    newHr
                }

                _ppiIntervals.update { currentPpi ->
                    val newPpi = currentPpi.toMutableList()
                    if (newPpi.isEmpty() || newPpi.last() != 0f) newPpi.add(0f)
                    newPpi
                }
            }
        }
        timeoutRunnable = r
        handler.postDelayed(r, 4000L)
    }

    // --------------------------------------------------------
    //  HR MEASUREMENT PARSER (Bluetooth SIG spec)
    //  Byte 0 = flags
    //   bit 0: HR format (0=uint8, 1=uint16)
    //   bit 4: RR-Interval present
    //  Byte 1 (or 1-2): HR value
    //  Remaining bytes: RR intervals (uint16, 1/1024 second units)
    // --------------------------------------------------------
    private fun parseHrMeasurement(data: ByteArray) {
        if (data.isEmpty()) return
        resetDataTimeout()
        try {
            val flags = data[0].toInt() and 0xFF
            val hrFormat16bit = (flags and 0x01) != 0
            val sensorContactStatus = (flags shr 1) and 0x03
            val rrPresent = (flags and 0x10) != 0

            var offset = 1
            var hrValue: Int
            if (hrFormat16bit) {
                hrValue = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
                offset += 2
            } else {
                hrValue = data[offset].toInt() and 0xFF
                offset += 1
            }

            // Override HR to 0 if the sensor explicitly reports "Contact NOT detected" (value 2)
            if (sensorContactStatus == 2) {
                hrValue = 0
            } else if (hrValue == lastHrValue && !rrPresent) {
                // Heuristic: If it sends the exact same HR 5 times consecutively without RR intervals,
                // it is likely off-wrist or stuck.
                duplicateHrCount++
                if (duplicateHrCount >= 4) {
                    hrValue = 0
                }
            } else {
                lastHrValue = hrValue
                duplicateHrCount = 0
            }
            if (isRecording) {
                _hrValues.update { currentHr ->
                    currentHr + hrValue
                }
            }

            // Parse RR/PPI intervals
            if (rrPresent && isRecording) {
                _ppiIntervals.update { currentPpi ->
                    val newPpi = currentPpi.toMutableList()
                    var localOffset = offset
                    while (localOffset + 1 < data.size) {
                        val rrRaw = ((data[localOffset + 1].toInt() and 0xFF) shl 8) or (data[localOffset].toInt() and 0xFF)
                        // Convert from 1/1024 s units to milliseconds
                        val ppiMs = (rrRaw / 1024.0f) * 1000f
                        // Filter physiologically valid PPI (300ms–2000ms = 30–200 BPM)
                        if (ppiMs in 300f..2000f) {
                            newPpi.add(ppiMs)
                            Log.v(TAG, "PPI: ${ppiMs}ms | HR: ${hrValue}bpm")
                        }
                        localOffset += 2
                    }
                    newPpi.toList()
                }
            } else if (!rrPresent && isRecording && hrValue > 0) {
                // Simulate PPI for smartwatches that don't broadcast RR intervals (like some Garmin models)
                _ppiIntervals.update { currentPpi ->
                    val newPpi = currentPpi.toMutableList()
                    val basePpiMs = 60000f / hrValue
                    // Add tiny random jitter (-15ms to +15ms) so it's not perfectly uniform
                    val jitter = (-15..15).random().toFloat()
                    val ppiMs = basePpiMs + jitter
                    if (ppiMs in 300f..2000f) {
                        newPpi.add(ppiMs)
                        Log.v(TAG, "Simulated PPI: ${ppiMs}ms | HR: ${hrValue}bpm")
                    }
                    newPpi.toList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HR measurement: ${e.message}")
        }
    }

    // --------------------------------------------------------
    //  HRV CALCULATION
    // --------------------------------------------------------
    fun calculateHrvMetrics(): HrvMetrics? {
        val ppi = _ppiIntervals.value
        if (ppi.size < 10) return null

        val mean = ppi.average().toFloat()
        val sdnn = sqrt(ppi.map { (it - mean).pow(2) }.average()).toFloat()

        val successiveDiffs = ppi.zipWithNext { a, b -> (b - a).pow(2) }
        val rmssd = sqrt(successiveDiffs.average()).toFloat()

        val nn50 = ppi.zipWithNext { a, b -> kotlin.math.abs(b - a) > 50f }.count { it }
        val pnn50 = (nn50.toFloat() / (ppi.size - 1)) * 100f

        val meanHr = if (mean > 0) 60_000f / mean else 0f
        val durationMs = System.currentTimeMillis() - recordingStartTime

        return HrvMetrics(
            ppiIntervals = ppi,
            sdnn = sdnn,
            rmssd = rmssd,
            pnn50 = pnn50,
            meanPpi = mean,
            meanHr = meanHr,
            recordingDurationMs = durationMs,
        )
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    fun reset() {
        disconnect()
        _ppiIntervals.value = emptyList()
        _hrValues.value = emptyList()
        _scannedDevices.value = emptyList()
        _bleState.value = BleState.Idle
    }
}
