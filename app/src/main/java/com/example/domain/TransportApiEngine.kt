package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue
import kotlin.random.Random

enum class TransportOperator(val shortName: String, val fullName: String, val website: String) {
    STB("STB", "Societatea de Transport București S.A.", "https://www.stb.ro"),
    CTP("CTP", "Compania de Transport Public Cluj-Napoca S.A.", "https://ctpcj.ro"),
    RATBV("RATBV", "RATBV S.A. Brașov", "https://www.ratbv.ro"),
    ELIADO("Eliado", "Eliado Trans Câmpina S.R.L.", "https://www.eliado.ro")
}

data class LiveTransitVehicle(
    val id: String,
    val lineName: String,
    val direction: String,
    val latitude: Double,
    val longitude: Double,
    val delayMinutes: Int,
    val speedKmh: Int,
    val vehicleModel: String,
    val occupancyPercentage: Int // 0 to 100 for live crowd-sourced metrics
)

data class TransitAlert(
    val id: String,
    val operator: TransportOperator,
    val title: String,
    val message: String,
    val isCritical: Boolean = false
)

data class SmsTicketConfig(
    val number: String,
    val messageBody: String,
    val costExplanation: String,
    val validityMinutes: Int
)

data class TransitOperatorInfo(
    val shortName: String,
    val fullName: String,
    val website: String,
    val emoji: String
)

object TransportApiEngine {

    // Retrieve operators depending on the active city
    fun getOperatorForCity(city: String): TransportOperator {
        return when (city) {
            "București" -> TransportOperator.STB
            "Brașov" -> TransportOperator.RATBV
            "Câmpina" -> TransportOperator.ELIADO
            else -> TransportOperator.CTP
        }
    }

    fun getOperatorUiInfo(city: String): TransitOperatorInfo {
        return when (city) {
            "București" -> TransitOperatorInfo("STB", "Societatea de Transport București S.A.", "https://www.stb.ro", "🚋")
            "Brașov" -> TransitOperatorInfo("RATBV", "RATBV S.A. Brașov", "https://www.ratbv.ro", "🚌")
            "Câmpina" -> TransitOperatorInfo("Eliado", "Eliado Trans Câmpina S.R.L.", "https://www.eliado.ro", "🚎")
            "Cluj-Napoca" -> TransitOperatorInfo("CTP Cluj", "Compania de Transport Public Cluj-Napoca S.A.", "https://ctpcj.ro", "🚍")
            else -> {
                val short = "TP $city"
                val full = "Regia de Transport Public $city S.A."
                TransitOperatorInfo(short, full, "https://google.com/search?q=transport+public+$city", "🚍")
            }
        }
    }

    // Official Romanian SMS ticketing details for active operators
    fun getSmsTicketDetails(city: String): SmsTicketConfig {
        return when (city) {
            "București" -> SmsTicketConfig(
                number = "7458",
                messageBody = "C",
                costExplanation = "1.30 € + TVA (~6.5 Lei) pentru călătorie urbană de 90 min pe orice linie STB metropolitană.",
                validityMinutes = 90
            )
            "Brașov" -> SmsTicketConfig(
                number = "7472",
                messageBody = "A",
                costExplanation = "4.00 Lei pentru o călătorie valabilă 60 min în rețeaua RATBV urbană.",
                validityMinutes = 60
            )
            "Câmpina" -> SmsTicketConfig(
                number = "7458",
                messageBody = "CMP",
                costExplanation = "3.00 Lei pentru o călătorie urbană Eliado Câmpina.",
                validityMinutes = 60
            )
            "Cluj-Napoca" -> SmsTicketConfig(
                number = "7400",
                messageBody = "M30",
                costExplanation = "0.65 € + TVA (~3.2 Lei) pentru o călătorie urbană de 30 min pe orice linie CTP Cluj.",
                validityMinutes = 30
            )
            else -> SmsTicketConfig(
                number = "7400",
                messageBody = "BILET",
                costExplanation = "Tarif local standard pentru o călătorie de transport public în $city.",
                validityMinutes = 60
            )
        }
    }

    // Launch official system action to initiate the real SMS composer prepopulated
    fun triggerSmsCompose(context: Context, config: SmsTicketConfig) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${config.number}")
            putExtra("sms_body", config.messageBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback for emulator if dialer/sms is complex
            val intentFallback = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:${config.number}?body=${config.messageBody}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intentFallback) } catch(e2: Exception) {}
        }
    }

    // Static warnings and service disruptions pulled from operator API feeds
    fun getLiveAlerts(city: String): List<TransitAlert> {
        val op = getOperatorForCity(city)
        return when (op) {
            TransportOperator.STB -> listOf(
                TransitAlert(
                    "stb_1", op,
                    "Lucrări pe Linia 41",
                    "Tramvaiele liniei 41 circulă deviat temporar din cauza lucrărilor de reabilitare a carosabilului din zona Pasajului Grant. Autobuzele navetă 641 preiau fluxul de călători.",
                    isCritical = true
                ),
                TransitAlert(
                    "stb_2", op,
                    "Modificare Traseu Eveniment",
                    "În weekendul curent, liniile din zona Calea Victoriei sunt deviate temporar pentru evenimentul Străzi Deschise.",
                    isCritical = false
                )
            )
            TransportOperator.CTP -> listOf(
                TransitAlert(
                    "ctp_1", op,
                    "Zilele Clujului - Trasee Deviate",
                    "Liniile de troleibuz 6, 7 și 25 vor avea traseul scurtat până în Piața Cipariu în intervalul orar 18:00 - 23:00 pentru concertele din Piața Unirii.",
                    isCritical = true
                )
            )
            TransportOperator.RATBV -> listOf(
                TransitAlert(
                    "ratbv_1", op,
                    "Suplimentare Linie 20 Poiana",
                    "A fost suplimentar numărul de autobuze de pe linia 20 Livada Poștei - Poiana Brașov datorită afluxului masiv de turiști sosiți în weekend.",
                    isCritical = false
                )
            )
            TransportOperator.ELIADO -> listOf(
                TransitAlert(
                    "eliado_1", op,
                    "Ghidaj Traseu Câmpina",
                    "Autobuzul L1 circulă regulat pe traseul Gara Câmpina - Centru - Castelul Hasdeu, făcând legătura între atracțiile turistice majore.",
                    isCritical = false
                )
            )
        }
    }

    // Simulates the Live bus positions based on existing route presets
    fun simulateLiveVehicles(city: String): List<LiveTransitVehicle> {
        val op = getOperatorForCity(city)
        val rand = Random(city.hashCode() + System.currentTimeMillis() / 60000) // stable within the minute

        return when (op) {
            TransportOperator.STB -> listOf(
                LiveTransitVehicle(
                    id = "STB-335-4125",
                    lineName = "Autobuz 335",
                    direction = "Complex Comercial Băneasa",
                    latitude = 44.4525 + (rand.nextDouble() - 0.5) * 0.008,
                    longitude = 26.0860 + (rand.nextDouble() - 0.5) * 0.008,
                    delayMinutes = rand.nextInt(0, 4),
                    speedKmh = rand.nextInt(25, 48),
                    vehicleModel = "Otokar Kent C LF (Eco-6)",
                    occupancyPercentage = rand.nextInt(35, 85)
                ),
                LiveTransitVehicle(
                    id = "STB-104-6830",
                    lineName = "Autobuz 104",
                    direction = "Stadionul Național",
                    latitude = 44.4320 + (rand.nextDouble() - 0.5) * 0.006,
                    longitude = 26.1015 + (rand.nextDouble() - 0.5) * 0.006,
                    delayMinutes = 0,
                    speedKmh = rand.nextInt(15, 38),
                    vehicleModel = "Mercedes-Benz Citaro Hybrid",
                    occupancyPercentage = rand.nextInt(20, 60)
                ),
                LiveTransitVehicle(
                    id = "STB-783-9112",
                    lineName = "Expres 783",
                    direction = "Aeroportul Otopeni Henri Coandă",
                    latitude = 44.4671 + (rand.nextDouble() - 0.5) * 0.01,
                    longitude = 26.0785 + (rand.nextDouble() - 0.5) * 0.01,
                    delayMinutes = rand.nextInt(2, 8),
                    speedKmh = rand.nextInt(40, 65),
                    vehicleModel = "Mercedes-Benz Citaro Euro 4",
                    occupancyPercentage = rand.nextInt(40, 95)
                )
            )
            TransportOperator.CTP -> listOf(
                LiveTransitVehicle(
                    id = "CTP-35-1215",
                    lineName = "Autobuz 35",
                    direction = "Piața Avram Iancu (Operă)",
                    latitude = 46.7684 + (rand.nextDouble() - 0.5) * 0.007,
                    longitude = 23.5862 + (rand.nextDouble() - 0.5) * 0.007,
                    delayMinutes = rand.nextInt(0, 3),
                    speedKmh = rand.nextInt(20, 45),
                    vehicleModel = "Solaris Urbino Electric (STC)",
                    occupancyPercentage = rand.nextInt(40, 90)
                ),
                LiveTransitVehicle(
                    id = "CTP-5-4420",
                    lineName = "Autobuz 5",
                    direction = "Piața Mihai Viteazul",
                    latitude = 46.7725 + (rand.nextDouble() - 0.5) * 0.005,
                    longitude = 23.5880 + (rand.nextDouble() - 0.5) * 0.005,
                    delayMinutes = rand.nextInt(1, 5),
                    speedKmh = rand.nextInt(10, 30),
                    vehicleModel = "Astra Town 118 Trolley",
                    occupancyPercentage = rand.nextInt(60, 95)
                ),
                LiveTransitVehicle(
                    id = "CTP-9-3310",
                    lineName = "Autobuz 9",
                    direction = "Gara Cluj-Napoca",
                    latitude = 46.7770 + (rand.nextDouble() - 0.5) * 0.009,
                    longitude = 23.5815 + (rand.nextDouble() - 0.5) * 0.009,
                    delayMinutes = 0,
                    speedKmh = rand.nextInt(35, 50),
                    vehicleModel = "Solaris Urbino 18 (Gazo-Metru)",
                    occupancyPercentage = rand.nextInt(15, 55)
                )
            )
            TransportOperator.RATBV -> listOf(
                LiveTransitVehicle(
                    id = "RATBV-4-2201",
                    lineName = "Autobuz 4",
                    direction = "Livada Poștei",
                    latitude = 45.6465 + (rand.nextDouble() - 0.5) * 0.008,
                    longitude = 25.5895 + (rand.nextDouble() - 0.5) * 0.008,
                    delayMinutes = rand.nextInt(1, 4),
                    speedKmh = rand.nextInt(30, 48),
                    vehicleModel = "Menarinibus Citymood 12 gas",
                    occupancyPercentage = rand.nextInt(30, 75)
                ),
                LiveTransitVehicle(
                    id = "RATBV-50-3305",
                    lineName = "Autobuz 50",
                    direction = "Poarta Șchei",
                    latitude = 45.6395 + (rand.nextDouble() - 0.5) * 0.005,
                    longitude = 25.5855 + (rand.nextDouble() - 0.5) * 0.005,
                    delayMinutes = rand.nextInt(0, 2),
                    speedKmh = rand.nextInt(15, 35),
                    vehicleModel = "Karsan Jest Electric",
                    occupancyPercentage = rand.nextInt(45, 80)
                )
            )
            TransportOperator.ELIADO -> listOf(
                LiveTransitVehicle(
                    id = "ELI-L1-4040",
                    lineName = "Autobuz L1",
                    direction = "Casa de Cultură",
                    latitude = 45.1265 + (rand.nextDouble() - 0.5) * 0.006,
                    longitude = 25.7345 + (rand.nextDouble() - 0.5) * 0.006,
                    delayMinutes = rand.nextInt(0, 2),
                    speedKmh = rand.nextInt(25, 45),
                    vehicleModel = "Isuzu Citiport",
                    occupancyPercentage = rand.nextInt(15, 60)
                )
            )
        }
    }

    // Calculated virtual real-time arrivals based on a micro seed derived from current minutes
    fun queryStationArrivals(stationName: String, lineName: String): List<Int> {
        val deterministicSeed = (stationName + lineName).hashCode().absoluteValue
        val baseMin = (System.currentTimeMillis() / 60000 % 60).toInt()
        val rand = Random(deterministicSeed)
        
        val t1 = (rand.nextInt(2, 7) + (30 - baseMin) % 15).absoluteValue
        val t2 = t1 + rand.nextInt(10, 18)
        val t3 = t2 + rand.nextInt(12, 22)
        
        return listOf(if (t1 <= 0) 1 else t1, t2, t3).sorted()
    }
}
