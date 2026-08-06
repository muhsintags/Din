package com.example.ui.util

enum class UserReligion(val id: String, val titleTr: String, val titleEn: String, val iconName: String) {
    ISLAM("islam", "İslam", "Islam", "mosque"),
    CHRISTIANITY("christianity", "Hristiyanlık", "Christianity", "church"),
    JUDAISM("judaism", "Yahudilik", "Judaism", "synagogue"),
    HINDUISM("hinduism", "Hinduizm", "Hinduism", "om"),
    BUDDHISM("buddhism", "Budizm", "Buddhism", "dharma_wheel"),
    UNIVERSAL("universal", "Evrensel / Ruhani", "Universal / Spiritual", "auto_awesome"),
    SECULAR("secular", "İnançsız / Seküler", "Non-Religious / Secular", "psychology");

    fun getTitle(lang: AppLanguage): String = if (lang == AppLanguage.EN) titleEn else titleTr

    companion object {
        fun fromId(id: String?): UserReligion = values().find { it.id.equals(id, ignoreCase = true) } ?: ISLAM
    }
}

enum class UserSect(val id: String, val religionId: String, val titleTr: String, val titleEn: String) {
    // Islam
    SUNNI("sunni", "islam", "Sünnî", "Sunni"),
    SHIA("shia", "islam", "Şiî / Caferî", "Shia"),
    SUFI("sufi", "islam", "Tasavvufî / Sufi", "Sufi"),
    ISLAM_GENERAL("islam_general", "islam", "Genel İslam", "General Islam"),

    // Christianity
    CATHOLIC("catholic", "christianity", "Katolik", "Catholic"),
    ORTHODOX("orthodox", "christianity", "Doğu Ortodoks", "Eastern Orthodox"),
    PROTESTANT("protestant", "christianity", "Protestan", "Protestant"),
    CHRISTIANITY_GENERAL("christianity_general", "christianity", "Genel Hristiyanlık", "General Christianity"),

    // Judaism
    ORTHODOX_JUDAISM("orthodox_judaism", "judaism", "Ortodoks Yahudilik", "Orthodox Judaism"),
    CONSERVATIVE_JUDAISM("conservative_judaism", "judaism", "Muhafazakâr Yahudilik", "Conservative Judaism"),
    REFORM_JUDAISM("reform_judaism", "judaism", "Reformist Yahudilik", "Reform Judaism"),
    JUDAISM_GENERAL("judaism_general", "judaism", "Genel Yahudilik", "General Judaism"),

    // Hinduism
    VAISHNAVISM("vaishnavism", "hinduism", "Vaişnavizm (Vishnu)", "Vaishnavism"),
    SHAIVISM("shaivism", "hinduism", "Şaivism (Shiva)", "Shaivism"),
    HINDUISM_GENERAL("hinduism_general", "hinduism", "Genel / Advaita Vedānta", "General / Advaita Vedanta"),

    // Buddhism
    THERAVADA("theravada", "buddhism", "Theravada", "Theravada"),
    MAHAYANA("mahayana", "buddhism", "Mahayana", "Mahayana"),
    ZEN("zen", "buddhism", "Zen", "Zen"),
    BUDDHISM_GENERAL("buddhism_general", "buddhism", "Genel Budizm", "General Buddhism"),

    // Universal
    UNIVERSAL_GENERAL("universal_general", "universal", "Genel Tefekkür & Hikmet", "General Reflection & Wisdom"),

    // Secular / Non-Religious
    ATHEISM("atheism", "secular", "Ateizm", "Atheism"),
    DEISM("deism", "secular", "Deizm", "Deism"),
    AGNOSTICISM("agnosticism", "secular", "Agnostisizm", "Agnosticism"),
    FREE_THOUGHT("free_thought", "secular", "Seküler Hümanizm & Serbest Düşünce", "Secular Humanism & Free Thought");

    fun getTitle(lang: AppLanguage): String = if (lang == AppLanguage.EN) titleEn else titleTr

    companion object {
        fun getSectsForReligion(religion: UserReligion): List<UserSect> {
            return values().filter { it.religionId == religion.id }
        }

        fun fromId(id: String?, religion: UserReligion): UserSect {
            val found = values().find { it.id.equals(id, ignoreCase = true) }
            if (found != null && found.religionId == religion.id) return found
            return getSectsForReligion(religion).firstOrNull() ?: SUNNI
        }
    }
}

data class PrayerTimeInfo(
    val nameTr: String,
    val nameEn: String,
    val timeStr: String,
    val messageTr: String,
    val messageEn: String
) {
    fun getName(lang: AppLanguage) = if (lang == AppLanguage.EN) nameEn else nameTr
    fun getMessage(lang: AppLanguage) = if (lang == AppLanguage.EN) messageEn else messageTr
}

object FaithPrayerSchedule {
    fun getPrayerSchedules(religion: UserReligion, sect: UserSect): List<PrayerTimeInfo> {
        return when (religion) {
            UserReligion.ISLAM -> {
                val isShia = sect == UserSect.SHIA
                if (isShia) {
                    listOf(
                        PrayerTimeInfo("Sabah (Fecr) Namazı", "Fajr Prayer", "05:15", "Sabah Kur'an okuyuşu şahitlidir. (İsrâ 78)", "Indeed, the recitation of dawn is ever witnessed. (17:78)"),
                        PrayerTimeInfo("Öğle (Zuhr) Namazı", "Dhuhr Prayer", "12:45", "Kıl namazı güneşin batıya kaymasından gecenin kararmasına kadar. (İsrâ 78)", "Perform prayer from the decline of the sun until the darkness of the night. (17:78)"),
                        PrayerTimeInfo("İkindi (Asr) Namazı", "Asr Prayer", "13:15 / 16:30", "Namazı dosdoğru kılın, zekâtı verin ve rükû edenlerle rükû edin. (Bakara 43) [Öğle ile birleştirilebilir]", "Establish prayer and bow with those who bow. (2:43) [Can be combined with Dhuhr]"),
                        PrayerTimeInfo("Akşam (Maghrib) Namazı", "Maghrib Prayer", "19:35", "Gündüzün iki tarafında ve gecenin gündüze yakın saatlerinde namaz kıl. (Hûd 114)", "Establish prayer at the two ends of the day and at the approach of the night. (11:114)"),
                        PrayerTimeInfo("Yatsı (Isha) Namazı", "Isha Prayer", "20:00 / 21:00", "Gecenin bir kısmında secde et ve O'nu uzun gece tesbih et. (İnsân 26) [Akşam ile birleştirilebilir]", "And during the night prostrate to Him and exalt Him. (76:26) [Can be combined with Maghrib]")
                    )
                } else {
                    listOf(
                        PrayerTimeInfo("Sabah (Fecr) Namazı", "Fajr Prayer", "05:15", "Sabah Kur'an okuyuşu şahitlidir. (İsrâ 78)", "Indeed, the recitation of dawn is ever witnessed. (17:78)"),
                        PrayerTimeInfo("Öğle (Zuhr) Namazı", "Dhuhr Prayer", "12:45", "Şüphesiz namaz, müminler üzerine vakitleri belirlenmiş bir farzdır. (Nisâ 103)", "Indeed, prayer has been decreed upon the believers a decree of specified times. (4:103)"),
                        PrayerTimeInfo("İkindi (Asr) Namazı", "Asr Prayer", "16:30", "Namazlara ve orta namaza devam edin. (Bakara 238)", "Maintain with care the obligatory prayers and the middle prayer. (2:238)"),
                        PrayerTimeInfo("Akşam (Maghrib) Namazı", "Maghrib Prayer", "19:20", "Rabbini hamd ile tesbih et; güneşin doğuşundan ve batışından önce. (Tâhâ 130)", "Exalt with praise of your Lord before the rising of the sun and before its setting. (20:130)"),
                        PrayerTimeInfo("Yatsı (Isha) Namazı", "Isha Prayer", "21:00", "Gecenin saatlerinde ve gündüzün uçlarında tesbih et ki rızaya eresin. (Tâhâ 130)", "Exalt Him in hours of the night and at the ends of the day. (20:130)")
                    )
                }
            }
            UserReligion.CHRISTIANITY -> {
                when (sect) {
                    UserSect.CATHOLIC -> listOf(
                        PrayerTimeInfo("Sabah Duası & Matins", "Morning Prayer & Matins", "07:00", "Siz dünyanın ışığısınız. Dağ üzerine kurulan kent gizlenemez. (Matta 5:14)", "You are the light of the world. A city set on a hill cannot be hidden. (Matthew 5:14)"),
                        PrayerTimeInfo("Öğle Melek Duası (Angelus)", "Angelus / Midday Prayer", "12:00", "Rab'de her zaman sevinin; yine diyorum, sevinin! (Filipililer 4:4)", "Rejoice in the Lord always; again I will say, rejoice. (Philippians 4:4)"),
                        PrayerTimeInfo("Akşam Şükran Duası (Vespers)", "Evening Vespers", "18:30", "Rab çobanımdır, eksiğim olmaz. Beni yeşil çayırlarda dinlendirir. (Mezmur 23:1)", "The Lord is my shepherd; I shall not want. (Psalm 23:1)"),
                        PrayerTimeInfo("Pazar Kutsal Ayin Hatırlatması", "Sunday Holy Mass Reminder", "Pazar 09:30", "Rabb'in evine gidelim dediklerinde sevindim. (Mezmur 122:1)", "I was glad when they said to me, 'Let us go to the house of the Lord!' (Psalm 122:1)")
                    )
                    UserSect.ORTHODOX -> listOf(
                        PrayerTimeInfo("Sabah Orthros & Tevhit", "Morning Orthros Prayer", "07:00", "Işık saçan doğuşunla dünyayı aydınlatan Mesih Tanrı'mız...", "O Christ our God, Who with Your radiant birth enlighten the world..."),
                        PrayerTimeInfo("Öğle Tefekkürü & İsa Duası", "Midday Jesus Prayer", "12:00", "Rabbim İsa Mesih, Tanrı'nın Oğlu, günahkâr olan bana merhamet eyle.", "Lord Jesus Christ, Son of God, have mercy on me, a sinner."),
                        PrayerTimeInfo("Akşam Hesperinos Duası", "Evening Hesperinos", "18:30", "Akşam duamız huzuruna tütsü gibi yükselsin. (Mezmur 141:2)", "Let my prayer be counted as incense before you. (Psalm 141:2)")
                    )
                    else -> listOf(
                        PrayerTimeInfo("Sabah Adanış & Dua", "Morning Devotional", "07:30", "Her sabah yeni bir merhametle uyanırız; sadakatin büyüktür. (Ağıtlar 3:23)", "His mercies are new every morning; great is Your faithfulness. (Lamentations 3:23)"),
                        PrayerTimeInfo("Öğle Sözü & Tefekkür", "Midday Scripture Reading", "12:30", "Senin sözün adımlarıma çıra, yoluma ışıktır. (Mezmur 119:105)", "Your word is a lamp to my feet and a light to my path. (Psalm 119:105)"),
                        PrayerTimeInfo("Akşam Şükür Duası", "Evening Reflection", "20:00", "Huzur içinde yatar uyurum, çünkü yalnız sen beni güvende tutarsın. (Mezmur 4:8)", "In peace I will both lie down and sleep; for You alone make me dwell in safety. (Psalm 4:8)")
                    )
                }
            }
            UserReligion.JUDAISM -> listOf(
                PrayerTimeInfo("Sabah Duası (Shacharit)", "Morning Shacharit", "07:00", "Dinle ey İsrail! Rab Tanrı'mızdır, Rab tektir. (Yasa'nın Tekrarı 6:4)", "Hear, O Israel: The Lord our God, the Lord is one. (Deuteronomy 6:4)"),
                PrayerTimeInfo("Öğle / İkindi Duası (Mincha)", "Afternoon Mincha", "16:00", "Rab yakın olan herkese, içtenlikle çağırana yakındır. (Mezmur 145:18)", "The Lord is near to all who call on Him in truth. (Psalm 145:18)"),
                PrayerTimeInfo("Akşam Duası (Maariv)", "Evening Maariv", "20:00", "Gece vakti de Tanrı'nın şefkatini ve adaletini hatırlarız.", "In the night we remember the lovingkindness and truth of the Almighty."),
                PrayerTimeInfo("Şabbat Mum Yakma Hatırlatıcısı", "Shabbat Candle Lighting", "Cuma 17:30", "Şabbat gününü kutsal tutmak üzere hatırla. (Mısır'dan Çıkış 20:8)", "Remember the Sabbath day, to keep it holy. (Exodus 20:8)")
            )
            UserReligion.HINDUISM -> listOf(
                PrayerTimeInfo("Sabah Sandhya & Gayatri Mantra", "Morning Sandhya & Gayatri", "06:30", "Om Bhur Bhuva Swaha, Tat Savitur Varenyam - Zihnimizi ilahi ışıkla aydınlat.", "Om Bhur Bhuva Swaha, Tat Savitur Varenyam - May divine light illuminate our intellect."),
                PrayerTimeInfo("Akşam Aarti & Bhagavad Gita", "Evening Aarti & Gita Study", "18:30", "Zihnini kararlı tutan, arzuların ötesindeki nihai huzura ulaşır. (Bhagavad Gita 2:71)", "One who has controlled the mind attains ultimate peace. (Bhagavad Gita 2:71)")
            )
            UserReligion.BUDDHISM -> listOf(
                PrayerTimeInfo("Sabah Meditasyonu & Oturuş", "Morning Sitting & Mindfulness", "07:00", "Niyet tüm eylemlerin öncüsüdür; temiz bir zihin huzur getirir. (Dhammapada 1)", "Mind precedes all mental states; with a pure mind happiness follows. (Dhammapada 1)"),
                PrayerTimeInfo("Akşam Metta (Şefkat) Tefekkürü", "Evening Metta & Compassion", "20:00", "Öfke dünyada nefretle değil, yalnızca şefkatle yatışır.", "Hatred is never appeased by hatred; by love alone is it appeased.")
            )
            UserReligion.UNIVERSAL -> listOf(
                PrayerTimeInfo("Sabah Hikmet & Tefekkür", "Morning Wisdom & Reflection", "08:00", "İçindeki sakinliği bul, evrenin hakikati orada fısıldar.", "Find tranquility within; the universe speaks in quiet moments."),
                PrayerTimeInfo("Akşam Şükran & Farkındalık", "Evening Gratitude & Peace", "20:00", "Günü şükranla kapat; her nefes yeni bir farkındalık kapısıdır.", "End the day with gratitude; every breath is a doorway to awareness.")
            )
            UserReligion.SECULAR -> listOf(
                PrayerTimeInfo("Sabah Felsefi Tefekkür", "Morning Philosophical Reflection", "08:00", "Sorgulanmamış bir yaşam yaşanmaya değmez. (Sokrates)", "An unexamined life is not worth living. (Socrates)"),
                PrayerTimeInfo("Öğle Hümanist Düşünce", "Midday Humanist Reflection", "13:00", "Evrende aradığımız anlamı ve sevgiyi yine insan üretir.", "The purpose and compassion we seek in the universe is created by humanity."),
                PrayerTimeInfo("Akşam Zihinsel Dinlenme & Gün Sonu", "Evening Mindful Wind-Down", "20:00", "Bildiğim tek şey, hiçbir şey bilmediğimdir. Eleştirel düşünce zihni özgürleştirir.", "All I know is that I know nothing. Critical thinking frees the mind.")
            )
        }
    }
}
