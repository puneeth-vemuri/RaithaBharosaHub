package com.raithabharosahub.util

import com.raithabharosahub.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NPK recommendation engine with crop-specific ideal ranges.
 * Zero Android dependencies - pure Kotlin logic.
 */
@Singleton
class NpkRecommendationEngine @Inject constructor() {

    /**
     * Crop-specific ideal NPK ranges in kg/ha.
     */
    private data class CropRange(
        val nitrogenMin: Float,
        val nitrogenMax: Float,
        val phosphorusMin: Float,
        val phosphorusMax: Float,
        val potassiumMin: Float,
        val potassiumMax: Float
    )

    private val cropRanges = mapOf(
        "Paddy" to CropRange(
            nitrogenMin = 80f,
            nitrogenMax = 120f,
            phosphorusMin = 40f,
            phosphorusMax = 60f,
            potassiumMin = 40f,
            potassiumMax = 60f
        ),
        "Ragi" to CropRange(
            nitrogenMin = 40f,
            nitrogenMax = 80f,
            phosphorusMin = 20f,
            phosphorusMax = 40f,
            potassiumMin = 20f,
            potassiumMax = 40f
        ),
        "Sugarcane" to CropRange(
            nitrogenMin = 150f,
            nitrogenMax = 250f,
            phosphorusMin = 60f,
            phosphorusMax = 90f,
            potassiumMin = 100f,
            potassiumMax = 150f
        ),
        "Custom" to CropRange(
            nitrogenMin = 60f,
            nitrogenMax = 100f,
            phosphorusMin = 30f,
            phosphorusMax = 50f,
            potassiumMin = 30f,
            potassiumMax = 50f
        )
    )

    /**
     * Nutrient status enum for recommendation.
     */
    enum class NutrientStatus {
        DEFICIENT, OPTIMAL, EXCESS
    }

    /**
     * NPK recommendation data class.
     */
    data class NpkRecommendation(
        val nutrient: String, // "Nitrogen", "Phosphorus", "Potassium"
        val status: NutrientStatus,
        val messageId: Int, // R.string.* resource ID
        val deviation: Float // % deviation from optimal midpoint
    )

    /**
     * Get recommendations for given NPK values and crop.
     */
    fun getRecommendations(
        nitrogen: Float,
        phosphorus: Float,
        potassium: Float,
        crop: String
    ): List<NpkRecommendation> {
        val range = cropRanges[crop] ?: cropRanges["Custom"]!!

        return listOf(
            analyzeNutrient("Nitrogen", nitrogen, range.nitrogenMin, range.nitrogenMax),
            analyzeNutrient("Phosphorus", phosphorus, range.phosphorusMin, range.phosphorusMax),
            analyzeNutrient("Potassium", potassium, range.potassiumMin, range.potassiumMax)
        )
    }

    /**
     * Analyze a single nutrient and return recommendation.
     */
    private fun analyzeNutrient(
        nutrientName: String,
        actualValue: Float,
        minOptimal: Float,
        maxOptimal: Float
    ): NpkRecommendation {
        val midpoint = (minOptimal + maxOptimal) / 2f
        
        // Calculate deviation percentage from midpoint
        val deviation = if (midpoint > 0) {
            ((actualValue - midpoint) / midpoint) * 100f
        } else {
            0f
        }

        val (status, messageId) = when {
            actualValue < minOptimal -> {
                Pair(NutrientStatus.DEFICIENT, getMessageId(nutrientName, NutrientStatus.DEFICIENT))
            }
            actualValue > maxOptimal -> {
                Pair(NutrientStatus.EXCESS, getMessageId(nutrientName, NutrientStatus.EXCESS))
            }
            else -> {
                Pair(NutrientStatus.OPTIMAL, getMessageId(nutrientName, NutrientStatus.OPTIMAL))
            }
        }

        return NpkRecommendation(
            nutrient = nutrientName,
            status = status,
            messageId = messageId,
            deviation = deviation
        )
    }

    /**
     * Get string resource ID for nutrient status message.
     * These IDs correspond to R.string.* values defined in strings.xml
     */
    private fun getMessageId(nutrient: String, status: NutrientStatus): Int {
        return when (nutrient) {
            "Nitrogen" -> when (status) {
                NutrientStatus.DEFICIENT -> R.string.npk_nitrogen_deficient
                NutrientStatus.OPTIMAL -> R.string.npk_nitrogen_optimal
                NutrientStatus.EXCESS -> R.string.npk_nitrogen_excess
            }
            "Phosphorus" -> when (status) {
                NutrientStatus.DEFICIENT -> R.string.npk_phosphorus_deficient
                NutrientStatus.OPTIMAL -> R.string.npk_phosphorus_optimal
                NutrientStatus.EXCESS -> R.string.npk_phosphorus_excess
            }
            "Potassium" -> when (status) {
                NutrientStatus.DEFICIENT -> R.string.npk_potassium_deficient
                NutrientStatus.OPTIMAL -> R.string.npk_potassium_optimal
                NutrientStatus.EXCESS -> R.string.npk_potassium_excess
            }
            else -> R.string.npk_nitrogen_optimal // fallback
        }
    }
}