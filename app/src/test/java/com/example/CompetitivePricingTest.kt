package com.example

import com.example.data.model.SearchRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class CompetitivePricingTest {

    @Test
    fun testDefaultCompetitivePricing() {
        // Test standard values specified in the requirements: Average: 45.0, expecting suggested discount R$ 41,85
        val route = SearchRoute(
            origin = "São Paulo",
            destination = "Campinas",
            averagePrice = 45.0,
            requestedDiscountPercentage = 7.0 // Default 7% cheaper
        )
        
        val suggestedPrice = route.calculateSuggestedPrice()
        
        // 45.0 * 0.93 = 41.85
        assertEquals(41.85, suggestedPrice, 0.01)
    }

    @Test
    fun testCustomDiscountCompetitivePricing() {
        // Test with 10% discount on R$ 100.00 average -> should be R$ 90.00
        val route = SearchRoute(
            origin = "Belo Horizonte",
            destination = "Ouro Preto",
            averagePrice = 100.00,
            requestedDiscountPercentage = 10.0
        )
        
        val suggestedPrice = route.calculateSuggestedPrice()
        assertEquals(90.00, suggestedPrice, 0.01)
    }

    @Test
    fun testRoundingPsychologicalPricing() {
        // Test nearest 0.05 rounding mechanism
        // Average R$ 33.00 with 7% discount -> 33.00 * 0.93 = 30.69.
        // Nearest 0.05 rounding should output R$ 30.70
        val route = SearchRoute(
            origin = "Curitiba",
            destination = "Joinville",
            averagePrice = 33.00,
            requestedDiscountPercentage = 7.0
        )
        
        val suggestedPrice = route.calculateSuggestedPrice()
        assertEquals(30.70, suggestedPrice, 0.01)
    }
}
