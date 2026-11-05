package ar.jltabbia.weathergim.data.model

enum class WeatherCondition(
    val title: String,
    val description: String,
    val defaultActivity: String
) {
    SUNNY("Soleado", "Cielo despejado y brillante", "Running al aire libre"),
    PARTLY_CLOUDY("Parcialmente Nublado", "Nubes dispersas con intervalos de sol", "Caminata enérgica"),
    CLOUDY("Nublado", "Cielo cubierto", "Entrenamiento funcional en parque"),
    RAINY("Lluvioso", "Precipitaciones frecuentes", "Yoga o Stretching en interior"),
    THUNDERSTORM("Tormentoso", "Tormentas eléctricas con lluvia", "Gimnasio bajo techo"),
    SNOWY("Nivoso", "Nevadas y frío intenso", "Esquí o senderismo de invierno"),
    WINDY("Ventoso", "Vientos moderados a fuertes", "Ciclismo indoor o running controlado")
}

data class ActivitySuggestion(
    val title: String,
    val category: String, // e.g. "Cardio", "Fuerza", "Flexibilidad"
    val intensity: String, // e.g. "Alta", "Media", "Suave"
    val duration: String, // e.g. "45 min"
    val icon: String, // emoji
    val description: String
)

data class LocationInfo(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false
)

data class HourlyForecast(
    val hour: Int, // e.g. 7 to 20
    val temperature: Int,
    val condition: WeatherCondition,
    val precipitationProb: Int, // percentage
    val activitySuggestions: List<ActivitySuggestion>
)

data class DailyForecast(
    val date: String, // e.g. "2025-05-20"
    val dayName: String, // e.g. "Hoy", "Vie", "Sáb"...
    val minTemp: Int,
    val maxTemp: Int,
    val condition: WeatherCondition,
    val hourlyForecasts: List<HourlyForecast>
)

data class WeatherInfo(
    val location: LocationInfo,
    val currentTemp: Int,
    val apparentTemp: Int,
    val humidity: Int,
    val windSpeed: Double,
    val uvIndex: Double,
    val condition: WeatherCondition,
    val description: String,
    val dailyForecasts: List<DailyForecast>
)
