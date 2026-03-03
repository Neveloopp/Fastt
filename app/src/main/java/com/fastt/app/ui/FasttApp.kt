package com.fastt.app.ui

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fastt.app.data.SettingsStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun FasttApp(
    sharedUrl: String?,
    onSharedUrlConsumed: () -> Unit
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val store = remember { SettingsStore(ctx) }
    val scope = rememberCoroutineScope()

    var theme by remember { mutableStateOf("system") }
    var lang by remember { mutableStateOf("system") }
    var accent by remember { mutableStateOf(0) }
    var lastAppliedLang by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        store.theme.collectLatest { theme = it }
    }
    LaunchedEffect(Unit) {
        store.lang.collectLatest {
            lang = it
            val locales = when (it) {
                "es" -> LocaleListCompat.forLanguageTags("es")
                "en" -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            AppCompatDelegate.setApplicationLocales(locales)

            // Force recreation only when user changes language.
            if (lastAppliedLang != null && lastAppliedLang != it) {
                activity?.recreate()
            }
            lastAppliedLang = it
        }
    }
    LaunchedEffect(Unit) {
        store.accent.collectLatest { accent = it }
    }

    val nav = rememberNavController()

    FasttTheme(themeMode = theme, accentIndex = accent) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    store = store,
                    prefillUrl = sharedUrl,
                    onPrefillConsumed = onSharedUrlConsumed,
                    onOpenSettings = { nav.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    themeMode = theme,
                    langMode = lang,
                    accentIndex = accent,
                    onThemeChange = { scope.launch { store.setTheme(it) } },
                    onLangChange = { scope.launch { store.setLang(it) } },
                    onAccentChange = { scope.launch { store.setAccent(it) } },
                    onBack = { nav.popBackStack() },
                    onOpenTerms = { nav.navigate("terms") }
                )
            }
            composable("terms") {
                TermsScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
