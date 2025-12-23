package dev.zhanghe.todo.domain

sealed class VoiceCommand {
    data class AddTodo(val task: String) : VoiceCommand()
    data class RemoveTodo(val task: String) : VoiceCommand()
    data class CompleteTodo(val task: String) : VoiceCommand()
}
