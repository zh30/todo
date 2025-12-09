package dev.zhanghe.todo.ui

import androidx.lifecycle.ViewModel
import dev.zhanghe.todo.data.TodoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TodoViewModel : ViewModel() {

    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems

    private var lastId = 0L

    init {
        // Add some sample data
        _todoItems.value = listOf(
        )
    }

    fun addTodoItem(task: String) {
        val newItem = TodoItem(id = newId(), task = task)
        _todoItems.value = _todoItems.value + newItem
    }

    fun addTodos(tasks: List<String>) {
        val newItems = tasks.map { TodoItem(id = newId(), task = it) }
        _todoItems.value = _todoItems.value + newItems
    }

    fun analyzeVoiceInput(text: String): List<String> {
        // Placeholder for local AI model analysis
        // For now, we'll split by spaces if it contains "和" (and), or just return the text as a single item
        // This simulates the behavior of breaking down a complex sentence into tasks
        return if (text.contains("和") || text.contains("，") || text.contains(",")) {
            text.split("和", "，", ",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(text.trim())
        }
    }

    fun toggleCompletion(item: TodoItem) {
        _todoItems.value = _todoItems.value.map {
            if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
        }
    }

    fun removeTodoItem(item: TodoItem) {
        _todoItems.value = _todoItems.value.filter { it.id != item.id }
    }

    private fun newId(): Long {
        return lastId++
    }
}
