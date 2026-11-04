package app.vplusstudio.zonmoment.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

val AMSTERDAM: ZoneId = ZoneId.of("Europe/Amsterdam")

/** Thrown when a data source answers with something we can't read. */
class DataFormatException(message: String) : Exception(message)

data class GeoPoint(val latitude: Double, val longitude: Double, val label: String)

object Parsers {
    private val POINT = Regex("""POINT\(\s*(-?[0-9.]+)\s+(-?[0-9.]+)\s*\)""")

    /** PDOK Locatieserver: `docs[0].centroide_ll` is "POINT(lon lat)". Returns null when the postcode is unknown. */
    fun parsePdok(json: String): GeoPoint? {
        val docs = JSONObject(json).optJSONObject("response")?.optJSONArray("docs") ?: return null
        if (docs.length() == 0) return null
        val doc = docs.getJSONObject(0)
        val match = POINT.find(doc.optString("centroide_ll")) ?: throw DataFormatException("PDOK: no centroid")
        val (lon, lat) = match.destructured
        return GeoPoint(lat.toDouble(), lon.toDouble(), doc.optString("weergavenaam"))
    }

    /**
     * Open-Meteo hourly `global_tilted_irradiance` (W/m²) in local time.
     * Radiation values are the mean of the *preceding* hour, so the value stamped 12:00 belongs to the
     * hour that starts at 11:00. We return a map keyed by the start of the hour it describes.
     */
    fun parseOpenMeteoIrradiance(json: String): Map<ZonedDateTime, Double> {
        val hourly = JSONObject(json).optJSONObject("hourly") ?: throw DataFormatException("Open-Meteo: no hourly")
        val times = hourly.getJSONArray("time")
        val values = hourly.getJSONArray("global_tilted_irradiance")
        val result = LinkedHashMap<ZonedDateTime, Double>()
        for (i in 0 until minOf(times.length(), values.length())) {
            val stamp = LocalDateTime.parse(times.getString(i)).atZone(AMSTERDAM)
            val value = if (values.isNull(i)) 0.0 else values.getDouble(i)
            result[stamp.minusHours(1)] = value
        }
        return result
    }

    /**
     * EnergyZero `Prices[]` with `price` (EUR/kWh) and `readingDate` (UTC).
     * Prices may come per hour or per quarter-hour; we average them per local hour.
     */
    fun parseEnergyZeroPrices(json: String): Map<ZonedDateTime, Double> {
        val prices = JSONObject(json).optJSONArray("Prices") ?: throw DataFormatException("EnergyZero: no Prices")
        val buckets = LinkedHashMap<ZonedDateTime, MutableList<Double>>()
        for (i in 0 until prices.length()) {
            val p = prices.getJSONObject(i)
            val hour = Instant.parse(p.getString("readingDate")).atZone(AMSTERDAM).truncatedTo(ChronoUnit.HOURS)
            buckets.getOrPut(hour) { mutableListOf() }.add(p.getDouble("price"))
        }
        return buckets.mapValues { (_, v) -> v.average() }
    }
}
