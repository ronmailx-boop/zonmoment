package app.vplusstudio.zonmoment

import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.data.Parsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class ParsersTest {
    @Test
    fun pdok_reads_centroid_as_lat_lon() {
        val json = """{"response":{"numFound":1,"docs":[{"centroide_ll":"POINT(4.89123 52.37456)","weergavenaam":"1012AB Amsterdam"}]}}"""
        val p = Parsers.parsePdok(json)!!
        assertEquals(52.37456, p.latitude, 1e-9)
        assertEquals(4.89123, p.longitude, 1e-9)
        assertEquals("1012AB Amsterdam", p.label)
    }

    @Test
    fun pdok_unknown_postcode_is_null() {
        assertNull(Parsers.parsePdok("""{"response":{"numFound":0,"docs":[]}}"""))
    }

    @Test
    fun open_meteo_values_belong_to_the_preceding_hour() {
        val json = """{"hourly":{"time":["2026-06-01T12:00","2026-06-01T13:00"],"global_tilted_irradiance":[700.0,null]}}"""
        val map = Parsers.parseOpenMeteoIrradiance(json)
        val elevenAm = ZonedDateTime.of(2026, 6, 1, 11, 0, 0, 0, AMSTERDAM)
        assertEquals(700.0, map.getValue(elevenAm), 1e-9)
        assertEquals(0.0, map.getValue(elevenAm.plusHours(1)), 1e-9) // null counts as no sun
    }

    @Test
    fun energyzero_quarter_hours_are_averaged_per_local_hour() {
        // 10:00–11:00 UTC on 1 June is 12:00–13:00 in Amsterdam (CEST, UTC+2).
        val json = """{"Prices":[
            {"price":0.10,"readingDate":"2026-06-01T10:00:00Z"},
            {"price":0.20,"readingDate":"2026-06-01T10:15:00Z"},
            {"price":0.30,"readingDate":"2026-06-01T10:30:00Z"},
            {"price":0.40,"readingDate":"2026-06-01T10:45:00Z"},
            {"price":0.50,"readingDate":"2026-06-01T11:00:00Z"}]}"""
        val map = Parsers.parseEnergyZeroPrices(json)
        val noon = ZonedDateTime.of(2026, 6, 1, 12, 0, 0, 0, AMSTERDAM)
        assertEquals(0.25, map.getValue(noon), 1e-9)
        assertEquals(0.50, map.getValue(noon.plusHours(1)), 1e-9)
    }
}
