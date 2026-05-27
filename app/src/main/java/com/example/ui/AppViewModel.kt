package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SavedItinerary
import com.example.data.TouristSpot
import com.example.data.api.GeminiRepository
import com.example.domain.OptimizedJourney
import com.example.domain.TransitNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val spotDao = database.touristSpotDao()
    private val itineraryDao = database.savedItineraryDao()

    // Screen States
    private val _selectedCity = MutableStateFlow("București")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    // Selected starting point (Gara Centrală/Nord or Hotel)
    private val _customStartSpot = MutableStateFlow<TouristSpot?>(null)
    val customStartSpot: StateFlow<TouristSpot?> = _customStartSpot.asStateFlow()

    // All spots from Database
    val allSpots: StateFlow<List<TouristSpot>> = spotDao.getAllSpotsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered attractions based on selectedCity
    val citySpots: StateFlow<List<TouristSpot>> = combine(allSpots, selectedCity) { spots, city ->
        spots.filter { it.city == city }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Itineraries
    val savedItineraries: StateFlow<List<SavedItinerary>> = itineraryDao.getAllSavedItinerariesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transit journey calculated locally
    val optimizedJourney: StateFlow<OptimizedJourney?> = combine(citySpots, selectedCity, customStartSpot) { spots, city, customStart ->
        val start = customStart ?: TransitNetwork.getStartSpot(city)
        val selectedSpots = spots.filter { it.isSelected }
        if (selectedSpots.isNotEmpty()) {
            TransitNetwork.optimizePath(start, selectedSpots, city)
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

    init {
        // Pre-populate database with default cities values if empty
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (spotDao.getSpotsByCity("București").isEmpty()) {
                    spotDao.insertSpots(TransitNetwork.BUCURESTI_PRESETS)
                }
                if (spotDao.getSpotsByCity("Cluj-Napoca").isEmpty()) {
                    spotDao.insertSpots(TransitNetwork.CLUJ_PRESETS)
                }
                if (spotDao.getSpotsByCity("Brașov").isEmpty()) {
                    spotDao.insertSpots(TransitNetwork.BRASOV_PRESETS)
                }
            }
        }
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
        _customStartSpot.value = null // reset custom start spot for the new city
        _aiRecommendation.value = ""
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

    fun clearAllCustomCurrentCity() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                spotDao.clearCustomSpotsByCity(_selectedCity.value)
            }
        }
    }

    fun askGeminiForItinerary() {
        val journey = optimizedJourney.value ?: return
        val currentCityName = _selectedCity.value
        val attractions = journey.orderedSpots.map { "${it.name} (${it.description})" }
        val startLocName = _customStartSpot.value?.name ?: TransitNetwork.getStartSpot(currentCityName).name
        
        val lines = TransitNetwork.getLinesForCity(currentCityName).map { line ->
            "${line.name} [Stații: ${line.stations.joinToString(" ➔ ") { it.name }}]"
        }

        viewModelScope.launch {
            _isAiLoading.value = true
            _aiRecommendation.value = "Se generează optimizarea AI cu Gemini 3.5 Flash... Vă rugăm să așteptați..."
            
            try {
                val recommendation = withContext(Dispatchers.IO) {
                    GeminiRepository.generateTravelItinerary(
                        city = currentCityName,
                        attractions = attractions,
                        busLines = lines,
                        startSpot = startLocName
                    )
                }
                _aiRecommendation.value = recommendation
            } catch (e: Exception) {
                _aiRecommendation.value = "Nu s-a putut apela AI-ul: ${e.localizedMessage}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
