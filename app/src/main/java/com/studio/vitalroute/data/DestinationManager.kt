package com.studio.vitalroute.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Destination(
    val lat: Double,
    val lng: Double,
    val name: String = ""
)

object DestinationManager {
    private val _destination = MutableStateFlow<Destination?>(null)
    val destination: StateFlow<Destination?> = _destination.asStateFlow()

    fun set(lat: Double, lng: Double, name: String = "") {
        _destination.value = Destination(lat, lng, name)
    }

    fun clear() {
        _destination.value = null
    }
}
