package dev.alexop.gymlog.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexop.gymlog.data.SetEntry
import dev.alexop.gymlog.data.WorkoutExerciseDetail
import dev.alexop.gymlog.domain.formatElapsed
import dev.alexop.gymlog.domain.formatKg
import dev.alexop.gymlog.domain.formatLastTimeLine
import dev.alexop.gymlog.domain.historyFromFinishedSets
import dev.alexop.gymlog.domain.lastPerformance
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(
    viewModel: GymViewModel,
    workoutId: Long,
    padding: PaddingValues,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onFinished: () -> Unit,
    onCancelled: () -> Unit,
) {
    val detail by viewModel.workout(workoutId).collectAsStateWithLifecycle(initialValue = null)
    val finished by viewModel.finishedSets.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val now = rememberNow()
    var confirmCancel by remember { mutableStateOf(false) }
    val workout = detail
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    if (workout == null) {
        Column(Modifier.fillMaxSize().padding(padding)) {
            QuietButton("Zurück", onBack)
        }
        return
    }

    val history = remember(finished) { historyFromFinishedSets(finished) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            QuietButton("Zurück", onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    workout.workout.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatElapsed(now - workout.workout.startedAt),
                    color = Accent,
                    fontSize = 28.sp,
                )
            }
            workout.exercises.forEach { exercise ->
                key(exercise.entry.id) {
                    val last = lastPerformance(exercise.entry.exerciseId, history)
                    ExerciseBlock(
                        exercise = exercise,
                        lastLines = last.orEmpty().map { formatLastTimeLine(it) },
                        onAddSet = { scope.launch { viewModel.addSet(exercise.entry.id) } },
                        onChangeSet = { scope.launch { viewModel.updateSet(it) } },
                        onDeleteSet = { scope.launch { viewModel.deleteSet(it) } },
                        onRemove = { scope.launch { viewModel.removeWorkoutExercise(exercise.entry.id) } },
                    )
                }
            }
            BigButton("Übung hinzufügen", onAddExercise)
            Spacer(Modifier.height(12.dp))
        }
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BigButton(
                text = "Training beenden",
                onClick = {
                    scope.launch {
                        viewModel.finish(workoutId)
                        onFinished()
                    }
                },
            )
            QuietButton("Abbrechen", { confirmCancel = true }, Modifier.fillMaxWidth())
        }
    }

    if (confirmCancel) {
        ConfirmDialog(
            text = "Training verwerfen?",
            onYes = {
                confirmCancel = false
                scope.launch {
                    viewModel.deleteWorkout(workoutId)
                    onCancelled()
                }
            },
            onNo = { confirmCancel = false },
        )
    }
}

@Composable
private fun ExerciseBlock(
    exercise: WorkoutExerciseDetail,
    lastLines: List<String>,
    onAddSet: () -> Unit,
    onChangeSet: (SetEntry) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Card, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(exercise.entry.name, color = Color.White, fontSize = 20.sp)
        if (lastLines.isNotEmpty()) {
            Text("Letztes Mal", color = Muted, fontSize = 14.sp)
            lastLines.forEach { line ->
                Text(line, color = Color.White, fontSize = 16.sp)
            }
        }
        exercise.sets.forEachIndexed { index, set ->
            key(set.id) {
                SetEditor(
                    index = index + 1,
                    set = set,
                    onChange = onChangeSet,
                    onDelete = { onDeleteSet(set.id) },
                )
            }
        }
        BigButton("Satz hinzufügen", onAddSet)
        QuietButton("Übung entfernen", onRemove, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SetEditor(
    index: Int,
    set: SetEntry,
    onChange: (SetEntry) -> Unit,
    onDelete: () -> Unit,
) {
    var current by remember(set.id) { mutableStateOf(set) }
    var weightText by remember(set.id) {
        mutableStateOf(if (set.weightKg == 0.0) "" else formatKg(set.weightKg))
    }
    var repsText by remember(set.id) {
        mutableStateOf(if (set.reps == 0) "" else set.reps.toString())
    }
    var noteText by remember(set.id) { mutableStateOf(set.note) }

    fun push(next: SetEntry) {
        current = next
        onChange(next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Satz $index", color = Muted, fontSize = 14.sp)
        Text(if (current.bodyweight) "Extra kg" else "kg", color = Muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val cleaned = sanitizeDecimal(raw)
                    weightText = cleaned
                    parseDecimal(cleaned)?.let { push(current.copy(weightKg = it)) }
                },
                modifier = Modifier.weight(1.2f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, color = Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors(),
                shape = RoundedCornerShape(8.dp),
            )
            ToggleButton(
                text = "Körpergewicht",
                selected = current.bodyweight,
                onClick = { push(current.copy(bodyweight = !current.bodyweight)) },
                modifier = Modifier.weight(1f),
            )
        }
        Text("Wdh.", color = Muted, fontSize = 13.sp)
        OutlinedTextField(
            value = repsText,
            onValueChange = { raw ->
                val cleaned = raw.filter { it.isDigit() }.take(4)
                repsText = cleaned
                push(current.copy(reps = cleaned.toIntOrNull() ?: 0))
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, color = Color.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        Text("RIR", color = Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..5).forEach { value ->
                ToggleButton(
                    text = value.toString(),
                    selected = current.rir == value,
                    onClick = { push(current.copy(rir = value)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        OutlinedTextField(
            value = noteText,
            onValueChange = {
                noteText = it
                push(current.copy(note = it))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Notiz") },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, color = Color.White),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        QuietButton("Satz löschen", onDelete, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Accent else Field,
            contentColor = if (selected) Color.Black else Color.White,
        ),
    ) {
        Text(text, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmDialog(text: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        containerColor = Color(0xFF141414),
        text = { Text(text, color = Color.White, fontSize = 20.sp) },
        confirmButton = {
            TextButton(onClick = onYes) { Text("Ja", color = Accent, fontSize = 18.sp) }
        },
        dismissButton = {
            TextButton(onClick = onNo) { Text("Nein", color = Color.White, fontSize = 18.sp) }
        },
    )
}

fun sanitizeDecimal(raw: String): String = buildString {
    var separator = false
    for (ch in raw) {
        when {
            ch.isDigit() -> append(ch)
            (ch == '.' || ch == ',') && !separator -> {
                append('.')
                separator = true
            }
        }
    }
}

fun parseDecimal(text: String): Double? {
    if (text.isBlank() || text == ".") return 0.0
    val normalized = if (text.endsWith('.')) text.dropLast(1) else text
    if (normalized.isBlank()) return 0.0
    return normalized.toDoubleOrNull()
}
