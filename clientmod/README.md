# hmmo-client — HalilIbrahimMMO eşlik istemci mod'u

Sunucunun zorunlu mod kontrolüne yanıt veren ve sunucunun bildirdiği
**aktif kabiliyeti ekranda gösteren** küçük Fabric istemci mod'u.

İki kanal dinler:

| Kanal | Yön | Ne yapar |
|---|---|---|
| `hmmo:modcheck` | sunucu ↔ istemci | Gelen isteğe yüklü mod listesini yollar (Faz 18) |
| `hmmo:hud` | sunucu → istemci | Çalışan kabiliyeti bildirir (Faz 22) |

`hmmo:hud` üzerinden gelen bilgiyle üç şey çizilir:

1. **Ekran kenarı renklenir** — kabiliyetin kendi rengiyle, kademesine göre
   (10/20/35/50 → %25/%45/%70/%100 yoğunluk).
2. **Kabiliyet rozeti** — simgesi, adı ve kalan süresi; hem HUD'un sol
   üstünde hem de envanter açıkken vanilla'nın iksir etkilerini listelediği
   sütunda.
3. **Kan Yemini açıkken** çevredeki düşman yaratıklar kırmızı parlar.

Bunlar **yalnızca sunum**tur. Mod'un hiçbir ekranı, komutu, tuş bağlaması ya
da ayar dosyası yoktur ve oyunun kurallarına karışmaz: bu paketlerin hepsini
yok sayan bir istemci aynı oyunu daha sade bir ekranla oynar. Otorite
tamamen sunucudadır.

**Neden istemcide?** Sunucu vanilla bir istemcinin HUD'una çizemez — "ekranı
boya" ya da "bu yaratığı yalnızca senin için parlat" diye bir paket yoktur.
Bukkit'in parlama bayrağı yaratığın kendi özelliğidir, bir izleyicinin değil;
sunucudan yapılsa görüş mesafesindeki herkes için parlardı.

## Neden ayrı bir proje?

Bu klasör kök `settings.gradle.kts`'e **dahil değildir**. Kök projedeki
`./gradlew build` ve `shadowJar` bu mod'dan hiç etkilenmez — farklı bir
platform, farklı bir araç zinciri (Fabric Loom).

## Derleme

Bu klasörün kendi Gradle wrapper'ı vardır (kök projeninkinin kopyası,
Gradle 9.6.1).

PowerShell:

```powershell
cd C:\Users\fxrkqn\Desktop\halilibrahimMMO\clientmod
.\gradlew.bat build
```

PowerShell'de `./gradlew` çalışmaz — Windows'ta çalıştırılabilir dosya
`gradlew.bat`'tır ve ters bölü ile çağrılır. Alternatif olarak kök
klasörden de derleyebilirsiniz:

```powershell
cd C:\Users\fxrkqn\Desktop\halilibrahimMMO
.\gradlew.bat -p clientmod build
```

`clientmod` kendi `settings.gradle.kts` dosyasına sahip olduğu için bu
komut yine bağımsız bir derleme başlatır; kök projeyi etkilemez.

Çıktı: `build/libs/hmmo-client-<sürüm>.jar`

`JAVA_HOME` JDK 26'yı göstermelidir (kök projeyle aynı kural).

## Yapılandırmanın kaynağı

Build dosyaları tahminle değil, **Fabric'in 26.2 için ürettiği resmi
şablondan** (fabricmc.net/develop/template, Kotlin buildscript) birebir
doğrulanarak yazıldı:

| Ayar | Değer | Not |
|---|---|---|
| Plugin id | `net.fabricmc.fabric-loom` | Eski `fabric-loom` id'si değil |
| Loom | `1.17-SNAPSHOT` | |
| Gradle | `9.5.1` | Şablonun wrapper'ı |
| Java | **25** | 26.2 manifest'i major 25 istiyor |
| `mappings(...)` | **yok** | 26.x unobfuscated, eşlenecek bir şey yok |
| Bağımlılık | `implementation` | `modImplementation` **değil** — remap yok |

Ağ API'si sınıf adları da Fabric'in güncel Networking dokümanından
doğrulandı: `CustomPacketPayload`, `CustomPacketPayload.Type`,
`StreamCodec`, `RegistryFriendlyByteBuf`, `Identifier` (26.x'te
`ResourceLocation` **değil**), `PayloadTypeRegistry.clientboundPlay()` /
`serverboundPlay()` (eski `playS2C()`/`playC2S()` değil).

## HUD API'si — 26.2'de neyin nerede olduğu

Faz 22'de eklenen çizim kodu, tahminle değil **derlenmiş 26.2 jar'ları
`javap` ile okunarak** yazıldı. 26.x'te HUD mimarisi değişti; eski
`HudRenderCallback` artık yok:

| İş | 26.2'deki API |
|---|---|
| HUD'a katman eklemek | `HudElementRegistry.addLast(Identifier, HudElement)` |
| Çizim yüzeyi | `HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker)` |
| Dikdörtgen / yazı / eşya | `GuiGraphicsExtractor#fill / #text / #item` |
| Açık ekranın üstüne çizmek | `ScreenEvents.AFTER_INIT` → `ScreenEvents.afterExtract(screen)` |
| Yaratık parlatma | `Entity#setGlowingTag(boolean)` |
| Parlama rengi | istemci tarafı takım + `PlayerTeam#setColor(Optional<TeamColor>)` |

**Bilinçli sınır:** `AbstractContainerScreen`'in `leftPos`/`topPos` alanları
`protected`. Bunlara ulaşmak için mixin ya da access widener gerekirdi;
ikisi de her oyun güncellemesinde kırılabilecek yapı. Panel her zaman
ortalandığı için rozetin yeri ekran boyutundan hesaplanıyor (176x166 varsayımı).
Daha geniş bir konteynerde rozet kenardan biraz daha uzağa düşer, o kadar.

## Sürüm kuralı — dikkat

Mod'un `id` değeri (`hmmo-client`) **kalıcıdır**. Yayınlandıktan sonra
değiştirilirse mod'u kurmuş her oyuncu doğrulamayı geçemez.

Yeni bir sürüm çıkarırken sıra şudur:

1. Yeni sürümü mod kurulum uygulamasına ekle,
2. **sonra** sunucudaki `modlar.yml` içindeki `minimum-versiyon` değerini
   yükselt.

Ters sırada yapılırsa, kurulum uygulamasını dürüstçe çalıştıran oyuncular
bile atılır.

Faz 22 mod'u **1.1.0**'a çıkardı; `modlar.yml`'deki minimum bilerek
`1.0.0`'da bırakıldı. 1.0.0 istemcisi `hmmo:hud` paketlerini sessizce yok
sayar ve oyunu eskisi gibi oynar, yani acele etmeye gerek yok.
