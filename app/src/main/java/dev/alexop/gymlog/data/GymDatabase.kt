package dev.alexop.gymlog.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["device", "name"], unique = true)],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val device: String,
    val name: String,
)

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val bodyweight: Boolean,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val note: String,
)

data class FinishedSetRow(
    val workoutId: Long,
    val finishedAt: Long,
    val exerciseId: Long,
    val exercisePosition: Int,
    val setPosition: Int,
    val bodyweight: Boolean,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val note: String,
)

@Dao
interface GymDao {
    @Query("SELECT COUNT(*) FROM templates")
    suspend fun templateCount(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM templates")
    suspend fun maxTemplatePosition(): Int

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Insert
    suspend fun insertTemplateExercise(row: TemplateExercise): Long

    @Insert
    suspend fun insertWorkout(workout: Workout): Long

    @Insert
    suspend fun insertWorkoutExercise(row: WorkoutExercise): Long

    @Insert
    suspend fun insertSet(set: SetEntry): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Update
    suspend fun updateTemplateExercise(row: TemplateExercise)

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Update
    suspend fun updateSet(set: SetEntry)

    @Query("SELECT * FROM exercises WHERE device = :device AND name = :name LIMIT 1")
    suspend fun findExercise(device: String, name: String): Exercise?

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun exercise(id: Long): Exercise?

    @Query("SELECT * FROM exercises ORDER BY id")
    fun observeExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM templates ORDER BY position, id")
    fun observeTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun template(id: Long): Template?

    @Query("SELECT * FROM template_exercises ORDER BY position")
    fun observeTemplateExercises(): Flow<List<TemplateExercise>>

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY position")
    suspend fun templateExercises(templateId: Long): List<TemplateExercise>

    @Query("SELECT * FROM template_exercises WHERE id = :id")
    suspend fun templateExercise(id: Long): TemplateExercise?

    @Query("DELETE FROM template_exercises WHERE id = :id")
    suspend fun deleteTemplateExercise(id: Long)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: Long)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeWorkout(): Workout?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: Long): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workout(id: Long): Workout?

    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun observeHistory(): Flow<List<Workout>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    fun observeWorkoutExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun workoutExercises(workoutId: Long): List<WorkoutExercise>

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteWorkoutExercises(workoutId: Long)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("SELECT * FROM sets WHERE workoutExerciseId IN (:ids) ORDER BY position")
    fun observeSets(ids: List<Long>): Flow<List<SetEntry>>

    @Query("SELECT * FROM sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY position DESC LIMIT 1")
    suspend fun lastSet(workoutExerciseId: Long): SetEntry?

    @Query("SELECT COALESCE(MAX(position), -1) FROM sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun maxSetPosition(workoutExerciseId: Long): Int

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteSetsForExercise(workoutExerciseId: Long)

    @Query(
        """
        SELECT w.id AS workoutId,
               w.finishedAt AS finishedAt,
               we.exerciseId AS exerciseId,
               we.position AS exercisePosition,
               s.position AS setPosition,
               s.bodyweight AS bodyweight,
               s.weightKg AS weightKg,
               s.reps AS reps,
               s.rir AS rir,
               s.note AS note
        FROM sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE w.finishedAt IS NOT NULL
        ORDER BY w.finishedAt DESC, we.position ASC, s.position ASC
        """,
    )
    fun observeFinishedSets(): Flow<List<FinishedSetRow>>
}

@Database(
    entities = [
        Exercise::class,
        Template::class,
        TemplateExercise::class,
        Workout::class,
        WorkoutExercise::class,
        SetEntry::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun build(context: Context): GymDatabase =
            Room.databaseBuilder(context.applicationContext, GymDatabase::class.java, "gymlog.db")
                .build()
    }
}
