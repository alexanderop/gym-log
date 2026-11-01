package dev.alexop.gymlog.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SetRecord(
    val bodyweight: Boolean,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val note: String = "",
)

data class ExerciseRecord(
    val name: String,
    val sets: List<SetRecord>,
)

data class WorkoutRecord(
    val title: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val exercises: List<ExerciseRecord>,
)

data class PastExercise(
    val exerciseId: Long,
    val sets: List<SetRecord>,
)

data class PastWorkout(
    val finishedAt: Long?,
    val exercises: List<PastExercise>,
)

data class FinishedSetRef(
    val workoutId: Long,
    val finishedAt: Long,
    val exerciseId: Long,
    val exercisePosition: Int,
    val setPosition: Int,
    val set: SetRecord,
)

data class WorkoutClock(
    val date: String,
    val time: String,
    val minutes: Int,
)

fun exerciseTitle(device: String, name: String): String = "${device.trim()} – ${name.trim()}"

fun formatKg(value: Double): String {
    val text = BigDecimal.valueOf(value)
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return if (text == "-0") "0" else text
}

fun durationMinutes(startedAtEpochMs: Long, finishedAtEpochMs: Long): Int {
    val ms = (finishedAtEpochMs - startedAtEpochMs).coerceAtLeast(0L)
    return ((ms + 30_000L) / 60_000L).toInt()
}

fun formatElapsed(elapsedMs: Long): String {
    val total = elapsedMs.coerceAtLeast(0L) / 1000L
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
    } else {
        "%d:%02d".format(Locale.US, minutes, seconds)
    }
}

fun workoutClock(startedAtEpochMs: Long, finishedAtEpochMs: Long, zone: ZoneId): WorkoutClock {
    val start = Instant.ofEpochMilli(startedAtEpochMs).atZone(zone)
    return WorkoutClock(
        date = start.format(DateTimeFormatter.ISO_LOCAL_DATE),
        time = start.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)),
        minutes = durationMinutes(startedAtEpochMs, finishedAtEpochMs),
    )
}

fun formatSetMarkdown(set: SetRecord): String {
    val base = "${loadText(set)} x ${set.reps}, RIR ${set.rir}"
    val note = set.note.trim().replace(Regex("\\s+"), " ")
    return if (note.isEmpty()) base else "$base, Notiz: $note"
}

fun formatLastTimeLine(set: SetRecord): String = "${loadText(set)} × ${set.reps} · RIR ${set.rir}"

fun formatWorkoutMarkdown(workout: WorkoutRecord, zone: ZoneId): String {
    val clock = workoutClock(workout.startedAtEpochMs, workout.finishedAtEpochMs, zone)
    val lines = mutableListOf("# ${workout.title} ${clock.date} (${clock.time}, ${clock.minutes} Min)")
    for (exercise in workout.exercises) {
        if (exercise.sets.isEmpty()) continue
        lines += "## ${exercise.name}"
        for (set in exercise.sets) {
            lines += "- ${formatSetMarkdown(set)}"
        }
    }
    return lines.joinToString("\n") + "\n"
}

fun historyFromFinishedSets(rows: List<FinishedSetRef>): List<PastWorkout> {
    return rows.groupBy { it.workoutId }.values.map { group ->
        val exercises = group
            .groupBy { it.exerciseId }
            .values
            .sortedBy { it.minOf(FinishedSetRef::exercisePosition) }
            .map { exerciseRows ->
                PastExercise(
                    exerciseId = exerciseRows.first().exerciseId,
                    sets = exerciseRows.sortedBy { it.setPosition }.map { it.set },
                )
            }
        PastWorkout(finishedAt = group.first().finishedAt, exercises = exercises)
    }
}

fun lastPerformance(exerciseId: Long, history: List<PastWorkout>): List<SetRecord>? {
    return history
        .asSequence()
        .filter { it.finishedAt != null }
        .sortedByDescending { it.finishedAt }
        .mapNotNull { workout ->
            workout.exercises
                .filter { it.exerciseId == exerciseId }
                .flatMap { it.sets }
                .takeIf { it.isNotEmpty() }
        }
        .firstOrNull()
}

private fun loadText(set: SetRecord): String {
    val kg = formatKg(set.weightKg)
    return when {
        set.bodyweight && kg == "0" -> "KG"
        set.bodyweight -> "KG +$kg kg"
        else -> "$kg kg"
    }
}
