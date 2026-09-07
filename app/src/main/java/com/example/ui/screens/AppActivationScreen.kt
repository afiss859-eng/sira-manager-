package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.license.AppLicenseConfig
import com.example.viewmodel.SiraViewModel
import kotlinx.coroutines.launch

@Composable
fun AppActivationScreen(
    viewModel: SiraViewModel,
    licenseConfig: AppLicenseConfig,
    onActivated: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0F1D),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("app_activation_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SIRA Official Emblem
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.sira_logo),
                        contentDescription = "Logo Officiel SIRA",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title & Slogan
            Text(
                text = "ORGANISATION SIRA",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                color = Color.White
            )

            Text(
                text = "Chemin d'aujourd'hui, Avenir de demain",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp)
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYSTÈME SÉCURISÉ SOUS LICENCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Activation Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (licenseConfig.isRevoked) "Licence Révoquée" else "Activation par Clé API",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (licenseConfig.isRevoked) Color(0xFFF87171) else Color.White
                    )

                    Text(
                        text = if (licenseConfig.isRevoked) {
                            "Cette application a été suspendue à distance depuis le panneau d'administration central. Contactez l'administrateur pour renouveler votre accès."
                        } else {
                            "Pour démarrer l'application, veuillez saisir la clé API de licence délivrée par le panel administrateur SIRA."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                    )

                    // Error Banner
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF450A0A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Key Input Field
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it.uppercase()
                            errorMessage = null
                        },
                        label = { Text("Clé API de l'Application") },
                        placeholder = { Text("Ex: SIRA-VIP-BURKINA-2026") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF38BDF8))
                        },
                        trailingIcon = {
                            if (apiKeyInput.isNotBlank()) {
                                IconButton(onClick = { apiKeyInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = Color.LightGray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF020617),
                            unfocusedContainerColor = Color(0xFF020617)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input_field")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Activate Button
                    Button(
                        onClick = {
                            if (apiKeyInput.isBlank()) {
                                errorMessage = "Veuillez entrer une clé API."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val result = viewModel.activateLicense(apiKeyInput)
                                isLoading = false
                                if (result.isSuccess) {
                                    onActivated()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Erreur d'activation."
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("activate_license_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Vérification auprès du Cloud...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activer l'Application", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Pre-filled VIP pilot key shortcut for immediate testing
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(
                        onClick = {
                            apiKeyInput = "SIRA-VIP-BURKINA-2026"
                            errorMessage = null
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Insérer Clé Démo Officielle (SIRA-VIP-BURKINA-2026)",
                            fontSize = 11.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal & Proprietary Notice
            Text(
                text = "© Tous droits réservés à SIRA • Souveraineté & Protection des Données",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
