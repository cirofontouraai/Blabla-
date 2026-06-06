package com.example.data.local

import androidx.room.*
import com.example.data.model.SearchRoute
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM search_routes ORDER BY lastUpdated DESC")
    fun getAllRoutesFlow(): Flow<List<SearchRoute>>

    @Query("SELECT * FROM search_routes WHERE isTracking = 1")
    fun getActiveTrackingRoutesFlow(): Flow<List<SearchRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SearchRoute): Long

    @Update
    suspend fun updateRoute(route: SearchRoute)

    @Delete
    suspend fun deleteRoute(route: SearchRoute)

    @Query("UPDATE search_routes SET isTracking = :isTracking WHERE id = :id")
    suspend fun updateTrackingStatus(id: Long, isTracking: Boolean)
}
