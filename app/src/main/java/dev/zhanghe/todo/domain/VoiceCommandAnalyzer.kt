package dev.zhanghe.todo.domain

interface VoiceCommandAnalyzer {
    suspend fun analyze(text: String): List<VoiceCommand>
    
    // Optional: Check if this analyzer is ready (e.g., model loaded)
    fun isReady(): Boolean = true
}
