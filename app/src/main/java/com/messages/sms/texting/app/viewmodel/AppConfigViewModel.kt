package com.messages.sms.texting.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messages.sms.texting.app.data.model.AppResponse
import com.messages.sms.texting.app.data.network.ApiClient
import com.messages.sms.texting.app.utils.AnalyticsManager
import com.messages.sms.texting.app.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Fetches this app's remote config (ad placements, on/off flags, dynamic branding) from the
 * ad panel, caching the last successful response in SharedPreferences so [appResponse] is
 * available instantly on startup — before the network round trip completes — instead of null.
 */
class AppConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    private val defaultAppName = "Messages"

    companion object {
        /** Reads the same cached config this ViewModel uses, for callers that can't hold a
         * ViewModel instance (e.g. a BroadcastReceiver) — used to kick off a native-ad preload
         * as early as possible after a call ends, before AfterCallScreen even composes. */
        fun readCachedResult(context: Context): com.messages.sms.texting.app.data.model.AppResult? {
            val cachedPrefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
            val cachedJson = cachedPrefs.getString("cached_app_config", null) ?: return null
            return try {
                Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    .decodeFromString<AppResponse>(cachedJson).result
            } catch (e: Exception) {
                null
            }
        }
    }

    private val _appResponse = MutableStateFlow(loadCachedResponse())
    val appResponse: StateFlow<AppResponse?> = _appResponse

    private val _dynamicAppName = MutableStateFlow(
        _appResponse.value?.result?.app_name?.takeIf { it.isNotBlank() }
            ?: prefs.getString("dynamic_app_name", defaultAppName)?.takeIf { it.isNotBlank() } ?: defaultAppName
    )
    val dynamicAppName: StateFlow<String> = _dynamicAppName

    // Drives the "you're offline" dialog and re-triggers a fetch once connectivity returns.
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        fetchAppData()
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            // NetworkMonitor emits the current status immediately on subscription — init{} already
            // did the initial fetchAppData(), so skip that first emission and only refetch on an
            // actual offline→online transition (real reconnect), not the startup snapshot.
            var isFirstEmission = true
            NetworkMonitor(getApplication()).isOnline.collectLatest { online ->
                _isOnline.value = online
                if (!isFirstEmission && online && (_appResponse.value == null || _appResponse.value?.title == "Loaded from cache")) {
                    fetchAppData()
                }
                isFirstEmission = false
            }
        }
    }

    private fun loadCachedResponse(): AppResponse? {
        val cachedJson = prefs.getString("cached_app_config", null) ?: return null
        return try {
            val jsonParser = Json { ignoreUnknownKeys = true; coerceInputValues = true }
            jsonParser.decodeFromString<AppResponse>(cachedJson).copy(title = "Loaded from cache")
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchAppData() {
        viewModelScope.launch {
            try {
                val response = ApiClient.fetchAppConfig()
                if (response.status == 200) {
                    _appResponse.value = response

                    response.result?.let { result ->
                        val editor = prefs.edit()

                        result.app_name?.takeIf { it.isNotBlank() }?.let {
                            editor.putString("dynamic_app_name", it)
                            _dynamicAppName.value = it
                        }

                        try {
                            val jsonString = Json { ignoreUnknownKeys = true }
                                .encodeToString(AppResponse.serializer(), response)
                            editor.putString("cached_app_config", jsonString)
                        } catch (e: Exception) {
                            // Cache-write failure — in-memory state above is already updated.
                        }

                        editor.apply()
                    }
                }
            } catch (e: Exception) {
                // Offline or server error — cached/default state (already loaded) stays as-is.
            }
        }
    }
}
