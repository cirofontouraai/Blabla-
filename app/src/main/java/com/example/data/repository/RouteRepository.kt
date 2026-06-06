package com.example.data.repository

import com.example.data.local.RouteDao
import com.example.data.model.SearchRoute
import kotlinx.coroutines.flow.Flow

class RouteRepository(private val routeDao: RouteDao) {
    val allRoutes: Flow<List<SearchRoute>> = routeDao.getAllRoutesFlow()
    val activeTrackingRoutes: Flow<List<SearchRoute>> = routeDao.getActiveTrackingRoutesFlow()

    suspend fun insertRoute(route: SearchRoute): Long {
        return routeDao.insertRoute(route)
    }

    suspend fun updateRoute(route: SearchRoute) {
        routeDao.updateRoute(route)
    }

    suspend fun deleteRoute(route: SearchRoute) {
        routeDao.deleteRoute(route)
    }

    suspend fun updateTrackingStatus(id: Long, isTracking: Boolean) {
        routeDao.updateTrackingStatus(id, isTracking)
    }
}
