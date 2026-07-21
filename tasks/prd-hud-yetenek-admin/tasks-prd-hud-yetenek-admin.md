## Relevant Files

- `core/src/main/java/com/halilibrahim/halilibrahimmmo/scoreboard/SidebarService.java` - Yeni: sidebar sözleşmesi (`PluginService` genişletir), aç/kapat tercihi ve render metotları.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/scoreboard/ScoreboardSidebarService.java` - Yeni: Bukkit `Scoreboard`/`Objective` tabanlı implementasyon, oyuncu başına in-memory aç/kapat tercihi (varsayılan açık, kalıcı değil).
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/scoreboard/SidebarListener.java` - Yeni: join/quit'te sidebar başlat/temizle; diğer sistemlerin (XP, seviye, sınıf, prestij) sidebar'ı yenilemesi için ortak tetikleyici.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/command/PanelCommand.java` - Yeni: `/panel` komutu, sidebar aç/kapat toggle.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/feedback/FeedbackService.java` - XP ilerleme BossBar'ı için yeni metot imzası.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/feedback/BossBarFeedbackService.java` - XP ilerleme BossBar implementasyonu; `celebrateLevelUp`'ın title/bossbar yerine chat mesajına dönüştürülmesi.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/feedback/FeedbackSettings.java` - Yeni XP bar gizlenme süresi alanı.
- `core/src/main/resources/feedback.yml` - Yeni XP bar süresi anahtarı (additive, mevcut anahtarlar korunur).
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/progression/ProgressionListener.java` - Her XP kazanımında BossBar tetikleme; level-up dalının chat-only olduğunu doğrulama.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/progression/ProgressionService.java` / `YamlProgressionService.java` - Admin "seviyeyi doğrudan ayarla" mutasyon metodu.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/skills/SkillListener.java` - Skill XP kazanımlarında BossBar tetikleme.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/skills/SkillService.java` / `CachedSkillService.java` - Admin "skill seviyesini doğrudan ayarla" mutasyon metodu.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/profile/ProfileService.java` / `CachedProfileService.java` - Admin "istatistiği doğrudan ayarla" mutasyon metodu.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/classes/ClassService.java` / `YamlClassService.java` - Admin "sınıfı zorla ayarla/temizle" mutasyon metodu (bekleme süresini atlar).
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/quests/QuestListener.java` - Görev XP ödülünde BossBar tetikleme.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/collections/CollectionListener.java` - Koleksiyon XP ödülünde BossBar tetikleme.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/command/PrestigeCommand.java` / `core/src/main/java/com/halilibrahim/halilibrahimmmo/gui/PrestigeMenu.java` - Prestij XP/reset sonrası BossBar ve sidebar tetikleme.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/abilities/AbilityService.java` / `YamlAbilityService.java` - Seçili yetenek durumu (`select`/`selected`), glint + geri sayım için süre/beklenen bilgiye erişim.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/abilities/AbilityActivationListener.java` - Yeni: sağ tık aktivasyonu, glint uygulama/kaldırma, action bar geri sayımı.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/gui/AbilitiesMenu.java` - Tıklama davranışı: aktivasyon yerine seçim + glint vurgusu.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/command/subcommand/PlayerAdminSubCommand.java` - Yeni: `/rpg oyuncu ...` alt komutları (`bilgi`, `seviye`, `xp`, `sinif`, `yetenek`, `istatistik`).
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/command/RpgCommand.java` - `permission()` override'ı kaldırıldı (bkz. 4.2 mimari notu); dallanma öncesi tek bir üst-düzey gate artık yok.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/profile/ProfileRepository.java` - Yeni `setStatistic` (doğrudan atama, `incrementStatistic`'ten farklı).
- `core/src/main/resources/paper-plugin.yml` - Yeni `halilibrahimmmo.admin.oyuncu` (geçiş, `default: true`) ve altı `halilibrahimmmo.admin.oyuncu.<eylem>` (`default: op`) izin düğümü.
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/locale/MessageKey.java` - Sidebar, XP BossBar, level-up chat, yetenek action bar ve admin komutları için yeni sabitler.
- `core/src/main/resources/messages.yml` - Yukarıdaki `MessageKey` sabitlerinin Türkçe karşılıkları (`sidebar.*`, `feedback.*`, `kabiliyet.*`, `rpg.oyuncu.*`).
- `core/src/main/java/com/halilibrahim/halilibrahimmmo/HalilIbrahimMMOPlugin.java` - Yeni servislerin/dinleyicilerin/komutların `onEnable()` içinde bağımlılık sırasına göre kaydı.
- `core/src/test/java/.../progression/AdminLevelAdjustmentTest.java` (veya benzer) - Admin seviye ayarlama mantığının saf-mantık testi (mevcut `ExperienceCurveTest` deseniyle).
- `PROJECT_MEMORY.md` - Yeni "Phase 8" bölümü: mimari kararlar ve doğrulama notları.
- `README.md` - Sidebar, XP BossBar, yetenek seçim/aktivasyon, admin komutları açıklamaları.

### Notes

- Yeni migration/veritabanı sütunu **yok**: sidebar aç/kapat tercihi ve seçili yetenek durumu kalıcı değildir, yalnızca oturum-içi (in-memory) tutulur; sunucu her yeniden başlatıldığında sidebar varsayılan olarak açık gelir (kullanıcı kararı).
- Her yeni oyuncu/admin metni `MessageKey` + `messages.yml` üzerinden gider; Java'da hiçbir Türkçe literal kurulmaz (§5.4).
- Admin mutasyon metotları yalnızca `core` içinde kalır, `api` modülüne sızdırılmaz (mevcut minimal-API deseni).
- `./gradlew test` (hızlı paket) her görev grubu sonunda; `./gradlew runServer` yalnızca 5.0'da canlı doğrulama için.

## Tasks

- [x] 1.0 Profil sidebar'ı (scoreboard) ve `/panel` komutu
  - [x] 1.1 Yeni `scoreboard` paketi: `SidebarService` arayüzü (`PluginService` genişletir) ve `ScoreboardSidebarService` implementasyonu (Bukkit `Scoreboard`/`Objective`, `DisplaySlot.SIDEBAR`).
  - [x] 1.2 Oyuncu başına in-memory aç/kapat tercihi (`ConcurrentHashMap<UUID, Boolean>`, varsayılan `true`, quit'te evict) — kalıcılık yok.
  - [x] 1.3 Sidebar içerik render mantığı: `ProfileService#cached`/`ClassService` senkron okumalarından oyuncu adı, sınıf görünen adı (yoksa "Sınıf seçilmedi"), seviye, XP/sonraki-seviye-XP, prestij; sabit satır sırası.
  - [x] 1.4 `messages.yml`'e yeni `sidebar.*` bölümü ve `MessageKey.java`'ya karşılık gelen sabitler.
  - [x] 1.5 Join'de sidebar'ı göster (varsayılan açık), quit'te durumu temizle (yeni `SidebarListener` veya mevcut bir listener'a ekleme).
  - [x] 1.6 Güncelleme tetikleyicileri: XP kazanımı, seviye atlama, sınıf değişimi, prestij sonrası — ilgili oyuncunun region scheduler'ında sidebar'ı yeniden çiz.
  - [x] 1.7 `/panel` komutu (`PanelCommand implements BasicCommand`): yalnızca oyuncu, tercihi tersine çevirir, Türkçe onay mesajı gönderir.
  - [x] 1.8 `HalilIbrahimMMOPlugin#onEnable`'a `SidebarService` servis kaydı (ProfileService/ClassService'den sonra) ve `/panel` komut kaydı.
  - [x] 1.9 `./gradlew build` ve `./gradlew test` çalıştır, yeşil olduğunu doğrula. Kullanıcının kendi makinesinde (JDK 26) çalıştırıldı: `BUILD SUCCESSFUL`.

- [x] 2.0 XP BossBar'ı ve kişiye özel seviye atlama chat mesajı
  - [x] 2.1 `FeedbackService`/`BossBarFeedbackService`'e XP ilerleme BossBar metodu ekle (oyuncu başına tek bar örneği günceller, yeni açmaz).
  - [x] 2.2 `feedback.yml`'e XP bar gizlenme süresi anahtarı ekle (additive, mevcut anahtarlar korunur).
  - [x] 2.3 `celebrateLevelUp`'ı BossBar/Title'dan çıkarıp yalnızca ilgili oyuncuya özel chat mesajına dönüştür; yeni `MessageKey` ekle.
  - [x] 2.4 `ProgressionListener#award`'ı her XP kazanımında (level atlasa da atlamasa da) XP BossBar'ını tetikleyecek şekilde güncelle; kaynak etiketini `ProgressionCause`'dan Türkçe çöz (her sebep için ayrı, tam yazılmış `MessageKey` şablonu — mevcut `FEEDBACK_QUEST_BOSSBAR` vb. deseniyle tutarlı).
  - [x] 2.5 `SkillListener`/skill XP yolunu da aynı BossBar'ı skill `displayName`'iyle tetikleyecek şekilde güncelle.
  - [x] 2.6 `QuestListener`/`CollectionListener`'ın `progression.award(...)` çağrılarına da aynı XP BossBar'ı eklendi (ayrıca sidebar da yenileniyor). **Kapsam düzeltmesi:** PRD'nin Kapsam Dışı bölümü gereği `PrestigeCommand`/`PrestigeMenu`'ye dokunulmadı — prestij XP kazandırmıyor, sıfırlıyor; mevcut `celebratePrestige` bossbar'ı değişmeden kalıyor (1.6'da eklenen sidebar yenilemesi zaten yeterli).
  - [x] 2.7 `MessageKey.java` + `messages.yml`'e yeni anahtarları ekle (BossBar XP metni, kaynak etiketleri, level-up chat mesajı). Python ile 166 `MessageKey` yolunun tamamı `messages.yml`'de doğrulandı, çakışma/duplikasyon yok.
  - [x] 2.8 `./gradlew build` ve `./gradlew test` çalıştır, mevcut testlerin bozulmadığını doğrula. Kullanıcının kendi makinesinde (JDK 26) çalıştırıldı: `BUILD SUCCESSFUL`.

- [x] 3.0 Menüden yetenek seçimi + sağ tıkla aktivasyon + glint + action bar geri sayımı
  - [x] 3.1 `AbilityService`'e seçili yetenek durumu ekle (`select(Player, String)`, `selected(UUID)`, in-memory). Ayrıca yeni `AbilityToolTag` enum (PICKAXE/AXE/SHOVEL/SWORD/HOE, Bukkit `Tag` üzerinden), `ActiveAbilityStatus` record ve `AbilityDefinition`'a `tool` alanı; `abilities.yml`'e her kabiliyet için `arac.tur` eklendi.
  - [x] 3.2 `AbilitiesMenu#handleClick`'i aktivasyon yerine seçim yapacak şekilde değiştir; seçili yeteneğin ikonuna glint uygula, seçim sonrası menü yeniden açılır.
  - [x] 3.3 Yeni `AbilityActivationListener` (`PlayerInteractEvent`): ana el kontrolü, seçili yetenek + uygun alet (`AbilityToolTag`) kontrolü, uygunsa mevcut `AbilityService#activate(...)` çağrısı.
  - [x] 3.4 Aktivasyon anında elindeki alete glint uygula (`YamlAbilityService#applyGlint`); süre bitince veya erken tüketimde (`handleBlockBreak` başarı dalı) kaldırılıyor; alet bırakma/slot değişimi/ayrılma `AbilityActivationListener` üzerinden temizleniyor (basitleştirme: koşulsuz temizlik, alt bölümdeki nota bakın).
  - [x] 3.5 Action bar geri sayımı: saniyede bir güncellenen `player.getScheduler().runAtFixedRate(...)` görevi; `PlayerEnergyService` artık yetenek geri sayımı aktifken kendi action bar'ını bastırıyor (`actionBarSuppressed` predicate).
  - [x] 3.6 Tek kullanımlık yeteneklerin bekleme penceresi zaten aynı `armed` haritası + `activeMillis` üzerinden yürüdüğü için glint+geri sayım otomatik olarak kapsanıyor, ek kod gerekmedi.
  - [x] 3.7 `messages.yml`/`MessageKey.java`'ya `ABILITY_SELECTED`, `ABILITY_COUNTDOWN_ACTIONBAR`, `GUI_ABILITY_SELECTED_MARKER` eklendi.
  - [x] 3.8 `HalilIbrahimMMOPlugin#onEnable`'a `AbilityActivationListener` kaydedildi; `PlayerEnergyService`/`ClassListener` constructor'ları güncellendi.
  - [x] 3.9 `/kabiliyet <id>` (`AbilityCommand`) hiç değiştirilmedi, `activate(...)` imzası aynı kaldı — regresyon yok.
  - [x] 3.10 `./gradlew build`/`test` kullanıcının kendi makinesinde (JDK 26) çalıştırıldı: `BUILD SUCCESSFUL`. `MessageKey`/`messages.yml`/`abilities.yml` tutarlılığı ayrıca script ile doğrulandı.

  **Ek not (kapsam dışı ama düzeltilen bir hata):** `AbilityService#handleEntityDamage` (Avcı Odağı'nın hasar çarpanı) Faz 3'ten beri hiçbir listener tarafından çağrılmıyordu — canlı bir hata. `ClassListener` zaten aynı `EntityDamageByEntityEvent`'i dinlediği için oraya tek satır eklendi.
  **Bilinçli sadeleştirme:** Alet bırakma/slot değiştirme geri bildirimi koşulsuz temizliyor (hangi item'ın glint taşıdığını takip etmiyor) — kabul edilebilir görsel sadeleştirme, oyun mantığını (armed/buff state) etkilemiyor.

- [x] 4.0 Admin oyuncu yönetimi komutları (`/rpg oyuncu ...`)
  - [x] 4.1 Yeni `PlayerAdminSubCommand` (`SubCommand`, `name() = "oyuncu"`), alt-eylemleri (`bilgi`, `seviye`, `xp`, `sinif`, `yetenek`, `istatistik`) elle ayrıştırır.
  - [x] 4.2 Her alt-eylem için ayrı, varsayılanı OP olan permission node (`halilibrahimmmo.admin.oyuncu.*` deseni). **Mimari not:** `SubCommand#permission()` tek bir düğüm döndürür ve `RpgCommand` bunu subcommand'a dallanmadan ÖNCE kontrol eder; altı bağımsız devredilebilir düğüm barındırmak için `PlayerAdminSubCommand.permission()` geçiş amaçlı, herkese açık bir düğüm döndürüyor (`halilibrahimmmo.admin.oyuncu`, `default: true`) ve gerçek yetki kontrolü her eylemde elle yapılıyor (`sender.hasPermission("halilibrahimmmo.admin.oyuncu." + eylem)`). Ayrıca `RpgCommand.permission()` override'ı tamamen kaldırıldı: eskiden `/rpg`'nin tamamını `halilibrahimmmo.admin`'e kilitliyordu, bu da yalnızca `halilibrahimmmo.admin.oyuncu.bilgi` gibi tek bir alt düğüm verilmiş bir personelin `/rpg`'ye hiç erişememesine yol açardı (Bukkit'te alt düğümler üst düğümü ima etmez). Diğer alt komutlar (`yenile`/`versiyon`/`hataayiklama`) zaten kendi `permission()` kontrolünü ayrıca yaptığından davranışları değişmedi.
  - [x] 4.3 Hedef oyuncu çevrimdışıysa Türkçe hata mesajı (`Bukkit.getPlayerExact` ile senkron kontrol).
  - [x] 4.4 `bilgi`: `ProfileService#cached`/`ClassService#displayName`/`SkillService#cached` senkron verilerinden profil/sınıf/beceri özetini birleştirip gösterir (hedef çevrimiçi olduğundan zaten önbellekte).
  - [x] 4.5 `seviye <değer>`: `ProgressionService`'e yeni `setLevel(playerId, playerName, level)` admin mutasyon metodu eklendi (seviyeyi `curve().maximumLevel()` ile kelepçeler, XP'yi 0'a sıfırlar). Bilinçli olarak unvan/kozmetik değerlendirmesi ve seviye-atlama kutlaması TETİKLEMİYOR — bu bir oyun XP yolu değil, kaba bir düzeltme aracı.
  - [x] 4.6 `xp ver <miktar>`: mevcut `ProgressionService.award(..., ProgressionCause.EXTERNAL)` yolunu kullanır; `ProgressionListener#award`'daki aynı desen tekrarlandı — seviye atlarsa `celebrateLevelUp`/`cosmetics.celebrate` + `titles.evaluate`/`cosmetics.evaluate` ile unvan/kozmetik bildirimleri de tetikleniyor. `EXTERNAL` sebebi zaten XP BossBar şablonunda `null`'a eşleniyor (mevcut `ProgressionListener` switch'i), yani admin XP'si organik XP kazanımlarıyla aynı BossBar gürültüsünü yaratmıyor.
  - [x] 4.7 `sinif <id|temizle>`: `ClassService`'e yeni `forceSetClass(playerId, playerName, classIdOrEmpty)` admin-only metodu eklendi (`select()`'in bekleme-süresi kontrolünü atlar, harcanan yetenek puanlarını korur, `repository.saveCooldown(playerId, 0L)` ile bekleme sıfırlanır). Komut çağırmadan önce sınıf id'sini senkron doğruluyor (`CLASS_UNKNOWN` mesajıyla erken çıkış).
  - [x] 4.8 `yetenek <id> seviye <değer>`: `SkillService`'e yeni `setLevel(playerId, skillId, level)` admin-only metodu eklendi (`curve().maximumLevel()` ile kelepçeler, XP'yi 0'a sıfırlar, mevcut `awardMatching`'in zincir-güvenli deseniyle aynı `updateChains` mekanizmasını kullanır).
  - [x] 4.9 `istatistik <anahtar> <değer>`: `ProfileRepository`'e yeni `setStatistic` (doğrudan UPDATE-then-INSERT atama, mevcut toplamalı `incrementStatistic`'ten farklı) ve `ProfileService`/`CachedProfileService`'e karşılık gelen sözleşme/implementasyon eklendi.
  - [x] 4.10 Her başarılı işlemde admine Türkçe onay (`messages.send`) + `PluginLogger#info` üzerinden Türkçe denetim satırı (hangi admin, hangi oyuncu, ne değiştirildi).
  - [x] 4.11 Admin değişikliklerinin sidebar'a yansıması doğrulandı: `seviye`/`xp ver`/`sinif` eylemleri hedefin scheduler'ında `sidebar.refresh(target)` çağırıyor (seviye/XP/sınıf sidebar'da gösteriliyor); `yetenek`/`istatistik` sidebar'da gösterilmediği için kasıtlı olarak yenileme yapmıyor. Tüm mutasyonlar zaten yazma-geçişli (`CachedProfileService`/`CachedSkillService`/`YamlClassService`) önbellekler üzerinden gittiği için başka hiçbir okuma yolu bayat veri görmüyor.
  - [x] 4.12 `messages.yml`/`MessageKey.java`'ya `rpg.oyuncu.*` bölümü (12 yeni anahtar: `kullanim`, `cevrimdisi`, `gecersiz-sayi`, `bilgi`, `bilgi-sinifsiz`, `seviye-ayarlandi`, `xp-verildi`, `sinif-ayarlandi`, `sinif-temizlendi`, `bilinmeyen-beceri`, `beceri-ayarlandi`, `istatistik-ayarlandi`) eklendi.
  - [x] 4.13 `HalilIbrahimMMOPlugin#onEnable`'daki `subCommands` listesine `PlayerAdminSubCommand` eklendi (mevcut `profileService`/`progressionService`/`classService`/`skillService`/`sidebarService`/`titleService`/`cosmeticService`/`feedbackService`/`messageService`/`pluginLogger` referansları geçirildi, yeni servis bağımlılığı yaratılmadı).
  - [x] 4.14 `./gradlew build`/`./gradlew test` kullanıcının kendi makinesinde (JDK 26) çalıştırıldı: `BUILD SUCCESSFUL` (build 12s, test 837ms). Python ile 181 `MessageKey` yolunun tamamı `messages.yml`'de doğrulandı (0 eksik, 0 çakışma/duplikasyon). Saf-mantık birim testi eklenmedi (admin mutasyonları `PlayerProfile`/`SkillProgress` gibi mevcut, zaten test edilebilir kayıtları yeniden kullanıyor; ayrı bir test dosyası mevcut `ExperienceCurveTest` deseniyle 5.0'da eklenebilir).

- [ ] 5.0 Uçtan uca doğrulama ve dokümantasyon güncellemesi
  - [x] 5.1 `./gradlew build` ve `./gradlew test` tam paket yeşil. Kullanıcının kendi makinesinde (JDK 26) doğrulandı: `BUILD SUCCESSFUL` (her ikisi de).
  - [ ] 5.2 `./gradlew runServer` ile canlı doğrulama: sidebar, `/panel`, XP BossBar, kişiye özel seviye atlama chat mesajı, menüden yetenek seçimi + sağ tık aktivasyon + glint + action bar geri sayımı, admin komutları. **Kullanıcı kendi yapacak** — aşağıya bakın.
  - [x] 5.3 `PROJECT_MEMORY.md`'ye yeni "Phase 8" bölümü (§22) eklendi; §14 de Phase 8'in tamamlandığını belirtecek şekilde güncellendi. Mevcut içerik silinmedi/çelişmedi.
  - [x] 5.4 `README.md` yeni "Profile sidebar" ve "Admin commands" bölümleriyle, ayrıca "Skills and abilities"/"Visual feedback"/"Inventory menu" bölümlerinin güncel davranışı yansıtacak şekilde revize edilmiş haliyle güncellendi.
  - [x] 5.5 Bu görev listesindeki tüm maddeler `[x]` — yalnızca 5.2 (canlı `runServer` doğrulaması, kullanıcı tarafından elle yapılacak) `[ ]` kalıyor.
