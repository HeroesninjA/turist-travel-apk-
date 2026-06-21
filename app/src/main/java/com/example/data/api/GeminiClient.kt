package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiRepository {
    suspend fun generateTravelItinerary(
        city: String,
        attractions: List<String>,
        busLines: List<String>,
        startSpot: String,
        isEnglish: Boolean = false,
        weatherInfoText: String = ""
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return if (isEnglish) {
                "Hint: The GEMINI_API_KEY has not been configured in the AI Studio Secrets panel. Please configure the API Key for real AI recommendations!"
            } else {
                "Indiciu: Nu s-a configurat cheia GEMINI_API_KEY în panoul de Secrete din AI Studio. Te rugăm să configurezi API Key pentru recomandări bazate pe AI real!"
            }
        }

        val attractionsListString = attractions.joinToString("\n- ")
        val busLinesListString = busLines.joinToString("\n- ")

        val prompt = if (isEnglish) {
            """
                I am in the city of $city. My starting point is: $startSpot.
                ${if (weatherInfoText.isNotEmpty()) "The current weather condition is: $weatherInfoText." else ""}
                I want to visit the following tourist attractions:
                - $attractionsListString
                
                The following bus/transit lines are available with stations nearby:
                - $busLinesListString
                
                Please compile a chronologically optimized itinerary to save time. Tell me exactly which buses to take from one station to another, the optimal sequence of attractions to minimize transit time, estimated hours, approximate ticket costs, and local insider tips for visiting. Take the current weather conditions into account in your advice if possible! Respond elegantly in Markdown format in English, using an enthusiastic and helpful tone!
            """.trimIndent()
        } else {
            """
                Sunt în orașul $city. Punctul meu de pornire este: $startSpot.
                ${if (weatherInfoText.isNotEmpty()) "Vremea curentă în oraș este: $weatherInfoText." else ""}
                Vreau să vizitez următoarele puncte turistice:
                - $attractionsListString
                
                În oraș avem următoarele linii de autobuz disponibile cu stații în apropiere:
                - $busLinesListString
                
                Te rog să compilezi un itinerar optimizat cronologic, salvând timp. Spune-mi exact ce autobuze să iau dintr-o stație în alta, ordinea optimă a atracțiilor pentru a reduce timpul de deplasare, ore estimate, costuri aproximative de bilet și secrete locale pentru vizitare. Include sfaturi specifice bazate pe condițiile meteo curente dacă este relevant! Răspunde elegant în format Markdown în limba Română, folosind un ton entuziast și util!
            """.trimIndent()
        }

        val systemInstruction = if (isEnglish) {
            "You are an elite tourist guide expert in Romanian transit networks. Help compile optimized itineraries using city public transport. Answer strictly in English."
        } else {
            "Ești un ghid turistic de elită din România, specializat în optimizarea călătoriilor cu transportul în comun din orașele românești. Răspunde strict în limba Română."
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: (if (isEnglish) "Could not obtain the AI itinerary." else "Nu s-a putut obține itinerariul AI.")
        } catch (e: Exception) {
            if (isEnglish) {
                "Error calling Gemini API: ${e.localizedMessage}. Check internet connection or API key!"
            } else {
                "Eroare la apelarea Gemini API: ${e.localizedMessage}. Verifică conexiunea la internet sau cheia API!"
            }
        }
    }

    suspend fun autoConfigureNewCity(
        city: String,
        isEnglish: Boolean = false
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                if (isEnglish) "GEMINI_API_KEY has not been configured in Secrets."
                else "GEMINI_API_KEY nu este configurat în Secrete."
            )
        }

        val prompt = """
            We want to automatically index and configure a new city named: "$city".
            You MUST return a valid JSON object. Do not include any text before or after the JSON.
            Ensure the coordinates are real-world, accurate geographical locations for this city.
            
            The JSON object must have exactly the following structure:
            {
              "cityName": "$city",
              "centerLatitude": 45.1234, // real decimal latitude of this city's center
              "centerLongitude": 25.1234, // real decimal longitude of this city's center
              "spots": [
                {
                  "name": "Landmark Name 1",
                  "description": "Short description of landmark 1",
                  "latitude": 45.1235,
                  "longitude": 25.1235,
                  "duration": 90
                }
                // provide 4 to 6 major attractions
              ],
              "stations": [
                {
                  "id": "STAT_1",
                  "name": "Station Name 1",
                  "latitude": 45.1230,
                  "longitude": 25.1234
                }
                // provide 4 to 5 transit stations close to the spots above
              ],
              "lines": [
                {
                  "name": "L1",
                  "color": "#10B981",
                  "type": "BUS",
                  "stationIds": ["STAT_1", "STAT_2"]
                }
                // provide 1 to 2 transit lines connecting the stations
              ],
              "weatherAdviceEn": "Sunny advice...",
              "weatherAdviceRo": "Sfaturi meteo...",
              "tempCelsius": 22
            }
            
            Provide only valid JSON code. No backticks (```json), no conversational filler.
        """.trimIndent()

        val systemInstruction = "You are a professional geo-mapping, public transit and tourism expert who outputs ONLY strict, valid raw JSON objects matching schemas."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        var text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw IllegalStateException("Empty response from AI")
        
        // Clean markdown block if present
        text = text.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```").substringBeforeLast("```").trim()
        }
        return text
    }

    suspend fun searchAndGenerateLandmark(
        city: String,
        query: String,
        isEnglish: Boolean = false
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                if (isEnglish) "GEMINI_API_KEY has not been configured in Secrets."
                else "GEMINI_API_KEY nu este configurat în Secrete."
            )
        }

        val prompt = if (isEnglish) {
            """
                We want to find and automatically configure a specific tourist landmark/attraction matching the query "$query" in the city of "$city".
                You MUST return a valid JSON object. Do not include any text before or after the JSON.
                Ensure the coordinates are real-world, accurate geographical locations for this landmark in this city.
                
                The JSON object must have exactly the following structure:
                {
                  "name": "Landmark Name", // complete official name of the attraction
                  "description": "Short, highly informative description of this landmark",
                  "latitude": 45.1235, // real decimal latitude coordinates
                  "longitude": 25.1235, // real decimal longitude coordinates
                  "duration": 90 // estimated optimal visit duration in minutes (integer)
                }
                
                Provide only valid JSON code. No backticks (```json), no conversational filler.
            """.trimIndent()
        } else {
            """
                Vrem să găsim și să configurăm automat un obiectiv/atracție turistică specifică ce corespunde căutării "$query" în orașul "$city".
                Trebuie să returnezi un obiect JSON valid. Nu include text înainte sau după JSON.
                Asigură-te că coordonatele sunt reale și precise pentru acest obiectiv în acest de oraș.
                
                Obiectul JSON trebuie să aibă exact următoarea structură:
                {
                  "name": "Nume Obiectiv Turistic", // numele oficial complet al atracției
                  "description": "Descriere scurtă și informativă a obiectivului",
                  "latitude": 45.1235, // coordonate reale latitudine (decimal)
                  "longitude": 25.1235, // coordonate reale longitudine (decimal)
                  "duration": 90 // timp estimat optim de vizită în minute (întreg)
                }
                
                Furnizează doar cod JSON valid. Fără delimitatori de formatare markdown (lucru precum ```json) sau text adițional explicativ.
            """.trimIndent()
        }

        val systemInstruction = "You are an expert tour guide and geographer specializing in Romanian cities and sights. Your output is ONLY a strict, valid raw JSON object matching the requested schema."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        var text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw IllegalStateException("Empty response from AI")
        
        // Clean markdown block if present
        text = text.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```").substringBeforeLast("```").trim()
        }
        return text
    }
}
