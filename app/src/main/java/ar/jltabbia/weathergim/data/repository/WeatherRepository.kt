package ar.jltabbia.weathergim.data.repository

import android.util.Log
import ar.jltabbia.weathergim.data.model.ActivitySuggestion
import ar.jltabbia.weathergim.data.model.DailyForecast
import ar.jltabbia.weathergim.data.model.HourlyForecast
import ar.jltabbia.weathergim.data.model.LocationInfo
import ar.jltabbia.weathergim.data.model.WeatherCondition
import ar.jltabbia.weathergim.data.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeatherRepository {

    val popularLocations = listOf(
        LocationInfo("Buenos Aires", "Argentina", -34.6037, -58.3816),
        LocationInfo("Madrid", "España", 40.4168, -3.7038),
        LocationInfo("Nueva York", "Estados Unidos", 40.7128, -74.0060),
        LocationInfo("Tokio", "Japón", 35.6762, 139.6503),
        LocationInfo("Londres", "Reino Unido", 51.5074, -0.1278),
        LocationInfo("París", "Francia", 48.8566, 2.3522),
        LocationInfo("Ciudad de México", "México", 19.4326, -99.1332),
        LocationInfo("Bogotá", "Colombia", 4.7110, -74.0721),
        LocationInfo("Santiago", "Chile", -33.4489, -70.6693),
        LocationInfo("Barcelona", "España", 41.3851, 2.1734)
    )

    suspend fun getWeatherData(location: LocationInfo): WeatherInfo = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                parseOpenMeteoResponse(location, responseString)
            } else {
                generateMockWeather(location)
            }
        } catch (e: Exception) {
            generateMockWeather(location)
        }
    }

    private fun parseOpenMeteoResponse(location: LocationInfo, jsonStr: String): WeatherInfo {
        val json = JSONObject(jsonStr)
        val current = json.getJSONObject("current")
        val currentTemp = current.getDouble("temperature_2m").toInt()
        val apparentTemp = current.getDouble("apparent_temperature").toInt()
        val humidity = current.getInt("relative_humidity_2m")
        val windSpeed = current.getDouble("wind_speed_10m")
        val weatherCode = current.getInt("weather_code")

        val condition = mapWmoCode(weatherCode)
        val description = getWeatherDescription(weatherCode)

        val dailyObj = json.getJSONObject("daily")
        val timeArr = dailyObj.getJSONArray("time")
        val weatherCodeArr = dailyObj.getJSONArray("weather_code")
        val maxArr = dailyObj.getJSONArray("temperature_2m_max")
        val minArr = dailyObj.getJSONArray("temperature_2m_min")

        val hourlyObj = json.getJSONObject("hourly")
        val hourlyTimeArr = hourlyObj.getJSONArray("time")
        val hourlyTempArr = hourlyObj.getJSONArray("temperature_2m")
        val hourlyCodeArr = hourlyObj.getJSONArray("weather_code")
        val hourlyPrecipArr = hourlyObj.getJSONArray("precipitation_probability")

        val dailyForecasts = mutableListOf<DailyForecast>()

        for (i in 0 until minOf(7, timeArr.length())) {
            val dateStr = timeArr.getString(i)
            val dayMin = minArr.getDouble(i).toInt()
            val dayMax = maxArr.getDouble(i).toInt()
            val dayCode = weatherCodeArr.getInt(i)
            val dayCondition = mapWmoCode(dayCode)

            val dayName = if (i == 0) "Hoy" else getDayOfWeek(dateStr)

            val hourlyList = mutableListOf<HourlyForecast>()
            for (h in 7..20) {
                val targetHourStr = String.format(Locale.US, "%sT%02d:00", dateStr, h)
                var foundTemp = dayMin + ((dayMax - dayMin) / 2)
                var foundCond = dayCondition
                var foundPrecip = 10

                for (j in 0 until hourlyTimeArr.length()) {
                    val tStr = hourlyTimeArr.getString(j)
                    if (tStr.startsWith(targetHourStr) || tStr == targetHourStr) {
                        foundTemp = hourlyTempArr.getDouble(j).toInt()
                        foundCond = mapWmoCode(hourlyCodeArr.getInt(j))
                        foundPrecip = hourlyPrecipArr.getInt(j)
                        break
                    }
                }

                val suggestions = generateActivitySuggestions(foundCond, foundTemp, h, foundPrecip)
                hourlyList.add(
                    HourlyForecast(
                        hour = h,
                        temperature = foundTemp,
                        condition = foundCond,
                        precipitationProb = foundPrecip,
                        activitySuggestions = suggestions
                    )
                )
            }

            dailyForecasts.add(
                DailyForecast(
                    date = dateStr,
                    dayName = dayName,
                    minTemp = dayMin,
                    maxTemp = dayMax,
                    condition = dayCondition,
                    hourlyForecasts = hourlyList
                )
            )
        }

        return WeatherInfo(
            location = location,
            currentTemp = currentTemp,
            apparentTemp = apparentTemp,
            humidity = humidity,
            windSpeed = windSpeed,
            uvIndex = 6.5,
            condition = condition,
            description = description,
            dailyForecasts = dailyForecasts
        )
    }

    private fun mapWmoCode(code: Int): WeatherCondition {
        return when (code) {
            0 -> WeatherCondition.SUNNY
            1, 2 -> WeatherCondition.PARTLY_CLOUDY
            3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.WINDY
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAINY
            95, 96, 99 -> WeatherCondition.THUNDERSTORM
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOWY
            else -> WeatherCondition.PARTLY_CLOUDY
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Cielo despejado"
            1, 2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Neblina y bruma"
            51, 53, 55, 61, 63 -> "Lluvia moderada"
            65 -> "Lluvia intensa"
            95, 96, 99 -> "Tormenta eléctrica"
            71, 73, 75 -> "Nevadas"
            else -> "Condiciones estables"
        }
    }

    private fun getDayOfWeek(dateStr: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(dateStr)
            val cal = Calendar.getInstance()
            if (date != null) {
                cal.time = date
            }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Dom"
                Calendar.MONDAY -> "Lun"
                Calendar.TUESDAY -> "Mar"
                Calendar.WEDNESDAY -> "Mié"
                Calendar.THURSDAY -> "Jue"
                Calendar.FRIDAY -> "Vie"
                Calendar.SATURDAY -> "Sáb"
                else -> "Hoy"
            }
        } catch (e: Exception) {
            "Día"
        }
    }

    private fun generateActivitySuggestions(condition: WeatherCondition, temp: Int, hour: Int, precip: Int): List<ActivitySuggestion> {
        val list = mutableListOf<ActivitySuggestion>()
        val isIndoorPreferred = precip > 40 || condition == WeatherCondition.RAINY || condition == WeatherCondition.THUNDERSTORM || temp < 5

        if (isIndoorPreferred) {
            list.add(
                ActivitySuggestion(
                    title = "Yoga & Movilidad Articular",
                    category = "Flexibilidad",
                    intensity = "Suave",
                    duration = "30 min",
                    icon = "🧘",
                    description = "Ideal para mantener la flexibilidad y relajación en interior."
                )
            )
            list.add(
                ActivitySuggestion(
                    title = "Entrenamiento Funcional en Gimnasio",
                    category = "Fuerza",
                    intensity = "Alta",
                    duration = "50 min",
                    icon = "🏋️",
                    description = "Rutina con pesas y ejercicios compuestos bajo techo."
                )
            )
            list.add(
                ActivitySuggestion(
                    title = "Cardio en Ciclismo Indoor / Spinning",
                    category = "Cardio",
                    intensity = "Alta",
                    duration = "40 min",
                    icon = "🚴",
                    description = "Excelente quema de calorías sin depender del clima exterior."
                )
            )
            list.add(
                ActivitySuggestion(
                    title = "Pilates Mat",
                    category = "Core y Postura",
                    intensity = "Media",
                    duration = "45 min",
                    icon = "🤸",
                    description = "Fortalecimiento de la zona media y control respiratorio."
                )
            )
        } else {
            list.add(
                ActivitySuggestion(
                    title = "Running al Aire Libre",
                    category = "Cardio",
                    intensity = "Alta",
                    duration = "45 min",
                    icon = "🏃‍♂️",
                    description = "Aprovecha el excelente clima para trote continuo o intervalos."
                )
            )
            list.add(
                ActivitySuggestion(
                    title = "Caminata Enérgica (Power Walking)",
                    category = "Cardio Moderado",
                    intensity = "Media",
                    duration = "60 min",
                    icon = "🚶‍♂️",
                    description = "Excelente para quema de grasa y activación matutina/vespertina."
                )
            )
            list.add(
                ActivitySuggestion(
                    title = "Ciclismo de Ruta o Urbano",
                    category = "Resistencia",
                    intensity = "Alta",
                    duration = "60 min",
                    icon = "🚴‍♂️",
                    description = "Ruta al aire libre con viento favorable y cielo despejado."
                )
            )
            list.add(
                ActivitySuggestion(
                    title = "Calistenia en Parque",
                    category = "Fuerza Corporal",
                    intensity = "Media-Alta",
                    duration = "40 min",
                    icon = "🌳",
                    description = "Uso de barras y peso corporal en espacios verdes."
                )
            )
        }
        return list
    }

    private fun generateMockWeather(location: LocationInfo): WeatherInfo {
        val baseTemp = 22
        val dailyList = mutableListOf<DailyForecast>()
        val cal = Calendar.getInstance()

        for (i in 0..6) {
            val dayMin = baseTemp - 4 + (i % 3)
            val dayMax = baseTemp + 6 + (i % 2)
            val cond = if (i == 3) WeatherCondition.RAINY else if (i == 5) WeatherCondition.PARTLY_CLOUDY else WeatherCondition.SUNNY

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val dayName = if (i == 0) "Hoy" else getDayOfWeek(dateStr)

            val hourlyList = mutableListOf<HourlyForecast>()
            for (h in 7..20) {
                val t = dayMin + ((h - 7) * (dayMax - dayMin) / 14)
                val precip = if (cond == WeatherCondition.RAINY) 70 else 5
                val suggestions = generateActivitySuggestions(cond, t, h, precip)
                hourlyList.add(
                    HourlyForecast(
                        hour = h,
                        temperature = t,
                        condition = cond,
                        precipitationProb = precip,
                        activitySuggestions = suggestions
                    )
                )
            }

            dailyList.add(
                DailyForecast(
                    date = dateStr,
                    dayName = dayName,
                    minTemp = dayMin,
                    maxTemp = dayMax,
                    condition = cond,
                    hourlyForecasts = hourlyList
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return WeatherInfo(
            location = location,
            currentTemp = 24,
            apparentTemp = 26,
            humidity = 55,
            windSpeed = 12.5,
            uvIndex = 7.0,
            condition = WeatherCondition.SUNNY,
            description = "Cielo despejado y excelente clima",
            dailyForecasts = dailyList
        )
    }
}
