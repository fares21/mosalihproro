package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.MainViewModel
import com.example.utils.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketFormScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    val currency = settings["store_currency"] ?: "د.إ"
    val lang = settings["language"] ?: "ar"
    val coroutineScope = rememberCoroutineScope()

    // Inputs
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var faultDescription by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var advanceInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Compressed Image paths
    var frontImagePath by remember { mutableStateOf<String?>(null) }
    var backImagePath by remember { mutableStateOf<String?>(null) }

    // Price Math
    val totalPrice = priceInput.toDoubleOrNull() ?: 0.0
    val advance = advanceInput.toDoubleOrNull() ?: 0.0
    val remainingAmount = maxOf(0.0, totalPrice - advance)

    // Camera Capture States & Launcher
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var currentCaptureSide by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                val side = currentCaptureSide ?: "front"
                val compressedPath = ImageUtils.compressAndSaveImage(context, uri, side)
                if (compressedPath != null) {
                    if (side == "front") {
                        frontImagePath = compressedPath
                        Toast.makeText(context, com.example.utils.Localization.get("photo_front_captured_success", lang), Toast.LENGTH_SHORT).show()
                    } else {
                        backImagePath = compressedPath
                        Toast.makeText(context, com.example.utils.Localization.get("photo_back_captured_success", lang), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, com.example.utils.Localization.get("photo_compression_failed", lang), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val launchCameraDirect = { side: String ->
        try {
            val photoDir = File(context.filesDir, "photos").apply { mkdirs() }
            val tempFile = File.createTempFile("temp_capture_${side}_", ".jpg", photoDir)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, String.format(com.example.utils.Localization.get("camera_error", lang), e.message), Toast.LENGTH_SHORT).show()
        }
    }

    val launchCamera = { side: String ->
        currentCaptureSide = side
        launchCameraDirect(side)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High quality top bar with Back button
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = com.example.utils.Localization.get("new_ticket_title", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            navigationIcon = {
                IconButton(onClick = onCancel) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
        ) {

        // Customer & Device Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
            ) {
                Text(
                    text = com.example.utils.Localization.get("client_device_info", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text(com.example.utils.Localization.get("client_name_placeholder", lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text(com.example.utils.Localization.get("client_phone_placeholder", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text(com.example.utils.Localization.get("device_model_placeholder", lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = faultDescription,
                    onValueChange = { faultDescription = it },
                    label = { Text(com.example.utils.Localization.get("fault_description_placeholder", lang)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Photos (Front and Back Capture)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
            ) {
                Text(
                    text = com.example.utils.Localization.get("device_photo_documentation", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = com.example.utils.Localization.get("photo_front_side", lang),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back Photo
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (backImagePath != null) {
                            AsyncImage(
                                model = File(backImagePath!!),
                                contentDescription = "Back photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCameraBack, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { launchCamera("back") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(com.example.utils.Localization.get("back_side_label", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Front Photo
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (frontImagePath != null) {
                            AsyncImage(
                                model = File(frontImagePath!!),
                                contentDescription = "Front photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { launchCamera("front") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(com.example.utils.Localization.get("front_side_label", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Financial Deal & Notes Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = if (lang == "fr") Alignment.Start else Alignment.End
            ) {
                Text(
                    text = com.example.utils.Localization.get("fees_cost", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = advanceInput,
                        onValueChange = { advanceInput = it },
                        label = { Text(com.example.utils.Localization.get("advance_payment", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text(com.example.utils.Localization.get("total_estimated_cost", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time remaining calculation display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = if (lang == "fr") Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Row(
                        horizontalArrangement = if (lang == "fr") Arrangement.Start else Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lang == "fr") {
                            Text(
                                text = com.example.utils.Localization.get("remaining_payment", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${String.format("%.2f", remainingAmount)} $currency",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "${String.format("%.2f", remainingAmount)} $currency",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = com.example.utils.Localization.get("remaining_payment", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(com.example.utils.Localization.get("write_tech_notes", lang)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel Button
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(com.example.utils.Localization.get("cancel", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Submit Button
            Button(
                onClick = {
                    if (customerName.isBlank() || customerPhone.isBlank() || deviceModel.isBlank() || faultDescription.isBlank()) {
                        Toast.makeText(context, com.example.utils.Localization.get("fill_required_fields", lang), Toast.LENGTH_LONG).show()
                    } else if (totalPrice <= 0) {
                        Toast.makeText(context, com.example.utils.Localization.get("price_positive_error", lang), Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.addTicket(
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            deviceModel = deviceModel.trim(),
                            fault = faultDescription.trim(),
                            price = totalPrice,
                            advance = advance,
                            notes = notes.trim(),
                            signaturePath = null,
                            frontImage = frontImagePath,
                            backImage = backImagePath
                        ) { insertedId ->
                            // Post-Save
                            coroutineScope.launch {
                                Toast.makeText(context, com.example.utils.Localization.get("save_success_with_id", lang) + insertedId, Toast.LENGTH_LONG).show()
                                onSuccess()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp)
            ) {
                Icon(Icons.Default.PostAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(com.example.utils.Localization.get("save_ticket", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
}
