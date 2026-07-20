# HalilIbrahimMMO

Original MMORPG progression plugin for Paper 26.2 (Minecraft 26.2), built for a Turkish
Minecraft community. No economy — progression is driven entirely by levels, skill XP,
prestige, quests, collections and achievements.

This is a multi-phase commercial-grade build. See [`PROJECT_MEMORY.md`](PROJECT_MEMORY.md)
for the full architectural record; it is kept up to date after every phase and must be
read before starting new work on this project.

## Requirements

- Java 26 (Oracle JDK or OpenJDK)
- Paper 26.2 (Build 60 or newer)
- Gradle Wrapper (bundled, no local Gradle install required)

## Project layout

- [`api/`](api) — Public developer API. Pure interfaces + a `ServicesManager`-based
  accessor. Other plugins depend on this module only.
- [`core/`](core) — Full plugin implementation (`HalilIbrahimMMOPlugin` and every
  internal system). Depends on `api`.

## Building

```powershell
./gradlew build
```

The shaded, ready-to-deploy jar is produced at `core/build/libs/HalilIbrahimMMO-<version>-all.jar`.

## Running a local test server

```powershell
./gradlew runServer
```

This downloads a Paper 26.2 test server into `run/` and boots it with the plugin installed.

## Database

SQLite is the zero-config default. MySQL, MariaDB and PostgreSQL are supported via
`config.yml` (`veritabani.tur`). All JDBC drivers and HikariCP are resolved at runtime
through Paper's `PluginLoader` mechanism — nothing is shaded into the plugin jar.

## Progression foundation

The current build includes asynchronous UUID-based player profiles, statistics and
global progression. `progression.yml` contains the configurable level cap, explicit
XP table and rewards for block breaking, mob kills and playtime. Players can view
their saved progression with `/profil`. Use Java 26 for every Gradle command; the
workspace's installed JDK is `C:\Program Files\Java\jdk-26.0.1`.

## Skills and abilities

Four independently levelled skills are now included: `kazicilik`, `odunculuk`,
`madencilik` and `avcilik`. Their shared explicit level curve and valid gameplay
sources are configurable in `skills.yml`. `abilities.yml` configures the shared,
non-persistent energy pool and the four bounded starter abilities. Players use
`/kabiliyet <id>` to activate an ability; block abilities apply to the next valid
block within 15 seconds, while Avcı Odağı is a short PvE-only damage boost.

The developer API exposes immutable skill definitions and asynchronous read-only
player skill lookups. It does not allow external plugins to award skill XP or
trigger abilities.

## Classes and prestige

Players can choose Savaşçı, Avcı, Madenci or Oduncu with `/sinif sec <id>`, inspect
their selected tree with `/sinif agac`, and spend shared talent points with
`/sinif ac <yetenek>`. A class change has a configurable seven-day, database-backed
cooldown. At global level 100, `/prestij onayla` resets only global level/XP and
opens another four-point talent cycle; the default maximum is 10 prestiges.

## Quests, collections and the adventure journal

`/gorev` lists every permanent, daily and weekly quest along with your progress;
quests advance automatically as you break the right blocks or kill the right
mobs, and daily/weekly quests reset on their own once their period ends.
`/koleksiyon` lists your collections, and `/koleksiyon <id>` shows exactly which
items you still need to pick up at least once. `/gunluk` shows your ten most
recent quest completions and collection milestones. Completing a quest,
obtaining a collection entry, and completing a whole collection all award
global XP through the same progression system as everything else.

## Leaderboards

`/liderlik seviye`, `/liderlik prestij`, `/liderlik gorev`, `/liderlik
koleksiyon` and `/liderlik <yetenek-id>` show the top-ranked players for
global level/XP, prestige, quest completions, collection entries obtained and
each configured skill. The same boards are browsable from `/menu`'s
Leaderboards icon. Results are cached for a short, configurable interval
(`leaderboards.yml`) rather than querying the database on every open.

## Titles and cosmetics

`titles.yml` and `cosmetics.yml` each configure a catalogue of unlockable
entries, one milestone condition per entry (reach a level/prestige, complete
a specific quest/collection, or reach a skill level). `/unvan` and
`/kozmetik` list what you've unlocked and let you equip one
(`/unvan sec <id>`, `/kozmetik sec <id>`); the same catalogues are also
browsable from `/menu`. An equipped title shows before your name in chat.
An equipped cosmetic plays a short particle effect when you join and every
time you level up.

## Visual feedback

Level-up, quest completion, whole-collection completion and prestige each
show a short boss bar and on-screen title to every player, independent of
any equipped cosmetic (`feedback.yml` controls how long they stay visible).

## Inventory menu

`/menu` opens a central hub with icons for leaderboards, your profile, skills,
abilities, class/talents, prestige, quests, collections, adventure journal,
titles and cosmetics. Every menu is a view over the same systems the text
commands use - selecting a class or spending a talent point in the GUI does
exactly what `/sinif sec`/`/sinif ac` does, and abilities activate the same
way `/kabiliyet` does.

## PlaceholderAPI and LuckPerms (optional)

Both are genuinely optional soft dependencies — the server boots identically
whether either, both, or neither is installed. When **PlaceholderAPI** is
present, this plugin registers a `halilibrahimmmo` expansion exposing
`%halilibrahimmmo_level%`, `_xp`, `_xp_to_next_level`, `_prestige`, `_class`,
`_title`, `_cosmetic`, and `_skill_<id>_level`/`_skill_<id>_xp` per configured
skill. When **LuckPerms** is present, this plugin registers a read-only
context calculator feeding `halilibrahimmmo:level`, `halilibrahimmmo:prestige`
and `halilibrahimmmo:class` into LuckPerms, so server admins can write their
own permission rules conditioned on progression — this plugin never writes to
LuckPerms itself.

## Testing

`./gradlew test` runs the fast unit/config suite (no external services
needed, including a migration-execution check against a real SQLite file).
`./gradlew integrationTest` additionally runs every shipped database
migration and a representative sample of repository read/write round-trips
against real MySQL, MariaDB and PostgreSQL containers via Testcontainers —
this requires a running Docker daemon and is intentionally excluded from
`build`/`test`/`check`, so it must be run explicitly and separately.

## Language policy

Everything a player or server admin sees in-game (messages, GUIs, commands, BossBars,
titles, config comments) is Turkish. Java source code — packages, classes, methods,
variables — stays in English.
