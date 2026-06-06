package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EstadoWidget(
    val moduloAtivo: String, // "MOTORISTA" ou "PASSAGEIRO"
    val statusBusca: String  // Ex: "RESTRITO RÁPIDO | Buscando..." ou "PROFUNDO | Coletando..."
)

/**
 * Singleton responsible for holding real-time UI state between the Bot AccessibilityService
 * and the floating overlay UI panel.
 */
object GerenciadorEstado {
    private val _estado = MutableStateFlow(EstadoWidget("MOTORISTA", "Inativo"))
    val estado: StateFlow<EstadoWidget> = _estado.asStateFlow()

    fun atualizarEstado(moduloAtivo: String, statusBusca: String) {
        _estado.value = EstadoWidget(moduloAtivo, statusBusca)
    }

    fun setModulo(modulo: String) {
        _estado.value = _estado.value.copy(moduloAtivo = modulo)
    }

    fun setStatus(status: String) {
        _estado.value = _estado.value.copy(statusBusca = status)
    }
}
