package dev.alexop.gymlog.data

import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dev.alexop.gymlog.domain.FinishedSetRef
import dev.alexop.gymlog.domain.SetRecord
import dev.alexop.gymlog.domain.exerciseTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class TemplateExerciseRow(
    val row: TemplateExercise,
    val title: String,
)

data class TemplateDetail(
    val template: Template,
    val rows: List<TemplateExerciseRow>,
)

data class WorkoutExerciseDetail(
    val entry: WorkoutExercise,
    val sets: List<SetEntry>,
)

data class WorkoutDetail(
    val workout: Workout,
    val exercises: List<WorkoutExerciseDetail>,
)

class GymRepository(private val db: GymDatabase) {
    private val dao = db.dao()

    suspend fun seedIfEmpty() {
        if (dao.templateCount() > 0) return
        db.withTransaction {
            val ids = mutableMapOf<Pair<String, String>, Long>()
            for ((device, name) in Seed.catalog) {
                ids[device to name] = dao.insertExercise(Exercise(device = device, name = name))
            }
            suspend fun insertTemplate(name: String, position: Int, exercises: List<Pair<String, String>>) {
                val templateId = dao.insertTemplate(Template(name = name, position = position))
                exercises.forEachIndexed { index, key ->
                    dao.insertTemplateExercise(
                        TemplateExercise(
                            templateId = templateId,
                            exerciseId = ids.getValue(key),
                            position = index,
                        ),
                    )
                }
            }
            insertTemplate("Oberkörper", 0, Seed.upper)
            insertTemplate("Unterkörper", 1, Seed.lower)
        }
    }

    fun observeTemplates(): Flow<List<TemplateDetail>> = combine(
        dao.observeTemplates(),
        dao.observeTemplateExercises(),
        dao.observeExercises(),
    ) { templates, links, exercises ->
        val byId = exercises.associateBy { it.id }
        templates.map { template ->
            val rows = links
                .filter { it.templateId == template.id }
                .sortedBy { it.position }
                .map { link ->
                    val exercise = byId[link.exerciseId]
                    val title = if (exercise == null) "" else exerciseTitle(exercise.device, exercise.name)
                    TemplateExerciseRow(link, title)
                }
            TemplateDetail(template, rows)
        }
    }

    fun observeExercises(): Flow<List<Exercise>> = dao.observeExercises()

    fun observeActive(): Flow<Workout?> = dao.observeActive()

    fun observeHistory(): Flow<List<Workout>> = dao.observeHistory()

    fun observeFinishedSets(): Flow<List<FinishedSetRef>> = dao.observeFinishedSets().map { rows ->
        rows.map { row ->
            FinishedSetRef(
                workoutId = row.workoutId,
                finishedAt = row.finishedAt,
                exerciseId = row.exerciseId,
                exercisePosition = row.exercisePosition,
                setPosition = row.setPosition,
                set = SetRecord(row.bodyweight, row.weightKg, row.reps, row.rir, row.note),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeWorkout(id: Long): Flow<WorkoutDetail?> {
        return dao.observeWorkout(id).flatMapLatest { workout ->
            if (workout == null) {
                flowOf(null)
            } else {
                dao.observeWorkoutExercises(id).flatMapLatest { exercises ->
                    if (exercises.isEmpty()) {
                        flowOf(WorkoutDetail(workout, emptyList()))
                    } else {
                        dao.observeSets(exercises.map { it.id }).map { sets ->
                            val grouped = sets.groupBy { it.workoutExerciseId }
                            WorkoutDetail(
                                workout = workout,
                                exercises = exercises.map { entry ->
                                    WorkoutExerciseDetail(
                                        entry,
                                        grouped[entry.id].orEmpty().sortedBy { it.position },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun createTemplate(name: String): Long {
        val title = name.trim().ifEmpty { "Neue Vorlage" }
        val position = dao.maxTemplatePosition() + 1
        return dao.insertTemplate(Template(name = title, position = position))
    }

    suspend fun renameTemplate(id: Long, name: String) {
        val template = dao.template(id) ?: return
        val title = name.trim()
        if (title.isEmpty() || title == template.name) return
        dao.updateTemplate(template.copy(name = title))
    }

    suspend fun deleteTemplate(id: Long) {
        db.withTransaction {
            dao.deleteTemplateExercises(id)
            dao.deleteTemplate(id)
        }
    }

    suspend fun addExerciseToTemplate(templateId: Long, exerciseId: Long) {
        val existing = dao.templateExercises(templateId)
        if (existing.any { it.exerciseId == exerciseId }) return
        val position = (existing.maxOfOrNull { it.position } ?: -1) + 1
        dao.insertTemplateExercise(
            TemplateExercise(templateId = templateId, exerciseId = exerciseId, position = position),
        )
    }

    suspend fun removeTemplateExercise(rowId: Long) {
        dao.deleteTemplateExercise(rowId)
    }

    suspend fun moveTemplateExercise(rowId: Long, up: Boolean) {
        val row = dao.templateExercise(rowId) ?: return
        db.withTransaction {
            val all = dao.templateExercises(row.templateId).sortedBy { it.position }.toMutableList()
            val index = all.indexOfFirst { it.id == rowId }
            val target = index + if (up) -1 else 1
            if (index < 0 || target !in all.indices) return@withTransaction
            val current = all[index]
            all[index] = all[target]
            all[target] = current
            all.forEachIndexed { position, item ->
                if (item.position != position) dao.updateTemplateExercise(item.copy(position = position))
            }
        }
    }

    suspend fun createExercise(device: String, name: String): Long {
        val trimmedDevice = device.trim()
        val trimmedName = name.trim()
        require(trimmedDevice.isNotEmpty() && trimmedName.isNotEmpty())
        dao.findExercise(trimmedDevice, trimmedName)?.let { return it.id }
        return dao.insertExercise(Exercise(device = trimmedDevice, name = trimmedName))
    }

    suspend fun startWorkout(templateId: Long): Long = db.withTransaction {
        dao.activeWorkout()?.let { return@withTransaction it.id }
        val template = dao.template(templateId) ?: return@withTransaction -1L
        val links = dao.templateExercises(templateId)
        val workoutId = dao.insertWorkout(
            Workout(title = template.name, startedAt = System.currentTimeMillis()),
        )
        for (link in links) {
            val exercise = dao.exercise(link.exerciseId) ?: continue
            dao.insertWorkoutExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    name = exerciseTitle(exercise.device, exercise.name),
                    position = link.position,
                ),
            )
        }
        workoutId
    }

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long) {
        val existing = dao.workoutExercises(workoutId)
        if (existing.any { it.exerciseId == exerciseId }) return
        val exercise = dao.exercise(exerciseId) ?: return
        val position = (existing.maxOfOrNull { it.position } ?: -1) + 1
        dao.insertWorkoutExercise(
            WorkoutExercise(
                workoutId = workoutId,
                exerciseId = exercise.id,
                name = exerciseTitle(exercise.device, exercise.name),
                position = position,
            ),
        )
    }

    suspend fun removeWorkoutExercise(workoutExerciseId: Long) {
        db.withTransaction {
            dao.deleteSetsForExercise(workoutExerciseId)
            dao.deleteWorkoutExercise(workoutExerciseId)
        }
    }

    suspend fun addSet(workoutExerciseId: Long) {
        val previous = dao.lastSet(workoutExerciseId)
        val position = dao.maxSetPosition(workoutExerciseId) + 1
        dao.insertSet(
            SetEntry(
                workoutExerciseId = workoutExerciseId,
                position = position,
                bodyweight = previous?.bodyweight ?: false,
                weightKg = previous?.weightKg ?: 0.0,
                reps = previous?.reps ?: 0,
                rir = previous?.rir ?: 2,
                note = "",
            ),
        )
    }

    suspend fun updateSet(set: SetEntry) {
        dao.updateSet(
            set.copy(
                weightKg = set.weightKg.coerceAtLeast(0.0),
                reps = set.reps.coerceAtLeast(0),
                rir = set.rir.coerceIn(0, 5),
            ),
        )
    }

    suspend fun deleteSet(id: Long) {
        dao.deleteSet(id)
    }

    suspend fun finishWorkout(id: Long) {
        val workout = dao.workout(id) ?: return
        if (workout.finishedAt != null) return
        dao.updateWorkout(workout.copy(finishedAt = System.currentTimeMillis()))
    }

    suspend fun deleteWorkout(id: Long) {
        db.withTransaction {
            val exercises = dao.workoutExercises(id)
            for (exercise in exercises) dao.deleteSetsForExercise(exercise.id)
            dao.deleteWorkoutExercises(id)
            dao.deleteWorkout(id)
        }
    }
}

fun SetEntry.toRecord() = SetRecord(bodyweight, weightKg, reps, rir, note)

private object Seed {
    val catalog = listOf(
        "Egym" to "Bench Press",
        "Egym" to "Shoulder Press",
        "Egym" to "Lat",
        "Egym" to "Rudern",
        "Egym" to "Bauch seitlich",
        "Egym" to "Bauch",
        "Kurzhantel" to "Hammer Curls",
        "Kurzhantel" to "Seitheben",
        "Kurzhantel" to "Einarmiges Rudern",
        "Körpergewicht" to "Klimmzüge Untergriff",
        "Körpergewicht" to "Dips",
        "Maschine" to "Beinbeugen sitzend",
        "Maschine" to "Beinstrecken",
        "Maschine" to "Hackenschmidt (gym 90)",
        "Körpergewicht" to "Hyperextensions",
        "Cardio" to "Crosstrainer",
        "Cardio" to "Indoor-Rad",
    )

    val upper = listOf(
        "Egym" to "Bench Press",
        "Egym" to "Shoulder Press",
        "Egym" to "Lat",
        "Egym" to "Rudern",
        "Egym" to "Bauch seitlich",
        "Kurzhantel" to "Hammer Curls",
        "Kurzhantel" to "Seitheben",
        "Kurzhantel" to "Einarmiges Rudern",
        "Körpergewicht" to "Klimmzüge Untergriff",
        "Körpergewicht" to "Dips",
    )

    val lower = listOf(
        "Maschine" to "Beinbeugen sitzend",
        "Maschine" to "Beinstrecken",
        "Maschine" to "Hackenschmidt (gym 90)",
        "Körpergewicht" to "Hyperextensions",
        "Egym" to "Bauch",
    )
}
