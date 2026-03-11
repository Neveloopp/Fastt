package com.fastt.app.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fastt.app.R
import com.fastt.app.data.SettingsStore
import com.fastt.app.model.TiktokData
import com.fastt.app.net.TiktokApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    store: SettingsStore,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<TiktokData?>(null) }

    val apiKey by store.apiKey.collectAsState(initial = "Neveloopp")

    val api = remember(apiKey) { TiktokApi { apiKey } }

    val infinite = rememberInfiniteTransition(label = "spin")
    val rot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinVal"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(id = R.string.open_settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snack) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // simple "animation": rotate the logo slightly
                Image(
                    painter = painterResource(id = R.drawable.logo_fastt),
                    contentDescription = null,
                    modifier = Modifier.size(84.dp)
                )
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.url_hint)) },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val url = input.trim()
                        if (url.isEmpty() || !url.contains("tiktok", ignoreCase = true)) {
                            scope.launch { snack.showSnackbar(ctx.getString(R.string.invalid_url)) }
                            return@Button
                        }
                        loading = true
                        data = null
                        scope.launch {
                            val res = withContext(Dispatchers.IO) { api.fetch(url) }
                            loading = false
                            res.onSuccess {
                                if (it.success && it.data != null) data = it.data
                                else snack.showSnackbar(ctx.getString(R.string.api_error))
                            }.onFailure {
                                snack.showSnackbar(ctx.getString(R.string.network_error) + " " + (it.message ?: ""))
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.fetch))
                }

                OutlinedButton(
                    onClick = { input = "" },
                    enabled = !loading,
                ) { Text("Clear") }
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AnimatedVisibility(visible = data != null) {
                data?.let { d ->
                    ResultCard(
                        data = d,
                        onDownload = { startDownload(ctx, d, snack, scope) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tip: pega cualquier link de TikTok (video / vm.tiktok.com / etc.) y dale Buscar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultCard(
    data: TiktokData,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = data.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(110.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@${data.author ?: "-"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = data.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(
                            data.views_formatted?.let { "👀 $it" },
                            data.likes_formatted?.let { "❤️ $it" },
                            data.comments_formatted?.let { "💬 $it" },
                            data.shares_formatted?.let { "🔁 $it" }
                        ).joinToString("   "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDownload,
                    enabled = !data.videoUrl.isNullOrBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.download))
                }

                OutlinedButton(
                    onClick = { /* future: share */ },
                    modifier = Modifier.width(110.dp)
                ) { Text("Share") }
            }

            Text(
                text = "Calidad: ${data.quality ?: "-"} • Resolución: ${data.resolution ?: "-"} • Duración: ${data.duration ?: 0}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun startDownload(
    context: Context,
    data: TiktokData,
    snack: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val url = data.videoUrl ?: return
    val filename = "Fastt_${data.id ?: System.currentTimeMillis()}.mp4"
    val request = DownloadManager.Request(Uri.parse(url)).apply {
        setTitle(filename)
        setDescription("TikTok video")
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
    }
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
    scope.launch { snack.showSnackbar(context.getString(R.string.download_started)) }
}
