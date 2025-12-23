package dev.zhanghe.todo.ui

import androidx.lifecycle.viewModelScope
import dev.zhanghe.todo.data.TodoItem
import dev.zhanghe.todo.domain.LocalLLMAnalyzer
import dev.zhanghe.todo.domain.RuleBasedAnalyzer
import dev.zhanghe.todo.domain.VoiceCommand
import dev.zhanghe.todo.domain.VoiceCommandAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.withContext

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val _todoItems = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoItems: StateFlow<List<TodoItem>> = _todoItems

    private val _isAiLoaded = MutableStateFlow(false)
    val isAiLoaded: StateFlow<Boolean> = _isAiLoaded

    private var voiceAnalyzer: VoiceCommandAnalyzer = RuleBasedAnalyzer()
    private var llmAnalyzer: LocalLLMAnalyzer? = null

    private var lastId = 0L

    init {
        // Check for embedded model
        checkForEmbeddedModel()
    }

    private fun checkForEmbeddedModel() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val assetFileName = "model.bin"
            val internalFile = java.io.File(context.filesDir, "embedded_model.bin")
            
            // Check if model exists in assets
            val existsInAssets = try {
                context.assets.open(assetFileName).close()
                true
            } catch (e: Exception) {
                false
            }

            if (existsInAssets) {
                if (!internalFile.exists()) {
                    // Copy to internal storage
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            context.assets.open(assetFileName).use { input ->
                                java.io.FileOutputStream(internalFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                if (internalFile.exists()) {
                    loadAiModel(internalFile.absolutePath)
                }
            }
        }
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

    fun analyzeVoiceInput(text: String, onResult: (List<VoiceCommand>) -> Unit) {
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

    fun executeVoiceCommands(commands: List<VoiceCommand>) {
        commands.forEach { command ->
            when (command) {
                is VoiceCommand.AddTodo -> addTodoItem(command.task)
                is VoiceCommand.RemoveTodo -> {
                    // Find the best match
                    val item = _todoItems.value.find { it.task.contains(command.task, ignoreCase = true) }
                    item?.let { removeTodoItem(it) }
                }
                is VoiceCommand.CompleteTodo -> {
                    // Find the best match
                    val item = _todoItems.value.find { it.task.contains(command.task, ignoreCase = true) }
                    item?.let { if (!it.isCompleted) toggleCompletion(it) }
                }
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
