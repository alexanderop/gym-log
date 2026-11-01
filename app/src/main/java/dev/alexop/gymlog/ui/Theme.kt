package dev.alexop.gymlog.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accent = Color(0xFFB7C4A8)
val Muted = Color(0xFF9A9A9A)
val Card = Color(0xFF141414)
val Field = Color(0xFF1C1C1C)
val Line = Color(0xFF2A2A2A)

private val Colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Accent,
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Card,
    onSurfaceVariant = Muted,
    outline = Line,
)

private val Type = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.White),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.White),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White),
    bodyLarge = TextStyle(fontSize = 18.sp, color = Color.White),
    bodyMedium = TextStyle(fontSize = 16.sp, color = Color.White),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White),
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = Type,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp),
        ),
        content = content,
    )
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Accent,
    unfocusedBorderColor = Line,
    cursorColor = Accent,
    focusedContainerColor = Field,
    unfocusedContainerColor = Field,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
)

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFF2A2A2A),
            disabledContentColor = Color(0xFF777777),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text, fontSize = 18.sp)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
    ) {
        Text(text, fontSize = 16.sp)
    }
}
