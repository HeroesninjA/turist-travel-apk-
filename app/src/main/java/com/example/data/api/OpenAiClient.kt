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
data class OpenAiMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenAiMessage>,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiMessageResponse(
    @Json(name = "role") val role: String?,
    @Json(name = "content") val content: String?
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    @Json(name = "message") val message: OpenAiMessageResponse?
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    @Json(name = "choices") val choices: List<OpenAiChoice>?
)

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun generateChat(
        @Header("Authorization") authorization: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

object OpenAiRetrofitClient {
    private const val BASE_URL = "https://api.openai.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: OpenAiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(OpenAiApiService::class.java)
    }
}

object OpenAiRepository {
    suspend fun generateTravelItinerary(
        apiKey: String,
        city: String,
        attractions: List<String>,
        busLines: List<String>,
        startSpot: String,
        isEnglish: Boolean = false,
        weatherInfoText: String = ""
    ): String {
        if (apiKey.trim().isEmpty()) {
            return if (isEnglish) {
                "Hint: The OpenAI API Key is empty. Please configure it in your Settings panel!"
            } else {
                "Indiciu: Cheia API OpenAI este goală. Te rugăm să o configurezi în panoul de Setări!"
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

        val messages = listOf(
            OpenAiMessage(role = "system", content = systemInstruction),
            OpenAiMessage(role = "user", content = prompt)
        )

        val request = OpenAiChatRequest(
            model = "gpt-4o-mini",
            messages = messages,
            temperature = 0.2f
        )

        return try {
            val authHeader = "Bearer ${apiKey.trim()}"
            val response = OpenAiRetrofitClient.service.generateChat(authHeader, request)
            response.choices?.firstOrNull()?.message?.content
                ?: (if (isEnglish) "Could not obtain the OpenAI itinerary." else "Nu s-a putut obține itinerariul OpenAI.")
        } catch (e: Exception) {
            if (isEnglish) {
                "Error calling OpenAI API: ${e.localizedMessage}. Check internet connection or API key!"
            } else {
                "Eroare la apelarea OpenAI API: ${e.localizedMessage}. Verifică conexiunea la internet sau cheia API!"
            }
        }
    }
}
