package dev.alexop.gymlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexop.gymlog.domain.exerciseTitle
import kotlinx.coroutines.launch

@Composable
fun PickExerciseScreen(
    viewModel: GymViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var device by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val preview = if (device.isBlank() || name.isBlank()) "" else exerciseTitle(device, name)
    val filtered = exercises.filter { exercise ->
        val title = exerciseTitle(exercise.device, exercise.name)
        query.isBlank() || title.contains(query.trim(), ignoreCase = true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuietButton("Zurück", onBack)
        Text("Übung", color = Color.White, fontSize = 28.sp)
        Text("Gerät", color = Muted, fontSize = 13.sp)
        OutlinedTextField(
            value = device,
            onValueChange = { device = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("z. B. Egym") },
            textStyle = TextStyle(fontSize = 20.sp, color = Color.White),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        Text("Übung", color = Muted, fontSize = 13.sp)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("z. B. Bench Press") },
            textStyle = TextStyle(fontSize = 20.sp, color = Color.White),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        if (preview.isNotEmpty()) {
            Text(preview, color = Accent, fontSize = 16.sp)
        }
        BigButton(
            text = "Anlegen und hinzufügen",
            enabled = device.isNotBlank() && name.isNotBlank(),
            onClick = {
                scope.launch {
                    val id = viewModel.createExercise(device, name)
                    onPicked(id)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Text("Vorhandene Übungen", color = Muted, fontSize = 14.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Suchen") },
            textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        filtered.forEach { exercise ->
            Text(
                exerciseTitle(exercise.device, exercise.name),
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(8.dp))
                    .clickable { onPicked(exercise.id) }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
