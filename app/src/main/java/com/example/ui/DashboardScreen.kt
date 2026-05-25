package com.example.ui

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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.database.Ticket
import java.util.Calendar
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.utils.PdfUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

import com.example.utils.Localization

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    tickets: List<Ticket>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    val currency = settings["store_currency"] ?: "د.إ"
    val lang = settings["language"] ?: "ar"
    
    // Earnings calculation
    val (todayEarnings, weekEarnings, monthEarnings) = remember(tickets) {
        calculateEarnings(tickets)
    }

    val activeCount = remember(tickets) {
        tickets.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }
    }
    val completedCount = remember(tickets) {
        tickets.count { it.status == "COMPLETED" || it.status == "DELIVERED" }
    }

    val storeName = settings["store_name"] ?: "متجر صيانة الهواتف"
    val storePhone = settings["store_phone"] ?: "0500000000"

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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
    ) {
        Text(
            text = Localization.get("dashboard_title", lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp),
            textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right
        )

        // Row of Quick Case stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCompactCard(
                title = Localization.get("active_tickets_stat", lang),
                value = activeCount.toString(),
                icon = Icons.Default.Build,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                lang = lang
            )
            StatsCompactCard(
                title = Localization.get("completed_tickets_stat", lang),
                value = completedCount.toString(),
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                lang = lang
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Financial Statistics Summary header
        Text(
            text = Localization.get("revenue_stats", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp),
            textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right
        )

        // The Vibrant 3-Column Grid representing the specific layout requested
        VibrantEarningsGrid(
            today = todayEarnings,
            week = weekEarnings,
            month = monthEarnings,
            currency = currency,
            lang = lang
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Next Ticket in Queue Section (Strictly PENDING status)
        val pendingTickets = remember(tickets) {
            tickets.filter { it.status == "PENDING" }.sortedBy { it.createdAt }
        }
        var pendingSkipCount by remember { mutableStateOf(0) }
        LaunchedEffect(pendingTickets) {
            if (pendingSkipCount >= pendingTickets.size) {
                pendingSkipCount = 0
            }
        }
        val nextTicket = pendingTickets.getOrNull(pendingSkipCount)

        Text(
            text = Localization.get("next_ticket_queue", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = nextTicket != null) { activeDetailsTicket = nextTicket }
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
            ) {
                if (nextTicket != null) {
                    val statusText = Localization.get("pending_status", lang)
                    val badgeColor = Color(0xFFFEF3C7)
                    val badgeTextColor = Color(0xFFD97706)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon buttons on the left or direction aware
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Service Action Wrench / Play button (بررمز تحويلها لقائمة الصيانة)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        viewModel.updateTicketStatus(nextTicket.id, "IN_PROGRESS")
                                        android.widget.Toast.makeText(context, String.format(Localization.get("ticket_shifted_to_progress_toast", lang), 1000 + nextTicket.id), android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = Localization.get("start_maintenance_action", lang),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Skip action button (بررمز التخطي للتذكرة التي بعدها)
                            if (pendingTickets.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable {
                                            pendingSkipCount = (pendingSkipCount + 1) % pendingTickets.size
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = Localization.get("skip_to_next_action", lang),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Reset skip count to go back to 1st ticket
                            if (pendingSkipCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            pendingSkipCount = 0
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FirstPage,
                                        contentDescription = Localization.get("reset_to_first_action", lang),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Badge and Ticket ID on the right
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(badgeColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${pendingSkipCount + 1} / ${pendingTickets.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                            
                            Text(
                                text = String.format(Localization.get("ticket_number_label", lang), 1000 + nextTicket.id),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${Localization.get("client_label_simple", lang)}: ${nextTicket.customerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Localization.get("phone_label_simple", lang)}: ${nextTicket.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Localization.get("device_label_simple", lang)}: ${nextTicket.deviceModel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Localization.get("fault_label_simple", lang)}: ${nextTicket.faultDescription}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.get("no_pending_tickets_empty", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card displaying the active device under maintenance (status == IN_PROGRESS)
        val inProgressTickets = remember(tickets) {
            tickets.filter { it.status == "IN_PROGRESS" }.sortedBy { it.createdAt }
        }
        val activeMaintenanceTicket = inProgressTickets.firstOrNull()

        Text(
            text = Localization.get("active_maintenance_device", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = activeMaintenanceTicket != null) { activeDetailsTicket = activeMaintenanceTicket }
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
            ) {
                if (activeMaintenanceTicket != null) {
                    val statusText = Localization.get("in_progress_status", lang)
                    val badgeColor = Color(0xFFDBEAFE) // Clean Light Blue
                    val badgeTextColor = Color(0xFF2563EB) // Clean Royal Blue

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Two icon-only buttons on the left so that the page is beautiful and un-cluttered
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ready for Delivery Check Button (برمز جاهز للتسليم)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)) // Light Green
                                    .clickable {
                                        viewModel.updateTicketStatus(activeMaintenanceTicket.id, "COMPLETED")
                                        
                                        // Auto queue shift: promote the next PENDING card to IN_PROGRESS
                                        val nextInQueue = nextTicket
                                        if (nextInQueue != null) {
                                            viewModel.updateTicketStatus(nextInQueue.id, "IN_PROGRESS")
                                        }
                                        pendingSkipCount = 0
                                        
                                        android.widget.Toast.makeText(context, Localization.get("ticket_shifted_to_unclaimed_toast", lang), android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = Localization.get("ready_for_delivery", lang),
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Ticket metadata on the right
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(badgeColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor
                                )
                            }
                            
                            Text(
                                text = String.format(Localization.get("ticket_number_label", lang), 1000 + activeMaintenanceTicket.id),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${Localization.get("client_label_simple", lang)}: ${activeMaintenanceTicket.customerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Localization.get("phone_label_simple", lang)}: ${activeMaintenanceTicket.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Localization.get("device_label_simple", lang)}: ${activeMaintenanceTicket.deviceModel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Localization.get("fault_label_simple", lang)}: ${activeMaintenanceTicket.faultDescription}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.get("no_active_maintenance_empty", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Forgotten Tickets Left at Shop Section
        val forgottenTickets = remember(tickets) {
            tickets.filter { it.status == "COMPLETED" }
                .sortedBy { it.createdAt }
                .take(5)
        }

        Text(
            text = Localization.get("forgotten_devices_shop", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
            ) {
                if (forgottenTickets.isNotEmpty()) {
                    forgottenTickets.forEachIndexed { index, ticket ->
                        val daysElapsed = remember(ticket.createdAt) {
                            val diffMs = System.currentTimeMillis() - ticket.createdAt
                            (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        }
                        val durationText = if (lang == "fr") {
                            when {
                                daysElapsed == 0 -> "Aujourd'hui"
                                daysElapsed == 1 -> "Depuis 1 jour"
                                else -> "Depuis $daysElapsed jours"
                            }
                        } else {
                            when {
                                daysElapsed == 0 -> "اليوم"
                                daysElapsed == 1 -> "منذ يوم واحد"
                                daysElapsed == 2 -> "منذ يومين"
                                daysElapsed in 3..10 -> "منذ $daysElapsed أيام"
                                else -> "منذ $daysElapsed يوماً"
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeDetailsTicket = ticket }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date / Duration Badge on the Left
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)) // Light Red
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = durationText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444) // Deep Red
                                )
                            }

                            // Ticket & Customer Info on the Right
                            Column(
                                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = ticket.customerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${ticket.deviceModel} • ${Localization.get("phone_label_simple", lang)}: ${ticket.customerPhone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Localization.get("remaining_payment_short", lang)}: ${ticket.remainingAmount} $currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ticket.remainingAmount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (index < forgottenTickets.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.get("no_unclaimed_devices_empty", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------ DIALOGS ------------------

    // 1. Details Dialog
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
                            Icon(Icons.Default.Close, contentDescription = Localization.get("close_btn", lang))
                        }
                        IconButton(
                            onClick = {
                                ticketToEdit = currentTicket
                                isEditingTicket = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = Localization.get("edit_ticket_btn", lang),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = String.format(Localization.get("ticket_details_title", lang), currentTicket.id),
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
                    DashboardDialogDetailRow(Localization.get("client_label_col", lang), currentTicket.customerName, lang = lang)
                    
                    // Clickable Call Row
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
                                    Toast.makeText(context, Localization.get("call_failed_toast", lang), Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lang == "fr") {
                            Text(
                                text = Localization.get("phone_label_col", lang),
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
                                contentDescription = Localization.get("call_client_action", lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
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
                                text = Localization.get("phone_label_col", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    DashboardDialogDetailRow(Localization.get("device_label_col", lang), currentTicket.deviceModel, lang = lang)
                    DashboardDialogDetailRow(Localization.get("fault_label_col", lang), currentTicket.faultDescription, lang = lang)
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    DashboardDialogDetailRow(Localization.get("entry_date_col", lang), sdf.format(currentTicket.createdAt), lang = lang)

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Finance block
                    DashboardDialogDetailRow(Localization.get("total_price_col", lang), "${currentTicket.totalPrice} $currency", lang = lang)
                    DashboardDialogDetailRow(Localization.get("advance_payment_col", lang), "${currentTicket.advancePayment} $currency", lang = lang)
                    
                    val remainingColor = if (currentTicket.remainingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    DashboardDialogDetailRow(Localization.get("remaining_payment_col", lang), "${currentTicket.remainingAmount} $currency", valueColor = remainingColor, lang = lang)

                    if (currentTicket.notes.isNotEmpty()) {
                        DashboardDialogDetailRow(Localization.get("additional_notes_col", lang), currentTicket.notes, lang = lang)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Photos preview
                    if (currentTicket.frontImagePath != null || currentTicket.backImagePath != null) {
                        Text(Localization.get("documented_photos_label", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentTicket.frontImagePath?.let {
                                val file = File(it)
                                if (file.exists()) {
                                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { fullScreenImageFile = file }) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = "Image preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            currentTicket.backImagePath?.let {
                                val file = File(it)
                                if (file.exists()) {
                                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { fullScreenImageFile = file }) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = "Image preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Modify operations/status section
                    Text(Localization.get("change_ticket_status_label", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DashboardStatusChangeBtn(Localization.get("btn_pending", lang), currentTicket.status == "PENDING", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "PENDING")
                            activeDetailsTicket = currentTicket.copy(status = "PENDING", partNeededDate = null)
                        }
                        DashboardStatusChangeBtn(Localization.get("btn_in_progress", lang), currentTicket.status == "IN_PROGRESS", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "IN_PROGRESS")
                            activeDetailsTicket = currentTicket.copy(status = "IN_PROGRESS", partNeededDate = null)
                        }
                        DashboardStatusChangeBtn(Localization.get("btn_part_needed", lang), currentTicket.status == "PART_NEEDED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "PART_NEEDED")
                            activeDetailsTicket = currentTicket.copy(status = "PART_NEEDED", partNeededDate = currentTicket.partNeededDate ?: System.currentTimeMillis())
                        }
                        DashboardStatusChangeBtn(Localization.get("btn_completed", lang), currentTicket.status == "COMPLETED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "COMPLETED")
                            activeDetailsTicket = currentTicket.copy(status = "COMPLETED", partNeededDate = null)
                        }
                        DashboardStatusChangeBtn(Localization.get("btn_delivered", lang), currentTicket.status == "DELIVERED", Modifier.weight(1f)) {
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
                                    context.startActivity(Intent.createChooser(shareIntent, Localization.get("share_pdf_chooser_title", lang)))
                                } else {
                                    Toast.makeText(context, Localization.get("pdf_generation_failed", lang), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("share_pdf_btn", lang), fontSize = 11.sp)
                        }

                        // WhatsApp / SMS text details share
                        Button(
                            onClick = {
                                dashboardShareTicketViaText(context, currentTicket, storeName, storePhone, currency, lang)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // WhatsApp Green
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Localization.get("whatsapp_text_btn", lang), fontSize = 11.sp)
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
                        Text(Localization.get("delete_ticket_btn", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // 2. Edit Dialog
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
                    text = String.format(Localization.get("edit_ticket_title_tk", lang), editing.id),
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
                        label = { Text(Localization.get("client_name_placeholder", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(Localization.get("client_phone_placeholder", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDevice,
                        onValueChange = { editDevice = it },
                        label = { Text(Localization.get("device_model_placeholder", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFault,
                        onValueChange = { editFault = it },
                        label = { Text(Localization.get("fault_description_placeholder", lang)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text(Localization.get("total_estimated_cost", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAdvance,
                        onValueChange = { editAdvance = it },
                        label = { Text(Localization.get("advance_payment", lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text(Localization.get("write_tech_notes", lang)) },
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
                        Text(Localization.get("cancel", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(Localization.get("save_changes_btn", lang))
                    }
                }
            }
        )
    }

    // 3. Delete Dialog
    if (ticketToDelete != null) {
        val targetTicket = ticketToDelete!!
        AlertDialog(
            onDismissRequest = { ticketToDelete = null },
            title = {
                Text(
                    text = Localization.get("delete_confirm_title", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = String.format(Localization.get("delete_confirm_msg", lang), targetTicket.customerName),
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
                    Text(Localization.get("cancel", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTicket(targetTicket)
                        Toast.makeText(context, String.format(Localization.get("ticket_deleted_success_toast", lang), targetTicket.customerName), Toast.LENGTH_SHORT).show()
                        ticketToDelete = null
                        activeDetailsTicket = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(Localization.get("yes_confirm_delete", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        )
    }

    // 4. Full Screen Image Preview Dialog
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
fun VibrantEarningsGrid(
    today: Double,
    week: Double,
    month: Double,
    currency: String,
    lang: String = "ar"
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (lang == "fr") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.get("revenue_details_title", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = Localization.get("total_sales_subtitle", lang),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = Localization.get("total_sales_subtitle", lang),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.get("revenue_details_title", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today
                GridStatItem(
                    period = Localization.get("today", lang),
                    value = "${String.format("%.1f", today)} $currency",
                    valueColor = MaterialTheme.colorScheme.primary,
                    borderColor = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                )

                // Week
                GridStatItem(
                    period = Localization.get("this_week", lang),
                    value = "${String.format("%.1f", week)} $currency",
                    valueColor = MaterialTheme.colorScheme.primary,
                    borderColor = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                )

                // Month (With vibrant bottom border highlighting like Design HTML)
                GridStatItem(
                    period = Localization.get("this_month", lang),
                    value = "${String.format("%.1f", month)} $currency",
                    valueColor = Color(0xFF16A34A), // Rich green
                    borderColor = Color(0xFF86EFAC).copy(alpha = 0.6f),
                    borderBottomHighlight = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun GridStatItem(
    period: String,
    value: String,
    valueColor: Color,
    borderColor: Color,
    borderBottomHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = period,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B) // Slate text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                textAlign = TextAlign.Center
            )
            if (borderBottomHighlight) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF22C55E))
                )
            }
        }
    }
}

@Composable
fun StatsCompactCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    lang: String = "ar"
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EarningsCard(
    periodTitle: String,
    amount: Double,
    currency: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "${String.format("%.2f", amount)} $currency",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = periodTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مجموع المبيعات والمقدمات",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun calculateEarnings(tickets: List<Ticket>): Triple<Double, Double, Double> {
    val now = Calendar.getInstance()
    
    // Start of Today
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Start of Week (Saturday as starting of Arabic business week, or standard)
    val weekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Start of Month
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var todayEarnings = 0.0
    var weekEarnings = 0.0
    var monthEarnings = 0.0

    for (ticket in tickets) {
        // Calculation: earned so far = advance + (if completed/delivered: remaining)
        val earned = ticket.advancePayment + if (ticket.status == "COMPLETED" || ticket.status == "DELIVERED") ticket.remainingAmount else 0.0
        
        if (ticket.createdAt >= todayStart) {
            todayEarnings += earned
        }
        if (ticket.createdAt >= weekStart) {
            weekEarnings += earned
        }
        if (ticket.createdAt >= monthStart) {
            monthEarnings += earned
        }
    }

    return Triple(todayEarnings, weekEarnings, monthEarnings)
}

@Composable
fun DashboardDialogDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface, lang: String = "ar") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End
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
fun DashboardStatusChangeBtn(
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

private fun dashboardShareTicketViaText(context: Context, ticket: Ticket, storeName: String, storePhone: String, currency: String, lang: String) {
    val statusStr = when(ticket.status) {
        "PENDING" -> Localization.get("pending_status", lang)
        "IN_PROGRESS" -> Localization.get("in_progress_status", lang)
        "PART_NEEDED" -> Localization.get("part_needed_status", lang)
        "COMPLETED" -> Localization.get("completed_status", lang)
        "DELIVERED" -> Localization.get("delivered_status", lang)
        else -> ticket.status
    }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(ticket.createdAt)
    
    val template = Localization.get("whatsapp_share_template", lang)
    val textMessage = String.format(
        template,
        ticket.id,
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
    context.startActivity(Intent.createChooser(intent, Localization.get("share_btn_text", lang)))
}
