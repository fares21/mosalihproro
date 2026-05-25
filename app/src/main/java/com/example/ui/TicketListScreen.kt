package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.MainViewModel
import com.example.database.Ticket
import com.example.utils.PdfUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketListScreen(
    viewModel: MainViewModel,
    tickets: List<Ticket>,
    onAddTicketClicked: () -> Unit,
    initialDetailsTicket: Ticket? = null,
    onDetailsClosed: () -> Unit = {}
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val lang = settings["language"] ?: "ar"

    val storeName = settings["store_name"] ?: "متجر صيانة الهواتف"
    val storePhone = settings["store_phone"] ?: "0500000000"
    val currency = settings["store_currency"] ?: "د.إ"

    // Filtering Tabs
    var selectedFilterTab by remember { mutableStateOf("ALL") }
    val filteredTickets = remember(tickets, selectedFilterTab) {
        if (selectedFilterTab == "ALL") {
            tickets
        } else {
            tickets.filter { it.status == selectedFilterTab }
        }
    }

    // Detail Modal State
    var activeDetailsTicket by remember { mutableStateOf<Ticket?>(null) }

    LaunchedEffect(initialDetailsTicket) {
        if (initialDetailsTicket != null) {
            activeDetailsTicket = initialDetailsTicket
        }
    }

    LaunchedEffect(activeDetailsTicket) {
        if (activeDetailsTicket == null && initialDetailsTicket != null) {
            onDetailsClosed()
        }
    }

    // Edit Dialog States
    var isEditingTicket by remember { mutableStateOf(false) }
    var ticketToEdit by remember { mutableStateOf<Ticket?>(null) }

    // Deletion Dialog Confirmation State
    var ticketToDelete by remember { mutableStateOf<Ticket?>(null) }

    // Full Screen Image Preview
    var fullScreenImageFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter header - solid white with subtle bottom border/shadow
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { 
                    Text(
                        text = com.example.utils.Localization.get("search_placeholder", lang), 
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right, 
                        fontSize = 13.sp
                    ) 
                },
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filtering Tab Row styled like M3 chips
            ScrollableTabRow(
                selectedTabIndex = when (selectedFilterTab) {
                    "ALL" -> 0
                    "PENDING" -> 1
                    "IN_PROGRESS" -> 2
                    "PART_NEEDED" -> 3
                    "COMPLETED" -> 4
                    "DELIVERED" -> 5
                    else -> 0
                },
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[
                            when (selectedFilterTab) {
                                "ALL" -> 0
                                "PENDING" -> 1
                                "IN_PROGRESS" -> 2
                                "PART_NEEDED" -> 3
                                "COMPLETED" -> 4
                                "DELIVERED" -> 5
                                else -> 0
                            }
                        ]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(selected = selectedFilterTab == "ALL", onClick = { selectedFilterTab = "ALL" }) {
                    Text(com.example.utils.Localization.get("nav_all", lang).let { if (it == "nav_all") (if (lang == "fr") "Tous" else "الكل") else it }, modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "ALL") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "PENDING", onClick = { selectedFilterTab = "PENDING" }) {
                    Text(com.example.utils.Localization.get("status_pending", lang), modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "PENDING") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "IN_PROGRESS", onClick = { selectedFilterTab = "IN_PROGRESS" }) {
                    Text(com.example.utils.Localization.get("status_inprogress", lang), modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "IN_PROGRESS") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "PART_NEEDED", onClick = { selectedFilterTab = "PART_NEEDED" }) {
                    Text(com.example.utils.Localization.get("status_parts", lang), modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "PART_NEEDED") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "COMPLETED", onClick = { selectedFilterTab = "COMPLETED" }) {
                    Text(com.example.utils.Localization.get("status_completed", lang), modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "COMPLETED") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "DELIVERED", onClick = { selectedFilterTab = "DELIVERED" }) {
                    Text(com.example.utils.Localization.get("status_delivered", lang), modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "DELIVERED") MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }

        // Tickets List
        if (filteredTickets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = com.example.utils.Localization.get("empty_search", lang),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTickets, key = { it.id }) { ticket ->
                    TicketCardItem(
                        ticket = ticket,
                        currency = currency,
                        onClick = { activeDetailsTicket = ticket },
                        lang = lang
                    )
                }
            }
        }
    }

    // Modal detailed View Dialog
    if (activeDetailsTicket != null) {
        val currentTicket = activeDetailsTicket!!
        AlertDialog(
            onDismissRequest = { activeDetailsTicket = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { activeDetailsTicket = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                ticketToEdit = currentTicket
                                isEditingTicket = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = com.example.utils.Localization.get("edit_ticket", lang),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = String.format(com.example.utils.Localization.get("ticket_details_title", lang), currentTicket.id) + " " + currentTicket.id,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .background(Color.Transparent)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
                ) {
                    // Client detail row
                    DialogDetailRow(com.example.utils.Localization.get("client_label_col", lang), currentTicket.customerName, lang = lang)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:${currentTicket.customerPhone}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, com.example.utils.Localization.get("call_failed_toast", lang), Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lang == "fr") {
                            Text(
                                text = com.example.utils.Localization.get("phone_label_col", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Left
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentTicket.customerPhone,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = com.example.utils.Localization.get("call_client_action", lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = com.example.utils.Localization.get("call_client_action", lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentTicket.customerPhone,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = com.example.utils.Localization.get("phone_label_col", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                    DialogDetailRow(com.example.utils.Localization.get("device_label_col", lang), currentTicket.deviceModel, lang = lang)
                    DialogDetailRow(com.example.utils.Localization.get("fault_label_col", lang), currentTicket.faultDescription, lang = lang)
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    DialogDetailRow(com.example.utils.Localization.get("entry_date_col", lang), sdf.format(currentTicket.createdAt), lang = lang)

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Finance block
                    DialogDetailRow(com.example.utils.Localization.get("total_price_col", lang), "${currentTicket.totalPrice} $currency", lang = lang)
                    DialogDetailRow(com.example.utils.Localization.get("advance_payment_col", lang), "${currentTicket.advancePayment} $currency", lang = lang)
                    
                    val remainingColor = if (currentTicket.remainingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    DialogDetailRow(com.example.utils.Localization.get("remaining_payment_col", lang), "${currentTicket.remainingAmount} $currency", valueColor = remainingColor, lang = lang)

                    if (currentTicket.notes.isNotEmpty()) {
                        DialogDetailRow(com.example.utils.Localization.get("additional_notes_col", lang), currentTicket.notes, lang = lang)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Photos preview
                    if (currentTicket.frontImagePath != null || currentTicket.backImagePath != null) {
                        Text(
                            text = com.example.utils.Localization.get("documented_photos_label", lang), 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 13.sp,
                            textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentTicket.frontImagePath?.let {
                                val file = File(it)
                                if (file.exists()) {
                                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { fullScreenImageFile = file }) {
                                        com.example.ui.LocalRenderedImage(file)
                                    }
                                }
                            }
                            currentTicket.backImagePath?.let {
                                val file = File(it)
                                if (file.exists()) {
                                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { fullScreenImageFile = file }) {
                                        com.example.ui.LocalRenderedImage(file)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Modify operations/status section
                    Text(
                        text = com.example.utils.Localization.get("change_ticket_status_label", lang), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 13.sp,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusChangeBtn(com.example.utils.Localization.get("btn_pending", lang), currentTicket.status == "PENDING", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "PENDING")
                            activeDetailsTicket = currentTicket.copy(status = "PENDING", partNeededDate = null)
                        }
                        StatusChangeBtn(com.example.utils.Localization.get("btn_in_progress", lang), currentTicket.status == "IN_PROGRESS", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "IN_PROGRESS")
                            activeDetailsTicket = currentTicket.copy(status = "IN_PROGRESS", partNeededDate = null)
                        }
                        StatusChangeBtn(com.example.utils.Localization.get("btn_part_needed", lang), currentTicket.status == "PART_NEEDED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "PART_NEEDED")
                            activeDetailsTicket = currentTicket.copy(status = "PART_NEEDED", partNeededDate = currentTicket.partNeededDate ?: System.currentTimeMillis())
                        }
                        StatusChangeBtn(com.example.utils.Localization.get("btn_completed", lang), currentTicket.status == "COMPLETED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "COMPLETED")
                            activeDetailsTicket = currentTicket.copy(status = "COMPLETED", partNeededDate = null)
                        }
                        StatusChangeBtn(com.example.utils.Localization.get("btn_delivered", lang), currentTicket.status == "DELIVERED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "DELIVERED")
                            activeDetailsTicket = currentTicket.copy(status = "DELIVERED", partNeededDate = null)
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF Invoice share
                        Button(
                            onClick = {
                                val file = PdfUtils.createInvoicePdf(context, currentTicket, storeName, storePhone, currency, lang)
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, com.example.utils.Localization.get("share_pdf_chooser_title", lang)))
                                } else {
                                    Toast.makeText(context, com.example.utils.Localization.get("pdf_generation_failed", lang), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(com.example.utils.Localization.get("share_pdf_btn", lang), fontSize = 11.sp)
                        }

                        // WhatsApp / SMS text details share
                        Button(
                            onClick = {
                                shareTicketViaText(context, currentTicket, storeName, storePhone, currency, lang)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // WhatsApp Green
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(com.example.utils.Localization.get("whatsapp_text_btn", lang), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete button
                    Button(
                        onClick = {
                            ticketToDelete = currentTicket
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(com.example.utils.Localization.get("delete_ticket_btn", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (isEditingTicket && ticketToEdit != null) {
        val editing = ticketToEdit!!
        var editName by remember(editing) { mutableStateOf(editing.customerName) }
        var editPhone by remember(editing) { mutableStateOf(editing.customerPhone) }
        var editDevice by remember(editing) { mutableStateOf(editing.deviceModel) }
        var editFault by remember(editing) { mutableStateOf(editing.faultDescription) }
        var editPrice by remember(editing) { mutableStateOf(editing.totalPrice.toString()) }
        var editAdvance by remember(editing) { mutableStateOf(editing.advancePayment.toString()) }
        var editNotes by remember(editing) { mutableStateOf(editing.notes) }

        AlertDialog(
            onDismissRequest = { isEditingTicket = false },
            title = {
                Text(
                    text = String.format(com.example.utils.Localization.get("edit_ticket_title_tk", lang), editing.id),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(com.example.utils.Localization.get("client_name_placeholder", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(com.example.utils.Localization.get("client_phone_placeholder", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDevice,
                        onValueChange = { editDevice = it },
                        label = { Text(com.example.utils.Localization.get("device_model_placeholder", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFault,
                        onValueChange = { editFault = it },
                        label = { Text(com.example.utils.Localization.get("fault_description_placeholder", lang)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text(com.example.utils.Localization.get("total_estimated_cost", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAdvance,
                        onValueChange = { editAdvance = it },
                        label = { Text(com.example.utils.Localization.get("advance_payment", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text(com.example.utils.Localization.get("write_tech_notes", lang)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isEditingTicket = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(com.example.utils.Localization.get("cancel_btn", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            val priceVal = editPrice.toDoubleOrNull() ?: 0.0
                            val advanceVal = editAdvance.toDoubleOrNull() ?: 0.0
                            val remainingVal = maxOf(0.0, priceVal - advanceVal)

                            val updated = editing.copy(
                                customerName = editName,
                                customerPhone = editPhone,
                                deviceModel = editDevice,
                                faultDescription = editFault,
                                totalPrice = priceVal,
                                advancePayment = advanceVal,
                                remainingAmount = remainingVal,
                                notes = editNotes
                            )

                            viewModel.updateTicket(updated)
                            activeDetailsTicket = updated
                            isEditingTicket = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(com.example.utils.Localization.get("save_changes_btn", lang))
                    }
                }
            }
        )
    }

    if (ticketToDelete != null) {
        val targetTicket = ticketToDelete!!
        AlertDialog(
            onDismissRequest = { ticketToDelete = null },
            title = {
                Text(
                    text = com.example.utils.Localization.get("delete_confirm_title", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = String.format(com.example.utils.Localization.get("delete_confirm_msg", lang), targetTicket.customerName),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { ticketToDelete = null }
                ) {
                    Text(com.example.utils.Localization.get("cancel_btn", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTicket(targetTicket)
                        Toast.makeText(context, String.format(com.example.utils.Localization.get("ticket_deleted_success_toast", lang), targetTicket.customerName), Toast.LENGTH_SHORT).show()
                        ticketToDelete = null
                        activeDetailsTicket = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(com.example.utils.Localization.get("yes_confirm_delete", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        )
    }

    if (fullScreenImageFile != null) {
        Dialog(onDismissRequest = { fullScreenImageFile = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { fullScreenImageFile = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Text(
                            text = com.example.utils.Localization.get("view_full_image", lang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right
                        )
                    }
                    AsyncImage(
                        model = fullScreenImageFile!!,
                        contentDescription = "Full Screen Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun LocalRenderedImage(file: File) {
    AsyncImage(
        model = file,
        contentDescription = "Image preview",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun TicketCardItem(
    ticket: Ticket,
    currency: String,
    onClick: () -> Unit,
    lang: String = "ar"
) {
    val deviceEmoji = remember(ticket.deviceModel, ticket.faultDescription) {
        val combined = (ticket.deviceModel + " " + ticket.faultDescription).lowercase()
        if (combined.contains("شحن") || combined.contains("شاحن") || combined.contains("سلك") || combined.contains("charger") || combined.contains("cable") || combined.contains("plug") || combined.contains("power")) {
            "🔌"
        } else if (combined.contains("حاسوب") || combined.contains("كمبيوتر") || combined.contains("لابتوب") || combined.contains("laptop") || combined.contains("pc") || combined.contains("computer")) {
            "💻"
        } else {
            "📱"
        }
    }

    val statusColors = when (ticket.status) {
        "PENDING" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706)) // Amber
        "IN_PROGRESS" -> Pair(Color(0xFFDBEAFE), Color(0xFF2563EB)) // Blue
        "PART_NEEDED" -> Pair(Color(0xFFFEE2E2), Color(0xFFEF4444)) // Red for parts needed
        "COMPLETED" -> Pair(Color(0xFFD1FAE5), Color(0xFF059669)) // Green
        "DELIVERED" -> Pair(Color(0xFFF3F4F6), Color(0xFF4B5563)) // Gray
        else -> Pair(Color.LightGray, Color.DarkGray)
    }

    val statusText = when (ticket.status) {
        "PENDING" -> com.example.utils.Localization.get("status_pending", lang)
        "IN_PROGRESS" -> com.example.utils.Localization.get("status_inprogress", lang)
        "PART_NEEDED" -> com.example.utils.Localization.get("status_parts", lang)
        "COMPLETED" -> com.example.utils.Localization.get("status_completed", lang)
        "DELIVERED" -> com.example.utils.Localization.get("status_delivered", lang)
        else -> ticket.status
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End
        ) {
            if (lang == "fr") {
                // Left content of row: device status emoji avatar (start)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (deviceEmoji == "🔌") Color(0xFFF1F5F9) else Color(0xFFEFF6FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = deviceEmoji,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right content of row: Ticket ID and Financials, status, etc. (end)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Client Name on the left
                        Text(
                            text = ticket.customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Left
                        )

                        // Status Badge on the right
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusColors.first)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColors.second,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle: device model - fault description
                    Text(
                        text = "${ticket.deviceModel} - ${ticket.faultDescription}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B), // Slate info text
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Left
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Code / Ticket ID on Left
                        Text(
                            text = "#TK-${1000 + ticket.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8), // slate 400
                            textAlign = TextAlign.Left
                        )

                        // Pricing status info on Right
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ticket.totalPrice} $currency",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ticket.remainingAmount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(com.example.utils.Localization.get("unpaid_label", lang), ticket.remainingAmount.toString(), currency),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = com.example.utils.Localization.get("paid_label", lang),
                                    fontSize = 10.sp,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            } else {
                // Arabic (RTL) Layout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status Badge on the left
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(statusColors.first)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = statusColors.second,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Client Name on the right
                        Text(
                            text = ticket.customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subtitle: device model - fault description
                    Text(
                        text = "${ticket.deviceModel} - ${ticket.faultDescription}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B), // Slate info text
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pricing status info on Left
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ticket.totalPrice} $currency",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ticket.remainingAmount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(com.example.utils.Localization.get("unpaid_label", lang), ticket.remainingAmount.toString(), currency),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = com.example.utils.Localization.get("paid_label", lang),
                                    fontSize = 10.sp,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }

                        // Code / Ticket ID on Right
                        Text(
                            text = "#TK-${1000 + ticket.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8), // slate 400
                            textAlign = TextAlign.Right
                        )
                    }
                }

                // Right content of row: device status emoji avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (deviceEmoji == "🔌") Color(0xFFF1F5F9) else Color(0xFFEFF6FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = deviceEmoji,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DialogDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface, lang: String = "ar") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (lang == "fr") {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Left
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Left,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Right
            )
        }
    }
}

@Composable
fun StatusChangeBtn(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        modifier = modifier.height(36.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun shareTicketViaText(context: Context, ticket: Ticket, storeName: String, storePhone: String, currency: String, lang: String) {
    val statusStr = when(ticket.status) {
        "PENDING" -> com.example.utils.Localization.get("status_pending_full_desc", lang)
        "IN_PROGRESS" -> com.example.utils.Localization.get("status_inprogress_desc", lang)
        "PART_NEEDED" -> com.example.utils.Localization.get("status_parts_full_desc", lang)
        "COMPLETED" -> com.example.utils.Localization.get("status_completed_full_desc", lang)
        "DELIVERED" -> com.example.utils.Localization.get("status_delivered_full_desc", lang)
        else -> ticket.status
    }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(ticket.createdAt)
    
    val template = com.example.utils.Localization.get("whatsapp_share_template", lang)
    
    val textMessage = String.format(
        template,
        ticket.id.toString(),
        storeName,
        ticket.customerName,
        ticket.customerPhone,
        dateStr,
        ticket.deviceModel,
        ticket.faultDescription,
        statusStr,
        "${ticket.totalPrice} $currency",
        "${ticket.advancePayment} $currency",
        "${ticket.remainingAmount} $currency",
        storePhone
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, textMessage)
    }
    context.startActivity(Intent.createChooser(intent, com.example.utils.Localization.get("share_dialog_title", lang)))
}
