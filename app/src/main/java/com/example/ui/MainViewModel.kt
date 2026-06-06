package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.SearchRoute
import com.example.data.repository.RouteRepository
import com.example.service.BlaBlaFloatingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RouteRepository
    val allRoutes: StateFlow<List<SearchRoute>>

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RouteRepository(database.routeDao())
        
        allRoutes = repository.allRoutes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * Recheck and sync permission and active service execution state.
     */
    fun checkPermissionsAndStates(context: Context) {
        val contextApp = getApplication<Application>()
        _hasOverlayPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(contextApp)
        } else {
            true
        }
        _isServiceRunning.value = isServiceRunning(contextApp, BlaBlaFloatingService::class.java)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        return manager?.getRunningServices(Integer.MAX_VALUE)?.any {
            it.service.className == serviceClass.name
        } ?: false
    }

    fun toggleService(context: Context) {
        val contextApp = getApplication<Application>()
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(contextApp)
        } else {
            true
        }

        if (!hasPermission) {
            _isServiceRunning.value = false
            return
        }

        val intent = Intent(contextApp, BlaBlaFloatingService::class.java)
        val currentlyRunning = isServiceRunning(contextApp, BlaBlaFloatingService::class.java)
        if (currentlyRunning) {
            contextApp.stopService(intent)
            _isServiceRunning.value = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                contextApp.startForegroundService(intent)
            } else {
                contextApp.startService(intent)
            }
            _isServiceRunning.value = true
        }
    }

    fun addNewRoute(origin: String, destination: String, averagePrice: Double, discount: Double) {
        viewModelScope.launch {
            val newRoute = SearchRoute(
                origin = origin,
                destination = destination,
                averagePrice = averagePrice,
                requestedDiscountPercentage = discount,
                isTracking = true
            )
            repository.insertRoute(newRoute)
        }
    }

    fun toggleRouteTracking(route: SearchRoute) {
        viewModelScope.launch {
            repository.updateTrackingStatus(route.id, !route.isTracking)
        }
    }

    fun deleteRoute(route: SearchRoute) {
        viewModelScope.launch {
            repository.deleteRoute(route)
        }
    }
}
