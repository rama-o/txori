package com.rama.txori

data class Task(
    val id: Long = 0,
    var label: String,
    var duration: Int = 0,
    var completion_count: Int = 0,
)