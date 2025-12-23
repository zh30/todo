package dev.zhanghe.todo.ui

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import dev.zhanghe.todo.R
import dev.zhanghe.todo.ui.theme.DeepDarkGreen
import dev.zhanghe.todo.ui.theme.NeonGreen
import dev.zhanghe.todo.ui.theme.SurfaceGreen
import androidx.compose.ui.window.Dialog
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TodoViewModel
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isAiLoaded by viewModel.isAiLoaded.collectAsState()
    var isLoadingModel by remember { mutableStateOf(false) }

    val modelPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isLoadingModel = true
            // Copy to internal storage
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val file = java.io.File(context.filesDir, "custom_llm.bin")
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        viewModel.loadAiModel(file.absolutePath)
                        isLoadingModel = false
                        android.widget.Toast.makeText(context, "AI Model loaded!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoadingModel = false
                        android.widget.Toast.makeText(context, "Failed to load model", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    // Determine current language state for display. 
    // This is a simplified check. Ideally we read from AppCompatDelegate or LocaleManager.
    val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
    val languageSubtitle = when (currentLocale?.language) {
        "zh" -> stringResource(R.string.language_zh)
        "en" -> stringResource(R.string.language_en)
        "es" -> stringResource(R.string.language_es)
        "fr" -> stringResource(R.string.language_fr)
        "hi" -> stringResource(R.string.language_hi)
        "ar" -> stringResource(R.string.language_ar)
        "bn" -> stringResource(R.string.language_bn)
        "pt" -> stringResource(R.string.language_pt)
        else -> stringResource(R.string.follow_system)
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { tag ->
                val localeList = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList() // Follow system
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
                showLanguageDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepDarkGreen
                )
            )
        },
        containerColor = DeepDarkGreen
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.language), color = Color.White) },
                supportingContent = { Text(languageSubtitle, color = Color.Gray) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            HorizontalDivider(color = SurfaceGreen)

            // AI Model Section
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_ai_model), color = Color.White) },
                supportingContent = { 
                    if (isLoadingModel) {
                        Text("Loading model...", color = NeonGreen)
                    } else {
                        Text(if (isAiLoaded) "Model Loaded (Ready)" else "No Model Loaded (Using Basic Parser)", color = Color.Gray)
                    }
                },
                trailingContent = {
                    Button(
                        onClick = { modelPickerLauncher.launch(arrayOf("*/*")) }, // Allow any file, or application/octet-stream
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceGreen)
                    ) {
                        Text("Select .bin", color = Color.White)
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(color = SurfaceGreen)
            
            // Privacy Policy Section (Required for RECORD_AUDIO permission)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy_policy), color = Color.White) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable {
                    // Open browser with privacy policy
                     val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/zh30/todoapp/blob/main/PRIVACY_POLICY.md"))
                     context.startActivity(intent)
                }
            )
            HorizontalDivider(color = SurfaceGreen)
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceGreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()) 
            ) {
                Text(
                    text = stringResource(R.string.language),
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LanguageOption(text = stringResource(R.string.follow_system), onClick = { onLanguageSelected("") })
                LanguageOption(text = stringResource(R.string.language_zh), onClick = { onLanguageSelected("zh") })
                LanguageOption(text = stringResource(R.string.language_en), onClick = { onLanguageSelected("en") })
                LanguageOption(text = stringResource(R.string.language_es), onClick = { onLanguageSelected("es") })
                LanguageOption(text = stringResource(R.string.language_fr), onClick = { onLanguageSelected("fr") })
                LanguageOption(text = stringResource(R.string.language_pt), onClick = { onLanguageSelected("pt") })
                LanguageOption(text = stringResource(R.string.language_hi), onClick = { onLanguageSelected("hi") })
                LanguageOption(text = stringResource(R.string.language_ar), onClick = { onLanguageSelected("ar") })
                LanguageOption(text = stringResource(R.string.language_bn), onClick = { onLanguageSelected("bn") })
            }
        }
    }
}

@Composable
fun LanguageOption(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
    ) {
        Text(text, modifier = Modifier.fillMaxWidth(), fontSize = 16.sp)
    }
}
