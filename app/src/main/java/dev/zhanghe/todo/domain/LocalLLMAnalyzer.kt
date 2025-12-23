package dev.zhanghe.todo.domain

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import org.json.JSONObject

class LocalLLMAnalyzer(
    private val context: Context,
    private val modelPath: String
) : VoiceCommandAnalyzer {

    private var llmInference: LlmInference? = null
    
    init {
        // Initialize in background if possible, but for now we just prepare
    }

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!File(modelPath).exists()) return@withContext false
                
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTemperature(0.1f) // Lower temperature for more consistent function calling
                    .setRandomSeed(101)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override fun isReady(): Boolean = llmInference != null

    override suspend fun analyze(text: String): List<VoiceCommand> {
        if (llmInference == null) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                // Prompt Engineering for FunctionGemma
                val prompt = """
                    <start_function_declaration>
                    {"name": "add_todo", "description": "Add a new todo item", "parameters": {"type": "object", "properties": {"task": {"type": "string", "description": "The task to add"}}, "required": ["task"]}}
                    {"name": "remove_todo", "description": "Remove a todo item", "parameters": {"type": "object", "properties": {"task": {"type": "string", "description": "The task to remove"}}, "required": ["task"]}}
                    {"name": "complete_todo", "description": "Mark a todo item as completed", "parameters": {"type": "object", "properties": {"task": {"type": "string", "description": "The task to complete"}}, "required": ["task"]}}
                    <end_function_declaration>
                    User input: "$text"
                """.trimIndent()
                
                val result = llmInference?.generateResponse(prompt) ?: ""
                
                parseFunctionCalls(result)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun parseFunctionCalls(text: String): List<VoiceCommand> {
        val commands = mutableListOf<VoiceCommand>()
        
        // Extract content between <start_function_call> and <end_function_call>
        val regex = Regex("<start_function_call>(.*?)<end_function_call>", RegexOption.DOT_MATCHES_ALL)
        val matches = regex.findAll(text)
        
        for (match in matches) {
            try {
                val jsonString = match.groupValues[1].trim()
                val json = JSONObject(jsonString)
                val functionName = json.getString("name")
                val arguments = json.getJSONObject("arguments")
                val task = arguments.getString("task")
                
                when (functionName) {
                    "add_todo" -> commands.add(VoiceCommand.AddTodo(task))
                    "remove_todo" -> commands.add(VoiceCommand.RemoveTodo(task))
                    "complete_todo" -> commands.add(VoiceCommand.CompleteTodo(task))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fallback: If no function calls detected but there's output, 
        // it might be a simple addition or the model failed tokens but gave plain text.
        if (commands.isEmpty() && text.isNotBlank() && !text.contains("<start_function_call>")) {
             // Simple fallback parsing for robustness
             text.lines()
                 .map { it.trim() }
                 .filter { it.isNotEmpty() && !it.startsWith("<") }
                 .forEach { commands.add(VoiceCommand.AddTodo(it)) }
        }
        
        return commands
    }
    
    fun close() {
        llmInference = null
    }
}
