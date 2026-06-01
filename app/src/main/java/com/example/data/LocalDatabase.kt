package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tourist_spots")
data class TouristSpot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val visitDurationMinutes: Int = 60,
    val description: String = "",
    val isCustom: Boolean = false,
    val isSelected: Boolean = false
)

@Entity(tableName = "saved_itineraries")
data class SavedItinerary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val city: String,
    val spotsCount: Int,
    val totalDurationMinutes: Int,
    val routeDetails: String, // Step-by-step description or serialized details
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "testing_logs")
data class TestingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val city: String,
    val placeName: String,
    val note: String,
    val observerName: String = "Testor Teren",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TouristSpotDao {
    @Query("SELECT * FROM tourist_spots ORDER BY id ASC")
    fun getAllSpotsFlow(): Flow<List<TouristSpot>>

    @Query("SELECT * FROM tourist_spots WHERE city = :city")
    suspend fun getSpotsByCity(city: String): List<TouristSpot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: TouristSpot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpots(spots: List<TouristSpot>)

    @Query("DELETE FROM tourist_spots WHERE id = :id")
    suspend fun deleteSpotById(id: Long)

    @Query("DELETE FROM tourist_spots WHERE isCustom = 1 AND city = :city")
    suspend fun clearCustomSpotsByCity(city: String)

    @Query("UPDATE tourist_spots SET isSelected = :isSelected WHERE id = :id")
    suspend fun updateSelection(id: Long, isSelected: Boolean)
}

@Dao
interface SavedItineraryDao {
    @Query("SELECT * FROM saved_itineraries ORDER BY timestamp DESC")
    fun getAllSavedItinerariesFlow(): Flow<List<SavedItinerary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItinerary(itinerary: SavedItinerary): Long

    @Query("DELETE FROM saved_itineraries WHERE id = :id")
    suspend fun deleteItineraryById(id: Long)
}

@Dao
interface TestingLogDao {
    @Query("SELECT * FROM testing_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<TestingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TestingLog): Long

    @Query("DELETE FROM testing_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM testing_logs WHERE city = :city")
    suspend fun clearLogsByCity(city: String)
}

@Database(entities = [TouristSpot::class, SavedItinerary::class, TestingLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun touristSpotDao(): TouristSpotDao
    abstract fun savedItineraryDao(): SavedItineraryDao
    abstract fun testingLogDao(): TestingLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bustour_optimizer_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

fun TouristSpot.translate(isEnglish: Boolean): TouristSpot {
    if (!isEnglish) return this
    val engName = when (name) {
        "Palatul Parlamentului" -> "Parliament Palace"
        "Centrul Vechi" -> "Old Town"
        "Ateneul Român" -> "Romanian Athenaeum"
        "Parcul Herăstrău (Mihai I)" -> "Herastrau Park"
        "Arcul de Triumf" -> "Triumph Arch"
        "Muzeul Național al Satului" -> "National Village Museum"
        "Parcul Cișmigiu" -> "Cismigiu Gardens"
        "Muzeul Național Grigore Antipa" -> "Antipa National Museum"
        "Cărturești Carusel" -> "Carturesti Carusel"
        "Biserica Stavropoleos" -> "Stavropoleos Church"
        "Muzeul de Artă al României" -> "National Art Museum of Romania"
        "Grădina Botanică Dimitrie Brândză" -> "Botanic Gardens"
        "Parcul Carol I" -> "Carol I Park"
        "Palatul Primăverii" -> "Spring Palace (Ceausescu Residence)"
        "Piața Revoluției" -> "Revolution Square"
        "Hanul lui Manuc" -> "Manuc's Inn"
        "Muzeul Național de Istorie a României" -> "National History Museum"
        "Palatul Cotroceni" -> "Cotroceni Palace"
        "Parcul Tineretului" -> "Tineretului Park"
        "Catedrala Mântuirii Neamului" -> "People's Salvation Cathedral"
        "Parcul Drumul Taberei" -> "Drumul Taberei Park"
        "Palatul Mogoșoaia" -> "Mogosoaia Palace"
        "Muzeul de Artă Contemporană (MNAC)" -> "National Museum of Contemporary Art"
        "Piața Universității" -> "University Square"
        "Opera Națională București" -> "Bucharest National Opera"
        // Cluj-Napoca presets:
        "Grădina Botanică Alexandru Borza" -> "Borza Botanical Garden"
        "Parcul Central Simion Bărnuțiu" -> "Central Park (Cluj)"
        "Piața Unirii & Biserica Sf. Mihail" -> "Unirii Square & St. Michael Church"
        "Catedrala Mitropolitană & Piața Avram Iancu" -> "Metropolitan Cathedral"
        "Dealul Cetățuia" -> "Cetatuia Hill"
        "Muzeul de Artă & Palatul Bánffy" -> "Cluj Art Museum & Banffy Palace"
        "Parcul Romulus Vuia (Etnografic)" -> "Ethnographic Museum"
        "Bastionul Croitorilor" -> "Tailors' Bastion"
        "Parcul Iulius (Lacul Gheorgheni)" -> "Iulius Lake Park"
        "Piața Muzeului" -> "Museum Square"
        "Pădurea Hoia-Baciu" -> "Hoia-Baciu Haunted Forest"
        "The Office Cluj & Podul de Fier" -> "The Office Cluj & Iron Bridge"
        "Teatrul Național și Opera Română" -> "National Theatre"
        "Turnul Pompierilor" -> "Firefighters' Tower"
        "Parcul Cetățuia Buburuza" -> "Buburuza Lookout Park"
        "Muzeul Național de Istorie a Transilvaniei" -> "National History Museum of Transylvania"
        "Biserica Reformată de pe ulița Lupilor" -> "Reformed Church"
        "Cluj Arena" -> "Cluj Arena Stadium"
        "BT Arena (Sala Polivalentă)" -> "BT Arena Multi-purpose Hall"
        "Parcul Rozelor" -> "Rose Park"
        "Biserica Calvaria (Mănăștur)" -> "Calvaria Church"
        "Catedrala Greco-Catolică Sf. Iosif (Cipariu)" -> "Cipariu Cathedral"
        "Observatorul Astronomic" -> "Astronomical Observatory"
        "Campusul Istoric USAMV" -> "USAMV Historic Campus"
        "Cetatea Fetei Florești" -> "Fetei Citadel (Forest)"
        // Brasov presets:
        "Biserica Neagră" -> "Black Church"
        "Piața Sfatului" -> "Council Square"
        "Telecabina Tâmpa" -> "Tampa Mountain Cable Car"
        "Turnul Alb" -> "White Tower"
        "Poarta Șchei" -> "Schei Gate"
        "Turnul Negru" -> "Black Tower"
        "Bastionul Țesătorilor" -> "Weavers' Bastion"
        "Strada Sforii" -> "Rope Street"
        "Prima Școală Românească" -> "First Romanian School"
        "Poarta Ecaterinei" -> "Catherine's Gate"
        "Parcul Central Nicolae Titulescu" -> "Nicolae Titulescu Central Park"
        "Bastionul Graft" -> "Graft Bastion"
        "Muzeul de Artă Brașov" -> "Brasov Art Museum"
        "Pietrele lui Solomon" -> "Solomon's Rocks"
        "Cetățuia de pe Strajă" -> "Brasov Citadel (Straja)"
        "Turnul Măcelarilor" -> "Butchers' Tower"
        "Bastionul Cojocarilor" -> "Furriers' Bastion"
        "Sinagoga Neologă din Brașov" -> "Brasov Hebrew Synagogue"
        "Casa Sfatului (Muzeul de Istorie)" -> "Council House (History Museum)"
        "Biserica Sfântul Nicolae" -> "St. Nicholas Church"
        "Promenada de sub Tâmpa" -> "Tampa Mountain Promenade"
        "Turnul Lemnarilor" -> "Woodworkers' Tower"
        "Cartierul Istoric Șchei" -> "Schei Historic District"
        "Grădina Zoologică Brașov (Noua)" -> "Brasov Zoo (Noua)"
        "Lacul Noua & Parc Agrement" -> "Noua Lake & Leisure Park"
        // Starting points
        "Gara de Nord (Hotel/Start)" -> "North Station (Hotel/Start)"
        "Gara Brașov (Hotel/Start)" -> "Brasov Station (Hotel/Start)"
        "Gara Cluj-Napoca (Hotel/Start)" -> "Cluj Station (Hotel/Start)"
        "Gara de Nord (Hotel/Start)" -> "North Station (Hotel/Start)"
        "Hotel / Start Personalizat" -> "Hotel / Custom Start"
        else -> name
    }
    val engDesc = when (name) {
        "Palatul Parlamentului" -> "One of the largest administrative buildings in the world."
        "Centrul Vechi" -> "The historical heart of Bucharest, full of life and vintage buildings."
        "Ateneul Român" -> "An architectural masterpiece of national historic importance."
        "Parcul Herăstrău (Mihai I)" -> "A huge, peaceful park set around a beautiful lake."
        "Arcul de Triumf" -> "Monument celebrating Romania's victory in World War I."
        "Muzeul Național al Satului" -> "Excursion into traditional Romanian rural life."
        "Parcul Cișmigiu" -> "The oldest public garden in Bucharest, featuring romantic lake views."
        "Muzeul Național Grigore Antipa" -> "Interactive zoology and dinosaur exhibits."
        "Cărturești Carusel" -> "One of the most spectacular bookstores in the world."
        "Biserica Stavropoleos" -> "A masterpiece of the Brancovenesc style and peaceful courtyard."
        "Muzeul de Artă al României" -> "The former Royal Palace holding Romanian art masterpieces."
        "Grădina Botanică Dimitrie Brândză" -> "Oasis of exotic greenhouses and thousands of plants."
        "Parcul Carol I" -> "Beautiful historic park with the magnificent Mausoleum."
        "Palatul Primăverii" -> "The luxurious former private residence of Nicolae Ceaușescu."
        "Piața Revoluției" -> "Central historic square with the Memorial of Rebirth."
        "Hanul lui Manuc" -> "The oldest operating inn building in Europe and traditional restaurant."
        "Muzeul Național de Istorie a României" -> "Priceless archaeological and national treasure exhibits."
        "Palatul Cotroceni" -> "Official Presidential palace and gorgeous historical museum."
        "Parcul Tineretului" -> "Immense park with leisure lake, great for cycling and relaxing."
        "Catedrala Mântuirii Neamului" -> "The largest Orthodox Cathedral in the world."
        "Parcul Drumul Taberei" -> "Also known as Moghioros Park, revitalized with bridges and greenhouses."
        "Palatul Mogoșoaia" -> "Exquisite Brancovenesc-style historical palace on the lake."
        "Muzeul de Artă Contemporană (MNAC)" -> "Modern art exhibitions located in the back wing of Parliament Palace."
        "Piața Universității" -> "The historic center-point of Bucharest surrounded by beautiful architecture."
        "Opera Națională București" -> "Traditional neoclassical complex hosting opera and ballet recitals."
        "Grădina Botanică Alexandru Borza" -> "A beautiful botanical garden hosting Japanese gardens and greenhouses."
        "Parcul Central Simion Bărnuțiu" -> "Old park with a charming lake, rowing boats, and bandstand."
        "Piața Unirii & Biserica Sf. Mihail" -> "The central square of Cluj-Napoca, dominated by the Cathedral."
        "Catedrala Mitropolitană & Piața Avram Iancu" -> "Outstanding Orthodox cathedral in Avram Iancu square."
        "Dealul Cetățuia" -> "Hilltop park with superb panoramic views over the city of Cluj."
        "Muzeul de Artă & Palatul Bánffy" -> "Banffy Palace housing spectacular visual arts."
        "Parcul Romulus Vuia (Etnografic)" -> "First outdoor museum in Romania depicting ancient Transylvanian villages."
        "Bastionul Croitorilor" -> "Well-preserved historic fortification towers of the ancient citadel."
        "Parcul Iulius (Lacul Gheorgheni)" -> "Modern green space next to the lake with custom boardwalks."
        "Piața Muzeului" -> "Historic square featuring the old Franciscan Church."
        "Pădurea Hoia-Baciu" -> "World famous forest known for mysterious tales and organic shape trees."
        "The Office Cluj & Podul de Fier" -> "Dynamic workplace and historic iron pedestrian bridge."
        "Teatrul Național și Opera Română" -> "Elegant baroque theater building representing Cluj's core culture."
        "Turnul Pompierilor" -> "Rehabilitated medieval fire observation tower with glass platform."
        "Parcul Cetățuia Buburuza" -> "Quieter family part of Cetatuia hill with viewpoints and playground."
        "Muzeul Național de Istorie a Transilvaniei" -> "Interactive national collections showing roman and dacian history."
        "Biserica Reformată de pe ulița Lupilor" -> "Famous gothic church representing central European medieval history."
        "Cluj Arena" -> "Ultra-modern football stadium hosting international games and Untold Festival."
        "BT Arena (Sala Polivalentă)" -> "Dynamic polyvalent indoor sports and music venue."
        "Parcul Rozelor" -> "Peaceful riverside park rich with dozens of rose variations."
        "Biserica Calvaria (Mănăștur)" -> "Historic Benedictine abbey ruins with scenic hilltop views."
        "Catedrala Greco-Catolică Sf. Iosif (Cipariu)" -> "Huge architectural masterpiece in Cipariu square."
        "Observatorul Astronomic" -> "Quiet educational observatory operated near USAMV college."
        "Campusul Istoric USAMV" -> "Orchards and green paths around the agricultural university."
        "Cetatea Fetei Florești" -> "Mystic fortress ruins on the hill, ideal for hiking."
        "Biserica Neagră" -> "The famous Gothic monument of Brasov and largest of its kind."
        "Piața Sfatului" -> "The heart of historic Brasov, surrounded by colorful saxon houses."
        "Telecabina Tâmpa" -> "Mountain overlooking the city, accessible by cable car or hiking trails."
        "Turnul Alb" -> "White lookout tower offering amazing views over the old town."
        "Poarta Șchei" -> "Schei Gate leading to the traditional Romanian neighborhood."
        "Turnul Negru" -> "Black tower overlooking the Black Church."
        "Bastionul Țesătorilor" -> "Well-preserved historic fortification hosting a museum."
        "Strada Sforii" -> "One of the narrowest medieval streets in Europe."
        "Prima Școală Românească" -> "Where first Romanian printing presses and books were designed."
        "Poarta Ecaterinei" -> "Only medieval gate preserved since the founding of Brasov stronghold."
        "Parcul Central Nicolae Titulescu" -> "Spacious downtown park with water fountains and massive trees."
        "Bastionul Graft" -> "Pretty fortification crossing the water stream of Graft."
        "Muzeul de Artă Brașov" -> "Exquisite local fine arts collection near City Hall."
        "Pietrele lui Solomon" -> "Deep gorges, mountain streams and outdoor recreation."
        "Cetățuia de pe Strajă" -> "Massive mountaintop fortress with rich medieval military history."
        "Turnul Măcelarilor" -> "Historic defense tower from the old saxon fortress wall."
        "Bastionul Cojocarilor" -> "Beautiful medieval bastion built near the base of Mount Tampa."
        "Sinagoga Neologă din Brașov" -> "Unique Byzantine style synagogue built in the late 19th century."
        "Casa Sfatului (Muzeul de Istorie)" -> "Old historic town hall standing at the center of Council Square."
        "Biserica Sfântul Nicolae" -> "Imposing Orthodox cathedral building in the Schei district."
        "Promenada de sub Tâmpa" -> "A beautiful tree-lined walking boulevard under Mount Tampa."
        "Turnul Lemnarilor" -> "Curated historic tower containing local sculptors' creations."
        "Cartierul Istoric Șchei" -> "Historic quiet narrow streets with traditional mountain cottage vibes."
        "Grădina Zoologică Brașov (Noua)" -> "Modern zoo setup inside Noua forest."
        "Lacul Noua & Parc Agrement" -> "Recreational park with boats, sporting zones and pine forests."
        else -> description
    }
    return this.copy(name = engName, description = engDesc)
}
