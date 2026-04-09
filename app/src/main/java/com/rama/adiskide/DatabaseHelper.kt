package com.rama.adiskide

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "tasks.db", null, 10) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
        CREATE TABLE tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            label TEXT,
            duration INTEGER,
            task_order INTEGER,
            task_group INTEGER,
            completion_count INTEGER,
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
        label: String,
        duration: Int,
        task_order: Int,
        task_group: Int,
        completion_count: Int,
    ) {

        val values = ContentValues().apply {
            put("label", label)
            put("duration", duration)
            put("task_order", task_order)
            put("task_group", task_group)
            put("completion_count", completion_count)
        }

        db.insert("tasks", null, values)
    }

    private fun insertInitialTasks(db: SQLiteDatabase) {
        insertTask(db, "Drink water", 1, 1, 1, 0)
    }
}