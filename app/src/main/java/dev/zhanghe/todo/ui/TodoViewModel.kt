package dev.zhanghe.todo.ui

import androidx.lifecycle.viewModelScope
import dev.zhanghe.todo.data.TodoItem
import dev.zhanghe.todo.domain.LocalLLMAnalyzer
import dev.zhanghe.todo.domain.RuleBasedAnalyzer
import dev.zhanghe.todo.domain.VoiceCommandAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems

    private val _isAiLoaded = MutableStateFlow(false)
    val isAiLoaded: StateFlow<Boolean> = _isAiLoaded

    private var voiceAnalyzer: VoiceCommandAnalyzer = RuleBasedAnalyzer()
    private var llmAnalyzer: LocalLLMAnalyzer? = null

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

    fun loadAiModel(path: String) {
        viewModelScope.launch {
            val analyzer = LocalLLMAnalyzer(getApplication(), path)
            if (analyzer.initialize()) {
                llmAnalyzer = analyzer
                voiceAnalyzer = analyzer
                _isAiLoaded.value = true
            } else {
                _isAiLoaded.value = false
                // Fallback remains RuleBased
            }
        }
    }

    fun analyzeVoiceInput(text: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val results = voiceAnalyzer.analyze(text)
            // If results are empty (AI failed), try fallback if we were using AI
            if (results.isEmpty() && voiceAnalyzer is LocalLLMAnalyzer) {
                val fallback = RuleBasedAnalyzer().analyze(text)
                onResult(fallback)
            } else {
                onResult(results)
            }
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
