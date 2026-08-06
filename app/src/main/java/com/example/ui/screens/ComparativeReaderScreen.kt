package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BibleBook
import com.example.data.model.BibleRepository
import com.example.data.model.Book
import com.example.data.model.QuranRepository
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import com.example.ui.viewmodel.ScriptureViewModel
import kotlinx.coroutines.launch

enum class ComparisonLayoutMode {
    PARALLEL_CARDS, // Unified side-by-side verse cards (Best for Mobile)
    SIDE_BY_SIDE    // Split column view
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

    var bookCountMode by remember { mutableIntStateOf(2) }
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

    LaunchedEffect(Unit) {
        loadSlotContent(1)
        loadSlotContent(2)
        loadSlotContent(3)
    }

    var verseFilterQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var fontSizeSp by remember { mutableIntStateOf(16) }
    var showFontSizeControls by remember { mutableStateOf(false) }
    var slotPickerIndex by remember { mutableStateOf<Int?>(null) }

    // Add Note Dialog State
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteQuoteText by remember { mutableStateOf("") }
    var noteTextQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    val slotColors = listOf(
        Color(0xFF2E7D32), // Emerald Green (Slot 1 - Quran)
        SacredGold,        // Sacred Gold (Slot 2 - Gospel/Torah)
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                        val title = "${getSlotFormattedTitle(slot1, lang == AppLanguage.EN)} & ${getSlotFormattedTitle(slot2, lang == AppLanguage.EN)}"
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

    // Bottom Sheet: Source Selector (Book & Chapter)
    if (slotPickerIndex != null) {
        val targetSlotNum = slotPickerIndex!!
        val currentSlot = when (targetSlotNum) {
            1 -> slot1
            2 -> slot2
            else -> slot3
        }

        var selectedCategory by remember { mutableStateOf(currentSlot.category) }
        var selectedSubBookId by remember { mutableStateOf(currentSlot.subBookId ?: "Matthew") }
        var selectedChapterNum by remember { mutableIntStateOf(currentSlot.chapterNumber) }
        var searchQuery by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { slotPickerIndex = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sheet Header
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(slotColors[targetSlotNum - 1]),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "M$targetSlotNum",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = if (lang == AppLanguage.EN) "Select Source Metin" else "Metin Kaynağı Seçin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { slotPickerIndex = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                // Category Tabs
                val categories = listOf(
                    "quran" to (if (lang == AppLanguage.EN) "Qur'an" else "Kur'an-ı Kerim"),
                    "sermon" to (if (lang == AppLanguage.EN) "Gospel (Injil)" else "İncil"),
                    "torah" to (if (lang == AppLanguage.EN) "Torah & Psalms" else "Tevrat & Zebur"),
                    "bukhari" to (if (lang == AppLanguage.EN) "Hadith" else "Hadis-i Şerif"),
                    "gita" to "Bhagavad Gita",
                    "talmud" to "Talmud"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { (catId, catTitle) ->
                        val isSel = (selectedCategory == catId)
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedCategory = catId
                                selectedSubBookId = when (catId) {
                                    "sermon" -> "Matthew"
                                    "torah" -> "Genesis"
                                    "bukhari" -> BibleRepository.bukhariBooks.first().id
                                    "gita" -> BibleRepository.gitaBooks.first().id
                                    "talmud" -> BibleRepository.talmudBooks.first().id
                                    else -> "Matthew"
                                }
                                selectedChapterNum = 1
                            },
                            label = { Text(catTitle, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = slotColors[targetSlotNum - 1],
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (selectedCategory == "quran") {
                                if (lang == AppLanguage.EN) "Search Surah name or number..." else "Sûre adı veya numarası ara..."
                            } else {
                                if (lang == AppLanguage.EN) "Search book name..." else "Kitap adı ara..."
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SacredGold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // List of Surahs or Books
                if (selectedCategory == "quran") {
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
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSurahs) { s ->
                            val isSel = (selectedCategory == currentSlot.category && selectedChapterNum == s.number)
                            Card(
                                onClick = {
                                    when (targetSlotNum) {
                                        1 -> slot1 = slot1.copy(category = "quran", subBookId = null, chapterNumber = s.number)
                                        2 -> slot2 = slot2.copy(category = "quran", subBookId = null, chapterNumber = s.number)
                                        3 -> slot3 = slot3.copy(category = "quran", subBookId = null, chapterNumber = s.number)
                                    }
                                    loadSlotContent(targetSlotNum)
                                    slotPickerIndex = null
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) slotColors[targetSlotNum - 1].copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSel) BorderStroke(1.5.dp, slotColors[targetSlotNum - 1]) else null,
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
                                                text = if (lang == AppLanguage.EN) "${s.ayahCount} verses" else "${s.ayahCount} ayet",
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
                            .heightIn(max = 160.dp),
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
                                    containerColor = if (isBookSel) slotColors[targetSlotNum - 1].copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = if (isBookSel) BorderStroke(1.5.dp, slotColors[targetSlotNum - 1]) else null,
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
                                            text = if (lang == AppLanguage.EN) "${b.chaptersCount} Chapters" else "${b.chaptersCount} Bölüm",
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
                    Text(
                        text = if (lang == AppLanguage.EN) "SELECT CHAPTER (1 - ${selectedBookObj.chaptersCount}):" else "BÖLÜM SEÇİN (1 - ${selectedBookObj.chaptersCount}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SacredGold,
                        letterSpacing = 1.sp
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 44.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items((1..selectedBookObj.chaptersCount).toList()) { ch ->
                            val isChSel = (selectedChapterNum == ch && selectedSubBookId == currentSlot.subBookId)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Main Scaffold Layout
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (lang == AppLanguage.EN) "Comparative Reading" else "Karşılaştırmalı Okuma",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            text = if (lang == AppLanguage.EN) "Intertextual Scripture Analysis" else "Metinlerarası Kutsal Analiz",
                            style = MaterialTheme.typography.labelSmall,
                            color = SacredGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "Arama",
                            tint = if (isSearchVisible) SacredGold else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showFontSizeControls = !showFontSizeControls },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Yazı Boyutu",
                            tint = if (showFontSizeControls) SacredGold else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Chapter Control Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (slot1.chapterNumber > 1) {
                                slot1 = slot1.copy(chapterNumber = slot1.chapterNumber - 1)
                                loadSlotContent(1)
                            }
                            if (slot2.chapterNumber > 1) {
                                slot2 = slot2.copy(chapterNumber = slot2.chapterNumber - 1)
                                loadSlotContent(2)
                            }
                        },
                        enabled = (slot1.chapterNumber > 1 || slot2.chapterNumber > 1),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(if (lang == AppLanguage.EN) "Prev" else "Önceki")
                    }

                    TextButton(
                        onClick = { slotPickerIndex = 1 }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SacredGold, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (lang == AppLanguage.EN) "Change Texts" else "Metin Değiştir",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SacredGold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            slot1 = slot1.copy(chapterNumber = slot1.chapterNumber + 1)
                            loadSlotContent(1)
                            slot2 = slot2.copy(chapterNumber = slot2.chapterNumber + 1)
                            loadSlotContent(2)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (lang == AppLanguage.EN) "Next" else "Sonraki")
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // SOURCE CARDS HEADER DECK
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Mode Switch Pills & Layout Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 2 Metin vs 3 Metin Pills
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            bookCountMode = count
                                            if (count == 3 && slot3.loadedBook == null) {
                                                loadSlotContent(3)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
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

                        // Layout Mode Toggle (Parallel Verse Cards vs Split Side-by-Side)
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = layoutMode == ComparisonLayoutMode.PARALLEL_CARDS,
                                onClick = { layoutMode = ComparisonLayoutMode.PARALLEL_CARDS },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Icon(Icons.Default.ViewAgenda, contentDescription = "Kart", modifier = Modifier.size(16.dp))
                            }
                            SegmentedButton(
                                selected = layoutMode == ComparisonLayoutMode.SIDE_BY_SIDE,
                                onClick = { layoutMode = ComparisonLayoutMode.SIDE_BY_SIDE },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Icon(Icons.Default.ViewColumn, contentDescription = "Sütun", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Row 2: Selected Source Cards with Swap Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source 1 Card
                        Card(
                            onClick = { slotPickerIndex = 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = slotColors[0].copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, slotColors[0].copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(slotColors[0]),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("M1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = getSlotCategoryName(slot1.category, lang == AppLanguage.EN),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = slotColors[0],
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = getSlotFormattedTitle(slot1, lang == AppLanguage.EN),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Swap Button
                        IconButton(
                            onClick = {
                                val tempSlot = slot1
                                slot1 = slot2.copy(slotIndex = 1)
                                slot2 = tempSlot.copy(slotIndex = 2)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Metinleri Takas Et",
                                tint = SacredGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Source 2 Card
                        Card(
                            onClick = { slotPickerIndex = 2 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = slotColors[1].copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, slotColors[1].copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(slotColors[1]),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("M2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = getSlotCategoryName(slot2.category, lang == AppLanguage.EN),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = slotColors[1],
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = getSlotFormattedTitle(slot2, lang == AppLanguage.EN),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Optional Source 3 Card
                        if (bookCountMode == 3) {
                            Card(
                                onClick = { slotPickerIndex = 3 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = slotColors[2].copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.dp, slotColors[2].copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(slotColors[2]),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("M3", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = getSlotCategoryName(slot3.category, lang == AppLanguage.EN),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = slotColors[2],
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = getSlotFormattedTitle(slot3, lang == AppLanguage.EN),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // OPTIONAL TOOLBARS: Font Size & Search Filter
            AnimatedVisibility(visible = showFontSizeControls) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.EN) "Font Size:" else "Yazı Boyutu:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = fontSizeSp.toFloat(),
                            onValueChange = { fontSizeSp = it.toInt() },
                            valueRange = 12f..26f,
                            steps = 14,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${fontSizeSp} sp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SacredGold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isSearchVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    OutlinedTextField(
                        value = verseFilterQuery,
                        onValueChange = { verseFilterQuery = it },
                        placeholder = { Text(if (lang == AppLanguage.EN) "Filter verses by keyword..." else "Ayet metinlerinde kelime ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SacredGold) },
                        trailingIcon = {
                            if (verseFilterQuery.isNotEmpty()) {
                                IconButton(onClick = { verseFilterQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // MAIN CONTENT AREA
            if (slot1.isLoading || slot2.isLoading || (bookCountMode == 3 && slot3.isLoading)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = SacredGold)
                        Text(
                            text = if (lang == AppLanguage.EN) "Loading comparative scriptures..." else "Kutsal metinler karşılaştırma için yükleniyor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val book1 = slot1.loadedBook
                val book2 = slot2.loadedBook
                val book3 = slot3.loadedBook

                val p1List = book1?.paragraphs ?: emptyList()
                val p2List = book2?.paragraphs ?: emptyList()
                val p3List = book3?.paragraphs ?: emptyList()

                val maxVersesCount = maxOf(p1List.size, p2List.size, if (bookCountMode == 3) p3List.size else 0)

                if (maxVersesCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == AppLanguage.EN) "No scripture content found for selected chapters." else "Seçilen bölümler için içerik bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (layoutMode == ComparisonLayoutMode.PARALLEL_CARDS) {
                    // MODE A: UNIFIED PARALLEL VERSE CARDS
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed((0 until maxVersesCount).toList()) { index, verseIdx ->
                            val v1Text = p1List.getOrNull(verseIdx)
                            val v2Text = p2List.getOrNull(verseIdx)
                            val v3Text = p3List.getOrNull(verseIdx)

                            val matchesFilter = verseFilterQuery.isBlank() ||
                                    (v1Text?.contains(verseFilterQuery, ignoreCase = true) == true) ||
                                    (v2Text?.contains(verseFilterQuery, ignoreCase = true) == true) ||
                                    (v3Text?.contains(verseFilterQuery, ignoreCase = true) == true)

                            if (matchesFilter) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Verse Index Badge Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = SacredGold.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = "${if (lang == AppLanguage.EN) "Passage" else "Ayet / Pasaj"} #${verseIdx + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SacredGold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        val combinedText = buildString {
                                                            if (!v1Text.isNullOrBlank()) append("[M1 ${getSlotFormattedTitle(slot1, lang == AppLanguage.EN)}]: $v1Text\n\n")
                                                            if (!v2Text.isNullOrBlank()) append("[M2 ${getSlotFormattedTitle(slot2, lang == AppLanguage.EN)}]: $v2Text\n\n")
                                                            if (bookCountMode == 3 && !v3Text.isNullOrBlank()) append("[M3 ${getSlotFormattedTitle(slot3, lang == AppLanguage.EN)}]: $v3Text")
                                                        }
                                                        clipboardManager.setText(AnnotatedString(combinedText))
                                                        Toast.makeText(context, if (lang == AppLanguage.EN) "Verses copied!" else "Ayetler kopyalandı!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        noteQuoteText = buildString {
                                                            if (!v1Text.isNullOrBlank()) append("• ${getSlotFormattedTitle(slot1, lang == AppLanguage.EN)}:\n$v1Text\n\n")
                                                            if (!v2Text.isNullOrBlank()) append("• ${getSlotFormattedTitle(slot2, lang == AppLanguage.EN)}:\n$v2Text")
                                                        }
                                                        showAddNoteDialog = true
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.EditNote, contentDescription = "Tefekkür Notu Ekle", tint = SacredGold, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        // Slot 1 Text Block
                                        if (!v1Text.isNullOrBlank()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = getSlotFormattedTitle(slot1, lang == AppLanguage.EN),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = slotColors[0]
                                                )
                                                Text(
                                                    text = v1Text,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = fontSizeSp.sp,
                                                        lineHeight = (fontSizeSp * 1.5).sp
                                                    ),
                                                    fontFamily = FontFamily.Serif,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                        // Slot 2 Text Block
                                        if (!v2Text.isNullOrBlank()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = getSlotFormattedTitle(slot2, lang == AppLanguage.EN),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = slotColors[1]
                                                )
                                                Text(
                                                    text = v2Text,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = fontSizeSp.sp,
                                                        lineHeight = (fontSizeSp * 1.5).sp
                                                    ),
                                                    fontFamily = FontFamily.Serif,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // Slot 3 Text Block (if 3-Metin active)
                                        if (bookCountMode == 3 && !v3Text.isNullOrBlank()) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = getSlotFormattedTitle(slot3, lang == AppLanguage.EN),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = slotColors[2]
                                                )
                                                Text(
                                                    text = v3Text,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = fontSizeSp.sp,
                                                        lineHeight = (fontSizeSp * 1.5).sp
                                                    ),
                                                    fontFamily = FontFamily.Serif,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // MODE B: SPLIT COLUMN VIEW
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Column 1
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, slotColors[0].copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = getSlotFormattedTitle(slot1, lang == AppLanguage.EN),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = slotColors[0],
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                HorizontalDivider(color = slotColors[0].copy(alpha = 0.3f))
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 8.dp)
                                ) {
                                    itemsIndexed(p1List) { idx, text ->
                                        if (verseFilterQuery.isBlank() || text.contains(verseFilterQuery, ignoreCase = true)) {
                                            Text(
                                                text = "${idx + 1}. $text",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = fontSizeSp.sp,
                                                    lineHeight = (fontSizeSp * 1.45).sp
                                                ),
                                                fontFamily = FontFamily.Serif
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Column 2
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, slotColors[1].copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = getSlotFormattedTitle(slot2, lang == AppLanguage.EN),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = slotColors[1],
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                HorizontalDivider(color = slotColors[1].copy(alpha = 0.3f))
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 8.dp)
                                ) {
                                    itemsIndexed(p2List) { idx, text ->
                                        if (verseFilterQuery.isBlank() || text.contains(verseFilterQuery, ignoreCase = true)) {
                                            Text(
                                                text = "${idx + 1}. $text",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = fontSizeSp.sp,
                                                    lineHeight = (fontSizeSp * 1.45).sp
                                                ),
                                                fontFamily = FontFamily.Serif
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Column 3 (Optional)
                        if (bookCountMode == 3) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, slotColors[2].copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = getSlotFormattedTitle(slot3, lang == AppLanguage.EN),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = slotColors[2],
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    HorizontalDivider(color = slotColors[2].copy(alpha = 0.3f))
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 8.dp)
                                    ) {
                                        itemsIndexed(p3List) { idx, text ->
                                            if (verseFilterQuery.isBlank() || text.contains(verseFilterQuery, ignoreCase = true)) {
                                                Text(
                                                    text = "${idx + 1}. $text",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = fontSizeSp.sp,
                                                        lineHeight = (fontSizeSp * 1.45).sp
                                                    ),
                                                    fontFamily = FontFamily.Serif
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
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
}

private fun getSlotCategoryName(category: String, isEn: Boolean): String {
    return when (category) {
        "quran" -> if (isEn) "Qur'an" else "Kur'an-ı Kerim"
        "sermon" -> if (isEn) "Gospel" else "İncil"
        "torah" -> if (isEn) "Torah / Psalms" else "Tevrat / Zebur"
        "bukhari" -> if (isEn) "Hadith" else "Hadis-i Şerif"
        "gita" -> "Bhagavad Gita"
        "talmud" -> "Talmud"
        else -> category.capitalize()
    }
}

private fun getSlotFormattedTitle(slot: SlotConfig, isEn: Boolean): String {
    val category = slot.category
    val subBookId = slot.subBookId
    val chNum = slot.chapterNumber

    if (category == "quran") {
        val surah = QuranRepository.surahs.find { it.number == chNum }
        val name = if (isEn) surah?.nameEnglish ?: "Surah $chNum" else "${surah?.number ?: chNum}. ${surah?.nameEnglish ?: "Sûre $chNum"} (${surah?.nameTurkish ?: ""})"
        return if (isEn) "Surah $name" else "Sûre: $name"
    }

    val bookList: List<BibleBook> = when (category) {
        "torah" -> BibleRepository.torahBooks
        "sermon" -> BibleRepository.bibleBooks
        "bukhari" -> BibleRepository.bukhariBooks
        "gita" -> BibleRepository.gitaBooks
        "talmud" -> BibleRepository.talmudBooks
        else -> BibleRepository.bibleBooks
    }

    val bookObj = bookList.find { it.id == subBookId } ?: bookList.firstOrNull()
    val bookName = if (isEn) bookObj?.nameEnglish ?: subBookId ?: "" else bookObj?.nameTurkish ?: subBookId ?: ""

    return "$bookName ${chNum}. ${if (isEn) "Chapter" else "Bölüm"}"
}
