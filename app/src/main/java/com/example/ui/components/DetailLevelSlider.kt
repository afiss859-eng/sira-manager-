package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailLevelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = when {
        value < 0.34f -> "Essentiel"
        value < 0.67f -> "Standard"
        else -> "Complet"
    }
    val hint = when (level) {
        "Essentiel" -> "Affiche uniquement les informations indispensables."
        "Standard" -> "Affichage équilibré pour la gestion quotidienne."
        else -> "Affiche les informations avancées et les détails supplémentaires."
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Niveau de détail", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(level, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(hint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Essentiel", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Standard", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Complet", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
