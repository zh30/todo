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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Determine current language state for display. 
    // This is a simplified check. Ideally we read from AppCompatDelegate or LocaleManager.
    val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
    val languageSubtitle = when (currentLocale?.language) {
        "zh" -> stringResource(R.string.language_zh)
        "en" -> stringResource(R.string.language_en)
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.language),
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LanguageOption(text = stringResource(R.string.follow_system), onClick = { onLanguageSelected("") })
                LanguageOption(text = stringResource(R.string.language_zh), onClick = { onLanguageSelected("zh") })
                LanguageOption(text = stringResource(R.string.language_en), onClick = { onLanguageSelected("en") })
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
