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

    // Default Starting places based on city
    fun getStartSpot(city: String): TouristSpot {
        return when (city) {
            "București" -> TouristSpot(-1, "Gara de Nord (Hotel/Start)", "București", 44.4468, 26.0725, 0, "Punctul de pornire al călătoriei.", false, false)
            "Brașov" -> TouristSpot(-1, "Gara Brașov (Hotel/Start)", "Brașov", 45.6611, 25.6111, 0, "Punctul de pornire al călătoriei.", false, false)
            "Câmpina" -> TouristSpot(-1, "Gara Câmpina (Hotel/Start)", "Câmpina", 45.1189, 25.7078, 0, "Punctul de pornire al călătoriei.", false, false)
            else -> TouristSpot(-1, "Gara Cluj-Napoca (Hotel/Start)", "Cluj-Napoca", 46.7865, 23.5875, 0, "Punctul de pornire al călătoriei.", false, false)
        }
    }

    fun getStationsForCity(city: String): List<BusStation> {
        return when (city) {
            "București" -> BUCURESTI_STATIONS + BUH_METRO_STATIONS + BUH_TROLLEY_STATIONS + BUH_TRAIN_STATIONS
            "Brașov" -> BRASOV_STATIONS + SBV_TROLLEY_STATIONS + SBV_TRAIN_STATIONS
            "Câmpina" -> CAMPINA_STATIONS + CMP_TROLLEY_STATIONS + CMP_TRAIN_STATIONS
            else -> CLUJ_STATIONS + CLJ_METRO_STATIONS + CLJ_TROLLEY_STATIONS + CLJ_TRAIN_STATIONS
        }
    }

    fun getLinesForCity(city: String): List<BusLine> {
        return when (city) {
            "București" -> BUCURESTI_LINES + BUH_NEW_LINES
            "Brașov" -> BRASOV_LINES + SBV_NEW_LINES
            "Câmpina" -> CAMPINA_LINES + CMP_NEW_LINES
            else -> CLUJ_LINES + CLJ_NEW_LINES
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
