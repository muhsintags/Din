package com.example.data.model

data class BibleBook(
    val id: String,
    val nameTurkish: String,
    val nameEnglish: String,
    val chaptersCount: Int,
    val bookNumber: Int,
    val sourceLanguage: String
)

object BibleRepository {
    val torahBooks = listOf(
        BibleBook("Genesis", "Yaratılış", "Genesis", 50, 1, "İbranice (Hebrew)"),
        BibleBook("Exodus", "Çıkış", "Exodus", 40, 2, "İbranice (Hebrew)"),
        BibleBook("Leviticus", "Levililer", "Leviticus", 27, 3, "İbranice (Hebrew)"),
        BibleBook("Numbers", "Sayılar", "Numbers", 36, 4, "İbranice (Hebrew)"),
        BibleBook("Deuteronomy", "Yasanın Tekrarı", "Deuteronomy", 34, 5, "İbranice (Hebrew)"),
        BibleBook("Joshua", "Yeşu", "Joshua", 24, 6, "İbranice (Hebrew)"),
        BibleBook("Judges", "Hakimler", "Judges", 21, 7, "İbranice (Hebrew)"),
        BibleBook("Ruth", "Rut", "Ruth", 4, 8, "İbranice (Hebrew)"),
        BibleBook("I_Samuel", "1. Samuel", "I Samuel", 31, 9, "İbranice (Hebrew)"),
        BibleBook("II_Samuel", "2. Samuel", "II Samuel", 24, 10, "İbranice (Hebrew)"),
        BibleBook("I_Kings", "1. Krallar", "I Kings", 22, 11, "İbranice (Hebrew)"),
        BibleBook("II_Kings", "2. Krallar", "II Kings", 25, 12, "İbranice (Hebrew)"),
        BibleBook("I_Chronicles", "1. Tarihler", "I Chronicles", 29, 13, "İbranice (Hebrew)"),
        BibleBook("II_Chronicles", "2. Tarihler", "II Chronicles", 36, 14, "İbranice (Hebrew)"),
        BibleBook("Ezra", "Ezra", "Ezra", 10, 15, "İbranice (Hebrew)"),
        BibleBook("Nehemiah", "Nehemya", "Nehemiah", 13, 16, "İbranice (Hebrew)"),
        BibleBook("Esther", "Ester", "Esther", 10, 17, "İbranice (Hebrew)"),
        BibleBook("Job", "Eyüp", "Job", 42, 18, "İbranice (Hebrew)"),
        BibleBook("Psalms", "Mezmurlar", "Psalms", 150, 19, "İbranice (Hebrew)"),
        BibleBook("Proverbs", "Süleyman'ın Özdeyişleri", "Proverbs", 31, 20, "İbranice (Hebrew)"),
        BibleBook("Ecclesiastes", "Vaiz", "Ecclesiastes", 12, 21, "İbranice (Hebrew)"),
        BibleBook("Song_of_Songs", "Ezgiler Ezgisi", "Song of Songs", 8, 22, "İbranice (Hebrew)"),
        BibleBook("Isaiah", "Yeşaya", "Isaiah", 66, 23, "İbranice (Hebrew)"),
        BibleBook("Jeremiah", "Yeremya", "Jeremiah", 52, 24, "İbranice (Hebrew)")
    )

    val bibleBooks = listOf(
        BibleBook("Matthew", "Matta", "Matthew", 28, 1, "Grekçe (Ancient Greek)"),
        BibleBook("Mark", "Markos", "Mark", 16, 2, "Grekçe (Ancient Greek)"),
        BibleBook("Luke", "Luka", "Luke", 24, 3, "Grekçe (Ancient Greek)"),
        BibleBook("John", "Yuhanna", "John", 21, 4, "Grekçe (Ancient Greek)"),
        BibleBook("Acts", "Elçilerin İşleri", "Acts", 28, 5, "Grekçe (Ancient Greek)"),
        BibleBook("Romans", "Romalılar", "Romans", 16, 6, "Grekçe (Ancient Greek)"),
        BibleBook("I_Corinthians", "1. Korintliler", "I Corinthians", 16, 7, "Grekçe (Ancient Greek)"),
        BibleBook("II_Corinthians", "2. Korintliler", "II Corinthians", 13, 8, "Grekçe (Ancient Greek)"),
        BibleBook("Galatians", "Galatyalılar", "Galatians", 6, 9, "Grekçe (Ancient Greek)"),
        BibleBook("Ephesians", "Efesliler", "Ephesians", 6, 10, "Grekçe (Ancient Greek)"),
        BibleBook("Philippians", "Filipililer", "Philippians", 4, 11, "Grekçe (Ancient Greek)"),
        BibleBook("Colossians", "Koloseliler", "Colossians", 4, 12, "Grekçe (Ancient Greek)"),
        BibleBook("I_Thessalonians", "1. Selanikliler", "I Thessalonians", 5, 13, "Grekçe (Ancient Greek)"),
        BibleBook("II_Thessalonians", "2. Selanikliler", "II Thessalonians", 3, 14, "Grekçe (Ancient Greek)"),
        BibleBook("I_Timothy", "1. Timoteyus", "I Timothy", 6, 15, "Grekçe (Ancient Greek)"),
        BibleBook("II_Timothy", "2. Timoteyus", "II Timothy", 4, 16, "Grekçe (Ancient Greek)"),
        BibleBook("Titus", "Titus", "Titus", 3, 17, "Grekçe (Ancient Greek)"),
        BibleBook("Philemon", "Filimon", "Philemon", 1, 18, "Grekçe (Ancient Greek)"),
        BibleBook("Hebrews", "İbraniler", "Hebrews", 13, 19, "Grekçe (Ancient Greek)"),
        BibleBook("James", "Yakup", "James", 5, 20, "Grekçe (Ancient Greek)"),
        BibleBook("I_Peter", "1. Petrus", "I Peter", 5, 21, "Grekçe (Ancient Greek)"),
        BibleBook("II_Peter", "2. Petrus", "II Peter", 3, 22, "Grekçe (Ancient Greek)"),
        BibleBook("I_John", "1. Yuhanna", "I John", 5, 23, "Grekçe (Ancient Greek)"),
        BibleBook("II_John", "2. Yuhanna", "II John", 1, 24, "Grekçe (Ancient Greek)"),
        BibleBook("III_John", "3. Yuhanna", "III John", 1, 25, "Grekçe (Ancient Greek)"),
        BibleBook("Jude", "Yahuda", "Jude", 1, 26, "Grekçe (Ancient Greek)"),
        BibleBook("Revelation", "Vahiy", "Revelation", 22, 27, "Grekçe (Ancient Greek)")
    )
}
