package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendSetupGuideDialog(
    lang: AppLanguage,
    currentBaseUrl: String,
    onUpdateBaseUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var customUrlInput by remember { mutableStateOf(currentBaseUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = SacredGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.EN) "Backend & Database Architecture" else "Sunucu & Veritabanı Mimarisi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) "example.com API Setup & Integration Guide" else "example.com API Yapılandırma ve Entegrasyon Rehberi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }
                }

                // Base URL Config Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.EN) "API Base Endpoint URL" else "Aktif API Sunucu Adresi (Base URL):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                                placeholder = { Text("https://example.com/") }
                            )
                            Button(
                                onClick = { onUpdateBaseUrl(customUrlInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = SacredGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (lang == AppLanguage.EN) "Apply" else "Güncelle", color = Color.White)
                            }
                        }
                    }
                }

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (lang == AppLanguage.EN) "1. Node.js Express" else "1. Node.js Sunucu") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (lang == AppLanguage.EN) "2. PostgreSQL DB" else "2. Veritabanı") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (lang == AppLanguage.EN) "3. New Content API" else "3. İçerik Ekleme") }
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> NodeJsGuideSection(lang)
                        1 -> DatabaseGuideSection(lang)
                        2 -> AddContentGuideSection(lang)
                    }
                }

                HorizontalDivider()

                // Footer Close Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (lang == AppLanguage.EN) "Close Guide" else "Rehberi Kapat")
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeJsGuideSection(lang: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "1. example.com Üzerinde Node.js & Express REST API Sunucusu Kurulumu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Mobil uygulamanın example.com/api/v1/miracles adresine GET isteği attığında dönmesi gereken varsayılan JSON yapısı ve Express.js rotası:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        CodeBox(
            code = """
// server.js (Node.js + Express)
const express = require('express');
const cors = require('cors');
const app = express();

app.use(cors());
app.use(express.json());

// GET /api/v1/miracles (List & Search API)
app.get('/api/v1/miracles', async (req, res) => {
  const { q, tag } = req.query;
  try {
    let query = 'SELECT * FROM miracles';
    // PostgreSQL / MySQL sorguları filtrelenip JSON dönülür
    const results = await db.query(query);
    res.status(200).json(results.rows);
  } catch (err) {
    res.status(500).json({ error: 'Sunucu hatasi' });
  }
});

app.listen(443, () => console.log('HTTPS Server Running on example.com:443'));
            """.trimIndent()
        )
    }
}

@Composable
private fun DatabaseGuideSection(lang: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "2. PostgreSQL İlişkisel Veritabanı Şeması ve İndeksleme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Gönderilerin, tıklanabilir hashtag'lerin ve etkileşimlerin yüksek performansla filtrelenebilmesi için oluşturulan SQL veritabanı yapısı:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        CodeBox(
            code = """
-- 1. Miracles Tablosu
CREATE TABLE miracles (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    author VARCHAR(100) NOT NULL,
    date VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    reference VARCHAR(150),
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Hashtags Tablosu ve İndeksler
CREATE TABLE miracle_hashtags (
    miracle_id VARCHAR(50) REFERENCES miracles(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (miracle_id, tag)
);

CREATE INDEX idx_hashtags_tag ON miracle_hashtags(tag);
            """.trimIndent()
        )
    }
}

@Composable
private fun AddContentGuideSection(lang: AppLanguage) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "3. Yeni İçerik / Gönderi Ekleme Mekanizması (Admin POST API)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "example.com sistemine yeni bir mucize gönderisi eklemek için Admin panelinden veya cURL ile gönderilmesi gereken JSON isteği:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        CodeBox(
            code = """
POST /api/v1/miracles
Content-Type: application/json
Authorization: Bearer <ADMIN_JWT_TOKEN>

{
  "id": "m7",
  "title": "Ateşsiz Yangın: Magma ve Yıldız Çekirdekleri",
  "category": "Astrofizik",
  "author": "Dr. Mehmet Kaya",
  "date": "24 Temmuz 2026",
  "content": "Evrendeki yıldızların nükleer füzyon mekanizması...",
  "imageUrl": "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86",
  "reference": "Waqi'a Suresi, 75. Ayet",
  "hashtags": ["#fizik", "#yildizlar", "#kuranmucizeleri"],
  "likesCount": 0,
  "commentsCount": 0
}
            """.trimIndent()
        )
    }
}

@Composable
private fun CodeBox(code: String) {
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFD4D4D4),
                lineHeight = 16.sp
            )
        }
    }
}
