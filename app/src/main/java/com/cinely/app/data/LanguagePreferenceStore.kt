package com.cinely.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.languageDataStore by preferencesDataStore(name = "language_prefs")

/** Une langue de contenu proposée dans les Paramètres, au format attendu par TMDB (ISO 639-1-ISO 3166-1). */
data class AppLanguage(val code: String, val label: String)

/**
 * IMPORTANT : ceci ne change QUE la langue du contenu renvoyé par TMDB (titres, synopsis,
 * genres, etc.). Les textes de l'interface de Cinely elle-même (boutons, menus...) restent
 * en français, car ils ne sont pas externalisés dans des ressources strings.xml localisables.
 * Faire une vraie traduction de l'appli serait un chantier séparé (extraction de toutes les
 * chaînes en dur vers res/values / res/values-en).
 */
val SUPPORTED_CONTENT_LANGUAGES = listOf(
    AppLanguage("fr-FR", "Français"),
    AppLanguage("en-US", "English"),
    AppLanguage("es-ES", "Español"),
    AppLanguage("de-DE", "Deutsch"),
    AppLanguage("it-IT", "Italiano"),
)

/**
 * Persiste la langue de contenu TMDB choisie par l'utilisateur sur disque via DataStore,
 * pour qu'elle soit restaurée à chaque ouverture de l'application.
 * Par défaut : français (fr-FR), pour ne pas changer le comportement existant.
 */
class LanguagePreferenceStore(private val context: Context, private val scope: CoroutineScope) {

    private val KEY_LANGUAGE = stringPreferencesKey("content_language")

    private val _language = MutableStateFlow(DEFAULT_LANGUAGE)
    val language: StateFlow<String> = _language

    init {
        scope.launch {
            val stored = context.languageDataStore.data.first()
            _language.value = stored[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
        }
    }

    fun setLanguage(code: String) {
        _language.value = code
        scope.launch {
            context.languageDataStore.edit { prefs ->
                prefs[KEY_LANGUAGE] = code
            }
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "fr-FR"
    }
}
