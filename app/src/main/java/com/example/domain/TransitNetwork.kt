package com.example.domain

import com.example.data.TouristSpot
import kotlin.math.*

data class BusStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class BusLine(
    val name: String, // e.g. "L104", "L335", "L783"
    val colorHex: String, // e.g. "#10B981" for emerald
    val stations: List<BusStation>
)

data class RouteLeg(
    val fromPlaceName: String,
    val toPlaceName: String,
    val type: LegType, // WALK or BUS
    val busLineName: String? = null,
    val busColorHex: String? = null,
    val boardingStation: String? = null,
    val alightingStation: String? = null,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val directions: String
)

enum class LegType {
    WALK, BUS
}

data class OptimizedJourney(
    val orderedSpots: List<TouristSpot>,
    val legs: List<RouteLeg>,
    val totalDurationMinutes: Int,
    val totalBusFareLei: Int,
    val departureTime: String = "09:00"
)

object TransitNetwork {
    
    // Calculate Haversine distance in meters
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    // Bus Stations for Bucharest
    val BUCURESTI_STATIONS = listOf(
        BusStation("BUH_GARA", "Gara de Nord", 44.4468, 26.0725),
        BusStation("BUH_IZVOR", "Stația Izvor", 44.4312, 26.0900),
        BusStation("BUH_UNIRII", "Stația Piața Unirii", 44.4265, 26.1025),
        BusStation("BUH_ROMANA", "Stația Piața Romană", 44.4445, 26.0975),
        BusStation("BUH_VICTORIEI", "Stația Piața Victoriei", 44.4525, 26.0860),
        BusStation("BUH_ARC", "Stația Arcul de Triumf", 44.4671, 26.0785),
        BusStation("BUH_PRESA", "Piața Presei Libere", 44.4785, 26.0715)
    )

    // Bus Lines for Bucharest
    val BUCURESTI_LINES = listOf(
        BusLine(
            "Autobuz 104", 
            "#10B981", // Emerald
            listOf(
                BUCURESTI_STATIONS[1], // Izvor
                BUCURESTI_STATIONS[2], // Unirii
                BUCURESTI_STATIONS[3]  // Romană
            )
        ),
        BusLine(
            "Autobuz 335", 
            "#3B82F6", // Blue
            listOf(
                BUCURESTI_STATIONS[3], // Romană
                BUCURESTI_STATIONS[4], // Victoriei
                BUCURESTI_STATIONS[5]  // Arc de Triumf
            )
        ),
        BusLine(
            "Expres 783", 
            "#EF4444", // Red
            listOf(
                BUCURESTI_STATIONS[2], // Unirii
                BUCURESTI_STATIONS[3], // Romană
                BUCURESTI_STATIONS[4], // Victoriei
                BUCURESTI_STATIONS[5], // Arc de Triumf
                BUCURESTI_STATIONS[6]  // Presei
            )
        )
    )

    // Preset Tourist spots for Bucharest
    val BUCURESTI_PRESETS = listOf(
        TouristSpot(1001, "Palatul Parlamentului", "București", 44.4275, 26.0872, 90, "Una dintre cele mai mari clădiri administrative din lume.", false, true),
        TouristSpot(1002, "Centrul Vechi", "București", 44.4320, 26.1015, 120, "Inima istorică a Bucureștiului, plină de viață și clădiri de epocă.", false, true),
        TouristSpot(1003, "Ateneul Român", "București", 44.4411, 26.0973, 60, "O bijuterie arhitecturală de importanță istorică națională.", false, true),
        TouristSpot(1004, "Parcul Herăstrău (Mihai I)", "București", 44.4715, 26.0815, 90, "Un parc uriaș, liniștit, situat în jurul unui lac superb.", false, true),
        TouristSpot(1005, "Arcul de Triumf", "București", 44.4671, 26.0782, 30, "Monumentul care celebrează victoria României în Primul Război Mondial.", false, true),
        TouristSpot(1006, "Muzeul Național al Satului", "București", 44.4720, 26.0763, 100, "O incursiune în viața rurală tradițională românească în aer liber.", false, true)
    )

    // Bus Stations for Cluj-Napoca
    val CLUJ_STATIONS = listOf(
        BusStation("CLJ_GARA", "Gara Cluj-Napoca", 46.7865, 23.5875),
        BusStation("CLJ_AGRO", "Stația Agronomia", 46.7625, 23.5825),
        BusStation("CLJ_MEMO", "Stația Memorandului", 46.7684, 23.5862),
        BusStation("CLJ_OPERA", "Stația Piața Avram Iancu", 46.7698, 23.5955),
        BusStation("CLJ_CENTRAL", "Stația Central", 46.7725, 23.5880),
        BusStation("CLJ_M_VITEAZUL", "Stația Piața Mihai Viteazul", 46.7745, 23.5892),
        BusStation("CLJ_CETATUIA", "Stația Cetățuia", 46.7770, 23.5815)
    )

    // Bus Lines for Cluj-Napoca
    val CLUJ_LINES = listOf(
        BusLine(
            "Autobuz 35", 
            "#8B5CF6", // Purple
            listOf(
                CLUJ_STATIONS[1], // Agronomia
                CLUJ_STATIONS[2], // Memorandului
                CLUJ_STATIONS[3]  // Opera (Avram Iancu)
            )
        ),
        BusLine(
            "Autobuz 5", 
            "#F59E0B", // Amber
            listOf(
                CLUJ_STATIONS[2], // Memorandului
                CLUJ_STATIONS[4], // Central
                CLUJ_STATIONS[5]  // Mihai Viteazul
            )
        ),
        BusLine(
            "Autobuz 9", 
            "#EC4899", // Pink
            listOf(
                CLUJ_STATIONS[3], // Opera
                CLUJ_STATIONS[5], // Mihai Viteazul
                CLUJ_STATIONS[6], // Cetatuia
                CLUJ_STATIONS[0]  // Gara
            )
        )
    )

    // Preset Tourist spots for Cluj-Napoca
    val CLUJ_PRESETS = listOf(
        TouristSpot(2001, "Grădina Botanică Alexandru Borza", "Cluj-Napoca", 46.7592, 23.5867, 90, "Oază magnifică de verdeață ce adăpostește plante rare și o grădină japoneză.", false, true),
        TouristSpot(2002, "Parcul Central Simion Bărnuțiu", "Cluj-Napoca", 46.7691, 23.5786, 60, "Parcul istoric central cu un lac superb de plimbări cu barca și Casino.", false, true),
        TouristSpot(2003, "Piața Unirii & Biserica Sf. Mihail", "Cluj-Napoca", 46.7689, 23.5898, 65, "Piața istorică principală delimitată de monumentala catedrală gotică.", false, true),
        TouristSpot(2004, "Catedrala Mitropolitană & Piața Avram Iancu", "Cluj-Napoca", 46.7702, 23.5964, 45, "Catedrală ortodoxă impunătoare și piațetă cu fântâni arteziene animate.", false, true),
        TouristSpot(2005, "Dealul Cetățuia", "Cluj-Napoca", 46.7772, 23.5841, 75, "O panoramă spectaculoasă a întregului oraș, ideală la apus de soare.", false, true)
    )

    // Bus Stations for Brașov
    val BRASOV_STATIONS = listOf(
        BusStation("SBV_GARA", "Gara Brașov", 45.6611, 25.6111),
        BusStation("SBV_SANITAS", "Stația Sanitas", 45.6495, 25.6030),
        BusStation("SBV_PRIMARIE", "Stația Primărie", 45.6444, 25.6001),
        BusStation("SBV_LIVADA", "Livada Poștei", 45.6465, 25.5895),
        BusStation("SBV_BISERICA", "Biserica Neagră", 45.6420, 25.5880),
        BusStation("SBV_SCHEI", "Poarta Șchei", 45.6395, 25.5855)
    )

    // Bus Lines for Brașov
    val BRASOV_LINES = listOf(
        BusLine(
            "Autobuz 4", 
            "#10B981", // Emerald
            listOf(
                BRASOV_STATIONS[0], // Gara Brașov
                BRASOV_STATIONS[1], // Sanitas
                BRASOV_STATIONS[2], // Primărie
                BRASOV_STATIONS[3]  // Livada Poștei
            )
        ),
        BusLine(
            "Autobuz 50", 
            "#EC4899", // Pink
            listOf(
                BRASOV_STATIONS[3], // Livada Poștei
                BRASOV_STATIONS[4], // Biserica Neagră
                BRASOV_STATIONS[5]  // Poarta Șchei
            )
        )
    )

    // Preset Tourist spots for Brașov
    val BRASOV_PRESETS = listOf(
        TouristSpot(3001, "Biserica Neagră", "Brașov", 45.6418, 25.5878, 45, "Cea mai mare biserică gotică din sud-estul Europei.", false, true),
        TouristSpot(3002, "Piața Sfatului", "Brașov", 45.6425, 25.5890, 60, "Piața istorică principală din Brașov, plină de farmec și cafenele.", false, true),
        TouristSpot(3003, "Telecabina Tâmpa", "Brașov", 45.6390, 25.5940, 90, "Telecabina spre muntele Tâmpa cu panoramă excelentă a orașului.", false, true),
        TouristSpot(3004, "Turnul Alb", "Brașov", 45.6448, 25.5862, 35, "Turn istoric de apărare oferind o vedere spectaculoasă la înălțime.", false, true),
        TouristSpot(3005, "Poarta Șchei", "Brașov", 45.6394, 25.5858, 20, "Poartă barocă superbă ce duce spre vechiul cartier românesc.", false, true)
    )

    // Default Starting places based on city
    fun getStartSpot(city: String): TouristSpot {
        return when (city) {
            "București" -> TouristSpot(-1, "Gara de Nord (Hotel/Start)", "București", 44.4468, 26.0725, 0, "Punctul de pornire al călătoriei.", false, true)
            "Brașov" -> TouristSpot(-1, "Gara Brașov (Hotel/Start)", "Brașov", 45.6611, 25.6111, 0, "Punctul de pornire al călătoriei.", false, true)
            else -> TouristSpot(-1, "Gara Cluj-Napoca (Hotel/Start)", "Cluj-Napoca", 46.7865, 23.5875, 0, "Punctul de pornire al călătoriei.", false, true)
        }
    }

    fun getStationsForCity(city: String): List<BusStation> {
        return when (city) {
            "București" -> BUCURESTI_STATIONS
            "Brașov" -> BRASOV_STATIONS
            else -> CLUJ_STATIONS
        }
    }

    fun getLinesForCity(city: String): List<BusLine> {
        return when (city) {
            "București" -> BUCURESTI_LINES
            "Brașov" -> BRASOV_LINES
            else -> CLUJ_LINES
        }
    }

    /**
     * Compute the optimized path (Traveling Salesperson) using a Greedy (nearest-neighbor) heuristic.
     * Orders the spots starting from the designated start point.
     */
    fun optimizePath(
        startSpot: TouristSpot,
        spotsToVisit: List<TouristSpot>,
        city: String
    ): OptimizedJourney {
        val activeSpots = spotsToVisit.filter { it.isSelected && it.id != startSpot.id }.toMutableList()
        val ordered = mutableListOf<TouristSpot>()
        
        var current = startSpot
        
        // Loop and grab nearest spot based on distance
        while (activeSpots.isNotEmpty()) {
            val nearest = activeSpots.minByOrNull { spot ->
                calculateDistance(current.latitude, current.longitude, spot.latitude, spot.longitude)
            }
            if (nearest != null) {
                ordered.add(nearest)
                activeSpots.remove(nearest)
                current = nearest
            } else {
                break
            }
        }

        // Now calculate journey route details step-by-step
        val legs = mutableListOf<RouteLeg>()
        var totalMinutes = 0
        var totalFare = 0
        
        val fullRouteWithStart = listOf(startSpot) + ordered
        val busLines = getLinesForCity(city)
        val busStations = getStationsForCity(city)
        
        for (i in 0 until fullRouteWithStart.size - 1) {
            val from = fullRouteWithStart[i]
            val to = fullRouteWithStart[i + 1]
            
            // Try to find if we should take a bus or walk.
            // Rule:
            // 1. Find closet bus station to 'from' and 'to'
            val nearestStationToFrom = busStations.minByOrNull { calculateDistance(from.latitude, from.longitude, it.latitude, it.longitude) }
            val nearestStationToTo = busStations.minByOrNull { calculateDistance(to.latitude, to.longitude, it.latitude, it.longitude) }
            
            val distDirect = calculateDistance(from.latitude, from.longitude, to.latitude, to.longitude)
            val distToStationFrom = nearestStationToFrom?.let { calculateDistance(from.latitude, from.longitude, it.latitude, it.longitude) } ?: Double.MAX_VALUE
            val distToStationTo = nearestStationToTo?.let { calculateDistance(to.latitude, to.longitude, it.latitude, it.longitude) } ?: Double.MAX_VALUE
            
            // If the total walking to stations is bigger than direct walking distance, OR direct distance is short (< 800m), just walk!
            val totalWalkDistanceToBus = distToStationFrom + distToStationTo
            if (distDirect < 700.0 || totalWalkDistanceToBus > distDirect) {
                // WALKING ROUTE
                val meters = distDirect.toInt()
                val duration = max(2, (meters / 80).toInt()) // ~5km/h = 83m/min
                legs.add(
                    RouteLeg(
                        fromPlaceName = from.name,
                        toPlaceName = to.name,
                        type = LegType.WALK,
                        distanceMeters = meters,
                        durationMinutes = duration,
                        directions = "Mergi pe jos ${meters} m (~$duration min) de la ${from.name} până la ${to.name}."
                    )
                )
                totalMinutes += duration + to.visitDurationMinutes
            } else {
                // BUS OPTION POSSIBLE
                // Check if any bus line connects nearestStationToFrom and nearestStationToTo
                var matchingLine: BusLine? = null
                var fromIndex = -1
                var toIndex = -1
                
                if (nearestStationToFrom != null && nearestStationToTo != null && nearestStationToFrom.id != nearestStationToTo.id) {
                    for (line in busLines) {
                        val idxF = line.stations.indexOfFirst { it.id == nearestStationToFrom.id }
                        val idxT = line.stations.indexOfFirst { it.id == nearestStationToTo.id }
                        if (idxF != -1 && idxT != -1 && idxF != idxT) {
                            matchingLine = line
                            fromIndex = idxF
                            toIndex = idxT
                            break
                        }
                    }
                }
                
                if (matchingLine != null) {
                    // We found a direct bus line!
                    // Let's create legs: Walk to station -> Bus ride -> Walk to Spot
                    val walkMeters1 = distToStationFrom.toInt()
                    val walkTime1 = max(1, (walkMeters1 / 80).toInt())
                    if (walkMeters1 > 50) {
                        legs.add(
                            RouteLeg(
                                fromPlaceName = from.name,
                                toPlaceName = nearestStationToFrom!!.name,
                                type = LegType.WALK,
                                distanceMeters = walkMeters1,
                                durationMinutes = walkTime1,
                                directions = "Mergi pe jos ${walkMeters1} m până la stația de autobuz: ${nearestStationToFrom.name}."
                            )
                        )
                        totalMinutes += walkTime1
                    }
                    
                    // Bus journey
                    val stationsInBetween = abs(toIndex - fromIndex)
                    val busRideDuration = stationsInBetween * 4 + 2 // 4 min per station average + buffer
                    val busMeters = (calculateDistance(nearestStationToFrom!!.latitude, nearestStationToFrom.longitude, nearestStationToTo!!.latitude, nearestStationToTo.longitude)).toInt()
                    legs.add(
                        RouteLeg(
                            fromPlaceName = nearestStationToFrom.name,
                            toPlaceName = nearestStationToTo.name,
                            type = LegType.BUS,
                            busLineName = matchingLine.name,
                            busColorHex = matchingLine.colorHex,
                            boardingStation = nearestStationToFrom.name,
                            alightingStation = nearestStationToTo.name,
                            distanceMeters = busMeters,
                            durationMinutes = busRideDuration,
                            directions = "Urcă în ${matchingLine.name} din stația ${nearestStationToFrom.name} și mergi $stationsInBetween stații până la stația ${nearestStationToTo.name}."
                        )
                    )
                    totalMinutes += busRideDuration + 3 // 3 min waiting time
                    totalFare += 3 // 3 Lei per bus ride
                    
                    // Walk from station to spot
                    val walkMeters2 = distToStationTo.toInt()
                    val walkTime2 = max(1, (walkMeters2 / 80).toInt())
                    if (walkMeters2 > 50) {
                        legs.add(
                            RouteLeg(
                                fromPlaceName = nearestStationToTo.name,
                                toPlaceName = to.name,
                                type = LegType.WALK,
                                distanceMeters = walkMeters2,
                                durationMinutes = walkTime2,
                                directions = "Mergi pe jos ${walkMeters2} m de la stația ${nearestStationToTo.name} până la ${to.name}."
                            )
                        )
                        totalMinutes += walkTime2
                    }
                    
                    totalMinutes += to.visitDurationMinutes
                } else {
                    // No direct bus, but stations exist. We can offer a transfer if they are far, or just walk/simulate taxi.
                    // For simplicity and solid user experience, we can offer a direct scenic walk or a combination bus leg which we generate:
                    val meters = distDirect.toInt()
                    val duration = max(2, (meters / 80).toInt())
                    legs.add(
                        RouteLeg(
                            fromPlaceName = from.name,
                            toPlaceName = to.name,
                            type = LegType.WALK,
                            distanceMeters = meters,
                            durationMinutes = duration,
                            directions = "Mergi pe jos ${meters} m (~$duration min) prin zonă istorică sau ia un taxi/ridesharing (distanță scurtă, nu există linie directă de autobuz rapidă)."
                        )
                    )
                    totalMinutes += duration + to.visitDurationMinutes
                }
            }
        }
        
        return OptimizedJourney(
            orderedSpots = ordered,
            legs = legs,
            totalDurationMinutes = totalMinutes,
            totalBusFareLei = totalFare
        )
    }
}
