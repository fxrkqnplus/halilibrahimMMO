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
| `hmmo_player_class_progress` | Phase 9.2, `ClassProgressSchemaMigration` v7 | Per (player, class) level/XP - the third leveling track, independent of talent ranks. |
| `hmmo_player_activation` | Phase 9.6, `PlayerActivationSchemaMigration` v8 | INSERT-only membership: a row means this player has run `/mmo`. |
| `hmmo_player_quest_assignments` | Phase 11.7, `QuestAssignmentSchemaMigration` v9 | One row per player/quest type: the comma-joined ids randomly assigned this period, and the period end. |
| `hmmo_player_onboarding_tasks` | Phase 11.8, `OnboardingSchemaMigration` v10 | INSERT-only membership per completed onboarding task. |
| `hmmo_player_banked_power` | Phase 12.4, `PrestigeBankedPowerSchemaMigration` v11 | The Güç banked at each prestige, so prestige never lowers Güç. |
| `hmmo_player_home` | Phase 16.6, `PlayerHomeSchemaMigration` v12 | One named home per player, stored as world UUID plus raw coordinates. |
| `hmmo_player_wealth` | Phase 17.11, `WealthSchemaMigration` v13 | One lifetime-cumulative Zenginlik Puanı total per player. Never decremented. |
| `hmmo_player_quest_pins` | Phase 17.12, `QuestPinSchemaMigration` v14 | One row per player/quest type: the comma-joined ids that player pinned to their sidebar (at most three). |

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
| `feedback.yml` | How long the level-up/quest/collection/prestige bossbar and on-screen title stay visible, plus every named sound cue. |
| `zenginlik.yml` | Phase 17: Zenginlik Puanı values per block, entity and quest, plus the milestone thresholds. |
| `bosslar.yml` | Phase 17: world boss spawn interval and distance ring, the five rarity tiers (multipliers, gear, behaviour flags, rewards, loot) and one archetype per creature kind with its own name pool. |

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
  done. All originally-planned phases (per the project's original brief)
  are now finished.
- **Phase 8 is complete** (see §22): a user-requested follow-on phase, not
  part of the original brief - a persistent sidebar HUD, per-source XP
  BossBar feedback with level-up split out to a personal chat message,
  menu-based ability selection with right-click activation and glint/action-bar
  feedback, and OP-only `/rpg oyuncu` admin tooling. `./gradlew build`/`test`
  are green; a live `./gradlew runServer` walkthrough of Phase 8's features
  specifically has not yet been performed in any session and should be done
  before considering it fully field-verified (same caveat §20 already
  recorded for Phase 7's PlaceholderAPI/LuckPerms live resolution).

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

## 22. Phase 8 — HUD sidebar, XP feedback, ability selection and admin tooling (Phase 8 complete)

A user-requested follow-on phase, built one parent task at a time with an
explicit stop-and-report after each: (1) a persistent profile sidebar, (2) a
per-source XP BossBar plus a personal-only level-up chat message, (3)
menu-based ability selection with right-click activation, glint and an
action-bar countdown, (4) OP-only `/rpg oyuncu` admin commands.

### 8.1 Sidebar (scoreboard HUD) and `/panel`

- New `scoreboard` package: `SidebarService` (`PluginService`) /
  `ScoreboardSidebarService` renders a per-player Bukkit `Scoreboard` on
  `DisplaySlot.SIDEBAR` - player name, selected class (or "not selected"),
  level, XP/next-level-XP and prestige, one fixed-order line each, using
  the `Team`-prefix-per-invisible-legacy-color-code-entry trick to render
  arbitrary text lines (the standard workaround for scoreboard lines not
  being freely settable strings).
- **The on/off preference is deliberately never persisted** - a plain
  in-memory `ConcurrentHashMap<UUID, Boolean>` (default `true`), evicted on
  quit - per an explicit user requirement that the sidebar always defaults
  to visible on every join/server restart, never remembering a prior
  session's choice. `/panel` (new `PanelCommand`) just flips this map.
- Every XP/level/class/prestige change that already had a hook now also
  calls `sidebar.refresh(player)` on the player's own region scheduler,
  including a **self-found gap fix**: `QuestListener`/`CollectionListener`
  call `ProgressionService.award(...)` directly rather than through
  `ProgressionListener`, so they were bypassing the sidebar-refresh hook
  until both gained an explicit `sidebar.refresh(...)` call alongside their
  existing BossBar trigger (see §8.2).

### 8.2 Per-source XP BossBar and personal level-up chat

- `FeedbackService`/`BossBarFeedbackService` gained
  `showExperienceProgress(...)`: one BossBar instance is reused per player
  (`ConcurrentHashMap<UUID, BossBar>`), never stacked, with its own
  auto-hide timer that resets on every subsequent XP gain while the bar is
  still visible (`feedback.yml`'s new `xp-cubugu-sure-saniye`).
- **`celebrateLevelUp` was rewritten to be chat-only** - the previous
  BossBar/Title level-up celebration (`FEEDBACK_LEVEL_UP_BOSSBAR/TITLE/
  SUBTITLE`) was removed outright and replaced with a single personal
  `MessageKey.FEEDBACK_LEVEL_UP_CHAT` message, per an explicit user
  requirement that level-up feedback be visible only to the leveling
  player, never a broadcast or an on-screen title. Quest/collection/
  prestige BossBar+Title feedback is unchanged and out of scope (§16/§19's
  behaviour there was never touched).
- **One fully-authored `MessageKey` template per XP cause**
  (`FEEDBACK_XP_BLOCK_BREAK_BOSSBAR`, `_MOB_KILL_`, `_PLAYTIME_`, `_QUEST_`,
  `_COLLECTION_`, `_SKILL_`), not a single generic template fed a
  Java-resolved source label - the same "no Turkish word ever built in
  Java" discipline §18/§19 already established for leaderboard/title
  labels. `ProgressionCause.EXTERNAL` (reserved since Phase 2, first
  actually used in §8.4's admin `xp ver`) intentionally maps to no BossBar
  template at all - admin-granted XP does not produce source-attribution
  BossBar noise the way organic gameplay XP does.
- `SkillExperienceCurve` gained `requiredForCurrentLevel(int)` (mirroring
  `ExperienceCurve`'s existing method), needed to compute the skill XP
  bar's "current/required" fraction.

### 8.3 Ability selection, right-click activation, glint and countdown

- **Selection and activation are now two separate steps**, replacing
  click-to-activate in `AbilitiesMenu`. `AbilityService` gained
  `select(Player, String)`/`selected(UUID)` (in-memory, not persisted -
  consistent with the sidebar preference and with abilities never having
  been persisted before this phase either); `AbilitiesMenu#handleClick`
  now selects and re-glints the chosen icon instead of calling the
  existing `activate(...)` path directly.
- New `AbilityToolTag` enum (`PICKAXE`/`AXE`/`SHOVEL`/`SWORD`/`HOE`, each
  wrapping a Bukkit `Tag<Material>` constant like `Tag.ITEMS_PICKAXES`)
  plus a new `arac.tur` key per ability in `abilities.yml` - this avoids
  hand-maintaining a material list per ability the way an ad-hoc
  `Set<Material>` would have required. New `AbilityActivationListener`
  (`PlayerInteractEvent`, main-hand-only, right-click block/air only)
  activates the selected ability when the held item matches its
  `AbilityToolTag`, calling the exact same `AbilityService#activate(...)`
  `/kabiliyet <id>` already calls - **`/kabiliyet <id>` itself was left
  completely unchanged** and remains a valid direct-activation path
  alongside menu-select-then-right-click, not replaced by it.
- Cosmetic-only glint (`ItemMeta#setEnchantmentGlintOverride(Boolean)`,
  `null` clears the override rather than forcing it off) is applied to the
  held tool for the ability's duration and removed on expiry or early
  consumption; `ActiveAbilityStatus` (id + expiry epoch millis) backs a
  once-a-second `player.getScheduler().runAtFixedRate(...)` action-bar
  countdown (`ABILITY_COUNTDOWN_ACTIONBAR`) that also covers one-shot
  abilities' pre-consumption waiting window, not just duration-based
  buffs - both share the same underlying `armed`/`activeMillis` state, so
  no separate code path was needed for the one-shot case, per an explicit
  user clarification during this phase's PRD review.
- `PlayerEnergyService` gained a `Predicate<Player> actionBarSuppressed`
  constructor parameter (a lambda checking `AbilityService#activeStatus`,
  not a direct `AbilityService` field) so the ability countdown wins over
  the pre-existing energy action bar without a circular service
  dependency between `EnergyService` and `AbilityService`.
- **Self-found and fixed, out of this phase's original scope:**
  `AbilityService#handleEntityDamage` (the `DAMAGE_BUFF` effect behind
  "Avcı Odağı") had zero call sites anywhere in the codebase since Phase
  3 - confirmed via an exhaustive grep - meaning that ability's damage
  multiplier had never actually applied. `ClassListener` already listens
  to the same `EntityDamageByEntityEvent` for the class PvE-damage
  modifier, so one line there now also calls
  `abilities.handleEntityDamage(event)`.

### 8.4 `/rpg oyuncu` admin commands

- New `command.subcommand.PlayerAdminSubCommand` (`SubCommand`,
  `name() = "oyuncu"`) adds six actions under `/rpg oyuncu <isim> <eylem>`:
  `bilgi`, `seviye <değer>`, `xp ver <miktar>`, `sinif <id|temizle>`,
  `yetenek <id> seviye <değer>`, `istatistik <anahtar> <değer>`. Every
  target must be online - admin actions go through the same cached,
  write-through services gameplay uses, never touch offline rows directly.
- **Permission architecture is the one genuinely new pattern this phase
  introduced.** §5.6 established a single `SubCommand#permission()` node
  checked by `RpgCommand` *before* dispatching to the subcommand at all -
  fine for one node per subcommand, but this subcommand needs six
  independently LuckPerms-delegatable nodes
  (`halilibrahimmmo.admin.oyuncu.bilgi`, `.seviye`, `.xp`, `.sinif`,
  `.yetenek`, `.istatistik`, all `default: op`). Bukkit permission nodes
  do not imply their parent, so gating the whole subcommand behind one
  umbrella node would have made it impossible to hand a staff member only
  `...oyuncu.bilgi` without also granting every other action. The fix:
  `PlayerAdminSubCommand.permission()` returns a new, deliberately
  permissive gate node (`halilibrahimmmo.admin.oyuncu`, `default: true`)
  that only lets `RpgCommand` dispatch into the subcommand at all; the six
  real per-action checks happen manually inside `execute()`
  (`sender.hasPermission("halilibrahimmmo.admin.oyuncu." + action)`,
  falling back to the existing `MessageKey.NO_PERMISSION`). **`RpgCommand`
  itself also lost its `permission()` override entirely** (previously
  hardcoded to `halilibrahimmmo.admin`), for the identical reason one
  level up: a staff member granted only a granular `oyuncu.*` node must
  still be able to reach `/rpg` at all for that check to ever run.
  `yenile`/`versiyon`/`hataayiklama` are unaffected - they already enforce
  their own `halilibrahimmmo.admin` node internally via the same
  `RpgCommand`-owned per-subcommand check this pattern relies on.
- **New admin mutation methods, added directly to the existing service
  interfaces (never exposed via the `api` module - deliberately kept
  internal, matching this codebase's existing minimal-public-surface
  discipline every prior phase's API addition followed):**
  `ProgressionService#setLevel` (clamps into `[1, curve().maximumLevel()]`,
  resets XP to 0, does **not** run title/cosmetic evaluation or the
  level-up celebration - a correction tool, not a gameplay XP path, unlike
  `award`); `ClassService#forceSetClass` (bypasses `select()`'s
  change-cooldown entirely via the same per-player `serial(...)` chain
  `select`/`spend` already use, preserves spent talent ranks, an empty
  string clears the class); `SkillService#setLevel` (same clamp-and-reset
  shape as `ProgressionService#setLevel`, reuses `CachedSkillService`'s
  existing per-skill `updateChains` chain-safety); `ProfileService#
  setStatistic` (a new `ProfileRepository` method - direct UPDATE-then-
  INSERT assignment, deliberately separate from the existing *additive*
  `incrementStatistic`, not a parameter flag on it).
- `xp ver` is the one action that reuses the **existing** gameplay path
  (`ProgressionService.award(..., ProgressionCause.EXTERNAL)`) rather than
  a bespoke mutation, specifically so admin-granted XP still runs
  achievement/title/cosmetic evaluation and the level-up chat message
  exactly like organic XP does - `PlayerAdminSubCommand` duplicates the
  same `celebrateLevelUp`/`cosmetics.celebrate`/`titles.evaluate`/
  `cosmetics.evaluate` call shape `ProgressionListener#award` already
  established, the same "each site duplicates a small notify block rather
  than sharing a helper" tolerance §19 already documented.
  `ProgressionCause.EXTERNAL` (reserved since Phase 2) is used for the
  first time here.
- Every successful action sends the admin a Turkish confirmation and logs
  a Turkish line via `PluginLogger` (which admin, which target, what
  changed); `seviye`/`xp ver`/`sinif` also call `sidebar.refresh(target)`
  on the target's own region scheduler so the change is visible
  immediately - `yetenek`/`istatistik` deliberately do not, since neither
  value is shown on the sidebar.
- New `paper-plugin.yml` permission entries: the `oyuncu` gate node plus
  six per-action nodes (see above). New `rpg.oyuncu.*` section in
  `messages.yml`/`MessageKey.java` (12 keys: usage, offline, invalid
  number, `bilgi`/`bilgi-sinifsiz`, and one confirmation message per
  action).
- `./gradlew build`/`./gradlew test` are green with Java 26 (verified by
  the user locally, this session's sandbox has no JDK 26/internet access
  to run Gradle itself). No new pure-logic unit test was added this
  phase - the admin mutations reuse existing, already-testable records
  (`PlayerProfile`, `SkillProgress`) rather than introducing new branching
  logic complex enough to warrant one the way `PrestigeRules`/
  `UnlockConditions` did; a `PlayerAdminSubCommand`-adjacent test would
  need a live server anyway (same reasoning §17/§20 already gave for not
  unit-testing GUI/PlaceholderAPI code).

## 23. Phase 9 — sidebar redesign, GUI-first commands, per-skill admin XP, onboarding gate, class rework (Phase 9 complete)

A large, user-requested follow-on phase, built in seven stop-and-report
sub-phases (9.1–9.7): (1) quick wins (Güç rename, admin skill-XP grant,
GUI-first `/sinif`/`/kabiliyet`), (2) class renaming/synergy-skill
mapping/class-level foundation, (3) class-exclusive abilities, (4) synergy
XP bonus and class-level wiring, (5) sound effects, (6) the `/mmo aktif`
onboarding gate, (7) the full sidebar redesign.

### 9.1 — Güç rename, admin per-skill XP grant, GUI-first commands

- The global level's Turkish label changed from "Seviye" to "Güç" everywhere
  it referred specifically to the *global* level (sidebar, `/profil`, admin
  info, leaderboard) - **text-only** `messages.yml` changes, no `MessageKey`
  additions. The one leaderboard entry lore shared between the global-level
  and per-skill boards (`GUI_LEADERBOARD_ENTRY_LEVEL_LORE`,
  `gui.liderlik.oge-seviye`) was deliberately left saying "Seviye" since
  renaming it would have incorrectly relabeled skill leaderboard entries too
  - confirmed by reading `LeaderboardDetailMenu#levelLore` before editing.
- `SkillService#addExperience(UUID, String, long)` (new, additive, reuses
  the normal `SkillExperienceCurve.award(...)` and can cross multiple
  levels) closes the one genuine gap that existed: admins could already
  **set** a skill's level (`SkillService#setLevel`, resets XP) but could not
  **add** XP to one. `/rpg oyuncu <isim> yetenek <id> xp <miktar>` is a new
  sibling verb under the existing `yetenek` action/permission node
  (`halilibrahimmmo.admin.oyuncu.yetenek` already covered both).
- `/sinif` and `/kabiliyet` now open their GUI directly instead of parsing
  chat arguments - `ClassCommand`/`AbilityCommand` were rewritten down to
  `GuiManager.open(player, new ClassMenu(...)/new AbilitiesMenu(...))`.
  **The old chat-argument forms were removed entirely** (`sec`/`agac`/`ac`,
  bare-listing), per an explicit user decision during plan review - both
  `ClassMenu`/`AbilitiesMenu` already covered the exact same actions.
  `/kabiliyet <id>` (direct activation) is unaffected - only the bare/no-arg
  form's behavior changed, from printing a list to opening the menu.
  `ABILITY_LIST`/`CLASS_LIST`/`CLASS_TREE`/`CLASS_USAGE` `MessageKey`s were
  deleted as now-genuinely-dead.

### 9.2 — Class renaming, synergy-skill mapping, class-level foundation

- All four classes got new `gorunen-ad` values (ids/talent ids/code
  untouched): `avci` → **Amansız Savaşçı**, `madenci` → **Mağara Adamı**
  (both user-specified), `savasci` → **Define Avcısı**, `oduncu` →
  **Orman Bekçisi** (both proposed and user-confirmed). Each class gained a
  `sinerji-beceri` field pointing at exactly one of the four skills
  (`avci`↔avcilik, `madenci`↔madencilik, `oduncu`↔odunculuk,
  `savasci`↔kazicilik) - a mapping forced once the first three are fixed by
  the user's own examples, and confirmed to already match every existing
  ability's configured skill with zero reassignment needed.
- **Third leveling track, deliberately flavor/display-only**: a new
  per-(player, class) level/XP pair, tracked independently of talent ranks
  (which already survive class switches) via a new `classLevels` map on
  `PlayerClassProgress` and a new table (`hmmo_player_class_progress`, V7
  `ClassProgressSchemaMigration`). New `ClassExperienceCurve` mirrors
  `SkillExperienceCurve`'s shape verbatim (a fourth near-duplicate curve
  class in this codebase now, alongside `ExperienceCurve`/
  `SkillExperienceCurve` - kept as three separate classes rather than one
  shared generic, consistent with this codebase's existing tolerance for
  that duplication). **Class level never gates anything mechanically** -
  ability unlock stays exactly as it always was (skill-level-gated); this
  track exists purely so a player can see "how much have I invested in this
  class" separately from "how good am I at the underlying skill."
- `YamlClassService` gained a `SkillService` dependency (to validate
  `sinerji-beceri` against real skill ids at parse time) - no circularity,
  since `SkillService` has zero dependency back on `classes`.
- `ClassService#awardClassExperience`/`#classProgress` are new, internal
  (not exposed via `api`), following the exact same "admin/internal mutation
  added directly to the service interface" precedent §22 already set.

### 9.3 — Class-exclusive abilities

- `AbilityDefinition` gained a `classId` field - an ability now belongs to
  exactly one class, checked in `activate(...)` against
  `ProfileService#cached(playerId).selectedClass()` (a new `ProfileService`
  dependency on `YamlAbilityService`), rejecting with a new
  `ABILITY_WRONG_CLASS` message on mismatch. `AbilitiesMenu` filters to only
  the player's selected class's abilities (a new "no class yet" empty state,
  `GUI_ABILITIES_NO_CLASS`); `ClassMenu#selectClass`'s `SELECTED` branch now
  also clears the player's in-memory ability *selection*
  (`AbilityService#clearSelection`, new) so a stale cross-class selection
  can't silently linger after switching.
- **8 new abilities** (every class now has exactly 3; the 4 pre-existing
  ones - `kazici-darbe`, `agac-kirici`, `damar-kirici`, `avci-odagi` - became
  each class's mid-tier signature ability unchanged). Three new
  `AbilityEffectType` shapes were needed beyond the original two
  (armed-for-a-block-break, buffed-for-damage): **instant** effects applied
  immediately inside `activateOnPlayerRegion` (`BRIEF_INVULNERABILITY`,
  `AREA_HEAL`, `SPEED_BURST`, `NIGHT_VISION` via vanilla `PotionEffect`s;
  `KNOCKBACK_SLAM` via nearby-entity velocity+damage), and one more
  **armed-for-a-block-break-with-a-chance** shape (`EXTRA_DROP_CHANCE`, a new
  parallel `dropBuffs` map, rolled inside `handleBlockBreak` independent of
  whatever else is armed - it fires on *any* normal block break, not just
  the ability's own tool). `AbilityToolTag.ANY` (matches every material,
  including an empty hand) was added for one tool-agnostic ability.
  `AbilityDefinition.limit`/`damageMultiplier` are deliberately dual-purposed
  per effect type rather than growing the record further (`limit` doubles as
  `KNOCKBACK_SLAM`'s radius; `damageMultiplier` doubles as
  `EXTRA_DROP_CHANCE`'s 0-1 chance) - `abilities.yml` gained matching
  `etki.sans`/`etki.yaricap` keys, and `YamlAbilityService#parse`'s
  validation became a `switch` over effect type instead of one blanket rule.
- One pre-existing talent (`madenci`'s `kazici-tasarrufu`, which reduced
  `kazici-darbe`'s cost) had to be retargeted, since `kazici-darbe` became
  `savasci`-exclusive under the new class-gating - renamed to
  `cevher-tasarrufu`, now reducing the new `madenci`-exclusive
  `cevher-sezgisi` instead. This is the one non-additive edit `classes.yml`
  needed this phase.

### 9.4 — Synergy XP bonus, class-level XP wiring, new-ability-unlocked toast

- `SkillService#awardBlock`/`#awardMob` now return
  `CompletableFuture<Optional<SkillAwardResult>>` (progress + pre-award
  level + actual granted amount + `leveledUp`) instead of a bare
  `SkillProgress` - the first time skill-XP awards exposed a level-up
  signal at all, mirroring `ProgressionAwardResult`'s existing shape.
  Both methods also gained a `playerName` parameter (only `SkillListener`
  called them; safe to widen).
- **The class synergy bonus (+20% XP in the selected class's one synergy
  skill) and the class-XP mirroring are both wired through
  `CachedSkillService`'s constructor**, via the same
  `Function`/`AtomicReference` circular-dependency bridge already
  established between `AbilityService`/`ClassService`
  (`SkillService` is constructed before `ClassService` in the bootstrap):
  a `BiFunction<UUID,String,Double> classSynergyMultiplier` decides the
  bonus, and a new `skills.SkillExperienceListener` callback (deliberately
  defined in the `skills` package, not `classes`, so `skills` never depends
  on `classes`) notifies a new `classes.ClassSynergyBridge` after every
  award - which resolves the real `ClassService` from an
  `AtomicReference<ClassService>` forward-reference and, if the skill
  matches the selected class's synergy skill, calls
  `awardClassExperience(...)` with the *same* granted amount (1:1
  mirroring - no separate class-XP rate, the simplest defensible default).
- **New-ability-unlocked toast**: `SkillListener` now checks, on every
  `leveledUp()` skill award, whether any of the player's selected class's
  abilities has a `requiredLevel` in `(previousLevel, newLevel]` - handles
  multi-level jumps correctly, not just +1 increments - and fires
  `FeedbackService#celebrateAbilityUnlocked` (new, bossbar+title/subtitle
  fade, same shape as quest/collection completion). Two more new chat-only
  celebrations were added alongside it for symmetry:
  `celebrateClassLevelUp`/`celebrateSkillLevelUp` (skill level-up previously
  had *no* dedicated feedback beyond the XP bossbar - this phase formalizes
  it). **`FeedbackService` was moved earlier in the plugin bootstrap**
  (right after `ProgressionService`, before `ClassService`/`SkillService`)
  purely so `ClassSynergyBridge` could depend on it - it has no other
  dependencies, so the reorder is inert everywhere else.

### 9.5 — Sound effects (first-ever `org.bukkit.Sound` usage)

- New `feedback.yml` `sesler:` section (7 named cues: level-up, skill
  level-up, class level-up, class-selected, ability-start, ability-end,
  ability-unlocked), each `{ ses: <Sound enum name>, ses-seviyesi, ton }`,
  parsed/validated at load time exactly like every other YAML enum in this
  codebase (`Sound.valueOf(...)` - deprecated-for-removal on this Paper
  version but not yet actually removed; left as-is rather than switching to
  a `Registry.SOUNDS` lookup, since every cue is config-driven and trivially
  changed later regardless of which lookup mechanism resolves the name).
  `BossBarFeedbackService` gained a private `playSound(Player, String key)`
  helper wired into the corresponding celebration methods, plus two new
  sound-only methods with no accompanying chat message
  (`celebrateClassSelected`, called from `ClassMenu`; `celebrateAbilityActivated`/
  `celebrateAbilityEnded`, called from `YamlAbilityService` - which gained a
  `FeedbackService` dependency for exactly this).
  `YamlAbilityService#clearActivationFeedback` only plays the "ended" sound
  when a feedback task was actually active, to avoid firing it on every
  drop/held-item-change/quit event regardless of whether an ability was
  running.

### 9.6 — `/mmo aktif` onboarding gate and class-intro/confirm GUIs

- New `activation` package (`ActivationService`/`CachedActivationService`/
  `ActivationRepository`), structurally identical to the
  `hmmo_player_unlocked_titles` INSERT-only membership pattern - presence of
  a row means activated, no update-able columns. New V8
  `PlayerActivationSchemaMigration`.
- **The onboarding gate is a full progression kill-switch, not a cosmetic
  sidebar toggle** (an explicit user decision during plan review, flagged as
  the single biggest judgment call in the phase): `ProgressionListener`'s
  shared `award(...)` method (covers block/mob/playtime XP),
  `SkillListener`'s block/mob handlers, `QuestListener`'s block/mob
  handlers, and `CollectionListener#onPickup` all now no-op for a
  not-yet-activated player. The playtime task additionally pre-filters
  online players by activation before even attempting an award.
  `ActivationService#isActivated` is warmed on join from
  `ProgressionListener#onJoin` (which already loads the profile first).
- **Self-found and fixed, not originally scoped**: `SidebarListener#onJoin`
  did not wait on the activation cache before calling
  `sidebar.initialize(...)`, meaning an already-activated *returning*
  player could momentarily (and with nothing to ever self-correct it) see
  the "please activate" prompt line instead of their real stats if the
  render ran before `ActivationService#isActivated` resolved. Fixed by
  chaining `activation.isActivated(...)` into the same join future
  `SidebarListener` already awaits before rendering.
- `/mmo aktif` (new `MmoCommand`) is idempotent (`MMO_ALREADY_ACTIVE` if
  already set) and, on success, immediately opens the new `ClassIntroMenu`
  (all 4 classes, a one-line flavor blurb per class plus their 3 abilities'
  live display names - pulled from `AbilityService#definitions()` filtered
  by `classId`, so this screen can never drift from the ability catalogue).
  Picking a class opens `ClassConfirmMenu`, which calls the *exact same*
  `ClassService#select(...)` path `ClassMenu` already uses (identical
  `ClassSelectionResult.Status` handling) - wording branches on whether the
  player already has a class (first-ever pick: no cooldown mentioned,
  applies immediately; a returning pick: shows the real
  `sinif-degistirme-bekleme-gun` value).
- `ScoreboardSidebarService` gained an `ActivationService` dependency and,
  for this sub-phase, only the minimal "not activated → single prompt line"
  branch (the full activated-state redesign is §23's 9.7). **This is also
  where the sidebar's line-count-varies-per-render problem first became
  real** (previously always exactly 5 lines) - `render(...)` now
  unregisters any `Team` beyond the current pass's line count, a latent gap
  in the original Phase 8 implementation that had gone unnoticed because
  line count never actually changed before now.

### 9.7 — Full sidebar redesign

- `QuestService#cachedAll(UUID)` (new, synchronous) exposes
  `CachedQuestService`'s already-existing internal per-player cache
  (warmed by the existing `QuestListener#onJoin` → `loadAll(...)` call) -
  no new caching machinery needed. Inherits the pre-existing, unchanged
  lazy-quest-reset staleness (§16) - a completed daily/weekly quest may
  still read `completed=true` briefly past period-end until the player's
  next progress-triggering action.
- `ScoreboardSidebarService` gained `SkillService`/`AbilityService`/
  `QuestService` dependencies, which required **moving its construction in
  `HalilIbrahimMMOPlugin#onEnable()` to after `QuestService`** (previously
  built right after `ClassService`) - confirmed safe since `EnergyService`,
  built in between in the old order, has no dependency on it either way.
- Final activated-state line set (name → combined Güç level+XP → prestige →
  selected class → combined class level+XP for that class → active
  ability+countdown → "X/3 açık" ability count → daily quest count → weekly
  quest count), each omitted-not-blank when not applicable (no class: skip
  class-level/ability lines entirely, show a `SIDEBAR_LINE_NO_CLASS_NUDGE`
  instead; no active ability: skip that line rather than showing an idle
  placeholder). `SIDEBAR_LINE_XP` (the old separate XP line) was deleted as
  now-genuinely-dead once Güç level+XP merged into one line.

### Verification (this phase)

- `./gradlew build`/`./gradlew test`/`:core:compileIntegrationTestJava` are
  all green with Java 26 in this session. `IntegrationDatabaseSupport
  .allMigrations()` and `AbstractMigrationCrossEngineTest`'s hardcoded
  `Set.of(1..6)` version assertion were both updated to include V7/V8 - the
  cross-engine containers themselves were not run this session (no Docker
  daemon available), same caveat §21 already documented.
- **A live `./gradlew runServer` boot was performed and is the one piece of
  in-game verification this session could actually do**: booted cleanly to
  `Done`, all 8 migrations (including the new V7/V8) applied against a
  fresh SQLite database with the previous session's stale
  `classes.yml`/`abilities.yml`/`feedback.yml`/`messages.yml` deleted first
  so they regenerated from the updated bundled resources, and the plugin
  enabled with zero exceptions - confirming every new/changed YAML file
  parses correctly (including all 7 `sesler:` `Sound.valueOf(...)` calls
  resolving to real enum constants) and the reordered service bootstrap has
  no circular-dependency or construction-order fault. The server was then
  stopped (no interactive console was available in this session to send it
  a graceful `stop`).
- **Not verified this session, requires a live client and is the
  responsibility of the next session/the user** (same category of gap
  every GUI-touching phase since §17 has honestly recorded): actually
  clicking through `ClassIntroMenu`/`ClassConfirmMenu`/the filtered
  `AbilitiesMenu`; right-click-activating each of the 6 new
  `AbilityEffectType`s in-world and confirming their Bukkit potion/velocity/
  drop-duplication effects actually do something sensible; hearing all 7
  sound cues; watching the redesigned sidebar render correctly across all
  four states (unactivated / activated-no-class / activated-with-class /
  mid-ability-countdown) and confirming no stale lines linger when
  switching classes back and forth; confirming the synergy XP bonus and
  class-level-up chat message actually fire correctly during real gameplay.

## 24. Phase 10 — admin GUI-only, rarity XP, ability rework, sidebar/chat/bossbar polish (Phase 10 complete)

A large, user-requested follow-on phase, built in eight stop-and-report
sub-phases (10.1-10.8): (1) `/haliladmin` GUI-only admin panel, replacing
`/rpg`'s chat-argument dispatch and removing every command that let staff
hand-set a player's XP/level/class/skill values, (2) rarity-tiered XP for
every skill, (3) ability-unlock gating verified already correct, (4)
duration-vs-energy-drain ability activation modes, longer cooldowns, and a
locked-ability display/tooltip fix in `AbilitiesMenu`, (5) `/menu`
reorganized into four categories, (6) per-title chat tag colors, (7) full
sidebar redesign, (8) simultaneous skill+class XP bossbars with per-class
colors and the specific block/mob name in the skill bar.

### 10.1 - `/haliladmin` GUI-only admin panel

- **The entire chat-argument admin command surface was deleted**, not just
  edited: `command.RpgCommand`, `command.SubCommand`, and all four
  `command.subcommand.*` classes (`ReloadSubCommand`, `VersionSubCommand`,
  `DebugToggleSubCommand`, `PlayerAdminSubCommand`) are gone. `/rpg oyuncu`'s
  five mutation actions (`seviye`, `xp ver`, `sinif`, `yetenek`,
  `istatistik`) are permanently removed per an explicit user requirement -
  there is no code path anywhere in this codebase that can hand-set a
  player's XP/level/class/skill values anymore. The backing service methods
  that only ever existed for those actions were deleted too, confirmed via
  grep before removal to have no other call sites:
  `ProgressionService#setLevel`, `ClassService#forceSetClass`,
  `SkillService#setLevel`, `SkillService#addExperience`,
  `ProfileService#setStatistic` (+ its dedicated `ProfileRepository` method).
  `bilgi` (read-only player info) survives, reimplemented as a GUI screen.
- New `command.AdminCommand` registers `/haliladmin`, gated by
  `halilibrahimmmo.admin` checked in the command itself (unlike `/menu`,
  which is open to everyone) before opening `gui.AdminMenu`. **Permission
  model deliberately simplified from Phase 8's design**: the six granular
  `halilibrahimmmo.admin.oyuncu.*` LuckPerms-delegation nodes are gone,
  replaced by the single `halilibrahimmmo.admin` node gating the whole
  panel - once every admin action collapsed into one GUI behind one command,
  the fine-grained per-action delegation Phase 8 built specifically for
  chat-argument dispatch no longer had a reason to exist.
- `gui.AdminMenu` (reload / debug toggle / version info / online-players
  browser) → `gui.AdminOnlinePlayersMenu` (one player-head per online
  player) → `gui.AdminPlayerInfoMenu` (read-only profile/class/skill
  summary, same fields `PlayerAdminSubCommand#info` used to render). Reload
  and debug-toggle call the exact same `HalilIbrahimMMOAPI#reload()`/
  `ConfigService#setDebugModeOverride(...)` the old chat commands called,
  then reopen `AdminMenu` so the debug-toggle item's lore reflects the new
  state immediately.
- `GuiServices` gained `ConfigService` and `HalilIbrahimMMOAPI` fields
  (needed by `AdminMenu`) - the first GUI-tree record to need either.
- Every `/rpg yenile`/`/rpg hataayiklama` reference across YAML comments,
  Javadoc and `README.md` was updated to `/haliladmin` in the same session -
  confirmed via an exhaustive grep for `/rpg` afterward, zero remaining.

### 10.2 - Rarity-tiered XP economy (all four skills, not just avcılık)

- **`SkillDefinition`'s single flat `reward` field is gone.** New record
  `SkillRewardEntry(long xp, String displayName)` backs a per-material map
  (`materialRewards`, BLOCK skills - same "material must be in the map to
  trigger at all" gate as before, just per-material XP now) or a per-entity
  map plus a `defaultEntityReward` fallback (MOB skill/`avcilik` - any
  non-player kill still counts, unlisted `EntityType`s fall back to the
  default entry so nothing silently grants zero). `skills.yml` was fully
  rewritten: e.g. `DIRT` → 1xp, `DIAMOND_ORE` → 14xp, `ANCIENT_DEBRIS` →
  20xp; `ZOMBIE` → 5xp, `ELDER_GUARDIAN` → 60xp, `WARDEN` → 200xp,
  `ENDER_DRAGON` → 250xp (~30 explicitly tiered mobs; the numbers are a
  first-pass design, trivially retunable in YAML since every value is
  data-driven).
  `SkillService#awardMob` gained an `EntityType` parameter (threaded from
  `SkillListener#onEntityDeath`'s `event.getEntity().getType()`) since MOB
  rewards are no longer a single flat number.
- `SkillAwardResult` gained `sourceDisplayName` (the specific ore/mob's
  Turkish name) so the skill XP bossbar can show it:
  `geribildirim.xp-beceri-bossbar` gained a `<kaynak>` placeholder,
  rendering e.g. "Madencilik +14xp (Elmas Cevheri) (42/500)" - only this one
  template changed; the separate *global* progression bossbar templates
  (`ProgressionListener`) were intentionally left alone.

### 10.3 - Ability unlock gating: verified already correct

`YamlAbilityService#activateOnPlayerRegion` already rejected activation
below `definition.requiredLevel()` on `definition.skillId()`'s level - and
every ability's configured skill already equals its owning class's
`sinerji-beceri` (Phase 9.2). "Abilities unlock strictly by the selected
class's synergy-skill level" was therefore already true; no code changed,
called out explicitly so a future session doesn't mistake it for an
oversight.

### 10.4 - Duration vs. energy-drain abilities, longer cooldowns, locked-ability display

- New `abilities.AbilityActivationMode` (`SURE`/`ENERJI`, YAML key
  `sure-modu`, default `SURE` - every ability's exact prior behavior).
  `ENERJI` mode is used by exactly one ability so far - `avci-odagi` (Avcı
  Odağı, the sustained `DAMAGE_BUFF` combat buff) - per an explicit user
  example (night vision = duration-based, the combat buff = energy-drain-
  per-hit). `AbilityService#handleEntityDamage` gained an `EnergyService`
  parameter (its one call site, `ClassListener#onDamage`, already had
  `EnergyService` available - no new circular-dependency wiring needed) and
  now debits `enerji-vurus-basi` (8, for `avci-odagi`) on every qualifying
  PvE hit, ending the buff early via the existing `clearActivationFeedback`
  path once energy is insufficient. `etki-suresi-saniye` remains a hard
  fallback expiry (120s for `avci-odagi`) regardless of energy, so the
  ability can never run unbounded. The action-bar countdown
  (`startActivationFeedback`) shows remaining energy instead of remaining
  seconds for `ENERJI`-mode abilities (new `kabiliyet.geri-sayim-enerji`).
- Every ability's `bekleme-suresi-saniye` was raised (roughly 60-100% longer,
  heavier for higher-tier abilities) - e.g. `hazine-sezgisi` 60→90s,
  `kaya-zirhi` 90→160s.
- **Self-found gap, not originally scoped**: `AbilitiesMenu` showed every
  ability belonging to the selected class with full name/description/cost
  regardless of whether the player's skill level actually unlocked it -
  fixed by checking `skills.cached(...)` per ability during render: locked
  abilities now render as a non-clickable red `BARRIER` item showing only
  the required level, no description or energy cost; unlocked abilities
  gained a hover description (new `aciklama` field on every
  `AbilityDefinition`/`abilities.yml` entry) and only show an energy-cost
  lore line when `energyCost() > 0`.

### 10.5 - `/menu` reorganized into categories

`AnaMenu`'s eleven flat icons became four category icons - İlerleme (new
`gui.ProgressionMenu`: profile/skills/class/prestige), Kabiliyetlerim
(unchanged, stayed top-level as this phase's most-reworked system), Görevler
(new `gui.TasksMenu`: quests/collections/journal), Topluluk (new
`gui.CommunityMenu`: leaderboards/titles/cosmetics). Every existing leaf
menu's back button was repointed from `AnaMenu` to its new category parent
(`AbilitiesMenu` is the one exception, staying pointed at `AnaMenu` since it
never moved into a category).

### 10.6 - Per-title chat tag colors

- New bounded `titles.TitleChatStyle` enum (6 members: color + bold flag),
  the same "YAML names a bounded enum constant, Java owns the real value"
  pattern `kaynak.tur`/`etki.tur`/`arac.tur` already use. `TitleDefinition`
  gained a `chatStyle` field parsed from a new `titles.yml` `sohbet-stili`
  key per title; `TitleService` gained a synchronous `equippedStyle(UUID)`
  getter mirroring the existing `equippedDisplayName` cache shape exactly
  (`CachedTitleService`'s `cacheEquipped` now populates both caches
  together).
- **`TitleChatListener` was rewritten to build the chat prefix as a directly-
  styled Adventure `Component`** (color/bold from `TitleChatStyle`) instead
  of one shared `MessageKey` template coloring every title identically -
  the one deliberate, documented exception in this codebase to "always
  render through a `MessageKey`/`messages.yml` template": a title's color is
  genuinely per-title configured data, and `MessageService` only ever
  substitutes unparsed string placeholders, which cannot vary a
  placeholder's own color per invocation. `MessageKey.TITLE_CHAT_PREFIX`/
  `unvan.sohbet-oneki` were deleted as now-genuinely-dead.

### 10.7 - Full sidebar redesign

- `ScoreboardSidebarService` gained a `TitleService` dependency (for the
  name-line tag, reusing `TitleChatStyle` so a player's chat tag and
  sidebar tag always match) - required moving its construction in
  `HalilIbrahimMMOPlugin#onEnable()` to after `TitleService`/
  `CosmeticService`, the same kind of reorder §9.7 already did once for
  `QuestService`.
- **The fixed 9-entry `LINE_ENTRIES` array is gone**, replaced by a
  `lineEntry(int index)` generator (single legacy code for index 0-15,
  two-code combinations beyond that) - needed because the new layout
  routinely exceeds 9 lines once per-ability and per-daily-quest lines are
  included. The stale-team cleanup in `render(...)` now scans forward from
  `board.getTeam(TEAM_PREFIX + lines.size())` until it finds an
  unregistered slot, rather than looping a fixed upper bound.
- Line changes: the name line now prefixes the equipped title in its
  configured color (10.6); the old "X/3 açık" ability-count summary became
  **one line per unlocked ability** of the selected class, green if it is
  the player's currently menu-selected ability
  (`AbilityService#selected`) or red otherwise, locked abilities omitted
  entirely; the old single daily-quest summary became **one line per DAILY
  quest**, showing its name, `current/target` progress and its configured
  XP reward (weekly quests kept the existing single summary line - only
  daily was asked to expand); a new always-shown footer line
  (`sidebar.alt-bilgi`, "`/panel` ile aç/kapat") was added to every state
  including unactivated/no-class, not just the fully-activated view.

### 10.8 - Simultaneous skill+class bossbars, per-class colors, source name

- **Fixed a pre-existing bug found during this phase's research, not
  originally scoped**: `BossBarFeedbackService`'s one reusable bar per
  player was keyed by `UUID` alone, so a single block break/mob kill's
  *global* progression bossbar and *skill* bossbar (`ProgressionListener`
  and `SkillListener` both call `showExperienceProgress` back-to-back on
  the same player) were silently clobbering each other - only whichever
  call landed last was ever visible. Fixed by rekeying
  `experienceBars`/`experienceBarHideTasks` to a
  `record ExperienceBarKey(UUID playerId, String category)`
  (`"global"`/`"skill"`/`"class"`), which also happens to be exactly what
  the new simultaneous class bar needed anyway.
  `FeedbackService#showExperienceProgress` gained `category`/
  `BossBar.Color` parameters; every existing call site
  (`ProgressionListener`, `SkillListener`, `QuestListener`,
  `CollectionListener`) was updated to pass its category and (for now,
  uniformly) `BossBar.Color.GREEN`.
- New bounded `classes.ClassBarColor` enum (4 members → 4 distinct
  `BossBar.Color`s), referenced from a new `classes.yml` `bossbar-rengi`
  key per class - same bounded-enum-from-YAML pattern as 10.6's
  `TitleChatStyle`. Assigned: Define Avcısı=SARI(yellow), Amansız
  Savaşçı=KIRMIZI(red), Mağara Adamı=MAVI(blue), Orman Bekçisi=YESIL(green).
  `PlayerClassDefinition` gained a `barColor` field.
- `ClassSynergyBridge#onSkillExperienceGained` (previously silent - it
  mirrored synergy-skill XP into the class-level track with zero
  player-facing feedback) now also shows a `"class"`-category bossbar in
  the class's own color right alongside the skill bar `SkillListener`
  already shows, so a synergy-skill kill/break now visibly shows **both**
  bars at once. New `MessageKey.FEEDBACK_XP_CLASS_BOSSBAR`/
  `geribildirim.xp-sinif-bossbar`.

### Verification (this phase)

- `./gradlew build`/`./gradlew test` are green after every sub-phase and at
  the end of the full phase, Java 26.
  `SkillMaterialConfigTest`/`ClassConfigTest`/`TitleConfigTest` were updated
  for the new `skills.yml` map shape and the new `sohbet-stili`/
  `bossbar-rengi` YAML fields respectively.
- **A live `./gradlew runServer` boot was performed after all eight
  sub-phases**, with the previous session's stale `config.yml`/
  `messages.yml`/`skills.yml`/`abilities.yml`/`titles.yml`/`classes.yml`/
  `quests.yml`/`collections.yml` deleted from `core/run/plugins/
  HalilIbrahimMMO/` first so every changed file regenerated from the
  updated bundled resources (same pattern §23 already used). The plugin
  enabled with zero exceptions, the server reached `Done` in ~8s, and the
  log had zero "eksik mesaj anahtari" (missing message key) warnings -
  confirming every new/renamed `MessageKey` has a matching `messages.yml`
  entry, every new bounded enum (`AbilityActivationMode`, `TitleChatStyle`,
  `ClassBarColor`) resolves against its YAML string in every bundled file,
  and the reordered `SidebarService` construction has no circular-
  dependency fault.
- **Not verified this session, requires a live client** (same honest
  caveat every GUI/feedback-touching phase since §17 has carried):
  right-click-activating `avci-odagi` in real PvE combat and watching it
  end early on energy depletion; confirming the two simultaneous bossbars
  actually render side-by-side (Adventure supports multiple bars per
  player, but this was never exercised live before); reading the
  redesigned sidebar across all states; confirming locked abilities render
  correctly in `AbilitiesMenu`; clicking through the new `/haliladmin`
  panel and `/menu` category screens.

## 25. Phase 11 — class-gated abilities, derived Güç, prestige rework, admin
    all-players panel, quest pools, onboarding, skill milestones (Phase 11
    complete)

A large, user-requested follow-on phase covering 10 concrete bugs/design
gaps plus one new feature (per-skill milestone boosts) and a sidebar
polish pass, built in ten stop-and-report sub-phases (11.1-11.10, in the
dependency order 11.1→11.2→11.3→11.6, everything else independent). The
load-bearing design fork was resolved before implementation started: making
Güç (global power) a pure derived sum of skill levels conflicts with
prestige's old "reset global level, re-hit the cap" design, since skills
live in `SkillService`/`ClassService`, not on `PlayerProfile`. Resolution:
**prestige now resets the player's currently-selected class's class-level
and all 4 skill levels back to 1/0 XP (losing skill-milestone passive
boosts as a result), in exchange for a permanent +25%-per-rank XP
multiplier and a permanent per-class "prestige boost"; prestiged players
always outrank non-prestiged players on the leaderboard regardless of raw
level.**

### 11.1 - Ability unlock gated by class level, not skill level

- `YamlAbilityService#activate` no longer needs the async
  `skills.load(...).thenAccept(...)` round-trip at all: `ClassService#
  classProgress` is already synchronous (in-memory `progressCache`,
  defaults to level 1), so activation now reads the class level directly
  through the existing `AtomicReference<ClassService>` bootstrap bridge
  (the same one already backing the `classModifiers` lambda) and schedules
  straight onto the player's region. One async hop removed, not added.
- `AbilitiesMenu#render`'s gate switched from `services.skills().cached(...)`
  to `services.classes().classProgress(playerId, definition.classId())`.
- `definition.skillId()` stays on `AbilityDefinition` unchanged - it is
  still independently used by `isEligibleBlock()`'s material-set check and
  by `abilities.yml` validation. Only the unlock *comparison* moved from
  skill level to class level.

### 11.2 - Güç becomes a derived sum of skill levels; independent Güç-XP removed

- `ProgressionService` gained `int cachedGlobalLevel(UUID)` (synchronous:
  sums every configured skill's cached level, defaulting each to 1 if
  uncached) and `CompletableFuture<Integer> globalLevel(UUID)` (async,
  offline-safe equivalent used by §11.4's admin panel).
- **Deleted outright, not left as dead code** (matching Phase 10.1's
  "delete fully" precedent): `ProgressionService#award`/`awardConfigured`,
  `progression.ExperienceCurve`, `progression.ProgressionSettings`,
  `progression.ProgressionAwardResult`, `resources/progression.yml`,
  `api.progression.ProgressionCause` (every member, including the
  already-dead `EXTERNAL`), and `HalilIbrahimMMOAPI#awardGlobalExperience`
  (+ its `HalilIbrahimMMOApiImpl` implementation). `PlayerProfile.
  globalLevel`/`globalExperience` remain DB columns - they simply stop
  being written; nothing reads them for gameplay anymore.
  `ProgressionListener` survives, shrunk to just `onJoin`/`onQuit` (profile
  load/evict, activation warm/evict, clearing the experience-progress HUD).
- **Quest and collection XP now redirect to the player's selected class's
  XP** instead of global XP: `QuestListener#notify` and both award call
  sites in `CollectionListener` resolve `profileService.cached(playerId)
  .selectedClass()` and call `ClassService#awardClassExperience`; the
  award is skipped (completion/journal/broadcast still fire) if no class
  is selected.
- **Fixed, not originally scoped**: `HalilIbrahimMMOApiImpl#findCachedPlayer`
  used to read `profile.globalLevel()` (a PlaceholderAPI/LuckPerms-facing
  method, backing `%halilibrahimmmo_level%` and the `halilibrahimmmo:level`
  LuckPerms context from Phase 7) - left alone, it would have silently
  frozen at whatever value happened to exist when this phase shipped.
  Fixed to source `globalLevel` from `progression.cachedGlobalLevel(...)`.
  `globalExperience`/`experienceToNextLevel` have no meaning under a
  derived, whole-integer Güç, so `CachedPlayerSnapshot` now reports both as
  `0` - documented with a Javadoc note on both `HalilIbrahimMMOExpansion`
  and `HalilIbrahimMMOContextCalculator` so a server admin who built a
  placeholder display around `%halilibrahimmmo_xp%`/`_xp_to_next_level%`
  isn't silently surprised.
- Skill/class XP bossbars (categories `"skill"`/`"class"`) are unaffected -
  only the `"global"`-category bossbar's *trigger* changed, see §11.6.

### 11.3 - Prestige rework

- New `SkillService#resetAll(UUID)` (every configured skill → level 1/XP 0,
  same write-through path `awardMatching` uses) and `ClassService#
  resetClassLevel(UUID, String, String)` (one specific class's
  `ClassLevelProgress` → level 1/XP 0, mirroring `awardClassExperience`'s
  update shape). `YamlPrestigeService#prestige` now chains: bump
  `prestige` only (globalLevel/globalExperience are dead fields, no longer
  touched by `PrestigeRules.apply`) → `skillService.resetAll(...)` → (if a
  class is selected) `classService.resetClassLevel(...)`.
- **Eligibility** moved from `profile.globalLevel()` against a curve-based
  cap to `progressionService.cachedGlobalLevel(playerId)` against a new
  `prestige.yml` key `gerekli-guc-seviyesi` (400 = 4 skills × 100 by
  default). `PrestigeService`'s constructor widened to take
  `SkillService`/`ClassService` - both were already constructed before
  `PrestigeService` in `HalilIbrahimMMOPlugin#onEnable()`, so no bootstrap
  reordering was needed.
- Skill-milestone passive boosts (§11.9) fall away automatically once
  skills reset to level 1 - no separate strip step needed, they recompute
  the same way class modifiers already recompute from talent ranks.
- **Permanent rewards.** +25% XP per prestige rank (`1 + 0.25 * prestige`,
  stacking additively), folded into the existing `classSynergyMultiplier`
  lambda in `CachedSkillService` (skill XP) and into
  `YamlClassService#awardClassExperience` (class XP) - admin-granted XP
  (§11.4) deliberately does **not** get this multiplier, since those are
  exact-amount corrections. Per-class permanent "prestige boost": new
  `classes.yml` field per class, `prestij-etkisi: { tur: <ClassEffectType>,
  deger-basina: <double> }`, reusing the existing bounded `ClassEffectType`
  enum (restricted at parse time to `MAX_HEALTH`/`PVE_DAMAGE_MULTIPLIER`/
  `MAX_ENERGY`) - `YamlClassService#modifiers` sums
  `prestigeCount * definition.prestigeEffectValuePerRank()` into the
  player's selected class's existing talent-summing switch, reusing the
  same stable-`NamespacedKey` remove-then-reapply pattern `applyPlayerEffects`
  already uses for `MAX_HEALTH`.
- **Leaderboard** query rewritten to a `LEFT JOIN` against
  `hmmo_player_skills`, `ORDER BY p.prestige DESC, total_level DESC` -
  prestige always outranks raw level regardless of magnitude. The `IN (...)`
  skill-id placeholder list is built dynamically from
  `skillService.definitions().keySet()` at query time, not hardcoded to 4
  `?`s, since `skills.yml` is data-driven. `LeaderboardRepository`'s
  constructor widened to take `SkillService` (already constructed earlier
  in bootstrap - pure widening, no reorder). Board secondary-metric lore
  switched from XP to prestige count.
- **Fanfare, additive to the existing personal `celebratePrestige`** (kept
  exactly as-is - this is not a replacement): new `MessageService#
  broadcast(MessageKey, Map)` (first broadcast mechanism in the codebase,
  iterates `Bukkit.getOnlinePlayers()`, reused by §11.6/§11.7) announces
  the player + new rank server-wide; a `Firework` (first use in the
  codebase) spawns at the prestiging player's own location (inherently
  local, not broadcast); `Sound.ENTITY_ENDER_DRAGON_DEATH` +
  `Sound.ENTITY_PLAYER_LEVELUP` play to every online player. Both
  `PrestigeCommand` and `PrestigeMenu` call the same sequence.
  `PrestigeMenu`'s info lore now explains the reset scope + the +25% XP +
  the per-class permanent boost before a player confirms.

### 11.4 - Admin panel: all-registered-players tab + skill/class/XP mutation

- New `ActivationRepository#findAllActivated()` (joins
  `hmmo_player_activation` with `hmmo_player_profiles` for the name, no
  migration needed) and its `ActivationService` async wrapper, backing a
  new `ActivatedPlayer(UUID playerId, String playerName)` record.
- Re-added offline-capable admin mutation, this time GUI-only and scoped
  to `/haliladmin` (Phase 8.4 had chat-argument equivalents; Phase 10.1
  deleted them entirely; this phase deliberately reintroduces the
  capability behind the GUI-only click model instead):
  `SkillService#adminSetLevel`/`adminAddExperience`, `ClassService#
  adminSetClassLevel`/`adminAddClassExperience`/`adminForceSetClass`. Every
  method writes straight to the repository/cache regardless of whether the
  target is online, matching the online path's write-through shape so a
  currently-online target's live cache stays correct too.
- New `gui.AdminAllPlayersMenu` (every activated player, online or not,
  capped like `AdminOnlinePlayersMenu`, no pagination v1) →
  `gui.AdminPlayerEditMenu` (read-only summary + click-increment rows -
  `LEFT`/`SHIFT_LEFT`/`RIGHT`/`SHIFT_RIGHT` → ±1/±10 skill/class level,
  `LEFT`/`SHIFT_LEFT` → +50/+500 skill/class XP) → `gui.
  AdminClassPickerMenu` (force-sets or clears the target's class,
  bypassing the change cooldown). `AdminMenu` gained a button; every
  mutation logs a Turkish `PluginLogger` line naming the acting admin, the
  target, and the new value.

### 11.5 - Ability activation polish: item-switch bug fix + selection cooldown

- **Fixed a real bug**: `AbilityActivationListener#onHeldItemChange`
  cleared activation feedback unconditionally on *every* hotbar slot
  change - now only clears when the newly-held item no longer matches the
  active/armed ability's configured tool.
- `YamlAbilityService#select` gained a configurable switch cooldown
  (`abilities.yml` `secim-bekleme-saniye`, default 120s) via a
  `ConcurrentHashMap<UUID, Long> lastSwitchMillis` - re-selecting the same
  ability id stays free. New `MessageKey.ABILITY_SELECTION_COOLDOWN`
  (`kabiliyet.secim-bekleme`) shows remaining seconds on rejection.

### 11.6 - Güç bossbar only at multiples of 10, broadcast at 25/50/75/100

Mirrors Phase 9.4's `(previousLevel, newLevel]` range-check pattern.
`SkillListener` now computes Güç before/after any skill award with
`leveledUp()`, shows the `"global"`-category bossbar only if a multiple of
10 falls in that range (replacing the old always-on-every-XP-gain bar,
already removed in §11.2), and calls `MessageService#broadcast` if
25/50/75/100 falls in that range.

### 11.7 - Quest pools: random daily/weekly assignment + completion broadcast

- `quests.yml`'s DAILY/WEEKLY catalogues expanded into real pools (10
  daily, 8 weekly entries; the 2 existing PERMANENT quests stay
  unpooled - every player always has both). New keys
  `gunluk-atanan-sayisi`/`haftalik-atanan-sayisi` (default 3/2) control how
  many each player is randomly assigned per period.
- New migration **V9** (`QuestAssignmentSchemaMigration`):
  `hmmo_player_quest_assignments` (`player_id`, `quest_type`,
  `assigned_quest_ids` comma-joined, `period_end_millis`), same
  single-`CREATE TABLE IF NOT EXISTS` shape as V7/V8.
  `CachedQuestService#randomAssignment` shuffles the type's pool
  (`Collections.shuffle`+`ThreadLocalRandom`) and takes
  `Math.min(pool.size(), assignedCount)` - safe even if a future config
  edit shrinks a pool below the configured assignment count. Every read
  path that used to iterate "every DAILY/WEEKLY quest in the catalogue"
  (`CachedQuestService`, `QuestsMenu`, `/gorev`, the sidebar's daily-quest
  lines) now filters to the player's currently-assigned ids plus all
  PERMANENT quests.
- **Self-found bug, not originally scoped**: `CachedQuestService`'s
  pre-existing private `load(...)` backfill loop unconditionally
  repopulated the cache for *every* configured quest definition, which
  would have silently defeated the new pool filtering the first time
  `find()`/`incrementNow()` ran for an unassigned pool quest - fixed by
  scoping that backfill to PERMANENT quests plus the one
  specifically-requested definition.
- `QuestListener#notify` now calls `messages.broadcast(...)` for
  DAILY/WEEKLY completions specifically (naming the player and quest, no
  XP amount shown - explicit requirement), leaving PERMANENT completions
  as personal-only feedback.

### 11.8 - New-player onboarding nudges

- New `onboarding` package (`OnboardingTask` - a fixed, deliberately
  non-YAML-configurable 3-entry enum, `OnboardingRepository`,
  `OnboardingService`/`CachedOnboardingService` - mirrors `activation`'s
  INSERT-only cache shape). New migration **V10**
  (`OnboardingSchemaMigration`): `hmmo_player_onboarding_tasks`
  (`player_id`, `task_id`, composite primary key preventing double-award).
- Three one-time tasks, each rewarding 25 XP through the player's selected
  class (§11.2's redirect target): `ilk-menu-acma` (hooked into
  `MenuCommand`), `becerileri-inceleme` (hooked into `SkillsMenu#populate`),
  `kabiliyetleri-inceleme` (hooked into `AbilitiesMenu#populate`). Each
  completion is personal chat feedback only, not a broadcast.
- Sidebar gained a section, visible only while incomplete tasks remain,
  omitted entirely once complete - the same "omitted not blank" convention
  §9.6/§10.7 already established.

### 11.9 - Per-skill milestone boosts, per-skill commands, GUI, leaderboard reuse

- `SkillDefinition` gained `milestoneHealthPerTier` (`skills.yml`
  `kilometre-tasi-can-kademe-basina`, default per-skill: 1). At skill
  levels 25/50/75/100, `CachedSkillService#applyMilestoneEffects` applies a
  cumulative `MAX_HEALTH` boost, reusing the exact stable-`NamespacedKey`
  remove-then-reapply pattern `YamlClassService#applyPlayerEffects`
  established for class effects. **Deliberate scope decision**: all four
  skills use `MAX_HEALTH` rather than four distinct per-skill `Attribute`
  constants, since `MAX_HEALTH` is the one `Attribute` already proven to
  compile in this codebase - with no compiler or live server available
  this session (see Verification below), an unverified newer attribute
  name risked an `IllegalArgumentException` at plugin-enable time that
  would break the whole plugin. Recomputed on join/level-up, and naturally
  zeroed by §11.3's prestige skill reset (no special-case strip needed).
- New commands `/madencilik`, `/odunculuk`, `/kazicilik`, `/avcilik`
  (matching the four skill ids exactly) → `command.SkillDetailCommand` →
  new `gui.SkillDetailMenu` (current level/XP, the skill's owning class via
  a reverse `sinerji-beceri` lookup, a milestone-info button, and a
  leaderboard button that opens the **existing, unmodified**
  `LeaderboardDetailMenu(services, skillId, displayName)` - zero
  duplication). New `gui.SkillMilestoneInfoMenu`: static per-tier
  explanation of what 25/50/75/100 grant for that one skill.

### 11.10 - Sidebar visual polish

Cosmetic only: `ScoreboardSidebarService`'s `lineEntry(int index)`
generator (already variable-length since §10.7) gained plain divider
`Component`s between existing logical groups (name/Güç, prestige,
class/class-level, active-ability list, onboarding [§11.8, only while
incomplete], daily/weekly quests, footer). No service-layer changes.

### Verification (this phase)

- **`./gradlew build`/`test`/`runServer` could not be executed this
  session.** This sandbox's network policy returns 403 for both
  `services.gradle.org` (the Gradle wrapper distribution) and
  `repo.papermc.io` (the Paper API Maven repository); `/root/.ccr/README.md`
  explicitly instructs not to retry or route around an organization egress
  policy denial, only to report the blocked host - which is what happened,
  disclosed up front before implementation began. **This is a materially
  larger verification gap than any prior phase and must be closed locally
  before merging**: run the full build/test suite, then a live
  `runServer` boot with `core/run/plugins/HalilIbrahimMMO/`'s bundled YAML
  deleted first (the established pattern since §10), checking for zero
  "eksik mesaj anahtari" warnings and all 10 migrations applying cleanly.
- In lieu of compiler feedback, an extensive manual verification pass was
  done instead of the usual build/test loop:
  - **Every `MessageKey` enum constant cross-checked programmatically
    against `messages.yml` in both directions** (261 keys, exact 1:1
    match, zero missing, zero orphaned) - this caught 5 genuinely dead
    keys (`FEEDBACK_XP_BLOCK_BREAK_BOSSBAR`/`_MOB_KILL_BOSSBAR`/
    `_PLAYTIME_BOSSBAR`/`_QUEST_BOSSBAR`/`_COLLECTION_BOSSBAR`) left over
    from the per-cause global bossbar path §11.2 deleted; removed from
    both `MessageKey.java` and `messages.yml`, matching the "delete fully,
    don't leave dead members" convention.
  - A systematic unused-import scan across all 83 changed/new Java files
    found and removed 3 genuinely unused imports (`Material` in
    `YamlAbilityService`, `PlayerProfile` in `YamlPrestigeService`,
    `AbilityDefinition` in `ScoreboardSidebarService`).
  - Every rewired constructor (`GuiServices`, `ScoreboardSidebarService`,
    `SkillListener`, `YamlClassService`, `YamlAbilityService`,
    `YamlPrestigeService`, `LeaderboardRepository`, `PrestigeCommand`,
    `SidebarListener`) was traced by hand to confirm
    `HalilIbrahimMMOPlugin#onEnable()` is the only call site and its
    argument list matches the new signature; confirmed no test constructs
    any of these directly.
  - `ClassConfigTest`/`SkillMaterialConfigTest` gained assertions for the
    new `classes.yml` `prestij-etkisi` field and `skills.yml`
    `kilometre-tasi-can-kademe-basina` field; new `PrestigeConfigTest` for
    `prestige.yml`'s `gerekli-guc-seviyesi`. `SqliteMigrationContractTest`
    and `AbstractMigrationCrossEngineTest`/`IntegrationDatabaseSupport`
    updated to include V9/V10 (both were already missing V7/V8 from Phase
    9 before this session started - fixed as part of the same pass); new
    `QuestAssignmentSchemaMigrationTest`/`OnboardingSchemaMigrationTest`
    pin their versions.
- **Not verified this session, requires a live client** (compounding the
  build gap above - none of this has been exercised at all, not even
  build-verified): the prestige fanfare (broadcast/firework/sounds)
  actually firing together; clicking through the new admin all-players
  panel end-to-end including an offline target; the onboarding sidebar
  section appearing/disappearing correctly; skill milestone `MAX_HEALTH`
  boosts actually applying in real combat; the four new per-skill commands
  and their GUI chain; quest pool rotation across a real period boundary;
  the new sidebar dividers rendering as intended.

## 26. Phase 12 — bugfixes, prestige rework, XP economy, sidebar polish (Phase 12 complete)

A user-requested follow-on phase covering 12 playtesting-driven
items, built in twelve stop-and-report sub-phases (12.1-12.12), plus one
bundled bugfix the user explicitly approved (12.2's class-XP prestige
double-dip). The two load-bearing design forks: (1) the sidebar's stale-
until-`/panel`-toggle problem was fixed by adding a periodic re-render
sweep rather than auditing every XP mutation call site for a missing
`refresh(...)` call - robust against any future XP source that forgets to
call it; (2) prestige's new multi-condition eligibility (all 4 skills ≥25,
selected class at its level cap, that class's synergy skill at its level
cap) required inventing a Güç-banking mechanism, since Güç had been a pure
derived sum since Phase 11 with nothing to preserve across a skill-level
reset.

### 12.1 - Creative-mode XP/progress exploit fix

- Confirmed via repo-wide grep that no `GameMode` check existed anywhere:
  `SkillListener.onBlockBreak`/`onEntityDeath` awarded full skill and
  (transitively, via `ClassSynergyBridge`) class XP regardless of gamemode.
  Fixed by adding an `isGameplayGameMode(player)` guard (excludes
  `CREATIVE`/`SPECTATOR`) to both handlers.
- Per explicit user decision, the same gate was added to
  `QuestListener.onBlockBreak`/`onEntityDeath` so quest progress is also
  blocked in Creative/Spectator, for full consistency with skill/class XP.
  `CollectionListener` was deliberately left untouched - it triggers off
  `EntityPickupItemEvent` (picking up an item), not block-break/mob-kill,
  so it isn't the same kind of no-risk grinding exploit.

### 12.2 - XP doubling (+ bundled prestige double-dip fix)

- Every flat XP number in `skills.yml` (all 4 skills' material/entity
  rewards), `quests.yml` (11 quests), `collections.yml` (2 collections'
  per-item and completion XP), and `onboarding.OnboardingTask`'s 3 (now 4,
  see 12.7) hardcoded rewards was doubled. `classes.yml`'s
  `sinerji-xp-bonusu` (a multiplier, not a flat reward) was left alone -
  it scales automatically once its inputs double.
- **Bundled fix, user-approved, not silently folded in**: found that
  `YamlClassService#awardClassExperience` re-applied the `1.0 + 0.25*prestige`
  multiplier on top of an amount from `ClassSynergyBridge` that, for a
  synergy skill, already carried that same multiplier via
  `CachedSkillService`'s `classSynergyMultiplier` lambda - i.e. class XP
  mirrored from a synergy skill compounded to `(1+0.25p)²` instead of the
  intended `(1+0.25p)`. Fixed by splitting `awardClassExperience` into a
  private `applyClassExperience(..., adjustedAmount)` helper, with a new
  `ClassService#awardClassExperienceExact(...)` (no multiplier applied,
  since the caller's amount already carries it) used exclusively by
  `ClassSynergyBridge`; every other caller (quests, collections, onboarding)
  still goes through the original `awardClassExperience`, unaffected.

### 12.3 + 12.4 - Prestige rework + Güç banking

- **Eligibility replaced entirely**: the flat `gerekli-guc-seviyesi: 400`
  check is gone. New `prestige.yml` key `gerekli-beceri-seviyesi: 25`
  backs a 3-condition check: every one of the 4 skills ≥ that level, the
  selected class's class-level == `classes.yml`'s
  `sinif-ilerleme.maksimum-seviye` (50), and that class's synergy skill's
  level == `skills.yml`'s `maksimum-seviye` (100). No class selected is
  automatically ineligible.
- New pure-logic types: `PrestigeEligibilityInput` (record of raw
  numbers), `PrestigeEligibilityReport` (per-condition booleans +
  `allMet()`), `PrestigeEligibility` (status + report holder).
  `PrestigeRules.eligibility(...)` now takes the input record and returns
  the holder; `PrestigeResult.Status.LEVEL_REQUIRED` was renamed
  `REQUIREMENTS_NOT_MET` (the `messages.yml` key path
  `prestij.seviye-gerekli` was deliberately kept stable to avoid comment
  churn, just its text updated). `PrestigeResult` gained an
  `Optional<PrestigeEligibilityReport> report` field.
- **Güç banking**: new migration **V11**
  (`PrestigeBankedPowerSchemaMigration` → `hmmo_player_banked_power`, one
  row per player, mirroring `hmmo_player_equipped_title`'s "new table over
  ALTER TABLE" precedent), a new tiny `prestige.BankedPowerService`/
  `CachedBankedPowerService` (join-warmed cache, same shape as
  `CachedActivationService`) - deliberately **not** bolted onto
  `ProgressionService`, keeping that service's "pure derived sum" contract
  intact in its own Javadoc. `ProgressionService#cachedGlobalLevel`/
  `globalLevel` became `bankedPower(playerId) + sum(skill levels)`.
  `YamlPrestigeService#prestige` computes `cachedGlobalLevel` **before**
  resetting skills and overwrites (not adds - the pre-reset value already
  transitively includes any prior banked amount) the player's banked
  power with it. Worked/verified example: skills sum to 190 → prestige
  banks 190 → skills reset to 1 each (sum 4) → Güç reads 194 → skills
  regrind back to a 190 sum → Güç reads 380 → second prestige banks 380 →
  reset → Güç reads 384. `ProgressionListener` now warms/evicts the banked-
  power cache on join/quit alongside profile/activation.
  `LeaderboardRepository`'s Güç board query gained a
  `LEFT JOIN hmmo_player_banked_power` folded into its `total_level` sum,
  so a heavily-prestiged player's rank reflects true Güç.
- **Display rework**: `/prestij` and `PrestigeMenu` no longer show a
  single "Güç X/Y" number - both render a per-condition checklist (4
  skill lines, class-level line, synergy-skill line, or a "no class
  selected" line) via new `MessageKey`s
  (`PRESTIGE_REQUIREMENT_SKILL_MET/_UNMET`,
  `_CLASS_LEVEL_MET/_UNMET`, `_SYNERGY_MET/_UNMET`, `_NO_CLASS`,
  `_HEADER`), built as a `List<Component>` (chat: joined with
  `Component.join(JoinConfiguration.newlines(), ...)` then sent once
  through `MessageService#prefixed`; GUI: appended straight into the info
  item's lore list) rather than the "join fragments into one placeholder"
  pattern - a multi-line checklist is naturally a list of already-rendered
  lines, not one inline value. `PrestigeCommand`/`PrestigeMenu` each
  duplicate the same requirement-line-building logic (new `ClassService`
  dependency added to `PrestigeCommand`'s constructor) - same accepted
  command/GUI-pair duplication as every other cross-linked feature in this
  codebase.

### 12.5 - Ability duration/cooldown tuning

`avci-odagi` (Avcı Odağı)'s `etki-suresi-saniye`: 120 → 30. `yikici-darbe`
(Amansız Savaşçı's last-tier ability, class id `avci` - **not** `savasci`,
which is "Define Avcısı")'s `bekleme-suresi-saniye`: 100 → 30, per explicit
user-specified value.

### 12.6 - Energy regen slowdown

`abilities.yml`'s `enerji.yenilenme-saniye-basina`: 4 → 3. The regen
*amount* is the correct lever, not `PlayerEnergyService`'s tick interval
(`runAtFixedRate(..., 20L, 20L)`, left untouched) - changing the interval
would also perturb the action-bar update rhythm and isn't independently
retunable in YAML later.

### 12.7 - Onboarding 4th task (use a skill command)

New `OnboardingTask.BECERI_KOMUTU_KULLANMA` (id `beceri-komutu-kullanma`,
50 XP), hooked into `SkillDetailCommand#execute` (fires on any one of
`/madencilik`/`/odunculuk`/`/kazicilik`/`/avcilik` - "used a command", not
"used all 4"), mirroring `SkillsMenu`/`AbilitiesMenu`'s existing
`completeOnboarding` pattern exactly. `ScoreboardSidebarService
.addOnboardingLines` gained a 4th `if` branch;
`MessageKey.SIDEBAR_LINE_ONBOARDING_SKILL_COMMAND`/
`ONBOARDING_COMMAND_COMPLETED` are the new keys.

### 12.8 - Admin XP/level mutation: full feedback parity

- **Confirmed and fixed a real gap**: `AdminPlayerEditMenu`'s four skill/
  class level/XP mutation actions bypassed the entire
  `FeedbackService`/Güç-milestone-broadcast/ability-unlock-notification/
  sidebar-refresh pipeline every organic level-up already gets - a target
  player saw no bossbar, chat message, sound, or "yeni kabiliyet açıldı"
  toast, and the sidebar stayed stale until `/panel` toggled.
- `SkillService.adminSetLevel`/`adminAddExperience` and
  `ClassService.adminSetClassLevel`/`adminAddClassExperience` all widened
  their return type from `CompletableFuture<Void>` to a result carrying
  previous/new level + `leveledUp` (new `skills.SkillAdminAwardResult` for
  the skill pair; the class pair reuses the already-existing
  `ClassExperienceAwardResult`, identical shape). `AdminPlayerEditMenu`
  gained `celebrateSkillLevelUpIfNeeded`/`celebrateClassLevelUpIfNeeded`
  (private helpers, one per pair) that replicate
  `SkillListener#handleAward`'s and `ClassSynergyBridge
  #onSkillExperienceGained`'s feedback exactly - deliberately duplicated
  rather than shared, matching this codebase's established tolerance for
  small duplication across independent call sites (§16/§19). A no-op if
  the target is currently offline (feedback needs a live `Player`).
  Admin-granted XP still deliberately skips the prestige/synergy
  multiplier (unchanged, existing behavior) - only feedback was added.

### 12.9 - Differentiated per-skill milestone boosts

- `SkillDefinition.milestoneHealthPerTier` (one shared `double`, all 4
  skills used the same formula) replaced with
  `milestones: Map<Integer, SkillMilestoneEffect>` keyed by tier level
  (25/50/75/100). New `SkillMilestoneEffectType` enum (`MAX_HEALTH`,
  `PVE_DAMAGE_MULTIPLIER`, `MAX_ENERGY`, `HASTE`, `SPEED`) - deliberately
  separate from `classes.ClassEffectType`, which also carries
  ability-scoped members that don't apply to a skill milestone.
  `skills.yml`'s `kilometre-tasi-can-kademe-basina` key replaced with a
  per-skill `kilometre-taslari` map (`{25: {tur, deger}, 50: {...}, ...}`,
  all 4 tiers required).
- Shipped tiers (each skill thematically distinct, no non-health effect
  type repeated across skills): **avcılık** 25→+10% PvE damage, 50→+4 can,
  75→+10% PvE damage (cumulative +20%), 100→+4 can (cumulative +8) -
  matches the user's own 25/50 example exactly. **madencilik** 25→Acele I,
  50→+2 can, 75→Acele II, 100→+15 enerji. **odunculuk** 25→Hız I, 50→+2
  can, 75→Hız II, 100→+15 enerji. **kazıcılık** 25→+10 enerji, 50→+2 can,
  75→+10 enerji (cumulative +20), 100→+10 enerji (cumulative +30) - purely
  energy-flavored to stay distinct from the other three.
- `CachedSkillService` gained `milestoneModifiers(UUID)` (synchronous,
  sums every reached tier's bonus by type - `HASTE`/`SPEED` take the
  *highest* reached amplifier, not a sum, so Haste I + Haste II reads as
  Haste II) and rewrote `applyMilestoneEffects` to apply `MAX_HEALTH` via
  the existing stable-`NamespacedKey` attribute-modifier trick, plus
  `HASTE`/`SPEED` via a `removePotionEffect`-then-`addPotionEffect`
  idempotent pair (**accepted tradeoff**: unlike the attribute-modifier
  key trick, Bukkit potion effects carry no per-source key, so this can
  also strip a genuinely player-drunk potion of the same type - only
  possible at the discrete moments this recomputes: join/level-up/
  prestige, not continuously).
- **New plumbing required and added**: `PlayerEnergyService`'s
  `maximumEnergyBonus` wiring (in `HalilIbrahimMMOPlugin`) now adds
  `skillService.milestoneModifiers(playerId).maxEnergyBonus()` alongside
  the existing class-modifier source. `ClassListener.onDamage` (the sole
  `PVE_DAMAGE_MULTIPLIER` consumer) widened to take `SkillService` and add
  `skills.milestoneModifiers(...).pveDamageBonus()` into its damage
  multiplier expression. `MAX_HEALTH` needed no new plumbing (already had
  an independent, co-existing modifier-key pattern).
- `SkillMilestoneInfoMenu` rewrote its hardcoded health-only lore to
  branch per effect type, one new `MessageKey` per type
  (`GUI_SKILL_MILESTONE_TIER_LORE_HEALTH/_DAMAGE/_ENERGY/_HASTE/_SPEED`).

### 12.10 - Sidebar periodic auto-refresh

`ScoreboardSidebarService.enable()` previously did nothing (comment: "every
player gets their own Scoreboard on join") - `SkillListener`
(skill XP/level-up) and `ClassSynergyBridge` (class-synergy XP) never
called `refresh(...)`, so those two very-high-frequency XP sources left
the sidebar visibly stale until a `/panel` off→on forced a fresh render.
Fixed by adding a periodic re-render, mirroring `PlayerEnergyService
.enable()`'s exact `runAtFixedRate` pattern, at a **2-second** interval
(`40L, 40L` - not 1s, since `refresh()` re-renders the whole line list,
heavier per-call than energy's O(1) tick; `refresh()`'s existing
early-return for a hidden sidebar makes idle players free). Existing
explicit `refresh(...)` call sites (quest/collection progress, class/
ability selection, prestige) were left in place for their lower-latency
value on those specific events - the periodic sweep is the safety net for
everything else (including 12.8's admin mutations, which also still call
`refresh(...)` explicitly for instant feedback on top).

### 12.11 - Sidebar color scheme

Every sidebar line was gray-label/white-value, monotone (`lineEntry(int)`
is an unrelated scoreboard-entry-key generator, not a color mechanism -
confirmed before touching anything). `messages.yml`'s `sidebar:` section
value-portion colors only: Güç → gold, Prestij → light_purple, Sınıf/
sınıf-seviye → aqua, daily/weekly quest progress → yellow, active-ability
→ gold. Ability selected/unselected (already green/red) and onboarding
lines (deliberately kept muted gray) were left unchanged. No Java changes.

### 12.12 - Title-in-sidebar-name order fix

**Confirmed and fixed a real bug**: `ScoreboardSidebarService#nameLine`
built `base` = the fully-rendered `"Oyuncu: <name>"` template, then did
`tag.append(base)` - prepending the equipped title tag before the *entire*
line instead of between the label and the name (rendered as
`"[EFSANE] Oyuncu: Vesen"` instead of the intended
`"Oyuncu: [EFSANE] Vesen"`). Fixed by adding a new no-placeholder
`MessageKey.SIDEBAR_LINE_NAME_LABEL` (`sidebar.oyuncu-etiket`, just the
gray "Oyuncu: " label) and rebuilding the line in Java as
label + title-tag + name. The title tag's per-title dynamic color (from
`TitleChatStyle`) still has to be built in Java regardless - the same
documented `MessageKey`-bypass exception §10.6 already established for
per-title chat-tag coloring, now with a second instance.

### Verification (this phase)

- `./gradlew build`/`./gradlew test` are green with Java 26
  (`JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`), including a forced
  `--rerun` pass - 23 test classes, 34 individual test cases, 0 failures/
  errors. `PrestigeRulesTest` was fully rewritten (6 cases) for the new
  `PrestigeEligibilityInput`/`PrestigeEligibility` signature.
  `PrestigeConfigTest` updated for `gerekli-beceri-seviyesi`.
  `SkillMaterialConfigTest` updated to validate the new per-skill
  `kilometre-taslari` tier/type/value structure instead of the old flat
  key. `SqliteMigrationContractTest`/`AbstractMigrationCrossEngineTest`/
  `IntegrationDatabaseSupport` updated to include V11.
- **A live `./gradlew runServer` boot was performed**, with
  `core/run/plugins/HalilIbrahimMMO/`'s stale bundled YAML *and* its
  SQLite database file deleted first (this phase's new migration made a
  clean-database boot the meaningful check) so every changed resource
  regenerated and all 11 migrations - including the new V11 - applied
  against a fresh database. The plugin enabled with zero exceptions, the
  server reached `Done` in ~7.7s, and the log had zero "eksik mesaj
  anahtari" (missing message key) warnings, confirming every new/renamed
  `MessageKey` (including the prestige checklist keys, the two new
  sidebar keys, the onboarding 4th-task keys, and all 5 milestone-lore
  keys) resolves against `messages.yml`, and the new `SkillMilestoneEffectType`/
  bounded-enum YAML values in `skills.yml` all parse without error.
- **Not verified this session, requires a live client** (same honest
  caveat every feature-touching phase since §17 has carried): actually
  observing the sidebar auto-refresh within ~2s during real skill/class
  XP gain without a `/panel` toggle; the prestige checklist rendering
  correctly for a partially-eligible player and the two-prestige Güç-
  banking arithmetic (194 → 384) across a real double-prestige; Haste/
  Speed milestone potion effects surviving a relog and not clobbering a
  genuinely-drunk potion in the exact same tick; the admin panel's
  bossbar/sound/toast parity on an online target; the differentiated
  milestone boosts (PvE damage, energy cap) actually taking effect in
  real combat; the creative-mode gate across skills/class/quests
  together.

## 27. Phase 13 — icon pack, class/milestone icons, ability visuals, ore
    reveal, item-switch fix, /menu polish and credits (Phase 13 complete)

A user-requested follow-on phase covering 10 playtesting-driven items, built
as one pass rather than stop-and-report sub-phases (every item is small and
independent except the three that all land in `abilities`). The one
load-bearing design decision: "sunucuya özel ikon paketi" was implemented as
a **named Unicode glyph set exposed as a MiniMessage tag**, not as a shipped
resource pack - a resource pack would need a texture/font asset, a
server-side pack URL and a client that accepts it, none of which this plugin
controls, whereas a named glyph set is authored entirely inside
`messages.yml` and works on a vanilla client.

### 13.1 - The icon pack (`locale.IconPack` + the `<simge:...>` tag)

- New `locale.IconPack`: a 24-constant enum of `(id, glyph)` pairs (`guc`,
  `prestij`, `sinif`, `seviye`, `xp`, `kabiliyet`, `enerji`, `can`, `gorev`,
  `koleksiyon`, `gunluk`, `liderlik`, `unvan`, `kozmetik`, the four skill
  ids, `tamam`, `eksik`, `ok`, `uyari`, `bilgi`, `kutlama`).
  **Every glyph is deliberately restricted to the Unicode Basic Multilingual
  Plane**, which Minecraft's bundled Unifont fallback covers - astral-plane
  emoji render as a missing-glyph box on a vanilla client and must never be
  added. The glyph carries no colour: the surrounding `messages.yml` markup
  owns that, so the same icon is gold on the sidebar and gray in a lore line.
- `MiniMessageService` registers one shared static `TagResolver` for
  `simge` alongside the per-call placeholder resolvers, so an icon works in
  *any* template without any call site knowing about it. **An unknown icon
  name resolves to `null`, not an exception** - MiniMessage then leaves the
  tag as literal text, the same way it already treats any other misspelled
  tag, so one typo can never take a gameplay message down mid-render.
- Icons were woven into the prefix, the whole `sidebar:` section, both XP
  bossbars, the `gui.ana`/`gui.krediler`/`gui.kilometre-tasi` items, and the
  chat lines for level-ups, quest/collection completion, prestige, titles
  and cosmetics. Every previously-hardcoded checkmark/cross literal in
  `messages.yml` (prestige checklist, collection entries, titles,
  cosmetics, sidebar ability list) now goes through `<simge:tamam>` /
  `<simge:eksik>`, which is the whole point of a *pack* - changing what
  "done" looks like is one line in `IconPack`.

### 13.2 - Per-class icons

`PlayerClassDefinition` gained a `Material icon` field parsed from a new
`classes.yml` `simge` key (`Material.matchMaterial`, which is safe
standalone unlike `Material#isBlock()`, §16). Assigned: Define
Avcısı=`GOLDEN_SHOVEL`, Amansız Savaşçı=`DIAMOND_SWORD`, Mağara
Adamı=`DIAMOND_PICKAXE`, Orman Bekçisi=`IRON_AXE`. `ClassIntroMenu`,
`ClassMenu` and `AdminClassPickerMenu` all rendered a hardcoded
`DIAMOND_SWORD` before and now read `definition.icon()`.

### 13.3 - Skill and class level on the XP bossbars

`geribildirim.xp-beceri-bossbar`/`xp-sinif-bossbar` gained a `<seviye>`
placeholder; `SkillListener#handleAward` and
`ClassSynergyBridge#onSkillExperienceGained` pass the just-updated level from
the `SkillProgress`/`ClassLevelProgress` they already hold. No signature
changes anywhere.

### 13.4 - Abilities no longer break when the held item changes

- **Confirmed and fixed the reported bug.** `AbilityActivationListener` had
  an `onHeldItemChange` and an `onDrop` handler that called
  `clearActivationFeedback(...)`, which cancels the action-bar
  countdown/energy readout, strips the glint and plays the
  `kabiliyet-bitis` (`ENTITY_ITEM_BREAK`) cue. Switching hotbar slots
  mid-ability therefore *looked and sounded exactly like the ability had
  broken* even though the underlying armed/buff state was untouched and
  still expired or triggered on schedule. **Both handlers were deleted
  outright**: a tool is only required to *start* an ability, and once
  running it now stays running for its full duration regardless of what the
  player holds. `onInteract`/`onQuit` are all that remain.
- **A second, latent bug fixed in the same pass:** `applyGlint` operated on
  `getItemInMainHand()`, so if a player *did* switch items the glint
  override was cleared from the wrong (new) item and the real ability tool
  stayed glowing forever. Glint is now applied to, and cleared from, a
  remembered hotbar slot (`ConcurrentHashMap<UUID, Integer> glintedSlots`);
  `startActivationFeedback` also clears any previous glint first, so
  activating a second ability cannot strand the first one's override.

### 13.5 - Skill milestone tiers reordered weak to strong

Every skill's 25/50/75/100 tiers were retuned so 25 is a simple entry bonus
and the skill's signature effect is always held back to 75/100 (the
previous layout put e.g. avcılık's +10% PvE damage at 25 and a flat health
bump at 100). Shipped: **kazıcılık** 25 = +2 can, 50 = +10 enerji, 75 = +20
enerji, 100 = +30 enerji. **odunculuk** 25 = +2 can, 50 = +10 enerji,
75 = Hız I, 100 = Hız II. **madencilik** 25 = +2 can, 50 = +10 enerji,
75 = Acele I, 100 = Acele II. **avcılık** 25 = +2 can, 50 = +4 can,
75 = +%10 PvE hasar, 100 = +%20 PvE hasar.
No Java change - `SkillMilestoneEffectType` and the cumulative/highest-
amplifier summing in `CachedSkillService#milestoneModifiers` (§12.9) already
covered every combination used. `SkillMilestoneInfoMenu` additionally stopped
rendering every tier as a `GOLDEN_APPLE`: it now switches on the effect type
(health = golden apple, PvE damage = diamond sword, energy = glowstone dust,
haste = golden pickaxe, speed = sugar).

### 13.6 - Mining's damage-absorb ability replaced with an ore reveal

- `kaya-zirhi` (a second, duplicate `BRIEF_INVULNERABILITY` that only
  differed from Define Refleksi in flavour) is gone, replaced by
  **`maden-feneri` ("Maden Feneri")** and a new
  `AbilityEffectType.ORE_REVEAL`. No `classes.yml` talent referenced
  `kaya-zirhi`, so its removal needed no retarget (unlike §9.3's
  `kazici-tasarrufu`).
- **A `BlockDisplay` clone of each ore, spawned with `setGlowing(true)`, is
  the mechanism** - blocks themselves cannot glow, the glow flag exists only
  on entities. Each display is scaled to 1.02 with a -0.01 translation
  (`org.bukkit.util.Transformation` + `org.joml`, both already on the
  runtime classpath - confirmed in this session's `runServer` log resolving
  `joml-1.10.8.jar`) so the outline surrounds the real block instead of
  z-fighting with it, gets `Display.Brightness(15, 15)` so it reads clearly
  in a dark cave, is tinted with the ability's own `AbilityColor` via
  `setGlowColorOverride`, is `setPersistent(false)`, and removes itself
  through **its own `Entity#getScheduler().runDelayed(...)`** (Folia-correct:
  an entity is only safely touched from its own region) after the ability's
  configured duration.
- **Which blocks count is not a second hardcoded ore list** - it is the
  ability's own skill's configured material set
  (`skills.settings().definitions().get(definition.skillId()).materials()`),
  so `skills.yml` stays the single source of truth for what an ore is. A
  hard `MAX_REVEALED_ORES = 120` cap bounds the entity count independently
  of the configured radius (12).
- **The "only in a wide mine" gate** (`isInsideWideMine`) is two cheap
  checks, both run *before* any energy is spent or cooldown written: the
  player must be genuinely underground (`getHighestBlockYAt(location) >
  blockY + 2`, so an open field never qualifies) and standing in real open
  space - the fraction of air in a 9x9x9 probe cube must meet the ability's
  configured `etki.acik-alan-orani` (0.30), which a one-block strip-mining
  tunnel cannot reach. Rejection sends the new
  `MessageKey.ABILITY_REQUIRES_MINE`.
- `AbilityDefinition`'s existing dual-purposing convention was extended
  rather than growing the record: `limit` doubles as ORE_REVEAL's radius
  (like `KNOCKBACK_SLAM`'s) and `damageMultiplier` doubles as its open-air
  ratio (like `EXTRA_DROP_CHANCE`'s chance).

### 13.7 - Per-ability colour, sound and particles

- New bounded `abilities.AbilityColor` (10 constants, each carrying *both* a
  `NamedTextColor` for text and a Bukkit `Color` for particle/glow tint) and
  `abilities.AbilityVisual` (`color`, `Optional<Sound>` + volume/pitch,
  `Optional<Particle>` + count), parsed from a new per-ability `gorsel`
  section. Same bounded-enum-from-YAML pattern as `ClassBarColor`/
  `TitleChatStyle`.
- `gorsel` is **optional**: an ability without one falls back to the single
  shared `kabiliyet-baslangic` cue in `feedback.yml` and spawns nothing,
  i.e. exactly the pre-Phase-13 behaviour, so `feedback.yml` stays
  meaningful and an admin-authored ability still loads. All 12 shipped
  abilities declare one, each with a distinct colour/sound/particle triple.
- `YamlAbilityService#playActivationVisual` replaced the single
  `feedback.celebrateAbilityActivated(player)` call at the end of
  `activateOnPlayerRegion`. `Particle.DUST` is special-cased in
  `spawnBurst` because it is the only particle whose colour is
  caller-controlled (`Particle.DustOptions`).
- The colour also tints the ability's name in `AbilitiesMenu`, built
  directly as a `Component.text(displayName, color)` - **the third instance
  of the documented "`MessageKey` cannot vary a placeholder's own colour"
  exception** (§10.6 title chat tags, §12.12 sidebar name line).
  `MessageKey.GUI_ABILITY_ITEM`/`gui.kabiliyetler.oge` was deleted as
  now-genuinely-dead, matching the same delete-fully convention §10.6 used
  for `TITLE_CHAT_PREFIX`. The sidebar's ability list was deliberately left
  green/red - that colour encodes *selection state*, which is more useful
  there than identity.

### 13.8 - Yıkıcı Darbe reworked into a real shockwave

`knockbackSlam` now plays `ENTITY_GENERIC_EXPLODE`, spawns an
`EXPLOSION_EMITTER` plus a ring of `EXPLOSION` particles at the caster and
one more on each thrown target, and throws targets at 2.6 horizontal / 0.85
vertical velocity (was 1.4 / 0.4). `hasar-carpani` went 2.0 to 9.0 (for this
effect it is raw damage, not a multiplier - now called out in the YAML
comment). Its `gorsel` deliberately adds `BLOCK_ANVIL_LAND` + `LARGE_SMOKE`
*on top of* the built-in explosion rather than duplicating it, giving the
"çatlama + patlama" the request asked for.

### 13.9 - `/menu` polish and the credits screen

- `AnaMenu` went from 27 to 45 slots: a black-glass outer frame around a
  gray-glass interior, a real `PLAYER_HEAD` header wearing the viewer's own
  skin (new `GuiItems.playerHead(OfflinePlayer, ...)` using `SkullMeta` -
  no menu in this codebase set a skull owner before) showing Güç/prestij/
  sınıf, the four category icons each with a "what is in here" lore line,
  and a credits button. The header reads `ProfileService`'s already-warmed
  synchronous cache rather than adding an async load, keeping this a
  single-pass render like every other hub menu.
- New `gui.CreditsMenu`, reachable from **both** `AnaMenu` and `AdminMenu` -
  a single `boolean fromAdmin` constructor flag decides which one its back
  button returns to, which is all that is needed given views are rebuilt per
  open and there is no navigation stack (§17). Lists the plugin + version,
  **Furkan Okan (`fxrkqnplus`)** as server owner/design, **Claude
  (Anthropic)** as code-development partner, and a thank-you item.

### Verification (this phase)

- `./gradlew build` / `./gradlew test` / `:core:compileIntegrationTestJava`
  are all green with Java 26 (`JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`) -
  40 test cases across 25 test classes, 0 failures. No migration was added this phase (nothing new is
  persisted), so no migration-version test changed.
- **Two new tests, both closing gaps previous phases had to check by hand:**
  `locale.MessageCatalogTest` asserts `MessageKey` and `messages.yml` are an
  exact 1:1 set match in both directions (the manual pass §11 had to do, and
  which found 5 dead keys that time) *and* that every `<simge:...>`
  reference names a real `IconPack` entry; `abilities.AbilityConfigTest`
  validates every bundled ability's class reference, tool tag, effect type,
  ORE_REVEAL radius/ratio and full `gorsel` block.
- **New platform fact, empirically confirmed this session (extends §16's
  `Material#isBlock()` finding):** on Paper 26.2 **`org.bukkit.Sound` is
  registry-backed, so merely referencing `Sound.valueOf(...)` from a plain
  JUnit test throws `ExceptionInInitializerError` caused by
  `IllegalStateException: No RegistryAccess implementation found`** - the
  class initialiser itself fails, not the lookup. `abilities.AbilityToolTag`
  fails the same way (its constants wrap `Tag<Material>` constants).
  **The fix used, and the pattern to prefer over a hand-maintained
  allow-list:** read the constant *names* reflectively without initialising
  the class -
  `Class.forName("org.bukkit.Sound", false, loader).getDeclaredFields()`
  filtered to static fields of the class's own type. This yields the exact,
  always-current valid name set for both `Sound` and `Particle` with no
  registry and nothing to maintain, and is strictly better than
  `CosmeticConfigTest`/`QuestConfigTest`'s existing hardcoded allow-lists
  (which were not migrated in this phase - they still work, they are just
  now known to have a better option available).
- **A live `./gradlew runServer` boot was performed**, with
  `core/run/plugins/HalilIbrahimMMO/`'s stale `abilities.yml`/`classes.yml`/
  `messages.yml`/`skills.yml` deleted first so all four regenerated from the
  updated bundled resources (the established pattern since §10). The plugin
  enabled with zero exceptions, the server reached `Done` in ~8.0s, and the
  log had zero "eksik mesaj anahtari" warnings - confirming the new
  `simge` tag resolver, the `simge:` class-icon key, all 12 `gorsel`
  sections' `Sound`/`Particle`/`AbilityColor` names, the new `ORE_REVEAL`
  effect type and every new `MessageKey` parse correctly on a real server.
  The log also confirms `joml-1.10.8.jar` is resolved into the server's
  runtime classpath, which is what `Transformation` needs.
- **Not verified this session, requires a live client** (the same honest
  caveat every feature-touching phase since §17 has carried): whether the
  chosen Unicode glyphs all render on a vanilla client rather than as
  missing-glyph boxes (BMP-only was the guard, but only a real client
  proves it); the `BlockDisplay` ore outlines actually appearing through
  walls, at the right size, in the ability's colour, and cleanly
  disappearing after 30s; the wide-mine gate accepting a real cave and
  rejecting a 1x2 tunnel; activating an ability and then switching/dropping
  the held item and seeing the countdown and glint survive; the 12
  per-ability sound/particle pairs actually feeling distinct; Yıkıcı
  Darbe's new knockback distance and 9 damage being fun rather than
  overtuned; the redesigned `/menu` and the credits screen rendering as
  intended at 45 slots.

## 28. Phase 14 — natural-block XP gate, death-proof boosts, duration
    formatting, class-change flow, quest board, XP economy rebalance,
    GUI-only command surface (Phase 14 complete)

A user-requested follow-on phase covering 15 playtesting-driven items, built
as one pass. Two load-bearing decisions were made before implementation
started: (1) "only world-generated blocks award anything" needed a
*persistent* record of player placements, which is stored per-chunk rather
than in this plugin's own database; (2) every visible duration had to become
"2 dakika 10 saniye" instead of "130 saniye" without ever writing a Turkish
unit word in Java.

**Phase 13 was merged into `main` at the start of this phase** - it had been
sitting unmerged on `phase-13-icon-pack-ability-visuals` while `main` still
pointed at Phase 12, and several Phase 14 items (Maden Feneri, per-ability
icons) build directly on it. Confirmed with the user before merging.

### 14.1 - Player-placed blocks award nothing and trigger nothing

- New `exploit.PlacedBlockTracker` (a `Listener`, not a `PluginService` -
  it owns no lifecycle, mirroring `BlockBreakRateLimiter`'s plain-class
  shape). `BlockPlaceEvent` records a position, `BlockBreakEvent` forgets it,
  both at `EventPriority.MONITOR` with `ignoreCancelled` so a cancelled
  interaction is never recorded and a forget only happens after every
  listener that queries it has already run (they are all at NORMAL).
- **Storage is the chunk's own `PersistentDataContainer`, not a new table.**
  Positions are packed into one `int` each
  (`((y + 4096) << 8) | ((z & 0xF) << 4) | (x & 0xF)`) and written as an
  `INTEGER_ARRAY`. This means the server persists them with the chunk, they
  survive restarts, and they are discarded automatically if the chunk is ever
  deleted or regenerated - none of which a `hmmo_*` table would give, and all
  of which matter for data that is only meaningful next to the blocks it
  describes. Writes go straight through (crash-safe); a
  `ConcurrentHashMap<ChunkKey, Set<Integer>>` exists purely so the *read*
  path (three listeners query every break) does not deserialize the same
  array repeatedly, and is evicted on `ChunkUnloadEvent`.
- Wired into `SkillListener#onBlockBreak`, `QuestListener#onBlockBreak`,
  `YamlAbilityService#handleBlockBreak` (top of the method, so no ability
  arms or fires off a placed block) and `breakSecondaryBlocks` (so an
  excavate/vein-mine/tree-fell sweep skips placed blocks too).
  `CollectionListener` is deliberately untouched, same reasoning §12.1 gave:
  it triggers off `EntityPickupItemEvent`, not a break.
- **Known, documented limitation:** blocks placed *before* this phase shipped
  are not in any chunk's container, so pre-existing player builds still count
  as natural. There is no way to reconstruct that history.

### 14.2 - Skill milestone and class boosts survive death

- Confirmed the report: death clears the HASTE/SPEED milestone potion effects
  outright and the MAX_HEALTH attribute modifiers do not survive the respawn
  either, so every 25/50/75/100 boost and every class health bonus was lost
  until the player relogged (join is where `applyMilestoneEffects`/
  `applyPlayerEffects` were called).
- Fixed with a `PlayerRespawnEvent` handler in **both** `SkillListener` and
  `ClassListener` (each keeping its own service, matching this codebase's
  self-contained-listener convention), each reapplying **one tick after** the
  event via `player.getScheduler().runDelayed(..., 1L)` - the server finishes
  resetting the player's attributes and effects *after* the event returns, so
  anything applied inside it is wiped again immediately. `ClassListener` also
  reconciles energy in the same pass so a max-energy bonus is not left
  clamped.

### 14.3 - Maden Feneri: 36 block radius, 7.5 minute cooldown, live cleanup

- `abilities.yml`: `yaricap` 12 to 36, `bekleme-suresi-saniye` 160 to 450.
  `MAX_REVEALED_ORES` 120 to 200 so a genuinely large cave still lights up.
- **`revealOres` was restructured column-major** (`x`/`z` outer, `y` inner)
  with a `world.isChunkLoaded(x >> 4, z >> 4)` guard per column and a
  min/max-height clamp. At radius 12 the old triple loop was harmless; at
  radius 36 an unguarded `getBlockAt` would synchronously generate dozens of
  chunks the player cannot even see.
- **Broken ores now stop glowing immediately.** New
  `ConcurrentHashMap<RevealedBlock, BlockDisplay> revealedOres` registry;
  `clearRevealedOre(block)` is called from the **very top** of
  `handleBlockBreak`, before the secondary-break and player-placed guards,
  because a vein-mined block is broken for real too and must stop glowing the
  same way. A re-activation that re-lights the same block drops the previous
  display rather than orphaning it; `disable()` removes every remaining one
  directly (no new task may be scheduled during shutdown, and this project
  targets Paper, where disable runs on the owning thread).

### 14.4 - Every visible duration is now hours/minutes/seconds

- New `locale.DurationFormatter` (static utility): `ofMillis`/`ofSeconds`
  produce e.g. "1 gün 2 saat 3 dakika 4 saniye", omitting zero units, largest
  first, never returning less than "1 saniye" (every call site is a
  "you must wait"/"this much is left" message where zero reads as a bug).
- **The unit words are not written in Java.** Four new `MessageKey`s
  (`sure.gun`/`saat`/`dakika`/`saniye`) plus a new
  `MessageService#plain(MessageKey, Map)` that renders a template and strips
  its style via `PlainTextComponentSerializer`. That keeps the class inside
  the "no Turkish literal in Java" rule while still producing the plain
  `String` the surrounding templates need (they substitute it as an unparsed
  placeholder, not a nested component). `plain(...)` is documented as
  existing *only* for this composition case - never for sending.
- Call sites converted: ability cooldown, ability selection cooldown, all
  five "ability is now active" messages, the action-bar countdown, the
  sidebar's active-ability line, the class-change cooldown, and
  `AbilitiesMenu`'s cooldown lore. The corresponding `messages.yml` templates
  lost their hardcoded " saniye"/"sn" suffixes.
- New `DurationFormatterTest` (6 cases) with a stub `MessageService` -
  pure-logic, same convention as `PrestigeRulesTest`/`UnlockConditionsTest`.

### 14.5 + 14.8 - Energy economy and readout

`abilities.yml`: `avci-odagi`'s `enerji-vurus-basi` 8 to 16 (the only
mob-kill energy cost in the game) and `enerji.yenilenme-saniye-basina` 3 to 6
(halving the time to refill). `EnergyService` gained a public
`maximumEnergy(Player)` (previously private on the implementation), because
both action-bar countdown variants now render `Enerji <mevcut>/<maksimum>`
alongside the remaining time - while an ability runs, the plain energy status
line is suppressed for its whole duration, so this is the only place energy
can be read.

### 14.6 - Abilities activate on air and on mobs, not just on blocks

- **Root cause found:** `AbilityActivationListener#onInteract` was
  `@EventHandler(ignoreCancelled = true)`. A right click on empty air arrives
  with `useInteractedBlock` already denied (there is no block), which makes
  `PlayerInteractEvent#isCancelled()` report `true` - so the entire
  air-click path was invisible and an attack ability could not be armed
  without first finding a wall. `ignoreCancelled` removed.
- Right-clicking a creature raises `PlayerInteractEntityEvent`, not
  `PlayerInteractEvent`, so a second handler was added. Both funnel into one
  `tryActivate(Player)` guarded by a 200 ms per-player debounce, since a
  single click can raise both events and the duplicate would otherwise be
  rejected with a cooldown message the player never triggered.

### 14.7 - Talent tooltips

`ClassMenu`'s talent items gained two lore lines above the existing rank
line: what one rank buys (branched per `ClassEffectType`, six new
`MessageKey`s, percentages converted from the stored 0-1 ratio) and the total
already accumulated (two more keys, one plain and one percentage-shaped).
Mirrors `SkillMilestoneInfoMenu`'s per-effect-type lore exactly.

### 14.9 - Class change reuses the first-pick screen

- **`ClassMenu` no longer selects a class at all.** Clicking any class icon
  opens `ClassIntroMenu` - the same screen a brand-new player sees after
  `/mmo`, with every class's blurb and ability list - which then routes
  through `ClassConfirmMenu`. `ClassIntroMenu` gained a `fromMenu` flag (the
  same single-boolean framing switch `CreditsMenu` already uses for its back
  button) adding a back button, a `CLOCK` status item showing the current
  class and whether the change cooldown has expired, and a "current class"
  marker plus glint on the equipped one.
- `ClassConfirmMenu` now loads class progress alongside the profile and
  spells out both irreversible facts before the button is clickable: the next
  change lockout, and that the class being left has its class level wiped.
  A player still inside the cooldown sees a `BARRIER` with the remaining wait
  instead of a confirm button (a `volatile boolean confirmable` guards the
  click, defaulting to `false` so a click arriving before the async render
  does nothing).
- On a confirmed change the **previous** class's level is reset via the
  existing `ClassService#resetClassLevel`, deliberately from the menu rather
  than inside `ClassService#select`, so the reset only happens on the
  explicitly-confirmed player path and never on an admin force-set.
- New `FeedbackService#celebrateClassChanged(Player, String)` fires the
  on-screen title, bossbar, personal chat line and a new
  `sinif-degistirildi` sound cue together. It is the first `celebrate*`
  method that owns its own chat message (the three surfaces are meant to
  carry identical wording); the first-ever pick still uses the older
  `celebrateClassSelected` + `CLASS_SELECTED` pair.

### 14.10 - Quest board rebuilt

- `QuestDefinition` gained `description` and `icon`, parsed from new required
  `aciklama`/`simge` keys on every quest. `quests.yml` was rewritten with a
  one-line description and a thematic icon per quest.
- `QuestsMenu` went from a flat 27-slot list to a 54-slot board with four
  labelled rows: Günlük / Haftalık / Kalıcı (each showing only that type's
  **currently assigned, not yet finished** quests) and a dedicated
  Tamamlanan row. A finished quest leaves its own section rather than being
  listed twice, which keeps the active rows an honest to-do list. Every entry
  shows its description, progress and XP reward on hover; completed ones use
  a green name, a "Tamamlandı" line and a glint.

### 14.11 - Sidebar quest lines

The weekly summary count line was replaced by one line per assigned weekly
quest, matching what daily already did (`SIDEBAR_LINE_QUESTS_WEEKLY` and
`questCounts(...)` deleted as now-dead). A completed quest of either type
switches to a new green `sidebar.tamamlanan-gorev-satiri` template carrying
"Tamamlandı" and the XP it paid out, instead of sitting at a maxed-out
fraction indistinguishable from an unclaimed one.

### 14.12 - Per-ability icons and selection feedback

`AbilityDefinition` gained `icon`, parsed from a new required `simge` key;
`AbilitiesMenu` renders it instead of the shared `BLAZE_POWDER`. All 12
abilities got a thematic material. The selected ability already glinted and
already sent a chat line; a new `kabiliyet-secildi` sound cue
(`FeedbackService#celebrateAbilitySelected`) completes the set.

### 14.13 + 14.14 - XP economy rebalanced by value, then tripled

- **The reported problem was real:** a passive cow and a zombie both paid the
  same amount (the MOB skill's `varsayilan` was 10 and `ZOMBIE` was 10), and
  redstone and gold ore were both 12.
- `skills.yml` was fully re-tiered by in-game value *first*, then every value
  (plus every quest, collection and onboarding reward) was multiplied by 3 as
  a separate step. Final spreads: kazıcılık 6-18, odunculuk 9-21, madencilik
  12 (coal) to 300 (ancient debris) with redstone deliberately far below gold
  (21 vs 60), avcılık 15 (unlisted/passive) to 2400 (Ender Dragon), with ~40
  explicitly tiered hostiles in between. The unlisted-mob default is the
  floor precisely because unlisted mobs are almost always farmable animals.
- The level curve was **not** touched, so levelling is genuinely ~3x faster.

### 14.15 - The command surface is GUI-only

- Nine commands were rewritten from chat-rendering implementations down to
  the same four-line "open a menu" shape `ClassCommand`/`MenuCommand` already
  had: `/profil`, `/gorev`, `/koleksiyon`, `/gunluk`, `/liderlik`, `/unvan`,
  `/kozmetik`, `/prestij`, `/kabiliyet`. Every argument form is gone,
  including `/kabiliyet <id>` direct activation (abilities are menu-select
  plus right-click only now), `/prestij onayla`, `/unvan sec <id>`,
  `/kozmetik sec <id>` and `/koleksiyon <id>`.
- `registerCommands(...)` collapsed from 18 parameters to 4.
- **20 now-dead `MessageKey`s were deleted from both `MessageKey.java` and
  `messages.yml`** (the whole `liderlik:` chat section, the title/cosmetic/
  quest/collection list templates, `profil.*`, `prestij.durum`, `gunluk.*`,
  `mmo.kullanim`), matching the delete-fully convention §10.6/§11 set. Three
  YAML sections became empty and were removed outright.
- **`/mmo` is the one deliberate exception** and is called out in its own
  Javadoc: it still accepts the historical `aktif` argument, but the bare
  form now does the same thing, so there is no failure path. It is not a
  feature screen (there is nothing to look at before activation) and it opens
  the class GUI immediately on success.

### Verification (this phase)

- `./gradlew build` / `./gradlew test` are green with Java 26
  (`JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`) - 26 test classes, 45 test
  cases, 0 failures; `:core:compileIntegrationTestJava` is green too. No
  migration was added this phase (chunk PDC is not a
  plugin table), so no migration-version test changed.
  `AbilityConfigTest`/`QuestConfigTest` gained assertions for the new
  required `simge` (and, for quests, `aciklama`) fields; new
  `DurationFormatterTest` covers the unit splitting.
- A repo-wide unused-import scan was run over `core/src` afterwards and found
  two (one newly stale in `ScoreboardSidebarService`, one pre-existing in
  `SkillRepository`); both removed. A programmatic `MessageKey`-vs-usage
  sweep confirmed zero orphaned constants remain, and `MessageCatalogTest`
  independently enforces the 1:1 match with `messages.yml`.
- **A live `./gradlew runServer` boot was performed**, with
  `core/run/plugins/HalilIbrahimMMO/`'s bundled YAML *and* its SQLite
  database deleted first so every changed resource regenerated and all 11
  migrations applied against a fresh database. The plugin enabled with zero
  exceptions, the server reached `Done` in ~6.5s, and the log had zero
  "eksik mesaj anahtari" warnings - confirming every new `MessageKey`
  resolves, the new `simge` keys in `abilities.yml`/`quests.yml` parse as
  real materials, and the new `sinif-degistirildi`/`kabiliyet-secildi` sound
  cues resolve against real `Sound` constants.
- **Not verified this session, requires a live client** (the same honest
  caveat every feature-touching phase since §17 has carried): placing a block
  and confirming it awards nothing when re-broken, including across a
  server restart and a chunk unload/reload cycle; dying and confirming the
  milestone/class boosts are back after respawn; Maden Feneri at radius 36
  (both the frame cost of the scan and the 200-entity outline density) and an
  outline vanishing the instant its ore is mined; right-click activation on
  air and on a mob without a double-activation message; the class-change
  confirm/reset flow end to end including the cooldown-locked state; the
  rebuilt quest board and the green completed sidebar lines; whether the
  tripled XP economy actually feels right in play.

## 29. Phase 15 — hayvancılık/tarımcılık skills, Tabiat Vârisi, two-pool
    talent trees, Turkish commands (Phase 15 complete)

A user-requested follow-on phase covering eight items. Four load-bearing
design decisions were made before implementation started, each turning a
one-off request into a reusable mechanism rather than a special case:

1. **Two new skills needed a third kind of XP source.** Breeding, taming,
   shearing, trading and tilling are none of them a block break or a mob
   kill. Rather than bolting bespoke hooks onto `CachedSkillService`, the
   skill model gained a bounded `SkillAction` enum and a per-skill
   `eylemler` YAML section, orthogonal to the existing `kaynak.tur`.
2. **The two level-25 unlocks (items 5 and 6) became milestone effect
   types**, not per-skill bespoke config blocks - so "what does level 25
   give me" stays one uniform question across all six skills.
3. **A class may now power several skills.** `sinerji-beceri` (one id)
   became `sinerji-beceriler` (a list), because Tabiat Vârisi replaces two
   classes and covers four skills.
4. **Talent point categories are derived from `ClassEffectType`, not
   declared.** An ability-scoped effect already requires an ability id and a
   stat effect already forbids one, so the two are structurally distinct
   already; a separate `kategori` key could only ever disagree with reality.

### 15.1 - Hayvancılık and tarımcılık

- New `skills.SkillAction` (12 bounded constants, `id()`-keyed like
  `IconPack`), `skills.SkillActionReward` (xp + display name + an optional
  `Set<Material>` gate) and `SkillSource.ACTION`. `SkillDefinition` gained
  `actionRewards`, **orthogonal to `source`**: a `BLOCK` or `MOB` skill may
  also declare actions (tarımcılık does both), while an `ACTION` skill has
  only actions and therefore can never collide with the block/mob
  resolution path (hayvancılık). `CachedSkillService#parseActions` threads
  an `actionOwners` map through the whole file so a second skill claiming
  an action already taken fails at load time instead of silently losing its
  rewards to whichever skill iterated first.
- `SkillService#awardAction(playerId, playerName, action, context)` mirrors
  `awardBlock`'s shape; `context` is the causing material for gated actions
  and `null` otherwise. Both it and the pre-existing block/mob paths now
  funnel into one new private `award(...)` in `CachedSkillService` - the
  multiplier, serial chain, persistence and synergy-bridge notification
  existed only inside `awardMatching` before this phase.
- **`skills.SkillAwardHandler` is extracted, not copied a third time.** The
  post-award feedback block (XP bossbar, skill level-up celebration,
  milestone reapplication, Güç bossbar/broadcast) was a private method on
  `SkillListener` while that was the only awarding listener; three copies is
  past where this codebase's usual duplication tolerance pays off.
  `isGameplayGameMode` moved onto it as a static.
- New `skills.HusbandryListener`: `EntityBreedEvent`, `EntityTameEvent`,
  `PlayerShearEntityEvent` (SHEEP only), `PlayerInteractEntityEvent` with a
  bucket on COW/MOOSHROOM/GOAT (milking raises no bucket-fill event of its
  own), `CraftItemEvent` (cake), `FurnaceExtractEvent` (cooked meat), plus
  the lead-carrying tick. **The lead tick is why this listener owns a
  `ScheduledTask`** - there is no event for "is still leading livestock" -
  started/stopped explicitly by `HalilIbrahimMMOPlugin`, mirroring
  `ProgressionListener#close()`'s existing discipline rather than turning a
  listener into a `PluginService` for one timer. It awards **once per tick
  regardless of herd size**: leading ten cows is the same job as leading
  one, and paying per animal would only reward stacking.
- New `skills.FarmingListener`: `PlayerTradeEvent` (Paper's own),
  `BlockFertilizeEvent` gated on `Ageable` (bone meal also works on grass
  and saplings, which are not farming), `CraftItemEvent` (bread),
  `FurnaceExtractEvent` (baked potato / dried kelp), and hoe tilling.
  **There is no "soil was tilled" event**, so the till handler re-checks one
  tick later that the block actually became `FARMLAND` - a right click that
  merely looked like tilling (protected block, hoe against a wall) pays out
  nothing.
- Both furnace/craft handlers coexist by material gate alone: a
  `COOKED_BEEF` extraction is rejected by tarımcılık's `urun-pisirme`
  material set and accepted by hayvancılık's `et-pisirme`, and vice versa.
- **The one exception to §14.1's player-placed rule.** `SkillListener`'s
  gate became `isEligibleTerrain(block)`: an `Ageable` block is exempt from
  the placed-block check (farming *is* planting and harvesting your own
  crops) but only counts **fully grown**, which is what stops
  break-and-replant farming. Hay bales are not `Ageable`, so they stay
  gated and only the world's own village bales count - exactly as
  requested. `QuestListener`/`YamlAbilityService` were deliberately left on
  the plain placed-block check; no shipped quest or ability targets a crop.

### 15.2 - The two level-25 unlocks as milestone effect types

- `SkillMilestoneEffectType` gained `TREE_FELLING` (value = maximum logs)
  and `BREED_REFUND` (value = 0-1 chance); `SkillMilestoneModifiers` gained
  `treeFellingLimit`/`breedRefundChance`. Unlike the five stat boosts these
  are **never applied to the player** - nothing in `applyMilestoneEffects`
  touches them, the odunculuk block-break path and the hayvancılık breeding
  path read them at the moment they fire. Highest reached tier wins for
  both (two tiers of one capability, not two stacking bonuses).
- **`AbilityService#fellTree(event, limit)` is new and deliberately lives in
  `abilities`**, not `skills`: the secondary-break bookkeeping it needs
  (re-firing a `BlockBreakEvent` per extra block for protection-plugin
  compatibility, marking each so it earns no second round of XP or quest
  progress) is that service's machinery, already used by `agac-kirici`.
  `skills` owns the decision of *when* a tree should fall.
- **Gated on the root log** (`Tag.LOGS` matched on the broken block, and no
  log directly beneath it): felling from mid-trunk would make every single
  log break cascade. Combined with the existing placed-block gate, a
  player-built log tower is inert. Shipped limit is 48 logs.
- Breed refund: rolled *after* the baby is already born, refunding
  `amount = 2` of `EntityBreedEvent#getBredWith()` - both parents' food
  together, matching how it was spent and the user's own worked example.
  **Platform note:** the accessor is `getBredWith()`, not `getBreedItem()`.

### 15.3 - Tabiat Vârisi replaces Define Avcısı and Orman Bekçisi

- `savasci`/`oduncu` are gone; `tabiat-varisi` ("Tabiat Vârisi") is new and
  powers **odunculuk + kazıcılık + tarımcılık + hayvancılık**. The other two
  are unchanged (`avci`/Amansız Savaşçı → avcılık, `madenci`/Mağara Adamı →
  madencilik).
- `PlayerClassDefinition.synergySkillId` → `Set<String> synergySkillIds`
  plus a `hasSynergyWith(skillId)` helper. Six call sites: the bootstrap
  synergy multiplier, `ClassSynergyBridge`, `YamlPrestigeService`,
  `PrestigeMenu`, `SkillDetailMenu`, and the parser.
- **Prestige eligibility takes the *minimum* level across a class's synergy
  skills**, so `PrestigeEligibilityInput`/`PrestigeRules` needed no change
  at all and "synergy skill at the cap" keeps meaning *all* of them are.
  `PrestigeMenu` renders **one checklist line per synergy skill** rather
  than one combined line - "your synergy skill is at 78" would not say
  which of four is holding the player back.
- **The 6 abilities of the two removed classes moved to Tabiat Vârisi and 2
  new ones were added, giving it 8 against the other classes' 3.** This is
  deliberate and is the class's whole identity: it covers four skills, so
  each of its abilities is individually more utility-shaped than decisive,
  and they unlock across the full class-level range (5/10/15/20/25/30/35/40)
  where avci/madenci unlock at 5/10/30 and 5/20/35. Two were renamed to fit
  (`hazine-sezgisi` → `toprak-sezgisi` "Toprak Sezgisi",
  `define-refleksi` → `doga-kalkani` "Doğa Kalkanı"); ids were renamed
  together with their `classes.yml` talent references.
- Two new `AbilityEffectType`s, both instant and both area-shaped (`limit`
  doubles as radius, the same dual-purposing convention §13.6 extended):
  **`GROWTH_SURGE`** (`filiz-patlamasi`, "Filiz Patlaması" - brings every
  immature `Ageable` within 6 blocks to maximum age, capped at
  `MAX_SURGED_CROPS = 256`, chunk-load-guarded column-major like
  `revealOres`) and **`LOVE_CALL`** (`suru-cagrisi`, "Sürü Çağrısı" -
  `Animals#setLoveModeTicks` on every adult non-in-love animal within 8
  blocks). Both report a count in their own new activation message.
- `ClassIntroMenu`'s hardcoded per-class-id lore switch and its
  `CLASS_SLOTS` array were updated for three classes;
  `GUI_CLASS_INTRO_LORE_SAVASCI`/`_ODUNCU` were deleted and
  `_TABIAT_VARISI` added, matching the delete-fully convention.

### 15.4 - Two talent point pools

- New `classes.TalentPointCategory` (`OZELLIK`/`KABILIYET`), **derived from
  `ClassEffectType`** via a new `category()`/`requiresAbility()` pair rather
  than declared in YAML (see decision 4 above). `requiresAbility()` also
  replaced the hand-written three-way `needsAbility` check in the parser,
  so adding an ability-scoped effect type can no longer forget it.
- Two new ability-scoped effects: **`ABILITY_DURATION_BONUS`** (extends
  `activeMillis` - `activateOnPlayerRegion` now computes one `activeMillis`
  local and every message/expiry/potion-duration/ore-expiry reads it) and
  **`ABILITY_EFFECT_BONUS`** (scales the ability's own scalar:
  `DAMAGE_BUFF`'s multiplier, `EXTRA_DROP_CHANCE`'s chance,
  `KNOCKBACK_SLAM`'s raw damage). `adjustedEffectValue` is deliberately
  **not clamped** - the one case where exceeding 1 would be meaningless, a
  drop chance, simply always succeeds, which is what a fully-ranked chance
  talent should do. `applyDropBuff`/`handleEntityDamage` had been reading
  `definition.damageMultiplier()` raw and now go through `classModifiers`.
- `classes.yml`'s `seviye-basina-yetenek-puani: 25` became a
  `yetenek-puani` section (`ozellik-seviye-basina: 25`,
  `kabiliyet-seviye-basina: 40`, `prestij-basina-ozellik: 4`,
  `prestij-basina-kabiliyet: 2`) backing the new `TalentPointSettings`
  record; `TalentPointBudget.pointsFor` gained a `pointsPerPrestige`
  parameter (the hardcoded `* 4` is gone) plus a category-dispatching
  overload. **Ability points are deliberately the scarcer pool.**
- `PlayerClassProgress.spentPoints(definitions, category)` is new and needs
  the catalogue, since only it knows a talent's category. A rank whose
  talent or class no longer exists in configuration **counts against
  nothing**, so removing an entry from `classes.yml` refunds it rather than
  leaving the player permanently short. `ClassService` gained
  `pointsFor(profile, category)`/`availablePoints(profile, progress, category)`.
- **`PlayerClassSnapshot` (the public API) was deliberately left reporting
  one combined budget** rather than gaining two more fields: an external
  consumer asking "how many points does this player have" is asking about
  total capacity, and the per-pool split is an internal spending rule.
- Every class's tree now offers both pools (avci 3+4, madenci 2+5,
  tabiat-varisi 3+7); `ClassMenu` shows two separate budget items, sorts
  features before abilities, and marks each talent's pool on its first lore
  line with a distinct icon material.

### 15.5 - Turkish command labels

- **This deliberately reverses §5.6's "command literals stay plain ASCII"
  rule for the player-facing half of the surface**, per an explicit user
  request. `/menü`, `/görev`, `/günlük`, `/ünvan`, `/sınıf`, `/kazıcılık`,
  `/avcılık` are now the primary labels, each keeping its old ASCII
  spelling as an **alias** so nothing a player already learned breaks and a
  non-Turkish keyboard can still reach every screen. New `/tarımcılık` and
  `/hayvancılık` follow the same pattern. `/haliladmin` stays ASCII-only -
  staff type it from a console as often as from a client. Commands with no
  diacritic to add (`/profil`, `/prestij`, `/kabiliyet`, `/koleksiyon`,
  `/liderlik`, `/kozmetik`, `/madencilik`, `/odunculuk`, `/panel`, `/mmo`)
  are untouched.
- **Empirically verified, not assumed** (see Verification): Brigadier
  matches a literal node by plain substring equality, so a non-ASCII label
  parses fine, and Paper's `registerCommand` accepted every one of them.

### Verification (this phase)

- `./gradlew build` / `./gradlew test` / `:core:compileIntegrationTestJava`
  are all green with Java 26 (`JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`)
  - **27 test classes, 58 test cases, 0 failures**, including a forced
  `--rerun-tasks` pass. No migration was added this phase (both new skills
  reuse `hmmo_player_skills`, and the new talent categories reuse
  `hmmo_player_class_talents`), so no migration-version test changed.
- Test changes: `ClassModifiersTest` and `TalentPointBudgetTest` rewritten
  for the widened records/signatures (both gained real coverage of the new
  duration/effect bonuses and the two independent pools);
  `SkillMaterialConfigTest` split into four cases and now validates the
  `eylemler` section, that **every `SkillAction` is claimed by exactly one
  skill**, and the full six-skill catalogue; `ClassConfigTest` now asserts
  the three-class set, that **every class offers both point categories**,
  and that **every skill is powered by exactly one class**;
  `AbilityConfigTest` gained radius validation for the two new area effects
  and a new case asserting **every ability's skill is one its own class
  powers** (the Phase 10.3 invariant nothing had been enforcing). New
  `PlayerClassProgressTest` covers per-pool spend counting including the
  removed-talent refund path.
- A repo-wide unused-import scan over every changed/new Java file found
  none. `MessageCatalogTest` independently enforces the `MessageKey` ↔
  `messages.yml` 1:1 match and that every `<simge:...>` names a real
  `IconPack` entry, which now includes the two new skill icons.
- **A live `./gradlew runServer` boot was performed**, with
  `core/run/plugins/HalilIbrahimMMO/`'s bundled YAML *and* its SQLite
  database deleted first so every changed resource regenerated and all 11
  migrations applied against a fresh database. The plugin enabled with zero
  exceptions, the server reached `Done` in ~8.4s, and the log had zero
  "eksik mesaj anahtari" warnings - confirming the six-skill `skills.yml`
  (including both `eylemler` sections and the two new milestone effect
  types), the three-class `classes.yml` with its `yetenek-puani` section
  and `sinerji-beceriler` lists, the 14-ability `abilities.yml` with the two
  new effect types, and every new `MessageKey` all parse on a real server.
- **A second `runServer` boot drove the console with the new Turkish
  commands**, which is what turns item 8 from "should work" into a fact:
  `menü`, `menu`, `görev`, `ünvan`, `sınıf`, `kazıcılık`, `tarımcılık` and
  `hayvancılık` each resolved to a real command node and reached execution,
  while a deliberately-bogus control (`bukomutyok`) produced "Unknown or
  incomplete command". (Every command dispatched from run-paper's console,
  **including vanilla `/stop`**, then hits a `CommandSourceStack.getLevel()`
  NPE - a console-source quirk of that environment, not a plugin fault, and
  equally present before this phase.)
- **Not verified this session, requires a live client** (the same honest
  caveat every feature-touching phase since §17 has carried): whether the
  Turkish labels render and tab-complete correctly in a real client's chat
  box; earning XP from each of the 12 new actions in real play; the
  lead-carrying tick firing every 20s; the breed refund landing in the
  inventory; a tree actually falling from its root log at odunculuk 25; the
  two new abilities' area effects; the redesigned two-pool talent screen;
  and whether the new skills' XP values feel right against the existing four.

## 30. Phase 16 — server design pass, quest rework, discovery quests, double
    right-click abilities, home and trade (Phase 16 complete)

A user-requested follow-on phase covering 14 items. Four load-bearing decisions
were taken before implementation, each turning a one-off request into a
mechanism rather than a special case:

1. **"Tree felling must never fire on a building" is a question about shape,
   not provenance.** §14.1's placed-block tracker cannot help here at all: a
   village house is generated by the *world*, so its timber is exactly as
   "natural" as a trunk. The answer had to be a geometric test.
2. **The quest→skill XP request was the fix for the missing quest level-up
   feedback.** Routing a quest's reward through the ordinary skill award path
   makes the existing, already-working synergy bridge produce the class XP
   *and* its feedback, instead of the quest listener assembling its own partial
   copy of that feedback - which is what was broken.
3. **The sidebar had to be rebuilt around a hard 15-line ceiling.** Minecraft
   renders at most 15 sidebar entries and silently drops the rest; the existing
   layout already exceeded that for Tabiat Vârisi before this phase added
   anything, so it was truncating invisibly.
4. **Two commands were allowed to take arguments again**, reversing §14.15's
   GUI-only rule for exactly the two cases where a menu cannot collect the
   input: a home's free-text name, and the name of the player to trade with.

### 16.1 - Tree felling only fells trees (`skills.TreeShape`)

- **Confirmed the report.** `SkillListener#fellTreeIfUnlocked` gated on
  "is a log" + "no log directly beneath", which is equally true of a village
  house's corner pillar, so odunculuk 25 was pulling the load-bearing timber
  out of generated buildings.
- New `skills.TreeShape` (static utility, no state, no lifecycle) answers
  `isNaturalTreeRoot(Block)` with three tests that must all hold: the block
  beneath is something a tree can root in (dirt family, moss, mud/clay/sand,
  nylium/soul soil); a same-type flood-fill of the trunk (bounded at 64 logs)
  touches at least 4 canopy blocks (`Tag.LEAVES`, plus nether wart blocks and
  shroomlight for the fungi, which are not leaves); and no trunk log touches
  anything man-made (`Tag.PLANKS`/stairs/slabs/walls/fences/doors/trapdoors/
  beds/wool/glass plus a small marker list of village workstations). Both
  answers come out of one 26-neighbour pass because a trunk touching a plank is
  disqualified however many leaves it also has - that is what keeps a house
  standing inside a forest safe.
- **Applied to the `agac-kirici` ability too**, not just the milestone: the
  ability had the identical hole. It stays armed and sends the new
  `ABILITY_REQUIRES_TREE` message rather than being consumed on a building.
- `TreeShape` lives in `skills` rather than a new package because `abilities`
  already imports `skills` (`SkillService`/`SkillDefinition`), so this adds no
  new dependency direction.

### 16.2 - Quests train a skill; class XP feedback unified

- `QuestDefinition` gained a **required `beceri`** field (validated against the
  live skill catalogue at parse time - `CachedQuestService` gained a
  `SkillService` constructor parameter for it). New
  `SkillService#awardExternal(playerId, name, skillId, amount, sourceDisplayName)`
  reuses `CachedSkillService`'s single private `award(...)` path, so a quest
  reward carries the same synergy/prestige multipliers, the same serial update
  chain and the same notification to `ClassSynergyBridge` as any block break.
- Consequence, and the point of the design: a quest's XP lands in the skill it
  trains **and** mirrors into the class track exactly when the player's class
  draws synergy from that skill. A zombie quest advances avcılık for everyone
  and Amansız Savaşçı as well for someone playing one. When the quest's skill
  is *not* a synergy skill of the selected class the bridge stays silent and
  the class is credited directly instead, so no quest is ever worth zero class
  XP for being off-speciality - and it is never double-credited.
- **New `classes.ClassAwardHandler`**, the class-XP twin of §15.1's
  `SkillAwardHandler`: bossbar in the class's colour, level-up chat line, sound
  cue and ability-unlock toast. Six independent sites awarded class XP and each
  had grown its own partial copy - `QuestListener` and `CollectionListener`
  both omitted the `<seviye>` placeholder §13.3 added, and **all four
  onboarding sites dropped the award result on the floor entirely**, so
  finishing a tutorial task could level a class in complete silence. Takes
  `AtomicReference`s for `ClassService`/`AbilityService` because its first
  consumer (`SkillService`, via the synergy bridge) is constructed before both.
- **New `quests.QuestCompletionHandler`** (chat line, fanfare, broadcast, XP,
  journal, unlock re-evaluation) shared by the two quest listeners, and new
  `onboarding.OnboardingRewarder` replacing four copies of the same block.
  `ClassSynergyBridge` shrank to just "is this a synergy skill" plus a delegate.

### 16.3 - Two new quest objective types

- `QuestObjectiveType` gained `STRUCTURE_DISCOVER` (`hedef.yapilar`) and
  `ITEM_OBTAIN` (`hedef.esyalar`). `QuestDefinition` now carries four target
  sets, only the relevant one populated - the same convention it has followed
  since it had two.
- **Structures are registry-backed, not an enum**, so `quests.yml` names them
  unqualified and lower-cased and the parser resolves them through
  `Registry.STRUCTURE.get(NamespacedKey.minecraft(...))`. `Structure.valueOf`
  does not exist and merely initialising the class outside a running server
  throws, so `QuestConfigTest` validates names via the **reflective
  field-name** trick §13 established for `Sound`/`Particle` - strictly better
  than an allow list because it cannot drift from the API.
- New `quests.QuestDiscoveryListener` owns both. **There is no "player entered
  a structure" event**, so it sweeps online players every 2s and asks
  `World#getStructures(chunkX, chunkZ)` which generated structures overlap
  their chunk, testing each bounding box - which is why this listener owns a
  `ScheduledTask` started/stopped explicitly by the plugin, the same shape
  `HusbandryListener`'s lead tick already has. Each (player, structure) pair is
  memoised for the session so standing in a mineshaft cannot tick a quest
  thousands of times.
- Item quests cover both ways an item reaches a player: `EntityPickupItemEvent`
  and an `InventoryClickEvent` on a container. The container half explicitly
  skips inventories held by a `GuiHolder` - our own menus are display icons,
  and a cancelled click on the quest board must not award a music disc.
- **19 new quests**: 13 structure discoveries (village → buried treasure →
  mineshaft → shipwreck → outpost → mansion → fortress → bastion → monument →
  trail ruins → ancient city → trial chambers → stronghold), 5 item quests
  (music disc, name tag, totem, elytra, dragon egg) and new pool entries.
  XP is tiered by difficulty: 1 800 for a village, 36 000 for the dragon egg.

### 16.4 - `PlacedBlockTracker#isEligibleTerrain` shared by skills and quests

Shipping a crop-harvest quest broke §15.1's recorded assumption that "no
shipped quest targets a crop". The `Ageable`-exemption test moved off
`SkillListener` onto `PlacedBlockTracker` (which already owns the "what counts
as player-placed" question) and `QuestListener` now uses it too - otherwise a
harvest quest would refuse the exact wheat the tarımcılık skill rewards.

### 16.5 - Abilities need a double right-click; Creative lifts every budget

- `AbilityActivationListener` gained a confirm step: the first qualifying right
  click stores the intent and puts a prompt on the action bar, and only a
  second click at the *same* ability for the *same* purpose within 3s fires it.
  Storing the intent rather than a bare timestamp is what stops a click that
  prompted for one ability from confirming a different one. Abilities cost
  energy and start long cooldowns, and a single right click is something
  players do constantly by accident.
- **Creative mode**: no selection cooldown, no ability cooldown, no energy cost,
  no per-hit energy drain, and a 6-hour duration standing in for "until turned
  off" (still a real expiry, so nothing is left armed forever by a logout).
  New `AbilityService#deactivate(Player)` clears every armed/buffed map and the
  vanilla potion effect an instant ability applied; in Creative the same double
  right-click toggles a running ability back off. The action-bar readout
  switches to a dedicated "Yaratıcı mod - kapatmak için çift sağ tıkla" line
  rather than counting down from six hours.

### 16.6 - `/ev` and `/takas`

- New `progression.FeatureUnlockRules` (pure, tested): **prestige always
  satisfies the gate**, because prestige resets the selected class to level 1
  and a purely level-based gate would take these features away from the players
  who worked hardest for them, permanently, every time they prestiged.
  Home = class level 15; trade = 20 to start one, 15 to be traded with.
- New `home` package + migration **V12** (`hmmo_player_home`, one named home per
  player, stored as world UUID + raw coordinates because a `Location` holds a
  live `World` that cannot survive a database round-trip). The Nether and the
  End are excluded **at both ends of the trip** - a home cannot be set in
  either and cannot be travelled to from either, since letting `/ev`
  short-circuit a nether portal deletes the part of the game those dimensions
  are. Warmed on join by `SidebarListener` (the sidebar reads it synchronously).
- New `trade` package, deliberately **in-memory only, no table**: a trade is a
  conversation between two players who are both online, and a half-finished one
  must not survive a restart. **Offered items live in the `TradeSession`, not
  in an inventory** - the moment something is offered it leaves the player's
  inventory and the screen renders copies of it. That single-authoritative-copy
  rule is what makes the feature safe against duplication, and gives crashes,
  kicks and closed windows one obvious correct response (`cancel`, which is
  idempotent and returns everything). Any change to the table clears **both**
  confirmations, so nobody can confirm, wait, and then pull their goods back.
  No distance check anywhere - remote is the point.
- **`GuiView` gained two default methods** for this: `handleOwnInventoryClick`
  (clicks in the player's own inventory, still pre-cancelled - only the trade
  screen overrides it, to offer an item by clicking where it already sits) and
  `handleClose`. `GuiListener` gained an `InventoryCloseEvent` handler. The
  "these menus are never storage" invariant §17/§21 established is *preserved*:
  the trade screen never holds a real item either.
- `TradeScreenRenderer` is declared in `trade` and implemented in `gui`
  (`TradeScreenBridge`), bound by the bootstrap once both halves exist - the
  same forward-reference shape `SkillExperienceListener` uses. The bridge
  repaints the already-open inventory rather than reopening it, because
  reopening fires a close event, which the trade screen treats as walking away
  from the table.

### 16.7 - Design pass

- **`skills.yml` gained a required `simge` per skill** and `SkillDefinition` an
  `icon`; `classes.yml` gained a required `cerceve` (stained-glass pane) per
  class and `PlayerClassDefinition` a `frame`. The abilities screen, the talent
  tree and each skill's detail screen are now framed in the owning class's own
  colour, so a screen is legible as *yours* before a label is read.
- `SkillsMenu` rebuilt: framed, per-skill icons, synergy marker, and each entry
  now opens that skill's detail screen. **Renamed "Yeteneklerim" →
  "Becerilerim"** (`gui.yetenekler.*` → `gui.beceriler.*`, `gui.ana.yetenekler`
  → `gui.ana.beceriler`) - sharing a word with the talent tree was a reliable
  way to make players open the wrong screen. New `/beceri` command (alias
  `/beceriler`).
- **Quest board rebuilt as tabs**: a column of four section buttons on the left
  and 42 entry slots for the selected section, replacing four rows of seven
  that competed for attention and silently dropped an eighth quest. This is
  also what makes the 19 new quests fit.
- **Sidebar rebuilt to a hard 15-line ceiling** (`MAXIMUM_LINES`, with
  `trimToFit` shedding overflow from just above the always-kept footer). Güç
  and prestij share a line; class and class level share a line; the per-ability
  list (up to 8 lines for Tabiat Vârisi, which alone overran the ceiling before
  a single quest was listed) became one line showing the active or selected
  ability. Daily and weekly quests each gained their own labelled header
  section, alongside the existing Başlangıç section. A new features line shows
  `/ev` and `/takas` state, plus a live line while a trade is open; it appears
  only once one of the two is unlocked, so it never competes with the
  onboarding section a new player needs.
- The skill-command onboarding line was shortened to
  "Beceri komutlarını incele (/madencilik, /avcılık vb.)".

### Verification (this phase)

- `./gradlew build` / `./gradlew test` / `:core:compileIntegrationTestJava` are
  all green with Java 26 (`JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`) -
  **29 test classes, 66 test cases, 0 failures**, including a forced
  `--rerun-tasks` pass.
- Test changes: `QuestConfigTest` rewritten for the four objective types, the
  required `beceri` field and reflective structure-name validation, and gained
  two cases (**every configured skill is trained by at least one quest**; each
  pool is at least as large as its assignment count). New
  `FeatureUnlockRulesTest` (4 cases, including the prestige-at-class-level-1
  case that is the whole reason the rule exists) and
  `PlayerHomeSchemaMigrationTest` pinning V12. `SkillMaterialConfigTest` gained
  a distinct-icon-per-skill case; `ClassConfigTest` validates `cerceve`;
  `PlayerClassProgressTest` updated for the widened record.
  `SqliteMigrationContractTest`/`AbstractMigrationCrossEngineTest`/
  `IntegrationDatabaseSupport` updated to include V12.
- A repo-wide unused-import scan over `core/src` found none. A programmatic
  sweep confirmed all 383 `MessageKey` constants have a real call site, and
  `MessageCatalogTest` independently enforces the 1:1 match with
  `messages.yml` and that every `<simge:...>` names a real `IconPack` entry.
- **A live `./gradlew runServer` boot was performed**, with
  `core/run/plugins/HalilIbrahimMMO/`'s bundled YAML *and* its SQLite database
  deleted first so every changed resource regenerated and all 12 migrations -
  including the new V12 - applied against a fresh database. The plugin enabled
  with zero exceptions, the server reached `Done` in ~7.8s, and the log had
  zero "eksik mesaj anahtari" warnings - confirming the rewritten `quests.yml`
  (including `Registry.STRUCTURE` resolution of all 13 structure quests), the
  new `simge`/`cerceve` keys and every new `MessageKey` parse on a real server.
- **A second boot drove the console with the new commands**: `beceri`, `ev`,
  `takas`, `home` and `trade` each resolved to a real command node and reached
  execution, while a deliberately-bogus control (`bukomutyok`) produced
  "Unknown or incomplete command". (Console-dispatched commands still hit the
  `CommandSourceStack.getLevel()` NPE §29 already recorded as a run-paper
  environment quirk, not a plugin fault.)
- **Not verified this session, requires a live client** (the same honest caveat
  every feature-touching phase since §17 has carried): breaking a village
  house's log pillar and confirming it no longer fells, versus a real tree that
  still does; a complete trade end to end including both sides confirming, one
  side quitting mid-trade and a window closed with goods on the table;
  `/ev ayarla` and teleporting from another dimension being refused; the
  double right-click prompt feeling responsive rather than fiddly; Creative
  toggling an ability off; the structure-discovery sweep actually firing on
  entering a village; the rebuilt sidebar staying inside 15 lines across all
  states; the tabbed quest board and the reframed class screens.

## 31. Phase 17 — interaction guards, world bosses, Zenginlik Puanı, quest
    rework, /rtp, admin redesign (Phase 17 complete)

A user-requested follow-on phase covering 14 items. Three load-bearing
decisions were taken before implementation, each turning a one-off request into
a mechanism rather than a special case:

1. **"The ability must not open when I right-click a door" is a question about
   the block, not about the ability.** The answer is one bounded list of blocks
   that already answer a right click with something of their own, consulted by
   the activation listener - not a per-ability opt-out.
2. **Zenginlik Puanı had to be a score, not a currency.** This project has had
   no economy since Phase 1 (§1) and adding a spendable number would have been
   exactly the thing that rule forbids. Every source is one-shot by
   construction, nothing is ever deducted, and what it buys is standing:
   leaderboard rank, titles, cosmetics and announcements.
3. **A world boss spawns near a player, not at a uniform point on the map.** A
   boss nobody can reach is a boss nobody fights; the announcement carries the
   coordinates so the whole server can converge on it.

### 17.1 - Right-click guards and message noise

- New `abilities.InteractableBlocks`: a bounded list (a `Set<Material>` of
  containers/workstations plus the `Tag` families - doors, trapdoors, fence
  gates, buttons, beds, shulker boxes, anvils, cauldrons, candles, flower pots,
  campfires, signs). `AbilityActivationListener#onInteract` now ignores a
  `RIGHT_CLICK_BLOCK` on any of them. **Sneaking is the deliberate escape
  hatch**, because vanilla already suppresses a block's own right-click
  behaviour while sneaking - so the gesture a player already knows is the one
  that gets their ability at a door.
  Chosen over `Material#isInteractable()`, which is documented as not
  comprehensive; a hand-written list is the same bounded-value discipline
  everything else in this project follows and cannot silently change meaning
  when Mojang adds a block. Entity right-clicks were left alone: §14.6 added
  that path on purpose so combat abilities can be started while aiming at a mob.
- Outside Creative, a click aimed at an **already-running** ability now returns
  silently before the confirm step. It previously produced the "click again"
  action-bar prompt and then, on the second click, a cooldown refusal - two
  messages for something the player could not have done anyway. The action-bar
  countdown is already saying it is running.

### 17.2 - Class change unequips the ability

`ClassConfirmMenu` already called `AbilityService#clearSelection`; the admin
force-set path (`AdminClassPickerMenu`) did not, so staff-driven changes left a
foreign class's ability equipped. Both now do. `clearSelection`'s contract also
widened from "selection only" to "the selection and anything it left running" -
armed block abilities, damage/drop buffs, instant potion effects and ORE_REVEAL
outlines all end, because none of them belong to the new class. **Cooldowns are
deliberately preserved**, so a class change can never be used to wipe one. The
player-touching half is hopped onto the target's own region scheduler, since
the admin path resolves on a database thread.

### 17.3 - Maden Feneri: nearest-first scan, bigger outline, Creative cleanup

- **The reported "only the ores behind walls glow" was a scan-order bug, not a
  rendering one.** `revealOres` swept column-major from the *western corner* of
  the search box and stopped at `MAX_REVEALED_ORES` (200), so at radius 36 the
  two hundred ores that got lit were whichever ones happened to lie west - the
  seam the player was standing in front of was frequently never reached.
  Replaced with an outward walk, one cube shell at a time
  (`shellOffsets(int)` enumerates the exact Chebyshev-distance surface rather
  than filtering the whole cube, so the whole scan stays O(r^3) and stops as
  soon as the budget is spent).
- The outline scale went 1.02 to 1.08 (translation -0.01 to -0.04): at 2% the
  halo of an ore the player could already see was hidden inside its own faces.
- `revealedOres`' value became a `RevealedOre(ownerId, display)` record, so
  `deactivate(Player)` - the Creative off switch - can take down that player's
  own outlines and nobody else's. Previously switching the ability off in
  Creative left the whole cave glowing until the (six-hour) Creative duration
  ran out.

### 17.4 - One-shot abilities expire normally in Creative

New `AbilityEffectType#resolvesImmediately()` (`KNOCKBACK_SLAM`,
`GROWTH_SURGE`, `LOVE_CALL`). Creative stretches an ability's duration to
`CREATIVE_DURATION_MILLIS` so a *sustained* buff stays up until switched off -
but a shockwave has already thrown everything nearby before the timer starts,
so holding it open only made Yıkıcı Darbe unusable again until the player
remembered to toggle it. Those three now keep their configured duration even in
Creative (2 seconds for Yıkıcı Darbe), which is what the request asked for.

### 17.5 - Talent tree laid out as two labelled blocks

`ClassMenu` was one unbroken run of talent items starting at slot 28, which ran
off its row and over the frame, and the only way to tell which of the two point
pools a talent belonged to was to hover it. Rebuilt: a class header at slot 4
(icon, class level and XP), the class icons and both point counters sharing row
1, then a labelled **Özellik** block (header + 6 slots) and a labelled
**Kabiliyet** block (header + 13 slots). `GUI_TALENT_CATEGORY_FEATURE`/`_ABILITY`
were repurposed from a per-item lore line into those two headers (text updated
in `messages.yml`, keys unchanged), and each gained a `_LORE` sibling. Talents
sort alphabetically inside a pool so a talent keeps its place between opens.

### 17.6 - Tabiat Vârisi cut from 8 abilities to 4

`kazici-darbe`, `yesil-yol`, `doga-kalkani` and `doga-sifasi` are gone. The
surviving four are **exactly one per synergy skill**, unlocking weakest to
strongest: Toprak Sezgisi (kazıcılık, 5) → Filiz Patlaması (tarımcılık, 15) →
Sürü Çağrısı (hayvancılık, 25) → Ağaç Kırıcı (odunculuk, 40). One orphaned
talent (`sifa-suresi`, which pointed at the removed `doga-sifasi`) was
retargeted rather than deleted, becoming `toprak-tasarrufu` on
`toprak-sezgisi` - the class keeps 3 feature + 7 ability talents, so
`ClassConfigTest`'s "every class offers both pools" invariant still holds.

### 17.7 - `/rtp` (alias `/rastgele`)

New `command.RandomTeleportCommand`: a random point 500-10 000 blocks either
side of world spawn, overworld only, 30-minute cooldown, on-screen title plus a
chat line carrying the landing coordinates
(`FeedbackService#celebrateRandomTeleport`, new `rastgele-isinlanma` sound cue).
**The search is asynchronous by necessity**: a candidate ten thousand blocks
away is almost never in a loaded chunk, and asking for its highest block would
generate that chunk synchronously - each candidate goes through
`World#getChunkAtAsync` first, and a candidate that turns out to be ocean, lava
or a cave ceiling costs another round of the loop rather than a bad landing (16
attempts, then the player keeps their turn). The cooldown is claimed *before*
the search so spamming the command cannot start a dozen at once, and refunded
if nothing was found. Nether and End are refused: the Nether's roof is what
every "highest block" answer lands on, and a random point in the End is either
void or a dragon's island.

### 17.8 - `/ev` teleport cooldown

New shared `util.PlayerCooldowns` (in-memory only, deliberately - a cooldown
surviving a restart would punish players for something the server did), used by
both `/rtp` and `/ev`. `HomeService.TELEPORT_COOLDOWN_MILLIS` is 5 minutes and
applies to the trip only: setting or deleting a home stays free, and the clock
starts only once the trip is actually happening, so "you have no home" never
costs five minutes. New `HomeActionResult.COOLDOWN` + `ev.bekleme` message.

### 17.9 - World bosses (new `boss` package)

- New `bosslar.yml` and package: `BossRarity` (5 tiers, each owning four
  fully-authored `MessageKey`s - nameplate, spawn broadcast, death broadcast,
  head name), `BossArchetype`, `BossWeaponType`, `BossLootEntry`,
  `BossRaritySettings`, `BossSettings`, `BossService`/`YamlBossService`,
  `BossListener`.
- **Rarity is picked by weight, not percentage.** The requested split
  (49/30/20/10/1) sums to 110, which no set of percentages can be; weights
  express exactly the ratio that was asked for and can never quietly stop
  summing to a whole. Shipped weights are the requested numbers verbatim.
- **Per-rarity MessageKeys rather than one template fed a rarity label** - the
  same discipline §8.2 established for per-cause XP bossbars. The concrete
  payoff: a rarity's colour lives in `messages.yml` where a server owner can
  change it, instead of being a Java colour constant that no placeholder could
  carry anyway (§10.6's documented limitation), so this needed no
  `MessageKey`-bypass exception at all.
- **A boss is a tagged vanilla mob and nothing else.** Health/damage/speed are
  attribute multipliers, gear comes from the rarity (the archetype picks the
  weapon *kind*, the rarity picks the item, so "a mystic boss carries a far
  better weapon" is one line per rarity rather than a matrix), and identity
  lives in the entity's own `PersistentDataContainer`. That is what makes the
  whole system **survive a restart with no table of its own**: the server saves
  the entity, the tag rides along, and `BossListener` reads the tag rather than
  an in-memory registry. **Known, documented limitation:** the flee/heal/blink
  behaviour loop is in-memory and is not re-attached to a boss that outlived a
  restart - it keeps its stats, its name and its full reward and simply fights
  like a very strong ordinary mob.
- Behaviour, per rarity: self-heal at half health (30s cooldown), blink toward
  the nearest player every 12s, and a one-time flee at a quarter health
  (teleport away + Speed II). Running takes priority over repositioning.
- 35 names across 5 archetypes (zombi/iskelet/boğulmuş/büyücü/baltacı), 7 each,
  **themed per archetype on purpose**: a name should say what you are looking
  at before you can read the rarity.
- On death: vanilla XP (`setDroppedExp`), class XP through the ordinary
  `ClassAwardHandler` path so a boss kill levels a class exactly like anything
  else, rarity-scaled loot, Zenginlik Puanı, and the boss's own head named after
  it in its rarity's colour. Ordinary skill XP/quest progress/wealth for the
  underlying creature still flow through `SkillListener` - the boss reward is
  entirely *on top*.

### 17.10 - `/haliladmin` rebuilt

`AdminMenu` went from a bare 27-slot grid to the same visual language as
`/menü`: 45 slots, framed, a `PLAYER_HEAD` header reading the live version and
online count, and every action carrying a lore line explaining it. New action:
**Dünya Bossu Çağır** (`BossService#spawnNow`), which starts the next boss
immediately without letting staff hand-pick its rarity.

### 17.11 - Zenginlik Puanı (new `wealth` package)

- New `zenginlik.yml`, migration **V13** (`hmmo_player_wealth`),
  `WealthService`/`CachedWealthService`/`WealthRepository`/`WealthSettings`/
  `WealthAward`, and `WealthAwardHandler` - the third member of the
  `SkillAwardHandler`/`ClassAwardHandler` family, so a milestone looks identical
  whichever of the four sources produced it.
- Sources: world-generated blocks (diamond ore 10 000, iron 500, ancient debris
  40 000, dragon egg 250 000...), kills (Warden 250 000, Ender Dragon 500 000,
  ~40 tiered mobs, unlisted mobs get the floor), **quest completion** and world
  bosses. Discovering somewhere valuable pays out through the discovery quest
  rather than a "you entered a structure" hook, because a quest completes
  exactly once and a structure can be walked into all day.
- **Wired into `SkillListener` rather than a listener of its own**, deliberately:
  wealth answers to exactly the same four gates skill XP does (activated,
  real gamemode, world-generated block, plausible break rate) and a second
  listener would have had to reproduce all four and keep them in step forever.
- Milestones (7 thresholds) fire a personal cue plus a server-wide broadcast;
  only the **highest** crossed is announced, since one boss kill can vault a new
  player past several at once.
- Surfaces: a blue `sidebar.zenginlik` line, a new `LeaderboardCategory.WEALTH`
  board (diamond icon), and a new `UnlockConditionType.WEALTH`.
  `UnlockCondition` gained a `long amount` field because wealth totals run past
  what an `int` holds; `UnlockContext` gained `wealth`, so `UnlockContextLoader`
  now depends on `WealthService`.
- **Titles went 5 → 19 and cosmetics 4 → 14**, including 5 wealth-gated titles
  and 3 wealth-gated cosmetics. `CosmeticConfigTest`'s hand-maintained particle
  allow-list was migrated to §27's reflective field-name technique, which is
  what made adding new particle effects a one-line change.

### 17.12 - Quest rework and sidebar pinning

- **`quests.yml` rewritten: 65 quests** (36 permanent, 16 daily, 13 weekly),
  every one carrying a new required **`kisa-ad`**. That field exists because a
  scoreboard line does not wrap, it truncates - the full titles are written for
  a menu tooltip and were being cut off mid-word on the panel.
  `QuestConfigTest` enforces a 20-character ceiling on it.
- **Two new objective mechanisms**, both driven by the specific quests that were
  asked for:
  - `hedef.boyut` (optional `NORMAL`/`NETHER`/`THE_END`) on block and kill
    objectives, so "kill a hundred endermen in the End" means the End rather
    than a plains biome at night.
  - `QuestObjectiveType.DEED` + a bounded `QuestDeed` enum (6 constants) and
    `QuestDeedListener`, for objectives that are a story rather than a counter.
    The ghast one is why that listener has state: Minecraft never reports "a
    ghast died to its own fireball", so the damage event notices the reflection
    and remembers the ghast, and the death event asks whether that is the one
    dying. `QuestConfigTest` asserts every deed is used by at least one quest -
    an unused constant would be a listener firing for nobody.
  - The requested quests landed as: mansion evokers (3 EVOKER), elder guardian,
    bastion gold blocks (10 `GOLD_BLOCK` in the Nether - the placed-block
    tracker already means only generated ones count), piglin bartering,
    10 blaze + 10 wither skeletons in the Nether, the ghast reflection, and
    100 endermen in the End.
- **Sidebar pinning**: migration **V14** (`hmmo_player_quest_pins`, same
  comma-joined shape as `hmmo_player_quest_assignments`),
  `QuestService#togglePin`/`cachedPins`/`loadPins`, `QuestPinResult`, capped at
  `MAXIMUM_PINS_PER_TYPE = 3` per type. Clicking an unfinished quest on the
  board pins it. A new **permanent** sidebar section exists only because of
  this - there are 36 permanent quests, so listing them all was never an option
  and listing none was the old behaviour.
  **For daily and weekly, pins act as a filter rather than an addition**, and
  that is a deliberate reading of the request: pinning nothing keeps the old
  default of showing every assigned one, and pinning is not allowed to *add* an
  unassigned pool quest, because `isRelevant(...)` means an unassigned pool
  quest cannot make progress at all - showing one would be a line that never
  moves.

### Verification (this phase)

- `./gradlew build` / `./gradlew test` / `:core:compileIntegrationTestJava` are
  all green with Java 26 (`JAVA_HOME=C:\Program Files\Java\jdk-26.0.1`),
  including a forced `--rerun-tasks` pass.
- Test changes: `UnlockConditionsTest` rewritten for the widened
  `UnlockCondition`/`UnlockContext` records and gained a WEALTH case;
  `QuestConfigTest` gained `kisa-ad` length validation, `DEED`/`boyut`
  validation and the every-deed-is-used case; `CosmeticConfigTest` migrated off
  its hardcoded particle allow-list; new `WealthSchemaMigrationTest` (V13) and
  `QuestPinSchemaMigrationTest` (V14); `SqliteMigrationContractTest`/
  `AbstractMigrationCrossEngineTest`/`IntegrationDatabaseSupport` updated to
  include V13/V14.
- An unused-import scan over every changed file found two (a stale `MessageKey`
  import in each of the two new `boss` classes); both removed. A programmatic
  sweep confirmed every `MessageKey` constant has a real call site, and
  `MessageCatalogTest` independently enforces the 1:1 match with `messages.yml`
  and that every `<simge:...>` names a real `IconPack` entry - which now
  includes the two new icons (`zenginlik`, `boss`).
- **A live `./gradlew runServer` boot was performed**, with
  `core/run/plugins/HalilIbrahimMMO/`'s bundled YAML *and* its SQLite database
  deleted first so every changed resource regenerated and all 14 migrations -
  including the new V13/V14 - applied against a fresh database. The plugin
  enabled with zero exceptions, the server reached `Done` in ~7.6s, and the log
  had zero "eksik mesaj anahtari" warnings - confirming the rewritten 65-quest
  `quests.yml` (including `boyut` and `DEED` parsing), the new `zenginlik.yml`
  and `bosslar.yml` (every rarity, weapon material, armour piece and loot line),
  the expanded `titles.yml`/`cosmetics.yml` and every new `MessageKey` parse on
  a real server.
- **Not verified this session, requires a live client** (the same honest caveat
  every feature-touching phase since §17 has carried): right-clicking a door
  with an ability selected and confirming nothing fires, versus sneak-clicking
  the same door and confirming it does; Maden Feneri lighting the seam directly
  in front of the player; Yıkıcı Darbe being re-triggerable after two seconds in
  Creative; a world boss actually spawning, fleeing at low health and dropping
  its named head; the Zenginlik milestone broadcast; the six new deed objectives
  firing; pinning quests and watching the sidebar sections change; `/rtp`
  landing somewhere survivable; the redesigned talent tree and admin panel.
