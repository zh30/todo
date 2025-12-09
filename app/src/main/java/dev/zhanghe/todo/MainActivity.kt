package dev.zhanghe.todo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.zhanghe.todo.ui.TodoListScreen
import dev.zhanghe.todo.ui.theme.TodoTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoTheme {
                TodoListScreen()
            }
        }
    }
}
