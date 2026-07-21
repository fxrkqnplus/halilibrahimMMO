# HalilIbrahimMMO

Türk Minecraft topluluğu için geliştirilmiş, özgün bir MMORPG ilerleme eklentisi
(Paper 26.2 / Minecraft 26.2). Ekonomi yoktur — tüm ilerleme seviye, beceri XP'si,
prestij, görevler, koleksiyonlar ve başarılar üzerinden işler.

Bu, çok fazlı, ticari kalitede bir yapıdır. Tüm mimari kararların kaydı için
[`PROJECT_MEMORY.md`](PROJECT_MEMORY.md) dosyasına bakın; her fazdan sonra güncel
tutulur ve bu proje üzerinde yeni bir işe başlamadan önce okunmalıdır.

## Gereksinimler

- Java 26 (Oracle JDK veya OpenJDK)
- Paper 26.2 (Build 60 veya üzeri)
- Gradle Wrapper (repo ile birlikte gelir, ayrıca yerel Gradle kurulumu gerekmez)

## Proje yapısı

- [`api/`](api) — Herkese açık geliştirici API'si. Saf arayüzler + bir
  `ServicesManager` tabanlı erişimci. Diğer eklentiler yalnızca bu modüle bağımlı olur.
- [`core/`](core) — Eklentinin tam uygulaması (`HalilIbrahimMMOPlugin` ve her bir
  iç sistem). `api` modülüne bağımlıdır.

## Derleme

```powershell
./gradlew build
```

Dağıtıma hazır, birleştirilmiş (shaded) jar dosyası
`core/build/libs/HalilIbrahimMMO-<surum>-all.jar` yolunda oluşturulur.

## Yerel test sunucusu çalıştırma

```powershell
./gradlew runServer
```

Bu komut `run/` klasörüne bir Paper 26.2 test sunucusu indirir ve eklenti kurulu
olarak başlatır.

## Veritabanı

SQLite, sıfır yapılandırma gerektiren varsayılan seçenektir. MySQL, MariaDB ve
PostgreSQL, `config.yml` (`veritabani.tur`) üzerinden desteklenir. Tüm JDBC
sürücüleri ve HikariCP, Paper'ın `PluginLoader` mekanizması üzerinden çalışma
zamanında çözümlenir — eklenti jar'ına hiçbir şey gömülmez (shade edilmez).

## İlerleme temeli

Mevcut yapı, UUID tabanlı asenkron oyuncu profillerini, istatistikleri ve genel
ilerlemeyi içerir. `progression.yml` dosyasında yapılandırılabilir seviye
tavanı, açık XP tablosu ve blok kırma/yaratık öldürme/oynama süresi ödülleri
bulunur. Oyuncular kayıtlı ilerlemelerini `/profil` ile görüntüleyebilir.
Genel seviye, oyun içinde "Güç" olarak adlandırılır. Her Gradle komutu için
Java 26 kullanın; çalışma alanında kurulu JDK `C:\Program Files\Java\jdk-26.0.1`
konumundadır.

## Beceriler ve kabiliyetler

Dört bağımsız olarak seviyelenen beceri bulunur: `kazicilik`, `odunculuk`,
`madencilik` ve `avcilik`. Paylaşılan açık seviye eğrileri `skills.yml`
dosyasında yapılandırılır; her beceri artık kırılan bloğun veya öldürülen
yaratığın **nadirliğine göre** farklı miktarda XP verir (ör. toprak çok az,
elmas/kadim kalıntı çok daha fazla; zombi az, warden/ejderha çok daha fazla).
`abilities.yml`, paylaşılan kalıcı olmayan enerji havuzunu ve sınıfa özel
kabiliyetleri yapılandırır.

Kabiliyetler iki şekilde etkinleştirilebilir: doğrudan `/kabiliyet <id>` ile,
ya da kabiliyet menüsünden birini seçip uygun alete (kazma/balta/kürek/kılıç,
kabiliyete göre değişir) sağ tıklayarak — seçili alet, kabiliyetin süresi
boyunca parlar ve eylem çubuğunda hangi kabiliyetin etkin olduğu ile ne kadar
süre (ya da tek seferlik kabiliyetlerde bekleme penceresi) kaldığı gösterilir.
Bazı kabiliyetler tamamen süreye dayalıdır (ör. Gece Görüşü); Amansız
Savaşçı'nın Avcı Odağı gibi savaşa dayalı bazı kabiliyetler ise her isabetli
vuruşta enerji tüketir ve enerji biterse ya da en fazla birkaç dakika sonra
kendiliğinden sona erer — ve yalnızca yaratıklara (PvE) karşı geçerlidir,
PvP'de hiçbir etkisi yoktur. Henüz açılmamış kabiliyetler menüde kilitli
olarak gösterilir, detayları açılana kadar gizli kalır.

Geliştirici API'si, değişmez beceri tanımlarını ve asenkron salt okunur oyuncu
beceri sorgularını sunar. Harici eklentilerin beceri XP'si vermesine veya
kabiliyet tetiklemesine izin vermez.

## Profil paneli (sidebar)

Ekranın sağında kalıcı bir bilgi paneli; oyuncu adını (varsa kuşanılan unvan
kendi rengiyle önde), Güç seviyesi/XP'sini, prestiji, seçili sınıfı ve o
sınıfın kendi seviyesini, açık kabiliyetlerin adlarını (seçili olan yeşil,
diğerleri kırmızı), her günlük görevi ayrı ayrı ilerleme ve XP ödülüyle, ve
haftalık görev özetini gösterir. `/panel` mevcut oturum için paneli açar/
kapatır — tercih kaydedilmez, her katılışta veya sunucu yeniden başladığında
panel varsayılan olarak görünür duruma döner.

## Sınıflar ve prestij

`/sinif` kabiliyet menüsünü açar; oyuncular Define Avcısı, Amansız Savaşçı,
Mağara Adamı veya Orman Bekçisi sınıflarından birini seçebilir, yetenek
ağaçlarını inceleyebilir ve paylaşılan yetenek puanlarını harcayabilir —
tamamı menü üzerinden, artık sohbet komutu argümanı yoktur. Her sınıfın kendi
"sinerji becerisi" vardır (ör. Amansız Savaşçı ↔ avcılık) ve o beceriden
kazanılan XP'nin bir kısmı otomatik olarak sınıfın kendi seviye izine de
yansır. Sınıf değişikliğinin yapılandırılabilir, veritabanı destekli yedi
günlük bir bekleme süresi vardır. Genel seviye 100'e ulaşıldığında
`/prestij onayla` yalnızca genel seviye/XP'yi sıfırlar ve dört puanlık yeni
bir yetenek döngüsü açar; varsayılan maksimum prestij sayısı 10'dur.

## Görevler, koleksiyonlar ve macera günlüğü

`/gorev` her kalıcı, günlük ve haftalık görevi ilerlemenizle birlikte listeler;
görevler doğru blokları kırdıkça veya doğru yaratıkları öldürdükçe otomatik
ilerler, günlük/haftalık görevler kendi periyotları bittiğinde kendiliğinden
sıfırlanır. `/koleksiyon` koleksiyonlarınızı listeler, `/koleksiyon <id>` ise
henüz en az bir kez toplamadığınız ögeleri gösterir. `/gunluk` son on görev
tamamlama ve koleksiyon kilometre taşınızı gösterir. Bir görevi tamamlamak,
bir koleksiyon ögesi edinmek ve bir koleksiyonun tamamını tamamlamak, her şey
gibi aynı ilerleme sistemi üzerinden genel XP kazandırır.

## Liderlik tabloları

`/liderlik seviye`, `/liderlik prestij`, `/liderlik gorev`, `/liderlik
koleksiyon` ve `/liderlik <yetenek-id>`; genel seviye/XP, prestij, tamamlanan
görev sayısı, edinilen koleksiyon ögesi sayısı ve her yapılandırılmış beceri
için en üst sıradaki oyuncuları gösterir. Aynı tablolara `/menu`'nün Topluluk
kategorisinden de göz atılabilir. Sonuçlar her açılışta veritabanını sorgulamak
yerine kısa, yapılandırılabilir bir süre için önbelleğe alınır
(`leaderboards.yml`).

## Unvanlar ve kozmetikler

`titles.yml` ve `cosmetics.yml`, her biri tek bir kazanma koşuluna sahip
(seviye/prestije ulaşma, belirli bir görevi/koleksiyonu tamamlama veya bir
beceri seviyesine ulaşma) açılabilir öge katalogları tanımlar. `/unvan` ve
`/kozmetik` neyi açtığınızı listeler ve birini kuşanmanızı sağlar
(`/unvan sec <id>`, `/kozmetik sec <id>`); aynı kataloglara `/menu`'den de
göz atılabilir. Kuşanılan bir unvan, sohbette adınızın önünde **kendi
yapılandırılmış rengi ve kalınlığıyla** görünür (her unvanın kendine özgü bir
sohbet stili vardır). Kuşanılan bir kozmetik, katıldığınızda ve her seviye
atladığınızda kısa bir parçacık efekti oynatır.

## Görsel geri bildirim

Her XP kazanımı, kaynağını belirten kısa bir cübuk (boss bar) gösterir (blok
kırma, yaratık öldürme, oynama süresi, görevler, koleksiyonlar, beceriler);
beceri cübuğu ayrıca hangi cevherin/yaratığın XP verdiğini de parantez
içinde gösterir. Seçili sınıfınızın sinerji becerisinden XP kazandığınızda,
beceri cübuğuyla **aynı anda**, her sınıfa özgü kendi renginde ayrı bir sınıf
XP cübuğu de görünür. Seviye atlamanın kendisi kişisel bir sohbet mesajıdır,
asla yayın veya ekran efekti değildir. Görev tamamlama, koleksiyon tamamlama
ve prestij hâlâ her oyuncuya kısa bir cübuk ve ekran başlığı gösterir, kuşanılan
kozmetikten bağımsız olarak (`feedback.yml` her şeyin ne kadar süre görünür
kalacağını denetler).

## Envanter menüsü

`/menu`, dört kategoriye ayrılmış bir merkez menü açar: **İlerleme** (profil,
beceriler, sınıf/yetenekler, prestij), **Kabiliyetlerim**, **Görevler**
(görevler, koleksiyonlar, macera günlüğü) ve **Topluluk** (liderlik tabloları,
unvanlar, kozmetikler). Her menü, ilgili metin komutlarının kullandığı aynı
sistemler üzerinden çalışır — GUI'de bir sınıf seçmek veya yetenek puanı
harcamak, tam olarak sınıf/kabiliyet menülerinin arka planda çağırdığı
servislerle aynı sonucu verir. Kabiliyetler menüsü bir kabiliyeti seçer
(simgesini parlatarak) ama doğrudan etkinleştirmez; etkinleştirme hâlâ
oyun içinde uygun alete sağ tıklayarak ya da `/kabiliyet <id>` ile olur.
Henüz açılmamış kabiliyetler menüde kilitli gösterilir.

## Yönetici paneli

`/haliladmin` (`halilibrahimmmo.admin` izni gerektirir, varsayılan op) tamamen
GUI üzerinden çalışan bir yönetici paneli açar — artık sohbet argümanlı bir
yönetici komutu yoktur. Sunduğu işlemler: `config.yml`/`messages.yml`/tüm
oyun verisi dosyalarını yeniden yükleme, mevcut oturum için ayrıntılı hata
ayıklama kaydını açıp kapatma, çalışan eklenti/Paper sürümünü görüntüleme ve
çevrimiçi oyuncuları gezip herhangi birinin salt okunur profil/sınıf/beceri
özetini görüntüleme. Buradan bir oyuncunun XP'sini, seviyesini, sınıfını veya
beceri değerlerini elle ayarlamanın bilinçli olarak hiçbir yolu yoktur — tüm
ilerleme yalnızca normal oynanış üzerinden gerçekleşir.

## PlaceholderAPI ve LuckPerms (isteğe bağlı)

İkisi de gerçek anlamda isteğe bağlı yumuşak bağımlılıktır — sunucu, ikisinden
biri, her ikisi ya da hiçbiri kurulu olmasa da aynı şekilde açılır.
**PlaceholderAPI** kuruluyken bu eklenti, `%halilibrahimmmo_level%`, `_xp`,
`_xp_to_next_level`, `_prestige`, `_class`, `_title`, `_cosmetic` ve her
yapılandırılmış beceri için `_skill_<id>_level`/`_skill_<id>_xp` yer
tutucularını sunan bir `halilibrahimmmo` eklentisi (expansion) kaydeder.
**LuckPerms** kuruluyken bu eklenti, `halilibrahimmmo:level`,
`halilibrahimmmo:prestige` ve `halilibrahimmmo:class` değerlerini LuckPerms'e
besleyen salt okunur bir bağlam hesaplayıcı (context calculator) kaydeder;
böylece sunucu yöneticileri ilerlemeye bağlı kendi izin kurallarını
yazabilir — bu eklenti LuckPerms'e asla kendisi yazmaz.

## Test

`./gradlew test`, hızlı birim/yapılandırma test paketini çalıştırır (harici
servis gerektirmez; gerçek bir SQLite dosyasına karşı bir göç/migration
çalıştırma kontrolü dahildir). `./gradlew integrationTest` ayrıca
Testcontainers üzerinden gerçek MySQL, MariaDB ve PostgreSQL konteynerlerine
karşı her gönderilen veritabanı göçünü ve temsili bir repository okuma/yazma
örneklem setini çalıştırır — bu, çalışan bir Docker daemon'u gerektirir ve
bilinçli olarak `build`/`test`/`check`'ten hariç tutulmuştur; bu yüzden ayrı
ve açıkça çalıştırılmalıdır.

## Dil politikası

Bir oyuncunun veya sunucu yöneticisinin oyun içinde gördüğü her şey (mesajlar,
GUI'ler, komutlar, BossBar'lar, unvanlar, config yorumları) Türkçedir. Java
kaynak kodu — paketler, sınıflar, metodlar, değişkenler — İngilizce kalır.
