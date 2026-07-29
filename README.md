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

Neredeyse her komut yalnızca ilgili GUI ekranını açan bir kısayoldur; sohbete
liste basan, sohbetten değer değiştiren hiçbir komut yoktur. Tek istisna
**`/ev` ve `/takas`**: biri eve verilecek ismi, diğeri takas yapılacak
oyuncunun adını ister — ikisi de bir menünün soramayacağı serbest metindir.

Komutlar **gerçek Türkçe harflerle** yazılır. Türkçe karakter içeren her
komutun eski ASCII yazımı da **takma ad (alias)** olarak çalışmaya devam eder,
yani `/menü` ve `/menu` aynı ekranı açar — Türkçe klavyesi olmayan bir oyuncu
da her ekrana ulaşabilir.

| Komut | Takma ad | Ne açar |
|---|---|---|
| `/menü` | `/menu` | Ana menü (dört kategori + kişisel başlık + Eklenti Hakkında) |
| `/mmo` | — | MMO sistemini ilk kez aktifleştirir, ardından sınıf tanıtım ekranını açar |
| `/profil` | — | Profil ekranı (Güç, prestij, sınıf) |
| `/sınıf` | `/sinif` | Sınıf ekranı: sınıf simgeleri + seçili sınıfın yetenek ağacı |
| `/kabiliyet` | — | Kabiliyet menüsü (seçim yapılır, etkinleştirme oyun içindedir) |
| `/görev` | `/gorev` | Görev panosu (günlük / haftalık / kalıcı / tamamlananlar) |
| `/koleksiyon` | — | Koleksiyon ekranı |
| `/günlük` | `/gunluk` | Macera günlüğü |
| `/prestij` | — | Prestij şart listesi ve onay düğmesi |
| `/liderlik` | — | Liderlik tabloları |
| `/ünvan` | `/unvan` | Unvan gardırobu |
| `/kozmetik` | — | Kozmetik gardırobu |
| `/beceri` | `/beceriler` | Becerilerim ekranı (altı becerinin tümü) |
| `/madencilik`, `/odunculuk` | — | O becerinin detay ekranı |
| `/kazıcılık`, `/avcılık`, `/tarımcılık`, `/hayvancılık` | `/kazicilik`, `/avcilik`, `/tarimcilik`, `/hayvancilik` | O becerinin detay ekranı |
| `/ev` | `/home` | Evine ışınlanır (`/ev ayarla <isim>`, `/ev sil`) — 5 dakikada bir |
| `/rtp` | `/rastgele` | Haritada rastgele, güvenli bir noktaya ışınlanır — 30 dakikada bir |
| `/takas <oyuncu>` | `/trade` | Takas isteği (`/takas kabul`, `/takas ret`, `/takas iptal`) |
| `/panel` | — | Sağdaki bilgi panelini gizler/gösterir |
| `/haliladmin` | — | Yönetici paneli (`halilibrahimmmo.admin` izni gerekir) |

**Ekranda görünen her süre** saat/dakika/saniye olarak yazılır: kabiliyet
bekleme süresi, sınıf değiştirme bekleme süresi, geri sayımlar. "130 saniye"
değil, "2 dakika 10 saniye" görürsünüz. Birim kelimeleri `messages.yml`
içindeki `sure:` bölümünden değiştirilebilir.

---

## İlerleme temeli

Oyun içinde **Güç** olarak adlandırılan genel seviye, bağımsız bir XP
kaynağına sahip değildir — altı becerinin (kazıcılık, odunculuk, madencilik,
avcılık, tarımcılık, hayvancılık) seviyelerinin toplamına, prestij yaptıkça
kalıcı olarak biriken Güç'ün eklenmesiyle hesaplanır. Prestij, sıfırladığı
seviyelere rağmen Güç'ü asla düşürmez.

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

  **Ekinler bu kuralın tek istisnasıdır**, çünkü tarım zaten "ek ve biç"
  demektir: kendi ektiğiniz buğday, havuç, patates, pancar da tam olarak
  olgunlaştığında XP verir. Olmamış bir ekini kırmak hiçbir şey kazandırmaz,
  dolayısıyla ekip-sökerek XP çevirmek mümkün değildir. Saman balyaları ekin
  değildir: yalnızca **köylerdeki doğal balyalar** sayılır, kendi koyduğunuz
  balya sayılmaz.

Her Gradle komutu için Java 26 kullanın; çalışma alanında kurulu JDK
`C:\Program Files\Java\jdk-26.0.1` konumundadır.

---

## Beceriler

**Altı** bağımsız olarak seviyelenen beceri vardır. Hepsi `skills.yml`
içindeki aynı açık 1–100 seviye eğrisini kullanır, ama her biri farklı bir
eylemden beslenir ve her blok/yaratık/iş **oyun içindeki gerçek değerine
göre** XP verir.

| Beceri | Simge | Nasıl kazanılır | En az → en çok XP |
|---|---|---|---|
| **Kazıcılık** (`/kazıcılık`) | Demir kürek | Toprak, kum, çakıl, kil gibi yumuşak blokları kazmak | 6 (toprak) → 18 (kil) |
| **Odunculuk** (`/odunculuk`) | Demir balta | Kütük kesmek | 9 (meşe) → 21 (nether sapları) |
| **Madencilik** (`/madencilik`) | Demir kazma | Cevher kırmak | 12 (kömür) → 300 (kadim kalıntı) |
| **Avcılık** (`/avcılık`) | Demir kılıç | Yaratık öldürmek | 15 (zararsız hayvanlar) → 2400 (Ejderha) |
| **Tarımcılık** (`/tarımcılık`) | Buğday | Tarlada yapılan her iş (aşağıda) | 3 (çapalama) → 30 (köylü ticareti) |
| **Hayvancılık** (`/hayvancılık`) | Kayış | Hayvan yetiştirmenin her adımı (aşağıda) | 3 (süt sağma) → 45 (evcilleştirme) |

Her becerinin kendine ait bir simgesi vardır ve bu simge her yerde aynıdır:
`/beceri` listesinde, o becerinin kendi detay ekranında ve liderlik
tablosunda. Sınıfının beslendiği beceriler listede ayrıca işaretlenir ve
parlar. Bir beceriye tıklamak doğrudan o becerinin detay ekranını açar.

Aradaki farklar bilinçli olarak büyük tutulmuştur. Örneğin madencilikte
redstone 21, altın 60, elmas 135, zümrüt 180 XP verir; avcılıkta bir inek ya
da domuz 15, bir zombi 24, bir Wither İskeleti 90, bir Warden 1800 XP verir.
Listelenmemiş her yaratık en düşük değeri alır, çünkü listelenmemiş yaratıklar
neredeyse her zaman çiftlikte sınırsız üretilebilen hayvanlardır.

### Tarımcılık

Tarımcılık hem blok kırmaktan hem de tarlanın etrafındaki işlerden beslenen
tek beceridir:

| Nasıl | XP |
|---|---|
| Toprağa çapa ile vurup tarla açmak | 3 |
| Ekinlere kemik tozu kullanmak | 6 |
| Saman balyası kırmak (yalnızca köylerdeki doğal balyalar) | 6 |
| Olgunlaşmış ekin biçmek (buğday, havuç, patates, pancar, böğürtlen) | 9 |
| Ocakta patates veya deniz yosunu pişirmek | 9 |
| Ekmek üretmek | 12 |
| Karpuz, balkabağı veya kakao kesmek | 12 |
| Nether mantarı toplamak | 15 |
| Meşale çiçeği / testi bitkisi biçmek | 18 |
| Köylülerle ticaret yapmak | 30 |

### Hayvancılık

Hayvancılık, hayvanı **yaşatmakla** ilgilidir; hayvan öldürmek her zaman
avcılığa yazılır, hayvancılığa değil. Hiçbir blok veya yaratık kaynağı yoktur,
tamamen şu eylemlerden beslenir:

| Nasıl | XP |
|---|---|
| Kova ile inek/mooshroom/keçi sağmak | 3 |
| Kayışla inek, domuz veya koyun taşımak (her 20 saniyede bir) | 6 |
| Koyun kırpmak | 9 |
| Ocakta hayvan eti pişirmek (biftek, pirzola, kuzu, tavşan, tavuk) | 12 |
| İki hayvanı çiftleştirip yavru elde etmek | 24 |
| Pasta üretmek | 30 |
| Kurt, at, eşek, katır, lama, kedi, papağan evcilleştirmek | 45 |

Kayışla taşıma ödülü **sürünün büyüklüğünden bağımsızdır**: on ineği götürmek
bir ineği götürmekle aynı iştir, hayvan başına ödemek yalnızca sürü
yığmayı ödüllendirirdi.

### Kilometre taşları

Bir beceri 25/50/75/100. seviyeye ulaştığında **kalıcı** bir bonus kazandırır.
Kademeler zayıftan güçlüye sıralanmıştır: 25. seviye basit bir giriş bonusu,
becerinin asıl güçlü bonusu her zaman 75. ve 100. seviyelerde.

| Beceri | 25 | 50 | 75 | 100 |
|---|---|---|---|---|
| Kazıcılık | +2 can | +10 enerji | +20 enerji | +30 enerji |
| Odunculuk | **Ağaç devirme** | +10 enerji | Hız I | Hız II |
| Madencilik | +2 can | +10 enerji | Acele I | Acele II |
| Avcılık | +2 can | +4 can | Yaratıklara +%10 hasar | Yaratıklara +%20 hasar |
| Tarımcılık | +2 can | +12 enerji | +6 can | +30 enerji |
| Hayvancılık | **Yem iadesi** | +4 can | +6 can | +25 enerji |

İki kilometre taşı bir istatistik değil, **açılan bir özelliktir**:

- **Ağaç devirme** (odunculuk 25) — bir ağacın toprakla temas eden **kök**
  kütüğünü kestiğinizde ağacın tamamı devrilir; bağlı bütün kütükler (en
  fazla 48) birden düşer. Gövdenin ortasından kesmek bunu tetiklemez, yoksa
  her kütük kırışı zincirleme bir yıkıma dönerdi.

  **Yalnızca gerçek ağaçlarda çalışır.** Bir köy evinin kütük direği de
  dünyanın kendi ürettiği bir bloktur, dolayısıyla "oyuncunun koyduğu blok"
  kuralı orada işe yaramaz. Bunun yerine üç şart birden aranır: kütüğün altında
  ağacın kök salabileceği bir zemin (toprak, yosun, çamur, kum/kil ya da
  nylium) olmalı; gövdeye değen en az 4 yaprak (nether mantarlarında wart
  bloğu/shroomlight) bulunmalı; ve gövde hiçbir yapı malzemesine
  (tahta/merdiven/basamak/çit/kapı/kapak/duvar/cam/yün ve köy iş istasyonları)
  değmemelidir. Ormanın ortasındaki bir evi güvende tutan da bu üçüncü şarttır.
  Aynı denetim **Ağaç Kırıcı kabiliyeti** için de geçerlidir; bina üzerinde
  kullanıldığında kabiliyet harcanmaz, kurulu kalır ve sizi uyarır.
- **Yem iadesi** (hayvancılık 25) — bir yavru doğduktan sonra %15 ihtimalle
  çiftleştirme yeminin **iki adedi birden** (iki ebeveyn için harcadığınız
  kadarı) elinize geri döner. Zar yavru doğduktan *sonra* atılır, yani
  şansınız tutmasa da zaten harcayacağınızdan fazlasını kaybetmezsiniz.

Bu bonuslar **ölünce kaybolmaz** — yeniden doğduğunuzda otomatik olarak geri
yüklenir. Yalnızca prestij ile o beceri sıfırlandığında düşerler. Her becerinin
detay ekranı (`/madencilik` vb.) mevcut seviye/XP'yi, becerinin beslediği
sınıfı, kilometre taşı açıklamalarını ve o becerinin liderlik tablosunu
gösterir.

---

## Sınıflar

**Üç** sınıf vardır. Her sınıfın bir veya birden fazla **sinerji becerisi**
vardır: o becerilerden kazanılan XP'nin bir kısmı otomatik olarak sınıfın
kendi seviye izine de yansır ve ayrıca **+%20 beceri XP bonusu** kazandırır,
dolayısıyla sınıfınıza uygun oynamak hem beceriyi hem sınıfı birlikte büyütür.

| Sınıf | Sinerji becerileri | Simge | Kabiliyet sayısı |
|---|---|---|---|
| **Amansız Savaşçı** (`avci`) | Avcılık | Elmas kılıç | 3 |
| **Mağara Adamı** (`madenci`) | Madencilik | Elmas kazma | 3 |
| **Tabiat Vârisi** (`tabiat-varisi`) | Odunculuk, Kazıcılık, Tarımcılık, Hayvancılık | Demir balta | 4 |

**Tabiat Vârisi** tek başına dört beceriyi birden besler — toprağın, ormanın,
tarlanın ve sürünün vârisidir. Kabiliyetleri de tam olarak bu dört becerinin
her birine bir tane düşer ve **en zayıftan en güçlüye** doğru açılır (5 → 15 →
25 → 40), böylece sınıf seviyesi yükseldikçe elindeki araç da gözle görülür
şekilde güçlenir. Amansız Savaşçı ve Mağara Adamı ise dar ama derin
uzmanlardır.

Her sınıf ayrıca prestij yaptıkça büyüyen, kendine özgü kalıcı bir bonus
taşır: Amansız Savaşçı PvE hasarı, Mağara Adamı maksimum enerji, Tabiat Vârisi
maksimum can.

### Yetenek ağacı: iki ayrı puan havuzu

Seçili sınıfın altında arttırılabilir yetenekler bulunur ve bunlar **birbirinin
yerine geçmeyen iki ayrı puan havuzundan** harcanır:

| Havuz | Ne alır | Nasıl kazanılır |
|---|---|---|
| **Özellik Puanı** | Kalıcı karakter istatistikleri: can, yaratıklara karşı hasar, maksimum enerji | Her 25 Güç seviyesinde 1, her prestijde 4 |
| **Kabiliyet Puanı** | Tek bir kabiliyete özel iyileştirmeler | Her 40 Güç seviyesinde 1, her prestijde 2 |

Bir kabiliyet puanı asla bir özellik yeteneğine, bir özellik puanı da asla bir
kabiliyet yeteneğine harcanamaz. Bölünmenin bütün amacı budur: kabiliyetlerini
güçlendirmek isteyen bir oyuncu bunu ham istatistiklerinden vazgeçerek satın
alamaz, tersi de geçerlidir. **Kabiliyet puanı bilerek daha nadirdir.**

Kabiliyet yetenekleri bir kabiliyetin **enerji maliyetini azaltabilir**,
**bekleme süresini kısaltabilir**, **etki sınırını genişletebilir**, **etki
süresini uzatabilir** veya **etkisinin gücünü arttırabilir** (bir hasar
çarpanını, bir düşme ihtimalini ya da ham hasarı). Menüde bir yeteneğin üstüne
geldiğinizde her rankın tam olarak ne verdiği ve şu ana kadar biriken toplam
etki yazar.

**Ekran iki ayrı bloktan oluşur.** En üstte oynadığınız sınıfı ve o sınıfın
kendi seviyesini/XP'sini gösteren bir başlık, altında sınıf simgeleri ve iki
puan sayacı yan yana, sonra **Özellik Yetenekleri** ve **Kabiliyet
Yetenekleri** kendi başlıkları altında ayrı satır blokları hâlinde. Özellik
yetenekleri kitap, kabiliyet yetenekleri büyülü kitap simgesi taşır ve her
blok kendi içinde alfabetik sıralanır, böylece bir yetenek her açılışta aynı
yerde durur. Ekran, sınıfınızın kendi renginde çerçevelenir.

Her iki havuz da tüm sınıflar arasında **paylaşılır**, dolayısıyla sınıf
değiştirmek harcanmış rankları silmez.

### Sınıf değiştirme

Sınıf değiştirme, ilk sınıf seçiminde kullanılan ekranın **aynısını** açar:
üç sınıf, her birinin tanıtım yazısı ve kabiliyet listesiyle. Ekranın
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

Sınıf değiştiğinde **seçili kabiliyetiniz kuşanılmış olmaktan çıkar** — ister
kendiniz değiştirin, ister bir yönetici değiştirsin. O sırada çalışan bir
kabiliyet varsa (kurulu bir blok kabiliyeti, bir hasar/düşme etkisi ya da
Maden Feneri'nin parıltıları) o da sona erer; hiçbiri artık sahip olmadığınız
bir sınıfa aittir. Kabiliyet bekleme süreleri bilerek korunur, yani sınıf
değiştirmek bir beklemeyi sıfırlamanın yolu değildir.

---

## Kabiliyetler

Her kabiliyet tam olarak bir sınıfa aittir; o sınıf seçili değilken
kullanılamaz. Açılma koşulu **sınıf seviyesidir** (beceri seviyesi değil).

| Sınıf | Kabiliyet | Sv. | Enerji | Bekleme | Etki |
|---|---|---|---|---|---|
| Tabiat Vârisi | Toprak Sezgisi | 5 | 20 | 1 dk 30 sn | Kırdığın bloklardan %50 ihtimalle ekstra düşme |
| Tabiat Vârisi | Filiz Patlaması | 15 | 25 | 3 dk | 6 blok yarıçapındaki tüm ekinleri anında olgunlaştırır |
| Tabiat Vârisi | Sürü Çağrısı | 25 | 30 | 4 dk | 8 blok yarıçapındaki tüm yetişkin hayvanları yem harcamadan çiftleşme moduna sokar |
| Tabiat Vârisi | Ağaç Kırıcı | 40 | 25 | 1 dk 15 sn | Bağlı tüm kütükleri (en fazla 32) birden keser |
| Amansız Savaşçı | Gece Görüşü | 5 | 15 | 2 dk | 60 saniye karanlıkta net görüş |
| Amansız Savaşçı | Avcı Odağı | 10 | 25 | 1 dk 30 sn | PvE hasarı %50 artar (enerji tüketir) |
| Amansız Savaşçı | Yıkıcı Darbe | 30 | 30 | 30 sn | Ayağının altında şok dalgası, 4 blok yarıçapı |
| Mağara Adamı | Cevher Sezgisi | 5 | 25 | 1 dk 30 sn | Kırdığın bloklardan %40 ihtimalle ekstra düşme |
| Mağara Adamı | Damar Kırıcı | 20 | 30 | 1 dk 40 sn | Bağlı aynı cevher damarını (16 blok) birden kazar |
| Mağara Adamı | Maden Feneri | 35 | 30 | 7 dk 30 sn | 36 blok yarıçapındaki cevherleri 30 sn parlatır |

### Nasıl kullanılır

1. `/kabiliyet` menüsünden bir kabiliyet seçin. Her kabiliyetin konusuna uygun
   kendi simgesi vardır; **seçili olan büyülü (parlayan)** görünür, seçildiğinde
   kısa bir ses efekti çalar ve sohbete onay mesajı gelir. Menü, sınıfınızın
   kendi renginde çerçevelenir.
2. Kabiliyetin istediği aleti (kazma / balta / kürek / kılıç, kabiliyete göre
   değişir) elinize alın.
3. **Çift sağ tıklayın.** İlk sağ tıklamada ekranın altında
   *"… kabiliyetini aktif etmek için tekrar sağ tıkla"* yazar; kabiliyeti
   gerçekten çalıştıran ikinci tıklamadır. Onay için 3 saniyeniz vardır.
   Bir bloğa, boş havaya ya da bir yaratığa tıklamak — üçü de çalışır;
   saldırı kabiliyetleri için önce bir duvar bulmanız gerekmez.

   Tek tıkla çalışmamasının sebebi basit: kabiliyetler enerji harcar ve uzun
   bekleme süreleri başlatır, sağ tıklamak ise kapı açarken, blok koyarken,
   hayvan beslerken sürekli yaptığınız bir şeydir.

**Kendi işi olan bloklar kabiliyeti tetiklemez.** Kapı, çiftlik kapısı, tuzak
kapısı, şalter, düğme, örs, çalışma masası, fırın çeşitleri, sandık, varil,
büyü masası, not bloğu, çiçek saksısı, tabela, yatak ve benzeri — sağ
tıkladığınızda zaten kendi işini yapan hiçbir blok kabiliyetinizi açmaz.
Üssünüzde dolaşırken kapı açmak artık enerji ve bekleme süresi harcamaz.
Gerçekten o kapının önünde kabiliyetinizi kullanmak istiyorsanız **shift
basılı tutup sağ tıklayın**: Minecraft zaten shift'liyken bloğun kendi
davranışını bastırır, yani sandığa blok koyarken kullandığınız hareketin
aynısı burada da işe yarar.

**Zaten çalışan bir kabiliyete tekrar tıklamak** (yaratıcı mod dışında)
hiçbir mesaj üretmez. Eskiden önce "tekrar sağ tıkla" yazar, sonra ikinci
tıklamada "beklemelisin" diye reddederdi; oysa eylem çubuğundaki geri sayım
kabiliyetin çalıştığını zaten söylüyor.

**Yaratıcı (Creative) modda hiçbir sınır yoktur:** kabiliyet değiştirme
beklemesi, kabiliyet bekleme süresi, enerji maliyeti ve süre sınırı
uygulanmaz. Kabiliyet siz kapatana kadar açık kalır — açarken olduğu gibi
**çift sağ tıklayarak kapatırsınız**. Eylem çubuğunda geri sayım yerine
"Yaratıcı mod — kapatmak için çift sağ tıkla" yazar.

Bunun **tek istisnası, işini anında bitiren kabiliyetlerdir**: Yıkıcı Darbe,
Filiz Patlaması ve Sürü Çağrısı. Şok dalgası zaten atıldıktan, ekinler zaten
olgunlaştıktan sonra süreyi açık tutmanın hiçbir anlamı yok — üstelik
kabiliyeti siz kapatana kadar tekrar kullanılamaz hâle getiriyordu. Bu üçü
yaratıcı modda da kendi normal sürelerini kullanır (Yıkıcı Darbe için 2
saniye), yani çift sağ tıkladıkça tekrar tekrar kullanılabilir.

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
Çalıştığında 36 blok yarıçapındaki cevherler 30 saniye boyunca duvarların
ardından bile parlar; işaretlenmiş bir cevheri kırdığınızda parıltısı anında
söner. Tarama **size en yakın cevherlerden başlayarak dışa doğru** genişler,
dolayısıyla gözünüzün önündeki açıkta duran kömür/demir damarı da en az
duvarın arkasındaki kadar kesin biçimde parlar. Yaratıcı modda kabiliyeti
çift sağ tıklayarak kapattığınızda **sizin yaktığınız bütün parıltılar da
anında söner**.

**Avcı Odağı** süreye değil enerjiye dayalıdır: her isabetli vuruşta enerji
tüketir, enerjiniz biterse ya da en fazla 30 saniye sonra kendiliğinden sona
erer — ve yalnızca yaratıklara (PvE) karşı geçerlidir, PvP'de hiçbir etkisi
yoktur.

**Filiz Patlaması** çevrenizdeki 6 blok yarıçapındaki bütün olmamış ekinleri
tek seferde hasada hazır hâle getirir — bir kova kemik tozu gibi, ama hiç
kemik tozu harcamadan. Zaten olgunlaşmış ekinlere dokunmaz, dolayısıyla size
kaç bitkinin gerçekten büyüdüğünü söyler.

**Sürü Çağrısı** çevrenizdeki 8 blok yarıçapındaki bütün **yetişkin**
hayvanları tek bir yem harcamadan çiftleşme moduna sokar. Yavrular ve zaten
çiftleşme modundaki hayvanlar atlanır, dolayısıyla aynı sürüye tekrar
kullanmak bir işe yaramaz.

### Enerji

Enerji, tüm kabiliyetlerin paylaştığı, kaydedilmeyen bir havuzdur (varsayılan
maksimum 100; sınıf ve kilometre taşı bonuslarıyla artar). Saniyede 6 puan
dolar. Eylem çubuğunda her zaman `mevcut/maksimum` biçiminde görünür.

---

## Profil paneli (sidebar)

Ekranın sağında kalıcı, renklendirilmiş bir bilgi paneli. Yukarıdan aşağıya:

1. **Oyuncu adı** — varsa kuşanılan unvan kendi rengiyle isimden önce
   ("Oyuncu: [Unvan] Ad").
2. **Güç ve prestij** — tek satırda, altın ve açık mor.
3. **Zenginlik** — mavi renkte toplam Zenginlik Puanınız.
4. **Sınıf ve sınıf seviyesi** — tek satırda, XP ilerlemesiyle birlikte.
5. **Kabiliyet** — çalışan bir kabiliyet varsa adı ve kalan süresi, yoksa
   seçili olan, o da yoksa "Kabiliyet seçilmedi".
6. **Ev / Takas durumu** — ikisinden biri açıldıktan sonra görünür; evinizin
   adını (ya da "Ayarlanmadı") ve takasın açık olup olmadığını gösterir.
   **Bir takas sürerken** hemen altında "Takas açık: <oyuncu>" satırı çıkar.
7. **Başlangıç Görevleri** — yalnızca tamamlanmamış olanlar varken; hepsi
   bitince bu bölüm tamamen kaybolur.
8. **Günlük**, **Haftalık** ve **Kalıcı Görevler** — her biri kendi başlığı
   altında ayrı bir bölüm. Tamamlanmamış görevler ilerlemeleri ve XP
   ödülleriyle sarı; **tamamlanmış görevler yeşil** listelenir. Görevler
   panelde **kısa adlarıyla** yazılır (bkz. Görevler bölümü).
9. En altta `/panel` hatırlatması.

Bölümler arasında ince ayırıcı çizgiler vardır. **Minecraft bu panelde en
fazla 15 satır gösterir**; eklenti fazlasını kendisi kırpar (en alttaki
`/panel` satırı her zaman korunur), bu yüzden satırlar bilinçli olarak
birleştirilmiştir — örneğin sınıfınızın açık kabiliyetleri artık tek tek
listelenmez, tam listeyi `/kabiliyet` ekranı gösterir. **Panelde hangi
görevlerin görüneceğine siz karar verebilirsiniz**: bkz. "Görevleri panele
sabitleme".

Panel, sunucuda gerçekleşen her değişikliği (beceri/sınıf XP kazanımı, unvan
değişikliği, yönetici müdahalesi dahil) yaklaşık 2 saniye içinde
**kendiliğinden** günceller — `/panel` yalnızca paneli mevcut oturum için
tamamen gizler/gösterir. Tercih kaydedilmez; her katılışta veya sunucu
yeniden başladığında panel varsayılan olarak görünür duruma döner.

---

## Prestij

Prestij yapabilmek için üç şart birden sağlanmalıdır: altı becerinin de en
az 25. seviyede olması, seçili sınıfın kendi sınıf seviyesinin maksimuma
(varsayılan 50) ulaşmış olması, ve o sınıfın sinerji becerilerinin
**hepsinin** kendi maksimum seviyesine (100) ulaşmış olması. `/prestij` bu
şartları ayrı ayrı ✔/✘ olarak listeler — sinerji becerilerinin her biri
kendi satırında görünür, böylece dört beceriyi birden besleyen Tabiat Vârisi
oynarken hangisinin sizi geciktirdiğini tek bakışta görürsünüz — ve onay
düğmesini aynı ekranda sunar.

Prestij, seçili sınıfın sınıf seviyesini ve altı becerinin tümünü sıfırlar —
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

Toplam **65 görev** vardır: 36 kalıcı, 16 günlük, 13 haftalık. `/görev`,
solunda dört sekme bulunan bir görev panosu açar. Aynı anda tek bir bölüm
gösterilir, o bölüme de 42 slot ayrılmıştır:

- **Günlük Görevler** — 16 görevlik havuzdan her gün rastgele atanan 3 görev.
- **Haftalık Görevler** — 13 görevlik havuzdan her hafta rastgele atanan 2 görev.
- **Kalıcı Görevler** — tek seferlik, hiç sıfırlanmayan, herkeste ortak 36 görev.
- **Tamamlanan Görevler** — bu dönemde bitirdikleriniz.

Bir görevin üstüne geldiğinizde **ne yapmanız gerektiğini anlatan açıklaması**,
mevcut ilerlemeniz ve **kazandıracağı XP ile hangi beceriye yazılacağı** yazar.
Tamamlanan bir görev kendi bölümünden çıkıp "Tamamlanan Görevler" sekmesine
geçer, böylece üst bölümler her zaman dürüst bir yapılacaklar listesi kalır.

### Görevleri panele sabitleme

Görev panosunda **tamamlanmamış bir göreve tıklamak onu sağdaki panele
sabitler** (tekrar tıklamak kaldırır). Her bölüm için en fazla **3** görev
sabitlenebilir. Sabitli görevler menüde parlar ve açıklamasında "Panele
sabitlendi" yazar.

- **Kalıcı görevler panelde yalnızca sabitlediklerinizle görünür.** 36 kalıcı
  görevin hepsini listelemek zaten mümkün değildi (panel 15 satır gösterir),
  bu bölüm de tam olarak bu yüzden var: hangi üç uzun soluklu hedefin gözünüzün
  önünde duracağına siz karar verirsiniz.
- **Günlük ve haftalıkta sabitleme bir filtredir.** Hiçbir şey sabitlemezseniz
  o dönem size atanmış görevlerin hepsi görünür (eskisi gibi); sabitlerseniz
  yalnızca seçtikleriniz görünür. Size atanmamış bir havuz görevi sabitlenip
  panele *eklenemez*, çünkü atanmamış bir görev zaten hiç ilerlemez — hiç
  hareket etmeyen bir satır olurdu.

Panelde görevler **kısa adlarıyla** yazılır (`quests.yml` içindeki `kisa-ad`).
Scoreboard satırları alt satıra kaymaz, kesilir; menüde okunan uzun başlıklar
panelde kelimenin ortasından kopuyordu.

### Görev ödülleri hangi beceriye yazılır?

Her görev **eğittiği beceriyi** kendisi bildirir ve ödülünü oraya öder — üstelik
sıradan bir beceri XP'si gibi, aynı sinerji ve prestij çarpanlarıyla. Bunun
doğrudan sonucu şudur: bir görev hem ilgili beceriyi hem de **o beceriden
beslenen sınıfı** birden büyütür.

| Örnek görev | Beceri | Sınıfı buysa ayrıca |
|---|---|---|
| Zombi Temizliği, Örümcek Avı | Avcılık | Amansız Savaşçı |
| Toprak Temizliği, Taş Ustası | Kazıcılık | Tabiat Vârisi |
| Haftalık Derin Kazı, Nether Madenciliği | Madencilik | Mağara Adamı |
| Günlük Oduncu, Haftalık Orman Kesimi | Odunculuk | Tabiat Vârisi |
| Günlük Hasat, Bereketli Tarla | Tarımcılık | Tabiat Vârisi |

Sınıfınız o beceriden beslenmiyorsa sınıf XP'nizi yine alırsınız — hiçbir görev
"yanlış uzmanlık" diye sıfır sınıf XP'si vermez, ama iki kere de saymaz.
Görevden gelen bir seviye atlaması artık diğer bütün seviye atlamalarıyla
**birebir aynı** geri bildirimi verir: sohbet mesajı, ses efekti, boss çubuğu
ve varsa "yeni kabiliyet açıldı" bildirimi.

### Görev türleri

Beş görev türü vardır:

- **Blok kırma** ve **yaratık öldürme** — klasik sayaçlar.
- **Yapı bulma** — o yapının içinde durmanız yeterlidir. Köy (1 800 XP),
  batık gemi, terk edilmiş maden, batık hazine, kadim kalıntılar, yağmacı
  karakolu, nether kalesi, bastion kalıntısı, okyanus anıtı, deneme odaları,
  kale, ormandaki konak (15 000 XP) ve **Kadim Şehir** (18 000 XP).
- **Eşya edinme** — hem yerden alınan hem de bir sandıktan çıkarılan eşyalar
  sayılır. Müzik diski, isim etiketi, ölümsüzlük totemi, elytra ve en zoru
  **ejderha yumurtası** (36 000 XP).
- **Belirli bir iş (`DEED`)** — sayaçla anlatılamayan, hikâyesi olan hedefler:
  bir ghast'ı **kendi ateş topuyla** öldürmek, piglinlerle takas yapmak,
  köylülerle ticaret, büyü masasında büyüleme, balık tutma, hayvan
  evcilleştirme.

**Boyut şartı.** Bir görev `NORMAL`, `NETHER` ya da `THE_END` boyutuna
kilitlenebilir; böylece "End'de 100 enderman öldür" gerçekten End'i şart
koşar, gece bir ovada öldürdüğünüz enderman sayılmaz.

**Bir yeri bulmak yarısıdır.** Kalıcı görevlerin bir kısmı, keşfettiğiniz
yerde iş bitirmenizi ister:

| Görev | Ne ister |
|---|---|
| Konağın Büyücüleri | Ormandaki konağı bul, 3 evoker öldür |
| Yaşlı Muhafız | Okyanus anıtını bul, Elder Guardian'ı öldür |
| Bastion'un Altını | Bastion kalıntısı bul, 10 altın bloğu kaz (Nether) |
| Piglin Ticareti | Piglinlerle 8 kez takas yap |
| Alev Avı / Kara Kemikler | Nether kalesinde 10 blaze ve 10 wither iskeleti öldür |
| Kendi Ateşiyle | Bir ghast'ı geri yolladığın ateş topuyla öldür |
| End'in Sakinleri | End'de toplam 100 enderman öldür |
| Derinin Bekçisi | Warden'ı uyandır ve öldür (45 000 XP) |

XP'ler kasıtlı olarak zorlukla ölçeklenir: bir köy bulmak (1 800) ile Warden'ı
öldürmek (45 000) arasında yirmi beş katlık bir fark vardır.

Görevler otomatik ilerler (Yaratıcı/İzleyici modda ve oyuncunun yerleştirdiği
bloklarda ilerleme kaydedilmez — ekinler bu kuralın istisnasıdır, tıpkı beceri
XP'sinde olduğu gibi), periyodu bitince yeni bir rastgele atamayla sıfırlanır.
Bir günlük/haftalık görevi tamamlamak sunucu genelinde (XP miktarı
gösterilmeden) duyurulur.

---

## Zenginlik Puanı

Herkes **0** puanla başlar ve bu sayı yalnızca **artar**. Zenginlik Puanı bir
para birimi **değildir**: harcanmaz, takas edilmez, hiçbir şey satın almaz —
bu sunucuda ekonomi yoktur. Ölçtüğü şey, dünyadan gerçekte ne çıkardığınızdır.

Her kaynak tasarımı gereği tek seferliktir, çiftlik kurup tekrarlanamaz:

| Kaynak | Örnekler |
|---|---|
| **Doğal bloklar** | Kömür 100, demir 500, altın 2 500, zümrüt 8 000, **elmas 10 000**, kadim kalıntı 40 000, spawner 15 000, ejderha yumurtası 250 000 |
| **Yaratıklar** | Zombi 50, enderman 300, blaze 500, wither iskeleti 800, evoker 2 500, Elder Guardian 12 000, Wither 150 000, **Warden 250 000**, **Ejderha 500 000** |
| **Görev tamamlama** | Varsayılan 2 500; keşif görevleri değerine göre (köy 4 000 → Kadim Şehir 40 000 → ejderha yumurtası 120 000) |
| **Dünya bossları** | Nadirliğine göre 2 000 (sıradan) → 400 000 (mistik) |

Oyuncunun kendi koyduğu bloklar hiçbir puan vermez; listede yer almayan
yaratıklar en düşük değeri alır (çünkü listelenmeyenler neredeyse her zaman
çiftlik hayvanlarıdır).

**Kilometre taşları** (10 bin, 50 bin, 250 bin, 1 milyon, 5 milyon, 25 milyon,
100 milyon) geçildiğinde hem size özel bir mesaj gelir hem de tüm sunucuya
duyurulur. Bir ödül aynı anda birkaç eşiği birden geçerse yalnızca en yükseği
duyurulur — yeni bir oyuncuyu tek bir boss birkaç eşik birden atlatabilir ve
aynı anda beş duyuru gürültüden başka bir şey olmaz.

Zenginlik puanınız panelde mavi renkte, `/liderlik` içinde kendi tablosunda
(elmas simgesiyle) görünür ve **kendine ait ünvanlar ve kozmetikler** açar
(Varlıklı 50 bin → Zengin 250 bin → Servet Sahibi 1 milyon → Hazine Kralı
5 milyon → Efsanevi Zengin 25 milyon).

Bütün değerler `zenginlik.yml` üzerinden değiştirilebilir.

---

## Dünya bossları

**Her 30 dakikada bir**, çevrimiçi bir oyuncunun 120–320 blok uzağında,
haritada isimli bir dünya bossu belirir ve **koordinatlarıyla birlikte tüm
sunucuya duyurulur**. Kimsenin ulaşamayacağı bir boss kimsenin dövüşmediği bir
bosstur; bu mesafe hem gerçekten yola çıkmayı gerektirir hem de bir sonraki
boss gelmeden varılabilir. `/haliladmin` panelinden beklemeden de çağrılabilir.

**Beş nadirlik** vardır ve hangisinin çıkacağı ağırlıklara göre belirlenir
(sıradan 49, nadir 30, epik 20, efsanevi 10, mistik 1 pay):

| Nadirlik | Can | Hasar | Zırh | Davranış | Sınıf XP | Oyun XP | Zenginlik |
|---|---|---|---|---|---|---|---|
| Sıradan | ×3 | ×1.5 | Deri | — | 400 | 60 | 2 000 |
| Nadir | ×6 | ×2.0 | Demir | İksir içer | 1 200 | 150 | 8 000 |
| Epik | ×10 | ×2.6 | Elmas | Işınlanır, iksir içer | 3 000 | 320 | 25 000 |
| Efsanevi | ×16 | ×3.2 | Netherite/Elmas | Kaçar, ışınlanır, iksir içer | 8 000 | 700 | 80 000 |
| Mistik | ×26 | ×4.0 | Tam netherite | Kaçar, ışınlanır, iksir içer | 25 000 | 1 800 | 400 000 |

Nadirlik arttıkça boss daha hızlı, daha canlı ve daha iyi silahlıdır; belirli
bir eşikten sonra kalıcı Güç/Direnç etkileri de taşır. **Canı yarıya
düştüğünde kendine yenilenme basar**, savaş sırasında oyuncuya doğru kısa
ışınlamalar yapar ve **canı çeyreğe düştüğünde bir kez uzağa kaçar** (Hız II
ile birlikte).

**Beş boss türü** vardır — zombi, iskelet, boğulmuş, büyücü, baltacı — ve her
birinin **kendine ait 7 isimlik havuzu** (toplam 35 isim) vardır. İsim,
nadirliği okuyacak kadar yaklaşmadan önce neyle karşı karşıya olduğunuzu
söylemelidir: *Diriltilmiş Jackson* bir zombidir, *Nişancı Edward* bir
iskelet, *Baltacı John* bir baltacı, *İnsan Yiyen Annabelle* de bir zombi.

Boss öldürüldüğünde:

- Öldüren oyuncunun **sınıfına XP** yazılır (normal seviye atlama geri
  bildiriminin tamamıyla: sohbet, ses, cübuk, "yeni kabiliyet açıldı"),
- yere **normal oyun XP'si** düşer,
- nadirliğine göre **ganimet** düşer (mistik bir bossta nether yıldızı,
  netherite külçeleri, büyülü altın elmalar ve %25 ihtimalle elytra),
- ve **bossun kendi ismini taşıyan, nadirlik renginde bir kafa** düşer:
  *"İnsan Yiyen Annabelle'nin Kafası"*.

Boss ayrıca sıradan bir yaratık olarak avcılık XP'si, görev ilerlemesi ve
Zenginlik Puanı da verir — yukarıdakilerin hepsi bunun **üstüne** eklenir.

Bir boss sunucu yeniden başlasa bile yerinde ve ödülleriyle birlikte kalır
(kimliği yaratığın kendi kalıcı verisinde saklanır). Yeniden başlatmadan sağ
kalan bir bossun yalnızca kaçma/iyileşme davranış döngüsü kaybolur; istatistik
ve ödülleriyle çok güçlü sıradan bir yaratık gibi dövüşmeye devam eder.

Bütün değerler, isim havuzları ve ganimet tabloları `bosslar.yml` üzerinden
değiştirilebilir.

---

## Rastgele ışınlanma (`/rtp`)

`/rtp` (takma ad `/rastgele`) sizi haritada rastgele, **güvenli** bir noktaya
ışınlar: dünya doğuş noktasından 500–10 000 blok uzakta, üstü açık, ayak
basacak katı zemini olan bir yer. Su, lav, kaktüs ya da ateş üstüne
bırakılmazsınız. İndiğinizde ekranda bir başlık ve sohbette **indiğiniz
koordinatlar** görünür.

**30 dakikada bir** kullanılabilir. Yalnızca normal dünyada çalışır: Nether'ın
tavanı her "en yüksek blok" cevabının indiği yerdir, End'de rastgele bir nokta
ise ya boşluktur ya da ejderhanın adası.

Güvenli bir nokta bulunamazsa (16 denemeden sonra) hakkınız yanmaz.

---

## Ev (`/ev`)

Sınıf seviyeniz **15**'e ulaştığında — ya da **bir kez prestij yaptıysanız**,
sınıf seviyeniz ne olursa olsun — kendinize bir ev ayarlayabilirsiniz.
Prestijin de sayılmasının sebebi basit: prestij sınıf seviyenizi 1'e
sıfırlar, dolayısıyla yalnızca seviyeye bakan bir kural bu özelliği tam da en
çok emek vermiş oyunculardan her prestijde geri alırdı.

| Komut | Ne yapar |
|---|---|
| `/ev ayarla <isim>` | Bulunduğunuz noktayı, verdiğiniz isimle eviniz yapar |
| `/ev` | Nerede olursanız olun evinize ışınlanır |
| `/ev sil` | Evinizi siler |

Her oyuncunun **bir** evi vardır; yeniden ayarlamak eskisinin üzerine yazar.
İsim en fazla 32 karakter olabilir ve panelde görünür.

**Eve ışınlanmak 5 dakikada bir kullanılabilir.** Ev ayarlamak ve silmek
serbesttir; sayaç yalnızca gerçekten yola çıktığınızda başlar, dolayısıyla
"henüz ev ayarlamadın" cevabı size beş dakikaya mal olmaz.

**Nether ve End'de çalışmaz** — ne oralarda ev ayarlayabilirsiniz, ne de
oradan evinize dönebilirsiniz. Bir nether portalını komutla kısa devre etmek,
o boyutların oyunda var oluş sebebini silerdi.

---

## Takas (`/takas`)

Sınıf seviyeniz **20**'ye ulaştığında (ya da bir kez prestij yaptıysanız)
**sunucudaki herhangi bir oyuncuyla, aranızda ne kadar mesafe olursa olsun**
takas yapabilirsiniz. Karşı tarafın da en az **15.** sınıf seviyesinde olması
veya bir kez prestij yapmış olması gerekir.

1. `/takas <oyuncu>` ile istek gönderin. Karşı tarafın yanıtlamak için
   1 dakikası vardır.
2. Karşı taraf `/takas kabul` derse ikinizde de takas tezgahı açılır
   (`/takas ret` ile reddedebilir).
3. **Envanterinizdeki bir eşyaya tıklayın**, tezgaha konur. Tezgahtaki kendi
   eşyanıza tıklarsanız geri alırsınız. Solda sizin teklifiniz, sağda karşı
   tarafınki görünür.
4. İkiniz de **Onayla**'ya bastığında eşyalar el değiştirir.

Tezgahta bir şey her değiştiğinde **iki tarafın da onayı sıfırlanır**; yoksa
biri onaylayıp diğerinin onayını bekler, sonra da kendi mallarını geri
çekebilirdi. Pencereyi kapatmak, `/takas iptal` yazmak ya da sunucudan
düşmek takası iptal eder ve **her eşya sahibine geri döner** — teklif edilen
eşyalar takas boyunca tek bir yerde tutulur, envanterde de tezgahta da
kopyası bulunmaz.

## Koleksiyonlar ve macera günlüğü

`/koleksiyon` koleksiyonlarınızı ve her koleksiyonun içinde hangi ögeleri
topladığınızı ✔/✘ olarak gösterir. `/günlük` son on görev tamamlama ve
koleksiyon kilometre taşınızı listeler. Bir görevi tamamlamak, bir koleksiyon
ögesi edinmek ve bir koleksiyonun tamamını tamamlamak, **seçili sınıfınızın**
XP'sini kazandırır (sınıf seçili değilse ödül verilmez).

## Liderlik tabloları

`/liderlik`; Güç (önce prestij, sonra toplam beceri seviyesi sırasına göre),
prestij, tamamlanan görev sayısı, edinilen koleksiyon ögesi sayısı,
**Zenginlik Puanı** (elmas simgesiyle) ve her yapılandırılmış beceri için en
üst sıradaki oyuncuları gösteren tablolara açılır. Aynı tablolara `/menü`'nün Topluluk kategorisinden de göz atılabilir.
Sonuçlar her açılışta veritabanını sorgulamak yerine kısa, yapılandırılabilir
bir süre için önbelleğe alınır (`leaderboards.yml`).

## Unvanlar ve kozmetikler

`titles.yml` **19 ünvan**, `cosmetics.yml` **14 kozmetik** tanımlar; her birinin
tek bir kazanma koşulu vardır: seviye, prestij, belirli bir görevi/koleksiyonu
tamamlama, bir beceri seviyesine ulaşma ya da bir **Zenginlik Puanı** eşiğini
geçme. Altı becerinin her birinin kendi ustalık ünvanı ve kendi kozmetiği,
zenginliğin de kendi beş ünvanı ve üç kozmetiği vardır. `/ünvan` ve
`/kozmetik` neyi açtığınızı gösterir ve tıklayarak kuşanmanızı sağlar; aynı
kataloglara `/menü`'den de göz atılabilir. Kuşanılan bir unvan, sohbette
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

`/menü`, çerçeveli bir merkez menü açar: en üstte kendi kafanı taşıyan,
Gücünü/prestijini/sınıfını özetleyen kişisel bir başlık, altında dört kategori
— **İlerleme** (profil, **Becerilerim**, sınıf/yetenekler, prestij),
**Kabiliyetlerim**, **Görevler** (görevler, koleksiyonlar, macera günlüğü) ve
**Topluluk** (liderlik tabloları, unvanlar, kozmetikler) — her biri ne
içerdiğini anlatan bir açıklama satırıyla, ve en altta **Eklenti Hakkında**
düğmesi.

**Sınıfa özel çerçeveler:** kabiliyet listesi, yetenek ağacı ve her becerinin
detay ekranı, o ekranın ait olduğu sınıfın kendi renginde camla çerçevelenir —
Amansız Savaşçı kırmızı, Mağara Adamı mavi, Tabiat Vârisi yeşil. Böylece bir
ekranın kime ait olduğu tek bir yazı okumadan anlaşılır.

**Eklenti Hakkında** ekranı (hem `/menü`'den hem `/haliladmin`'den açılır)
sürüm bilgisini ve emeği geçenleri listeler: **Furkan Okan (fxrkqnplus)** ve
**Claude (Anthropic)**.

## Yönetici paneli

`/haliladmin` (`halilibrahimmmo.admin` izni gerektirir, varsayılan op) tamamen
GUI üzerinden çalışan bir yönetici paneli açar — sohbet argümanlı bir yönetici
komutu yoktur. Panel `/menü` ile aynı görsel dilde çerçevelenmiştir: en üstte
kendi kafanızı taşıyan, çalışan sürümü ve çevrimiçi oyuncu sayısını gösteren
bir başlık, altında ne yaptığını açıklayan birer satırla her işlem. Sunduğu
işlemler: `config.yml`/`messages.yml`/tüm oyun verisi dosyalarını yeniden
yükleme, mevcut oturum için ayrıntılı hata ayıklama kaydını açıp kapatma,
çalışan eklenti/Paper sürümünü görüntüleme, **beklemeden bir dünya bossu
çağırma** (türünü, nadirliğini ve yerini yine sistem seçer), çevrimiçi
oyuncuları gezip herhangi birinin salt okunur profil/sınıf/beceri özetini
görüntüleme, ve **kayıtlı her oyuncuyu** (çevrimdışı olanlar dahil) listeleyip
beceri/sınıf seviyesini ve XP'sini tıklamalarla ayarlama ya da oyuncunun
sınıfını zorla değiştirme (bu da, oyuncunun kendi değiştirmesi gibi, seçili
kabiliyetini kuşanmasından çıkarır). Bir seviye atlamaya yol açan her müdahale, hedef
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
Güç artık altı becerinin toplamından türetildiği için (bkz. "İlerleme
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

## Zorunlu Fabric modları (`modlar.yml`)

Sunucuya giren her oyuncunun Fabric kullandığı ve önceden belirlenmiş mod
listesinin tamamını kurduğu, giriş anında kontrol edilir. Kontrolü geçemeyen
oyuncu, hangi modun eksik olduğunu ve ne yapması gerektiğini yazan Türkçe bir
mesajla sunucudan atılır. Bunun dışında oyuncu istediği kadar ekstra mod
kullanabilir; yalnızca `yasakli-modlar` listesindekiler engellenir.

**Bu bir hile koruması değildir.** Paper, istemcinin mod listesini kendi
başına göremez; kontrol, zorunlu listedeki `hmmo-client` eşlik mod'unun
bildirdiği listeye dayanır. Değiştirilmiş bir istemci yalan söyleyebilir.
Amaç, yanlış modpack kuran iyi niyetli oyuncuyu bozuk bir deneyimden
korumaktır.

Ayarlar `modlar.yml` içindedir:

- `dogrulama.etkin` — tüm sistemi tek anahtarla kapatır (bakım/geliştirme).
- `dogrulama.zaman-asimi-saniye` — istemcinin cevap vermesi için tanınan süre.
- `dogrulama.brand-kontrolu` — Fabric olmayan istemciyi liste beklemeden atar.
- `dogrulama.azami-mod-sayisi` — bildirilebilecek azami mod sayısı. Fabric API
  tek başına ~100 alt modül olarak göründüğü için tipik bir oyuncu 150-200 mod
  bildirir; varsayılan 512'yi düşürmeyin.
- `dogrulama.bypass-izni` — varsayılan `hmmo.mod.bypass`. **Bu izni kendinize
  vermeyi unutmayın**, aksi halde siz de sunucuya giremezsiniz.
- `zorunlu-modlar` — her kayıtta mod id'si, görünen ad ve minimum sürüm.
  `minimum-versiyon` bir **alt sınırdır**: oyuncu daha yeni sürüm kurabilir.

Mod id'si jar dosyasının adı değildir; her jar'ın içindeki `fabric.mod.json`
dosyasında yazan kimliktir. Listeyi elle yazmayın — modpack her
güncellendiğinde `tools/mod-listesi-cikar.ps1` script'ini çalıştırıp çıktısını
`modlar.yml`'ye kopyalayın.

Çevrimiçi oyuncuların doğrulama durumunu `/haliladmin` panelindeki **Mod
Doğrulama** ekranından görebilirsiniz (salt okunur).

Eşlik mod'unun kaynak kodu `clientmod/` klasöründedir ve kök derlemeden
bağımsızdır; ayrıntılar için `clientmod/README.md`.

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
