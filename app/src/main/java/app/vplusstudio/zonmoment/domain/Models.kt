package app.vplusstudio.zonmoment.domain

import java.time.ZonedDateTime

/** Which way the panels face. Azimuths use the Open-Meteo convention: 0 = south, -90 = east, 90 = west. */
enum class RoofDirection(val azimuths: List<Int>) {
    SOUTH(listOf(0)),
    SOUTH_EAST(listOf(-45)),
    SOUTH_WEST(listOf(45)),
    EAST(listOf(-90)),
    WEST(listOf(90)),
    EAST_WEST(listOf(-90, 90)),
}

/** A household appliance the user wants to schedule. */
enum class Appliance(val powerKw: Double, val durationHours: Int) {
    WASHING_MACHINE(1.0, 2),
    DISHWASHER(1.0, 2),
    DRYER(2.0, 2),
    EV_CHARGING(3.7, 3),
}

data class UserSettings(
    val postcode: String,
    val latitude: Double,
    val longitude: Double,
    val panelCount: Int,
    val wattPeakPerPanel: Int,
    val roof: RoofDirection,
    val tiltDegrees: Int,
    val dynamicContract: Boolean,
    val appliances: Set<Appliance>,
) {
    val kWp: Double get() = panelCount * wattPeakPerPanel / 1000.0
}

/** One hour of forecast: expected solar output and (optional) market price, starting at [start]. */
data class HourPoint(
    val start: ZonedDateTime,
    val solarKw: Double,
    val priceEurPerKwh: Double?,
)

/** The recommended time window for one appliance. */
data class Recommendation(
    val appliance: Appliance,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    /** Share of the appliance's energy expected to come from the user's own panels, 0..1. */
    val solarShare: Double,
    /** Expected cost of the grid energy in the window, when prices are known. */
    val gridCostEur: Double?,
)
