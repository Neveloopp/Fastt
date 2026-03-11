package com.fastt.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Términos y condiciones") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("1) Uso", style = MaterialTheme.typography.titleMedium)
            Text(
                "Fastt solo facilita descargar el archivo de video usando un enlace que tú pegas. " +
                "Eres responsable del contenido y de respetar derechos de autor y las normas de la plataforma.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("2) Privacidad", style = MaterialTheme.typography.titleMedium)
            Text(
                "La app no crea cuentas ni pide login. El enlace que pegas se envía a la API para obtener el videoUrl. " +
                "No guardamos historial (salvo ajustes locales como tema/idioma/acento).",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("3) API", style = MaterialTheme.typography.titleMedium)
            Text(
                "La app usa la API indicada por ti. Si la API cambia, se cae o bloquea, la app puede dejar de funcionar.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("4) Descargas", style = MaterialTheme.typography.titleMedium)
            Text(
                "Las descargas se guardan en la carpeta Descargas del dispositivo con el nombre Fastt_<id>.mp4.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("5) Contacto", style = MaterialTheme.typography.titleMedium)
            Text(
                "Si quieres, agrega tu contacto aquí antes de publicar.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
