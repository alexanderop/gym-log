package dev.alexop.gymlog

import dev.alexop.gymlog.domain.ExerciseRecord
import dev.alexop.gymlog.domain.SetRecord
import dev.alexop.gymlog.domain.WorkoutRecord
import dev.alexop.gymlog.domain.durationMinutes
import dev.alexop.gymlog.domain.exerciseTitle
import dev.alexop.gymlog.domain.formatElapsed
import dev.alexop.gymlog.domain.formatKg
import dev.alexop.gymlog.domain.formatLastTimeLine
import dev.alexop.gymlog.domain.formatSetMarkdown
import dev.alexop.gymlog.domain.formatWorkoutMarkdown
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MarkdownTest {
    private val berlin = ZoneId.of("Europe/Berlin")

    @Test
    fun weightDropsTrailingZero() {
        assertEquals("120", formatKg(120.0))
        assertEquals("22.5", formatKg(22.5))
        assertEquals("22.5", formatKg(22.50))
        assertEquals("10.25", formatKg(10.25))
        assertEquals("10.12", formatKg(10.124))
        assertEquals("10.13", formatKg(10.125))
        assertEquals("0", formatKg(0.0))
        assertEquals("0", formatKg(-0.0))
    }

    @Test
    fun setLines() {
        assertEquals(
            "120 kg x 8, RIR 2",
            formatSetMarkdown(SetRecord(false, 120.0, 8, 2)),
        )
        assertEquals(
            "120 kg x 7, RIR 1, Notiz: ...",
            formatSetMarkdown(SetRecord(false, 120.0, 7, 1, "  ...  ")),
        )
        assertEquals(
            "22.5 kg x 10, RIR 0",
            formatSetMarkdown(SetRecord(false, 22.5, 10, 0)),
        )
        assertEquals(
            "KG x 12, RIR 2",
            formatSetMarkdown(SetRecord(true, 0.0, 12, 2)),
        )
        assertEquals(
            "KG +10 kg x 8, RIR 1",
            formatSetMarkdown(SetRecord(true, 10.0, 8, 1)),
        )
        assertEquals(
            "KG +10.5 kg x 6, RIR 3, Notiz: band",
            formatSetMarkdown(SetRecord(true, 10.5, 6, 3, "band")),
        )
    }

    @Test
    fun lastTimeLineUsesTimesAndDot() {
        assertEquals("120 kg × 8 · RIR 2", formatLastTimeLine(SetRecord(false, 120.0, 8, 2)))
        assertEquals("KG × 12 · RIR 2", formatLastTimeLine(SetRecord(true, 0.0, 12, 2)))
        assertEquals("KG +10 kg × 8 · RIR 1", formatLastTimeLine(SetRecord(true, 10.0, 8, 1, "ignored")))
    }

    @Test
    fun workoutMarkdownMatchesSampleAndSkipsEmptyExercises() {
        val start = ZonedDateTime.of(2026, 10, 31, 12, 59, 0, 0, berlin).toInstant().toEpochMilli()
        val markdown = formatWorkoutMarkdown(
            WorkoutRecord(
                title = "Oberkörper",
                startedAtEpochMs = start,
                finishedAtEpochMs = start + 66 * 60_000,
                exercises = listOf(
                    ExerciseRecord(
                        "Egym – Bench Press",
                        listOf(
                            SetRecord(false, 120.0, 8, 2),
                            SetRecord(false, 120.0, 7, 1, "..."),
                        ),
                    ),
                    ExerciseRecord("Egym – Lat", emptyList()),
                    ExerciseRecord(
                        "Körpergewicht – Dips",
                        listOf(SetRecord(true, 0.0, 12, 2)),
                    ),
                ),
            ),
            berlin,
        )
        assertEquals(
            """
            # Oberkörper 2026-10-31 (12:59, 66 Min)
            ## Egym – Bench Press
            - 120 kg x 8, RIR 2
            - 120 kg x 7, RIR 1, Notiz: ...
            ## Körpergewicht – Dips
            - KG x 12, RIR 2
            """.trimIndent() + "\n",
            markdown,
        )
    }

    @Test
    fun durationRoundsHalfUpAndElapsedFormats() {
        assertEquals(66, durationMinutes(0, 66 * 60_000))
        assertEquals(1, durationMinutes(0, 30_000))
        assertEquals(0, durationMinutes(0, 29_999))
        assertEquals(0, durationMinutes(5_000, 1_000))
        assertEquals("0:05", formatElapsed(5_000))
        assertEquals("12:04", formatElapsed(12 * 60_000 + 4_000))
        assertEquals("1:06:02", formatElapsed(3_962_000))
    }

    @Test
    fun exerciseTitleUsesEnDash() {
        assertEquals("Egym – Bench Press", exerciseTitle(" Egym ", " Bench Press "))
        assertEquals("Maschine – Hackenschmidt (gym 90)", exerciseTitle("Maschine", "Hackenschmidt (gym 90)"))
    }
}
