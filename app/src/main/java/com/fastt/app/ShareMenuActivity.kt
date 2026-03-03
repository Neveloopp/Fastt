package com.fastt.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fastt.app.data.SettingsStore
import com.fastt.app.model.TiktokData
import com.fastt.app.net.TiktokApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Share target Activity (transparent) that shows a mini menu over the caller app (TikTok),
 * so the user can download without feeling "kicked out" of TikTok.
 */
class ShareMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = extractSharedTiktokUrl(intent)
        if (sharedUrl.isNullOrBlank()) {
            finish()
            return
        }

        val store = SettingsStore(this)

        setContent {
            ShareMiniMenu(
                store = store,
                url = sharedUrl,
                onClose = { finish() }
            )
        }
    }

    private fun extractSharedTiktokUrl(intent: Intent?): String? {
        if (intent == null) return null
        val text = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return null

        val trimmed = text.trim()
        return if (trimmed.contains("tiktok", ignoreCase = true)) trimmed else null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareMiniMenu(
    store: SettingsStore,
    url: String,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    val apiKey by store.apiKey.collectAsState(initial = "Neveloopp")
    val api = remember(apiKey) { TiktokApi { apiKey } }

    var loading by remember { mutableStateOf(true) }
    var data by remember { mutableStateOf<TiktokData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(url, apiKey) {
        loading = true
        error = null
        data = null
        val result = withContext(Dispatchers.IO) { api.fetch(url) }
        if (result.isSuccess) {
            val res = result.getOrNull()
            if (res?.success == true) {
                data = res.data
            } else {
                error = "No se pudo obtener el video"
            }
        } else {
            error = result.exceptionOrNull()?.message ?: "Error"
        }
        loading = false
    }

    // A lightweight bottom sheet "mini menu"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MaterialTheme {
        SnackbarHost(hostState = snack)

        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = sheetState,
            dragHandle = null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fastt", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                }
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    text = error!!,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val d = data
                if (d == null) {
                    Text("No hay datos.", modifier = Modifier.padding(16.dp))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column {
                                AsyncImage(
                                    model = d.thumbnail ?: d.avatar,
                                    contentDescription = "Miniatura",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                                Text(
                                    text = d.description ?: d.url ?: "TikTok",
                                    modifier = Modifier.padding(12.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val ok = enqueueDownload(LocalContext.current, d)
                                scope.launch {
                                    snack.showSnackbar(
                                        if (ok) "Descarga iniciada" else "No se pudo iniciar la descarga"
                                    )
                                }
                                if (ok) onClose()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Descargar")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun enqueueDownload(context: Context, data: TiktokData): Boolean {
    val url = data.videoUrl ?: return false

    fun sanitize(name: String): String {
        val cleaned = name
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9 _.-]"), "")
            .trim()
        return cleaned.take(60).ifBlank { "TikTok" }
    }

    val baseTitle = sanitize(data.description ?: "TikTok")
    val id = data.id ?: System.currentTimeMillis().toString()
    val filename = "${baseTitle}_${id}.mp4"

    val request = DownloadManager.Request(Uri.parse(url)).apply {
        setTitle(baseTitle)
        setDescription("TikTok video")
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Fastt/$filename")
        addRequestHeader("User-Agent", "Mozilla/5.0")
        addRequestHeader("Referer", "https://www.tiktok.com/")
    }

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
    return true
}
