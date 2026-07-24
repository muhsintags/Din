package com.example.data.repository

import com.example.data.model.MiraclePost
import com.example.data.network.MiraclesApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class MiraclesRepository {

    private var baseUrl: String = "https://muhsintags.github.io/mucizeler-admin-panel/"
    private var apiService: MiraclesApiService = createApiService(baseUrl)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _posts = MutableStateFlow<List<MiraclePost>>(emptyList())
    val posts: StateFlow<List<MiraclePost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLiveApi = MutableStateFlow(false)
    val isLiveApi: StateFlow<Boolean> = _isLiveApi.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        _posts.value = getInitialDefaultPosts()
    }

    fun getBaseUrl(): String = baseUrl

    private fun createApiService(url: String): MiraclesApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MiraclesApiService::class.java)
    }

    suspend fun fetchMiracles(): Result<List<MiraclePost>> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _errorMessage.value = null

        var fetchedList: List<MiraclePost>? = null

        // 1. Try Direct OkHttp queries on potential JSON endpoints
        val candidateUrls = listOf(
            "https://raw.githubusercontent.com/muhsintags/mucizeler-admin-panel/main/data.json",
            "https://raw.githubusercontent.com/muhsintags/mucizeler-admin-panel/main/db.json",
            "https://raw.githubusercontent.com/muhsintags/mucizeler-admin-panel/main/miracles.json",
            "https://raw.githubusercontent.com/muhsintags/mucizeler-admin-panel/main/posts.json",
            "https://muhsintags.github.io/mucizeler-admin-panel/data.json",
            "https://muhsintags.github.io/mucizeler-admin-panel/api/v1/miracles"
        )

        for (url in candidateUrls) {
            try {
                val request = Request.Builder().url(url).get().build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val parsed = parseJsonToMiracles(bodyString)
                        if (parsed.isNotEmpty()) {
                            fetchedList = parsed
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next url
            }
        }

        if (fetchedList != null && fetchedList.isNotEmpty()) {
            _posts.value = fetchedList
            _isLiveApi.value = true
            _isLoading.value = false
            Result.success(fetchedList)
        } else {
            // Fallback to initial defaults gracefully
            _isLiveApi.value = false
            _posts.value = _posts.value.ifEmpty { getInitialDefaultPosts() }
            _isLoading.value = false
            Result.success(_posts.value)
        }
    }

    private fun parseJsonToMiracles(jsonString: String): List<MiraclePost> {
        val list = mutableListOf<MiraclePost>()
        try {
            val trimmed = jsonString.trim()
            val jsonArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)
                when {
                    rootObj.has("miracles") -> rootObj.getJSONArray("miracles")
                    rootObj.has("posts") -> rootObj.getJSONArray("posts")
                    rootObj.has("data") -> rootObj.getJSONArray("data")
                    else -> null
                }
            } else null

            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "post_$i")
                    val title = obj.optString("title", obj.optString("baslik", "Mucize Gönderisi"))
                    val category = obj.optString("category", obj.optString("kategori", "Bilim & Kur'an"))
                    val author = obj.optString("author", obj.optString("yazar", "Admin"))
                    val date = obj.optString("date", obj.optString("tarih", "Günün Paylaşımı"))
                    val content = obj.optString("content", obj.optString("icerik", obj.optString("metin", "")))
                    val imageUrl = obj.optString("imageUrl", obj.optString("resim", obj.optString("image", "")))
                    val reference = obj.optString("reference", obj.optString("ayet", obj.optString("kaynak", "")))

                    val hashtagsList = mutableListOf<String>()
                    val tagsArr = obj.optJSONArray("hashtags") ?: obj.optJSONArray("tags")
                    if (tagsArr != null) {
                        for (j in 0 until tagsArr.length()) {
                            val tag = tagsArr.optString(j)
                            if (tag.isNotBlank()) hashtagsList.add(if (tag.startsWith("#")) tag else "#$tag")
                        }
                    } else {
                        hashtagsList.addAll(listOf("#kuranmucizeleri", "#bilim"))
                    }

                    if (title.isNotBlank() && content.isNotBlank()) {
                        list.add(
                            MiraclePost(
                                id = id,
                                title = title,
                                category = category,
                                author = author,
                                date = date,
                                content = content,
                                imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=800&auto=format&fit=crop" },
                                reference = reference,
                                hashtags = hashtagsList
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return list
    }

    fun toggleBookmark(postId: String) {
        _posts.value = _posts.value.map { post ->
            if (post.id == postId) {
                post.copy(isBookmarked = !post.isBookmarked)
            } else post
        }
    }

    private fun getInitialDefaultPosts(): List<MiraclePost> {
        return listOf(
            MiraclePost(
                id = "m1",
                title = "Kainatın Genişlemesi ve Uzay Dokusu",
                category = "Kosmoloji & Fizik",
                author = "Scriptorium Bilim Kurulu",
                date = "24 Temmuz 2026",
                content = "1929 yılında Edwin Hubble'ın galaksilerin birbirine olan uzaklığının arttığını keşfetmesiyle modern astrofizikte 'Genişleyen Evren' modeli kabul edildi. Bundan 1400 yıl önce indirilen Kur'an-ı Kerim'de Zariyat Suresi 47. ayette şöyle buyrulmaktadır: 'Göğü gücümüzle biz kurduk ve şüphesiz biz onu genişletenleriz.' Ayette geçen 'mûsi'ûn' kelimesi uzayın dokusal genişlemesini mucizevi şekilde tarif etmektedir.",
                imageUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=800&auto=format&fit=crop",
                reference = "Zariyat Suresi, 47. Ayet",
                hashtags = listOf("#kuranmucizeleri", "#astronomi", "#kosmoz", "#bilimveayet", "#zariyat"),
                isBookmarked = true
            ),
            MiraclePost(
                id = "m2",
                title = "Denizlerin Karışmaması: Yüzey Gerilimi ve Tuzluluk Engeli",
                category = "Oseanografi",
                author = "Dr. Tarık Akman",
                date = "23 Temmuz 2026",
                content = "Cebelitarık Boğazı'nda Akdeniz ve Atlas Okyanusu suları buluşur fakat birbirine karışmaz. Okyanus bilimci Jacques Cousteau'nun araştırdığı bu su engeli (berzah), farklı yoğunluk ve tuzluluk oranlarının oluşturduğu görünmez bir duvar gibidir. Rahman Suresi 19-20. ayetlerde 'İki denizi birbirine kavuşmak üzere salıverdi. Aralarında bir engel vardır, birbirine geçip karışmazlar' buyrulmaktadır.",
                imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop",
                reference = "Rahman Suresi, 19-20. Ayetler",
                hashtags = listOf("#denizler", "#oseanografi", "#kuranmucizeleri", "#cebelitarik", "#bilim"),
                isBookmarked = false
            ),
            MiraclePost(
                id = "m3",
                title = "Dişi Arıların Mimarisi ve Çiçek Kodlaması",
                category = "Biyoloji & Tabiat",
                author = "Biyolog Zeynep Erdem",
                date = "22 Temmuz 2026",
                content = "Arapça dil bilgisine göre fiiller çekimlenirken eril ve dişi ayrımı yapılır. Nahl Suresi 68-69. ayetlerde kovan yapma, bal toplama ve yolları takip etme görevi dişi arılara has fiil kalıplarıyla (ittahizî, kulî, uslukî) emredilmiştir. Modern zooloji, kovandaki tüm işçi ve mimar arıların dişi olduğunu 20. yüzyılda genetik incelemelerle tespit etmiştir.",
                imageUrl = "https://images.unsplash.com/photo-1587049352847-4a222e784d38?w=800&auto=format&fit=crop",
                reference = "Nahl Suresi, 68-69. Ayetler",
                hashtags = listOf("#biyoloji", "#arinahlsuresi", "#tabiat", "#kuranmucizeleri", "#genetik"),
                isBookmarked = false
            ),
            MiraclePost(
                id = "m4",
                title = "Anne Karnındaki Üç Karanlık Evre ve Embriyoloji",
                category = "Tıp & Embriyoloji",
                author = "Op. Dr. Kemal Yılmaz",
                date = "21 Temmuz 2026",
                content = "Dünyaca ünlü embriyolog Keith L. Moore, Kur'an-ı Kerim'de insanın yaratılış safhalarını incelemiş ve 'Üç karanlık içinde yaratılış' (Zümer 6) ifadesinin karın duvarı, rahim duvarı ve amniyon zarı olduğunu doğrulamıştır. Aynı şekilde Mü'minun Suresi 12-14. ayetlerde çiğnenmiş et parçası (mudga) ve kemiklerin etle giydirilmesi sıralaması modern ultrason verileriyle birebir örtüşür.",
                imageUrl = "https://images.unsplash.com/photo-1516549655169-df83a0774514?w=800&auto=format&fit=crop",
                reference = "Zümer 6 & Mü'minun 12-14",
                hashtags = listOf("#embriyoloji", "#tip", "#insan", "#kuranmucizeleri", "#biomedikal"),
                isBookmarked = true
            ),
            MiraclePost(
                id = "m5",
                title = "Gökyüzünün Korunmuş Tavan (Atmosfer) Olması",
                category = "Atmosfer Fiziği",
                author = "Fizikçi Murat Özcan",
                date = "20 Temmuz 2026",
                content = "Dünyamızı çevreleyen atmosfer ve Manyetosfer (Van Allen radyasyon kuşakları), uzaydan gelen öldürücü kozmik ışınları ve meteorları engelleyen devasa bir kalkan işlevi görür. Enbiya Suresi 32. ayette 'Gökyüzünü korunmuş bir tavan yaptık' buyrularak atmosferin koruyucu zırh özelliği mucizevi bir dille vurgulanmıştır.",
                imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop",
                reference = "Enbiya Suresi, 32. Ayet",
                hashtags = listOf("#atmosfer", "#fizik", "#dunya", "#kuranmucizeleri", "#uzaykalkani"),
                isBookmarked = false
            ),
            MiraclePost(
                id = "m6",
                title = "Demirin Uzaydan İndirilmesi (Meteortik Köken)",
                category = "Jeoloji & Astrofizik",
                author = "Scriptorium Editörü",
                date = "19 Temmuz 2026",
                content = "Demir atomunun çekirdek yapısı o kadar yüksek enerji gerektirir ki Dünya'da oluşması imkansızdır. Demir ancak Güneş'ten kat kat büyük süpernova yıldız patlamalarında oluşup meteorlarla Dünya'ya yağmıştır. Hadid Suresi 25. ayette 'Demiri de indirdik (enzelna)' ifadesi tam da bu uzaysal kökene işaret etmektedir.",
                imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop",
                reference = "Hadid Suresi, 25. Ayet",
                hashtags = listOf("#jeoloji", "#kimya", "#demir", "#hadidsuresi", "#kuranmucizeleri"),
                isBookmarked = true
            )
        )
    }
}

