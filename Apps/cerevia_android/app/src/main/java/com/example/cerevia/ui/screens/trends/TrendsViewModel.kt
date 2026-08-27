package com.example.cerevia.ui.screens.trends

import androidx.lifecycle.ViewModel
import com.example.cerevia.bluetooth.GarminBleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TrendsViewModel @Inject constructor(
    val bleManager: GarminBleManager
) : ViewModel() {
    val ppiIntervals = bleManager.ppiIntervals
    val bleState = bleManager.bleState
}
