package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashGameDao {

    @Query("SELECT * FROM flash_games ORDER BY lastPlayed DESC, id DESC")
    fun getAllGames(): Flow<List<FlashGameEntity>>

    @Query("SELECT * FROM flash_games WHERE isFavorite = 1 ORDER BY lastPlayed DESC")
    fun getFavoriteGames(): Flow<List<FlashGameEntity>>

    @Query("SELECT * FROM flash_games WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT 10")
    fun getRecentGames(): Flow<List<FlashGameEntity>>

    @Query("SELECT * FROM flash_games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Long): FlashGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: FlashGameEntity): Long

    @Update
    suspend fun updateGame(game: FlashGameEntity)

    @Delete
    suspend fun deleteGame(game: FlashGameEntity)

    @Query("DELETE FROM flash_games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("UPDATE flash_games SET lastPlayed = :timestamp, playTimeMinutes = playTimeMinutes + :addedMinutes WHERE id = :id")
    suspend fun updatePlaySession(id: Long, timestamp: Long, addedMinutes: Int)

    @Query("UPDATE flash_games SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}
