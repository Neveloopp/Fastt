package com.fastt.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fastt.app.R

@Composable
fun SettingsScreen(
    themeMode: String,
    langMode: String,
    accentIndex: Int,
    onThemeChange: (String) -> Unit,
    onLangChange: (String) -> Unit,
    onAccentChange: (Int) -> Unit,
    onBack: () -> Unit,
    onOpenTerms: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_fastt),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Column {
                    Text("Fastt", style = MaterialTheme.typography.titleLarge)
                    Text("Downloader", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionCard(
                title = stringResource(id = R.string.theme),
                icon = Icons.Filled.DarkMode
            ) {
                SegmentedChoice(
                    options = listOf(
                        "system" to stringResource(id = R.string.theme_system),
                        "light" to stringResource(id = R.string.theme_light),
                        "dark" to stringResource(id = R.string.theme_dark)
                    ),
                    selected = themeMode,
                    onSelect = onThemeChange
                )
            }

            SectionCard(
                title = stringResource(id = R.string.language),
                icon = Icons.Filled.Language
            ) {
                SegmentedChoice(
                    options = listOf(
                        "system" to stringResource(id = R.string.language_system),
                        "es" to stringResource(id = R.string.language_es),
                        "en" to stringResource(id = R.string.language_en)
                    ),
                    selected = langMode,
                    onSelect = onLangChange
                )
            }

            SectionCard(
                title = stringResource(id = R.string.customize),
                icon = Icons.Filled.Brush
            ) {
                AccentPicker(
                    selectedIndex = accentIndex,
                    onSelect = onAccentChange
                )
            }

            ElevatedCard {
                ListItem(
                    headlineContent = { Text(stringResource(id = R.string.terms)) },
                    supportingContent = { Text("Lee los términos, privacidad y uso de la API.") },
                    leadingContent = { Icon(Icons.Filled.Policy, contentDescription = null) },
                    trailingContent = { Text("→") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Divider()
                TextButton(
                    onClick = onOpenTerms,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) { Text("Abrir") }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun SegmentedChoice(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { idx, (key, label) ->
            SegmentedButton(
                selected = selected == key,
                onClick = { onSelect(key) },
                shape = SegmentedButtonDefaults.itemShape(idx, options.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}

@Composable
private fun AccentPicker(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val count = accentOptionsCount()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(id = R.string.accent), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until count) {
                val selected = i == selectedIndex
                AssistChip(
                    onClick = { onSelect(i) },
                    label = { Text("A${i+1}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
