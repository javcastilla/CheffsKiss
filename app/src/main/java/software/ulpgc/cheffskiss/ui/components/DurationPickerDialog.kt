package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import software.ulpgc.cheffskiss.ui.theme.*

@Composable
fun DurationPickerDialog(
    hours: Int,
    minutes: Int,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHours by remember { mutableIntStateOf(hours) }
    var selectedMinutes by remember { mutableIntStateOf(minutes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Select duration", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NumberPicker(
                        value = selectedHours,
                        range = 0..23,
                        label = "h",
                        onValueChange = { selectedHours = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    NumberPicker(
                        value = selectedMinutes,
                        range = 0..59,
                        label = "min",
                        onValueChange = { selectedMinutes = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }

                    Button(
                        onClick = { onConfirm(selectedHours, selectedMinutes) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Confirm", fontWeight = FontWeight.Bold, color = OnPrimary) }
                }
            }
        }
    }
}

@Composable
private fun NumberPicker(
    value: Int,
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value.toString().padStart(2, '0')) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(
            onClick = {
                val next = value + 1
                onValueChange(if (next > range.last) range.first else next)
            },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CKSurfaceVariant, contentColor = OnSurface),
            modifier = Modifier.size(40.dp)
        ) { Text("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(2)
                        textValue = digits
                        val num = digits.toIntOrNull() ?: return@OutlinedTextField
                        if (num in range) onValueChange(num)
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier.width(80.dp)
                )
                Text(label, fontSize = 11.sp, color = CKOnSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }

        FilledIconButton(
            onClick = {
                val prev = value - 1
                onValueChange(if (prev < range.first) range.last else prev)
            },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CKSurfaceVariant, contentColor = OnSurface),
            modifier = Modifier.size(40.dp)
        ) { Text("▼", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface) }
    }
}