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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alexop.gymlog.data.WorkoutDetail
import dev.alexop.gymlog.data.toRecord
import dev.alexop.gymlog.domain.durationMinutes
import dev.alexop.gymlog.domain.formatSetMarkdown
import dev.alexop.gymlog.domain.workoutClock
import java.time.ZoneId

@Composable
fun SummaryScreen(
    detail: WorkoutDetail,
    padding: PaddingValues,
    primary: String?,
    onPrimary: (() -> Unit)?,
    showDelete: Boolean,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val end = detail.workout.finishedAt ?: detail.workout.startedAt
    val clock = workoutClock(detail.workout.startedAt, end, ZoneId.systemDefault())
    val logged = detail.exercises.filter { it.sets.isNotEmpty() }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuietButton("Zurück", onBack)
            Text(detail.workout.title, color = Color.White, fontSize = 28.sp)
            Text(
                "${clock.date} · ${clock.time} · ${durationMinutes(detail.workout.startedAt, end)} Min",
                color = Muted,
                fontSize = 16.sp,
            )
            if (logged.isEmpty()) {
                Text("Keine Sätze", color = Muted, fontSize = 16.sp)
            }
            logged.forEach { exercise ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Card, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(exercise.entry.name, color = Color.White, fontSize = 20.sp)
                    exercise.sets.forEach { set ->
                        Text(formatSetMarkdown(set.toRecord()), color = Color.White, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BigButton("Als Markdown kopieren", onCopy)
            BigButton("Teilen", onShare)
            if (primary != null && onPrimary != null) BigButton(primary, onPrimary)
            if (showDelete) QuietButton("Löschen", { confirm = true }, Modifier.fillMaxWidth())
        }
    }

    if (confirm) {
        ConfirmDialog(
            text = "Training löschen?",
            onYes = {
                confirm = false
                onDelete()
            },
            onNo = { confirm = false },
        )
    }
}
