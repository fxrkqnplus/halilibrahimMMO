# PRD: HUD, XP Geri Bildirimi, Yetenek Aktivasyonu ve Admin Komutları

> Faz 8 kapsamı. Bu PRD, PROJECT_MEMORY.md'deki tüm mimari kararlara (§5 servis
> deseni, §5.4 MessageKey disiplini, §5.6 komut kayıt yolu, ekonomi yasağı)
> uyularak uygulanacaktır. Oyuncuya görünen her metin Türkçe, kaynak kod İngilizce.

## 1. Giriş / Genel Bakış

Sunucudaki oyuncular ilerlemelerini (sınıf, seviye, XP) ancak `/profil` veya
`/menu` açarak görebiliyor; XP kazanımı anlık olarak hissedilmiyor ve seviye
atlama bildirimi ekran ortasında title olarak çıkıyor. Ayrıca yetenek (ability)
aktivasyonu yalnızca `/kabiliyet <id>` komutuyla yapılabiliyor ve adminlerin
oyuncu verilerini düzeltmek için hiçbir aracı yok.

Bu faz dört şey ekler: (1) ekranın sağında kalıcı bir profil bilgi ekranı
(scoreboard sidebar), (2) her XP kazanımında ilerlemeyi gösteren BossBar ve
kişiye özel seviye atlama chat mesajı, (3) menüden seçilip alete sağ tıkla
aktive edilen, süresi boyunca alete büyü parlaması veren ve action bar'da geri
sayımı görünen yetenek aktivasyonu, (4) yalnızca yetkili adminlerin
kullanabileceği oyuncu verisi düzenleme komutları.

## 2. Hedefler

1. Her oyuncu, hiçbir komut yazmadan sınıfını, seviyesini ve mevcut XP
   durumunu ekranın sağında sürekli görebilmeli.
2. Her XP kazanımı, hangi alandan geldiği ve bir sonraki seviyeye kalan
   miktarla birlikte anında görselleştirilmeli.
3. Seviye atlama bildirimi ekran title'ı yerine yalnızca ilgili oyuncunun
   chat'ine düşmeli; başka hiçbir oyuncu bu mesajı görmemeli.
4. Yetenekler komut ezberi gerektirmeden, menüden seçilip uygun alete sağ
   tıklanarak kullanılabilmeli; aktif yetenek ve kalan süresi ekranda
   görünmeli.
5. Adminler, oyuncuların seviye/XP/sınıf/istatistik/yetenek verilerini
   oyun içinden güvenli şekilde görüntüleyip düzenleyebilmeli.

## 3. Kullanıcı Hikâyeleri

- Bir **oyuncu** olarak, ekranımın sağında adımı, sınıfımı, seviyemi ve XP
  durumumu sürekli görmek istiyorum; böylece ilerlememi takip etmek için menü
  açmam gerekmez.
- Bir **oyuncu** olarak, sidebar'ı istemediğimde tek komutla kapatıp tekrar
  açabilmek istiyorum.
- Bir **oyuncu** olarak, blok kırıp XP kazandığımda hangi alandan ne kadar
  kazandığımı ve seviyeye ne kaldığını BossBar'da görmek istiyorum.
- Bir **oyuncu** olarak, seviye atladığımda bunu yalnızca kendi chat'imde
  görmek istiyorum; tüm sunucuya duyurulmasını istemiyorum.
- Bir **oyuncu** olarak, menüden Madenci yeteneğini seçip kazmama sağ
  tıklayarak aktive etmek istiyorum; süre boyunca kazmam parlasın ve action
  bar'da kalan süreyi göreyim.
- Bir **admin** olarak, bir oyuncunun profilini görüntüleyip seviyesini,
  XP'sini, sınıfını veya yetenek seviyesini düzeltebilmek istiyorum; böylece
  veri bozulmalarını veya destek taleplerini oyun içinden çözebilirim.

## 4. Fonksiyonel Gereksinimler

### A. Profil sidebar'ı (scoreboard)

1. Sistem, her çevrimiçi oyuncuya ekranın sağında bir scoreboard sidebar
   göstermelidir. İçerik: oyuncu adı, seçili sınıfın görünen adı (sınıf yoksa
   "Sınıf seçilmedi" karşılığı), global seviye, mevcut XP / sonraki seviye
   için gereken XP, prestij sayısı.
2. Sidebar oyuncu giriş yaptığında otomatik açılmalıdır (varsayılan: açık).
3. Yeni bir kök komut (`/panel`) sidebar'ı kapatıp açabilmelidir (toggle).
4. Sidebar içeriği; XP kazanımı, seviye atlama, sınıf değişimi ve prestij
   sonrasında güncellenmelidir. Veri, mevcut senkron profil önbelleğinden
   (`ProfileService#cached`) okunmalı, sidebar güncellemesi için asla veritabanı
   sorgusu beklenmemelidir.
5. Sidebar başlığı ve tüm satır şablonları `messages.yml`'de tanımlanmalıdır.

### B. XP BossBar'ı ve kişiye özel seviye atlama mesajı

6. Sistem, bir oyuncu global XP kazandığında o oyuncuya bir BossBar
   göstermelidir. Çubuğun doluluk oranı, mevcut seviyedeki ilerlemeyi
   (kazanılan XP / sonraki seviye için gereken XP) yansıtmalıdır.
7. BossBar metni, XP'nin hangi alandan/kaynaktan kazanıldığını (ör. Madencilik,
   Avcılık, Görev, Koleksiyon) ve kazanılan miktarı içermelidir.
8. Oyuncu başına tek bir BossBar örneği kullanılmalıdır: art arda XP
   kazanımları yeni bar açmak yerine mevcut barı güncellemeli; son kazanımdan
   sonra `feedback.yml`'de yapılandırılan süre dolunca bar gizlenmelidir.
9. Seviye atlandığında mevcut ekran-ortası title gösterimi kaldırılmalı,
   yerine yalnızca seviye atlayan oyuncunun chat'ine giden bir mesaj
   gönderilmelidir. Bu mesaj hiçbir koşulda diğer oyunculara gösterilmemeli
   veya sunucuya duyurulmamalıdır.
10. Görev/koleksiyon/prestij anlarının mevcut görsel geri bildirimi
    (BossBar + title) değiştirilmemelidir; yalnızca seviye atlama davranışı
    değişmektedir.

### C. Menüden yetenek seçimi ve sağ tıkla aktivasyon

11. `/menu` içindeki yetenekler menüsünde bir yeteneğe tıklamak, yeteneği
    anında aktive etmek yerine "seçili yetenek" olarak işaretlemelidir.
    Seçili yetenek menüde büyü parlamasıyla vurgulanmalıdır.
12. Oyuncu, seçili yeteneğe uygun bir alet (ör. Maden Patlaması için
    herhangi bir kazma) elindeyken sağ tık yaptığında yetenek aktive
    olmalıdır. Enerji ve bekleme süresi kontrolleri mevcut
    `AbilityService` yolundan aynen geçmelidir.
13. Uygun olmayan aletle sağ tık hiçbir şey yapmamalı (mesaj spam'i
    olmamalı); uygun aletle ama yetersiz enerji/bekleme süresinde yapılan
    denemede mevcut Türkçe hata mesajları gösterilmelidir.
14. Süreli yeteneklerde, aktivasyon anında oyuncunun elindeki alete büyü
    parlaması (enchantment glint) uygulanmalı ve süre bitince
    kaldırılmalıdır. Parlama yalnızca görseldir; alete kalıcı büyü, NBT
    kalıntısı veya davranış değişikliği bırakmamalıdır.
15. Yetenek aktifken ekranın alt kısmında (action bar) yeteneğin adı ve
    kalan süresi gösterilmeli, geri sayım en az saniyede bir
    güncellenmelidir. Mevcut enerji action bar bildirimiyle çakışma,
    yetenek geri sayımı öncelikli olacak şekilde çözülmelidir. Bu geri
    sayım, süreli yeteneklerin yanı sıra **tek kullanımlık yeteneklerin**
    "sonraki geçerli eylemi bekleme" penceresinde de (ör. 3×3 kazı için
    sonraki blok kırılana kadar geçen 15 saniyelik bekleme) aynı şekilde
    gösterilmelidir; oyuncu bu süre boyunca hem elindeki alette glint hem
    de action bar'da kalan bekleme süresini görür.
16. `/kabiliyet <id>` komutu geriye dönük uyumluluk için aynen çalışmaya
    devam etmelidir.

### D. Admin komutları

17. `/rpg` şemsiye komutu altına `oyuncu` alt komutu eklenmelidir:
    - `/rpg oyuncu <isim> bilgi` — profil özeti (seviye, XP, prestij, sınıf,
      yetenek seviyeleri, temel istatistikler).
    - `/rpg oyuncu <isim> seviye <değer>` — global seviyeyi ayarlar (XP o
      seviyenin başına sıfırlanır).
    - `/rpg oyuncu <isim> xp ver <miktar>` — normal ilerleme yolundan XP verir
      (seviye atlama/başarım tetikleyicileri çalışır).
    - `/rpg oyuncu <isim> sinif <id|temizle>` — sınıfı ayarlar veya kaldırır;
      sınıf değişim bekleme süresini atlar.
    - `/rpg oyuncu <isim> yetenek <id> seviye <değer>` — bir yeteneğin (skill)
      seviyesini ayarlar.
    - `/rpg oyuncu <isim> istatistik <anahtar> <değer>` — bir istatistik
      sayacını ayarlar.
18. Her alt komut, varsayılanı OP olan ayrı bir permission node ile
    korunmalıdır (`halilibrahimmmo.admin.oyuncu.*` deseni), böylece LuckPerms
    ile tek tek devredilebilir.
19. Komutlar yalnızca çevrimiçi oyuncular üzerinde çalışmalıdır; hedef
    çevrimdışıysa Türkçe bir hata mesajı gösterilmelidir.
20. Her başarılı admin işlemi hem komutu kullanan admine Türkçe onay mesajı
    döndürmeli hem de konsola İngilizce olmayan, Türkçe bir denetim satırı
    loglanmalıdır (mevcut `PluginLogger` üzerinden).
21. Admin değişiklikleri; sidebar, BossBar ve önbellekler dahil tüm görünür
    durumlara anında yansımalıdır.

## 5. Kapsam Dışı (Non-Goals)

- Ekonomi/para/coin/banka: asla eklenmeyecek (kalıcı proje kuralı).
- Çevrimdışı oyuncular üzerinde admin düzenlemesi (ilk sürümde yok).
- Sidebar içeriğinin oyuncu tarafından özelleştirilmesi.
- Yeni yetenek/skill içeriği: yalnızca aktivasyon biçimi değişiyor, denge
  değerleri ve yetenek tanımları aynen kalıyor.
- PlaceholderAPI tabanlı harici scoreboard eklentileriyle entegrasyon.
- Seviye atlama dışındaki geri bildirim anlarının (görev, koleksiyon,
  prestij) değiştirilmesi.

## 6. Tasarım Notları

- Sidebar ve tüm yeni metinler `MessageKey` + `messages.yml` disiplinine
  uyar; Java tarafında hiçbir Türkçe kelime kurulmaz, yalnızca sayılar ve
  zaten-Türkçe config verileri placeholder olarak geçirilir.
- Sidebar satır düzeni ve `/panel` komut adı dahil görünür her şey
  `messages.yml`/ilgili YAML üzerinden düzenlenebilir olmalıdır.
- BossBar süresi ve XP barının gizlenme gecikmesi `feedback.yml`'e eklenir
  (mevcut dosya, ek anahtarlar — §5.3 gereği yalnızca eksik anahtar ekleyen
  yaklaşım).

## 7. Teknik Notlar

- Yeni paketler §4 desenine göre: `scoreboard` (SidebarService),
  `abilities` genişletmesi (seçili yetenek + sağ tık dinleyici + glint/geri
  sayım), `command/subcommand` altına admin alt komutları.
- Sidebar/BossBar güncellemeleri oyuncunun kendi region scheduler'ında
  çalışmalı; DB erişimi gerekmez (senkron önbellekler yeterli).
- `/panel` tercihi kalıcı **değildir**: yalnızca oturum-içi (in-memory)
  tutulur; sunucu her yeniden başlatıldığında (ve oyuncu her girişinde)
  sidebar varsayılan olarak açık gelir. Migration/veritabanı sütunu
  gerekmez.
- Glint: `ItemMeta#setEnchantmentGlintOverride(true)` ile uygulanır, süre
  sonunda `null`'a çevrilir; oyuncu aleti bırakır/değiştirir/çıkarsa da
  temizlenmelidir (quit/drop/slot değişimi kenar durumları).
- Sağ tık dinleyicisi `PlayerInteractEvent`'in çift el (main/off hand) çift
  tetiklenmesine karşı korunmalıdır.
- Seviye atlama title kaldırımı `BossBarFeedbackService`'te yalnızca
  level-up dalını etkiler; migration gerekmez.
- Admin `xp ver` mevcut `ProgressionService.award(...)` yolunu kullanır ki
  başarım/unvan tetikleyicileri tutarlı kalsın; `seviye`/`yetenek seviye`
  için servislerde yeni kontrollü mutasyon metotları gerekir (API modülüne
  mutasyon sızdırılmaz, yalnızca core-içi).

## 8. Başarı Ölçütleri

- Oyuncular komut yazmadan seviye/XP/sınıf bilgisine erişebiliyor
  (sidebar açık oranı yüksek kalıyor, `/profil` kullanımı azalıyor).
- Seviye atlama mesajı yalnızca ilgili oyuncuda görünüyor (test: iki
  istemcili doğrulama).
- Yetenekler menü + sağ tık ile komutsuz kullanılabiliyor.
- Adminler destek taleplerini oyun içinden, konsola dokunmadan çözebiliyor.
- `./gradlew build` ve `./gradlew test` yeşil; `runServer` ile canlı doğrulama.

## 9. Açık Sorular

Karar verildi (kullanıcı onayı):
- `/panel` tercihi kalıcı değildir; her yeniden başlatmada varsayılan açık
  gelir (bkz. §7 Teknik Notlar).
- Tek kullanımlık yeteneklerin bekleme penceresinde de action bar geri
  sayımı gösterilir (bkz. §4.C madde 15).

Hâlâ açık:
1. Prestij sayısı sidebar'da her zaman mı gösterilsin, yoksa yalnızca
   prestij > 0 iken mi?
2. Admin `bilgi` çıktısında hangi istatistik anahtarları listelensin
   (tümü mü, seçili bir alt küme mi)?
