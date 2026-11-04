package app.vplusstudio.zonmoment.data

import app.vplusstudio.zonmoment.domain.HourPoint
import app.vplusstudio.zonmoment.domain.SolarModel
import app.vplusstudio.zonmoment.domain.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** No connection, timeout or server error: something the user can retry. */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class Forecast(val hours: List<HourPoint>, val pricesAvailable: Boolean)

/**
 * Fetches everything the app needs. All sources are public and need no API key:
 * - PDOK Locatieserver (Dutch government) for postcode → coordinates
 * - Open-Meteo for irradiance on the user's roof
 * - EnergyZero for Dutch day-ahead electricity prices
 */
object ForecastRepository {
    private const val TIMEOUT_MS = 15_000

    suspend fun geocode(postcode: String): GeoPoint? {
        val q = URLEncoder.encode(postcode.replace(" ", "").uppercase(), "UTF-8")
        val json = get("https://api.pdok.nl/bzk/locatieserver/search/v3_1/free?q=$q&fq=type:postcode&rows=1")
        return Parsers.parsePdok(json)
    }

    suspend fun forecast(settings: UserSettings, today: LocalDate = LocalDate.now(AMSTERDAM)): Forecast {
        val faces = settings.roof.azimuths.map { azimuth -> irradiance(settings, azimuth) }
        val hoursInAll = faces.map { it.keys }.reduce { a, b -> a intersect b }.sorted()
        val solar = SolarModel.combineFaces(faces.map { face -> hoursInAll.map { face.getValue(it) } }, settings.kWp)

        val prices = if (settings.dynamicContract) {
            runCatching { prices(today) }.getOrNull() // prices are a bonus; the solar plan still works without them
        } else {
            null
        }

        val points = hoursInAll.mapIndexed { i, start -> HourPoint(start, solar[i], prices?.get(start)) }
        return Forecast(points, pricesAvailable = prices != null && points.any { it.priceEurPerKwh != null })
    }

    private suspend fun irradiance(settings: UserSettings, azimuth: Int) = Parsers.parseOpenMeteoIrradiance(
        get(
            "https://api.open-meteo.com/v1/forecast?latitude=${settings.latitude}&longitude=${settings.longitude}" +
                "&hourly=global_tilted_irradiance&tilt=${settings.tiltDegrees}&azimuth=$azimuth" +
                "&timezone=Europe%2FAmsterdam&forecast_days=2",
        ),
    )

    private suspend fun prices(today: LocalDate): Map<java.time.ZonedDateTime, Double> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        val from = today.atStartOfDay(AMSTERDAM).withZoneSameInstant(ZoneOffset.UTC).format(fmt)
        val till = today.plusDays(2).atStartOfDay(AMSTERDAM).minusNanos(1_000_000)
            .withZoneSameInstant(ZoneOffset.UTC).format(fmt)
        return Parsers.parseEnergyZeroPrices(
            get("https://api.energyzero.nl/v1/energyprices?fromDate=$from&tillDate=$till&interval=4&usageType=1&inclBtw=true"),
        )
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Zonmoment-Android")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw NetworkException("HTTP $code from ${URL(url).host}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw NetworkException("Network error for ${URL(url).host}", e)
        } finally {
            conn.disconnect()
        }
    }
}
