package dev.alexop.gymlog

import dev.alexop.gymlog.domain.FinishedSetRef
import dev.alexop.gymlog.domain.PastExercise
import dev.alexop.gymlog.domain.PastWorkout
import dev.alexop.gymlog.domain.SetRecord
import dev.alexop.gymlog.domain.historyFromFinishedSets
import dev.alexop.gymlog.domain.lastPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastTimeTest {
    private fun set(kg: Double, reps: Int, rir: Int, bodyweight: Boolean = false) =
        SetRecord(bodyweight, kg, reps, rir)

    @Test
    fun picksSetsFromMostRecentFinishedWorkout() {
        val older = PastWorkout(
            finishedAt = 10,
            exercises = listOf(PastExercise(1, listOf(set(100.0, 5, 3)))),
        )
        val newer = PastWorkout(
            finishedAt = 20,
            exercises = listOf(
                PastExercise(1, listOf(set(120.0, 8, 2), set(120.0, 7, 1))),
                PastExercise(2, listOf(set(0.0, 12, 2, bodyweight = true))),
            ),
        )
        val running = PastWorkout(
            finishedAt = null,
            exercises = listOf(PastExercise(1, listOf(set(200.0, 1, 0)))),
        )
        val history = listOf(older, running, newer)

        assertEquals(listOf(set(120.0, 8, 2), set(120.0, 7, 1)), lastPerformance(1, history))
        assertEquals(listOf(set(0.0, 12, 2, bodyweight = true)), lastPerformance(2, history))
        assertNull(lastPerformance(3, history))
    }

    @Test
    fun skipsFinishedWorkoutThatHasNoSetsForTheExercise() {
        val withSets = PastWorkout(
            finishedAt = 10,
            exercises = listOf(PastExercise(7, listOf(set(80.0, 10, 2)))),
        )
        val emptyLater = PastWorkout(
            finishedAt = 30,
            exercises = listOf(PastExercise(7, emptyList())),
        )
        assertEquals(listOf(set(80.0, 10, 2)), lastPerformance(7, listOf(emptyLater, withSets)))
    }

    @Test
    fun ignoresANewerUnfinishedWorkout() {
        val finished = PastWorkout(finishedAt = 5, exercises = listOf(PastExercise(4, listOf(set(40.0, 12, 1)))))
        val running = PastWorkout(finishedAt = null, exercises = listOf(PastExercise(4, listOf(set(90.0, 3, 0)))))
        assertEquals(listOf(set(40.0, 12, 1)), lastPerformance(4, listOf(running, finished)))
    }

    @Test
    fun historyRowsGroupByWorkoutAndKeepSetOrder() {
        val rows = listOf(
            FinishedSetRef(2, 50, 9, 1, 1, set(60.0, 8, 2)),
            FinishedSetRef(2, 50, 9, 1, 0, set(60.0, 10, 3)),
            FinishedSetRef(2, 50, 3, 0, 0, set(100.0, 5, 1)),
            FinishedSetRef(1, 10, 9, 0, 0, set(50.0, 12, 2)),
        )
        val history = historyFromFinishedSets(rows)
        val latest = lastPerformance(9, history)
        assertEquals(listOf(set(60.0, 10, 3), set(60.0, 8, 2)), latest)
        val bench = history.first { it.finishedAt == 50L }.exercises.first { it.exerciseId == 3L }
        assertEquals(listOf(set(100.0, 5, 1)), bench.sets)
    }
}
