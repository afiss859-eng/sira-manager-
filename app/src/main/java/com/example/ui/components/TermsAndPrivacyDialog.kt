package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*

@Composable
fun TermsAndPrivacyDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("terms_privacy_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with Official Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.sira_logo),
                            contentDescription = "Logo Officiel SIRA",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Conditions & Confidentialité",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "Organisation SIRA • Tous droits réservés",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SleekOutline)
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Legal Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Banner SIRA
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SleekBlueContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = SleekBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "« Chemin d'aujourd'hui, Avenir de demain »\nProtection stricte et souveraineté totale de vos données commerciales.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SleekBlueOnContainer
                            )
                        }
                    }

                    LegalSection(
                        title = "1. Propriété Exclusive & Tous Droits Réservés",
                        content = "L'ensemble des marques, logos, identités visuelles, algorithmes, logiciels et bases de données afférents à la plateforme « SIRA » constituent la propriété exclusive de l'Organisation SIRA. Toute contrefaçon ou reproduction non autorisée est rigoureusement interdite par les lois nationales et internationales. Tous droits réservés à SIRA."
                    )

                    LegalSection(
                        title = "2. Architecture 100% Offline-First & Bases Locales",
                        content = "Chaque commerçant enregistré bénéficie d'une base de données locale autonome chiffrée sur son appareil. Aucune transaction, solde de trésorerie ou liste de clients n'est transmise à des tiers sans l'accord explicite de l'utilisateur. En cas de synchronisation cloud, le chiffrement de bout en bout est appliqué."
                    )

                    LegalSection(
                        title = "3. Vérification d'Identité (KYC) & Moteur OCR",
                        content = "Le moteur OCR intégré a pour unique fin de faciliter la saisie rapide des coordonnées du client pour l'émission des factures et la tenue du carnet de crédits. Le commerçant demeure seul responsable de la conformité de l'identification physique de ses clients burkinabè et étrangers."
                    )

                    LegalSection(
                        title = "4. Gestion Sécurisée des Clés API & Contrôle Central",
                        content = "Les clés d'accès aux passerelles monétiques (Orange Money, CinetPay, PayDunya) et aux services d'Intelligence Artificielle sont sous le contrôle exclusif des administrateurs autorisés. Les commerçants réguliers opèrent en mode lecture/écriture sur leurs flux de vente locaux."
                    )

                    LegalSection(
                        title = "5. Loi Applicable & Protection des Données Personnelles",
                        content = "Les présentes conditions sont régies par la législation en vigueur au Burkina Faso, notamment la Loi N° 001-2021/AN portant protection des personnes à l'égard du traitement des données à caractère personnel."
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("J'ai lu et j'accepte les conditions", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = SleekTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = SleekTextSecondary
        )
    }
}
