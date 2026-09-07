package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.auth.GoogleAuthManager
import com.example.auth.SiraAuthUser
import com.example.auth.SiraRole
import com.example.ui.theme.*

@Composable
fun GoogleAuthDialog(
    authManager: GoogleAuthManager,
    onDismiss: () -> Unit,
    onUserSwitched: (SiraAuthUser) -> Unit
) {
    val currentUser by authManager.currentUser.collectAsState()
    var isNewLoginMode by remember { mutableStateOf(false) }
    var inputEmail by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var inputShop by remember { mutableStateOf("") }
    var inputCity by remember { mutableStateOf("Ouagadougou") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("google_auth_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.sira_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Authentification SIRA",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "Compte Google & Accès aux bases",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Active Session
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SleekSurfaceVariant,
                    border = BorderStroke(1.dp, SleekOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (currentUser.role == SiraRole.SUPER_ADMIN) SleekBluePrimary else SleekSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.displayName.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser.displayName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (currentUser.role == SiraRole.SUPER_ADMIN) Color(0xFFFEF3C7) else SleekBlueContainer
                                ) {
                                    Text(
                                        text = if (currentUser.role == SiraRole.SUPER_ADMIN) "ADMIN" else "MARCHAND",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentUser.role == SiraRole.SUPER_ADMIN) Color(0xFFD97706) else SleekBlueOnContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentUser.email,
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                            Text(
                                text = "Base Locale : sira_user_${currentUser.id}.db",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SleekBluePrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isNewLoginMode) {
                    Text(
                        text = "COMPTES DISPONIBLES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Switch: Admin Account
                    AccountOptionRow(
                        email = "sawadogoafis125@gmail.com",
                        name = "Afis Sawadogo",
                        roleLabel = "Super Administrateur (Centre de Contrôle)",
                        isAdmin = true,
                        isSelected = currentUser.email.equals("sawadogoafis125@gmail.com", ignoreCase = true),
                        onClick = {
                            val user = authManager.signInWithGoogle(
                                email = "sawadogoafis125@gmail.com",
                                displayName = "Afis Sawadogo",
                                shopName = "SIRA Direction Centrale",
                                city = "Ouagadougou"
                            )
                            onUserSwitched(user)
                            onDismiss()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { isNewLoginMode = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecter un autre compte Google")
                    }
                } else {
                    // Form to sign in with any Google account
                    Text(
                        text = "CONNEXION NOUVEAU COMPTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Adresse Email Google") },
                        placeholder = { Text("exemple@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nom complet du commerçant") },
                        placeholder = { Text("Ex: Votre nom complet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputShop,
                        onValueChange = { inputShop = it },
                        label = { Text("Nom de la boutique") },
                        placeholder = { Text("Ex: Nom de votre commerce") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isNewLoginMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retour")
                        }

                        Button(
                            onClick = {
                                if (inputEmail.isNotBlank()) {
                                    val user = authManager.signInWithGoogle(
                                        email = inputEmail,
                                        displayName = inputName,
                                        shopName = inputShop,
                                        city = inputCity
                                    )
                                    onUserSwitched(user)
                                    onDismiss()
                                }
                            },
                            enabled = inputEmail.contains("@"),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary)
                        ) {
                            Text("Se connecter")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountOptionRow(
    email: String,
    name: String,
    roleLabel: String,
    isAdmin: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SleekBlueContainer.copy(alpha = 0.6f) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) SleekBluePrimary else SleekOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isAdmin) SleekBluePrimary else Color(0xFF6B7280)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SleekTextPrimary
                )
                Text(
                    text = email,
                    fontSize = 11.sp,
                    color = SleekTextSecondary
                )
                Text(
                    text = roleLabel,
                    fontSize = 10.sp,
                    color = if (isAdmin) SleekBluePrimary else SleekSecondary
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SleekBluePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
