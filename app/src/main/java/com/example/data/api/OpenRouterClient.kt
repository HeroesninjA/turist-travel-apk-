package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenRouterMessage>,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterMessageResponse(
    @Json(name = "role") val role: String?,
    @Json(name = "content") val content: String?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessageResponse?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    @Json(name = "choices") val choices: List<OpenRouterChoice>?
)

interface OpenRouterApiService {
    @POST("v1/chat/completions")
    suspend fun generateChat(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}

object OpenRouterRetrofitClient {
    private const val BASE_URL = "https://openrouter.ai/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: OpenRouterApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(OpenRouterApiService::class.java)
    }
}

object OpenRouterRepository {
    suspend fun generateTravelItinerary(
        apiKey: String,
        model: String,
        city: String,
        attractions: List<String>,
        busLines: List<String>,
        startSpot: String,
        isEnglish: Boolean = false,
        weatherInfoText: String = ""
    ): String {
        // Even if API key is blank, some openrouter models or endpoints allow free/keyless, but normally openrouter requires a key.
        // We will provide a constructive message if empty, but OpenRouter allows some free usage sometimes.
        val resolvedKey = apiKey.trim()
        val authHeader = if (resolvedKey.isEmpty()) {
            return if (isEnglish) {
                "Hint: The OpenRouter API Key is empty. Please configure it in your Settings panel!"
            } else {
                "Indiciu: Cheia API OpenRouter este goală. Te rugăm să o configurezi în panoul de Setări!"
            }
        } else {
            "Bearer $resolvedKey"
        }

        val resolvedModel = if (model.trim().isEmpty()) "meta-llama/llama-3-8b-instruct:free" else model.trim()

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

        val messages = listOf(
            OpenRouterMessage(role = "system", content = systemInstruction),
            OpenRouterMessage(role = "user", content = prompt)
        )

        val request = OpenRouterChatRequest(
            model = resolvedModel,
            messages = messages,
            temperature = 0.2f
        )

        return try {
            val response = OpenRouterRetrofitClient.service.generateChat(
                authorization = authHeader,
                referer = "https://ai.studio/build",
                title = "AI Studio Bus Tour Planner",
                request = request
            )
            response.choices?.firstOrNull()?.message?.content
                ?: (if (isEnglish) "Could not obtain the OpenRouter itinerary." else "Nu s-a putut obține itinerariul OpenRouter.")
        } catch (e: Exception) {
            if (isEnglish) {
                "Error calling OpenRouter API: ${e.localizedMessage}. Check internet connection, model ID, or API key!"
            } else {
                "Eroare la apelarea OpenRouter API: ${e.localizedMessage}. Verifică conexiunea la internet, ID-ul modelului sau cheia API!"
            }
        }
    }

    suspend fun autoConfigureNewCity(
        apiKey: String,
        model: String,
        city: String,
        isEnglish: Boolean = false
    ): String {
        val resolvedKey = apiKey.trim()
        if (resolvedKey.isEmpty()) {
            throw IllegalStateException(
                if (isEnglish) "OpenRouter API Key has not been configured in Settings."
                else "Cheia API OpenRouter nu este configurată în Setări."
            )
        }
        val authHeader = "Bearer $resolvedKey"
        val resolvedModel = if (model.trim().isEmpty()) "meta-llama/llama-3-8b-instruct:free" else model.trim()

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

        val messages = listOf(
            OpenRouterMessage(role = "system", content = systemInstruction),
            OpenRouterMessage(role = "user", content = prompt)
        )

        val request = OpenRouterChatRequest(
            model = resolvedModel,
            messages = messages,
            temperature = 0.2f
        )

        val response = OpenRouterRetrofitClient.service.generateChat(
            authorization = authHeader,
            referer = "https://ai.studio/build",
            title = "AI Studio Bus Tour Planner",
            request = request
        )
        var text = response.choices?.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from AI")

        text = text.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```").substringBeforeLast("```").trim()
        }
        return text
    }

    suspend fun searchAndGenerateLandmark(
        apiKey: String,
        model: String,
        city: String,
        query: String,
        isEnglish: Boolean = false
    ): String {
        val resolvedKey = apiKey.trim()
        if (resolvedKey.isEmpty()) {
            throw IllegalStateException(
                if (isEnglish) "OpenRouter API Key has not been configured in Settings."
                else "Cheia API OpenRouter nu este configurată în Setări."
            )
        }
        val authHeader = "Bearer $resolvedKey"
        val resolvedModel = if (model.trim().isEmpty()) "meta-llama/llama-3-8b-instruct:free" else model.trim()

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

        val messages = listOf(
            OpenRouterMessage(role = "system", content = systemInstruction),
            OpenRouterMessage(role = "user", content = prompt)
        )

        val request = OpenRouterChatRequest(
            model = resolvedModel,
            messages = messages,
            temperature = 0.2f
        )

        val response = OpenRouterRetrofitClient.service.generateChat(
            authorization = authHeader,
            referer = "https://ai.studio/build",
            title = "AI Studio Bus Tour Planner",
            request = request
        )
        var text = response.choices?.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from AI")

        text = text.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```").substringBeforeLast("```").trim()
        }
        return text
    }
}
