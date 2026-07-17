package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Book
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import com.example.ui.viewmodel.ScriptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScriptureViewModel,
    onNavigateToBook: (Book) -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language
    val history by viewModel.readingHistory.collectAsState()
    val books = viewModel.books

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
                            text = "Scriptorium",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = Loc.get("settings", lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // 1. Daily Verse (Günlük Ayet)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val selectedBooks by viewModel.selectedBooksForVerse.collectAsState()
                    val activeVerseState by viewModel.activeVerse.collectAsState()
                    val isVerseLoading by viewModel.isVerseLoading.collectAsState()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Text(
                                text = Loc.get("daily_verse", lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.refreshActiveVerse() },
                            modifier = Modifier.size(36.dp).testTag("refresh_verse_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = Loc.get("refresh_verse", lang),
                                tint = SacredGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Selection Row for Holy Books
                    Text(
                        text = if (lang == AppLanguage.EN) "Select the source for daily verses:" else "Ayetlerin geleceği kaynakları seçin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(viewModel.books) { book ->
                            val isSelected = selectedBooks.contains(book.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { viewModel.toggleVerseBookSelection(book.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = Loc.get(book.id, lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    activeVerseState.let { activeVerse ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isVerseLoading) { onNavigateToBook(activeVerse.book) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                                if (isVerseLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = SacredGold,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                } else {
                                    // Quote icon background
                                    Icon(
                                        imageVector = Icons.Filled.FormatQuote,
                                        contentDescription = null,
                                        tint = SacredGold.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .size(100.dp)
                                            .align(Alignment.BottomEnd)
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\"${activeVerse.text}\"",
                                            fontFamily = FontFamily.Serif,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 18.sp,
                                            lineHeight = 28.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(16.dp)
                                                    .height(1.dp)
                                                    .background(SacredGold)
                                            )
                                            Text(
                                                text = activeVerse.reference,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SacredGold,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Traditions (Ana Gelenekler)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Text(
                            text = Loc.get("all_scriptures", lang).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                    }

                    val traditions = listOf(
                        Triple(Loc.get("quran", lang).split(" ").first(), Icons.Filled.AutoStories, "Kur'an-ı Kerim"),
                        Triple(Loc.get("torah", lang).split(" ").first(), Icons.Filled.MenuBook, "Tevrat"),
                        Triple(Loc.get("gospel", lang).split(" ").first(), Icons.Filled.HistoryEdu, "İncil"),
                        Triple(if (lang == AppLanguage.EN) "Psalms" else "Zebur", Icons.Filled.Spa, "Zebur")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        traditions.forEach { (name, icon, query) ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clickable { onNavigateToSearch(query) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Recent Readings (Son Okunanlar)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            Text(
                                text = Loc.get("last_read", lang).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.EN) "Your reading history will be listed here." else "Okuma geçmişiniz burada listelenecektir.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            history.take(3).forEach { hist ->
                                val correspondingBook = books.firstOrNull { 
                                    it.title.equals(hist.bookTitle, ignoreCase = true) || 
                                    (hist.bookTitle.contains("Tevrat", ignoreCase = true) && it.id == "torah") ||
                                    (hist.bookTitle.contains("İncil", ignoreCase = true) && it.id == "sermon") ||
                                    (hist.bookTitle.contains("Quran", ignoreCase = true) && it.id == "quran") ||
                                    (hist.bookTitle.contains("Kur'an", ignoreCase = true) && it.id == "quran") ||
                                    (hist.bookTitle.contains("Torah", ignoreCase = true) && it.id == "torah") ||
                                    (hist.bookTitle.contains("Gospel", ignoreCase = true) && it.id == "sermon")
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            correspondingBook?.let { onNavigateToBook(it) }
                                        },
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
                                                imageVector = Icons.Filled.Book,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Column {
                                                val bookTitleTranslated = when (hist.bookTitle) {
                                                    "Kur'an-ı Kerim", "The Holy Quran" -> Loc.get("quran", lang)
                                                    "Tevrat", "Torah" -> Loc.get("torah", lang)
                                                    "İncil", "Gospel" -> Loc.get("gospel", lang)
                                                    "Dağdaki Vaaz", "Sermon on the Mount" -> Loc.get("sermon", lang)
                                                    else -> hist.bookTitle
                                                }
                                                Text(
                                                    text = bookTitleTranslated,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val subtitleTranslated = if (lang == AppLanguage.EN) {
                                                    hist.subtitle
                                                        .replace("Sure", "Surah")
                                                        .replace("Bölüm", "Chapter")
                                                        .replace("Ayet", "Verse")
                                                } else {
                                                    hist.subtitle
                                                }
                                                Text(
                                                    text = subtitleTranslated,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "%${hist.progressPercent}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = SacredGold
                                            )
                                            Text(
                                                text = hist.dateText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
