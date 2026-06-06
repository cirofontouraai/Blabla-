package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_routes")
data class SearchRoute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val origin: String,
    val destination: String,
    val averagePrice: Double,
    val requestedDiscountPercentage: Double = 7.0, // Default 7% cheaper
    val lastUpdated: Long = System.currentTimeMillis(),
    val isTracking: Boolean = true
) {
    /**
     * Calculates a competitive suggested price aligned with market intelligence rules.
     * Keeps a slight margin lower (e.g. 7% cheaper) to ensure booking priority, 
     * rounded to a psychological price ending (e.g. ending in .85 or .90).
     */
    fun calculateSuggestedPrice(): Double {
        val multiplier = 1.0 - (requestedDiscountPercentage / 100.0)
        val rawSuggested = averagePrice * multiplier
        // Apply classic psychological pricing rounding (e.g., R$ 41.85 instead of R$ 41.853)
        val rounded = Math.round(rawSuggested * 20.0) / 20.0 // Round to nearest 0.05
        return if (rounded > 0) rounded else averagePrice
    }
}
