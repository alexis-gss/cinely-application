package com.cinely.app

import android.app.Application

class TestApplication : Application() {
    // onCreate() volontairement vide : pas d'ApiKeyStore, pas de WorkManager,
    // pas de NetworkModule. Les tests Room construisent AppDatabase eux-mêmes.
}