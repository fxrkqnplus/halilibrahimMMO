# PROJECT_MEMORY.md

> **Read this file completely before starting any new phase.** It is the single
> source of truth for every architectural decision made so far. Nothing recorded
> here should ever be contradicted in a later phase without updating this file and
> explaining why.

---

## 1. Project identity

| Item | Value |
|---|---|
| Plugin name | `HalilIbrahimMMO` |
| Root Java package | `com.halilibrahim.halilibrahimmmo` |
| Target platform | Paper **only** (never Bukkit/Spigot/Forge/Fabric) |
| Target Minecraft/Paper version | `26.2` (verified against PaperMC's live Fill API - build 60+ exists; we build against `26.2.build.60-beta`) |
| Java version | 26 (language level, toolchain-pinned) |
| Build tool | Gradle 9.6.1, Kotlin DSL, wrapper committed to the repo |
| Economy | **None.** No money/coins/banks/Vault anywhere, ever. Progression is levels/XP/prestige/quests/collections only. |
| Player-facing language | 100% Turkish (messages, GUIs, commands, config comments, admin messages) |
| Code language | English (packages, classes, methods, variables, Javadoc, exception messages) |

This split is deliberate and permanent: **anything a human reads on their screen in
Minecraft or in `config.yml`/`messages.yml` comments is Turkish. Anything only a
programmer reads (source code, Javadoc, stack traces, internal exception
messages) is English.**

---

## 2. Verified real-world facts about the target platform

These were confirmed live against PaperMC's infrastructure during Phase 1 (not
assumed) because "Minecraft 26.2" looked unusual at first glance - it is real:

- Minecraft/Paper moved to a year-based version scheme; `26.2` is a real,
  currently-supported Paper version (`https://fill.papermc.io/v3/projects/paper`).
- Paper API artifacts for `26.2` are published as **immutable per-build versions**,
  not a single overwritable `-R0.1-SNAPSHOT` like older Paper versions
  (`io.papermc.paper:paper-api:26.2.build.60-beta`), from
  `https://repo.papermc.io/repository/maven-public/`.
- Paper 26.2 requires Java 25 minimum; we target Java 26.
- The `paper-plugin.yml` + `PluginLoader` + `MavenLibraryResolver` API confirmed
  against Paper 26.x Javadoc (`jd.papermc.io/paper/26.2/...`) and PaperMC's own docs.
- `BasicCommand` + `LifecycleEvents.COMMANDS` / `JavaPlugin#registerCommand(...)`
  confirmed as the only command registration path for `paper-plugin.yml`-based
  plugins (legacy `commands:` YAML section does not exist for this format).

**Lesson for future phases:** when a server/API detail in a future instruction
looks surprising, verify it against `https://fill.papermc.io` and
`https://repo.papermc.io` before assuming it is wrong. Do not silently
"correct" version numbers that were provided.

---

## 3. Module layout

Two Gradle subprojects, aggregated from the root:

```
halilibrahimMMO/
├── api/    - public developer API, zero internal dependencies
└── core/   - full plugin implementation, depends on :api
```

**Why two modules:** third-party plugins must be able to `compileOnly` a small,
stable, dependency-free jar (`api`) instead of the entire implementation. `core`'s
`shadowJar` merges `api`'s compiled classes into the final shipped jar (Shadow is
**only** used for this - never for bundling third-party libraries, see §5).

Every future phase adds new **packages** inside `core` (e.g. `skills`, `abilities`,
`classes`, `quests`, `collections`, `gui`, ...). We do **not** create a new Gradle
module per system - that would fragment the build for no real isolation benefit at
this project's scale. Only `api` vs `core` is a hard module boundary.

---

## 4. Package structure (as of Phase 1)

```
com.halilibrahim.halilibrahimmmo/                (core module, main package)
├── HalilIbrahimMMOPlugin.java                    - JavaPlugin entry point / bootstrap wiring only
├── HalilIbrahimMMOLoader.java                     - PluginLoader, declares runtime Maven libraries
├── HalilIbrahimMMOApiImpl.java                    - HalilIbrahimMMOAPI implementation
├── core/                                          - cross-cutting framework, no gameplay logic
│   ├── PluginService.java                         - enable()/disable()/reload() lifecycle contract
│   ├── ServiceContainer.java                       - manual/reflection-free DI container
│   └── LifecycleException.java
├── config/
│   ├── ConfigService.java / YamlConfigService.java
│   ├── CoreConfig.java                             - immutable config.yml snapshot (record)
│   └── DatabaseSettings.java                       - immutable veritabani.* snapshot (record)
├── locale/
│   ├── MessageService.java / MiniMessageService.java
│   └── MessageKey.java                             - enum of every player/admin-facing string
├── logging/
│   └── PluginLogger.java                           - SLF4J wrapper + debug-mode gate
├── database/
│   ├── DatabaseType.java                           - SQLITE, MYSQL, MARIADB, POSTGRESQL
│   ├── DatabaseService.java / HikariDatabaseService.java
│   ├── dialect/                                    - SqlDialect + one impl per engine family
│   └── migration/                                  - Migration, MigrationRunner, CoreSchemaMigration
├── command/
│   ├── RpgCommand.java                             - /rpg umbrella BasicCommand
│   ├── SubCommand.java
│   └── subcommand/                                 - ReloadSubCommand, VersionSubCommand, DebugToggleSubCommand
└── util/
    └── Async.java                                  - GlobalRegionScheduler hop-back helper

com.halilibrahim.halilibrahimmmo.api/              (api module)
├── HalilIbrahimMMOAPI.java                         - public facade interface
├── HalilIbrahimMMOProvider.java                    - ServicesManager-based static accessor
└── exception/ServiceUnavailableException.java
```

**Rule for future phases:** every new subsystem gets its own top-level package
under `com.halilibrahim.halilibrahimmmo` (e.g. `skills`, `abilities`, `quests`),
mirroring the `config`/`database`/`locale` pattern: an interface extending
`PluginService`, one primary implementation, and a `dialect`/`migration`-style
sub-package only if the system genuinely needs one. Never put gameplay logic
directly in `HalilIbrahimMMOPlugin`.

---

## 5. Key architectural decisions

### 5.1 Dependency injection: manual `ServiceContainer`, not a framework
`core.ServiceContainer` is a `Map<Class<?>, Object>`-backed service locator,
populated once, in order, in `HalilIbrahimMMOPlugin#onEnable()`. No reflection, no
classpath scanning, no proxies. This satisfies "DI where appropriate" while
respecting "no reflection unless required" and keeping startup cost near zero -
important for a plugin targeting 500+ concurrent players. Every subsystem is
registered and resolved by **interface** type (`ConfigService`, `DatabaseService`,
`MessageService`, ...), never by concrete class.

`HalilIbrahimMMOPlugin#startService(...)` enables a service, registers it in the
container, and appends it to an ordered list used for reverse-order shutdown in
`onDisable()`. **Registration order in `onEnable()` is the dependency order** -
`ConfigService` → `MessageService` → `DatabaseService`. When adding a new service
that depends on an existing one, register it *after* its dependency.

### 5.2 Runtime libraries via Paper `PluginLoader`, not shading
`HalilIbrahimMMOLoader implements PluginLoader` declares HikariCP + all four JDBC
drivers (SQLite/MySQL/MariaDB/PostgreSQL) through `MavenLibraryResolver`. Paper
downloads them into the server's shared `libraries/` cache at startup and hands
them to the plugin's runtime classloader - **verified working in Phase 1's local
`runServer` test** (Paper's `[MavenLibraryResolver]` log lines showed every
coordinate being resolved and the plugin loading successfully).

**Critical duality to remember:** these libraries are declared **twice**, for two
different reasons, and both are required:
1. `core/build.gradle.kts` → `compileOnly(...)` - so `javac` can resolve
   `com.zaxxer.hikari.*` / `org.sqlite.*` / etc. at compile time.
2. `core/src/main/resources/runtime-libraries.txt` (generated from the same
   `gradle.properties` versions via `expand()`) - read by `HalilIbrahimMMOLoader`
   at plugin-load time to fetch the actual jars for the running server.

`compileOnly` guarantees Shadow never bundles these into the shipped jar (only
`runtimeClasspath` dependencies get shaded, and `:api`'s compiled classes are the
only thing on that configuration). **Never change a driver/HikariCP dependency to
`implementation`** - that would both bloat the jar and risk classloader conflicts
with other plugins.

Single source of truth for every third-party version: `gradle.properties`. Never
hardcode a version string anywhere else (build scripts read the property; the
runtime manifest is generated from it).

### 5.3 Configuration: ship-and-copy, never rewrite in place
`config.yml` / `messages.yml` are bundled resources with full Turkish comments.
`YamlConfigService`/`MiniMessageService` copy the bundled resource to the plugin
data folder **only if the file does not already exist**, then always parse from
disk. Reload (`/rpg yenile` or `HalilIbrahimMMOAPI#reload()`) re-reads from disk -
it never programmatically serializes the whole file back out, so admin edits and
comments always survive a reload. If a future phase needs to *add* new keys to an
existing config, that must be done via an explicit, additive migration step (read
old file, write only missing keys with their comments) - never a blind overwrite.

`CoreConfig` and `DatabaseSettings` are **Java records** (immutable). Every
(re)load produces a brand new `CoreConfig`, published atomically through an
`AtomicReference` in `YamlConfigService`, so concurrent readers on other threads
never see a half-updated config. The one deliberate exception is the in-memory-only
debug mode override (`/rpg hataayiklama`), applied via `CoreConfig#withDebugMode(...)`
(a wither method) and cleared on the next disk reload.

### 5.4 Locale: `MessageKey` enum + MiniMessage, never raw literal strings
Every player/admin-facing string is a `MessageKey` enum constant mapped to a
dotted path in `messages.yml`. No code anywhere calls
`sender.sendMessage("literal text")` - always
`messageService.send(audience, MessageKey.XXX, placeholders)`. `MessageService#send`
always auto-prepends `MessageKey.PREFIX`. Placeholders are inserted via
`Placeholder.unparsed(name, value)` (never `Placeholder.parsed`, to avoid players'
own data injecting MiniMessage tags in future phases where user-generated text
might flow through a placeholder).

**Rule for future phases:** adding any new player-facing text = add a new
`MessageKey` constant + a new key in `messages.yml` (Turkish, MiniMessage-formatted).
Never concatenate raw strings into a `Component` for anything a player sees.

### 5.5 Database: Hikari + `SqlDialect` + versioned `Migration`s
- `DatabaseService` is 100% asynchronous: every method returns a
  `CompletableFuture`, executed on a dedicated bounded daemon thread pool
  (`HalilIbrahimMMO-DB-*` threads) - **never** the Bukkit main thread, **never**
  Paper's region schedulers (those are reserved for gameplay code touching Bukkit
  API; see `util.Async` for hopping a database result back onto the
  `GlobalRegionScheduler` when a future phase needs to touch Bukkit API after a
  query completes).
- `SqlDialect.forType(DatabaseType)` hides syntax differences (auto-increment
  column type, current-timestamp expression) between SQLite / MySQL+MariaDB
  (share `MySqlDialect`) / PostgreSQL. Add new methods here, not `if (type == ...)`
  branches in repository code, whenever a new syntax difference is discovered.
- **One sanctioned synchronous exception:** `HalilIbrahimMMOPlugin#onEnable()`
  calls `migrationRunner.runPendingMigrations().get()` - i.e. it *waits* for
  migrations to finish before continuing startup. The actual JDBC work still
  happens on the database thread pool (never directly on the main thread); we
  merely block plugin enable on the result, which happens once, during server
  startup, before any player can connect. This is intentional and must stay the
  **only** place in the codebase that blocks on a database future.
- Every migration is a `Migration` implementation with a permanently unique
  `version()`, applied in ascending order, tracked in `hmmo_schema_version`.
  **Never renumber or delete a shipped migration.** New schema changes are always
  a *new* migration with the next version number, even if they alter a table an
  earlier migration created.
- All tables are prefixed `hmmo_` (shared-database safety on Turkish shared
  hosting where multiple plugins often share one MySQL schema).

### 5.6 Commands: `paper-plugin.yml` + `BasicCommand`, never legacy `plugin.yml`
`paper-plugin.yml` has no `commands:` section (that's a legacy-format-only
feature). Every command is registered programmatically via
`JavaPlugin#registerCommand(label, description, aliases, BasicCommand)` inside
`onEnable()`. `/rpg` is the permanent umbrella command for cross-cutting
plugin-management actions (reload, version, debug toggle, and future admin
tooling); gameplay commands (`/profil`, `/yetenekler`, `/görevler`, ...) get their
own root `BasicCommand` registrations in later phases and are never nested under
`/rpg`. Command *literals* typed by admins stay plain ASCII (e.g. `hataayiklama`,
not `hataayıklama`) for keyboard-typing convenience; all **displayed** text uses
correct Turkish diacritics.

`RpgCommand` dispatches to `SubCommand` implementations by name, checks
`SubCommand#permission()` itself (so every subcommand cannot be forgotten to be
permission-checked), and delegates unknown-subcommand/no-permission messaging to
`MessageService`. Internal admin subcommands are allowed to depend directly on
internal services (`ConfigService`, ...); they also deliberately reuse
`HalilIbrahimMMOAPI` where the functionality is equally something a third-party
plugin might want (reload, version), so the internal command and the public API
never drift apart.

---

## 6. Database schema (cumulative - update every phase, never remove old rows)

| Table | Introduced | Purpose |
|---|---|---|
| `hmmo_schema_version` | Phase 1 (`MigrationRunner`, not a `Migration` itself) | Bookkeeping: `version INTEGER PRIMARY KEY`, `description`, `applied_at`. Tracks which `Migration`s have run. |
| `hmmo_plugin_meta` | Phase 1, `CoreSchemaMigration` v1 | Generic key/value table (`meta_key` PK, `meta_value`). Proves the migration pipeline end-to-end; not yet used for real data. |
| `hmmo_player_profiles` | Phase 2, `PlayerProfileSchemaMigration` v2 | Persists player name, global level, XP, prestige and selected class. |
| `hmmo_player_statistics` | Phase 2, `PlayerProfileSchemaMigration` v2 | Extensible per-player statistic counters. |
| `hmmo_player_skills` | Phase 3, `SkillSchemaMigration` v3 | Persists one level/XP record per player and configured skill ID. |
| `hmmo_player_class_talents` | Phase 4, `ClassSchemaMigration` v4 | Persists rank allocations per player, class and configured talent branch. |
| `hmmo_player_class_cooldowns` | Phase 4, `ClassSchemaMigration` v4 | Persists the epoch-millisecond time at which a player may change class again. |
| `hmmo_player_quest_progress` | Phase 5, `QuestSchemaMigration` v5 | One row per player/quest: progress count, completed flag and the repeating quest's current period-end epoch millis. |
| `hmmo_player_collection_entries` | Phase 5, `QuestSchemaMigration` v5 | One row per player/collection/entry a player has obtained at least once. |
| `hmmo_player_journal_entries` | Phase 5, `QuestSchemaMigration` v5 | Append-only adventure journal: one row per quest completion or collection milestone, with a self-contained display name captured at write time. |
| `hmmo_player_unlocked_titles` | Phase 6 (part 3), `TitleCosmeticSchemaMigration` v6 | One row per player/title a player has ever unlocked (INSERT-only membership). |
| `hmmo_player_equipped_title` | Phase 6 (part 3), `TitleCosmeticSchemaMigration` v6 | The single title id a player currently has equipped. |
| `hmmo_player_unlocked_cosmetics` | Phase 6 (part 3), `TitleCosmeticSchemaMigration` v6 | One row per player/cosmetic a player has ever unlocked (INSERT-only membership). |
| `hmmo_player_equipped_cosmetic` | Phase 6 (part 3), `TitleCosmeticSchemaMigration` v6 | The single cosmetic id a player currently has equipped. |

Phase 2 will add the real player-profile schema as new `Migration` classes with
version `2`, `3`, ... - `CoreSchemaMigration` (v1) itself must never be edited.

---

## 7. Configuration files shipped so far

| File | Purpose |
|---|---|
| `config.yml` | `genel.*` (locale, debug mode), `veritabani.*` (database type, SQLite/server connection, Hikari pool sizing) |
| `messages.yml` | `onek` (prefix), `genel.yetkisiz`, `rpg.*` (reload/version/debug/unknown-subcommand strings) |
| `progression.yml` | Global XP cap, explicit XP curve and global event rewards. |
| `skills.yml` | Shared explicit skill XP curve plus YAML-defined skill gameplay sources and rewards. |
| `abilities.yml` | Shared ephemeral energy settings and bounded active ability definitions. |
| `classes.yml` | Configured class catalogue, shared-talent point cadence, class-change cooldown and bounded talent effects. |
| `prestige.yml` | Maximum prestige count. |
| `quests.yml` | Configured permanent/daily/weekly quest catalogue: objective (block break or mob kill), target count and XP reward. |
| `collections.yml` | Configured collection catalogue: per-entry item and XP, plus a completion bonus once every entry is obtained. |
| `leaderboards.yml` | Entry count per board and the database re-query refresh interval. |
| `titles.yml` | Configured title catalogue: display name, icon and one unlock condition per title. |
| `cosmetics.yml` | Configured cosmetic catalogue: display name, icon, particle effect and one unlock condition per cosmetic. |
| `feedback.yml` | How long the level-up/quest/collection/prestige bossbar and on-screen title stay visible. |

Both are Turkish-commented, UTF-8, copied-not-rewritten (§5.3). Future phases add
new files listed in the original brief (`skills.yml`, `quests.yml`,
`abilities.yml`, `classes.yml`, `gui.yml`, `collections.yml`, `prestige.yml`,
`leaderboards.yml`, `titles.yml`, `cosmetics.yml`) - each new system gets its own
file, never crammed into `config.yml`.

---

## 8. Dependency version catalog (`gradle.properties` - single source of truth)

| Dependency | Version |
|---|---|
| Paper API | `26.2.build.60-beta` |
| Gradle | `9.6.1` (wrapper committed) |
| Shadow plugin | `9.6.0` (`com.gradleup.shadow`) |
| run-paper plugin | `3.0.2` (`xyz.jpenilla.run-paper`) |
| HikariCP | `7.1.0` |
| sqlite-jdbc | `3.53.2.0` |
| mysql-connector-j | `9.7.0` |
| mariadb-java-client | `3.5.9` |
| postgresql (JDBC) | `42.7.13` |

Bump versions only in `gradle.properties`. Never hardcode a version literal
anywhere else (build scripts and `HalilIbrahimMMOLoader` both derive from it).

---

## 9. Coding standards / conventions established in Phase 1

- Java 26, records for immutable data (`CoreConfig`, `DatabaseSettings`), switch
  expressions for exhaustive enum handling (`SqlDialect.forType`, JDBC URL/driver
  builders in `HikariDatabaseService`).
- Every internal subsystem: `XyzService` interface extending `PluginService`, one
  primary implementation (`YamlConfigService`, `HikariDatabaseService`, ...).
- No field or parameter is ever `null`-checked defensively without reason; instead
  prefer constructors that cannot produce an invalid state and `Optional`/explicit
  exceptions where absence is meaningful.
- No comments narrating obvious code. Javadoc explains *why*, not *what*, and is
  written on every public interface/class.
- No TODOs, no placeholder method bodies. If a phase's brief doesn't need a
  capability yet, it simply isn't in the code at all until the phase that needs it.
- Exception messages and Javadoc: English. Any string that can reach a player or
  admin via chat/console/GUI: Turkish, MiniMessage-formatted, routed through
  `MessageKey/MessageService` (or `PluginLogger` for console-only lines, which are
  also written in Turkish since server admins are Turkish).
- Package-private is preferred over `public` for anything not part of a
  deliberate cross-package contract; `final` classes unless a class is explicitly
  designed for extension.

---

## 10. What Phase 1 deliberately does NOT include

Per the project's own phased brief - do not add these early, they belong to later
phases: player profiles/statistics, global XP/levels, skills, abilities, mana/
energy, classes, skill trees, prestige, quests, collections, adventure journal,
GUIs, bossbars/titles/particles, leaderboards, PlaceholderAPI/LuckPerms/other
integrations, anti-exploit systems. Phase 1 is exclusively: project scaffolding,
config/locale/logging/database frameworks, and a minimal `/rpg` diagnostic command
that proves every framework piece works together end-to-end.

---

## 11. Phase 1 verification performed

- `./gradlew build` - clean success, both modules compile, `shadowJar` produces
  `core/build/libs/HalilIbrahimMMO-1.0.0-SNAPSHOT-all.jar` containing the merged
  `:api` classes and the correctly UTF-8-substituted `paper-plugin.yml`/
  `runtime-libraries.txt`.
- `./gradlew runServer` - downloaded real Paper `26.2` build `62`, booted it, and
  confirmed via server log: `PluginInitializerManager` resolved and downloaded all
  5 runtime libraries (HikariCP + 4 JDBC drivers) through `HalilIbrahimMMOLoader`,
  and listed `Paper plugins (1): - HalilIbrahimMMO (1.0.0-SNAPSHOT)` successfully.
  Full boot-to-`Done` was not completed in this session (an unrelated local test
  environment issue - a corrupted throwaway world save from an earlier port
  conflict during iteration - blocked full world load; this is a local `run/`
  sandbox artifact, not a plugin defect, and `run/` is gitignored/disposable).

---

## 12. Phase 2 — profile and global progression

- `profile.ProfileService` / `CachedProfileService` own an immutable,
  write-through UUID profile cache. Per-player updates are chained before their
  asynchronous JDBC writes, preventing concurrent XP awards from overwriting one
  another.
- `progression.ProgressionService` / `YamlProgressionService` load a fully
  explicit level table from `progression.yml`. The shipped default caps global
  progression at level 100. XP at the cap is discarded until the prestige phase
  intentionally changes that behaviour.
- `PlayerProfileSchemaMigration` v2 creates `hmmo_player_profiles` and
  `hmmo_player_statistics`. The profile table persists player name, global level,
  XP, prestige and selected class; statistics use extensible string keys.
- Valid uncancelled block breaks, player-killed mobs and each online minute route
  through the shared progression/statistic path. `/profil` renders an asynchronous
  profile summary. The developer API now exposes immutable async profile lookup
  and controlled global-XP awards.
- `./gradlew build --console=plain` succeeds when `JAVA_HOME` is set to
  `C:\Program Files\Java\jdk-26.0.1`; Java 11 in the ambient `JAVA_HOME` cannot
  run Gradle 9.6.1. `ExperienceCurveTest` covers multi-level carry and validation.

## 13. Phase 3 — skills, abilities and energy

- `skills.CachedSkillService` owns the immutable `skills.yml` snapshot plus an
  asynchronous write-through UUID/skill cache. `SkillSchemaMigration` v3 creates
  `hmmo_player_skills`; updates for a player/skill pair are chained before their
  JDBC writes, just like global profile progression.
- The shipped four YAML skills are `kazicilik`, `odunculuk`, `madencilik` and
  `avcilik`. They share a fully explicit 1–100 XP table, but each stores separate
  XP/level values. Eligible block breaks and non-player mob kills award both the
  existing global progression reward and the relevant skill reward.
- `abilities.YamlAbilityService` validates `abilities.yml` before atomically
  publishing it. The defaults are one-shot 3×3 excavation, capped tree felling,
  capped same-ore vein mining and a timed PvE-only damage multiplier. Secondary
  block effects re-fire `BlockBreakEvent` for protection compatibility, are capped
  by YAML limits, and are marked so they never generate extra global/skill XP.
- `energy.PlayerEnergyService` is intentionally in-memory only: players join at
  full shared energy, regenerate while online on player schedulers, receive an
  action-bar update only after a value change, and are removed from memory when
  they leave. Bukkit world/entity/player access stays on Paper-owned region or
  player scheduler threads.
- `/kabiliyet <id>` is the player-facing root command. All Turkish feedback uses
  `MessageKey` / `messages.yml`. `HalilIbrahimMMOAPI` exposes immutable skill
  definitions and asynchronous read-only per-player skill snapshots; no external
  skill awards or ability activations are public yet.
- `HalilIbrahimMMOAPI#reload()` now reloads `skills.yml` and `abilities.yml` in
  addition to the earlier configuration resources. A failed parse leaves the
  previously published skill/ability snapshot active.
- `./gradlew build --console=plain` succeeds with Java 26. `SkillExperienceCurveTest`
  covers level carry/validation and `EnergyStateTest` covers energy consumption and
  capped regeneration.

## 14. Remaining phases

- Phase 6 is **complete** (see §17, §18, §19): inventory GUIs, leaderboards,
  titles, cosmetics and visual feedback are all done.
- Phase 7 is **complete** (see §20, §21): PlaceholderAPI/LuckPerms hooks,
  exploit hardening and cross-engine repository integration tests are all
  done. All originally-planned phases are now finished.

## 15. Phase 4 — classes, talent trees and prestige

- `classes.YamlClassService` owns the atomically published `classes.yml` catalogue,
  asynchronous persistent talent ranks, selected-class modifiers and the database-
  backed class-change cooldown. `prestige.YamlPrestigeService` owns the separate
  `prestige.yml` setting and resets only global level/XP when a qualifying player
  confirms prestige.
- The bundled classes are `savasci`, `avci`, `madenci` and `oduncu`. Every class
  has three four-rank talent branches; all ranks consume one shared player budget.
  The budget is `prestige * 4 + floor(globalLevel / 25)`, so existing ranks survive
  every prestige and each level-1-to-100 cycle contributes four more points.
- Class effects are deliberately bounded by `ClassEffectType`: maximum health,
  PvE damage, maximum energy, and named existing-ability energy/cooldown/limit
  modifiers. The selected class alone is active. Ability reductions are applied
  before activation, and player health/energy are reconciled safely on a player
  region scheduler after class or talent changes.
- `/sinif sec <id>`, `/sinif agac` and `/sinif ac <yetenek>` are the Phase 4
  command surface; inventory GUIs remain a Phase 6 responsibility. `/prestij`
  displays status and `/prestij onayla` performs the explicit irreversible global
  XP reset at level 100. Default class-change cooldown is 7 days and default
  maximum prestige is 10, both YAML-configurable.
- The developer API exposes read-only configured class definitions and async
  player class/talent snapshots. It intentionally exposes no mutation methods.
- `core/build.gradle.kts` now puts Paper API on the test classpath, fixing the
  pre-existing `SkillMaterialConfigTest` build regression. Unit tests cover point
  carryover, class modifier bounds, prestige rules and the reserved v4 migration.

## 16. Phase 5 — quests, collections and adventure journal

- `quests.CachedQuestService` owns the atomically published `quests.yml`
  catalogue plus an asynchronous write-through per-player-per-quest progress
  cache, mirroring `CachedSkillService`. Each `QuestDefinition` has a `QuestType`
  (`PERMANENT`, `DAILY`, `WEEKLY`) and a `QuestObjectiveType` (`BLOCK_BREAK` with
  a material set, or `MOB_KILL` with an optional entity-type set - empty means
  any non-player kill). Several quests may share the same objective event, so
  every matching quest is incremented independently, each behind its own
  per-player-per-quest serial update chain.
- `quests.QuestResetRules` is a pure, dependency-free utility (tested in
  isolation like `PrestigeRules`/`TalentPointBudget`) computing a repeating
  quest's period-end epoch millis and whether it has expired. Expiry is checked
  lazily on the next award attempt - there is no scheduled reset task - and an
  expired quest's progress and completion flag are reset in the same write that
  applies the new increment.
- `collections.CachedCollectionService` owns the `collections.yml` catalogue
  (a collection is a named set of `CollectionEntryDefinition`s, each one
  Material a player must obtain at least once) plus an asynchronous
  write-through per-player-per-collection obtained-entry-id cache. Persistence
  is a simple `INSERT`-only membership table (`hmmo_player_collection_entries`);
  correctness against duplicate/racing pickups relies entirely on each
  collection's own per-player serial update chain, the same pattern every other
  cached service in this codebase already uses - there is no database-level
  uniqueness check beyond the composite primary key.
- Both quest completion and each newly obtained collection entry (plus a
  collection's completion bonus) award global XP through the existing
  `ProgressionService.award(...)` path with `ProgressionCause.QUEST` /
  `ProgressionCause.COLLECTION` - values that were already reserved on that enum
  since Phase 2. `QuestListener`/`CollectionListener` compose the reward call
  themselves after their service call returns; `QuestService`/`CollectionService`
  never depend on `ProfileService`/`ProgressionService` directly, keeping quest
  and collection progress tracking decoupled from how it happens to be rewarded.
- `journal.DatabaseJournalService` is a thin, cache-free, purely database-backed
  service (no YAML settings, so `reload()` stays the `PluginService` default
  no-op) that appends one `hmmo_player_journal_entries` row per quest completion
  or collection milestone. Each row captures a fully self-contained,
  already-Turkish `reference_name` at write time, so a player's journal stays
  readable even if the underlying quest/collection is later renamed or removed
  from configuration. `/gunluk` reads the ten most recent rows.
- New collection entries are obtained through `EntityPickupItemEvent` (any
  `Material` a player picks up, not restricted to blocks), a Bukkit event not
  used by any earlier phase. Quest block/mob objectives reuse the same
  `BlockBreakEvent`/`EntityDeathEvent` hooks and `AbilityService.isSecondaryBreak`
  exclusion that `ProgressionListener`/`SkillListener` already established.
- `/gorev`, `/koleksiyon` (optionally `/koleksiyon <id>` for a per-entry
  checklist) and `/gunluk` are the Phase 5 command surface, all argument-light
  like `/profil`; inventory GUIs remain a Phase 6 responsibility. List-style
  output continues the established "join plain data fragments into one
  placeholder" pattern (`CLASS_LIST`/`CLASS_TREE`) rather than nesting
  MiniMessage tags inside a placeholder value, which `Placeholder.unparsed`
  would render literally instead of parsing.
- The developer API exposes read-only configured quest/collection definitions
  and async per-player progress snapshots (`getQuests`/`findQuest`,
  `getCollections`/`findCollection`), deliberately as minimal as the existing
  skill/class snapshots - no mutation methods, no internal enum types crossing
  the `api`/`core` boundary.
- **Platform fact learned this phase:** `Material.isBlock()` (and anything else
  that resolves through `Material.asBlockType()`) requires a live
  `io.papermc.paper.registry.RegistryAccess`, which does not exist in a plain
  JUnit test outside a running Paper server - it throws
  `IllegalStateException: No RegistryAccess implementation found` wrapped in an
  `ExceptionInInitializerError`. `Material.matchMaterial(...)` alone is safe
  standalone. Config-validation tests that need to assert "is this a block"
  must do it the way `SkillMaterialConfigTest` already did (an allow-list of
  known block-material names), not by calling `Material#isBlock()` from a test;
  calling it from production code inside `enable()` on a live server remains
  fine, since that is exactly what `CachedQuestService`'s YAML parser does.
- `./gradlew build` and `./gradlew test` are green with Java 26.
  `QuestResetRulesTest` covers permanent/daily/weekly expiry arithmetic,
  `QuestSchemaMigrationTest` reserves v5, and `QuestConfigTest`/
  `CollectionConfigTest` validate the bundled YAML catalogues.

## 17. Phase 6 (part 1) — inventory GUI framework

Phase 6's brief bundles five different things (GUIs, visual feedback,
leaderboards, titles, cosmetics). Rather than build all five in one pass, this
part delivers the inventory GUI framework plus a complete GUI over every
system that already existed (Phases 2-5) as one coherent, tested unit.
Leaderboards, titles, cosmetics and standalone visual-feedback polish are
still open - see §14.

- `gui.GuiView` is the one interface every menu implements: `title()`/`size()`
  describe the inventory, `populate(Player, Inventory)` fills it (may resolve
  asynchronously through the existing services, hopping back onto the
  player's own region scheduler before touching the `Inventory` - the same
  rule every command already follows), and `handleClick(...)` reacts to a
  click. `gui.GuiHolder` is a bare `InventoryHolder` wrapper so a single
  global `gui.GuiListener` (`InventoryClickEvent`) can find the owning
  `GuiView` and dispatch to it; every click inside one of our inventories is
  pre-cancelled (these are display/action screens, never item storage), and
  only clicks that landed in the top inventory are forwarded to the view.
  `gui.GuiManager.open(player, view)` is the only way a menu gets shown.
- Views are **stateless across opens**: a `GuiView` instance is constructed
  fresh per `GuiManager.open(...)` call and only ever shown to the player it
  was built for. There is no navigation stack - "back" and post-action
  refreshes are just `GuiManager.open(player, new SomeMenu(services))` again;
  the whole tree is cheap enough to rebuild from the underlying services on
  every open that a stack was not worth the complexity.
- `gui.GuiServices` is a single record bundling every service a menu might
  need (all of Phases 2-5's services plus `MessageService`/`JavaPlugin`),
  because the main hub links to all 8 sub-menus and cross-linking would
  otherwise mean threading a dozen constructor parameters through every menu
  class. `command.MenuCommand` (`/menu`) is the only entry point; it just
  opens `gui.AnaMenu` with the shared `GuiServices` instance built once in
  `HalilIbrahimMMOPlugin#onEnable()`.
- `gui.GuiItems` builds every `ItemStack` from already-rendered
  `MessageService`-produced `Component`s (never a raw literal), turning off
  the default italic lore/name decoration and exposing an optional
  enchantment-glint flag (`ItemMeta#setEnchantmentGlintOverride`, purely
  cosmetic) used to highlight a player's selected class in `ClassMenu`.
- Menus: `AnaMenu` (hub, 8 static icons) → `ProfileMenu`, `SkillsMenu`,
  `AbilitiesMenu` (click activates, mirrors `/kabiliyet`), `ClassMenu` (click
  selects a class or spends a talent point, mirrors `/sinif sec`/`/sinif ac`
  - reopens itself after either action to refresh), `PrestigeMenu` (status +
  confirm button, mirrors `/prestij onayla`), `QuestsMenu`, `CollectionsMenu`
  (click opens `CollectionDetailMenu`, a per-entry ✔/✘ checklist using each
  entry's own configured `Material` as its icon) and `JournalMenu`. None of
  these introduce new backend logic - every click calls the exact same
  service methods (`ClassService#select`, `PrestigeService#prestige`, ...)
  the equivalent text command already calls, so the command surface and the
  GUI can never drift into inconsistent behaviour.
- Every GUI title, item name and lore line is a `MessageKey`/`messages.yml`
  entry, same discipline as chat messages - `gui.*` is a new top-level section
  in `messages.yml`. Multi-value item names (a skill's display name, a
  quest's progress) are inserted as unparsed placeholders into a single
  templated line, the same "plain data fragment into one placeholder"
  pattern `CLASS_LIST`/`CLASS_TREE` already established - never by nesting a
  second MiniMessage-tagged string inside a placeholder value.
- No new database tables or migrations - the GUI layer is presentation-only
  over Phases 2-5's existing persistence.
- Verified via `./gradlew build`/`./gradlew test` (green, no new tests - GUI
  click/render logic needs a live server or mocking neither of which this
  codebase uses yet, consistent with existing interactive commands like
  `ClassCommand`/`PrestigeCommand` also having no dedicated unit tests) and a
  live `./gradlew runServer` boot: all five migrations applied, plugin
  enabled successfully, server reached `Done`.

## 18. Phase 6 (part 2) — leaderboards

- `leaderboards.LeaderboardService` / `CachedLeaderboardService` rank players
  by metrics that already exist on Phases 2-5's tables (`hmmo_player_profiles`
  level/XP/prestige, `hmmo_player_skills` per-skill level/XP, completed rows
  in `hmmo_player_quest_progress`, row counts in
  `hmmo_player_collection_entries`) - **no new migration or table was added**,
  this system is pure read-only aggregation over existing persistence.
- **Deliberately not the write-through per-player cache pattern** every other
  `Cached*Service` uses: those caches only ever hold *currently loaded*
  players, which cannot answer "top N across the whole player base."
  `CachedLeaderboardService` instead keeps a small time-to-live cache (one
  entry per fixed category, one per skill id, refreshed from
  `leaderboards.yml`'s `yenileme-araligi-saniye`) directly over
  `LeaderboardRepository`'s hand-written JDBC (`database.execute(...)`, same
  style as `QuestRepository`). Two callers racing on a stale key may both
  issue one database query each - an accepted tradeoff, not something a
  single-flight guard was added for, since nothing here is a per-player
  mutation that needs ordering.
- `LeaderboardCategory` (the four fixed boards: `GLOBAL_LEVEL`, `PRESTIGE`,
  `QUEST_COMPLETIONS`, `COLLECTION_ENTRIES`) lives **in the `api` module**
  and is imported as-is by `core` - the same shape `api.progression.
  ProgressionCause` already established for an enum that needs to cross the
  api/core boundary, rather than maintaining two parallel enums. Per-skill
  boards take a plain configured skill id string instead, since the skill
  catalogue is YAML-driven (`skills.yml`), not a fixed enum.
  `LeaderboardEntry`/`LeaderboardEntrySnapshot` mirror every other subsystem's
  core-type/api-snapshot split, translated in `HalilIbrahimMMOApiImpl`.
- `LeaderboardRepository`'s two query methods intentionally select exactly
  three columns in every branch (`player_name`, a primary metric, a
  secondary metric or literal `0`) so a single `ResultSet`-reading helper
  covers every board - a level/skill board's secondary value is XP, every
  other board leaves it at `0`. No new `SqlDialect` method was needed: plain
  `ORDER BY ... LIMIT ?` already works unmodified on SQLite/MySQL/MariaDB/
  PostgreSQL.
- `/liderlik [seviye|prestij|gorev|koleksiyon|<yetenek-id>]` and the GUI's
  `LeaderboardMenu` (hub of boards, opened from a new `AnaMenu` icon at slot
  9) → `LeaderboardDetailMenu` (one ranked top-N list, slots 10-25, clamped
  independently of the configured `giris-sayisi` so an admin can't overflow
  past the back button) are the Phase 6 part 2 command/GUI surface. Both
  reuse the exact same `LeaderboardService` calls, so they can never drift.
  As with every other player-facing string in this codebase, category/board
  labels are separate `MessageKey`s per board **shape** (not a single
  generic template with a Java-built label) specifically so no Turkish unit
  word (`Seviye`, `XP`, `Prestij`, `görev tamamlandı`, `öge toplandı`) is
  ever constructed in Java - Java only ever supplies numbers, player names,
  or already-Turkish config data (a skill's own `displayName`) as
  placeholder values.
- The developer API gained `getLeaderboard(LeaderboardCategory)` and
  `getSkillLeaderboard(String skillId)`, both read-only, mirroring the
  minimal-surface pattern every prior phase's API addition followed.
- `./gradlew build`/`./gradlew test` are green with Java 26.
  `LeaderboardConfigTest` validates the bundled `leaderboards.yml` (same
  bundled-resource-validation pattern as `ClassConfigTest`/`QuestConfigTest`).
  No migration-version-pin test was added since no migration exists for this
  phase. No pure-rules test either - rank assignment is a trivial
  `index + 1` inline in the repository, not complex enough to warrant its
  own class the way `PrestigeRules`/`QuestResetRules` do.

## 19. Phase 6 (part 3) — titles, cosmetics, visual feedback (Phase 6 complete)

- New shared `unlock` package (`UnlockConditionType`, `UnlockCondition`,
  `UnlockContext`, `UnlockConditions`, `UnlockConditionParser`,
  `UnlockContextLoader`) backs **both** `titles` and `cosmetics` - they are
  structurally identical systems (YAML catalogue with one unlock condition
  per entry, an INSERT-only unlocked-set membership table, a single-scalar
  equipped-id table, no cooldown) that only differ in what the "equipped"
  state renders as (a chat prefix for titles, a particle burst for
  cosmetics). `UnlockCondition` is one flat record reused across all five
  condition types (`LEVEL`, `PRESTIGE`, `QUEST_COMPLETED`,
  `COLLECTION_COMPLETED`, `SKILL_LEVEL`), mirroring `QuestDefinition`'s
  "both field sets present, only the relevant one populated" convention.
  `UnlockContextLoader` is the one deliberate, documented exception to
  "quest/collection services stay decoupled from profile/progression"
  (§16) - title/cosmetic unlock criteria are inherently a cross-cutting
  read over every other system's state, unlike quest/collection rewards
  which only ever push outward. It depends on `ProfileService`,
  `SkillService`, `QuestService` and `CollectionService` directly.
- **Performance-driven design decision:** unlock evaluation
  (`TitleService#evaluate`/`CosmeticService#evaluate`) rebuilds a full
  `UnlockContext` (level, prestige, every completed quest id, every
  completed collection id, every skill's level) and checks it against
  every not-yet-unlocked catalogue entry. This is deliberately **not**
  triggered on every XP award (block break/mob kill are far too
  high-frequency at 500+ concurrent players) - it only runs on four
  genuinely discrete milestone transitions: an actual level-up (see
  below), an actual prestige success, an actual quest completion, an
  actual whole-collection completion (not per-entry pickup). A title or
  cosmetic whose only condition is `SKILL_LEVEL` therefore unlocks the
  next time the player triggers any of those four events after crossing
  the threshold, not the instant the skill itself levels up - an accepted
  tradeoff, not a bug, and not worth a fifth high-frequency trigger point.
- **Level-up detection did not exist before this phase.**
  `ProgressionService#award`/`awardConfigured` now return
  `progression.ProgressionAwardResult` (`PlayerProfileSnapshot` +
  `leveledUp` boolean) instead of a bare `PlayerProfileSnapshot`.
  `YamlProgressionService#award` captures the pre-update level via a
  local `AtomicInteger` set inside the `profiles.update(...)` mutator
  lambda (which already receives the old `PlayerProfile`), then compares
  it against the updated level. The public
  `HalilIbrahimMMOAPI#awardGlobalExperience(...)` contract was
  deliberately **not** changed (still returns
  `CompletableFuture<PlayerProfileSnapshot>`) -
  `HalilIbrahimMMOApiImpl` adapts with
  `.thenApply(ProgressionAwardResult::snapshot)`.
- Achievement evaluation and visual feedback are wired into the same four
  places, following the codebase's established "listeners compose
  cross-cutting side effects themselves after their own service call
  resolves" pattern (§16): `ProgressionListener` (only when
  `result.leveledUp()`), `QuestListener#notify` (every completed quest),
  `CollectionListener#notify` (only when `award.collectionCompleted()`),
  and **both** `PrestigeCommand#confirm` and `PrestigeMenu#handleClick`
  (confirmed by reading both files that `PrestigeMenu` independently
  duplicates the `prestige.prestige(...)` call, exactly like
  `ClassCommand`/`ClassMenu` already duplicate class selection - a hook
  placed in only one of the two would silently miss GUI-triggered
  prestige). Each of these five call sites duplicates a small
  `notifyTitles`/`notifyCosmetics` forEach-and-message block rather than
  sharing a cross-cutting helper - consistent with this codebase's
  existing tolerance for that kind of small duplication in exchange for
  every listener/command staying self-contained.
- `feedback.FeedbackService`/`BossBarFeedbackService` is genuinely
  greenfield - no Adventure `BossBar`, on-screen `Title`, or Bukkit
  `Particle` API was used anywhere in this codebase before this phase.
  It is universal (not equip-based): every player gets the same
  bossbar/title text for level-up/quest/collection/prestige regardless of
  any equipped cosmetic. A cosmetic only ever adds an *extra* particle
  burst on top, at join and level-up, via
  `CosmeticService#celebrate(Player, CelebrationMoment)` - this keeps
  "visual feedback polish" (universal UX) and "cosmetics" (player choice)
  from overlapping despite both touching the same moments.
- `titles.TitleChatListener` is the first chat-rendering code in this
  project (`AsyncChatEvent#renderer(...)`). It reads a synchronous,
  join-warmed `ConcurrentHashMap<UUID, String>` cache of the equipped
  title's *display name* (`TitleService#equippedDisplayName`) rather than
  doing an async DB round-trip per chat message; the prefix Component
  itself is rendered once via `MessageKey.TITLE_CHAT_PREFIX` and the
  player's own already-a-`Component` message is appended directly
  afterward (not re-localized, not passed through
  `Placeholder.unparsed`, since it is the player's own content, not new
  plugin-authored text). No title equipped → the event is left untouched
  and Paper's default renderer applies.
- Titles/cosmetics list commands (`/unvan`, `/kozmetik`) render two
  separate joined-fragment placeholders (`acik`/`kilitli`) rather than a
  single list with an inline locked/unlocked marker word, specifically so
  no Turkish status word is ever built in Java - the same discipline
  every other phase's `MessageKey` usage follows.
- No `ALTER TABLE` was used anywhere - `TitleCosmeticSchemaMigration` v6
  adds four brand-new tables (mirroring `QuestSchemaMigration` v5's
  "several related tables in one migration" shape) rather than adding
  columns to the Phase 2 `hmmo_player_profiles` table the way
  `selected_class` already does for classes; this keeps the new feature
  fully self-contained in its own migration and avoids introducing this
  codebase's first schema-altering migration.
- `./gradlew build`/`./gradlew test` are green with Java 26.
  `UnlockConditionsTest` is a pure-logic test (all 5 condition types,
  `PrestigeRulesTest`-style) since `UnlockConditions.isSatisfied` has real
  branching logic worth covering in isolation. `TitleConfigTest`/
  `CosmeticConfigTest` validate the bundled catalogues structurally *and*
  cross-reference `QUEST_COMPLETED`/`COLLECTION_COMPLETED` conditions
  against the actual quest/collection ids in `quests.yml`/`collections.yml`
  (the same cross-file validation `ClassConfigTest` already does against
  `abilities.yml`). `CosmeticConfigTest` validates `efekt` particle names
  against a known allow-list rather than calling `Particle.valueOf(...)`
  from the test, matching `QuestConfigTest`'s established workaround for
  APIs that need a live server `RegistryAccess` outside a running Paper
  server. `FeedbackConfigTest` validates the bundled `feedback.yml`.
  `TitleCosmeticSchemaMigrationTest` pins `version() == 6`.

## 20. Phase 7 (part 1) — PlaceholderAPI and LuckPerms hooks

- **PlaceholderAPI and LuckPerms are `compileOnly` Gradle dependencies,
  never routed through `HalilIbrahimMMOLoader`/`MavenLibraryResolver`.**
  That mechanism exists specifically for third-party *libraries* no other
  plugin provides (HikariCP, JDBC drivers); PlaceholderAPI/LuckPerms are
  themselves plugins whose classes are already on the server's classpath
  if and only if those plugins are installed - structurally identical to
  how `paper-api` itself is handled. Their coordinates live in
  `gradle.properties` (`placeholderApiVersion=2.12.3`,
  `luckPermsApiVersion=5.5` - verified live against
  `https://repo.helpch.at/releases/` and Maven Central respectively, not
  guessed) like every other dependency version in this project.
  PlaceholderAPI needed a new repo (`https://repo.helpch.at/releases/` -
  the modern replacement for the old `repo.extendedclip.com` URL, which
  now 301-redirects there) added to the root `build.gradle.kts`; LuckPerms'
  API publishes straight to Maven Central, no new repo needed.
- **Both are declared as genuinely optional dependencies** in
  `paper-plugin.yml`'s new `dependencies.server` block
  (`required: false, join-classpath: true, load: BEFORE`) and guarded in
  `HalilIbrahimMMOPlugin#onEnable()` with
  `getPluginManager().getPlugin("X") != null`.
  **A real, empirically-confirmed pitfall discovered while verifying
  this:** an `if`-guard around `new SomeIntegrationType(...)` is *not*
  enough by itself if `HalilIbrahimMMOPlugin` itself declares a field or
  local variable of that integration type. Paper instantiates the main
  plugin class via `Class.forName(...)`, which triggers full bytecode
  verification of every method in `HalilIbrahimMMOPlugin` - and
  StackMapTable-based verification resolves *every* type any method
  mentions (field declarations included), regardless of whether the
  branch that uses it is ever reached at runtime. The very first
  `runServer` boot with neither plugin installed threw
  `NoClassDefFoundError: net/luckperms/api/context/ContextCalculator`
  and **the entire plugin failed to load** (not just the LuckPerms
  integration) purely because `HalilIbrahimMMOContextCalculator
  implements ContextCalculator<Player>` was a field type on
  `HalilIbrahimMMOPlugin`, `join-classpath`/`required: false`
  notwithstanding. **The fix, and the pattern to follow for any future
  optional integration:** isolate every reference to an optional
  dependency's types into its own small class (`placeholder.PlaceholderApiHook`,
  `luckperms.LuckPermsHook`, both a single static `register(...) ->
  Runnable` method) that `HalilIbrahimMMOPlugin` only ever calls from
  *inside* the already-passed guard, and stores the teardown as a plain
  `Runnable` field - never the concrete integration type. A class is only
  linked/verified on first active use, so `PlaceholderApiHook`/
  `LuckPermsHook` are never loaded at all when their target plugin is
  absent, and `HalilIbrahimMMOPlugin`'s own bytecode never mentions
  `me.clip.placeholderapi.*`/`net.luckperms.api.*` types anywhere. Both
  `Runnable` fields are explicitly invoked-then-nulled in `onDisable()`
  before the reverse-order service shutdown, mirroring
  `progressionListener.close()`'s existing explicit-teardown discipline.
  This was caught and fixed in the same session by actually booting the
  server without either plugin installed rather than assuming the guard
  was sufficient - a reminder that "should work" reasoning about
  classloading/verification order is worth empirically checking, not
  trusting.
- **New synchronous read path, additive to the async-only public API.**
  Every per-player method on `HalilIbrahimMMOAPI` was `CompletableFuture`-
  returning before this phase, but PlaceholderAPI's
  `onPlaceholderRequest(Player, String)` and LuckPerms'
  `ContextCalculator#calculate` are both called synchronously and cannot
  await a future. `TitleService#equippedDisplayName(UUID)` (added in
  Phase 6 part 3 for chat rendering, the same problem) was the only
  existing precedent - every other subsystem already had the identical
  `ConcurrentHashMap<UUID, X>` join-warmed cache shape, it just never had
  a *public* synchronous getter. `HalilIbrahimMMOAPI` gained
  `findCachedPlayer(UUID)` (returns the new `api.CachedPlayerSnapshot`
  record: level, XP, XP-to-next-level, prestige, raw `selectedClassId`,
  resolved `equippedTitle`/`equippedCosmetic` display names) and
  `findCachedSkill(UUID, String)`, both `Optional`-returning and
  documented as in-memory-only/possibly-stale/empty-until-loaded. This
  required exactly three small core changes: `ProfileService#cached(UUID)`
  and `SkillService#cached(UUID, String)` (both trivial
  `Optional.ofNullable(cache.get(...))` - the cache already existed,
  private), and `CosmeticService` gained the display-name half
  `TitleService` already had (`CachedCosmeticService` only cached the
  equipped *id* before this phase, not the display name - copied
  `TitleService`'s `equippedIdCache`+`equippedDisplayNameCache`+
  `cacheEquipped(...)` pattern verbatim). **`ClassService` needed no
  change at all** - the selected class id already lives directly on the
  `PlayerProfile` record already cached by `ProfileService`, so
  `findCachedPlayer` reads `profile.selectedClass()` straight off the
  profile it already fetched.
- **`CachedPlayerSnapshot` carries a raw id for class but resolved
  display names for title/cosmetic - a deliberate, noted asymmetry, not
  an inconsistency.** `selectedClassId` follows the established
  `QuestSnapshot`/`CollectionSnapshot`/`SkillSnapshot` "ids only, resolve
  display names via the catalogue getter" convention, because the
  LuckPerms context calculator needs a stable id admins can write
  permission rules against, not translated text. `equippedTitle`/
  `equippedCosmetic` skip that convention and carry pre-resolved display
  names instead, because they only ever feed PlaceholderAPI text (no
  permission-matching use case) and `TitleService`/`CosmeticService`
  already only expose the *display name* synchronously (`equippedDisplayName`),
  not the raw id.
- **LuckPerms hook is a read-only `ContextCalculator<Player>`
  (`luckperms.HalilIbrahimMMOContextCalculator`), not automatic group/
  permission granting** - a deliberate choice the user made explicitly
  when asked. It feeds `halilibrahimmmo:level`, `halilibrahimmmo:prestige`
  and (only when non-empty) `halilibrahimmmo:class` into LuckPerms'
  `ContextConsumer`, letting server admins write their own permission
  rules conditioned on progression. The plugin never writes to LuckPerms
  in any way.
- **PlaceholderAPI expansion** (`placeholder.HalilIbrahimMMOExpansion`,
  identifier `halilibrahimmmo`) exposes `%halilibrahimmmo_level%`, `_xp`,
  `_xp_to_next_level`, `_prestige`, `_class`, `_title`, `_cosmetic`, and
  parameterized `_skill_<id>_level`/`_skill_<id>_xp`. The skill placeholder
  parser matches the `_level`/`_xp` **suffix** via `endsWith(...)` rather
  than splitting on the last underscore, because configured skill ids may
  themselves legally contain underscores (`[a-z0-9_-]+` is the id pattern
  used everywhere else in this codebase) - splitting on the last
  underscore would silently misparse a skill id like `yakin_dovus`.
  Uses the online-only `onPlaceholderRequest(Player, String)` overload,
  not the offline-capable `onRequest(OfflinePlayer, ...)` one, since every
  cache this plugin maintains is itself online-only (evicted on quit) -
  there is no offline data to serve either way.
- **No `MessageKey`/`messages.yml` changes anywhere in this phase.**
  Placeholder identifiers and LuckPerms context keys are technical/
  code-facing strings (consistent with command literals staying ASCII,
  §5.6), and every *value* either resolves to is a number or already-
  Turkish config data (a class/title/cosmetic's own `displayName`), never
  a Java-authored literal.
- **No new tests**, matching the precedent Phase 6 part 1 already set for
  GUI code: a `PlaceholderExpansion`/`ContextCalculator` fundamentally
  requires the *other* plugin's classes loaded to exercise at all, and
  the one piece of real branching logic (the skill-suffix parser) is two
  `endsWith` checks, not complex enough to extract into its own
  pure-tested class the way `UnlockConditions`/`PrestigeRules` warranted.
- `./gradlew build`/`./gradlew test` are green with Java 26.
  `./gradlew runServer` was verified booting cleanly to `Done` with
  **neither** PlaceholderAPI nor LuckPerms installed (proving the
  optional-dependency guard works, zero `ClassNotFoundException`/
  `NoClassDefFoundError`, zero attempted registration in the log) -
  actually installing either plugin to verify a placeholder/context
  resolves live was not done in this session; a future session should do
  that before considering this phase fully field-verified.

## 21. Phase 7 (part 2) — exploit hardening and cross-engine integration tests (Phase 7 complete)

Two parallel research passes (a full-codebase exploit audit, and a
database/dialect/test-infrastructure audit) drove this phase. Full findings
detail lives in the session history; the durable decisions are below.

### Exploit hardening

- **Fixed: `InventoryDragEvent` was never registered or handled anywhere**
  in the codebase - `GuiListener` only cancelled `InventoryClickEvent`. A
  player could drag a real item from their own inventory into a plugin
  GUI's top inventory, breaking the "these menus are never storage"
  invariant §17 already documents. `GuiListener` now also cancels any drag
  whose top inventory holder is a `GuiHolder`, mirroring the exact same
  unconditional-cancel-before-any-other-check shape the click handler
  already used. No new listener registration needed - same class, second
  `@EventHandler` method.
- **Fixed: no plugin-side sanity check on `BlockBreakEvent` frequency** -
  `ProgressionListener`/`SkillListener`/`QuestListener` each awarded on
  every event with no throttling, letting a nuker-style client farm
  global/skill/quest XP faster than legitimate play allows. New
  `exploit.BlockBreakRateLimiter` - a single, dependency-free,
  `ConcurrentHashMap<UUID, ArrayDeque<Long>>`-backed sliding-window counter
  (20 breaks/second ceiling, hardcoded not config-driven - a sanity limit,
  not a per-server balance knob) - is constructed **once** in
  `HalilIbrahimMMOPlugin#onEnable()` and shared across all three listeners
  (each gained a constructor parameter) rather than tripling identical
  throttling logic. Each listener's `onBlockBreak` calls
  `limiter.allow(playerId)` right after the existing
  `abilities.isSecondaryBreak(event)` exclusion and skips only its reward
  path if it returns `false` - **the block itself always still breaks**,
  only progression is ever declined. Mob-kill listeners are deliberately
  **not** throttled: `EntityDeathEvent#getKiller()` is server-authoritative,
  not client-spoofable, so there's no equivalent gap there.
- **Documented, not code-changed:** ability activation
  (`YamlAbilityService#activateOnPlayerRegion`) and `energy.EnergyState`'s
  check-then-act cooldown/energy logic are safe today only because every
  call site runs on the player's own Paper region/entity scheduler (which
  serializes per-entity) - unlike every persistent cached service, which
  uses an explicit per-key chain. Both classes now carry a Javadoc/comment
  explaining this so a future change calling either off the player's own
  scheduler doesn't silently reintroduce a double-activation/double-spend
  race. No lock was retrofitted for a currently-non-exploitable path -
  that would be exactly the unnecessary defensive complexity §9 warns
  against.
- **Deliberately left alone** (found safe or explicitly out of scope, not
  overlooked): every cached service's mutation paths already route through
  their serial chain correctly; `ProfileService#incrementStatistic` is
  safe without chaining (disjoint table, atomic SQL increment); class-
  change cooldown is fully chain-protected; command permissions match the
  documented §5.6 design with no gap; zero SQL-injection surface exists
  anywhere (100% `PreparedStatement` parameterization, verified by an
  exhaustive grep); collection item-pickup provenance (no check on *how*
  a picked-up item was obtained) is a policy question, not an exploit -
  one-time-per-entry rewards mean it can't be repeat-farmed;
  `incrementStatistic`'s upsert has a narrow duplicate-key race on
  brand-new rows that can only ever *undercount*, never something a
  player benefits from - a resilience gap, not an exploit, left for a
  future pass if it ever matters.

### Cross-engine repository integration tests

- **Confirmed before this phase: no test in this repository had ever
  executed a `Migration#migrate(Connection, SqlDialect)` against a live
  `Connection`, and MySQL/MariaDB/PostgreSQL had never been connected to
  at all** - only reasoned about structurally via the dialect abstraction,
  plus driver-jar resolution during `runServer` (which only proves the
  jars load, not that any SQL runs correctly). This means
  `dialect.autoIncrementPrimaryKeyType()` (used once, in
  `QuestSchemaMigration`'s journal table) and the UPDATE-then-INSERT
  upsert idiom (used throughout `ProfileRepository`, `ClassRepository`,
  `TitleRepository`, `CosmeticRepository`) had never actually been proven
  against real MySQL/MariaDB/PostgreSQL syntax before now.
- **New Gradle `integrationTest` source set/task**
  (`core/build.gradle.kts`), deliberately **excluded from
  `test`/`check`/`build`** since it requires a running Docker daemon -
  `./gradlew build`/`test` remain exactly as fast and Docker-free as
  before this phase. `gradle.properties` gained `testcontainersVersion=2.0.5`
  (verified live against Maven Central, not guessed).
  **Load-bearing discovery, worth remembering for any future Testcontainers
  work:** Testcontainers 2.x renamed every per-engine module's package
  *and* artifact id - `org.testcontainers:mysql`/`mariadb`/`postgresql`
  (1.x) became `org.testcontainers:testcontainers-mysql`/`-mariadb`/
  `-postgresql` (2.x), and the container classes themselves moved from
  `org.testcontainers.containers.MySQLContainer` (now deprecated) to
  `org.testcontainers.mysql.MySQLContainer` (new home) - and the 2.x
  classes **dropped their self-referential generic parameter entirely**
  (`MySQLContainer`, not `MySQLContainer<?>`/`MySQLContainer<SelfType>`).
  A first attempt using the old 1.x-shaped coordinates/imports/generics
  failed at dependency resolution and then at compilation in exactly that
  order - caught and fixed by actually compiling, not assumed away.
- **`database.IntegrationDatabaseSupport`** (new, `integrationTest`
  source set, public - every type it touches already is) is the one
  shared helper every concrete test class reuses:
  `settingsFor(DatabaseType, JdbcDatabaseContainer)` builds a
  `DatabaseSettings` from any of the three Testcontainers JDBC containers
  (they share `JdbcDatabaseContainer`'s host/port/database/username/
  password shape even post-2.x-restructuring), and
  `provisionAndMigrate(...)` builds a real `HikariDatabaseService` and
  runs every shipped migration through the same `MigrationRunner`
  `HalilIbrahimMMOPlugin#onEnable()` uses.
- **Test coverage, deliberately representative rather than exhaustive:**
  `SqliteMigrationContractTest` (fast `test` suite, no Docker, the first
  test to run a migration against even a real file-based connection - a
  control check independent of Docker availability) plus
  `{MySql,MariaDb,PostgreSql}MigrationIntegrationTest` (all 6 migrations
  apply cleanly on every real engine) and two representative repository
  families - `ClassRepositoryCrossEngineTest` (the UPDATE-then-INSERT
  upsert idiom, the most-repeated pattern in the whole repository layer)
  and `TitleRepositoryCrossEngineTest` (the INSERT-only membership
  pattern shared by `CollectionRepository`/`CosmeticRepository`, plus its
  own equip upsert) - each an abstract base (shared assertions + a
  `DockerClientFactory.instance().isDockerAvailable()` `Assumptions.assumeTrue`
  skip-gate) with one concrete `@Testcontainers` subclass per engine, all
  living in the same package as the repository under test for
  package-private access (`ClassRepository`/`TitleRepository`'s methods
  are package-private, same as every other repository in this codebase).
  Not an exhaustive per-repository suite - proportionate to what's
  actually at risk of being engine-specific (the two SQL idioms every
  other repository already reuses), not blanket coverage for its own sake.
- **Verified in this session:** `./gradlew build`/`./gradlew test` green
  (now including `SqliteMigrationContractTest`) and
  `./gradlew :core:compileIntegrationTestJava` succeeds cleanly with zero
  warnings. **Not verified in this session** (Docker daemon was not
  running): actually executing `./gradlew integrationTest` against real
  containers - the tests are compiled and structurally ready, and should
  be run with Docker started before trusting cross-engine correctness as
  proven rather than merely well-designed.
