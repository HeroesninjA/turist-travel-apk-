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
        TouristSpot(1001, "Palatul Parlamentului", "București", 44.4275, 26.0872, 90, "Una dintre cele mai mari clădiri administrative din lume.", false, false),
        TouristSpot(1002, "Centrul Vechi", "București", 44.4320, 26.1015, 120, "Inima istorică a Bucureștiului, plină de viață și clădiri de epocă.", false, false),
        TouristSpot(1003, "Ateneul Român", "București", 44.4411, 26.0973, 60, "O bijuterie arhitecturală de importanță istorică națională.", false, false),
        TouristSpot(1004, "Parcul Herăstrău (Mihai I)", "București", 44.4715, 26.0815, 90, "Un parc uriaș, liniștit, situat în jurul unui lac superb.", false, false),
        TouristSpot(1005, "Arcul de Triumf", "București", 44.4671, 26.0782, 30, "Monumentul care celebrează victoria României în Primul Război Mondial.", false, false),
        TouristSpot(1006, "Muzeul Național al Satului", "București", 44.4720, 26.0763, 100, "O incursiune în viața rurală tradițională românească în aer liber.", false, false),
        TouristSpot(1007, "Parcul Cișmigiu", "București", 44.4355, 26.0912, 60, "Cea mai veche grădină publică din București, un lac romantic și alei liniștite.", false, false),
        TouristSpot(1008, "Muzeul Național Grigore Antipa", "București", 44.4531, 26.0847, 90, "Expoziții interactive de zoologie, biodiversitate și fosile de dinozaur.", false, false),
        TouristSpot(1009, "Cărturești Carusel", "București", 44.4319, 26.1022, 45, "Una dintre cele mai spectaculoase librării din lume, situată în Centrul Vechi.", false, false),
        TouristSpot(1010, "Biserica Stavropoleos", "București", 44.4315, 26.0991, 30, "O capodoperă a stilului brâncovenesc, faimoasă pentru curtea sa interioară.", false, false),
        TouristSpot(1011, "Muzeul de Artă al României", "București", 44.4396, 26.0959, 90, "Fostul Palat Regal găzduiește colecții remarcabile de artă românească.", false, false),
        TouristSpot(1012, "Grădina Botanică Dimitrie Brândză", "București", 44.4372, 26.0642, 80, "Oaze de verdeață, sere exotice tropicale și mii de specii de plante în Cotroceni.", false, false),
        TouristSpot(1013, "Parcul Carol I", "București", 44.4140, 26.0965, 70, "Parc istoric frumos cu Mausoleul impunător și fântâni elegante.", false, false),
        TouristSpot(1014, "Palatul Primăverii", "București", 44.4685, 26.0920, 60, "Fostul palat luxos de protocol al soților Nicolae și Elena Ceaușescu.", false, false),
        TouristSpot(1015, "Piața Revoluției", "București", 44.4372, 26.0959, 30, "Piața istorică centrală cu Memorialul Renașterii și clădiri celebre.", false, false),
        TouristSpot(1016, "Hanul lui Manuc", "București", 44.4294, 26.1025, 45, "Cel mai vechi han funcțional din Europa, oferind o ambianță tradițională excelentă.", false, false),
        TouristSpot(1017, "Muzeul Național de Istorie a României", "București", 44.4312, 26.0975, 80, "Exponate arheologice și istorice inestimabile, incluzând Tezaurul istoric național.", false, false),
        TouristSpot(1018, "Palatul Cotroceni", "București", 44.4338, 26.0617, 90, "Reședința oficială a Președintelui și un muzeu istoric de o rară frumusețe.", false, false),
        TouristSpot(1019, "Parcul Tineretului", "București", 44.4080, 26.1040, 60, "Un parc modern imens cu lac de agrement, piste și un ambient extrem de relaxant.", false, false),
        TouristSpot(1020, "Catedrala Mântuirii Neamului", "București", 44.4255, 26.0825, 60, "Cea mai mare catedrală ortodoxă din lume, o structură arhitecturală colosală.", false, false),
        TouristSpot(1021, "Parcul Drumul Taberei", "București", 44.4225, 26.0330, 50, "Cunoscut și ca Parcul Moghioroș, revitalizat cu poduri cochete și sere moderne.", false, false),
        TouristSpot(1022, "Palatul Mogoșoaia", "București", 44.5275, 25.9930, 100, "O clădire istorică în stil brâncovenesc deosebit, situată în exteriorul orașului.", false, false),
        TouristSpot(1023, "Muzeul de Artă Contemporană (MNAC)", "București", 44.4270, 26.0845, 75, "Situat în aripa din spate a Palatului Parlamentului, cu expoziții avangardiste.", false, false),
        TouristSpot(1024, "Piața Universității", "București", 44.4355, 26.1025, 30, "Kilometrul zero al democrației bucureștene, încadrat de clădiri universitare emblematice.", false, false),
        TouristSpot(1025, "Opera Națională București", "București", 44.4345, 26.0790, 90, "Clădire istorică neoclasică, faimos centru de cultură pentru spectacole lirice și balet.", false, false)
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
        TouristSpot(2001, "Grădina Botanică Alexandru Borza", "Cluj-Napoca", 46.7592, 23.5867, 90, "Oază magnifică de verdeață ce adăpostește plante rare și o grădină japoneză.", false, false),
        TouristSpot(2002, "Parcul Central Simion Bărnuțiu", "Cluj-Napoca", 46.7691, 23.5786, 60, "Parcul istoric central cu un lac superb de plimbări cu barca și Casino.", false, false),
        TouristSpot(2003, "Piața Unirii & Biserica Sf. Mihail", "Cluj-Napoca", 46.7689, 23.5898, 65, "Piața istorică principală delimitată de monumentala catedrală gotică.", false, false),
        TouristSpot(2004, "Catedrala Mitropolitană & Piața Avram Iancu", "Cluj-Napoca", 46.7702, 23.5964, 45, "Catedrală ortodoxă impunătoare și piațetă cu fântâni arteziene animate.", false, false),
        TouristSpot(2005, "Dealul Cetățuia", "Cluj-Napoca", 46.7772, 23.5841, 75, "O panoramă spectaculoasă a întregului oraș, ideală la apus de soare.", false, false),
        TouristSpot(2006, "Muzeul de Artă & Palatul Bánffy", "Cluj-Napoca", 46.7701, 23.5910, 50, "Palat baroc splendid ce găzduiește colecții naționale valoroase de artă.", false, false),
        TouristSpot(2007, "Parcul Romulus Vuia (Etnografic)", "Cluj-Napoca", 46.7775, 23.5550, 120, "Primul muzeu în aer liber din România cu gospodării tradiționale transilvănene.", false, false),
        TouristSpot(2008, "Bastionul Croitorilor", "Cluj-Napoca", 46.7675, 23.5960, 30, "Unul dintre puțele turnuri de apărare care s-au păstrat intacte din vechea cetate.", false, false),
        TouristSpot(2009, "Parcul Iulius (Lacul Gheorgheni)", "Cluj-Napoca", 46.7725, 23.6235, 60, "Zonă modernă de recreere în jurul lacului, plină de spații verzi și pontoane.", false, false),
        TouristSpot(2010, "Piața Muzeului", "Cluj-Napoca", 46.7716, 23.5878, 40, "Cea mai veche piață din Cluj-Napoca, flancată de Biserica Franciscană.", false, false),
        TouristSpot(2011, "Pădurea Hoia-Baciu", "Cluj-Napoca", 46.7740, 23.5220, 120, "Pădure faimoasă la nivel mondial pentru peisajele sale misterioase și legende.", false, false),
        TouristSpot(2012, "The Office Cluj & Podul de Fier", "Cluj-Napoca", 46.7766, 23.6065, 30, "O zonă modernă vibrantă, îmbinând arhitectura de birouri cu malul Someșului.", false, false),
        TouristSpot(2013, "Teatrul Național și Opera Română", "Cluj-Napoca", 46.7700, 23.5971, 45, "Clădire neobarocă superbă destinată spectacolelor lirice și teatrale.", false, false),
        TouristSpot(2014, "Turnul Pompierilor", "Cluj-Napoca", 46.7732, 23.5898, 40, "Turn istoric reabilitat recent cu o platformă panoramică superbă.", false, false),
        TouristSpot(2015, "Parcul Cetățuia Buburuza", "Cluj-Napoca", 46.7795, 23.5805, 50, "Zonă adiacentă cetățuii cu alei umbroase, spații de joacă și belvedere retras.", false, false),
        TouristSpot(2016, "Muzeul Național de Istorie a Transilvaniei", "Cluj-Napoca", 46.7720, 23.5870, 75, "Colecții arheologice valoroase despre istoria antică, romană și medievală a Transilvaniei.", false, false),
        TouristSpot(2017, "Biserica Reformată de pe ulița Lupilor", "Cluj-Napoca", 46.7675, 23.5935, 45, "O clădire monument istoric gotic de tip sală, una dintre cele mai vaste din Europa de Est.", false, false),
        TouristSpot(2018, "Cluj Arena", "Cluj-Napoca", 46.7685, 23.5720, 45, "Cel mai modern stadion multifuncțional din inima Transilvaniei, cu o arhitectură high-tech.", false, false),
        TouristSpot(2019, "BT Arena (Sala Polivalentă)", "Cluj-Napoca", 46.7675, 23.5700, 40, "Cea mai mare sală polivalentă din România, găzduiește concerte mari și evenimente sportive.", false, false),
        TouristSpot(2020, "Parcul Rozelor", "Cluj-Napoca", 46.7615, 23.5595, 50, "Parc renumit pentru sutele de soiuri de trandafiri și faleza liniștită pe malul Someșului.", false, false),
        TouristSpot(2021, "Biserica Calvaria (Mănăștur)", "Cluj-Napoca", 46.7588, 23.5570, 45, "O veche mănăstire benedictină fortificată, fiind una dintre cele mai bătrâne biserici din Cluj.", false, false),
        TouristSpot(2022, "Catedrala Greco-Catolică Sf. Iosif (Cipariu)", "Cluj-Napoca", 46.7650, 23.5990, 40, "Catedrală monumentală cu design modern magnific, aflată în Piața Cipariu.", false, false),
        TouristSpot(2023, "Observatorul Astronomic", "Cluj-Napoca", 46.7565, 23.5885, 60, "Situat în campusul USAMV, ideal pentru explorarea stelelor și activități educaționale.", false, false),
        TouristSpot(2024, "Campusul Istoric USAMV", "Cluj-Napoca", 46.7610, 23.5725, 45, "Grădini, livezi istorice și oază verde extinsă în una dintre faimoasele universități clujene.", false, false),
        TouristSpot(2025, "Cetatea Fetei Florești", "Cluj-Napoca", 46.7320, 23.4910, 90, "Loc istoric plin de mister situat pe deal, înconjurat de pădure, ideal pentru drumeții.", false, false)
    )

    // Bus Stations for Brașov
    val BRASOV_STATIONS = listOf(
        BusStation("SBV_GARA", "Gara Brașov", 45.6611, 25.6111),
        BusStation("SBV_SANITAS", "Stația Sanitas", 45.6480, 25.6010),
        BusStation("SBV_PRIMARIE", "Stația Primărie", 45.6450, 25.5990),
        BusStation("SBV_LIVADA", "Stația Livada Poștei", 45.6435, 25.5925),
        BusStation("SBV_NEAGRA", "Stația Biserica Neagră", 45.6418, 25.5878),
        BusStation("SBV_SCHEI", "Stația Poarta Șchei", 45.6394, 25.5858)
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
        TouristSpot(3001, "Biserica Neagră", "Brașov", 45.6418, 25.5878, 45, "Cea mai mare biserică gotică din sud-estul Europei.", false, false),
        TouristSpot(3002, "Piața Sfatului", "Brașov", 45.6425, 25.5890, 60, "Piața istorică principală din Brașov, plină de farmec și cafenele.", false, false),
        TouristSpot(3003, "Telecabina Tâmpa", "Brașov", 45.6390, 25.5940, 90, "Telecabina spre muntele Tâmpa cu panoramă excelentă a orașului.", false, false),
        TouristSpot(3004, "Turnul Alb", "Brașov", 45.6448, 25.5862, 35, "Turn istoric de apărare oferind o vedere spectaculoasă la înălțime.", false, false),
        TouristSpot(3005, "Poarta Șchei", "Brașov", 45.6394, 25.5858, 20, "Poartă barocă superbă ce duce spre vechiul cartier românesc.", false, false),
        TouristSpot(3006, "Turnul Negru", "Brașov", 45.6436, 25.5862, 30, "Turn de strajă din secolul al XV-lea cu vedere panoramică spre Biserica Neagră.", false, false),
        TouristSpot(3007, "Bastionul Țesătorilor", "Brașov", 45.6378, 25.5880, 45, "Unul dintre cele mai bine conservate bastioane medievale, adăpostind o machetă rară.", false, false),
        TouristSpot(3008, "Strada Sforii", "Brașov", 45.6410, 25.5885, 15, "Una dintre cele mai înguste străzi din Europa, un reper fotografic iconic.", false, false),
        TouristSpot(3009, "Prima Școală Românească", "Brașov", 45.6362, 25.5818, 60, "Situată în Șchei, locul unde s-au tipărit primele cărți în limba română.", false, false),
        TouristSpot(3010, "Poarta Ecaterinei", "Brașov", 45.6398, 25.5855, 20, "Singura poartă medievală de acces în cetate păstrată în forma sa originală.", false, false),
        TouristSpot(3011, "Parcul Central Nicolae Titulescu", "Brașov", 45.6455, 25.6025, 40, "Un park mare și liniștit în centrul orașului cu alei largi și fântâni.", false, false),
        TouristSpot(3012, "Bastionul Graft", "Brașov", 45.6450, 25.5860, 30, "Bastion fortificat pitoresc deasupra pârâului Graft, legat de Turnul Alb.", false, false),
        TouristSpot(3013, "Muzeul de Artă Brașov", "Brașov", 45.6460, 25.6015, 45, "Expoziție de pictură și sculptură românească valoroasă, aproape de primărie.", false, false),
        TouristSpot(3014, "Pietrele lui Solomon", "Brașov", 45.6178, 25.5615, 120, "Zonă naturală de chei spectaculoase cu spații verzi pentru recreere.", false, false),
        TouristSpot(3015, "Cetățuia de pe Strajă", "Brașov", 45.6482, 25.5930, 60, "Fortăreață istorică pe dealul Strajă, monument istoric de importanță națională.", false, false),
        TouristSpot(3016, "Turnul Măcelarilor", "Brașov", 45.6385, 25.5890, 35, "Turn vechi de apărare din secolul al XV-lea, parte integrantă din fortificații.", false, false),
        TouristSpot(3017, "Bastionul Cojocarilor", "Brașov", 45.6390, 25.5905, 40, "Bastion istoric ridicat pe latura de sud a cetății sub muntele Tâmpa.", false, false),
        TouristSpot(3018, "Sinagoga Neologă din Brașov", "Brașov", 45.6412, 25.5870, 30, "O clădire religioasă splendidă în stil bizantin, cu detalii decorative fermecătoare.", false, false),
        TouristSpot(3019, "Casa Sfatului (Muzeul de Istorie)", "Brașov", 45.6426, 25.5888, 50, "Simbolul central al orașului Brașov, fostul sediu administrativ medieval.", false, false),
        TouristSpot(3020, "Biserica Sfântul Nicolae", "Brașov", 45.6358, 25.5812, 45, "O biserică orthodoxă impunătoare din Șchei, fondată în secolul al XIII-lea.", false, false),
        TouristSpot(3021, "Promenada de sub Tâmpa", "Brașov", 45.6395, 25.5915, 60, "Aleea pietonală umbroasă Tiberiu Brediceanu, perfectă pentru plimbări relaxante pe sub pădure.", false, false),
        TouristSpot(3022, "Turnul Lemnarilor", "Brașov", 45.6380, 25.5885, 30, "Turn istoric restaurat cochet, găzduiește expoziții de sculptură și ateliere de artă.", false, false),
        TouristSpot(3023, "Cartierul Istoric Șchei", "Brașov", 45.6330, 25.5750, 90, "Explorare pe străduțele vechi și întortocheate, inima spiritului românesc brașovean.", false, false),
        TouristSpot(3024, "Grădina Zoologică Brașov (Noua)", "Brașov", 45.6175, 25.6425, 90, "Una dintre cele mai moderne grădini zoologice din țară, amplasată în pădurea Noua.", false, false),
        TouristSpot(3025, "Lacul Noua & Parc Agrement", "Brașov", 45.6190, 25.6410, 80, "Zonă superbă de relaxare cu bărci, pontoane, terenuri de sport și un aer minunat de munte.", false, false)
    )

    // Default Starting places based on city
    fun getStartSpot(city: String): TouristSpot {
        return when (city) {
            "București" -> TouristSpot(-1, "Gara de Nord (Hotel/Start)", "București", 44.4468, 26.0725, 0, "Punctul de pornire al călătoriei.", false, false)
            "Brașov" -> TouristSpot(-1, "Gara Brașov (Hotel/Start)", "Brașov", 45.6611, 25.6111, 0, "Punctul de pornire al călătoriei.", false, false)
            else -> TouristSpot(-1, "Gara Cluj-Napoca (Hotel/Start)", "Cluj-Napoca", 46.7865, 23.5875, 0, "Punctul de pornire al călătoriei.", false, false)
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
                
                if (matchingLine != null && nearestStationToFrom != null && nearestStationToTo != null) {
                    // We found a direct bus line!
                    // Let's create legs: Walk to station -> Bus ride -> Walk to Spot
                    val walkMeters1 = distToStationFrom.toInt()
                    val walkTime1 = max(1, (walkMeters1 / 80).toInt())
                    if (walkMeters1 > 50) {
                        legs.add(
                            RouteLeg(
                                fromPlaceName = from.name,
                                toPlaceName = nearestStationToFrom.name,
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
                    val busMeters = (calculateDistance(nearestStationToFrom.latitude, nearestStationToFrom.longitude, nearestStationToTo.latitude, nearestStationToTo.longitude)).toInt()
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
