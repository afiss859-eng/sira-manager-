package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Sale
import com.example.viewmodel.SiraNavTab
import com.example.viewmodel.SiraViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GlobalSearchDialog(
    viewModel: SiraViewModel,
    onDismiss: () -> Unit,
    onNavigateToTab: (SiraNavTab) -> Unit,
    onSelectSale: (Sale) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.FRENCH) }

    Dialog(
        onDismissRequest = {
            viewModel.clearSearch()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("global_search_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input"),
                    placeholder = { Text("Recherche rapide (Nom, Téléphone, Référence)...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Recherche")
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (query.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ManageSearch,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Recherche globale SIRA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tapez un nom de produit, client, téléphone, N° de facture ou référence...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (results.totalCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun résultat trouvé pour « $query »",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "${results.totalCount} résultat(s) trouvé(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Products section
                        if (results.products.isNotEmpty()) {
                            item {
                                SectionHeader("Produits en stock (${results.products.size})")
                            }
                            items(results.products) { product ->
                                SearchResultCard(
                                    icon = Icons.Default.Inventory2,
                                    title = product.name,
                                    subtitle = "${product.category} • En stock: ${product.quantity}",
                                    badge = "${numberFormat.format(product.salePrice.toLong())} FCFA",
                                    onClick = {
                                        onNavigateToTab(SiraNavTab.STOCK)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Customers section
                        if (results.customers.isNotEmpty()) {
                            item {
                                SectionHeader("Clients & Partenaires (${results.customers.size})")
                            }
                            items(results.customers) { customer ->
                                SearchResultCard(
                                    icon = Icons.Default.Person,
                                    title = customer.fullName,
                                    subtitle = "${customer.phone} • ${customer.address ?: "Burkina Faso"}",
                                    badge = if (customer.creditBalance > 0) "Dû: ${numberFormat.format(customer.creditBalance.toLong())} F" else "À jour",
                                    onClick = {
                                        onNavigateToTab(SiraNavTab.PARTENAIRES)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Sales section
                        if (results.sales.isNotEmpty()) {
                            item {
                                SectionHeader("Ventes & Factures (${results.sales.size})")
                            }
                            items(results.sales) { sale ->
                                SearchResultCard(
                                    icon = Icons.Default.Receipt,
                                    title = "Facture ${sale.reference} - ${sale.customerName}",
                                    subtitle = "${sale.paymentMethod.label} • ${sale.status.label}",
                                    badge = "${numberFormat.format(sale.totalAmount.toLong())} FCFA",
                                    onClick = {
                                        onSelectSale(sale)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Purchases section
                        if (results.purchases.isNotEmpty()) {
                            item {
                                SectionHeader("Achats & Ravitaillements (${results.purchases.size})")
                            }
                            items(results.purchases) { purchase ->
                                SearchResultCard(
                                    icon = Icons.Default.ShoppingBag,
                                    title = "Achat ${purchase.reference}",
                                    subtitle = "Fournisseur: ${purchase.supplierName}",
                                    badge = "${numberFormat.format(purchase.totalAmount.toLong())} FCFA",
                                    onClick = {
                                        onNavigateToTab(SiraNavTab.VENTES)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // Cash Transactions
                        if (results.cashTransactions.isNotEmpty()) {
                            item {
                                SectionHeader("Caisse & Opérations (${results.cashTransactions.size})")
                            }
                            items(results.cashTransactions) { tx ->
                                SearchResultCard(
                                    icon = Icons.Default.AccountBalanceWallet,
                                    title = "${tx.type.label} (${tx.channel.label})",
                                    subtitle = tx.description,
                                    badge = "${if (tx.type.isCredit) "+" else "-"}${numberFormat.format(tx.amount.toLong())} FCFA",
                                    onClick = {
                                        onNavigateToTab(SiraNavTab.CAISSE)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SearchResultCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
