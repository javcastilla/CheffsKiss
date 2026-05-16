package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.domain.enum.Measurement
import software.ulpgc.cheffskiss.ui.screen.label
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOutlineVariant
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementUnitDropdown(
    selected: Measurement,
    onSelected: (Measurement) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.width(96.dp),
    ) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(44.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = CKOutlineVariant.copy(alpha = 0.4f),
                focusedContainerColor = Background,
                unfocusedContainerColor = Background,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Background),
        ) {
            Measurement.entries.forEach { measurement ->
                DropdownMenuItem(
                    text = {
                        Text(
                            measurement.label(),
                            fontSize = 13.sp,
                            color = OnSurface,
                        )
                    },
                    onClick = {
                        onSelected(measurement)
                        expanded = false
                    },
                )
            }
        }
    }
}
