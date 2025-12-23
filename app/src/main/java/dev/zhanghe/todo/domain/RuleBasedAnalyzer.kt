package dev.zhanghe.todo.domain

class RuleBasedAnalyzer : VoiceCommandAnalyzer {
    override suspend fun analyze(text: String): List<VoiceCommand> {
        val splitDelimiters = listOf(
            "和", "，", ",", "然后", "接着", "plus", "and", "then", ";", "；", "。"
        )
        // Normalize
        var processed = text
        
        // Simple heuristic: if it looks like a list "1. Buy milk 2. Buy eggs"
        val numberedListRegex = Regex("\\d+[\\.\\、\\s]\\s*")
        val strings = if (numberedListRegex.containsMatchIn(processed)) {
             processed.split(numberedListRegex)
                 .map { it.trim() }
                 .filter { it.isNotEmpty() }
        } else {
            // Standard split
            processSplit(processed, splitDelimiters)
        }

        return strings.map { VoiceCommand.AddTodo(it) }
    }

    private fun processSplit(text: String, delimiters: List<String>): List<String> {
        var currentPieces = listOf(text)
        for (delimiter in delimiters) {
            val newPieces = mutableListOf<String>()
            for (piece in currentPieces) {
                if (piece.contains(delimiter, ignoreCase = true)) {
                    newPieces.addAll(piece.split(delimiter, ignoreCase = true))
                } else {
                    newPieces.add(piece)
                }
            }
            currentPieces = newPieces
        }
        return currentPieces.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
