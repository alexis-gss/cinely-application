package com.cinely.app

import android.app.Application
import androidx.work.*
import com.cinely.app.data.ApiKeyStore
import com.cinely.app.data.AppDatabase
import com.cinely.app.data.LanguagePreferenceStore
import com.cinely.app.data.NetworkModule
import com.cinely.app.data.Repository
import com.cinely.app.worker.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

class CinelyApplication : Application() {

    lateinit var repository: Repository
        private set
    lateinit var apiKeyStore: ApiKeyStore
        private set
    lateinit var languageStore: LanguagePreferenceStore
        private set

    override fun onCreate() {
        super.onCreate()

        apiKeyStore = ApiKeyStore.getInstance(this)
        // Portée process (et non liée à une Activity) : la langue doit survivre aux changements
        // d'écran et être lisible par le RefreshWorker en tâche de fond, pas seulement par l'UI
        // au premier plan.
        languageStore = LanguagePreferenceStore(this, CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val db = AppDatabase.getInstance(this)
        // Ni la clé ni la langue ne sont figées à la compilation : les intercepteurs réseau les
        // relisent à chaque appel, ce qui permet de les changer à chaud depuis l'écran Paramètres
        // sans redémarrer l'appli.
        val api = NetworkModule.provideApi(this, apiKeyStore = apiKeyStore, languageStore = languageStore)
        repository = Repository(api, db)

        schedulePeriodicRefresh()
    }

    /**
     * Un seul appel réseau groupé par série suivie, une fois par jour maximum,
     * plutôt qu'à chaque ouverture de l'appli : c'est le principal levier
     * pour limiter la consommation du quota API TMDB.
     */
    private fun schedulePeriodicRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<RefreshWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "refresh_followed_items",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
