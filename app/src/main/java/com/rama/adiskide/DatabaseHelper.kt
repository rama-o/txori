package com.rama.adiskide

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "tasks.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT,
                label TEXT,
                difficulty INTEGER,
                date_creation INTEGER,
                date_completion INTEGER
            )
        """.trimIndent()
        )

        insertInitialTasks(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tasks")
        onCreate(db)
    }

    private fun insertTask(
        db: SQLiteDatabase,
        type: TaskType,
        label: String,
        date: Long,
        difficulty: Int
    ) {

        val values = ContentValues().apply {
            put("type", type.name)
            put("label", label)
            put("difficulty", difficulty)
            put("date_creation", date)
        }

        db.insert("tasks", null, values)
    }

    private fun insertInitialTasks(db: SQLiteDatabase) {

        val now = System.currentTimeMillis()

        insertTask(db, TaskType.ROUTINE, "Drink water", now, 1)
        insertTask(db, TaskType.ROUTINE, "Clean room", now, 1)
        insertTask(db, TaskType.ROUTINE, "Brush teeth", now, 1)
        insertTask(db, TaskType.ROUTINE, "Take shower", now, 1)
        insertTask(db, TaskType.ROUTINE, "Stretch", now, 1)
        insertTask(db, TaskType.BURNER, "Run", now, 1)
        insertTask(db, TaskType.BURNER, "Run More", now, 1)

    }
}