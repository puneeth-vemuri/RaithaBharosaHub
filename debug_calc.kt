fun main() {
    // Test inputs from testYellowBand_CautionZone
    val moisture = 20f
    val temperature = 22f
    val rainProbability = 0.5f
    
    // Calculate normalized moisture
    val normalizedMoisture = when {
        moisture < 10f -> 0f
        moisture > 45f -> 0f
        moisture in 20f..35f -> 0.8f + (moisture - 20f) / (35f - 20f) * 0.2f
        moisture < 20f -> (moisture - 10f) / (20f - 10f) * 0.8f
        else -> 1f - (moisture - 35f) / (45f - 35f) * 0.2f
    }.coerceIn(0f, 1f)
    
    // Calculate normalized temperature
    val normalizedTemperature = when {
        temperature < 15f -> 0f
        temperature > 35f -> 0f
        temperature in 20f..30f -> 0.8f + (temperature - 20f) / (30f - 20f) * 0.2f
        temperature < 20f -> (temperature - 15f) / (20f - 15f) * 0.8f
        else -> 1f - (temperature - 30f) / (35f - 30f) * 0.2f
    }.coerceIn(0f, 1f)
    
    val inversePrecipitation = 1f - rainProbability
    
    val rawScore = (normalizedMoisture * 0.4f) +
                   (normalizedTemperature * 0.3f) +
                   (inversePrecipitation * 0.3f)
    
    val score = (rawScore * 100f).coerceIn(0f, 100f)
    
    println("=== DEBUG CALCULATION ===")
    println("Inputs: moisture=$moisture, temperature=$temperature, rainProbability=$rainProbability")
    println("normalizedMoisture = $normalizedMoisture")
    println("normalizedTemperature = $normalizedTemperature")
    println("inversePrecipitation = $inversePrecipitation")
    println("rawScore = $rawScore")
    println("score = $score")
    println("State: ${if (score > 70f) "GREEN" else if (score >= 40f) "YELLOW" else "RED"}")
    
    // Also calculate for testGreenBand_IdealConditions to compare
    println("\n=== For testGreenBand_IdealConditions ===")
    val moisture2 = 28f
    val temperature2 = 26f
    val rainProbability2 = 0.1f
    
    val normalizedMoisture2 = when {
        moisture2 < 10f -> 0f
        moisture2 > 45f -> 0f
        moisture2 in 20f..35f -> 0.8f + (moisture2 - 20f) / (35f - 20f) * 0.2f
        moisture2 < 20f -> (moisture2 - 10f) / (20f - 10f) * 0.8f
        else -> 1f - (moisture2 - 35f) / (45f - 35f) * 0.2f
    }.coerceIn(0f, 1f)
    
    val normalizedTemperature2 = when {
        temperature2 < 15f -> 0f
        temperature2 > 35f -> 0f
        temperature2 in 20f..30f -> 0.8f + (temperature2 - 20f) / (30f - 20f) * 0.2f
        temperature2 < 20f -> (temperature2 - 15f) / (20f - 15f) * 0.8f
        else -> 1f - (temperature2 - 30f) / (35f - 30f) * 0.2f
    }.coerceIn(0f, 1f)
    
    val inversePrecipitation2 = 1f - rainProbability2
    val rawScore2 = (normalizedMoisture2 * 0.4f) + (normalizedTemperature2 * 0.3f) + (inversePrecipitation2 * 0.3f)
    val score2 = (rawScore2 * 100f).coerceIn(0f, 100f)
    
    println("Inputs: moisture=$moisture2, temperature=$temperature2, rainProbability=$rainProbability2")
    println("normalizedMoisture = $normalizedMoisture2")
    println("normalizedTemperature = $normalizedTemperature2")
    println("inversePrecipitation = $inversePrecipitation2")
    println("rawScore = $rawScore2")
    println("score = $score2")
    println("State: ${if (score2 > 70f) "GREEN" else if (score2 >= 40f) "YELLOW" else "RED"}")
}