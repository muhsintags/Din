package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import com.example.ui.viewmodel.AppThemeSetting
import com.example.ui.viewmodel.FontFamilySetting
import com.example.ui.viewmodel.LineHeightSetting
import com.example.ui.viewmodel.ScriptureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: ScriptureViewModel,
    onNavigateBack: () -> Unit
) {
    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language

    // Premium Theme Color Definitions for Live Interactive Preview
    val previewBg = when (readerSettings.theme) {
        AppThemeSetting.LIGHT -> Color(0xFFFCFCFC)
        AppThemeSetting.DARK -> Color(0xFF141617)
        AppThemeSetting.SEPIA -> Color(0xFFFAF2E3)
    }
    val previewTextColor = when (readerSettings.theme) {
        AppThemeSetting.LIGHT -> Color(0xFF1C1B1F)
        AppThemeSetting.DARK -> Color(0xFFE3E2E6)
        AppThemeSetting.SEPIA -> Color(0xFF422B14)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Loc.get("appearance_settings", lang) ?: "Görünüm Ayarları",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = Loc.get("back", lang) ?: "Geri",
                            tint = MaterialTheme.colorScheme.primary
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 1. Live Dynamic Preview Card
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = (Loc.get("preview", lang) ?: "Önizleme").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SacredGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = previewBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SacredGold.copy(alpha = 0.4f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val previewTextSize = readerSettings.fontSizeSp.sp
                        val previewLineHeight = (readerSettings.fontSizeSp * readerSettings.lineHeight.value).sp
                        val previewFontFamily = if (readerSettings.fontFamily == FontFamilySetting.SERIF) FontFamily.Serif else FontFamily.SansSerif

                        Text(
                            text = viewModel.dailyVerseText,
                            fontFamily = previewFontFamily,
                            fontSize = previewTextSize,
                            lineHeight = previewLineHeight,
                            color = previewTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        HorizontalDivider(
                            color = previewTextColor.copy(alpha = 0.15f),
                            thickness = 1.dp,
                            modifier = Modifier.width(64.dp).padding(vertical = 4.dp)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (readerSettings.fontFamily == FontFamilySetting.SERIF) "Serif" else "Sans-Serif",
                                style = MaterialTheme.typography.labelMedium,
                                color = previewTextColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "•",
                                color = previewTextColor.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "${readerSettings.fontSizeSp.toInt()}px",
                                style = MaterialTheme.typography.labelMedium,
                                color = previewTextColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Theme Setting Card Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Loc.get("theme", lang) ?: "Tema",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val themes = listOf(
                            Triple(Loc.get("light", lang) ?: "Açık", AppThemeSetting.LIGHT, Color(0xFFF9F9F9)),
                            Triple(Loc.get("dark", lang) ?: "Karanlık", AppThemeSetting.DARK, Color(0xFF1C1C1E)),
                            Triple(Loc.get("sepia", lang) ?: "Sepya", AppThemeSetting.SEPIA, Color(0xFFF5EBD3))
                        )
                        themes.forEach { (label, setting, bg) ->
                            val isSelected = readerSettings.theme == setting
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) SacredGold else Color.Gray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateThemeSetting(setting) },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (setting == AppThemeSetting.DARK) Color.White else SacredGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (setting == AppThemeSetting.DARK) Color.White else Color(0xFF2C2C2C)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Metin Boyutu Setting Card Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = Icons.Filled.FormatSize,
                                contentDescription = null,
                                tint = SacredGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = Loc.get("font_size", lang) ?: "Yazı Boyutu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(SacredGold.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${readerSettings.fontSizeSp.toInt()}px",
                                style = MaterialTheme.typography.labelLarge,
                                color = SacredGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = readerSettings.fontSizeSp,
                            onValueChange = { viewModel.updateFontSize(it) },
                            valueRange = 14f..32f,
                            colors = SliderDefaults.colors(
                                thumbColor = SacredGold,
                                activeTrackColor = SacredGold,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("appearance_size_slider")
                        )
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Font Family Option Card Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FontDownload,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Loc.get("font_family", lang) ?: "Yazı Tipi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Serif
                        val isSerif = readerSettings.fontFamily == FontFamilySetting.SERIF
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateFontFamily(FontFamilySetting.SERIF) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSerif) SacredGold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSerif) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Aa",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSerif) SacredGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) "Serif (Classic)" else "Serif (Klasik)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSerif) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Sans-serif
                        val isSans = readerSettings.fontFamily == FontFamilySetting.SANS_SERIF
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateFontFamily(FontFamilySetting.SANS_SERIF) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSans) SacredGold.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSans) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Aa",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSans) SacredGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lang == AppLanguage.EN) "Sans-serif" else "Sans-serif (Modern)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSans) SacredGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 5. Satır Aralığı Card Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LineWeight,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Loc.get("line_height", lang) ?: "Satır Aralığı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val lineHeights = listOf(
                            Triple(Loc.get("tight", lang) ?: "Dar", LineHeightSetting.TIGHT, "1.2x"),
                            Triple(Loc.get("normal", lang) ?: "Normal", LineHeightSetting.NORMAL, "1.6x"),
                            Triple(Loc.get("wide", lang) ?: "Geniş", LineHeightSetting.WIDE, "2.0x")
                        )
                        lineHeights.forEach { (label, setting, scale) ->
                            val isSelected = readerSettings.lineHeight == setting
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateLineHeight(setting) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SacredGold else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isSelected) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = scale,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Uygulama Dili Card Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = SacredGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Loc.get("app_language", lang) ?: "Uygulama Dili",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val languages = listOf(
                            "Türkçe" to AppLanguage.TR,
                            "English" to AppLanguage.EN
                        )
                        languages.forEach { (label, setting) ->
                            val isSelected = lang == setting
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateLanguage(setting) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SacredGold else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isSelected) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
