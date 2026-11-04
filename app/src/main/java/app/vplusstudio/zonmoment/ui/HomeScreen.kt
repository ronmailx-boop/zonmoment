package app.vplusstudio.zonmoment.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.vplusstudio.zonmoment.R
import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.domain.HourPoint
import app.vplusstudio.zonmoment.domain.Recommendation
import app.vplusstudio.zonmoment.domain.UserSettings
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** Below this share we tell the user not to expect much sun. */
private const val LITTLE_SUN = 0.25

@Composable
fun HomeScreen(
    settings: UserSettings,
    state: HomeState,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(
                R.string.system_summary,
                settings.postcode,
                String.format(Locale.ROOT, "%.1f", settings.kWp),
                stringResource(settings.roof.label),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
            OutlinedButton(onClick = onSettings) { Text(stringResource(R.string.settings)) }
        }

        when (state) {
            HomeState.Loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loading), Modifier.padding(top = 8.dp))
                }
            }
            is HomeState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(state.message), color = MaterialTheme.colorScheme.error)
                Button(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
            }
            is HomeState.Ready -> Ready(settings, state)
        }
    }
}

@Composable
private fun Ready(settings: UserSettings, state: HomeState.Ready) {
    if (settings.dynamicContract && !state.forecast.pricesAvailable) {
        Text(stringResource(R.string.prices_unavailable), style = MaterialTheme.typography.bodySmall)
    }
    DaySection(stringResource(R.string.today), state.today, isToday = true)
    DaySection(stringResource(R.string.tomorrow), state.tomorrow, isToday = false)

    val today = LocalDate.now(AMSTERDAM)
    // Day-ahead prices for tomorrow are published around 13:00–14:00; until then tomorrow is planned on sun only.
    val tomorrowHasPrices = state.forecast.hours.any { it.start.toLocalDate() == today.plusDays(1) && it.priceEurPerKwh != null }
    if (settings.dynamicContract && state.forecast.pricesAvailable && !tomorrowHasPrices) {
        Text(stringResource(R.string.tomorrow_prices_later), style = MaterialTheme.typography.bodySmall)
    }
    Text(stringResource(R.string.chart_title), style = MaterialTheme.typography.titleMedium)
    listOf(today, today.plusDays(1)).forEach { day ->
        val hours = state.forecast.hours.filter { it.start.toLocalDate() == day && it.start.hour in 6..21 }
        if (hours.isNotEmpty()) {
            Text(
                if (day == today) stringResource(R.string.today) else stringResource(R.string.tomorrow),
                style = MaterialTheme.typography.labelLarge,
            )
            SolarChart(hours, maxKw = settings.kWp)
        }
    }
    if (state.forecast.pricesAvailable) {
        Text(stringResource(R.string.chart_legend_prices), style = MaterialTheme.typography.bodySmall)
    }
    Text(stringResource(R.string.forecast_disclaimer), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun DaySection(title: String, recs: List<Recommendation?>, isToday: Boolean) {
    Text("$title · ${stringResource(R.string.best_moment)}", style = MaterialTheme.typography.titleMedium)
    recs.forEach { rec ->
        if (rec == null) {
            if (isToday) Text(stringResource(R.string.no_window), style = MaterialTheme.typography.bodyMedium)
            return@forEach
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${rec.appliance.emoji}  ${stringResource(rec.appliance.label)}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text("${rec.start.hhmm()} – ${rec.end.hhmm()}", style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.solar_share, (rec.solarShare * 100).roundToInt()))
                if (rec.solarShare < LITTLE_SUN) {
                    Text(stringResource(R.string.little_sun), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Bars: expected solar kW per hour. Line: price, scaled to the day's own min/max. */
@Composable
private fun SolarChart(hours: List<HourPoint>, maxKw: Double) {
    val peak = hours.maxOf { it.solarKw }
    val description = stringResource(
        R.string.chart_description,
        String.format(Locale.ROOT, "%.1f", peak),
        hours.maxBy { it.solarKw }.start.hhmm(),
    )
    val prices = hours.mapNotNull { it.priceEurPerKwh }
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .semantics { contentDescription = description },
        ) {
            val slot = size.width / hours.size
            val scaleKw = maxOf(maxKw, 0.1)
            hours.forEachIndexed { i, h ->
                val barH = (h.solarKw / scaleKw).toFloat().coerceIn(0f, 1f) * size.height
                drawRect(
                    color = SunYellow,
                    topLeft = Offset(i * slot + slot * 0.15f, size.height - barH),
                    size = Size(slot * 0.7f, barH),
                )
            }
            if (prices.size == hours.size && prices.size > 1) {
                val lo = prices.min()
                val span = (prices.max() - lo).takeIf { it > 1e-9 } ?: 1.0
                val path = Path()
                prices.forEachIndexed { i, p ->
                    val x = i * slot + slot / 2
                    val y = size.height - ((p - lo) / span).toFloat() * size.height * 0.9f - size.height * 0.05f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, PriceBlue, style = Stroke(width = 3.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(hours.first(), hours[hours.size / 2], hours.last()).forEach {
                Text(it.start.hhmm(), style = MaterialTheme.typography.labelSmall, color = onSurface)
            }
        }
    }
}
