package dev.alexop.gymlog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alexop.gymlog.data.WorkoutDetail
import dev.alexop.gymlog.data.toRecord
import dev.alexop.gymlog.domain.formatWorkoutMarkdown
import kotlinx.coroutines.launch
import java.time.ZoneId

sealed interface Route {
    data object Home : Route
    data class Workout(val id: Long) : Route
    data class Done(val id: Long) : Route
    data object History : Route
    data class HistoryDetail(val id: Long) : Route
    data class Template(val id: Long) : Route
    data class Pick(val mode: String, val ownerId: Long) : Route
}

fun Route.encode(): String = when (this) {
    Route.Home -> "home"
    is Route.Workout -> "workout:$id"
    is Route.Done -> "done:$id"
    Route.History -> "history"
    is Route.HistoryDetail -> "history:$id"
    is Route.Template -> "template:$id"
    is Route.Pick -> "pick:$mode:$ownerId"
}

fun decodeRoute(raw: String): Route {
    val parts = raw.split(":")
    return when (parts[0]) {
        "workout" -> Route.Workout(parts[1].toLong())
        "done" -> Route.Done(parts[1].toLong())
        "history" -> if (parts.size == 1) Route.History else Route.HistoryDetail(parts[1].toLong())
        "template" -> Route.Template(parts[1].toLong())
        "pick" -> Route.Pick(parts[1], parts[2].toLong())
        else -> Route.Home
    }
}

@Composable
fun GymApp(viewModel: GymViewModel) {
    val saver = listSaver<SnapshotStateList<Route>, String>(
        save = { list -> list.map { it.encode() } },
        restore = { saved ->
            mutableStateListOf<Route>().apply {
                addAll(saved.map { decodeRoute(it) })
                if (isEmpty()) add(Route.Home)
            }
        },
    )
    val stack = rememberSaveable(saver = saver) { mutableStateListOf(Route.Home) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val route = stack.last()

    fun push(next: Route) {
        stack.add(next)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            when (val current = route) {
                Route.Home -> HomeScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onHistory = { push(Route.History) },
                    onEdit = { push(Route.Template(it)) },
                    onCreate = {
                        scope.launch {
                            val id = viewModel.createTemplate("Neue Vorlage")
                            push(Route.Template(id))
                        }
                    },
                    onStart = { templateId ->
                        scope.launch {
                            val id = viewModel.start(templateId)
                            if (id > 0) push(Route.Workout(id))
                        }
                    },
                    onResume = { push(Route.Workout(it)) },
                )

                is Route.Workout -> WorkoutScreen(
                    viewModel = viewModel,
                    workoutId = current.id,
                    padding = padding,
                    onBack = { pop() },
                    onAddExercise = { push(Route.Pick("workout", current.id)) },
                    onFinished = {
                        if (stack.last() is Route.Workout) stack[stack.lastIndex] = Route.Done(current.id)
                    },
                    onCancelled = { pop() },
                )

                is Route.Done -> SummaryRoute(
                    viewModel = viewModel,
                    workoutId = current.id,
                    padding = padding,
                    primary = "Fertig",
                    onPrimary = { pop() },
                    onBack = { pop() },
                    snackbar = snackbar,
                    showDelete = false,
                )

                Route.History -> HistoryScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onBack = { pop() },
                    onOpen = { push(Route.HistoryDetail(it)) },
                )

                is Route.HistoryDetail -> SummaryRoute(
                    viewModel = viewModel,
                    workoutId = current.id,
                    padding = padding,
                    primary = null,
                    onPrimary = null,
                    onBack = { pop() },
                    snackbar = snackbar,
                    showDelete = true,
                )

                is Route.Template -> TemplateScreen(
                    viewModel = viewModel,
                    templateId = current.id,
                    padding = padding,
                    onBack = { pop() },
                    onAdd = { push(Route.Pick("template", current.id)) },
                )

                is Route.Pick -> PickExerciseScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onBack = { pop() },
                    onPicked = { exerciseId ->
                        scope.launch {
                            if (current.mode == "template") {
                                viewModel.addExerciseToTemplate(current.ownerId, exerciseId)
                            } else {
                                viewModel.addExerciseToWorkout(current.ownerId, exerciseId)
                            }
                            pop()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryRoute(
    viewModel: GymViewModel,
    workoutId: Long,
    padding: PaddingValues,
    primary: String?,
    onPrimary: (() -> Unit)?,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    showDelete: Boolean,
) {
    val detail by viewModel.workout(workoutId).collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val value = detail
    if (value == null) {
        BackOnly(padding, onBack)
        return
    }
    SummaryScreen(
        detail = value,
        padding = padding,
        primary = primary,
        onPrimary = onPrimary,
        showDelete = showDelete,
        onBack = onBack,
        onCopy = {
            copyText(context, markdownOf(value))
            scope.launch { snackbar.showSnackbar("Kopiert") }
        },
        onShare = { shareMarkdown(context, value.workout.title, value.workout.startedAt, markdownOf(value)) },
        onDelete = {
            scope.launch {
                viewModel.deleteWorkout(workoutId)
                onBack()
            }
        },
    )
}

private fun markdownOf(detail: WorkoutDetail): String {
    val end = detail.workout.finishedAt ?: detail.workout.startedAt
    return formatWorkoutMarkdown(
        dev.alexop.gymlog.domain.WorkoutRecord(
            title = detail.workout.title,
            startedAtEpochMs = detail.workout.startedAt,
            finishedAtEpochMs = end,
            exercises = detail.exercises.map { exercise ->
                dev.alexop.gymlog.domain.ExerciseRecord(
                    name = exercise.entry.name,
                    sets = exercise.sets.map { it.toRecord() },
                )
            },
        ),
        ZoneId.systemDefault(),
    )
}

@Composable
private fun BackOnly(padding: PaddingValues, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(padding),
    ) {
        QuietButton("Zurück", onBack)
    }
}
