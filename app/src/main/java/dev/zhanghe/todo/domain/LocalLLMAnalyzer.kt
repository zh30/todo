package dev.zhanghe.todo.domain

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
                    .setTemperature(0.8f)
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

    override suspend fun analyze(text: String): List<String> {
        if (llmInference == null) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                // Prompt Engineering
                val prompt = """
                    You are a helpful assistant.
                    Split the following text into a list of distinct todo items.
                    Text: "$text"
                    
                    Output ONLY the items, one per line. Do not number them. Do not include bullet points.
                """.trimIndent()
                
                val result = llmInference?.generateResponse(prompt) ?: ""
                
                // Parse result (assuming one per line)
                result.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    // Filter out common hallucinations or conversational filler if necessary
                    .filter { !it.startsWith("Here are") } 
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    fun close() {
        // LlmInference doesn't explicitly have close() in some versions, 
        // but it's good practice to null it out or check docs. 
        // The current API might not need manual close, relying on GC, but we'll monitor.
        llmInference = null
    }
}
