package com.cinely.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cinely.app.CinelyApplication
import kotlinx.coroutines.flow.first

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CinelyApplication
        // Set nothing util an apikey is defined.
        if (app.apiKeyStore.apiKey.value.isNullOrBlank()) {
            return Result.success()
        }
        return try {
            val repo = app.repository
            val followed = repo.observeFollowed()
            // On ne lit qu'un instantané : pas besoin de collecter le Flow en continu ici.
            val snapshot = followed.first()
            snapshot.forEach { item ->
                repo.refreshFollowedIfStale(item, staleAfterMillis = 0L) // forcé, on est déjà dans le job journalier
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
