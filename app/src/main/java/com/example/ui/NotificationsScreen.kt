package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.database.Ticket
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBackClicked: () -> Unit,
    onTicketClick: (Ticket) -> Unit
) {
    val tickets by viewModel.allTicketsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val lang = settings["language"] ?: "ar"

    // Filter tickets with status "PART_NEEDED" (الأجهزة التي تنتظر قطع)
    val partNeededTickets = remember(tickets) {
        tickets.filter { it.status == "PART_NEEDED" }
            .map { ticket ->
                val days = if (ticket.partNeededDate != null) {
                    val diff = System.currentTimeMillis() - ticket.partNeededDate
                    maxOf(0L, TimeUnit.MILLISECONDS.toDays(diff))
                } else {
                    0L
                }
                Pair(ticket, days)
            }
            // Sort automatically: most delayed (most elapsed days) first. 
            // Older timestamp = higher days = should appear first.
            .sortedWith(compareByDescending<Pair<Ticket, Long>> { it.second }
                .thenBy { it.first.partNeededDate ?: 0L })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Simple Top bar
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = com.example.utils.Localization.get("notifications_subtitle", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = com.example.utils.Localization.get("back_btn", lang)
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (partNeededTickets.isEmpty()) {
            // Beautiful Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = com.example.utils.Localization.get("no_notifications", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = com.example.utils.Localization.get("notifications_desc", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = String.format(com.example.utils.Localization.get("delayed_parts_needed_count", lang), partNeededTickets.size),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (lang == "fr") TextAlign.Left else TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }

                items(partNeededTickets, key = { it.first.id }) { (ticket, days) ->
                    // Determine status indicator color
                    val indicator = when {
                        days <= 2 -> Pair("🔴", Color(0xFFEF4444))
                        days in 3..4 -> Pair("🟠", Color(0xFFF97316)) // Orange
                        else -> Pair("🟡", Color(0xFFEAB308)) // Yellow
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTicketClick(ticket) }
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (lang == "fr") {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = com.example.utils.Localization.get("view_details", lang),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${indicator.first} ${com.example.utils.Localization.get("device_label", lang)} ${ticket.deviceModel}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Left
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${com.example.utils.Localization.get("client", lang)} ${ticket.customerName}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Left
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${com.example.utils.Localization.get("fault", lang)} ${ticket.faultDescription}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Left
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = String.format(com.example.utils.Localization.get("waiting_duration", lang), days),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = indicator.second,
                                        textAlign = TextAlign.Left
                                    )
                                }
                            } else {
                                // Arabic RTL
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${indicator.first} ${com.example.utils.Localization.get("device_label", lang)} ${ticket.deviceModel}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Right
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "${com.example.utils.Localization.get("client", lang)} ${ticket.customerName}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${com.example.utils.Localization.get("fault", lang)} ${ticket.faultDescription}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Right
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = String.format(com.example.utils.Localization.get("waiting_duration", lang), days),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = indicator.second,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = com.example.utils.Localization.get("view_details", lang),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
