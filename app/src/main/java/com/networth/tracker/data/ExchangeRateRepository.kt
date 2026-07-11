package com.networth.tracker.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ExchangeRateState(
    val rate: Double = AssetRepository.DEFAULT_USD_TO_INR,
    val lastUpdated: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFromCache: Boolean = false
)

class ExchangeRateRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadCachedState())
    val state: StateFlow<ExchangeRateState> = _state.asStateFlow()

    suspend fun refresh(force: Boolean = false) {
        val cached = _state.value
        if (!force && cached.lastUpdated != null &&
            System.currentTimeMillis() - cached.lastUpdated < CACHE_TTL_MS
        ) {
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        try {
            val rate = withContext(Dispatchers.IO) { fetchUsdToInrRate() }
            val now = System.currentTimeMillis()
            cacheRate(rate, now)
            _state.update {
                ExchangeRateState(
                    rate = rate,
                    lastUpdated = now,
                    isLoading = false,
                    isFromCache = false
                )
            }
        } catch (e: Exception) {
            val fallback = loadCachedState()
            _state.update {
                it.copy(
                    rate = fallback.rate,
                    lastUpdated = fallback.lastUpdated,
                    isLoading = false,
                    error = e.message ?: "Failed to fetch exchange rate",
                    isFromCache = fallback.lastUpdated != null
                )
            }
        }
    }

    private fun loadCachedState(): ExchangeRateState {
        val rate = prefs.getFloat(KEY_RATE, AssetRepository.DEFAULT_USD_TO_INR.toFloat()).toDouble()
        val lastUpdated = prefs.getLong(KEY_UPDATED, 0L).takeIf { it > 0 }
        return ExchangeRateState(
            rate = rate,
            lastUpdated = lastUpdated,
            isFromCache = lastUpdated != null
        )
    }

    private fun cacheRate(rate: Double, timestamp: Long) {
        prefs.edit()
            .putFloat(KEY_RATE, rate.toFloat())
            .putLong(KEY_UPDATED, timestamp)
            .apply()
    }

    private fun fetchUsdToInrRate(): Double {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Exchange rate API returned $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val inrRate = json.getJSONObject("rates").getDouble("INR")
            if (inrRate <= 0) {
                throw IllegalStateException("Invalid INR rate received")
            }
            return inrRate
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val PREFS_NAME = "exchange_rate_prefs"
        private const val KEY_RATE = "usd_inr_rate"
        private const val KEY_UPDATED = "usd_inr_updated"
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour
        private const val API_URL = "https://api.exchangerate-api.com/v4/latest/USD"
    }
}
