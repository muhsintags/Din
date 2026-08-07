package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.theme.SacredGold
import java.io.File
import java.io.FileOutputStream

enum class StoryTemplate(val title: String, val badge: String, val bgColors: List<Color>, val cardBg: Color, val textColor: Color, val accentColor: Color) {
    NGL_PINK(
        title = "NGL Pembe",
        badge = "✨ SCRIPTorium • Tefekkür ✨",
        bgColors = listOf(Color(0xFFFF007A), Color(0xFF7928CA), Color(0xFF4A00E0)),
        cardBg = Color(0xF2FFFFFF),
        textColor = Color(0xFF111827),
        accentColor = Color(0xFFE11D48)
    ),
    SACRED_GOLD(
        title = "Altın Medine",
        badge = "🕌 SCRIPTorium • Kutsal Metin",
        bgColors = listOf(Color(0xFF091712), Color(0xFF133227), Color(0xFF091712)),
        cardBg = Color(0x1AD4AF37),
        textColor = Color(0xFFF9FAFB),
        accentColor = Color(0xFFD4AF37)
    ),
    MIDNIGHT_GLOW(
        title = "Aura Gece",
        badge = "🌌 SCRIPTorium • Gece Tefekkürü",
        bgColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF020617)),
        cardBg = Color(0x3338BDF8),
        textColor = Color(0xFFF8FAFC),
        accentColor = Color(0xFF38BDF8)
    ),
    PARCHMENT_MANUSCRIPT(
        title = "Tarihi Parşömen",
        badge = "📜 SCRIPTorium • Kadim Kitap",
        bgColors = listOf(Color(0xFFF5EBE0), Color(0xFFE6D5C3), Color(0xFFD5C3B1)),
        cardBg = Color(0xCCFFFFFF),
        textColor = Color(0xFF2C1A0E),
        accentColor = Color(0xFF8C6239)
    ),
    AURORA_HORIZON(
        title = "Şafak Işığı",
        badge = "🌅 SCRIPTorium • Sabah Tefekkürü",
        bgColors = listOf(Color(0xFF1A2A6C), Color(0xFFB21F1F), Color(0xFFFDBB2D)),
        cardBg = Color(0xE6FFFFFF),
        textColor = Color(0xFF1E293B),
        accentColor = Color(0xFFD97706)
    ),
    MINIMAL_DARK(
        title = "Sade Siyah",
        badge = "⚡ SCRIPTorium",
        bgColors = listOf(Color(0xFF09090B), Color(0xFF18181B), Color(0xFF09090B)),
        cardBg = Color(0xFF18181B),
        textColor = Color(0xFFFAFAFA),
        accentColor = Color(0xFFA1A1AA)
    )
}

enum class CardRatio(val label: String, val aspectRatio: Float) {
    STORY_9_16("Hikaye (9:16)", 0.5625f),
    SQUARE_1_1("Kare (1:1)", 1.0f)
}

private fun cleanVerseQuotes(raw: String): String {
    var t = raw.trim()
    while (t.startsWith("\"") || t.startsWith("“") || t.startsWith("”") || t.startsWith("'") || t.startsWith("«") || t.startsWith("„")) {
        t = t.substring(1).trim()
    }
    while (t.endsWith("\"") || t.endsWith("“") || t.endsWith("”") || t.endsWith("'") || t.endsWith("»") || t.endsWith("“") || t.endsWith("„")) {
        t = t.substring(0, t.length - 1).trim()
    }
    return t
}

@Composable
fun StoryVerseShareDialog(
    verseText: String,
    reference: String,
    originalText: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cleanText = remember(verseText) { cleanVerseQuotes(verseText) }
    var selectedTemplate by remember { mutableStateOf(StoryTemplate.NGL_PINK) }
    var selectedRatio by remember { mutableStateOf(CardRatio.STORY_9_16) }
    var userHandle by remember { mutableStateOf("@scriptorium") }
    var showOriginal by remember { mutableStateOf(false) }

    val picture = remember { Picture() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(selectedTemplate.bgColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Şablonla Paylaş",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instagram & NGL Hikaye Kartı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Live Story Card Preview Box with Picture Capture
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (selectedRatio == CardRatio.STORY_9_16) 0.85f else 0.95f)
                                .aspectRatio(selectedRatio.aspectRatio)
                                .clip(RoundedCornerShape(24.dp))
                                .drawWithCache {
                                    val width = size.width.toInt()
                                    val height = size.height.toInt()
                                    onDrawWithContent {
                                        val pictureCanvas = androidx.compose.ui.graphics.Canvas(
                                            picture.beginRecording(width, height)
                                        )
                                        draw(this, layoutDirection, pictureCanvas, size) {
                                            this@onDrawWithContent.drawContent()
                                        }
                                        picture.endRecording()
                                        drawContent()
                                    }
                                }
                                .background(Brush.linearGradient(selectedTemplate.bgColors))
                                .padding(if (selectedRatio == CardRatio.STORY_9_16) 20.dp else 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render template card content
                            VerseStoryCardContent(
                                template = selectedTemplate,
                                verseText = verseText,
                                reference = reference,
                                originalText = if (showOriginal) originalText else null,
                                userHandle = userHandle,
                                isStoryRatio = selectedRatio == CardRatio.STORY_9_16
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Template Selection Row
                    Text(
                        text = "ŞABLON SEÇİNİ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(StoryTemplate.values()) { tmpl ->
                            val isSelected = tmpl == selectedTemplate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) SacredGold else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedTemplate = tmpl }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(tmpl.bgColors))
                                    )
                                    Text(
                                        text = tmpl.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Format & Customization Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Ratio Chips
                        CardRatio.values().forEach { ratio ->
                            val isSelected = ratio == selectedRatio
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRatio = ratio },
                                label = { Text(ratio.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (ratio == CardRatio.STORY_9_16) Icons.Filled.CropPortrait else Icons.Filled.CropSquare,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Handle and Original Text Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = userHandle,
                            onValueChange = { userHandle = it },
                            label = { Text("Kullanıcı Adı / Not") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!originalText.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Orijinal", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = showOriginal,
                                    onCheckedChange = { showOriginal = it }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Instagram Story Share Primary Button
                    Button(
                        onClick = {
                            shareVerseImage(
                                context = context,
                                picture = picture,
                                verseText = verseText,
                                reference = reference,
                                isInstagramDirect = true
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE1306C)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instagram Story'de Paylaş",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Generic Image Share Button
                        OutlinedButton(
                            onClick = {
                                shareVerseImage(
                                    context = context,
                                    picture = picture,
                                    verseText = verseText,
                                    reference = reference,
                                    isInstagramDirect = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Görsel Paylaş", style = MaterialTheme.typography.labelMedium)
                        }

                        // Copy Text Button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Ayet", "\"$cleanText\"\n— $reference")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Ayet kopyalandı", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Metni Kopyala", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseStoryCardContent(
    template: StoryTemplate,
    verseText: String,
    reference: String,
    originalText: String?,
    userHandle: String,
    isStoryRatio: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isStoryRatio) 12.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // NGL Badge / Top Emblem
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (template == StoryTemplate.NGL_PINK) Color(0xFFFF4D8D)
                    else template.accentColor.copy(alpha = 0.2f)
                )
                .border(
                    width = 1.dp,
                    color = template.accentColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = template.badge,
                style = MaterialTheme.typography.labelSmall,
                color = if (template == StoryTemplate.NGL_PINK) Color.White else template.textColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Central Main Card Overlay
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = template.cardBg
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = if (template == StoryTemplate.SACRED_GOLD) {
                BorderStroke(1.5.dp, template.accentColor)
            } else null
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Opening Quote Icon
                Icon(
                    imageVector = Icons.Filled.FormatQuote,
                    contentDescription = null,
                    tint = template.accentColor,
                    modifier = Modifier.size(32.dp)
                )

                // Optional Original Script
                if (!originalText.isNullOrBlank()) {
                    Text(
                        text = originalText,
                        color = template.textColor.copy(alpha = 0.85f),
                        fontSize = if (isStoryRatio) 16.sp else 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Translated Verse Text
                val cleanText = remember(verseText) { cleanVerseQuotes(verseText) }
                Text(
                    text = "\"$cleanText\"",
                    color = template.textColor,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = if (isStoryRatio) 17.sp else 15.sp,
                    lineHeight = if (isStoryRatio) 26.sp else 22.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )

                // Divider Line
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(template.accentColor)
                )

                // Reference Citation
                Text(
                    text = reference.uppercase(),
                    color = template.accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Bottom User Branding / Handle Tag
        if (userHandle.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = template.textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = userHandle,
                    style = MaterialTheme.typography.labelSmall,
                    color = template.textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun createStoryBitmap(picture: Picture): Bitmap {
    val srcWidth = picture.width.coerceAtLeast(1)
    val srcHeight = picture.height.coerceAtLeast(1)

    val targetWidth = 1080
    val scale = targetWidth.toFloat() / srcWidth.toFloat()
    val targetHeight = (srcHeight * scale).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(scale, scale)
    canvas.drawPicture(picture)
    return bitmap
}

private fun shareVerseImage(
    context: Context,
    picture: Picture,
    verseText: String,
    reference: String,
    isInstagramDirect: Boolean
) {
    val cleanText = cleanVerseQuotes(verseText)
    try {
        val bitmap = createStoryBitmap(picture)

        val cachePath = File(context.cacheDir, "shared_verses")
        cachePath.mkdirs()
        val file = File(cachePath, "scriptorium_story_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "\"$cleanText\"\n\n— $reference\n\nScriptorium")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (isInstagramDirect) {
            shareIntent.setPackage("com.instagram.android")
        }

        val chooser = Intent.createChooser(shareIntent, "Ayet Şablonunu Paylaş")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback to generic chooser if instagram app is not installed
        try {
            val bitmap = createStoryBitmap(picture)

            val cachePath = File(context.cacheDir, "shared_verses")
            cachePath.mkdirs()
            val file = File(cachePath, "scriptorium_story_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "\"$cleanText\"\n\n— $reference\n\nScriptorium")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(fallbackIntent, "Ayet Şablonunu Paylaş")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (err: Exception) {
            Toast.makeText(context, "Paylaşım başlatılamadı: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
