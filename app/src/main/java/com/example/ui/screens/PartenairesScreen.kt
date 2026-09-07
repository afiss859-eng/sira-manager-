package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Customer
import com.example.data.entity.KycStatus
import com.example.data.entity.Supplier
import com.example.ui.theme.*
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PartenairesScreen(
    viewModel: SiraViewModel,
    onAddCustomerClick: () -> Unit,
    onEditCustomerClick: (Customer) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers else customers.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true) ||
                    (it.idDocumentNumber?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedSubTab == 0) {
                FloatingActionButton(
                    onClick = onAddCustomerClick,
                    containerColor = SleekBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_customer")
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "Ajouter Client") }
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).testTag("partenaires_screen")) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Clients & partenaires", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, color = SleekTextPrimary)
                Text("Identité, crédit et fournisseurs au même endroit", fontSize = 12.sp, color = SleekTextSecondary)
                Spacer(Modifier.height(12.dp))
                Surface(shape = SiraPillShape, color = SleekSurfaceVariant, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SleekTextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                            if (searchQuery.isBlank()) Text("Rechercher un client, téléphone ou CNIB", fontSize = 12.sp, color = SleekTextTertiary)
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = SleekTextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = SleekTextSecondary, modifier = Modifier.size(16.dp)) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PartnerTab("Clients", customers.size, selectedSubTab == 0) { selectedSubTab = 0 }
                    PartnerTab("Fournisseurs", suppliers.size, selectedSubTab == 1) { selectedSubTab = 1 }
                }
            }

            if (selectedSubTab == 0) {
                Surface(shape = SiraCardShape, color = Color(0xFFFFF8E7), border = BorderStroke(1.dp, Color(0xFFFFE6A8)), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFFFEDB3), modifier = Modifier.size(36.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF9A6700), modifier = Modifier.size(18.dp)) } }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("KYC avant crédit", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF7A4F00))
                            Text("CNIB ou passeport requis pour sécuriser un compte à crédit.", fontSize = 11.sp, color = Color(0xFF8A5A00))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (filteredCustomers.isEmpty()) EmptyPartnerState("Aucun client trouvé", "Ajoutez un client pour commencer.", Icons.Default.PersonAdd)
                else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredCustomers, key = { it.id }) { customer -> CustomerKycCard(customer, numberFormat) { onEditCustomerClick(customer) } }
                }
            } else {
                if (suppliers.isEmpty()) EmptyPartnerState("Aucun fournisseur", "Les fournisseurs enregistrés apparaîtront ici.", Icons.Default.LocalShipping)
                else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(suppliers, key = { it.id }) { SupplierCard(it, numberFormat) }
                }
            }
        }
    }
}

@Composable
private fun PartnerTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = SiraPillShape, color = if (selected) SleekBlueContainer else SleekSurface, border = BorderStroke(1.dp, if (selected) SleekBluePrimary.copy(alpha = 0.16f) else SleekOutline.copy(alpha = 0.65f)), modifier = Modifier.weight(1f).clickable { onClick() }) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) SleekBlueOnContainer else SleekTextSecondary)
            Spacer(Modifier.width(6.dp)); Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) SleekBlueOnContainer else SleekTextTertiary)
        }
    }
}

@Composable
private fun CustomerKycCard(customer: Customer, numberFormat: NumberFormat, onClick: () -> Unit) {
    val badge = when (customer.kycStatus) {
        KycStatus.VERIFIE -> Triple(Color(0xFFEAF8EE), "KYC CERTIFIÉ", Color(0xFF18864B))
        KycStatus.EN_ATTENTE -> Triple(Color(0xFFFFF6DB), "EN ATTENTE", Color(0xFF9A6700))
        KycStatus.NON_FOURNI -> Triple(SleekSurfaceVariant, "SANS KYC", SleekTextTertiary)
    }
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, if (customer.creditBalance > 0) SleekError.copy(alpha = 0.2f) else SleekOutline.copy(alpha = 0.7f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Text(customer.fullName.take(2).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekBlueOnContainer) } }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(customer.fullName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                    Text("${customer.phone} • ${customer.address ?: "Burkina Faso"}", fontSize = 11.sp, color = SleekTextSecondary, maxLines = 1)
                }
                Surface(shape = SiraPillShape, color = badge.first) { Text(badge.second, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badge.third) }
            }
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = SleekOutline.copy(alpha = 0.5f)); Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Badge, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(if (!customer.idDocumentNumber.isNullOrBlank()) "${customer.idDocumentType.label} • ${customer.idDocumentNumber}" else "Pièce d’identité non fournie", fontSize = 10.sp, color = SleekTextSecondary, maxLines = 1) }
                Text(if (customer.creditBalance > 0) "Dette ${numberFormat.format(customer.creditBalance.toLong())} F" else "Solde 0 F", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (customer.creditBalance > 0) SleekError else SleekSuccess)
            }
        }
    }
}

@Composable
private fun SupplierCard(supplier: Supplier, numberFormat: NumberFormat) {
    Surface(shape = SiraCardShape, color = SleekSurface, border = BorderStroke(1.dp, SleekOutline.copy(alpha = 0.7f)), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = SleekSecondaryContainer, modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = SleekSecondary, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(supplier.companyName ?: supplier.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                    Text("${supplier.name} • ${supplier.phone}", fontSize = 11.sp, color = SleekTextSecondary, maxLines = 1)
                }
                Text(if (supplier.balanceOwed > 0) "À payer" else "Équilibré", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (supplier.balanceOwed > 0) SleekError else SleekSuccess)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(supplier.address ?: "Ouagadougou", fontSize = 10.sp, color = SleekTextTertiary)
                if (supplier.balanceOwed > 0) Text("${numberFormat.format(supplier.balanceOwed.toLong())} FCFA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SleekError)
            }
        }
    }
}

@Composable
private fun EmptyPartnerState(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = SleekBlueContainer, modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = SleekBluePrimary, modifier = Modifier.size(25.dp)) } }
            Spacer(Modifier.height(10.dp)); Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary); Spacer(Modifier.height(4.dp)); Text(message, fontSize = 11.sp, color = SleekTextSecondary)
        }
    }
}
