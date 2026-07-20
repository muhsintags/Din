package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuranRepository
import com.example.data.model.QuranSurah
import com.example.data.model.QuranVerse
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.ScriptureViewModel
import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderView(
    viewModel: ScriptureViewModel,
    onNavigateBack: () -> Unit
) {
    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language

    val currentSelectedSurah by viewModel.currentSelectedSurah.collectAsState()
    val currentSurahContent by viewModel.currentSurahContent.collectAsState()
    val isSurahLoading by viewModel.isSurahLoading.collectAsState()
    val surahError by viewModel.surahError.collectAsState()

    val currentPlayingUrl by viewModel.currentPlayingUrl.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val isAudioLoading by viewModel.isAudioLoading.collectAsState()
    val activePlayingVerseIndex by viewModel.activePlayingVerseIndex.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            QuranRepository.surahs
        } else {
            QuranRepository.surahs.filter {
                it.nameTurkish.contains(searchQuery, ignoreCase = true) ||
                it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                it.number.toString() == searchQuery
            }
        }
    }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var selectedVerseForNote by remember { mutableStateOf<QuranVerse?>(null) }
    var noteTextQuery by remember { mutableStateOf("") }

    var fontSizeMultiplier by remember { mutableStateOf(1f) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Automatic progress and timing tracker
    val startTime = remember(currentSelectedSurah?.number) { System.currentTimeMillis() }
    var elapsedSeconds by remember(currentSelectedSurah?.number) { mutableStateOf(0) }
    LaunchedEffect(currentSelectedSurah?.number) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }
    val autoMinutes = (elapsedSeconds / 60).coerceAtLeast(0)

    val maxScrolledVerseIndex = remember(currentSelectedSurah?.number) { mutableStateOf(0) }
    val visibleIndices = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }
        }
    }
    val versesCount = currentSurahContent?.verses?.size ?: 1
    LaunchedEffect(visibleIndices.value, currentSelectedSurah?.number) {
        val lastVisible = visibleIndices.value.lastOrNull() ?: 0
        // Item 0 is the download card, item 1 is Bismillah.
        // Verses start at index 2.
        val currentVerseIdx = (lastVisible - 2).coerceIn(0, versesCount - 1)
        if (currentVerseIdx > maxScrolledVerseIndex.value) {
            maxScrolledVerseIndex.value = currentVerseIdx
        }
    }

    val totalPagesOfSurah = (versesCount / 5.0).coerceAtLeast(1.0).toInt()
    val autoPagesRead = ((maxScrolledVerseIndex.value + 1).toFloat() / versesCount.toFloat() * totalPagesOfSurah)
        .toInt().coerceIn(1, totalPagesOfSurah)

    // Check if user has scrolled to the bottom (lastVisible index is >= versesCount + 2)
    val isBottomReached = remember(currentSelectedSurah?.number) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= versesCount + 2
        }
    }

    // Auto-scroll to playing verse
    LaunchedEffect(activePlayingVerseIndex) {
        activePlayingVerseIndex?.let { index ->
            if (index >= 0) {
                // Scroll to index + 1 (to account for the header)
                coroutineScope.launch {
                    listState.animateScrollToItem((index + 1).coerceAtLeast(0))
                }
            }
        }
    }

    if (showAddNoteDialog && selectedVerseForNote != null) {
        val verse = selectedVerseForNote!!
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Add Verse Reflection" else "Ayet Tefekkürü Ekle",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "${currentSelectedSurah?.nameEnglish ?: "Quran"} Surah, Verse ${verse.number}"
                        } else {
                            "${currentSelectedSurah?.nameTurkish ?: "Kur'an"} Suresi, ${verse.number}. Ayet"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = SacredGold
                    )
                    
                    Text(
                        text = verse.textArabic,
                        style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Right),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = verse.textTurkish,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = noteTextQuery,
                        onValueChange = { noteTextQuery = it },
                        label = { Text(if (lang == AppLanguage.EN) "Your Reflection Note" else "Tefekkür Notunuz") },
                        placeholder = { Text(if (lang == AppLanguage.EN) "Write your thoughts about this verse..." else "Bu ayet hakkındaki düşüncelerinizi yazın...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("quran_note_input_field"),
                        singleLine = false,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bookTitle = if (lang == AppLanguage.EN) {
                            "Quran - ${currentSelectedSurah?.nameEnglish} ${verse.number}"
                        } else {
                            "Kur'an - ${currentSelectedSurah?.nameTurkish} ${verse.number}"
                        }
                        viewModel.addNoteOrHighlight(
                            bookTitle = bookTitle,
                            quoteText = verse.textTurkish,
                            noteText = noteTextQuery,
                            isHighlightOnly = noteTextQuery.trim().isEmpty()
                        )
                        showAddNoteDialog = false
                        noteTextQuery = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (lang == AppLanguage.EN) "Save Note" else "Notu Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text(if (lang == AppLanguage.EN) "Cancel" else "İptal")
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
                    Column {
                        Text(
                            text = currentSelectedSurah?.let {
                                if (lang == AppLanguage.EN) {
                                    "${it.number}. Surah ${it.nameEnglish}"
                                } else {
                                    "${it.number}. ${it.nameTurkish} Suresi"
                                }
                            } ?: (if (lang == AppLanguage.EN) "Holy Quran" else "Kur'an-ı Kerim"),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (currentSelectedSurah != null) {
                            Text(
                                text = "${currentSelectedSurah?.nameEnglish} • ${currentSelectedSurah?.nameArabic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSelectedSurah != null) {
                            viewModel.stopAudio()
                            viewModel.selectSurah(QuranRepository.surahs[0]) // Reset or let user select again by setting surah null
                            // Actually let's clear the selected surah to show list
                            val clearSurahField = viewModel.javaClass.getDeclaredField("_currentSelectedSurah").apply {
                                isAccessible = true
                            }
                            val mFlow = clearSurahField.get(viewModel) as MutableStateFlow<QuranSurah?>
                            mFlow.value = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = if (lang == AppLanguage.EN) "Back" else "Geri",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (currentSelectedSurah != null) {
                        // Font size controls
                        IconButton(onClick = { fontSizeMultiplier = (fontSizeMultiplier - 0.1f).coerceAtLeast(0.8f) }) {
                            Icon(Icons.Filled.Remove, if (lang == AppLanguage.EN) "Decrease Font Size" else "Yazıyı Küçült", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { fontSizeMultiplier = (fontSizeMultiplier + 0.1f).coerceAtMost(1.8f) }) {
                            Icon(Icons.Filled.Add, if (lang == AppLanguage.EN) "Increase Font Size" else "Yazıyı Büyüt", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentSelectedSurah == null) {
                // SURAH SELECTION VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Live Holy Quran API" else "Canlı Kur'an-ı Kerim API",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SacredGold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "Listen to all 114 surahs in Arabic with English translation and the beautiful recitation of Mishary Rashid Alafasy."
                        } else {
                            "Diyanet Türkçe meali ve Mishary Rashid Alafasy'nin eşsiz kıraati eşliğinde, 114 surenin tamamını arapça ve türkçe sesli dinleyin."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (lang == AppLanguage.EN) "Search surah name or number..." else "Sure ismi veya numara ara...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = if (lang == AppLanguage.EN) "Search" else "Ara") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = if (lang == AppLanguage.EN) "Clear" else "Temizle")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("surah_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quran Bulk Offline Download Banner
                    val downloadedBooks by viewModel.downloadedBooks.collectAsState()
                    val downloadProgress by viewModel.downloadProgress.collectAsState()
                    val isQuranDownloaded = downloadedBooks.contains("quran")
                    val quranProgress = downloadProgress["quran"]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isQuranDownloaded) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isQuranDownloaded) SacredGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isQuranDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = if (isQuranDownloaded) SacredGold else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isQuranDownloaded) {
                                        if (lang == AppLanguage.EN) "All Surahs Offline!" else "Tüm Sureler Çevrimdışı!"
                                    } else {
                                        if (lang == AppLanguage.EN) "Download Entire Book Offline" else "Tüm Kitabı Çevrimdışı İndir"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isQuranDownloaded) {
                                        if (lang == AppLanguage.EN) "All 114 surahs are saved to your device. Read anytime without internet." else "114 surenin tamamı arapça ve türkçe mealleriyle cihaza kaydedildi. İnternetsiz okuyabilirsiniz."
                                    } else if (quranProgress != null) {
                                        if (lang == AppLanguage.EN) "Downloading: %${(quranProgress * 100).toInt()}" else "İndiriliyor: %${(quranProgress * 100).toInt()}"
                                    } else {
                                        if (lang == AppLanguage.EN) "Read and listen to all 114 surahs offline even when you don't have internet access." else "İnternetiniz yokken bile tüm 114 sureyi arapça ve türkçe mealleriyle anında okuyabilirsiniz."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (quranProgress != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { quranProgress },
                                        color = SacredGold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            if (quranProgress == null) {
                                if (isQuranDownloaded) {
                                    IconButton(onClick = { viewModel.deleteBookDownload("quran") }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = if (lang == AppLanguage.EN) "Delete Download" else "İndirmeyi Sil",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.downloadBook("quran") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (lang == AppLanguage.EN) "Download" else "İndir", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Surah List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(filteredSurahs) { _, surah ->
                            Card(
                                onClick = { viewModel.selectSurah(surah) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("surah_card_${surah.number}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Surah Number badge
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = surah.number.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Surah names
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == AppLanguage.EN) surah.nameEnglish else surah.nameTurkish,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${surah.nameEnglish} • ${surah.ayahCount} ${if (lang == AppLanguage.EN) "Verses" else "Ayet"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Arabic Name
                                    Text(
                                        text = surah.nameArabic,
                                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                                        color = SacredGold,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ACTIVE SURAH VIEW
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isSurahLoading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = SacredGold)
                                Text(
                                    text = if (lang == AppLanguage.EN) "Loading verses..." else "Ayetler yükleniyor...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else if (surahError != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = surahError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.loadSurahContent(currentSelectedSurah!!.number) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                                ) {
                                    Text(if (lang == AppLanguage.EN) "Retry" else "Tekrar Dene")
                                }
                            }
                        }
                    } else if (currentSurahContent != null) {
                        val content = currentSurahContent!!
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                        ) {
                            // Offline Download Card
                            item {
                                val downloadedSurahs by viewModel.downloadedSurahs.collectAsState()
                                val downloadProgress by viewModel.downloadProgress.collectAsState()
                                val isSurahDownloaded = downloadedSurahs.contains(content.number)
                                val surahProgress = downloadProgress["surah_${content.number}"]

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSurahDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                                                    contentDescription = null,
                                                    tint = if (isSurahDownloaded) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isSurahDownloaded) {
                                                            if (lang == AppLanguage.EN) "Available Offline" else "Çevrimdışı Kullanılabilir"
                                                        } else {
                                                            if (lang == AppLanguage.EN) "Offline Reader & Audio" else "Çevrimdışı Okuma ve Ses"
                                                        },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (surahProgress != null) {
                                                        Text(
                                                            text = if (lang == AppLanguage.EN) "Downloading: %${(surahProgress * 100).toInt()}" else "İndiriliyor: %${(surahProgress * 100).toInt()}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (isSurahDownloaded) {
                                                                if (lang == AppLanguage.EN) "Read and listen offline without internet." else "İnternetsiz de okuyabilir ve dinleyebilirsiniz."
                                                            } else {
                                                                if (lang == AppLanguage.EN) "Save translation and audio to device." else "Meali ve tüm ses dosyalarını cihaza kaydedin."
                                                            },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            if (surahProgress != null) {
                                                CircularProgressIndicator(
                                                    progress = { surahProgress },
                                                    color = SacredGold,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else if (isSurahDownloaded) {
                                                IconButton(
                                                    onClick = { viewModel.deleteSurahDownload(content.number) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = if (lang == AppLanguage.EN) "Delete Download" else "İndirmeyi Sil",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else {
                                                Button(
                                                    onClick = { viewModel.downloadSurah(content.number) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(if (lang == AppLanguage.EN) "Download" else "İndir", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                        if (surahProgress != null) {
                                            LinearProgressIndicator(
                                                progress = { surahProgress },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = SacredGold,
                                                trackColor = Color.Transparent
                                            )
                                        }
                                    }
                                }
                            }

                            // Basmele / Header
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = FontFamily.Serif,
                                                textAlign = TextAlign.Center,
                                                fontSize = (24 * fontSizeMultiplier).sp
                                            ),
                                            color = SacredGold
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.EN) {
                                                "In the name of Allah, the Entirely Merciful, the Especially Merciful."
                                            } else {
                                                "Rahmân ve Rahîm olan Allah'ın adıyla."
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = FontStyle.Italic,
                                                textAlign = TextAlign.Center
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Verses List
                            itemsIndexed(content.verses) { index, verse ->
                                val isVersePlaying = activePlayingVerseIndex == index
                                val isSelected = isVersePlaying

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verse_card_${verse.number}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    border = if (isSelected) {
                                        CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(SacredGold)
                                        )
                                    } else {
                                        CardDefaults.outlinedCardBorder()
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Verse number & controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = verse.number.toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            // Verse Play & Note buttons
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Note button
                                                IconButton(
                                                    onClick = {
                                                        selectedVerseForNote = verse
                                                        showAddNoteDialog = true
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.EditNote,
                                                        contentDescription = if (lang == AppLanguage.EN) "Add Note" else "Not Ekle",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Play button
                                                IconButton(
                                                    onClick = {
                                                        if (isVersePlaying) {
                                                            if (isAudioPlaying) {
                                                                viewModel.togglePlayPause()
                                                            } else {
                                                                viewModel.playAudio(verse.audioUrl, index)
                                                            }
                                                        } else {
                                                            viewModel.playAudio(verse.audioUrl, index)
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    if (isVersePlaying && isAudioLoading) {
                                                        CircularProgressIndicator(
                                                            color = SacredGold,
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = if (isVersePlaying && isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                            contentDescription = if (lang == AppLanguage.EN) "Listen" else "Dinle",
                                                            tint = if (isVersePlaying) SacredGold else MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Arabic Text
                                        Text(
                                            text = verse.textArabic,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = FontFamily.Serif,
                                                textAlign = TextAlign.Right,
                                                lineHeight = (38 * fontSizeMultiplier).sp,
                                                fontSize = (22 * fontSizeMultiplier).sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                        // Turkish Text
                                        Text(
                                            text = verse.textTurkish,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                lineHeight = (24 * fontSizeMultiplier).sp,
                                                fontSize = (16 * fontSizeMultiplier).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Okumayı Kaydet / Tamamla Block
                            item {
                                val historyList by viewModel.readingHistory.collectAsState()
                                val bookTitle = if (lang == AppLanguage.EN) "Holy Quran" else "Kur'an-ı Kerim"
                                val previousPagesRead = historyList
                                    .filter { it.bookTitle == bookTitle }
                                    .sumOf { it.pagesRead }
                                val newTotalPagesRead = previousPagesRead + autoPagesRead
                                val totalPages = 604
                                val calculatedProgress = ((newTotalPagesRead.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(1, 100)

                                var isSavedSuccessfully by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp, bottom = 40.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SacredGold.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "Reading Session Summary" else "Okuma Oturumu Özeti",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            // Auto-track badge
                                            Surface(
                                                color = SacredGold.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == AppLanguage.EN) "Auto-Calculated" else "Otomatik Hesaplandı",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SacredGold,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (isSavedSuccessfully) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = if (lang == AppLanguage.EN) "Success" else "Başarılı",
                                                    tint = SacredGold
                                                )
                                                Text(
                                                    text = if (lang == AppLanguage.EN) "Your reading record was added successfully!" else "Okuma kaydınız başarıyla eklendi!",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        } else {
                                            // Show automated metrics in clean, beautiful badge rows
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Book details
                                                Text(
                                                    text = if (lang == AppLanguage.EN) {
                                                        "Surah: ${currentSelectedSurah?.nameEnglish ?: "Fatiha"} Surah"
                                                    } else {
                                                        "Sure: ${currentSelectedSurah?.nameTurkish ?: "Fatiha"} Suresi"
                                                    },
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )

                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                                // Page metric
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = SacredGold, modifier = Modifier.size(18.dp))
                                                        Text(
                                                            text = if (lang == AppLanguage.EN) "Pages Read" else "Okunan Sayfa",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = if (lang == AppLanguage.EN) "$autoPagesRead pages" else "$autoPagesRead sayfa",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // Duration metric
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = SacredGold, modifier = Modifier.size(18.dp))
                                                        Text(
                                                            text = if (lang == AppLanguage.EN) "Reading Time" else "Okuma Süresi",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = if (lang == AppLanguage.EN) "$autoMinutes min" else "$autoMinutes dk",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // Cumulative progress metric
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = SacredGold, modifier = Modifier.size(18.dp))
                                                        Text(
                                                            text = if (lang == AppLanguage.EN) "New Book Progress" else "Yeni Kitap İlerlemesi",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = "%$calculatedProgress",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SacredGold
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.updateReadingSessionProgress(
                                                        bookTitle = if (lang == AppLanguage.EN) "Holy Quran" else "Kur'an-ı Kerim",
                                                        subtitle = if (lang == AppLanguage.EN) {
                                                            "${currentSelectedSurah?.nameEnglish ?: "Fatiha"} Surah (Chapter ${currentSelectedSurah?.number ?: 1})"
                                                        } else {
                                                            "${currentSelectedSurah?.nameTurkish ?: "Fatiha"} Suresi (${currentSelectedSurah?.number ?: 1}. Bölüm)"
                                                        },
                                                        progress = calculatedProgress,
                                                        surahOrChapter = if (lang == AppLanguage.EN) {
                                                            "${currentSelectedSurah?.nameEnglish ?: "Fatiha"} Surah (Chapter ${currentSelectedSurah?.number ?: 1})"
                                                        } else {
                                                            "${currentSelectedSurah?.nameTurkish ?: "Fatiha"} Suresi (${currentSelectedSurah?.number ?: 1}. Bölüm)"
                                                        },
                                                        pagesRead = autoPagesRead,
                                                        isCompleted = isBottomReached.value,
                                                        contemplationMinutes = autoMinutes
                                                    )
                                                    isSavedSuccessfully = true
                                                },
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                                            ) {
                                                Text(
                                                    text = if (lang == AppLanguage.EN) "Save Reading Progress" else "Okuma İlerlemesini Kaydet",
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Persistent Audio Footer Control Bar
                    AnimatedVisibility(
                        visible = currentPlayingUrl != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(SacredGold.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Headphones,
                                            contentDescription = null,
                                            tint = SacredGold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.EN) {
                                                "${currentSelectedSurah?.nameEnglish} Surah"
                                            } else {
                                                "${currentSelectedSurah?.nameTurkish} Suresi"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.EN) {
                                                "Reading Verse ${activePlayingVerseIndex?.plus(1) ?: 1}..."
                                            } else {
                                                "Ayet ${activePlayingVerseIndex?.plus(1) ?: 1} okunuyor..."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Pause / Play
                                    IconButton(
                                        onClick = { viewModel.togglePlayPause() },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            imageVector = if (isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (lang == AppLanguage.EN) "Play/Pause" else "Oynat/Duraklat",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }

                                    // Stop
                                    IconButton(
                                        onClick = { viewModel.stopAudio() },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Stop,
                                            contentDescription = if (lang == AppLanguage.EN) "Stop" else "Durdur",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
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
}
