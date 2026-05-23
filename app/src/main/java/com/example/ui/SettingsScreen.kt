package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()

    // Temp screen states initialized from DB map
    var storeName by remember { mutableStateOf("") }
    var storePhone by remember { mutableStateOf("") }
    var storeCurrency by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderHour by remember { mutableStateOf("10") }
    var maintenanceReminderDelay by remember { mutableStateOf("10") }
    var maintenanceRingtoneName by remember { mutableStateOf("رنة النظام الافتراضية") }

    // Synchronize UI buffers once settings load
    LaunchedEffect(settings) {
        if (settings.isNotEmpty()) {
            storeName = settings["store_name"] ?: "متجر صيانة الهواتف"
            storePhone = settings["store_phone"] ?: "0500000000"
            storeCurrency = settings["store_currency"] ?: "د.إ"
            reminderEnabled = settings["reminder_enabled"]?.toBoolean() ?: true
            reminderHour = settings["reminder_hour"] ?: "10"
            maintenanceReminderDelay = settings["maintenance_reminder_delay"] ?: "10"
            maintenanceRingtoneName = settings["maintenance_ringtone_name"] ?: "رنة النظام الافتراضية"
        }
    }

    val ringtonePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context) ?: "رنة صيانة مخصصة"
                viewModel.saveSetting("maintenance_ringtone_uri", uri.toString())
                viewModel.saveSetting("maintenance_ringtone_name", title)
                maintenanceRingtoneName = title
            } else {
                viewModel.saveSetting("maintenance_ringtone_uri", "")
                viewModel.saveSetting("maintenance_ringtone_name", "رنة النظام الافتراضية")
                maintenanceRingtoneName = "رنة النظام الافتراضية"
            }
        }
    }

    val launchRingtonePicker = {
        try {
            val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_RINGTONE)
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                val existingUriStr = settings["maintenance_ringtone_uri"]
                if (!existingUriStr.isNullOrEmpty()) {
                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(existingUriStr))
                }
            }
            ringtonePickerLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "⚠️ نظام الهاتف لا يحتوي على تطبيق اختيار النغمات؛ سيتم استخدام النغمة الافتراضية.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "إعدادات النظام الفني والشركة",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Section 1: Store profile Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "بيانات المتجر والشركة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("اسم المتجر / الشركة") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = { Text("رقم الهاتف للتواصل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = storeCurrency,
                    onValueChange = { storeCurrency = it },
                    label = { Text("العملة النقدية في الفواتير (مثال: د.إ, $, د.ج)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: WorkManager Automated Alerts
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "التنبيهات وجدولة تذكيرات الصيانة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                    Text(
                        text = "تفعيل إشعارات التذكير التلقائية",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reminderHour,
                    onValueChange = { reminderHour = it },
                    label = { Text("الساعة التي ترغب بالتنبيه بها (من ٠ إلى ٢٣)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("10") },
                    singleLine = true,
                    enabled = reminderEnabled,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* ستقوم خدمة التذكيرات (WorkManager) بالبحث والتحقق من التذاكر المعلقة التي مضى عليها ٢٤ ساعة بدون إصلاح في هذا الحين وطرح إشعار بهاتفك.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val permission = "android.permission.POST_NOTIFICATIONS"
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(context, "⚠️ الرجاء منح إذن الإشعارات من إعدادات الهاتف لتتمكن من تلقي التنبيهات!", Toast.LENGTH_LONG).show()
                            }
                        }

                        val activeTicketsCount = viewModel.ticketsState.value.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }
                        val channelId = "phone_repair_reminders"
                        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val channel = android.app.NotificationChannel(
                                channelId,
                                "تذكير تذاكر الصيانة المنسية",
                                android.app.NotificationManager.IMPORTANCE_HIGH
                            ).apply {
                                description = "إشعارات للتذاكر التي تجاوزت ٢٤ ساعة دون تسليم"
                            }
                            notificationManager.createNotificationChannel(channel)
                        }

                        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.stat_notify_chat)
                            .setContentTitle("تنبيه تجريبي لتذاكر الصيانة المنسية!")
                            .setContentText("لديك $activeTicketsCount تذاكر معلقة أو منسية لم تسلم بعد. يرجى مراجعتها.")
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)

                        notificationManager.notify(991, builder.build())
                        Toast.makeText(context, "🕒 تم فحص التذاكر وإرسال تنبيه بالعدد الحالي: ($activeTicketsCount) تذكرة معلقة!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إجراء فحص تجريبي للتذاكر المعلقة الآن 🕒", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Alarm / Individual Timer Settings
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إعدادات مؤقت تذكير الصيانة الفوري",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = maintenanceReminderDelay,
                    onValueChange = { maintenanceReminderDelay = it },
                    label = { Text("مدة مؤقت التذكير الافتراضي (بالدقائق)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("10") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "نغمة رنة تذكير الصيانة المحددة:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )

                Button(
                    onClick = { launchRingtonePicker() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = maintenanceRingtoneName,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* عند تفعيل خيار \"يحتاج وقت\" لأي جهاز صيانة في لوحة التحكم، سينطلق المنبه المبرمج بهاتفك بعد انقضاء المدة المحددة مع تشغيل الرنة المرتئاة.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            val permission = "android.permission.POST_NOTIFICATIONS"
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(context, "⚠️ الرجاء منح إذن الإشعارات من إعدادات الهاتف لتتمكن من تلقي التذكير الفوري!", Toast.LENGTH_LONG).show()
                            }
                        }

                        viewModel.scheduleDeviceReminder(
                            ticketId = 9999L,
                            deviceModel = "جهاز تجريبي (صيانة برو)",
                            delayMinutes = 0,
                            ringtoneUri = settings["maintenance_ringtone_uri"]
                        )
                        Toast.makeText(context, "⏰ تم إرسال طلب منبه صيانة فوري! سيصدر التنبيه حالاً بجهازك.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال تنبيه تجريبي فوري الآن 🔔", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val hourInt = reminderHour.toIntOrNull()
                val delayInt = maintenanceReminderDelay.toIntOrNull()
                if (hourInt == null || hourInt !in 0..23) {
                    Toast.makeText(context, "الرجاء تحديد ساعة تذكير صالحة بين 0 و 23", Toast.LENGTH_LONG).show()
                } else if (delayInt == null || delayInt <= 0) {
                    Toast.makeText(context, "الرجاء تحديد مدة تذكير صحيحة بالدقائق (أكبر من 0)", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.saveSetting("store_name", storeName.trim())
                    viewModel.saveSetting("store_phone", storePhone.trim())
                    viewModel.saveSetting("store_currency", storeCurrency.trim())
                    viewModel.saveSetting("reminder_enabled", reminderEnabled.toString())
                    viewModel.saveSetting("reminder_hour", hourInt.toString())
                    viewModel.saveSetting("maintenance_reminder_delay", delayInt.toString())
                    Toast.makeText(context, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("حفظ التعديلات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
