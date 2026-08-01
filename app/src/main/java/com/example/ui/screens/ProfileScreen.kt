package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage
import com.example.ui.util.PrayerTimeInfo
import com.example.ui.util.Loc
import com.example.ui.viewmodel.ScriptureViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ScriptureViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToAppearance: () -> Unit
) {
    val readerSettings by viewModel.readerSettings.collectAsState()
    val ctx = LocalContext.current
    val lang = readerSettings.language
    val userReligion by viewModel.userReligion.collectAsState()
    val userSect by viewModel.userSect.collectAsState()
    val userLocationInfo by viewModel.userLocationInfo.collectAsState()
    val livePrayerTimes by viewModel.livePrayerTimes.collectAsState()
    val isLocationLoading by viewModel.isLocationLoading.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val selectedBooks by viewModel.selectedBooksForVerse.collectAsState()
    val currentIntervalMinutes by viewModel.notificationIntervalMinutes.collectAsState()
    var intervalInput by remember(currentIntervalMinutes) {
        mutableStateOf(
            if (currentIntervalMinutes % 60 == 0) (currentIntervalMinutes / 60).toString()
            else currentIntervalMinutes.toString()
        )
    }
    var intervalUnitIsHours by remember(currentIntervalMinutes) {
        mutableStateOf(currentIntervalMinutes % 60 == 0)
    }
    val userState by viewModel.userState.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showForgetMeDialog by remember { mutableStateOf(false) }
    var isReligionCardExpanded by rememberSaveable { mutableStateOf(false) }

    var tempName by remember(userState.displayName) { mutableStateOf(userState.displayName ?: "") }
    var tempBio by remember(userState.bio) { mutableStateOf(userState.bio ?: "") }
    var tempPhotoUrl by remember(userState.photoUrl) { mutableStateOf(userState.photoUrl ?: "") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = viewModel.copyImageUriToInternalStorage(uri)
            if (savedPath != null) {
                tempPhotoUrl = savedPath
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "About Us" else "Hakkımızda",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Scriptorium v1.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "This application is a modern sanctuary of reading, gathering the most rooted and ancient sources of wisdom in human history. It is designed to offer simplicity, tranquility, and a deep reading experience."
                        } else {
                            "Bu uygulama, insanlık tarihinin en köklü ve kadim bilgelik kaynaklarını bir araya getiren modern bir okuma mabedidir. Sadelik, sükûnet ve derin okuma deneyimi sunmak amacıyla tasarlanmıştır."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(if (lang == AppLanguage.EN) "Close" else "Kapat", color = SacredGold)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showEditProfileDialog) {
        val presets = listOf(
            Pair(if (lang == AppLanguage.EN) "Lotus / Peace" else "Lotus / Dinginlik", "https://images.unsplash.com/photo-1542362567-b07eac79094d?w=200&auto=format&fit=crop&q=60"),
            Pair(if (lang == AppLanguage.EN) "Mountain / Fortitude" else "Dağ / Metanet", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=200&auto=format&fit=crop&q=60"),
            Pair(if (lang == AppLanguage.EN) "Universe / Contemplation" else "Evren / Tefekkür", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&auto=format&fit=crop&q=60"),
            Pair(if (lang == AppLanguage.EN) "Forest / Serenity" else "Orman / Sükunet", "https://images.unsplash.com/photo-1448375240586-882707db888b?w=200&auto=format&fit=crop&q=60")
        )

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Edit Profile" else "Profili Düzenle",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Image Preview and Gallery Picker
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tempPhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = tempPhotoUrl,
                                    contentDescription = "Profil Önizleme",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().testTag("select_from_gallery_button"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                                Text(if (lang == AppLanguage.EN) "Select from Gallery" else "Galeriden Seç", style = MaterialTheme.typography.labelLarge)
                            }

                            if (tempPhotoUrl.isNotEmpty()) {
                                TextButton(
                                    onClick = { tempPhotoUrl = "" },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                    )
                                    Text(if (lang == AppLanguage.EN) "Remove Photo" else "Fotoğrafı Kaldır", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(if (lang == AppLanguage.EN) "Name" else "İsim") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                    )

                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text(if (lang == AppLanguage.EN) "Biography / Bio" else "Biyografi / Bio") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input"),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = tempPhotoUrl,
                        onValueChange = { tempPhotoUrl = it },
                        label = { Text(if (lang == AppLanguage.EN) "Profile Picture URL or File Path" else "Profil Resmi URL'si veya Dosya Yolu") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_photo_url_input")
                    )

                    Text(
                        text = if (lang == AppLanguage.EN) "Or select a preset avatar:" else "Veya hazır bir avatar seçin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { (name, url) ->
                            val isSelected = tempPhotoUrl == url
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { tempPhotoUrl = url }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProfile(tempName, tempBio, tempPhotoUrl)
                        showEditProfileDialog = false
                    },
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text(if (lang == AppLanguage.EN) "Save" else "Kaydet", color = SacredGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(if (lang == AppLanguage.EN) "Cancel" else "İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Scriptorium Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = Loc.get("profile", lang),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Profile Header
            item {
                val displayName = userState.displayName ?: (if (lang == AppLanguage.EN) "Journey of Wisdom" else "Bilgelik Yolcusu")
                val email = if (userState.isDemo || userState.email == "misafir@scriptorium.org") {
                    if (lang == AppLanguage.EN) "Guest Reader" else "Misafir Okuyucu"
                } else {
                    userState.email ?: "yolcu@scriptorium.org"
                }
                val initials = displayName.split(" ")
                    .filter { it.isNotBlank() }
                    .map { it.first().uppercase() }
                    .take(2)
                    .joinToString("")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userState.photoUrl != null) {
                            AsyncImage(
                                model = userState.photoUrl,
                                contentDescription = if (lang == AppLanguage.EN) "Profile Picture" else "Profil Resmi",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = if (initials.isEmpty()) "BY" else initials,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        userState.bio?.let { bio ->
                            if (bio.isNotEmpty()) {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text(if (lang == AppLanguage.EN) "Edit Profile" else "Profili Düzenle", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // 2. Statistics Grid
            item {
                val notesHighlights by viewModel.notesHighlights.collectAsState()
                val readingHistory by viewModel.readingHistory.collectAsState()

                val pagesRead = readingHistory.filter { it.bookTitle != "Sessiz Tefekkür" && it.bookTitle != "Silent Meditation" }.sumOf { if (it.pagesRead > 0) it.pagesRead else it.progressPercent / 10 }
                val completedCount = readingHistory.filter { it.bookTitle != "Sessiz Tefekkür" && it.bookTitle != "Silent Meditation" }.count { it.isCompleted || it.progressPercent >= 100 }
                val reflectionMinutes = readingHistory.sumOf { it.contemplationMinutes }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val stats = listOf(
                        Pair(pagesRead.toString(), Loc.get("pages_read", lang)),
                        Pair(completedCount.toString(), Loc.get("books_completed", lang)),
                        Pair(reflectionMinutes.toString(), if (lang == AppLanguage.EN) "Meditation (Min)" else "Tefekkür (Dk)")
                    )

                    stats.forEach { (value, label) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = value,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Tefekkür Kronometresi Card
            item {
                var tefekkürTimeSeconds by remember { mutableStateOf(0) }
                var isTimerRunning by remember { mutableStateOf(false) }

                LaunchedEffect(isTimerRunning) {
                    if (isTimerRunning) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            tefekkürTimeSeconds++
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        SacredGold.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HourglassEmpty,
                                contentDescription = null,
                                tint = SacredGold
                            )
                            Text(
                                text = Loc.get("meditation_timer", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = String.format("%02d:%02d", tefekkürTimeSeconds / 60, tefekkürTimeSeconds % 60),
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SacredGold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isTimerRunning) {
                                Button(
                                    onClick = { isTimerRunning = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = if (lang == AppLanguage.EN) "Start" else "Başlat")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == AppLanguage.EN) "Start" else "Başlat")
                                }
                            } else {
                                Button(
                                    onClick = { isTimerRunning = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = if (lang == AppLanguage.EN) "Stop" else "Durdur")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == AppLanguage.EN) "Stop" else "Durdur")
                                }
                            }

                            if (tefekkürTimeSeconds >= 10) {
                                OutlinedButton(
                                    onClick = {
                                        isTimerRunning = false
                                        val minutes = (tefekkürTimeSeconds + 30) / 60
                                        viewModel.updateReadingSessionProgress(
                                            bookTitle = if (lang == AppLanguage.EN) "Silent Meditation" else "Sessiz Tefekkür",
                                            subtitle = if (lang == AppLanguage.EN) "Meditation Timer Session" else "Tefekkür Kronometresi Seansı",
                                            progress = 100,
                                            surahOrChapter = (if (lang == AppLanguage.EN) "Meditation Session (" else "Tefekkür Seansı (") + String.format("%02d:%02d", tefekkürTimeSeconds / 60, tefekkürTimeSeconds % 60) + ")",
                                            pagesRead = 0,
                                            isCompleted = true,
                                            contemplationMinutes = minutes
                                        )
                                        tefekkürTimeSeconds = 0
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SacredGold),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = Loc.get("save", lang))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Loc.get("save", lang))
                                }

                                OutlinedButton(
                                    onClick = {
                                        isTimerRunning = false
                                        tefekkürTimeSeconds = 0
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (lang == AppLanguage.EN) "Reset" else "Sıfırla")
                                }
                            }
                        }
                    }
                }
            }

            // 3. Scriptorium List Sections (Kütüphanem)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Loc.get("library", lang).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val librarySections = listOf(
                        Triple(Loc.get("reading_history", lang), Icons.Filled.History, onNavigateToHistory),
                        Triple(Loc.get("notes_highlights", lang), Icons.Filled.EditNote, onNavigateToNotes)
                    )

                    librarySections.forEach { (title, icon, action) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { action() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Settings & Support
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "SETTINGS & SUPPORT" else "AYARLAR VE DESTEK",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Appearance Settings Row
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAppearance() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatSize,
                                    contentDescription = Loc.get("appearance_settings", lang),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Loc.get("appearance_settings", lang),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Inter / Playfair",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Religion & Sect Preferences Card (Collapsible Drawer / Accordion)
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("religion_sect_preferences_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Clickable Header to Expand/Collapse
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isReligionCardExpanded = !isReligionCardExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SelfImprovement,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.EN) "Religion, Sect & Prayer Preferences" else "Din, Mezhep ve İbadet Ayarları",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isReligionCardExpanded) {
                                                if (lang == AppLanguage.EN) "Select your faith tradition to receive personalized prayer & reflection notifications." else "İnanç geleneğinizi seçerek kişiselleştirilmiş namaz, dua ve tefekkür bildirimleri alın."
                                            } else {
                                                "${if (lang == AppLanguage.EN) "Selected" else "Seçili"}: ${userReligion.getTitle(lang)} • ${userSect.getTitle(lang)}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isReligionCardExpanded) MaterialTheme.colorScheme.onSurfaceVariant else SacredGold,
                                            fontWeight = if (!isReligionCardExpanded) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isReligionCardExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isReligionCardExpanded) "Daralt" else "Genişlet",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            AnimatedVisibility(visible = isReligionCardExpanded) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // 1. Religion Selector
                                    Text(
                                        text = if (lang == AppLanguage.EN) "SELECT RELIGION" else "DİN SEÇİMİ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )

                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val allReligions = com.example.ui.util.UserReligion.values()
                                        items(allReligions.size) { index ->
                                            val religion = allReligions[index]
                                            val isSelected = userReligion == religion
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setUserReligion(religion) },
                                                label = { Text(religion.getTitle(lang)) },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = SacredGold.copy(alpha = 0.2f),
                                                    selectedLabelColor = SacredGold
                                                )
                                            )
                                        }
                                    }

                                    // 2. Sect / Denomination Selector
                                    Text(
                                        text = if (lang == AppLanguage.EN) "SELECT SECT / TRADITION" else "MEZHEP / GELENEK SEÇİMİ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )

                                    val sectsForCurrentReligion = com.example.ui.util.UserSect.getSectsForReligion(userReligion)
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(sectsForCurrentReligion.size) { index ->
                                            val sect = sectsForCurrentReligion[index]
                                            val isSelected = userSect == sect
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.setUserSect(sect) },
                                                label = { Text(sect.getTitle(lang)) },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null
                                            )
                                        }
                                    }

                                    // 3. Location & Live API Status Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = SacredGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (userLocationInfo != null) "${userLocationInfo?.cityName}, ${userLocationInfo?.countryName}" else if (lang == AppLanguage.EN) "Detecting Location..." else "Konum Tespit Ediliyor...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (userLocationInfo != null) (if (lang == AppLanguage.EN) "Location-based Prayer Times" else "Konuma Göre Namaz Vakitleri") else (if (lang == AppLanguage.EN) "Otomatik Konum Tespiti" else "Otomatik Konum Tespiti"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.refreshLocationAndPrayerTimes() },
                                            enabled = !isLocationLoading
                                        ) {
                                            if (isLocationLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = SacredGold,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Refresh,
                                                    contentDescription = "Yenile",
                                                    tint = SacredGold
                                                )
                                            }
                                        }
                                    }

                                    // 4. Prayer / Worship Schedule Overview
                                    Text(
                                        text = if (lang == AppLanguage.EN) "PRAYER & WORSHIP SCHEDULE" else "İBADET & NAMAZ VAKİTLERİ ÇİZELGESİ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )

                                    val schedules = if (livePrayerTimes.isNotEmpty()) livePrayerTimes else com.example.ui.util.FaithPrayerSchedule.getPrayerSchedules(userReligion, userSect)
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        schedules.forEach { prayer ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AccessTime,
                                                        contentDescription = null,
                                                        tint = SacredGold,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = prayer.getName(lang),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = prayer.timeStr,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SacredGold
                                                )
                                            }
                                            Text(
                                                text = prayer.getMessage(lang),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 24.dp)
                                            )
                                            if (prayer != schedules.last()) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notifications Switch Row
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = Loc.get("notifications", lang),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = Loc.get("notifications", lang),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    (ctx as? android.app.Activity)?.let { activity ->
                                                        androidx.core.app.ActivityCompat.requestPermissions(
                                                            activity,
                                                            arrayOf(permission),
                                                            101
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        viewModel.toggleNotifications()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.testTag("notification_switch")
                                )
                            }

                            if (notificationsEnabled) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.EN) "NOTIFICATION FREQUENCY" else "BİLDİRİM SIKLIĞI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = intervalInput,
                                            onValueChange = { newValue ->
                                                if (newValue.all { it.isDigit() }) {
                                                    intervalInput = newValue
                                                }
                                            },
                                            label = { Text(if (lang == AppLanguage.EN) "Interval Value" else "Süre Değeri") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f).testTag("notification_interval_input")
                                        )
                                        
                                        Row(
                                            modifier = Modifier.weight(1.8f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = !intervalUnitIsHours,
                                                onClick = { intervalUnitIsHours = false },
                                                label = { Text(if (lang == AppLanguage.EN) "Minutes" else "Dakika") }
                                            )
                                            FilterChip(
                                                selected = intervalUnitIsHours,
                                                onClick = { intervalUnitIsHours = true },
                                                label = { Text(if (lang == AppLanguage.EN) "Hours" else "Saat") }
                                            )
                                        }
                                    }
                                    
                                    val parsedValue = intervalInput.toIntOrNull() ?: 0
                                    val isInputValid = if (intervalUnitIsHours) parsedValue >= 1 else parsedValue >= 2
                                    val errorText = when {
                                        intervalInput.isEmpty() -> if (lang == AppLanguage.EN) "Please enter a valid number." else "Lütfen geçerli bir sayı girin."
                                        !isInputValid -> if (intervalUnitIsHours) {
                                            if (lang == AppLanguage.EN) "At least 1 hour must be selected." else "En az 1 saat seçilmelidir."
                                        } else {
                                            if (lang == AppLanguage.EN) "At least 2 minutes must be selected." else "En az 2 dakika seçilmelidir."
                                        }
                                        else -> null
                                    }
                                    
                                    if (errorText != null) {
                                        Text(
                                            text = errorText,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            val minutes = if (intervalUnitIsHours) parsedValue * 60 else parsedValue
                                            viewModel.updateNotificationInterval(minutes)
                                        },
                                        enabled = isInputValid && errorText == null,
                                        modifier = Modifier.fillMaxWidth().testTag("save_interval_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (lang == AppLanguage.EN) "Update Notification Interval" else "Bildirim Sıklığını Güncelle")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                val permission = android.Manifest.permission.POST_NOTIFICATIONS
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    (ctx as? android.app.Activity)?.let { activity ->
                                                        androidx.core.app.ActivityCompat.requestPermissions(
                                                            activity,
                                                            arrayOf(permission),
                                                            101
                                                        )
                                                    }
                                                }
                                            }
                                            viewModel.sendTestNotification()
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("send_test_notification_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SacredGold),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SacredGold)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = "Test",
                                            modifier = Modifier.padding(end = 8.dp).size(18.dp)
                                        )
                                        Text(if (lang == AppLanguage.EN) "Send Test Notification" else "Test Bildirimi Gönder")
                                    }
                                    
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    Text(
                                        text = if (lang == AppLanguage.EN) "NOTIFICATION SOURCES" else "BİLDİRİM GELECEK KAYNAKLAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    viewModel.books.forEach { book ->
                                        val isSelected = selectedBooks.contains(book.id)
                                        val bookTitle = Loc.get(book.id, lang)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.toggleVerseBookSelection(book.id) }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                val bookIcon = when (book.id) {
                                                    "quran" -> Icons.Filled.AutoStories
                                                    "torah" -> Icons.AutoMirrored.Filled.MenuBook
                                                    "sermon" -> Icons.Filled.HistoryEdu
                                                    else -> Icons.Filled.Book
                                                }
                                                Icon(
                                                    imageVector = bookIcon,
                                                    contentDescription = bookTitle,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = bookTitle,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { viewModel.toggleVerseBookSelection(book.id) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MaterialTheme.colorScheme.primary,
                                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // About Us Row
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = if (lang == AppLanguage.EN) "About Us" else "Hakkımızda",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) "About Us" else "Hakkımızda",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // 5. "Beni Unut (!)" / "Forget Me (!)" Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { showForgetMeDialog = true }
                        .testTag("forget_me_button"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "(!)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.EN) "Forget Me (!)" else "Beni Unut (!)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (lang == AppLanguage.EN)
                                    "Permanently delete all reading history, notes, downloaded books, settings, and external memory backup files from this phone."
                                else
                                    "Cihazınızdaki ve harici bellekteki tüm okuma geçmişini, notları, indirilen kitapları ve yedek dosyasını kalıcı olarak siler.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. Logout Button
            item {
                Button(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("logout_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.1f),
                        contentColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = if (lang == AppLanguage.EN) "Log Out" else "Oturumu Kapat",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (lang == AppLanguage.EN) "Log Out" else "Oturumu Kapat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showForgetMeDialog) {
        AlertDialog(
            onDismissRequest = { showForgetMeDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "(!)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Forget Me & Erase Data (!)" else "Beni Unut ve Tüm Verileri Sil (!)",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN)
                            "ARE YOU SURE?\n\nThis action will PERMANENTLY DELETE:\n• All reading history & progress\n• All notes & highlighted verses\n• All downloaded offline books & surahs\n• Language, font & theme settings\n• External memory backup file (scriptorium_user_backup.json)\n\nEven if you reinstall the app, these files will be gone permanently!"
                        else
                            "EMİN MİSİNİZ?\n\nBu işlem aşağıdakileri KALICI OLARAK SİLECEKTİR:\n• Tüm okuma geçmişi ve ilerleme kayıtları\n• Tüm notlar ve fosforlu ayetler\n• İndirilen tüm çevrimdışı kitaplar ve sureler\n• Dil, yazı tipi ve görünüm ayarları\n• Telefon hafızasındaki yedek dosyası (scriptorium_user_backup.json)\n\nUygulama silinip tekrar yüklense bile bu veriler bir daha geri getirilemez!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForgetMeDialog = false
                        viewModel.forgetMeAndClearAllData {
                            android.widget.Toast.makeText(
                                ctx,
                                if (lang == AppLanguage.EN) "All data and external memory backups have been permanently erased." else "Tüm verileriniz ve harici bellek yedeği kalıcı olarak silindi.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_forget_me_button")
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Yes, Forget Me" else "Evet, Beni Unut",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showForgetMeDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (lang == AppLanguage.EN) "Cancel" else "Vazgeç")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
