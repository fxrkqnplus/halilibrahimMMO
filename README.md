# HalilIbrahimMMO

Türk Minecraft topluluğu için geliştirilmiş, özgün bir MMORPG ilerleme eklentisi
(Paper 26.2 / Minecraft 26.2). Ekonomi yoktur — tüm ilerleme seviye, beceri XP'si,
prestij, görevler, koleksiyonlar ve başarılar üzerinden işler.

Bu dosya **iki bölümden** oluşur:

- **[Bölüm 1 — Oyuncu Rehberi](#bölüm-1--oyuncu-rehberi)**: sunucuya giren bir
  oyuncunun bilmesi gereken her şey. Komutlar, beceriler, sınıflar,
  kabiliyetler, görevler, zenginlik, bosslar, ev/takas, unvanlar.
- **[Bölüm 2 — Teknik Bölüm](#bölüm-2--teknik-bölüm)**: sunucu yöneticisi ve
  geliştirici için. Kurulum, derleme, veritabanı, yapılandırma dosyaları,
  entegrasyonlar, test.

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
| `/menü` | `/menu` | Ana menü (dört kategori + kişisel başlık + Eklenti Hakkında) |
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
  Aynı denetim **Ağaç Kırıcı kabiliyeti** için de geçerlidir; bina üzerinde
  kullanıldığında kabiliyet harcanmaz, kurulu kalır ve sizi uyarır.

  **Kendi diktiğiniz ağaçlarda da çalışır.** Fidan siz koyduğunuz için
  "oyuncunun koyduğu blok" olarak kaydediliyor, ve fidan büyüdüğünde kök kütük
  tam o noktada duruyordu — yani ektiğiniz her ağaç, gövdesinin geri kalanı XP
  verse bile devrilmeyi reddediyordu. Artık bir fidan ağaca dönüştüğü anda o
  kayıt siliniyor: büyüttüğünüz ağaç, vahşi bir ağaçtan hiçbir şekilde ayırt
  edilmiyor. (Aynısı mantar ve bambu için de geçerlidir.) Ağaç büyütmek zaten
  gerçek bir fidan, gerçek zaman ve gerçek alan harcar; kısır döngü değildir.
- **Geniş kazı** (kazıcılık 75) — kazıcılığa ait bir bloğu kırdığınızda
  çevresindeki 3x3 alandaki uygun bloklar da (en fazla 8) birden düşer. Kürek
  işinin tamamı bu tek özellikte toplanır.
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

**Her sınıfın tam olarak dört kabiliyeti vardır** ve hepsi aynı basamaklarda
açılır: **sınıf seviyesi 10 → 20 → 35 → 50**. Basamak yükseldikçe kabiliyet
güçlenir ve bekleme süresi uzar; 50. seviyedeki kabiliyet sınıfın "son sözü"dür
— en güçlü etki, en uzun bekleme (10 dakika).

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
kullanılamaz. Açılma koşulu **sınıf seviyesidir** (beceri seviyesi değil), ve
her sınıfta **dört kabiliyet** aynı dört basamakta açılır: **10 → 20 → 35 →
50**. Sıra ilerledikçe etki büyür, bekleme süresi uzar.

| Sınıf | Kabiliyet | Sv. | Enerji | Bekleme | Etki |
|---|---|---|---|---|---|
| Tabiat Vârisi | Toprak Sezgisi | 10 | 20 | 1 dk 30 sn | Kırdığın bloklardan %35 ihtimalle ekstra düşme |
| Tabiat Vârisi | Filiz Patlaması | 20 | 25 | 2 dk 30 sn | 7 blok yarıçapındaki tüm ekinleri anında olgunlaştırır |
| Tabiat Vârisi | Sürü Çağrısı | 35 | 30 | 4 dk | 10 blok yarıçapındaki tüm yetişkin hayvanları yem harcamadan çiftleşme moduna sokar |
| Tabiat Vârisi | **Ağaç Kırıcı** | 50 | 10 + kütük başına 5 | **10 dk** | Bağlı tüm kütükleri (en fazla 64) birden keser |
| Amansız Savaşçı | Gece Görüşü | 10 | 15 | 1 dk 30 sn | 90 saniye karanlıkta net görüş |
| Amansız Savaşçı | Avcı Odağı | 20 | 25 | 2 dk 30 sn | PvE hasarı %50 artar (vuruş başına enerji tüketir) |
| Amansız Savaşçı | Yıkıcı Darbe | 35 | 30 | 4 dk | Ayağının altında şok dalgası, 5 blok yarıçapı, 11 ham hasar |
| Amansız Savaşçı | **Kan Yemini** | 50 | 40 | **10 dk** | 45 saniye boyunca yaratıklara verdiğin hasarın %35'i cana döner |
| Mağara Adamı | Cevher Sezgisi | 10 | 25 | 1 dk 30 sn | Kırdığın bloklardan %30 ihtimalle ekstra düşme |
| Mağara Adamı | Damar Kırıcı | 20 | 10 + blok başına 4 | 2 dk 30 sn | Bağlı aynı cevher damarını (en fazla 24 blok) birden kazar |
| Mağara Adamı | Maden Feneri | 35 | 30 | 5 dk | 36 blok yarıçapındaki cevherleri 40 sn parlatır |
| Mağara Adamı | **Kaya Patlaması** | 50 | 15 + blok başına 3 | **10 dk** | Kırdığın cevherin çevresindeki 3x3x3 küpteki bütün cevherler birden düşer |

**50. seviyedeki kabiliyet sınıfın son sözüdür**: en güçlü etki, en uzun
bekleme. On dakikada bir kullanılan bir şeyin gerçekten bir şey yapması
gerekiyordu.

### Nasıl kullanılır

1. `/kabiliyet` menüsünden bir kabiliyet **kuşanın**. Ekran dört kabiliyeti
   **eşit aralıklı dört kaide** hâlinde gösterir ve her kaidenin altında o
   kademenin numarası ile gerekli seviye yazar (ulaştığınız kademeler yeşil).
   Böylece ekran bir liste değil, sınıfınızın ilerleme çizgisidir. Her
   kabiliyetin konusuna uygun kendi simgesi vardır; **kuşanılı olan büyülü
   (parlayan)** görünür, seçildiğinde kısa bir ses efekti çalar ve sohbete onay
   mesajı gelir. Menü, sınıfınızın kendi renginde çerçevelenir.

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
sesi, **yankı sesi** (dört tick sonra biraz daha tiz çalan ikinci vuruş) ve
partikül gösterisi vardır. Gösterinin **şekli** partikül türünden ayrı bir
ayardır: `PATLAMA` (saçılma), `HALKA` (genişleyen halkalar), `SARMAL` (yukarı
sarılan helis) ya da `KUBBE` (baş üstünde kubbe). Aynı partikül sarmalda kılıç
savurması, halkada şok dalgası okunur — iki kabiliyet aynı partikülü paylaşsa
bile birbirine benzemez.

Farklı bir kabiliyete geçmek kısa bir bekleme süresine tabidir (varsayılan
2 dakika); aynı kabiliyeti yeniden seçmek serbesttir. Henüz açılmamış
kabiliyetler menüde kilitli gösterilir.

**Kaya Patlaması** (Mağara Adamı 50) kurulu bir kabiliyettir: kurduktan sonra
kırdığınız ilk uygun cevherin çevresindeki **3x3x3 küpteki** bütün cevherler
birden düşer. Damar Kırıcı yalnızca aynı türden bağlı blokları alır; Kaya
Patlaması aradaki taşa bakmadan küpün içindeki her cevheri alır — madencilik
hattının en ağır aleti budur.

**Kan Yemini** (Amansız Savaşçı 50) 45 saniye boyunca bir yaratığa verdiğiniz
hasarın %35'ini cana çevirir. Avcı Odağı bir vuruşun ne kadar sert olduğunu
değiştirir, Kan Yemini ise bir avın ne kadar sürebileceğini — ikisinin
birbirinden farkı budur. Canınızın üstüne çıkamaz ve yalnızca yaratıklara karşı
çalışır.

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

**Filiz Patlaması** çevrenizdeki 7 blok yarıçapındaki bütün olmamış ekinleri
tek seferde hasada hazır hâle getirir — bir kova kemik tozu gibi, ama hiç
kemik tozu harcamadan. Zaten olgunlaşmış ekinlere dokunmaz, dolayısıyla size
kaç bitkinin gerçekten büyüdüğünü söyler.

**Sürü Çağrısı** çevrenizdeki 10 blok yarıçapındaki bütün **yetişkin**
hayvanları tek bir yem harcamadan çiftleşme moduna sokar. Yavrular ve zaten
çiftleşme modundaki hayvanlar atlanır, dolayısıyla aynı sürüye tekrar
kullanmak bir işe yaramaz.

Kabiliyetin kendisi **2 saniye** sürer (hem normal hem yaratıcı modda) ve
kendiliğinden kapanır: çağrı gittikten sonra açık kalmasının bir işlevi yok.
Hayvanların çiftleşme penceresi bundan bağımsızdır — vanilla'daki normal
süresi neyse odur, yani sürünün eşleşmeye vakti olur. Bekleme süresi
**4 dakikadır**.

### Enerji

Enerji, kaydedilmeyen, oyuncu başına bir havuzdur (varsayılan maksimum 100;
sınıf ve kilometre taşı bonuslarıyla artar). Üç kuralı vardır ve üçü birlikte
enerjinin ne işe yaradığını belirler:

1. **Enerjiyi yalnızca kabiliyetler harcar.** Başka hiçbir şey — koşmak,
   savaşmak, blok kırmak — enerjiye dokunmaz.
2. **Bir kabiliyet açıkken enerji hiç yenilenmez.** Kabiliyet bittikten sonra
   saniyede 3 puan dolmaya başlar (boş bardan doluya yaklaşık 33 saniye).
3. **Süpürme kabiliyetleri blok başına ödeme yapar.** Ağaç Kırıcı düşen her
   kütük için 5, Damar Kırıcı her blok için 4, Kaya Patlaması her blok için 3
   enerji harcar. Enerjiniz biterse süpürme **orada durur** ve size kaç blokta
   durduğu söylenir.

Yani "bir seferde kaç kütük devirirsin" sorusunun cevabı blok sınırı değil,
**enerji barınızdır**. 100 enerjilik bir barla Ağaç Kırıcı yaklaşık 18 kütük
alır; maksimum enerji bonuslarınız arttıkça bu sayı da artar.

**Süpürme aleti yer.** Düşen her blok için elinizdeki alet 1 dayanıklılık
kaybeder — o blokları elle kırsaydınız ödeyeceğiniz bedelin aynısı. Aynı kural
kilometre taşıyla açılan ağaç devirme ve geniş kazı için de geçerlidir.
Dayanıklılık büyüsü (Unbreaking) normal şekilde çalışır.

Eylem çubuğunda enerji her zaman `mevcut/maksimum` biçiminde görünür; bir
kabiliyet çalışırken kabiliyetin kendi geri sayımının yanında okunur.

---

## Profil paneli (sidebar)

Ekranın sağında kalıcı, renklendirilmiş bir bilgi paneli. Yukarıdan aşağıya:

1. **Oyuncu adı** — varsa kuşanılan unvan kendi rengiyle isimden önce
   ("Oyuncu: [Unvan] Ad").
2. **Güç, prestij ve zenginlik** — üçü tek satırda: altın Güç, açık mor
   prestij, mavi Zenginlik Puanı.
3. **Sınıf ve sınıf seviyesi** — tek satırda, XP ilerlemesiyle birlikte.
4. **Kabiliyet** — çalışan bir kabiliyet varsa adı ve kalan süresi, yoksa
   seçili olan, o da yoksa "Kabiliyet seçilmedi".
5. **Ev / Takas durumu** — ikisinden biri açıldıktan sonra görünür; evinizin
   adını (ya da "Ayarlanmadı") ve takasın açık olup olmadığını gösterir.
   **Bir takas sürerken** hemen altında "Takas açık: <oyuncu>" satırı çıkar.
6. **Başlangıç Görevleri** — yalnızca tamamlanmamış olanlar varken; hepsi
   bitince bu bölüm tamamen kaybolur.
7. **Görevler** — tek başlık altında, türü satırın **kendi renginden**
   okunur: **günlük sarı**, **haftalık altın**, **kalıcı camgöbeği**.
   Tamamlanmamış görevler ilerlemeleri ve XP ödülleriyle, tamamlananlar
   ✔ işaretiyle listelenir. Görevler panelde **kısa adlarıyla** yazılır
   ("40 kütük kır", "8 enderman öldür" — bkz. Görevler bölümü).
8. En altta `/panel` hatırlatması.

**Zenginlik Puanı kısaltılarak yazılır**: 12 600 yerine `12.6K`, 1 050 000
yerine `1.05M`. Sekiz haneli bir sayıyı panelde rakam rakam saymak zorunda
kalmazsınız. Aynı kısaltma liderlik tablosunda ve sıralama duyurularında da
kullanılır.

**Minecraft bu panelde en fazla 15 satır gösterir** ve fazlasını sessizce
atar, bu yüzden kırpmayı eklentinin kendisi yapar. Satırlar bu tavana göre
bilinçli olarak birleştirilmiştir: Güç/prestij/zenginlik tek satır, sınıf ve
sınıf seviyesi tek satır, üç görev türü tek başlık. (Sınıfınızın açık
kabiliyetleri de bu yüzden tek tek listelenmez; tam listeyi `/kabiliyet`
ekranı gösterir.)

Panel yine de taşarsa **önce süsleme atılır** — ayırıcı çizgiler ve en alttaki
`/panel` satırı — çünkü onlar bilgi taşımaz. Bir görev satırı, ancak başka
hiçbir şey atılamıyorsa gider. Yani her kategoriden üç görev sabitlediğinizde
dokuzunun da görünmesi beklenir. **Panelde hangi görevlerin görüneceğine siz
karar verirsiniz**: bkz. "Görevleri panele sabitleme".

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
- **Güç ve sıralaması**, **Zenginlik ve sıralaması**.
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

- Üstte **sunucu adı**, tanıtım satırı ve **çevrimiçi / maksimum** oyuncu
  sayısı.
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
Karşılığında kalıcı olarak her prestij rankında +%25 XP kazanımı (hem beceri
hem sınıf XP'sinde) ve seçili sınıfa özgü kalıcı bir bonus (can/hasar/enerji)
kazanılır. Prestijli oyuncular liderlik tablosunda ham seviyeden bağımsız
olarak her zaman önde gösterilir. Prestij yapmak sunucu genelinde bir duyuru,
bir havai fişek ve ses efektiyle kutlanır; varsayılan maksimum prestij
sayısı 10'dur.

---

## Görevler

Toplam **85 görev** vardır: 43 kalıcı, 23 günlük, 19 haftalık. `/görev`,
solunda dört sekme bulunan bir görev panosu açar. Aynı anda tek bir bölüm
gösterilir, o bölüme de 42 slot ayrılmıştır:

- **Günlük Görevler** — 23 görevlik havuzdan her gün rastgele atanan 3 görev.
- **Haftalık Görevler** — 19 görevlik havuzdan her hafta rastgele atanan 2 görev.
- **Kalıcı Görevler** — tek seferlik, hiç sıfırlanmayan, herkeste ortak 43 görev.
- **Tamamlanan Görevler** — bu dönemde bitirdikleriniz.

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

**Büyük sayılar kısaltılarak yazılır**: `12.6K`, `712.2K`, `1.2M`, `1.05M`.
Zenginlik puanları normal oynayışta yedi-sekiz haneye çıkıyor ve `12600000`
gibi bir sayı okunmaz, sayılır.

### Sıralama duyuruları

Zenginlik liderlik tablosu **belirli aralıklarla** (varsayılan 25 dakika)
yeniden okunur ve bir önceki okumayla karşılaştırılır. Bir sıra değişmişse
sohbete **turda en fazla bir** duyuru düşer:

- **Birini geçtiyseniz** iki isim de anılır: *"Vesen aman tanımıyor! Kaan,
  Vesen adlı oyuncunun gölgesinde kalarak 3. sıraya düştü."*
- **Birkaç basamak birden tırmandıysanız** yükselişin kendisi anlatılır:
  *"Vesen önünde engel tanımıyor! Yükselişlerinin ardı arkası kesilmeyerek
  zenginlikte 2. sıraya ulaştı."*

Her iki şekil için de **15 farklı metin** vardır ve her seferinde rastgele biri
seçilir; zamanlayıcıyla tekrarlanan tek bir sabit cümle bir süre sonra haber
olmaktan çıkar. Aralık ve metinlerin tamamı yapılandırılabilir. Turda yalnızca
**en dikkat çekici tek değişiklik** duyurulur (en çok basamak tırmanan; eşitlik
varsa en yükseğe varan), çünkü yirmi beş dakikada birkaç kişi kıpırdar ve
hepsini birden yazmak tabloyu duvar yazısına çevirir.

Duyurular yalnızca **tablonun görünür kısmı** (varsayılan ilk 10) için
çalışır; kimsenin göremediği bir sıra değişikliği duyurulmaya da değmez.

Bütün değerler, kilometre taşları, duyuru aralığı ve duyuru metinleri
`zenginlik.yml` ve `messages.yml` üzerinden değiştirilebilir.

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
**Zenginlik Puanı** ve **her becerinin kendi tablosu** olmak üzere on bir
tabloya açılır. Aynı tablolara `/menü`'nün Topluluk kategorisinden de
ulaşılabilir. Sonuçlar her açılışta veritabanını sorgulamak yerine kısa,
yapılandırılabilir bir süre için önbelleğe alınır (`leaderboards.yml`).

**Her tablonun ne ölçtüğünü söyleyen kendi simgesi vardır**: Güç netherite
külçesi, prestij nether yıldızı, görevler harita, koleksiyon ender sandığı,
zenginlik elmas — beceri tabloları ise o becerinin `/beceri` listesinde
gördüğünüz **kendi simgesini** taşır. Her tablonun üstüne geldiğinizde tam
olarak neyi sıraladığı yazar.

Bir tabloyu açtığınızda **ilk üç sıra podyum olarak** görünür (altın, demir ve
bakır blok) ve **kendi satırınız yeşil renkte işaretlenir** — herkesin ilk
baktığı satır zaten kendisininkidir.

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

**Her iki ekran da birebir aynı düzendedir**: üstte kendi kafanızı taşıyan ve
kaç tanesini açtığınızı gösteren bir başlık (ör. "Açılan unvan: 7/19"), altında
katalog, en altta geri düğmesi. Aynı sistemin iki örneği oldukları için
aralarında gezinirken düzeni yeniden öğrenmeniz gerekmez.

**Henüz açmadığınız bir ünvanın ya da kozmetiğin adı gösterilmez.** Yerinde
gri bir yer tutucu durur ve üstüne geldiğinizde yalnızca **onu neyin açacağı**
yazar — "Gereken: 50. prestij", "Gereken: madencilik becerisinde 50. seviye",
"Gereken: 250K zenginlik puanı" gibi. Bir ünvan kazanılıp kuşanılan bir
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
| `/rtp` ile inmek | Işınlanma sesi + ters portal parçacıkları |
| Bir dünya bossunun ölmesi | Herkese zafer sesi; **cesedin başında** patlama, totem ve ışık huzmesi — *nerede* olduğunu savaşa gidenlere söylemek için |
| Tek kişi geceyi uyuması | O dünyadaki herkese çan sesi + duyuru |

Kabiliyetlerin kendi gösterileri de `abilities.yml`'de ayrıntılıdır: her
kabiliyetin rengi, ana sesi, dört tick sonra çalan **yankı sesi** ve partikül
**şekli** (saçılma / genişleyen halkalar / yukarı sarılan helis / kubbe) ayrı
ayrı ayarlanır.

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
çalışan eklenti/Paper sürümünü görüntüleme, **beklemeden bir dünya bossu
çağırma** (türünü, nadirliğini ve yerini yine sistem seçer; onay mesajı boss
gerçekten doğduktan sonra gelir, yer bulunamazsa bunu açıkça söyler), çevrimiçi
oyuncuları gezip herhangi birinin salt okunur profil/sınıf/beceri özetini
görüntüleme, ve **kayıtlı her oyuncuyu** (çevrimdışı olanlar dahil) listeleyip
beceri/sınıf seviyesini ve XP'sini tıklamalarla ayarlama ya da oyuncunun
sınıfını zorla değiştirme (bu da, oyuncunun kendi değiştirmesi gibi, seçili
kabiliyetini kuşanmasından çıkarır). Bir seviye atlamaya yol açan her müdahale, hedef
oyuncu çevrimiçiyse organik bir seviye atlamayla birebir aynı geri bildirimi
(cübuk, ses, "yeni kabiliyet açıldı" ekran yazısı, panel güncellemesi)
tetikler. Her yönetici müdahalesi Türkçe bir sunucu günlüğü kaydı bırakır.

### Zenginlik ayarlama: paneldeki tek "sohbete yaz" alanı

Oyuncu düzenleme ekranındaki **Zenginlik** satırına tıkladığınızda ekran
kapanır ve panel sizden **sayıyı sohbete yazmanızı** ister. Yazdığınız satır
sohbete düşmez, doğrudan panele gider; vazgeçmek için `iptal` yazın, 60 saniye
içinde cevap vermezseniz soru kendiliğinden düşer.

Bu, paneldeki **tek** serbest metin alanıdır ve bilerek öyledir: zenginlik
totalleri rahatlıkla milyonlara çıkar, tıkla-arttır satırlarıyla 4 812 500
yazmak bir düzeltme aracı değil bir işkencedir.

İki şeyi bilerek yapar:

- **Ekler değil, üzerine yazar.** Bir rollback'i düzelten personel sayının kaç
  *olması gerektiğini* bilir, ne kadar ekleyeceğini değil.
- **Eklentideki tek zenginlik düşürme yoludur.** Oyun içi hiçbir yol bu sayıyı
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
- [`clientmod/`](clientmod) — Zorunlu Fabric eşlik mod'u. **Kök derlemeye dâhil
  değildir**, kendi Gradle wrapper'ı vardır (bkz. "İki ayrı derleme zinciri").

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

## Veritabanı

SQLite, sıfır yapılandırma gerektiren varsayılan seçenektir. MySQL, MariaDB ve
PostgreSQL, `config.yml` (`veritabani.tur`) üzerinden desteklenir. Tüm JDBC
sürücüleri ve HikariCP, Paper'ın `PluginLoader` mekanizması üzerinden çalışma
zamanında çözümlenir — eklenti jar'ına hiçbir şey gömülmez (shade edilmez).

Tüm tablolar `hmmo_` ön ekiyle oluşturulur, böylece birden fazla eklentinin aynı
şemayı paylaştığı ortak veritabanlarında çakışma olmaz. Şema değişiklikleri
sürüm numaralı göç (migration) dosyalarıyla yapılır ve `hmmo_schema_version`
tablosunda takip edilir; gönderilmiş bir göç asla değiştirilmez veya
numarası yeniden verilmez.

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
| `prestige.yml` | Prestij şartları ve maksimum prestij sayısı |
| `leaderboards.yml` | Tablo başına gösterilecek oyuncu sayısı ve önbellek tazeleme aralığı |
| `titles.yml` / `cosmetics.yml` | 19 unvan / 14 kozmetik: görünen ad, simge, açılma koşulu, sohbet stili |
| `feedback.yml` | Cübuk/başlık süreleri, isimli ses efektleri, boss doğuş sesi listesi |
| `zenginlik.yml` | Zenginlik Puanı değerleri, kilometre taşları, sıralama duyurusu aralığı |
| `bosslar.yml` | Boss doğuş aralığı ve mesafesi, beş nadirlik, arketipler ve isim havuzları |
| `sunucu.yml` | Tek kişi uykusu ve TAB listesi davranış anahtarları (metinler `messages.yml`'de) |
| `modlar.yml` | Zorunlu/yasaklı Fabric mod listesi ve doğrulama ayarları |

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
