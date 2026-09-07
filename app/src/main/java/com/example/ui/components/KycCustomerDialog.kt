package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ai.KycOcrResult
import com.example.data.entity.Customer
import com.example.data.entity.IdDocumentType
import com.example.data.entity.KycStatus
import com.example.viewmodel.SiraViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.entity.CashChannel
import com.example.data.entity.CashOpType
import com.example.data.entity.CashTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycCustomerDialog(
    initialCustomer: Customer? = null,
    viewModel: SiraViewModel,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(initialCustomer?.fullName ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "+226 ") }
    var email by remember { mutableStateOf(initialCustomer?.email ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "Ouagadougou") }
    var idDocType by remember { mutableStateOf(initialCustomer?.idDocumentType ?: IdDocumentType.CNIB) }
    var idDocNumber by remember { mutableStateOf(initialCustomer?.idDocumentNumber ?: "") }
    var kycNotes by remember { mutableStateOf(initialCustomer?.kycNotes ?: "") }
    var isKycVerified by remember { mutableStateOf(initialCustomer?.kycStatus == KycStatus.VERIFIE) }
    var photoUriString by remember { mutableStateOf(initialCustomer?.idDocumentPhotoUri) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var docTypeExpanded by remember { mutableStateOf(false) }
    var attachTransaction by remember { mutableStateOf(false) }
    var txAmountText by remember { mutableStateOf("") }
    var txFeeText by remember { mutableStateOf("") }
    var txChannel by remember { mutableStateOf(CashChannel.ORANGE_MONEY) }
    var txChannelExpanded by remember { mutableStateOf(false) }

    val isOcrLoading by viewModel.isOcrLoading.collectAsState()
    val ocrResult by viewModel.ocrStatus.collectAsState()

    // Image Picker (zero-permission modern PickVisualMedia)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bitmap
            } catch (e: Exception) {
                // Ignore decoding error
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("kyc_customer_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialCustomer == null) "Nouveau Client & KYC" else "Dossier Client / KYC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // KYC Compliance Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exigence KYC Burkina Faso : La conformité de la pièce d'identité doit être vérifiée physiquement par le commerçant avant toute certification ou crédit.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    // ID Document Photo Capture / Selection
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Photo du document d'identité (CNIB, Passeport)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedBitmap != null) {
                                Image(
                                    bitmap = selectedBitmap!!.asImageBitmap(),
                                    contentDescription = "Document d'identité",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (!photoUriString.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUriString,
                                    contentDescription = "Document d'identité",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                                        Text("Aucun document scanné", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("select_id_photo_button")
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Photographier", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (selectedBitmap != null) {
                                            viewModel.runOcrOnDocument(selectedBitmap!!) { res ->
                                                if (res.fullName.isNotBlank()) fullName = res.fullName
                                                if (res.documentNumber.isNotBlank()) idDocNumber = res.documentNumber
                                                idDocType = if (res.documentType.contains("PASSPORT", ignoreCase = true) || res.documentType.contains("PASSEPORT", ignoreCase = true)) {
                                                    IdDocumentType.PASSEPORT
                                                } else if (res.documentType.contains("PERMIS", ignoreCase = true)) {
                                                    IdDocumentType.PERMIS
                                                } else {
                                                    IdDocumentType.CNIB
                                                }
                                                kycNotes = "Extrait OCR SIRA : Confiance ${res.confidenceScore}% (${res.documentType})"
                                            }
                                        }
                                    },
                                    enabled = selectedBitmap != null && !isOcrLoading,
                                    modifier = Modifier.weight(1f).testTag("run_ocr_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    if (isOcrLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("OCR IA SIRA", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Customer information fields
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nom et Prénoms *") },
                        modifier = Modifier.fillMaxWidth().testTag("customer_fullname_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Numéro de téléphone (WhatsApp / Orange / Moov) *") },
                        modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Ville / Quartier (ex: Ouagadougou, Gounghin)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                    )

                    // ID Document Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = docTypeExpanded,
                        onExpandedChange = { docTypeExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = idDocType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type de document d'identité") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = docTypeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = docTypeExpanded,
                            onDismissRequest = { docTypeExpanded = false }
                        ) {
                            IdDocumentType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        idDocType = type
                                        docTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = idDocNumber,
                        onValueChange = { idDocNumber = it },
                        label = { Text("Numéro de la pièce (ex: B14285901) *") },
                        modifier = Modifier.fillMaxWidth().testTag("customer_id_number_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = kycNotes,
                        onValueChange = { kycNotes = it },
                        label = { Text("Notes de vérification KYC") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    // Mandatory verification switch
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Certification de conformité KYC", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Je confirme avoir examiné l'original de la pièce.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isKycVerified,
                                onCheckedChange = { isKycVerified = it },
                                modifier = Modifier.testTag("kyc_verified_switch")
                            )
                        }
                    }

                    // Opération Caisse / Mobile Money immédiate (avec notification directe)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Valider une transaction associée",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Déclenche la notification avec ID, commissions et soldes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = attachTransaction,
                                    onCheckedChange = { attachTransaction = it },
                                    modifier = Modifier.testTag("attach_transaction_switch")
                                )
                            }

                            if (attachTransaction) {
                                Spacer(modifier = Modifier.height(10.dp))

                                // Channel Selection
                                ExposedDropdownMenuBox(
                                    expanded = txChannelExpanded,
                                    onExpandedChange = { txChannelExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = txChannel.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Canal de paiement") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = txChannelExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = txChannelExpanded,
                                        onDismissRequest = { txChannelExpanded = false }
                                    ) {
                                        CashChannel.values().forEach { ch ->
                                            DropdownMenuItem(
                                                text = { Text(ch.label) },
                                                onClick = {
                                                    txChannel = ch
                                                    txChannelExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = txAmountText,
                                        onValueChange = { txAmountText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Montant (FCFA) *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1.2f).testTag("kyc_tx_amount_input"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = txFeeText,
                                        onValueChange = { txFeeText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Commission") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.8f).testTag("kyc_tx_fee_input"),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val customer = Customer(
                                id = initialCustomer?.id ?: 0L,
                                fullName = fullName.trim(),
                                phone = phone.trim(),
                                email = email.trim().ifBlank { null },
                                address = address.trim().ifBlank { "Burkina Faso" },
                                idDocumentType = idDocType,
                                idDocumentNumber = idDocNumber.trim().ifBlank { null },
                                idDocumentPhotoUri = photoUriString,
                                kycStatus = if (isKycVerified) KycStatus.VERIFIE else if (idDocNumber.isNotBlank()) KycStatus.EN_ATTENTE else KycStatus.NON_FOURNI,
                                kycNotes = kycNotes.ifBlank { null },
                                creditBalance = initialCustomer?.creditBalance ?: 0.0
                            )

                            if (attachTransaction && (txAmountText.toDoubleOrNull() ?: 0.0) > 0.0) {
                                val amount = txAmountText.toDoubleOrNull() ?: 0.0
                                val fee = txFeeText.toDoubleOrNull() ?: 0.0
                                val tx = CashTransaction(
                                    reference = "TX-KYC-" + System.currentTimeMillis().toString().takeLast(6),
                                    type = CashOpType.DEPOT,
                                    channel = txChannel,
                                    amount = amount,
                                    fee = fee,
                                    beneficiaryOrPayer = customer.fullName,
                                    phone = customer.phone,
                                    description = "Opération validée post-scan KYC (CNIB/Doc: ${customer.idDocumentNumber ?: "N/A"})"
                                )
                                viewModel.recordCashOp(tx)
                            }

                            onSave(customer)
                        },
                        enabled = fullName.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_customer_button")
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}
