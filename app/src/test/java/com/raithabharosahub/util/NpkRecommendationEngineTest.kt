package com.raithabharosahub.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for [NpkRecommendationEngine].
 *
 * IMPORTANT: [NpkRecommendationEngine.NpkRecommendation.messageId] is an opaque
 * R.string integer constant (e.g. R.string.npk_nitrogen_deficient). It cannot be
 * resolved to a human-readable string on the JVM without Android runtime context.
 * All assertions in this file therefore check [NutrientStatus] and [nutrient] name,
 * which are pure Kotlin values with no Android dependency.
 *
 * Paddy ideal ranges (kg/ha):
 *   N: 80–120  P: 40–60  K: 40–60
 *
 * Ragi ideal ranges (kg/ha):
 *   N: 40–80   P: 20–40  K: 20–40
 */
class NpkRecommendationEngineTest {

    private lateinit var engine: NpkRecommendationEngine

    @Before
    fun setUp() {
        engine = NpkRecommendationEngine()
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private fun getRecommendation(
        nitrogen: Float, phosphorus: Float, potassium: Float, crop: String
    ) = engine.getRecommendations(nitrogen, phosphorus, potassium, crop)

    // ------------------------------------------------------------------
    // Paddy — Nitrogen DEFICIENT  (N < 80)
    // ------------------------------------------------------------------

    /**
     * PRD: "Paddy N below optimal → recommendation contains 'Urea'"
     *
     * The engine returns NpkRecommendation with status=DEFICIENT for the
     * Nitrogen entry when nitrogen < 80 (Paddy min).
     * The task expects the recommendation to "contain 'Urea'"; in the source
     * that text lives in R.string.npk_nitrogen_deficient ("... apply Urea before sowing").
     * On JVM we assert: status = DEFICIENT (same semantic intent, zero Android dep).
     */
    @Test
    fun `paddy N below optimal 80 results in DEFICIENT status`() {
        val recs = getRecommendation(
            nitrogen = 60f,     // below Paddy min 80
            phosphorus = 50f,   // within range 40-60
            potassium = 50f,    // within range 40-60
            crop = "Paddy"
        )
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertEquals(
            "Paddy N=60 (below min 80) must be DEFICIENT",
            NpkRecommendationEngine.NutrientStatus.DEFICIENT,
            nRec.status
        )
    }

    @Test
    fun `paddy N below optimal has negative deviation`() {
        val recs = getRecommendation(60f, 50f, 50f, "Paddy")
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertTrue(
            "Deviation for deficient N must be negative, got ${nRec.deviation}",
            nRec.deviation < 0f
        )
    }

    // ------------------------------------------------------------------
    // Paddy — Phosphorus EXCESS  (P > 60)
    // ------------------------------------------------------------------

    /**
     * PRD: "Paddy P above optimal → recommendation contains 'reduce Phosphorus'"
     *
     * Engine returns status=EXCESS for Phosphorus when phosphorus > 60 (Paddy max).
     * We assert status=EXCESS (the "reduce" text lives in the R.string).
     */
    @Test
    fun `paddy P above optimal 60 results in EXCESS status`() {
        val recs = getRecommendation(
            nitrogen = 100f,    // within Paddy range 80-120
            phosphorus = 80f,   // above Paddy max 60
            potassium = 50f,
            crop = "Paddy"
        )
        val pRec = recs.first { it.nutrient == "Phosphorus" }
        assertEquals(
            "Paddy P=80 (above max 60) must be EXCESS",
            NpkRecommendationEngine.NutrientStatus.EXCESS,
            pRec.status
        )
    }

    @Test
    fun `paddy P above optimal has positive deviation`() {
        val recs = getRecommendation(100f, 80f, 50f, "Paddy")
        val pRec = recs.first { it.nutrient == "Phosphorus" }
        assertTrue(
            "Deviation for excess P must be positive, got ${pRec.deviation}",
            pRec.deviation > 0f
        )
    }

    // ------------------------------------------------------------------
    // Ragi — Potassium OPTIMAL  (K in 20–40)
    // ------------------------------------------------------------------

    /**
     * PRD: "Ragi K within range → recommendation contains 'optimal'"
     *
     * Engine returns status=OPTIMAL for Potassium when 20 <= K <= 40.
     */
    @Test
    fun `ragi K within optimal range 20-40 results in OPTIMAL status`() {
        val recs = getRecommendation(
            nitrogen = 60f,     // within Ragi range 40-80
            phosphorus = 30f,   // within Ragi range 20-40
            potassium = 30f,    // within Ragi range 20-40
            crop = "Ragi"
        )
        val kRec = recs.first { it.nutrient == "Potassium" }
        assertEquals(
            "Ragi K=30 (in optimal 20-40) must be OPTIMAL",
            NpkRecommendationEngine.NutrientStatus.OPTIMAL,
            kRec.status
        )
    }

    @Test
    fun `ragi K within optimal has near-zero deviation`() {
        val recs = getRecommendation(60f, 30f, 30f, "Ragi")
        val kRec = recs.first { it.nutrient == "Potassium" }
        // K=30, midpoint=(20+40)/2=30, deviation = (30-30)/30*100 = 0
        assertEquals("K at midpoint should have 0% deviation", 0f, kRec.deviation, 0.001f)
    }

    // ------------------------------------------------------------------
    // All three nutrients optimal — returns three OPTIMAL items
    // ------------------------------------------------------------------

    @Test
    fun `paddy all nutrients within range returns three OPTIMAL recommendations`() {
        val recs = getRecommendation(
            nitrogen = 100f,    // in 80-120
            phosphorus = 50f,   // in 40-60
            potassium = 50f,    // in 40-60
            crop = "Paddy"
        )
        assertEquals("Should return exactly 3 recommendations", 3, recs.size)
        recs.forEach { rec ->
            assertEquals(
                "${rec.nutrient} is within Paddy range and should be OPTIMAL",
                NpkRecommendationEngine.NutrientStatus.OPTIMAL, rec.status
            )
        }
    }

    // ------------------------------------------------------------------
    // Boundary tests
    // ------------------------------------------------------------------

    @Test
    fun `paddy N exactly at min boundary 80 is OPTIMAL`() {
        val recs = getRecommendation(80f, 50f, 50f, "Paddy")
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertEquals(
            "N=80 is exactly at Paddy min, must be OPTIMAL (not DEFICIENT)",
            NpkRecommendationEngine.NutrientStatus.OPTIMAL, nRec.status
        )
    }

    @Test
    fun `paddy N exactly at max boundary 120 is OPTIMAL`() {
        val recs = getRecommendation(120f, 50f, 50f, "Paddy")
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertEquals(
            "N=120 is exactly at Paddy max, must be OPTIMAL (not EXCESS)",
            NpkRecommendationEngine.NutrientStatus.OPTIMAL, nRec.status
        )
    }

    @Test
    fun `paddy N just below min 79 is DEFICIENT`() {
        val recs = getRecommendation(79f, 50f, 50f, "Paddy")
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertEquals(
            "N=79 is just below Paddy min 80, must be DEFICIENT",
            NpkRecommendationEngine.NutrientStatus.DEFICIENT, nRec.status
        )
    }

    @Test
    fun `paddy N just above max 121 is EXCESS`() {
        val recs = getRecommendation(121f, 50f, 50f, "Paddy")
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertEquals(
            "N=121 is just above Paddy max 120, must be EXCESS",
            NpkRecommendationEngine.NutrientStatus.EXCESS, nRec.status
        )
    }

    // ------------------------------------------------------------------
    // Unknown crop falls back to Custom range (N 60-100, P 30-50, K 30-50)
    // ------------------------------------------------------------------

    @Test
    fun `unknown crop falls back to Custom range`() {
        val recs = getRecommendation(
            nitrogen = 50f,     // below Custom min 60
            phosphorus = 40f,   // within Custom range 30-50
            potassium = 40f,    // within Custom range 30-50
            crop = "Wheat"      // not in the map
        )
        val nRec = recs.first { it.nutrient == "Nitrogen" }
        assertEquals(
            "N=50 is below Custom min 60; unknown crop should fallback to Custom range → DEFICIENT",
            NpkRecommendationEngine.NutrientStatus.DEFICIENT, nRec.status
        )
    }

    // ------------------------------------------------------------------
    // Result list ordering and nutrient names
    // ------------------------------------------------------------------

    @Test
    fun `getRecommendations always returns Nitrogen Phosphorus Potassium in order`() {
        val recs = getRecommendation(100f, 50f, 50f, "Paddy")
        assertEquals(3, recs.size)
        assertEquals("First rec must be Nitrogen",  "Nitrogen",  recs[0].nutrient)
        assertEquals("Second rec must be Phosphorus","Phosphorus", recs[1].nutrient)
        assertEquals("Third rec must be Potassium", "Potassium", recs[2].nutrient)
    }
}
