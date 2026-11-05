package ar.jltabbia.weathergim.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.jltabbia.weathergim.data.model.WeatherCondition
import ar.jltabbia.weathergim.data.model.WeatherInfo
import ar.jltabbia.weathergim.ui.components.AnimatedWeatherIcon
import ar.jltabbia.weathergim.ui.components.DailyForecastList
import ar.jltabbia.weathergim.ui.components.HourActivityDetailCard
import ar.jltabbia.weathergim.ui.components.HourlyForecastTimeline
import ar.jltabbia.weathergim.ui.components.LocationSelectionDialog

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val state by viewModel.weatherState.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadCurrentDeviceLocation(context)
        } else {
            viewModel.showLocationDialog = true
        }
    }

    if (viewModel.showLocationDialog) {
        LocationSelectionDialog(
            popularLocations = viewModel.popularLocations,
            onLocationSelected = { loc ->
                viewModel.loadWeather(loc)
            },
            onUseCurrentLocation = {
                val permissionCheck = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadCurrentDeviceLocation(context)
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            onDismiss = {
                viewModel.showLocationDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            WeatherTopBar(
                locationName = when (state) {
                    is WeatherState.Success -> (state as WeatherState.Success).weather.location.name
                    else -> "Cargando..."
                },
                onLocationClick = { viewModel.showLocationDialog = true },
                onGpsClick = {
                    val permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        viewModel.loadCurrentDeviceLocation(context)
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            )
        }
    ) { innerPadding ->
        val backgroundBrush = when (state) {
            is WeatherState.Success -> {
                val cond = (state as WeatherState.Success).weather.condition
                getBackgroundBrush(cond)
            }
            else -> Brush.verticalGradient(
                colors = listOf(Color(0xFF1976D2), Color(0xFF0D47A1))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            when (val s = state) {
                is WeatherState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is WeatherState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadWeather(viewModel.popularLocations.first()) }) {
                            Text("Reintentar")
                        }
                    }
                }
                is WeatherState.Success -> {
                    val weather = s.weather
                    val selectedDay = weather.dailyForecasts.getOrElse(viewModel.selectedDayIndex) { weather.dailyForecasts.first() }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Current Weather Hero Card
                        CurrentWeatherHero(weather = weather)

                        Spacer(modifier = Modifier.height(16.dp))

                        // 7-Day Forecast List
                        DailyForecastList(
                            dailyForecasts = weather.dailyForecasts,
                            selectedIndex = viewModel.selectedDayIndex,
                            onDaySelected = { viewModel.selectDay(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hourly Forecast Timeline (07:00 to 20:00) with Hour Selection
                        HourlyForecastTimeline(
                            hourlyForecasts = selectedDay.hourlyForecasts,
                            selectedHour = viewModel.selectedHourlyForecast,
                            onHourSelected = { viewModel.selectHour(it) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Multiple Physical Activity Suggestions for Selected Hour
                        viewModel.selectedHourlyForecast?.let { hourForecast ->
                            HourActivityDetailCard(hourlyForecast = hourForecast)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherTopBar(
    locationName: String,
    onLocationClick: () -> Unit,
    onGpsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x22000000))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onLocationClick() }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = locationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = onGpsClick) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Ubicación GPS",
                tint = Color.White
            )
        }
    }
}

@Composable
fun CurrentWeatherHero(weather: WeatherInfo) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weather.location.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = weather.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${weather.currentTemp}°",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Sensación ${weather.apparentTemp}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                AnimatedWeatherIcon(
                    condition = weather.condition,
                    size = 96.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weather stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherStatItem(label = "Humedad", value = "${weather.humidity}%", icon = "💧")
                WeatherStatItem(label = "Viento", value = "${weather.windSpeed} km/h", icon = "💨")
                WeatherStatItem(label = "Índice UV", value = "${weather.uvIndex}", icon = "☀️")
            }
        }
    }
}

@Composable
fun WeatherStatItem(label: String, value: String, icon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

private fun getBackgroundBrush(condition: WeatherCondition): Brush {
    return when (condition) {
        WeatherCondition.SUNNY -> Brush.verticalGradient(
            colors = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5), Color(0xFF0D47A1))
        )
        WeatherCondition.PARTLY_CLOUDY -> Brush.verticalGradient(
            colors = listOf(Color(0xFF5C6BC0), Color(0xFF3949AB), Color(0xFF1A237E))
        )
        WeatherCondition.CLOUDY -> Brush.verticalGradient(
            colors = listOf(Color(0xFF78909C), Color(0xFF455A64), Color(0xFF263238))
        )
        WeatherCondition.RAINY -> Brush.verticalGradient(
            colors = listOf(Color(0xFF37474F), Color(0xFF263238), Color(0xFF000000))
        )
        WeatherCondition.THUNDERSTORM -> Brush.verticalGradient(
            colors = listOf(Color(0xFF212121), Color(0xFF111111), Color(0xFF000000))
        )
        WeatherCondition.SNOWY -> Brush.verticalGradient(
            colors = listOf(Color(0xFF90CAF9), Color(0xFF546E7A), Color(0xFF37474F))
        )
        WeatherCondition.WINDY -> Brush.verticalGradient(
            colors = listOf(Color(0xFF29B6F6), Color(0xFF0288D1), Color(0xFF01579B))
        )
    }
}
