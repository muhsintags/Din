package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.ui.theme.SacredGold
import com.example.ui.viewmodel.ScriptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: ScriptureViewModel,
    onNavigateToBook: (Book) -> Unit,
    initialQuery: String = ""
) {
    var searchQuery by remember { mutableStateOf(initialQuery) }

    // Synchronize initial query if set
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            searchQuery = initialQuery
        }
    }

    val allBooks = viewModel.books

    // 1. Matching Book Metadata (title, description, etc.)
    val matchedBooks = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            allBooks.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.description.contains(searchQuery, ignoreCase = true) ||
                book.category.contains(searchQuery, ignoreCase = true) ||
                book.authorOrSource.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 2. Matching Scripture Passages
    val matchedPassages = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            val matches = mutableListOf<PassageMatch>()
            allBooks.forEach { book ->
                book.paragraphs.forEachIndexed { index, paragraph ->
                    if (paragraph.contains(searchQuery, ignoreCase = true)) {
                        matches.add(PassageMatch(book, index, paragraph))
                    }
                }
            }
            matches
        }
    }

    // 3. Matching Dictionary Concepts
    val matchedDictionaryTerms = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            DictionaryData.defaultTerms.filter { term ->
                term.term.contains(searchQuery, ignoreCase = true) ||
                term.origin.contains(searchQuery, ignoreCase = true) ||
                term.meaningTr.contains(searchQuery, ignoreCase = true) ||
                term.meaningEn.contains(searchQuery, ignoreCase = true) ||
                term.exampleTr.contains(searchQuery, ignoreCase = true) ||
                term.exampleEn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalResultsCount = matchedBooks.size + matchedPassages.size + matchedDictionaryTerms.size

    var expandedConceptTerm by remember { mutableStateOf<String?>(null) }

    val popularConcepts = listOf(
        "Hikmet", "Adalet", "Barış", "Merhamet", "Aydınlanma", "Hakikat"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = "Scriptorium Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kutsal Metinler",
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
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // 1. Large Search Bar (Kavram, metin veya öğreti ara...)
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Kavram, metin veya öğreti ara...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .testTag("search_text_input"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 2. Popular Concepts Chips (POPÜLER KAVRAMLAR)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "POPÜLER KAVRAMLAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(popularConcepts) { concept ->
                            FilterChip(
                                selected = searchQuery.equals(concept, ignoreCase = true),
                                onClick = { searchQuery = concept },
                                label = {
                                    Text(
                                        text = "#$concept",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null,
                                modifier = Modifier.testTag("concept_chip_$concept")
                            )
                        }
                    }
                }
            }

            // 3. Search Results or Suggested Readings (ÖNERİLEN OKUMALAR)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "ÖNERİLEN OKUMALAR" else "ARAMA SONUÇLARI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "$totalResultsCount Sonuç",
                            style = MaterialTheme.typography.labelMedium,
                            color = SacredGold
                        )
                    }
                }
            }

            if (searchQuery.isEmpty()) {
                // SUGGESTED READINGS (ÖNERİLEN OKUMALAR) - original list of books
                items(allBooks) { book ->
                    BookCard(book = book, onNavigateToBook = onNavigateToBook)
                }
            } else {
                if (totalResultsCount == 0) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Aradığınız kavramla eşleşen metin bulunamadı.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // SECTION 1: DICTIONARY TERMS MATCHES
                    if (matchedDictionaryTerms.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Sözlük Kavramları (${matchedDictionaryTerms.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(matchedDictionaryTerms) { term ->
                            val isExpanded = expandedConceptTerm == term.term
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedConceptTerm = if (isExpanded) null else term.term
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = term.term,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = term.origin,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                        
                                        // Category Tag
                                        Surface(
                                            color = when (term.category) {
                                                TermCategory.ISLAMIC -> MaterialTheme.colorScheme.primaryContainer
                                                TermCategory.BIBLICAL -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> MaterialTheme.colorScheme.tertiaryContainer
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = when (term.category) {
                                                    TermCategory.ISLAMIC -> "İslami"
                                                    TermCategory.BIBLICAL -> "Kitab-ı Mukaddes"
                                                    else -> "Genel"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = when (term.category) {
                                                    TermCategory.ISLAMIC -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    TermCategory.BIBLICAL -> MaterialTheme.colorScheme.onSecondaryContainer
                                                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                                                }
                                            )
                                        }
                                    }

                                    // Meaning
                                    HighlightedText(
                                        text = term.meaningTr,
                                        query = searchQuery
                                    )

                                    if (isExpanded) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Örnek Bağlam / Ayet:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SacredGold
                                            )
                                            HighlightedText(
                                                text = term.exampleTr,
                                                query = searchQuery
                                            )
                                            if (term.meaningEn.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "English Meaning:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                                HighlightedText(
                                                    text = term.meaningEn,
                                                    query = searchQuery
                                                )
                                                if (term.exampleEn.isNotEmpty()) {
                                                    HighlightedText(
                                                        text = term.exampleEn,
                                                        query = searchQuery
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Detayları görmek için tıklayın...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: SCRIPTURE MATCHES (VERSES)
                    if (matchedPassages.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoStories,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Metin İçindeki Ayetler (${matchedPassages.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(matchedPassages) { match ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToBook(match.book) },
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bookmark,
                                                contentDescription = null,
                                                tint = SacredGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = match.book.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Pasaj ${match.paragraphIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    HighlightedText(
                                        text = match.text,
                                        query = searchQuery
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Kitapta Oku",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: SCRIPTURE BOOKS MATCHES
                    if (matchedBooks.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Eşleşen Kutsal Kitaplar (${matchedBooks.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(matchedBooks) { book ->
                            BookCard(book = book, onNavigateToBook = onNavigateToBook)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    onNavigateToBook: (Book) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToBook(book) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
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
                    imageVector = when (book.iconName) {
                        "auto_stories" -> Icons.Filled.AutoStories
                        "menu_book" -> Icons.Filled.MenuBook
                        "history_edu" -> Icons.Filled.HistoryEdu
                        "nature" -> Icons.Filled.Nature
                        "church" -> Icons.Filled.Church
                        "potted_plant" -> Icons.Filled.LocalFlorist
                        else -> Icons.Filled.Book
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = book.authorOrSource,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = "Oku",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

data class PassageMatch(
    val book: Book,
    val paragraphIndex: Int,
    val text: String
)

@Composable
fun HighlightedText(
    text: String,
    query: String,
    color: androidx.compose.ui.graphics.Color = SacredGold
) {
    val lowercaseText = text.lowercase()
    val lowercaseQuery = query.lowercase().trim()
    if (lowercaseQuery.isEmpty() || !lowercaseText.contains(lowercaseQuery)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        return
    }
    val annotatedString = buildAnnotatedString {
        var startIdx = 0
        while (true) {
            val idx = lowercaseText.indexOf(lowercaseQuery, startIdx)
            if (idx == -1) {
                append(text.substring(startIdx))
                break
            }
            append(text.substring(startIdx, idx))
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                append(text.substring(idx, idx + lowercaseQuery.length))
            }
            startIdx = idx + lowercaseQuery.length
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
