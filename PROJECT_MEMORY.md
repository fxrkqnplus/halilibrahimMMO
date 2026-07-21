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
