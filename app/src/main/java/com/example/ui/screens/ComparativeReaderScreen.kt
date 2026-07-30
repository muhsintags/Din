package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BibleBook
import com.example.data.model.BibleRepository
import com.example.data.model.Book
import com.example.data.model.QuranRepository
import com.example.data.model.QuranSurah
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage
import com.example.ui.viewmodel.ScriptureViewModel
import kotlinx.coroutines.launch

enum class ComparisonLayoutMode {
    PARALLEL_CARDS, // Unified side-by-side verse cards (Best for Mobile)
    SIDE_BY_SIDE,   // Side-by-side scrollable columns
    STACKED_PANELS  // Stacked full-width panels
}

data class SlotConfig(
    val slotIndex: Int,
    var category: String, // "quran", "sermon", "torah", "bukhari", "gita", "talmud"
    var subBookId: String?, // e.g. "Matthew", "Genesis" (null for Quran)
    var chapterNumber: Int = 1,
    var langMode: String = "tr", // "tr", "original", "en"
    var loadedBook: Book? = null,
    var isLoading: Boolean = false,
    var error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparativeReaderScreen(
    viewModel: ScriptureViewModel,
    initialBook: Book? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language
    val surahs = remember { QuranRepository.surahs }

    var bookCountMode by remember { mutableIntStateOf(if (initialBook != null) 2 else 2) }
    var layoutMode by remember { mutableStateOf(ComparisonLayoutMode.PARALLEL_CARDS) }

    // Slot states
    var slot1 by remember {
        mutableStateOf(
            SlotConfig(
                slotIndex = 1,
                category = "quran",
                subBookId = null,
                chapterNumber = 1,
                langMode = "tr"
            )
        )
    }

    var slot2 by remember {
        mutableStateOf(
            SlotConfig(
                slotIndex = 2,
                category = "sermon",
                subBookId = "Matthew",
                chapterNumber = 5,
                langMode = "tr"
            )
        )
    }

    var slot3 by remember {
        mutableStateOf(
            SlotConfig(
                slotIndex = 3,
                category = "torah",
                subBookId = "Genesis",
                chapterNumber = 1,
                langMode = "tr"
            )
        )
    }

    // Helper to load content for a slot
    fun loadSlotContent(slotNumber: Int) {
        scope.launch {
            when (slotNumber) {
                1 -> {
                    slot1 = slot1.copy(isLoading = true, error = null)
                    try {
                        val book = viewModel.fetchComparativeSlotBook(
                            category = slot1.category,
                            subBookId = slot1.subBookId,
                            chapterNumber = slot1.chapterNumber
                        )
                        slot1 = slot1.copy(loadedBook = book, isLoading = false)
                    } catch (e: Exception) {
                        slot1 = slot1.copy(isLoading = false, error = e.localizedMessage ?: "Yüklenemedi")
                    }
                }
                2 -> {
                    slot2 = slot2.copy(isLoading = true, error = null)
                    try {
                        val book = viewModel.fetchComparativeSlotBook(
                            category = slot2.category,
                            subBookId = slot2.subBookId,
                            chapterNumber = slot2.chapterNumber
                        )
                        slot2 = slot2.copy(loadedBook = book, isLoading = false)
                    } catch (e: Exception) {
                        slot2 = slot2.copy(isLoading = false, error = e.localizedMessage ?: "Yüklenemedi")
                    }
                }
                3 -> {
                    slot3 = slot3.copy(isLoading = true, error = null)
                    try {
                        val book = viewModel.fetchComparativeSlotBook(
                            category = slot3.category,
                            subBookId = slot3.subBookId,
                            chapterNumber = slot3.chapterNumber
                        )
                        slot3 = slot3.copy(loadedBook = book, isLoading = false)
                    } catch (e: Exception) {
                        slot3 = slot3.copy(isLoading = false, error = e.localizedMessage ?: "Yüklenemedi")
                    }
                }
            }
        }
    }

    // Initial loading on launch
    LaunchedEffect(Unit) {
        loadSlotContent(1)
        loadSlotContent(2)
        loadSlotContent(3)
    }

    // Search, Scaling and Picker States
    var verseFilterQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) } // Default hidden for cleaner view
    var isSelectionPanelExpanded by remember { mutableStateOf(true) } // Collapsible selector panel
    var fontSizeSp by remember { mutableIntStateOf(16) } // Adjustable font size scale (12..28 sp)
    var showFontSizeControls by remember { mutableStateOf(false) }
    var slotPickerIndex by remember { mutableStateOf<Int?>(null) }
    var versePickerIndex by remember { mutableStateOf<Int?>(null) }

    // Add Note Dialog
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteQuoteText by remember { mutableStateOf("") }
    var noteTextQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    val slotColors = listOf(
        Color(0xFF2E7D32), // Emerald Green (Slot 1)
        SacredGold,         // Sacred Gold (Slot 2)
        Color(0xFF1E88E5)  // Sapphire Blue (Slot 3)
    )

    // Dialogue: Add Note
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Add Comparative Reflection" else "Karşılaştırmalı Not Ekle",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Selected Scripture Passage:" else "Seçili Karşılaştırmalı Pasaj:",
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
                        label = { Text(if (lang == AppLanguage.EN) "Your Reflection" else "Tefekkür Notunuz") },
                        placeholder = { Text(if (lang == AppLanguage.EN) "Write your thoughts comparing these verses..." else "Metinler arasındaki paralellikleri veya farkları not edin...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("comparative_note_input"),
                        singleLine = false,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = "${slot1.loadedBook?.contentTitle ?: "Metin 1"} & ${slot2.loadedBook?.contentTitle ?: "Metin 2"}"
                        viewModel.addNoteOrHighlight(
                            bookTitle = title,
                            quoteText = noteQuoteText,
                            noteText = noteTextQuery,
                            isHighlightOnly = noteTextQuery.isBlank()
                        )
                        showAddNoteDialog = false
                        noteTextQuery = ""
                        Toast.makeText(
                            context,
                            if (lang == AppLanguage.EN) "Note saved to profile!" else "Karşılaştırma notu profilinize kaydedildi!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (lang == AppLanguage.EN) "Save" else "Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text(if (lang == AppLanguage.EN) "Cancel" else "İptal")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Bottom Sheet: Verse Picker / Jump to Ayah
    if (versePickerIndex != null) {
        val targetSlotNum = versePickerIndex!!
        val targetSlot = when (targetSlotNum) {
            1 -> slot1
            2 -> slot2
            else -> slot3
        }
        val book = targetSlot.loadedBook
        val paragraphs = if (targetSlot.langMode == "original") {
            book?.originalParagraphs?.ifEmpty { book.paragraphs } ?: emptyList()
        } else {
            book?.paragraphs ?: emptyList()
        }

        var verseFilter by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { versePickerIndex = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = getSlotFormattedTitle(targetSlot, lang == AppLanguage.EN),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = slotColors[targetSlotNum - 1]
                        )
                        Text(
                            text = "${paragraphs.size} ${if (lang == AppLanguage.EN) "verses / passages" else "ayet / pasaj mevcut"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { versePickerIndex = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = verseFilter,
                    onValueChange = { verseFilter = it },
                    placeholder = { Text(if (lang == AppLanguage.EN) "Search verse number or text..." else "Ayet no veya kelime ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                val filteredIndexed = paragraphs.mapIndexed { idx, t -> idx to t }.filter { (idx, t) ->
                    verseFilter.isBlank() || t.contains(verseFilter, ignoreCase = true) || (idx + 1).toString() == verseFilter.trim()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredIndexed) { (pIdx, pText) ->
                        Card(
                            onClick = {
                                versePickerIndex = null
                                scope.launch {
                                    listState.animateScrollToItem(pIdx)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(slotColors[targetSlotNum - 1]),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${pIdx + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = pText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Bottom Sheet: Modern Multi-Tab Scripture / Book / Chapter Picker
    if (slotPickerIndex != null) {
        val targetSlotNum = slotPickerIndex!!
        val currentSlot = when (targetSlotNum) {
            1 -> slot1
            2 -> slot2
            else -> slot3
        }

        var selectedCategory by remember { mutableStateOf(currentSlot.category) }
        var selectedSubBookId by remember { mutableStateOf(currentSlot.subBookId) }
        var selectedChapterNum by remember { mutableIntStateOf(currentSlot.chapterNumber) }
        var searchQuery by remember { mutableStateOf("") }

        val categories = if (lang == AppLanguage.EN) listOf(
            "quran" to ("Holy Quran" to "🕌"),
            "sermon" to ("New Testament (Gospel)" to "⛪"),
            "torah" to ("Old Testament (Torah)" to "📜"),
            "bukhari" to ("Sahih al-Bukhari" to "📿"),
            "gita" to ("Bhagavad Gita" to "🕉️"),
            "talmud" to ("Talmud" to "🕯️")
        ) else listOf(
            "quran" to ("Kur'an-ı Kerim" to "🕌"),
            "sermon" to ("İncil (Yeni Ahit)" to "⛪"),
            "torah" to ("Tevrat (Eski Ahit)" to "📜"),
            "bukhari" to ("Sahih-i Buharî" to "📿"),
            "gita" to ("Bhagavad Gita" to "🕉️"),
            "talmud" to ("Talmud" to "🕯️")
        )

        ModalBottomSheet(
            onDismissRequest = { slotPickerIndex = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(slotColors[targetSlotNum - 1]),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$targetSlotNum",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column {
                            Text(
                                text = if (lang == AppLanguage.EN) "Select Scripture for Slot $targetSlotNum" else "Metin $targetSlotNum İçin Kutsal Eser Seçin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == AppLanguage.EN) "Set tradition, book/surah, and chapter" else "Kategori, Sûre/Kitap ve Bölüm belirleyin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { slotPickerIndex = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. Tradition Category Pills Row
                Text(
                    text = if (lang == AppLanguage.EN) "1. SACRED TRADITION & SCRIPTURE TYPE" else "1. KUTSAL GELENEK VE METİN TÜRÜ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SacredGold,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { (catId, pair) ->
                        val (catTitle, icon) = pair
                        val isSel = (selectedCategory == catId)
                        Surface(
                            onClick = {
                                selectedCategory = catId
                                selectedSubBookId = when (catId) {
                                    "sermon" -> "Matthew"
                                    "torah" -> "Genesis"
                                    "bukhari" -> BibleRepository.bukhariBooks.first().id
                                    "gita" -> BibleRepository.gitaBooks.first().id
                                    "talmud" -> BibleRepository.talmudBooks.first().id
                                    else -> null
                                }
                                selectedChapterNum = 1
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) slotColors[targetSlotNum - 1] else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shadowElevation = if (isSel) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(icon, fontSize = 14.sp)
                                Text(
                                    text = catTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 2. Search & List of Books / Surahs
                Text(
                    text = if (lang == AppLanguage.EN) {
                        if (selectedCategory == "quran") "2. SELECT SURAH & CHAPTER" else "2. SELECT BOOK & CHAPTER"
                    } else {
                        if (selectedCategory == "quran") "2. SÛRE VE BÖLÜM SEÇİN" else "2. KİTAP VE BÖLÜM SEÇİN"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SacredGold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (lang == AppLanguage.EN) {
                                if (selectedCategory == "quran") "Search Surah or verse (e.g. Mary, Baqarah, 19)..." else "Search book or chapter (e.g. Matthew, Genesis)..."
                            } else {
                                if (selectedCategory == "quran") "Sûre veya ayet ara (örn: Meryem, Bakara, 19)..." else "Kitap veya bölüm ara (örn: Matta, Yaratılış)..."
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SacredGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedCategory == "quran") {
                    // Quran Surahs List
                    val filteredSurahs = surahs.filter {
                        searchQuery.isBlank() ||
                                it.nameArabic.contains(searchQuery, ignoreCase = true) ||
                                it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                                it.nameTurkish.contains(searchQuery, ignoreCase = true) ||
                                it.number.toString() == searchQuery.trim()
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSurahs) { s ->
                            val isSel = (selectedChapterNum == s.number)
                            Card(
                                onClick = {
                                    selectedChapterNum = s.number
                                    when (targetSlotNum) {
                                        1 -> slot1 = slot1.copy(category = "quran", subBookId = null, chapterNumber = s.number)
                                        2 -> slot2 = slot2.copy(category = "quran", subBookId = null, chapterNumber = s.number)
                                        3 -> slot3 = slot3.copy(category = "quran", subBookId = null, chapterNumber = s.number)
                                    }
                                    loadSlotContent(targetSlotNum)
                                    slotPickerIndex = null
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) slotColors[targetSlotNum - 1].copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, slotColors[targetSlotNum - 1]) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSel) slotColors[targetSlotNum - 1] else MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${s.number}", color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Column {
                                            Text(
                                                text = if (lang == AppLanguage.EN) "${s.number}. ${s.nameEnglish}" else "${s.number}. ${s.nameEnglish} (${s.nameTurkish})",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = if (lang == AppLanguage.EN) "${s.ayahCount} verses • ${s.revelationType}" else "${s.ayahCount} ayet • ${s.revelationType}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(s.nameArabic, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SacredGold)
                                }
                            }
                        }
                    }
                } else {
                    // Bible/Torah/Gita/Bukhari/Talmud Books + Chapters
                    val booksList: List<BibleBook> = when (selectedCategory) {
                        "torah" -> BibleRepository.torahBooks
                        "sermon" -> BibleRepository.bibleBooks
                        "bukhari" -> BibleRepository.bukhariBooks
                        "gita" -> BibleRepository.gitaBooks
                        "talmud" -> BibleRepository.talmudBooks
                        else -> BibleRepository.bibleBooks
                    }

                    val filteredBooks = booksList.filter {
                        searchQuery.isBlank() ||
                                it.nameTurkish.contains(searchQuery, ignoreCase = true) ||
                                it.nameEnglish.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredBooks) { b ->
                            val isBookSel = (selectedSubBookId == b.id)
                            Card(
                                onClick = {
                                    selectedSubBookId = b.id
                                    selectedChapterNum = 1
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBookSel) slotColors[targetSlotNum - 1].copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isBookSel) androidx.compose.foundation.BorderStroke(1.5.dp, slotColors[targetSlotNum - 1]) else null,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.EN) b.nameEnglish else "${b.nameTurkish} (${b.nameEnglish})",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (lang == AppLanguage.EN) "${b.chaptersCount} Chapters • Source: ${b.sourceLanguage}" else "${b.chaptersCount} Bölüm • Kaynak: ${b.sourceLanguage}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isBookSel) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = slotColors[targetSlotNum - 1])
                                    }
                                }
                            }
                        }
                    }

                    val selectedBookObj = booksList.find { it.id == selectedSubBookId } ?: booksList.first()
                    val selectedBookName = if (lang == AppLanguage.EN) selectedBookObj.nameEnglish else selectedBookObj.nameTurkish
                    Text(
                        text = if (lang == AppLanguage.EN) "3. SELECT CHAPTER ($selectedBookName: 1 - ${selectedBookObj.chaptersCount}):" else "3. BÖLÜM SEÇİN ($selectedBookName: 1 - ${selectedBookObj.chaptersCount}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SacredGold,
                        letterSpacing = 1.sp
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 46.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items((1..selectedBookObj.chaptersCount).toList()) { ch ->
                            val isChSel = (selectedChapterNum == ch)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChSel) slotColors[targetSlotNum - 1] else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        selectedChapterNum = ch
                                        when (targetSlotNum) {
                                            1 -> slot1 = slot1.copy(category = selectedCategory, subBookId = selectedSubBookId, chapterNumber = ch)
                                            2 -> slot2 = slot2.copy(category = selectedCategory, subBookId = selectedSubBookId, chapterNumber = ch)
                                            3 -> slot3 = slot3.copy(category = selectedCategory, subBookId = selectedSubBookId, chapterNumber = ch)
                                        }
                                        loadSlotContent(targetSlotNum)
                                        slotPickerIndex = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$ch",
                                    color = if (isChSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (lang == AppLanguage.EN) "Comparative Scripture Reader" else "Karşılaştırmalı Okuma",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (lang == AppLanguage.EN) {
                                if (bookCountMode == 2) "2-Text Dual Analysis" else "3-Text Triple Analysis"
                            } else {
                                if (bookCountMode == 2) "2 Metin İkili Analiz" else "3 Metin Üçlü Analiz"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = SacredGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSearchVisible) SacredGold.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchVisible) SacredGold else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showFontSizeControls = !showFontSizeControls },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (showFontSizeControls) SacredGold.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Font Scaling",
                            tint = if (showFontSizeControls) SacredGold else MaterialTheme.colorScheme.primary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // CONTROL & SELECTOR PANEL
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Header bar with Mode Toggle, Layout Selector, and Panel Collapse/Expand Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 2 Metin vs 3 Metin Pills
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf(
                                2 to (if (lang == AppLanguage.EN) "2 Texts" else "2 Metin"),
                                3 to (if (lang == AppLanguage.EN) "3 Texts" else "3 Metin")
                            ).forEach { (count, label) ->
                                val isSel = (bookCountMode == count)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            bookCountMode = count
                                            if (count == 3 && slot3.loadedBook == null) {
                                                loadSlotContent(3)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Layout mode & Expand/Collapse Toggle
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Layout mode icons group
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { layoutMode = ComparisonLayoutMode.PARALLEL_CARDS },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (layoutMode == ComparisonLayoutMode.PARALLEL_CARDS) SacredGold.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewAgenda,
                                        contentDescription = "Paralel Kartlar",
                                        tint = if (layoutMode == ComparisonLayoutMode.PARALLEL_CARDS) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { layoutMode = ComparisonLayoutMode.SIDE_BY_SIDE },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (layoutMode == ComparisonLayoutMode.SIDE_BY_SIDE) SacredGold.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewColumn,
                                        contentDescription = "Sütun Yan Yana",
                                        tint = if (layoutMode == ComparisonLayoutMode.SIDE_BY_SIDE) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { layoutMode = ComparisonLayoutMode.STACKED_PANELS },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (layoutMode == ComparisonLayoutMode.STACKED_PANELS) SacredGold.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewStream,
                                        contentDescription = "Üst Üste",
                                        tint = if (layoutMode == ComparisonLayoutMode.STACKED_PANELS) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Panel Expand/Collapse Button
                            Surface(
                                onClick = { isSelectionPanelExpanded = !isSelectionPanelExpanded },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isSelectionPanelExpanded) SacredGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (!isSelectionPanelExpanded) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelectionPanelExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (!isSelectionPanelExpanded) SacredGold else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.EN) {
                                            if (isSelectionPanelExpanded) "Hide" else "Selection Panel"
                                        } else {
                                            if (isSelectionPanelExpanded) "Gizle" else "Seçim Paneli"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSelectionPanelExpanded) SacredGold else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Row 2A: EXPANDED SELECTOR CARDS FOR EACH SLOT
                    AnimatedVisibility(
                        visible = isSelectionPanelExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InteractiveSlotHeaderCard(
                                slotNum = 1,
                                slot = slot1,
                                color = slotColors[0],
                                isEn = (lang == AppLanguage.EN),
                                onSelectClick = { slotPickerIndex = 1 },
                                onVerseClick = { versePickerIndex = 1 },
                                onLangToggle = {
                                    slot1 = slot1.copy(
                                        langMode = when (slot1.langMode) {
                                            "tr" -> "original"
                                            "original" -> "en"
                                            else -> "tr"
                                        }
                                    )
                                },
                                onPrevChapter = {
                                    if (slot1.chapterNumber > 1) {
                                        slot1 = slot1.copy(chapterNumber = slot1.chapterNumber - 1)
                                        loadSlotContent(1)
                                    }
                                },
                                onNextChapter = {
                                    slot1 = slot1.copy(chapterNumber = slot1.chapterNumber + 1)
                                    loadSlotContent(1)
                                }
                            )

                            InteractiveSlotHeaderCard(
                                slotNum = 2,
                                slot = slot2,
                                color = slotColors[1],
                                isEn = (lang == AppLanguage.EN),
                                onSelectClick = { slotPickerIndex = 2 },
                                onVerseClick = { versePickerIndex = 2 },
                                onLangToggle = {
                                    slot2 = slot2.copy(
                                        langMode = when (slot2.langMode) {
                                            "tr" -> "original"
                                            "original" -> "en"
                                            else -> "tr"
                                        }
                                    )
                                },
                                onPrevChapter = {
                                    if (slot2.chapterNumber > 1) {
                                        slot2 = slot2.copy(chapterNumber = slot2.chapterNumber - 1)
                                        loadSlotContent(2)
                                    }
                                },
                                onNextChapter = {
                                    slot2 = slot2.copy(chapterNumber = slot2.chapterNumber + 1)
                                    loadSlotContent(2)
                                }
                            )

                            if (bookCountMode == 3) {
                                InteractiveSlotHeaderCard(
                                    slotNum = 3,
                                    slot = slot3,
                                    color = slotColors[2],
                                    isEn = (lang == AppLanguage.EN),
                                    onSelectClick = { slotPickerIndex = 3 },
                                    onVerseClick = { versePickerIndex = 3 },
                                    onLangToggle = {
                                        slot3 = slot3.copy(
                                            langMode = when (slot3.langMode) {
                                                "tr" -> "original"
                                                "original" -> "en"
                                                else -> "tr"
                                            }
                                        )
                                    },
                                    onPrevChapter = {
                                        if (slot3.chapterNumber > 1) {
                                            slot3 = slot3.copy(chapterNumber = slot3.chapterNumber - 1)
                                            loadSlotContent(3)
                                        }
                                    },
                                    onNextChapter = {
                                        slot3 = slot3.copy(chapterNumber = slot3.chapterNumber + 1)
                                        loadSlotContent(3)
                                    }
                                )
                            }
                        }
                    }

                    // Row 2B: COLLAPSED VIEW - ULTRA-SLEEK COMPACT BADGES
                    AnimatedVisibility(
                        visible = !isSelectionPanelExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactAcademicSlotBadge(
                                slotNum = 1,
                                slot = slot1,
                                color = slotColors[0],
                                isEn = (lang == AppLanguage.EN),
                                onClick = { slotPickerIndex = 1 }
                            )
                            CompactAcademicSlotBadge(
                                slotNum = 2,
                                slot = slot2,
                                color = slotColors[1],
                                isEn = (lang == AppLanguage.EN),
                                onClick = { slotPickerIndex = 2 }
                            )
                            if (bookCountMode == 3) {
                                CompactAcademicSlotBadge(
                                    slotNum = 3,
                                    slot = slot3,
                                    color = slotColors[2],
                                    isEn = (lang == AppLanguage.EN),
                                    onClick = { slotPickerIndex = 3 }
                                )
                            }
                        }
                    }

                    // Font Scaling Control Panel (Animate in/out)
                    AnimatedVisibility(
                        visible = showFontSizeControls,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null,
                                        tint = SacredGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.EN) "Text Size:" else "Metin Boyutu:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${fontSizeSp} sp",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SacredGold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (fontSizeSp > 12) fontSizeSp -= 1 },
                                        enabled = fontSizeSp > 12,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Küçült",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    listOf(13, 16, 20, 24).forEach { size ->
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (fontSizeSp == size) SacredGold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .clickable { fontSizeSp = size }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$size",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (fontSizeSp == size) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { if (fontSizeSp < 28) fontSizeSp += 1 },
                                        enabled = fontSizeSp < 28,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Büyüt",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Row 3: Toggleable Quick Verse Filter Input
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = verseFilterQuery,
                            onValueChange = { verseFilterQuery = it },
                            placeholder = { Text(if (lang == AppLanguage.EN) "Filter text in all scriptures..." else "Karşılaştırılan metinlerde kelime/ayet filtrele...") },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = SacredGold) },
                            trailingIcon = {
                                if (verseFilterQuery.isNotEmpty()) {
                                    IconButton(onClick = { verseFilterQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }

            // MAIN CONTENT VIEW AREA
            val book1 = slot1.loadedBook
            val book2 = slot2.loadedBook
            val book3 = slot3.loadedBook

            val p1List = if (slot1.langMode == "original") book1?.originalParagraphs?.ifEmpty { book1.paragraphs } ?: emptyList() else book1?.paragraphs ?: emptyList()
            val p2List = if (slot2.langMode == "original") book2?.originalParagraphs?.ifEmpty { book2.paragraphs } ?: emptyList() else book2?.paragraphs ?: emptyList()
            val p3List = if (slot3.langMode == "original") book3?.originalParagraphs?.ifEmpty { book3.paragraphs } ?: emptyList() else book3?.paragraphs ?: emptyList()

            val maxParagraphCount = maxOf(p1List.size, p2List.size, if (bookCountMode == 3) p3List.size else 0)

            val isAnyLoading = slot1.isLoading || slot2.isLoading || (bookCountMode == 3 && slot3.isLoading)

            if (isAnyLoading) {
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
                        CircularProgressIndicator(color = SacredGold)
                        Text(
                            text = if (lang == AppLanguage.EN) "Fetching comparative scripture texts..." else "Metinler yükleniyor ve hizalanıyor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (maxParagraphCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(48.dp), tint = SacredGold)
                        Text(
                            text = if (lang == AppLanguage.EN) "No scripture text found to display." else "Görüntülenecek metin bulunamadı.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { slotPickerIndex = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (lang == AppLanguage.EN) "Select Scripture" else "Eser Seç")
                        }
                    }
                }
            } else {
                when (layoutMode) {
                    ComparisonLayoutMode.PARALLEL_CARDS -> {
                        // UNIFIED SIDE-BY-SIDE PARALLEL VERSE CARDS
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items((0 until maxParagraphCount).toList()) { idx ->
                                val v1Text = p1List.getOrNull(idx)
                                val v2Text = p2List.getOrNull(idx)
                                val v3Text = if (bookCountMode == 3) p3List.getOrNull(idx) else null

                                val isMatchesFilter = verseFilterQuery.isBlank() ||
                                        (v1Text?.contains(verseFilterQuery, ignoreCase = true) == true) ||
                                        (v2Text?.contains(verseFilterQuery, ignoreCase = true) == true) ||
                                        (v3Text?.contains(verseFilterQuery, ignoreCase = true) == true)

                                if (isMatchesFilter) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Card Top Bar: Ayet Badge + Action Buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(SacredGold),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${idx + 1}",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = if (lang == AppLanguage.EN) "Verse / Passage ${idx + 1}" else "Ayet / Pasaj ${idx + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SacredGold
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    // Add note button
                                                    IconButton(
                                                        onClick = {
                                                            noteQuoteText = "[${getSlotFormattedTitle(slot1, lang == AppLanguage.EN)}] $v1Text\n\n[${getSlotFormattedTitle(slot2, lang == AppLanguage.EN)}] $v2Text"
                                                            showAddNoteDialog = true
                                                        },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.EditNote,
                                                            contentDescription = "Note",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    // Copy button
                                                    IconButton(
                                                        onClick = {
                                                            val copyStr = "• ${getSlotFormattedTitle(slot1, lang == AppLanguage.EN)}:\n$v1Text\n\n• ${getSlotFormattedTitle(slot2, lang == AppLanguage.EN)}:\n$v2Text"
                                                            clipboardManager.setText(AnnotatedString(copyStr))
                                                            Toast.makeText(context, if (lang == AppLanguage.EN) "Passages copied!" else "Pasajlar kopyalandı!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copy",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                            // Text 1 Box
                                            if (v1Text != null) {
                                                VerseBlockItem(
                                                    slotNum = 1,
                                                    title = getSlotFormattedTitle(slot1, lang == AppLanguage.EN),
                                                    text = v1Text,
                                                    color = slotColors[0],
                                                    isOriginal = slot1.langMode == "original",
                                                    fontSize = fontSizeSp.sp
                                                )
                                            }

                                            // Text 2 Box
                                            if (v2Text != null) {
                                                VerseBlockItem(
                                                    slotNum = 2,
                                                    title = getSlotFormattedTitle(slot2, lang == AppLanguage.EN),
                                                    text = v2Text,
                                                    color = slotColors[1],
                                                    isOriginal = slot2.langMode == "original",
                                                    fontSize = fontSizeSp.sp
                                                )
                                            }

                                            // Text 3 Box
                                            if (bookCountMode == 3 && v3Text != null) {
                                                VerseBlockItem(
                                                    slotNum = 3,
                                                    title = getSlotFormattedTitle(slot3, lang == AppLanguage.EN),
                                                    text = v3Text,
                                                    color = slotColors[2],
                                                    isOriginal = slot3.langMode == "original",
                                                    fontSize = fontSizeSp.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ComparisonLayoutMode.SIDE_BY_SIDE -> {
                        // SIDE BY SIDE COLUMNS
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ScriptureColumnList(
                                    title = getSlotFormattedTitle(slot1, lang == AppLanguage.EN),
                                    paragraphs = p1List,
                                    color = slotColors[0],
                                    filter = verseFilterQuery,
                                    isOriginal = slot1.langMode == "original",
                                    fontSize = (fontSizeSp * 0.85f).sp
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ScriptureColumnList(
                                    title = getSlotFormattedTitle(slot2, lang == AppLanguage.EN),
                                    paragraphs = p2List,
                                    color = slotColors[1],
                                    filter = verseFilterQuery,
                                    isOriginal = slot2.langMode == "original",
                                    fontSize = (fontSizeSp * 0.85f).sp
                                )
                            }
                            if (bookCountMode == 3) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ScriptureColumnList(
                                        title = getSlotFormattedTitle(slot3, lang == AppLanguage.EN),
                                        paragraphs = p3List,
                                        color = slotColors[2],
                                        filter = verseFilterQuery,
                                        isOriginal = slot3.langMode == "original",
                                        fontSize = (fontSizeSp * 0.85f).sp
                                    )
                                }
                            }
                        }
                    }

                    ComparisonLayoutMode.STACKED_PANELS -> {
                        // STACKED FULL WIDTH PANELS
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            item {
                                StackedScriptureCard(
                                    title = getSlotFormattedTitle(slot1, lang == AppLanguage.EN),
                                    paragraphs = p1List,
                                    color = slotColors[0],
                                    filter = verseFilterQuery,
                                    isOriginal = slot1.langMode == "original",
                                    fontSize = fontSizeSp.sp
                                )
                            }
                            item {
                                StackedScriptureCard(
                                    title = getSlotFormattedTitle(slot2, lang == AppLanguage.EN),
                                    paragraphs = p2List,
                                    color = slotColors[1],
                                    filter = verseFilterQuery,
                                    isOriginal = slot2.langMode == "original",
                                    fontSize = fontSizeSp.sp
                                )
                            }
                            if (bookCountMode == 3) {
                                item {
                                    StackedScriptureCard(
                                        title = getSlotFormattedTitle(slot3, lang == AppLanguage.EN),
                                        paragraphs = p3List,
                                        color = slotColors[2],
                                        filter = verseFilterQuery,
                                        isOriginal = slot3.langMode == "original",
                                        fontSize = fontSizeSp.sp
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

private fun getSlotFormattedTitle(slot: SlotConfig, isEn: Boolean): String {
    if (slot.category == "quran") {
        val surahNum = slot.chapterNumber
        val surahMeta = QuranRepository.surahs.find { it.number == surahNum }
        return if (isEn) {
            if (surahMeta != null) "$surahNum. Surah: ${surahMeta.nameEnglish}" else "Surah $surahNum"
        } else {
            if (surahMeta != null) "$surahNum. Sûre: ${surahMeta.nameArabic} (${surahMeta.nameTurkish})" else "$surahNum. Sûre"
        }
    } else {
        val allBooks = com.example.data.model.BibleRepository.run {
            torahBooks + bibleBooks + bukhariBooks + gitaBooks + talmudBooks
        }
        val targetBook = allBooks.find { it.id.equals(slot.subBookId, ignoreCase = true) }
        val bookName = if (isEn) {
            targetBook?.nameEnglish ?: slot.subBookId ?: "Book"
        } else {
            targetBook?.nameTurkish ?: slot.subBookId ?: "Kitap"
        }
        return if (isEn) "$bookName Chapter ${slot.chapterNumber}" else "$bookName ${slot.chapterNumber}. Bölüm"
    }
}

@Composable
fun CompactAcademicSlotBadge(
    slotNum: Int,
    slot: SlotConfig,
    color: Color,
    isEn: Boolean,
    onClick: () -> Unit
) {
    val categoryTitle = if (isEn) {
        when (slot.category) {
            "quran" -> "Quran"
            "sermon" -> "Gospel"
            "torah" -> "Torah"
            "bukhari" -> "Bukhari"
            "gita" -> "Gita"
            "talmud" -> "Talmud"
            else -> "Text"
        }
    } else {
        when (slot.category) {
            "quran" -> "Kur'an"
            "sermon" -> "İncil"
            "torah" -> "Tevrat"
            "bukhari" -> "Buharî"
            "gita" -> "Gita"
            "talmud" -> "Talmud"
            else -> "Metin"
        }
    }
    val title = getSlotFormattedTitle(slot, isEn)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M$slotNum",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = if (isEn) "Select / Change" else "Seç / Değiştir",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun InteractiveSlotHeaderCard(
    slotNum: Int,
    slot: SlotConfig,
    color: Color,
    isEn: Boolean,
    onSelectClick: () -> Unit,
    onVerseClick: () -> Unit,
    onLangToggle: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    val categoryTitle = if (isEn) {
        when (slot.category) {
            "quran" -> "Holy Quran"
            "sermon" -> "New Testament (Gospel)"
            "torah" -> "Old Testament (Torah)"
            "bukhari" -> "Hadith (Bukhari)"
            "gita" -> "Bhagavad Gita"
            "talmud" -> "Talmud"
            else -> "Sacred Text"
        }
    } else {
        when (slot.category) {
            "quran" -> "Kur'an-ı Kerim"
            "sermon" -> "Ahd-i Cedid (İncil)"
            "torah" -> "Ahd-i Atik (Tevrat)"
            "bukhari" -> "Hadis (Buharî)"
            "gita" -> "Bhagavad Gita"
            "talmud" -> "Talmud"
            else -> "Kutsal Eser"
        }
    }

    val bookTitle = getSlotFormattedTitle(slot, isEn)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.width(260.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Slot Badge & Category Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M$slotNum",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Language Mode Toggle Pill
                Surface(
                    onClick = onLangToggle,
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = when (slot.langMode) {
                            "original" -> if (isEn) "ORIG" else "ORJ"
                            "en" -> "EN"
                            else -> "TR"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Main Book Title Button (Clickable to change book/surah)
            Card(
                onClick = onSelectClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isEn) "Tap to change" else "Değiştirmek İçin Dokunun",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Change",
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Bottom Actions Bar: Chapter Nav & Verse Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev / Next Chapter Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onPrevChapter,
                        enabled = slot.chapterNumber > 1,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Prev",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = if (isEn) "Ch. ${slot.chapterNumber}" else "Bölüm ${slot.chapterNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = onNextChapter,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Ayet List / Direct Jump Button
                TextButton(
                    onClick = onVerseClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = "Verses",
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isEn) "Verses" else "Ayet Seç",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VerseBlockItem(
    slotNum: Int,
    title: String,
    text: String,
    color: Color,
    isOriginal: Boolean,
    fontSize: TextUnit = 15.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Color accent vertical pill line
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.45f).sp
                ),
                fontFamily = if (isOriginal) FontFamily.Serif else FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ScriptureColumnList(
    title: String,
    paragraphs: List<String>,
    color: Color,
    filter: String,
    isOriginal: Boolean,
    fontSize: TextUnit = 13.sp
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filteredList = paragraphs.filter { filter.isBlank() || it.contains(filter, ignoreCase = true) }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredList) { idx, text ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = fontSize,
                                lineHeight = (fontSize.value * 1.4f).sp
                            ),
                            fontFamily = if (isOriginal) FontFamily.Serif else FontFamily.Default,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StackedScriptureCard(
    title: String,
    paragraphs: List<String>,
    color: Color,
    filter: String,
    isOriginal: Boolean,
    fontSize: TextUnit = 15.sp
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = FontFamily.Serif
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = color
                )
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                val filteredList = paragraphs.filter { filter.isBlank() || it.contains(filter, ignoreCase = true) }

                filteredList.forEach { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.45f).sp
                        ),
                        fontFamily = if (isOriginal) FontFamily.Serif else FontFamily.Default,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }
}
