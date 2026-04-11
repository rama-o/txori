package com.rama.adiskide

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "tasks.db", null, 15) {

    override fun onCreate(db: SQLiteDatabase) {

        // TASKS
        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                label TEXT,
                duration INTEGER,
                completion_count INTEGER DEFAULT 0
            )
            """.trimIndent()
        )

        // WORKOUTS
        db.execSQL(
            """
            CREATE TABLE workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT
            )
            """.trimIndent()
        )

        // STEPS / ORDER
        db.execSQL(
            """
            CREATE TABLE workout_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workout_id INTEGER,
                task_id INTEGER,
                step_order INTEGER,
                FOREIGN KEY(workout_id) REFERENCES workouts(id),
                FOREIGN KEY(task_id) REFERENCES tasks(id)
            )
            """.trimIndent()
        )

        insertInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS workout_steps")
        db.execSQL("DROP TABLE IF EXISTS workouts")
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }

    // PUBLIC QUERY HELPERS

    /** Returns all tasks ordered by label. */
    fun getAllTasks(db: SQLiteDatabase): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        val cursor = db.rawQuery(
            "SELECT id, label, duration, completion_count FROM tasks ORDER BY label", null
        )
        while (cursor.moveToNext()) {
            tasks.add(
                Task(
                    id = cursor.getLong(0),
                    label = cursor.getString(1),
                    duration = cursor.getInt(2),
                    completion_count = cursor.getInt(3)
                )
            )
        }
        cursor.close()
        return tasks
    }

    /** Returns the ordered steps (as Task objects) for a given workout id. */
    fun getWorkoutTasks(db: SQLiteDatabase, workoutId: Long): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        val cursor = db.rawQuery(
            """
            SELECT t.id, t.label, t.duration, t.completion_count
            FROM workout_steps ws
            JOIN tasks t ON ws.task_id = t.id
            WHERE ws.workout_id = ?
            ORDER BY ws.step_order
            """.trimIndent(),
            arrayOf(workoutId.toString())
        )
        while (cursor.moveToNext()) {
            tasks.add(
                Task(
                    id = cursor.getLong(0),
                    label = cursor.getString(1),
                    duration = cursor.getInt(2),
                    completion_count = cursor.getInt(3)
                )
            )
        }
        cursor.close()
        return tasks
    }

    /** Returns all workouts as a list of Pair(id, name). */
    fun getWorkouts(db: SQLiteDatabase): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        val cursor = db.rawQuery("SELECT id, name FROM workouts ORDER BY id", null)
        while (cursor.moveToNext()) {
            result.add(Pair(cursor.getLong(0), cursor.getString(1)))
        }
        cursor.close()
        return result
    }

    // PRIVATE HELPERS

    private fun getOrCreateTaskId(db: SQLiteDatabase, label: String, duration: Int): Long {
        val cursor = db.rawQuery(
            "SELECT id FROM tasks WHERE label = ? AND duration = ?",
            arrayOf(label, duration.toString())
        )
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(0)
            cursor.close()
            return id
        }
        cursor.close()
        val values = ContentValues().apply {
            put("label", label)
            put("duration", duration)
        }
        return db.insert("tasks", null, values)
    }

    private fun insertWorkout(db: SQLiteDatabase, name: String): Long {
        val values = ContentValues().apply { put("name", name) }
        return db.insert("workouts", null, values)
    }

    private fun insertWorkoutStep(
        db: SQLiteDatabase,
        workoutId: Long,
        taskId: Long,
        order: Int
    ) {
        val values = ContentValues().apply {
            put("workout_id", workoutId)
            put("task_id", taskId)
            put("step_order", order)
        }
        db.insert("workout_steps", null, values)
    }

    /** Returns all steps across all workouts, in workout order. */
    fun getAllWorkoutTasks(db: SQLiteDatabase): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        val cursor = db.rawQuery(
            """
        SELECT t.id, t.label, t.duration, t.completion_count
        FROM workout_steps ws
        JOIN tasks t ON ws.task_id = t.id
        ORDER BY ws.workout_id, ws.step_order
        """.trimIndent(),
            null
        )
        while (cursor.moveToNext()) {
            tasks.add(
                Task(
                    id = cursor.getLong(0),
                    label = cursor.getString(1),
                    duration = cursor.getInt(2),
                    completion_count = cursor.getInt(3)
                )
            )
        }
        cursor.close()
        return tasks
    }

    private fun insertInitialData(db: SQLiteDatabase) {

        // ── Workout 1: Upper Body Flow ──────────────────────────
        val w1 = insertWorkout(db, "Upper Body Flow")
        var n = 1
        fun s1(label: String, duration: Int) {
            insertWorkoutStep(db, w1, getOrCreateTaskId(db, label, duration), n++)
        }

        s1("Break", 10)
        s1("Chest Opener", 90)
        s1("Break", 60)
        s1("Dead Hang", 60)
        s1("Break", 60)
        repeat(2) {
            s1("Pull-Up x8", 60)
            s1("Break", 60)
            s1("Push-Up x40", 60)
            s1("Break", 60)
        }
        s1("Pull-Up x8", 60)
        s1("Break", 60)
        s1("Push-Up x40", 60)
        s1("Break", 120)  // final break is 2:00

        // ── Workout 2: Strength Block ───────────────────────────
        val w2 = insertWorkout(db, "Strength Block")
        var n2 = 1
        fun s2(label: String, duration: Int) {
            insertWorkoutStep(db, w2, getOrCreateTaskId(db, label, duration), n2++)
        }

        repeat(2) {
            s2("Chin-Up x8", 60)
            s2("Break", 60)
        }
        s2("Chin-Up x8", 60)
        s2("Break", 120)  // 2:00 after last set
        s2("Hip Thrust x60", 60)
        s2("Break", 60)
        s2("Hip Thrust x60", 60)
        s2("Break", 60)

        // ── Workout 3: Mobility & Core ──────────────────────────
        val w3 = insertWorkout(db, "Mobility & Core")
        var n3 = 1
        fun s3(label: String, duration: Int) {
            insertWorkoutStep(db, w3, getOrCreateTaskId(db, label, duration), n3++)
        }

        s3("Wall Sit", 60)
        s3("Break", 60)
        s3("Split Stretch", 60)
        s3("Break", 60)
        s3("Crunches x15", 60)
        s3("Break", 60)
        s3("Crunches x15", 60)
        s3("Break", 60)
        s3("Flutter Kicks", 30)
        s3("Break", 60)
        s3("Plank", 60)
        s3("Break", 60)
        s3("Dead Hang", 60)
    }
}
