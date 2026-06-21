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
    val stations: List<BusStation>,
    val type: LegType = LegType.BUS
)

data class RouteLeg(
    val fromPlaceName: String,
    val toPlaceName: String,
    val type: LegType, // WALK, BUS, METRO, TROLLEY, TRAIN, TAXI
    val busLineName: String? = null,
    val busColorHex: String? = null,
    val boardingStation: String? = null,
    val alightingStation: String? = null,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val directions: String
)

enum class LegType {
    WALK, BUS, METRO, TROLLEY, TRAIN, TAXI
}

data class OptimizedJourney(
    val orderedSpots: List<TouristSpot>,
    val legs: List<RouteLeg>,
    val totalDurationMinutes: Int,
    val totalBusFareLei: Int,
    val totalTaxiCostLei: Int = 0,
    val departureTime: String = "09:00"
)

data class CityBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
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
        TouristSpot(1001, "Palatul Parlamentului", "București", 44.4275, 26.0872, 90, "Una dintre cele mai mari clădiri administrative din lume.", true, false),
        TouristSpot(1002, "Centrul Vechi", "București", 44.4320, 26.1015, 120, "Inima istorică a Bucureștiului, plină de viață și clădiri de epocă.", true, false),
        TouristSpot(1003, "Ateneul Român", "București", 44.4411, 26.0973, 60, "O bijuterie arhitecturală de importanță istorică națională.", true, false),
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
        TouristSpot(1025, "Opera Națională București", "București", 44.4345, 26.0790, 90, "Clădire istorică neoclasică, faimos centru de cultură pentru spectacole lirice și balet.", false, false),
        TouristSpot(1026, "Parcul Alexandru Ioan Cuza (IOR)", "București", 44.4230, 26.1550, 90, "Unul dintre cele mai îngrijite și frumoase parcuri ale capitalei, axat în jurul lacului Titan.", false, false),
        TouristSpot(1027, "Therme București", "București", 44.6050, 26.0790, 180, "Cel mai mare centru de wellness, relaxare și divertisment cu ape termale din Europa, situat în nord.", false, false),
        TouristSpot(1028, "Palatul Șuțu (Muzeul Bucureștiului)", "București", 44.4348, 26.1030, 60, "O superbă clădire neogotică sclipitoare care găzduiește istoria capitalei chiar în Piața Universității.", false, false),
        TouristSpot(1029, "Teatrul Național I.L. Caragiale", "București", 44.4363, 26.1035, 75, "Epicentrul artelor spectacolului din România, o capodoperă arhitecturală cu acoperiș emblematic roșu.", false, false),
        TouristSpot(1030, "Observatorul Astronomic Vasile Urseanu", "București", 44.4475, 26.0965, 45, "O vilă istorică superbă dotată cu o cupolă de observații unde puteți admira stelele și planetele.", false, false),
        TouristSpot(1031, "Parcul Kiseleff", "București", 44.4590, 26.0820, 50, "Oază istorică romantică pe marginea Bulevardului Kiseleff, plină de statui inedite și arbori bătrâni.", false, false),
        TouristSpot(1032, "Palatul Cantacuzino", "București", 44.4485, 26.0885, 50, "Bijuterie impresionantă în stil art nouveau francez cu forme eclectice, celebrând memoria lui George Enescu.", false, false),
        TouristSpot(1033, "Pasajul Macca-Vilacrosse", "București", 44.4332, 26.0988, 30, "Un încântător pasaj acoperit cu geamuri galbene ce oferă o atmosferă orientală boemă plină de narghilele și puburi.", false, false),
        TouristSpot(1034, "Palatul CEC", "București", 44.4315, 26.0965, 40, "Un superb monument de arhitectură eclectică franceză de secol XIX cu impunătoarea sa cupolă din sticlă și oțel.", false, false),
        TouristSpot(1035, "Muzeul Colecțiilor de Artă", "București", 44.4442, 26.0898, 90, "Fostul Palat Romanit de pe Calea Victoriei, expunând colecții de pictură românească, europeană și orientală.", false, false),
        TouristSpot(1036, "Palatul Justiției", "București", 44.4302, 26.0985, 45, "Monunent masiv pe cheiul Dâmboviței, proiectat în stilul Renașterii Franceze, sediu istoric de judecată.", false, false),
        TouristSpot(1037, "Palatul Patriarhiei", "București", 44.4239, 26.0975, 60, "Centrul spiritual ortodox construit pe Dealul Mitropoliei, o clădire monumentală restaurată minuțios.", false, false),
        TouristSpot(1038, "Muzeul Militar Național", "București", 44.4422, 26.0772, 90, "Moștenire istorică militară rară cuprinzând uniforme de epocă, armament de colecție și piese de aviație.", false, false),
        TouristSpot(1039, "Arena Națională", "București", 44.4372, 26.1525, 45, "Cel mai mare stadion din România, o bijuterie sportivă modernă cu acoperiș retractabil panoramic.", false, false),
        TouristSpot(1040, "Muzeul Național al Literaturii Române", "București", 44.4479, 26.0928, 60, "Spațiu cultural avangardist dedicat manuscriselor, cărților vechi și scriitorilor naționali remarcabili.", false, false),
        TouristSpot(1041, "Palatul Kretzulescu", "București", 44.4422, 26.0898, 50, "O elegantă clădire eclectică rafinată proiectată de Petre Antonescu, situată lângă minunatul Parc Cișmigiu.", false, false),
        TouristSpot(1042, "Biserica Kretzulescu", "București", 44.4382, 26.0960, 30, "Biserică istorică din cărămidă roșie lîngă Piața Revoluției, stil brâncovenesc autentic impunător.", false, false),
        TouristSpot(1043, "Muzeul Tehnic Dimitrie Leonida", "București", 44.4162, 26.0945, 60, "Remarcabil muzeu de inovații tehnologice, vehicule retro și motoare cu abur din Parcul Carol.", false, false),
        TouristSpot(1044, "Palatul Primăriei Capitalei", "București", 44.4350, 26.0925, 45, "Sediul central administrativ ridicat de Petre Antonescu în superb stil neoromânesc monumental vizavi de Cișmigiu.", false, false),
        TouristSpot(1045, "Turnul de Artă (Pantelimon)", "București", 44.4402, 26.1672, 50, "Fost turn de apă de 37 de metri reamenajat cu trepte metalice spirale într-un vibrant hub cultural urban.", false, false),
        TouristSpot(1046, "Muzeul Național al Hărților și Cărții Vechi", "București", 44.4608, 26.0893, 60, "O inedită vilă elegantă din nord ce găzduiește colecții fascinante de hărți de epocă ale lumii.", false, false),
        TouristSpot(1047, "Parcul Plumbuita", "București", 44.4715, 26.1365, 45, "Bătrân parc de promenadă în jurul mănăstirii fortificate ridicate de domnitorul Mihnea Turcitul.", false, false),
        TouristSpot(1048, "Parcul Circului de Stat", "București", 44.4565, 26.1090, 50, "Gaston deosebit renumit pentru lotușii roz rari, aleile cochete din jur și Circul de Stat.", false, false),
        TouristSpot(1049, "Palatul Ghica Tei", "București", 44.4632, 26.1265, 60, "Fostă reședință de vară somptuoasă de secol XIX a domnitorului Grigore Dimitrie Ghica pe malul lacului.", false, false),
        TouristSpot(1050, "Cimitirul Bellu", "București", 44.4022, 26.0998, 70, "Un adevărat muzeu de sculptură în aer liber, panteonul celor mai luminate minți ale spiritului românesc.", false, false)
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
        TouristSpot(2001, "Grădina Botanică Alexandru Borza", "Cluj-Napoca", 46.7592, 23.5867, 90, "Oază magnifică de verdeață ce adăpostește plante rare și o grădină japoneză.", true, false),
        TouristSpot(2002, "Parcul Central Simion Bărnuțiu", "Cluj-Napoca", 46.7691, 23.5786, 60, "Parcul istoric central cu un lac superb de plimbări cu barca și Casino.", true, false),
        TouristSpot(2003, "Piața Unirii & Biserica Sf. Mihail", "Cluj-Napoca", 46.7689, 23.5898, 65, "Piața istorică principală delimitată de monumentala catedrală gotică.", true, false),
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
        TouristSpot(3001, "Biserica Neagră", "Brașov", 45.6418, 25.5878, 45, "Cea mai mare biserică gotică din sud-estul Europei.", true, false),
        TouristSpot(3002, "Piața Sfatului", "Brașov", 45.6425, 25.5890, 60, "Piața istorică principală din Brașov, plină de farmec și cafenele.", true, false),
        TouristSpot(3003, "Telecabina Tâmpa", "Brașov", 45.6390, 25.5940, 90, "Telecabina spre muntele Tâmpa cu panoramă excelentă a orașului.", true, false),
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

    // Bus Stations for Câmpina
    val CAMPINA_STATIONS = listOf(
        BusStation("CMP_GARA", "Gara Câmpina", 45.1189, 25.7078),
        BusStation("CMP_AUTOGARA", "Stația Autogară Câmpina", 45.1245, 25.7185),
        BusStation("CMP_CASTEL", "Stația Castelul Hasdeu", 45.1325, 25.7200),
        BusStation("CMP_GRIGORESCU", "Stația Muzeul Grigorescu", 45.1293, 25.7300),
        BusStation("CMP_CENTRAL", "Stația Casa de Cultură", 45.1265, 25.7345)
    )

    // Bus Lines for Câmpina
    val CAMPINA_LINES = listOf(
        BusLine(
            "Autobuz L1", 
            "#3B82F6", // Blue
            listOf(
                CAMPINA_STATIONS[0], // Gara Câmpina
                CAMPINA_STATIONS[1], // Autogară
                CAMPINA_STATIONS[2], // Castelul Hasdeu
                CAMPINA_STATIONS[3], // Muzeul Grigorescu
                CAMPINA_STATIONS[4]  // Casa de Cultură
            )
        )
    )

    // Preset Tourist spots for Câmpina
    val CAMPINA_PRESETS = listOf(
        TouristSpot(4001, "Castelul \"Iulia Hasdeu\"", "Câmpina", 45.1328, 25.7196, 90, "Un castel încărcat de mister, construit de savantul Bogdan Petriceicu Hasdeu în memoria fiicei sale geniale, Iulia.", true, false),
        TouristSpot(4002, "Muzeul Memorial \"Nicolae Grigorescu\"", "Câmpina", 45.1293, 25.7300, 60, "Casa memorială unde marele pictor Nicolae Grigorescu a trăit și creat în ultimii săi ani de viață.", true, false),
        TouristSpot(4003, "Casa de Cultură Câmpina", "Câmpina", 45.1266, 25.7346, 45, "Centrul cultural principal al orașului, gazdă a numeroase spectacole, expoziții și evenimente locale.", true, false),
        TouristSpot(4004, "Primăria Câmpina", "Câmpina", 45.1252, 25.7381, 30, "Clădirea administrativă centrală din Câmpina, situată pe pitorescul Bulevard al Culturii.", true, false),
        TouristSpot(4005, "Biserica de Lemn \"Adormirea Maicii Domnului\"", "Câmpina", 45.1215, 25.7320, 30, "O pitorească biserică istorică de lemn datând de la 1714, formată dintr-un singur trunchi de stejar.", false, false),
        TouristSpot(4006, "Capela în stil Gotic \"Hernea\"", "Câmpina", 45.1365, 25.7285, 40, "O capelă gotică misterioasă, monument de arhitectură, ridicată în memoria pionierului petrolului, Dumitru Hernea.", false, false),
        TouristSpot(4007, "Bulevardul Culturii (Aleea cu Platani)", "Câmpina", 45.1278, 25.7410, 50, "Zonă de promenadă superbă și relaxantă mărginită de platani uriași, considerat unul dintre cele mai ozonate locuri.", false, false),
        TouristSpot(4008, "Fântâna cu Cireși & Dealul Muscel", "Câmpina", 45.1310, 25.7485, 60, "Cel mai înalt punct de belvedere din zonă, oferind panorame uluitoare spre valea Doftanei și dealurile prahovene.", false, false),
        TouristSpot(4009, "Lacul Câmpina (Lacul Peștelui)", "Câmpina", 45.1165, 25.7225, 45, "Un lac natural liniștit ideal pentru plimbări relaxante pe mal, pescuit și evadare în mijlocul naturii locale.", false, false)
    )

    // Bus Stations for Sinaia
    val SINAIA_STATIONS = listOf(
        BusStation("SIN_GARA", "Gara Sinaia", 45.3510, 25.5539),
        BusStation("SIN_CENTRU", "Centru Sinaia", 45.3524, 25.5529),
        BusStation("SIN_PELES", "Castelul Peleș", 45.3599, 25.5427),
        BusStation("SIN_TELE", "Telecabina Sinaia", 45.3486, 25.5451)
    )

    val SINAIA_LINES = listOf(
        BusLine(
            "Traseu Turistic T1",
            "#10B981",
            listOf(SINAIA_STATIONS[0], SINAIA_STATIONS[1], SINAIA_STATIONS[2], SINAIA_STATIONS[3])
        )
    )

    val SINAIA_PRESETS = listOf(
        TouristSpot(5001, "Castelul Peleș", "Sinaia", 45.3599, 25.5427, 120, "Fosta reședință de vară a regilor României, o capodoperă a arhitecturii renascentiste.", true, false),
        TouristSpot(5002, "Castelul Pelișor", "Sinaia", 45.3591, 25.5414, 60, "Construit pe domeniul Peleșului, cu un stil romantic de neegalat, preferatul Reginei Maria.", true, false),
        TouristSpot(5003, "Mănăstirea Sinaia", "Sinaia", 45.3551, 25.5492, 45, "Cunoscută ca și \"Catedrala Carpaților\", adăpostește o istorie bogată și un muzeu valoros.", true, false),
        TouristSpot(5004, "Cazinoul Sinaia", "Sinaia", 45.3533, 25.5531, 60, "Clădire istorică impunătoare situată în Parcul Dimitrie Ghica, gazdă pentru numeroase evenimente culturale.", false, false),
        TouristSpot(5005, "Parcul Dimitrie Ghica", "Sinaia", 45.3524, 25.5529, 45, "Parcul central al orașului, amenajat în 1881, oferind alei liniștite, statui și copaci seculari.", false, false),
        TouristSpot(5006, "Gara Regală Sinaia", "Sinaia", 45.3510, 25.5539, 30, "O clădire cu o arhitectură elegantă rezervată exclusiv familiei regale în trecut.", false, false),
        TouristSpot(5007, "Cota 1400", "Sinaia", 45.3515, 25.5173, 90, "Un punct de atracție important oferind o priveliște uimitoare asupra văii.", false, false),
        TouristSpot(5008, "Telecabina Sinaia", "Sinaia", 45.3486, 25.5451, 60, "Poarta spre traseele de cota 1400 și 2000 pentru exploratorii montani.", false, false)
    )

    // Bus Stations for Sibiu
    val SIBIU_STATIONS = listOf(
        BusStation("SIB_GARA", "Gara Sibiu", 45.8016, 24.1614),
        BusStation("SIB_PIATA_MARE", "Piața Mare", 45.7966, 24.1520),
        BusStation("SIB_PARC", "Parcul Sub Arini", 45.7876, 24.1444),
        BusStation("SIB_ASTRA", "Muzeul ASTRA", 45.7533, 24.1167)
    )

    val SIBIU_LINES = listOf(
        BusLine(
            "Autobuz Tursib 1",
            "#EF4444",
            listOf(SIBIU_STATIONS[0], SIBIU_STATIONS[1], SIBIU_STATIONS[2], SIBIU_STATIONS[3])
        )
    )

    val SIBIU_PRESETS = listOf(
        TouristSpot(6001, "Piața Mare", "Sibiu", 45.7966, 24.1520, 60, "Inima centrului istoric, înconjurată de clădiri majestuoase și o atmosferă vibrantă.", true, false),
        TouristSpot(6002, "Turnul Sfatului", "Sibiu", 45.7975, 24.1517, 45, "Simbol arhitectural din Sibiu cu panorame uluitoare din vârful podului ce face legătura cu Piața Mică.", true, false),
        TouristSpot(6003, "Podul Minciunilor", "Sibiu", 45.7984, 24.1504, 30, "Primul pod de fontă din România de care se leagă fascinante legende locale urbane.", true, false),
        TouristSpot(6004, "Muzeul Național Brukenthal", "Sibiu", 45.7967, 24.1511, 90, "Cel mai vechi muzeu din România expunând colecții de artă de neprețuit în palatul guvernatorului de odinioară.", false, false),
        TouristSpot(6005, "Piața Mică", "Sibiu", 45.7976, 24.1513, 60, "Renumită pentru celebrele \"ochiuri ale Sibiului\" (lucarnele) ce scrutează orașul tăcut din acoperișuri.", false, false),
        TouristSpot(6006, "Catedrala Evanghelică \"Sf. Maria\"", "Sibiu", 45.7977, 24.1500, 45, "O impunătoare biserică gotică cu cel mai înalt turn din Transilvania.", false, false),
        TouristSpot(6007, "Catedrala Sfânta Treime", "Sibiu", 45.7937, 24.1481, 45, "O bijuterie arhitecturală bizantină, sediul Mitropoliei Ortodoxe.", false, false),
        TouristSpot(6008, "Muzeul ASTRA", "Sibiu", 45.7533, 24.1167, 180, "Cel mai mare ansamblu muzeal etnografic în aer liber cu arhitectură și tradiție din România.", false, false),
        TouristSpot(6009, "Parcul Sub Arini", "Sibiu", 45.7876, 24.1444, 90, "O splendidă grădină istorică seculară, cu alei frumos umbrite de specii rare de arbori.", false, false),
        TouristSpot(6010, "Zidurile Cetății (Bulevardul Corneliu Coposu)", "Sibiu", 45.7941, 24.1554, 45, "Rămășițe păstrate formidabil ale fortificațiilor medievale alături de impunătoarele turnuri de apărare.", false, false)
    )

    // Bus Stations for Sighișoara
    val SIGHISOARA_STATIONS = listOf(
        BusStation("SIG_GARA", "Gara Sighișoara", 46.2238, 24.7981),
        BusStation("SIG_CENTRU", "Centru", 46.2197, 24.7964),
        BusStation("SIG_CETATE", "Cetatea Sighișoara", 46.2185, 24.7925)
    )

    val SIGHISOARA_LINES = listOf(
        BusLine(
            "Autobuz T1",
            "#F59E0B",
            listOf(SIGHISOARA_STATIONS[0], SIGHISOARA_STATIONS[1], SIGHISOARA_STATIONS[2])
        )
    )

    val SIGHISOARA_PRESETS = listOf(
        TouristSpot(7001, "Turnul cu Ceas", "Sighișoara", 46.2192, 24.7933, 60, "Simbolul orașului și principalul punct de acces în Cetate.", true, false),
        TouristSpot(7002, "Biserica din Deal", "Sighișoara", 46.2177, 24.7915, 45, "Una dintre cele mai reprezentative biserici gotice din Transilvania.", true, false),
        TouristSpot(7003, "Scara Acoperită", "Sighișoara", 46.2183, 24.7921, 30, "Construită în 1642, facilitează accesul către Biserica din Deal pe timp de iarnă.", false, false),
        TouristSpot(7004, "Piața Cetății", "Sighișoara", 46.2188, 24.7929, 45, "Inima Cetății, fostul centru comercial unde se adunau breslele orașului.", true, false),
        TouristSpot(7005, "Casa Vlad Dracul", "Sighișoara", 46.2190, 24.7930, 30, "Casa unde s-a născut Vlad Țepeș (astăzi restaurant și mic muzeu).", false, false)
    )

    // Bus Stations for Constanța
    val CONSTANTA_STATIONS = listOf(
        BusStation("CTA_GARA", "Gara Constanța", 44.1678, 28.6366),
        BusStation("CTA_P_OVIDIU", "Piața Ovidiu", 44.1738, 28.6582),
        BusStation("CTA_CAZINO", "Cazinoul din Constanța", 44.1705, 28.6631),
        BusStation("CTA_DELFINARIU", "Delfinariu", 44.2045, 28.6416)
    )

    val CONSTANTA_LINES = listOf(
        BusLine(
            "Linia 100",
            "#0284C7",
            listOf(CONSTANTA_STATIONS[0], CONSTANTA_STATIONS[1], CONSTANTA_STATIONS[2], CONSTANTA_STATIONS[3])
        )
    )

    val CONSTANTA_PRESETS = listOf(
        TouristSpot(8001, "Cazinoul din Constanța", "Constanța", 44.1705, 28.6631, 90, "Simbolul istoric Art Nouveau al litoralului românesc.", true, false),
        TouristSpot(8002, "Piața Ovidiu (Muzeul de Istorie)", "Constanța", 44.1738, 28.6582, 90, "Piața centrală dominată de statuia poetului Ovidiu și Muzeul de Istorie Națională.", true, false),
        TouristSpot(8003, "Moscheea Carol I", "Constanța", 44.1729, 28.6593, 45, "Primul edificiu din beton armat din România, oferind o priveliște superbă din minaret.", false, false),
        TouristSpot(8004, "Acvariul din Constanța", "Constanța", 44.1712, 28.6634, 60, "Situat lângă Cazino, găzduiește o diversitate magnifică de pești și floră acvatică.", true, false),
        TouristSpot(8005, "Farul Genovez", "Constanța", 44.1717, 28.6622, 30, "Construit în jurul anului 1860, un monument istoric de pe faleza orașului.", false, false),
        TouristSpot(8006, "Delfinariul Constanța", "Constanța", 44.2045, 28.6416, 90, "Una dintre atracțiile principale de pe litoral, situată în cadrul Complexului Muzeal de Științe ale Naturii.", false, false)
    )

    // Bus Stations for Iași
    val IASI_STATIONS = listOf(
        BusStation("IAS_GARA", "Gara Iași", 47.1652, 27.5714),
        BusStation("IAS_PALAT", "Palatul Culturii", 47.1575, 27.5866),
        BusStation("IAS_COPOU", "Parcul Copou", 47.1776, 27.5663),
        BusStation("IAS_UNIRII", "Piața Unirii", 47.1659, 27.5815)
    )

    val IASI_LINES = listOf(
        BusLine(
            "Tramvai 9",
            "#16A34A",
            listOf(IASI_STATIONS[2], IASI_STATIONS[3], IASI_STATIONS[0], IASI_STATIONS[1])
        )
    )

    val IASI_PRESETS = listOf(
        TouristSpot(9001, "Palatul Culturii", "Iași", 47.1575, 27.5866, 120, "O capodoperă neogotică uimitoare ce găzduiește Complexul Muzeal Național Moldova.", true, false),
        TouristSpot(9002, "Biserica Sfinții Trei Ierarhi", "Iași", 47.1610, 27.5843, 60, "Faimoasă pentru superba broderie scuptată în piatră ce îi împodobește exteriorul.", true, false),
        TouristSpot(9003, "Parcul Copou & Teiul lui Eminescu", "Iași", 47.1776, 27.5663, 90, "Cea mai veche grădină publică din Iași, plină de poezie și romantism.", true, false),
        TouristSpot(9004, "Catedrala Mitropolitană", "Iași", 47.1614, 27.5822, 60, "O clădire grandioasă, principalul centru religios al Moldovei.", false, false),
        TouristSpot(9005, "Grădina Botanică Anastasie Fătu", "Iași", 47.1856, 27.5544, 120, "Cea mai veche și mai mare grădină botanică din România, o adevărată oază de liniște.", false, false),
        TouristSpot(9006, "Piața Unirii", "Iași", 47.1659, 27.5815, 45, "Locul încărcat de istorie unde s-a jucat Hora Unirii în 1859.", false, false)
    )

    // Bus Stations for Timișoara
    val TIMISOARA_STATIONS = listOf(
        BusStation("TIM_GARA", "Gara de Nord", 45.7505, 21.2014),
        BusStation("TIM_VICT", "Piața Victoriei", 45.7523, 21.2255),
        BusStation("TIM_UNIR", "Piața Unirii", 45.7578, 21.2286),
        BusStation("TIM_ROZE", "Parcul Rozelor", 45.7483, 21.2312)
    )

    val TIMISOARA_LINES = listOf(
        BusLine(
            "Troleibuz 14",
            "#F43F5E",
            listOf(TIMISOARA_STATIONS[0], TIMISOARA_STATIONS[1], TIMISOARA_STATIONS[2], TIMISOARA_STATIONS[3])
        )
    )

    val TIMISOARA_PRESETS = listOf(
        TouristSpot(10001, "Piața Unirii", "Timișoara", 45.7578, 21.2286, 60, "Cea mai veche și mai pitorească piață a orașului, flancată de catedrale și Palatul Baroc.", true, false),
        TouristSpot(10002, "Piața Victoriei și Catedrala Mitropolitană", "Timișoara", 45.7523, 21.2255, 60, "Inima culturală și istorică (locul de naștere al Revoluției), mărginită de Catedrala impunătoare.", true, false),
        TouristSpot(10003, "Parcul Rozelor", "Timișoara", 45.7483, 21.2312, 45, "Una dintre atracțiile verzi celebre, cu sute de specii de trandafiri excepționali.", true, false),
        TouristSpot(10004, "Muzeul de Artă (Palatul Baroc)", "Timișoara", 45.7572, 21.2283, 90, "Găzduiește colecții prestigioase într-un palat baroc admirabil de secol XVIII.", false, false),
        TouristSpot(10005, "Piața Libertății", "Timișoara", 45.7554, 21.2274, 45, "Un spațiu urban reconectat cu istoria, adăpostind vechea Primărie și cercuri armonioase din pavaj.", false, false),
        TouristSpot(10006, "Bastionul Maria Theresia", "Timișoara", 45.7572, 21.2330, 60, "Ultimul rest important al vechii cetăți fortificate a Timișoarei, integrând galerii minunate.", false, false)
    )

    // Bus Stations for Oradea
    val ORADEA_STATIONS = listOf(
        BusStation("ORA_GARA", "Gara Oradea", 47.0682, 21.9421),
        BusStation("ORA_UNIRII", "Piața Unirii", 47.0544, 21.9279),
        BusStation("ORA_CETATE", "Cetatea Oradea", 47.0514, 21.9463),
        BusStation("ORA_AQUAPARK", "Nymphaea", 47.0506, 21.9567)
    )

    val ORADEA_LINES = listOf(
        BusLine(
            "Tramvai 1R",
            "#F97316",
            listOf(ORADEA_STATIONS[0], ORADEA_STATIONS[1], ORADEA_STATIONS[2], ORADEA_STATIONS[3])
        )
    )

    val ORADEA_PRESETS = listOf(
        TouristSpot(11001, "Piața Unirii", "Oradea", 47.0544, 21.9279, 60, "Sufletul orașului, înconjurată de magnifice clădiri Art Nouveau (Vulturul Negru, Palatul Episcopal).", true, false),
        TouristSpot(11002, "Palatul Vulturul Negru", "Oradea", 47.0541, 21.9284, 45, "Cel mai spectaculos palat Secession din Transilvania cu un pasaj din sticlă uluitor.", true, false),
        TouristSpot(11003, "Cetatea Oradea", "Oradea", 47.0514, 21.9463, 120, "O cetate pentagonală impunătoare (stil renascentist), complet renovată, fosta reședință voievodală.", true, false),
        TouristSpot(11004, "Aquapark Nymphaea", "Oradea", 47.0506, 21.9567, 180, "Cel mai modern parc acvatic din vestul țării, perfect pentru relaxare termală și distracție.", false, false),
        TouristSpot(11005, "Catedrala Romano-Catolică & Șirul Canonicilor", "Oradea", 47.0682, 21.9329, 60, "O imensă catedrală barocă alături de faimosul coridor lung cu zeci de arcade superbe.", false, false),
        TouristSpot(11006, "Pietonala Vasile Alecsandri", "Oradea", 47.0558, 21.9298, 45, "Stradă pietonală vibrantă plină de terase cochete, baruri, clădiri renovate și muzică stradală.", true, false)
    )

    // Bus Stations for Alba Iulia
    val ALBA_IULIA_STATIONS = listOf(
        BusStation("ALB_GARA", "Gara Alba Iulia", 46.0620, 23.5780),
        BusStation("ALB_CETATE", "Cetatea Alba Carolina", 46.0683, 23.5721),
        BusStation("ALB_CENTRU", "Centru Alba Iulia", 46.0699, 23.5794)
    )

    val ALBA_IULIA_LINES = listOf(
        BusLine(
            "Autobuz 103",
            "#EAB308",
            listOf(ALBA_IULIA_STATIONS[0], ALBA_IULIA_STATIONS[2], ALBA_IULIA_STATIONS[1])
        )
    )

    val ALBA_IULIA_PRESETS = listOf(
        TouristSpot(12001, "Cetatea Alba Carolina", "Alba Iulia", 46.0683, 23.5721, 120, "Cea mai mare și bine conservată cetate în stil Vauban din România.", true, false),
        TouristSpot(12002, "Catedrala Încoronării", "Alba Iulia", 46.0688, 23.5701, 60, "Locul istoric unde au fost încoronați Ferdinand și Maria ca regi ai României Mari.", true, false),
        TouristSpot(12003, "Catedrala Romano-Catolică Sf. Mihail", "Alba Iulia", 46.0673, 23.5704, 45, "Cea mai veche și mai lungă catedrală din țară, o capodoperă a arhitecturii romanice timpurii.", false, false),
        TouristSpot(12004, "Muzeul Național al Unirii", "Alba Iulia", 46.0679, 23.5723, 90, "Găzduiește documente și artefacte vitale privind Marea Unire de la 1918.", false, false),
        TouristSpot(12005, "Obeliscul lui Horea, Cloșca și Crișan", "Alba Iulia", 46.0674, 23.5684, 30, "Un monument simbolic dedicat conducătorilor răscoalei țărănești din 1784.", false, false)
    )

    // Bus Stations for Suceava
    val SUCEAVA_STATIONS = listOf(
        BusStation("SUC_GARA", "Gara Suceava", 47.6698, 26.2652),
        BusStation("SUC_CETATE", "Cetatea de Scaun", 47.6441, 26.2709),
        BusStation("SUC_CENTRU", "Centru Suceava", 47.6433, 26.2550)
    )

    val SUCEAVA_LINES = listOf(
        BusLine(
            "Autobuz 2",
            "#8B5CF6",
            listOf(SUCEAVA_STATIONS[0], SUCEAVA_STATIONS[2], SUCEAVA_STATIONS[1])
        )
    )

    val SUCEAVA_PRESETS = listOf(
        TouristSpot(13001, "Cetatea de Scaun a Sucevei", "Suceava", 47.6441, 26.2709, 120, "Fosta reședință a domnitorilor Moldovei, o fortăreață medievală remarcabilă.", true, false),
        TouristSpot(13002, "Muzeul Satului Bucovinean", "Suceava", 47.6445, 26.2690, 90, "Muzeu în aer liber care reflectă arhitectura și viața tradițională din Bucovina.", true, false),
        TouristSpot(13003, "Mănăstirea Sfântul Ioan cel Nou", "Suceava", 47.6432, 26.2604, 60, "Ansamblu monahal cu o valoare istorică și spirituală deosebită, inclus în patrimoniul UNESCO.", true, false),
        TouristSpot(13004, "Biserica Mirăuți", "Suceava", 47.6465, 26.2607, 45, "Cea mai veche biserică din Suceava, fosta catedrală mitropolitană a Moldovei.", false, false),
        TouristSpot(13005, "Muzeul Național al Bucovinei", "Suceava", 47.6425, 26.2562, 90, "Oferă colecții bogate de istorie, arheologie și artă din regiunea Bucovinei.", false, false)
    )

    // Bus Stations for Craiova
    val CRAIOVA_STATIONS = listOf(
        BusStation("CRA_GARA", "Gara Craiova", 44.3274, 23.8055),
        BusStation("CRA_ROMANESCU", "Parcul Nicolae Romanescu", 44.2986, 23.8042),
        BusStation("CRA_CENTRU", "Centru Craiova", 44.3188, 23.7946)
    )

    val CRAIOVA_LINES = listOf(
        BusLine(
            "Linia 1T",
            "#A855F7",
            listOf(CRAIOVA_STATIONS[0], CRAIOVA_STATIONS[2], CRAIOVA_STATIONS[1])
        )
    )

    val CRAIOVA_PRESETS = listOf(
        TouristSpot(14001, "Parcul Nicolae Romanescu", "Craiova", 44.2986, 23.8042, 180, "Unul dintre cele mai mari și spectaculoase parcuri naturale din Europa de Est.", true, false),
        TouristSpot(14002, "Muzeul de Artă (Palatul Jean Mihail)", "Craiova", 44.3184, 23.7940, 90, "Palat somptuos ce expune printre altele sculpturi celebre de Constantin Brâncuși.", true, false),
        TouristSpot(14003, "Grădina Botanică Alexandru Buia", "Craiova", 44.3242, 23.7885, 90, "Un spațiu verde pitoresc și educativ cu numeroase specii de plante indigene și exotice.", true, false),
        TouristSpot(14004, "Muzeul Olteniei", "Craiova", 44.3186, 23.7963, 60, "Muzeu dedicat istoriei, etnografiei și naturii din regiunea Olteniei.", false, false),
        TouristSpot(14005, "Piața Mihai Viteazul", "Craiova", 44.3188, 23.7946, 45, "Piața centrală vibrantă, unde tronează statuia lui Mihai Viteazul.", false, false),
        TouristSpot(14006, "Catedrala Mitropolitană Sf. Dumitru", "Craiova", 44.3142, 23.7915, 45, "Cea mai importantă biserică ortodoxă din oraș, cu arhitectură bizantină impresionantă.", false, false)
    )

    // Bus Stations for Arad
    val ARAD_STATIONS = listOf(
        BusStation("ARAD_GARA", "Gara Arad", 46.1895, 21.3255),
        BusStation("ARAD_CENTRU", "Centru Arad", 46.1738, 21.3168),
        BusStation("ARAD_CETATE", "Cetatea Arad", 46.1663, 21.3250)
    )

    val ARAD_LINES = listOf(
        BusLine("Tramvai 3", "#10B981", listOf(ARAD_STATIONS[0], ARAD_STATIONS[1], ARAD_STATIONS[2]))
    )

    val ARAD_PRESETS = listOf(
        TouristSpot(15001, "Palatul Administrativ", "Arad", 46.1765, 21.3180, 60, "O clădire monumentală în stil renascentist, sediul actualei Primării.", true, false),
        TouristSpot(15002, "Cetatea Aradului", "Arad", 46.1663, 21.3250, 90, "O imensă fortăreață austro-ungară în stil Vauban, situată pe malul Mureșului.", true, false),
        TouristSpot(15003, "Palatul Culturii", "Arad", 46.1738, 21.3168, 60, "Edificiu impresionant cu elemente neoclasice și corintice, gazdă pentru muzee și filarmonică.", true, false),
        TouristSpot(15004, "Biserica Roșie", "Arad", 46.1760, 21.3213, 45, "Cunoscută pentru arhitectura sa neo-gotică și impresionanta culoare a cărămizilor aparente.", false, false),
        TouristSpot(15005, "Teatrul Clasic Ioan Slavici", "Arad", 46.1731, 21.3155, 60, "O altă capodoperă a arhitecturii neoclasice arădene.", false, false)
    )

    // Bus Stations for Galați
    val GALATI_STATIONS = listOf(
        BusStation("GAL_GARA", "Gara Galați", 45.4385, 28.0614),
        BusStation("GAL_FALEZA", "Faleza Dunării", 45.4265, 28.0416),
        BusStation("GAL_CENTRU", "Centru Galați", 45.4353, 28.0550)
    )

    val GALATI_LINES = listOf(
        BusLine("Traseul 11", "#0284C7", listOf(GALATI_STATIONS[0], GALATI_STATIONS[2], GALATI_STATIONS[1]))
    )

    val GALATI_PRESETS = listOf(
        TouristSpot(16001, "Faleza Dunării", "Galați", 45.4265, 28.0416, 120, "Cea mai lungă alee pietonală de-a lungul Dunării, presărată cu sculpturi impresionante din metal.", true, false),
        TouristSpot(16002, "Turnul de Televiziune", "Galați", 45.4196, 28.0415, 60, "Oferă o priveliște panoramică incredibilă asupra orașului, Dunării și munților Măcin.", true, false),
        TouristSpot(16003, "Muzeul de Istorie a Naturii și Grădina Botanică", "Galați", 45.4230, 28.0260, 90, "Un complex uimitor care include o minunată grădină japoneză și un mare acvariu.", true, false),
        TouristSpot(16004, "Biserica Precista", "Galați", 45.4290, 28.0487, 45, "Cea mai veche construcție din Galați, o cetate-biserică fortificată de pe faleza Dunării.", false, false),
        TouristSpot(16005, "Muzeul de Artă Vizuală", "Galați", 45.4355, 28.0556, 60, "Primul muzeu de artă contemporană românească din țară.", false, false)
    )

    // Bus Stations for Târgu Mureș
    val TARGU_MURES_STATIONS = listOf(
        BusStation("TGM_GARA", "Gara Târgu Mureș", 46.5365, 24.5420),
        BusStation("TGM_CENTRU", "Centru", 46.5458, 24.5615),
        BusStation("TGM_CETATE", "Cetatea Medievală", 46.5463, 24.5668)
    )

    val TARGU_MURES_LINES = listOf(
        BusLine("Autobuz 18", "#EF4444", listOf(TARGU_MURES_STATIONS[0], TARGU_MURES_STATIONS[1], TARGU_MURES_STATIONS[2]))
    )

    val TARGU_MURES_PRESETS = listOf(
        TouristSpot(17001, "Palatul Culturii", "Târgu Mureș", 46.5435, 24.5583, 90, "Un capodoperă a stilului Secession cu o magnifică sală a oglinzilor, unică în România.", true, false),
        TouristSpot(17002, "Cetatea Medievală din Târgu Mureș", "Târgu Mureș", 46.5463, 24.5668, 120, "Complex fortificat impresionant care găzduiește și Biserica Reformată.", true, false),
        TouristSpot(17003, "Piața Trandafirilor", "Târgu Mureș", 46.5458, 24.5615, 60, "Centrul orașului cu o promenadă superbă, o piață vibrantă plină de monumente și grădini cu flori.", true, false),
        TouristSpot(17004, "Grădina Zoologică din Târgu Mureș", "Târgu Mureș", 46.5510, 24.5822, 120, "Cea mai mare și diversă grădină zoologică din România, situată la Platoul Cornești.", false, false),
        TouristSpot(17005, "Biserica Reformată din Cetate", "Târgu Mureș", 46.5460, 24.5660, 45, "Un superb monument gotic situat central în cetatea orașului.", false, false)
    )

    // Bus Stations for Satu Mare
    val SATU_MARE_STATIONS = listOf(
        BusStation("SM_GARA", "Gara Satu Mare", 47.7850, 22.8905),
        BusStation("SM_CENTRU", "Centrul Vechi", 47.7933, 22.8753),
        BusStation("SM_TURN", "Turnul Pompierilor", 47.7906, 22.8731)
    )

    val SATU_MARE_LINES = listOf(
        BusLine("Linia 1", "#F59E0B", listOf(SATU_MARE_STATIONS[0], SATU_MARE_STATIONS[1], SATU_MARE_STATIONS[2]))
    )

    val SATU_MARE_PRESETS = listOf(
        TouristSpot(18001, "Turnul Pompierilor", "Satu Mare", 47.7906, 22.8731, 45, "O clădire simbolică de la începutul secolului XX cu panoramă frumoasă a orașului.", true, false),
        TouristSpot(18002, "Palatul Administrativ", "Satu Mare", 47.7908, 22.8785, 30, "Una dintre cele mai înalte clădiri istorice construite din România perioadei comuniste.", true, false),
        TouristSpot(18003, "Catedrala Romano-Catolică Înălțarea Domnului", "Satu Mare", 47.7936, 22.8750, 45, "Catedrală impunătoare în stil neoclasic din inima orașului.", false, false),
        TouristSpot(18004, "Teatrul de Nord", "Satu Mare", 47.7925, 22.8762, 60, "O clădire superbă cu o fațadă bogat ornamentată.", false, false),
        TouristSpot(18005, "Templul Mare (Sinagoga Ortodoxă)", "Satu Mare", 47.7938, 22.8716, 45, "O clădire de patrimoniu impresionantă recunoscută pentru elementele decorative.", false, false)
    )

    // Bus Stations for Bacău
    val BACAU_STATIONS = listOf(
        BusStation("BAC_GARA", "Gara Bacău", 46.5651, 26.9038),
        BusStation("BAC_CENTRU", "Centru Bacău", 46.5670, 26.9142),
        BusStation("BAC_PARC", "Parcul Cancicov", 46.5590, 26.9090)
    )

    val BACAU_LINES = listOf(
        BusLine("Traseu 22", "#3B82F6", listOf(BACAU_STATIONS[0], BACAU_STATIONS[1], BACAU_STATIONS[2]))
    )

    val BACAU_PRESETS = listOf(
        TouristSpot(19001, "Parcul Cancicov", "Bacău", 46.5590, 26.9090, 90, "Un minunat parc central de mari dimensiuni adorat pentru plimbări și aer curat.", true, false),
        TouristSpot(19002, "Ansamblul Curtea Domnească", "Bacău", 46.5683, 26.9150, 60, "Ruine istorice din timpul lui Alexandru cel Bun și Ștefan cel Mare.", true, false),
        TouristSpot(19003, "Casa Memorială George Bacovia", "Bacău", 46.5741, 26.9126, 60, "Casa părintească unde a trăit și a creat faimosul poet simbolist.", false, false),
        TouristSpot(19004, "Observatorul Astronomic Victor Anestin", "Bacău", 46.5658, 26.9123, 60, "Centru dedicat expunerii galaxiilor și cunoașterii adâncurilor universului.", true, false),
        TouristSpot(19005, "Catedrala Înălțarea Domnului", "Bacău", 46.5684, 26.9130, 45, "Cea mai impunătoare catedrală ortodoxă a orașului.", false, false)
    )

    // Bus Stations for Ploiești
    val PLOIESTI_STATIONS = listOf(
        BusStation("PLO_GARA", "Gara de Sud", 44.9285, 26.0289),
        BusStation("PLO_CENTRU", "Piața Eroilor", 44.9404, 26.0235),
        BusStation("PLO_CEAS", "Muzeul Ceasului", 44.9380, 26.0200)
    )

    val PLOIESTI_LINES = listOf(
        BusLine("Traseul 2", "#8B5CF6", listOf(PLOIESTI_STATIONS[0], PLOIESTI_STATIONS[1], PLOIESTI_STATIONS[2]))
    )

    val PLOIESTI_PRESETS = listOf(
        TouristSpot(20001, "Muzeul Ceasului Nicolae Simache", "Ploiești", 44.9380, 26.0200, 90, "Colecție unică în Europa de Est de ceasuri vechi cu valoare istorică și artistică extraordinară.", true, false),
        TouristSpot(20002, "Palatul Culturii", "Ploiești", 44.9415, 26.0225, 60, "O clădire imensă și spectaculoasă în centrul orașului conținând Muzeul Omului și Tribunalul.", true, false),
        TouristSpot(20003, "Parcul Bucov", "Ploiești", 44.9600, 26.0600, 120, "O imensă zonă de agrement incluzând o grădină zoologică, lac și plaje artificiale.", false, false),
        TouristSpot(20004, "Muzeul Județean de Artă Prahova", "Ploiești", 44.9355, 26.0255, 60, "Păstrează importante pânze și sculpturi de mare valoare locală și internațională.", false, false),
        TouristSpot(20005, "Hipodromul Ploiești", "Ploiești", 44.9125, 26.0460, 90, "Unul dintre puținele hipodromuri din România dedicat curselor de trap și galop.", true, false)
    )

    // Bus Stations for Miercurea Ciuc
    val MIERCUREA_CIUC_STATIONS = listOf(
        BusStation("MCI_GARA", "Gara Miercurea Ciuc", 46.3650, 25.7950),
        BusStation("MCI_CASTEL", "Castelul Mikó", 46.3575, 25.8016),
        BusStation("MCI_CENTRU", "Piața Majláth Gusztáv", 46.3606, 25.8016)
    )

    val MIERCUREA_CIUC_LINES = listOf(
        BusLine("Linia Verde", "#10B981", listOf(MIERCUREA_CIUC_STATIONS[0], MIERCUREA_CIUC_STATIONS[1], MIERCUREA_CIUC_STATIONS[2]))
    )

    val MIERCUREA_CIUC_PRESETS = listOf(
        TouristSpot(21001, "Castelul Mikó", "Miercurea Ciuc", 46.3575, 25.8016, 90, "Un frumos castel renascentist unde funcționează Muzeul Secuiesc al Ciucului.", true, false),
        TouristSpot(21002, "Mănăstirea și Biserica Franciscană Șumuleu Ciuc", "Miercurea Ciuc", 46.3765, 25.8286, 120, "Cel mai mare loc de pelerinaj catolic din Europa de Est.", true, false),
        TouristSpot(21003, "Parcul Central Miercurea Ciuc", "Miercurea Ciuc", 46.3610, 25.8030, 45, "Inima verde a orașului cu statui și un mic lac liniștitor.", false, false),
        TouristSpot(21004, "Piața Cetății și Promenada", "Miercurea Ciuc", 46.3585, 25.8010, 45, "Loc de promenadă excelent situat imediat lângă castelul Miko.", true, false)
    )

    // Bus Stations for Pitești
    val PITESTI_STATIONS = listOf(
        BusStation("PIT_GARA", "Gara Pitești Sud", 44.8465, 24.8820),
        BusStation("PIT_TRIVALE", "Parcul Trivale", 44.8580, 24.8550),
        BusStation("PIT_CENTRU", "Piața Vasile Milea", 44.8565, 24.8697)
    )

    val PITESTI_LINES = listOf(
        BusLine("Linia 13B", "#EF4444", listOf(PITESTI_STATIONS[0], PITESTI_STATIONS[2], PITESTI_STATIONS[1]))
    )

    val PITESTI_PRESETS = listOf(
        TouristSpot(22001, "Parcul Trivale", "Pitești", 44.8580, 24.8550, 120, "Pădure-parc idilică renumită pentru liniștea sa, locul ideal de repaus.", true, false),
        TouristSpot(22002, "Muzeul Județean Argeș", "Pitești", 44.8585, 24.8720, 60, "O clădire istorică rafinată ce ascunde istoria zonei, o prezență constantă în documentele județului.", true, false),
        TouristSpot(22003, "Mănăstirea Trivale", "Pitești", 44.8595, 24.8520, 45, "Situată chiar în inima parcului omonim, o frumoasă mănăstire de lemn în stil tradițional.", true, false),
        TouristSpot(22004, "Piața Primăriei și Fântâna Muzicală", "Pitești", 44.8560, 24.8695, 60, "O piață superbă celebră pentru spectacolul cu apă, sunet și muzică de pe timpul serii.", false, false),
        TouristSpot(22005, "Galeria de Artă Rudolf Schweitzer-Cumpăna", "Pitești", 44.8540, 24.8710, 45, "Expune lucrări faimoase provenind din mari pictori ai zonei și la nivel național.", false, false)
    )

    // Bus Stations for Brăila
    val BRAILA_STATIONS = listOf(
        BusStation("BRA_GARA", "Gara Brăila", 45.2750, 27.9450),
        BusStation("BRA_FALEZA", "Faleza Dunării", 45.2710, 27.9780),
        BusStation("BRA_CENTRU", "Centru Istoric", 45.2692, 27.9575)
    )

    val BRAILA_LINES = listOf(
        BusLine("Tramvai 21", "#F43F5E", listOf(BRAILA_STATIONS[0], BRAILA_STATIONS[2], BRAILA_STATIONS[1]))
    )

    val BRAILA_PRESETS = listOf(
        TouristSpot(23001, "Faleza Dunării Brăila", "Brăila", 45.2710, 27.9780, 120, "O promenadă pitorească pe malul Dunării, cu terase și vapoare.", true, false),
        TouristSpot(23002, "Ceasul Public din Brăila", "Brăila", 45.2725, 27.9740, 30, "Un monument istoric emblematic de tip orologiu creat în 1909.", true, false),
        TouristSpot(23003, "Teatrul Maria Filotti", "Brăila", 45.2730, 27.9725, 60, "Una dintre cele mai prestigioase clădiri de teatru din România, cu o arhitectură uluitoare.", true, false),
        TouristSpot(23004, "Muzeul Brăilei Carol I", "Brăila", 45.2745, 27.9730, 60, "Muzeu dedicat istoriei și arheologiei zonei adiacente Dunării.", false, false),
        TouristSpot(23005, "Piața Traian", "Brăila", 45.2735, 27.9715, 45, "Centrul vechi și spațios al orașului, marcat de statuia împăratului Traian.", false, false)
    )

    // Bus Stations for Baia Mare
    val BAIA_MARE_STATIONS = listOf(
        BusStation("BAI_GARA", "Gara Baia Mare", 47.6533, 23.5516),
        BusStation("BAI_CENTRU", "Centrul Vechi", 47.6605, 23.5828),
        BusStation("BAI_TURN", "Turnul Ștefan", 47.6601, 23.5820)
    )

    val BAIA_MARE_LINES = listOf(
        BusLine("Linia 1", "#EF4444", listOf(BAIA_MARE_STATIONS[0], BAIA_MARE_STATIONS[1], BAIA_MARE_STATIONS[2]))
    )

    val BAIA_MARE_PRESETS = listOf(
        TouristSpot(24001, "Turnul Ștefan", "Baia Mare", 47.6601, 23.5820, 45, "Clopotnița unei vechi biserici medievale, simbolul orașului Baia Mare.", true, false),
        TouristSpot(24002, "Centrul Istoric", "Baia Mare", 47.6605, 23.5828, 60, "Piața centrală cu clădiri vechi pline de istorie și o atmosferă medievală autentică.", true, false),
        TouristSpot(24003, "Muzeul de Mineralogie", "Baia Mare", 47.6506, 23.5654, 90, "Găzduiește o colecție vastă și valoroasă de minerale unice la nivel mondial (flori de mină).", true, false),
        TouristSpot(24004, "Muzeul Satului", "Baia Mare", 47.6698, 23.5875, 90, "Un parc etnografic în aer liber care expune arhitectura tradițională a Maramureșului.", false, false),
        TouristSpot(24005, "Parcul Regina Maria", "Baia Mare", 47.6667, 23.5786, 60, "Parcul principal al orașului, ideal pentru promenadă și recreere pe malul râului Săsar.", false, false)
    )

    // Bus Stations for Bistrița
    val BISTRITA_STATIONS = listOf(
        BusStation("BIS_GARA", "Gara Bistrița Nord", 47.1265, 24.4855),
        BusStation("BIS_CENTRU", "Piața Centrală", 47.1328, 24.4962),
        BusStation("BIS_PARC", "Parcul Municipal", 47.1352, 24.4960)
    )

    val BISTRITA_LINES = listOf(
        BusLine("Linia 2", "#10B981", listOf(BISTRITA_STATIONS[0], BISTRITA_STATIONS[1], BISTRITA_STATIONS[2]))
    )

    val BISTRITA_PRESETS = listOf(
        TouristSpot(25001, "Biserica Evanghelică", "Bistrița", 47.1328, 24.4962, 60, "Emblema orașului, cu cel mai înalt turn de piatră din Transilvania.", true, false),
        TouristSpot(25002, "Ansamblul Șirul Sugălete", "Bistrița", 47.1325, 24.4958, 45, "Un ansamblu arhitectural cu arcade din perioada gotică târzie.", true, false),
        TouristSpot(25003, "Parcul Municipal", "Bistrița", 47.1352, 24.4960, 60, "Un parc umbros ce flanchează albia pârâului Bistrița.", false, false),
        TouristSpot(25004, "Turnul Dogarilor", "Bistrița", 47.1305, 24.4950, 45, "Singurul turn original care s-a păstrat din vechea cetate a orașului Bistrița.", true, false),
        TouristSpot(25005, "Muzeul Județean", "Bistrița", 47.1332, 24.4925, 60, "Se află într-o clădire istorică impresionantă, vechea cazarmă.", false, false)
    )

    // Bus Stations for Târgoviște
    val TARGOVISTE_STATIONS = listOf(
        BusStation("TGV_GARA", "Gara Târgoviște", 44.9198, 25.4665),
        BusStation("TGV_CHINDIA", "Turnul Chindiei", 44.9315, 25.4578),
        BusStation("TGV_CENTRU", "Centru", 44.9255, 25.4580)
    )

    val TARGOVISTE_LINES = listOf(
        BusLine("Traseul 14", "#8B5CF6", listOf(TARGOVISTE_STATIONS[0], TARGOVISTE_STATIONS[2], TARGOVISTE_STATIONS[1]))
    )

    val TARGOVISTE_PRESETS = listOf(
        TouristSpot(26001, "Ansamblul Monumental Curtea Domnească", "Târgoviște", 44.9315, 25.4578, 120, "Ruinele reședinței domnești a Țării Românești, un complex de importanță istorică.", true, false),
        TouristSpot(26002, "Turnul Chindiei", "Târgoviște", 44.9318, 25.4582, 60, "Monument emblematic construit de Vlad Țepeș, ce oferă o priveliște grandioasă asupra ruinelor și orașului.", true, false),
        TouristSpot(26003, "Mănăstirea Dealu", "Târgoviște", 44.9568, 25.4674, 90, "Vestit așezământ monahal unde se află capul lui Mihai Viteazul.", true, false),
        TouristSpot(26004, "Parcul Chindia", "Târgoviște", 44.9350, 25.4575, 60, "Cel mai mare parc al orașului, situat la poalele ruinelor Curții Domnești.", false, false),
        TouristSpot(26005, "Muzeul Tiparului", "Târgoviște", 44.9288, 25.4581, 45, "Muzeul Tiparului și al Cărții Vechi, prezentând o latură culturală esențială a orașului.", false, false)
    )

    // Bus Stations for Tulcea
    val TULCEA_STATIONS = listOf(
        BusStation("TLC_GARA", "Gara Tulcea", 45.1788, 28.8020),
        BusStation("TLC_FALEZA", "Faleza Dunării", 45.1805, 28.8035),
        BusStation("TLC_ACVARIU", "Acvariu", 45.1800, 28.8055)
    )

    val TULCEA_LINES = listOf(
        BusLine("Linia 1", "#3B82F6", listOf(TULCEA_STATIONS[0], TULCEA_STATIONS[1], TULCEA_STATIONS[2]))
    )

    val TULCEA_PRESETS = listOf(
        TouristSpot(27001, "Faleza Ivan Patzaichin", "Tulcea", 45.1805, 28.8035, 90, "Loc animat unde puteți admira ambarcațiunile ce pleacă spre și vin din Deltă.", true, false),
        TouristSpot(27002, "Centrul Muzeal Ecoturistic Delta Dunării", "Tulcea", 45.1800, 28.8055, 120, "Include unul dintre cele mai mari și spectaculoase acvarii cu faună specifică din zonă.", true, false),
        TouristSpot(27003, "Monumentul Independenței", "Tulcea", 45.1824, 28.8105, 60, "Situat pe deal, oferă o panoramă superbă a orașului Tulcea și a Dunării ce se desparte spre mare.", true, false),
        TouristSpot(27004, "Muzeul de Artă Populară", "Tulcea", 45.1785, 28.8050, 60, "Expune bogăția multi-etnică a zonei prin expoziții remarcabile din zona Dobrogei.", false, false),
        TouristSpot(27005, "Parcul Ciuperca", "Tulcea", 45.1795, 28.7905, 90, "Complex de agrement lacustru pitoresc, perfect pentru plimbare cu barca și activități nautice.", false, false)
    )

    // Bus Stations for Piatra Neamț
    val PIATRA_NEAMT_STATIONS = listOf(
        BusStation("PN_GARA", "Gara Piatra Neamț", 46.9272, 26.3755),
        BusStation("PN_CENTRU", "Piața Curtea Domnească", 46.9328, 26.3683),
        BusStation("PN_TELEGONDOLA", "Stație Telegondolă", 46.9320, 26.3710)
    )

    val PIATRA_NEAMT_LINES = listOf(
        BusLine("Telegondola T1", "#0284C7", listOf(PIATRA_NEAMT_STATIONS[0], PIATRA_NEAMT_STATIONS[1], PIATRA_NEAMT_STATIONS[2]))
    )

    val PIATRA_NEAMT_PRESETS = listOf(
        TouristSpot(28001, "Curtea Domnească", "Piatra Neamț", 46.9328, 26.3683, 60, "Complex istoric din secolul XV, cu Biserica și falnicul Turn al lui Ștefan.", true, false),
        TouristSpot(28002, "Telegondola Piatra Neamț", "Piatra Neamț", 46.9320, 26.3710, 60, "Atracție majoră oferind o vedere aeriană asupra orașului până la baza masivului Cozla.", true, false),
        TouristSpot(28003, "Muzeul de Istorie și Arheologie", "Piatra Neamț", 46.9315, 26.3670, 60, "Fondat de Constantin Matasă, găzduiește vestigii celebre din superba cultură Cucuteni.", false, false),
        TouristSpot(28004, "Parcul Tineretului", "Piatra Neamț", 46.9295, 26.3695, 45, "Un colț plin de verdeață în proximitatea Curții Domnești.", false, false),
        TouristSpot(28005, "Lacul Bâtca Doamnei", "Piatra Neamț", 46.9400, 26.3650, 90, "Zonă naturală pitorească renumită ca habitat pentru numeroase păsări, excelentă pentru plimbări.", false, false)
    )

    // Bus Stations for Râmnicu Vâlcea
    val RAMNICU_VALCEA_STATIONS = listOf(
        BusStation("RV_GARA", "Gara Râmnicu Vâlcea", 45.1010, 24.3645),
        BusStation("RV_CENTRU", "Centrul Vechi", 45.1045, 24.3630),
        BusStation("RV_ZAVOI", "Parcul Zăvoi", 45.1035, 24.3580)
    )

    val RAMNICU_VALCEA_LINES = listOf(
        BusLine("Linia 1", "#F97316", listOf(RAMNICU_VALCEA_STATIONS[0], RAMNICU_VALCEA_STATIONS[1], RAMNICU_VALCEA_STATIONS[2]))
    )

    val RAMNICU_VALCEA_PRESETS = listOf(
        TouristSpot(29001, "Parcul Zăvoi", "Râmnicu Vâlcea", 45.1035, 24.3580, 90, "Unul dintre cele mai vechi parcuri din țară, legat de intonarea imnului național.", true, false),
        TouristSpot(29002, "Muzeul Satului Vâlcean", "Râmnicu Vâlcea", 45.1205, 24.3750, 60, "Muzeu în aer liber prezentând gospodării specifice zonei rurale vâlcene din diferite epoci.", false, false),
        TouristSpot(29003, "Casa Memorială Anton Pann", "Râmnicu Vâlcea", 45.1055, 24.3650, 45, "Muzeu dedicat celebrului scriitor și compozitor, situat în centrul orașului.", true, false),
        TouristSpot(29004, "Arhiepiscopia Râmnicului", "Râmnicu Vâlcea", 45.1110, 24.3615, 60, "Centru religios important cu biserici valoroase și picturi de epocă spectaculoase.", false, false),
        TouristSpot(29005, "Scuarul Mircea cel Bătrân", "Râmnicu Vâlcea", 45.1040, 24.3640, 45, "Zonă de plimbare modernizată în inima orașului, marcată de un peisaj liniștitor.", false, false)
    )

    // Bus Stations for Drobeta-Turnu Severin
    val DROBETA_STATIONS = listOf(
        BusStation("DTS_GARA", "Gara Drobeta-Turnu Severin", 44.6225, 22.6515),
        BusStation("DTS_DUNARE", "Faleza Dunării", 44.6235, 22.6590),
        BusStation("DTS_CASTEL", "Castelul de Apă", 44.6275, 22.6565)
    )

    val DROBETA_LINES = listOf(
        BusLine("Linia 3", "#2563EB", listOf(DROBETA_STATIONS[0], DROBETA_STATIONS[1], DROBETA_STATIONS[2]))
    )

    val DROBETA_PRESETS = listOf(
        TouristSpot(30001, "Piciorul Podului lui Traian", "Drobeta-Turnu Severin", 44.6240, 22.6655, 60, "Ruinele podului monumental construit de faimosul arhitect Apollodor din Damasc.", true, false),
        TouristSpot(30002, "Castelul de Apă", "Drobeta-Turnu Severin", 44.6275, 22.6565, 45, "Un castel simbolic pentru oraș, funcționând azi ca spațiu expozițional și de belvedere superb.", true, false),
        TouristSpot(30003, "Muzeul Regiunii Porților de Fier", "Drobeta-Turnu Severin", 44.6245, 22.6610, 90, "Muzeu vast cuprinzând zone cu acvarii, și expoziții de istorie și etnologie a Dunării.", false, false),
        TouristSpot(30004, "Cetatea Severinului", "Drobeta-Turnu Severin", 44.6250, 22.6635, 60, "Cetate medievală restaurată situată într-un parc pitoresc fix pe malul apei.", true, false),
        TouristSpot(30005, "Parcul Teiuș", "Drobeta-Turnu Severin", 44.6280, 22.6480, 45, "Parc generos de relaxare aproape de malul fluviului Dunărea.", false, false)
    )

    // Bus Stations for Deva
    val DEVA_STATIONS = listOf(
        BusStation("DEV_GARA", "Gara Deva", 45.8850, 22.9150),
        BusStation("DEV_CETATE", "Baza Telecabină Cetate", 45.8885, 22.8985),
        BusStation("DEV_CENTRU", "Centru Pietonal", 45.8825, 22.9110)
    )

    val DEVA_LINES = listOf(
        BusLine("Linia 6", "#65A30D", listOf(DEVA_STATIONS[0], DEVA_STATIONS[2], DEVA_STATIONS[1]))
    )

    val DEVA_PRESETS = listOf(
        TouristSpot(31001, "Cetatea Devei", "Deva", 45.8885, 22.8970, 120, "Cetate medievală maiestuoasă situată pe deal, oferind vizitatorilor o panoramă uimitoare.", true, false),
        TouristSpot(31002, "Telecabina Cetatea Devei", "Deva", 45.8885, 22.8985, 30, "Mijloc modern de ascensiune rapidă spre nivelul superioar al fortificației istorice.", true, false),
        TouristSpot(31003, "Magna Curia", "Deva", 45.8860, 22.8995, 90, "Castel renascentist unde este amenajat Muzeul Civilizației Dacice și Romane.", false, false),
        TouristSpot(31004, "Parcul Cetății", "Deva", 45.8855, 22.9020, 60, "Parc relaxant plin de specii de arbori rari situat la poalele ruinelor cetății.", false, false),
        TouristSpot(31005, "Piața Unirii", "Deva", 45.8830, 22.9105, 45, "Punctul focal al centrului civic, presărat cu terase, cafenele primitoare și monumente.", true, false)
    )

    // Bus Stations for Botoșani
    val BOTOSANI_STATIONS = listOf(
        BusStation("BOT_GARA", "Gara Botoșani", 47.7475, 26.6535),
        BusStation("BOT_CENTRU", "Piața 1 Decembrie", 47.7435, 26.6630),
        BusStation("BOT_PARC", "Parcul Mihai Eminescu", 47.7445, 26.6685)
    )

    val BOTOSANI_LINES = listOf(
        BusLine("Tramvai 1", "#BE123C", listOf(BOTOSANI_STATIONS[0], BOTOSANI_STATIONS[1], BOTOSANI_STATIONS[2]))
    )

    val BOTOSANI_PRESETS = listOf(
        TouristSpot(32001, "Parcul Mihai Eminescu", "Botoșani", 47.7445, 26.6685, 60, "Parc istoric cu un lac, amenajat pitoresc, care l-a inspirat evident pe poetul național.", true, false),
        TouristSpot(32002, "Casa Memorială Nicolae Iorga", "Botoșani", 47.7430, 26.6645, 45, "Muzeu amenajat admirabil în casele de secol XIX aparținând familiei reputatului istoric.", true, false),
        TouristSpot(32003, "Centrul Vechi", "Botoșani", 47.7435, 26.6630, 60, "Arie pietonală unică având cel mai bine păstrat complex de arhitectură comercială de secol XIX.", true, false),
        TouristSpot(32004, "Muzeul Județean", "Botoșani", 47.7420, 26.6615, 60, "O clădire extinsă ce menține și descrie numeroase obiecte prețioase de interes major.", false, false),
        TouristSpot(32005, "Biserica Uspenia", "Botoșani", 47.7440, 26.6650, 45, "Biserica fondată de familia domnitoare și faimoasă pentru că e locul de botez al poetului național.", false, false)
    )

    // Bus Stations for Sfântu Gheorghe
    val SFANTU_GHEORGHE_STATIONS = listOf(
        BusStation("SFG_GARA", "Gara Sfântu Gheorghe", 45.8560, 25.7955),
        BusStation("SFG_CENTRU", "Piața Libertății", 45.8645, 25.7885),
        BusStation("SFG_PARC", "Parcul Elisabeta", 45.8655, 25.7865)
    )

    val SFANTU_GHEORGHE_LINES = listOf(
        BusLine("Linia 2", "#14B8A6", listOf(SFANTU_GHEORGHE_STATIONS[0], SFANTU_GHEORGHE_STATIONS[1], SFANTU_GHEORGHE_STATIONS[2]))
    )

    val SFANTU_GHEORGHE_PRESETS = listOf(
        TouristSpot(33001, "Muzeul Național Secuiesc", "Sfântu Gheorghe", 45.8610, 25.7870, 90, "Unul dintre cele mai vechi muzee din țară, adăpostit într-o clădire proiectată de Kós Károly.", true, false),
        TouristSpot(33002, "Piața Libertății", "Sfântu Gheorghe", 45.8645, 25.7885, 45, "Zonă centrală superbă cu monumente istorice, alei cu vizibilitate și o statuie stradală tipică de dragon.", true, false),
        TouristSpot(33003, "Parcul Elisabeta", "Sfântu Gheorghe", 45.8655, 25.7865, 60, "Cel mai iubit parc al orașului, ideal pentru odihnă, dispunând pe lacul de nufăr de un spațiu minunat.", false, false),
        TouristSpot(33004, "Biserica Reformată Fortificată", "Sfântu Gheorghe", 45.8665, 25.7815, 60, "Lăcaș medieval impresionant la extremitatea centrului, masiv și protejat de ziduri de piatră.", true, false),
        TouristSpot(33005, "Galeria de Artă MAGMA", "Sfântu Gheorghe", 45.8640, 25.7895, 45, "Atracție contemporană ce prezintă variate lucrări inedite de artă modernă și vernisaje regulate.", false, false)
    )

    // Bus Stations for Giurgiu
    val GIURGIU_STATIONS = listOf(
        BusStation("GR_GARA", "Gara Giurgiu Oraș", 43.8965, 25.9680),
        BusStation("GR_DUNARE", "Portul Giurgiu", 43.8820, 25.9650),
        BusStation("GR_CENTRU", "Piața Unirii", 43.8935, 25.9655)
    )

    val GIURGIU_LINES = listOf(
        BusLine("Linia 4", "#DC2626", listOf(GIURGIU_STATIONS[0], GIURGIU_STATIONS[2], GIURGIU_STATIONS[1]))
    )

    val GIURGIU_PRESETS = listOf(
        TouristSpot(34001, "Podul Prieteniei", "Giurgiu", 43.8850, 26.0020, 45, "Podul celebru ce leagă simbolic România de Bulgaria, traversând maiestuos Dunărea.", true, false),
        TouristSpot(34002, "Turnul cu Ceas", "Giurgiu", 43.8930, 25.9660, 30, "Ales drept principal simbol al orașului, un martor tăcut al istoriei vechi a Giurgiului.", true, false),
        TouristSpot(34003, "Faleza Dunării", "Giurgiu", 43.8800, 25.9640, 60, "O zonă minunată perfectă pentru o plimbare calmă cu o cafea spre punctul de trecere.", false, false),
        TouristSpot(34004, "Parcul Alei", "Giurgiu", 43.8920, 25.9720, 45, "Un spațiu urban înverzit, loc imediat de relaxare la îndemână printre clădirile gri.", false, false),
        TouristSpot(34005, "Cetatea Giurgiu", "Giurgiu", 43.8860, 25.9605, 60, "Ruinele unei celebre fortărețe direct pe insula din fața orașului, cu impact masiv.", true, false)
    )

    // Bus Stations for Călărași
    val CALARASI_STATIONS = listOf(
        BusStation("CL_GARA", "Gara Călărași Sud", 44.1950, 27.3200),
        BusStation("CL_DUNARE", "Faleza Brațul Borcea", 44.1880, 27.3250),
        BusStation("CL_CENTRU", "Centrul Civic", 44.1985, 27.3300)
    )

    val CALARASI_LINES = listOf(
        BusLine("Linia 5", "#2563EB", listOf(CALARASI_STATIONS[0], CALARASI_STATIONS[2], CALARASI_STATIONS[1]))
    )

    val CALARASI_PRESETS = listOf(
        TouristSpot(35001, "Muzeul Dunării de Jos", "Călărași", 44.1970, 27.3295, 60, "Expune bogate colecții arheologice valoroase despre evoluția civilizațiilor de pe râu.", true, false),
        TouristSpot(35002, "Faleza Brațul Borcea", "Călărași", 44.1880, 27.3250, 90, "Loc predilect de promenadă imediat pe malul apei într-un mediu cu peisaj generos.", true, false),
        TouristSpot(35003, "Parcul Dumbrava", "Călărași", 44.1955, 27.3400, 60, "O imensă zonă complexă de verdeață masivă ce cuprinde și grădina zoologică celebră.", false, false),
        TouristSpot(35004, "Biserica Sfinții Împărați", "Călărași", 44.1985, 27.3325, 45, "Superb lăcaș cu fresce speciale, un element esențial al prezenței religioase din urbe.", false, false),
        TouristSpot(35005, "Monumentul Eroilor", "Călărași", 44.1990, 27.3305, 30, "Un punct reperabil ce semnalează vizibil recunoștința orașului direct pe bulevardul mare.", false, false)
    )

    // Bus Stations for Slobozia
    val SLOBOZIA_STATIONS = listOf(
        BusStation("IL_GARA", "Gara Slobozia Veche", 44.5650, 27.3680),
        BusStation("IL_CENTRU", "Piața Revoluției", 44.5620, 27.3620),
        BusStation("IL_PARC", "Parcul Ialomița", 44.5550, 27.3600)
    )

    val SLOBOZIA_LINES = listOf(
        BusLine("Traseul 7", "#F59E0B", listOf(SLOBOZIA_STATIONS[0], SLOBOZIA_STATIONS[1], SLOBOZIA_STATIONS[2]))
    )

    val SLOBOZIA_PRESETS = listOf(
        TouristSpot(36001, "Muzeul Județean Ialomița", "Slobozia", 44.5615, 27.3610, 60, "Muzeul principal extrem de interesant detaliind precis moștenirea de pe câmpiile extinse.", true, false),
        TouristSpot(36002, "Muzeul Național al Agriculturii", "Slobozia", 44.5630, 27.3550, 90, "O clădire națională foarte unică din inima Bărăganului ce reține istoria vastă curățită.", true, false),
        TouristSpot(36003, "Mănăstirea Sfinții Voievozi", "Slobozia", 44.5580, 27.3650, 60, "Punct esențial religios localizat aproape perfect în zona de agrement liniștită a Ialomiței.", false, false),
        TouristSpot(36004, "Parcul de Vacanță Hermes", "Slobozia", 44.5680, 27.3450, 60, "Nostalgic domeniu cunoscut frecvent public la nivel național în deceniile pre-digitalizare.", false, false),
        TouristSpot(36005, "Monumentul Eroilor", "Slobozia", 44.5625, 27.3625, 30, "Un monument impresionant chiar lângă zona administrativă din centrul revoluționar.", true, false)
    )

    // Bus Stations for Zalău
    val ZALAU_STATIONS = listOf(
        BusStation("ZAL_GARA", "Gara Zalău Nord", 47.2050, 23.0650),
        BusStation("ZAL_CENTRU", "Piața 1 Decembrie 1918", 47.1915, 23.0560),
        BusStation("ZAL_PARC", "Parcul Central", 47.1885, 23.0535)
    )

    val ZALAU_LINES = listOf(
        BusLine("Linia 21", "#8B5CF6", listOf(ZALAU_STATIONS[0], ZALAU_STATIONS[1], ZALAU_STATIONS[2]))
    )

    val ZALAU_PRESETS = listOf(
        TouristSpot(37001, "Muzeul Județean C.C.", "Zalău", 47.1910, 23.0555, 60, "Include colecții inestimabile despre evoluția romanilor și exponate dacice găsite la Porolissum.", true, false),
        TouristSpot(37002, "Clădirea Primăriei", "Zalău", 47.1920, 23.0560, 45, "Centrul de decizie spectaculos cu un fronton asimetric minunat perfect integrat Ardealului.", false, false),
        TouristSpot(37003, "Parcul Central Municipal", "Zalău", 47.1885, 23.0535, 60, "O fâșie superbă înverzită imediat de pe centura urbei cu opțiuni grozave pentru copii.", false, false),
        TouristSpot(37004, "Castrul Roman Porolissum", "Zalău", 47.1800, 23.1550, 120, "Destinație epică esențială de graniță, un sit remarcabil și vast unde viața se resimte din ruine.", true, false),
        TouristSpot(37005, "Biserica Reformată", "Zalău", 47.1915, 23.0530, 45, "Un lăcaș cu o turlă semeț dominată de credința trecului și un minimalism calvinist atrăgător.", false, false)
    )

    // Bus Stations for Focșani
    val FOCSANI_STATIONS = listOf(
        BusStation("FOC_GARA", "Gara Focșani", 45.6965, 27.1935),
        BusStation("FOC_CENTRU", "Piața Unirii", 45.6980, 27.1840),
        BusStation("FOC_CRANG", "Crângul Petrești", 45.7190, 27.2065)
    )

    val FOCSANI_LINES = listOf(
        BusLine("Linia 9", "#EC4899", listOf(FOCSANI_STATIONS[0], FOCSANI_STATIONS[1], FOCSANI_STATIONS[2]))
    )

    val FOCSANI_PRESETS = listOf(
        TouristSpot(38001, "Piața Unirii și Borna de Hotar", "Focșani", 45.6980, 27.1840, 45, "Loc fundamental și istoric de mare emoție simbolică reprezentând frontiera cu sudul țării.", true, false),
        TouristSpot(38002, "Teatrul Mr. Gheorghe Pastia", "Focșani", 45.6995, 27.1855, 60, "Bijuterie impunătoare de cultură cu foaiere strălucitoare ce se ridică printre copacii mari.", true, false),
        TouristSpot(38003, "Muzeul Unirii", "Focșani", 45.6975, 27.1850, 60, "Instituție dedicată marilor artizani din est care unificau principatele pline de manuscrise de preț.", true, false),
        TouristSpot(38004, "Crângul Petrești", "Focșani", 45.7190, 27.2065, 90, "Lază vastă rezervată din afara nucleului urbei absolut ideală toamna ca drumeție în natură veritabilă.", false, false),
        TouristSpot(38005, "Biserica Sfântul Ioan", "Focșani", 45.6985, 27.1825, 45, "Așezământ ortodox ridicat demult fiind centrul spiritual marcant al locuitorilor fideli voievodali.", false, false)
    )

    // Bus Stations for Buzău
    val BUZAU_STATIONS = listOf(
        BusStation("BZ_GARA", "Gara Buzău", 45.1435, 26.8285),
        BusStation("BZ_CRANG", "Parcul Crâng", 45.1500, 26.8120),
        BusStation("BZ_CENTRU", "Piața Dacia", 45.1525, 26.8220)
    )

    val BUZAU_LINES = listOf(
        BusLine("Linia 7", "#3B82F6", listOf(BUZAU_STATIONS[0], BUZAU_STATIONS[2], BUZAU_STATIONS[1]))
    )

    val BUZAU_PRESETS = listOf(
        TouristSpot(39001, "Parcul Crâng", "Buzău", 45.1500, 26.8120, 90, "Un imens parc natural rezidual din vechii Codri ai Vlăsiei cu lac și monumente minunate.", true, false),
        TouristSpot(39002, "Palatul Comunal", "Buzău", 45.1530, 26.8225, 45, "Clădire simbol a orașului situată excelent în piața Dacia, absolut splendidă.", true, false),
        TouristSpot(39003, "Muzeul Județean Buzău", "Buzău", 45.1550, 26.8210, 60, "Deține valoroase piese din istoria regiunii inclusiv un inedit telefon cu manivelă.", false, false),
        TouristSpot(39004, "Vulcanii Noroioși", "Buzău", 45.3400, 26.7150, 120, "Fenomen geologic rar de proporții majore, format din emisii de gaze naturale din pământ.", true, false),
        TouristSpot(39005, "Cimitirul Eroilor", "Buzău", 45.1490, 26.8160, 45, "Cel mai extins și important cimitir al eroilor vizitat anual de zeci de pasionați militari.", false, false)
    )

    // Bus Stations for Reșița
    val RESITA_STATIONS = listOf(
        BusStation("CS_GARA", "Gara Reșița Sud", 45.2935, 21.8905),
        BusStation("CS_MUZEU", "Muzeul de Locomotive", 45.3160, 21.8765),
        BusStation("CS_CENTRU", "Centrul Civic", 45.3015, 21.8885)
    )

    val RESITA_LINES = listOf(
        BusLine("Tramvai 1R", "#EF4444", listOf(RESITA_STATIONS[0], RESITA_STATIONS[2], RESITA_STATIONS[1]))
    )

    val RESITA_PRESETS = listOf(
        TouristSpot(40001, "Muzeul de Locomotive", "Reșița", 45.3160, 21.8765, 90, "Rezervație superbă în aer liber cuprinzând vestite locomotive vechi extrem de autentice.", true, false),
        TouristSpot(40002, "Centrul Civic", "Reșița", 45.3015, 21.8885, 45, "Punctul administrativ cu o fântână arteziană cinetică unică realizată spectaculos.", true, false),
        TouristSpot(40003, "Funicularul Industrial", "Reșița", 45.2950, 21.8960, 45, "Structură suspendată masivă, un simbol al istoriei industriale a metalurgiei bănățene.", false, false),
        TouristSpot(40004, "Lacul Secu", "Reșița", 45.2500, 21.9500, 120, "O zonă turistică și minunată și extinsă direct în munți, fiind foarte ideală vara.", false, false),
        TouristSpot(40005, "Biserica Sfinții Apostoli", "Reșița", 45.2980, 21.8880, 45, "Cel mai respectat spațiu teologic cu caracter istoric situat chiar în inima Văii urbane.", false, false)
    )

    // Bus Stations for Târgu Jiu
    val TARGU_JIU_STATIONS = listOf(
        BusStation("GJ_GARA", "Gara Târgu Jiu", 45.0350, 23.2790),
        BusStation("GJ_PARC", "Parcul Central", 45.0395, 23.2685),
        BusStation("GJ_COLOANA", "Parcul Coloanei", 45.0370, 23.2855)
    )

    val TARGU_JIU_LINES = listOf(
        BusLine("Linia 9", "#10B981", listOf(TARGU_JIU_STATIONS[0], TARGU_JIU_STATIONS[1], TARGU_JIU_STATIONS[2]))
    )

    val TARGU_JIU_PRESETS = listOf(
        TouristSpot(41001, "Poarta Sărutului", "Târgu Jiu", 45.0390, 23.2680, 30, "Monumet absolut esențial, opera de excepție mondială a sculptorului Constantin Brâncuși.", true, false),
        TouristSpot(41002, "Masa Tăcerii", "Târgu Jiu", 45.0400, 23.2675, 30, "Situată strategic pe aleea principală aproape perfect de malul foarte pitoresc al Jiului.", true, false),
        TouristSpot(41003, "Coloana Infinitului", "Târgu Jiu", 45.0370, 23.2855, 45, "Cea mai emblematică piesă sculpturală ce țintește cerul oferind o stare transcendentală.", true, false),
        TouristSpot(41004, "Muzeul de Artă", "Târgu Jiu", 45.0385, 23.2695, 60, "Amenajat într-un spectaculos palat din parcul extins punând baze artistice variate.", false, false),
        TouristSpot(41005, "Calea Eroilor", "Târgu Jiu", 45.0380, 23.2760, 60, "Axa perfectă ce parcurge vizual orașul de la Coloana Infinitului până direct la râu.", true, false)
    )

    // Bus Stations for Slatina
    val SLATINA_STATIONS = listOf(
        BusStation("OT_GARA", "Gara Slatina", 44.4285, 24.3685),
        BusStation("OT_ESPLANADA", "Esplanada", 44.4320, 24.3600),
        BusStation("OT_OLT", "Faleza Olt", 44.4345, 24.3510)
    )

    val SLATINA_LINES = listOf(
        BusLine("Traseul 1", "#8B5CF6", listOf(SLATINA_STATIONS[0], SLATINA_STATIONS[1], SLATINA_STATIONS[2]))
    )

    val SLATINA_PRESETS = listOf(
        TouristSpot(42001, "Esplanada Slatina", "Slatina", 44.4320, 24.3600, 60, "Zonă modernă de relaxare pietonală perfectă din centrul frumos amenajat al municipiului.", true, false),
        TouristSpot(42002, "Muzeul Județean Olt", "Slatina", 44.4290, 24.3620, 60, "Un sediu bogat având și rare colecții de artă tradițională autentică pentru public.", true, false),
        TouristSpot(42003, "Faleza Oltului", "Slatina", 44.4345, 24.3510, 45, "Zonă de relaxare extinsă, un coridor aflat aproape de râul Olt și valea mare.", false, false),
        TouristSpot(42004, "Biserica Sfânta Treime", "Slatina", 44.4310, 24.3585, 45, "O atracție de o frumusețe copleșitoare cu elemente religioase neștiute vechi de secole.", false, false),
        TouristSpot(42005, "Dealul Grădiște", "Slatina", 44.4350, 24.3650, 60, "O colină frumoasă ce oferă perspectivă spectaculoasă panoramică deasupra locatarilor.", true, false)
    )

    // Bus Stations for Alexandria
    val ALEXANDRIA_STATIONS = listOf(
        BusStation("TR_GARA", "Gara Alexandria", 43.9740, 25.3280),
        BusStation("TR_CENTRU", "Centrul Civic", 43.9700, 25.3295),
        BusStation("TR_PARC", "Parcul Pădurea Vedea", 43.9680, 25.3225)
    )

    val ALEXANDRIA_LINES = listOf(
        BusLine("Linia Verde", "#EAB308", listOf(ALEXANDRIA_STATIONS[0], ALEXANDRIA_STATIONS[1], ALEXANDRIA_STATIONS[2]))
    )

    val ALEXANDRIA_PRESETS = listOf(
        TouristSpot(43001, "Parcul Pădurea Vedea", "Alexandria", 43.9680, 25.3225, 90, "Locație ideală și pitorească de relaxare absolut minunată traversată de râul calm Vedea.", true, false),
        TouristSpot(43002, "Catedrala Sfântul Alexandru", "Alexandria", 43.9685, 25.3315, 60, "Catedrală monumentală masivă și splendidă de o frumusețe aparte în pusta Bărăganului.", true, false),
        TouristSpot(43003, "Muzeul Județean", "Alexandria", 43.9690, 25.3300, 60, "Clădire în superb stil clasic, complet cu monede sau pietre rare referitoare la regiune.", false, false),
        TouristSpot(43004, "Monumentul Eroilor", "Alexandria", 43.9710, 25.3290, 30, "Evidențiază simbolic demnitatea celor ce au susținut adesea luptele de curaj locale.", false, false),
        TouristSpot(43005, "Esplanada Pietonală", "Alexandria", 43.9705, 25.3300, 45, "Fâșie cu pomi stradali ce invită la pasaje relaxante și discuții la cafenele cu farmec.", true, false)
    )

    // Bus Stations for Vaslui
    val VASLUI_STATIONS = listOf(
        BusStation("VS_GARA", "Gara Vaslui", 46.6340, 27.7285),
        BusStation("VS_CENTRU", "Piața Civică", 46.6365, 27.7320),
        BusStation("VS_PARC", "Parcul Copou", 46.6415, 27.7350)
    )

    val VASLUI_LINES = listOf(
        BusLine("Traseul 3", "#14B8A6", listOf(VASLUI_STATIONS[0], VASLUI_STATIONS[1], VASLUI_STATIONS[2]))
    )

    val VASLUI_PRESETS = listOf(
        TouristSpot(44001, "Parcul Copou", "Vaslui", 46.6415, 27.7350, 90, "Unul dintre cele mai minunate repere din Moldova cu arbori seculari enormi și relaxanți.", true, false),
        TouristSpot(44002, "Piața Civică", "Vaslui", 46.6365, 27.7320, 60, "Zonă modernizată imensă și superbă străjuită masiv de clădiri mari importante vizual.", true, false),
        TouristSpot(44003, "Curtea Domnească", "Vaslui", 46.6385, 27.7300, 60, "Zonă istorică asezată peste numeroase ruine ale lui Ștefan Cel Mare, atracție unică.", true, false),
        TouristSpot(44004, "Muzeul Ștefan cel Mare", "Vaslui", 46.6375, 27.7310, 90, "Conține excepționale probe arheologice și artefacte de valoare ale luptelor moldave.", true, false),
        TouristSpot(44005, "Ansamblul Podul Înalt", "Vaslui", 46.5400, 27.7900, 90, "Locația uimitoarei și glorioasei batalii purtate istoric cu vitejie extremă de la 1475.", false, false)
    )

    // Bus Stations for Hunedoara
    val HUNEDOARA_STATIONS = listOf(
        BusStation("HD_GARA", "Gara Hunedoara", 45.7510, 22.9050),
        BusStation("HD_CASTEL", "Castelul Corvinilor", 45.7485, 22.8885),
        BusStation("HD_CENTRU", "Centrul Vechi", 45.7505, 22.8950)
    )

    val HUNEDOARA_LINES = listOf(
        BusLine("Linia 2", "#3B82F6", listOf(HUNEDOARA_STATIONS[0], HUNEDOARA_STATIONS[2], HUNEDOARA_STATIONS[1]))
    )

    val HUNEDOARA_PRESETS = listOf(
        TouristSpot(45001, "Castelul Corvinilor", "Hunedoara", 45.7490, 22.8880, 120, "Unul dintre cele mai impresionante castele gotice din Europa de Est, fascinant.", true, false),
        TouristSpot(45002, "Pietonala Corvin", "Hunedoara", 45.7525, 22.8980, 45, "Alee spectaculoasă la picior bogată în decorațiuni și statui relaxante pentru vizitatori.", true, false),
        TouristSpot(45003, "Combinatul Siderurgic", "Hunedoara", 45.7550, 22.9030, 45, "Porțile mastodontului amintind de zilele de glorie a metalurgiei vechi de fier din oraș.", false, false),
        TouristSpot(45004, "Muzeul de Trenulețe", "Hunedoara", 45.7515, 22.9000, 60, "O uimitoare miniatură excelent elaborată care animă viața rurală și urbană feroviară.", true, false),
        TouristSpot(45005, "Catedrala Eroilor", "Hunedoara", 45.7533, 22.8966, 30, "Construcție religioasă impozantă perfect localizată în vecinătatea plimbărilor centrale.", false, false)
    )

    // Bus Stations for Turda
    val TURDA_STATIONS = listOf(
        BusStation("TRD_GARA", "Gara Turda", 46.5685, 23.8205),
        BusStation("TRD_CENTRU", "Centru Turda", 46.5720, 23.7820),
        BusStation("TRD_SALINA", "Salina Turda", 46.5880, 23.7875)
    )

    val TURDA_LINES = listOf(
        BusLine("Linia 10", "#8B5CF6", listOf(TURDA_STATIONS[0], TURDA_STATIONS[1], TURDA_STATIONS[2]))
    )

    val TURDA_PRESETS = listOf(
        TouristSpot(46001, "Salina Turda", "Turda", 46.5880, 23.7875, 120, "O mină de sare subterană absolut uluitoare unică vizual la nivel global, cu lifturi imense.", true, false),
        TouristSpot(46002, "Cheile Turzii", "Turda", 46.5620, 23.6820, 120, "O rezervație minunată de defileu cu drumeție deasupra potecilor incredibile de piatră.", true, false),
        TouristSpot(46003, "Castrul Roman Potaissa", "Turda", 46.5750, 23.7745, 60, "Situl spectaculos ce arată baza celei mai importante legiuni romane a Macedonicii.", false, false),
        TouristSpot(46004, "Muzeul de Istorie Turda", "Turda", 46.5700, 23.7820, 60, "Situat în superbul palat princiar conținând o pictură murală uriașă și piese fabuloase.", false, false),
        TouristSpot(46005, "Mormântul lui Mihai Viteazul", "Turda", 46.5450, 23.7780, 45, "Așezământul memorial respectat pe colina unde trupul neînfricatului voievod a căzut.", true, false)
    )

    // Bus Stations for Mangalia
    val MANGALIA_STATIONS = listOf(
        BusStation("MGL_GARA", "Gara Mangalia", 43.8185, 28.5830),
        BusStation("MGL_PORT", "Portul Turistic Mangalia", 43.8130, 28.5840),
        BusStation("MGL_PLAJA", "Plaja Mangalia", 43.8135, 28.5875)
    )

    val MANGALIA_LINES = listOf(
        BusLine("MiniBus Plajă", "#06B6D4", listOf(MANGALIA_STATIONS[0], MANGALIA_STATIONS[1], MANGALIA_STATIONS[2]))
    )

    val MANGALIA_PRESETS = listOf(
        TouristSpot(47001, "Portul Turistic", "Mangalia", 43.8130, 28.5840, 60, "Un port modern, curat și animat pentru plimbări și iahturi dincolo de sudul însorit.", true, false),
        TouristSpot(47002, "Moscheea Esmahan Sultan", "Mangalia", 43.8125, 28.5815, 30, "Cel mai vechi locaș musulman unic existent din România, superb protejat de istorie.", true, false),
        TouristSpot(47003, "Muzeul Callatis", "Mangalia", 43.8145, 28.5835, 60, "Expoziție esențială referitoare remarcabil la vechea civilizație greacă ce fonda cetatea.", false, false),
        TouristSpot(47004, "Herghelia Mangalia", "Mangalia", 43.8400, 28.5700, 60, "Casa faimoșilor cai pur sânge arab care surprinde constant vizitatorii prin măreția sa.", true, false),
        TouristSpot(47005, "Plaja Mangalia", "Mangalia", 43.8135, 28.5875, 120, "Fâșia extremă la Marea Neagră perfectă sub soarele de foc cu o textură fină de nisip.", true, false)
    )

    // Bus Stations for Bușteni
    val BUSTENI_STATIONS = listOf(
        BusStation("BS_GARA", "Gara Bușteni", 45.4140, 25.5395),
        BusStation("BS_CENTRU", "Centru Bușteni", 45.4150, 25.5385),
        BusStation("BS_TELE", "Telecabina Bușteni", 45.4095, 25.5360)
    )

    val BUSTENI_LINES = listOf(
        BusLine("Linia Panoramică", "#10B981", listOf(BUSTENI_STATIONS[0], BUSTENI_STATIONS[1], BUSTENI_STATIONS[2]))
    )

    val BUSTENI_PRESETS = listOf(
        TouristSpot(48001, "Castelul Cantacuzino", "Bușteni", 45.4145, 25.5420, 90, "Edificiul superb creat de Nababul neo-românesc cu panorame fantastice la munții masivi.", true, false),
        TouristSpot(48002, "Sfinxul și Babele", "Bușteni", 45.4095, 25.5360, 120, "Acest punct te teleportează fix pe platoul spectaculos marcând formele sacre uimitoare.", true, false),
        TouristSpot(48003, "Crucea Caraiman", "Bușteni", 45.4160, 25.4975, 120, "Monumental omagial pe creastă, o călătorie epuizantă mental cu răsplată incredibilă vizual.", false, false),
        TouristSpot(48004, "Mănăstirea Caraiman", "Bușteni", 45.4100, 25.5325, 45, "Lăcaș asezat absolut fabulos sub stâncile imense ale Văii liniștite ca meditație tăcută.", true, false),
        TouristSpot(48005, "Cascada Urlătoarea", "Bușteni", 45.3980, 25.5300, 60, "Drum montan calm de la margine de codru spre incredibilul șuvoi și picături de legendă.", true, false)
    )

    // Bus Stations for Curtea de Argeș
    val CURTEA_AR_STATIONS = listOf(
        BusStation("CA_GARA", "Gara Regală Curtea de Argeș", 45.1380, 24.6730),
        BusStation("CA_CENTRU", "Centrul Orașului", 45.1435, 24.6750),
        BusStation("CA_MANASTIRE", "Mănăstirea Curtea de Argeș", 45.1555, 24.6765)
    )

    val CURTEA_AR_LINES = listOf(
        BusLine("Linia Regală", "#F59E0B", listOf(CURTEA_AR_STATIONS[0], CURTEA_AR_STATIONS[1], CURTEA_AR_STATIONS[2]))
    )

    val CURTEA_AR_PRESETS = listOf(
        TouristSpot(49001, "Mănăstirea Curtea de Argeș", "Curtea de Argeș", 45.1555, 24.6765, 90, "Capodoperă minunată voievodală, așezământul sacru monumental definit de faimoasa legendă.", true, false),
        TouristSpot(49002, "Fântâna lui Manole", "Curtea de Argeș", 45.1545, 24.6765, 30, "Lângă faimoasa biserică izbucnește fântâna uimitoare dedicată geniului zidar sacrificat.", true, false),
        TouristSpot(49003, "Gara Regală", "Curtea de Argeș", 45.1380, 24.6730, 30, "Ultimul stop fastuos preluat regal pentru regii suverani construind țara, un tezaur mic.", true, false),
        TouristSpot(49004, "Biserica Domnească Sfântul Nicolae", "Curtea de Argeș", 45.1425, 24.6775, 45, "Biserică extrem de istorică monumentală de la fondarea valahă cu picturi masiv pătate.", false, false),
        TouristSpot(49005, "Ruinele Sân Nicoară", "Curtea de Argeș", 45.1430, 24.6790, 30, "Situl cetății de zid sfâșiate, amintind de puterea bisericii impunătoare odată falnică.", false, false)
    )

    // Bus Stations for Gura Humorului
    val GURA_HUMORULUI_STATIONS = listOf(
        BusStation("GH_GARA", "Gara Gura Humorului", 47.5505, 25.8820),
        BusStation("GH_CENTRU", "Centru Gura Humorului", 47.5535, 25.8885),
        BusStation("GH_VORONET", "Mănăstirea Voroneț", 47.5175, 25.8640)
    )

    val GURA_HUMORULUI_LINES = listOf(
        BusLine("Linia Voroneț", "#3B82F6", listOf(GURA_HUMORULUI_STATIONS[0], GURA_HUMORULUI_STATIONS[1], GURA_HUMORULUI_STATIONS[2]))
    )

    val GURA_HUMORULUI_PRESETS = listOf(
        TouristSpot(50001, "Mănăstirea Voroneț", "Gura Humorului", 47.5175, 25.8640, 120, "Capela sixtină a Estului, faimoasă pur și simplu global prin albastrul ei formidabil.", true, false),
        TouristSpot(50002, "Parcul Ariniș", "Gura Humorului", 47.5450, 25.8845, 90, "Zona vastă magnifică perfectă extrem de relaxantă la râu și trasee variate verzi.", true, false),
        TouristSpot(50003, "Pârtia Șoimul", "Gura Humorului", 47.5410, 25.8860, 120, "Pârtie abruptă și senzațională unde primești perspectivă superioară la telescaunul vizionării pitorești.", false, false),
        TouristSpot(50004, "Muzeul Obiceiurilor Populare", "Gura Humorului", 47.5535, 25.8880, 60, "O fărâmă incredibil detaliată prezentând traiul bucovinean autentic respectat din zona extremului nord.", false, false),
        TouristSpot(50005, "Mănăstirea Humor", "Gura Humorului", 47.5935, 25.8565, 90, "Un frate superb de zid al mănăstirilor colorate bucovinene, dominat absolut formidabil în peisaj.", true, false)
    )

    // Bus Stations for Vatra Dornei
    val VATRA_DORNEI_STATIONS = listOf(
        BusStation("VD_GARA", "Gara Vatra Dornei", 47.3450, 25.3550),
        BusStation("VD_CENTRU", "Centru Vatra Dornei", 47.3465, 25.3585),
        BusStation("VD_PARC", "Parcul Central", 47.3480, 25.3530)
    )

    val VATRA_DORNEI_LINES = listOf(
        BusLine("Minibus Dorna", "#8B5CF6", listOf(VATRA_DORNEI_STATIONS[0], VATRA_DORNEI_STATIONS[1], VATRA_DORNEI_STATIONS[2]))
    )

    val VATRA_DORNEI_PRESETS = listOf(
        TouristSpot(51001, "Parcul Central", "Vatra Dornei", 47.3480, 25.3530, 90, "Zonă de poveste liniștită a resortului plină de brazi gigantici și simpaticele veverițe mereu prezente.", true, false),
        TouristSpot(51002, "Cazinoul Vatra Dornei", "Vatra Dornei", 47.3475, 25.3540, 45, "Ruină impunătoare a epocii balneare amintind nostalgic de fastul uimitor odinioară faimos.", true, false),
        TouristSpot(51003, "Pârtia Dealu Negru", "Vatra Dornei", 47.3400, 25.3400, 120, "Domeniu schiabil vast perfect pentru coborâri uriașe admirând toată zarea din înălțimi muncești.", false, false),
        TouristSpot(51004, "Muzeul de Științele Naturii", "Vatra Dornei", 47.3460, 25.3570, 60, "O colecție spectaculoasă surprinzând ecosistemul carpatic excelent păstrat având detalii fascinante autohtone.", false, false),
        TouristSpot(51005, "Izvoarele Minerale", "Vatra Dornei", 47.3485, 25.3555, 30, "Ape recunoscute extrem național aducătoare continuu și miraculos de sănătate bucovineană absolută.", true, false)
    )

    // Bus Stations for Sovata
    val SOVATA_STATIONS = listOf(
        BusStation("SOV_GARA", "Gara Sovata", 46.5940, 25.0640),
        BusStation("SOV_CENTRU", "Centru Sovata", 46.5960, 25.0760),
        BusStation("SOV_URSU", "Lacul Ursu", 46.6025, 25.0830)
    )

    val SOVATA_LINES = listOf(
        BusLine("Linia Lacuri", "#10B981", listOf(SOVATA_STATIONS[0], SOVATA_STATIONS[1], SOVATA_STATIONS[2]))
    )

    val SOVATA_PRESETS = listOf(
        TouristSpot(52001, "Lacul Ursu", "Sovata", 46.6025, 25.0830, 120, "Lac helioterm fabulos cu saramură unică masiv încălzită ideal cu un aspect uriaș verde.", true, false),
        TouristSpot(52002, "Mocănița Sovata", "Sovata", 46.5935, 25.0655, 120, "Cursa pe ecartament îngust spectaculoasă prin munți pufnind cu aburi clasici perfect calmi și fericiți.", true, false),
        TouristSpot(52003, "Muntele de Sare", "Sovata", 46.6050, 25.0880, 90, "Fenomenul uluitor geologic săpat de ape ce a scos perfect relieful ciudat și minunat izolat.", true, false),
        TouristSpot(52004, "Lacul Aluniș", "Sovata", 46.6010, 25.0845, 60, "Sora mică adesea alăturată ursului oferind mediu absolut reconfortant cu tratament benefic.", false, false),
        TouristSpot(52005, "Turnul de Belvedere", "Sovata", 46.6070, 25.0900, 45, "Urcuș minunat dominând așezarea panoramic de unde poți observa valea calmă incredibil de departe.", false, false)
    )

    // Bus Stations for Băile Felix
    val BAILE_FELIX_STATIONS = listOf(
        BusStation("BF_GARA", "Gara Băile Felix", 46.9850, 21.9800),
        BusStation("BF_CENTRU", "Centru Băile Felix", 46.9875, 21.9825),
        BusStation("BF_NUFERI", "Lacul cu Nuferi", 46.9900, 21.9855)
    )

    val BAILE_FELIX_LINES = listOf(
        BusLine("Traseul Termal", "#F59E0B", listOf(BAILE_FELIX_STATIONS[0], BAILE_FELIX_STATIONS[1], BAILE_FELIX_STATIONS[2]))
    )

    val BAILE_FELIX_PRESETS = listOf(
        TouristSpot(53001, "Lacul cu Nuferi", "Băile Felix", 46.9900, 21.9855, 60, "Microclimat fenomenal unic de basm cu celebre și fragile flori plutitoare tropicale uimitoare.", true, false),
        TouristSpot(53002, "Ștrandul Apollo", "Băile Felix", 46.9880, 21.9840, 120, "Ape miraculos termale deschise și adorate masiv de mulțimile dornice tot anul absolut constant.", true, false),
        TouristSpot(53003, "Biserica de Lemn din Brusturi", "Băile Felix", 46.9920, 21.9810, 30, "Lăcaș asezat autentic de istorie cu arhitectură clasică fermecătoare din vechile tradiții locale maramuresene.", false, false),
        TouristSpot(53004, "Rezervația cu Nuferi", "Băile Felix", 46.9895, 21.9865, 45, "Habitatul lărgit cald protejat natural atrăgător continuând senzația din jur și plimbarea pitoreasca.", false, false),
        TouristSpot(53005, "Pădurea Felix", "Băile Felix", 46.9830, 21.9900, 90, "Fâșia vastă plină și umbroasă extrem indicată de doctori ca traseu aerat minunat zilnic.", true, false)
    )

    // Bus Stations for Slănic Moldova
    val SLANIC_MOLDOVA_STATIONS = listOf(
        BusStation("SM_GARA", "Gara Slănic Moldova", 46.2100, 26.4400),
        BusStation("SM_CENTRU", "Centru Slănic Moldova", 46.2080, 26.4380),
        BusStation("SM_PARC", "Parcul Central", 46.2060, 26.4350)
    )

    val SLANIC_MOLDOVA_LINES = listOf(
        BusLine("Linia Izvoare", "#EC4899", listOf(SLANIC_MOLDOVA_STATIONS[0], SLANIC_MOLDOVA_STATIONS[1], SLANIC_MOLDOVA_STATIONS[2]))
    )

    val SLANIC_MOLDOVA_PRESETS = listOf(
        TouristSpot(54001, "Traseul Izvoarelor", "Slănic Moldova", 46.2030, 26.4300, 90, "Celebrele miracole de stâncă gustate de zeci și recomandate extrem ca purificatoare masive mereu.", true, false),
        TouristSpot(54002, "Cazinoul Slănic Moldova", "Slănic Moldova", 46.2075, 26.4370, 45, "Monumentul principal balnear impunător în perfect stil românesc ce tronează superb la poarta parcului.", true, false),
        TouristSpot(54003, "Parcul Central", "Slănic Moldova", 46.2060, 26.4350, 60, "Oaza principală liniștită dominând peisajul turistic cu arbori groși absolut uriași și foișoare muzicale.", true, false),
        TouristSpot(54004, "Muntele Pufu", "Slănic Moldova", 46.1950, 26.4400, 120, "Vârful excelent cucerit vizionând panorama largă incredibil de vastă printre superbele creste ale Nemirei.", false, false),
        TouristSpot(54005, "Cascada Slănicului", "Slănic Moldova", 46.2010, 26.4250, 45, "Șuvoiul natural gălăgios rece atrăgător mereu și spargător fermecător peste toate treptele râuletului de sus.", true, false)
    )

    // Bus Stations for Băile Herculane
    val BAILE_HERCULANE_STATIONS = listOf(
        BusStation("BH_GARA", "Gara Băile Herculane", 44.8780, 22.4160),
        BusStation("BH_CENTRU", "Centru Băile Herculane", 44.8750, 22.4130),
        BusStation("BH_TERMAL", "Băile Termale", 44.8720, 22.4100)
    )

    val BAILE_HERCULANE_LINES = listOf(
        BusLine("Linia Hercules", "#EA580C", listOf(BAILE_HERCULANE_STATIONS[0], BAILE_HERCULANE_STATIONS[1], BAILE_HERCULANE_STATIONS[2]))
    )

    val BAILE_HERCULANE_PRESETS = listOf(
        TouristSpot(55001, "Statuia lui Hercules", "Băile Herculane", 44.8785, 22.4170, 30, "Simbolul incontestabil masiv ce veghează peisajul stațiunii imperiale având o forță mitică extremă.", true, false),
        TouristSpot(55002, "Băile Imperiale Austriece", "Băile Herculane", 44.8760, 22.4140, 90, "Ansamblu masiv absolut uluitor dominând arhitectura istorică rămasă cu iz de lux habsburgic cert.", true, false),
        TouristSpot(55003, "Valea Cernei", "Băile Herculane", 44.8850, 22.4200, 120, "Defileu impresionant imens săpat de apă formând cascade uriașe și stânci imense abrupt așezate.", true, false),
        TouristSpot(55004, "Grota Haiducilor", "Băile Herculane", 44.8820, 22.4180, 60, "Peșteră misterioasă adâncă ascunzând povești vechi fantastice despre hoți români extrem de viteji mereu.", false, false),
        TouristSpot(55005, "Crucea Albă", "Băile Herculane", 44.8700, 22.4080, 90, "Locație la mare înălțime vizuală permițând cuprinderea perfectă și incredibilă a stațiunii aflate jos.", false, false)
    )

    // Bus Stations for Călimănești
    val CALIMANESTI_STATIONS = listOf(
        BusStation("CAL_GARA", "Gara Călimănești", 45.2400, 24.3400),
        BusStation("CAL_CENTRU", "Centru Călimănești", 45.2425, 24.3425),
        BusStation("CAL_COZIA", "Mănăstirea Cozia", 45.2670, 24.3160)
    )

    val CALIMANESTI_LINES = listOf(
        BusLine("Linia Oltului", "#0284C7", listOf(CALIMANESTI_STATIONS[0], CALIMANESTI_STATIONS[1], CALIMANESTI_STATIONS[2]))
    )

    val CALIMANESTI_PRESETS = listOf(
        TouristSpot(56001, "Mănăstirea Cozia", "Călimănești", 45.2670, 24.3160, 90, "Ctitoria glorioasă voievodală absolut imensă și impunătoare pe malul apei străjuită masiv prin ziduri.", true, false),
        TouristSpot(56002, "Castrul Roman Arutela", "Călimănești", 45.2750, 24.3100, 60, "Ruine faimoase milenare așezate calm lăsând vizibil o mare organizare antică imperială din munți.", true, false),
        TouristSpot(56003, "Parcul Central Călimănești", "Călimănești", 45.2440, 24.3450, 60, "Grădini largi superbe permițând pacienților pași excepționali printre fântâni termale frumos artistic amenajate mereu.", true, false),
        TouristSpot(56004, "Mănăstirea Turnu", "Călimănești", 45.2800, 24.3050, 90, "Așezământ cu grote mici săpate în piatră aflate liniștit retras departe sub stânci magnifice impunătoare.", false, false),
        TouristSpot(56005, "Izvoarele Termale", "Călimănești", 45.2420, 24.3410, 45, "Puncte calde minerale oferind direct din pământ sursa uriașă tămăduitoare a acestei faimoase văi oltenești.", false, false)
    )

    // Bus Stations for Borsec
    val BORSEC_STATIONS = listOf(
        BusStation("BOR_GARA", "Gara Borsec", 46.9660, 25.5700),
        BusStation("BOR_CENTRU", "Centru Borsec", 46.9680, 25.5730),
        BusStation("BOR_IZVOARE", "Izvoarele Borsec", 46.9700, 25.5750)
    )

    val BORSEC_LINES = listOf(
        BusLine("Traseul Apelor", "#6366F1", listOf(BORSEC_STATIONS[0], BORSEC_STATIONS[1], BORSEC_STATIONS[2]))
    )

    val BORSEC_PRESETS = listOf(
        TouristSpot(57001, "Poiana Zânelor", "Borsec", 46.9720, 25.5780, 120, "Rezervație extrem de faimoasă complexă uluitoare prin aleea de tratament având băi masiv carstice terapeutice.", true, false),
        TouristSpot(57002, "Izvorul Principal", "Borsec", 46.9700, 25.5750, 45, "Sursa originară glorioasă cu renume internațional gustată intens apreciată peste tot în vasta Europă modernă.", true, false),
        TouristSpot(57003, "Peștera de Gheață", "Borsec", 46.9750, 25.5800, 60, "Grota obscură uimitoare ce păstrează bucăți solide mari de îngheț tăcut absolut pe tot anul complet.", true, false),
        TouristSpot(57004, "Grota Urșilor", "Borsec", 46.9780, 25.5820, 60, "Formațiune de pietre colosale prăbușite uriaș formând trecători înguste fascinante pentru curajoșii aventurieri montani mari.", false, false),
        TouristSpot(57005, "Pârtia Speranța", "Borsec", 46.9640, 25.5680, 120, "Domeniu montan atrăgător oferind rapid iubitorilor alb pante pitoresti foarte sigure frumos organizate vizual comod.", false, false)
    )

    // Bus Stations for Băile Govora
    val BAILE_GOVORA_STATIONS = listOf(
        BusStation("BG_GARA", "Gara Băile Govora", 45.0800, 24.1800),
        BusStation("BG_CENTRU", "Centru Băile Govora", 45.0830, 24.1830),
        BusStation("BG_PARC", "Parcul Balnear", 45.0860, 24.1860)
    )

    val BAILE_GOVORA_LINES = listOf(
        BusLine("Linia Parc", "#8B5CF6", listOf(BAILE_GOVORA_STATIONS[0], BAILE_GOVORA_STATIONS[1], BAILE_GOVORA_STATIONS[2]))
    )

    val BAILE_GOVORA_PRESETS = listOf(
        TouristSpot(58001, "Parcul Balnear", "Băile Govora", 45.0860, 24.1860, 120, "Imensă amenajare unică istorică având pomi diverși extraordinari oferind o atmosferă complet purificată din aer liniștit.", true, false),
        TouristSpot(58002, "Izvoarele Minerale Govora", "Băile Govora", 45.0850, 24.1840, 60, "Surse spectaculoase cu componente bogate rare ce aduc rezolvări fantastice apreciate intens și cu rezultate dovedite.", true, false),
        TouristSpot(58003, "Hotelul Palace", "Băile Govora", 45.0840, 24.1830, 45, "Clădire glorioasă mare construită genial cu uși uriașe captând absolut toată lumina prin ferestre perfecte de designer.", true, false),
        TouristSpot(58004, "Mănăstirea Govora", "Băile Govora", 45.0500, 24.1500, 90, "Locație medievală remarcabilă faimoasă prin prima carte masiv tipărită cultural aducând imensă iluminare istorică așezământului muntean.", false, false),
        TouristSpot(58005, "Mănăstirea Dintr-un Lemn", "Băile Govora", 45.0400, 24.2000, 60, "Bisericuță miraculoasă unică zămislită ingenios legendar făcută toată masiv având obârșia simplă dintr-un singur arbore masiv gorun.", false, false)
    )

    // Bus Stations for Câmpulung Moldovenesc
    val CAMPULUNG_MOLDOVENESC_STATIONS = listOf(
        BusStation("CPM_GARA", "Gara Câmpulung Moldovenesc", 47.5300, 25.5500),
        BusStation("CPM_CENTRU", "Centru Câmpulung Mold.", 47.5320, 25.5530),
        BusStation("CPM_PARC", "Parcul Central", 47.5340, 25.5550)
    )

    val CAMPULUNG_MOLDOVENESC_LINES = listOf(
        BusLine("Linia Moldovei", "#14B8A6", listOf(CAMPULUNG_MOLDOVENESC_STATIONS[0], CAMPULUNG_MOLDOVENESC_STATIONS[1], CAMPULUNG_MOLDOVENESC_STATIONS[2]))
    )

    val CAMPULUNG_MOLDOVENESC_PRESETS = listOf(
        TouristSpot(59001, "Muzeul Arta Lemnului", "Câmpulung Moldovenesc", 47.5325, 25.5535, 90, "Expoziție magistrală uriașă dezvăluind cultura meșterilor absolut fascinanți locali ce modelau viața magnific din copacii gigantici.", true, false),
        TouristSpot(59002, "Masivul Rarău", "Câmpulung Moldovenesc", 47.4500, 25.5600, 150, "Crestete ascuțite imense spectaculoase formând peisaj extrem uluitor urcând faimoasa zonă montană deosebit curată.", true, false),
        TouristSpot(59003, "Pietrele Doamnei", "Câmpulung Moldovenesc", 47.4550, 25.5650, 120, "Stânci gotice legendare tăiate ascuțit înălțându-se fermecător având un misterios profil de povești faimoase și regale uitate.", true, false),
        TouristSpot(59004, "Muzeul Lingurilor de Lemn", "Câmpulung Moldovenesc", 47.5335, 25.5555, 60, "Colecția intimă senzațională prezentând o diversitate masivă colosală adunând fantezii creative artizanale impecabil sculptate uluitor detaliat.", false, false),
        TouristSpot(59005, "Schitul Rarău", "Câmpulung Moldovenesc", 47.4600, 25.5700, 60, "Popas spiritual extrem pur trăit și cald situat frumos așezat magnific printre cele mai aspre stânci de munte.", false, false)
    )

    // --- NEW MULTI-MODAL Transit Network Definitions ---

    // Bucharest Extra Modes (Metro, Trolley, Train)
    val BUH_METRO_STATIONS = listOf(
        BusStation("MET_VICTORIEI", "Metrou Piața Victoriei", 44.4526, 26.0861),
        BusStation("MET_ROMANA", "Metrou Piața Romană", 44.4446, 26.0976),
        BusStation("MET_UNIRII", "Metrou Piața Unirii", 44.4266, 26.1026),
        BusStation("MET_TINERETULUI", "Metrou Tineretului", 44.4082, 26.1042),
        BusStation("MET_PARLAMENT", "Metrou Izvor/Palat", 44.4313, 26.0901)
    )

    val BUH_TROLLEY_STATIONS = listOf(
        BusStation("TRO_GARA", "Troleibuz Gara de Nord", 44.4469, 26.0726),
        BusStation("TRO_ROMANA", "Troleibuz Piața Romană", 44.4444, 26.0974),
        BusStation("TRO_ARENA", "Troleibuz Arena Națională", 44.4373, 26.1526)
    )

    val BUH_TRAIN_STATIONS = listOf(
        BusStation("TRN_GARA", "Tren Gara de Nord", 44.4467, 26.0724),
        BusStation("TRN_HERASTRAU", "Tren Halta Herăstrău", 44.4716, 26.0816),
        BusStation("TRN_THERME", "Tren Halta Balotești (Therme)", 44.6051, 26.0791)
    )

    val BUH_NEW_LINES = listOf(
        BusLine("Metrou M2", "#3B82F6", BUH_METRO_STATIONS, LegType.METRO),
        BusLine("Troleibuz 86", "#14B8A6", BUH_TROLLEY_STATIONS, LegType.TROLLEY),
        BusLine("Tren Regional T1", "#8B5CF6", BUH_TRAIN_STATIONS, LegType.TRAIN)
    )

    // Cluj Extra Modes (Metro Cluj, Trolley, Train)
    val CLJ_METRO_STATIONS = listOf(
        BusStation("MET_CLJ_FLORESTI", "Metrou Florești", 46.7321, 23.4911),
        BusStation("MET_CLJ_MANASTUR", "Metrou Mănăștur (Calvaria)", 46.7589, 23.5571),
        BusStation("MET_CLJ_CENTRAL", "Metrou Piața Unirii", 46.7690, 23.5899),
        BusStation("MET_CLJ_M_VITEAZUL", "Metrou Piața Mihai Viteazul", 46.7746, 23.5893),
        BusStation("MET_CLJ_IULIUS", "Metrou Iulius Park", 46.7726, 23.6236)
    )

    val CLJ_TROLLEY_STATIONS = listOf(
        BusStation("TRO_CLJ_BUCIUM", "Troleibuz Bucium", 46.7562, 23.5590),
        BusStation("TRO_CLJ_MEMO", "Troleibuz Memorandului", 46.7685, 23.5863),
        BusStation("TRO_CLJ_OPERA", "Troleibuz Piața Avram Iancu", 46.7699, 23.5956)
    )

    val CLJ_TRAIN_STATIONS = listOf(
        BusStation("TRN_CLJ_GARA", "Tren Gara Cluj", 46.7866, 23.5876),
        BusStation("TRN_CLJ_EAST", "Tren Cluj Est (Someșeni)", 46.7767, 23.6067)
    )

    val CLJ_NEW_LINES = listOf(
        BusLine("Metrou Cluj M1 (Simulat)", "#EF4444", CLJ_METRO_STATIONS, LegType.METRO),
        BusLine("Troleibuz 6", "#10B981", CLJ_TROLLEY_STATIONS, LegType.TROLLEY),
        BusLine("Tren Regional Tetarom", "#EC4899", CLJ_TRAIN_STATIONS, LegType.TRAIN)
    )

    // Brașov Extra Modes (Trolley, Train)
    val SBV_TROLLEY_STATIONS = listOf(
        BusStation("TRO_SBV_GARA", "Troleibuz Gara Brașov", 45.6612, 25.6112),
        BusStation("TRO_SBV_LIVADA", "Troleibuz Livada Poștei", 45.6436, 25.5926)
    )

    val SBV_TRAIN_STATIONS = listOf(
        BusStation("TRN_SBV_GARA", "Tren Gara Centrală", 45.6610, 25.6110),
        BusStation("TRN_SBV_BARTOLOMEU", "Tren Halta Bartolomeu", 45.6620, 25.5680)
    )

    val SBV_NEW_LINES = listOf(
        BusLine("Troleibuz 2", "#14B8A6", SBV_TROLLEY_STATIONS, LegType.TROLLEY),
        BusLine("Tren Regio T3", "#8B5CF6", SBV_TRAIN_STATIONS, LegType.TRAIN)
    )

    // Câmpina Extra Modes (Trolley, Train)
    val CMP_TROLLEY_STATIONS = listOf(
        BusStation("TRO_CMP_AUTOGARA", "Electric Bus Autogară", 45.1246, 25.7186),
        BusStation("TRO_CMP_CENTRAL", "Electric Bus Casa de Cultură", 45.1266, 25.7346)
    )

    val CMP_TRAIN_STATIONS = listOf(
        BusStation("TRN_CMP_GARA", "Tren Gara Câmpina", 45.1190, 25.7079),
        BusStation("TRN_CMP_POIANA", "Tren Halta Poiana Câmpina", 45.1230, 25.6980)
    )

    val CMP_NEW_LINES = listOf(
        BusLine("Troleibuz Ecologic E1", "#F59E0B", CMP_TROLLEY_STATIONS, LegType.TROLLEY),
        BusLine("Tren Regional Poiana", "#8B5CF6", CMP_TRAIN_STATIONS, LegType.TRAIN)
    )

    // Dynamic cities registry
    val DYNAMIC_STATIONS = java.util.concurrent.ConcurrentHashMap<String, List<BusStation>>()
    val DYNAMIC_LINES = java.util.concurrent.ConcurrentHashMap<String, List<BusLine>>()
    val DYNAMIC_START_SPOTS = java.util.concurrent.ConcurrentHashMap<String, TouristSpot>()
    val DYNAMIC_BOUNDS = java.util.concurrent.ConcurrentHashMap<String, CityBounds>()

    // Default Starting places based on city
    fun getStartSpot(city: String): TouristSpot {
        val dynamic = DYNAMIC_START_SPOTS[city]
        if (dynamic != null) return dynamic

        return when (city) {
            "București" -> TouristSpot(-1, "Gara de Nord (Hotel/Start)", "București", 44.4468, 26.0725, 0, "Punctul de pornire al călătoriei.", false, false)
            "Brașov" -> TouristSpot(-1, "Gara Brașov (Hotel/Start)", "Brașov", 45.6611, 25.6111, 0, "Punctul de pornire al călătoriei.", false, false)
            "Câmpina" -> TouristSpot(-1, "Gara Câmpina (Hotel/Start)", "Câmpina", 45.1189, 25.7078, 0, "Punctul de pornire al călătoriei.", false, false)
            "Sinaia" -> TouristSpot(-1, "Gara Sinaia (Hotel/Start)", "Sinaia", 45.3510, 25.5539, 0, "Punctul de pornire al călătoriei.", false, false)
            "Sibiu" -> TouristSpot(-1, "Gara Sibiu (Hotel/Start)", "Sibiu", 45.8016, 24.1614, 0, "Punctul de pornire al călătoriei.", false, false)
            "Sighișoara" -> TouristSpot(-1, "Gara Sighișoara (Hotel/Start)", "Sighișoara", 46.2238, 24.7981, 0, "Punctul de pornire al călătoriei.", false, false)
            "Constanța" -> TouristSpot(-1, "Gara Constanța (Hotel/Start)", "Constanța", 44.1678, 28.6366, 0, "Punctul de pornire al călătoriei.", false, false)
            "Iași" -> TouristSpot(-1, "Gara Iași (Hotel/Start)", "Iași", 47.1652, 27.5714, 0, "Punctul de pornire al călătoriei.", false, false)
            "Timișoara" -> TouristSpot(-1, "Gara Timișoara (Hotel/Start)", "Timișoara", 45.7505, 21.2014, 0, "Punctul de pornire al călătoriei.", false, false)
            "Oradea" -> TouristSpot(-1, "Gara Oradea (Hotel/Start)", "Oradea", 47.0682, 21.9421, 0, "Punctul de pornire al călătoriei.", false, false)
            "Alba Iulia" -> TouristSpot(-1, "Gara Alba Iulia (Hotel/Start)", "Alba Iulia", 46.0620, 23.5780, 0, "Punctul de pornire al călătoriei.", false, false)
            "Suceava" -> TouristSpot(-1, "Gara Suceava (Hotel/Start)", "Suceava", 47.6698, 26.2652, 0, "Punctul de pornire al călătoriei.", false, false)
            "Craiova" -> TouristSpot(-1, "Gara Craiova (Hotel/Start)", "Craiova", 44.3274, 23.8055, 0, "Punctul de pornire al călătoriei.", false, false)
            "Arad" -> TouristSpot(-1, "Gara Arad (Hotel/Start)", "Arad", 46.1895, 21.3255, 0, "Punctul de pornire al călătoriei.", false, false)
            "Galați" -> TouristSpot(-1, "Gara Galați (Hotel/Start)", "Galați", 45.4385, 28.0614, 0, "Punctul de pornire al călătoriei.", false, false)
            "Târgu Mureș" -> TouristSpot(-1, "Gara Târgu Mureș (Hotel/Start)", "Târgu Mureș", 46.5365, 24.5420, 0, "Punctul de pornire al călătoriei.", false, false)
            "Satu Mare" -> TouristSpot(-1, "Gara Satu Mare (Hotel/Start)", "Satu Mare", 47.7850, 22.8905, 0, "Punctul de pornire al călătoriei.", false, false)
            "Bacău" -> TouristSpot(-1, "Gara Bacău (Hotel/Start)", "Bacău", 46.5651, 26.9038, 0, "Punctul de pornire al călătoriei.", false, false)
            "Ploiești" -> TouristSpot(-1, "Gara Ploiești Sud (Hotel/Start)", "Ploiești", 44.9285, 26.0289, 0, "Punctul de pornire al călătoriei.", false, false)
            "Miercurea Ciuc" -> TouristSpot(-1, "Gara Miercurea Ciuc (Hotel/Start)", "Miercurea Ciuc", 46.3650, 25.7950, 0, "Punctul de pornire al călătoriei.", false, false)
            "Pitești" -> TouristSpot(-1, "Gara Pitești Sud (Hotel/Start)", "Pitești", 44.8465, 24.8820, 0, "Punctul de pornire al călătoriei.", false, false)
            "Brăila" -> TouristSpot(-1, "Gara Brăila (Hotel/Start)", "Brăila", 45.2750, 27.9450, 0, "Punctul de pornire al călătoriei.", false, false)
            "Baia Mare" -> TouristSpot(-1, "Gara Baia Mare (Hotel/Start)", "Baia Mare", 47.6533, 23.5516, 0, "Punctul de pornire al călătoriei.", false, false)
            "Bistrița" -> TouristSpot(-1, "Gara Bistrița Nord (Hotel/Start)", "Bistrița", 47.1265, 24.4855, 0, "Punctul de pornire al călătoriei.", false, false)
            "Târgoviște" -> TouristSpot(-1, "Gara Târgoviște (Hotel/Start)", "Târgoviște", 44.9198, 25.4665, 0, "Punctul de pornire al călătoriei.", false, false)
            "Tulcea" -> TouristSpot(-1, "Gara Tulcea (Hotel/Start)", "Tulcea", 45.1788, 28.8020, 0, "Punctul de pornire al călătoriei.", false, false)
            "Piatra Neamț" -> TouristSpot(-1, "Gara Piatra Neamț (Hotel/Start)", "Piatra Neamț", 46.9272, 26.3755, 0, "Punctul de pornire al călătoriei.", false, false)
            "Râmnicu Vâlcea" -> TouristSpot(-1, "Gara Râmnicu Vâlcea (Hotel/Start)", "Râmnicu Vâlcea", 45.1010, 24.3645, 0, "Punctul de pornire al călătoriei.", false, false)
            "Drobeta-Turnu Severin" -> TouristSpot(-1, "Gara Drobeta-Turnu Severin (Hotel/Start)", "Drobeta-Turnu Severin", 44.6225, 22.6515, 0, "Punctul de pornire al călătoriei.", false, false)
            "Deva" -> TouristSpot(-1, "Gara Deva (Hotel/Start)", "Deva", 45.8850, 22.9150, 0, "Punctul de pornire al călătoriei.", false, false)
            "Botoșani" -> TouristSpot(-1, "Gara Botoșani (Hotel/Start)", "Botoșani", 47.7475, 26.6535, 0, "Punctul de pornire al călătoriei.", false, false)
            "Sfântu Gheorghe" -> TouristSpot(-1, "Gara Sfântu Gheorghe (Hotel/Start)", "Sfântu Gheorghe", 45.8560, 25.7955, 0, "Punctul de pornire al călătoriei.", false, false)
            "Giurgiu" -> TouristSpot(-1, "Gara Giurgiu Oraș (Hotel/Start)", "Giurgiu", 43.8965, 25.9680, 0, "Punctul de pornire al călătoriei.", false, false)
            "Călărași" -> TouristSpot(-1, "Gara Călărași Sud (Hotel/Start)", "Călărași", 44.1950, 27.3200, 0, "Punctul de pornire al călătoriei.", false, false)
            "Slobozia" -> TouristSpot(-1, "Gara Slobozia Veche (Hotel/Start)", "Slobozia", 44.5650, 27.3680, 0, "Punctul de pornire al călătoriei.", false, false)
            "Zalău" -> TouristSpot(-1, "Gara Zalău Nord (Hotel/Start)", "Zalău", 47.2050, 23.0650, 0, "Punctul de pornire al călătoriei.", false, false)
            "Focșani" -> TouristSpot(-1, "Gara Focșani (Hotel/Start)", "Focșani", 45.6965, 27.1935, 0, "Punctul de pornire al călătoriei.", false, false)
            "Buzău" -> TouristSpot(-1, "Gara Buzău (Hotel/Start)", "Buzău", 45.1435, 26.8285, 0, "Punctul de pornire al călătoriei.", false, false)
            "Reșița" -> TouristSpot(-1, "Gara Reșița Sud (Hotel/Start)", "Reșița", 45.2935, 21.8905, 0, "Punctul de pornire al călătoriei.", false, false)
            "Târgu Jiu" -> TouristSpot(-1, "Gara Târgu Jiu (Hotel/Start)", "Târgu Jiu", 45.0350, 23.2790, 0, "Punctul de pornire al călătoriei.", false, false)
            "Slatina" -> TouristSpot(-1, "Gara Slatina (Hotel/Start)", "Slatina", 44.4285, 24.3685, 0, "Punctul de pornire al călătoriei.", false, false)
            "Alexandria" -> TouristSpot(-1, "Gara Alexandria (Hotel/Start)", "Alexandria", 43.9740, 25.3280, 0, "Punctul de pornire al călătoriei.", false, false)
            "Vaslui" -> TouristSpot(-1, "Gara Vaslui (Hotel/Start)", "Vaslui", 46.6340, 27.7285, 0, "Punctul de pornire al călătoriei.", false, false)
            "Hunedoara" -> TouristSpot(-1, "Gara Hunedoara (Hotel/Start)", "Hunedoara", 45.7510, 22.9050, 0, "Punctul de pornire al călătoriei.", false, false)
            "Turda" -> TouristSpot(-1, "Gara Turda (Hotel/Start)", "Turda", 46.5685, 23.8205, 0, "Punctul de pornire al călătoriei.", false, false)
            "Mangalia" -> TouristSpot(-1, "Gara Mangalia (Hotel/Start)", "Mangalia", 43.8185, 28.5830, 0, "Punctul de pornire al călătoriei.", false, false)
            "Bușteni" -> TouristSpot(-1, "Gara Bușteni (Hotel/Start)", "Bușteni", 45.4140, 25.5395, 0, "Punctul de pornire al călătoriei.", false, false)
            "Curtea de Argeș" -> TouristSpot(-1, "Gara Regală Curtea de Argeș (Hotel/Start)", "Curtea de Argeș", 45.1380, 24.6730, 0, "Punctul de pornire al călătoriei.", false, false)
            "Gura Humorului" -> TouristSpot(-1, "Gara Gura Humorului (Hotel/Start)", "Gura Humorului", 47.5505, 25.8820, 0, "Punctul de pornire al călătoriei.", false, false)
            "Vatra Dornei" -> TouristSpot(-1, "Gara Vatra Dornei (Hotel/Start)", "Vatra Dornei", 47.3450, 25.3550, 0, "Punctul de pornire al călătoriei.", false, false)
            "Sovata" -> TouristSpot(-1, "Gara Sovata (Hotel/Start)", "Sovata", 46.5940, 25.0640, 0, "Punctul de pornire al călătoriei.", false, false)
            "Băile Felix" -> TouristSpot(-1, "Gara Băile Felix (Hotel/Start)", "Băile Felix", 46.9850, 21.9800, 0, "Punctul de pornire al călătoriei.", false, false)
            "Slănic Moldova" -> TouristSpot(-1, "Gara Slănic Moldova (Hotel/Start)", "Slănic Moldova", 46.2100, 26.4400, 0, "Punctul de pornire al călătoriei.", false, false)
            "Băile Herculane" -> TouristSpot(-1, "Gara Băile Herculane (Hotel/Start)", "Băile Herculane", 44.8780, 22.4160, 0, "Punctul de pornire al călătoriei.", false, false)
            "Călimănești" -> TouristSpot(-1, "Gara Călimănești (Hotel/Start)", "Călimănești", 45.2400, 24.3400, 0, "Punctul de pornire al călătoriei.", false, false)
            "Borsec" -> TouristSpot(-1, "Gara Borsec (Hotel/Start)", "Borsec", 46.9660, 25.5700, 0, "Punctul de pornire al călătoriei.", false, false)
            "Băile Govora" -> TouristSpot(-1, "Gara Băile Govora (Hotel/Start)", "Băile Govora", 45.0800, 24.1800, 0, "Punctul de pornire al călătoriei.", false, false)
            "Câmpulung Moldovenesc" -> TouristSpot(-1, "Gara Câmpulung Moldovenesc (Hotel/Start)", "Câmpulung Moldovenesc", 47.5300, 25.5500, 0, "Punctul de pornire al călătoriei.", false, false)
            "Cluj-Napoca" -> TouristSpot(-1, "Gara Cluj-Napoca (Hotel/Start)", "Cluj-Napoca", 46.7865, 23.5875, 0, "Punctul de pornire al călătoriei.", false, false)
            else -> TouristSpot(-1, "Gara $city (Hotel/Start)", city, 46.7865, 23.5875, 0, "Punctul de pornire al călătoriei.", false, false)
        }
    }

    fun getStationsForCity(city: String): List<BusStation> {
        val dynamic = DYNAMIC_STATIONS[city]
        if (dynamic != null) return dynamic

        return when (city) {
            "București" -> BUCURESTI_STATIONS + BUH_METRO_STATIONS + BUH_TROLLEY_STATIONS + BUH_TRAIN_STATIONS
            "Brașov" -> BRASOV_STATIONS + SBV_TROLLEY_STATIONS + SBV_TRAIN_STATIONS
            "Câmpina" -> CAMPINA_STATIONS + CMP_TROLLEY_STATIONS + CMP_TRAIN_STATIONS
            "Sinaia" -> SINAIA_STATIONS
            "Sibiu" -> SIBIU_STATIONS
            "Sighișoara" -> SIGHISOARA_STATIONS
            "Constanța" -> CONSTANTA_STATIONS
            "Iași" -> IASI_STATIONS
            "Timișoara" -> TIMISOARA_STATIONS
            "Oradea" -> ORADEA_STATIONS
            "Alba Iulia" -> ALBA_IULIA_STATIONS
            "Suceava" -> SUCEAVA_STATIONS
            "Craiova" -> CRAIOVA_STATIONS
            "Arad" -> ARAD_STATIONS
            "Galați" -> GALATI_STATIONS
            "Târgu Mureș" -> TARGU_MURES_STATIONS
            "Satu Mare" -> SATU_MARE_STATIONS
            "Bacău" -> BACAU_STATIONS
            "Ploiești" -> PLOIESTI_STATIONS
            "Miercurea Ciuc" -> MIERCUREA_CIUC_STATIONS
            "Pitești" -> PITESTI_STATIONS
            "Brăila" -> BRAILA_STATIONS
            "Baia Mare" -> BAIA_MARE_STATIONS
            "Bistrița" -> BISTRITA_STATIONS
            "Târgoviște" -> TARGOVISTE_STATIONS
            "Tulcea" -> TULCEA_STATIONS
            "Piatra Neamț" -> PIATRA_NEAMT_STATIONS
            "Râmnicu Vâlcea" -> RAMNICU_VALCEA_STATIONS
            "Drobeta-Turnu Severin" -> DROBETA_STATIONS
            "Deva" -> DEVA_STATIONS
            "Botoșani" -> BOTOSANI_STATIONS
            "Sfântu Gheorghe" -> SFANTU_GHEORGHE_STATIONS
            "Giurgiu" -> GIURGIU_STATIONS
            "Călărași" -> CALARASI_STATIONS
            "Slobozia" -> SLOBOZIA_STATIONS
            "Zalău" -> ZALAU_STATIONS
            "Focșani" -> FOCSANI_STATIONS
            "Buzău" -> BUZAU_STATIONS
            "Reșița" -> RESITA_STATIONS
            "Târgu Jiu" -> TARGU_JIU_STATIONS
            "Slatina" -> SLATINA_STATIONS
            "Alexandria" -> ALEXANDRIA_STATIONS
            "Vaslui" -> VASLUI_STATIONS
            "Hunedoara" -> HUNEDOARA_STATIONS
            "Turda" -> TURDA_STATIONS
            "Mangalia" -> MANGALIA_STATIONS
            "Bușteni" -> BUSTENI_STATIONS
            "Curtea de Argeș" -> CURTEA_AR_STATIONS
            "Gura Humorului" -> GURA_HUMORULUI_STATIONS
            "Vatra Dornei" -> VATRA_DORNEI_STATIONS
            "Sovata" -> SOVATA_STATIONS
            "Băile Felix" -> BAILE_FELIX_STATIONS
            "Slănic Moldova" -> SLANIC_MOLDOVA_STATIONS
            "Băile Herculane" -> BAILE_HERCULANE_STATIONS
            "Călimănești" -> CALIMANESTI_STATIONS
            "Borsec" -> BORSEC_STATIONS
            "Băile Govora" -> BAILE_GOVORA_STATIONS
            "Câmpulung Moldovenesc" -> CAMPULUNG_MOLDOVENESC_STATIONS
            "Cluj-Napoca" -> CLUJ_STATIONS + CLJ_METRO_STATIONS + CLJ_TROLLEY_STATIONS + CLJ_TRAIN_STATIONS
            else -> emptyList()
        }
    }

    fun getLinesForCity(city: String): List<BusLine> {
        val dynamic = DYNAMIC_LINES[city]
        if (dynamic != null) return dynamic

        return when (city) {
            "București" -> BUCURESTI_LINES + BUH_NEW_LINES
            "Brașov" -> BRASOV_LINES + SBV_NEW_LINES
            "Câmpina" -> CAMPINA_LINES + CMP_NEW_LINES
            "Sinaia" -> SINAIA_LINES
            "Sibiu" -> SIBIU_LINES
            "Sighișoara" -> SIGHISOARA_LINES
            "Constanța" -> CONSTANTA_LINES
            "Iași" -> IASI_LINES
            "Timișoara" -> TIMISOARA_LINES
            "Oradea" -> ORADEA_LINES
            "Alba Iulia" -> ALBA_IULIA_LINES
            "Suceava" -> SUCEAVA_LINES
            "Craiova" -> CRAIOVA_LINES
            "Arad" -> ARAD_LINES
            "Galați" -> GALATI_LINES
            "Târgu Mureș" -> TARGU_MURES_LINES
            "Satu Mare" -> SATU_MARE_LINES
            "Bacău" -> BACAU_LINES
            "Ploiești" -> PLOIESTI_LINES
            "Miercurea Ciuc" -> MIERCUREA_CIUC_LINES
            "Pitești" -> PITESTI_LINES
            "Brăila" -> BRAILA_LINES
            "Baia Mare" -> BAIA_MARE_LINES
            "Bistrița" -> BISTRITA_LINES
            "Târgoviște" -> TARGOVISTE_LINES
            "Tulcea" -> TULCEA_LINES
            "Piatra Neamț" -> PIATRA_NEAMT_LINES
            "Râmnicu Vâlcea" -> RAMNICU_VALCEA_LINES
            "Drobeta-Turnu Severin" -> DROBETA_LINES
            "Deva" -> DEVA_LINES
            "Botoșani" -> BOTOSANI_LINES
            "Sfântu Gheorghe" -> SFANTU_GHEORGHE_LINES
            "Giurgiu" -> GIURGIU_LINES
            "Călărași" -> CALARASI_LINES
            "Slobozia" -> SLOBOZIA_LINES
            "Zalău" -> ZALAU_LINES
            "Focșani" -> FOCSANI_LINES
            "Buzău" -> BUZAU_LINES
            "Reșița" -> RESITA_LINES
            "Târgu Jiu" -> TARGU_JIU_LINES
            "Slatina" -> SLATINA_LINES
            "Alexandria" -> ALEXANDRIA_LINES
            "Vaslui" -> VASLUI_LINES
            "Hunedoara" -> HUNEDOARA_LINES
            "Turda" -> TURDA_LINES
            "Mangalia" -> MANGALIA_LINES
            "Bușteni" -> BUSTENI_LINES
            "Curtea de Argeș" -> CURTEA_AR_LINES
            "Gura Humorului" -> GURA_HUMORULUI_LINES
            "Vatra Dornei" -> VATRA_DORNEI_LINES
            "Sovata" -> SOVATA_LINES
            "Băile Felix" -> BAILE_FELIX_LINES
            "Slănic Moldova" -> SLANIC_MOLDOVA_LINES
            "Băile Herculane" -> BAILE_HERCULANE_LINES
            "Călimănești" -> CALIMANESTI_LINES
            "Borsec" -> BORSEC_LINES
            "Băile Govora" -> BAILE_GOVORA_LINES
            "Câmpulung Moldovenesc" -> CAMPULUNG_MOLDOVENESC_LINES
            "Cluj-Napoca" -> CLUJ_LINES + CLJ_NEW_LINES
            else -> emptyList()
        }
    }

    fun getBoundsForCity(city: String): CityBounds {
        val dynamic = DYNAMIC_BOUNDS[city]
        if (dynamic != null) return dynamic

        return when (city) {
            "București" -> CityBounds(44.4100, 44.4900, 26.0500, 26.1300)
            "Brașov" -> CityBounds(45.6250, 45.6750, 25.5700, 25.6250)
            "Câmpina" -> CityBounds(45.1000, 45.1500, 25.6800, 25.7500)
            "Sinaia" -> CityBounds(45.3350, 45.3850, 25.5100, 25.5800)
            "Sibiu" -> CityBounds(45.7500, 45.8200, 24.1100, 24.2000)
            "Sighișoara" -> CityBounds(46.2100, 46.2300, 24.7800, 24.8100)
            "Constanța" -> CityBounds(44.1500, 44.2200, 28.6000, 28.6700)
            "Iași" -> CityBounds(47.1400, 47.1900, 27.5400, 27.6100)
            "Timișoara" -> CityBounds(45.7300, 45.7800, 21.1900, 21.2500)
            "Oradea" -> CityBounds(47.0300, 47.0800, 21.8900, 21.9700)
            "Alba Iulia" -> CityBounds(46.0600, 46.0800, 23.5600, 23.5900)
            "Suceava" -> CityBounds(47.6300, 47.6800, 26.2400, 26.2800)
            "Craiova" -> CityBounds(44.2900, 44.3400, 23.7800, 23.8200)
            "Arad" -> CityBounds(46.1600, 46.2000, 21.3000, 21.3400)
            "Galați" -> CityBounds(45.4100, 45.4500, 28.0200, 28.0700)
            "Târgu Mureș" -> CityBounds(46.5200, 46.5600, 24.5300, 24.5900)
            "Satu Mare" -> CityBounds(47.7700, 47.8100, 22.8600, 22.9100)
            "Bacău" -> CityBounds(46.5400, 46.5900, 26.8900, 26.9300)
            "Ploiești" -> CityBounds(44.9100, 44.9700, 26.0000, 26.0700)
            "Miercurea Ciuc" -> CityBounds(46.3400, 46.3900, 25.7700, 25.8300)
            "Pitești" -> CityBounds(44.8300, 44.8800, 24.8400, 24.9000)
            "Brăila" -> CityBounds(45.2500, 45.2900, 27.9300, 27.9900)
            "Baia Mare" -> CityBounds(47.6200, 47.6800, 23.5300, 23.6100)
            "Bistrița" -> CityBounds(47.1000, 47.1500, 24.4600, 24.5200)
            "Târgoviște" -> CityBounds(44.9000, 44.9500, 25.4300, 25.4800)
            "Tulcea" -> CityBounds(45.1600, 45.1900, 28.7700, 28.8200)
            "Piatra Neamț" -> CityBounds(46.9100, 46.9500, 26.3400, 26.3900)
            "Râmnicu Vâlcea" -> CityBounds(45.0800, 45.1300, 24.3400, 24.3900)
            "Drobeta-Turnu Severin" -> CityBounds(44.6000, 44.6500, 22.6200, 22.6800)
            "Deva" -> CityBounds(45.8600, 45.9100, 22.8700, 22.9400)
            "Botoșani" -> CityBounds(47.7200, 47.7700, 26.6300, 26.6900)
            "Sfântu Gheorghe" -> CityBounds(45.8400, 45.8900, 25.7600, 25.8100)
            "Giurgiu" -> CityBounds(43.8700, 43.9100, 25.9400, 25.9900)
            "Călărași" -> CityBounds(44.1700, 44.2200, 27.3000, 27.3500)
            "Slobozia" -> CityBounds(44.5400, 44.5800, 27.3400, 27.3900)
            "Zalău" -> CityBounds(47.1600, 47.2100, 23.0300, 23.0800)
            "Focșani" -> CityBounds(45.6700, 45.7200, 27.1600, 27.2200)
            "Buzău" -> CityBounds(45.1200, 45.1800, 26.7900, 26.8500)
            "Reșița" -> CityBounds(45.2700, 45.3200, 21.8500, 21.9100)
            "Târgu Jiu" -> CityBounds(45.0100, 45.0600, 23.2400, 23.3100)
            "Slatina" -> CityBounds(44.4000, 44.4600, 24.3300, 24.3900)
            "Alexandria" -> CityBounds(43.9500, 43.9900, 25.3000, 25.3500)
            "Vaslui" -> CityBounds(46.6100, 46.6600, 27.7000, 27.7500)
            "Hunedoara" -> CityBounds(45.7300, 45.7700, 22.8700, 22.9200)
            "Turda" -> CityBounds(46.5400, 46.6000, 23.7500, 23.8300)
            "Mangalia" -> CityBounds(43.8000, 43.8500, 28.5600, 28.6000)
            "Bușteni" -> CityBounds(45.3900, 45.4300, 25.5000, 25.5500)
            "Curtea de Argeș" -> CityBounds(45.1300, 45.1700, 24.6500, 24.6900)
            "Gura Humorului" -> CityBounds(47.5100, 47.6000, 25.8400, 25.9000)
            "Vatra Dornei" -> CityBounds(47.3300, 47.3800, 25.3200, 25.3800)
            "Sovata" -> CityBounds(46.5800, 46.6200, 25.0400, 25.1000)
            "Băile Felix" -> CityBounds(46.9700, 47.0100, 21.9600, 22.0100)
            "Slănic Moldova" -> CityBounds(46.1800, 46.2300, 26.4000, 26.4600)
            "Băile Herculane" -> CityBounds(44.8600, 44.9000, 22.3900, 22.4400)
            "Călimănești" -> CityBounds(45.2300, 45.2800, 24.3100, 24.3600)
            "Borsec" -> CityBounds(46.9500, 46.9900, 25.5500, 25.6000)
            "Băile Govora" -> CityBounds(45.0600, 45.1000, 24.1600, 24.2100)
            "Câmpulung Moldovenesc" -> CityBounds(47.5100, 47.5500, 25.5300, 25.5800)
            else -> CityBounds(46.7450, 46.7950, 23.5500, 23.6300)
        }
    }

    // Transfer/Multi-hop Connection Struct and Routing Engine
    data class Connection(
        val line1: BusLine,
        val line2: BusLine? = null,
        val transferStation: BusStation? = null,
        val idxFrom1: Int,
        val idxTo1: Int,
        val idxFrom2: Int = -1,
        val idxTo2: Int = -1
    )

    fun findConnection(
        fromStation: BusStation,
        toStation: BusStation,
        lines: List<BusLine>,
        allowTransfer: Boolean
    ): Connection? {
        // 1. Direct Search
        for (line in lines) {
            val idxF = line.stations.indexOfFirst { it.id == fromStation.id }
            val idxT = line.stations.indexOfFirst { it.id == toStation.id }
            if (idxF != -1 && idxT != -1 && idxF != idxT) {
                return Connection(line1 = line, idxFrom1 = idxF, idxTo1 = idxT)
            }
        }
        
        // 2. Transfer Search
        if (allowTransfer) {
            for (line1 in lines) {
                val idxF1 = line1.stations.indexOfFirst { it.id == fromStation.id }
                if (idxF1 == -1) continue
                
                for (line2 in lines) {
                    if (line1.name == line2.name) continue
                    val idxT2 = line2.stations.indexOfFirst { it.id == toStation.id }
                    if (idxT2 == -1) continue
                    
                    // Look for common intersection station
                    val line2Ids = line2.stations.map { it.id }.toSet()
                    val common = line1.stations.firstOrNull { s1 ->
                        s1.id != fromStation.id && s1.id != toStation.id && line2Ids.contains(s1.id)
                    }
                    if (common != null) {
                        val idxTo1 = line1.stations.indexOfFirst { it.id == common.id }
                        val idxFrom2 = line2.stations.indexOfFirst { it.id == common.id }
                        if (idxTo1 != -1 && idxFrom2 != -1) {
                            return Connection(
                                line1 = line1,
                                line2 = line2,
                                transferStation = common,
                                idxFrom1 = idxF1,
                                idxTo1 = idxTo1,
                                idxFrom2 = idxFrom2,
                                idxTo2 = idxT2
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Compute optimized Traveling Salesperson journey, respecting multi-hop preferences,
     * transport category toggles (allowing removal of methods), taxi pricing, and route strategy parameters.
     */
    fun optimizePath(
        startSpot: TouristSpot,
        spotsToVisit: List<TouristSpot>,
        city: String,
        preference: String = "DEFAULT",
        enabledModes: Set<LegType> = setOf(LegType.WALK, LegType.BUS, LegType.METRO, LegType.TROLLEY, LegType.TRAIN, LegType.TAXI)
    ): OptimizedJourney {
        val activeSpots = spotsToVisit.filter { it.isSelected && it.id != startSpot.id }.toMutableList()
        val ordered = mutableListOf<TouristSpot>()
        
        var current = startSpot
        
        // Greedy Traveling Salesperson Order
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

        val legs = mutableListOf<RouteLeg>()
        var totalMinutes = 0
        var totalFare = 0
        var totalTaxiCost = 0
        
        val fullRouteWithStart = listOf(startSpot) + ordered
        val allLines = getLinesForCity(city)
        
        // Eliminare metode: filter lines to only those actively selected by user
        val allowedLines = allLines.filter { enabledModes.contains(it.type) }
        val allowedStations = allowedLines.flatMap { it.stations }.distinctBy { it.id }

        // Walk thresholds based on strategy preference
        val walkDirectThreshold = when (preference) {
            "LESS_WALKING" -> 200.0 // Avoid walks bigger than 200m if possible
            "CHEAPER" -> 1500.0     // Walk more to save money
            "FASTER" -> 400.0       // Ride faster modes if possible
            else -> 700.0
        }

        val allowTransfers = (preference == "MORE_TRANSFERS" || preference == "LESS_WALKING" || preference == "DEFAULT")

        for (i in 0 until fullRouteWithStart.size - 1) {
            val from = fullRouteWithStart[i]
            val to = fullRouteWithStart[i + 1]
            
            val distDirect = calculateDistance(from.latitude, from.longitude, to.latitude, to.longitude)
            
            val topFromStations = allowedStations.sortedBy { calculateDistance(from.latitude, from.longitude, it.latitude, it.longitude) }
            val topToStations = allowedStations.sortedBy { calculateDistance(to.latitude, to.longitude, it.latitude, it.longitude) }
            
            val nearestStationToFrom = topFromStations.firstOrNull()
            val nearestStationToTo = topToStations.firstOrNull()
            
            val distToStationFrom = nearestStationToFrom?.let { calculateDistance(from.latitude, from.longitude, it.latitude, it.longitude) } ?: Double.MAX_VALUE
            val distToStationTo = nearestStationToTo?.let { calculateDistance(to.latitude, to.longitude, it.latitude, it.longitude) } ?: Double.MAX_VALUE
            val totalWalkDistanceToTransit = distToStationFrom + distToStationTo

            // Determine if direct taxi / Uber is selected
            val runTaxiDirect = enabledModes.contains(LegType.TAXI) && 
                (preference == "FASTER" || allowedLines.isEmpty() || (preference == "LESS_WALKING" && allowedLines.isEmpty()))

            if (runTaxiDirect) {
                // TAXX / UBER DIRECT ROUTE
                val meters = distDirect.toInt()
                val duration = max(2, (meters / 250).toInt()) // Fast: 15km/h city flow
                val baseFee = 6.0
                val costPerKm = 3.5
                val taxiFare = (baseFee + (meters / 1000.0) * costPerKm).roundToInt().coerceAtLeast(10)
                
                legs.add(
                    RouteLeg(
                        fromPlaceName = from.name,
                        toPlaceName = to.name,
                        type = LegType.TAXI,
                        distanceMeters = meters,
                        durationMinutes = duration,
                        directions = "Ia un Ridesharing / Taxi de la ${from.name} până la ${to.name} • Tarif estimat Uber/Taxi: $taxiFare Lei (Distanță: $meters m, ~$duration min)."
                    )
                )
                totalMinutes += duration + to.visitDurationMinutes
                totalTaxiCost += taxiFare
            } else if (allowedStations.isEmpty() || (preference != "LESS_WALKING" && distDirect < walkDirectThreshold) || (preference != "LESS_WALKING" && totalWalkDistanceToTransit > distDirect)) {
                // WALKING DIRECT
                val meters = distDirect.toInt()
                val duration = max(2, (meters / 80).toInt()) // ~5 km/h
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
                // SEARCHING FOR PUBLIC TRANSIT CONNECTIONS
                var bestConn: Connection? = null
                var bestFrom: BusStation? = null
                var bestTo: BusStation? = null

                if (preference == "LESS_WALKING") {
                    var minTotalWalk = Double.MAX_VALUE
                    for (fSt in topFromStations.take(4)) {
                        for (tSt in topToStations.take(4)) {
                            if (fSt.id != tSt.id) {
                                val c = findConnection(fSt, tSt, allowedLines, true)
                                if (c != null) {
                                    val wF = calculateDistance(from.latitude, from.longitude, fSt.latitude, fSt.longitude)
                                    val wT = calculateDistance(to.latitude, to.longitude, tSt.latitude, tSt.longitude)
                                    val currentWalk = wF + wT
                                    // Make sure it doesn't walk more than just walking direct
                                    if (currentWalk < minTotalWalk && currentWalk < distDirect) {
                                        minTotalWalk = currentWalk
                                        bestConn = c
                                        bestFrom = fSt
                                        bestTo = tSt
                                    }
                                }
                            }
                        }
                    }
                } else {
                    for (fSt in topFromStations.take(1)) {
                        for (tSt in topToStations.take(1)) {
                            if (fSt.id != tSt.id) {
                                val c = findConnection(fSt, tSt, allowedLines, allowTransfers)
                                if (c != null) {
                                    bestConn = c
                                    bestFrom = fSt
                                    bestTo = tSt
                                    break
                                }
                            }
                        }
                        if (bestConn != null) break
                    }
                }
                
                val conn = bestConn

                if (conn != null && bestFrom != null && bestTo != null) {
                    val finalDistToStationFrom = calculateDistance(from.latitude, from.longitude, bestFrom.latitude, bestFrom.longitude)
                    val finalDistToStationTo = calculateDistance(to.latitude, to.longitude, bestTo.latitude, bestTo.longitude)

                    // Walk to first station
                    val walkMeters1 = finalDistToStationFrom.toInt()
                    val walkTime1 = max(1, (walkMeters1 / 80).toInt())
                    if (walkMeters1 > 40) {
                        legs.add(
                            RouteLeg(
                                fromPlaceName = from.name,
                                toPlaceName = bestFrom.name,
                                type = LegType.WALK,
                                distanceMeters = walkMeters1,
                                durationMinutes = walkTime1,
                                directions = "Mergi pe jos ${walkMeters1} m până la stația: ${bestFrom.name}."
                            )
                        )
                        totalMinutes += walkTime1
                    }

                    if (conn.line2 != null && conn.transferStation != null) {
                        // MULTI-HOP / TRANSFER ITINERARY
                        val stations1 = abs(conn.idxTo1 - conn.idxFrom1)
                        val transitTime1 = stations1 * 4 + 2
                        val label1 = when (conn.line1.type) {
                            LegType.METRO -> "Metroul"
                            LegType.TROLLEY -> "Troleibuzul"
                            LegType.TRAIN -> "Trenul"
                            else -> "Autobuzul"
                        }
                        legs.add(
                            RouteLeg(
                                fromPlaceName = bestFrom.name,
                                toPlaceName = conn.transferStation.name,
                                type = conn.line1.type,
                                busLineName = conn.line1.name,
                                busColorHex = conn.line1.colorHex,
                                boardingStation = bestFrom.name,
                                alightingStation = conn.transferStation.name,
                                distanceMeters = (stations1 * 1000),
                                durationMinutes = transitTime1,
                                directions = "Urcă în $label1 ${conn.line1.name} la ${bestFrom.name}, mergi $stations1 stații și coboară la ${conn.transferStation.name}."
                            )
                        )
                        totalMinutes += transitTime1 + 3
                        totalFare += 3

                        // Wait / change vehicles
                        legs.add(
                            RouteLeg(
                                fromPlaceName = conn.transferStation.name,
                                toPlaceName = conn.transferStation.name,
                                type = LegType.WALK,
                                distanceMeters = 50,
                                durationMinutes = 4,
                                directions = "Schimbă mijlocul de transport în stația ${conn.transferStation.name} (~4 min)."
                            )
                        )
                        totalMinutes += 4

                        // Hop 2
                        val stations2 = abs(conn.idxTo2 - conn.idxFrom2)
                        val transitTime2 = stations2 * 4 + 2
                        val label2 = when (conn.line2.type) {
                            LegType.METRO -> "Metroul"
                            LegType.TROLLEY -> "Troleibuzul"
                            LegType.TRAIN -> "Trenul"
                            else -> "Autobuzul"
                        }
                        legs.add(
                            RouteLeg(
                                fromPlaceName = conn.transferStation.name,
                                toPlaceName = bestTo.name,
                                type = conn.line2.type,
                                busLineName = conn.line2.name,
                                busColorHex = conn.line2.colorHex,
                                boardingStation = conn.transferStation.name,
                                alightingStation = bestTo.name,
                                distanceMeters = (stations2 * 1000),
                                durationMinutes = transitTime2,
                                directions = "Urcă în $label2 ${conn.line2.name} și călătorește $stations2 stații până la ${bestTo.name}."
                            )
                        )
                        totalMinutes += transitTime2 + 3
                        totalFare += 3
                    } else {
                        // DIRECT SINGLE-HOP ROUTE
                        val stationsInBetween = abs(conn.idxTo1 - conn.idxFrom1)
                        val transitTime = stationsInBetween * 4 + 2
                        val labelVal = when (conn.line1.type) {
                            LegType.METRO -> "Metroul"
                            LegType.TROLLEY -> "Troleibuzul"
                            LegType.TRAIN -> "Trenul"
                            else -> "Autobuzul"
                        }
                        val transitMeters = (stationsInBetween * 1000)
                        legs.add(
                            RouteLeg(
                                fromPlaceName = bestFrom.name,
                                toPlaceName = bestTo.name,
                                type = conn.line1.type,
                                busLineName = conn.line1.name,
                                busColorHex = conn.line1.colorHex,
                                boardingStation = bestFrom.name,
                                alightingStation = bestTo.name,
                                distanceMeters = transitMeters,
                                durationMinutes = transitTime,
                                directions = "Călătorește cu $labelVal ${conn.line1.name} din stația ${bestFrom.name} până la stația ${bestTo.name} ($stationsInBetween stații)."
                            )
                        )
                        totalMinutes += transitTime + 3
                        totalFare += 3
                    }

                    // Walk to destination spot
                    val walkMeters2 = finalDistToStationTo.toInt()
                    val walkTime2 = max(1, (walkMeters2 / 80).toInt())
                    if (walkMeters2 > 40) {
                        legs.add(
                            RouteLeg(
                                fromPlaceName = bestTo.name,
                                toPlaceName = to.name,
                                type = LegType.WALK,
                                distanceMeters = walkMeters2,
                                durationMinutes = walkTime2,
                                directions = "De la stația ${bestTo.name}, mergi pe jos ${walkMeters2} m până la ${to.name}."
                            )
                        )
                        totalMinutes += walkTime2
                    }

                    totalMinutes += to.visitDurationMinutes
                } else {
                    // Fallback to walk direct if transit connection failed but enabled modes exist
                    val meters = distDirect.toInt()
                    val duration = max(2, (meters / 80).toInt())
                    legs.add(
                        RouteLeg(
                            fromPlaceName = from.name,
                            toPlaceName = to.name,
                            type = LegType.WALK,
                            distanceMeters = meters,
                            durationMinutes = duration,
                            directions = "Mergi pe jos ${meters} m (~$duration min) până la ${to.name}."
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
            totalBusFareLei = totalFare,
            totalTaxiCostLei = totalTaxiCost
        )
    }
}
