package app.vplusstudio.zonmoment

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.data.Forecast
import app.vplusstudio.zonmoment.domain.Appliance
import app.vplusstudio.zonmoment.domain.HourPoint
import app.vplusstudio.zonmoment.domain.Planner
import app.vplusstudio.zonmoment.domain.RoofDirection
import app.vplusstudio.zonmoment.domain.UserSettings
import app.vplusstudio.zonmoment.ui.HomeScreen
import app.vplusstudio.zonmoment.ui.HomeState
import app.vplusstudio.zonmoment.ui.SetupScreen
import app.vplusstudio.zonmoment.ui.ZonmomentTheme
import app.vplusstudio.zonmoment.ui.hhmm
import app.vplusstudio.zonmoment.ui.label
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.exp
import kotlin.math.pow

/**
 * Renders the Google Play images (icon, feature graphic, phone screenshots) from the real Compose screens
 * with a sample household, so they always match the app. Output: store/images/.
 * Excluded from normal test runs; see testOptions in app/build.gradle.kts.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "nl-w360dp-h640dp-xxhdpi")
class StoreScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val settings = UserSettings(
        postcode = "3512 JK", latitude = 52.09, longitude = 5.12,
        panelCount = 10, wattPeakPerPanel = 400, roof = RoofDirection.SOUTH, tiltDegrees = 35,
        dynamicContract = true, appliances = Appliance.entries.toSet(),
    )

    /** A sunny day and a cloudier day, with a typical Dutch day-ahead price curve (dip at midday, evening peak). */
    private fun sampleState(): HomeState.Ready {
        val today = LocalDate.now(AMSTERDAM)
        val midnight = today.atStartOfDay(AMSTERDAM)
        val hours = (0 until 48).map { i ->
            val t = midnight.plusHours(i.toLong())
            val h = t.hour
            val peak = if (i < 24) 3.3 else 2.1
            val sun = if (h in 7..18) peak * exp(-((h - 12.5) / 2.6).pow(2)) else 0.0
            val price = 0.24 + 0.09 * exp(-((h - 8.0) / 1.5).pow(2)) + 0.13 * exp(-((h - 19.0) / 1.8).pow(2)) -
                0.07 * exp(-((h - 13.5) / 2.2).pow(2)) - 0.03 * exp(-((h - 3.5) / 2.0).pow(2))
            HourPoint(t, (sun * 100).toInt() / 100.0, (price * 1000).toInt() / 1000.0)
        }
        val now = ZonedDateTime.of(today.atTime(7, 0), AMSTERDAM)
        val apps = Appliance.entries
        return HomeState.Ready(
            forecast = Forecast(hours, pricesAvailable = true),
            today = apps.map { Planner.bestWindow(hours, it, today, now, true) },
            tomorrow = apps.map { Planner.bestWindow(hours, it, today.plusDays(1), now, true) },
        )
    }

    private fun save(name: String) {
        val dir = File(System.getProperty("zonmoment.screenshot.dir") ?: "build/store-images").apply { mkdirs() }
        compose.waitForIdle()
        // Draw the view ourselves: captureToImage needs PixelCopy, which Robolectric does not provide.
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Composable
    private fun Frame(caption: String, sub: String, phone: @Composable () -> Unit) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFC94D), Color(0xFFF5A000))))
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(caption, fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(sub, fontSize = 16.sp, lineHeight = 21.sp, color = Ink.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                ZonmomentTheme(dark = false, dynamicColor = false) {
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) { phone() }
                }
            }
        }
    }

    /** Shows the home screen scrolled down by [scroll]. */
    @Composable
    private fun Home(scroll: Dp) {
        Box(Modifier.fillMaxSize().clipToBounds()) {
            HomeScreen(
                settings, sampleState(), onRefresh = {}, onSettings = {},
                modifier = Modifier
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .requiredHeight(2400.dp)
                    .offset(y = -scroll),
            )
        }
    }

    @Test
    fun phone1_best_moment() {
        compose.setContent {
            Frame("Elke dag het beste moment", "Was, droog en laad op je eigen zonnestroom") { Home(0.dp) }
        }
        save("phone-1")
    }

    @Test
    fun phone2_chart() {
        compose.setContent {
            Frame("Zon én stroomprijs per uur", "Met een dynamisch contract kies je ook de goedkoopste uren") { Home(SCROLL_TO_CHART) }
        }
        save("phone-2")
    }

    @Test
    fun phone3_setup() {
        compose.setContent {
            Frame("In een minuut ingesteld", "Postcode, panelen en dak. Geen account nodig.") {
                SetupScreen(initial = settings, onSaved = {}, onCancel = null)
            }
        }
        save("phone-3")
    }

    @Test
    fun phone4_notification() {
        val state = sampleState()
        compose.setContent {
            Frame("Elke ochtend een seintje", "Je gegevens blijven op je telefoon. Geen reclame, geen tracking.") {
                Box(
                    Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF29426B), Color(0xFF6F86B5)))),
                ) {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(36.dp))
                        Text("07:30", fontSize = 64.sp, fontWeight = FontWeight.Light, color = Color.White)
                        Text(stringResource(R.string.today), fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(32.dp))
                        NotificationCard(state)
                    }
                }
            }
        }
        save("phone-4")
    }

    @Composable
    private fun NotificationCard(state: HomeState.Ready) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color(0xF2FFFFFF), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).background(SunYellowDeep, CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_notification), null, Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Zonmoment · nu", fontSize = 12.sp, color = Color(0xFF555555))
                }
                Text(stringResource(R.string.notification_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                state.today.filterNotNull().forEach { r ->
                    Text(
                        stringResource(R.string.notification_line, stringResource(r.appliance.label), r.start.hhmm(), r.end.hhmm(), (r.solarShare * 100).toInt()),
                        fontSize = 14.sp, color = Color(0xFF333333),
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "nl-w512dp-h512dp-mdpi")
    fun icon() {
        compose.setContent { AppIcon(Modifier.fillMaxSize()) }
        save("icon-512")
    }

    @Composable
    private fun AppIcon(modifier: Modifier) {
        Box(modifier.background(SunYellow)) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
        }
    }

    @Test
    @Config(qualifiers = "nl-w1024dp-h500dp-mdpi")
    fun feature_graphic() {
        val state = sampleState()
        compose.setContent {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFFFFD36B), Color(0xFFF5A000))))
                    .padding(horizontal = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(Modifier.size(84.dp).background(SunYellow, RoundedCornerShape(20.dp)))
                        Spacer(Modifier.width(20.dp))
                        Text("Zonmoment", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Gebruik je eigen zonnestroom.\nElke ochtend het beste moment.", fontSize = 32.sp, lineHeight = 42.sp, color = Ink)
                }
                Spacer(Modifier.width(32.dp))
                Box(Modifier.width(300.dp)) {
                    ZonmomentTheme(dark = false, dynamicColor = false) { NotificationCard(state) }
                }
            }
        }
        save("feature-graphic")
    }

    private companion object {
        val Ink = Color(0xFF1D1B16)
        val SunYellow = Color(0xFFF5B400)
        val SunYellowDeep = Color(0xFF8A5A00)
        val SCROLL_TO_CHART = 1215.dp
    }
}
