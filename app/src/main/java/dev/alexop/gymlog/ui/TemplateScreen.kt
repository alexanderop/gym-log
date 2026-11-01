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
import kotlinx.coroutines.launch

@Composable
fun TemplateScreen(
    viewModel: GymViewModel,
    templateId: Long,
    padding: PaddingValues,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val detail = templates.firstOrNull { it.template.id == templateId }
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var name by remember(templateId) { mutableStateOf("") }
    var nameReady by remember(templateId) { mutableStateOf(false) }
    val loadedName = detail?.template?.name
    if (!nameReady && loadedName != null) {
        name = loadedName
        nameReady = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuietButton("Zurück", onBack)
        Text("Vorlage", color = Color.White, fontSize = 28.sp)
        if (detail == null) {
            return@Column
        }
        Text("Name", color = Muted, fontSize = 13.sp)
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) scope.launch { viewModel.renameTemplate(templateId, it) }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 22.sp, color = Color.White),
            singleLine = true,
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        Text("Übungen", color = Muted, fontSize = 14.sp)
        detail.rows.forEach { row ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Card, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(row.title, color = Color.White, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuietButton(
                        "Hoch",
                        { scope.launch { viewModel.moveTemplateExercise(row.row.id, up = true) } },
                        Modifier.weight(1f),
                    )
                    QuietButton(
                        "Runter",
                        { scope.launch { viewModel.moveTemplateExercise(row.row.id, up = false) } },
                        Modifier.weight(1f),
                    )
                }
                QuietButton(
                    "Entfernen",
                    { scope.launch { viewModel.removeTemplateExercise(row.row.id) } },
                    Modifier.fillMaxWidth(),
                )
            }
        }
        BigButton("Übung hinzufügen", onAdd)
        QuietButton("Vorlage löschen", { confirm = true }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }

    if (confirm) {
        ConfirmDialog(
            text = "Vorlage löschen?",
            onYes = {
                confirm = false
                scope.launch {
                    viewModel.deleteTemplate(templateId)
                    onBack()
                }
            },
            onNo = { confirm = false },
        )
    }
}
