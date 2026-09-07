package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.license.AppLicenseConfig
import com.example.license.KeypadLayoutType
import com.example.ui.theme.SleekBluePrimary
import com.example.viewmodel.SiraViewModel
import kotlinx.coroutines.launch

@Composable
fun AppCustomizationDialog(
    viewModel: SiraViewModel,
    licenseConfig: AppLicenseConfig,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var appName by remember { mutableStateOf(licenseConfig.appName) }
    var shopName by remember { mutableStateOf(licenseConfig.shopName) }
    var selectedColor by remember { mutableStateOf(licenseConfig.themeColorHex) }
    var logoUrl by remember { mutableStateOf(licenseConfig.logoUrl) }
    var bgUrl by remember { mutableStateOf(licenseConfig.bgUrl) }
    var selectedKeypad by remember { mutableStateOf(licenseConfig.keypadLayout) }
    var cguText by remember { mutableStateOf(licenseConfig.cguText) }
    var privacyText by remember { mutableStateOf(licenseConfig.privacyText) }
    var isSaving by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#005AC1" to "Bleu SIRA",
        "#146C2E" to "Vert Émeraude",
        "#B43403" to "Terre Cuite",
        "#6750A4" to "Pourpre Royal",
        "#0F172A" to "Noir Onyx",
        "#C2410C" to "Orange Sahel"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("app_customization_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SleekBluePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = SleekBluePrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Personnalisation Complète",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Configurée via Clé : ${licenseConfig.key}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Customization Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. App Name
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("Nom de l'application affiché") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Shop Name
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Nom du Commerce / Boutique") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Theme Colors (Background & Accent)
                    Column {
                        Text(
                            text = "Couleur Principale & Fond Thème",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetColors.forEach { (hex, name) ->
                                val color = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (e: Exception) {
                                    Color.Blue
                                }
                                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Keypad Layout (Disposition des touches)
                    Column {
                        Text(
                            text = "Disposition des Touches / Clavier",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        KeypadLayoutType.values().forEach { layout ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedKeypad == layout) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedKeypad = layout }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedKeypad == layout,
                                        onClick = { selectedKeypad = layout }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = layout.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            text = when(layout) {
                                                KeypadLayoutType.GRID_4 -> "Disposition classique 4 colonnes rapide"
                                                KeypadLayoutType.GRID_2 -> "2 gros boutons tactiles ergonomiques"
                                                KeypadLayoutType.LIST_COMPACT -> "Format liste fluide"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Logo & Photo URL
                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("URL Image / Photo du Logo Personnalisé") },
                        placeholder = { Text("https://... ou laisser vide pour logo SIRA officiel") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 6. Background Image URL
                    OutlinedTextField(
                        value = bgUrl,
                        onValueChange = { bgUrl = it },
                        label = { Text("URL Photo de Fond (Wallpaper)") },
                        placeholder = { Text("https://... ou laisser vide") },
                        leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 7. Conditions Générales d'Utilisation
                    OutlinedTextField(
                        value = cguText,
                        onValueChange = { cguText = it },
                        label = { Text("Conditions d'utilisation personnalisées (CGU)") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 8. Confidentialité
                    OutlinedTextField(
                        value = privacyText,
                        onValueChange = { privacyText = it },
                        label = { Text("Paramètres & Politique de confidentialité") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                viewModel.saveLicenseCustomization(
                                    appName = appName,
                                    shopName = shopName,
                                    themeColorHex = selectedColor,
                                    logoUrl = logoUrl,
                                    bgUrl = bgUrl,
                                    cguText = cguText,
                                    privacyText = privacyText,
                                    keypadLayout = selectedKeypad
                                )
                                isSaving = false
                                onDismiss()
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Text("Enregistrer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
