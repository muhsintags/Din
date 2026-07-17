package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SacredGold
import com.example.ui.util.AppLanguage
import com.example.ui.util.Loc
import com.example.ui.viewmodel.FontFamilySetting
import com.example.ui.viewmodel.ScriptureViewModel

// 1. Data Class for easy modification and clean code architecture
data class DictionaryTerm(
    val term: String,                    // Term in origin language or turkish
    val origin: String,                  // Origin (Arabic, Hebrew, Greek, etc.)
    val category: TermCategory,          // Category (Islamic, Biblical, General)
    val meaningTr: String,               // Meaning in Turkish
    val meaningEn: String,               // Meaning in English
    val exampleTr: String = "",          // Example context / verse in Turkish
    val exampleEn: String = ""           // Example context / verse in English
)

enum class TermCategory {
    ALL, ISLAMIC, BIBLICAL, GENERAL
}

object DictionaryData {
    val defaultTerms = listOf(
        DictionaryTerm(
            term = "Tefekkür",
            origin = "Arapça (Fkr)",
            category = TermCategory.ISLAMIC,
            meaningTr = "Yaratılış, hayatın anlamı ve kutsal metinler üzerinde derinlemesine, sakin ve bilinçli bir biçimde düşünme eylemi.",
            meaningEn = "The act of deep, quiet, and conscious reflection and contemplation on creation, the meaning of life, and sacred scriptures.",
            exampleTr = "Onlar göklerin ve yerin yaratılışı üzerinde tefekkür ederler. (Âl-i İmrân, 191)",
            exampleEn = "And they contemplate the creation of the heavens and the earth. (Quran, 3:191)"
        ),
        DictionaryTerm(
            term = "Ahid (Covenant)",
            origin = "İbranice (Berit) / Arapça",
            category = TermCategory.BIBLICAL,
            meaningTr = "Tanrı ile insanlar veya peygamberler arasında karşılıklı sevgi, bağlılık ve sorumluluğa dayanan kutsal sözleşme.",
            meaningEn = "A sacred covenant or solemn agreement made between God and humanity or prophets, implying mutual commitment and responsibility.",
            exampleTr = "Seninle aramda bir ahit keseceğim ve seni son derece çoğaltacağım. (Yaratılış 17:2)",
            exampleEn = "I will establish my covenant between me and you and will greatly multiply your numbers. (Genesis 17:2)"
        ),
        DictionaryTerm(
            term = "Mesih (Messiah)",
            origin = "İbranice (Mašíaḥ) / Arapça",
            category = TermCategory.BIBLICAL,
            meaningTr = "Kurtarıcı ünvanı; Tanrı tarafından kutsal yağ ile meshedilerek özel bir göreve atanmış olan kimse (Hz. İsa).",
            meaningEn = "A title meaning 'the anointed one'; a savior or leader anointed by God for a special divine mission (Jesus Christ).",
            exampleTr = "Melekler demişti ki: Ey Meryem! Şüphesiz Allah seni, kendisinden bir kelime ile müjdeliyor: Adı Mesih İsa'dır.",
            exampleEn = "The angels said: O Mary! God gives you glad tidings of a Word from Him: His name will be Messiah Jesus."
        ),
        DictionaryTerm(
            term = "Hanîf",
            origin = "Arapça (Hnf)",
            category = TermCategory.ISLAMIC,
            meaningTr = "Cahiliye döneminde putperestliği reddederek Hz. İbrahim'in tek tanrılı saf inancına bağlı kalan kimse.",
            meaningEn = "A monotheist in pre-Islamic Arabia who rejected polytheism and remained faithful to the pure monotheism of Abraham.",
            exampleTr = "İbrahim ne bir Yahudi ne de bir Hristiyandı; o, dosdoğru Müslüman bir hanîf idi. (Âl-i İmrân, 67)",
            exampleEn = "Abraham was neither a Jew nor a Christian, but he was one pure of faith (hanîf), a Muslim. (Quran, 3:67)"
        ),
        DictionaryTerm(
            term = "Hikmet (Wisdom)",
            origin = "Arapça (Hkm)",
            category = TermCategory.GENERAL,
            meaningTr = "Eşyanın hakikatini kavrama, derin bilgi ve bilgece davranma yeteneği; dürüst, erdemli ve adil yaşama sanatı.",
            meaningEn = "The deep understanding of the reality of things, paired with sound judgment and virtuous action; the art of living justly and wisely.",
            exampleTr = "Allah hikmeti dilediğine verir. Kime hikmet verilmişse, şüphesiz ona büyük bir hayır verilmiştir. (Bakara, 269)",
            exampleEn = "He grants wisdom to whom He pleases, and whoever is granted wisdom has indeed been granted abundant good. (Quran, 2:269)"
        ),
        DictionaryTerm(
            term = "Havari (Apostle)",
            origin = "Habeşçe / Arapça",
            category = TermCategory.BIBLICAL,
            meaningTr = "Hz. İsa'nın öğreti ve mesajını yaymak üzere seçtiği ve özel yetkiyle görevlendirdiği on iki yakın yardımcısından her biri.",
            meaningEn = "Each of the twelve close disciples chosen and commissioned by Jesus Christ to preach and spread His gospel and teachings.",
            exampleTr = "Havariler şöyle demişti: Biz Allah yolunun yardımcılarıyız, Allah'a iman ettik. (Saff, 14)",
            exampleEn = "The disciples said: We are helpers of God; we have believed in God. (Quran, 61:14)"
        ),
        DictionaryTerm(
            term = "Esbâb-ı Nüzûl",
            origin = "Arapça (Nzl)",
            category = TermCategory.ISLAMIC,
            meaningTr = "Kur'an ayetlerinin veya surelerinin hangi tarihi olaylar, sorular veya durumlar üzerine indirildiğini inceleyen bilim dalı.",
            meaningEn = "The historical occurrences, questions, or contexts that triggered the revelation of specific Quranic verses or chapters.",
            exampleTr = "Bu bilim dalı, ayetlerin bağlamını ve asıl kastını anlamak için kritik bir öneme sahiptir.",
            exampleEn = "This discipline is crucial for understanding the chronological context and target message of the scriptures."
        ),
        DictionaryTerm(
            term = "Apokrif (Apocrypha)",
            origin = "Yunanca (Apokryphos)",
            category = TermCategory.BIBLICAL,
            meaningTr = "Gizli veya şüpheli anlamına gelen, resmi kutsal kitap kanonuna dahil edilmeyen ancak dini ve tarihi öneme sahip yazılar.",
            meaningEn = "Meaning hidden or obscure; historical religious writings not accepted into the official biblical canon but containing spiritual insights.",
            exampleTr = "Apokrif metinler Erken Hristiyanlık ve Musevilik tarihindeki farklı ekolleri anlamaya yardımcı olur.",
            exampleEn = "Apocryphal books help scholars study the diverse theological landscapes of early Judaism and Christianity."
        ),
        DictionaryTerm(
            term = "Tefsir (Exegesis)",
            origin = "Arapça (Fsr)",
            category = TermCategory.ISLAMIC,
            meaningTr = "Kutsal yazıları, özellikle Kur'an ayetlerini dilbilimsel, tarihsel ve ilahiyat açılarından açıklayan ve yorumlayan ilim.",
            meaningEn = "The critical interpretation and comprehensive explanation of sacred texts, especially the Quran, using historical and linguistic analysis.",
            exampleTr = "Klasik tefsirler ayetlerin edebi, tarihi ve felsefi boyutlarını aydınlatır.",
            exampleEn = "Classical commentaries elucidate the literary, historical, and philosophical dimensions of the verses."
        ),
        DictionaryTerm(
            term = "Vahy (Revelation)",
            origin = "Arapça (Why)",
            category = TermCategory.GENERAL,
            meaningTr = "Tanrısal bilgi, buyruk veya hikmetlerin peygamberlerin kalbine doğrudan ya da bir elçi melek vasıtasıyla ulaştırılması.",
            meaningEn = "The supernatural communication of divine knowledge, commandments, or wisdom directly or via an angelic messenger to the prophets.",
            exampleTr = "Biz Nuh'a ve ondan sonraki peygamberlere vahyettiğimiz gibi sana da vahyettik. (Nisâ, 163)",
            exampleEn = "Indeed, We have revealed to you as We revealed to Noah and the prophets after him. (Quran, 4:163)"
        ),
        DictionaryTerm(
            term = "Zebur (Psalms)",
            origin = "İbranice (Mizmor) / Arapça",
            category = TermCategory.BIBLICAL,
            meaningTr = "Davud peygambere indirilen, Tanrı'yı öven şiirsel dualar, ilahiler ve bilgelik sözlerini içeren kutsal kitap.",
            meaningEn = "The collection of sacred songs, poems, and prayers revealed to King David, praising God's majesty and seeking righteousness.",
            exampleTr = "Gerçekten biz Davud'a da Zebur'u verdik. (İsrâ, 55)",
            exampleEn = "And to David We gave the Psalms. (Quran, 17:55)"
        ),
        DictionaryTerm(
            term = "Hanefî / Şafiî / Caferî",
            origin = "Arapça",
            category = TermCategory.ISLAMIC,
            meaningTr = "İslam hukukunun (fıkıh) kurumsallaşmış farklı yorum, anlama ve ameli uygulama ekollerinden (mezhep) bazıları.",
            meaningEn = "Some of the mainstream traditional schools of Islamic jurisprudence and legal methodology (madhhab).",
            exampleTr = "Fıkhi ekoller, metinlerin yorumlanmasındaki zenginlik ve pratik çözümleri gösterir.",
            exampleEn = "Legal schools showcase the analytical wealth and pragmatic adaptability in interpreting core scriptures."
        ),
        DictionaryTerm(
            term = "Adalet (Righteousness)",
            origin = "Arapça / İbranice (Tzedek)",
            category = TermCategory.GENERAL,
            meaningTr = "Her şeyi yerli yerine koyma, hak sahibine hakkını verme ve her koşulda doğruluktan sapmama ilkesi.",
            meaningEn = "The standard of moral perfection, giving everyone their due, and acting in alignment with divine truth and fairness.",
            exampleTr = "Allah adaleti, iyilik yapmayı ve yakınlara yardım etmeyi emreder. (Nahl, 90)",
            exampleEn = "God commands justice and fair dealing, and kindness to close relatives. (Quran, 16:90)"
        ),
        DictionaryTerm(
            term = "Sükûnet (Shalom/Salam)",
            origin = "Arapça (Skn) / İbranice",
            category = TermCategory.GENERAL,
            meaningTr = "Kutsal metinlerin vaat ettiği içsel barış, dinginlik ve kalbin ilahi huzurla mutmain olması durumu.",
            meaningEn = "The inner state of deep peace, calmness, and spiritual tranquility resulting from alignment with divine guidance.",
            exampleTr = "O, inananların kalplerine güven ve sükûnet indirendir. (Fetih, 4)",
            exampleEn = "It is He who sent down tranquility into the hearts of the believers. (Quran, 48:4)"
        ),
        DictionaryTerm(
            term = "Aydınlanma (Enlightenment)",
            origin = "Türkçe / İngilizce",
            category = TermCategory.GENERAL,
            meaningTr = "Zihnin ve kalbin batıl inançlardan, cehaletten ve karanlıktan kurtularak ilahi hakikat ve bilgiyle nurlanması.",
            meaningEn = "The process of the mind and heart becoming illuminated with divine truth and knowledge, breaking free from ignorance and darkness.",
            exampleTr = "Şüphesiz bu Kur'an en doğru yola iletir. (İsrâ, 9)",
            exampleEn = "Indeed, this Quran guides to that which is most suitable. (Quran, 17:9)"
        ),
        DictionaryTerm(
            term = "Barış (Peace)",
            origin = "Türkçe / Arapça (Selâm)",
            category = TermCategory.GENERAL,
            meaningTr = "Savaşın, çatışmanın ve huzursuzluğun olmaması hali; hem toplumsal düzeyde uyum hem de bireysel düzeyde içsel huzur.",
            meaningEn = "The absence of war, conflict, and unrest; representing both societal harmony and individual inner tranquility.",
            exampleTr = "Ey iman edenler! Hepiniz topluca barış ve selamete girin. (Bakara, 208)",
            exampleEn = "O you who have believed, enter into peace completely. (Quran, 2:208)"
        ),
        DictionaryTerm(
            term = "Merhamet (Mercy/Compassion)",
            origin = "Arapça (Rhm) / İbranice (Rahamim)",
            category = TermCategory.GENERAL,
            meaningTr = "Bir kimsenin veya başka bir canlının acısına, kederine karşı duyarlı olma ve ona iyilikle, şefkatle yaklaşma duygusu.",
            meaningEn = "Compassion or forgiveness shown toward someone whom it is within one's power to punish or harm; benevolent treatment of others.",
            exampleTr = "O, merhametlilerin en merhametlisidir. (Yusuf, 92)",
            exampleEn = "And He is the most merciful of the merciful. (Quran, 12:92)"
        ),
        DictionaryTerm(
            term = "Hakikat (Truth)",
            origin = "Arapça (Hkk) / Grekçe (Aletheia)",
            category = TermCategory.GENERAL,
            meaningTr = "Gerçeklik, asıl, değişmeyen doğru; kutsal metinlerin ulaştırmak istediği nihai gerçeklik ve varoluşsal bilgelik.",
            meaningEn = "The quality or state of being true, actual, or honest; the ultimate reality and existential wisdom conveyed by sacred texts.",
            exampleTr = "De ki: Hak geldi, batıl yok oldu. (İsrâ, 81)",
            exampleEn = "Say, 'Truth has come, and falsehood has departed.' (Quran, 17:81)"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: ScriptureViewModel,
    onNavigateToSettings: () -> Unit
) {
    val readerSettings by viewModel.readerSettings.collectAsState()
    val lang = readerSettings.language

    // State for Search and Categories
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TermCategory.ALL) }
    var expandedTermId by remember { mutableStateOf<String?>(null) }
    
    // Dialog state for adding a custom term (making it super customizable)
    var showAddDialog by remember { mutableStateOf(false) }
    var newTermName by remember { mutableStateOf("") }
    var newTermOrigin by remember { mutableStateOf("") }
    var newTermCategory by remember { mutableStateOf(TermCategory.GENERAL) }
    var newTermMeaningTr by remember { mutableStateOf("") }
    var newTermMeaningEn by remember { mutableStateOf("") }
    var newTermExampleTr by remember { mutableStateOf("") }
    var newTermExampleEn by remember { mutableStateOf("") }

    // Initial rich predefined dictionary terms list
    val initialTerms = remember {
        mutableStateListOf<DictionaryTerm>().apply {
            addAll(DictionaryData.defaultTerms)
        }
    }

    // Filtered dictionary list based on search and category
    val filteredTerms = remember(searchQuery, selectedCategory, initialTerms.size) {
        initialTerms.filter { term ->
            val matchCategory = selectedCategory == TermCategory.ALL || term.category == selectedCategory
            val query = searchQuery.lowercase().trim()
            val matchQuery = query.isEmpty() || 
                    term.term.lowercase().contains(query) ||
                    term.origin.lowercase().contains(query) ||
                    term.meaningTr.lowercase().contains(query) ||
                    term.meaningEn.lowercase().contains(query)
            matchCategory && matchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = SacredGold
                        )
                        Text(
                            text = if (lang == AppLanguage.EN) "Scriptorium Dictionary" else "Kavramlar Sözlüğü",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Theme",
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
                .padding(horizontal = 16.dp)
        ) {
            
            // 2. Beautiful Modern Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (lang == AppLanguage.EN) "Search theology, terms or wisdom..." else "Kavram, terim veya bilgelik ara...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = SacredGold
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
                    .padding(vertical = 8.dp)
                    .testTag("dictionary_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SacredGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                )
            )

            // 3. Category Scrolling Row (Filters)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                val categories = listOf(
                    Triple(TermCategory.ALL, if (lang == AppLanguage.EN) "All" else "Hepsi", Icons.Filled.AllInclusive),
                    Triple(TermCategory.ISLAMIC, if (lang == AppLanguage.EN) "Quranic" else "Kur'anî", Icons.Filled.Star),
                    Triple(TermCategory.BIBLICAL, if (lang == AppLanguage.EN) "Biblical" else "Kitab-ı Mukaddes", Icons.Filled.MenuBook),
                    Triple(TermCategory.GENERAL, if (lang == AppLanguage.EN) "Theology" else "Genel Teoloji", Icons.Filled.SelfImprovement)
                )

                items(categories) { (cat, label, icon) ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else SacredGold
                                )
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SacredGold,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            selectedBorderColor = SacredGold,
                            borderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // 4. Dictionary Terms List
            if (filteredTerms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (lang == AppLanguage.EN) "No matching concepts found." else "Aradığınız kriterde kavram bulunamadı.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { searchQuery = ""; selectedCategory = TermCategory.ALL },
                            colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                        ) {
                            Text(if (lang == AppLanguage.EN) "Reset Filters" else "Aramayı Temizle", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTerms) { item ->
                        val isExpanded = expandedTermId == item.term
                        val displayFont = if (readerSettings.fontFamily == FontFamilySetting.SERIF) FontFamily.Serif else FontFamily.SansSerif

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, shape = RoundedCornerShape(16.dp))
                                .animateContentSize(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isExpanded) SacredGold.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedTermId = if (isExpanded) null else item.term
                                    }
                                    .padding(16.dp)
                            ) {
                                // Term Title & Category Tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.term,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = item.origin,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SacredGold,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (item.category) {
                                                    TermCategory.ISLAMIC -> Color(0xFFE8F5E9)
                                                    TermCategory.BIBLICAL -> Color(0xFFE3F2FD)
                                                    TermCategory.GENERAL -> Color(0xFFFFF3E0)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (item.category) {
                                                TermCategory.ISLAMIC -> if (lang == AppLanguage.EN) "Quran" else "Kur'an"
                                                TermCategory.BIBLICAL -> if (lang == AppLanguage.EN) "Bible" else "Kitab-ı Mukaddes"
                                                TermCategory.GENERAL -> if (lang == AppLanguage.EN) "Theology" else "Genel"
                                                else -> ""
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (item.category) {
                                                TermCategory.ISLAMIC -> Color(0xFF2E7D32)
                                                TermCategory.BIBLICAL -> Color(0xFF1565C0)
                                                TermCategory.GENERAL -> Color(0xFFEF6C00)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Meaning preview or full text
                                Text(
                                    text = if (lang == AppLanguage.EN) item.meaningEn else item.meaningTr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = displayFont,
                                    lineHeight = 22.sp,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Expandable Extra Info (Examples)
                                if (isExpanded) {
                                    val exampleText = if (lang == AppLanguage.EN) item.exampleEn else item.exampleTr
                                    if (exampleText.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FormatQuote,
                                                contentDescription = null,
                                                tint = SacredGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = exampleText,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp,
                                                fontFamily = displayFont,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. Beautiful Add Custom Term Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.EN) "Add Custom Concept" else "Yeni Kavram Ekle",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newTermName,
                        onValueChange = { newTermName = it },
                        label = { Text(if (lang == AppLanguage.EN) "Concept Name" else "Kavram Adı") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SacredGold)
                    )

                    OutlinedTextField(
                        value = newTermOrigin,
                        onValueChange = { newTermOrigin = it },
                        label = { Text(if (lang == AppLanguage.EN) "Origin (e.g., Hebrew, Arabic)" else "Köken (Örn: Arapça, İbranice)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SacredGold)
                    )

                    // Category Selection Row
                    Text(
                        text = if (lang == AppLanguage.EN) "Category" else "Kategori",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val cats = listOf(
                            TermCategory.ISLAMIC to (if (lang == AppLanguage.EN) "Quran" else "Kur'an"),
                            TermCategory.BIBLICAL to (if (lang == AppLanguage.EN) "Bible" else "Kitap"),
                            TermCategory.GENERAL to (if (lang == AppLanguage.EN) "General" else "Genel")
                        )
                        cats.forEach { (cat, label) ->
                            val active = newTermCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) SacredGold else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(
                                        width = 1.dp,
                                        color = if (active) SacredGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { newTermCategory = cat },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newTermMeaningTr,
                        onValueChange = { newTermMeaningTr = it },
                        label = { Text("Türkçe Açıklama / Anlamı") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SacredGold)
                    )

                    OutlinedTextField(
                        value = newTermMeaningEn,
                        onValueChange = { newTermMeaningEn = it },
                        label = { Text("English Meaning / Explanation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SacredGold)
                    )

                    OutlinedTextField(
                        value = newTermExampleTr,
                        onValueChange = { newTermExampleTr = it },
                        label = { Text("Örnek Cümle / Ayet (Türkçe)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SacredGold)
                    )

                    OutlinedTextField(
                        value = newTermExampleEn,
                        onValueChange = { newTermExampleEn = it },
                        label = { Text("Example Context / Verse (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SacredGold)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTermName.isNotBlank() && (newTermMeaningTr.isNotBlank() || newTermMeaningEn.isNotBlank())) {
                            initialTerms.add(
                                0, // Insert at top
                                DictionaryTerm(
                                    term = newTermName.trim(),
                                    origin = newTermOrigin.ifBlank { if (lang == AppLanguage.EN) "Unknown" else "Bilinmiyor" }.trim(),
                                    category = newTermCategory,
                                    meaningTr = newTermMeaningTr.ifBlank { newTermMeaningEn }.trim(),
                                    meaningEn = newTermMeaningEn.ifBlank { newTermMeaningTr }.trim(),
                                    exampleTr = newTermExampleTr.trim(),
                                    exampleEn = newTermExampleEn.trim()
                                )
                            )
                            // Reset state
                            newTermName = ""
                            newTermOrigin = ""
                            newTermCategory = TermCategory.GENERAL
                            newTermMeaningTr = ""
                            newTermMeaningEn = ""
                            newTermExampleTr = ""
                            newTermExampleEn = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold)
                ) {
                    Text(if (lang == AppLanguage.EN) "Save" else "Ekle", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(if (lang == AppLanguage.EN) "Cancel" else "İptal")
                }
            }
        )
    }
}
