# CLAUDE.md

## Zorunlu ilk adım
Herhangi bir kod yazmadan, herhangi bir dosyayı değiştirmeden önce
**PROJECT_MEMORY.md dosyasını baştan sona oku.** Bu proje çok fazlı
geliştiriliyor; o dosya tüm mimari kararların tek doğruluk kaynağı. Yeni bir
faz bitirdiğinde PROJECT_MEMORY.md'yi güncelle (yeni bölüm ekle, eskisini asla
çelişecek şekilde silme).

## Komutlar
- Build: `./gradlew build` → shaded jar
  `core/build/libs/HalilIbrahimMMO-<version>-all.jar`
- Test: `./gradlew test` (JUnit 5, `useJUnitPlatform`)
- Yerel test sunucusu: `./gradlew runServer` (Paper 26.2'yi `core/run/`
  altına indirir)
- `JAVA_HOME` mutlaka JDK 26'yı göstermeli (bu makinede
  `C:\Program Files\Java\jdk-26.0.1`); daha eski bir JDK ile Gradle 9.6.1
  çalışmaz.

## Dil ayrımı (kesinlikle karıştırma)
- Oyuncunun/admin'in ekranda gördüğü HER ŞEY Türkçe: mesajlar, GUI, komut
  çıktıları, BossBar/title metinleri, config.yml/messages.yml yorumları.
- Kaynak kod İngilizce: paket/sınıf/metod/değişken adları, Javadoc, exception
  mesajları.
- Oyuncuya giden hiçbir metin `sender.sendMessage("literal")` ile yazılmaz;
  her zaman yeni bir `MessageKey` enum sabiti + `messages.yml`'de karşılığı
  üzerinden gönderilir.

## Ekonomi kısıtı
Para/coin/banka/Vault asla eklenmez. İlerleme sadece level/XP/prestij/görev/
koleksiyon üzerinden olur.

## Pratik notlar
PROJECT_MEMORY.md'de henüz kayıtlı olmayan, iş sırasında öğrenilen küçük
pratik notlar buraya eklenir (ör. yerel ortama özgü tuhaflıklar, sık
karşılaşılan build hataları). Bir not kalıcı bir mimari karara dönüşürse
PROJECT_MEMORY.md'ye taşınır ve buradan silinir.
