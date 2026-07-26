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

---

## Her şey menüden: komut listesi

Eklentinin **hiçbir oyuncu komutunun argümanı yoktur**. Her komut yalnızca
ilgili GUI ekranını açan bir kısayoldur; sohbete liste basan, sohbetten
değer değiştiren hiçbir komut kalmamıştır.

| Komut | Ne açar |
|---|---|
| `/menu` | Ana menü (dört kategori + kişisel başlık + Eklenti Hakkında) |
| `/mmo` | MMO sistemini ilk kez aktifleştirir, ardından sınıf tanıtım ekranını açar |
| `/profil` | Profil ekranı (Güç, prestij, sınıf) |
| `/sinif` | Sınıf ekranı: sınıf simgeleri + seçili sınıfın yetenek ağacı |
| `/kabiliyet` | Kabiliyet menüsü (seçim yapılır, etkinleştirme oyun içindedir) |
| `/gorev` | Görev panosu (günlük / haftalık / kalıcı / tamamlananlar) |
| `/koleksiyon` | Koleksiyon ekranı |
| `/gunluk` | Macera günlüğü |
| `/prestij` | Prestij şart listesi ve onay düğmesi |
| `/liderlik` | Liderlik tabloları |
| `/unvan` | Unvan gardırobu |
| `/kozmetik` | Kozmetik gardırobu |
| `/madencilik`, `/odunculuk`, `/kazicilik`, `/avcilik` | O becerinin detay ekranı |
| `/panel` | Sağdaki bilgi panelini gizler/gösterir |
| `/haliladmin` | Yönetici paneli (`halilibrahimmmo.admin` izni gerekir) |

**Ekranda görünen her süre** saat/dakika/saniye olarak yazılır: kabiliyet
bekleme süresi, sınıf değiştirme bekleme süresi, geri sayımlar. "130 saniye"
değil, "2 dakika 10 saniye" görürsünüz. Birim kelimeleri `messages.yml`
içindeki `sure:` bölümünden değiştirilebilir.

---

## İlerleme temeli

Oyun içinde **Güç** olarak adlandırılan genel seviye, bağımsız bir XP
kaynağına sahip değildir — dört becerinin (kazıcılık, odunculuk, madencilik,
avcılık) seviyelerinin toplamına, prestij yaptıkça kalıcı olarak biriken
Güç'ün eklenmesiyle hesaplanır. Prestij, sıfırladığı seviyelere rağmen Güç'ü
asla düşürmez.

İki kalıcı kural her XP kaynağı için geçerlidir:

- **Yaratıcı (Creative) veya izleyici (Spectator) moddayken** hiçbir şekilde
  beceri, sınıf veya görev XP'si kazanılmaz.
- **Yalnızca dünyanın kendisinin oluşturduğu bloklar sayılır.** Bir oyuncunun
  (kendisinin ya da başkasının) yerleştirdiği bir blok kırıldığında ne XP
  verir, ne görev ilerletir, ne de üzerinde herhangi bir kabiliyet çalışır —
  evin kolonundaki kütüğü ya da sonradan yere koyduğunuz bir cevheri
  kırmak hiçbir şey kazandırmaz. Yerleştirilen bloklar, ait oldukları chunk'ın
  kendi kalıcı verisinde saklanır; sunucu yeniden başlasa da unutulmazlar.
  (Eklenti kurulmadan *önce* yerleştirilmiş eski yapılar bu kaydın dışındadır.)

Her Gradle komutu için Java 26 kullanın; çalışma alanında kurulu JDK
`C:\Program Files\Java\jdk-26.0.1` konumundadır.

---

## Beceriler

Dört bağımsız olarak seviyelenen beceri vardır. Hepsi `skills.yml` içindeki
aynı açık 1–100 seviye eğrisini kullanır, ama her biri farklı bir eylemden
beslenir ve her blok/yaratık **oyun içindeki gerçek değerine göre** XP verir.

| Beceri | Nasıl kazanılır | En az → en çok XP |
|---|---|---|
| **Kazıcılık** (`/kazicilik`) | Toprak, kum, çakıl, kil gibi yumuşak blokları kazmak | 6 (toprak) → 18 (kil) |
| **Odunculuk** (`/odunculuk`) | Kütük kesmek | 9 (meşe) → 21 (nether sapları) |
| **Madencilik** (`/madencilik`) | Cevher kırmak | 12 (kömür) → 300 (kadim kalıntı) |
| **Avcılık** (`/avcilik`) | Yaratık öldürmek | 15 (zararsız hayvanlar) → 2400 (Ejderha) |

Aradaki farklar bilinçli olarak büyük tutulmuştur. Örneğin madencilikte
redstone 21, altın 60, elmas 135, zümrüt 180 XP verir; avcılıkta bir inek ya
da domuz 15, bir zombi 24, bir Wither İskeleti 90, bir Warden 1800 XP verir.
Listelenmemiş her yaratık en düşük değeri alır, çünkü listelenmemiş yaratıklar
neredeyse her zaman çiftlikte sınırsız üretilebilen hayvanlardır.

### Kilometre taşları

Bir beceri 25/50/75/100. seviyeye ulaştığında **kalıcı** bir bonus kazandırır.
Kademeler zayıftan güçlüye sıralanmıştır: 25. seviye basit bir giriş bonusu,
becerinin asıl güçlü bonusu her zaman 75. ve 100. seviyelerde.

| Beceri | 25 | 50 | 75 | 100 |
|---|---|---|---|---|
| Kazıcılık | +2 can | +10 enerji | +20 enerji | +30 enerji |
| Odunculuk | +2 can | +10 enerji | Hız I | Hız II |
| Madencilik | +2 can | +10 enerji | Acele I | Acele II |
| Avcılık | +2 can | +4 can | Yaratıklara +%10 hasar | Yaratıklara +%20 hasar |

Bu bonuslar **ölünce kaybolmaz** — yeniden doğduğunuzda otomatik olarak geri
yüklenir. Yalnızca prestij ile o beceri sıfırlandığında düşerler. Her becerinin
detay ekranı (`/madencilik` vb.) mevcut seviye/XP'yi, becerinin beslediği
sınıfı, kilometre taşı açıklamalarını ve o becerinin liderlik tablosunu
gösterir.

---

## Sınıflar

Dört sınıf vardır; her birinin bir **sinerji becerisi** ve o sınıfa özel üç
kabiliyeti bulunur. Sinerji becerisinden kazanılan XP'nin bir kısmı otomatik
olarak sınıfın kendi seviye izine de yansır, dolayısıyla sınıfınıza uygun
oynamak hem beceriyi hem sınıfı birlikte büyütür.

| Sınıf | Sinerji becerisi | Simge | Kalıcı prestij bonusu |
|---|---|---|---|
| **Define Avcısı** (`savasci`) | Kazıcılık | Altın kürek | Sınıfa özgü |
| **Amansız Savaşçı** (`avci`) | Avcılık | Elmas kılıç | Sınıfa özgü |
| **Mağara Adamı** (`madenci`) | Madencilik | Elmas kazma | Sınıfa özgü |
| **Orman Bekçisi** (`oduncu`) | Odunculuk | Demir balta | Sınıfa özgü |

### Yetenek ağacı

Seçili sınıfın altında üç arttırılabilir özellik (yetenek) bulunur. Bunlar
tüm sınıflar arasında **paylaşılan** tek bir yetenek puanı havuzundan
harcanır, dolayısıyla sınıf değiştirmek harcanmış rankları silmez. Menüde bir
yeteneğin üstüne geldiğinizde artık **ne işe yaradığı** yazar: her rankın ne
kadar can / hasar / enerji verdiği, ya da hangi kabiliyetin enerji maliyetini
veya bekleme süresini ne kadar azalttığı, ve şu ana kadar biriken toplam etki.

### Sınıf değiştirme

Sınıf değiştirme, ilk sınıf seçiminde kullanılan ekranın **aynısını** açar:
dört sınıf, her birinin tanıtım yazısı ve kabiliyet listesiyle. Ekranın
üstündeki durum ögesi mevcut sınıfınızı ve şu anda değiştirip
değiştiremeyeceğinizi (değiştiremiyorsanız ne kadar kaldığını) söyler.

Bir sınıf seçtiğinizde bir onay penceresi açılır ve iki şey açıkça yazılır:
bir sonraki değişiklik için ne kadar bekleyeceğiniz (varsayılan 7 gün) ve
**ayrıldığınız sınıfın seviyesinin tamamen sıfırlanacağı** (1. seviye, 0 XP;
geri alınamaz). Bekleme süresi dolmamışsa onay düğmesi hiç görünmez, yerine
kalan süreyi gösteren kilitli bir öge vardır.

Değişiklik onaylandığında ekranın ortasında bir başlık, üstte bir boss cübuğu
ve sohbette kişisel bir mesaj olarak **"Sınıf Değiştirildi — <Sınıf İsmi>"**
görünür ve buna özel bir ses efekti çalar.

---

## Kabiliyetler

Her kabiliyet tam olarak bir sınıfa aittir; o sınıf seçili değilken
kullanılamaz. Açılma koşulu **sınıf seviyesidir** (beceri seviyesi değil).

| Sınıf | Kabiliyet | Sv. | Enerji | Bekleme | Etki |
|---|---|---|---|---|---|
| Define Avcısı | Hazine Sezgisi | 5 | 20 | 1 dk 30 sn | Kırdığın bloklardan %50 ihtimalle ekstra düşme |
| Define Avcısı | Kazıcı Darbesi | 10 | 20 | 1 dk | Sonraki kırışta çevredeki 9 bloğu birden kazar |
| Define Avcısı | Define Refleksi | 30 | 30 | 2 dk 30 sn | 3 saniye neredeyse tüm hasara dirençlilik |
| Orman Bekçisi | Yeşil Yol | 5 | 20 | 1 dk 15 sn | 10 saniye artan hareket hızı |
| Orman Bekçisi | Ağaç Kırıcı | 15 | 25 | 1 dk 15 sn | Bağlı tüm kütükleri (en fazla 32) birden keser |
| Orman Bekçisi | Doğa Şifası | 30 | 25 | 2 dk | 8 saniye hızlı can yenilenmesi |
| Amansız Savaşçı | Gece Görüşü | 5 | 15 | 2 dk | 60 saniye karanlıkta net görüş |
| Amansız Savaşçı | Avcı Odağı | 10 | 25 | 1 dk 30 sn | PvE hasarı %50 artar (enerji tüketir) |
| Amansız Savaşçı | Yıkıcı Darbe | 30 | 30 | 30 sn | Ayağının altında şok dalgası, 4 blok yarıçapı |
| Mağara Adamı | Cevher Sezgisi | 5 | 25 | 1 dk 30 sn | Kırdığın bloklardan %40 ihtimalle ekstra düşme |
| Mağara Adamı | Damar Kırıcı | 20 | 30 | 1 dk 40 sn | Bağlı aynı cevher damarını (16 blok) birden kazar |
| Mağara Adamı | Maden Feneri | 35 | 30 | 7 dk 30 sn | 36 blok yarıçapındaki cevherleri 30 sn parlatır |

### Nasıl kullanılır

1. `/kabiliyet` menüsünden bir kabiliyet seçin. Her kabiliyetin konusuna uygun
   kendi simgesi vardır; **seçili olan büyülü (parlayan)** görünür, seçildiğinde
   kısa bir ses efekti çalar ve sohbete onay mesajı gelir.
2. Kabiliyetin istediği aleti (kazma / balta / kürek / kılıç, kabiliyete göre
   değişir) elinize alın.
3. **Sağ tıklayın.** Bir bloğa, boş havaya ya da bir yaratığa — üçü de
   çalışır. Saldırı kabiliyetlerini kullanmak için önce bir duvar bulmanız
   gerekmez.

Alet **yalnızca kabiliyeti başlatmak** için gereklidir: kabiliyet bir kez
etkinleştikten sonra elindeki eşyayı değiştirmen, hatta düşürmen onu bozmaz.
Etkin bir kabiliyet varken eylem çubuğunda kabiliyetin adı, kalan süresi ve
**mevcut/maksimum enerjiniz** (ör. `Enerji 70/100`) sürekli gösterilir.

Her kabiliyetin `abilities.yml` içinde kendine ait bir rengi, etkinleştirme
sesi ve partikül efekti vardır. Farklı bir kabiliyete geçmek kısa bir bekleme
süresine tabidir (varsayılan 2 dakika); aynı kabiliyeti yeniden seçmek
serbesttir. Henüz açılmamış kabiliyetler menüde kilitli gösterilir.

**Maden Feneri** özel bir durumdur: yalnızca **geniş ve üstü kapalı bir
madende** kullanılabilir (dar bir tünelde ya da açık arazide çalışmaz).
Çalıştığında 36 blok yarıçapındaki tüm cevherler 30 saniye boyunca duvarların
ardından bile parlar; işaretlenmiş bir cevheri kırdığınızda parıltısı anında
söner.

**Avcı Odağı** süreye değil enerjiye dayalıdır: her isabetli vuruşta enerji
tüketir, enerjiniz biterse ya da en fazla 30 saniye sonra kendiliğinden sona
erer — ve yalnızca yaratıklara (PvE) karşı geçerlidir, PvP'de hiçbir etkisi
yoktur.

### Enerji

Enerji, tüm kabiliyetlerin paylaştığı, kaydedilmeyen bir havuzdur (varsayılan
maksimum 100; sınıf ve kilometre taşı bonuslarıyla artar). Saniyede 6 puan
dolar. Eylem çubuğunda her zaman `mevcut/maksimum` biçiminde görünür.

---

## Profil paneli (sidebar)

Ekranın sağında kalıcı, renklendirilmiş bir bilgi paneli; oyuncu adını
(varsa kuşanılan unvan kendi rengiyle isimden önce, "Oyuncu: [Unvan] Ad"
sırasıyla), Güç seviyesini (altın), prestiji (açık mor), seçili sınıfı ve o
sınıfın kendi seviyesini (camgöbeği), açık kabiliyetlerin adlarını (seçili
olan yeşil, diğerleri kırmızı), henüz tamamlanmamış ilk-adım görevlerini
(tamamlanınca bu bölüm tamamen kaybolur) ve **atanmış her günlük ve haftalık
görevi ayrı bir satırda** gösterir. Tamamlanmamış görevler ilerlemeleri ve XP
ödülleriyle sarı; **tamamlanmış görevler yeşil renkte, yanında "Tamamlandı"
yazısı ve kazandırdığı XP ile** listelenir. Bölümler arasında ince ayırıcı
çizgiler vardır.

Panel, sunucuda gerçekleşen her değişikliği (beceri/sınıf XP kazanımı, unvan
değişikliği, yönetici müdahalesi dahil) yaklaşık 2 saniye içinde
**kendiliğinden** günceller — `/panel` yalnızca paneli mevcut oturum için
tamamen gizler/gösterir. Tercih kaydedilmez; her katılışta veya sunucu
yeniden başladığında panel varsayılan olarak görünür duruma döner.

---

## Prestij

Prestij yapabilmek için üç şart birden sağlanmalıdır: dört becerinin de en
az 25. seviyede olması, seçili sınıfın kendi sınıf seviyesinin maksimuma
(varsayılan 50) ulaşmış olması, ve o sınıfın sinerji becerisinin de kendi
maksimum seviyesine (100) ulaşmış olması. `/prestij` bu şartları ayrı ayrı
✔/✘ olarak listeler ve onay düğmesini aynı ekranda sunar.

Prestij, seçili sınıfın sınıf seviyesini ve dört becerinin tümünü sıfırlar —
**ancak Güç düşmez**: sıfırlanmadan önceki Güç kalıcı olarak "biriktirilir" ve
becerileriniz tekrar seviye atladıkça Güç kaldığı yerden artmaya devam eder.
Karşılığında kalıcı olarak her prestij rankında +%25 XP kazanımı (hem beceri
hem sınıf XP'sinde) ve seçili sınıfa özgü kalıcı bir bonus (can/hasar/enerji)
kazanılır. Prestijli oyuncular liderlik tablosunda ham seviyeden bağımsız
olarak her zaman önde gösterilir. Prestij yapmak sunucu genelinde bir duyuru,
bir havai fişek ve ses efektiyle kutlanır; varsayılan maksimum prestij
sayısı 10'dur.

---

## Görevler

`/gorev` dört bölümlü bir görev panosu açar:

- **Günlük Görevler** — geniş bir havuzdan her gün rastgele atanan 3 görev.
- **Haftalık Görevler** — havuzdan her hafta rastgele atanan 2 görev.
- **Kalıcı Görevler** — tek seferlik, hiç sıfırlanmayan, herkeste ortak görevler.
- **Tamamlanan Görevler** — bu dönemde bitirdikleriniz, ayrı bir bölümde.

Bir görevin üstüne geldiğinizde **ne yapmanız gerektiğini anlatan açıklaması**,
mevcut ilerlemeniz ve **kazandıracağı XP miktarı** yazar. Tamamlanan bir görev
kendi bölümünden çıkıp yeşil ve parlayarak "Tamamlanan Görevler" bölümüne
geçer, böylece üst bölümler her zaman dürüst bir yapılacaklar listesi kalır.

Görevler doğru blokları kırdıkça veya doğru yaratıkları öldürdükçe otomatik
ilerler (Yaratıcı/İzleyici modda ve oyuncunun yerleştirdiği bloklarda ilerleme
kaydedilmez), periyodu bitince yeni bir rastgele atamayla sıfırlanır. Bir
günlük/haftalık görevi tamamlamak sunucu genelinde (XP miktarı gösterilmeden)
duyurulur.

## Koleksiyonlar ve macera günlüğü

`/koleksiyon` koleksiyonlarınızı ve her koleksiyonun içinde hangi ögeleri
topladığınızı ✔/✘ olarak gösterir. `/gunluk` son on görev tamamlama ve
koleksiyon kilometre taşınızı listeler. Bir görevi tamamlamak, bir koleksiyon
ögesi edinmek ve bir koleksiyonun tamamını tamamlamak, **seçili sınıfınızın**
XP'sini kazandırır (sınıf seçili değilse ödül verilmez).

## Liderlik tabloları

`/liderlik`; Güç (önce prestij, sonra toplam beceri seviyesi sırasına göre),
prestij, tamamlanan görev sayısı, edinilen koleksiyon ögesi sayısı ve her
yapılandırılmış beceri için en üst sıradaki oyuncuları gösteren tablolara
açılır. Aynı tablolara `/menu`'nün Topluluk kategorisinden de göz atılabilir.
Sonuçlar her açılışta veritabanını sorgulamak yerine kısa, yapılandırılabilir
bir süre için önbelleğe alınır (`leaderboards.yml`).

## Unvanlar ve kozmetikler

`titles.yml` ve `cosmetics.yml`, her biri tek bir kazanma koşuluna sahip
(seviye/prestije ulaşma, belirli bir görevi/koleksiyonu tamamlama veya bir
beceri seviyesine ulaşma) açılabilir öge katalogları tanımlar. `/unvan` ve
`/kozmetik` neyi açtığınızı gösterir ve tıklayarak kuşanmanızı sağlar; aynı
kataloglara `/menu`'den de göz atılabilir. Kuşanılan bir unvan, sohbette
adınızın önünde **kendi yapılandırılmış rengi ve kalınlığıyla** görünür.
Kuşanılan bir kozmetik, katıldığınızda ve her seviye atladığınızda kısa bir
parçacık efekti oynatır.

## Görsel geri bildirim

Bir beceriden XP kazanıldığında, o becerinin **güncel seviyesini** ve hangi
cevherin/yaratığın XP verdiğini gösteren bir beceri cübuğu (boss bar) görünür.
Seçili sınıfınızın sinerji becerisinden XP kazandığınızda, beceri cübuğuyla
**aynı anda**, her sınıfa özgü kendi renginde ve o sınıfın güncel seviyesini
gösteren ayrı bir sınıf XP cübuğu de görünür (görev/koleksiyon ödülleri de bu
sınıf XP cübuğu üzerinden gösterilir). Seviye atlamanın kendisi kişisel bir
sohbet mesajıdır. Güç yalnızca 10'un katlarına ulaşıldığında kişisel bir
cübukla belirtilir; 25/50/75/100. Güç seviyelerine ulaşmak sunucu genelinde
duyurulur. Görev tamamlama, koleksiyon tamamlama, sınıf değiştirme ve prestij
kısa bir cübuk ve ekran başlığı gösterir (`feedback.yml` her şeyin ne kadar
süre görünür kalacağını ve hangi ses efektinin çalacağını denetler).

## Envanter menüsü

`/menu`, çerçeveli bir merkez menü açar: en üstte kendi kafanı taşıyan,
Gücünü/prestijini/sınıfını özetleyen kişisel bir başlık, altında dört kategori
— **İlerleme** (profil, beceriler, sınıf/yetenekler, prestij),
**Kabiliyetlerim**, **Görevler** (görevler, koleksiyonlar, macera günlüğü) ve
**Topluluk** (liderlik tabloları, unvanlar, kozmetikler) — her biri ne
içerdiğini anlatan bir açıklama satırıyla, ve en altta **Eklenti Hakkında**
düğmesi.

**Eklenti Hakkında** ekranı (hem `/menu`'den hem `/haliladmin`'den açılır)
sürüm bilgisini ve emeği geçenleri listeler: **Furkan Okan (fxrkqnplus)** ve
**Claude (Anthropic)**.

## Yönetici paneli

`/haliladmin` (`halilibrahimmmo.admin` izni gerektirir, varsayılan op) tamamen
GUI üzerinden çalışan bir yönetici paneli açar — sohbet argümanlı bir yönetici
komutu yoktur. Sunduğu işlemler: `config.yml`/`messages.yml`/tüm oyun verisi
dosyalarını yeniden yükleme, mevcut oturum için ayrıntılı hata ayıklama
kaydını açıp kapatma, çalışan eklenti/Paper sürümünü görüntüleme, çevrimiçi
oyuncuları gezip herhangi birinin salt okunur profil/sınıf/beceri özetini
görüntüleme, ve **kayıtlı her oyuncuyu** (çevrimdışı olanlar dahil) listeleyip
beceri/sınıf seviyesini ve XP'sini tıklamalarla ayarlama ya da oyuncunun
sınıfını zorla değiştirme. Bir seviye atlamaya yol açan her müdahale, hedef
oyuncu çevrimiçiyse organik bir seviye atlamayla birebir aynı geri bildirimi
(cübuk, ses, "yeni kabiliyet açıldı" ekran yazısı, panel güncellemesi)
tetikler. Her yönetici müdahalesi Türkçe bir sunucu günlüğü kaydı bırakır.

## PlaceholderAPI ve LuckPerms (isteğe bağlı)

İkisi de gerçek anlamda isteğe bağlı yumuşak bağımlılıktır — sunucu, ikisinden
biri, her ikisi ya da hiçbiri kurulu olmasa da aynı şekilde açılır.
**PlaceholderAPI** kuruluyken bu eklenti, `%halilibrahimmmo_level%`, `_xp`,
`_xp_to_next_level`, `_prestige`, `_class`, `_title`, `_cosmetic` ve her
yapılandırılmış beceri için `_skill_<id>_level`/`_skill_<id>_xp` yer
tutucularını sunan bir `halilibrahimmmo` eklentisi (expansion) kaydeder.
Güç artık dört becerinin toplamından türetildiği için (bkz. "İlerleme
temeli") `_xp` ve `_xp_to_next_level` her zaman `0` döner — `_level` ise
güncel Gücü göstermeye devam eder.
**LuckPerms** kuruluyken bu eklenti, `halilibrahimmmo:level`,
`halilibrahimmmo:prestige` ve `halilibrahimmmo:class` değerlerini LuckPerms'e
besleyen salt okunur bir bağlam hesaplayıcı (context calculator) kaydeder;
böylece sunucu yöneticileri ilerlemeye bağlı kendi izin kurallarını
yazabilir — bu eklenti LuckPerms'e asla kendisi yazmaz.

Geliştirici API'si, değişmez beceri/sınıf/görev tanımlarını ve asenkron salt
okunur oyuncu sorgularını sunar. Harici eklentilerin XP vermesine veya
kabiliyet tetiklemesine izin vermez.

## Test

`./gradlew test`, hızlı birim/yapılandırma test paketini çalıştırır (harici
servis gerektirmez; gerçek bir SQLite dosyasına karşı bir göç/migration
çalıştırma kontrolü dahildir). `./gradlew integrationTest` ayrıca
Testcontainers üzerinden gerçek MySQL, MariaDB ve PostgreSQL konteynerlerine
karşı her gönderilen veritabanı göçünü ve temsili bir repository okuma/yazma
örneklem setini çalıştırır — bu, çalışan bir Docker daemon'u gerektirir ve
bilinçli olarak `build`/`test`/`check`'ten hariç tutulmuştur; bu yüzden ayrı
ve açıkça çalıştırılmalıdır.

## Sunucuya özel ikon paketi

Eklenti, kendi adlandırılmış ikon setini taşır: `messages.yml` içinde
`<simge:guc>`, `<simge:prestij>`, `<simge:kabiliyet>`, `<simge:tamam>` gibi
bir etiket yazdığınız her yerde ilgili simge basılır. İkonlar panelde,
menülerde, boss cübuklarında ve sohbet mesajlarında kullanılır. Simgenin
kendi rengi yoktur — etrafındaki MiniMessage renk etiketi neyse onu alır,
böylece aynı simge panelde altın, menüde gri görünebilir. Kullanılabilir
adların tam listesi `messages.yml` başlığındaki yorumdadır. Tüm simgeler
kasıtlı olarak Minecraft'ın varsayılan yazı tipinin kapsadığı Unicode
aralığından seçilmiştir; ayrıca bir kaynak paketi (resource pack) kurmanız
gerekmez.

## Dil politikası

Bir oyuncunun veya sunucu yöneticisinin oyun içinde gördüğü her şey (mesajlar,
GUI'ler, komutlar, BossBar'lar, unvanlar, config yorumları) Türkçedir. Java
kaynak kodu — paketler, sınıflar, metodlar, değişkenler — İngilizce kalır.
