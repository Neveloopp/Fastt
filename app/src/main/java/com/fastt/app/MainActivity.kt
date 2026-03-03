package com.fastt.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fastt.app.ui.FasttApp

/**
 * NOTE:
 * - Avoid blocking disk IO before super.onCreate() (can crash on some devices/ROMs).
 * - Language is applied reactively inside FasttApp via SettingsStore.
 */
class MainActivity : AppCompatActivity() {

    private var sharedUrlState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
