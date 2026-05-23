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

@Composable
fun HeaderBrandBar() {
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

            // Right: Logo + Brand Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = "صيانة برو",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = "نسخة مفعلة (Premium)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A) // Premium green
                    )
                }

                // Custom App Icon styled precisely with Vibrant gradient
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout(viewModel: MainViewModel) {
    var activeScreen by remember { mutableStateOf("DASHBOARD") }
    val tickets by viewModel.ticketsState.collectAsState()

    Scaffold(
        topBar = {
            if (activeScreen == "ADD_TICKET") {
                CenterAlignedTopAppBar(
                    title = { Text("إنشاء تذكرة جديدة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = { activeScreen = "TICKET_LIST" }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                HeaderBrandBar()
            }
        },
        bottomBar = {
            if (activeScreen != "ADD_TICKET") {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeScreen == "DASHBOARD",
                        onClick = { activeScreen = "DASHBOARD" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("المؤشرات", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == "TICKET_LIST",
                        onClick = { activeScreen = "TICKET_LIST" },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        label = { Text("التذاكر", fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeScreen == "SETTINGS",
                        onClick = { activeScreen = "SETTINGS" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("الإعدادات", fontWeight = FontWeight.Bold) }
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
                "DASHBOARD" -> DashboardScreen(viewModel = viewModel, tickets = tickets)
                "TICKET_LIST" -> TicketListScreen(
                    viewModel = viewModel,
                    tickets = tickets,
                    onAddTicketClicked = { activeScreen = "ADD_TICKET" }
                )
                "ADD_TICKET" -> TicketFormScreen(
                    viewModel = viewModel,
                    onSuccess = { activeScreen = "TICKET_LIST" }
                )
                "SETTINGS" -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
