package app.vplusstudio.zonmoment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.vplusstudio.zonmoment.R
import app.vplusstudio.zonmoment.data.ForecastRepository
import app.vplusstudio.zonmoment.data.NetworkException
import app.vplusstudio.zonmoment.domain.Appliance
import app.vplusstudio.zonmoment.domain.RoofDirection
import app.vplusstudio.zonmoment.domain.UserSettings
import kotlinx.coroutines.launch

private val POSTCODE = Regex("^[1-9][0-9]{3}\\s?[A-Za-z]{2}$")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    initial: UserSettings?,
    onSaved: (UserSettings) -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var postcode by remember { mutableStateOf(initial?.postcode ?: "") }
    var panels by remember { mutableStateOf(initial?.panelCount?.toString() ?: "10") }
    var wp by remember { mutableStateOf(initial?.wattPeakPerPanel?.toString() ?: "400") }
    var roof by remember { mutableStateOf(initial?.roof ?: RoofDirection.SOUTH) }
    var dynamic by remember { mutableStateOf(initial?.dynamicContract ?: false) }
    var appliances by remember {
        mutableStateOf(initial?.appliances ?: setOf(Appliance.WASHING_MACHINE, Appliance.DISHWASHER))
    }
    var error by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = postcode,
            onValueChange = { postcode = it.take(7) },
            label = { Text(stringResource(R.string.postcode)) },
            placeholder = { Text(stringResource(R.string.postcode_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = panels,
                onValueChange = { panels = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.panel_count)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = wp,
                onValueChange = { wp = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(R.string.watt_peak)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Text(stringResource(R.string.roof_direction), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoofDirection.entries.forEach { dir ->
                FilterChip(selected = roof == dir, onClick = { roof = dir }, label = { Text(stringResource(dir.label)) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dynamic_contract), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.dynamic_contract_help), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = dynamic, onCheckedChange = { dynamic = it })
        }

        Text(stringResource(R.string.appliances), style = MaterialTheme.typography.titleSmall)
        Appliance.entries.forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = a in appliances,
                    onCheckedChange = { on -> appliances = if (on) appliances + a else appliances - a },
                )
                Text("${a.emoji}  ${stringResource(a.label)}")
            }
        }

        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy,
                onClick = {
                    val count = panels.toIntOrNull()
                    val peak = wp.toIntOrNull()
                    error = when {
                        !POSTCODE.matches(postcode.trim()) -> R.string.error_postcode_format
                        count == null || count !in 1..200 -> R.string.error_panels
                        peak == null || peak !in 100..800 -> R.string.error_wp
                        appliances.isEmpty() -> R.string.error_appliances
                        else -> null
                    }
                    if (error != null) return@Button
                    busy = true
                    scope.launch {
                        try {
                            val point = ForecastRepository.geocode(postcode.trim())
                            if (point == null) {
                                error = R.string.error_postcode_unknown
                            } else {
                                onSaved(
                                    UserSettings(
                                        postcode = postcode.trim().uppercase(),
                                        latitude = point.latitude,
                                        longitude = point.longitude,
                                        panelCount = count!!,
                                        wattPeakPerPanel = peak!!,
                                        roof = roof,
                                        tiltDegrees = 35,
                                        dynamicContract = dynamic,
                                        appliances = appliances,
                                    ),
                                )
                            }
                        } catch (e: NetworkException) {
                            error = R.string.error_network
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = R.string.error_data
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(stringResource(if (busy) R.string.saving else R.string.save)) }
            if (onCancel != null) TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
}
