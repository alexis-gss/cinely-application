package com.cinely.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ApiKeyStore private constructor(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)

    private val _apiKey = MutableStateFlow(prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() })
    val apiKey: StateFlow<String?> = _apiKey

    fun save(key: String) {
        val trimmed = key.trim()
        _apiKey.value = trimmed.ifBlank { null }
        // apply() écrit en mémoire immédiatement et persiste en arrière-plan, contrairement
        // à commit() qui bloque le thread appelant jusqu'à l'écriture disque (lint: ApplySharedPref).
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
    }

    fun clear() {
        _apiKey.value = null
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    companion object {
        private const val PREFS_NAME = "cinely_settings_secure"
        private const val KEY_API_KEY = "tmdb_api_key"

        @Volatile
        private var INSTANCE: ApiKeyStore? = null

        fun getInstance(context: Context): ApiKeyStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiKeyStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        // SharedPreferences chiffrees (AES256-GCM pour les valeurs, AES256-SIV pour les cles)
        // via Jetpack Security. La cle maitre est generee et stockee dans l'Android Keystore,
        // jamais accessible en clair, meme sur un appareil root sans exploit du Keystore.
        // Remplace le stockage en clair precedent (issue securite : cle API TMDB lisible
        // directement dans /data/data/com.cinely.app/shared_prefs/*.xml).
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
