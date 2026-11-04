package app.vplusstudio.zonmoment

import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.domain.Appliance
import app.vplusstudio.zonmoment.domain.HourPoint
import app.vplusstudio.zonmoment.domain.Planner
import app.vplusstudio.zonmoment.domain.SolarModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class PlannerTest {
    private val day = LocalDate.of(2026, 6, 1)
    private val midnight: ZonedDateTime = day.atStartOfDay(AMSTERDAM)

    /** A sunny day: solar kW per hour 0..23, peaking at 13:00. */
    private val sunny = listOf(
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 0.4, 0.9, 1.5, 2.1, 2.6,
        2.9, 3.0, 2.8, 2.3, 1.7, 1.0, 0.5, 0.1, 0.0, 0.0, 0.0, 0.0,
    )

    private fun hours(solar: List<Double>, prices: List<Double>? = null) =
        solar.indices.map { h -> HourPoint(midnight.plusHours(h.toLong()), solar[h], prices?.get(h)) }

    @Test
    fun fixed_contract_picks_the_sunniest_window_and_prefers_earliest_on_ties() {
        val rec = Planner.bestWindow(hours(sunny), Appliance.WASHING_MACHINE, day, midnight, dynamicContract = false)!!
        // From 09:00 every 2-hour window covers 1 kW fully, so the earliest full window wins.
        assertEquals(9, rec.start.hour)
        assertEquals(11, rec.end.hour)
        assertEquals(1.0, rec.solarShare, 1e-9)
    }

    @Test
    fun big_load_goes_to_the_peak() {
        val rec = Planner.bestWindow(hours(sunny), Appliance.EV_CHARGING, day, midnight, dynamicContract = false)!!
        assertEquals(12, rec.start.hour) // 12–15 holds 2.9 + 3.0 + 2.8
    }

    @Test
    fun dynamic_contract_minimises_grid_cost() {
        val prices = List(24) { h -> if (h == 20 || h == 21) 0.01 else 0.40 }
        val noSun = List(24) { 0.0 }
        val rec = Planner.bestWindow(hours(noSun, prices), Appliance.DISHWASHER, day, midnight, dynamicContract = true)!!
        assertEquals(20, rec.start.hour)
        assertEquals(0.02, rec.gridCostEur!!, 1e-9)
    }

    @Test
    fun never_suggests_the_past_or_outside_07_to_22() {
        val now = midnight.plusHours(14).plusMinutes(5)
        val rec = Planner.bestWindow(hours(sunny), Appliance.WASHING_MACHINE, day, now, dynamicContract = false)!!
        assertEquals(15, rec.start.hour)

        val late = midnight.plusHours(20).plusMinutes(30)
        assertNull(Planner.bestWindow(hours(sunny), Appliance.WASHING_MACHINE, day, late, dynamicContract = false))
    }

    @Test
    fun solar_model_splits_east_west_roofs() {
        // 4 kWp split over two faces; only the east face gets sun this hour.
        val kw = SolarModel.combineFaces(listOf(listOf(1000.0), listOf(0.0)), kWp = 4.0)
        assertEquals(2.0 * SolarModel.PERFORMANCE_RATIO, kw.single(), 1e-9)
    }
}
