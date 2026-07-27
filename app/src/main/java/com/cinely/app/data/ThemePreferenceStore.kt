package com.cinely.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Persiste le choix de thème (clair/sombre) de l'utilisateur sur disque via
 * DataStore, pour qu'il soit restauré à chaque ouverture de l'application.
 * Par défaut : thème clair (isDarkTheme = false).
 */
class ThemePreferenceStore(private val context: Context, private val scope: CoroutineScope) {

    private val KEY_DARK_THEME = booleanPreferencesKey("is_dark_theme")

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    init {
        scope.launch {
            val stored = context.themeDataStore.data.first()
            _isDarkTheme.value = stored[KEY_DARK_THEME] ?: false
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        scope.launch {
            context.themeDataStore.edit { prefs ->
                prefs[KEY_DARK_THEME] = enabled
            }
        }
    }
}