package app.vplusstudio.zonmoment.domain

/**
 * Turns plane-of-array irradiance (W/m²) into expected AC output (kW).
 *
 * Standard test conditions are 1000 W/m², so output ≈ kWp × irradiance / 1000 × performance ratio.
 * The performance ratio covers inverter, temperature, cable and soiling losses.
 */
object SolarModel {
    const val PERFORMANCE_RATIO = 0.85

    fun outputKw(irradianceWm2: Double, kWp: Double): Double {
        if (irradianceWm2 <= 0.0 || kWp <= 0.0) return 0.0
        val kw = kWp * (irradianceWm2 / 1000.0) * PERFORMANCE_RATIO
        return kw.coerceAtMost(kWp)
    }

    /**
     * Combines one irradiance series per roof face. Panels are split evenly across faces,
     * so an east-west roof with 4 kWp is two 2 kWp halves.
     */
    fun combineFaces(irradiancePerFace: List<List<Double>>, kWp: Double): List<Double> {
        if (irradiancePerFace.isEmpty()) return emptyList()
        val perFaceKwp = kWp / irradiancePerFace.size
        val hours = irradiancePerFace.minOf { it.size }
        return List(hours) { h -> irradiancePerFace.sumOf { outputKw(it[h], perFaceKwp) } }
    }
}
