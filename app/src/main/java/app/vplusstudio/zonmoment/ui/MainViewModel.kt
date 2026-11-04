package app.vplusstudio.zonmoment.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vplusstudio.zonmoment.R
import app.vplusstudio.zonmoment.data.AMSTERDAM
import app.vplusstudio.zonmoment.data.Forecast
import app.vplusstudio.zonmoment.data.ForecastRepository
import app.vplusstudio.zonmoment.data.NetworkException
import app.vplusstudio.zonmoment.data.SettingsStore
import app.vplusstudio.zonmoment.domain.Planner
import app.vplusstudio.zonmoment.domain.Recommendation
import app.vplusstudio.zonmoment.domain.UserSettings
import app.vplusstudio.zonmoment.work.DailyAdvice
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

sealed interface HomeState {
    data object Loading : HomeState
    data class Error(@StringRes val message: Int) : HomeState
    data class Ready(
        val forecast: Forecast,
        val today: List<Recommendation?>,
        val tomorrow: List<Recommendation?>,
    ) : HomeState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    var settings by mutableStateOf(store.load())
        private set
    var editing by mutableStateOf(settings == null)
        private set
    var home by mutableStateOf<HomeState>(HomeState.Loading)
        private set

    init {
        if (settings != null) refresh()
    }

    fun startEditing() {
        editing = true
    }

    fun cancelEditing() {
        if (settings != null) editing = false
    }

    /** Called by the setup screen after it has resolved the postcode. */
    fun saveSettings(new: UserSettings) {
        store.save(new)
        settings = new
        editing = false
        DailyAdvice.schedule(getApplication())
        refresh()
    }

    fun refresh() {
        val s = settings ?: return
        home = HomeState.Loading
        viewModelScope.launch {
            home = try {
                val forecast = ForecastRepository.forecast(s)
                val now = ZonedDateTime.now(AMSTERDAM)
                val today = LocalDate.now(AMSTERDAM)
                val apps = s.appliances.sortedBy { it.ordinal }
                HomeState.Ready(
                    forecast = forecast,
                    today = apps.map { Planner.bestWindow(forecast.hours, it, today, now, s.dynamicContract) },
                    tomorrow = apps.map { Planner.bestWindow(forecast.hours, it, today.plusDays(1), now, s.dynamicContract) },
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: NetworkException) {
                HomeState.Error(R.string.error_network)
            } catch (e: Exception) {
                HomeState.Error(R.string.error_data)
            }
        }
    }
}
