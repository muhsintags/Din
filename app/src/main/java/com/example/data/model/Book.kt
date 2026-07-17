package com.example.data.model

data class Book(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val authorOrSource: String,
    val iconName: String,
    val coverUrl: String,
    val contentTitle: String,
    val subContentTitle: String,
    val introText: String,
    val paragraphs: List<String>,
    val footnotes: List<Pair<String, String>> = emptyList(),
    val originalLanguageName: String = "",
    val originalIntroText: String = "",
    val originalParagraphs: List<String> = emptyList(),
    val audioUrl: String = ""
)

object BookRepository {
    val books = listOf(
        Book(
            id = "quran",
            title = "Kur'an-ı Kerim",
            category = "Kutsal Metinler",
            description = "İslam'ın kutsal kitabı Hz. Muhammed'e vahyedilmiştir.",
            authorOrSource = "İslamî Dini",
            iconName = "auto_stories",
            coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAahoAuBkfv4gA97aHanH7Dkih3tYOnGJgqWa7rELPgfVPEmQi6Sey4T25hP0-z31bmZr6FklLEqK_1SsDwqHdDL3cBbcYttnmBFWwf2376lbw_sIImVxGTczZ2t8Q6i3c4LtSLqbz7DOxVk99fCZblDJJFR8nwa-ltuebuZ6VwWkxwVhua-G9sqn7Hok0qiWU0HgqAIM24ZPOSr_nP0OMM9qAkC3KeAxIdKU6LAyKdSMy_uMx-PRWUgw",
            contentTitle = "Fetih Suresi",
            subContentTitle = "Bakara Suresi",
            introText = "Doğrusu biz sana apaçık bir fetih ihsân ettik. Tâ ki Allah senin geçmiş ve gelecek günahlarını bağışlasın, üzerindeki nimetini tamamlasın ve seni dosdoğru bir yola iletsin.",
            paragraphs = listOf(
                "Şüphesiz biz sana apaçık bir fetih ihsan ettik. Ta ki Allah, senin geçmiş ve gelecek günahlarını bağışlasın, üzerindeki nimetini tamamlasın ve seni dosdoğru bir yola iletsin.",
                "Ve sana şanlı bir zaferle yardım etsin. İmanlerine iman katsınlar diye müminlerin kalplerine sükûnet indiren O'dur. Göklerin ve yerin orduları Allah'ındır. Allah bilendir, hikmet sahibidir.",
                "Bütün bunlar, mümin erkeklerle mümin kadınları, içinde ebedi kalacakları, altından nehirler akan cennetlere koyması ve onların günahlarını örtmesi içindir. İşte bu, Allah katında büyük bir kurtuluştur."
            ),
            footnotes = listOf(
                "1:1" to "Fetih kelimesi, kapalı olanın açılması, sükûnet ve kalplerin aydınlanması anlamlarına gelir.",
                "1:2" to "Bağışlama vaadi, elçinin görevindeki masumiyeti ve insanlığa olan rehberlik gücünü pekiştirir."
            ),
            originalLanguageName = "Arapça (Arabic)",
            originalIntroText = "إِنَّا فَتَحْنَا لَكَ فَتْحًا مُّبِينًا لِّيَغْفِرَ لَكَ اللَّهُ مَا تَقَدَّمَ مِن ذَنبِكَ وَمَا تَأَخَّرَ وَيُتِمَّ نِعْمَتَهُ عَلَيْكَ وَيَهْدِيَكَ صِرَاطًا مُّسْتَقِيمًا",
            originalParagraphs = listOf(
                "إِنَّا فَتَحْنَا لَكَ فَتْحًا مُّبِينًا",
                "لِّيَغْفِرَ لَكَ اللَّهُ مَا تَقَدَّمَ مِن ذَنبِكَ وَمَا تَأَخَّرَ وَيُتِمَّ نِعْمَتَهُ عَلَيْكَ وَيَهْدِيَكَ صِرَاطًا mُّسْتَقِيمًا",
                "wَيَنصُرَكَ اللَّهُ نَصْرًا عَzِيزًا. هُوَ الَّذِي أَنزَلَ السَّكِينَةَ فِي قُلُوبِ الْمُؤْمِنِينَ لِيَzْدَادُوا إِيمَانًا مَّعَ إِيمَانِهِمْ ۗ وَلِلَّهِ جُنُودُ السَّمَاوَاتِ وَالْأَرْضِ ۚ وَكَانَ اللَّهُ عَلِيمًا حَكِيمًا"
            )
        ),
        Book(
            id = "torah",
            title = "Tevrat",
            category = "Kutsal Metinler",
            description = "Tanah, Yahudilik dininin kutsal kitabıdır. Tek bir peygambere değil, farklı dönemlerde yaşamış birçok peygambere vahyedilen 24 kitabın birleşimiyle oluşmuştur.",
            authorOrSource = "İbranî Geleneği",
            iconName = "menu_book",
            coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuD6rzbh79ixK7IHnjERinDAG9yEjNtC30hCLbuDS7yoxyf6rouqg29nOLf_nzmpU78EwzXJe6p1tWVIWrDlhvum4Iqa6u0TnO-IwrpTIQYRqPExxi16Ec1M-jGgAgowmeBh-zy1rrxHJO0IsoJZT3qbsucxsJyevgd8YJ4Aq8zKLGnL_X-HEcni8iw3mD3Q82EE-LHUXOMtbQi-V4sO8PjSsZ1PgOfvziyUWmZJF9TIO70eO_m89sgKQQ",
            contentTitle = "Yaratılış",
            subContentTitle = "Bölüm 1 - 3",
            introText = TorahContent.introText,
            paragraphs = TorahContent.paragraphs,
            footnotes = TorahContent.footnotes,
            originalLanguageName = "İbranice (Hebrew)",
            originalIntroText = "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת הַשָּׁמַיִם וְאֵת הָאָרֶץ.",
            originalParagraphs = TorahContent.originalParagraphs,
            audioUrl = "https://audio.wordproject.org/bibles/audio/20/01/1.mp3"
        ),
        Book(
            id = "sermon",
            title = "İncil",
            category = "Kutsal Metinler",
            description = "Hristiyanlığın kutsal kitabı İsa'nın sözlerini içerir, ancak havariler tarafından yazılmıştır.",
            authorOrSource = "İncil (Matta)",
            iconName = "church",
            coverUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDZLBLFfgfJglvrr0EJpNX0i_-RQKNKoNSaMY1kPDhn7UuXgjODkXTeF01UxWZumZjyTS0JDvfH0iC2YadTAtPekF7mw5qqPWd1vFb_ojcbVuV9hDUWAicnoXjy_iu6S8dWvAOkI8P939gqVGbRS8d_eWsrkLCj81FxRyVfyoj3wbEYaMvnZcWUnMuV90Q3vdJ7Xbt2p3x5-WuTLRP_WQVsmS8ANqNPwHXpkMweu5dRZItKVrxcMUCv6A",
            contentTitle = "Dağdaki Vaaz",
            subContentTitle = "Matta 5 - 7",
            introText = SermonContent.introText,
            paragraphs = SermonContent.paragraphs,
            footnotes = SermonContent.footnotes,
            originalLanguageName = "Grekçe (Ancient Greek)",
            originalIntroText = "Ἰδὼν δὲ toὺς ὄχλους ἀνέβη εἰς τὸ ὄρος·",
            originalParagraphs = SermonContent.originalParagraphs,
            audioUrl = "https://audio.wordproject.org/bibles/audio/20/40/5.mp3"
        )
    )
}
