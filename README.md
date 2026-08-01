# HalilIbrahimMMO

Türk Minecraft topluluğu için geliştirilmiş, özgün bir MMORPG ilerleme eklentisi
(Paper 26.2 / Minecraft 26.2). Ekonomi yoktur — tüm ilerleme seviye, beceri XP'si,
prestij, görevler, koleksiyonlar ve başarılar üzerinden işler.

Bu dosya **iki bölümden** oluşur:

- **[Bölüm 1 — Oyuncu Rehberi](#bölüm-1--oyuncu-rehberi)**: sunucuya giren bir
  oyuncunun bilmesi gereken her şey. Komutlar, beceriler, sınıflar,
  kabiliyetler, enerji, görevler, **saygınlık ve ölüm cezaları**, prestij,
  prestij ödülleri, **boss zekâsı**, ev/takas/bulut sandığı, unvanlar,
  kabiliyet HUD'u.
- **[Bölüm 2 — Teknik Bölüm](#bölüm-2--teknik-bölüm)**: sunucu yöneticisi ve
  geliştirici için. Yönetici paneli, kurulum, derleme, veritabanı,
  yapılandırma dosyaları, **moderasyon kaydı**, entegrasyonlar, test.

Oyunun içinde de aynı anlatımın kısa hâli vardır: **`/yardım`** (ya da
`/bilgi`, `/rehber`) sunucunun kendi el kitabını açar.

Tüm mimari kararların iç kaydı için `PROJECT_MEMORY.md` dosyasına bakın; her
fazdan sonra güncel tutulur ve bu proje üzerinde yeni bir işe başlamadan önce
okunmalıdır. (Bu dosya depoya dâhil değildir, yerel çalışma kopyasında durur.)

---

# Bölüm 1 — Oyuncu Rehberi

## Her şey menüden: komut listesi

Neredeyse her komut yalnızca ilgili GUI ekranını açan bir kısayoldur; sohbete
liste basan, sohbetten değer değiştiren hiçbir komut yoktur. Tek istisna
**`/ev`, `/takas` ve `/profil <oyuncu>`**: biri eve verilecek ismi, diğerleri
bir oyuncu adını ister — üçü de bir menünün soramayacağı serbest metindir.
(`/harita` da bir ekran açmaz, ama argüman da almaz: tek yaptığı adresi
söylemektir.)

Komutlar **gerçek Türkçe harflerle** yazılır. Türkçe karakter içeren her
komutun eski ASCII yazımı da **takma ad (alias)** olarak çalışmaya devam eder,
yani `/menü` ve `/menu` aynı ekranı açar — Türkçe klavyesi olmayan bir oyuncu
da her ekrana ulaşabilir.

| Komut | Takma ad | Ne açar |
|---|---|---|
| `/menü` | `/menu` | Ana menü (dört kategori + kişisel başlık + Rehber + Eklenti Hakkında) |
| `/yardım` | `/yardim`, `/bilgi`, `/rehber` | **Oyun içi rehber**: sınıflar, beceriler, kabiliyetler, enerji, odak ağacı, görevler, saygınlık, prestij, bosslar, komutlar |
| `/çanta` | `/canta`, `/backpack` | **Bulut sandığı** — ölünce kaybolmayan kişisel depo (30. sınıf seviyesi ya da prestij) |
| `/mmo` | — | MMO sistemini ilk kez aktifleştirir, ardından sınıf tanıtım ekranını açar |
| `/profil` | — | Kendi profil ekranınız (Güç, prestij, sınıf) |
| `/profil <oyuncu>` | — | **Başka bir oyuncunun** tam profili + takas daveti düğmesi |
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
| `/harita` | `/map` | Canlı oyun içi haritanın adresini verir (tıklanabilir) |
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

---

## Beceriler

**Altı** bağımsız olarak seviyelenen beceri vardır. Hepsi `skills.yml`
içindeki aynı açık 1–100 seviye eğrisini kullanır, ama her biri farklı bir
eylemden beslenir ve her blok/yaratık/iş **oyun içindeki gerçek değerine
göre** XP verir.

| Beceri | Simge | Nasıl kazanılır | En az → en çok XP |
|---|---|---|---|
| **Kazıcılık** (`/kazıcılık`) | Demir kürek | Toprak, kum, çakıl, kil gibi yumuşak blokları kazmak | 30 (toprak) → 90 (kil) |
| **Odunculuk** (`/odunculuk`) | Demir balta | Kütük kesmek | 45 (meşe) → 105 (nether sapları) |
| **Madencilik** (`/madencilik`) | Demir kazma | Cevher kırmak | 12 (kömür) → 300 (kadim kalıntı) |
| **Avcılık** (`/avcılık`) | Demir kılıç | Yaratık öldürmek | 15 (zararsız hayvanlar) → 2400 (Ejderha) |
| **Tarımcılık** (`/tarımcılık`) | Buğday | Tarlada yapılan her iş (aşağıda) | 15 (çapalama) → 150 (köylü ticareti) |
| **Hayvancılık** (`/hayvancılık`) | Kayış | Hayvan yetiştirmenin her adımı (aşağıda) | 15 (süt sağma) → 225 (evcilleştirme) |

**Dört becerinin XP'si beş katına çıkarıldı.** Kazıcılık, odunculuk, tarımcılık
ve hayvancılık, tek bir eylemin çok az XP vermesi yüzünden diğer ikisine göre
belirgin şekilde yavaş seviye atlıyordu; bu dördünün bütün blok ve eylem
değerleri 5 ile çarpıldı. Madencilik ve avcılık kasıtlı olarak dokunulmadan
bırakıldı — onlar zaten değerli cevher ve zorlu yaratıklardan büyük ödüller
alıyor. **Görev ödülleri bu artışın dışındadır**; onların kendi dengesi vardır.

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
| Toprağa çapa ile vurup tarla açmak | 15 |
| Ekinlere kemik tozu kullanmak | 30 |
| Saman balyası kırmak (yalnızca köylerdeki doğal balyalar) | 30 |
| Olgunlaşmış ekin biçmek (buğday, havuç, patates, pancar, böğürtlen) | 45 |
| Ocakta patates veya deniz yosunu pişirmek | 45 |
| Ekmek üretmek | 60 |
| Karpuz, balkabağı veya kakao kesmek | 60 |
| Nether mantarı toplamak | 75 |
| Meşale çiçeği / testi bitkisi biçmek | 90 |
| Köylülerle ticaret yapmak | 150 |

### Hayvancılık

Hayvancılık, hayvanı **yaşatmakla** ilgilidir; hayvan öldürmek her zaman
avcılığa yazılır, hayvancılığa değil. Hiçbir blok veya yaratık kaynağı yoktur,
tamamen şu eylemlerden beslenir:

| Nasıl | XP |
|---|---|
| Kova ile inek/mooshroom/keçi sağmak | 15 |
| Kayışla inek, domuz veya koyun taşımak (her 20 saniyede bir) | 30 |
| Koyun kırpmak | 45 |
| Ocakta hayvan eti pişirmek (biftek, pirzola, kuzu, tavşan, tavuk) | 60 |
| İki hayvanı çiftleştirip yavru elde etmek | 120 |
| Pasta üretmek | 150 |
| Kurt, at, eşek, katır, lama, kedi, papağan evcilleştirmek | 225 |

Kayışla taşıma ödülü **sürünün büyüklüğünden bağımsızdır**: on ineği götürmek
bir ineği götürmekle aynı iştir, hayvan başına ödemek yalnızca sürü
yığmayı ödüllendirirdi.

### Kilometre taşları

Bir beceri 25/50/75/100. seviyeye ulaştığında **kalıcı** bir bonus kazandırır.
Kademeler zayıftan güçlüye sıralanmıştır: 25. seviye basit bir giriş bonusu,
becerinin asıl güçlü bonusu her zaman 75. ve 100. seviyelerde.

**Her beceri kendi işine yarayan bir şey kazandırır.** Ekstra can artık yalnızca
avcılıkta vardır — dövüşün içinde ayakta kalmak avcılığın kendi konusudur.
Diğer beş beceri, o beceriyi oynarken hissedeceğiniz bir şey verir.

| Beceri | 25 | 50 | 75 | 100 |
|---|---|---|---|---|
| Kazıcılık | %10 çift düşme | +15 enerji | **Geniş kazı** | %30 çift düşme |
| Odunculuk | **Ağaç devirme (48)** | %12 çift düşme | **Ağaç devirme (96)** | %30 çift düşme |
| Madencilik | %8 çift düşme | Acele I | %20 çift düşme | Acele II |
| Avcılık | +1 kalp | Yaratıklara +%5 hasar | +3 kalp | Yaratıklara +%20 hasar |
| Tarımcılık | %8 çift düşme | **%50 otomatik ekim** | %20 çift düşme | **%100 otomatik ekim** |
| Hayvancılık | **%15 yem iadesi** | **%10 ikiz doğum** | %45 yem iadesi | %30 ikiz doğum |

**Can, kalp olarak yazılır.** Bir kalp iki can puanıdır; ekranda gördüğünüz sayı
doğrudan HUD'unuzdaki kalp sayısıdır. (Yapılandırma dosyaları hâlâ can puanı
kullanır, çeviriyi eklenti yapar.)

**Çift düşme yalnızca o becerinin kendi bloklarında çalışır.** Maksimum
odunculuk kütükleri ikiye katlar, elmas cevherini değil; her beceri kendi
listesini besler.

Aynı özelliğin iki kademesi (ağaç devirme 48/96, ikiz doğum %10/%30) **toplanmaz,
yükseği geçerlidir** — bunlar bir yeteneğin iki gücüdür, iki ayrı bonus değil.

Bazı kilometre taşları bir istatistik değil, **açılan bir özelliktir**:

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
  Bir binanın kütüğünü kırdığınızda hiçbir şey olmaz ve **size hiçbir şey de
  söylenmez** — kütük kırmak zaten gün boyu yaptığınız bir şey, sizi bunun için
  uyarmanın bir anlamı yok.

  **Kendi diktiğiniz ağaçlarda da çalışır.** Fidan siz koyduğunuz için
  "oyuncunun koyduğu blok" olarak kaydediliyor, ve fidan büyüdüğünde kök kütük
  tam o noktada duruyordu — yani ektiğiniz her ağaç, gövdesinin geri kalanı XP
  verse bile devrilmeyi reddediyordu. Artık bir fidan ağaca dönüştüğü anda o
  kayıt siliniyor: büyüttüğünüz ağaç, vahşi bir ağaçtan hiçbir şekilde ayırt
  edilmiyor. (Aynısı mantar ve bambu için de geçerlidir.) Ağaç büyütmek zaten
  gerçek bir fidan, gerçek zaman ve gerçek alan harcar; kısır döngü değildir.
- **Geniş kazı** (kazıcılık 75) — **kürekle** kazıcılığa ait bir bloğu
  kırdığınızda çevresindeki 3x3 alandaki uygun bloklar da (en fazla 8) birden
  düşer. Kürek işinin tamamı bu tek özellikte toplanır — ve **yalnızca kürekle
  çalışır**: kazıcılık küreğin işidir, ve elinizde kazma varken tek blokluk bir
  yol açarken sekiz blok daha almak bir ödülün tam tersidir.
- **Otomatik ekim** (tarımcılık 50 ve 100) — hasat ettiğiniz **tam olgun** ekin,
  ihtimale göre kendini yerine yeniden eker (50. seviyede yarı yarıya, 100.
  seviyede her zaman). Tohum harcamaz: elle yeniden ekmek zaten tohum bakımından
  nötrdür, kilometre taşının bütün amacı o tıklamayı ortadan kaldırmaktır.
  Olmamış bir ekin bu yola hiç ulaşmaz, dolayısıyla kır-ek döngüsü kurulamaz.
- **Yem iadesi** (hayvancılık 25/75) — bir yavru doğduktan sonra belirli bir
  ihtimalle çiftleştirme yeminin **iki adedi birden** (iki ebeveyn için
  harcadığınız kadarı) elinize geri döner. Zar yavru doğduktan *sonra* atılır,
  yani şansınız tutmasa da zaten harcayacağınızdan fazlasını kaybetmezsiniz.
- **İkiz doğum** (hayvancılık 50/100) — bir çiftleştirme belirli bir ihtimalle
  **ikinci bir yavru** daha verir. İkiz, doğan yavrunun tıpatıp aynısı olarak
  gelir (mooshroom mooshroom kalır) ve hiçbir büyüme avantajı taşımaz: fazladan
  bir hayvandır, büyüme süresinin kısayolu değil.

Bu bonuslar **ölünce kaybolmaz** — yeniden doğduğunuzda otomatik olarak geri
yüklenir. Yalnızca prestij ile o beceri sıfırlandığında düşerler. Her becerinin
detay ekranı (`/madencilik` vb.) mevcut seviye/XP'yi, becerinin beslediği
sınıfı, kilometre taşı açıklamalarını ve o becerinin liderlik tablosunu
gösterir.

### Kilometre taşlarını kapatıp açmak

**Bir şey *yapan* kilometre taşları istediğiniz zaman kapatılabilir**: ağaç
devirme, geniş kazı, otomatik ekim, çift düşme, yem iadesi ve ikiz doğum. Her
birinin bazen tam olarak istenmeyen şey olduğu bir an vardır — tek bir fidan
keserken bütün ağacın devrilmesi, tek blokluk bir yol açarken 3x3'ün gitmesi,
tarlayı başka bir şeye çevirirken ekinin kendini geri ekmesi. Kazandığınız bir
şey, sıradan işi yapamamak anlamına gelmemeli.

Beceri detay ekranındaki **Kilometre Taşları** sayfasından, açılmış her böyle
kademeye tıklayarak kapatıp geri açabilirsiniz. Kapalıyken kademe gri görünür.
Tercihiniz **kaydedilir**: sunucu yeniden başlasa da unutulmaz.

İstatistik bonuslarında (can, hasar, enerji, Acele, Hız) böyle bir düğme
yoktur — kimse daha az can istemez, ve hiç kullanılmayacak bir düğme ekranı
kalabalıklaştırmaktan başka bir işe yaramaz.

---

## Sınıflar

**Üç** sınıf vardır. Her sınıfın bir veya birden fazla **sinerji becerisi**
vardır: o becerilerden kazanılan XP'nin bir kısmı otomatik olarak sınıfın
kendi seviye izine de yansır ve ayrıca **+%20 beceri XP bonusu** kazandırır,
dolayısıyla sınıfınıza uygun oynamak hem beceriyi hem sınıfı birlikte büyütür.

| Sınıf | Sinerji becerileri | Simge | Kabiliyet sayısı |
|---|---|---|---|
| **Amansız Savaşçı** (`avci`) | Avcılık | Elmas kılıç | 4 |
| **Mağara Adamı** (`madenci`) | Madencilik | Elmas kazma | 4 |
| **Tabiat Vârisi** (`tabiat-varisi`) | Odunculuk, Kazıcılık, Tarımcılık, Hayvancılık | Demir balta | 4 |

Sınıf seçim ekranı (`/mmo` ile ilk kez, `/sınıf` ile sonradan) her sınıfı kendi
renginde bir kaide üstünde gösterir; altında hangi becerilerden ekstra XP
aldığı, prestijde biriken kalıcı bonusu ve dört kabiliyetinin hangi seviyede
açıldığı yazar.

**Her sınıfın tam olarak dört kabiliyeti vardır** ve hepsi aynı basamaklarda
açılır: **sınıf seviyesi 10 → 20 → 35 → 50**. Basamak yükseldikçe kabiliyet
güçlenir ve bekleme süresi uzar; 50. seviyedeki kabiliyet sınıfın "son sözü"dür
— en güçlü etki, en uzun bekleme (5–10 dakika).

**Tabiat Vârisi** tek başına dört beceriyi birden besler — toprağın, ormanın,
tarlanın ve sürünün vârisidir. Kabiliyetleri de tam olarak bu dört becerinin
her birine bir tane düşer. Amansız Savaşçı ve Mağara Adamı ise dar ama derin
uzmanlardır.

Her sınıf ayrıca prestij yaptıkça büyüyen, kendine özgü kalıcı bir bonus
taşır: Amansız Savaşçı PvE hasarı, Mağara Adamı maksimum enerji, Tabiat Vârisi
maksimum can.

### Yetenek ağacı: iki ayrı puan havuzu

Seçili sınıfın altında arttırılabilir yetenekler bulunur ve bunlar **birbirinin
yerine geçmeyen iki ayrı puan havuzundan** harcanır:

| Havuz | Ne alır | Nasıl kazanılır |
|---|---|---|
| **Özellik Puanı** | Kalıcı karakter istatistikleri (can, yaratıklara karşı hasar, maksimum enerji) **ve bulut sandığının genişletmeleri** | Her **30** Güç seviyesinde 1, her prestijde 4 |
| **Kabiliyet Puanı** | Tek bir kabiliyete özel iyileştirmeler | Her **60** Güç seviyesinde 1, her prestijde 2 |

**Ekonominin tek bir kuralı vardır ve her iki kaynakta da aynıdır: özellik
puanı, kabiliyet puanının tam iki katı hızda gelir.** 30'a karşı 60 Güç, 4'e
karşı 2 prestij. "Kabiliyet puanı daha nadirdir" cümlesinin tek bir sayısı
olmalı, iki kaynakta iki farklı oranı değil.

**Kazanımı bir tık zorlaştırıldı** (eskiden 25/40'tı): altı becerisi de 100
olan, hiç prestij yapmamış bir oyuncu artık 24 özellik + 15 kabiliyet değil,
**20 özellik + 10 kabiliyet** puanına sahiptir. Üstüne bulut sandığının iki
genişletmesi de özellik puanından ödenir.

**Odak ağacındaki puan sayaçlarının üstüne geldiğinizde nasıl kazanıldıkları
yazar.** Oradaki sayılar `classes.yml`'den okunur, yani sunucu ekonomiyi
değiştirdiğinde ekranın söylediği de aynı düzenlemede değişir.

Bir kabiliyet puanı asla bir özellik yeteneğine, bir özellik puanı da asla bir
kabiliyet yeteneğine harcanamaz. Bölünmenin bütün amacı budur: kabiliyetlerini
güçlendirmek isteyen bir oyuncu bunu ham istatistiklerinden vazgeçerek satın
alamaz, tersi de geçerlidir. **Kabiliyet puanı bilerek daha nadirdir.**

Kabiliyet yetenekleri bir kabiliyetin **enerji maliyetini azaltabilir**,
**bekleme süresini kısaltabilir**, **etki sınırını genişletebilir**, **etki
süresini uzatabilir** veya **etkisinin gücünü arttırabilir** (bir hasar
çarpanını, bir düşme ihtimalini ya da ham hasarı).

**Menüdeki açıklamalar mutlak değer konuşur, yüzde değil.** Bir yeteneğin
üstüne geldiğinizde tek satırda **üç sayı** görürsünüz: rank başına ne
değiştiği, kabiliyetin varsayılanı ve **sizin şu anki değeriniz**.

| Eskiden | Şimdi |
|---|---|
| "Bekleme süresini %50 azaltır" | "Her rank **-15 saniye** kısaltır *(varsayılan 2 dk, şu an 1 dk 30 sn)*" |
| "Kabiliyet etkisini %75 arttırır" | "Her rank **+%5** arttırır *(varsayılan %15, şu an %25)*" |
| "Kabiliyet etkisini %100 arttırır" | "Her rank kazmana **+1 Verimlilik** ekler *(varsayılan +3, şu an +5)*" |

Sebebi basit: "%50 azaltır" cümlesi, temel değeri bilmeyen birine hiçbir şey
söylemez ve küçük bir sayının yüzdesi ("üç seviyelik bir büyüde %100") olduğundan
çok daha büyük okunur. Yapılandırma dosyası hâlâ oran saklar — kabiliyetin temel
değeri ileride değişirse yetenek onunla birlikte ölçeklensin diye — ama ekranda
gördüğünüz artık gerçek sayıdır. Birim de kabiliyete göre değişir: Verimlilik
seviyesi, Hız seviyesi, hayvan sayısı, blok sınırı ya da blok yarıçapı.

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
kullanılamaz. Açılma koşulu **sınıf seviyesidir** (beceri seviyesi değil), ve
her sınıfta **dört kabiliyet** aynı dört basamakta açılır: **10 → 20 → 35 →
50**. Sıra ilerledikçe etki büyür, bekleme süresi uzar.

| Sınıf | Kabiliyet | Sv. | Alet | Enerji | Bekleme | Etki |
|---|---|---|---|---|---|---|
| Tabiat Vârisi | **Filiz Patlaması** | 10 | Çapa | 25 | **7 dk 30 sn** | 7 blok yarıçapındaki ekinler dalga dalga olgunlaşır, bitince kendiliğinden kapanır |
| Tabiat Vârisi | **Sürü Çağrısı** | 20 | Herhangi | 30 | 4 dk | 10 blok yarıçapındaki tüm yetişkin hayvanları yem harcamadan çiftleşme moduna sokar |
| Tabiat Vârisi | **Doğa Ana'nın Koruyucuları** | 35 | Herhangi | 20 + kalp başına 5 | 4 dk | En yakın 6 hayvan sana saldıran yaratığın üstüne koşar; yaralananları enerjinle iyileştirirsin (10 sn) |
| Tabiat Vârisi | **Asil Kurt** | 50 | Herhangi | 40 | **10 dk** | **Yalnızca geceleri.** 1 dakika kurda dönüşürsün: zırhsız, hızlı, dayanıklı ve güçlü; canavarlar kaçar, kurtlar seninle avlanır |
| Amansız Savaşçı | **B Planı** | 10 | Kılıç/Balta | 15 | 2 dk | 5 saniye boyunca hiçbir yaratık seni göremez + Görünmezlik + Hız 2 |
| Amansız Savaşçı | Avcı Odağı | 20 | Kılıç/Balta | 25 | 2 dk 30 sn | PvE hasarı %50 artar (vuruş başına enerji tüketir) |
| Amansız Savaşçı | Yıkıcı Darbe | 35 | Kılıç/Balta | 30 | **30 sn** | Ayağının altında şok dalgası, 5 blok yarıçapı, 11 ham hasar |
| Amansız Savaşçı | **Kan Yemini** | 50 | Kılıç/Balta | 40 | **7 dk 30 sn** | 45 saniye boyunca hasarın %15'i cana döner + Hız 1 |
| Mağara Adamı | **Bulldozer** | 10 | Kazma | **0** + blok başına 3 | 2 dk 30 sn | **1 dakika boyunca** kazdığın her blok 3x3x3 küple birlikte düşer |
| Mağara Adamı | **Madenci Refleksleri** | 20 | Kazma | 30 | 2 dk | 10 saniye boyunca kazmana **+3 Verimlilik** ekler |
| Mağara Adamı | **Kaya Patlaması** | 35 | Kazma | 35 | 5 dk | **Maden Bombası** bırakır: normal TNT'den 6 kat güçlü patlar, cevhere ve cana dokunmaz |
| Mağara Adamı | **Maden Feneri** | 50 | Kazma | 30 | **10 dk** | 36 blok yarıçapındaki cevherleri 40 sn parlatır |

**Tabiat Vârisi'nin sırası yeniden düzenlendi ve Ağaç Kırıcı kabiliyeti
kaldırıldı.** Sebebi basitti: odunculuğun 25. kilometre taşı zaten **kalıcı**
ağaç devirme veriyor, ve aynı işi 10. basamakta bir kabiliyet olarak da vermek
sınıfın ilk kabiliyetini "zaten sahip olduğun şey" hâline getiriyordu. Kilometre
taşı ağaç devirmesi hiçbir şekilde değişmedi — hâlâ 25. seviyede açılır ve hâlâ
çalışır. Yerine gelen **Asil Kurt** sınıfın son sözü oldu.

Yeni sıra tarımcılık (10) → hayvancılık (20) → kazıcılık (35) → odunculuk (50).

**50. seviyedeki kabiliyet sınıfın son sözüdür**: en güçlü etki, en uzun
bekleme. On dakikada bir kullanılan bir şeyin gerçekten bir şey yapması
gerekiyordu.

**Amansız Savaşçı'nın dört kabiliyeti de kılıçla DA baltayla DA çalışır.**
Balta bu oyunda meşru bir savaş silahıdır — daha yavaş, daha sert vuran — ve
bir avcı odağının ya da bir kan yemininin bıçağın şekliyle hiçbir ilgisi yok.

### Nasıl kullanılır

1. `/kabiliyet` menüsünden bir kabiliyet **kuşanın**. Ekran dört kabiliyeti
   **eşit aralıklı dört kaide** hâlinde gösterir ve her kaidenin altında o
   kademenin numarası ile gerekli seviye yazar (ulaştığınız kademeler yeşil).
   Böylece ekran bir liste değil, sınıfınızın ilerleme çizgisidir. Her
   kabiliyetin konusuna uygun kendi simgesi vardır; **kuşanılı olan büyülü
   (parlayan)** görünür, seçildiğinde kısa bir ses efekti çalar ve sohbete onay
   mesajı gelir. Menü, sınıfınızın kendi renginde çerçevelenir.

   **Ekrandaki her sayı sizin sayınızdır.** Bekleme süresi, etki süresi, enerji
   maliyeti, blok başına enerji, blok/yarıçap sınırı ve kabiliyetin kendi etkisi
   (can dönüş oranı, Verimlilik seviyesi, çağrılan hayvan sayısı…) **odak
   ağacında harcadığınız rankler işlenmiş hâlde** yazar; herhangi biri
   varsayılandan farklıysa "Odak ağacıyla güçlendirildi" satırı çıkar. Eskiden
   burada her zaman varsayılan değerler yazıyordu — yani kabiliyetlerinize
   baktığınız tek ekran, harcadığınız puanların görünmediği tek ekrandı.

   Aynı ekranın altında **"Kabiliyeti Çıkar"** düğmesi vardır: kuşandığınız
   kabiliyeti bırakır ve o an çalışan etkisini sonlandırır. Bekleme süreleri
   sıfırlanmaz — ne kabiliyetin kendi bekleme süresi, ne de farklı bir
   kabiliyete geçmek için beklenen süre. Çıkarıp takmak bir kısayol değildir.
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

**Blok koymak da kabiliyeti tetiklemez.** Elinizde bir blok varken bir bloğa
sağ tıklamak "blok koyuyorum" demektir, "kabiliyetimi aç" değil — aynısı kova,
kayık, vagon, hayvan yumurtası, kemik tozu, çakmak taşı, kayış, tablo ve
zırh askısı gibi kendi işlevi olan eşyalar için de geçerlidir. Bu özellikle
**aleti "herhangi bir şey" olan kabiliyetler** için önemliydi (ör. Sürü
Çağrısı): eldeki eşya onları elemediği için bir duvar örmek kabiliyeti iki
blokta bir harcıyordu.

Burada shift **kaçış yolu değildir**, çünkü sandığa ya da kapıya blok dayamanın
yolu zaten shift'tir — onu muaf tutmak açığı tam da en çok vurduğu yerde geri
getirirdi. Elinizde blok varken kabiliyetinizi açmak isterseniz **boş havaya**
sağ tıklayın; orada hiçbir şey yerleştirilmediği için kabiliyet normal çalışır.
**Boş elle** bir bloğa tıklamak da her zaman çalışır.

**Zaten çalışan bir kabiliyete tekrar tıklamak** (yaratıcı mod dışında)
hiçbir mesaj üretmez; eylem çubuğundaki geri sayım kabiliyetin çalıştığını
zaten söylüyor. **Bekleme süresi dolmamış bir kabiliyete tıklamak da onay
istemi çıkarmaz**: doğrudan eylem çubuğunda kalan süre yazar, sohbete hiçbir
şey gitmez. İkisinde de eskiden önce "tekrar sağ tıkla" yazıp sonra ikinci
tıklamayı reddediyordu — yani sizden bir reddi onaylamanız isteniyordu.

**Yaratıcı (Creative) modda hiçbir sınır yoktur:** kabiliyet değiştirme
beklemesi, kabiliyet bekleme süresi, enerji maliyeti ve süre sınırı
uygulanmaz. Kabiliyet siz kapatana kadar açık kalır — açarken olduğu gibi
**çift sağ tıklayarak kapatırsınız**. Eylem çubuğunda geri sayım yerine
"Yaratıcı mod — kapatmak için çift sağ tıkla" yazar.

Bunun **tek istisnası, işini anında bitiren kabiliyetlerdir**: Yıkıcı Darbe,
Filiz Patlaması, Sürü Çağrısı ve Kaya Patlaması. Şok dalgası zaten atıldıktan,
ekinler zaten olgunlaştıktan, bomba zaten bırakıldıktan sonra süreyi açık
tutmanın hiçbir anlamı yok — üstelik kabiliyeti siz kapatana kadar tekrar
kullanılamaz hâle getiriyordu. Bu dördü yaratıcı modda da kendi normal
sürelerini kullanır (Yıkıcı Darbe için 1 saniye), yani çift sağ tıkladıkça
tekrar tekrar kullanılabilir.

Alet **yalnızca kabiliyeti başlatmak** için gereklidir: kabiliyet bir kez
etkinleştikten sonra elindeki eşyayı değiştirmen, hatta düşürmen onu bozmaz.
Etkin bir kabiliyet varken eylem çubuğunda kabiliyetin adı, kalan süresi ve
**mevcut/maksimum enerjiniz** (ör. `Enerji 70/100`) sürekli gösterilir.

Her kabiliyetin `abilities.yml` içinde kendine ait bir rengi, etkinleştirme
sesi, **yankı sesi** (dört tick sonra biraz daha tiz çalan ikinci vuruş) ve
partikül gösterisi vardır. Gösterinin **şekli** partikül türünden ayrı bir
ayardır: `PATLAMA` (saçılma), `HALKA` (genişleyen halkalar), `SARMAL` (yukarı
sarılan helis) ya da `KUBBE` (baş üstünde kubbe). Aynı partikül sarmalda kılıç
savurması, halkada şok dalgası okunur — iki kabiliyet aynı partikülü paylaşsa
bile birbirine benzemez.

Farklı bir kabiliyete geçmek kısa bir bekleme süresine tabidir (varsayılan
2 dakika); aynı kabiliyeti yeniden seçmek serbesttir. Henüz açılmamış
kabiliyetler menüde kilitli gösterilir.

**B Planı** (Amansız Savaşçı 10) 5 saniye boyunca sizi bütün yaratıklara karşı
görünmez yapar — ve bu, görünmezlik iksirinden başka bir şeydir: size **zaten
nişan almış** olan her yaratık hedefini kaybeder ve süre boyunca sizi yeniden
bulamaz. Aynı süre boyunca Görünmezlik ve Hız 2 alırsınız. Kaybedilmiş bir
dövüşten çıkmanın yoludur, kazanmanın değil.

**Kan Yemini** (Amansız Savaşçı 50) 45 saniye boyunca bir yaratığa verdiğiniz
hasarın **%15'ini** cana çevirir ve size **Hız 1** verir. Yemin altındayken
**ekranınızın kenarları gerçekten kızarır** ve **çevrenizdeki düşman yaratıklar
kırmızı parlar**; ölen her yaratığın ruhu da size doğru akar (kendi sesiyle
birlikte). Avcı Odağı bir vuruşun ne kadar sert olduğunu değiştirir, Kan Yemini
ise bir avın ne kadar sürebileceğini. Canınızın üstüne çıkamaz ve yalnızca
yaratıklara karşı çalışır.

Kenar kızarması ve mob parlaması **eşlik mod'unun çizdiği şeylerdir** (bkz.
"Kabiliyet HUD'u"). Daha önce bunun yerine kameranızın önüne kırmızı parçacıklar
saçılıyordu; oyunda ürettiği şey ekran kenarında bir kızarma değil, arkanızda
kırmızı bir izdi — yani anlatmak istediğinin tam tersi.

Odak ağacından can dönüşümünü (**+%5**), Hız seviyesini (**+1**) ve bekleme
süresini (**-30 sn**) rank rank güçlendirebilirsiniz.

#### Çılgınlık: nabız, kalp atışı ve kombo merdiveni

Kan Yemini ve **Asil Kurt** aynı sunumu paylaşır, çünkü ikisi de aynı şeyin iki
anlatımıdır: biri fazla ileri gitmiş bir avcı, diğeri avlayan şeyin kendisi.

- **Ekranın kenarı nabız gibi atar.** Sabit bir kızarma değil, saniyede bir
  büyüyüp küçülen bir soluk alıştır.
- **Kalp atışı sesi** yalnızca size gelir — yanınızda savaşan arkadaşınız
  duymaz. Bu sizin bedeninizde olan bir şey.
- **Yaklaşan her düşman kırmızı parlar** ve yakındakiler uzaktakilerden daha
  parlak yanar. (Oyunun bir sınırı burada devreye giriyor: Minecraft'ın parlama
  çizgisi rengini yalnızca on altı sohbet renginden biri olarak alabiliyor,
  yani gerçek bir geçiş yapılamıyor. Uzak için koyu, yakın için parlak kırmızı
  kullanılıyor; "yaklaştıkça artan" hissinin geri kalanını düzgün ölçeklenen
  ekran tinti taşıyor.)
- **Kombo merdiveni**: ekranın altında, kabiliyet açıldığından beri verdiğiniz
  **toplam hasara** göre kızışan bir satır belirir. Öldürdüğünüz yaratık
  sayısına göre değil — bir tavukla bir ravager aynı başarı değildir. Altı
  basamak vardır ve son ikisi ekranda **titrer**:

  > Güzel! → Devam et. → Durmayı aklından bile geçirme. → HIZINI KESME! →
  > AKLINI MI KAÇIRDIN?! → HEPSİNİ ÖLDÜR!?

- **Kabiliyet bittikçe bütün bu efektler yavaşça söner** ve son saniyede
  tamamen kaybolur, böylece süre dolmadan önce dolmak üzere olduğunu bir sayıya
  bakmadan anlarsınız. Merdiven her aktivasyonda sıfırdan başlar.

**Bulldozer** (Mağara Adamı 10) **sürekli** bir kabiliyettir: açtıktan sonra
1 dakika boyunca kazdığınız **her blok**, merkezinde durduğu 3x3x3 küple
birlikte düşer. Cevher olması gerekmez — bir tünel dünyanın koyduğu her şeyin
içinden geçer. Her blok 3 enerji ve kazmanızdan 1 dayanıklılık götürür;
**enerjiniz biterse kabiliyet orada kapanır**. Bedrock, spawner, ganimet
sandığı ve envanteri olan hiçbir bloğa dokunmaz.

**Açmak artık bedavadır** (eskiden 25 enerjiydi). Bulldozer'ın gerçek fiyatı
zaten blok başına ödediğiniz enerji; üstüne bir de açılış bedeli almak, tünele
girmeden barın dörtte birini götürüyordu.

**Madenci Refleksleri** (Mağara Adamı 20) 10 saniye boyunca elinizdeki kazmaya
**+3 Verimlilik** ekler. Haste iksiri değil, gerçek büyüdür ve mevcut
büyünün **üstüne biner**: Verimlilik 3 kazma Verimlilik 6, Verimlilik 5 kazma
Verimlilik 8 olur. Süre dolduğunda kazmanız tam olduğu hâline döner — kazmayı
başka bir slota taşısanız, düşürseniz, hatta sunucu yeniden başlasa bile.

**Kaya Patlaması** (Mağara Adamı 35) bulunduğunuz yere bir **Maden Bombası**
bırakır. Fitili yandıktan sonra **normal bir TNT gibi, parçacıklı ve gürültülü**
patlar — ama:

- **Hiçbir oyuncuya ve hiçbir yaratığa zarar vermez.** Yanında durabilirsiniz.
- **Hiçbir cevhere dokunmaz.** Kayayı temizler, cevherler havada asılı kalır.
- Ortaya çıkan cevherler **5 saniye boyunca parlar**, sonra söner.
- Kaç blok temizlendiği ve kaç cevher çıktığı size özel olarak yazılır.

**Gücü, normal bir TNT'nin altı katıdır** — yani altı katı *hacim*: vanilla bir
TNT taşta yaklaşık 4 bloklık bir çukur açar, bu da **7 blok** yarıçapında bir
oda açar. Eskiden 35 bloktu ve bir TNT patlamasına hiç benzemiyordu: koca bir
salonu bir anda yutuyordu. Kraterin kenarı bilerek tırtıklıdır; kusursuz bir
küre patlama gibi değil hata gibi okunur.

Dar bir tünelde kazarken bırakmak için tasarlanmıştır: bir anda çalışılabilir
bir oda açar ve içindeki her damarı size gösterir. Bekleme süresi 5 dakikadır;
odak ağacından bekleme süresini kısaltıp yarıçapı 11 bloğa kadar
büyütebilirsiniz.

**Doğa Ana'nın Koruyucuları** (Tabiat Vârisi 35) yalnızca **çevrenizde hayvan
varken** kullanılabilir — çağıracak kimse yoksa enerji de harcamaz, bekleme de
başlatmaz. **Aletsizdir**: elinizde ne varsa onunla açılır, çünkü bu bir savaş
kabiliyeti değil bir yardım çağrısıdır. Çalıştığında en yakın 6 hayvan (inek,
domuz, koyun, tavuk, panda, kutup ayısı, tavşan, demir golem…) size saldıran
yaratığın üstüne **koşarak** gider, ona vurur ve onu sizden uzaklaştırır.
10 saniye sürer. Amaç öldürmek değil, üstünüzden almaktır.

**Yaralanan koruyucularınızı enerjinizle ayakta tutarsınız: 5 enerji = 1 kalp.**
Koyununuzun 2 kalbi gittiyse 10 enerjiniz eksilir ve o iki kalp geri gelir;
bunu görsel ve sesli bir efekt de takip eder. Bu olmadan çağrı hiçbir işe
yaramıyordu — bir koyun yağmacının iki vuruşuna dayanıyor, koruyucular siz
farkına varmadan ölüyordu.

- **Enerjiniz biterse kabiliyet kapanmaz**, yalnızca iyileştirme durur; kabiliyet
  süresi dolunca kapanır.
- **Bütün koruyucularınız ölürse kabiliyet kendiliğinden sona erer** ve size
  söylenir.
- **Evcilleştirilmiş at, eşek, kurt ve kedi ile köylüler çağrıya katılmaz.**
  Kendi kurdunuzu yağmacının üstüne sürmek kurtarmayı yasa çevirirdi.

Odak ağacından süresini, çağırdığı hayvan sayısını ve bekleme süresini
geliştirebilirsiniz.

**Maden Feneri** özel bir durumdur: yalnızca **geniş ve üstü kapalı bir
madende** kullanılabilir (dar bir tünelde ya da açık arazide çalışmaz).
Çalıştığında 36 blok yarıçapındaki cevherler 40 saniye boyunca duvarların
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

**Filiz Patlaması** (Tabiat Vârisi 10) çevrenizdeki 7 blok yarıçapındaki bütün
olmamış ekinleri hasada hazır hâle getirir — bir kova kemik tozu gibi, ama hiç
kemik tozu harcamadan. Zaten olgunlaşmış ekinlere dokunmaz, dolayısıyla size
kaç bitkinin gerçekten büyüdüğünü söyler.

**Büyüme artık bir anda olmuyor, dalga dalga yayılıyor.** Ekinler size olan
uzaklıklarına göre sıralanır ve altı geçişte, her geçişte bir yaş kademesi
ilerleyecek şekilde büyür: yanınızdaki buğday tam boyuna varırken en dışarıdaki
daha yeni filizlenir. Yaklaşık 2,5 saniye sürer ve **son dalga bittiği anda
kabiliyet kendiliğinden kapanır** — işi bitmiş bir kabiliyeti açık tutmanın
kimseye faydası yok. Bekleme süresi **7 dakika 30 saniyedir**.

**Sürü Çağrısı** (Tabiat Vârisi 20) çevrenizdeki 10 blok yarıçapındaki bütün
**yetişkin** hayvanları tek bir yem harcamadan çiftleşme moduna sokar. Yavrular
ve zaten çiftleşme modundaki hayvanlar atlanır, dolayısıyla aynı sürüye tekrar
kullanmak bir işe yaramaz.

Kabiliyetin kendisi **2 saniye** sürer (hem normal hem yaratıcı modda) ve
kendiliğinden kapanır: çağrı gittikten sonra açık kalmasının bir işlevi yok.
Hayvanların çiftleşme penceresi bundan bağımsızdır — vanilla'daki normal
süresi neyse odur, yani sürünün eşleşmeye vakti olur. Bekleme süresi
**4 dakikadır**.

**Asil Kurt** (Tabiat Vârisi 50, sınıfın son sözü) **yalnızca geceleri**
çağrılabilir; gündüz denerseniz ne enerji ne bekleme süresi harcanır, sadece
reddedilirsiniz. Açtığınız anda **tüm sunucu** üç kez kurt sesi duyar, ve bir
dakika boyunca:

- **Zırhınız çıkar ve giyemezsiniz.** Çıkan parçalar envanterinize gider —
  yeri yoksa ayağınıza düşer, hiçbir şey silinmez. Süre boyunca zırh giymeye
  çalışmak işe yaramaz, tekrar çıkarılır.
- **Hız, Dayanıklılık ve Güç** alırsınız ve parlarsınız.
- **Hiçbir canavar size saldırmaz** — size nişan almış olanlar hedefini
  kaybeder, yenisi de sizi bulamaz.
- **Çevrenizdeki kurtlar ve köpekler** en son vurduğunuz hedefin üstüne atılır.
- **Sizi gören** inek, koyun, at, kedi, domuz, tavuk ve köylüler kaçar. Duvarın
  arkasındaki hayvan kaçmaz: görmediği bir şeyden korkmaz.
- Ekranınız **Kan Yemini gibi kızarır ve nabız gibi atar**, kalp atışı sesi
  gelir, ve aynı **kombo merdiveni** işler (aşağıya bakın).

Bekleme süresi **10 dakikadır**. Odak ağacından enerjisini, beklemesini ve
çağrının menzilini geliştirebilirsiniz.

> **Not — kurt adam modeli.** İsteğin bu tek parçası yapılamadı ve bunu açıkça
> yazmak gerekiyor: bir karakter modeli bir *dosyadır* (mesh + doku), ve ne
> sunucu vanilla bir istemcinin oyuncu modelini değiştirebilir ne de hiç görsel
> içermeyen eşlik mod'u onu üretebilir. Bunun için ya bir kaynak paketi ya da
> mod'a eklenecek gerçek bir model dosyası gerekir. Maddenin geri kalanının
> tamamı yapıldı; görsel kimlik yerine oyunun kendi verebildiği en güçlü hâl
> kullanıldı: zırhsız, parlayan, hızlı ve etrafında her şeyin kaçtığı bir
> oyuncu.

### Enerji

Enerji, kaydedilmeyen, oyuncu başına bir havuzdur (varsayılan maksimum 100;
sınıf ve kilometre taşı bonuslarıyla artar). Üç kuralı vardır ve üçü birlikte
enerjinin ne işe yaradığını belirler:

1. **Enerjiyi yalnızca kabiliyetler harcar.** Başka hiçbir şey — koşmak,
   savaşmak, blok kırmak — enerjiye dokunmaz.
2. **Bir kabiliyet açıkken enerji hiç yenilenmez.** Kabiliyet bittikten sonra
   saniyede 3 puan dolmaya başlar (boş bardan doluya yaklaşık 33 saniye).
3. **Süpürme kabiliyetleri blok başına ödeme yapar.** Bulldozer kazılan her
   blok için 3 enerji harcar. Enerjiniz biterse **kabiliyet orada kapanır** ve
   size kaç blokta durduğu söylenir.

Yani "bir seferde kaç blok kazarsın" sorusunun cevabı blok sınırı değil,
**enerji barınızdır**. 100 enerjilik bir barla Bulldozer yaklaşık 33 blok alır;
maksimum enerji bonuslarınız arttıkça bu sayı da artar.

**Bulldozer "sürekli" bir kabiliyettir**: ilk blokta tükenmez, 1 dakika açık
kalır ve o süre boyunca yaptığınız her kazı süpürmeyle gelir. **Enerji bitince
erken kapanır** — süreyi dolduran şey saat değil, barınızdır.

**Süpürme aleti yer.** Düşen her blok için elinizdeki alet 1 dayanıklılık
kaybeder — o blokları elle kırsaydınız ödeyeceğiniz bedelin aynısı. Aynı kural
kilometre taşıyla açılan ağaç devirme ve geniş kazı için de geçerlidir.
Dayanıklılık büyüsü (Unbreaking) normal şekilde çalışır.

Eylem çubuğunda enerji her zaman `mevcut/maksimum` biçiminde görünür; bir
kabiliyet çalışırken kabiliyetin kendi geri sayımının yanında okunur.

**Kabiliyet kapandıktan sonra bar dolarken ayrı bir satır görürsünüz**
("Enerji yenileniyor: 42/100 — saniyede 3"). Kabiliyet açıkken enerji hiç
yenilenmediği için, bu satır bir sonraki kullanıma ne kadar kaldığını
okuyabildiğiniz tek yerdir.

---

## Profil paneli (sidebar)

Ekranın sağında kalıcı, renklendirilmiş bir bilgi paneli. Yukarıdan aşağıya:

1. **Oyuncu adı** — varsa kuşanılan unvan kendi rengiyle isimden önce
   ("Oyuncu: [Unvan] Ad").
2. **Güç, prestij ve saygınlık** — üçü tek satırda: altın Güç, açık mor
   prestij, mavi Saygınlık Puanı.
3. **Sınıf ve sınıf seviyesi** — tek satırda, XP ilerlemesiyle birlikte.
4. **Kabiliyet** — çalışan bir kabiliyet varsa adı ve kalan süresi, yoksa
   seçili olan, o da yoksa "Kabiliyet seçilmedi".
5. **Ev / Takas / Çanta durumu** — üçünden biri açıldıktan sonra görünür;
   evinizin adını (ya da "Ayarlanmadı"), takasın ve bulut sandığının açık olup
   olmadığını tek satırda gösterir. **Bir takas sürerken** hemen altında
   "Takas açık: <oyuncu>" satırı çıkar.
6. **Başlangıç Görevleri** — yalnızca tamamlanmamış olanlar varken; hepsi
   bitince bu bölüm tamamen kaybolur.
7. **Görevler** — her tür **kendi başlığı** altında: **Günlük Görevler**
   (sarı), **Haftalık Görevler** (altın), **Kalıcı Görevler** (camgöbeği).
   Başlık altındaki satırlar da aynı renktedir, yani renk ve başlık aynı şeyi
   söyler. Tamamlanmamış görevler ilerlemeleri ve XP ödülleriyle listelenir;
   **tamamlananlar ✔ işaretiyle ve yeşil renkte** görünür — bitmiş bir görevin
   türünün renginde durmasının bir anlamı yoktu, önemli olan bitmiş olması. Görevler panelde **kısa adlarıyla** yazılır
   ("40 kütük kır", "8 enderman öldür" — bkz. Görevler bölümü).
8. En altta `/panel` hatırlatması.

**Saygınlık Puanı kısaltılarak yazılır**: 12 600 yerine `12.6K`, 1 050 000
yerine `1.05M`. Sekiz haneli bir sayıyı panelde rakam rakam saymak zorunda
kalmazsınız. Aynı kısaltma liderlik tablosunda ve sıralama duyurularında da
kullanılır.

**Minecraft bu panelde en fazla 15 satır gösterir** ve fazlasını sessizce
atar, bu yüzden kırpmayı eklentinin kendisi yapar. Satırlar bu tavana göre
bilinçli olarak birleştirilmiştir: Güç/prestij/saygınlık tek satır, sınıf ve
sınıf seviyesi tek satır. (Sınıfınızın açık kabiliyetleri de bu yüzden tek tek
listelenmez; tam listeyi `/kabiliyet` ekranı gösterir.)

Panel yine de taşarsa satırlar **taşıdıkları bilgiye göre** feda edilir:
önce ayırıcı çizgiler ve en alttaki `/panel` satırı, sonra **en alttaki görev
başlığı**, en son bir görev satırı. Yani çok görev sabitleyen bir oyuncu bir
başlığı kaybedebilir ama görevi asla — bir ayraç dururken hiçbir görev
atılmaz. **Panelde hangi görevlerin görüneceğine siz karar verirsiniz**:
bkz. "Görevleri panele sabitleme".

Panel, sunucuda gerçekleşen her değişikliği (beceri/sınıf XP kazanımı, unvan
değişikliği, yönetici müdahalesi dahil) yaklaşık 2 saniye içinde
**kendiliğinden** günceller — `/panel` yalnızca paneli mevcut oturum için
tamamen gizler/gösterir. Tercih kaydedilmez; her katılışta veya sunucu
yeniden başladığında panel varsayılan olarak görünür duruma döner.

---

## Başka bir oyuncunun profili (`/profil <isim>`)

`/profil` kendi özetinizi açar; `/profil <isim>` **başka bir oyuncunun tam
kaydını** açar. Oyuncu çevrimdışıysa da çalışır — ekranda gördüğünüz hemen her
şey kalıcı olarak saklanır.

Ekranda ne var:

- Oyuncunun **kendi kafası**, prestij sayısı ve sınıfı.
- **Güç ve sıralaması**, **Saygınlık ve sıralaması**.
- **Sınıf ve sınıf seviyesi** (XP'siyle birlikte).
- **Altı becerinin seviyesi ve her birinin sıralaması.**
- **Odak ağacı** — o oyuncunun gerçekten puan harcadığı yetenekler ve rankları.
  Hiç harcamamışsa tek satır yazar, boş bir tablo göstermez.
- **Sınıfının dört kabiliyeti**, açık olanlar yeşil, kilitli olanlar gerekli
  seviyesiyle birlikte gri.
- **Açtığı unvanlar ve kozmetikler**, sayaçlarıyla.

**Her sayının yanında sırası var, çünkü bir sayının anlamı ancak diğerlerine
göre vardır.** "Güç 312" tek başına, hiç kimsenin Güç'ünü görmemiş birine
hiçbir şey söylemez; "Güç 312 — #4" ise bir oyuncuyu aramaya değer kılan şeyin
ta kendisidir.

### Takas daveti

Ekranın altında bir **takas düğmesi** vardır ve yalnızca **gerçekten
çalışacaksa** düğmedir: iki tarafın da takası açmış olması, karşı tarafın
çevrimiçi olması ve baktığınız kişinin siz olmamanız gerekir. Diğer
durumlarda gri bir açıklama görürsünüz — ekran reddedeceği bir tıklamaya davet
etmemeli.

Tıkladığınızda karşı tarafa takas daveti gider; **iki tarafa da sohbette
bilgi** düşer ve **iki tarafa da farklı bir ses efekti** çalar (gönderene kısa
bir onay, alana dikkat çekici bir çan). Karşı taraf `/takas kabul` ya da
`/takas ret` ile yanıtlar; davet 1 dakika içinde yanıtlanmazsa düşer.

---

## TAB listesi

TAB tuşunu basılı tuttuğunuzda görünen ekran eklenti tarafından yönetilir:

- Üstte **sunucu adı** (**Halil İbrahim Sofrası**), tanıtım satırı
  (*"Beceri odaklı hayatta kalma sunucusu."*) ve **çevrimiçi / maksimum**
  oyuncu sayısı.
- Altta **canlı TPS**, **kendi ping'iniz**, sunucu adresi **`oyna.fxrkqn.org`**
  ve `/harita` hatırlatması.
- Listedeki her oyuncunun adının önünde, varsa **kuşandığı unvan kendi
  renginde** görünür — sohbette ve sağdaki panelde gördüğünüz etiketin aynısı.
  Adın yanında o oyuncunun **Güç**'ü yazar.

TPS'in rengi sunucunun durumunu söyler: **19 ve üstü yeşil**, 15-19 arası
sarı, 15'in altı kırmızı. Liste birkaç saniyede bir kendiliğinden tazelenir.
`sunucu.yml` üzerinden tamamen kapatılabilir.

---

## Uyku ve gece

**Gece tek bir oyuncunun uyuması sabahı getirir.** Yatakta vanilla'nın "geceyi
uyudu" saydığı kadar kaldığınızda saat sabaha alınır, yağmur/fırtına dinilir ve
o dünyadaki herkese kısa bir duyuru ile çan sesi gider. Yalnızca normal dünyada
çalışır (Nether ve End'de zaten gece yoktur).

Bu bir dünya ayarı (`playersSleepingPercentage`) değildir — eklenti kendi işini
yapar, dolayısıyla `sunucu.yml`'den kapatılabilir ve eklenti kaldırıldığında
dünyanızda hiçbir kalıntı bırakmaz.

---

## Canlı harita (`/harita`)

`/harita` (ya da `/map`) sunucunun canlı oyun içi harita adresini verir:
**https://harita.fxrkqn.org:8100**. Sohbetteki bağlantıya tıklayarak doğrudan
açabilirsiniz.

---

## Prestij

Prestij yapabilmek için üç şart birden sağlanmalıdır: altı becerinin de en
az 25. seviyede olması, seçili sınıfın kendi sınıf seviyesinin maksimuma
(varsayılan 50) ulaşmış olması, ve o sınıfın sinerji becerilerinin
**hepsinin** o sınıfın istediği seviyeye ulaşmış olması.

**Bu son şart sınıf başına ayarlanır.** Amansız Savaşçı ve Mağara Adamı tek bir
beceri beslediği için o beceriyi 100'e çıkarmaları istenir. **Tabiat Vârisi
dört beceri birden beslediğinden dördünün de 100 olmasını beklemek onu
diğerlerinden kat kat zor yapıyordu**; ondan **odunculuk, kazıcılık, tarımcılık
ve hayvancılığın her birinde en az 30. seviye** istenir. `/prestij` bu
şartları ayrı ayrı ✔/✘ olarak listeler — sinerji becerilerinin her biri
kendi satırında görünür, böylece dört beceriyi birden besleyen Tabiat Vârisi
oynarken hangisinin sizi geciktirdiğini tek bakışta görürsünüz — ve onay
düğmesini aynı ekranda sunar.

Prestij, seçili sınıfın sınıf seviyesini ve altı becerinin tümünü sıfırlar —
**ancak Güç düşmez**: sıfırlanmadan önceki Güç kalıcı olarak "biriktirilir" ve
becerileriniz tekrar seviye atladıkça Güç kaldığı yerden artmaya devam eder.
**Saygınlık Puanı da asla sıfırlanmaz** — puanın anlamı "dünyadan ne
çıkardın"dır ve prestij bunu değiştirmez.

Prestij ayrıca **kuşandığınız kabiliyeti çıkarır**: sınıf seviyeniz 1'e
döndüğü için o kabiliyet zaten kilitlenir, ve kuşanılı bırakmak size
kullanamayacağınız bir şeyi taşıtmak olurdu. Kabiliyet değiştirme bekleme
süreniz bilerek korunur — prestij, bir beklemeyi sıfırlamanın yolu değildir.

Her prestijin karşılığı beştir:

| Ödül | Ne kadar |
|---|---|
| **Kalıcı XP bonusu** | Rank başına **+%50** (hem beceri hem sınıf XP'sinde) |
| **Saygınlık** | Her prestijde **+1.000.000 Saygınlık Puanı** |
| **Sınıf bonusu** | Seçili sınıfa özgü kalıcı bir bonus (can / hasar / enerji) |
| **Kalıcı ödül seçimi** | Her prestijde **bir tane**, aşağıya bakın |
| **Fiziksel ödül sandığı** | Her prestijde **bir tane**, aşağıya bakın |

XP bonusu Faz 22'de **%25'ten %50'ye çıkarıldı**: prestij altı beceri seviyesini
ve bir sınıf seviyesini siliyor, yani saatlerce oyun; rank başına dörtte bir
fazla XP bunu ödül gibi hissettirecek kadar hızlı geri ödemiyordu. Prestijin
bütün amacı bir sonraki tırmanışın bir öncekinden hızlı olmasıdır.

Prestijli oyuncular liderlik tablosunda ham seviyeden bağımsız olarak her zaman
önde gösterilir. Prestij yapmak sunucu genelinde bir duyuru, **hiç kimseye zarar
vermeyen** bir havai fişek (dar bir madende patladığında sizi öldürmez artık) ve
ses efektiyle kutlanır; ekranınıza altın bir başlık gelir ve altında yeni XP
oranınız yazar — görünmeyen bir ödül ödül değildir. Varsayılan maksimum prestij
sayısı 10'dur.

### Kalıcı ödül seçimi

Prestij yaptığınız anda önünüze **kalıcı bir ödül seçme ekranı** açılır. Dört
seçenek vardır ve hepsi kalıcıdır, hepsi tekrar tekrar seçilebilir:

| Ödül | Ne verir |
|---|---|
| **Dayanıklılık** | +1 kalp maksimum can |
| **Vahşet** | Yaratıklara +%5 hasar |
| **Soluk** | +15 maksimum enerji |
| **Bilgelik** | +%10 beceri XP'si (sinerji becerisinden gelen sınıf XP'si de pay alır) |

**Seçim yapılmadan başka bir MMO ekranı açılmaz.** Ekranı kapatıp `/menü`,
`/sınıf`, `/kabiliyet`, `/liderlik`, `/madencilik` ya da başka bir komut
yazarsanız önce yine bu ekran gelir. Ödül prestijin bir parçasıdır; pencereyi
kapatıp unutmak saatlerce emeğin karşılığını çöpe atmak olurdu.

Hak **saklanmaz, hesaplanır**: "prestij sayınız eksi şimdiye kadar seçtikleriniz".
Bu yüzden sunucu menünün ortasında çökse, bir yönetici prestijinizi düzenlese ya
da art arda on kez prestij yapıp hiç bakmasanız bile hak kaybolmaz — her biri
sırayla karşınıza çıkar.

### Fiziksel ödül sandığı

Kalıcı ödülü seçtikten hemen sonra **ikinci bir ekran** açılır ve bu sefer
**elle tutulur** bir ödül seçersiniz. Bu da zorunludur.

| Sandık | İçinde ne var |
|---|---|
| **Savaş Sandığı** | 4 büyülü altın elma, 2 ölümsüzlük totemi, 2 netherite külçesi |
| **Maden Sandığı** | 6 elmas bloğu, 4 kadim kalıntı, 32 tecrübe şişesi |
| **Doğa Sandığı** | 16 kemik bloğu, 32 altın havuç, 8 kayış, 2 isim etiketi |
| **Kâşif Sandığı** | 16 ender incisi, 64 havai fişek, 8 altın elma |

Sebebi şu: kalıcı ödül sonsuza kadar büyüyen bir sayıdır ve tam olarak bu
yüzden **o anda hiçbir şey hissettirmez**. Fiziksel ödül ise hemen elinizde
olan, hemen kullanacağınız bir şeydir. Ekranlar bu sırayla açılır — önce
düşünmeyi gerektiren karar, sonra kutlama.

Envanteriniz doluysa sığmayan eşyalar ayağınıza düşer; on saatlik bir emeğin
karşılığı dolu bir hotbar yüzünden silinmez.

Sandıkların içerikleri **tamamen yapılandırılabilir** (`prestige.yml`): sunucu
sahibi yeni bir sandık ekleyebilir, birini çıkarabilir ya da içindekileri
değiştirebilir. İki kural vardır — en az iki sandık olmalı (tek seçenek seçim
değildir) ve hiçbir sandık boş olamaz.

---

## Görevler

Toplam **85 görev** vardır: 43 kalıcı, 23 günlük, 19 haftalık. `/görev`,
solunda beş sekme bulunan bir görev panosu açar. Aynı anda tek bir bölüm
gösterilir, o bölüme de 42 slot ayrılmıştır:

- **Başlangıç Görevleri** — sunucuyu tanımanız için dört adım. Panonun **ilk**
  sekmesidir ve diğer üçünü açan da budur (aşağıya bakın).
- **Günlük Görevler** — 23 görevlik havuzdan her gün rastgele atanan 3 görev.
- **Haftalık Görevler** — 19 görevlik havuzdan her hafta rastgele atanan 2 görev.
- **Kalıcı Görevler** — tek seferlik, hiç sıfırlanmayan, herkeste ortak 43 görev.
- **Tamamlanan Görevler** — bitirdikleriniz, **başlangıç adımları dâhil**.

### Başlangıç görevleri her şeyin önündedir

**Başlangıç görevlerinin dördü de bitmeden günlük, haftalık ve kalıcı görevler
açılmaz.** O sekmeler panoda görünür ama kilitlidir (tıklarsanız ne gerektiğini
söyler), sağdaki panelde hiçbir görev listelenmez ve hiçbir görev panele
sabitlenemez.

Sebebi yeni oyuncunun ilk on dakikasıdır: eskiden aynı anda hem bir öğretici hem
üç ayrı görev havuzu veriliyordu ve hangisinin kendisi için olduğunu söylemenin
hiçbir yolu yoktu. Artık tek bir liste var, ve onu bitirmek geri kalanını açıyor.

Dört adım şunlar (her biri 150 XP):

1. Ana menüyü aç (`/menü`)
2. Becerilerini incele (`/beceri`)
3. Kabiliyetlerini incele (`/kabiliyet`)
4. Bir beceri komutu kullan (`/madencilik`, `/avcılık` vb.)

Bitirdiğiniz adımlar **Tamamlanan Görevler sekmesinde durmaya devam eder**:
sunucuda yaptığınız ilk şey, biter bitmez kaydınızdan silinmemeli.

**Her becerinin üç türde de görevi vardır.** Hayvancılık ve tarımcılık uzun
süre yalnızca birkaç göreve sahipti; artık ikisinin de kalıcı, günlük ve
haftalık hedefleri var (çiftleştirme, koyun kırpma, evcilleştirme, hasat,
nether mantarı, bostan).

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

### Biten bir görevi panelden kaldırma

Tamamlanmış bir görev, dönemi bitene kadar (kalıcı görevlerde ise sonsuza kadar)
panelde yeşil bir satır olarak durur. **Görev panosunun "Tamamlanan Görevler"
sekmesinde o göreve tıklamak onu panelden kalıcı olarak kaldırır.**

Bu **tek yönlüdür**, sabitleme gibi bir aç/kapa değil: "bunu okudum" geri
alınacak bir durum değildir. Tıklamadan önce açıklama satırı bunu söyler.

Kaldırma yalnızca görev **tamamlanmış hâlde kaldığı sürece** geçerlidir:
dönemi biten bir günlük görev yeniden atandığında artık tamamlanmış olmadığı
için panele geri döner — o zaten yeni bir görevdir.

Panelde görevler **kısa adlarıyla** yazılır (`quests.yml` içindeki `kisa-ad`)
ve bu kısa adlar birer **emir cümlesidir**: "40 kütük kır", "50 cevher kaz",
"8 enderman öldür", "Bir köy keşfet". Çıplak bir isim ("40 Kütük") görevin
neyi saydığını söylüyordu ama ne yapmanız gerektiğini hiç söylemiyordu.
Scoreboard satırları alt satıra kaymaz, kesilir; bu yüzden kısa adlar en fazla
24 karakterdir.

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
- **Yapı bulma** — o yapının içinde durmanız yeterlidir. **Bilerek yalnızca
  iki tane kaldı**: köy (1 800 XP) ve batık gemi. Aşağıya bakın.
- **Eşya edinme** — hem yerden alınan hem de bir sandıktan çıkarılan eşyalar
  sayılır. Müzik diski, isim etiketi, ölümsüzlük totemi, elytra, yankı
  parçası, deneme anahtarı ve en zoru **ejderha yumurtası** (36 000 XP).
- **Belirli bir iş (`DEED`)** — sayaçla anlatılamayan, hikâyesi olan hedefler:
  bir ghast'ı **kendi ateş topuyla** öldürmek, piglinlerle takas yapmak,
  köylülerle ticaret, büyü masasında büyüleme, balık tutma, hayvan
  evcilleştirme, çiftleştirme, koyun kırpma, **gezgin tüccarı bulmak** ve
  **doğal bir raylı sandığı açmak**.

**Boyut şartı.** Bir görev `NORMAL`, `NETHER` ya da `THE_END` boyutuna
kilitlenebilir; böylece "End'de 100 enderman öldür" gerçekten End'i şart
koşar, gece bir ovada öldürdüğünüz enderman sayılmaz.

**Bir yere varmak tek başına görev değildir.** Neredeyse her keşif görevi,
oraya vardıktan sonra yapılacak bir iş ister — bir sandık açmak, bir yaratık
öldürmek, bir eşya çıkarmak:

| Görev | Ne ister |
|---|---|
| Terk Edilmiş Maden | Bir mineshaft bul, **2 doğal raylı sandık aç** |
| Batık Hazine | Hazine haritasını takip et, sandıktan **Denizin Kalbi'ni** çıkar |
| Yağmacı Karakolu | Karakolu bul, 8 yağmacı öldür |
| Ormandaki Konak | Konağı bul, 8 intikamcı öldür |
| Konağın Büyücüleri | Aynı konakta 3 evoker öldür |
| Nether Kalesi | Kaleyi bul, 12 alev çubuğu topla |
| Alev Avı / Kara Kemikler | Aynı kalede 10 blaze ve 10 wither iskeleti öldür |
| Bastion Kalıntısı | Bastion bul, 5 piglin vahşisi öldür |
| Bastion'un Altını | Aynı bastion'dan 10 altın bloğu kaz |
| Okyanus Anıtı | Anıtı bul, 64 prizmarin blok sök |
| Yaşlı Muhafız | Aynı anıtta Elder Guardian'ı öldür |
| Kadim Kalıntılar | Kalıntıları bul, fırçayla **`relic` plağını** çıkar |
| Kadim Şehir | Şehri bul, 4 yankı parçası al (18 000 XP) |
| Deneme Odaları | Odaları bul, 4 deneme anahtarı kazan |
| Son Kapı | Kaleyi bul, 20 gümüş balığı ez |
| Kendi Ateşiyle | Bir ghast'ı geri yolladığın ateş topuyla öldür |
| End'in Sakinleri | End'de toplam 100 enderman öldür |
| Derinin Bekçisi | Warden'ı uyandır ve öldür (45 000 XP) |

Geriye kalan üç "sadece bul" görevi bilerek bırakıldı: **köy**, **batık gemi**
ve **gezgin tüccar**. Üçü de oyunun ilk saatlerinde size yön veren, zorluğu
zaten keşfin kendisinde olan görevlerdir.

**Raylı sandık görevleri evde kurulamaz.** Bir görev yalnızca **dünyanın
oluşturduğu** raylı sandıklarda ilerler; kendi koyduğunuz bir sandık vagonunun
ganimet tablosu olmadığı için hiç sayılmaz.

XP'ler kasıtlı olarak zorlukla ölçeklenir: bir köy bulmak (1 800) ile Warden'ı
öldürmek (45 000) arasında yirmi beş katlık bir fark vardır.

Görevler otomatik ilerler (Yaratıcı/İzleyici modda ve oyuncunun yerleştirdiği
bloklarda ilerleme kaydedilmez — ekinler bu kuralın istisnasıdır, tıpkı beceri
XP'sinde olduğu gibi), periyodu bitince yeni bir rastgele atamayla sıfırlanır.
Bir günlük/haftalık görevi tamamlamak sunucu genelinde (XP miktarı
gösterilmeden) duyurulur.

---

## Saygınlık Puanı

Herkes **0** puanla başlar. Saygınlık Puanı bir para birimi **değildir**:
harcanmaz, takas edilmez, hiçbir şey satın almaz — bu sunucuda ekonomi yoktur.
Ölçtüğü şey **itibarınızdır**: liderlik tablosundaki yeriniz, açtığınız ünvan ve
kozmetikler, ve tüm sunucuya duyurulan anlar.

> **Bu sistem eskiden "Zenginlik" adını taşıyordu ve yalnızca artıyordu.** İki
> şey birden değişti: adı **Saygınlık** oldu, ve artık **düşebiliyor**. Saygınlık
> kazanmak dünyadan bir şey çıkarmaktır; kaybetmek ise ölmektir.

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

Saygınlık puanınız panelde mavi renkte, `/liderlik` içinde kendi tablosunda
(elmas simgesiyle) görünür ve **kendine ait ünvanlar ve kozmetikler** açar
(Varlıklı 50 bin → Zengin 250 bin → Servet Sahibi 1 milyon → Hazine Kralı
5 milyon → Efsanevi Zengin 25 milyon).

**Büyük sayılar kısaltılarak yazılır**: `12.6K`, `712.2K`, `1.2M`, `1.05M`.
Saygınlık puanları normal oynayışta yedi-sekiz haneye çıkıyor ve `12600000`
gibi bir sayı okunmaz, sayılır.

### Ölmek saygınlığa mal olur

Ölmek puan kaybettirir, ve **kaybınız ölmeden önceki puanınıza göre belirlenir**.
Sebebi basit: elli bin puanı olan biri için beş bin puan bir öğleden sonradır,
elli puanı olan biri için ise her şeydir. Aynı sabit sayı ikisine birden hizmet
edemez.

| Saygınlığın | Kendi hatanla | Bir yaratık öldürdü | Bir oyuncu öldürdü |
|---|---|---|---|
| 100'ün altında | — | — | **hepsi** |
| 100 – 1.000 | — | — | -100 |
| 1.000 – 10.000 | -100 | -250 | -500 |
| 10.000 – 100.000 | -1.000 | -2.500 | -5.000 |
| 100.000 – 1.000.000 | -10.000 | -25.000 | -50.000 |
| 1.000.000 ve üstü | -100.000 | -250.000 | -500.000 |

Birkaç ayrıntı:

- **1.000'in altında yalnızca bir oyuncunun elinden ölmek bir şeye mal olur.**
  Öğrenirken boğulan yeni bir oyuncunun kaybedecek bir şeyi yoktur ve
  kaybettirmek ona hiçbir şey öğretmez.
- **Puan hiçbir zaman sıfırın altına inmez.**
- **En üst bant açık uçludur**: on milyonu geçmek, kaybedemez hâle gelmek
  demek değildir.
- **Kim öldürdüğü ölüm anından değil, son aldığınız hasardan** çözülür. Bir
  iskeletin oku sizi uçurumdan attıysa bu "kendi hatanız" değildir; birinin
  üstünüze sürdüğü kurt sizi öldürdüyse bu o oyuncunun hesabına yazılır.
- Kaybınız **yalnızca size** yazılır. Kimse kimsenin kaybını görmez — ölmek
  zaten yeterince kötü bir andır.

### Sıralama duyuruları

Saygınlık liderlik tablosu **belirli aralıklarla** (varsayılan 25 dakika)
yeniden okunur ve bir önceki okumayla karşılaştırılır. Bir sıra değişmişse
sohbete **turda en fazla bir** duyuru düşer:

- **Birini geçtiyseniz** iki isim de anılır: *"Vesen aman tanımıyor! Kaan,
  Vesen adlı oyuncunun gölgesinde kalarak 3. sıraya düştü."*
- **Birkaç basamak birden tırmandıysanız** yükselişin kendisi anlatılır:
  *"Vesen önünde engel tanımıyor! Yükselişlerinin ardı arkası kesilmeyerek
  saygınlıkta 2. sıraya ulaştı."*

Her iki şekil için de **15 farklı metin** vardır ve her seferinde rastgele biri
seçilir; zamanlayıcıyla tekrarlanan tek bir sabit cümle bir süre sonra haber
olmaktan çıkar. Aralık ve metinlerin tamamı yapılandırılabilir. Turda yalnızca
**en dikkat çekici tek değişiklik** duyurulur (en çok basamak tırmanan; eşitlik
varsa en yükseğe varan), çünkü yirmi beş dakikada birkaç kişi kıpırdar ve
hepsini birden yazmak tabloyu duvar yazısına çevirir.

Duyurular yalnızca **tablonun görünür kısmı** (varsayılan ilk 10) için
çalışır; kimsenin göremediği bir sıra değişikliği duyurulmaya da değmez.

### Yükseliş ve düşüş duyuruları

Sıralama duyurularının **arasına** ikinci bir duyuru türü girer. **35–40
dakikada bir** (aralık her turda yeniden çekilir, böylece duyuru bir habere
benzer, saate değil) çevrimiçi herkesin puanı bir önceki turla karşılaştırılır
ve **yalnızca en çok kıpırdayan tek kişi** duyurulur:

- **Yükseliyorsa** övgüyle: *"Vesen son yarım saatte durmadı! Saygınlığı 42.5K
  puan arttı."*
- **Düşüyorsa** iğnelemeyle: *"Vesen yerden yere çakılmaya devam ediyor!
  Saygınlığı tamı tamına 15K puan düştü!"* — ya da *"Vesen kendine gelmekte
  zorlanıyor. Saygınlığını toparlaması için bir şeyler yapması lazım."*

Yükseliş için 6, düşüş için 7 farklı metin vardır ve her turda rastgele biri
seçilir. Ölçüm herkes için sürekli tutulur ama **hiçbir oyuncuya kendi
"ortalaması" gösterilmez** — bir sayı olarak ilginç değildir; ilginç olan
sunucuda o an en hızlı tırmanan ve en hızlı dibe giden kişidir, ve bunu yüksek
sesle söylemek özel bir puanı bir rekabete çevirir.

Bütün değerler, kilometre taşları, iki duyuru aralığı ve bütün duyuru metinleri
`sayginlik.yml` ve `messages.yml` üzerinden değiştirilebilir.

---

## Dünya bossları

**Her 30 dakikada bir**, çevrimiçi bir oyuncunun 120–320 blok uzağında,
haritada isimli bir dünya bossu belirir ve **X/Y/Z koordinatlarıyla birlikte
tüm sunucuya duyurulur**. Kimsenin ulaşamayacağı bir boss kimsenin dövüşmediği
bir bosstur; bu mesafe hem gerçekten yola çıkmayı gerektirir hem de bir sonraki
boss gelmeden varılabilir. `/haliladmin` panelinden beklemeden de çağrılabilir.

Bir boss belirdiğinde **her seferinde farklı bir ses efekti** çalar (ejderha
kükremesi, wither doğuşu, Warden çığlığı, ravager, elder guardian laneti,
illusioner, çan yankısı, ejderha kanadı — sekiz cue arasından rastgele).
Yarım saatte bir duyulan bir olay için tek sabit ses, haritadaki en dramatik
anı bir bildirim zilinden ibaret bırakırdı.

### Efsanevi ve Mistik: ekranı kesen doğuşlar

**Efsanevi** ya da **Mistik** bir boss doğduğunda duyuru sohbette kalmaz:
ekranınızın ortasına, koordinatlarıyla birlikte bir **başlık** gelir ve normal
kutlamaların iki katı süre durur.

**Mistik** bunun da üstüne çıkar: **iki ses aynı anda** çalar — elder guardian
laneti ve altına serilen derin bir ejderha uğultusu — ve ekranınıza **elder
guardian'ın kendi görünüşü** düşer, vanilla'nın bir oyuncuya gösterebileceği en
gürültülü şey. Bu sunucuda tek bir anlamı vardır, o yüzden gördüğünüz an tek
kelime okumadan ne doğduğunu bilirsiniz. (Lanet sesi tek başına ince ve yüksek,
yani bir uyarı zili gibi duyuluyordu; altındaki uğultu onu bir olaya çeviriyor.)

Diğer üç nadirlik bilerek yalnızca sohbete duyurulur: yarım saatte bir gelen bir
boss için ekranı kesmek duyuru olmaktan çıkıp rahatsızlığa dönüşürdü. Efsanevi
yaklaşık 11 doğuşta bir, Mistik 110 doğuşta bir gelir.

### Haritada işaret

Boss doğduğu anda **Xaero's Minimap haritanızda bir işaret** olarak belirir:
bossun baş harfleri, nadirliğinin renginde. Yakınında değilseniz Xaero's ekran
kenarında **hangi yönde olduğunu** da gösterir, yani koordinatı okumadan da
yola çıkabilirsiniz. Xaero's Minimap sunucunun zorunlu modlarından biri
olduğu için bu işaret herkeste çalışır.

### Boss zekâsı: ne kadar güçlüyse o kadar akıllı

Bir bossun nadirliği artık yalnızca canını ve hasarını değil, **nasıl
davrandığını** da belirler. Beş zekâ seviyesi vardır ve merdiven tek yönlüdür —
üst seviye, alt seviyenin yaptığı her şeyi yapar:

| Seviye | Nadirlik | Ne yapar |
|---|---|---|
| **Sersem** | Sıradan | Haritada dolaşır ve vurur. Çok güçlü sıradan bir yaratık. |
| **Uyanık** | Nadir | + Tırmanır (merdiven, sarmaşık), oyuncunun atlayabildiği yerlere zıplar, tuzak kapısı ve çit kapısı açar, başıboşken çevredeki hayvanlara ve köylülere saldırır |
| **Kurnaz** | Epik | + Mağaraya girmez, uçurum kenarına gitmez, önündeki kapıyı kırar, elinde yay varsa kaçarak nişan alır, canı azalınca kaçıp toparlanır ve geri döner |
| **Usta** | Efsanevi | + **Köy basar**: önce demir golemi, sonra köylüleri hedef alır; yolundaki kapıları kırar, tarlaları çiğner |
| **Deha** | Mistik | Usta ile aynı repertuvar, daha hızlı toparlanma ve daha çok kaçış hakkıyla |

**Kaçış artık gerçekten kaçıştır.** Canı belirli bir orana düşen boss ışınlanıp
uzaklaşır, peşindeki kimse kalmayınca **yavaş yavaş** iyileşir ve toparlandığını
hissettiği anda geri döner. Toparlanma bilerek tam dolmaz — savaşı silen değil,
ondan sağ çıkan bir boss olması gerekiyor. Kaçış hakkı ömür boyu sayılıdır
(Epik/Efsanevi 1, Mistik 2), yani sonsuza kadar kaçamaz.

> **Düzeltilen bir hata:** bossların kalp efekti çıkarıp canlarının hiç
> dolmaması gerçekti ve sebebi tekti. İyileştirme bir **yenilenme iksiriyle**
> yapılıyordu, ama zombi, iskelet ve boğulmuş **undead** yaratıklardır ve
> Minecraft'ta undead yaratıklar yenilenmeden hiç etkilenmez. Can artık
> doğrudan yazılıyor.

> **Düzeltilen bir hata daha:** yay/arbalet taşıyan iskelet bossların elle
> vurması da gerçekti. Bir iskelet yalnızca elinde **yay** varken vanilla'nın ok
> atma davranışına geçiyor; **arbalet** verildiğinde sessizce yumruk atma
> davranışına düşüyor — ve üst nadirliklere arbalet veriliyordu. Artık her
> kademe yay taşıyor, üstüne Kurnaz ve üzeri bosslar **kaçarak nişan alıp
> düzenli aralıklarla ağır bir isabet atışı** yapıyor.

**Köy baskını sunucu sahibinin kontrolündedir.** Boss'un kırdığı her kapı ve
çiğnediği her ekin, vanilla bir zombinin kapı kırarken çıkardığı **aynı olayı**
çıkarır — yani zombileri zaten durduran her koruma eklentisi bunu da durdurur,
bu eklentiyi hiç tanımadan. `mobGriefing` gamerule'u kapalıysa hiçbiri olmaz.

Yaratıcı ve izleyici modundaki oyuncular bossa **görünmez**: bakmaya gelen bir
yetkili bossu kaçırtmaz ve kaçış hakkını harcatmaz.

### Kaybolma kuralları

- **Kimse dokunmadıysa**, bir sonraki boss doğduğunda eskisi kaybolur. Yarım
  saatte bir yeni bir duyuru yapılırken haritada kimsenin gitmediği eski
  bossların birikmesinin bir anlamı yok.
- **Ama bir oyuncu ona bir kez bile vurduysa kaybolmaz.** Yay ya da trident de
  sayılır. Birinin kılıcının altında buharlaşan bir boss, haritada iki boss
  olmasından çok daha kötüdür.
- **Bosslar güneşte yanmaz.** Koordinatı duyurulup gidilmesi beklenen bir
  yaratığın siz varmadan kendi kendine ölmesi, duyuruyu yalan yapardı.

### Boss can çubuğu

Bir bossa **96 blok** mesafeye yaklaştığınızda ekranın üstünde o bossa ait bir
can çubuğu belirir. Çubukta bossun **ismi**, **anlık/maksimum canı** ve
parantez içinde **nadirliği** yazar; çubuğun rengi de nadirliğin kendi
rengidir (sıradan beyaz, nadir yeşil, epik mor, efsanevi sarı, mistik pembe).
Çubuk yarım saniyede bir güncellenir, uzaklaştığınızda kaybolur, boss
öldüğünde herkesin ekranından kalkar.

Savaşa hiç gitmeyen birinin ekranında bir yabancının canının durmaması için
çubuk sunucu geneline gösterilmez; duyurulan koordinata gidenlerin de
çubuksuz kalmaması için izleyici listesi sürekli yeniden hesaplanır.

**Beş nadirlik** vardır ve hangisinin çıkacağı ağırlıklara göre belirlenir
(sıradan 49, nadir 30, epik 20, efsanevi 10, mistik 1 pay):

| Nadirlik | Can | Hasar | Zırh | Davranış | Sınıf XP | Oyun XP | Saygınlık |
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

**Zombi, iskelet, boğulmuş ve büyücü bossları kalıcı Hız 2 taşır.** Koşamayan
bir boss, ilan edilen koordinata gidilip bir kere vurulan ve yürüyerek
uzaklaşılan bir hedeftir; bu, nadirliğin hız çarpanından ayrı ve türe özgü bir
ayardır (`bosslar.yml` → `hiz-seviyesi`).

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
  *"İnsan Yiyen Annabelle'nin Kafası"*. Kafanın altında **onu kimin öldürdüğü**
  yazar — bir kupa, kimin kazandığını söylemediği sürece yalnızca bir eşyadır,
  ve ölüm duyurusu sohbetten kayıp gittikten sonra o dövüşün geriye kalan tek
  kaydı budur. Bossu bir lav birikintisi ya da başka bir yaratık bitirdiyse
  kafa yine düşer ve "öldüren: bilinmiyor" der.

Boss ayrıca sıradan bir yaratık olarak avcılık XP'si, görev ilerlemesi ve
Saygınlık Puanı da verir — yukarıdakilerin hepsi bunun **üstüne** eklenir.

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

## Oyun içi rehber (`/yardım`)

`/yardım` (ya da `/bilgi`, `/rehber`) sunucunun kendi el kitabını açar. Ana
menüdeki **Rehber** düğmesinden de ulaşılır.

On kategori vardır; her birine tıkladığınızda o konunun sayfaları açılır ve
**bir sayfanın üstüne geldiğinizde anlatım orada yazar**:

| Kategori | Ne anlatır |
|---|---|
| **Başlangıç** | `/mmo`, sınıf seçimi, panelin okunması, başlangıç görevleri |
| **Sınıflar** | Üç sınıfın mantığı, sinerji becerileri, sınıf seviyesi, sınıf değiştirme |
| **Beceriler** | Altı beceri, XP'nin nasıl kazanıldığı, XP vermeyen durumlar, kilometre taşları, Güç |
| **Kabiliyetler** | Kuşanma, çift sağ tık, açılma seviyeleri, tetiklemeyen tıklamalar |
| **Enerji** | Üç kural, blok başına maliyetler, maksimum enerjiyi arttırma |
| **Odak Ağacı** | İki puan havuzu, kabiliyet yeteneklerinin ne yaptığı, puanların kaybolmaması |
| **Görevler** | Üç tür, beş çeşit, ödülün hangi beceriye yazıldığı, panele sabitleme |
| **Saygınlık Puanı** | Neden para değil, nasıl arttığı, kilometre taşları ve sıralama |
| **Prestij** | Mantığı, üç şart, karşılığında ne alındığı |
| **Dünya Bossları** | Ne olduğu, beş nadirlik, kaybolma ve güneş kuralları |
| **Komutlar** | Bütün komutlar ve ne açtıkları |
| **Ek Özellikler** | Bulut sandığı, ev, takas, unvan/kozmetik, uyku, TAB listesi |

Rehberin **bütün metni `rehber.yml` dosyasındadır**: sunucu sahibi istediği
kategoriyi ekleyebilir, sırayı değiştirebilir, hepsini kendi üslubuyla yeniden
yazabilir. Tek satır kod değişmez.

---

## Bulut sandığı (`/çanta`)

Sınıf seviyeniz **30**'a ulaştığında — ya da **bir kez prestij yaptıysanız** —
`/çanta` (`/canta`, `/backpack`) komutuyla kişisel bir depo açılır.

- **18 slotla açılır**, odak ağacından **32 slota** kadar genişletilir (aşağıya
  bakın).
- **Ölünce hiçbir şey kaybolmaz.** İçindekiler envanterinizde değil, sunucunun
  veritabanında durur — ne lav, ne düşme, ne de bir hırsız ona ulaşabilir.
- **Her yerde açılır**: hangi biyomda, hangi dünyada, hangi boyutta olursanız
  olun. Yanınızda taşıdığınız bir blok değil, bulutta bir hesap gibi düşünün.
- Pencereyi kapattığınız anda içerik kaydedilir; sunucu çökse bile en son
  gördüğünüz hâliyle durur.
- **Sağdaki panelde** ev ile takasın yanında durumu görünür.

Ev (15) ve takas (20) ile aynı merdivenin en üst basamağıdır: alabileceğiniz
en kalıcı şeyi en sona bıraktık.

### Sandığı genişletmek

Sandık **18 slot** (iki sıra) açılır. **Odak ağacının Özellik bloğundaki
"Bulut Sandığını Genişlet" düğmesinden**, bir **özellik puanı** karşılığında
**+7 slot** satın alabilirsiniz — en fazla iki kez:

| Genişletme | Kapasite |
|---|---|
| yok | 18 slot |
| 1 | 25 slot |
| 2 | **32 slot** |

Baştan tam boy verilen bir sandık, açıldığı anda anlamını kaybeden bir ödüldür;
iki sıra gerçekten işe yarayacak kadar çok ve ilk genişletmeyi isteyecek kadar
azdır. Puan, can ve hasar yetenekleriyle **aynı havuzdan** çıkar, yani bu gerçek
bir tercihtir: iki kalp mi, yedi slot mu?

Satın almadığınız slotlar pencerede **kırmızı panelle kapalı** görünür ve
tıklanamaz. (Minecraft 25 slotluk bir sandık yapamaz — bir sandık her zaman
dokuzun katıdır — o yüzden pencere tam satır açılır ve fazlası kilitlenir. Yedi
alıp dokuz vermek bir kez hoş, sonsuza kadar kafa karıştırıcı olurdu.)

Sandığı açtığınızda sohbete artık uzun bir açıklama düşmez; yalnızca açık mavi
**"Bulut Sandığı"** yazar ve **üzerine geldiğinizde** eski açıklamanın tamamını
(ölümde kaybolmadığı, kapanışta kaydedildiği ve mevcut kapasiteniz) okursunuz.
Günde otuz kere okunan iki satır bilgi değil gürültüdür.

---

## Ev (`/ev`)

Sınıf seviyeniz **15**'e ulaştığında — ya da **bir kez prestij yaptıysanız**,
sınıf seviyeniz ne olursa olsun — kendinize bir ev ayarlayabilirsiniz.
Prestijin de sayılmasının sebebi basit: prestij sınıf seviyenizi 1'e
sıfırlar, dolayısıyla yalnızca seviyeye bakan bir kural bu özelliği tam da en
çok emek vermiş oyunculardan her prestijde geri alırdı.

Eşiği geçtiğiniz **seviye atlamasında sohbetinize "Yeni özellik açıldı: /ev"**
mesajı gelir ve komutun nasıl kullanılacağını söyler. Eskiden bunu yalnızca
paneldeki durum satırının "Kilitli"den "Açık"a dönmesinden anlayabilirdiniz;
kimse savaşın ortasında oraya bakmıyor.

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
veya bir kez prestij yapmış olması gerekir. Tıpkı `/ev` gibi, eşiği geçtiğiniz
seviye atlamasında sohbetinize **"Yeni özellik açıldı: /takas"** mesajı gelir.

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
**Saygınlık Puanı** ve **her becerinin kendi tablosu** olmak üzere on bir
tabloya açılır. Aynı tablolara `/menü`'nün Topluluk kategorisinden de
ulaşılabilir. Sonuçlar her açılışta veritabanını sorgulamak yerine kısa,
yapılandırılabilir bir süre için önbelleğe alınır (`leaderboards.yml`).

**Her tablonun ne ölçtüğünü söyleyen kendi simgesi vardır**: Güç netherite
külçesi, prestij nether yıldızı, görevler harita, koleksiyon ender sandığı,
saygınlık elmas — beceri tabloları ise o becerinin `/beceri` listesinde
gördüğünüz **kendi simgesini** taşır. Her tablonun üstüne geldiğinizde tam
olarak neyi sıraladığı yazar.

Bir tabloyu açtığınızda **ilk üç sıra podyum olarak** görünür (altın, demir ve
bakır blok) ve **kendi satırınız yeşil renkte işaretlenir** — herkesin ilk
baktığı satır zaten kendisininkidir.

## Unvanlar ve kozmetikler

`titles.yml` **19 ünvan**, `cosmetics.yml` **14 kozmetik** tanımlar; her birinin
tek bir kazanma koşulu vardır: seviye, prestij, belirli bir görevi/koleksiyonu
tamamlama, bir beceri seviyesine ulaşma ya da bir **Saygınlık Puanı** eşiğini
geçme. Altı becerinin her birinin kendi ustalık ünvanı ve kendi kozmetiği,
saygınlığın de kendi beş ünvanı ve üç kozmetiği vardır. `/ünvan` ve
`/kozmetik` neyi açtığınızı gösterir ve tıklayarak kuşanmanızı sağlar; aynı
kataloglara `/menü`'den de göz atılabilir. Kuşanılan bir unvan, sohbette
adınızın önünde **kendi yapılandırılmış rengi ve kalınlığıyla** görünür.
Kuşanılan bir kozmetik, katıldığınızda ve her seviye atladığınızda kısa bir
parçacık efekti oynatır.

**Her iki ekran da birebir aynı düzendedir**: üstte kendi kafanızı taşıyan ve
kaç tanesini açtığınızı gösteren bir başlık (ör. "Açılan unvan: 7/19"), altında
katalog, en altta geri düğmesi. Aynı sistemin iki örneği oldukları için
aralarında gezinirken düzeni yeniden öğrenmeniz gerekmez.

**Henüz açmadığınız bir ünvanın ya da kozmetiğin adı gösterilmez.** Yerinde
gri bir yer tutucu durur ve üstüne geldiğinizde yalnızca **onu neyin açacağı**
yazar — "Gereken: 50. prestij", "Gereken: madencilik becerisinde 50. seviye",
"Gereken: 250K saygınlık puanı" gibi. Bir ünvan kazanılıp kuşanılan bir
şeydir; bütün listeyi hiçbirini kazanmadan menüden okumak sürprizi bedavaya
harcar. Ama açıklamasız bir boşluk da hedef değil sadece boşluktur, o yüzden
şart her zaman yazar.

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

**Sessiz kalan anlar kalmadı.** Aşağıdakilerin hepsinin artık kendi ses efekti
ve çoğunun kendi parçacık efekti vardır:

| An | Ne olur |
|---|---|
| Yetenek ağacında puan harcamak | Büyü masası sesi + büyü parçacıkları |
| `/ev ayarla` | Yeniden doğuş çıpası sesi + köylü parçacıkları |
| Eve ışınlanmak | Enderman ışınlanma sesi + portal parçacıkları |
| Takas daveti göndermek / almak | İki tarafa **farklı** cue (kısa onay / çan) |
| Kabiliyet kuşanmak / çıkarmak | Kısa tık / şalter sesi |
| Görev tamamlamak | Başarı sesi + totem parçacıkları |
| **Prestij yapmak** | Kendi ses cue'su, altın başlık, totem ve ışık huzmesi sütunu, **kimseye zarar vermeyen** havai fişek |
| **Efsanevi/Mistik boss doğuşu** | Ekranın ortasına başlık; Mistik'te ayrıca elder guardian görünüşü ve kendi laneti |
| Bir kabiliyet çalışırken | Ekran kenarı kabiliyetin renginde, rozeti sol üstte (eşlik mod'u) |
| `/rtp` ile inmek | Işınlanma sesi + ters portal parçacıkları |
| Bir dünya bossunun ölmesi | Herkese zafer sesi; **cesedin başında** patlama, totem ve ışık huzmesi — *nerede* olduğunu savaşa gidenlere söylemek için |
| Tek kişi geceyi uyuması | O dünyadaki herkese çan sesi + duyuru |

Kabiliyetlerin kendi gösterileri de `abilities.yml`'de ayrıntılıdır: her
kabiliyetin rengi, ana sesi, dört tick sonra çalan **yankı sesi** ve partikül
**şekli** (saçılma / genişleyen halkalar / yukarı sarılan helis / kubbe) ayrı
ayrı ayarlanır.

## Kabiliyet HUD'u

Sunucunun zorunlu eşlik mod'u (`hmmo-client`), **çalışan kabiliyetinizi
ekranınıza çizer**. Sunucu yalnızca "şu kabiliyet açık, rengi bu, ağırlığı bu,
şu kadar kaldı" der; çizimi mod yapar.

| Ne | Nerede |
|---|---|
| **Ekran kenarı, kabiliyetin renginde** | Oyun ekranının dört kenarı, içe doğru soluklaşarak |
| **Kabiliyet rozeti** (simge + ad + kalan süre) | HUD'un sol üstü |
| **Aynı rozet** | **Envanteri açtığınızda**, iksir etkilerinin listelendiği sütunda |
| **Kırmızı parlayan düşmanlar** | Kan Yemini ve Asil Kurt açıkken, çevrenizdeki hostile yaratıklar |
| **Kombo satırı** | Ekranın altında, ortada — yalnızca Kan Yemini ve Asil Kurt'ta |

**Kenarın ne kadar koyu olduğu kabiliyetin kademesine bağlıdır**: 10. seviye
kabiliyetler hafif bir renk verir, 50. seviye olanlar ekranı gerçekten
boyar (%25 / %45 / %70 / %100). Sunucunun "gücüne göre" sorusuna verebileceği
en dürüst cevap kademedir — bir madencilik aleti ile bir savaş buff'ının hasar
sayıları zaten kıyaslanamaz.

**Efektler kabiliyetle birlikte söner.** Kenar boyaması kalan süreyle orantılı
olarak yavaşça zayıflar ve **son saniyede tamamen kaybolur**, yani süre dolmak
üzereyken bunu geri sayıma bakmadan görürsünüz. Kan Yemini ve Asil Kurt'ta
kenar ayrıca **nabız gibi atar**, ve sönerken nabız da zayıflar — bir kabiliyet
son ana kadar tam güçle atıp sonra birden kesilmez.

**Bunların hepsi yalnızca sunumdur.** Mod'un hiçbir ekranı, komutu, tuş
bağlaması ya da ayar dosyası yoktur ve oyunun kurallarına karışmaz: bu
paketleri hiç almayan bir istemci aynı oyunu daha sade bir ekranla oynar.
Kabiliyetin ne yaptığına her zaman ve yalnızca sunucu karar verir.

**Neden mod?** Sunucu vanilla bir istemcinin ekranına çizemez — "ekranı boya"
diye bir paket yoktur, ve Minecraft'ın parlama bayrağı yaratığın kendi
özelliğidir, izleyicinin değil; sunucudan yapılsa görüş mesafesindeki herkes
için parlardı.

---

## Envanter menüsü

`/menü`, çerçeveli bir merkez menü açar: en üstte kendi kafanı taşıyan,
Gücünü/prestijini/sınıfını özetleyen kişisel bir başlık, altında dört kategori
— **İlerleme** (profil, **Becerilerim**, sınıf/yetenekler, prestij),
**Kabiliyetlerim**, **Görevler** (görevler, koleksiyonlar, macera günlüğü) ve
**Topluluk** (liderlik tabloları, unvanlar, kozmetikler) — her biri ne
içerdiğini anlatan bir açıklama satırıyla, ve en altta **Rehber** ile
**Eklenti Hakkında** düğmeleri.

**Uzun açıklamalar artık alt alta sarılır.** Minecraft eşya açıklamalarını
sarmaz, sağa doğru uzatır; iki cümlelik her açıklama ekranın dışına taşan ince
bir şerit hâline geliyor ve okunmuyordu. Artık bütün açıklama satırları kelime
sınırında bölünüp alt alta yazılır — rengini ve kalınlığını da koruyarak.
Düzeltme tek bir yerde yapıldığı için görev panosundan yetenek ağacına, oyun içi
rehberden yönetici paneline kadar **bütün ekranları** kapsar.

**Her ekran çerçevelidir ve her düğme ne yaptığını söyler.** Kategori
ekranlarının hepsi kendi renginde çerçevelenir (İlerleme turuncu, Görevler
camgöbeği, Topluluk mor, Rehber mavi, yönetici turuncu) ve içindeki her
girişin altında ne açtığını anlatan bir satır vardır. Liste ekranlarının
(koleksiyonlar, günlük, oyuncu listeleri) en altında geri düğmesini taşıyan
bir zemin sırası bulunur — hem ekranı sonlandırır hem de uzun bir listenin
geri düğmesinin üzerine taşmasını imkânsız kılar.

**Sınıfa özel çerçeveler:** kabiliyet listesi, yetenek ağacı, her becerinin
detay ekranı ve sınıf seçim ekranındaki kaideler, o ekranın ait olduğu sınıfın
kendi renginde camla çerçevelenir — Amansız Savaşçı kırmızı, Mağara Adamı mavi,
Tabiat Vârisi yeşil. Böylece bir ekranın kime ait olduğu tek bir yazı okumadan
anlaşılır.

**`/profil` artık kendi tam kaydınızı açar** — `/profil <isim>`'in başkası için
açtığı ekranın aynısı, size yöneltilmiş hâli: Güç ve sıralaması, saygınlık ve
sıralaması, altı becerinin seviyesi ve sıralaması, odak ağacında harcadığınız
rankler, sınıfınızın dört kabiliyeti, açtığınız unvan ve kozmetikler.

**Eklenti Hakkında** ekranı (hem `/menü`'den hem `/haliladmin`'den açılır)
sürüm bilgisini ve emeği geçenleri listeler: **Furkan Okan (fxrkqnplus)** ve
**Claude (Anthropic)**.

---

# Bölüm 2 — Teknik Bölüm

Buradan aşağısı sunucu yöneticisi ve geliştirici içindir: yönetici paneli,
kurulum, derleme, veritabanı, yapılandırma dosyaları, entegrasyonlar ve test.

## Yönetici paneli

`/haliladmin` (`halilibrahimmmo.admin` izni gerektirir, varsayılan op) tamamen
GUI üzerinden çalışan bir yönetici paneli açar — sohbet argümanlı bir yönetici
komutu yoktur. Panel `/menü` ile aynı görsel dilde çerçevelenmiştir: en üstte
kendi kafanızı taşıyan, çalışan sürümü ve çevrimiçi oyuncu sayısını gösteren
bir başlık, altında ne yaptığını açıklayan birer satırla her işlem. Sunduğu
işlemler: `config.yml`/`messages.yml`/tüm oyun verisi dosyalarını yeniden
yükleme, mevcut oturum için ayrıntılı hata ayıklama kaydını açıp kapatma,
çalışan eklenti/Paper sürümünü görüntüleme, mod doğrulama durumunu okuma, ve
**oyuncu düzenleme**.

**Çevrimiçi oyuncular** ve **tüm oyuncular** listelerinin ikisi de aynı
düzenleme ekranını açar. (Eskiden çevrimiçi liste salt okunur bir özet
açıyordu, yani tam karşınızda duran biriyle ilgilenen personel, aynı ismi
diğer listede yeniden bulmak zorunda kalıyordu.)

### Oyuncu düzenleme ekranı

Turuncu çerçeveli, oyuncunun **gerçek kafasını** taşıyan ve çevrimiçi/çevrimdışı
durumunu söyleyen bir ekran. Düzenlenebilecekler:

| Satır | Sol tık | Shift+Sol | Sağ tık | Shift+Sağ |
|---|---|---|---|---|
| Beceri seviyesi (altı beceri, **kendi simgeleriyle**) | +1 | +10 | −1 | −10 |
| Beceri XP'si | +50 | +500 | — | — |
| Sınıf seviyesi (**sınıfın kendi simgesiyle**) | +1 | +10 | −1 | −10 |
| Sınıf XP'si | +50 | +500 | — | — |
| **Özellik Puanı** | +1 | +5 | −1 | −5 |
| **Kabiliyet Puanı** | +1 | +5 | −1 | −5 |

Ayrıca **sınıfı zorla ayarlama** (bekleme süresi olmadan; bu da oyuncunun kendi
değiştirmesi gibi seçili kabiliyeti kuşanmasından çıkarır), **kabiliyet
kuşandırma/çıkarma** (yalnızca çevrimiçi oyuncuda — kuşanılan kabiliyet bellekte
durur ve çıkışta unutulur) ve **saygınlık ayarlama** düğmeleri vardır.

Puan satırları oyuncunun toplam ve harcanmamış puanını, ayrıca bunun ne
kadarının yönetici tarafından verildiğini gösterir. Verilen puan **türetilen
bütçeye yazılmaz, yanına eklenir**: bütçe her okumada Güç ve prestijden yeniden
hesaplandığı için içine yazılan şey bir sonraki seviye atlamada buharlaşırdı.
Negatife düşmez — negatif bir bağış, oyuncunun sonradan dürüstçe kazandığı
puanları sessizce yutardı.

Bir seviye atlamaya yol açan her müdahale, hedef oyuncu çevrimiçiyse organik bir
seviye atlamayla birebir aynı geri bildirimi (cübuk, ses, "yeni kabiliyet
açıldı" ekran yazısı, panel güncellemesi) tetikler. Her yönetici müdahalesi
Türkçe bir sunucu günlüğü kaydı bırakır.

### Dünya bossu çağırma ekranı

**Dünya Bossu Çağır** artık bir düğme değil bir ekrandır:

- **Rastgele nadirlik** — sistem ağırlıklara göre seçsin (eski davranış).
- **Beş nadirlik düğmesi** — Sıradan / Nadir / Epik / Efsanevi / Mistik. Etkinlik
  yürüten personel "Mistik boss çıkacak" diyip gerçekten çıkarabilmeli.
- **Bossun Adı** — tıklayın, adı sohbete yazın. Yalnızca bir sonraki çağırma
  için geçerlidir; yazmazsanız türünün kendi isim havuzundan çekilir.

Onay mesajı boss **gerçekten doğduktan sonra** gelir; doğacak yer bulunamazsa
bunu açıkça söyler.

### Saygınlık ayarlama: paneldeki tek "sohbete yaz" alanı

Oyuncu düzenleme ekranındaki **Saygınlık** satırına tıkladığınızda ekran
kapanır ve panel sizden **sayıyı sohbete yazmanızı** ister. Yazdığınız satır
sohbete düşmez, doğrudan panele gider; vazgeçmek için `iptal` yazın, 60 saniye
içinde cevap vermezseniz soru kendiliğinden düşer.

Bu, paneldeki **tek** serbest metin alanıdır ve bilerek öyledir: saygınlık
totalleri rahatlıkla milyonlara çıkar, tıkla-arttır satırlarıyla 4 812 500
yazmak bir düzeltme aracı değil bir işkencedir.

İki şeyi bilerek yapar:

- **Ekler değil, üzerine yazar.** Bir rollback'i düzelten personel sayının kaç
  *olması gerektiğini* bilir, ne kadar ekleyeceğini değil.
- **Eklentideki tek saygınlık düşürme yoludur.** Oyun içi hiçbir yol bu sayıyı
  azaltmaz — puanın anlamı "dünyadan ne çıkardın"dır ve geri alınması onu
  başka bir şeye çevirirdi. Ama yanlış kalmış bir totali düzeltmenin de bir
  yolu olmalıydı. Çevrimdışı oyuncularda da çalışır.

## Gereksinimler

- Java 26 (Oracle JDK veya OpenJDK)
- Paper 26.2 (Build 60 veya üzeri)
- Gradle Wrapper (repo ile birlikte gelir, ayrıca yerel Gradle kurulumu gerekmez)

## Proje yapısı

- [`api/`](api) — Herkese açık geliştirici API'si. Saf arayüzler + bir
  `ServicesManager` tabanlı erişimci. Diğer eklentiler yalnızca bu modüle bağımlı olur.
- [`core/`](core) — Eklentinin tam uygulaması (`HalilIbrahimMMOPlugin` ve her bir
  iç sistem). `api` modülüne bağımlıdır.
- [`clientmod/`](clientmod) — Zorunlu Fabric eşlik mod'u: mod doğrulamasına
  yanıt verir ve kabiliyet HUD'unu çizer. **Kök derlemeye dâhil değildir**,
  kendi Gradle wrapper'ı vardır (bkz. "İki ayrı derleme zinciri").

## Derleme

```powershell
./gradlew build
```

Dağıtıma hazır, birleştirilmiş (shaded) jar dosyası
`core/build/libs/HalilIbrahimMMO-<surum>-all.jar` yolunda oluşturulur.
Her Gradle komutu için Java 26 kullanın; bu makinede kurulu JDK
`C:\Program Files\Java\jdk-26.0.1` konumundadır.

### İki ayrı derleme zinciri

Depoda **birbirinden bağımsız iki Gradle derlemesi** vardır ve bunlar asla tek
komutla birlikte derlenmez:

| | Sunucu eklentisi | İstemci mod'u |
|---|---|---|
| Konum | kök (`api` + `core`) | `clientmod/` |
| Gradle | 9.6.1 | 9.5.1 (ayrı wrapper) |
| JDK | 26 | 25 hedefli derleme |
| Komut | `./gradlew build` | `cd clientmod` → `.\gradlew.bat build` |
| Çıktı | `core/build/libs/HalilIbrahimMMO-<sürüm>-all.jar` | `clientmod/build/libs/hmmo-client-<sürüm>.jar` |
| Nereye gider | sunucunun `plugins/` klasörü | oyuncunun `mods/` klasörü |

PowerShell'de `clientmod` içinde `./gradlew` çalışmaz, `.\gradlew.bat` gerekir.

## Yerel test sunucusu çalıştırma

```powershell
./gradlew runServer
```

Bu komut `core/run/` klasörüne bir Paper 26.2 test sunucusu indirir ve eklenti
kurulu olarak başlatır.

## Sürüm yükseltirken: yapılandırma dosyaları

Eklenti, paketlenmiş YAML dosyalarını veri klasörüne **yalnızca dosya yoksa**
kopyalar; var olan bir dosyanın üzerine asla yazmaz. Bu, yaptığınız
düzenlemelerin ve yorum satırlarının bir yeniden yükleme sonrası hayatta
kalmasını sağlar — ama şu sonucu da doğurur:

> **Yeni mesaj anahtarı veya yeni ayar getiren bir sürüme geçerken**, ilgili
> dosyaları `plugins/HalilIbrahimMMO/` altından silip yeniden ürettirmeniz
> gerekir. Aksi halde yeni anahtarlar eksik kalır ve sunucu günlüğü
> "eksik mesaj anahtari" uyarılarıyla dolar.

Elle yaptığınız değişiklikler varsa, silmeden önce yedekleyip yeni dosyayla
karşılaştırın.

**Bu sürüme (Faz 23) geçerken silinmesi gereken dosyalar:** `abilities.yml`,
`bosslar.yml`, `classes.yml`, `cosmetics.yml`, `feedback.yml`, `messages.yml`,
`prestige.yml`, `rehber.yml`, `titles.yml`.

> **`prestige.yml` bu listede zorunludur.** Yeni `fiziksel-oduller` bölümü
> olmadan eklenti **hiç açılmaz** — boş ya da eksik bir ödül, hiçbir şey
> yapmayan bir düğme demek olduğu için bilerek hata sayılır.

> **`zenginlik.yml` artık `sayginlik.yml`.** Eski dosya okunmaz; yenisi
> kendiliğinden üretilir. Eski dosyada elle yaptığınız ayarlar varsa yenisine
> taşıyıp eskisini silin. **Veritabanı tablosu değişmedi**, dolayısıyla hiçbir
> oyuncunun puanı kaybolmaz.

Veritabanı kendiliğinden güncellenir (iki yeni göç: v19, v20).

**Mevcut oyuncular için not:**

- **Tabiat Vârisi'nin kabiliyet sırası değişti ve Ağaç Kırıcı kaldırıldı**:
  Filiz Patlaması 20 → 10, Sürü Çağrısı 50 → 20, Doğa Ana'nın Koruyucuları 35'te
  kaldı, 50. basamağa **Asil Kurt** geldi. Harcanmış yetenek rankleri kaybolmaz:
  Ağaç Kırıcı'ya harcanan üç yetenek aynı id ve aynı havuzda kalarak Asil
  Kurt'a yönlendirildi. **Odunculuğun 25. kilometre taşı ağaç devirmesi hiç
  değişmedi.**
- **Bulut sandığı 18 slota indi.** 18'in üstünde eşyası olan hiç kimsenin eşyası
  silinmez — kapasitenizin ötesindeki slotlar kilitli görünür ama içindekiler
  kayıtta durur ve genişletme aldıkça yeniden erişilir olur.
- **Yetenek puanı kazanımı 25/40'tan 30/60'a çıktı.** Harcadığınız rankler
  yerinde kalır, ama **toplam** puanınız düşebilir; bu durumda yeni puan
  kazanana kadar harcayacak puanınız olmaz. Hiçbir rank geri alınmaz.
- **Daha önce prestij yapmış her oyuncunun bekleyen bir fiziksel ödül sandığı
  olur** (kalıcı ödülde olduğu gibi, hak "prestij eksi alınan" olarak
  hesaplanır). İlk girişte sandık ekranı karşılarına çıkar — bu bir kayıp değil,
  geriye dönük bir kazançtır.
- **Ölmek artık saygınlığa mal oluyor.** Bkz. "Ölmek saygınlığa mal olur".

## Veritabanı

SQLite, sıfır yapılandırma gerektiren varsayılan seçenektir. MySQL, MariaDB ve
PostgreSQL, `config.yml` (`veritabani.tur`) üzerinden desteklenir. Tüm JDBC
sürücüleri ve HikariCP, Paper'ın `PluginLoader` mekanizması üzerinden çalışma
zamanında çözümlenir — eklenti jar'ına hiçbir şey gömülmez (shade edilmez).

Tüm tablolar `hmmo_` ön ekiyle oluşturulur, böylece birden fazla eklentinin aynı
şemayı paylaştığı ortak veritabanlarında çakışma olmaz. Şema değişiklikleri
sürüm numaralı göç (migration) dosyalarıyla yapılır ve `hmmo_schema_version`
tablosunda takip edilir; gönderilmiş bir göç asla değiştirilmez veya
numarası yeniden verilmez. Şu an **20 göç** vardır; en yenileri
`hmmo_player_backpack_upgrades` (v19, bulut sandığı genişletme sayısı) ve
`hmmo_player_prestige_physical` (v20, alınan fiziksel prestij sandığı sayısı).
Sürüm yükseltirken veritabanına elle dokunmak gerekmez — eksik göçler ilk
açılışta kendiliğinden uygulanır.

> **Saygınlık yeniden adlandırıldı, tablo adlandırılmadı.** Faz 23'te `wealth`
> paketi `reputation` oldu ve `zenginlik.yml` `sayginlik.yml` olarak yeniden
> adlandırıldı, ama **veritabanı tablosu `hmmo_player_wealth` olarak kaldı** ve
> onu oluşturan göç sınıfının adı da değişmedi. Bir göçün yaptığı iş kalıcıdır:
> tabloyu yeniden adlandırmak, o göçü çoktan uygulamış her sunucuda yeni bir göç
> ve veri taşıma gerektirirdi — kazancı sıfır olan bir risk. Hiçbir puan
> kaybolmaz.

Kilometre taşı aç/kapa tercihleri **hiçbir tabloya yazılmaz**: oyuncunun kendi
`PersistentDataContainer`'ında dururlar. Sunucu onu oyuncu dosyasıyla zaten
kaydettiği için tercih yeniden başlatmayı atlatır, ama şema, göç ve saklama
süresi sorusu doğurmaz — yerleştirilmiş blokların chunk'ın kendi verisinde
tutulmasıyla aynı gerekçe.

Bulut sandığının içeriği `ItemStack.serializeItemsAsBytes(...)` ile
serileştirilip **Base64 metin** olarak saklanır: eşyanın taşıyabildiği her
bileşen (büyü, özel isim, yazılı kitap, shulker içeriği) korunur ve metin
sütunu dört veritabanı motorunda da aynı davranır — BLOB davranışı ise
gerçekten motora göre değişir.

## Yapılandırma dosyaları

Hepsi Türkçe yorumludur ve `/haliladmin` panelindeki **Yeniden Yükle** düğmesiyle
sunucuyu yeniden başlatmadan uygulanabilir.

| Dosya | Ne denetler |
|---|---|
| `config.yml` | Dil, hata ayıklama modu, veritabanı bağlantısı ve havuz ayarları |
| `messages.yml` | Oyuncunun/yöneticinin gördüğü **her** metin, ikon paketi etiketleri |
| `skills.yml` | Altı becerinin seviye eğrisi, blok/yaratık/eylem başına XP, kilometre taşları, simgeler |
| `abilities.yml` | Enerji havuzu ve yenilenme hızı, her kabiliyetin süresi/beklemesi/maliyeti/etkisi/görseli |
| `classes.yml` | Sınıf kataloğu, sinerji becerileri, yetenek ağaçları, iki puan havuzu, sınıf renkleri/çerçeveleri |
| `quests.yml` | 85 görev: hedef türü, sayı, XP, eğittiği beceri, simge, açıklama ve panel için `kisa-ad` |
| `collections.yml` | Koleksiyon kataloğu ve tamamlama bonusları |
| `prestige.yml` | Prestij şartları, maksimum prestij sayısı, prestij saygınlık ödülü, dört kalıcı ödülün değerleri ve **fiziksel ödül sandıklarının içerikleri** |
| `leaderboards.yml` | Tablo başına gösterilecek oyuncu sayısı ve önbellek tazeleme aralığı |
| `titles.yml` / `cosmetics.yml` | 19 unvan / 14 kozmetik: görünen ad, simge, açılma koşulu, sohbet stili |
| `feedback.yml` | Cübuk/başlık süreleri, isimli ses efektleri, boss doğuş sesi listesi |
| `sayginlik.yml` | Saygınlık Puanı değerleri, kilometre taşları, sıralama ve eğilim duyurusu aralıkları |
| `bosslar.yml` | Boss doğuş aralığı ve mesafesi, beş nadirlik (**`zeka` seviyesi**, kaçış/toparlanma oranları dâhil), arketipler ve isim havuzları |
| `sunucu.yml` | Tek kişi uykusu ve TAB listesi davranış anahtarları (metinler `messages.yml`'de) |
| `modlar.yml` | Zorunlu/yasaklı Fabric mod listesi ve doğrulama ayarları |
| `rehber.yml` | **Oyun içi rehberin bütün metni**: kategoriler, konular, gövde satırları |

`rehber.yml` kasıtlı olarak `messages.yml`'nin dışındadır. `messages.yml` kodun
doldurduğu **şablonları** tutar; bir görevin açıklaması, bir becerinin adı ve
bir rehber sayfası ise **içeriktir**. Kategori ve konu ekleyebilir, sırayı
değiştirebilir, hepsini kendi üslubunuzla yeniden yazabilirsiniz — tek bir
Java sabiti gerekmez. Faz 22'den beri **eklenti uzun satırları kelime sınırında
bölüp alt alta yazar**, dolayısıyla bir satırın taşması artık ekranın dışına
kaçmaz. Yine de `GuideConfigTest` bir üst sınır zorlar: bir rehber satırını
60–70 karakterin altında tutmak, sarma sonrası da en okunaklı sonucu verir.

`sunucu.yml` bilerek **yalnızca davranış anahtarı** taşır; TAB listesinin
başlığı/alt bilgisi, sunucu adresi, uyku duyurusu ve harita adresi gibi
oyuncunun gördüğü her metin `messages.yml`'nin `tab:`, `uyku:` ve `harita:`
bölümlerindedir. Harita adresi orada **iki kez** geçer (tıklanan bağlantı ve
okunan metin), çünkü MiniMessage'ın `click:open_url` etiketi yer tutucu kabul
etmez — adresi değiştirirken ikisini birden değiştirin.

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

> **API'de kırıcı bir değişiklik (Faz 23):** `LeaderboardCategory.WEALTH`
> sabiti `REPUTATION` oldu. Bu enum `api` modülündedir; bu eklentinin liderlik
> tablolarını okuyan bir üçüncü taraf eklenti varsa yeniden derlenmesi gerekir.

## Moderasyon kaydı: köylü ve evcil hayvan ölümleri

Sunucuda ölen **her köylü** (gezgin tüccarlar dâhil) ve **evcilleştirilmiş her**
at, eşek, katır, köpek ve kedi için konsola tek satırlık bir kayıt düşer:

```
[OLUM KAYDI] 12.03.2026 21:41:07 | villager | isim: Demirci Ali | dunya: world | konum: 214, 68, -1032 | olduren: Vesen
```

- **Öldüren, ölüm olayından değil son alınan hasardan** çözülür. Bukkit yalnızca
  son vuruş bir oyuncunun eliyle geldiyse bir "öldüren" verir; bir zombinin
  bitirdiği, çatıdan itilerek düşen ya da birinin kurdunun parçaladığı bir
  köylü aksi hâlde "bilinmiyor" olarak kaydedilirdi. Evcil bir hayvanın vuruşu
  **sahibinin adıyla** yazılır.
- **Vahşi bir kurt kayda girmez, birinin evcilleştirdiği kurt girer.** Tepedeki
  kurt yabanıl hayattır; üsteki kurt mülktür, ve bu kaydın var olma sebebi tam
  olarak bu ayrımdır.
- **Tabloya değil konsola yazılır.** Sorulan soru ("herkesin kullandığı tüccarı
  kim öldürdü") saatler sonra, bir kez, log dosyasına bakılarak sorulur — ve
  `grep` bunu zaten mükemmel yapar. Bir tablo; şema, göç, saklama politikası ve
  okunacak bir ekran isterdi.

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

## Kabiliyet HUD kanalı (`hmmo:hud`)

Faz 22'den beri eşlik mod'u ikinci bir kanal daha dinler. `hmmo:hud`
**tek yönlüdür**: sunucu söyler, istemci dinler. HUD'un sunucuya
söyleyebileceği, sunucunun zaten bilmediği bir şey yoktur; istemcinin
konuşabildiği bir kanal ise doğrulanması gereken bir kanaldır.

| Alan | Ne taşır |
|---|---|
| protokol sürümü + mesaj türü | `0` temizle, `1` kabiliyet |
| kabiliyet id'si, görünen adı, simge materyali | rozeti çizmek için |
| RGB renk | kabiliyetin `gorsel.renk` değeri |
| kalan / toplam milisaniye | geri sayım (`-1` = yaratıcı modda süresiz) **ve sönme** |
| yoğunluk (0-100) | kabiliyetin kademesinden türetilir: 10/20/35/50 → %25/%45/%70/%100 |
| bayraklar | bit 0: düşmanları parlat, bit 1: kenar nabız gibi atsın, bit 2: parlama mesafeye göre değişsin |
| kombo satırı + basamak | sunucunun **render ettiği düz metin** ve 0-6 arası basamak |

**Kombo satırının metni sunucudan gelir, mod'dan değil.** Cümleler Türkçedir ve
oyuncunun okuduğu her metin gibi `messages.yml`'de durmalıdır; kimsenin
düzenleyemeyeceği bir mod, altı cümleyi saklamak için en kötü yerdir. Mod
yalnızca nereye çizeceğine ve ne kadar sarsacağına karar verir.

**Protokol Faz 23'te sürüm 2'ye çıktı** (sönme, nabız, kombo ve mesafeye göre
parlama için) ve **geriye dönük uyumlu değildir** — ama buradaki başarısızlık
ucuzdur: 1.x mod'undaki bir istemci sürüm baytını tanımaz, hiçbir şey çizmez,
çökmez ve kapıda bırakılmaz.

Mesaj **durumsuzdur**: her paket bütün resmi taşır ve en yenisi kazanır.
Kabiliyet çalışırken sunucu saniyede bir tekrar gönderir, dolayısıyla geç
katılan, bir paket kaçıran ya da yeniden bağlanan istemci bir saniye içinde
kendini düzeltir. Sunucu susarsa istemci birkaç saniye sonra HUD'u kendiliğinden
kapatır — bir bağlantı kopması ana menüde kırmızı bir ekran kenarı bırakmaz.

**Bu kanal hiçbir oyun kuralı taşımaz.** Paketleri hiç almayan bir istemci
(eski bir mod sürümü, doğrulamayı bir şekilde geçmiş bir oyuncu) aynı oyunu
daha sade bir ekranla oynar. `modlar.yml`'deki `minimum-versiyon` bu yüzden
**bilerek 1.0.0'da bırakılmıştır**: yeni sürüm oyuncunun kurulum uygulamasına
ulaşmadan minimumu yükseltmek, dürüstçe kurulum yapan oyuncuları kapıda
bırakırdı.

## Test

`./gradlew test`, hızlı birim/yapılandırma test paketini çalıştırır (harici
servis gerektirmez; gerçek bir SQLite dosyasına karşı bir göç/migration
çalıştırma kontrolü dahildir). `./gradlew integrationTest` ayrıca
Testcontainers üzerinden gerçek MySQL, MariaDB ve PostgreSQL konteynerlerine
karşı her gönderilen veritabanı göçünü ve temsili bir repository okuma/yazma
örneklem setini çalıştırır — bu, çalışan bir Docker daemon'u gerektirir ve
bilinçli olarak `build`/`test`/`check`'ten hariç tutulmuştur; bu yüzden ayrı
ve açıkça çalıştırılmalıdır.

Hızlı paket, yapılandırma dosyalarının kendisini de doğrular ve bunların bir
kısmı **tasarım kararlarını zorlayan** testlerdir:

- Her `MessageKey` sabiti ile `messages.yml` **birebir** eşleşmeli (iki yönde
  de fazlalık olamaz) ve her `<simge:...>` gerçek bir ikon adı olmalı.
- **Her sınıf tam olarak dört kabiliyete ve tam olarak 10/20/35/50
  basamaklarına sahip olmalı.** Kabiliyet ekranı dört sabit kaide çizer;
  beşinci bir kabiliyet hiç görünmez, üçüncüsü boşluk bırakır — ikisi de ancak
  biri ekranı açtığında fark edilirdi.
- Her kabiliyetin becerisi, kendi sınıfının beslediği becerilerden biri olmalı.
- Her `QuestDeed` en az bir görev tarafından kullanılmalı (kimsenin
  tetiklemediği bir dinleyici, bitirilemeyen bir görev demektir) ve her beceri
  en az bir görev tarafından eğitilmeli.
- Günlük/haftalık havuzlar, o dönem atanan görev sayısından küçük olamaz.
- **Rehberin hiçbir satırı kesilmemeli.** Eşya açıklaması alt satıra kaymaz;
  `rehber.yml`'deki fazla uzun bir satır ancak bir oyuncu üstüne geldiğinde
  fark edilirdi, bu yüzden derleme zamanında ölçülür.
- Her kabiliyetin **yalnızca kendi etki türünün istediği alanları** olmalı:
  blok başına enerji yalnızca süpürme kabiliyetlerinde, `hiz-seviyesi` yalnızca
  Kan Yemini'nde, `hayvan-sayisi` ve `enerji-kalp-basi` yalnızca koruyucu
  çağrısında.
- **Dört kalıcı prestij ödülünün de pozitif bir değeri olmalı.** Ekran dördünü
  de koşulsuz gösterir; değeri olmayan bir ödül, ne yaptığı konusunda yalan
  söyleyen bir düğme olurdu.

Şu an **43 test sınıfında 124 test** vardır ve hepsi geçer.

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
