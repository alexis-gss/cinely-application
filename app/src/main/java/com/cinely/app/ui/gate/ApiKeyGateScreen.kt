package com.cinely.app.ui.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinely.app.R
import com.cinely.app.data.ApiKeyStore
import com.cinely.app.data.Repository
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun ApiKeyGateScreen(repository: Repository, apiKeyStore: ApiKeyStore) {
    var input by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun validate() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            errorMessage = "Renseigne une clé avant de valider"
            return
        }
        errorMessage = null
        isChecking = true
        scope.launch {
            try {
                // 1. On teste la clé AVANT de l'enregistrer
                repository.testApiKey(trimmed)

                // 2. Si le test passe (pas d'exception), on enregistre la clé définitivement
                apiKeyStore.save(trimmed)

            } catch (e: HttpException) {
                errorMessage = if (e.code() == 401) "Clé API invalide, vérifie qu'elle est correctement copiée"
                else "Erreur TMDB (code ${e.code()}), réessaie"
            } catch (e: Exception) {
                errorMessage = "Impossible de vérifier la clé, vérifie ta connexion"
            } finally {
                isChecking = false
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Logo Cinely",
                modifier = Modifier
                    .size(96.dp)
                    .padding(8.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Cinely", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Renseigne ta clé API TMDB pour continuer",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it; errorMessage = null },
                label = { Text("Clé API TMDB (v3)") },
                singleLine = true,
                isError = errorMessage != null,
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { validate() },
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Vérification…")
                } else {
                    Text("Valider")
                }
            }
        }
    }
}
