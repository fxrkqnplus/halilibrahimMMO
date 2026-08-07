# HalilIbrahimMMO

Türk Minecraft topluluğu için geliştirilmiş, özgün bir MMORPG ilerleme
eklentisi (Paper 26.2 / Minecraft 26.2). **Ekonomi yoktur** — para, coin, banka
ve Vault hiçbir yerde geçmez; tüm ilerleme seviye, beceri XP'si, prestij,
görevler, koleksiyonlar ve saygınlık üzerinden işler.

Sürüm: **1.0.0-beta** · Eşlik istemci mod'u: **hmmo-client 1.4.0** (zorunlu)

Bu dosya iki bölümden oluşur:

- **[Bölüm 1 — Oyuncu Rehberi](#bölüm-1--oyuncu-rehberi)** — sunucuya giren bir
  oyuncunun bilmesi gereken her şey.
- **[Bölüm 2 — Teknik Bölüm](#bölüm-2--teknik-bölüm)** — sunucu yöneticisi ve
  geliştirici için kurulum, derleme, yapılandırma ve bakım.

Oyunun içinde aynı anlatımın kısa hâli vardır: **`/yardım`** sunucunun kendi el
kitabını açar. Mimari kararların iç kaydı `PROJECT_MEMORY.md` dosyasındadır.

---

# Bölüm 1 — Oyuncu Rehberi

## Başlarken

1. **`/mmo`** yaz. MMO sistemi ilk girişte kapalıdır ve açılmadan hiçbir şey
   ilerlemez — bu kasıtlıdır, sisteme girmeyi sen seçersin.
2. Açılan ekrandan **sınıfını seç**. En önemli kararın budur: hangi
   kabiliyetleri açacağını ve hangi becerilerden ekstra XP alacağını belirler.
3. Sağdaki **paneli** oku. Güç, prestij, saygınlık, sınıf, kuşandığın kabiliyet
   ve takip ettiğin görevler orada.
4. Panelin **Başlangıç** bölümündeki dört küçük görevi bitir. Hepsi tek
   seferliktir, sınıf XP'si verir ve bitince bölüm panelden kalkar.

## Komutlar

Neredeyse her komut bir GUI ekranı açan kısayoldur; sohbete liste basan ya da
sohbetten değer değiştiren komut yoktur. Türkçe karakterli her komutun **ASCII
yazımı da çalışır** (`/menü` ≡ `/menu`).

| Komut | Takma ad | Ne açar |
|---|---|---|
| `/menü` | `/menu` | Ana menü |
| `/yardım` | `/yardim`, `/bilgi`, `/rehber` | Oyun içi rehber (12 bölüm) |
| `/mmo` | — | MMO'yu aktifleştirir, sınıf tanıtımını açar |
| `/profil` | — | Kendi profilin |
| `/profil <oyuncu>` | — | Başka bir oyuncunun profili + takas daveti |
| `/sınıf` | `/sinif` | Sınıf ekranı ve odak ağacı |
| `/kabiliyet` | — | Kabiliyet menüsü |
| `/beceri` | `/beceriler` | Altı becerinin özeti |
| `/madencilik`, `/odunculuk`, `/kazıcılık`, `/avcılık`, `/tarımcılık`, `/hayvancılık` | ASCII yazımları | O becerinin detay ekranı |
| `/görev` | `/gorev` | Görev panosu |
| `/koleksiyon` | — | Koleksiyonlar |
| `/günlük` | `/gunluk` | Macera günlüğü |
| `/prestij` | — | Prestij şartları ve onayı |
| `/liderlik` | — | Liderlik tabloları |
| `/ünvan` | `/unvan` | Unvan gardırobu |
| `/kozmetik` | — | Kozmetik gardırobu |
| `/çanta` | `/canta`, `/backpack` | Bulut sandığı |
| `/ev` | `/home` | Evine ışınlanır (`/ev ayarla <isim>`, `/ev sil`) |
| `/takas <oyuncu>` | `/trade` | Takas isteği (`kabul`, `ret`, `iptal`) |
| `/rtp` | `/rastgele` | Rastgele güvenli bir noktaya ışınlanır |
| `/harita` | `/map` | Canlı harita adresini verir |
| `/panel` | — | Sağdaki paneli gizler/gösterir |
| `/credits` | `/yapımcı`, `/halilibrahimmmo` | Emeği Geçenler |
| `/haliladmin` | — | Yönetici paneli (izin gerekir) |

Ekranda görünen her süre saat/dakika/saniye olarak yazılır: "130 saniye" değil,
"2 dakika 10 saniye".

---

## İlerleme temeli

**Güç**, oyun içindeki genel seviyendir ve kendi XP kaynağı **yoktur**: altı
becerinin seviyelerinin toplamına, prestij yaptıkça kalıcı olarak biriken Güç
eklenerek hesaplanır. Bu yüzden prestij Güç'ünü asla düşürmez.

İki kural her XP kaynağı için geçerlidir:

- **Yaratıcı ve İzleyici modda** hiçbir beceri, sınıf veya görev XP'si
  kazanılmaz.
- **Yalnızca dünyanın kendi ürettiği bloklar sayılır.** Bir oyuncunun
  yerleştirdiği blok kırıldığında ne XP verir, ne görev ilerletir, ne de
  üzerinde kabiliyet çalışır. Kayıt chunk'ın kendi verisinde durur; sunucu
  yeniden başlasa da unutulmaz.
  **Ekinler tek istisnadır** — tarım zaten "ek ve biç" demektir: kendi ektiğin
  buğday da tam olgunlaştığında XP verir, olmamış ekin hiçbir şey vermez.

---

## Beceriler

Altı beceri, her biri 1–100 arası bağımsız seviyelenir. Her blok, yaratık ve iş
**gerçek değerine göre** XP verir.

| Beceri | Nasıl kazanılır | En az → en çok XP |
|---|---|---|
| **Kazıcılık** | Toprak, kum, çakıl, kil kazmak | 30 → 90 |
| **Odunculuk** | Kütük kesmek | 45 → 105 |
| **Madencilik** | Cevher kırmak | 12 (kömür) → 300 (kadim kalıntı) |
| **Avcılık** | Yaratık öldürmek | 15 (hayvan) → 2400 (Ejderha) |
| **Tarımcılık** | Tarladaki her iş | 15 (çapalama) → 150 (köylü ticareti) |
| **Hayvancılık** | Hayvan yetiştirmenin her adımı | 15 (sağma) → 225 (evcilleştirme) |

Aradaki farklar bilinçlidir: madencilikte redstone 21, altın 60, elmas 135,
zümrüt 180; avcılıkta bir zombi 24, bir Wither İskeleti 90, bir Warden 1800 XP
verir. Listelenmemiş her yaratık en düşük değeri alır — çünkü listelenmemişler
neredeyse her zaman çiftlikte sınırsız üretilebilen hayvanlardır.

**Tarımcılık ve hayvancılık blok kırmakla değil eylemle artar.** Çapalamak,
kemik tozu kullanmak, ekin biçmek, pişirmek, sağmak, kırpmak, çiftleştirmek,
kayışla hayvan taşımak, evcilleştirmek, köylüyle ticaret yapmak — hepsi sayılır.
Kayışla taşıma ödülü sürünün büyüklüğünden bağımsızdır: hayvan başına ödemek
yalnızca sürü yığmayı ödüllendirirdi.

### Kilometre taşları

Her beceri 25/50/75/100. seviyede **kalıcı** bir bonus açar. Kademeler zayıftan
güçlüye sıralanır ve her beceri kendi işine yarayan bir şey verir.

| Beceri | 25 | 50 | 75 | 100 |
|---|---|---|---|---|
| Kazıcılık | %10 çift düşme | +15 enerji | **Geniş kazı** | %30 çift düşme |
| Odunculuk | **Ağaç devirme (48)** | %12 çift düşme | **Ağaç devirme (96)** | %30 çift düşme |
| Madencilik | %8 çift düşme | Acele I | %20 çift düşme | Acele II |
| Avcılık | +1 kalp | Yaratıklara +%5 hasar | +3 kalp | Yaratıklara +%20 hasar |
| Tarımcılık | %8 çift düşme | **%50 otomatik ekim** | %20 çift düşme | **%100 otomatik ekim** |
| Hayvancılık | **%15 yem iadesi** | **%10 ikiz doğum** | %45 yem iadesi | %30 ikiz doğum |

Bir şey *yapan* kilometre taşları:

- **Ağaç devirme** — ağacın **kök** kütüğünü kestiğinde bağlı bütün kütükler
  düşer. Yalnızca gerçek ağaçlarda çalışır: kütüğün altında toprak/yosun/çamur
  olmalı, gövdeye en az 4 yaprak değmeli ve gövde hiçbir yapı malzemesine
  (tahta, merdiven, çit, cam, yün, iş istasyonu) değmemelidir. Ormanın
  ortasındaki evi güvende tutan üçüncü şarttır. Kendi diktiğin ağaçlarda da
  çalışır.
- **Geniş kazı** — **kürekle** kazdığında çevredeki 3x3 alan da düşer (en fazla
  8 blok). Yalnızca kürekle: kazma varken tek blokluk yol açarken sekiz blok
  daha almak ödülün tersidir.
- **Otomatik ekim** — hasat ettiğin **tam olgun** ekin kendini yeniden eker,
  tohum harcamadan.
- **Yem iadesi** — yavru doğduktan *sonra* zar atılır ve çiftleştirme yeminin
  iki adedi birden geri gelebilir.
- **İkiz doğum** — bir çiftleştirme ikinci bir yavru daha verebilir.

Aynı özelliğin iki kademesi toplanmaz, **yükseği geçerlidir**. Bonuslar ölünce
kaybolmaz, yeniden doğunca geri gelir; yalnızca prestij onları sıfırlar.

**Bir şey yapan her kilometre taşı kapatılabilir.** Beceri detay ekranındaki
Kilometre Taşları sayfasından tıklayarak açıp kapatırsın; tercih kaydedilir ve
sunucu yeniden başlasa da unutulmaz. Tek fidan keserken ağacın devrilmesi ya da
tek blokluk yol açarken 3x3'ün gitmesi bazen tam olarak istenmeyen şeydir.
İstatistik bonuslarında (can, hasar, enerji) düğme yoktur — kimse daha az can
istemez.

---

## Sınıflar

Üç sınıf vardır. Her sınıfın bir veya birden fazla **sinerji becerisi** vardır:
o becerilerden kazanılan XP'nin bir kısmı sınıf seviyesine de yansır ve ayrıca
**+%20 beceri XP bonusu** kazandırır.

| Sınıf | Sinerji becerileri | Prestij bonusu | Kabiliyet |
|---|---|---|---|
| **Amansız Savaşçı** | Avcılık | PvE hasarı | 4 |
| **Mağara Adamı** | Madencilik | Maksimum enerji | 4 |
| **Tabiat Vârisi** | Odunculuk, Kazıcılık, Tarımcılık, Hayvancılık | Maksimum can | 4 |

Sınıf seviyesi beceri seviyesinden bağımsız, kendi XP çizgisi olan bir
ilerlemedir; maksimum **50**'dir. Sınıf XP'si sinerji becerilerinden,
görevlerden ve dünya bossu öldürmekten gelir.

Tabiat Vârisi tek başına dört beceriyi besler ve kabiliyetleri de tam olarak bu
dört becerinin her birine bir tane düşer. Diğer ikisi dar ama derin uzmandır.

### Sınıf değiştirmek

`/sınıf` ekranından, ilk seçimdekiyle aynı tanıtım ve onay ekranıyla yapılır.
Onay penceresinde şunlar açıkça yazar:

- **Ayrıldığın sınıf tamamen sıfırlanır**: seviyesi 1'e döner ve o sınıfın odak
  ağacındaki bütün rankler silinir.
- **Buna karşılık o rankler için harcadığın bütün özellik ve kabiliyet puanları
  geri gelir** ve yeni sınıfının ağacına yeniden harcanır.
- Bir daha değiştirmek için **7 gün** beklersin.

Sınıf değiştiğinde seçili kabiliyetin kuşanılmış olmaktan çıkar ve o sırada
çalışan bir kabiliyet varsa sona erer. Kabiliyet bekleme süreleri bilerek
korunur: sınıf değiştirmek bir beklemeyi sıfırlamanın yolu değildir.

### Odak ağacı: iki ayrı puan havuzu

| Havuz | Ne alır | Nasıl kazanılır |
|---|---|---|
| **Özellik Puanı** | Can, PvE hasarı, maksimum enerji **ve bulut sandığı genişletmeleri** | Her 30 Güç seviyesinde 1, her prestijde 4 |
| **Kabiliyet Puanı** | Tek bir kabiliyete özel iyileştirmeler | Her 60 Güç seviyesinde 1, her prestijde 2 |

Tek kural: **özellik puanı her zaman kabiliyet puanının tam iki katı hızda
gelir.** Bir kabiliyet puanı asla bir özellik yeteneğine harcanamaz, tersi de
geçerlidir — kabiliyetlerini güçlendirmek isteyen biri bunu ham
istatistiklerinden vazgeçerek satın alamaz.

**Her iki havuzun da bir üst sınırı vardır**: sınıfının bütün yeteneklerini son
rankına kadar doldurduğunda o havuz artmayı bırakır. Harcanacak yer kalmamışken
sayacın yükselmeye devam etmesi hiçbir şey ifade etmiyordu. Özellik havuzunun
sınırına bulut sandığının iki genişletmesi de dâhildir.

Kabiliyet yetenekleri bir kabiliyetin enerji maliyetini, bekleme süresini, etki
sınırını, etki süresini, etkisinin gücünü ya da blok başına enerjisini
değiştirir. **Menüdeki açıklamalar mutlak değer konuşur, yüzde değil**: bir
yeteneğin üstüne geldiğinde rank başına ne değiştiği, kabiliyetin varsayılanı ve
senin şu anki değerin tek satırda yazar.

---

## Kabiliyetler

Her kabiliyet tam olarak bir sınıfa aittir ve **sınıf seviyesiyle** açılır:
**10 → 20 → 35 → 50**. Basamak yükseldikçe etki büyür, bekleme süresi uzar.
50. seviyedeki kabiliyet sınıfın son sözüdür: en güçlü etki, en uzun bekleme.

| Sınıf | Kabiliyet | Sv. | Alet | Enerji | Bekleme | Etki |
|---|---|---|---|---|---|---|
| Tabiat Vârisi | **Filiz Patlaması** | 10 | Çapa | 25 | 7 dk 30 sn | 7 blok yarıçapındaki ekinler dalga dalga olgunlaşır |
| Tabiat Vârisi | **Sürü Çağrısı** | 20 | Herhangi | 30 | 4 dk | 10 blok yarıçapındaki yetişkin hayvanlar yemsiz çiftleşme moduna girer |
| Tabiat Vârisi | **Doğa Ana'nın Koruyucuları** | 35 | Herhangi | 20 + kalp başına 5 | 4 dk | En yakın 6 hayvan sana saldıranın üstüne koşar; yaralananları enerjinle iyileştirirsin |
| Tabiat Vârisi | **Asil Kurt** | 50 | Herhangi | 40 | 10 dk | **Yalnızca geceleri.** 1 dakika kurda dönüşürsün |
| Amansız Savaşçı | **B Planı** | 10 | Kılıç/Balta | 15 | 2 dk | 5 saniye hiçbir yaratık seni göremez + Görünmezlik + Hız 2 |
| Amansız Savaşçı | **Avcı Odağı** | 20 | Kılıç/Balta | 25 | 2 dk 30 sn | PvE hasarı %50 artar (vuruş başına enerji) |
| Amansız Savaşçı | **Yıkıcı Darbe** | 35 | Kılıç/Balta | 30 | 30 sn | 5 blok yarıçapında şok dalgası, 11 ham hasar |
| Amansız Savaşçı | **Kan Yemini** | 50 | Kılıç/Balta | 40 | 7 dk 30 sn | 45 saniye hasarın %15'i cana döner + Hız 1 |
| Mağara Adamı | **Bulldozer** | 10 | Kazma | 0 + blok başına 3 | 2 dk 30 sn | 1 dakika boyunca kazdığın her blok 3x3x3 küple düşer |
| Mağara Adamı | **Madenci Refleksleri** | 20 | Kazma | 30 | 2 dk | 10 saniye kazmana **+3 Verimlilik** ekler |
| Mağara Adamı | **Kaya Patlaması** | 35 | Kazma | 35 | 5 dk | Maden Bombası bırakır: cevhere ve cana dokunmaz |
| Mağara Adamı | **Maden Feneri** | 50 | Kazma | 30 | 10 dk | 36 blok yarıçapındaki cevherler 40 sn parlar |

Amansız Savaşçı'nın dört kabiliyeti de **kılıçla ya da baltayla** çalışır. Balta
bu oyunda meşru bir savaş silahıdır ve bir avcı odağının bıçağın şekliyle
ilgisi yoktur.

### Nasıl kullanılır

1. `/kabiliyet` menüsünden birini **kuşan**. Ekrandaki her sayı senin sayındır:
   bekleme, süre, enerji ve etki değerleri odak ağacında harcadığın rankler
   işlenmiş hâlde yazar.
2. Kabiliyetin istediği aleti eline al.
3. **Çift sağ tıkla.** İlk tık uyarır, ikincisi çalıştırır; onay için 3 saniyen
   var. Bloğa, boş havaya ya da bir yaratığa tıklamak — üçü de çalışır.

Tek tıkla çalışmamasının sebebi basit: kabiliyetler enerji harcar ve uzun
bekleme süreleri başlatır, sağ tıklamak ise kapı açarken sürekli yaptığın bir
şeydir.

**Kabiliyeti tetiklemeyen tıklamalar:** kendi işi olan bloklar (kapı, sandık,
fırın, örs, tabela, yatak, düğme…) ve elindeki eşyanın kullanıldığı tıklamalar
(blok koymak, kova, kayık, kayış, kemik tozu…). Blokta shift+sağ tık kaçış
yoludur; eşyada değildir — orada **boş havaya** tıkla.

Alet yalnızca **başlatmak** için gerekir; kabiliyet çalıştıktan sonra eşya
değiştirmen onu bozmaz. Farklı bir kabiliyete geçmek 2 dakika bekleme
gerektirir, aynısını yeniden seçmek serbesttir. Ekrandaki **Kabiliyeti Çıkar**
düğmesi kuşandığını bırakır ama hiçbir beklemeyi sıfırlamaz.

**Yaratıcı modda hiçbir sınır yoktur** — kabiliyet sen kapatana kadar açık kalır
ve çift sağ tıklayarak kapatılır. İstisna, işini anında bitiren dört
kabiliyettir (Yıkıcı Darbe, Filiz Patlaması, Sürü Çağrısı, Kaya Patlaması);
onlar orada da kendi normal sürelerini kullanır.

### Asil Kurt

Tabiat Vârisi'nin son sözü ve eklentinin en büyük tek mekaniği. **Yalnızca
geceleri** çağrılabilir; gündüz denersen ne enerji ne bekleme harcanır. Açtığın
anda tüm sunucu üç kez uluma duyar ve bir dakika boyunca gerçekten bir kurt
olursun — herkes seni öyle görür, çünkü model eşlik mod'unda vardır ve yürür,
koşar, zıplar, düşer, çömelir.

**Kazandıkların**

- Hız 2, Dayanıklılık 2, Güç 3, Zıplama 5
- **10 fazladan kalp** (canın da o kadar dolar)
- **20 bloğa kadar düşme hasarı yok**; üstünde blok başına 1 can
- Canavarlar seni göremez, çevredeki kurtlar son vurduğun hedefe saldırır,
  hayvanlar ve köylüler kaçar
- Et düşüren avını yerinde yersin — pişmiş etin doyurduğu kadar açlık
- **Havadayken shift'e basılı tut**: kendini yere fırlatırsın. İnişte 6 blok
  yarıçapında bir sarsıntı olur ve yaratıklar merkeze yakınlıklarına göre hasar
  alıp savrulur. Oyunculara dokunmaz ve dalışın kendisi düşme hasarı vermez.

**Ödediklerin**

- Zırhın çıkar ve süre boyunca giyemezsin (çıkanlar envanterine gider)
- **Suya giremezsin** — canın gider ve ekranda "SUDAN ÇIK!" yazar
- **Slotbarın gider, yerine Kurt Pençeleri gelir.** Slot değiştiremez, eşya
  bırakamaz, envanterine dokunamaz ve yerden bir şey alamazsın. Slotbarın
  güvenle saklanır ve süre bitince geri gelir — sunucu çökse bile, çünkü kayıt
  oyuncunun kendi verisindedir.
- Bekleme süresi 10 dakika

**Sabah olursa**: ekranında **GÜNEŞTEN KAÇ** yazar ve canın azalmaya başlar.
Kabiliyet kendi saatiyle bitmez — gökyüzü ışığının hiç ulaşmadığı bir yere
(mağara, kapalı bir oda) girip **5 saniye** beklemen gerekir. O zaman yavaşça
insana dönersin ve slotbarın geri gelir.

### Çılgınlık: Kan Yemini ve Asil Kurt

İkisi aynı sunumu paylaşır, çünkü ikisi de aynı şeyin iki anlatımıdır: biri
fazla ileri gitmiş bir avcı, diğeri avlayan şeyin kendisi.

- **Ekranın kenarı nabız gibi atar** ve yalnızca sana duyulan bir **kalp atışı**
  çalar.
- **Yaklaştıkça kırmızı parlayanlar** — ama farklı şeyler:

  | | Canavarlar | Diğer moblar | Oyuncular |
  |---|---|---|---|
  | **Kan Yemini** | ✔ | ✔ | ✘ |
  | **Asil Kurt** | ✘ | ✔ | ✔ |

  Kan Yemini bir canavar avcısıdır; hiçbir oyuncu yanmaz, yani yemin insanlara
  doğrultulmuş bir duvar hilesine dönüşmez. Asil Kurt ise dünyanın kaçtığı
  yırtıcıdır: canavarları zaten kendisine kör etmiştir, yanan şey av ve
  insandır.
- **Kombo merdiveni**: kabiliyet açıldığından beri verdiğin **toplam hasara**
  göre kızışan bir satır — öldürdüğün yaratık sayısına göre değil, çünkü bir
  tavukla bir ravager aynı başarı değildir. Altı basamak; basamak yükseldikçe
  yazı büyür ve son ikisi ekranda titrer.
- Son 5 saniyede efektler saniyede bir kademe sönerek biter.

### Diğer kabiliyetler hakkında bilinmesi gerekenler

- **Bulldozer** sürekli bir kabiliyettir: her blok 3 enerji ve kazmandan 1
  dayanıklılık götürür; **enerjin biterse orada kapanır**. Bedrock, spawner,
  ganimet sandığı ve envanteri olan hiçbir bloğa dokunmaz. Açmak bedavadır.
- **Madenci Refleksleri** gerçek büyüdür ve mevcut büyünün üstüne biner
  (Verimlilik 5 kazma → Verimlilik 8). Süre dolunca kazma tam olduğu hâline
  döner — başka slota taşınsa, düşürülse, hatta sunucu yeniden başlasa bile.
- **Kaya Patlaması** normal bir TNT'nin altı katı *hacim* açar (yaklaşık 7 blok
  yarıçapında bir oda), hiçbir oyuncuya ve yaratığa zarar vermez, hiçbir cevhere
  dokunmaz ve çıkan cevherler 5 saniye parlar. Kraterin kenarı bilerek
  tırtıklıdır; kusursuz bir küre patlama gibi değil hata gibi okunur.
- **Maden Feneri** yalnızca **geniş ve üstü kapalı bir madende** kullanılabilir.
  Tarama sana en yakın cevherden başlayarak dışa doğru genişler, yani gözünün
  önündeki damar da duvarın arkasındaki kadar kesin parlar.
- **Doğa Ana'nın Koruyucuları** yalnızca çevrende hayvan varken kullanılabilir —
  çağıracak kimse yoksa enerji de harcamaz, bekleme de başlatmaz. Yaralanan
  koruyucularını enerjinle ayakta tutarsın: **5 enerji = 1 kalp**.
  Evcilleştirilmiş at, kurt, kedi ve köylüler çağrıya katılmaz.
- **Filiz Patlaması**'nın süresi sabit değildir — tarlanın kendisi belirler ve
  son ekin olgunlaştığı anda kendiliğinden kapanır.
- **B Planı** görünmezlik iksirinden başka bir şeydir: sana zaten nişan almış
  her yaratık hedefini kaybeder. Kaybedilmiş bir dövüşten çıkmanın yoludur,
  kazanmanın değil.

---

## Enerji

Kabiliyetlerin tek yakıtıdır. Üç kural:

1. Enerjiyi **yalnızca kabiliyetler** harcar. Koşmak, savaşmak, blok kırmak
   enerjiye dokunmaz.
2. Bir kabiliyet açıkken enerji **hiç yenilenmez**; kabiliyet bitince saniyede
   3 puan dolmaya başlar.
3. Süpürme kabiliyetleri **blok başına** öder ve enerji bitince orada durur.

Varsayılan maksimum 100'dür; odak ağacındaki Dayanıklılık yetenekleri, beceri
kilometre taşları ve Mağara Adamı'nın prestij bonusu bunu arttırır. Bir
kabiliyet çalışırken eylem çubuğunda her zaman `Enerji mevcut/maksimum` yazar.

---

## Görevler

**85 görev**, üç tür:

- **Günlük** — 23 görevlik havuzdan her gün rastgele atanan 3 görev
- **Haftalık** — 19 görevlik havuzdan her hafta rastgele atanan 2 görev
- **Kalıcı** — tek seferlik, hiç sıfırlanmayan, herkeste ortak 43 görev

Beş çeşit vardır: blok kırma, yaratık öldürme, **yapı bulma** (o yapının içinde
durmak yeterli), **eşya edinme** (yerden almak da sandıktan da sayılır) ve
**kahramanlık** (sayıyla değil hikâyeyle biten görev — bir ghast'ı kendi
ateşiyle öldürmek gibi). Bazı görevler boyut ister.

Her görev **eğittiği beceriyi** kendisi bildirir ve ödülünü oraya öder — sıradan
beceri XP'si gibi, aynı sinerji ve prestij çarpanlarıyla. Yani bir görev hem
beceriyi hem o beceriden beslenen sınıfı birden büyütür.

Görev panosunda tamamlanmamış bir göreve tıklamak onu **panele sabitler**;
tekrar tıklamak kaldırır. Her tür için en fazla 3 görev sabitlenebilir. Kalıcı
görevler panelde yalnızca sabitlediklerinle görünür — 43 tanesini listelemek
mümkün değildir.

**Başlangıç görevleri bir kapıdır**: hepsi bitmeden günlük/haftalık/kalıcı
sekmeleri kilitli görünür ve panelde hiçbir görev gösterilmez.

---

## Saygınlık Puanı

Bu sunucuda para yoktur. Saygınlık bir bakiye değil bir **skordur**: harcanmaz,
hiçbir şeye takas edilmez. Satın aldığı tek şey itibardır — liderlik tablosunda
sıra, unvanlar, kozmetikler ve sunucu duyuruları.

Dört kaynaktan artar: dünyanın ürettiği değerli blokları kırmak (demir 500,
elmas 10.000, kadim kalıntı 40.000), yaratık öldürmek (Warden 250.000, Ejderha
500.000), görev tamamlamak ve dünya bossu öldürmek.

**Ölünce düşer.** Kayıp, ölmeden önceki puana göre altı bant hâlinde hesaplanır
ve sebep ölüm olayından değil **son aldığın hasardan** çözülür — uçurumdan
itilen ya da başkasının kurduna öldürülen biri "kendi hatası" bandına düşmez.
1.000'in altında hiçbir şey kaybetmezsin ve en üst bant açık uçludur.

35-40 dakikada bir, çevrimiçi herkesin puanı bir önceki turla karşılaştırılır ve
**yalnızca en çok kıpırdayan tek kişi** duyurulur — yükseliyorsa övgüyle,
düşüyorsa iğnelemeyle.

---

## Prestij

Prestij, ilerlemenin bir kısmını geri vererek **kalıcı güç satın almaktır**. En
fazla 10 kez yapılabilir.

**Sıfırlar**: seçili sınıfın sınıf seviyesi ve altı becerinin tümü (kilometre
taşı bonusları dâhil). **Güç düşmez** — sıfırlanmadan önceki Güç kalıcı olarak
biriktirilir ve üstüne yeniden yükselirsin.

**Şartlar** (üçü birden):

- Altı becerinin de en az 25. seviyede olması
- Seçili sınıfın 50. sınıf seviyesine ulaşması
- Sınıfın sinerji becerilerinin **hepsinin** o sınıfın istediği seviyeye
  ulaşması (tek beceri besleyen sınıflar 100 ister; Tabiat Vârisi dördünde de
  30 — dört beceriyi birden 100 yapmak onu diğerlerinden kat kat zor yapardı)

`/prestij` bunları tek tek ✔/✘ olarak listeler.

**Karşılığında**:

- Her rankta kalıcı **+%25 XP** (hem beceri hem sınıf, üst üste biner)
- Sınıfına özgü kalıcı bonus
- 4 özellik + 2 kabiliyet puanı, 1.000.000 saygınlık
- `/ev`, `/takas` ve `/çanta` kalıcı olarak açılır
- **İki zorunlu seçim**: dört **kalıcı bonustan** biri (can, PvE hasarı,
  maksimum enerji, beceri XP'si — aynısı her rankta tekrar seçilebilir ve üst
  üste biner) ve iki **eşya sandığından** biri (savaş ya da maden). Seçimini
  yapana kadar diğer MMO ekranları açılmaz.

---

## Dünya bossları

**30 dakikada bir**, çevrimiçi bir oyuncunun 120-320 blok uzağında adı olan bir
yaratık doğar. Doğuşu sesle, sunucu duyurusuyla ve koordinatla bildirilir; Xaero
haritasında da işaretlenir. Yakınına gidince ekranın üstünde can çubuğu çıkar.

Beş nadirlik vardır — Sıradan → Nadir → Epik → Efsanevi → **Mistik** — ve
nadirlik canını, hasarını, hızını, zırhını, silahını, ganimetini ve verdiği
saygınlığı birden belirler.

Boss zekâsı tek bir merdivendir ve her davranış o seviyeden türetilir: engel
aşma, kapı/çit açma, kapı kırma (yalnızca `mobGriefing` açıkken), başıboşken
hayvan avlama, mağara/uçurum kaçınma, köy baskını. Canı düşen boss ışınlanıp
**kaçar**, takipçisi kalmayınca yavaşça iyileşir ve toparlanınca döner; ömür
boyu kaç kez kaçabileceği sınırlıdır.

Bosslar güneşte yanmaz — sabahı bekleyerek bedavaya ölmelerini sağlayamazsın.
Kimse dokunmadıysa bir sonraki boss doğduğunda eskisi kaybolur; bir oyuncu bir
kez bile vurduysa dövüş bitene kadar durur. Öldüren oyuncu sınıf XP'si,
saygınlık, ganimet ve bossun **kendi adını taşıyan kafasını** alır.

---

## Ek özellikler

**Bulut sandığı (`/çanta`)** — ölünce kaybolmayan kişisel depo. İçindekiler
veritabanında durur, envanterinde değil; ne lav ne ölüm ona ulaşabilir. 30.
sınıf seviyesinde ya da prestijle açılır. **18 slotla** başlar; odak ağacındaki
düğmeden bir özellik puanı karşılığında **+7 slot**, en fazla iki kez
(18 → 25 → 32). Satın alınmamış slotlar kırmızı panelle kapalıdır.

**Ev (`/ev`)** — 15. sınıf seviyesinde açılır, 5 dakikada bir kullanılır. Nether
ve End'de ne ayarlanır ne oraya gidilir — portalı anlamsız kılmamak için.

**Takas (`/takas`)** — 20. sınıf seviyesinde açılır (karşı taraf 15). Mesafe
şartı yoktur; uzaktan takas etmek amacın ta kendisidir. Masaya konan eşya anında
envanterinden çıkar, bu yüzden kopyalanamaz.

**Unvanlar ve kozmetikler** — 19 unvan, 14 kozmetik. Unvan adının önünde kendi
renginde görünür: sohbette, panelde ve TAB listesinde. Kozmetik seviye
atladığında ve girişte partikül patlatır. Kilitli olanın üstüne gelince onu
neyin açacağı yazar.

**Koleksiyonlar ve macera günlüğü** — koleksiyonlar eşya toplamayı, günlük ise
tamamladığın her görevi ve koleksiyon kilometre taşını tarih sırasına dizer.

**Liderlik tabloları** — Güç, prestij, saygınlık ve her beceri için ayrı ayrı.

**Uyku** — tek bir oyuncunun uyuması sabahı getirir ve yağmuru dindirir.

**TAB listesi** — sunucu adı, çevrimiçi sayısı, canlı TPS ve ping; her oyuncunun
adının önünde unvanı, yanında Güç'ü.

**`/rtp`** — haritada rastgele, güvenli bir noktaya ışınlar. 30 dakikada bir.

---

## Panel ve HUD

Sağdaki panel (`/panel` ile gizlenir) adını, Güç/prestij/saygınlığını, sınıfını
ve sınıf seviyesini, kuşandığın kabiliyeti, `/ev` `/takas` `/çanta` durumunu ve
sabitlediğin görevleri gösterir. Sınıf son seviyeye ulaştığında XP yerine
**(MAX)** yazar, ve maksimum seviyedeki bir sınıf ya da beceri için artık
bossbar'da "+XP" geribildirimi çıkmaz.

**Kabiliyet HUD'u eşlik mod'unun çizdiği şeydir** ve dört parçadan oluşur:

- **Rozet** — kabiliyetin simgesi, adı ve geri sayımı. Oyun ekranında **sağ
  üstte**, bir envanter açıkken panelin **solunda**.
- **Ekran kenarı** — yalnızca altı kabiliyette çizilir (B Planı, Madenci
  Refleksleri, Asil Kurt, Filiz Patlaması, Sürü Çağrısı, Kan Yemini). Bir sinyal
  her yerdeyse sinyal değildir.
- **Kombo satırı** — yalnızca çılgınlık kabiliyetlerinde.
- **Parlama ve kurt adam modeli** — yukarıda anlatıldı.

Mod'u güncellememiş bir oyuncu tanımadığı paketi sessizce yok sayar: oyun aynı
kalır, ekran sade olur. HUD, bu sunucuda yokluğuna izin verilen tek şeydir —
hiçbir oyun kuralı ona bağlı değildir.

---

# Bölüm 2 — Teknik Bölüm

## Gereksinimler

| | |
|---|---|
| Sunucu | Paper **26.2** (yalnızca Paper; Bukkit/Spigot/Forge/Fabric desteklenmez) |
| Java | Çalıştırmak için 25+, derlemek için **26** |
| Veritabanı | SQLite (varsayılan, kurulum gerektirmez) / MySQL / MariaDB / PostgreSQL |
| İstemci | **hmmo-client 1.4.0** Fabric mod'u — zorunlu |
| İsteğe bağlı | PlaceholderAPI, LuckPerms |

## Kurulum

1. `HalilIbrahimMMO-1.0.0-beta-all.jar` dosyasını `plugins/` klasörüne koy.
2. Sunucuyu başlat. Paper çalışma zamanı kütüphanelerini (HikariCP + dört JDBC
   sürücüsü) kendisi indirir; bütün yapılandırma dosyaları oluşturulur ve
   veritabanı göçleri uygulanır.
3. `plugins/HalilIbrahimMMO/config.yml` içinden veritabanını seç (varsayılan
   SQLite'tır ve hiçbir şey yapmanı gerektirmez).
4. `hmmo-client-1.4.0.jar` dosyasını oyunculara dağıt.

## Yönetici paneli (`/haliladmin`)

`halilibrahimmmo.admin` izni gerekir (varsayılan: OP).

| Düğme | Ne yapar |
|---|---|
| **Yeniden Yükle** | Bütün yapılandırma dosyalarını diskten yeniden okur |
| **Hata Ayıklama** | Debug kaydını açar/kapatır (bellekte, diske yazmaz) |
| **Çevrimiçi Oyuncular** | Çevrimiçi bir oyuncunun kaydını düzenlemeye götürür |
| **Tüm Oyuncular** | Çevrimdışılar dâhil kayıtlı her oyuncu |
| **Dünya Bossu** | Nadirlik ve isim seçerek boss çağırır |
| **Yedek Al** | Elle yedek alır; son yedeğin ne kadar önce alındığını gösterir |
| **Mod Kontrolü** | Zorunlu mod listesinin canlı durumu |
| **Sürüm / Hakkında** | Sürüm bilgisi ve emeği geçenler |

**Oyuncu düzenleme ekranı**: altı becerinin seviye ve XP'si, sınıf seviyesi ve
XP'si, sınıfı zorla değiştirme, kuşanmış kabiliyet, iki puan havuzuna ek puan
verme, saygınlık ve **Envanterini Gör**.

**Envanter ekranı** (yalnızca çevrimiçi oyuncu için): oyuncunun envanteri,
zırhı, sol eli, canı/açlığı/zırh puanı ve konumu; zırh ve aletlerde dayanıklılık
satırı. Üstteki bir eşyaya tıklamak onu **yöneticiye alır**, kendi
envanterindeki bir eşyaya tıklamak **oyuncuya verir**. Her aktarım önce alan
tarafta yer olup olmadığını kontrol eder: yer yoksa veren taraf hiçbir şey
kaybetmez. Ekran oyuncunun gerçek envanterini açmaz — bir kopyayı çizip
aktarımları kendisi yapar, böylece iki oyuncu aynı yığına aynı anda dokunamaz.

## Yedekleme

`yedek.yml` bir çökmenin gerçekten kaybettirdiği şeyi hedefler: yarım yazılmış
bir SQLite dosyası ve son otomatik kayıtta kalmış oyuncu envanterleri.

Nasıl çalışır:

1. Bütün dünyalara "kendini kaydet" denir ve beklenir.
2. SQLite veritabanının **tutarlı** bir kopyası çıkarılır (`VACUUM INTO`). Canlı
   `.db` dosyası arşive **konulmaz** — yarım yazılmış bir dosyayı geri yüklemek
   ikinci bir arızadır. Harici veritabanlarında (MySQL/MariaDB/PostgreSQL)
   arşive bunu söyleyen bir not dosyası konur.
3. Seçilen içerik zip'lenir, saklama kotasının dışında kalan eski arşivler
   silinir.

Yedek dört sebepten alınır ve dosya adı sebebi taşır: `acilis`, `cokme`,
`zamanli`, `elle`. **Çökme tespiti** eklentinin veri klasöründeki bir işaret
dosyasıyla yapılır: açılışta yazılır, düzgün kapanışta silinir. Açılışta hâlâ
duruyorsa önceki kapanış düzgün değildi ve güvenlik yedeği alınır.

Varsayılanlar: 6 saatte bir, 8 arşiv saklanır, eklenti verisi + oyuncu verisi
dâhil, **dünyaların tamamı hariç** (uzun ömürlü bir haritada gigabaytlarcadır),
2 GB üst sınır. Sınırı aşan bir arşiv yazıldığı yerde silinir ve sebebi konsola
yazılır — dolu bir diskin sunucuyu durdurmasındansa yedeğin alınmaması yeğdir.

> Bu, sunucunun kendi yedeğinin yerine geçmez. Aynı diskteki bir arşiv disk
> arızasından kurtarmaz.

## Açılış hata kontrolü

Herhangi bir servis dosya okumadan önce `ConfigIntegrityCheck` bütün paket
YAML'larını okur ve bozuk olanı **dosya adı ve satır numarasıyla** Türkçe
bildirir. Bir servis yine de açılamazsa hata o servisin adıyla sarılır ve
konsola kök sebep, kaç servisin başarıyla açıldığı ve ne yapılması gerektiği
yazılır. Bozuk bir dosyayı silmek her zaman güvenlidir: bir sonraki açılışta
varsayılan hâliyle yeniden üretilir ve hiçbir oyuncu verisi kaybolmaz.

## Proje yapısı

```
halilibrahimMMO/
├── api/         Üçüncü taraf eklentiler için bağımlılıksız genel API
├── core/        Eklentinin tamamı (:api'ye bağlı)
├── clientmod/   hmmo-client Fabric mod'u - AYRI bir Gradle derlemesi
├── tasarim/     Kurt adam referans görselleri ve kaynak dokusu
└── tools/       Mod listesi çıkarıcı, kurt adam dokusu üreteci
```

`core`'un `shadowJar`'ı yalnızca `api`'nin sınıflarını birleştirir; hiçbir
üçüncü taraf kütüphane jar'a gömülmez. Çalışma zamanı kütüphaneleri Paper'ın
`PluginLoader` + `MavenLibraryResolver` yoluyla indirilir.

## Derleme

Depoda **birbirinden bağımsız iki derleme** vardır. Karıştırma: kökteki
`./gradlew` istemci mod'unu görmez, `clientmod/` içindeki de eklentiyi.

### Sunucu eklentisi (kök dizin)

```bash
./gradlew build
```

Çıktı: `core/build/libs/HalilIbrahimMMO-1.0.0-beta-all.jar`.
`JAVA_HOME` mutlaka JDK 26'yı göstermelidir.

| Komut | Ne yapar |
|---|---|
| `./gradlew build` | Derler, testleri çalıştırır, shaded jar üretir |
| `./gradlew test` | Yalnızca JUnit 5 testleri |
| `./gradlew runServer` | Paper 26.2'yi indirip `core/run/` altında test sunucusu açar |

### İstemci mod'u (`clientmod/`)

```bash
cd clientmod
./gradlew build
```

Çıktı: `clientmod/build/libs/hmmo-client-1.4.0.jar`. Kendi wrapper'ı vardır
(Gradle 9.5.1) ve Java 25 hedefler. PowerShell'de `.\gradlew.bat` gerekir.
Birim testi yoktur; doğrulama canlı sunucuya girerek yapılır.

### Kurt adam dokusu

```bash
python tools/kurt-adam-doku.py
```

`clientmod/.../textures/entity/kurt_adam.png` dosyasını yeniden üretir.
**Bu betiğin parça tablosu ile `WerewolfModel.java`'daki `texOffs` çağrıları
aynı olmak zorundadır** — birini değiştiren diğerini de değiştirir. Yerleşim
modelin kendi tanımıdır ve doku ona göre üretilir: bir kutu-UV yerleşimi bitmiş
bir görüntüden geri çıkarılamaz, ters yön denenmiş ve bozuk bir model
üretmişti. Renkler kaynak sanatın (`tasarim/kurt-adam/`) paletinden sayılarak
alınmıştır.

## Veritabanı

Bütün tablolar `hmmo_` önekiyle başlar (paylaşımlı veritabanı güvenliği). Şema
sürümlenmiş `Migration` sınıflarıyla yönetilir; **hiçbir göç asla yeniden
numaralandırılmaz veya silinmez** — yeni bir şema değişikliği her zaman bir
sonraki numarayla yeni bir göçtür.

Mevcut şema sürümü: **V20**. Faz 25 yeni göç eklemez.

Veritabanı katmanının tamamı asenkrondur ve `CompletableFuture` döndürür; tek
istisna açılışta göçlerin beklenmesidir.

## Yapılandırma dosyaları

| Dosya | İçerik |
|---|---|
| `config.yml` | Genel ayarlar ve veritabanı bağlantısı |
| `messages.yml` | Oyuncuya ve yöneticiye giden **her** metin |
| `progression.yml` | Güç eğrisi ve genel olay ödülleri |
| `skills.yml` | Beceri XP eğrisi, kaynaklar, kilometre taşları |
| `abilities.yml` | Enerji ayarları ve 12 kabiliyetin tanımı |
| `classes.yml` | Sınıflar, sınıf seviye eğrisi, odak ağacı, puan ekonomisi |
| `prestige.yml` | Prestij şartları, kalıcı ödüller, fiziksel ödül sandıkları |
| `quests.yml` | 85 görev |
| `collections.yml` | Koleksiyonlar |
| `leaderboards.yml` | Liderlik tablosu ayarları |
| `titles.yml` / `cosmetics.yml` | Unvanlar ve kozmetikler |
| `feedback.yml` | Bossbar/başlık süreleri ve ses efektleri |
| `sayginlik.yml` | Saygınlık değerleri ve eşikleri |
| `bosslar.yml` | Dünya bossu doğuşu, nadirlikler, arketipler |
| `modlar.yml` | Zorunlu/yasak Fabric mod listesi |
| `sunucu.yml` | Uyku, TAB listesi, sunucu bilgisi anahtarları |
| `rehber.yml` | Oyun içi rehberin bütün metni |
| `yedek.yml` | Yedekleme |

**Kopyala-üzerine-yazma kuralı**: dosyalar yalnızca yoksa jar'dan kopyalanır,
sonra hep diskten okunur. Yeniden yükleme dosyayı asla programatik olarak geri
yazmaz — elle yapılan düzenlemeler ve yorumlar her zaman korunur.

> **1.0.0-beta'ya yükseltirken**: yeni sürümle gelen anahtarlar mevcut dosyalara
> **eklenmez**. Silinip yeniden ürettirilmesi gerekenler: `messages.yml`,
> `rehber.yml`, `prestige.yml`, `modlar.yml`. `yedek.yml` yeni bir dosyadır ve
> kendiliğinden oluşur. Veritabanına dokunulmaz, hiçbir oyuncu verisi
> etkilenmez.

## Zorunlu Fabric modları

`modlar.yml` hangi mod'ların **zorunlu**, hangilerinin **yasak** olduğunu
tanımlar. Girişte oyuncuya mod listesi sorulur; cevap vermeyen ya da listeyi
geçemeyen atılır.

> **TUZAK — `default: op`**: bypass izni `hmmo.mod.bypass` varsayılan olarak
> OP'lerdedir, yani sunucu sahibi yapı gereği muaftır. "Kapı çalışmıyor"
> sanmanın en yaygın sebebi budur. Test ederken OP'siz bir hesap kullanın.

`hmmo-client` için minimum sürüm bilerek **1.0.0**'da tutulur: minimum, yeni
sürüm oyuncuların kurulumuna ulaştıktan *sonra* yükseltilir. Faz 25 protokolü
değiştirmedi (hâlâ 3), yani 1.3.0 istemcisi de eksiksiz çalışır.

## Kabiliyet HUD kanalı (`hmmo:hud`)

Tek yönlüdür: sunucu ne çizileceğini söyler, istemci hiç cevap vermez. Protokol
sürümü **3**. Üç mesaj türü vardır — temizle, kabiliyet, kurt formundakiler — ve
hepsi **durumsuzdur**: her mesaj resmin tamamını taşır, böylece paket düşüren
bir istemci bir sonraki mesajla kendiliğinden düzelir.

Biçim tek bir sınıfta yaşar ve her iki tarafta bire bir aynadır:
`hud/HudProtocol.java` (sunucu) ve `client/HudProtocol.java` (istemci).

Kurt adam ve parlama tamamen istemci tarafıdır: sunucu yalnızca "şu oyuncular
kurt" ve "şunlar parlasın" der. Parlama, varlığın paylaşımlı bayrağı istemcinin
kendi kopyasında yazılarak yapılır — sunucudaki hiçbir şey değişmez,
dolayısıyla kabiliyeti olmayan bir oyuncu hiçbir şey görmez.

## Entegrasyonlar

- **PlaceholderAPI** — Güç, prestij, sınıf, saygınlık, enerji ve beceri başına
  birer placeholder.
- **LuckPerms** — prestij ve Güç eşiklerinde otomatik grup ataması.

İkisi de isteğe bağlıdır; yoksa eklenti onları hiç aramaz.

## Moderasyon kaydı

Öldürülen her köylü ve **evcilleştirilmiş** at/eşek/katır/köpek/kedi için
konsola tarih, tür, isim, dünya, koordinat ve öldüren yazılır. Öldüren, ölüm
olayından değil son alınan hasardan çözülür. Tabloya değil konsola, çünkü
sorulan soru bir log dosyasında `grep`'lenen bir sorudur.

## Test

```bash
./gradlew test
```

JUnit 5. Testler saf mantığı hedefler: XP eğrileri, puan bütçeleri, göç
sözleşmeleri, ölüm cezası bantları, açıklama sarma, yedekleme ayarları ve
`rehber.yml`'nin yapısal geçerliliği. Bukkit gerektiren hiçbir şey birim
testine sokulmaz.

Çapraz motor göç testleri (`:core:compileIntegrationTestJava`) Testcontainers
kullanır ve Docker gerektirir; normal `test` görevinde çalışmaz.

## Dil politikası

Bu ayrım kalıcıdır ve asla karıştırılmaz:

- **Oyuncunun ve yöneticinin ekranda gördüğü her şey Türkçe**: mesajlar, GUI,
  komut çıktıları, bossbar/başlık metinleri, konsol satırları ve yapılandırma
  dosyalarının yorumları.
- **Kaynak kod İngilizce**: paket/sınıf/metod/değişken adları, Javadoc, istisna
  mesajları.

Oyuncuya giden hiçbir metin koda gömülmez: her metin bir `MessageKey` sabiti ve
`messages.yml`'deki karşılığı üzerinden gider.

---

## Emeği geçenler

Geliştirici: **Furkan Okan** (`fxrkqnplus`). Oyun içinde `/credits`.
