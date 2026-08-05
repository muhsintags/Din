<div align="center">

# 📖 Din Kütüphanesi

Farklı dinlere ait kutsal ve temel metinleri tek bir uygulamada bir araya getiren Android uygulaması.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)
![License](https://img.shields.io/badge/Lisans-MIT-blue?style=flat-square)

</div>

<br>

## İçerik

<div align="center">

| Metin | Açıklama |
|:---:|:---|
| 🕋 **Kur'an-ı Kerim** | Sure ve ayet bazında orijinal metin ve çeviri |
| 📗 **Sahih-i Buhari** | Hadis metinleri |
| ✝️ **İncil** | Tam metin erişimi |
| 🕎 **Tevrat** | Tam metin erişimi |
| 📘 **Talmud** | Metin erişimi |
| 🕉️ **Bhagavad Gita** | Tam metin erişimi |

</div>

<br>

## Özellikler

- **Çoklu dil desteği** — Google Translate API entegrasyonu ile anında çeviri
- **Çevrimdışı okuma** — internet bağlantısı olmadan metinlere erişim
- **Sade arayüz** — okumaya odaklı, dikkat dağıtmayan tasarım

<br>

## Kullanılan Teknolojiler

<div align="center">

| Katman | Teknoloji |
|:---|:---|
| Dil | Kotlin |
| Platform | Android SDK |
| Veritabanı | Room |
| Çeviri | Google Translate API |
| CI/CD | GitHub Actions |

</div>

<br>

## İndir

<div align="center">

[![APKPure](https://img.shields.io/badge/APKPure-24C36B?style=for-the-badge&logo=googleplay&logoColor=white)](#)
[![Google Play](https://img.shields.io/badge/Google%20Play-Yakında-lightgrey?style=for-the-badge&logo=googleplay&logoColor=white)](#)

</div>

> Uygulama şu an geliştirme aşamasında. APKPure bağlantısı yayınlandığında güncellenecektir. Google Play sürümü, geliştirici hesabı ücretinin karşılanmasının ardından planlanmaktadır.

<br>

## Kurulum

```bash
git clone https://github.com/muhsintags/Din.git
cd Din
./gradlew assembleDebug
```

Android Studio'da doğrudan açıp çalıştırabilir, ya da GitHub Codespaces üzerinden geliştirme yapabilirsiniz.

**Ortam değişkenleri:**

```bash
cp .env.example .env
```

Gerekli API anahtarlarını `.env` dosyasına girin.

<br>

## Proje Yapısı

```
Din/
├── app/                  # Ana uygulama modülü
├── gradle/               # Gradle wrapper dosyaları
├── Versions/             # Sürüm geçmişi
├── build.gradle.kts      # Proje seviyesi build yapılandırması
├── settings.gradle.kts   # Modül ayarları
└── metadata.json         # Uygulama meta verileri
```

<br>

## Yol Haritası

- [x] Kur'an, Sahih-i Buhari, İncil, Tevrat, Talmud ve Bhagavad Gita metinlerinin entegrasyonu
- [x] Google Translate API ile çoklu dil desteği
- [x] GitHub Actions ile otomatik debug build
- [ ] Release imzalama altyapısının güvenli hale getirilmesi
- [ ] APKPure üzerinden ilk yayın
- [ ] Google Play Store yayını (geliştirici ücreti sonrası)
- [ ] Türkçe tefsir/açıklama içeriklerinin eklenmesi

<br>

## Katkıda Bulunma

1. Depoyu fork'layın
2. Yeni bir dal oluşturun (`git checkout -b ozellik/yeni-ozellik`)
3. Değişikliklerinizi commit'leyin
4. Dalınızı push'layın
5. Pull Request açın

<br>

## Lisans

MIT Lisansı ile lisanslanmıştır. Detaylar için `LICENSE` dosyasına bakınız.

<br>

<div align="center">

**Geliştirici:** [@muhsintags](https://github.com/muhsintags)

</div>
