package ar.jltabbia.weathergim.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.jltabbia.weathergim.data.model.HourlyForecast
import ar.jltabbia.weathergim.data.model.LocationInfo
import ar.jltabbia.weathergim.data.model.WeatherInfo
import ar.jltabbia.weathergim.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()

    val popularLocations = repository.popularLocations

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    var selectedDayIndex by mutableStateOf(0)
        private set

    var selectedHourlyForecast by mutableStateOf<HourlyForecast?>(null)
        private set

    var showLocationDialog by mutableStateOf(false)

    init {
        loadWeather(popularLocations.first())
    }

    fun selectDay(index: Int) {
        selectedDayIndex = index
        val state = _weatherState.value
        if (state is WeatherState.Success) {
            val day = state.weather.dailyForecasts.getOrNull(index)
            selectedHourlyForecast = day?.hourlyForecasts?.firstOrNull()
        }
    }

    fun selectHour(hourly: HourlyForecast) {
        selectedHourlyForecast = hourly
    }

    fun loadWeather(location: LocationInfo) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val info = repository.getWeatherData(location)
                selectedDayIndex = 0
                selectedHourlyForecast = info.dailyForecasts.firstOrNull()?.hourlyForecasts?.firstOrNull()
                _weatherState.value = WeatherState.Success(info)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(e.message ?: "Error al obtener el clima")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun loadCurrentDeviceLocation(context: Context) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                var bestLocation: Location? = null

                if (locationManager != null) {
                    val providers = locationManager.getProviders(true)
                    for (provider in providers) {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                bestLocation = loc
                            }
                        }
                    }
                }

                if (bestLocation != null) {
                    val currentLoc = LocationInfo(
                        name = "Mi Ubicación",
                        country = "GPS Actual",
                        latitude = bestLocation.latitude,
                        longitude = bestLocation.longitude,
                        isCurrentLocation = true
                    )
                    loadWeather(currentLoc)
                } else {
                    loadWeather(popularLocations.first())
                }
            } catch (e: Exception) {
                loadWeather(popularLocations.first())
            }
        }
    }
}

sealed interface WeatherState {
    object Loading : WeatherState
    data class Success(val weather: WeatherInfo) : WeatherState
    data class Error(val message: String) : WeatherState
}
