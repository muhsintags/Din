package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import com.example.data.model.BibleBook
import com.example.data.model.BibleRepository
import com.example.data.model.Book
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.FontFamilySetting
import com.example.ui.viewmodel.ScriptureViewModel
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderView(
    viewModel: ScriptureViewModel,
    book: Book,
    onNavigateBack: () -> Unit
) {
    val isTorah = book.id == "torah"
    
    // Select correct state depending on which book we are in
    val currentSelectedBook by (if (isTorah) viewModel.currentSelectedTorahBook else viewModel.currentSelectedSermonBook).collectAsState()
    val currentSelectedChapter by (if (isTorah) viewModel.currentSelectedTorahChapter else viewModel.currentSelectedSermonChapter).collectAsState()
    
    val activeBookContentState by viewModel.activeBookContent.collectAsState()
    val activeBook = activeBookContentState ?: book
    val isBookLoading by viewModel.isBookLoading.collectAsState()
    val bookError by viewModel.bookError.collectAsState()
    
    val readerSettings by viewModel.readerSettings.collectAsState()
    val isEnglish = readerSettings.language == AppLanguage.EN
    val lang = readerSettings.language

    val currentPlayingUrl by viewModel.currentPlayingUrl.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val isAudioLoading by viewModel.isAudioLoading.collectAsState()
    val activePlayingVerseIndex by viewModel.activePlayingVerseIndex.collectAsState()
    val downloadedChapters by viewModel.downloadedChapters.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val chapterKey = currentSelectedBook?.let { "${if (isTorah) "torah" else "sermon"}_${it.id}_$currentSelectedChapter" } ?: ""
    val isChapterDownloaded = downloadedChapters.contains(chapterKey)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Automatic progress and timing tracker
    val startTime = remember(chapterKey) { System.currentTimeMillis() }
    var elapsedSeconds by remember(chapterKey) { mutableStateOf(0) }
    LaunchedEffect(chapterKey) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }
    val autoMinutes = (elapsedSeconds / 60).coerceAtLeast(0)

    val maxScrolledParagraphIndex = remember(chapterKey) { mutableStateOf(0) }
    val visibleIndices = remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }
        }
    }
    
    val paragraphsCount = activeBook?.paragraphs?.size ?: 1
    LaunchedEffect(visibleIndices.value, chapterKey) {
        val lastVisible = visibleIndices.value.lastOrNull() ?: 0
        // Item 0 is the download/header block.
        // Paragraphs start at item 1.
        val currentParagraphIdx = (lastVisible - 1).coerceIn(0, paragraphsCount - 1)
        if (currentParagraphIdx > maxScrolledParagraphIndex.value) {
            maxScrolledParagraphIndex.value = currentParagraphIdx
        }
    }

    val totalPagesOfChapter = (paragraphsCount / 4.0).coerceAtLeast(1.0).toInt()
    val autoPagesRead = ((maxScrolledParagraphIndex.value + 1).toFloat() / paragraphsCount.toFloat() * totalPagesOfChapter)
        .toInt().coerceIn(1, totalPagesOfChapter)

    // Check if user has scrolled to the bottom (lastVisible index is >= paragraphsCount + 1)
    val isBottomReached = remember(chapterKey) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= paragraphsCount + 1
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var languageMode by remember { mutableStateOf("turkish") } // "turkish", "original", "bilingual"
    var isBookmarked by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteQuoteText by remember { mutableStateOf("") }
    var noteTextQuery by remember { mutableStateOf("") }
    
    val booksList = if (isTorah) BibleRepository.torahBooks else BibleRepository.bibleBooks
    val filteredBooks = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            booksList
        } else {
            booksList.filter {
                it.nameTurkish.contains(searchQuery, ignoreCase = true) ||
                it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                it.bookNumber.toString() == searchQuery
            }
        }
    }
    
    // Dialog for adding notes
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Add Reflection or Note" else "Tefekkür veya Not Ekle",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Selected Passage:" else "Seçilen Pasaj:",
                        style = MaterialTheme.typography.labelLarge,
                        color = SacredGold
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = noteQuoteText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = noteTextQuery,
                        onValueChange = { noteTextQuery = it },
                        label = { Text(if (lang == AppLanguage.EN) "Your Reflection Note (Optional)" else "Tefekkür Notunuz (İsteğe Bağlı)") },
                        placeholder = { Text(if (lang == AppLanguage.EN) "Write your thoughts about this passage..." else "Bu pasaj hakkındaki düşüncelerinizi yazın...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("bible_note_input_field"),
                        singleLine = false,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.addNoteOrHighlight(
                                bookTitle = activeBook.title + " - " + activeBook.contentTitle,
                                quoteText = noteQuoteText,
                                noteText = null,
                                isHighlightOnly = true
                            )
                            showAddNoteDialog = false
                            noteTextQuery = ""
                        }
                    ) {
                        Text(if (lang == AppLanguage.EN) "Highlight Only" else "Yalnızca İşaretle", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            viewModel.addNoteOrHighlight(
                                bookTitle = activeBook.title + " - " + activeBook.contentTitle,
                                quoteText = noteQuoteText,
                                noteText = noteTextQuery,
                                isHighlightOnly = false
                            )
                            showAddNoteDialog = false
                            noteTextQuery = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (lang == AppLanguage.EN) "Save Note" else "Notu Kaydet")
                    }
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
                            text = if (currentSelectedBook != null && currentSelectedChapter != null) {
                                val name = if (lang == AppLanguage.EN) currentSelectedBook?.nameEnglish else currentSelectedBook?.nameTurkish
                                "$name $currentSelectedChapter"
                            } else if (currentSelectedBook != null) {
                                if (lang == AppLanguage.EN) currentSelectedBook!!.nameEnglish else currentSelectedBook!!.nameTurkish
                            } else {
                                if (isTorah) {
                                    if (lang == AppLanguage.EN) "Torah (Tanakh)" else "Tevrat (Tanah)"
                                } else {
                                    if (lang == AppLanguage.EN) "Gospel (Bible)" else "İncil (Ahit)"
                                }
                            },
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (currentSelectedBook != null) {
                            Text(
                                text = "${currentSelectedBook?.nameEnglish} • ${currentSelectedBook?.sourceLanguage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentSelectedChapter != null) {
                            if (isTorah) viewModel.selectTorahChapter(null) else viewModel.selectSermonChapter(null)
                        } else if (currentSelectedBook != null) {
                            if (isTorah) viewModel.selectTorahBook(null) else viewModel.selectSermonBook(null)
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
                    if (currentSelectedBook != null && currentSelectedChapter != null) {
                        IconButton(onClick = { isBookmarked = !isBookmarked }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (lang == AppLanguage.EN) "Bookmark" else "Yer İmi",
                                tint = if (isBookmarked) SacredGold else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (currentSelectedBook != null && currentSelectedChapter != null && !isBookLoading) {
                ExtendedFloatingActionButton(
                    onClick = {
                        noteQuoteText = activeBook.paragraphs.firstOrNull() ?: ""
                        showAddNoteDialog = true
                    },
                    icon = { Icon(Icons.Filled.EditNote, if (lang == AppLanguage.EN) "Add Note" else "Not Ekle") },
                    text = { Text(if (lang == AppLanguage.EN) "Quick Note" else "Hızlı Not Al") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentSelectedBook == null) {
                // BOOK INDEX VIEW (FİHRİST DÜZENİ)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isTorah) {
                            if (lang == AppLanguage.EN) "Live Torah Library" else "Canlı Tevrat Kütüphanesi"
                        } else {
                            if (lang == AppLanguage.EN) "Live Gospel Library" else "Canlı İncil Kütüphanesi"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SacredGold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = if (isTorah) {
                            if (lang == AppLanguage.EN) "Contemplate the 24 volumes of Hebrew scriptures and live academic translations with Sefaria API integration." else "Sefaria API entegrasyonu ile 24 ciltlik İbranice kutsal metinleri ve Kitab-ı Mukaddes dilindeki canlı akademik çevirileri tefekkür edin."
                        } else {
                            if (lang == AppLanguage.EN) "Read the teachings of Jesus Christ in their Greek originals and modern translations with Bible-API integration." else "Bible-API entegrasyonu ile Grekçe asılları ve yapay zekâ destekli klasik Türkçe çevirileri ile İsa Mesih'in öğretilerini okuyun."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (lang == AppLanguage.EN) "Search book name or number..." else "Kitap ismi veya numara ara...") },
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
                            .testTag("bible_book_search_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Book List
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(filteredBooks) { _, bibleBook ->
                            Card(
                                onClick = {
                                    if (isTorah) viewModel.selectTorahBook(bibleBook) else viewModel.selectSermonBook(bibleBook)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bible_book_card_${bibleBook.id}"),
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
                                    // Book Number badge
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = bibleBook.bookNumber.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Book names
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == AppLanguage.EN) bibleBook.nameEnglish else bibleBook.nameTurkish,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.EN) {
                                                "${bibleBook.nameTurkish} • ${bibleBook.sourceLanguage}"
                                            } else {
                                                "${bibleBook.nameEnglish} • ${bibleBook.sourceLanguage}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Chapters Count on Right
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${bibleBook.chaptersCount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SacredGold
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.EN) "Chapters" else "Bölüm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (currentSelectedChapter == null) {
                // CHAPTER FAST ACCESS GRID (HIZLI ERİŞİM ŞEMASI)
                val selectedBook = currentSelectedBook!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "${selectedBook.nameEnglish} Chapters" else "${selectedBook.nameTurkish} Bölümleri",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SacredGold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = if (lang == AppLanguage.EN) {
                            "Select the chapter you want to read to load live API integration and academic study text."
                        } else {
                            "Okumak istediğiniz bölümü seçerek canlı API entegrasyonu ve akademik tefekkür metnini yükleyin."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Chapter Grid / Flow Row for Fast Access
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (ch in 1..selectedBook.chaptersCount) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (isTorah) viewModel.selectTorahChapter(ch) else viewModel.selectSermonChapter(ch)
                                            }
                                            .testTag("chapter_button_$ch"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ch.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ACTIVE CHAPTER VIEW
                if (isBookLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = SacredGold)
                            Text(
                                text = if (lang == AppLanguage.EN) "Loading and translating chapter..." else "Bölüm yükleniyor ve çevriliyor...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (bookError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                                text = bookError!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = {
                                    val currentBook = currentSelectedBook
                                    val currentChapter = currentSelectedChapter
                                    if (currentBook != null && currentChapter != null) {
                                        if (isTorah) viewModel.selectTorahChapter(currentChapter) else viewModel.selectSermonChapter(currentChapter)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                            ) {
                                Text(if (lang == AppLanguage.EN) "Retry" else "Tekrar Dene")
                            }
                        }
                    }
                } else {
                    // SHOW RENDERED SCRIPTURE
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                    ) {
                        // Header info
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = activeBook.subContentTitle.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SacredGold,
                                    letterSpacing = 3.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = activeBook.contentTitle,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline)
                                )
                                if (activeBook.introText.isNotEmpty()) {
                                    Text(
                                        text = activeBook.introText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontSize = (readerSettings.fontSizeSp - 2).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Reading Mode & Audio Row (No prompt inputs)
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (activeBook.originalParagraphs.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "READING LANGUAGE" else "OKUMA DİLİ",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val modes = listOf(
                                                    "turkish" to (if (lang == AppLanguage.EN) "Turkish" else "Türkçe"),
                                                    "original" to activeBook.originalLanguageName,
                                                    "bilingual" to (if (lang == AppLanguage.EN) "Bilingual" else "İki Dilli")
                                                )
                                                modes.forEach { (mode, label) ->
                                                    val isSelected = languageMode == mode
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { languageMode = mode },
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                        ),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(36.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                                fontSize = 11.sp,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.padding(horizontal = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Divider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    // Offline Status & Download Actions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isChapterDownloaded) {
                                                    if (lang == AppLanguage.EN) "Available Offline" else "Çevrimdışı Kullanılabilir"
                                                } else {
                                                    if (lang == AppLanguage.EN) "Offline Reader" else "Çevrimdışı Okuma"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChapterDownloaded) SacredGold else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isChapterDownloaded) {
                                                    if (lang == AppLanguage.EN) "Read without an internet connection." else "İnternet bağlantısı olmadan okuyun."
                                                } else {
                                                    if (lang == AppLanguage.EN) "Download this chapter for offline study." else "Çevrimdışı ders çalışmak için bu bölümü indirin."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        val progress = downloadProgress[chapterKey]
                                        if (progress != null && progress < 1f) {
                                            CircularProgressIndicator(
                                                progress = { progress },
                                                color = SacredGold,
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            if (isChapterDownloaded) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteBibleChapterDownload(
                                                            if (isTorah) "torah" else "sermon",
                                                            currentSelectedBook!!.id,
                                                            currentSelectedChapter!!
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = if (lang == AppLanguage.EN) "Delete download" else "İndirmeyi sil",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.downloadBibleChapter(
                                                            if (isTorah) "torah" else "sermon",
                                                            currentSelectedBook!!,
                                                            currentSelectedChapter!!
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Download,
                                                        contentDescription = if (lang == AppLanguage.EN) "Download chapter" else "Bölümü indir",
                                                        tint = SacredGold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val realHumanAudioUrl = viewModel.getRealHumanAudioUrl(isTorah)
                                    if (realHumanAudioUrl != null) {
                                        Divider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SacredGold.copy(alpha = 0.08f))
                                                .clickable {
                                                    if (currentPlayingUrl == realHumanAudioUrl) {
                                                        viewModel.togglePlayPause()
                                                    } else {
                                                        viewModel.playAudio(realHumanAudioUrl, -1)
                                                    }
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(SacredGold),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (currentPlayingUrl == realHumanAudioUrl && isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = if (lang == AppLanguage.EN) "Listen to Real Human Voice" else "Gerçek İnsan Sesinden Dinle",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = if (lang == AppLanguage.EN) "Beautiful professional recitation (Full Chapter)" else "Harika ve huşu dolu profesyonel seslendirme (Tüm Bölüm)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (currentPlayingUrl == realHumanAudioUrl && isAudioLoading) {
                                                CircularProgressIndicator(
                                                    color = SacredGold,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Render Verses / Paragraphs
                        items(activeBook.paragraphs.size) { index ->
                            val paragraphText = activeBook.paragraphs[index]
                            val originalText = activeBook.originalParagraphs.getOrNull(index) ?: ""

                            val customTextSize = readerSettings.fontSizeSp.sp
                            val customLineHeight = (readerSettings.fontSizeSp * readerSettings.lineHeight.value).sp
                            val customFontFamily = if (readerSettings.fontFamily == FontFamilySetting.SERIF) FontFamily.Serif else FontFamily.SansSerif

                            val isRtl = activeBook.originalLanguageName.contains("İbranice") ||
                                        activeBook.originalLanguageName.contains("Hebrew")
                            val origTextAlign = if (isRtl) TextAlign.Right else TextAlign.Left

                            val isVersePlaying = activePlayingVerseIndex == index

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isVersePlaying) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        noteQuoteText = if (languageMode == "original" && originalText.isNotEmpty()) originalText else paragraphText
                                        showAddNoteDialog = true
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (isVersePlaying) 12.dp else 4.dp)
                                ) {
                                    // Action bar for the paragraph
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Paragraph Index Badge
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Actions: Note & Audio Play
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    noteQuoteText = if (languageMode == "original" && originalText.isNotEmpty()) originalText else paragraphText
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

                                            IconButton(
                                                onClick = {
                                                    if (isVersePlaying) {
                                                        if (isAudioPlaying) {
                                                            viewModel.togglePlayPause()
                                                        } else {
                                                            val audioUrl = viewModel.getBibleVerseAudioUrl(
                                                                text = paragraphText,
                                                                isEnglish = isEnglish
                                                            )
                                                            viewModel.playAudio(audioUrl, index)
                                                        }
                                                    } else {
                                                        val audioUrl = viewModel.getBibleVerseAudioUrl(
                                                            text = paragraphText,
                                                            isEnglish = isEnglish
                                                        )
                                                        viewModel.playAudio(audioUrl, index)
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

                                    // Render content texts
                                    when (languageMode) {
                                        "original" -> {
                                            if (originalText.isNotEmpty()) {
                                                Text(
                                                    text = if (originalText.contains(": ")) originalText.substringAfter(": ") else originalText,
                                                    fontSize = customTextSize,
                                                    lineHeight = customLineHeight,
                                                    fontFamily = customFontFamily,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = origTextAlign,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                        "bilingual" -> {
                                            if (originalText.isNotEmpty()) {
                                                Text(
                                                    text = if (originalText.contains(": ")) originalText.substringAfter(": ") else originalText,
                                                    fontSize = (readerSettings.fontSizeSp - 2).sp,
                                                    lineHeight = (customLineHeight.value - 4).sp,
                                                    fontFamily = customFontFamily,
                                                    color = SacredGold,
                                                    textAlign = origTextAlign,
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = paragraphText,
                                                fontSize = customTextSize,
                                                lineHeight = customLineHeight,
                                                fontFamily = customFontFamily,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        else -> { // "turkish"
                                            Text(
                                                text = paragraphText,
                                                fontSize = customTextSize,
                                                lineHeight = customLineHeight,
                                                fontFamily = customFontFamily,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    Divider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }

                        // Footnotes Section
                        if (activeBook.footnotes.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp, bottom = 48.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = SacredGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.EN) "EXPLANATIONS & FOOTNOTES" else "AÇIKLAMALAR & DİPNOTLAR",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    activeBook.footnotes.forEach { (ref, note) ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = ref,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = SacredGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 22.sp
                                            )
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }

                        // Okumayı Kaydet / Tamamla Block
                        item {
                            var pagesInput by remember { mutableStateOf(autoPagesRead.toString()) }
                            var contemplationInput by remember { mutableStateOf(autoMinutes.toString()) }
                            var isCompletedCheck by remember { mutableStateOf(isBottomReached.value) }
                            var isSavedSuccessfully by remember { mutableStateOf(false) }

                            var hasManuallyEditedPages by remember { mutableStateOf(false) }
                            var hasManuallyEditedMinutes by remember { mutableStateOf(false) }

                            LaunchedEffect(autoPagesRead) {
                                if (!hasManuallyEditedPages) {
                                    pagesInput = autoPagesRead.toString()
                                }
                            }
                            LaunchedEffect(autoMinutes) {
                                if (!hasManuallyEditedMinutes) {
                                    contemplationInput = autoMinutes.toString()
                                }
                            }
                            LaunchedEffect(isBottomReached.value) {
                                if (isBottomReached.value) {
                                    isCompletedCheck = true
                                }
                            }

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
                                            text = if (lang == AppLanguage.EN) "Add Reading Record" else "Okuma Kaydı Ekle",
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
                                                text = if (lang == AppLanguage.EN) "Auto-tracked" else "Otomatik Sayılıyor",
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
                                                text = if (lang == AppLanguage.EN) "Your reading record was successfully added!" else "Okuma kaydınız başarıyla eklendi!",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    } else {
                                        // Hangi bölüm okundu
                                        val displaySection = if (lang == AppLanguage.EN) {
                                            (currentSelectedBook?.nameEnglish ?: activeBook.title)
                                        } else {
                                            (currentSelectedBook?.nameTurkish ?: activeBook.title)
                                        }
                                        val displayChapter = currentSelectedChapter?.let { if (lang == AppLanguage.EN) " Chapter $it" else " Bölüm $it" } ?: ""
                                        Text(
                                            text = if (lang == AppLanguage.EN) "Chapter Read: $displaySection$displayChapter" else "Okunan Bölüm: $displaySection$displayChapter",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        // Kaç sayfa okundu (Input)
                                        OutlinedTextField(
                                            value = pagesInput,
                                            onValueChange = { 
                                                hasManuallyEditedPages = true
                                                pagesInput = it.filter { char -> char.isDigit() } 
                                            },
                                            label = { Text(if (lang == AppLanguage.EN) "How Many Pages Read?" else "Kaç Sayfa Okundu?") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SacredGold,
                                                focusedLabelColor = SacredGold
                                            )
                                        )

                                        // Kaç dakika tefekkür edildi (Input)
                                        OutlinedTextField(
                                            value = contemplationInput,
                                            onValueChange = { 
                                                hasManuallyEditedMinutes = true
                                                contemplationInput = it.filter { char -> char.isDigit() } 
                                            },
                                            label = { Text(if (lang == AppLanguage.EN) "How Many Minutes Read?" else "Kaç Dakika Okundu?") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = SacredGold,
                                                focusedLabelColor = SacredGold
                                            )
                                        )

                                        // Bölüm Tamamlandı mı?
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "Is Chapter Completed?" else "Bölüm Tamamlandı mı?",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Switch(
                                                checked = isCompletedCheck,
                                                onCheckedChange = { isCompletedCheck = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = SacredGold,
                                                    checkedTrackColor = SacredGold.copy(alpha = 0.5f)
                                                )
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val pagesNum = pagesInput.toIntOrNull() ?: 1
                                                val contemplationNum = contemplationInput.toIntOrNull() ?: 0
                                                viewModel.updateReadingSessionProgress(
                                                    bookTitle = if (lang == AppLanguage.EN) {
                                                        if (isTorah) "Torah" else "Gospel"
                                                    } else {
                                                        if (isTorah) "Tevrat" else "İncil"
                                                    },
                                                    subtitle = if (lang == AppLanguage.EN) {
                                                        "$displaySection$displayChapter ($pagesNum pages)"
                                                    } else {
                                                        "$displaySection$displayChapter ($pagesNum sayfa)"
                                                    },
                                                    progress = if (isCompletedCheck) 100 else 50,
                                                    surahOrChapter = "$displaySection$displayChapter",
                                                    pagesRead = pagesNum,
                                                    isCompleted = isCompletedCheck,
                                                    contemplationMinutes = contemplationNum
                                                )
                                                isSavedSuccessfully = true
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                                        ) {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "Save Reading and Contemplation" else "Okuma ve Tefekkürü Kaydet",
                                                color = Color.White
                                            )
                                        }
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
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
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
                                            "${currentSelectedBook?.nameEnglish ?: "Bible"} Book"
                                        } else {
                                            "${currentSelectedBook?.nameTurkish ?: "Kutsal Kitap"}"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (activePlayingVerseIndex == -1) {
                                            if (lang == AppLanguage.EN) {
                                                "Listening to Professional Human Recitation..."
                                            } else {
                                                "Profesyonel İnsan Sesinden Dinleniyor..."
                                            }
                                        } else {
                                            if (lang == AppLanguage.EN) {
                                                "Reading Verse ${activePlayingVerseIndex?.plus(1) ?: 1}..."
                                            } else {
                                                "Ayet ${activePlayingVerseIndex?.plus(1) ?: 1} okunuyor..."
                                            }
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
