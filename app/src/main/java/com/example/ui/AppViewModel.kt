package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SavedItinerary
import com.example.data.TouristSpot
import com.example.data.TestingLog
import com.example.data.api.GeminiRepository
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork
import com.example.domain.LegType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WeatherInfo(
    val tempCelsius: Int,
    val conditionRo: String,
    val conditionEn: String,
    val windKmh: Int,
    val rainProbabilityPercent: Int,
    val uvIndex: Int,
    val adviceRo: String,
    val adviceEn: String,
    val iconEmoji: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val spotDao = database.touristSpotDao()
    private val itineraryDao = database.savedItineraryDao()
    private val testingLogDao = database.testingLogDao()

    // Settings & API preferences
    private val prefs = application.getSharedPreferences("bus_tour_prefs", android.content.Context.MODE_PRIVATE)

    private val _openaiApiKey = MutableStateFlow(prefs.getString("openai_api_key", "") ?: "")
    val openaiApiKey: StateFlow<String> = _openaiApiKey.asStateFlow()

    private val _openrouterApiKey = MutableStateFlow(prefs.getString("openrouter_api_key", "") ?: "")
    val openrouterApiKey: StateFlow<String> = _openrouterApiKey.asStateFlow()

    private val _openrouterModel = MutableStateFlow(prefs.getString("openrouter_model", "google/gemma-2-9b-it:free") ?: "google/gemma-2-9b-it:free")
    val openrouterModel: StateFlow<String> = _openrouterModel.asStateFlow()

    private val _aiProvider = MutableStateFlow(prefs.getString("ai_provider", "GEMINI") ?: "GEMINI")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    fun updateOpenAiApiKey(apiKey: String) {
        _openaiApiKey.value = apiKey
        prefs.edit().putString("openai_api_key", apiKey).apply()
    }

    fun updateOpenRouterApiKey(apiKey: String) {
        _openrouterApiKey.value = apiKey
        prefs.edit().putString("openrouter_api_key", apiKey).apply()
    }

    fun updateOpenRouterModel(model: String) {
        _openrouterModel.value = model
        prefs.edit().putString("openrouter_model", model).apply()
    }

    fun updateAiProvider(provider: String) {
        _aiProvider.value = provider
        prefs.edit().putString("ai_provider", provider).apply()
    }

    // Screen States
    private val _selectedCity = MutableStateFlow("București")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    // Selected starting point (Gara Centrală/Nord or Hotel)
    private val _customStartSpot = MutableStateFlow<TouristSpot?>(null)
    val customStartSpot: StateFlow<TouristSpot?> = _customStartSpot.asStateFlow()

    // All spots from Database
    val allSpots: StateFlow<List<TouristSpot>> = spotDao.getAllSpotsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All field-testing logs from Database
    val testingLogs: StateFlow<List<TestingLog>> = testingLogDao.getAllLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered attractions based on selectedCity
    val citySpots: StateFlow<List<TouristSpot>> = combine(allSpots, selectedCity) { spots, city ->
        spots.filter { it.city == city }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Itineraries
    val savedItineraries: StateFlow<List<SavedItinerary>> = itineraryDao.getAllSavedItinerariesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Route Optimization Preferences
    private val _routingPreference = MutableStateFlow<String>("DEFAULT")
    val routingPreference: StateFlow<String> = _routingPreference.asStateFlow()

    private val _enabledTransitModes = MutableStateFlow<Set<LegType>>(
        setOf(LegType.WALK, LegType.BUS, LegType.METRO, LegType.TROLLEY, LegType.TRAIN, LegType.TAXI)
    )
    val enabledTransitModes: StateFlow<Set<LegType>> = _enabledTransitModes.asStateFlow()

    fun updateRoutingPreference(preference: String) {
        _routingPreference.value = preference
    }

    fun toggleTransitMode(mode: LegType) {
        val current = _enabledTransitModes.value
        _enabledTransitModes.value = if (current.contains(mode)) {
            if (current.size <= 1) current else current - mode
        } else {
            current + mode
        }
    }

    // Transit journey calculated locally
    val optimizedJourney: StateFlow<OptimizedJourney?> = combine(
        citySpots, 
        selectedCity, 
        customStartSpot,
        routingPreference,
        enabledTransitModes
    ) { spots, city, customStart, pref, modes ->
        val start = customStart ?: TransitNetwork.getStartSpot(city)
        val selectedSpots = spots.filter { it.isSelected }
        if (selectedSpots.isNotEmpty()) {
            TransitNetwork.optimizePath(start, selectedSpots, city, pref, modes)
        } else {
            null
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Gemini API recommendation text
    private val _aiRecommendation = MutableStateFlow<String>("")
    val aiRecommendation: StateFlow<String> = _aiRecommendation.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _selectedWeatherPreset = MutableStateFlow("DEFAULT")
    val selectedWeatherPreset: StateFlow<String> = _selectedWeatherPreset.asStateFlow()

    fun setWeatherPreset(preset: String) {
        _selectedWeatherPreset.value = preset
    }

    val weatherPresetsByCity = mapOf(
        "București" to mapOf(
            "DEFAULT" to WeatherInfo(
                tempCelsius = 26,
                conditionRo = "Senin",
                conditionEn = "Sunny",
                windKmh = 12,
                rainProbabilityPercent = 5,
                uvIndex = 8,
                adviceRo = "Vreme perfectă pentru o vizită la Ateneu sau Parcul Herăstrău. Hidratează-te bine!",
                adviceEn = "Perfect weather for visiting the Athenaeum or Herăstrău Park. Stay hydrated!",
                iconEmoji = "☀️"
            ),
            "SUNNY" to WeatherInfo(
                tempCelsius = 24,
                conditionRo = "Senin de primăvară",
                conditionEn = "Spring Sunny",
                windKmh = 8,
                rainProbabilityPercent = 0,
                uvIndex = 7,
                adviceRo = "Soare calm și cer limpede. Clădirea Ateneului va arăta fabulos în fotografii!",
                adviceEn = "Calm sun and clear skies. The Athenaeum building will look fabulous in photos!",
                iconEmoji = "☀️"
            ),
            "RAINY" to WeatherInfo(
                tempCelsius = 16,
                conditionRo = "Furtună Torențială",
                conditionEn = "Heavy Torrential Storm",
                windKmh = 25,
                rainProbabilityPercent = 95,
                uvIndex = 1,
                adviceRo = "Ploaie abundentă! Evită parcurile în aer liber; vizitează mai degrabă Palatul Parlamentului sau Muzeul Antipa.",
                adviceEn = "Heavy rain! Avoid outdoor parks; visit the Palace of Parliament or Antipa Museum instead.",
                iconEmoji = "⛈️"
            ),
            "CLOUDY" to WeatherInfo(
                tempCelsius = 21,
                conditionRo = "Noros și Răcoros",
                conditionEn = "Cloudy and Breezy",
                windKmh = 18,
                rainProbabilityPercent = 30,
                uvIndex = 4,
                adviceRo = "Cer acoperit, perfect pentru plimbări urbane fără arșiță prin Centrul Vechi sau Calea Victoriei.",
                adviceEn = "Overcast sky, perfect for cool urban walks along the Old Town or Calea Victoriei without the heat.",
                iconEmoji = "☁️"
            ),
            "HEATWAVE" to WeatherInfo(
                tempCelsius = 38,
                conditionRo = "Caniculă Extremă",
                conditionEn = "Extreme Heatwave",
                windKmh = 6,
                rainProbabilityPercent = 0,
                uvIndex = 11,
                adviceRo = "Cod Portocaliu! Căldură caniculară. Protejează-te la umbră în Parcul Cișmigiu sau mall-uri răcoroase.",
                adviceEn = "Orange Alert! Sizzling heatwave. Seek shade in Cișmigiu Park or air-conditioned museums.",
                iconEmoji = "🥵"
            )
        ),
        "Brașov" to mapOf(
            "DEFAULT" to WeatherInfo(
                tempCelsius = 15,
                conditionRo = "Ploaie Ușoară de Munte",
                conditionEn = "Light Mountain Rain",
                windKmh = 10,
                rainProbabilityPercent = 75,
                uvIndex = 3,
                adviceRo = "Plouă slab la munte; ia o umbrelă dacă iei telecabina spre Tâmpa sau mergi pe Strada Sforii.",
                adviceEn = "Light mountain rain; take an umbrella if you ride the cable car to Tâmpa or walk Rope Street.",
                iconEmoji = "🌧️"
            ),
            "SUNNY" to WeatherInfo(
                tempCelsius = 22,
                conditionRo = "Senin de Munte",
                conditionEn = "Sunny Alpine Weather",
                windKmh = 12,
                rainProbabilityPercent = 5,
                uvIndex = 8,
                adviceRo = "Vreme excepțională la munte. Excelent pentru a urca pe Tâmpa sau a explora Piața Sfatului.",
                adviceEn = "Exceptional alpine weather. Excellent for hiking up Tâmpa or exploring the Council Square.",
                iconEmoji = "☀️"
            ),
            "RAINY" to WeatherInfo(
                tempCelsius = 11,
                conditionRo = "Aversă Rece de Munte",
                conditionEn = "Cold Mountain Rain",
                windKmh = 22,
                rainProbabilityPercent = 90,
                uvIndex = 2,
                adviceRo = "Frig și ploaie. Vizitează spații călduroase precum Biserica Neagră sau relaxează-te într-o cafenea în Piața Sfatului.",
                adviceEn = "Cold & wet. Better visit cozy indoor spaces like the Black Church or warm up in a cafe in Council Square.",
                iconEmoji = "🌧️"
            ),
            "CLOUDY" to WeatherInfo(
                tempCelsius = 16,
                conditionRo = "Ceață și Nori joși",
                conditionEn = "Fog & Low Clouds",
                windKmh = 9,
                rainProbabilityPercent = 40,
                uvIndex = 4,
                adviceRo = "Vizibilitate redusă spre Tâmpa. Străzile înguste medievale precum Strada Sforii vor avea un aer mistic pitoresc.",
                adviceEn = "Limited visibility at the Peak. The narrow streets like Rope Street will look atmospheric and mystical.",
                iconEmoji = "🌫️"
            ),
            "HEATWAVE" to WeatherInfo(
                tempCelsius = 31,
                conditionRo = "Căldură neobișnuită",
                conditionEn = "Unusually Warm",
                windKmh = 8,
                rainProbabilityPercent = 10,
                uvIndex = 9,
                adviceRo = "Aer destul de cald. Mergi la umbra pădurii din jurul Turnului Alb sau pe aleea de sub Tâmpa.",
                adviceEn = "Warm mountain air. Walk under the shady canopy near the White Tower or under Tâmpa hill paths.",
                iconEmoji = "🥵"
            )
        ),
        "Cluj-Napoca" to mapOf(
            "DEFAULT" to WeatherInfo(
                tempCelsius = 20,
                conditionRo = "Parțial Noros",
                conditionEn = "Partly Cloudy",
                windKmh = 16,
                rainProbabilityPercent = 20,
                uvIndex = 6,
                adviceRo = "Vânt plăcut și soare blând; excelent pentru a explora Grădina Botanică sau Turnul Croitorilor.",
                adviceEn = "Pleasant breeze and mild sun; excellent for walking around the Botanical Garden or Tailor's Tower.",
                iconEmoji = "⛅"
            ),
            "SUNNY" to WeatherInfo(
                tempCelsius = 27,
                conditionRo = "Senin și Cald",
                conditionEn = "Sunny and Warm",
                windKmh = 10,
                rainProbabilityPercent = 0,
                uvIndex = 8,
                adviceRo = "Zile pline de soare! perfect pentru a savura o înghețată în Piața Unirii sau a urca pe Cetățuie.",
                adviceEn = "Sunny days! Perfect for eating ice cream in Unirii Square or climbing up Cetățuia Castle hill.",
                iconEmoji = "☀️"
            ),
            "RAINY" to WeatherInfo(
                tempCelsius = 14,
                conditionRo = "Aversă tomnatică",
                conditionEn = "Autumn-like Rain",
                windKmh = 20,
                rainProbabilityPercent = 80,
                uvIndex = 2,
                adviceRo = "Ploaie rece. În loc de Grădina Botanică, recomandăm Catedrala Sfântul Mihail sau Muzeul de Artă.",
                adviceEn = "Chilly rain. Instead of the Botanical Garden, explore St. Michael's Church or Cluj Art Museum.",
                iconEmoji = "🌧️"
            ),
            "CLOUDY" to WeatherInfo(
                tempCelsius = 19,
                conditionRo = "Cer Acoperit",
                conditionEn = "Overcast Cloudy",
                windKmh = 15,
                rainProbabilityPercent = 25,
                uvIndex = 4,
                adviceRo = "Noros și stabil. Vizitează serele Grădinii Botanice pentru colecții adăpostite frumos.",
                adviceEn = "Stable overcast sky. Visit the Botanical Garden greenhouses to see exotic collections shielded from wind.",
                iconEmoji = "⛅"
            ),
            "HEATWAVE" to WeatherInfo(
                tempCelsius = 35,
                conditionRo = "Arșiță Caniculară",
                conditionEn = "Sizzling Heatwave",
                windKmh = 7,
                rainProbabilityPercent = 10,
                uvIndex = 10,
                adviceRo = "Zăpușeală mare! Adăpostește-te în zone umbroase precum Parcul Central sau vizitează Muzeul Etnografic.",
                adviceEn = "Heavy heat! Retreat to the dense shade of Central Park or explore the indoor Ethnographic Museum.",
                iconEmoji = "🥵"
            )
        )
    )

    val currentWeather: StateFlow<WeatherInfo> = combine(selectedCity, _selectedWeatherPreset) { city, preset ->
        val presets = weatherPresetsByCity[city] ?: weatherPresetsByCity["București"] ?: weatherPresetsByCity.values.firstOrNull() ?: emptyMap()
        presets[preset] ?: presets["DEFAULT"] ?: presets.values.firstOrNull() ?: WeatherInfo(26, "Senin", "Sunny", 12, 5, 8, "Vreme perfectă.", "Perfect weather.", "☀️")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeatherInfo(26, "Senin", "Sunny", 12, 5, 8, "Vreme perfectă.", "Perfect weather.", "☀️")
    )

    init {
        viewModelScope.launch {
            optimizedJourney.collectLatest { journey ->
                if (journey != null && _aiRecommendation.value.isNotEmpty() && !_aiRecommendation.value.startsWith("Se generează") && !_aiRecommendation.value.startsWith("Generating") && !_aiRecommendation.value.startsWith("⚠️")) {
                     _aiRecommendation.value = "" // Clear so it auto-regenerates when reaching the tab
                }
            }
        }
        // Pre-populate database with default cities values if empty
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val currentBucurestiCount = spotDao.getSpotsByCity("București").count { !it.isCustom }
                if (currentBucurestiCount < TransitNetwork.BUCURESTI_PRESETS.size) {
                    spotDao.insertSpots(TransitNetwork.BUCURESTI_PRESETS)
                }
                if (spotDao.getSpotsByCity("Cluj-Napoca").isEmpty()) {
                    spotDao.insertSpots(TransitNetwork.CLUJ_PRESETS)
                }
                if (spotDao.getSpotsByCity("Brașov").isEmpty()) {
                    spotDao.insertSpots(TransitNetwork.BRASOV_PRESETS)
                }
                if (spotDao.getSpotsByCity("Câmpina").isEmpty()) {
                    spotDao.insertSpots(TransitNetwork.CAMPINA_PRESETS)
                }

                // Ensure at least some default spots are selected for all cities so users have a beautiful out-of-the-box experience
                val cities = listOf("București", "Cluj-Napoca", "Brașov", "Câmpina")
                cities.forEach { city ->
                    val spots = spotDao.getSpotsByCity(city)
                    if (spots.isNotEmpty() && spots.count { it.isSelected } == 0) {
                        spots.take(3).forEach { spot ->
                            spotDao.updateSelection(spot.id, true)
                        }
                    }
                }
            }
        }
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
        _customStartSpot.value = null // reset custom start spot for the new city
        _aiRecommendation.value = ""

        // Auto-select the first 3 spots if absolutely none are selected for this city
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val currentSpots = spotDao.getSpotsByCity(city)
                if (currentSpots.isNotEmpty() && currentSpots.count { it.isSelected } == 0) {
                    currentSpots.take(3).forEach { spot ->
                        spotDao.updateSelection(spot.id, true)
                    }
                }
            }
        }
    }

    fun addCustomTouristSpot(name: String, description: String, lat: Double, lng: Double, duration: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val spot = TouristSpot(
                    name = name,
                    city = _selectedCity.value,
                    latitude = lat,
                    longitude = lng,
                    visitDurationMinutes = duration,
                    description = description,
                    isCustom = true,
                    isSelected = true
                )
                spotDao.insertSpot(spot)
            }
        }
    }

    fun deleteSpot(spotId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                spotDao.deleteSpotById(spotId)
            }
        }
    }

    fun toggleSpotSelection(spotId: Long, isSelected: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                spotDao.updateSelection(spotId, isSelected)
            }
        }
    }

    fun deselectAllSpots() {
        _customStartSpot.value = null // reset custom starting point to default
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                spotDao.deselectAllSpotsForCity(_selectedCity.value)
            }
        }
    }

    fun updateCustomStartSpot(lat: Double, lng: Double, name: String = "Hotel / Start Personalizat") {
        val city = _selectedCity.value
        _customStartSpot.value = TouristSpot(
            id = -999,
            name = name,
            city = city,
            latitude = lat,
            longitude = lng,
            visitDurationMinutes = 0,
            description = "Punctul tău personalizat de pornire.",
            isCustom = true,
            isSelected = true
        )
    }

    fun saveCurrentItinerary(journey: OptimizedJourney) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val names = journey.orderedSpots.joinToString(" ➔ ") { it.name }
                val details = "Pornire: ${customStartSpot.value?.name ?: "Gara Centrală"}\n" +
                        "Traseu: $names\n\n" +
                        "Etape de transport:\n" +
                        journey.legs.joinToString("\n") { leg ->
                            "- ${leg.directions} (${leg.durationMinutes} min)"
                        }
                
                val itinerary = SavedItinerary(
                    city = _selectedCity.value,
                    spotsCount = journey.orderedSpots.size,
                    totalDurationMinutes = journey.totalDurationMinutes,
                    routeDetails = details
                )
                itineraryDao.insertItinerary(itinerary)
            }
        }
    }

    fun deleteSavedItinerary(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                itineraryDao.deleteItineraryById(id)
            }
        }
    }

    fun clearAllSavedItineraries() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                itineraryDao.deleteAllItineraries()
            }
        }
    }

    fun clearAllCustomCurrentCity() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                spotDao.clearCustomSpotsByCity(_selectedCity.value)
            }
        }
    }

    fun askGeminiForItinerary(isEnglish: Boolean = false) {
        val journey = optimizedJourney.value
        if (journey == null) {
            _aiRecommendation.value = if (isEnglish) {
                "⚠️ **No Selected Attractions Found!**\n\nPlease go to the **'Spots'** (Obiective) or **'Map'** (Hartă) tab and **include (select) tourist attractions** using the switches or buttons, then click generate again."
            } else {
                "⚠️ **Niciun obiectiv selectat!**\n\nTe rugăm să mergi la fila **'Obiective'** sau **'Hartă'** și să **selectezi (bifezi) puncte turistice** de vizitat (apasă butonul Include sau bifează caseta), apoi apasă din nou pe generare."
            }
            return
        }
        val currentCityName = _selectedCity.value
        val attractions = journey.orderedSpots.map { spot ->
            val name = if (isEnglish) translateSpotNameInVm(spot.name) else spot.name
            val desc = if (isEnglish) translateSpotDescInVm(spot.description) else spot.description
            "$name ($desc)"
        }
        val rawStartLocName = _customStartSpot.value?.name ?: TransitNetwork.getStartSpot(currentCityName).name
        val startLocName = if (isEnglish) translateSpotNameInVm(rawStartLocName) else rawStartLocName
        
        val lines = TransitNetwork.getLinesForCity(currentCityName).map { line ->
            "${line.name} [Stații: ${line.stations.joinToString(" ➔ ") { it.name }}]"
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            val provider = _aiProvider.value
            _aiRecommendation.value = if (isEnglish) {
                "Generating AI travel guide with $provider... Please wait..."
            } else {
                "Se generează optimizarea AI cu $provider... Vă rugăm să așteptați..."
            }
            
            try {
                val weather = currentWeather.value
                val weatherText = if (isEnglish) {
                    "${weather.tempCelsius}°C - ${weather.conditionEn}. Recommendation: ${weather.adviceEn}"
                } else {
                    "${weather.tempCelsius}°C - ${weather.conditionRo}. Recomandare: ${weather.adviceRo}"
                }
                val recommendation = withContext(Dispatchers.IO) {
                    when (provider) {
                        "OPENAI" -> {
                            com.example.data.api.OpenAiRepository.generateTravelItinerary(
                                apiKey = _openaiApiKey.value,
                                city = currentCityName,
                                attractions = attractions,
                                busLines = lines,
                                startSpot = startLocName,
                                isEnglish = isEnglish,
                                weatherInfoText = weatherText
                            )
                        }
                        "OPENROUTER" -> {
                            com.example.data.api.OpenRouterRepository.generateTravelItinerary(
                                apiKey = _openrouterApiKey.value,
                                model = _openrouterModel.value,
                                city = currentCityName,
                                attractions = attractions,
                                busLines = lines,
                                startSpot = startLocName,
                                isEnglish = isEnglish,
                                weatherInfoText = weatherText
                            )
                        }
                        else -> {
                            GeminiRepository.generateTravelItinerary(
                                city = currentCityName,
                                attractions = attractions,
                                busLines = lines,
                                startSpot = startLocName,
                                isEnglish = isEnglish,
                                weatherInfoText = weatherText
                            )
                        }
                    }
                }
                _aiRecommendation.value = recommendation
            } catch (e: Exception) {
                _aiRecommendation.value = if (isEnglish) {
                    "Could not call AI: ${e.localizedMessage}"
                } else {
                    "Nu s-a putut apela AI-ul: ${e.localizedMessage}"
                }
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    private fun translateSpotNameInVm(name: String): String {
        return when (name) {
            "Gara de Nord (Hotel/Start)" -> "North Station (Hotel/Start)"
            "Gara de Nord" -> "North Station"
            "Palatul Parlamentului" -> "Palace of the Parliament"
            "Centrul Vechi" -> "Old Town"
            "Ateneul Român" -> "Romanian Athenaeum"
            "Parcul Herăstrău (Mihai I)" -> "Herăstrău Park (Mihai I)"
            "Arcul de Triumf" -> "Triumph Arch"
            "Muzeul Național al Satului" -> "National Village Museum"
            "Parcul Cișmigiu" -> "Cișmigiu Gardens"
            "Muzeul Național Grigore Antipa" -> "Grigore Antipa Natural History Museum"
            "Cărturești Carusel" -> "Cărturești Carusel Bookstore"
            "Biserica Stavropoleos" -> "Stavropoleos Church"
            "Muzeul de Artă al României" -> "National Museum of Art"
            "Grădina Botanică Dimitrie Brândză" -> "Botanical Garden"
            "Parcul Carol I" -> "Carol I Park"
            "Palatul Primăverii" -> "Spring Palace"
            "Piața Revoluției" -> "Revolution Square"
            "Hanul lui Manuc" -> "Manuc's Inn"
            "Muzeul Național de Istorie a României" -> "National History Museum"
            "Palatul Cotroceni" -> "Cotroceni Palace"
            "Parcul Tineretului" -> "Tineretului Park"
            "Catedrala Mântuirii Neamului" -> "People's Salvation Cathedral"
            "Parcul Drumul Taberei" -> "Drumul Taberei Park"
            "Palatul Mogoșoaia" -> "Mogoșoaia Palace"
            "Muzeul de Artă Contemporană (MNAC)" -> "National Museum of Contemporary Art"
            "Piața Universității" -> "University Square"
            "Opera Națională București" -> "Bucharest National Opera"

            "Gara Cluj-Napoca (Hotel/Start)" -> "Cluj-Napoca Railway Station (Hotel/Start)"
            "Gara Cluj-Napoca" -> "Cluj-Napoca Railway Station"
            "Grădina Botanică Alexandru Borza" -> "Botanical Garden"
            "Parcul Central Simion Bărnuțiu" -> "Central Park Simion Bărnuțiu"
            "Piața Unirii & Biserica Sf. Mihail" -> "Union Square & St. Michael Church"
            "Catedrala Mitropolitană & Piața Avram Iancu" -> "Metropolitan Cathedral & Avram Iancu Square"
            "Dealul Cetățuia" -> "Cetățuia Hill"
            "Muzeul de Artă & Palatul Bánffy" -> "Art Museum & Bánffy Palace"
            "Parcul Romulus Vuia (Etnografic)" -> "Romulus Vuia Ethnographic Park"
            "Bastionul Croitorilor" -> "Tailors' Tower"
            "Parcul Iulius (Lacul Gheorgheni)" -> "Iulius Park (Gheorgheni Lake)"
            "Piața Muzeului" -> "Museum Square"
            "Pădurea Hoia-Baciu" -> "Hoia-Baciu Haunted Forest"
            "The Office Cluj & Podul de Fier" -> "The Office Cluj & The Iron Bridge"
            "Teatrul Național și Opera Română" -> "National Theatre and Romanian Opera"
            "Turnul Pompierilor" -> "Firemen's Tower"
            "Parcul Cetățuia Buburuza" -> "Buburuza Cetățuia Park"
            "Muzeul Național de Istorie a Transilvaniei" -> "National History Museum of Transylvania"
            "Biserica Reformată de pe ulița Lupilor" -> "Reformed Church on Wolves' Street"
            "Cluj Arena" -> "Cluj Arena Stadium"
            "BT Arena (Sala Polivalentă)" -> "BT Arena Multi-purpose Hall"
            "Parcul Rozelor" -> "Rose Park"
            "Biserica Calvaria (Mănăștur)" -> "Calvaria Church (Mănăștur)"
            "Catedrala Greco-Catolică Sf. Iosif (Cipariu)" -> "St. Joseph Greek-Catholic Cathedral"
            "Observatorul Astronomic" -> "Astronomical Observatory"
            "Campusul Istoric USAMV" -> "Historical USAMV Campus"
            "Cetatea Fetei Florești" -> "Cetatea Fetei Florești (Hiking Spot)"

            "Gara Brașov (Hotel/Start)" -> "Brașov Railway Station (Hotel/Start)"
            "Gara Brașov" -> "Brașov Railway Station"
            "Biserica Neagră" -> "The Black Church"
            "Piața Sfatului" -> "Council Square"
            "Telecabina Tâmpa" -> "Tâmpa Cable Car"
            "Turnul Alb" -> "The White Tower"
            "Poarta Șchei" -> "Șchei Gate"
            "Turnul Negru" -> "The Black Tower"
            "Bastionul Țesătorilor" -> "Weavers' Bastion"
            "Strada Sforii" -> "Rope Street"
            "Prima Școală Românească" -> "First Romanian School"
            "Poarta Ecaterinei" -> "Catherine's Gate"
            "Parcul Central Nicolae Titulescu" -> "Nicolae Titulescu Central Park"
            "Bastionul Graft" -> "Graft Bastion"
            "Muzeul de Artă Brașov" -> "Brașov Art Museum"
            "Pietrele lui Solomon" -> "Solomon's Rocks"
            "Cetățuia de pe Strajă" -> "The Citadel on Strajă Hill"
            "Turnul Măcelarilor" -> "Butchers' Tower"
            "Bastionul Cojocarilor" -> "Furriers' Bastion"
            "Sinagoga Neologă din Brașov" -> "Neologue Synagogue of Brașov"
            "Casa Sfatului (Muzeul de Istorie)" -> "Council House (History Museum)"
            "Biserica Sfântul Nicolae" -> "St. Nicholas Church"
            "Promenada de sub Tâmpa" -> "Promenade under Tâmpa Mountain"
            "Turnul Lemnarilor" -> "Woodworkers' Tower"
            "Cartierul Istoric Șchei" -> "Șchei Historical Quarter"
            "Grădina Zoologică Brașov (Noua)" -> "Brașov Zoo (Noua)"
            "Lacul Noua & Parc Agrement" -> "Noua Lake & Leisure Park"
            else -> name
        }
    }

    private fun translateSpotDescInVm(description: String): String {
        return when (description) {
            "Punctul de pornire al călătoriei." -> "The starting point of your custom tour."
            "Una dintre cele mai mari clădiri administrative din lume." -> "One of the largest administrative buildings in the world."
            "Inima istorică a Bucureștiului, plină de viață și clădiri de epocă." -> "The historical heart of Bucharest, bursting with life and old buildings."
            "O bijuterie arhitecturală de importanță istorică națională." -> "An architectural treasure of national historical importance."
            "Un parc uriaș, liniștit, situat în jurul unui lac superb." -> "A massive, peaceful park arranged around a pristine lake."
            "Monumentul care celebrează victoria României în Primul Război Mondial." -> "The monument celebrating Romania's victory in World War I."
            "O incursiune în viața rurală tradițională românească în aer liber." -> "An open-air museum exploration of traditional Romanian village life."
            "Cea mai veche grădină publică din București, un lac romantic și alei liniștite." -> "The oldest public garden in Bucharest, featuring a romantic lake and quiet alleys."
            "Expoziții interactive de zoologie, biodiversitate și fosile de dinozaur." -> "Interactive exhibitions of zoology, biodiversity, and dinosaur fossils."
            "Una dintre cele mai spectaculoase librării din lume, situată în Centrul Vechi." -> "One of the most spectacular bookshops in the world, in the Old Town."
            "O capodoperă a stilului brâncovenesc, faimoasă pentru curtea sa interioară." -> "A masterpiece of Brâncovenesc style, famous for its interior courtyard."
            "Fostul Palat Regal găzduiește colecții remarcabile de artă românească." -> "The former Royal Palace, hosting remarkable collections of Romanian art."
            "Oaze de verdeață, sere exotice tropicale și mii de specii de plante în Cotroceni." -> "A green oasis with exotic tropical greenhouses and thousands of plant species in Cotroceni."
            "Parc istoric frumos cu Mausoleul impunător și fântâni elegante." -> "Beautiful historical park with a majestic Mausoleum and elegant fountains."
            "Fostul palat luxos de protocol al soților Nicolae și Elena Ceaușescu." -> "The former luxurious private residence of Nicolae and Elena Ceaușescu."
            "Piața istorică centrală cu Memorialul Renașterii și clădiri celebre." -> "The central historical square with the Memorial of Rebirth."
            "Cel mai vechi han funcțional din Europa, oferind o ambianță tradițională excelentă." -> "The oldest active inn in Europe, offering an excellent traditional Romanian vibe."
            "Exponate arheologice și istorice inestimabile, incluzând Tezaurul istoric național." -> "Invaluable archaeological and historical exhibits, including the national Treasury."
            "Reședința oficială a Președintelui și un muzeu istoric de o rară frumusețe." -> "The official Presidential residence and a historic museum of rare beauty."
            "Un parc modern imens cu lac de agrement, piste și un ambient extrem de relaxant." -> "A massive modern park with a recreational lake, tracks, and relaxing ambiance."
            "Cea mai mare catedrală ortodoxă din lume, o structură arhitecturală colosală." -> "The largest Orthodox Cathedral in the world, a colossal architectural masterwork."
            "Cunoscut și ca Parcul Moghioroș, revitalizat cu poduri cochete și sere moderne." -> "Also known as Moghioroș Park, revitalized with chic bridges and modern greenhouses."
            "O clădire istorică în stil brâncovenesc deosebit, situată în exteriorul orașului." -> "A beautiful brâncovenesc-style castle situated just outside the city."
            "Situat în aripa din spate a Palatului Parlamentului, cu expoziții avangardiste." -> "Located in the back wing of the Palace of the Parliament, featuring avant-garde exhibitions."
            "Kilometrul zero al democrației bucureștene, încadrat de clădiri universitare emblematice." -> "The landmark of Romanian democracy, surrounded by iconic university buildings."
            "Clădire istorică neoclasică, faimos centru de cultura pentru spectacole lirice și balet." -> "A historic neoclassical building, a famous cultural venue for opera and ballet."

            "Oază magnifică de verdeață ce adăpostește plante rare și o grădină japoneză." -> "A magnificent green oasis sheltering rare plants and a Japanese garden."
            "Parcul istoric central cu un lac superb de plimbări cu barca și Casino." -> "The historic central park with a boating lake and the Casino building."
            "Piața istorică principală delimitată de monumentala catedrală gotică." -> "The main historical square dominated by the monumental Gothic Cathedral."
            "Catedrală ortodoxă impunătoare și piațetă cu fântâni arteziene animate." -> "An imposing Orthodox Cathedral and public square with animated artesian fountains."
            "O panoramă spectaculaosă a întregului oraș, ideală la apus de soare." -> "A spectacular panoramic view of the entire city, ideal at sunset."
            "O panoramă spectaculoasă a întregului oraș, ideală la apus de soare." -> "A spectacular panoramic view of the entire city, ideal at sunset."
            "Palat baroc splendid ce găzduiește colecții naționale valoroase de artă." -> "A splendid baroque palace hosting valuable national art collections."
            "Primul muzeu în aer liber din România cu gospodării tradiționale transilvănene." -> "Romania's first open-air museum featuring historic Transylvanian homesteads."
            "Unul dintre puțele turnuri de apărare care s-au păstrat intacte din vechea cetate." -> "One of the few defensive towers preserved intact from the old citadel."
            "Zonă modernă de recreere în jurul lacului, plină de spații verzi și pontoane." -> "A modern lakeside recreation area filled with green spaces and boardwalks."
            "Cea mai veche piață din Cluj-Napoca, flancată de Biserica Franciscană." -> "The oldest square in Cluj-Napoca, flanked by the elegant Franciscan Church."
            "Pădurea faimoasă la nivel mondial pentru peisajele sale misterioase și legende." -> "The world-famous forest known for its mysterious landscapes and legends."
            "O zonă modernă vibrantă, îmbinând arhitectura de birouri cu malul Someșului." -> "A vibrant modern area combining office development with the Someș riverfront."
            "Clădire neobarocă superbă destinată spectacolelor lirice și teatrale." -> "A superb neo-baroque building designed for opera and theatrical performances."
            "Turn istoric reabilitat recent cu o platformă panoramică superbă." -> "A newly rehabilitated historical tower with a magnificent panoramic platform."
            "Zonă adiacentă cetățuii cu alei umbroase, spații de joacă și belvedere retras." -> "Area near Cetățuia with shaded alleys, playgrounds, and cozy viewpoints."
            "Colecții arheologice valoroase despre istoria antică, romană și medievală a Transilvaniei." -> "Valuable archaeological collections about the ancient, Roman, and medieval history of Transylvania."
            "O clădire monument istoric gotic de tip sală, una dintre cele mai vaste din Europa de Est." -> "A historic monumental Gothic hall church, one of the largest in Eastern Europe."
            "Cel mai modern stadion multifuncțional din inima Transilvaniei, cu o arhitectură high-tech." -> "The most modern multi-use stadium in the heart of Transylvania, featuring high-tech architecture."
            "Cea mai mare sală polivalentă din România, găzduiește concerte mari și evenimente sportive." -> "The largest multi-purpose arena in Romania, hosting major concerts and sports events."
            "Parc renumit pentru sutele de soiuri de trandafiri și faleza liniștită pe malul Someșului." -> "A park famous for hundreds of rose varieties and a peaceful Someș riverfront path."
            "O veche mănăstire benedictină fortificată, fiind una dintre cele mai bătrâne biserici din Cluj." -> "An ancient fortified Benedictine monastery, one of the oldest standing churches in Cluj."
            "Catedrală monumentală cu design modern magnific, aflată în Piața Cipariu." -> "A monumental cathedral with a magnificent modern design, located in Cipariu Square."
            "Situat în campusul USAMV, ideal pentru explorarea stelelor și activități educaționale." -> "Located on the USAMV campus, ideal for star-gazing and educational outreach."
            "Grădini, livezi istorice și oază verde extinsă în una dintre faimoasele universități clujene." -> "Gardens, orchards, and an expansive green sanctuary inside USAMV University."
            "Loc istoric plin de mister situat pe deal, înconjurat de pădure, ideal pentru drumeții." -> "A mysterious historical site situated on a hill, surrounded by forest, ideal for hiking."

            "Cea mai mare biserică gotică din sud-estul Europei." -> "The largest Gothic church in Southeastern Europe."
            "Piața istorică principală din Brașov, plină de farmec și cafenele." -> "The main historical square in Brașov, filled with charm and cozy cafes."
            "Telecabina spre muntele Tâmpa cu panoramă excelentă a orașului." -> "The cable car climbing Tâmpa Mountain, offering scenic panoramic city views."
            "Turn istoric de apărare oferind o vedere spectaculoasă la înălțime." -> "A historic defense tower providing spectacular aerial views from the hill."
            "Poartă barocă superbă ce duce spre vechiul cartier românesc." -> "A beautiful baroque gate leading into the historic Romanian quarter Schei."
            "Turn de strajă din secolul al XV-lea cu vedere panoramică spre Biserica Neagră." -> "A 15th-century defense tower with panoramic views looking towards the Black Church."
            "Unul dintre cele mai bine conservate bastioane medievale, adăpostind o machetă rară." -> "One of the best-preserved medieval bastions, hosting a rare scale model."
            "Una dintre cele mai înguste străzi din Europa, un reper fotografic iconic." -> "One of the narrowest alleys in Europe, a truly iconic photo landmark."
            "Situată în Șchei, locul unde s-au tipărit primele cărți în limba română." -> "Located in Șchei, the historic cradle where the first Romanian books were printed."
            "Singura poartă medievală de acces în cetate păstrată în forma sa originală." -> "The only medieval city entry gate surviving fully in its original form."
            "Un park mare și liniștit în centrul orașului cu alei largi și fântâni." -> "A large and tranquil park in the city center with wide paths and fountains."
            "Bastion fortificat pitoresc deasupra pârâului Graft, legat de Turnul Alb." -> "A picturesque fortified bastion over Graft creek, linked physically to the White Tower."
            "Expoziție de pictură și sculptură românească valoroasă, aproape de primărie." -> "A rich collection of valuable Romanian paintings and sculptures near City Hall."
            "Zonă naturală de chei spectaculoase cu spații verzi pentru recreere." -> "A spectacular natural gorge area with lush green spaces for picnics."
            "Fortăreață istorică pe dealul Strajă, monument istoric de importanță națională." -> "A hilltop citadel on Strajă hill, a national historical heritage site."
            "Turn vechi de apărare din secolul al XV-lea, parte integrantă din fortificații." -> "An ancient 15th-century defense tower, integral to the old town walls."
            "Bastion istoric ridicat pe latura de sud a cetății sub muntele Tâmpa." -> "A historic bastion erected on the southern walls right under Tâmpa Mountain."
            "O clădire religioasă splendidă în stil bizantin, cu detalii decorative fermecătoare." -> "A splendid Religious monument designed in Byzantine style with charming decor."
            "Simbolul central al orașului Brașov, fostul sediu administrativ medieval." -> "The central symbol of Brașov, formerly the medieval administrative headquarters."
            "O biserică orthodoxă impunătoare din Șchei, fondată în secolul al XIII-lea." -> "An imposing Orthodox Church in Șchei, with foundations from the 13th century."
            "Aleea pietonală umbroasă Tiberiu Brediceanu, perfectă pentru plimbări relaxante pe sub pădure." -> "The shaded pedestrian alley under Tâmpa, perfect for relaxing forest walks."
            "Turnul Lemnarilor" -> "The Woodworkers' Tower"
            "Turnul Lemnarilor, găzduiește expoziții de sculptură și ateliere de artă." -> "The Woodworkers' Tower, showcasing woodcarving exhibits and art workshops."
            "Turn istoric restaurat cochet, găzduiește expoziții de sculptură și ateliere de artă." -> "A beautifully restored historic tower, hosting sculpture exhibits and art workshops."
            "Explorare pe străduțele vechi și întortocheate, inima spiritului românesc brașovean." -> "An exploration of winding old cobblestone streets, the cradle of Romanian heritage."
            "Una dintre cele mai moderne grădini zoologice din țară, amplasată în pădurea Noua." -> "One of the country's most modern zoos, nestling beautifully in Noua Forest."
            "Zonă superbă de relaxare cu bărci, pontoane, terenuri de sport și un aer minunat de munte." -> "A stellar lakeside park with rental boats, sports grounds, and pure mountain air."
            else -> description
        }
    }

    fun addTestingLog(placeName: String, note: String, observerName: String = "Testor Teren") {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val log = TestingLog(
                    city = _selectedCity.value,
                    placeName = placeName,
                    note = note,
                    observerName = observerName
                )
                testingLogDao.insertLog(log)
            }
        }
    }

    fun deleteTestingLog(logId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                testingLogDao.deleteLogById(logId)
            }
        }
    }

    fun clearAllTestingLogsCurrentCity() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                testingLogDao.clearLogsByCity(_selectedCity.value)
            }
        }
    }

    fun factoryResetData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                spotDao.deleteAllCustomSpots()
                itineraryDao.deleteAllItineraries()
                testingLogDao.deleteAllLogs()
            }
        }
    }
}
