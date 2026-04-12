package com.rama.txori

sealed class SessionItem {
    data class Header(val sessionId: Long, val name: String, val tasks: List<Task>) : SessionItem()
    data class Row(val sessionId: Long, val task: Task) : SessionItem()
}