package com.fastt.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fastt_prefs")

class SettingsStore(private val context: Context) {
    private val themeKey = stringPreferencesKey(PrefsKeys.THEME)
    private val langKey = stringPreferencesKey(PrefsKeys.LANG)
    private val accentKey = intPreferencesKey(PrefsKeys.ACCENT)
    private val apiKeyKey = stringPreferencesKey(PrefsKeys.API_KEY)

    val theme: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }
    val lang: Flow<String> = context.dataStore.data.map { it[langKey] ?: "system" }
    val accent: Flow<Int> = context.dataStore.data.map { it[accentKey] ?: 0 }
    val apiKey: Flow<String> = context.dataStore.data.map { it[apiKeyKey] ?: "Neveloopp" }

    suspend fun setTheme(value: String) = context.dataStore.edit { it[themeKey] = value }
    suspend fun setLang(value: String) = context.dataStore.edit { it[langKey] = value }
    suspend fun setAccent(value: Int) = context.dataStore.edit { it[accentKey] = value }
    suspend fun setApiKey(value: String) = context.dataStore.edit { it[apiKeyKey] = value }
}
