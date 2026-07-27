package com.cinely.app.data

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val API_KEY_PARAM = "api_key"

    private class ApiKeyInterceptor(private val apiKeyStore: ApiKeyStore) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()

            // Si la clé est déjà dans l'URL (ex: test de validation), on ne la remplace pas
            if (original.url.queryParameter(API_KEY_PARAM) != null) {
                return chain.proceed(original)
            }

            val key = apiKeyStore.apiKey.value.orEmpty()
            val newUrl = original.url.newBuilder()
                .addQueryParameter(API_KEY_PARAM, key)
                .build()
            return chain.proceed(original.newBuilder().url(newUrl).build())
        }
    }

    /**
     * Force la valeur du paramètre "language" sur chaque requête TMDB à partir de la
     * préférence enregistrée par l'utilisateur dans Paramètres, en remplaçant la valeur
     * par défaut ("fr-FR") déclarée dans TmdbApi. Comme le cache OkHttp est indexé sur
     * l'URL complète (donc sur "language" inclus), changer de langue ne renvoie jamais
     * de contenu périmé d'une autre langue : chaque langue a ses propres entrées de cache.
     */
    private class LanguageInterceptor(private val languageStore: LanguagePreferenceStore) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            if (original.url.queryParameter("language") == null) {
                return chain.proceed(original)
            }
            val newUrl = original.url.newBuilder()
                .setQueryParameter("language", languageStore.language.value)
                .build()
            return chain.proceed(original.newBuilder().url(newUrl).build())
        }
    }

    private class CacheControlInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath
            val response = chain.proceed(request)

            // On NE MET PAS en cache les erreurs (codes HTTP non 2xx comme les 401)
            if (!response.isSuccessful) {
                return response
            }

            val maxAgeSeconds = when {
                path.contains("/configuration") -> TimeUnit.DAYS.toSeconds(30)
                path.contains("/search/") -> TimeUnit.HOURS.toSeconds(1)
                path.contains("/season/") -> TimeUnit.DAYS.toSeconds(3)
                else -> TimeUnit.DAYS.toSeconds(3)
            }

            return response.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", CacheControl.Builder().maxAge(maxAgeSeconds.toInt(), TimeUnit.SECONDS).build().toString())
                .build()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun provideApi(context: Context, apiKeyStore: ApiKeyStore, languageStore: LanguagePreferenceStore): TmdbApi {
        val cacheDir = File(context.cacheDir, "tmdb_http_cache")
        val cache = Cache(cacheDir, 20L * 1024 * 1024)

        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(ApiKeyInterceptor(apiKeyStore))
            .addInterceptor(LanguageInterceptor(languageStore))
            .addNetworkInterceptor(CacheControlInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(TmdbApi::class.java)
    }
}