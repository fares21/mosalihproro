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
    onAddTicketClicked: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

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
            horizontalAlignment = Alignment.End
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("بحث بالاسم أو الهاتف...", textAlign = TextAlign.Right, fontSize = 13.sp) },
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
                    "COMPLETED" -> 3
                    "DELIVERED" -> 4
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
                                "COMPLETED" -> 3
                                "DELIVERED" -> 4
                                else -> 0
                            }
                        ]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(selected = selectedFilterTab == "ALL", onClick = { selectedFilterTab = "ALL" }) {
                    Text("الكل", modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "ALL") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "PENDING", onClick = { selectedFilterTab = "PENDING" }) {
                    Text("قيد الانتظار", modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "PENDING") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "IN_PROGRESS", onClick = { selectedFilterTab = "IN_PROGRESS" }) {
                    Text("قيد الصيانة", modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "IN_PROGRESS") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "COMPLETED", onClick = { selectedFilterTab = "COMPLETED" }) {
                    Text("مكتمل", modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "COMPLETED") MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Tab(selected = selectedFilterTab == "DELIVERED", onClick = { selectedFilterTab = "DELIVERED" }) {
                    Text("تم التسليم", modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedFilterTab == "DELIVERED") MaterialTheme.colorScheme.primary else Color.Gray)
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
                        text = "لا توجد تذاكر صيانة مطابقة حالياً",
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
                        onClick = { activeDetailsTicket = ticket }
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
                                contentDescription = "تعديل التذكرة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = "تفاصيل تذكرة الصيانة #${currentTicket.id}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Right
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
                    horizontalAlignment = Alignment.End
                ) {
                    // Client detail row
                    DialogDetailRow("العميل:", currentTicket.customerName)
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
                                    Toast.makeText(context, "فشل بدء المكالمة", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال بالعميل",
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
                            text = "الهاتف:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right
                        )
                    }
                    DialogDetailRow("نوع وهاتف الجهاز:", currentTicket.deviceModel)
                    DialogDetailRow("عطل الجهاز:", currentTicket.faultDescription)
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    DialogDetailRow("تاريخ الدخول:", sdf.format(currentTicket.createdAt))

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Finance block
                    DialogDetailRow("السعر الإجمالي للخدمة:", "${currentTicket.totalPrice} $currency")
                    DialogDetailRow("المبلغ المدفوع (مقدماً):", "${currentTicket.advancePayment} $currency")
                    
                    val remainingColor = if (currentTicket.remainingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    DialogDetailRow("المبلغ المتبقي:", "${currentTicket.remainingAmount} $currency", valueColor = remainingColor)

                    if (currentTicket.notes.isNotEmpty()) {
                        DialogDetailRow("ملاحظات وشروط إضافية:", currentTicket.notes)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Photos preview
                    if (currentTicket.frontImagePath != null || currentTicket.backImagePath != null) {
                        Text("الصور الموثقة للجهاز (انقر لعرضها بالكامل):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    Text("تغيير حالة الصيانة للتذكرة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusChangeBtn("معلق", currentTicket.status == "PENDING", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "PENDING")
                            activeDetailsTicket = currentTicket.copy(status = "PENDING")
                        }
                        StatusChangeBtn("بالصيانة", currentTicket.status == "IN_PROGRESS", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "IN_PROGRESS")
                            activeDetailsTicket = currentTicket.copy(status = "IN_PROGRESS")
                        }
                        StatusChangeBtn("جاهز", currentTicket.status == "COMPLETED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "COMPLETED")
                            activeDetailsTicket = currentTicket.copy(status = "COMPLETED")
                        }
                        StatusChangeBtn("سلمت", currentTicket.status == "DELIVERED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "DELIVERED")
                            activeDetailsTicket = currentTicket.copy(status = "DELIVERED")
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
                                val file = PdfUtils.createInvoicePdf(context, currentTicket, storeName, storePhone, currency)
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة فاتورة صيانة الهاتف"))
                                } else {
                                    Toast.makeText(context, "فشل إنشاء فاتورة PDF الموحدة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة PDF", fontSize = 11.sp)
                        }

                        // WhatsApp / SMS text details share
                        Button(
                            onClick = {
                                shareTicketViaText(context, currentTicket, storeName, currency)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // WhatsApp Green
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب / نص", fontSize = 11.sp)
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
                        Text("حذف التذكرة المحددة بصفة نهائية", fontWeight = FontWeight.Bold)
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
                    text = "تعديل بيانات التذكرة #${editing.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("اسم العميل / الزبون") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDevice,
                        onValueChange = { editDevice = it },
                        label = { Text("نوع وموديل الجهاز") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFault,
                        onValueChange = { editFault = it },
                        label = { Text("تفاصيل عطل الجهاز") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("السعر الإجمالي للخدمة") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAdvance,
                        onValueChange = { editAdvance = it },
                        label = { Text("المبلغ المدفوع مقدماً") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("ملاحظات وشروط إضافية") },
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
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("حفظ التعديلات")
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
                    text = "تأكيد الحذف الآمن",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد تماماً من رغبتك في حذف تذكرة صيانة العميل/الزبون (${targetTicket.customerName}) نهائياً من النظام؟ لا يمكن الرجوع عن هذا الخيار بعد تأكيده.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { ticketToDelete = null }
                ) {
                    Text("إلغاء", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTicket(targetTicket)
                        Toast.makeText(context, "تم حذف تذكرة العميل ${targetTicket.customerName} بنجاح!", Toast.LENGTH_SHORT).show()
                        ticketToDelete = null
                        activeDetailsTicket = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، تأكيد الحذف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            text = "عرض الصورة الكاملة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Right
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
    onClick: () -> Unit
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
        "COMPLETED" -> Pair(Color(0xFFD1FAE5), Color(0xFF059669)) // Green
        "DELIVERED" -> Pair(Color(0xFFF3F4F6), Color(0xFF4B5563)) // Gray
        else -> Pair(Color.LightGray, Color.DarkGray)
    }

    val statusText = when (ticket.status) {
        "PENDING" -> "قيد الانتظار"
        "IN_PROGRESS" -> "قيد الصيانة"
        "COMPLETED" -> "تمت الصيانة"
        "DELIVERED" -> "تم التسليم"
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
            horizontalArrangement = Arrangement.End
        ) {
            // Left content of row: Ticket ID and Financials, status, etc. (weighted)
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
                                text = "(متبقي ${ticket.remainingAmount} $currency)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(مدفوع)",
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

@Composable
fun DialogDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
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

private fun shareTicketViaText(context: Context, ticket: Ticket, storeName: String, currency: String) {
    val statusStr = when(ticket.status) {
        "PENDING" -> "قيد الانتظار"
        "IN_PROGRESS" -> "قيد الصيانة"
        "COMPLETED" -> "تم الانتهاء وجاهز للتسليم"
        "DELIVERED" -> "تم التسليم ومكتمل الحساب"
        else -> ticket.status
    }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(ticket.createdAt)
    
    val textMessage = """
        *تذكرة صيانة هاتف رقم (#${ticket.id})*
        مرحباً بك في $storeName! تفاصيل خدمة صيانة هاتفك أدناه:
        
        • تاريخ تسجيل الطلبية: $dateStr
        • الجهاز: ${ticket.deviceModel}
        • العطل المحدد: ${ticket.faultDescription}
        • حالة الجهاز: $statusStr
        
        ------------------------------
        • السعر الكلي: ${ticket.totalPrice} $currency
        • الدفعة المقدمة: ${ticket.advancePayment} $currency
        • المتبقي لسداده: ${ticket.remainingAmount} $currency
        ------------------------------
        
        شكرًا لثقتك بنا!
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, textMessage)
    }
    context.startActivity(Intent.createChooser(intent, "إرسال ومشاركة تفاصيل الصيانة"))
}
