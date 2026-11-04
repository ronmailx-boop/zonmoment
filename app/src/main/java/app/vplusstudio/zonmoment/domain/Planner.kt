package app.vplusstudio.zonmoment.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Finds the best window to run an appliance.
 *
 * With a fixed contract the goal is the highest share of own solar power.
 * With a dynamic contract the goal is the lowest cost of the energy that still comes from the grid,
 * because the sun covers the rest at (almost) no cost.
 */
object Planner {
    /** Only suggest windows people can actually use. */
    val EARLIEST: LocalTime = LocalTime.of(7, 0)
    val LATEST_END: LocalTime = LocalTime.of(22, 0)

    fun bestWindow(
        hours: List<HourPoint>,
        appliance: Appliance,
        day: LocalDate,
        now: ZonedDateTime,
        dynamicContract: Boolean,
    ): Recommendation? {
        val sorted = hours.sortedBy { it.start }
        val earliestStart = now.truncatedTo(ChronoUnit.HOURS).let { if (it.isBefore(now)) it.plusHours(1) else it }
        val candidates = sorted.indices.mapNotNull { i ->
            val window = sorted.subList(i, minOf(i + appliance.durationHours, sorted.size))
            if (window.size < appliance.durationHours) return@mapNotNull null
            if (!isConsecutive(window)) return@mapNotNull null
            val start = window.first().start
            val end = window.last().start.plusHours(1)
            if (start.toLocalDate() != day) return@mapNotNull null
            if (start.isBefore(earliestStart)) return@mapNotNull null
            if (start.toLocalTime().isBefore(EARLIEST)) return@mapNotNull null
            if (end.toLocalDate() != day || end.toLocalTime().isAfter(LATEST_END)) return@mapNotNull null
            score(window, appliance, start, end)
        }
        if (candidates.isEmpty()) return null

        val pricesKnown = candidates.all { it.gridCostEur != null }
        return if (dynamicContract && pricesKnown) {
            candidates.minWith(compareBy<Recommendation> { it.gridCostEur!! }.thenByDescending { it.solarShare })
        } else {
            // Highest solar share; among equal shares the descending start order makes the earliest window win.
            candidates.maxWith(compareBy<Recommendation> { it.solarShare }.thenByDescending { it.start })
        }
    }

    private fun isConsecutive(window: List<HourPoint>): Boolean =
        window.zipWithNext().all { (a, b) -> ChronoUnit.MINUTES.between(a.start, b.start) == 60L }

    private fun score(window: List<HourPoint>, appliance: Appliance, start: ZonedDateTime, end: ZonedDateTime): Recommendation {
        val needKwh = appliance.powerKw * appliance.durationHours
        var solarKwh = 0.0
        var gridCost: Double? = 0.0
        for (h in window) {
            val fromSun = minOf(h.solarKw, appliance.powerKw)
            solarKwh += fromSun
            val price = h.priceEurPerKwh
            gridCost = if (gridCost == null || price == null) null else gridCost + (appliance.powerKw - fromSun) * price
        }
        return Recommendation(
            appliance = appliance,
            start = start,
            end = end,
            solarShare = (solarKwh / needKwh).coerceIn(0.0, 1.0),
            gridCostEur = gridCost,
        )
    }
}
