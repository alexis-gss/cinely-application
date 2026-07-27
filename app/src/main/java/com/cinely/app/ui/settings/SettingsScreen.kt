package com.cinely.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cinely.app.data.ApiKeyStore
import com.cinely.app.data.ExportImportManager
import com.cinely.app.data.LanguagePreferenceStore
import com.cinely.app.data.Repository
import com.cinely.app.data.SUPPORTED_CONTENT_LANGUAGES
import com.cinely.app.data.ThemePreferenceStore
import com.cinely.app.ui.components.SectionTitle
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: Repository,
    apiKeyStore: ApiKeyStore,
    themeStore: ThemePreferenceStore,
    languageStore: LanguagePreferenceStore,
    onBack: () -> Unit
) {
    val isDarkTheme by themeStore.isDarkTheme.collectAsState()
    val currentLanguage by languageStore.language.collectAsState()

    val currentKey by apiKeyStore.apiKey.collectAsState()
    var input by remember(currentKey) { mutableStateOf(currentKey.orEmpty()) }
    var isChecking by remember { mutableStateOf(false) }
    var keyMessage by remember { mutableStateOf<String?>(null) }
    var keyMessageIsError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bundle = repository.exportSnapshot()
                ExportImportManager.writeToUri(context, uri, bundle)
                snackbarMessage = "Export réussi (${bundle.watched.size} vus, ${bundle.followed.size} suivis)"
            } catch (e: Exception) {
                snackbarMessage = "Échec de l'export : ${e.message}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bundle = ExportImportManager.readFromUri(context, uri)
                repository.importSnapshot(bundle, replaceExisting = false)
                snackbarMessage = "Import réussi (${bundle.watched.size} vus, ${bundle.followed.size} suivis)"
            } catch (e: Exception) {
                snackbarMessage = "Échec de l'import : fichier invalide ou corrompu"
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); snackbarMessage = null }
    }

    fun saveKey() {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            keyMessageIsError = true
            keyMessage = "La clé ne peut pas être vide"
            return
        }
        if (trimmed == currentKey) {
            keyMessageIsError = false
            keyMessage = "Déjà enregistrée"
            return
        }
        isChecking = true
        keyMessage = null
        scope.launch {
            val previous = currentKey
            apiKeyStore.save(trimmed)
            try {
                repository.testApiKey()
                keyMessageIsError = false
                keyMessage = "Nouvelle clé enregistrée"
            } catch (e: HttpException) {
                previous?.let { apiKeyStore.save(it) } ?: apiKeyStore.clear()
                keyMessageIsError = true
                keyMessage = if (e.code() == 401) "Clé invalide, ancienne clé restaurée" else "Erreur TMDB (code ${e.code()})"
            } catch (e: Exception) {
                previous?.let { apiKeyStore.save(it) } ?: apiKeyStore.clear()
                keyMessageIsError = true
                keyMessage = "Vérification impossible, vérifie ta connexion"
            } finally {
                isChecking = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres de l'application") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = 12.dp
            ),
        ) {
            item {
                SectionTitle("Clé API TMDB")
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; keyMessage = null },
                    placeholder = { Text("Clé API TMDB…") },
                    singleLine = true,
                    enabled = !isChecking,
                    isError = keyMessageIsError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                keyMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (keyMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { saveKey() }, enabled = !isChecking, modifier = Modifier.fillMaxWidth()) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Vérification…")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Enregistrer la clé")
                        }
                    }
                }
            }
            item {
                var showClearCacheDialog by remember { mutableStateOf(false) }
                var showDeleteAllDialog by remember { mutableStateOf(false) }
                SectionTitle("Données locales")
                Text(
                    "Exporte tes données ainsi que ton historique de vus dans un fichier, ou importe une sauvegarde précédente.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch(ExportImportManager.defaultFileName()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Exporter")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/gzip", "application/octet-stream", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Importer")
                    }
                }
                OutlinedButton(
                    onClick = { showClearCacheDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Vider le cache")
                }
                OutlinedButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Supprimer toutes les données")
                }
                if (showClearCacheDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearCacheDialog = false },
                        title = { Text("Vider le cache ?") },
                        text = { Text("Les images et données temporaires seront supprimées. Tes films/séries suivis et ton historique de vus ne sont pas affectés.") },
                        confirmButton = {
                            TextButton(onClick = {
                                context.cacheDir.deleteRecursively()
                                showClearCacheDialog = false
                                snackbarMessage = "Cache vidé"
                            }) {
                                Text("Vider")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearCacheDialog = false }) {
                                Text("Annuler")
                            }
                        }
                    )
                }
                if (showDeleteAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAllDialog = false },
                        title = { Text("Supprimer toutes les données ?") },
                        text = { Text("Cette action est irréversible: tous tes films/séries suivis, ton historique de vus et tes favoris seront définitivement supprimés.") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    repository.clearAllData()
                                    showDeleteAllDialog = false
                                    snackbarMessage = "Toutes les données ont été supprimées"
                                }
                            }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAllDialog = false }) {
                                Text("Annuler")
                            }
                        }
                    )
                }
            }
            item {
                SectionTitle("Apparence")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isDarkTheme) "Thème sombre" else "Thème clair",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { themeStore.setDarkTheme(it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                SectionTitle("Langue du contenu")
                Text(
                    "Change la langue des titres, synopsis et fiches renvoyés par TMDB. " +
                        "Les textes de l'appli elle-même (menus, boutons…) restent en français. " +
                        "Les films/séries déjà suivis ou marqués vus gardent leur titre existant " +
                        "jusqu'à leur prochaine mise à jour automatique.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                FlowRowLanguages(
                    selected = currentLanguage,
                    onSelect = { languageStore.setLanguage(it) }
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                SectionTitle("À propos")
                val context = LocalContext.current
                val versionName = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    } catch (e: Exception) {
                        "?"
                    }
                }
                val uriHandler = LocalUriHandler.current
                val annotatedText = buildAnnotatedString {
                    append("Cinely - version $versionName by ")
                    pushStringAnnotation(tag = "URL", annotation = "https://alexis-gousseau.com")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Alexis Gousseau")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )
            }
        }
    }
}

/**
 * Sélecteur de langue de contenu TMDB : une rangée de FilterChip défilable horizontalement,
 * une seule langue active à la fois. Le clic est immédiat (pas de bouton "Enregistrer") :
 * la préférence est persistée par LanguagePreferenceStore dès la sélection.
 */
@Composable
private fun FlowRowLanguages(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SUPPORTED_CONTENT_LANGUAGES) { lang ->
            FilterChip(
                selected = lang.code == selected,
                onClick = { onSelect(lang.code) },
                label = { Text(lang.label) }
            )
        }
    }
}
