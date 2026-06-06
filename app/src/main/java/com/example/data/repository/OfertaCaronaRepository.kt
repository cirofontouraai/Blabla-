package com.example.data.repository

import com.example.data.local.OfertaCaronaDao
import com.example.data.model.OfertaCaronaEntity
import kotlinx.coroutines.flow.Flow

class OfertaCaronaRepository(private val ofertaCaronaDao: OfertaCaronaDao) {
    val allOfertas: Flow<List<OfertaCaronaEntity>> = ofertaCaronaDao.getAllFlow()

    suspend fun insertOferta(oferta: OfertaCaronaEntity): Long {
        return ofertaCaronaDao.insert(oferta)
    }

    suspend fun insertAll(ofertas: List<OfertaCaronaEntity>) {
        ofertaCaronaDao.insertAll(ofertas)
    }

    suspend fun updateOferta(oferta: OfertaCaronaEntity) {
        ofertaCaronaDao.update(oferta)
    }

    suspend fun deleteOferta(oferta: OfertaCaronaEntity) {
        ofertaCaronaDao.delete(oferta)
    }

    suspend fun clearAll() {
        ofertaCaronaDao.clearAll()
    }

    fun getOfertasByDate(data: String): Flow<List<OfertaCaronaEntity>> {
        return ofertaCaronaDao.getOfertasByDate(data)
    }

    fun getOfertasByTimeSlot(data: String, startHour: String, endHour: String): Flow<List<OfertaCaronaEntity>> {
        return ofertaCaronaDao.getOfertasByTimeSlot(data, startHour, endHour)
    }
}
