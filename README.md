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

Mevcut yapı, UUID tabanlı asenkron oyuncu profillerini ve istatistiklerini
içerir. Oyun içinde "Güç" olarak adlandırılan genel seviye, bağımsız bir XP
kaynağına sahip değildir — dört becerinin (kazıcılık, odunculuk, madencilik,
avcılık) seviyelerinin toplamına, prestij yaptıkça kalıcı olarak biriken
Güç'ün eklenmesiyle hesaplanır — prestij sıfırladığı seviyeler yüzünden Güç
asla düşmez, yalnızca artmaya devam eder. Oyuncular güncel Gücünü `/profil`
ile görüntüleyebilir. **Yaratıcı (Creative) veya izleyici (Spectator)
moddayken hiçbir şekilde beceri, sınıf veya görev XP'si kazanılmaz.** Her
Gradle komutu için Java 26 kullanın; çalışma alanında kurulu JDK
`C:\Program Files\Java\jdk-26.0.1` konumundadır.

## Beceriler ve kabiliyetler

Dört bağımsız olarak seviyelenen beceri bulunur: `kazicilik`, `odunculuk`,
`madencilik` ve `avcilik`. Paylaşılan açık seviye eğrileri `skills.yml`
dosyasında yapılandırılır; her beceri artık kırılan bloğun veya öldürülen
yaratığın **nadirliğine göre** farklı miktarda XP verir (ör. toprak çok az,
elmas/kadim kalıntı çok daha fazla; zombi az, warden/ejderha çok daha fazla).
Bir beceri 25/50/75/100. seviyelere ulaştığında kalıcı bir bonus kazandırır
— her beceri kendine özgü, farklılaştırılmış bonuslar verir: avcılık ekstra
yaratık hasarı ve can, madencilik Acele (Haste) ve enerji, odunculuk Hız ve
enerji, kazıcılık ise ekstra enerji ve can (bonuslar, prestij ile o beceri
sıfırlanınca kaybedilir). Her becerinin kendi detay ekranı `/madencilik`, `/odunculuk`, `/kazicilik`
ve `/avcilik` komutlarıyla açılır: mevcut seviye/XP, becerinin beslediği
sınıf, kilometre taşı açıklamaları ve o becerinin liderlik tablosu buradan
görüntülenir. `abilities.yml`, paylaşılan kalıcı olmayan enerji havuzunu ve
sınıfa özel kabiliyetleri yapılandırır.

Kabiliyetler, oyuncunun seçili **sınıfının seviyesine** göre açılır (beceri
seviyesine göre değil) ve iki şekilde etkinleştirilebilir: doğrudan
`/kabiliyet <id>` ile, ya da kabiliyet menüsünden birini seçip uygun alete
(kazma/balta/kürek/kılıç, kabiliyete göre değişir) sağ tıklayarak — seçili
alet, kabiliyetin süresi boyunca parlar ve eylem çubuğunda hangi kabiliyetin
etkin olduğu ile ne kadar süre (ya da tek seferlik kabiliyetlerde bekleme
penceresi) kaldığı gösterilir. Farklı bir kabiliyete geçmek kısa bir bekleme
süresine tabidir (varsayılan 120 saniye); aynı kabiliyeti yeniden seçmek
serbesttir. Bazı kabiliyetler tamamen süreye dayalıdır (ör. Gece Görüşü);
Amansız Savaşçı'nın Avcı Odağı gibi savaşa dayalı bazı kabiliyetler ise her
isabetli vuruşta enerji tüketir ve enerji biterse ya da en fazla 30 saniye
sonra kendiliğinden sona erer — ve yalnızca yaratıklara (PvE) karşı
geçerlidir, PvP'de hiçbir etkisi yoktur. Henüz açılmamış kabiliyetler menüde
kilitli olarak gösterilir, detayları açılana kadar gizli kalır.

Geliştirici API'si, değişmez beceri tanımlarını ve asenkron salt okunur oyuncu
beceri sorgularını sunar. Harici eklentilerin beceri XP'si vermesine veya
kabiliyet tetiklemesine izin vermez.

## Profil paneli (sidebar)

Ekranın sağında kalıcı, renklendirilmiş bir bilgi paneli; oyuncu adını
(varsa kuşanılan unvan kendi rengiyle isimden önce, "Oyuncu: [Unvan] Ad"
sırasıyla), Güç seviyesini (altın), prestiji (açık mor), seçili sınıfı ve o
sınıfın kendi seviyesini (camgöbeği), açık kabiliyetlerin adlarını (seçili
olan yeşil, diğerleri kırmızı), henüz tamamlanmamış ilk-adım görevlerini
(tamamlanınca bu bölüm tamamen kaybolur), her günlük görevi ayrı ayrı
ilerleme ve XP ödülüyle (sarı), ve haftalık görev özetini — mantıksal
bölümler arasında ince ayırıcı çizgilerle — gösterir. Panel, sunucuda
gerçekleşen her değişikliği (beceri/sınıf XP kazanımı, unvan değişikliği,
yönetici müdahalesi dahil) yaklaşık 2 saniye içinde **kendiliğinden**
günceller — `/panel` ile açıp kapatmaya gerek yoktur, bu komut yalnızca
paneli mevcut oturum için tamamen gizler/gösterir. Tercih kaydedilmez, her
katılışta veya sunucu yeniden başladığında panel varsayılan olarak görünür
duruma döner.

## Sınıflar ve prestij

`/sinif` kabiliyet menüsünü açar; oyuncular Define Avcısı, Amansız Savaşçı,
Mağara Adamı veya Orman Bekçisi sınıflarından birini seçebilir, yetenek
ağaçlarını inceleyebilir ve paylaşılan yetenek puanlarını harcayabilir —
tamamı menü üzerinden, artık sohbet komutu argümanı yoktur. Her sınıfın kendi
"sinerji becerisi" vardır (ör. Amansız Savaşçı ↔ avcılık) ve o beceriden
kazanılan XP'nin bir kısmı otomatik olarak sınıfın kendi seviye izine de
yansır. Sınıf değişikliğinin yapılandırılabilir, veritabanı destekli yedi
günlük bir bekleme süresi vardır.

Prestij yapabilmek için üç şart birden sağlanmalıdır: dört becerinin de en
az 25. seviyede olması, seçili sınıfın kendi sınıf seviyesinin maksimuma
(varsayılan 50) ulaşmış olması, ve o sınıfın sinerji becerisinin de kendi
maksimum seviyesine (100) ulaşmış olması. `/prestij` bu üç şartı ayrı ayrı
✔/✘ olarak listeler. Şartlar sağlandığında `/prestij onayla` seçili sınıfın
sınıf seviyesini ve dört becerinin tümünü sıfırlar — **ancak Güç düşmez**:
sıfırlanmadan önceki Güç kalıcı olarak "biriktirilir" ve becerileriniz
tekrar seviye atladıkça Güç kaldığı yerden artmaya devam eder. Karşılığında
kalıcı olarak her prestij rankında +%25 XP kazanımı (hem beceri hem sınıf
XP'sinde) ve seçili sınıfa özgü kalıcı bir bonus (sınıfa göre can/hasar/
enerji) kazanılır. Prestijli oyuncular liderlik tablosunda ham seviyeden
bağımsız olarak her zaman önde gösterilir. Prestij yapmak sunucu genelinde
bir duyuru, bir havai fişek ve ses efektiyle kutlanır; varsayılan maksimum
prestij sayısı 10'dur.

## Görevler, koleksiyonlar ve macera günlüğü

`/gorev` oyuncuya o an **atanmış** kalıcı, günlük ve haftalık görevleri
ilerlemenizle birlikte listeler; günlük/haftalık görevler geniş bir havuzdan
her periyot başına rastgele seçilip atanır (varsayılan: 3 günlük, 2
haftalık), kalıcı görevler ise herkese her zaman atanmıştır. Görevler doğru
blokları kırdıkça veya doğru yaratıkları öldürdükçe otomatik ilerler
(Yaratıcı/İzleyici moddayken ilerleme kaydedilmez), periyodu bitince yeni
bir rastgele atamayla sıfırlanır. `/koleksiyon`
koleksiyonlarınızı listeler, `/koleksiyon <id>` ise henüz en az bir kez
toplamadığınız ögeleri gösterir. `/gunluk` son on görev tamamlama ve
koleksiyon kilometre taşınızı gösterir. Bir görevi tamamlamak, bir koleksiyon
ögesi edinmek ve bir koleksiyonun tamamını tamamlamak, seçili sınıfınızın
XP'sini kazandırır (sınıf seçili değilse ödül verilmez). Bir günlük/haftalık
görevi tamamlamak ayrıca sunucu genelinde (XP miktarı gösterilmeden)
duyurulur.

## Liderlik tabloları

`/liderlik seviye`, `/liderlik prestij`, `/liderlik gorev`, `/liderlik
koleksiyon` ve `/liderlik <yetenek-id>`; Güç (önce prestij, sonra toplam
beceri seviyesi sırasına göre), tamamlanan görev sayısı, edinilen koleksiyon
ögesi sayısı ve her yapılandırılmış beceri için en üst sıradaki oyuncuları
gösterir. Aynı tablolara `/menu`'nün Topluluk
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

Bir beceriden XP kazanıldığında, hangi cevherin/yaratığın XP verdiğini
parantez içinde gösteren bir beceri cübuğu (boss bar) görünür. Seçili
sınıfınızın sinerji becerisinden XP kazandığınızda, beceri cübuğuyla
**aynı anda**, her sınıfa özgü kendi renginde ayrı bir sınıf XP cübuğu de
görünür (görev/koleksiyon ödülleri de bu sınıf XP cübuğu üzerinden
gösterilir). Seviye atlamanın kendisi kişisel bir sohbet mesajıdır. Güç
(dört becerinin toplamı) yalnızca 10'un katlarına ulaşıldığında kişisel bir
cübukla belirtilir; 25/50/75/100. Güç seviyelerine ulaşmak sunucu genelinde
duyurulur. Görev tamamlama, koleksiyon tamamlama ve prestij hâlâ her
oyuncuya kısa bir cübuk ve ekran başlığı gösterir, kuşanılan kozmetikten
bağımsız olarak — günlük/haftalık görev tamamlama ve prestij ayrıca sunucu
genelinde duyurulur, prestij bir havai fişek ve ses efektiyle de kutlanır
(`feedback.yml` her şeyin ne kadar süre görünür kalacağını denetler).

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
ayıklama kaydını açıp kapatma, çalışan eklenti/Paper sürümünü görüntüleme,
çevrimiçi oyuncuları gezip herhangi birinin salt okunur profil/sınıf/beceri
özetini görüntüleme, ve **kayıtlı her oyuncuyu** (çevrimdışı olanlar dahil)
listeleyip beceri/sınıf seviyesini ve XP'sini tıklamalarla ayarlama ya da
oyuncunun sınıfını zorla değiştirme. Bir seviye atlamaya yol açan her
müdahale, hedef oyuncu çevrimiçiyse organik bir seviye atlamayla birebir
aynı geri bildirimi (cübuk, ses, "yeni kabiliyet açıldı" ekran yazısı,
panel güncellemesi) tetikler. Her yönetici müdahalesi Türkçe bir sunucu
günlüğü kaydı bırakır.

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
