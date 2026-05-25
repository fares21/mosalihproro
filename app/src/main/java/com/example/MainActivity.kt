package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.database.Ticket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request post notifications permission for Android 13 or later (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                val isActivated by mainViewModel.isActivated.collectAsState()
                val settings by mainViewModel.settingsState.collectAsState()
                val lang = settings["language"] ?: "ar"
                val layoutDirection = if (lang == "fr") androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl

                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
                ) {
                    if (!isActivated) {
                        ActivationScreen(
                            viewModel = mainViewModel,
                            onActivationSuccess = {
                                // Handled reactive flow automatic
                            }
                        )
                    } else {
                        AppMainLayout(mainViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderBrandBar(
    viewModel: MainViewModel,
    onNotificationClick: () -> Unit
) {
    val tickets by viewModel.allTicketsState.collectAsState()
    val partNeededCount = remember(tickets) { tickets.count { it.status == "PART_NEEDED" } }
    val settings by viewModel.settingsState.collectAsState()
    val lang = settings["language"] ?: "ar"

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: User profile placeholder as requested by the mockup html
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    fontSize = 18.sp
                )
            }

            // Center: Notification icon with Badge in the middle of Top Bar
            Box(
                modifier = Modifier
                    .wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = com.example.utils.Localization.get("nav_notifications", lang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (partNeededCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .background(Color(0xFFEF4444), CircleShape)
                            .size(17.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = partNeededCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: Logo + Brand Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                if (lang == "fr") {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6750A4), Color(0xFF9B88D6))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .shadow(1.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 12.dp)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = com.example.utils.Localization.get("app_name", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = com.example.utils.Localization.get("premium_version", lang),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = com.example.utils.Localization.get("app_name", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = com.example.utils.Localization.get("premium_version", lang),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A) // Premium green
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6750A4), Color(0xFF9B88D6))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .shadow(1.dp, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom micro repairs screw symbol
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 12.dp)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout(viewModel: MainViewModel) {
    var activeScreen by remember { mutableStateOf("DASHBOARD") }
    var initialDetailsTicket by remember { mutableStateOf<Ticket?>(null) }
    val tickets by viewModel.ticketsState.collectAsState()
    val allTickets by viewModel.allTicketsState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val lang = settings["language"] ?: "ar"

    LaunchedEffect(activeScreen) {
        if (activeScreen != "TICKET_LIST") {
            initialDetailsTicket = null
        }
    }

    Scaffold(
        topBar = {
            if (activeScreen == "ADD_TICKET" || activeScreen == "NOTIFICATIONS") {
                // Handled in sub-screens
            } else {
                HeaderBrandBar(
                    viewModel = viewModel,
                    onNotificationClick = { activeScreen = "NOTIFICATIONS" }
                )
            }
        },
        bottomBar = {
            if (activeScreen != "ADD_TICKET" && activeScreen != "NOTIFICATIONS") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeScreen == "DASHBOARD",
                        onClick = { activeScreen = "DASHBOARD" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text(com.example.utils.Localization.get("nav_metrics", lang), fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == "TICKET_LIST",
                        onClick = { activeScreen = "TICKET_LIST" },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text(com.example.utils.Localization.get("nav_tickets", lang), fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == "SETTINGS",
                        onClick = { activeScreen = "SETTINGS" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(com.example.utils.Localization.get("nav_settings", lang), fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeScreen == "TICKET_LIST") {
                FloatingActionButton(
                    onClick = { activeScreen = "ADD_TICKET" },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Ticket", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeScreen) {
                "DASHBOARD" -> DashboardScreen(viewModel = viewModel, tickets = allTickets)
                "TICKET_LIST" -> TicketListScreen(
                    viewModel = viewModel,
                    tickets = tickets,
                    onAddTicketClicked = { activeScreen = "ADD_TICKET" },
                    initialDetailsTicket = initialDetailsTicket,
                    onDetailsClosed = { initialDetailsTicket = null }
                )
                "ADD_TICKET" -> TicketFormScreen(
                    viewModel = viewModel,
                    onSuccess = { activeScreen = "TICKET_LIST" },
                    onCancel = { activeScreen = "TICKET_LIST" }
                )
                "SETTINGS" -> SettingsScreen(viewModel = viewModel)
                "NOTIFICATIONS" -> NotificationsScreen(
                    viewModel = viewModel,
                    onBackClicked = { activeScreen = "DASHBOARD" },
                    onTicketClick = { ticket ->
                        initialDetailsTicket = ticket
                        activeScreen = "TICKET_LIST"
                    }
                )
            }
        }
    }
}
