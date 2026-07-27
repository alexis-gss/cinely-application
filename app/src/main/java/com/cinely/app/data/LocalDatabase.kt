package com.cinely.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

// ---------- Entités ----------

/** Une œuvre suivie (film ou série/anime en cours ou à venir) pour l'onglet Planning. */
@Serializable
@Entity(tableName = "followed_items")
data class FollowedItem(
    @PrimaryKey val tmdbId: Int,
    val mediaType: String,               // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val releaseDate: String? = null,
    val nextAirDate: String? = null,     // format ISO yyyy-MM-dd, fourni par TMDB
    val nextEpisodeName: String? = null,
    val nextSeasonNumber: Int? = null,
    val nextEpisodeNumber: Int? = null,
    val status: String? = null,
    val lastAiredSeasonNumber: Int? = null,  // saison du dernier épisode réellement diffusé
    val lastAiredEpisodeNumber: Int? = null, // numéro du dernier épisode réellement diffusé
    val airedEpisodesCount: Int? = null, // nombre total d'épisodes déjà diffusés à ce jour (toutes saisons sorties confondues), pour le badge "reste à voir" de Bookmark
    val lastCheckedAt: Long = 0L         // timestamp du dernier rafraîchissement (throttling)
)

/** Un élément marqué comme vu : film, documentaire, épisode de série ou d'anime. */
@Serializable
@Entity(tableName = "watched_items")
data class WatchedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tmdbId: Int,
    val mediaType: String,           // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val seasonNumber: Int? = null,   // null pour un film
    val episodeNumber: Int? = null,  // null pour un film
    val episodeName: String? = null,
    val watchedAt: Long,             // timestamp epoch millis
    val genres: String? = null,      // genres TMDB séparés par des virgules (ex: "Action,Drame"), pour l'écran Statistiques
    val durationMinutes: Int? = null // durée TMDB en minutes (film ou épisode), pour le temps de visionnage estimé
)

/** Un film ou une série ajouté aux favoris (indépendant du suivi et de l'historique vu). */
@Serializable
@Entity(tableName = "favorite_items", primaryKeys = ["tmdbId", "mediaType"])
data class FavoriteItem(
    val tmdbId: Int,
    val mediaType: String,           // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val addedAt: Long                // timestamp epoch millis
)

// ---------- DAO ----------

@Dao
interface FollowedItemDao {
    @Query("SELECT * FROM followed_items ORDER BY nextAirDate IS NULL, nextAirDate ASC")
    fun observeAll(): Flow<List<FollowedItem>>

    @Query("SELECT * FROM followed_items")
    suspend fun getAllOnce(): List<FollowedItem>

    @Query("SELECT * FROM followed_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType LIMIT 1")
    suspend fun find(tmdbId: Int, mediaType: String): FollowedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FollowedItem)

    @Query("DELETE FROM followed_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun remove(tmdbId: Int, mediaType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FollowedItem>)

    // Dans FollowedItemDao
    @Query("DELETE FROM followed_items")
    suspend fun clearAllFollowedItems()
}

@Dao
interface WatchedItemDao {
    @Query("SELECT * FROM watched_items ORDER BY watchedAt DESC")
    fun observeAll(): Flow<List<WatchedItem>>

    @Query("SELECT * FROM watched_items")
    suspend fun getAllOnce(): List<WatchedItem>

    @Query("SELECT COUNT(*) FROM watched_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType AND (seasonNumber IS :seasonNumber) AND (episodeNumber IS :episodeNumber)")
    suspend fun countMatching(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?): Int

    @Insert
    suspend fun insert(item: WatchedItem)

    @Query("DELETE FROM watched_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType AND (seasonNumber IS :seasonNumber) AND (episodeNumber IS :episodeNumber)")
    suspend fun deleteMatching(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?)

    @Query("DELETE FROM watched_items")
    suspend fun clearAllWatchedItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchedItem>)
}

@Dao
interface FavoriteItemDao {
    @Query("SELECT * FROM favorite_items ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteItem>>

    @Query("SELECT * FROM favorite_items")
    suspend fun getAllOnce(): List<FavoriteItem>

    @Query("SELECT * FROM favorite_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType LIMIT 1")
    suspend fun find(tmdbId: Int, mediaType: String): FavoriteItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavoriteItem)

    @Query("DELETE FROM favorite_items WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun remove(tmdbId: Int, mediaType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FavoriteItem>)

    @Query("DELETE FROM favorite_items")
    suspend fun clearAllFavoriteItems()
}

// ---------- Database ----------

@Database(
    entities = [FollowedItem::class, WatchedItem::class, FavoriteItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followedItemDao(): FollowedItemDao
    abstract fun watchedItemDao(): WatchedItemDao
    abstract fun favoriteItemDao(): FavoriteItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinely.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}