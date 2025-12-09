package dev.zhanghe.todo.data

import androidx.compose.runtime.Immutable

@Immutable
data class TodoItem(
    val id: Long,
    val task: String,
    val isCompleted: Boolean = false
)
