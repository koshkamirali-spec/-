package com.risefit.app.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-process state shared between AlarmService and AlarmRingActivity. -1 = nothing is ringing. */
object AlarmState {
    private val _ringingId = MutableStateFlow(-1)
    val ringingId: StateFlow<Int> = _ringingId

    fun set(id: Int) {
        _ringingId.value = id
    }
}
