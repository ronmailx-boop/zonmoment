package app.vplusstudio.zonmoment.data

import android.content.Context
import androidx.core.content.edit
import app.vplusstudio.zonmoment.domain.Appliance
import app.vplusstudio.zonmoment.domain.RoofDirection
import app.vplusstudio.zonmoment.domain.UserSettings

/** Settings live only on the phone. Nothing is sent anywhere except coordinates to the forecast service. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("zonmoment", Context.MODE_PRIVATE)

    fun load(): UserSettings? {
        if (!prefs.contains(KEY_LAT)) return null
        return runCatching {
            UserSettings(
                postcode = prefs.getString(KEY_POSTCODE, "")!!,
                latitude = prefs.getFloat(KEY_LAT, 0f).toDouble(),
                longitude = prefs.getFloat(KEY_LON, 0f).toDouble(),
                panelCount = prefs.getInt(KEY_PANELS, 10),
                wattPeakPerPanel = prefs.getInt(KEY_WP, 400),
                roof = RoofDirection.valueOf(prefs.getString(KEY_ROOF, RoofDirection.SOUTH.name)!!),
                tiltDegrees = prefs.getInt(KEY_TILT, 35),
                dynamicContract = prefs.getBoolean(KEY_DYNAMIC, false),
                appliances = prefs.getStringSet(KEY_APPLIANCES, emptySet())!!
                    .mapNotNull { name -> Appliance.entries.firstOrNull { it.name == name } }
                    .toSet(),
            )
        }.getOrNull()
    }

    fun save(s: UserSettings) {
        prefs.edit {
            putString(KEY_POSTCODE, s.postcode)
            putFloat(KEY_LAT, s.latitude.toFloat())
            putFloat(KEY_LON, s.longitude.toFloat())
            putInt(KEY_PANELS, s.panelCount)
            putInt(KEY_WP, s.wattPeakPerPanel)
            putString(KEY_ROOF, s.roof.name)
            putInt(KEY_TILT, s.tiltDegrees)
            putBoolean(KEY_DYNAMIC, s.dynamicContract)
            putStringSet(KEY_APPLIANCES, s.appliances.map { it.name }.toSet())
        }
    }

    private companion object {
        const val KEY_POSTCODE = "postcode"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_PANELS = "panels"
        const val KEY_WP = "wp"
        const val KEY_ROOF = "roof"
        const val KEY_TILT = "tilt"
        const val KEY_DYNAMIC = "dynamic"
        const val KEY_APPLIANCES = "appliances"
    }
}
