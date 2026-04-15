package com.rama.txori

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "tasks.db", null, 23) {

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

        // SESSIONS (was groups)
        db.execSQL(
            """
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT
            )
            """.trimIndent()
        )

        // SESSION STEPS (was group_steps)
        db.execSQL(
            """
            CREATE TABLE session_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER,
                task_id INTEGER,
                step_order INTEGER,
                FOREIGN KEY(session_id) REFERENCES sessions(id),
                FOREIGN KEY(task_id) REFERENCES tasks(id)
            )
            """.trimIndent()
        )

        insertInitialData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS session_steps")
        db.execSQL("DROP TABLE IF EXISTS sessions")
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }

    // PUBLIC QUERY HELPERS

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

    /** Get tasks for a session */
    fun getSessionTasks(db: SQLiteDatabase, sessionId: Long): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        val cursor = db.rawQuery(
            """
            SELECT t.id, t.label, t.duration, t.completion_count
            FROM session_steps ss
            JOIN tasks t ON ss.task_id = t.id
            WHERE ss.session_id = ?
            ORDER BY ss.step_order
            """.trimIndent(),
            arrayOf(sessionId.toString())
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

    fun getSessions(db: SQLiteDatabase): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        val cursor = db.rawQuery("SELECT id, name FROM sessions ORDER BY id", null)
        while (cursor.moveToNext()) {
            result.add(Pair(cursor.getLong(0), cursor.getString(1)))
        }
        cursor.close()
        return result
    }

    // PUBLIC WRITE HELPERS

    fun createSession(db: SQLiteDatabase, name: String): Long {
        val values = ContentValues().apply { put("name", name) }
        return db.insert("sessions", null, values)
    }

    fun deleteSession(db: SQLiteDatabase, sessionId: Long) {
        db.delete("session_steps", "session_id = ?", arrayOf(sessionId.toString()))
        db.delete("sessions", "id = ?", arrayOf(sessionId.toString()))
    }

    fun addTaskToSession(db: SQLiteDatabase, sessionId: Long, label: String, duration: Int): Long {
        val taskId = getOrCreateTaskId(db, label, duration)
        val cursor = db.rawQuery(
            "SELECT COALESCE(MAX(step_order), 0) FROM session_steps WHERE session_id = ?",
            arrayOf(sessionId.toString())
        )
        val maxOrder = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        val values = ContentValues().apply {
            put("session_id", sessionId)
            put("task_id", taskId)
            put("step_order", maxOrder + 1)
        }
        db.insert("session_steps", null, values)
        return taskId
    }

    fun removeTaskFromSession(db: SQLiteDatabase, sessionId: Long, taskId: Long) {
        db.delete(
            "session_steps",
            "session_id = ? AND task_id = ?",
            arrayOf(sessionId.toString(), taskId.toString())
        )
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

    private fun insertSession(db: SQLiteDatabase, name: String): Long {
        val values = ContentValues().apply { put("name", name) }
        return db.insert("sessions", null, values)
    }

    private fun insertSessionStep(
        db: SQLiteDatabase,
        sessionId: Long,
        taskId: Long,
        order: Int
    ) {
        val values = ContentValues().apply {
            put("session_id", sessionId)
            put("task_id", taskId)
            put("step_order", order)
        }
        db.insert("session_steps", null, values)
    }

    fun getAllSessionTasks(db: SQLiteDatabase): MutableList<Task> {
        val tasks = mutableListOf<Task>()
        val cursor = db.rawQuery(
            """
            SELECT t.id, t.label, t.duration, t.completion_count
            FROM session_steps ss
            JOIN tasks t ON ss.task_id = t.id
            ORDER BY ss.session_id, ss.step_order
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

        var n = 1
        val s0Id = insertSession(db, "Morning Reset")
        fun s0(label: String, duration: Int) {
            insertSessionStep(db, s0Id, getOrCreateTaskId(db, label, duration), n++)
        }

        s0("Drink Water", 60 * 5)
        s0("Brush Teeth", 60 * 5)
        s0("Shower", 60 * 10)
        s0("Tidy Room", 60 * 5)
        s0("Wash Dishes", 60 * 5)

        val s1Id = insertSession(db, "Workout")
        fun s1(label: String, duration: Int) {
            insertSessionStep(db, s1Id, getOrCreateTaskId(db, label, duration), n++)
        }

        fun addRest(unit: Int = 60) {
            s1("Rest", unit)
        }

        s1("Getting Ready", 15)
        s1("Chest Opener", 90)
        addRest()
        s1("Dead Hang", 40)
        addRest()

        s1("Pull-Up x8", 30)
        addRest(90)
        s1("Push-Up x40", 60)
        addRest()

        s1("Pull-Up x8", 30)
        addRest(90)
        s1("Push-Up x30", 60)
        addRest()

        s1("Pull-Up x8", 30)
        addRest(90)
        s1("Push-Up x30", 60)
        addRest()

        repeat(3) {
            s1("Chin-Up x8", 30)
            addRest(90)
        }

        s1("Hip Thrust x30", 60)
        addRest()
        s1("Hip Thrust x30", 60)
        addRest()

        s1("Wall Sit", 60)
        addRest()

        repeat(2) {
            s1("Crunches x12", 60)
            addRest()
        }

        repeat(2) {
            s1("Flutter Kicks", 30)
            addRest()
        }

        s1("Plank", 60)
        addRest()
        s1("Dead Hang", 60)
        addRest(15)
        s1("Deep Squat", 60)
        addRest(15)
        s1("Split Stretch", 60)
    }
}