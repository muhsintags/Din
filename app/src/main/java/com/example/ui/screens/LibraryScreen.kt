package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
fun LibraryScreen(
    viewModel: ScriptureViewModel,
    onNavigateToBook: (Book) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language
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
                            text = Loc.get("all_scriptures", lang),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Holy Scriptures Library" else "Kutsal Metinler Kütüphanesi",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Loc.get("library_promo", lang),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Group books by category
            val categories = books.map { it.category }.distinct()

            categories.forEach { category ->
                val categoryBooks = books.filter { it.category == category }

                if (categoryBooks.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Category Title Header
                            val categoryTranslated = when (category) {
                                "Diğer Metinler" -> if (lang == AppLanguage.EN) "Other Scriptures" else "Diğer Metinler"
                                "Kutsal Metinler" -> if (lang == AppLanguage.EN) "Holy Scriptures" else "Kutsal Metinler"
                                "Kur'an-ı Kerim" -> Loc.get("quran", lang)
                                "Kitab-ı Mukaddes" -> Loc.get("bible", lang)
                                else -> category
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                            ) {
                                Text(
                                    text = categoryTranslated,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                )
                            }

                            // Book items in category
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                categoryBooks.forEach { book ->
                                    val bookTitle = Loc.get(book.id, lang)
                                    val bookDescription = when (book.id) {
                                        "quran" -> Loc.get("quran_download_promo", lang)
                                        "torah", "gospel" -> Loc.get("bible_promo_desc", lang)
                                        "sermon" -> if (lang == AppLanguage.EN) "Discover the cornerstone teachings of love, peace, and spirituality." else "Sevgi, barış ve maneviyatın temel öğretilerini keşfedin."
                                        else -> book.description
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("book_card_${book.id}"),
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
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Soft book cover (2D flat elegant design)
                                            ScriptureBookCover(
                                                bookId = book.id,
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .height(84.dp)
                                            )

                                            // Text description + button column
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = bookTitle,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = bookDescription,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontStyle = FontStyle.Italic,
                                                        maxLines = 2,
                                                        lineHeight = 20.sp
                                                    )
                                                }

                                                Button(
                                                    onClick = { onNavigateToBook(book) },
                                                    shape = RoundedCornerShape(20.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .height(36.dp)
                                                        .testTag("read_button_${book.id}")
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = Loc.get("read", lang),
                                                            style = MaterialTheme.typography.labelLarge
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Filled.AutoStories,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
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
        }
    }
}

@Composable
fun ScriptureBookCover(bookId: String, modifier: Modifier = Modifier) {
    val (gradient, icon, iconColor, label) = when (bookId) {
        "quran" -> Quadruple(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF0F5A32), androidx.compose.ui.graphics.Color(0xFF063B1E))),
            Icons.Filled.AutoStories,
            SacredGold,
            "K.K"
        )
        "torah" -> Quadruple(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF1B365D), androidx.compose.ui.graphics.Color(0xFF0F1E3D))),
            Icons.Filled.MenuBook,
            androidx.compose.ui.graphics.Color(0xFFE5A93B),
            "TEV"
        )
        "sermon" -> Quadruple(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF8B1E1E), androidx.compose.ui.graphics.Color(0xFF4A1010))),
            Icons.Filled.MenuBook,
            androidx.compose.ui.graphics.Color(0xFFF1C40F),
            "İNC"
        )
        "talmud" -> Quadruple(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3F3B5C), androidx.compose.ui.graphics.Color(0xFF24213B))),
            Icons.Filled.MenuBook,
            androidx.compose.ui.graphics.Color(0xFFD4AF37),
            "TAL"
        )
        "bukhari" -> Quadruple(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF1D5C6A), androidx.compose.ui.graphics.Color(0xFF0F363F))),
            Icons.Filled.AutoStories,
            SacredGold,
            "BUH"
        )
        else -> Quadruple(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF4A5568), androidx.compose.ui.graphics.Color(0xFF2D3748))),
            Icons.Filled.MenuBook,
            androidx.compose.ui.graphics.Color.White,
            "MET"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(gradient)
            .border(1.5.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.5.dp, iconColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = iconColor,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
