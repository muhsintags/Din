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
        ),
        Book(
            id = "talmud",
            title = "Talmud",
            category = "Diğer Metinler",
            description = "Yahudi sivil ve şer'î hukuk, gelenek ve felsefe tartışmaları kütüphanesi.",
            authorOrSource = "Babil Akademileri",
            iconName = "menu_book",
            coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f",
            contentTitle = "Berakhot 2a",
            subContentTitle = "Şema Vakitleri",
            introText = "Talmud (Babil Talmudu, Berakhot 2a), akşam Şema dualarının okunma vakitleri ve hahamların kutsal metin tartışmalarıyla başlar.",
            paragraphs = listOf(
                "MİŞNA: Akşamları Şema ne zamandan itibaren okunabilir? Rahiplerin, arınma kurbanlarının ardından yemek yemek üzere içeri girdikleri saatten, birinci nöbetin sonuna kadar.",
                "GEMARA: Bu hükmün dayanağı nedir? Tevrat'ta şöyle der: 'Yatarken ve kalkarken.' Akşam Şeması yatma vaktine karşılık gelir.",
                "Hahamlar şöyle öğretirler: 'Yatarken' ifadesi yatakta olunan genel vakti belirtir, bu da akşama tekabül eder."
            ),
            originalLanguageName = "Aramice / İbranice",
            originalIntroText = "מאימתי קורין את שמע בערבין? משעה שהכהנים נכנסים לאכול בתרומתן...",
            originalParagraphs = listOf(
                "מאימתי קורין את שמע בערבין משעה שהכהנים נכנסים לאכול בתרומתן עד סוף האשמורה הראשונה דברי רבי אליעזר",
                "וחכמים אומרים עד חצות רבן גמליאל אומר עד שיעله עמוד השחר"
            ),
            footnotes = listOf(
                "Mişna" to "Talmud'un çekirdeğini oluşturan, Yahudi sözlü kanunlarının ilk yazılı derlemesidir.",
                "Gemara" to "Mişna üzerine yapılan geniş akademik ve teolojik tartışmaları içeren bölümdür."
            )
        ),
        Book(
            id = "bukhari",
            title = "Sahih-i Buharî",
            category = "Diğer Metinler",
            description = "İslam dünyasında Kur'an'dan sonra en güvenilir kabul edilen hadis külliyatı.",
            authorOrSource = "İmam Buharî",
            iconName = "auto_stories",
            coverUrl = "https://images.unsplash.com/photo-1584282479234-df7a6b986872",
            contentTitle = "Hadis 1",
            subContentTitle = "Vahyin Başlangıcı",
            introText = "Sahih-i Buharî, ünlü niyet hadisi ile başlar: 'Ameller ancak niyetlere göredir...'",
            paragraphs = listOf(
                "1: Ömer bin el-Hattâb'dan (r.a.) nakledildiğine göre, o minberde iken şöyle demiştir: Resûlullah'ın (s.a.v.) şöyle buyurduğunu işittim: 'Ameller ancak niyetlere göredir. Herkes için ancak niyet ettiği şey vardır. Kimin hicreti Allah ve Resûlü için ise hicreti Allah ve Resûlü'nedir. Kimin de hicreti elde edeceği bir dünyalık veya evleneceği bir kadın için ise hicreti, hicret ettiği şeyedir.'"
            ),
            originalLanguageName = "Arapça (Arabic)",
            originalIntroText = "حَدَّثَنَا الْحُمَيْدِيُّ عَبْدُ اللَّهِ بْنُ الزُّבَيْرِ، قَالَ حَدَّثَنَا سُفْيَانُ...",
            originalParagraphs = listOf(
                "إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى، فمن كانت هجرته إلى الله ورسوله فهجرته إلى الله ورسوله، ومن كانت هجرته لدنيا يصيبها أو امرأة يتزوجها فهجرته إلى ما هاجر إليه"
            ),
            footnotes = listOf(
                "Kaynak" to "İmam Buharî hadis külliyatı, Kitabu Bed'il-Vahy, 1. Hadis.",
                "Hadis No" to "Hadis numarası 1'dir. Sahih derecesindedir."
            )
        )
    )
}
