package app.vplusstudio.zonmoment

import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.data.ForecastRepository
import app.vplusstudio.zonmoment.domain.Appliance
import app.vplusstudio.zonmoment.domain.Planner
import app.vplusstudio.zonmoment.domain.RoofDirection
import app.vplusstudio.zonmoment.domain.UserSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Calls the real PDOK, Open-Meteo and EnergyZero APIs, so a format change shows up here
 * before users notice. Skipped unless ZONMOMENT_LIVE=1, because normal builds must not depend on the network.
 */
class LiveApiTest {
    @Before
    fun onlyWhenAsked() {
        assumeTrue("set ZONMOMENT_LIVE=1 to run", System.getenv("ZONMOMENT_LIVE") == "1")
    }

    @Test
    fun real_apis_give_a_full_plan() = runBlocking {
        val geo = ForecastRepository.geocode("1012 AB")
        assertNotNull("PDOK did not find 1012 AB", geo)
        geo!!
        assertEquals(52.37, geo.latitude, 0.05)
        assertEquals(4.90, geo.longitude, 0.05)

        for (roof in listOf(RoofDirection.SOUTH, RoofDirection.EAST_WEST)) {
            val settings = UserSettings(
                postcode = "1012AB", latitude = geo.latitude, longitude = geo.longitude,
                panelCount = 10, wattPeakPerPanel = 400, roof = roof, tiltDegrees = 35,
                dynamicContract = true, appliances = Appliance.entries.toSet(),
            )
            val forecast = ForecastRepository.forecast(settings)
            val today = LocalDate.now(AMSTERDAM)
            val now = ZonedDateTime.now(AMSTERDAM)

            assertTrue("expected ~48 hours, got ${forecast.hours.size}", forecast.hours.size >= 46)
            assertTrue("EnergyZero prices missing", forecast.pricesAvailable)
            assertTrue("solar output above kWp", forecast.hours.all { it.solarKw in 0.0..settings.kWp })
            assertTrue("no sun at all in two days", forecast.hours.any { it.solarKw > 0.0 })
            assertTrue("sun at night", forecast.hours.filter { it.start.hour < 5 }.all { it.solarKw == 0.0 })

            println("\n== ${geo.label}, ${settings.kWp} kWp, $roof, dynamic contract")
            for (h in forecast.hours) {
                println("%s  sun %.2f kW  price %s".format(h.start.toLocalDateTime(), h.solarKw, h.priceEurPerKwh?.let { "€%.3f".format(it) } ?: "-"))
            }
            for (day in listOf(today, today.plusDays(1))) {
                for (a in Appliance.entries) {
                    for (dynamic in listOf(false, true)) {
                        val r = Planner.bestWindow(forecast.hours, a, day, now, dynamic)
                        println(
                            "$day ${if (dynamic) "dynamic" else "fixed  "} $a: " +
                                (r?.let { "%s–%s sun %d%% grid %s".format(it.start.toLocalTime(), it.end.toLocalTime(), (it.solarShare * 100).toInt(), it.gridCostEur?.let { c -> "€%.2f".format(c) }) } ?: "no window left"),
                        )
                    }
                }
            }
        }
    }
}
