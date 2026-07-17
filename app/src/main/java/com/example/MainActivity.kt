package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Book
import com.example.ui.screens.AppearanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotesHighlightsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.ReadingHistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.DictionaryScreen
import com.example.ui.util.Loc
import com.example.ui.util.AppLanguage
import com.example.ui.theme.ScriptoriumTheme
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.ScriptureViewModel
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

enum class NavigationScreen {
    HOME, LIBRARY, DICTIONARY, SEARCH, PROFILE, READER, HISTORY, NOTES, APPEARANCE
}

class MainActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DailyVerseReceiver.scheduleAlarm(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                DailyVerseReceiver.scheduleAlarm(this)
            }
        } else {
            DailyVerseReceiver.scheduleAlarm(this)
        }
        setContent {
            val viewModel: ScriptureViewModel = viewModel()
            val readerSettings by viewModel.readerSettings.collectAsState()
            val userState by viewModel.userState.collectAsState()
            val lang = readerSettings.language

            ScriptoriumTheme(themeSetting = readerSettings.theme) {
                // Alternatif Güncelleme Kontrolü (GitHub üzerinden)
                var updateInfoState by remember { mutableStateOf<com.example.ui.util.UpdateInfo?>(null) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    try {
                        val update = com.example.ui.util.UpdateManager.checkForUpdate()
                        if (update != null) {
                            updateInfoState = update
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Güncelleme kontrolü başarısız oldu", e)
                    }
                }

                // Güncelleme Dialogu
                updateInfoState?.let { updateInfo ->
                    AlertDialog(
                        onDismissRequest = {
                            if (!updateInfo.isForceUpdate) {
                                updateInfoState = null
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    tint = SacredGold
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) "Update Available!" else "Güncelleme Mevcut!",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 20.sp
                                )
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) {
                                        "A new version of Scriptorium is available. Update now to experience the latest features and stability enhancements."
                                    } else {
                                        "Scriptorium için yeni bir sürüm mevcut. En son özellikleri ve kararlılık iyileştirmelerini deneyimlemek için şimdi güncelleyin."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Sürüm Bilgi Kutusu
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.EN) {
                                                "Current: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                            } else {
                                                "Mevcut: v${BuildConfig.VERSION_NAME} (Kod ${BuildConfig.VERSION_CODE})"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (lang == AppLanguage.EN) {
                                                "New Version: v${updateInfo.versionName} (Build ${updateInfo.versionCode})"
                                            } else {
                                                "Yeni Sürüm: v${updateInfo.versionName} (Kod ${updateInfo.versionCode})"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SacredGold
                                        )
                                    }
                                }
                                
                                val changelog = if (lang == AppLanguage.EN) updateInfo.changelogEn else updateInfo.changelogTr
                                if (changelog.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (lang == AppLanguage.EN) "What's New:" else "Yenilikler:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = changelog,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    com.example.ui.util.UpdateManager.launchUpdate(context, updateInfo.updateUrl)
                                    if (!updateInfo.isForceUpdate) {
                                        updateInfoState = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "Update Now" else "Şimdi Güncelle",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            if (!updateInfo.isForceUpdate) {
                                TextButton(
                                    onClick = { updateInfoState = null }
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.EN) "Later" else "Daha Sonra",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                if (!userState.isLoggedIn) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {}
                    )
                } else {
                    var currentScreen by remember { mutableStateOf(NavigationScreen.HOME) }
                var activeBookForReader by remember { mutableStateOf<Book?>(null) }
                var searchScreenInitialQuery by remember { mutableStateOf("") }

                // Keep track of primary tab screen to return back correctly
                var lastPrimaryScreen by remember { mutableStateOf(NavigationScreen.HOME) }

                // Custom Back Pressed Handler
                BackHandler(enabled = currentScreen != NavigationScreen.HOME) {
                    when (currentScreen) {
                        NavigationScreen.LIBRARY,
                        NavigationScreen.DICTIONARY,
                        NavigationScreen.SEARCH,
                        NavigationScreen.PROFILE -> {
                            currentScreen = NavigationScreen.HOME
                        }
                        NavigationScreen.READER -> {
                            // Back to either home, library or search depending on where we launched it
                            currentScreen = lastPrimaryScreen
                        }
                        NavigationScreen.HISTORY,
                        NavigationScreen.NOTES,
                        NavigationScreen.APPEARANCE -> {
                            currentScreen = NavigationScreen.PROFILE
                        }
                        else -> {
                            currentScreen = NavigationScreen.HOME
                        }
                    }
                }

                val showBottomBar = currentScreen in listOf(
                    NavigationScreen.HOME,
                    NavigationScreen.LIBRARY,
                    NavigationScreen.DICTIONARY,
                    NavigationScreen.SEARCH,
                    NavigationScreen.PROFILE
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp // subtle shadow
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == NavigationScreen.HOME,
                                    onClick = {
                                        currentScreen = NavigationScreen.HOME
                                        lastPrimaryScreen = NavigationScreen.HOME
                                    },
                                    icon = { Icon(Icons.Filled.Home, contentDescription = Loc.get("home", lang)) },
                                    label = { Text(Loc.get("home", lang)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_home")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == NavigationScreen.LIBRARY,
                                    onClick = {
                                        currentScreen = NavigationScreen.LIBRARY
                                        lastPrimaryScreen = NavigationScreen.LIBRARY
                                    },
                                    icon = { Icon(Icons.Filled.AutoStories, contentDescription = Loc.get("library", lang)) },
                                    label = { Text(Loc.get("library", lang)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_library")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == NavigationScreen.DICTIONARY,
                                    onClick = {
                                        currentScreen = NavigationScreen.DICTIONARY
                                        lastPrimaryScreen = NavigationScreen.DICTIONARY
                                    },
                                    icon = { Icon(Icons.Filled.Book, contentDescription = Loc.get("dictionary", lang)) },
                                    label = { Text(Loc.get("dictionary", lang)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_dictionary")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == NavigationScreen.SEARCH,
                                    onClick = {
                                        currentScreen = NavigationScreen.SEARCH
                                        searchScreenInitialQuery = "" // Reset filter
                                        lastPrimaryScreen = NavigationScreen.SEARCH
                                    },
                                    icon = { Icon(Icons.Filled.Search, contentDescription = Loc.get("search", lang)) },
                                    label = { Text(Loc.get("search", lang)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_search")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == NavigationScreen.PROFILE,
                                    onClick = {
                                        currentScreen = NavigationScreen.PROFILE
                                        lastPrimaryScreen = NavigationScreen.PROFILE
                                    },
                                    icon = { Icon(Icons.Filled.Person, contentDescription = Loc.get("profile", lang)) },
                                    label = { Text(Loc.get("profile", lang)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_profile")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            NavigationScreen.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToBook = { book ->
                                        activeBookForReader = book
                                        currentScreen = NavigationScreen.READER
                                    },
                                    onNavigateToSearch = { query ->
                                        searchScreenInitialQuery = query
                                        currentScreen = NavigationScreen.SEARCH
                                        lastPrimaryScreen = NavigationScreen.SEARCH
                                    },
                                    onNavigateToSettings = {
                                        currentScreen = NavigationScreen.APPEARANCE
                                    }
                                )
                            }
                            NavigationScreen.LIBRARY -> {
                                LibraryScreen(
                                    viewModel = viewModel,
                                    onNavigateToBook = { book ->
                                        activeBookForReader = book
                                        currentScreen = NavigationScreen.READER
                                    },
                                    onNavigateToSettings = {
                                        currentScreen = NavigationScreen.APPEARANCE
                                    }
                                )
                            }
                            NavigationScreen.DICTIONARY -> {
                                DictionaryScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = {
                                        currentScreen = NavigationScreen.APPEARANCE
                                    }
                                )
                            }
                            NavigationScreen.SEARCH -> {
                                SearchScreen(
                                    viewModel = viewModel,
                                    onNavigateToBook = { book ->
                                        activeBookForReader = book
                                        currentScreen = NavigationScreen.READER
                                    },
                                    initialQuery = searchScreenInitialQuery
                                )
                            }
                            NavigationScreen.PROFILE -> {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateToHistory = { currentScreen = NavigationScreen.HISTORY },
                                    onNavigateToNotes = { currentScreen = NavigationScreen.NOTES },
                                    onNavigateToAppearance = { currentScreen = NavigationScreen.APPEARANCE }
                                )
                            }
                            NavigationScreen.READER -> {
                                activeBookForReader?.let { book ->
                                    ReaderScreen(
                                        viewModel = viewModel,
                                        book = book,
                                        onNavigateBack = {
                                            currentScreen = lastPrimaryScreen
                                        }
                                    )
                                }
                            }
                            NavigationScreen.HISTORY -> {
                                ReadingHistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = NavigationScreen.PROFILE }
                                )
                            }
                            NavigationScreen.NOTES -> {
                                NotesHighlightsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = NavigationScreen.PROFILE }
                                )
                            }
                            NavigationScreen.APPEARANCE -> {
                                AppearanceScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        // If we came from Reader, go back to Reader, otherwise go back to Profile/Home
                                        if (activeBookForReader != null && lastPrimaryScreen == NavigationScreen.READER) {
                                            currentScreen = NavigationScreen.READER
                                        } else {
                                            currentScreen = lastPrimaryScreen
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
              }
            }
        }
    }
}
