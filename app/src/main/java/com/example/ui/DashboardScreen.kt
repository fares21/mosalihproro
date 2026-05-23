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

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    tickets: List<Ticket>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    val currency = settings["store_currency"] ?: "د.إ"
    
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
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "لوحة التحكم والإحصائيات",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Row of Quick Case stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCompactCard(
                title = "تذاكر نشطة",
                value = activeCount.toString(),
                icon = Icons.Default.Build,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatsCompactCard(
                title = "تذاكر منتهية",
                value = completedCount.toString(),
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Financial Statistics Summary header
        Text(
            text = "إحصائيات الأرباح",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // The Vibrant 3-Column Grid representing the specific layout requested
        VibrantEarningsGrid(
            today = todayEarnings,
            week = weekEarnings,
            month = monthEarnings,
            currency = currency
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
            text = "التذكرة التالية في قائمة الانتظار",
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
                horizontalAlignment = Alignment.End
            ) {
                if (nextTicket != null) {
                    val statusText = "قيد الانتظار"
                    val badgeColor = Color(0xFFFEF3C7)
                    val badgeTextColor = Color(0xFFD97706)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon buttons on the left
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Service Action Wrench / Play button (برمز تحويلها لقائمة الصيانة)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable {
                                        viewModel.updateTicketStatus(nextTicket.id, "IN_PROGRESS")
                                        android.widget.Toast.makeText(context, "تم تحويل التذكرة #${1000 + nextTicket.id} إلى قائمة الصيانة", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "بدء الصيانة فورياً",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Skip action button (برمز التخطي للتذكرة التي بعدها)
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
                                        contentDescription = "تخطي للبطاقة التالية",
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
                                        contentDescription = "العودة للتذكرة الأولى",
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
                                text = "رقم التذكرة: #TK-${1000 + nextTicket.id}",
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
                        text = "العميل: ${nextTicket.customerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الهاتف: ${nextTicket.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الجهاز: ${nextTicket.deviceModel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "العطل: ${nextTicket.faultDescription}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Right
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
                                text = "لا توجد تذاكر معلقة في قائمة الانتظار حالياً 👍",
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
            text = "الجهاز الذي قيد الصيانة حالياً",
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
                horizontalAlignment = Alignment.End
            ) {
                if (activeMaintenanceTicket != null) {
                    val statusText = "قيد الصيانة"
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
                                        android.widget.Toast.makeText(context, "تم تحويل الجهاز إلى صيانة منتهية وجاهز للتسليم! ✅", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "جاهز للتسليم",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Needs More Time Register Alarm Button (برمز يحتاج وقت)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)) // Light Amber
                                    .clickable {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            val permission = "android.permission.POST_NOTIFICATIONS"
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                android.widget.Toast.makeText(context, "⚠️ تنبيه: يرجى تفعيل إذن الإشارات لتلقي هذا التنبيه على شاشتك!", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        val delayMin = settings["maintenance_reminder_delay"]?.toIntOrNull() ?: 10
                                        val ringtoneUri = settings["maintenance_ringtone_uri"]
                                        viewModel.scheduleDeviceReminder(activeMaintenanceTicket.id, activeMaintenanceTicket.deviceModel, delayMin, ringtoneUri)
                                        android.widget.Toast.makeText(context, "⏰ تم ضبط منبه لـ ${activeMaintenanceTicket.deviceModel} بعد $delayMin دقيقة بنجاح!", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "يحتاج وقت إضافي (تفعيل المنبه)",
                                    tint = Color(0xFFD97706),
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
                                text = "رقم التذكرة: #TK-${1000 + activeMaintenanceTicket.id}",
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
                        text = "العميل: ${activeMaintenanceTicket.customerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الهاتف: ${activeMaintenanceTicket.customerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الجهاز: ${activeMaintenanceTicket.deviceModel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "العطل: ${activeMaintenanceTicket.faultDescription}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Right
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
                                text = "لا توجد أجهزة قيد الصيانة النشطة حالياً 👍",
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
            text = "أجهزة منسية طال انتظارها (جاهزة ولم تُستلم)",
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
                horizontalAlignment = Alignment.End
            ) {
                if (forgottenTickets.isNotEmpty()) {
                    forgottenTickets.forEachIndexed { index, ticket ->
                        val daysElapsed = remember(ticket.createdAt) {
                            val diffMs = System.currentTimeMillis() - ticket.createdAt
                            (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        }
                        val durationText = when {
                            daysElapsed == 0 -> "اليوم"
                            daysElapsed == 1 -> "منذ يوم واحد"
                            daysElapsed == 2 -> "منذ يومين"
                            daysElapsed in 3..10 -> "منذ $daysElapsed أيام"
                            else -> "منذ $daysElapsed يوماً"
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
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = ticket.customerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${ticket.deviceModel} • هاتف: ${ticket.customerPhone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "المبلغ المتبقي: ${ticket.remainingAmount} $currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ticket.remainingAmount > 0) Color(0xFFEF4444) else Color(0xFF10B981)
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
                                text = "لا توجد أجهزة منسية جاهزة للاستلام حالياً 👍",
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
                    DashboardDialogDetailRow("العميل:", currentTicket.customerName)
                    
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

                    DashboardDialogDetailRow("نوع وهاتف الجهاز:", currentTicket.deviceModel)
                    DashboardDialogDetailRow("عطل الجهاز:", currentTicket.faultDescription)
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    DashboardDialogDetailRow("تاريخ الدخول:", sdf.format(currentTicket.createdAt))

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Finance block
                    DashboardDialogDetailRow("السعر الإجمالي للخدمة:", "${currentTicket.totalPrice} $currency")
                    DashboardDialogDetailRow("المبلغ المدفوع (مقدماً):", "${currentTicket.advancePayment} $currency")
                    
                    val remainingColor = if (currentTicket.remainingAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                    DashboardDialogDetailRow("المبلغ المتبقي:", "${currentTicket.remainingAmount} $currency", valueColor = remainingColor)

                    if (currentTicket.notes.isNotEmpty()) {
                        DashboardDialogDetailRow("ملاحظات وشروط إضافية:", currentTicket.notes)
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
                    Text("تغيير حالة الصيانة للتذكرة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DashboardStatusChangeBtn("معلق", currentTicket.status == "PENDING", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "PENDING")
                            activeDetailsTicket = currentTicket.copy(status = "PENDING")
                        }
                        DashboardStatusChangeBtn("بالصيانة", currentTicket.status == "IN_PROGRESS", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "IN_PROGRESS")
                            activeDetailsTicket = currentTicket.copy(status = "IN_PROGRESS")
                        }
                        DashboardStatusChangeBtn("جاهز", currentTicket.status == "COMPLETED", Modifier.weight(1f)) {
                            viewModel.updateTicketStatus(currentTicket.id, "COMPLETED")
                            activeDetailsTicket = currentTicket.copy(status = "COMPLETED")
                        }
                        DashboardStatusChangeBtn("سلمت", currentTicket.status == "DELIVERED", Modifier.weight(1f)) {
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
                                dashboardShareTicketViaText(context, currentTicket, storeName, currency)
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

    // 3. Delete Dialog
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
    currency: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموع المبيعات والمقدمات",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل الأرباح والمداخيل",
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
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today
                GridStatItem(
                    period = "اليوم",
                    value = "${String.format("%.1f", today)} $currency",
                    valueColor = MaterialTheme.colorScheme.primary,
                    borderColor = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                )

                // Week
                GridStatItem(
                    period = "الأسبوع",
                    value = "${String.format("%.1f", week)} $currency",
                    valueColor = MaterialTheme.colorScheme.primary,
                    borderColor = Color(0xFFF1F5F9),
                    modifier = Modifier.weight(1f)
                )

                // Month (With vibrant bottom border highlighting like Design HTML)
                GridStatItem(
                    period = "الشهر",
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
    modifier: Modifier = Modifier
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
            horizontalAlignment = Alignment.End
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
fun DashboardDialogDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
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

private fun dashboardShareTicketViaText(context: Context, ticket: Ticket, storeName: String, currency: String) {
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
