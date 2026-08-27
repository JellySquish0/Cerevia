package com.example.cerevia.ui.screens.bluetooth

import androidx.lifecycle.ViewModel
import com.example.cerevia.bluetooth.GarminBleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeviceScanViewModel @Inject constructor(
    val bleManager: GarminBleManager
) : ViewModel() {
    
    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}
