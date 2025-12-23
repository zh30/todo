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
import androidx.compose.runtime.DisposableEffect
import android.os.Bundle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
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
import dev.zhanghe.todo.domain.VoiceCommand
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        SettingsScreen(
            onBack = { showSettings = false },
            viewModel = viewModel
        )
        return
    }

    val todoItems by viewModel.todoItems.collectAsState()
    var newTodoText by remember { mutableStateOf("") }
    // State to show the voice generation dialog
    var voiceResultCommands by remember { mutableStateOf<List<VoiceCommand>?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) } // New loading state
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Voice Recognition State
    var isRecording by remember { mutableStateOf(false) }
    val speechRecognizer = remember { android.speech.SpeechRecognizer.createSpeechRecognizer(context) }
    
    DisposableEffect(Unit) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isRecording = false
            }
            override fun onError(error: Int) {
                isRecording = false
                val message = when(error) {
                    android.speech.SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.voice_error_no_match)
                    android.speech.SpeechRecognizer.ERROR_NETWORK -> context.getString(R.string.voice_error_network)
                    else -> context.getString(R.string.voice_error_generic, error)
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    isAnalyzing = true // Start loading
                    viewModel.analyzeVoiceInput(spokenText) { analyzed ->
                        isAnalyzing = false // Stop loading
                        voiceResultCommands = analyzed
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, user can try pressing again
        } else {
             android.widget.Toast.makeText(context, context.getString(R.string.permission_mic_needed), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (voiceResultCommands != null) {
        VoiceResultDialog(
            initialCommands = voiceResultCommands!!,
            onDismiss = { voiceResultCommands = null },
            onConfirm = { finalCommands ->
                viewModel.executeVoiceCommands(finalCommands)
                voiceResultCommands = null
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
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.app_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
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
                            contentDescription = stringResource(R.string.cd_more_options)
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
                
                // Voice Button (Press and Hold)
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .clip(CircleShape)
                        .background(if (isRecording) Color.Red else NeonGreen) // Visual feedback
                        .pointerInput(Unit) {
                             detectTapGestures(
                                 onPress = {
                                     if (isAnalyzing) return@detectTapGestures // Prevent while analyzing
                                     val isAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
                                     if (!isAvailable) {
                                         android.widget.Toast.makeText(context, context.getString(R.string.voice_not_supported), android.widget.Toast.LENGTH_SHORT).show()
                                         return@detectTapGestures
                                     }

                                     if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                         try {
                                             isRecording = true
                                             val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                 putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                 putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                                 putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                                                 putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                             }
                                             speechRecognizer.startListening(intent)
                                             
                                             tryAwaitRelease()
                                             
                                             speechRecognizer.stopListening()
                                             isRecording = false
                                         } catch (e: Exception) {
                                             isRecording = false
                                             e.printStackTrace()
                                             android.widget.Toast.makeText(context, context.getString(R.string.voice_error_init, e.message), android.widget.Toast.LENGTH_SHORT).show()
                                         }
                                     } else {
                                         requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                     }
                                 }
                             )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
                        contentDescription = stringResource(R.string.cd_add_by_voice),
                        modifier = Modifier.size(28.dp),
                        tint = Color.Black
                    )
                    
                    if (isAnalyzing) {
                         androidx.compose.material3.CircularProgressIndicator(
                             modifier = Modifier.size(56.dp),
                             color = Color.White,
                             strokeWidth = 2.dp
                         )
                    }
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
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_mic),
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
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, Color.Gray, CircleShape)
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
    initialCommands: List<VoiceCommand>,
    onDismiss: () -> Unit,
    onConfirm: (List<VoiceCommand>) -> Unit
) {
    // Local state to manage the list within the dialog before confirming
    var commands by remember { mutableStateOf(initialCommands) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DeepDarkGreen,
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
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.voice_dialog_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commands) { command ->
                        val (icon, tint, label, task) = when (command) {
                            is VoiceCommand.AddTodo -> quadruple(Icons.Default.Add, NeonGreen, "Add", command.task)
                            is VoiceCommand.RemoveTodo -> quadruple(Icons.Default.Delete, RedDelete, "Remove", command.task)
                            is VoiceCommand.CompleteTodo -> quadruple(Icons.Default.Check, NeonGreen, "Done", command.task)
                        }

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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(task, color = Color.White)
                                    }
                                }
                                IconButton(onClick = { commands = commands - command }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_remove),
                                        tint = Color.Gray
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
                        onClick = { onConfirm(commands) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
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

// Simple helper for multiple return values in the dialog
data class CommandRowData(val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: Color, val label: String, val task: String)
fun quadruple(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, task: String) = CommandRowData(icon, tint, label, task)
