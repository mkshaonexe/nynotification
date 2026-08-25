# Quiet Inbox — Parallel Build Plan

> **Companion document:** [`design.md`](design.md) — read it fully before writing any code.
> **Execution model:** one AI agent conversation per phase. Phases inside the same wave run
> **at the same time, in separate conversations**, and never touch the same file.

---

## 0. How to run this

### 0.1 Waves

```
WAVE 1  ── Phase 0  ─────────────────────────────────  RUN ALONE. Nothing else may start.
                │
                │  (project compiles, every route reachable as a stub)
                ▼
WAVE 2  ── Phase 1 ─┐
           Phase 2 ─┤
           Phase 3 ─┤
           Phase 4 ─┤
           Phase 5 ─┼── ALL NINE IN PARALLEL, one conversation each
           Phase 6 ─┤
           Phase 7 ─┤
           Phase 8 ─┤
           Phase 9 ─┘
                │
                ▼
WAVE 3  ── Phase 10 ────────────────────────────────  RUN ALONE, after all of Wave 2 merged.
```

**Wave 1 is not optional and must not be parallelised.** Phase 0 creates every shared file —
Gradle, manifest, database, DI, theme, navigation, string resources and the frozen contracts
in §2. Every Wave-2 phase codes against those contracts and against stubs, which is the only
reason nine agents can work simultaneously without conflicting.

### 0.2 Master prompt

Paste this at the top of **every** phase conversation, then attach `design.md` and `TASKS.md`,
then say `Execute Phase N.`

```text
You are a senior Android engineer implementing one phase of a production app.

CONTEXT
- Two documents are attached: design.md (the specification) and TASKS.md (this plan).
- design.md is authoritative. If the code and design.md disagree, design.md wins.
- You are executing exactly ONE phase. Other agents are executing the other phases
  RIGHT NOW, in parallel, in separate conversations.

HARD RULES — violating any of these breaks the parallel build
1. Create and edit ONLY the files listed under "Owns" for your phase.
2. Never edit any file listed under "Frozen" or owned by another phase. Not even
   one line. Not even to add an import, a string, or a dependency.
3. Never edit: build.gradle.kts (any), settings.gradle.kts, gradle/libs.versions.toml,
   AndroidManifest.xml, res/values/strings.xml, res/values/themes.xml, di/AppModule.kt,
   the Room database/entity/DAO files, or the navigation graph.
   Phase 0 has already put everything you need in them.
4. Every dependency you need is ALREADY declared. Every string you need has a home:
   write your own strings into res/values/strings_<yourphase>.xml, which only you own.
   Every Hilt binding you need has a home: your own di/<Phase>Module.kt.
5. The contracts in TASKS.md §2 are frozen. Implement them exactly — same package,
   same names, same signatures. Do not "improve" a signature.
6. Replace the stub implementation for your feature. Do not delete the stub file;
   fill it in, keeping its package, file name and public entry-point signature.

QUALITY BAR
- Kotlin, Jetpack Compose, Material 3, Hilt, coroutines/Flow. No new libraries.
- No hardcoded colors, sizes, or user-facing strings in Kotlin. Use the theme tokens
  and your strings file.
- Every public function gets a KDoc line when its purpose is not obvious.
- Write the tests listed in your phase's "Tests" section. They must pass.
- Touch targets >= 48dp. Content descriptions on every icon-only control.
- No android.permission.INTERNET usage, no networking, no analytics, ever.

WHEN YOU FINISH
1. Run: ./gradlew :app:assembleDebug :app:testDebugUnitTest
2. Both must pass. Fix your own code until they do. If a failure is in a file you do
   not own, STOP and report it instead of editing that file.
3. Reply with: files created, files edited, tests added, anything in design.md you
   could not implement and why.

Now read design.md end to end, then execute the phase I name next. Ask nothing;
if a detail is genuinely unspecified, choose the option most consistent with
design.md's principles and note the choice in your final report.
```

### 0.3 Conflict rules (why this works)

| Shared resource | How conflict is avoided |
|---|---|
| Gradle files, version catalog | Phase 0 declares **every** dependency the whole project will ever need. Wave-2 agents never open these files. |
| `AndroidManifest.xml` | Phase 0 declares the service, receivers, `<queries>` and all permissions up front. |
| Strings | Each phase writes only `res/values/strings_<phase>.xml`. Android merges all `values/*.xml`. |
| Hilt graph | Each phase owns `di/<Phase>Module.kt`. Hilt merges modules automatically. |
| Room schema | Frozen in Phase 0. Wave-2 phases may add **@Query methods only** to their own DAO extension interface — see §2.9. |
| Navigation | Phase 0 defines all routes and wires every stub. Wave-2 phases only fill in screen bodies. |
| Theme / components | Phase 0 builds the whole component inventory. Wave-2 phases consume, never modify. |

---

## 1. File ownership matrix

`R` = may read · `W` = owns, may write · blank = must not open

| Path | P0 | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | P9 | P10 |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| `*/build.gradle.kts`, `libs.versions.toml`, `settings.gradle.kts` | W | R | R | R | R | R | R | R | R | R | W |
| `AndroidManifest.xml` | W | R | R | R | R | R | R | R | R | R | W |
| `res/values/strings.xml`, `themes.xml`, `colors.xml` | W | R | R | R | R | R | R | R | R | R | W |
| `res/values/strings_p<N>.xml` | — | W1 | — | W3 | W4 | W5 | W6 | W7 | W8 | W9 | R |
| `core/model/**` | W | R | R | R | R | R | R | R | R | R | R |
| `core/time/**`, `core/util/**` | W | R | R | R | R | R | R | R | R | R | R |
| `data/db/**` (entities, DAOs, DB, migrations) | W | R | R | R | R | R | R | R | R | R | R |
| `data/prefs/SettingsDataStore.kt` | W | R | R | R | R | R | R | R | R | R | R |
| `data/repo/**` | W | R | R | R | R | R | R | R | R | R | R |
| `ui/theme/**`, `ui/components/**` | W | — | — | — | R | R | R | R | R | R | W |
| `di/AppModule.kt`, `di/DatabaseModule.kt` | W | R | R | R | R | R | R | R | R | R | R |
| `di/P<N>Module.kt` | — | W1 | W2 | W3 | W4 | W5 | W6 | W7 | W8 | W9 | R |
| `MainActivity.kt`, `QuietInboxApp.kt`, `navigation/**` | W | R | R | R | R | R | R | R | R | R | W |
| `service/**` | — | **W** | R | R | — | — | — | — | R | — | R |
| `core/signal/**` | W\*\* | R | **W** | R | — | — | — | — | — | — | R |
| `core/firewall/**` | W\*\* | R | R | **W** | — | — | — | — | — | — | R |
| `core/apps/**` | W | R | R | R | R | R | R | R | R | R | R |
| `feature/inbox/**` | — | — | — | — | **W** | — | — | — | — | — | R |
| `feature/home/**` | — | — | — | — | — | **W** | — | — | — | — | R |
| `feature/rules/**` | — | — | — | — | — | — | **W** | — | — | — | R |
| `feature/schedules/**` | — | — | — | — | — | — | — | **W** | — | — | R |
| `feature/onboarding/**`, `feature/permissions/**` | — | R | — | — | — | — | — | — | **W** | — | R |
| `feature/settings/**` | — | — | — | — | — | — | — | — | — | **W** | R |
| `data/work/**` | — | — | — | — | — | — | — | W7\* | W8\* | **W** | R |
| `test/**`, `androidTest/**` | W | W1 | W2 | W3 | W4 | W5 | W6 | W7 | W8 | W9 | W |

\* Phase 7 owns `SchedulePauseWorker.kt` only; Phase 8 owns `ListenerHealthWorker.kt` only;
Phase 9 owns everything else in `data/work/`.

\*\* Phase 0 *creates* the interfaces and the stub classes in `core/signal/` and
`core/firewall/` during Wave 1, then hands ownership over: in Wave 2 only Phases 2, 3 and 7
write there. There is no simultaneous access — Wave 1 finishes before Wave 2 starts.
`core/firewall/DefaultScheduleEvaluator.kt` belongs to Phase 7; every other file in
`core/firewall/` belongs to Phase 3.

---

## 2. Frozen contracts

Phase 0 writes these **exactly** as specified. Every other phase consumes or implements them
without changing a signature. This section is the contract that makes parallelism safe.

### 2.1 `core/model/SignalClass.kt`

```kotlin
package com.quietinbox.core.model

enum class SignalClass { ALERT, ONGOING, PROGRESS, TRANSPORT, SERVICE, GROUP_SUMMARY }
```

### 2.2 `core/model/CapturedNotification.kt`

```kotlin
package com.quietinbox.core.model

/** A parsed, engine-ready notification. Free of Android types so it is unit-testable. */
data class CapturedNotification(
    val sbnKey: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val body: String?,
    val subText: String?,
    val senderName: String?,
    val senderDigits: String?,      // digits only, may be null
    val channelId: String?,
    val androidCategory: String?,   // Notification.CATEGORY_*
    val importance: Int,
    val postedAt: Long,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val isGroupSummary: Boolean,
    val isClearable: Boolean,
    val hasProgress: Boolean,
    val groupKey: String?,
)
```

### 2.3 `core/signal/SignalClassifier.kt` — implemented by **Phase 2**

```kotlin
package com.quietinbox.core.signal

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass

sealed interface IngestDecision {
    data class Insert(val signalClass: SignalClass) : IngestDecision
    data class UpdateExisting(val rowId: Long, val bumpCount: Boolean) : IngestDecision
    data class Drop(val reason: String) : IngestDecision
}

interface SignalClassifier {
    fun classify(n: CapturedNotification): SignalClass
    fun contentHash(n: CapturedNotification): String
    /** Decides insert / update / drop given what is already stored for this key. */
    suspend fun decide(n: CapturedNotification): IngestDecision
}
```

**Wiring rule (applies to §2.3, §2.4 and §2.5).** Phase 0 creates the concrete class
`DefaultSignalClassifier` in `core/signal/` with a stub body that always returns
`Insert(ALERT)`, and binds `SignalClassifier -> DefaultSignalClassifier` in `AppModule`.
Phase 2 **fills in that class body** — it never touches the binding. The same pattern applies
to `DefaultFirewallEngine` (Phase 3) and `DefaultScheduleEvaluator` (Phase 7). No Wave-2
phase ever edits a Hilt module owned by Phase 0.

### 2.4 `core/firewall/FirewallEngine.kt` — implemented by **Phase 3**

```kotlin
package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass

enum class FirewallAction { ALLOW, SILENCE }

data class FirewallVerdict(
    val action: FirewallAction,
    val ruleId: Int,          // matches the rule numbers in design.md 5.3
    val ruleLabel: String,    // human readable, shown in the Inbox detail sheet
)

interface FirewallEngine {
    suspend fun evaluate(n: CapturedNotification, signalClass: SignalClass): FirewallVerdict
}
```

Phase 0 ships `DefaultFirewallEngine` as a stub returning `ALLOW / 9 / "Default"`, already
bound in `AppModule`. Phase 3 fills in the body.

### 2.5 `core/firewall/ScheduleEvaluator.kt` — implemented by **Phase 7**

```kotlin
package com.quietinbox.core.firewall

import com.quietinbox.data.db.entity.ScheduleEntity

data class ActiveSchedule(val schedule: ScheduleEntity, val extraAllowedApps: Set<String>)

interface ScheduleEvaluator {
    /** @param nowEpochMs wall clock. Must handle windows that cross midnight. */
    suspend fun activeAt(nowEpochMs: Long): ActiveSchedule?
}
```

Phase 0 ships `DefaultScheduleEvaluator` as a stub returning `null`, already bound in
`AppModule`. Phase 7 fills in the body.

### 2.6 `core/apps/InstalledAppsProvider.kt` — implemented by **Phase 0**

```kotlin
package com.quietinbox.core.apps

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isLaunchable: Boolean,
    val isPreinstalled: Boolean,
    val wasUpdatedSystemApp: Boolean,
    val notificationCount: Int,     // from our own DB; drives ordering
) {
    /** design.md 5.7 — Chrome must be a user app, the launcher must not. */
    val isUserFacing: Boolean get() = isLaunchable || wasUpdatedSystemApp || notificationCount > 0
    val isSystemComponent: Boolean get() = !isUserFacing && isPreinstalled
}

interface InstalledAppsProvider {
    suspend fun all(includeSystemComponents: Boolean): List<InstalledApp>
    suspend fun label(packageName: String): String
    fun iconFile(packageName: String): java.io.File
}
```

### 2.7 `service/ingest/IngestPipeline.kt` — implemented by **Phase 1**

```kotlin
package com.quietinbox.service.ingest

import com.quietinbox.core.model.CapturedNotification

interface IngestPipeline {
    /** @return the sbnKey if the caller should cancel this notification, else null. */
    suspend fun onPosted(n: CapturedNotification): String?
    suspend fun onRemoved(sbnKey: String, reason: Int)
    suspend fun backfill(active: List<CapturedNotification>)
}
```

### 2.8 `core/health/ListenerHealth.kt` — implemented by **Phase 1**, consumed by **Phase 8**

```kotlin
package com.quietinbox.core.health

import kotlinx.coroutines.flow.StateFlow

enum class HealthState { CONNECTED, NO_ACCESS, STALE, UNKNOWN }

data class HealthSnapshot(
    val state: HealthState,
    val lastCaptureAt: Long?,
    val totalCaptured: Long,
    val ingestErrors: Long,
)

interface ListenerHealth {
    val snapshot: StateFlow<HealthSnapshot>
    fun refresh()
    /** Disable + re-enable the component and requestRebind. */
    fun forceRebind()
}
```

### 2.9 DAO extension rule

Phase 0 writes `NotificationDao`, `RuleDao`, `ScheduleDao`, `StatsDao`, `AppCacheDao` with the
queries every phase needs (listed in Phase 0's task list). If a Wave-2 phase needs an extra
query it may **not** edit those files. Instead it creates its own DAO in its own package:

```kotlin
// feature/home/data/HomeStatsDao.kt   — owned by Phase 5
@Dao interface HomeStatsDao { @Query("...") fun ... }
```

and registers it in its own `di/P5Module.kt` via `QuietDatabase` — Phase 0 exposes the
`RoomDatabase` instance itself, so a phase can build extra DAOs without touching the DB class:

```kotlin
@Provides fun homeStatsDao(db: QuietDatabase): HomeStatsDao = db.homeStatsDao()
```

> **Exception, and the only one:** adding an abstract DAO getter to `QuietDatabase` *does*
> require editing a Phase-0 file. To avoid it, Phase 0 declares the getters for all five
> feature DAOs (`homeStatsDao()`, `inboxDao()`, `rulesUiDao()`, `schedulesUiDao()`,
> `settingsDao()`) up front, each returning an interface that Phase 0 creates **empty**. Each
> Wave-2 phase fills in the `@Query` methods of its own empty interface. That file is listed
> in that phase's "Owns".

### 2.10 Navigation routes (Phase 0, frozen)

```kotlin
package com.quietinbox.navigation

import kotlinx.serialization.Serializable

@Serializable data object Onboarding
@Serializable data object Home
@Serializable data object Inbox
@Serializable data class InboxFiltered(val packageName: String? = null, val day: Int? = null)
@Serializable data class NotificationDetail(val id: Long)
@Serializable data object Settings
@Serializable data object PermissionsHealth
@Serializable data object AllowRules
@Serializable data object Schedules
@Serializable data class ScheduleEdit(val id: Long = 0L)
@Serializable data object MutedApps
@Serializable data object Storage
@Serializable data object AppLock
@Serializable data object Privacy
@Serializable data object About
```

Every route is wired to a stub composable by Phase 0. A Wave-2 phase replaces only the body
of the stubs it owns.

---

## PHASE 0 — Foundation & contracts

> **WAVE 1. Run alone. Nothing else may start until this is merged and green.**
> Estimated: the largest phase. Do not rush it — nine agents depend on its correctness.

**Goal.** Delete the prototype, stand up a production skeleton that compiles, launches, and
exposes every contract and stub the other phases need.

**Owns.** Everything. This is the only phase with write access to shared files.

**Tasks**

- [ ] **0.1 Purge.** Delete `data/NotificationRepository.kt`, `data/NotificationDb.kt`,
      `ui/screens/MyNotificationScreens.kt`, `ui/viewmodel/MyNotificationViewModel.kt`,
      `.env`, `.env.example`, `app/src/test/java/com/example/*`, `assets/.aistudio/`.
      Remove the AI-Studio section from `README.md`.
- [ ] **0.2 Identity.** `namespace = "com.quietinbox"`, `applicationId = "com.quietinbox.app"`,
      `app_name = "Quiet Inbox"`, `minSdk = 26`, `versionCode = 1`, `versionName = "1.0.0"`.
      Move every source file from `com.example` to `com.quietinbox`.
- [ ] **0.3 Dependencies.** In `gradle/libs.versions.toml` **remove** retrofit, converter-moshi,
      okhttp, logging-interceptor, moshi-kotlin, moshi-kotlin-codegen, firebase-bom,
      firebase-ai, secrets-gradle-plugin, coil, camera*, play-services-location,
      accompanist-permissions. **Add** hilt-android, hilt-compiler, hilt-navigation-compose,
      androidx-datastore-preferences, androidx-work-runtime-ktx, androidx-hilt-work,
      androidx-paging-runtime, androidx-paging-compose, androidx-room-paging,
      androidx-biometric, kotlinx-serialization-json, kotlin-serialization plugin,
      turbine (test), androidx-benchmark-macro-junit4 (androidTest).
      Bump `composeBom` to the latest stable Compose BOM available at build time — do **not**
      keep `2024.09.00`. Remove the `secrets { }` block from `app/build.gradle.kts`.
- [ ] **0.4 Release config.** `isMinifyEnabled = true`, `isShrinkResources = true` for release;
      write real `proguard-rules.pro` entries for Room, Hilt, kotlinx-serialization and the
      listener service. Room: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` and
      `exportSchema = true`; check the generated JSON into git.
- [ ] **0.5 Manifest.** Declare, once and for all:
      `BIND_NOTIFICATION_LISTENER_SERVICE` (on the service), `POST_NOTIFICATIONS`,
      `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `USE_BIOMETRIC`.
      **Do not add `INTERNET`. Do not add `QUERY_ALL_PACKAGES`.**
      Add `<queries><intent><action android:name="android.intent.action.MAIN"/>
      <category android:name="android.intent.category.LAUNCHER"/></intent></queries>`.
      Declare `QuietListenerService` with the `NotificationListenerService` intent filter, and
      `BootReceiver` for `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`.
      Set `android:allowBackup="false"`, `android:enableOnBackInvokedCallback="true"`, and
      empty allow-lists in `backup_rules.xml` / `data_extraction_rules.xml`.
- [ ] **0.6 App class & DI.** `@HiltAndroidApp class QuietInboxApp`, `AppModule`,
      `DatabaseModule`, `Configuration.Provider` for Hilt-WorkManager.
- [ ] **0.7 Database.** Implement every table in design.md §6 as Room entities with the stated
      indices, the FTS4 table with `contentEntity`, `QuietDatabase` (version 1,
      `exportSchema = true`, **no** destructive fallback), and the five DAOs plus the five
      empty feature-DAO interfaces from §2.9. Required queries:
      - `NotificationDao`: `findByKey(sbnKey)`, `latestForKey(sbnKey)`,
        `insert`, `updateSession(rowId, lastSeenAt, title, body, updateCount)`,
        `markEnded(sbnKey, endedAt, reason)`, `pagingSource(filters)`,
        `searchPaging(query, digits, filters)`, `setSeen`, `setStarred`, `deleteByIds`,
        `purgeOlderThan(cutoff)`, `countOverCap()`, `countsSince(from)`.
      - `RuleDao`, `ScheduleDao` (+ `schedule_apps` join), `StatsDao` (daily rollups),
        `AppCacheDao`.
- [ ] **0.8 Settings.** `SettingsDataStore` exposing a `Flow<AppSettings>` and suspend setters
      for every key in design.md §6.
- [ ] **0.9 Contracts.** Create every file in §2 verbatim, plus stub concrete classes
      `DefaultSignalClassifier`, `DefaultFirewallEngine` and `DefaultScheduleEvaluator`,
      each bound in `AppModule`, so the app runs before Wave 2 lands. Wave-2 phases fill in
      those class bodies and never touch the bindings.
- [ ] **0.10 `InstalledAppsProvider`.** Implement it for real (design.md §5.7), including the
      per-package WebP icon cache in `filesDir/appicons/` and invalidation on
      `PACKAGE_REPLACED`. This is shared infrastructure, so it belongs here, not in Wave 2.
- [ ] **0.11 `core/time/Clock.kt`.** An injectable clock. No production code may call
      `System.currentTimeMillis()` directly — tests must be able to control time.
- [ ] **0.12 Design system.** `ui/theme/`: `Tokens.kt` (every colour, spacing, radius and type
      value from design.md §8), `Color.kt`, `Type.kt` (with `tnum` on numeric styles),
      `Shape.kt`, `Theme.kt` (dark/light, dynamic colour **off** by default, driven by
      `themeMode` from settings). Delete Purple40/Purple80.
- [ ] **0.13 Component inventory.** Build all 17 components listed in design.md §8, each with
      a `@Preview` for light and dark, and a Roborazzi screenshot test.
- [ ] **0.14 Navigation.** `navigation/Routes.kt` (§2.10), `QuietNavHost`, the 3-tab scaffold,
      type-safe args, and a **stub composable for every route** that renders its screen title
      and an "under construction" `EmptyState`. Start destination is `Onboarding` when
      `onboardingCompleted == false`, else `Home`.
- [ ] **0.15 Strings.** All shared strings in `res/values/strings.xml`. Create empty
      `strings_p1.xml` … `strings_p9.xml` so Wave-2 agents have a file to open.
- [ ] **0.16 Icon.** Replace the default launcher icon with a flat monochrome mark on the
      accent colour (adaptive icon + monochrome layer for themed icons).

**Tests** — `SettingsDataStoreTest`, `QuietDatabaseTest` (insert/read/index sanity),
`InstalledAppsProviderTest` (Robolectric: a launchable preinstalled package classifies as
user-facing; a non-launchable preinstalled one as a component), theme/component Roborazzi
baselines.

**Done when**

1. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` is green.
2. The app installs, shows onboarding, and every one of the 15 routes is reachable.
3. `grep -r "com.example" app/src` returns nothing.
4. The merged manifest contains no `INTERNET` and no `QUERY_ALL_PACKAGES`.
5. `app/schemas/1.json` is checked in.

---

## PHASE 1 — Capture pipeline

> **WAVE 2 · parallel.** The heart of the app: the part the prototype never had.

**Depends on:** §2.2, §2.3, §2.4, §2.7, §2.8, `NotificationDao`, `Clock`.
**Owns:** `service/**`, `core/health/**` (implementation),
`di/P1Module.kt`, `res/values/strings_p1.xml`, its tests.

**Tasks**

- [ ] **1.1 `QuietListenerService : NotificationListenerService`.** Hilt-injected via
      `@AndroidEntryPoint`. Implement `onListenerConnected`, `onListenerDisconnected`,
      `onNotificationPosted(sbn, rankingMap)`, `onNotificationRemoved(sbn, rankingMap, reason)`.
      All work is dispatched to a `SupervisorJob` + `Dispatchers.IO` scope; the callbacks
      themselves must return in microseconds.
- [ ] **1.2 `NotificationParser`.** Turn a `StatusBarNotification` into a
      `CapturedNotification` using the full extraction table in design.md §5.1. Handle
      `MessagingStyle` (`EXTRA_MESSAGES` → last `Person.name`), `InboxStyle`
      (`EXTRA_TEXT_LINES`), `BigTextStyle`, and `EXTRA_CONVERSATION_TITLE`.
- [ ] **1.3 Phone-number normalisation.** `senderDigits` = all digits of the sender/title when
      it parses as a number; also store the last 9 digits so `+880 1709-093872`,
      `01709093872` and `1709093872` all match. Unit-test with BD, IN, US and E.164 formats.
- [ ] **1.4 `IngestPipeline` implementation.** Order of operations, exactly:
      parse → `SignalClassifier.decide()` → write to Room → `FirewallEngine.evaluate()` →
      record the decision → return the key to cancel if the verdict is `SILENCE`.
      **The row is written before the firewall runs.** Capture must never depend on the
      delivery decision (design.md principle 2).
- [ ] **1.5 Cancellation.** When `onPosted` returns a key, call `cancelNotification(key)` on the
      service. Guard against cancelling our own package.
- [ ] **1.6 Backfill.** In `onListenerConnected`, parse `getActiveNotifications()` and run them
      through `backfill()`, which inserts anything whose `sbnKey` is unknown and does not
      apply the firewall (they were already shown).
- [ ] **1.7 Removal.** `onNotificationRemoved` sets `endedAt` and `removalReason`; marks the row
      seen when the reason is `REASON_CANCEL` / `REASON_CANCEL_ALL`; **ignores**
      `REASON_LISTENER_CANCEL` for seen-tracking, since that was us.
- [ ] **1.8 `ListenerHealth` implementation.** Track `lastCaptureAt`, `totalCaptured`,
      `ingestErrors`; derive `STALE` when connected but nothing captured in 24h and
      `NO_ACCESS` from `NotificationManagerCompat.getEnabledListenerPackages()`.
      `forceRebind()` toggles the component with `setComponentEnabledSetting` then calls
      `requestRebind`.
- [ ] **1.9 `BootReceiver`.** On `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`, call
      `requestRebind` and invalidate the icon cache for a replaced package.
- [ ] **1.10 Crash isolation.** Every ingest runs inside `runCatching`; a failure increments
      `ingestErrors` and is logged locally. One malformed notification must never kill the
      service.

**Tests** — `NotificationParserTest` (MessagingStyle, InboxStyle, BigText, missing extras,
null title); `PhoneNormalizerTest`; `IngestPipelineTest` with fakes for classifier and
firewall, asserting write-before-decide ordering; `ListenerHealthTest`.

**Done when** a real device with notification access granted records rows for every incoming
notification, survives a reboot, and `Settings → About → Diagnostics` shows a live capture
count.

---

## PHASE 2 — Signal Engine (spam & duplicate collapse)

> **WAVE 2 · parallel.** Pure Kotlin. No Android UI, no framework types.
> **This phase fixes the reported hotspot / download spam bug.**

**Depends on:** §2.1, §2.2, §2.3, `NotificationDao`, `Clock`.
**Owns:** `core/signal/**`, `di/P2Module.kt`, its tests. (No strings file — no UI.)

**Tasks**

- [ ] **2.1 `SignalTuning.kt`.** One object holding `BURST_WINDOW_MS = 10_000`,
      `MAX_EVENTS_PER_KEY_PER_HOUR = 60`, `BODY_MAX_CHARS = 4_000`. Nothing else in the
      codebase may hardcode these.
- [ ] **2.2 `classify()`.** Implement the exact table in design.md §5.2, in that priority
      order: `ONGOING` → `PROGRESS` → `TRANSPORT` → `SERVICE` → `GROUP_SUMMARY` → `ALERT`.
- [ ] **2.3 `contentHash()`.** `sha256(title + '|' + body + '|' + subText)`, null-safe,
      whitespace-normalised, truncated to `BODY_MAX_CHARS` first.
- [ ] **2.4 Session collapse.** For `ONGOING` / `PROGRESS` / `TRANSPORT` / `SERVICE`: if a row
      exists for `sbnKey` with `endedAt == null`, return
      `UpdateExisting(rowId, bumpCount = true)`. Otherwise `Insert(class)`.
      **This is the hotspot fix: 600 posts → 1 row.**
- [ ] **2.5 Group-summary drop.** Return `Drop("group summary")` for `GROUP_SUMMARY` when at
      least one non-summary row shares the `groupKey` within the last 5 minutes; otherwise
      treat it as `ALERT` (some apps post only the summary).
- [ ] **2.6 Identical-content suppression.** Same `sbnKey` + same `contentHash` as the latest
      row → `UpdateExisting(rowId, bumpCount = true)`.
- [ ] **2.7 Burst coalescing.** Same `sbnKey`, class `ALERT`, latest row newer than
      `now - BURST_WINDOW_MS` → `UpdateExisting(rowId, bumpCount = true)` with refreshed
      title/body.
- [ ] **2.8 Rate ceiling.** More than `MAX_EVENTS_PER_KEY_PER_HOUR` inserts for one key inside
      one hour → force session behaviour for the remainder of the hour and set
      `wasRateLimited = true` on the row.
- [ ] **2.9 No wiring changes.** Phase 0 already bound `SignalClassifier` to
      `DefaultSignalClassifier`. You fill in that class's body. Do not create a binding, do
      not touch `AppModule.kt`. `di/P2Module.kt` exists only if you need to provide an extra
      internal dependency of your own.

**Tests** — this phase carries the project's most important test:

- [ ] `HotspotSpamTest` — feed 600 `CapturedNotification`s with the same `sbnKey`,
      `isOngoing = true`, changing body ("3.09 MB" → "3.10 MB" …) across a simulated
      10 minutes. **Assert exactly 1 insert and 599 updates.**
- [ ] `DownloadProgressTest` — 400 progress ticks, same key. **Assert exactly 1 insert.**
- [ ] `MessageBurstTest` — "typing…", "1 new message", "2 new messages" within 10s → 1 row;
      the same three 60s apart → 3 rows.
- [ ] `GroupSummaryTest`, `ContentHashTest`, `RateCeilingTest`, `ClassifyTableTest` (one case
      per row of the design.md §5.2 table).

**Done when** the two spam tests pass and coverage of `core/signal` is ≥ 90%.

---

## PHASE 3 — Firewall & rule matching

> **WAVE 2 · parallel.** Pure Kotlin decision logic.

**Depends on:** §2.2, §2.4, §2.5, `RuleDao`, `SettingsDataStore`, `Clock`.
**Owns:** `core/firewall/**` (except `ScheduleEvaluator` implementation, which is Phase 7),
`di/P3Module.kt`, `res/values/strings_p3.xml` (rule labels), its tests.

**Tasks**

- [ ] **3.1 `FirewallEngine` implementation.** The nine-rule ladder in design.md §5.3,
      evaluated in order, first match wins. Each branch returns the correct `ruleId` and a
      human-readable `ruleLabel` from `strings_p3.xml`.
- [ ] **3.2 Safety floor (rules 0–1).** Our own package, `CATEGORY_ALARM`, `CATEGORY_CALL`, and
      an ongoing telecom notification are **always** `ALLOW` and are not user-configurable.
      Write a test that proves no setting can silence an alarm.
- [ ] **3.3 System & media pass-through (rule 2).** Honour `leaveSystemAndMediaAlone`.
- [ ] **3.4 Pause (rule 3).** Read `pausedUntilEpochMs` and compare against `Clock`. Expired
      pauses must self-clear.
- [ ] **3.5 `RuleMatcher` (rule 4).** Match `AllowRule`s of type `APP` (exact package),
      `SENDER` (case/diacritic-insensitive contains, plus digit-tail match against
      `senderDigits`), and `WORD` (whole-word by default, `CONTAINS` when the rule says so).
      Matching runs against title + body + subText + senderName.
- [ ] **3.6 OTP break-through (rule 5).** Detect a standalone 4–8 digit token plus an OTP word
      from the multilingual set in design.md §5.6. **Never extract or persist the code.**
      Handle Android 15+ redacted bodies gracefully — a redacted body must not throw and must
      still allow the notification through when the title/app suggests a code.
- [ ] **3.7 Schedules (rule 6).** Delegate to the injected `ScheduleEvaluator`. Phase 7 supplies
      the real one; your code must work against Phase 0's null-returning stub.
- [ ] **3.8 Muted apps (rule 7) and Quiet Mode (rule 8).**
- [ ] **3.9 Decision log.** Write every verdict to `firewall_decisions`. Batch writes so a
      notification storm cannot cause per-row I/O.
- [ ] **3.10 No wiring changes.** Fill in `DefaultFirewallEngine`, which Phase 0 already
      bound. `di/P3Module.kt` is only for extra internal dependencies of your own.

**Tests** — `FirewallLadderTest` with one case per rule and one case proving each rule's
precedence over the next; `RuleMatcherTest` (Bangla and English sender names, `+880` numbers,
partial digits, word boundaries); `OtpDetectionTest` (positive, negative, redacted body);
`SafetyFloorTest` (alarm survives Quiet Mode + muted app + active schedule).

**Done when** the ladder is exhaustively tested and no path can silence an alarm or a call.

---

## PHASE 4 — Inbox

> **WAVE 2 · parallel.**

**Depends on:** `NotificationDao` paging queries, `InboxDao` (the empty interface Phase 0
created for you), `InstalledAppsProvider`, `ui/components/**`.
**Owns:** `feature/inbox/**` including `feature/inbox/data/InboxDao.kt`, `di/P4Module.kt`,
`res/values/strings_p4.xml`, its tests.

**Tasks**

- [ ] **4.1 `InboxViewModel`.** Expose `PagingData<InboxItem>` combining the query, the state
      filter (All/Unseen/Seen/Starred), the app filter, and an optional day filter from the
      heatmap deep link.
- [ ] **4.2 Search.** Debounce 250ms. Route to FTS for text; when the query contains ≥4 digits
      after stripping separators, additionally match `senderDigits`. Union and de-duplicate.
      Highlight matches in the row.
- [ ] **4.3 Day grouping.** Sticky headers: *Today*, *Yesterday*, then `d MMM`. Use the user's
      locale and time zone; recompute on `ACTION_TIMEZONE_CHANGED`.
- [ ] **4.4 `NotificationRow` binding.** App icon (from `InstalledAppsProvider.iconFile`),
      app name, title, one body line, relative time, unread dot, silenced badge.
- [ ] **4.5 Session rows.** For `ONGOING`/`PROGRESS`, render the range and update count instead
      of a single timestamp — *"10:17 PM – 10:27 PM · 10 min · 612 updates"*. This is where
      the Phase 2 fix becomes visible; get the copy right.
- [ ] **4.6 Gestures.** Swipe right = star, swipe left = delete with an undo snackbar,
      long-press = multi-select with a contextual top bar (mark seen · star · delete ·
      mute this app).
- [ ] **4.7 Detail sheet.** Full body, app, channel, category, time, the firewall verdict and
      its `ruleLabel`, and actions: **Copy code** (only when a code pattern is present,
      computed at render time), **Open app**, **Mute this app**, **Always allow this sender**.
      Opening a row marks it seen.
- [ ] **4.8 Empty states.** Distinct copy for: no notification access (with fix button),
      access granted but nothing captured yet, no results for a search, no results for a
      filter (with "clear filters").
- [ ] **4.9 Transport rows.** Hidden unless `showTransportInInbox` is on.

**Tests** — `InboxViewModelTest` (filter × search matrix with a fake DAO);
`SearchNormalizationTest` (`+880 1709-093872` found by `1709093872` and by `093872`);
Roborazzi screenshots for row, session row, empty states, dark + light + 200% font scale.

**Done when** 50,000 seeded rows scroll smoothly and every filter/search combination returns
correct results.

---

## PHASE 5 — Home & statistics

> **WAVE 2 · parallel.**

**Depends on:** `StatsDao`, `HomeStatsDao` (your empty interface), `SettingsDataStore`,
`ListenerHealth`, `ui/components/**`.
**Owns:** `feature/home/**` including `feature/home/data/HomeStatsDao.kt`, `di/P5Module.kt`,
`res/values/strings_p5.xml`, its tests.

**Tasks**

- [ ] **5.1 `QuietSwitch`.** 56dp, unmissable, with "On since 9:14 AM" / "Off" subtitle. Writes
      `quietModeEnabled` to DataStore — it must survive process death (fixes audit A11).
- [ ] **5.2 `PauseChipRow`.** 5 min · 15 min · 1 hour · Until I turn it back on. Sets
      `pausedUntilEpochMs`. While paused, the switch shows a countdown and a "Resume now"
      action.
- [ ] **5.3 `HealthBanner`.** Bind `ListenerHealth.snapshot`: green *Connected · capturing*,
      amber *Connected, but nothing captured in 24h*, red *Notification access is off*. Each
      non-green state has a one-tap fix that navigates to `PermissionsHealth`.
- [ ] **5.4 `SegmentedRange`.** All · 30d · 7d, persisted across launches.
- [ ] **5.5 Stat tiles.** All nine metrics in design.md §7, computed by SQL aggregate — never
      by loading rows into memory. Numbers use `tnum` so they do not jitter while updating.
- [ ] **5.6 Streaks.** A *quiet day* = Quiet Mode on for ≥ 4h that local day. Compute from
      `daily_stats.quietMinutes`. Current streak counts back from today; longest is all-time.
- [ ] **5.7 `ActivityHeatmap`.** 7 rows × up to 26 columns, 5 intensity buckets from the
      range's 90th percentile, accent-derived ramp. Tapping a cell navigates to
      `InboxFiltered(day = …)`. Must have a content description per cell
      (*"24 August, 41 silenced"*).
- [ ] **5.8 Focus-reclaimed estimate.** Shown only when `silenced >= 50`. Always renders its
      assumption. `StatsTuning.SECONDS_PER_INTERRUPTION = 8`, user-adjustable to 4/8/15 and
      hideable. Do not use the 23-minute figure.
- [ ] **5.9 Noisiest apps.** Top 5 by capture count in range, horizontal bars, each with an
      inline **Mute** action that writes to `muted_apps` without navigating away.
- [ ] **5.10 `daily_stats` maintenance.** Read only. Phase 9's `RetentionWorker` rebuilds it;
      until that lands, compute on the fly and cache in memory.

**Tests** — `StreakCalculatorTest` (gaps, today-only, a 13-day run, DST transition);
`HeatmapBucketingTest`; `StatsQueryTest` against an in-memory DB; Roborazzi of the whole Home
screen in dark, light, and 200% font scale.

**Done when** Home renders correct numbers for a seeded 90-day dataset and the heatmap deep
link filters the Inbox to the right day.

---

## PHASE 6 — Always-allow rules, app picker & muted apps

> **WAVE 2 · parallel.**

**Depends on:** `RuleDao`, `InstalledAppsProvider`, `AppPickerSheet`.
**Owns:** `feature/rules/**` including `feature/rules/data/RulesUiDao.kt`, `di/P6Module.kt`,
`res/values/strings_p6.xml`, its tests.

**Tasks**

- [ ] **6.1 Always-allow screen.** Three sections — **Apps**, **People & numbers**, **Words** —
      each a list with an add button and swipe-to-delete. Plain-language empty states
      ("Nobody breaks through yet. Add the people who matter.").
- [ ] **6.2 App picker sheet.** Backed by `InstalledAppsProvider`. Search field, A→Z with
      "apps that notify you most" pinned on top, multi-select with checkboxes, app icons.
- [ ] **6.3 The system-app switch.** A single **"Show system components"** toggle, default
      **off**. It must behave exactly as design.md §5.7 specifies: Chrome and other
      preinstalled-but-launchable apps appear with the switch **off**; the launcher, SystemUI
      and carrier stubs only appear with it **on**; any package that has actually notified the
      user always appears. Write an explicit test for the Chrome case.
- [ ] **6.4 People & numbers.** Free-text entry that accepts a name or a phone number. Numbers
      are normalised on save (design.md §5.4) and shown back in the format the user typed.
      A "recent senders" list offers one-tap add from real captured data.
- [ ] **6.5 Words.** Whole-word by default, with a *"match anywhere in the text"* switch per
      rule expressed in plain language, not as "wildcard".
- [ ] **6.6 Muted apps screen.** The inverse list: apps silenced even when Quiet Mode is off.
      Reachable from Settings and from the Inbox row action.
- [ ] **6.7 Live preview.** Each rule row shows *"matched 14 times in the last 7 days"*, so the
      user can tell a rule is working. Computed by one aggregate query.

**Tests** — `AppClassificationTest` (Chrome-style package = user app; launcher-style = system
component; notified-but-unlaunchable = always shown); `RuleCrudTest`;
`NumberNormalizationRoundTripTest`; Roborazzi of the picker with the toggle on and off.

**Done when** the picker shows a sane app list on a real device with the toggle off, and
Chrome is in it.

---

## PHASE 7 — Schedules

> **WAVE 2 · parallel.** Also supplies the real `ScheduleEvaluator` that Phase 3 consumes.

**Depends on:** §2.5, `ScheduleDao`, `schedule_apps`, `AppPickerSheet`, `TimeRangePicker`.
**Owns:** `feature/schedules/**` including `feature/schedules/data/SchedulesUiDao.kt`,
`core/firewall/DefaultScheduleEvaluator.kt` (**only** this file inside `core/firewall`),
`data/work/SchedulePauseWorker.kt`, `di/P7Module.kt`, `res/values/strings_p7.xml`, its tests.

**Tasks**

- [ ] **7.1 `DefaultScheduleEvaluator`.** Minute-of-day comparison with modulo-1440 handling so
      a 22:00 → 07:00 window works. Day-of-week bitmask is evaluated against the **start** day
      for overnight windows. Uses `Clock`, never `System.currentTimeMillis()`.
- [ ] **7.2 Precedence.** When several schedules are active, `OPEN` wins over `QUIET` — an
      explicit "let everything through" window must beat an overlapping quiet window. Test it.
- [ ] **7.3 List screen.** Each schedule as a card: name, time range, day pills, policy, an
      enable switch, and a live *"Active now"* badge.
- [ ] **7.4 Edit screen.** Name, start/end via `TimeRangePicker`, day-of-week pills, policy
      segmented control (**Quiet** / **Let everything through**), and — the requirement from
      the brief — a per-schedule **extra allowed apps** picker writing to `schedule_apps`,
      reusing Phase 6's `AppPickerSheet` with the same system-components toggle.
- [ ] **7.5 Presets.** Three one-tap starters, created only on the user's tap, never seeded:
      **Sleep** 22:00–07:00 daily · **Work** 09:00–18:00 Sun–Thu · **Prayer** a short window.
      Presets are ordinary editable schedules, not a separate concept.
- [ ] **7.6 `SchedulePauseWorker`.** A one-shot worker whose only job is to refresh the UI and
      the status notification when a window opens or closes. Correctness never depends on it —
      the evaluator reads the clock (design.md §5.5), so no exact-alarm permission is needed.
- [ ] **7.7 Overlap warning.** When a new schedule overlaps an existing one, show an inline
      explanation of which will win. Do not block saving.

**Tests** — `ScheduleEvaluatorTest`: overnight window, exact boundary minutes, day mask,
DST spring-forward and fall-back, overlapping `OPEN` vs `QUIET`, disabled schedule ignored.
Roborazzi of list and edit screens.

**Done when** a 22:00–07:00 quiet schedule silences at 23:30 and at 06:30 but not at 08:00,
proven by test.

---

## PHASE 8 — Onboarding, permissions & reliability

> **WAVE 2 · parallel.** This phase decides whether the app still works next month.

**Depends on:** §2.8 `ListenerHealth`, `PermissionCard`, `HealthBanner`, `SettingsDataStore`.
**Owns:** `feature/onboarding/**`, `feature/permissions/**`,
`data/work/ListenerHealthWorker.kt`, `di/P8Module.kt`, `res/values/strings_p8.xml`, its tests.

**Tasks**

- [ ] **8.1 Onboarding, four screens** exactly as design.md §9.1. No horizontal pager with ten
      slides — four screens, a visible skip after step 2, and no fake "loading" delays.
- [ ] **8.2 Prominent disclosure.** Step 2 states, before the permission request, what is
      accessed and that it never leaves the device. This is a Google Play requirement for
      `BIND_NOTIFICATION_LISTENER_SERVICE`, not a nicety. Wording is a release gate.
- [ ] **8.3 Live permission detection.** Poll
      `NotificationManagerCompat.getEnabledListenerPackages()` on `ON_RESUME` and advance
      automatically when access is granted — never make the user press "I did it".
- [ ] **8.4 "Show me how".** An expander with per-OEM wording for Samsung, Xiaomi/Redmi/POCO,
      Oppo/Realme/OnePlus, Vivo, Huawei and stock, selected from `Build.MANUFACTURER`, with a
      stock fallback. Text only — no screenshots to maintain.
- [ ] **8.5 Step 3 defaults.** Pre-select Phone, Messages and the default clock/alarm app,
      resolved by intent, not by hardcoded package names.
- [ ] **8.6 Permissions & health screen.** The full table in design.md §9.4: every item, its
      live state, one line of why, and a working fix button. Includes battery-optimisation
      exemption and the OEM autostart intent (best-effort, with written steps when the intent
      is unavailable).
- [ ] **8.7 `ListenerHealthWorker`.** Periodic, every 6h, `requiresBatteryNotLow`. If the
      listener is not connected, call `ListenerHealth.forceRebind()`. Log each attempt to the
      local diagnostics counter.
- [ ] **8.8 OEM aggressiveness notice.** Detect the known-aggressive manufacturers and show the
      battery card once, dismissible, never nagging.
- [ ] **8.9 Re-run onboarding** from Settings → About.

**Tests** — `OnboardingFlowTest` (Compose UI: cannot pass step 2 without access; auto-advances
when granted); `OemGuidanceTest` (every manufacturer resolves to non-empty guidance, unknown
falls back to stock); `ListenerHealthWorkerTest` with a fake health source.

**Done when** a fresh install on a real Xiaomi or Samsung device reaches Home with capture
working, guided only by the in-app text.

---

## PHASE 9 — Settings, retention, export, app lock, privacy

> **WAVE 2 · parallel.**

**Depends on:** `SettingsDataStore`, `NotificationDao` purge queries, `StatsDao`.
**Owns:** `feature/settings/**` including `feature/settings/data/SettingsDao.kt`,
`data/work/**` except `SchedulePauseWorker.kt` and `ListenerHealthWorker.kt`,
`di/P9Module.kt`, `res/values/strings_p9.xml`, its tests.

**Tasks**

- [ ] **9.1 Settings root.** A flat list of the eight rows in design.md §9.4. No nested
      accordions, no search field, no icons-only rows.
- [ ] **9.2 Storage & retention.** Show the real DB size and row count. Retention picker
      30 / 90 / 365 / Forever, defaulting to 90. Explain in one plain sentence that starred
      items are never deleted. "Delete all history" behind a typed confirmation.
- [ ] **9.3 `RetentionWorker`.** Daily, flex 6h, `requiresBatteryNotLow`. Purges beyond the
      retention window, enforces the 50,000-row cap by dropping the oldest unstarred rows,
      never touches starred rows, then rebuilds `daily_stats` for changed days. Runs `VACUUM`
      at most once a week.
- [ ] **9.4 Export.** JSON and CSV to a user-chosen location via `ACTION_CREATE_DOCUMENT`. The
      app writes to the URI the user picked and nothing else. Streamed, so a 50,000-row export
      does not allocate the whole file in memory. Import is **not** in v1.0.
- [ ] **9.5 App lock.** `androidx.biometric`, off by default. When on, require auth on cold
      start and after 60s in the background. A `FLAG_SECURE` option for the Inbox.
- [ ] **9.6 Appearance.** Theme mode (System / Light / Dark) and a dynamic-colour switch,
      default off.
- [ ] **9.7 Privacy screen.** Plain-language, honest: what is stored, where, that it never
      leaves the device, that the app ships without the internet permission and how to verify
      that, and the Android 15+ OTP-redaction caveat.
- [ ] **9.8 About & diagnostics.** Version, licences (`OssLicensesMenuActivity` is *not*
      allowed — it pulls Play Services; generate a static list at build time instead),
      re-run onboarding, and local diagnostics: captured count, ingest errors, last rebind,
      DB size.
- [ ] **9.9 `DigestWorker`.** Optional, off by default: one summary notification at chosen
      times — *"38 notifications while you were focused. 2 from people you allow."* Uses our
      own channel and requires `POST_NOTIFICATIONS`. This is the one notification the app is
      allowed to post.

**Tests** — `RetentionWorkerTest` (starred survive; cap enforced; `daily_stats` rebuilt);
`ExportTest` (streamed round-trip of 10,000 rows, correct CSV escaping);
`AppLockTest` (Robolectric); Roborazzi of every settings screen.

**Done when** retention actually prunes, export produces a valid file, and no Play Services
dependency has crept in.

---

## PHASE 10 — Integration, hardening & release

> **WAVE 3. Run alone, after every Wave-2 phase is merged.**

**Owns:** everything. This phase may edit shared files again.

**Tasks**

- [ ] **10.1 Confirm the stubs are gone.** `DefaultSignalClassifier`,
      `DefaultFirewallEngine` and `DefaultScheduleEvaluator` must all contain real logic, not
      Phase 0's placeholder returns. Grep for the placeholder strings and fail the phase if
      any survive.
- [ ] **10.2 Cross-phase seams.** Verify the heatmap → Inbox deep link, the Inbox "mute this
      app" → Phase 6 store, the "always allow this sender" → Phase 3 matcher, and the health
      banner → permissions screen.
- [ ] **10.3 Copy pass.** One person's voice across every string. Remove every trace of
      "vault", "master block", "smart shield", "intercept". Verify against the plain-language
      principle in design.md §3.
- [ ] **10.4 Delete dead code.** Any remaining simulator, seeded demo data, unused entity
      column, or commented-out dependency. `grep -ri "simulat\|seed\|demo\|TODO\|FIXME"` must
      come back clean or justified.
- [ ] **10.5 Performance.** Macrobenchmark: Inbox scroll with 50,000 rows, no frame > 16ms;
      cold start < 800ms on a Pixel 6a. Generate and check in a Baseline Profile.
- [ ] **10.6 Accessibility.** TalkBack walkthrough of all three tabs; 200% font scale and
      *largest* display size on every screen; RTL pass; contrast audit of the heatmap ramp.
- [ ] **10.7 The no-internet assertion.** An automated test that parses the **merged** manifest
      and fails if `android.permission.INTERNET` or `QUERY_ALL_PACKAGES` is present. This test
      protects the product's main marketing claim; it must run in CI.
- [ ] **10.8 Endurance.** Install on a real device, use it for 72h, then verify: listener still
      connected, capture count sane, no ANR, battery impact not visible in Settings, DB size
      proportionate.
- [ ] **10.9 Reboot & update matrix.** reboot · force-stop then notify · app update ·
      notification access revoked and re-granted · 48h idle in Doze.
- [ ] **10.10 Release build.** R8 + resource shrinking on, verify no reflection breakage in
      Room/Hilt/serialization, check the mapping file in, measure the APK (target < 8 MB).
- [ ] **10.11 Room migration test.** 1 → current with data intact, using the checked-in schema
      JSON.
- [ ] **10.12 Play readiness.** Data-safety answers (*notification content: processed on
      device, not collected, not shared*), the sensitive-permission justification, store
      listing copy that leads with filtering and history so the permission is demonstrably
      core functionality, screenshots, and a privacy policy URL.
- [ ] **10.13 Quality gates.** Every one of the ten gates in design.md §13, checked off
      individually in the final report.

**Done when** all ten quality gates pass and a signed release APK is installed and used for
72 hours without a single failure.

---

## 3. Progress board

| Wave | Phase | Title | Owner conversation | Status |
|:-:|:-:|---|---|---|
| 1 | 0 | Foundation & contracts | | ☐ |
| 2 | 1 | Capture pipeline | | ☐ |
| 2 | 2 | Signal Engine (spam fix) | | ☐ |
| 2 | 3 | Firewall & rules | | ☐ |
| 2 | 4 | Inbox | | ☐ |
| 2 | 5 | Home & statistics | | ☐ |
| 2 | 6 | Allow rules & app picker | | ☐ |
| 2 | 7 | Schedules | | ☐ |
| 2 | 8 | Onboarding & reliability | | ☐ |
| 2 | 9 | Settings & retention | | ☐ |
| 3 | 10 | Integration & release | | ☐ |

## 4. If an agent gets stuck

| Symptom | What it means | Action |
|---|---|---|
| "I need a dependency that isn't declared" | Phase 0 missed it | Stop. Report it. Do not edit Gradle. It goes into a Phase 0 patch. |
| "I need to change a frozen contract" | Two phases disagree | Stop. Report it. Changing a signature mid-wave breaks every other agent. |
| "The build fails in a file I don't own" | Another agent's work-in-progress | Stop and report. Never fix another phase's file. |
| "design.md doesn't specify X" | Genuine gap | Choose what best fits design.md §3, implement it, and list the choice in your final report. |
