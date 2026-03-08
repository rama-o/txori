package com.rama.adiskide

data class Task(
    val id: Long = 0,
    val type: TaskType,
    val label: String,
    val difficulty: Int = 0,
    val dateCreation: Long,
    val dateCompletion: Long? = null
)