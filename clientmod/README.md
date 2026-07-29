# hmmo-client — HalilIbrahimMMO eşlik istemci mod'u

Sunucunun zorunlu mod kontrolüne yanıt veren küçük Fabric istemci mod'u.
Oyuna hiçbir ekran, komut, tuş bağlaması veya ayar eklemez; tek işi
`hmmo:modcheck` kanalından gelen isteğe yüklü mod listesini yollamaktır.

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

## Kalan doğrulanmamış nokta

Mod bu depoda henüz derlenmedi. Geriye tek belirsizlik kaldı:
`StreamCodec.of(encoder, decoder)` imzası ve
`net.minecraft.network.RegistryFriendlyByteBuf` paket yolu. İkisi de
dokümandaki örneklerle tutarlı; hata verirse `CustomPacketPayload.codec(...)`
yardımcısı denenmeli.

## Sürüm kuralı — dikkat

Mod'un `id` değeri (`hmmo-client`) **kalıcıdır**. Yayınlandıktan sonra
değiştirilirse mod'u kurmuş her oyuncu doğrulamayı geçemez.

Yeni bir sürüm çıkarırken sıra şudur:

1. Yeni sürümü mod kurulum uygulamasına ekle,
2. **sonra** sunucudaki `modlar.yml` içindeki `minimum-versiyon` değerini
   yükselt.

Ters sırada yapılırsa, kurulum uygulamasını dürüstçe çalıştıran oyuncular
bile atılır.
