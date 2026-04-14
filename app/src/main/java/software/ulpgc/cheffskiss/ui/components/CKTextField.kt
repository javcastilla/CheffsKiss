package software.ulpgc.cheffskiss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import software.ulpgc.cheffskiss.ui.theme.*

val SurfaceContainerLow = Color(0xFFEDF5E1)
val SurfaceContainerHigh = Color(0xFFE2EBD7)

@Composable
 fun CKTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    helper: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CKOnSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Outline.copy(alpha = 0.5f)) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType
            ),
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                cursorColor = Primary,
                errorBorderColor = Color(0xFFBA1A1A)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        helper?.invoke()
    }
}
@Composable
fun CKHelperText(text: String, color: Color = Color(0xFFBA1A1A)) {
    Text(
        text,
        fontSize = 11.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun CKPasswordStrength(strength: Int, label: String, color: Color) {
    Column(
        modifier = Modifier.padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index < strength) color else SurfaceContainerHigh,
                            RoundedCornerShape(50.dp)
                        )
                )
            }
        }
        if (label.isNotEmpty()) {
            Text(
                text = "Strength: $label",
                fontSize = 11.sp,
                color = CKOnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
