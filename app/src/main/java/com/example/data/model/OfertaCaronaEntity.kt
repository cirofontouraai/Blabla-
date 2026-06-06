package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an harvested ride offer from BlaBlaCar, persisted locally inside the Room database.
 * Optimized with indices for fast filtering by date/time ranges.
 */
@Entity(
    tableName = "ofertas_carona",
    indices = [
        Index(value = ["dataViagem"]),
        Index(value = ["horarioViagem"]),
        Index(value = ["dataViagem", "horarioViagem"])
    ]
)
data class OfertaCaronaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trechoOrigem: String,
    val trechoDestino: String,
    val preco: Double,
    val notaMotorista: Double,
    val avaliacoesQtde: Int?,
    val dataViagem: String,      // Format: "YYYY-MM-DD" for fast sorting and searching
    val horarioViagem: String,   // Format: "HH:mm" to allow range comparison
    val modeloCarro: String?
)
