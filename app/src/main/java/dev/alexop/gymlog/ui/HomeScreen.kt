package dev.alexop.gymlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexop.gymlog.domain.formatElapsed
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: GymViewModel,
    padding: PaddingValues,
    onHistory: () -> Unit,
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit,
    onStart: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    val now = rememberNow()
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Gym Log", fontSize = 28.sp, color = Color.White, modifier = Modifier.weight(1f))
            QuietButton("Verlauf", onHistory)
        }
        val running = active
        if (running != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Training läuft", color = Muted, fontSize = 14.sp)
                Text(running.title, color = Color.White, fontSize = 22.sp)
                Text(formatElapsed(now - running.startedAt), color = Accent, fontSize = 28.sp)
                BigButton("Weiter", { onResume(running.id) })
            }
        }
        Text("Vorlagen", color = Muted, fontSize = 14.sp)
        templates.forEach { detail ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(detail.template.name, color = Color.White, fontSize = 22.sp)
                Text(exerciseCount(detail.rows.size), color = Muted, fontSize = 16.sp)
                BigButton(
                    text = "Starten",
                    onClick = { onStart(detail.template.id) },
                    enabled = running == null,
                )
                QuietButton("Bearbeiten", { onEdit(detail.template.id) }, Modifier.fillMaxWidth())
            }
        }
        BigButton("Neue Vorlage", onCreate)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

fun exerciseCount(count: Int): String = if (count == 1) "1 Übung" else "$count Übungen"
