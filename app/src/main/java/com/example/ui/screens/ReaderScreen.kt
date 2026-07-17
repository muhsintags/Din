package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.Book
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.AppThemeSetting
import com.example.ui.viewmodel.FontFamilySetting
import com.example.ui.viewmodel.LineHeightSetting
import com.example.ui.viewmodel.ScriptureViewModel
import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ScriptureViewModel,
    book: Book,
    onNavigateBack: () -> Unit
) {
    if (book.id == "quran") {
        QuranReaderView(viewModel = viewModel, onNavigateBack = onNavigateBack)
        return
    }

    if (book.id == "torah" || book.id == "sermon") {
        BibleReaderView(viewModel = viewModel, book = book, onNavigateBack = onNavigateBack)
        return
    }

    val activeBookContentState by viewModel.activeBookContent.collectAsState()
    val book = activeBookContentState ?: book
    val isBookLoading by viewModel.isBookLoading.collectAsState()
    val bookError by viewModel.bookError.collectAsState()

    LaunchedEffect(book.id) {
        viewModel.loadBookContent(book)
    }

    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showSettingsPanel by remember { mutableStateOf(false) }
    var languageMode by remember { mutableStateOf("turkish") } // "turkish", "original", "bilingual"
    var isBookmarked by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteQuoteText by remember { mutableStateOf("") }
    var noteTextQuery by remember { mutableStateOf("") }
    var aiQueryText by remember { mutableStateOf("") }

    // Scroll progress line calculations
    val scrollProgress = remember {
        derivedStateOf {
            if (listState.layoutInfo.totalItemsCount == 0) 0f
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                (lastVisible.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Add Note or Highlight" else "Not veya İşaretleme Ekle",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Selected Verse/Passage:" else "Seçilen Ayet/Pasaj:",
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
                            .testTag("note_input_field"),
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
                                bookTitle = book.title,
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
                                bookTitle = book.title,
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

    // Set Active Book inside ViewModel for progress tracking
    LaunchedEffect(book) {
        viewModel.setActiveBook(book)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = book.title,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = if (lang == AppLanguage.EN) "Back" else "Geri",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isBookmarked = !isBookmarked }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (lang == AppLanguage.EN) "Bookmark" else "Yer İmi",
                                tint = if (isBookmarked) SacredGold else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showSettingsPanel = !showSettingsPanel }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = Loc.get("appearance_settings", lang),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // Reading progress indicator line
                LinearProgressIndicator(
                    progress = { scrollProgress.value },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Contextual FAB for notes taking
            ExtendedFloatingActionButton(
                onClick = {
                    noteQuoteText = book.paragraphs.firstOrNull() ?: ""
                    showAddNoteDialog = true
                },
                icon = { Icon(Icons.Filled.EditNote, if (lang == AppLanguage.EN) "Add Note" else "Not Ekle") },
                text = { Text(if (lang == AppLanguage.EN) "Quick Note" else "Hızlı Not Al") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("reader_quick_note_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 120.dp)
            ) {
                // Metadata Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = book.subContentTitle.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = SacredGold,
                            letterSpacing = 3.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = book.contentTitle,
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
                        if (book.introText.isNotEmpty()) {
                            Text(
                                text = book.introText,
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

                // Polished Control Panel Card (Downloads, Language Switcher, Audio Player)
                item {
                    val downloadedBooks by viewModel.downloadedBooks.collectAsState()
                    val downloadProgress by viewModel.downloadProgress.collectAsState()
                    val isDownloaded = downloadedBooks.contains(book.id)
                    val progress = downloadProgress[book.id]

                    val currentPlayingUrl by viewModel.currentPlayingUrl.collectAsState()
                    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
                    val isAudioLoading by viewModel.isAudioLoading.collectAsState()

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
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. LANGUAGE MODE ROW (Only if original paragraphs exist)
                            if (book.originalParagraphs.isNotEmpty()) {
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
                                            "turkish" to (if (lang == AppLanguage.EN) "English" else "Türkçe"),
                                            "original" to book.originalLanguageName,
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
                                                        fontSize = 12.sp,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }

                            // 2. AUDIO PLAYER ROW
                            if (book.audioUrl.isNotEmpty()) {
                                val isCurrentBookPlaying = currentPlayingUrl == book.audioUrl
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurrentBookPlaying && isAudioPlaying) {
                                                        SacredGold.copy(alpha = 0.2f)
                                                    } else {
                                                        MaterialTheme.colorScheme.secondaryContainer
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentBookPlaying && isAudioPlaying) Icons.Filled.Headphones else Icons.Filled.PlayLesson,
                                                contentDescription = null,
                                                tint = if (isCurrentBookPlaying && isAudioPlaying) SacredGold else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "Audiobook Narrator" else "Sesli Kitap Okuyucu",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isCurrentBookPlaying) {
                                                    if (isAudioPlaying) {
                                                        if (lang == AppLanguage.EN) "Beautifully narrated audiobook is playing..." else "Muhteşem tonlu seslendirme çalınıyor..."
                                                    } else {
                                                        if (lang == AppLanguage.EN) "Narration paused." else "Seslendirme duraklatıldı."
                                                    }
                                                } else {
                                                    if (lang == AppLanguage.EN) "Listen with a deep, resonant, and peaceful voice." else "Derin, tok ve huzur veren erkek sesiyle dinleyin."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (isCurrentBookPlaying && isAudioLoading) {
                                            CircularProgressIndicator(
                                                color = SacredGold,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    if (isCurrentBookPlaying) {
                                                        viewModel.togglePlayPause()
                                                    } else {
                                                        viewModel.playAudio(book.audioUrl, -1)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCurrentBookPlaying && isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = if (lang == AppLanguage.EN) "Listen" else "Dinle",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            if (isCurrentBookPlaying) {
                                                IconButton(
                                                    onClick = { viewModel.stopAudio() },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Stop,
                                                        contentDescription = if (lang == AppLanguage.EN) "Stop" else "Durdur",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }

                            // 3. OFFLINE DOWNLOAD MODULE ROW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                                        contentDescription = null,
                                        tint = if (isDownloaded) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isDownloaded) {
                                                if (lang == AppLanguage.EN) "Offline Mode Active" else "Çevrimdışı Mod Aktif"
                                            } else {
                                                if (lang == AppLanguage.EN) "Offline Download Module" else "Çevrimdışı İndirme Modülü"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (progress != null) {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "Downloading: %${(progress * 100).toInt()}" else "İndiriliyor: %${(progress * 100).toInt()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                text = if (isDownloaded) {
                                                    if (lang == AppLanguage.EN) "All texts and audio are ready offline on your device!" else "Tüm metin ve sesler cihazınızda çevrimdışı hazır!"
                                                } else {
                                                    if (lang == AppLanguage.EN) "Download to listen to texts and narration without internet." else "Metinleri ve seslendirmeyi internetsiz dinlemek için indirin."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (progress != null) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        color = SacredGold,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (isDownloaded) {
                                    IconButton(
                                        onClick = { viewModel.deleteBookDownload(book.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = if (lang == AppLanguage.EN) "Delete Download" else "İndirmeyi Sil",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.downloadBook(book.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (lang == AppLanguage.EN) "Download" else "İndir", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            if (progress != null) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SacredGold,
                                    trackColor = Color.Transparent
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            val isAiLoaded = book.contentTitle != (com.example.data.model.BookRepository.books.firstOrNull { it.id == book.id }?.contentTitle ?: "")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isAiLoaded) Icons.Filled.AutoAwesome else Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(20.dp)
                                      )
                                      Column {
                                          Text(
                                              text = if (isAiLoaded) {
                                                  if (lang == AppLanguage.EN) "Live & Offline Academic Resource Active" else "Canlı & Çevrimdışı Akademik Kaynak Aktif"
                                              } else {
                                                  if (lang == AppLanguage.EN) "Matched with Local Resource" else "Yerel Kaynak ile Eşleşti"
                                              },
                                              style = MaterialTheme.typography.titleSmall,
                                              fontWeight = FontWeight.Bold,
                                              color = MaterialTheme.colorScheme.onSurface
                                          )
                                          Text(
                                              text = if (isAiLoaded) {
                                                  "${book.title} • ${book.contentTitle} (${book.originalLanguageName})"
                                              } else {
                                                  when (book.id) {
                                                      "quran" -> if (lang == AppLanguage.EN) "Holy Quran (Surah Al-Fath • English & Arabic)" else "Kur'an-ı Kerim (Fetih Suresi • Türkçe & Arapça)"
                                                      "torah" -> if (lang == AppLanguage.EN) "Torah (Genesis 1-3 • English & Hebrew)" else "Tevrat (Yaratılış 1-3 • Türkçe & İbranice)"
                                                      "sermon" -> if (lang == AppLanguage.EN) "Gospel (Sermon on the Mount • English & Greek)" else "İncil (Dağdaki Vaaz • Türkçe & Grekçe)"
                                                      else -> if (lang == AppLanguage.EN) "Embedded Offline Text (English & Original Language)" else "Gömülü Çevrimdışı Metin (Türkçe & Orijinal Dil)"
                                                  }
                                              },
                                              style = MaterialTheme.typography.bodySmall,
                                              color = MaterialTheme.colorScheme.onSurfaceVariant
                                          )
                                      }
                                  }
                                  Box(
                                      modifier = Modifier
                                          .clip(RoundedCornerShape(4.dp))
                                          .background(
                                              if (isAiLoaded) {
                                                  SacredGold.copy(alpha = 0.2f)
                                              } else {
                                                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                              }
                                          )
                                          .padding(horizontal = 8.dp, vertical = 4.dp)
                                  ) {
                                      Text(
                                          text = if (isAiLoaded) {
                                              if (lang == AppLanguage.EN) "LIVE/ARCHIVE" else "CANLI/ARŞİV"
                                          } else {
                                              if (lang == AppLanguage.EN) "EMBEDDED" else "GÖMÜLÜ"
                                          },
                                          style = MaterialTheme.typography.labelSmall,
                                          color = if (isAiLoaded) SacredGold else MaterialTheme.colorScheme.primary,
                                          fontWeight = FontWeight.Bold
                                      )
                                  }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.EN) "Live Library & Archive Fetch (Free)" else "Canlı Kütüphane & Arşivden Getir (Bedava)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (lang == AppLanguage.EN) "Load any section of the sacred scriptures including the 24 volumes of Torah (Sefaria), Bible (Bible API), and the Holy Quran completely free, without API keys, instantly in English & Original language." else "24 cilt Tevrat (Sefaria), İncil (Bible API) ve Kur'an-ı Kerim dahil kütüphanedeki kutsal metinlerin dilediğiniz bölümünü tamamen ücretsiz, anahtarsız ve sınırsız olarak anında Türkçe & Orijinal dilinde yükleyin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = aiQueryText,
                                    onValueChange = { aiQueryText = it },
                                    label = { Text(if (lang == AppLanguage.EN) "Chapter or Subject Title" else "Bölüm veya Konu Başlığı") },
                                    placeholder = {
                                        Text(
                                            when (book.id) {
                                                "torah" -> if (lang == AppLanguage.EN) "e.g., Genesis 1, Exodus 20, Psalms 23..." else "Örn: Yaratılış 1, Çıkış 20, Mezmurlar 23..."
                                                "sermon" -> if (lang == AppLanguage.EN) "e.g., Matthew 5, Matthew 6, Matthew 7..." else "Örn: Matta 5, Matta 6, Matta 7..."
                                                else -> if (lang == AppLanguage.EN) "e.g., Chapter 1..." else "Örn: Bölüm 1..."
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        if (aiQueryText.isNotEmpty()) {
                                            IconButton(onClick = { aiQueryText = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = if (lang == AppLanguage.EN) "Clear" else "Temizle")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                 // Complete books list for quick access
                                 val allBooksOfCategory = when (book.id) {
                                     "torah" -> {
                                         if (lang == AppLanguage.EN) {
                                             listOf(
                                                 "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
                                                 "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah", "Esther", "Job", "Psalms",
                                                 "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel",
                                                 "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi"
                                             )
                                         } else {
                                             listOf(
                                                 "Yaratılış", "Çıkış", "Levililer", "Sayılar", "Yasanın Tekrarı",
                                                 "Yeşu", "Hakimler", "Rut", "1. Samuel", "2. Samuel",
                                                 "1. Krallar", "2. Krallar", "1. Tarihler", "2. Tarihler",
                                                 "Ezra", "Nehemya", "Ester", "Eyüp", "Mezmurlar",
                                                 "Özdeyişler", "Vaiz", "Ezgi", "Yeşaya", "Yeremya",
                                                 "Ağıtlar", "Hezekiel", "Daniel", "Hoşea", "Yoel",
                                                 "Amos", "Obadya", "Yunus", "Mika", "Nahum",
                                                 "Habakkuk", "Sefanya", "Hagay", "Zekeriya", "Malaki"
                                             )
                                         }
                                     }
                                     "sermon" -> {
                                         if (lang == AppLanguage.EN) {
                                             listOf(
                                                 "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians", "Galatians",
                                                 "Ephesians", "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus",
                                                 "Philemon", "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
                                             )
                                         } else {
                                             listOf(
                                                 "Matta", "Markos", "Luka", "Yuhanna", "Elçilerin İşleri",
                                                 "Romalılar", "1. Korintliler", "2. Korintliler", "Galatyalılar",
                                                 "Efesliler", "Filipililer", "Koloseliler", "1. Selanikliler",
                                                 "2. Selanikliler", "1. Timoteyus", "2. Timoteyus", "Titus",
                                                 "Filimon", "İbraniler", "Yakup", "1. Petrus", "2. Petrus",
                                                 "1. Yuhanna", "2. Yuhanna", "3. Yuhanna", "Yahuda", "Vahiy"
                                             )
                                         }
                                     }
                                     else -> emptyList()
                                 }

                                 if (allBooksOfCategory.isNotEmpty()) {
                                     Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                         Text(
                                             text = if (lang == AppLanguage.EN) "Quick Book Selection (${allBooksOfCategory.size} Books):" else "Hızlı Kitap Seçimi (${allBooksOfCategory.size} Kitap):",
                                             style = MaterialTheme.typography.labelSmall,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                         )
                                         LazyRow(
                                             horizontalArrangement = Arrangement.spacedBy(6.dp),
                                             modifier = Modifier.fillMaxWidth()
                                         ) {
                                             items(allBooksOfCategory) { itemBook ->
                                                 SuggestionChip(
                                                     onClick = { aiQueryText = "$itemBook 1" },
                                                     label = { Text(itemBook, style = MaterialTheme.typography.labelSmall) }
                                                 )
                                             }
                                         }
                                     }
                                 }

                                // Quick suggestions chips
                                val suggestions = when (book.id) {
                                    "torah" -> if (lang == AppLanguage.EN) listOf("Genesis 1", "Psalms 23", "Exodus 20") else listOf("Yaratılış 1", "Mezmurlar 23", "Çıkış 20")
                                    "sermon" -> if (lang == AppLanguage.EN) listOf("Matthew 5", "Matthew 6", "Matthew 7") else listOf("Matta 5", "Matta 6", "Matta 7")
                                    else -> if (lang == AppLanguage.EN) listOf("Chapter 1", "Chapter 2") else listOf("Bölüm 1", "Bölüm 2")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suggestions.forEach { suggestion ->
                                        SuggestionChip(
                                            onClick = { aiQueryText = suggestion },
                                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (aiQueryText.isNotBlank()) {
                                            viewModel.loadBookContentWithAI(book.id, aiQueryText)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SacredGold,
                                        contentColor = Color.White
                                    ),
                                    enabled = aiQueryText.isNotBlank() && !isBookLoading,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isBookLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (lang == AppLanguage.EN) "Fetching Chapter..." else "Bölüm Getiriliyor...")
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (lang == AppLanguage.EN) "Load Chapter Live/Archive (Free)" else "Bölümü Canlı/Arşivden Yükle (Ücretsiz)")
                                    }
                                }

                                if (bookError != null) {
                                    Text(
                                        text = "${if (lang == AppLanguage.EN) "Error" else "Hata"}: $bookError",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Core Scripture Paragraphs
                items(book.paragraphs.size) { index ->
                    val paragraphText = book.paragraphs[index]
                    val originalText = book.originalParagraphs.getOrNull(index) ?: ""
                    val isFirst = index == 0

                    val customTextSize = readerSettings.fontSizeSp.sp
                    val customLineHeight = (readerSettings.fontSizeSp * readerSettings.lineHeight.value).sp
                    val customFontFamily = if (readerSettings.fontFamily == FontFamilySetting.SERIF) FontFamily.Serif else FontFamily.SansSerif

                    val isRtl = book.originalLanguageName.contains("İbranice") ||
                                book.originalLanguageName.contains("Arapça") ||
                                book.originalLanguageName.contains("Hebrew") ||
                                book.originalLanguageName.contains("Arabic")
                    val origTextAlign = if (isRtl) TextAlign.Right else TextAlign.Left

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                noteQuoteText = if (languageMode == "original" && originalText.isNotEmpty()) originalText else paragraphText
                                showAddNoteDialog = true
                            }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (languageMode) {
                                "original" -> {
                                    if (originalText.isNotEmpty()) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "${index + 1}  ",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                                            )
                                            Text(
                                                text = originalText,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = (readerSettings.fontSizeSp + 1).sp,
                                                lineHeight = (readerSettings.fontSizeSp * 1.5).sp,
                                                color = SacredGold,
                                                textAlign = origTextAlign,
                                                fontStyle = FontStyle.Italic,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                "bilingual" -> {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "${index + 1}  ",
                                            fontFamily = FontFamily.SansSerif,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Turkish Text
                                            Text(
                                                text = paragraphText,
                                                fontFamily = customFontFamily,
                                                fontSize = customTextSize,
                                                lineHeight = customLineHeight,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            // Original Text Box
                                            if (originalText.isNotEmpty()) {
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                    ),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = originalText,
                                                        fontFamily = FontFamily.Serif,
                                                        fontSize = (readerSettings.fontSizeSp - 1).sp,
                                                        lineHeight = (readerSettings.fontSizeSp * 1.4).sp,
                                                        color = SacredGold,
                                                        textAlign = origTextAlign,
                                                        fontStyle = FontStyle.Italic,
                                                        modifier = Modifier.padding(8.dp).fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // standard turkish mode
                                    if (isFirst && paragraphText.isNotEmpty()) {
                                        val firstChar = paragraphText.take(1)
                                        val restOfText = paragraphText.drop(1)

                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 12.dp, top = 4.dp)
                                                    .background(Color.Transparent),
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                Text(
                                                    text = firstChar,
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (readerSettings.fontSizeSp * 2.8).sp,
                                                    lineHeight = (readerSettings.fontSizeSp * 2.4).sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.align(Alignment.TopCenter)
                                                )
                                            }
                                            Text(
                                                text = restOfText,
                                                fontFamily = customFontFamily,
                                                fontSize = customTextSize,
                                                lineHeight = customLineHeight,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "${index + 1}  ",
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                                            )
                                            Text(
                                                text = paragraphText,
                                                fontFamily = customFontFamily,
                                                fontSize = customTextSize,
                                                lineHeight = customLineHeight,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footnotes section
                if (book.footnotes.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.EN) "Critical Notes" else "Kritik Notlar",
                                style = MaterialTheme.typography.labelLarge,
                                color = SacredGold,
                                letterSpacing = 1.sp
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                book.footnotes.forEach { (ref, note) ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = ref,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(36.dp)
                                        )
                                        Text(
                                            text = note,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp,
                                                fontStyle = FontStyle.Italic
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Appearance Customization Slider Sheet
            AnimatedVisibility(
                visible = showSettingsPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Loc.get("appearance_settings", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { showSettingsPanel = false }) {
                                Icon(Icons.Filled.Close, contentDescription = if (lang == AppLanguage.EN) "Close" else "Kapat")
                            }
                        }

                        // Theme Options
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = Loc.get("theme", lang)?.uppercase() ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf(
                                    Triple(if (lang == AppLanguage.EN) "Light" else "Aydınlık", AppThemeSetting.LIGHT, Color.White),
                                    Triple(if (lang == AppLanguage.EN) "Dark" else "Karanlık", AppThemeSetting.DARK, Color(0xFF1E2120)),
                                    Triple(if (lang == AppLanguage.EN) "Sepia" else "Antik", AppThemeSetting.SEPIA, Color(0xFFF4ECD8))
                                )
                                themes.forEach { (label, setting, bg) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .border(
                                                width = if (readerSettings.theme == setting) 2.dp else 1.dp,
                                                color = if (readerSettings.theme == setting) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.updateThemeSetting(setting) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (setting == AppThemeSetting.DARK) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Text Size Slider
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "FONT SIZE" else "METİN BOYUTU",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${readerSettings.fontSizeSp.toInt()}px",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                              ) {
                                  Icon(
                                      imageVector = Icons.Filled.FormatSize,
                                      contentDescription = null,
                                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                      modifier = Modifier.size(16.dp)
                                  )
                                  Slider(
                                      value = readerSettings.fontSizeSp,
                                      onValueChange = { viewModel.updateFontSize(it) },
                                      valueRange = 14f..32f,
                                      colors = SliderDefaults.colors(
                                          thumbColor = MaterialTheme.colorScheme.primary,
                                          activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                      ),
                                      modifier = Modifier
                                          .weight(1f)
                                          .testTag("font_size_slider")
                                  )
                                  Icon(
                                      imageVector = Icons.Filled.FormatSize,
                                      contentDescription = null,
                                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                      modifier = Modifier.size(24.dp)
                                  )
                              }
                          }

                          // Font Family Option
                          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                              Text(
                                  text = if (lang == AppLanguage.EN) "FONT FAMILY" else "YAZI TİPİ",
                                  style = MaterialTheme.typography.labelMedium,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                              Row(
                                  modifier = Modifier.fillMaxWidth(),
                                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                              ) {
                                  Card(
                                      modifier = Modifier
                                          .weight(1f)
                                          .clickable { viewModel.updateFontFamily(FontFamilySetting.SERIF) },
                                      colors = CardDefaults.cardColors(
                                          containerColor = if (readerSettings.fontFamily == FontFamilySetting.SERIF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                      ),
                                      shape = RoundedCornerShape(8.dp)
                                  ) {
                                      Box(
                                          modifier = Modifier.fillMaxWidth().height(48.dp),
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Text(
                                              text = if (lang == AppLanguage.EN) "Serif (Classic)" else "Serif (Klasik)",
                                              fontFamily = FontFamily.Serif,
                                              fontWeight = FontWeight.Bold,
                                              color = if (readerSettings.fontFamily == FontFamilySetting.SERIF) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                          )
                                      }
                                  }

                                  Card(
                                      modifier = Modifier
                                          .weight(1f)
                                          .clickable { viewModel.updateFontFamily(FontFamilySetting.SANS_SERIF) },
                                      colors = CardDefaults.cardColors(
                                          containerColor = if (readerSettings.fontFamily == FontFamilySetting.SANS_SERIF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                      ),
                                      shape = RoundedCornerShape(8.dp)
                                  ) {
                                      Box(
                                          modifier = Modifier.fillMaxWidth().height(48.dp),
                                          contentAlignment = Alignment.Center
                                      ) {
                                          Text(
                                              text = if (lang == AppLanguage.EN) "Sans-serif (Modern)" else "Sans-serif (Modern)",
                                              fontFamily = FontFamily.SansSerif,
                                              fontWeight = FontWeight.Bold,
                                              color = if (readerSettings.fontFamily == FontFamilySetting.SANS_SERIF) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                          )
                                      }
                                  }
                              }
                          }

                          // Line Spacing Option
                          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                              Text(
                                  text = if (lang == AppLanguage.EN) "LINE SPACING" else "SATIR ARALIĞI",
                                  style = MaterialTheme.typography.labelMedium,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant
                              )
                              Row(
                                  modifier = Modifier.fillMaxWidth(),
                                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                              ) {
                                  val lineHeights = listOf(
                                      (if (lang == AppLanguage.EN) "Tight" else "Dar") to LineHeightSetting.TIGHT,
                                      (if (lang == AppLanguage.EN) "Medium" else "Orta") to LineHeightSetting.NORMAL,
                                      (if (lang == AppLanguage.EN) "Wide" else "Geniş") to LineHeightSetting.WIDE
                                  )
                                  lineHeights.forEach { (label, setting) ->
                                      Card(
                                          modifier = Modifier
                                              .weight(1f)
                                              .clickable { viewModel.updateLineHeight(setting) },
                                          colors = CardDefaults.cardColors(
                                              containerColor = if (readerSettings.lineHeight == setting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                          ),
                                          shape = RoundedCornerShape(8.dp)
                                      ) {
                                          Box(
                                              modifier = Modifier.fillMaxWidth().height(44.dp),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Text(
                                                  text = label,
                                                  style = MaterialTheme.typography.labelLarge,
                                                  fontWeight = FontWeight.Bold,
                                                  color = if (readerSettings.lineHeight == setting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
  }
