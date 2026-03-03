package com.fastt.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fastt.app.ui.FasttApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var sharedUrlState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        // Apply language BEFORE composing UI, otherwise string resources won't refresh reliably.
        runBlocking {
            try {
                val key = stringPreferencesKey(com.fastt.app.data.PrefsKeys.LANG)
                val lang = applicationContext.dataStore.data.first()[key] ?: "system"
                val locales = when (lang) {
                    "es" -> LocaleListCompat.forLanguageTags("es")
                    "en" -> LocaleListCompat.forLanguageTags("en")
                    else -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(locales)
            } catch (_: Exception) {
            }
        }

        super.onCreate(savedInstanceState)

        sharedUrlState = extractSharedTiktokUrl(intent)
        setContent {
            FasttApp(
                sharedUrl = sharedUrlState,
                onSharedUrlConsumed = { sharedUrlState = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedUrlState = extractSharedTiktokUrl(intent)
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

private val android.content.Context.dataStore by preferencesDataStore(name = "fastt_prefs")
