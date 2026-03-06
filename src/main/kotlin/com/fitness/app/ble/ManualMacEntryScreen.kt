package com.fitness.app.ble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Manual MAC Entry Screen
 * WHY: Fallback pairing when BLE scan is unreliable
 * Use case: Ring already paired elsewhere, weak signal, office testing
 */
@Composable
fun ManualMacEntryScreen(
    onConfirm: (deviceName: String, macAddress: String) -> Unit,
    onCancel: () -> Unit
) {
    var deviceName by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var macError by remember { mutableStateOf<String?>(null) }
    
    // Validate MAC on change
    LaunchedEffect(macAddress) {
        macError = if (macAddress.isNotBlank()) {
            MacAddressValidator.getErrorMessage(macAddress)
        } else {
            null
        }
    }
    
    val isValid = macAddress.isNotBlank() && 
                  MacAddressValidator.isValidMacAddress(macAddress)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            "Manual Pairing",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Enter ring's MAC address directly",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Info card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "When to use manual entry:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Ring is already paired to another device\n" +
                        "• BLE scan doesn't find the ring\n" +
                        "• You know the MAC from another app",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Device Name (optional)
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name (Optional)") },
            placeholder = { Text("e.g., My Smart Ring") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Text(
            "For display only - you can name it anything",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // MAC Address (required)
        OutlinedTextField(
            value = macAddress,
            onValueChange = { 
                // Auto-format as user types
                val formatted = MacAddressValidator.formatMacAddress(it)
                macAddress = formatted ?: it
            },
            label = { Text("MAC Address *") },
            placeholder = { Text("XX:XX:XX:XX:XX:XX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = macError != null,
            supportingText = {
                if (macError != null) {
                    Text(
                        macError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "Enter exactly as shown in Bluetooth settings",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Example
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Example MAC addresses:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "AA:BB:CC:DD:EE:FF\n35:88:CC:F8:96:55",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = {
                    val name = deviceName.ifBlank { "Manual Device" }
                    onConfirm(name, macAddress.trim())
                },
                enabled = isValid,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Confirm Device",
                    fontWeight = FontWeight.Bold,
                    color = if (isValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
