package com.example.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PaymentManager {
    private val _availableEngines = listOf(
        CashPaymentEngine(),
        CinetPayPaymentEngine(),
        PayDunyaPaymentEngine(),
        OrangeMoneyDirectEngine()
    )

    val availableEngines: List<PaymentEngine> get() = _availableEngines

    private val _currentEngine = MutableStateFlow<PaymentEngine>(_availableEngines[0])
    val currentEngine: StateFlow<PaymentEngine> = _currentEngine.asStateFlow()

    fun selectEngine(engineId: String) {
        val found = _availableEngines.find { it.id == engineId }
        if (found != null) {
            _currentEngine.value = found
        }
    }
}
