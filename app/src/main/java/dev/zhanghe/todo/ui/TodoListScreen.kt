package dev.zhanghe.todo.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zhanghe.todo.R
import dev.zhanghe.todo.data.TodoItem
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.zhanghe.todo.ui.theme.NeonGreen
import dev.zhanghe.todo.ui.theme.SurfaceGreen
import dev.zhanghe.todo.ui.theme.DeepDarkGreen
import dev.zhanghe.todo.ui.theme.RedDelete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel = viewModel()
) {
    // Simplified navigation state: if true, show Settings Screen
    var showSettings by remember { mutableStateOf(false) }
    
    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    val todoItems by viewModel.todoItems.collectAsState()
    var newTodoText by remember { mutableStateOf("") }
    // State to show the voice generation dialog
    var voiceResultTodos by remember { mutableStateOf<List<String>?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            if (spokenText.isNotBlank()) {
                // Instead of adding directly, analyze and show dialog
                val analyzed = viewModel.analyzeVoiceInput(spokenText)
                voiceResultTodos = analyzed
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
            }
            speechRecognizerLauncher.launch(intent)
        }
    }

    if (voiceResultTodos != null) {
        VoiceResultDialog(
            initialTasks = voiceResultTodos!!,
            onDismiss = { voiceResultTodos = null },
            onConfirm = { finalTasks ->
                viewModel.addTodos(finalTasks)
                voiceResultTodos = null
            }
        )
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepDarkGreen, // Match background
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = SurfaceGreen
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings), color = Color.White) },
                            onClick = {
                                showMenu = false
                                showSettings = true
                            }
                        )
                    }
                }
            )
        },
        containerColor = DeepDarkGreen,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newTodoText,
                    onValueChange = { newTodoText = it },
                    placeholder = { Text(stringResource(R.string.add_todo_hint), color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50)) // Pill shape
                        .border(1.dp, SurfaceGreen, RoundedCornerShape(50)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceGreen,
                        unfocusedContainerColor = SurfaceGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newTodoText.isNotBlank()) {
                            viewModel.addTodoItem(newTodoText)
                            newTodoText = ""
                            keyboardController?.hide()
                        }
                    }),
                    singleLine = true
                )
                
                // Voice Button
                FloatingActionButton(
                    onClick = {
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.voice_not_supported), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {
                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(56.dp),
                    containerColor = NeonGreen,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
                        contentDescription = "Add by voice",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (todoItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DeepDarkGreen),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Mic,
                        contentDescription = "Mic",
                        tint = SurfaceGreen,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_state_hint),
                        color = Color.Gray,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DeepDarkGreen)
            ) {
                items(todoItems, key = { it.id }) { item ->
                    TodoListItem(
                        item = item,
                        onToggleCompletion = { viewModel.toggleCompletion(item) },
                        onDeleteItem = { viewModel.removeTodoItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoListItem(item: TodoItem, onToggleCompletion: () -> Unit, onDeleteItem: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp), // Increased vertical padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom Checkbox
        IconButton(onClick = onToggleCompletion) {
            if (item.isCompleted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Text(
            text = item.task,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = if (item.isCompleted) Color.Gray else Color.White,
            fontSize = 16.sp
        )
        // Removed Delete Icon from main list as per design
    }
}

@Composable
fun VoiceResultDialog(
    initialTasks: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // Local state to manage the list within the dialog before confirming
    var tasks by remember { mutableStateOf(initialTasks) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DeepDarkGreen, // Using app background color for the dialog as well, or SurfaceGreen? Design shows dark.
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.voice_dialog_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Empty box to balance the cross icon if needed, or just spacers.
                     Box(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceGreen),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { tasks = tasks - task }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete, // Use outlined delete for style
                                        contentDescription = "Remove",
                                        tint = Color.Gray // Or maybe a softer delete color
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onConfirm(tasks) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_all),
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
