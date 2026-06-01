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
}
