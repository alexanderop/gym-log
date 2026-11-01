package dev.alexop.gymlog.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexop.gymlog.data.Workout
import dev.alexop.gymlog.domain.durationMinutes
import dev.alexop.gymlog.domain.workoutClock
import kotlinx.coroutines.launch
import java.time.ZoneId

@Composable
fun HistoryScreen(
    viewModel: GymViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Workout?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuietButton("Zurück", onBack)
        Text("Verlauf", color = Color.White, fontSize = 28.sp)
        if (history.isEmpty()) {
            Text("Noch keine Trainings.", color = Muted, fontSize = 16.sp)
        }
        history.forEach { workout ->
            val end = workout.finishedAt ?: workout.startedAt
            val clock = workoutClock(workout.startedAt, end, ZoneId.systemDefault())
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(workout.title, color = Color.White, fontSize = 22.sp)
                Text(
                    "${clock.date} · ${clock.time} · ${durationMinutes(workout.startedAt, end)} Min",
                    color = Muted,
                    fontSize = 16.sp,
                )
                BigButton("Öffnen", { onOpen(workout.id) })
                QuietButton("Löschen", { pendingDelete = workout }, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    val deleting = pendingDelete
    if (deleting != null) {
        ConfirmDialog(
            text = "Training löschen?",
            onYes = {
                pendingDelete = null
                scope.launch { viewModel.deleteWorkout(deleting.id) }
            },
            onNo = { pendingDelete = null },
        )
    }
}
