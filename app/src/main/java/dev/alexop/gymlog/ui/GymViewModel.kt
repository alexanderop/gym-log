package dev.alexop.gymlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.alexop.gymlog.GymLogApp
import dev.alexop.gymlog.data.Exercise
import dev.alexop.gymlog.data.GymRepository
import dev.alexop.gymlog.data.SetEntry
import dev.alexop.gymlog.data.TemplateDetail
import dev.alexop.gymlog.data.Workout
import dev.alexop.gymlog.data.WorkoutDetail
import dev.alexop.gymlog.domain.FinishedSetRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GymViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: GymRepository = (app as GymLogApp).repository

    val templates: StateFlow<List<TemplateDetail>> = repo.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exercises: StateFlow<List<Exercise>> = repo.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val active: StateFlow<Workout?> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val history: StateFlow<List<Workout>> = repo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val finishedSets: StateFlow<List<FinishedSetRef>> = repo.observeFinishedSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun workout(id: Long): Flow<WorkoutDetail?> = repo.observeWorkout(id)

    suspend fun start(templateId: Long) = repo.startWorkout(templateId)
    suspend fun createTemplate(name: String) = repo.createTemplate(name)
    suspend fun renameTemplate(id: Long, name: String) = repo.renameTemplate(id, name)
    suspend fun deleteTemplate(id: Long) = repo.deleteTemplate(id)
    suspend fun addExerciseToTemplate(templateId: Long, exerciseId: Long) =
        repo.addExerciseToTemplate(templateId, exerciseId)

    suspend fun removeTemplateExercise(rowId: Long) = repo.removeTemplateExercise(rowId)
    suspend fun moveTemplateExercise(rowId: Long, up: Boolean) = repo.moveTemplateExercise(rowId, up)
    suspend fun createExercise(device: String, name: String) = repo.createExercise(device, name)
    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long) =
        repo.addExerciseToWorkout(workoutId, exerciseId)

    suspend fun removeWorkoutExercise(id: Long) = repo.removeWorkoutExercise(id)
    suspend fun addSet(workoutExerciseId: Long) = repo.addSet(workoutExerciseId)
    suspend fun updateSet(set: SetEntry) = repo.updateSet(set)
    suspend fun deleteSet(id: Long) = repo.deleteSet(id)
    suspend fun finish(id: Long) = repo.finishWorkout(id)
    suspend fun deleteWorkout(id: Long) = repo.deleteWorkout(id)
}
