package com.example.data.local

import androidx.room.*
import com.example.data.model.OfertaCaronaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfertaCaronaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(oferta: OfertaCaronaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ofertas: List<OfertaCaronaEntity>)

    @Update
    suspend fun update(oferta: OfertaCaronaEntity)

    @Delete
    suspend fun delete(oferta: OfertaCaronaEntity)

    @Query("DELETE FROM ofertas_carona")
    suspend fun clearAll()

    @Query("SELECT * FROM ofertas_carona ORDER BY id DESC")
    fun getAllFlow(): Flow<List<OfertaCaronaEntity>>

    /**
     * Query indexed by date of the trip (dataViagem).
     */
    @Query("SELECT * FROM ofertas_carona WHERE dataViagem = :data ORDER BY horarioViagem ASC")
    fun getOfertasByDate(data: String): Flow<List<OfertaCaronaEntity>>

    /**
     * Query allowing searches in custom time slots/ranges (turnos/faixas de horário) for a given date.
     * E.g. Morning ("06:00" to "12:00"), Afternoon ("12:00" to "18:00"), etc.
     */
    @Query("SELECT * FROM ofertas_carona WHERE dataViagem = :data AND horarioViagem BETWEEN :startHour AND :endHour ORDER BY horarioViagem ASC")
    fun getOfertasByTimeSlot(data: String, startHour: String, endHour: String): Flow<List<OfertaCaronaEntity>>
}
