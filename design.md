# Quiet Inbox — Product & Engineering Design

> **Status:** v1.0 design baseline · **Owner:** MK Shaon · **Last updated:** 2026-08-26
> **Scope:** Android-only, 100% offline, no account, no backend, no payments.
> This document is the single source of truth. `TASKS.md` implements it. When code and this
> document disagree, this document wins — change the document first, then the code.

---

## 0. Naming placeholder (decide once, in Phase 0, nowhere else)

| Key | Value | Note |
|---|---|---|
| Product name | **Quiet Inbox** | PLACEHOLDER — change only in `res/values/strings.xml` |
| `namespace` | `com.quietinbox` | PLACEHOLDER — change only in `app/build.gradle.kts` |
| `applicationId` | `com.quietinbox.app` | PLACEHOLDER — must be final before first Play upload |

Everything else in this document is a real decision, not a placeholder.

---

## 1. The problem, stated honestly

A modern Android phone posts 60–150 notifications a day. Almost none of them matter.
Two bad options exist today:

1. **Leave notifications on** → constant interruption, no deep work.
2. **Turn notifications off / Do Not Disturb** → peace, but now you *fear* you missed
   something: an OTP, a delivery code, a message from one person who actually matters.

Nobody chooses (2) for long, because **silence without a safety net is scarier than noise.**

### The offer that is hard to refuse

> **Your phone stops buzzing. Nothing gets lost.**

Quiet Inbox silences everything *by default*, captures **100% of it** to a private, searchable
inbox on the device, and lets exactly the things you named — a person, a number, a word,
an app — break through. You are never trading safety for silence. That trade is the whole
product.

### Why the user comes back every day

- **Recovery:** "What was that OTP I swiped away?" → search `otp` → found. Android's own
  notification history is off by default and only keeps 24 hours.
- **Proof:** a dashboard showing *"we stopped 1,284 interruptions for you this month"*
  turns an invisible benefit into a visible one. Invisible benefits get uninstalled.
- **Trust:** the app ships **without the `INTERNET` permission**. It is physically incapable
  of sending a single notification anywhere. That claim is verifiable by anyone who opens
  the APK, and no competitor that runs ads or syncs can make it.

### Non-goals for v1.0

Payments, subscriptions, accounts, cloud sync, cross-device, AI/LLM features, Wear OS,
widgets, Tasker-style scripting, plugins, themes marketplace. All deliberately excluded.

---

## 2. Current state (audit of the existing codebase)

The repository today is an AI-Studio prototype. It looks like the product but does not do
any of it. Findings, severity-ordered:

| # | Finding | Evidence | Impact |
|---|---|---|---|
| A1 | **The app cannot read a single real notification.** There is no `NotificationListenerService`, and `AndroidManifest.xml` declares zero permissions, services or receivers. | `app/src/main/AndroidManifest.xml` | Blocker — the core product does not exist |
| A2 | The "engine" is `processSimulatedNotification(...)`, driven only by a UI simulator dialog. All blocking/allowing is fake. | `data/NotificationRepository.kt:236` | Blocker |
| A3 | The DB is pre-seeded on first launch with **fake demo notifications and 13 hardcoded apps** (Instagram, Slack…) presented to the user as real data. | `NotificationRepository.seedDatabaseIfNeeded()` | Ship-stopper: fabricated user data |
| A4 | `fallbackToDestructiveMigration()` with `version = 1`, `exportSchema = false`. | `data/NotificationDb.kt` | Every schema change wipes the user's entire archive |
| A5 | `allowBackup="true"` on an app whose database is a verbatim copy of every private message the user receives. | manifest | Serious privacy leak to cloud backup |
| A6 | OTP codes stored in a plaintext column (`otpCode`). Also unaware that **Android 15+ redacts OTP notifications from untrusted listeners**. | `SavedNotificationEntity` | Privacy + a feature that silently breaks on modern Android |
| A7 | Retrofit + OkHttp + Moshi + Firebase BOM + `GEMINI_API_KEY`/secrets plugin are wired into an app that must be 100% offline. | `app/build.gradle.kts`, `.env.example` | Dead weight, and it destroys the "no INTERNET" trust claim |
| A8 | No dedupe, no rate limiting. `maxPerHour` and `cooldownMinutes` columns exist and are never read. | `BlockedAppEntity` | This is exactly the hotspot/download spam bug reported |
| A9 | No installed-app enumeration. The app list is a hardcoded array of 13 packages. | `NotificationRepository` | Unusable on a real device |
| A10 | Schedules are stored but nothing ever evaluates them. Dead feature. | `ScheduleEntity` | Advertised, non-functional |
| A11 | `masterBlockEnabled` is an in-memory `MutableStateFlow` — lost on process death. | `MyNotificationViewModel:12` | Setting silently resets |
| A12 | One 2,342-line screen file, one 283-line god ViewModel, repository constructed by hand inside the ViewModel, no DI, DB instance per ViewModel. | `ui/screens/MyNotificationScreens.kt` | Cannot be worked on in parallel; untestable |
| A13 | Denormalized CSV columns (`allowedPackages = "com.a,com.b"`). | `FocusModeEntity` | No indexing, no integrity, O(n) string parsing per notification |
| A14 | Default Compose template theme (Purple40/Purple80) + dynamic color on. | `ui/theme/` | No brand, no design system |
| A15 | Five overlapping concepts in the nav bar: Dashboard / Vault / Focus Modes / App Blocker / Smart Shield. | `MyNotificationAppView` | Too complex for the stated audience (non-technical and older users) |
| A16 | `isMinifyEnabled = false` for release; tests are the untouched template `ExampleUnitTest`. | `app/build.gradle.kts` | Not release-grade |

**Verdict:** keep the *idea* and the Room/Compose stack choice. Replace everything else.
Phase 0 rebuilds the foundation; nothing from `NotificationRepository.kt`,
`MyNotificationScreens.kt`, or `MyNotificationViewModel.kt` survives.

---

## 3. Product principles

1. **Zero-configuration default.** Grant permission → capture starts. The user is never asked
   to pick apps before the app is useful.
2. **Capture is unconditional.** Blocked, allowed, silenced, snoozed — every notification is
   stored. There is no state in which the app is silently discarding data.
3. **Three concepts, not eight.** *Quiet Mode*, *Always allow*, *Schedules*. Nothing else is a
   top-level idea. (`Focus modes` from the old app are folded into *Schedules*.)
4. **Plain words.** "Quiet Mode", not "Master Block Toggle". "Always allow", not "Whitelist".
   "Inbox", not "Vault". Nothing in the UI requires knowing what a package name is.
5. **Never leave the device.** No `INTERNET` permission. No analytics. No crash SDK.
6. **Honest numbers.** Every derived statistic states its assumption. We never invent
   "hours saved" from thin air.
7. **Reversible.** Every destructive action confirms; deletion is the only unrecoverable one.

---

## 4. Information architecture

Three bottom-nav destinations. Maximum depth from any tab: **2**.

```
┌─ Home ──────────────┬─ Inbox ─────────────┬─ Settings ──────────┐
│ Quiet Mode switch   │ Search              │ Permissions & health│
│ Pause chips         │ Filters             │ Always allow        │
│ Listener health     │   All / Unseen      │   Apps              │
│ Today at a glance   │   Seen / Starred    │   People & numbers  │
│ Range: All/30d/7d   │ Grouped by day      │   Words             │
│ Stat tiles          │ Row → Detail        │ Schedules           │
│ Activity heatmap    │                     │ Muted apps          │
│ Noisiest apps       │                     │ Storage & retention │
│ Streak              │                     │ App lock            │
└─────────────────────┴─────────────────────┴─ Privacy / About ───┘
```

Every screen below the tab bar is reached by one tap from its tab and returns with the
system back gesture. No hamburger menu, no nested tabs, no horizontal pagers.

---

## 5. Core engines

Three pure, independently testable engines sit between the OS and the database.

```
StatusBarNotification
        │
        ▼
┌───────────────────┐   parse extras, sender, phone digits, icons
│ 1. Capture        │
└─────────┬─────────┘
          ▼
┌───────────────────┐   is this new information, or the same thing again?
│ 2. Signal Engine  │───► NEW · UPDATE · COLLAPSE · DROP
└─────────┬─────────┘
          ▼
┌───────────────────┐   should the user be interrupted right now?
│ 3. Firewall       │───► ALLOW · SILENCE(cancel) · SNOOZE
└─────────┬─────────┘
          ▼
   Room (always written)  +  optional cancelNotification(key)
```

### 5.1 Capture

Extracted per notification, from `StatusBarNotification` / `Notification.extras`:

| Field | Source |
|---|---|
| `sbnKey` | `sbn.key` — stable across updates of the same notification |
| `packageName`, `postedAt` | `sbn.packageName`, `sbn.postTime` |
| `title` | `EXTRA_TITLE`, fallback `EXTRA_TITLE_BIG` |
| `body` | `EXTRA_BIG_TEXT` → `EXTRA_TEXT` → `EXTRA_TEXT_LINES.join` |
| `subText`, `summary` | `EXTRA_SUB_TEXT`, `EXTRA_SUMMARY_TEXT` |
| `senderName` | `MessagingStyle` → last `Message.person.name`; else `EXTRA_CONVERSATION_TITLE`; else `EXTRA_TITLE` |
| `senderDigits` | all digits of `senderName`/`title` if it parses as a phone number, normalized (see 5.4) |
| `channelId`, `importance` | `sbn.notification.channelId`, `RankingMap.getRanking(key).importance` |
| `category` | `notification.category` (`CATEGORY_MESSAGE`, `_CALL`, `_ALARM`, `_TRANSPORT`, `_PROGRESS`, `_SERVICE`, …) |
| `flags` | `FLAG_ONGOING_EVENT`, `FLAG_FOREGROUND_SERVICE`, `FLAG_GROUP_SUMMARY`, `FLAG_AUTO_CANCEL` |
| `isClearable` | `sbn.isClearable` |
| `progressMax` | `EXTRA_PROGRESS_MAX`, `EXTRA_PROGRESS_INDETERMINATE` |

App icons are **never** stored per row. They are cached once per package as a WebP file in
`filesDir/appicons/<package>.webp` and invalidated on `PACKAGE_REPLACED`.

`onListenerConnected()` calls `getActiveNotifications()` and backfills anything posted while
the service was unbound.

### 5.2 Signal Engine — the fix for notification spam

**The reported bug:** turning on the mobile hotspot for ten minutes produced hundreds of
identical "1 device is connected… 3.10 MB" rows. A download produced one row per progress
tick. This is not a display bug — it is a missing concept. Android *updates* those
notifications; the prototype recorded every update as a new event.

Every incoming notification is classified into exactly one `SignalClass`:

| Class | Detected by | Storage behaviour |
|---|---|---|
| `ONGOING` | `FLAG_ONGOING_EVENT` or `FLAG_FOREGROUND_SERVICE` or `!isClearable` | **Session**: one row, updated in place |
| `PROGRESS` | `progressMax > 0` or `progressIndeterminate` or `CATEGORY_PROGRESS` | **Session** |
| `TRANSPORT` | `CATEGORY_TRANSPORT` (media players) | **Session**, hidden from Inbox by default |
| `SERVICE` | `CATEGORY_SERVICE`, `CATEGORY_STATUS`, `CATEGORY_SYSTEM`, `CATEGORY_NAVIGATION`, `CATEGORY_LOCATION_SHARING` | **Session** |
| `GROUP_SUMMARY` | `FLAG_GROUP_SUMMARY` and ≥1 child with the same `groupKey` | **Dropped** (the children carry the content) |
| `ALERT` | everything else | **Event**: normal row |

**Session rows.** A session row is keyed by `sbnKey`. On every subsequent post with the same
key the engine writes `lastSeenAt = now`, `updateCount++`, and refreshes `title`/`body` —
it never inserts. When `onNotificationRemoved` fires for that key, `endedAt = now`. The Inbox
renders it as one item:

> **Hotspot on** · Android System
> 10:17 PM – 10:27 PM · 10 min · 1 device · 3.1 MB

Ten minutes of hotspot = **one row**, not six hundred. A 400 MB download = **one row** that
shows the final state.

**Event rows** get three further guards, applied in order:

1. **Identical-content suppression.** `contentHash = sha256(title|body|subText)`. Same
   `sbnKey` + same `contentHash` → `updateCount++` on the existing row, no insert.
2. **Burst coalescing.** Same `sbnKey`, class `ALERT`, new post within
   `BURST_WINDOW = 10s` → update the existing row instead of inserting. This collapses
   "typing…" / "2 new messages" / "3 new messages" chains into the latest state.
3. **Rate ceiling.** More than `MAX_EVENTS_PER_KEY_PER_HOUR = 60` inserts for one key in one
   hour → switch that key to session behaviour for the rest of the hour and mark the row
   `wasRateLimited = true`, so the UI can say "and 340 more updates".

All constants live in one file, `core/signal/SignalTuning.kt`, and are unit-tested.

**Removal handling.** `onNotificationRemoved(sbn, rankingMap, reason)` records
`removalReason`. `REASON_LISTENER_CANCEL` means *we* cancelled it and must not be counted as
a user dismissal; `REASON_CANCEL` / `REASON_CANCEL_ALL` mark the row `seenBy = SWIPE`.

### 5.3 Firewall — the delivery decision

Evaluated synchronously in `onNotificationPosted`, **after** the row is written. First match
wins:

| # | Rule | Result |
|---|---|---|
| 0 | Package is **us** | `ALLOW` (never touch our own notifications) |
| 1 | `CATEGORY_ALARM`, `CATEGORY_CALL`, or an active phone call (`com.android.server.telecom` ongoing) | `ALLOW` — hard safety floor, not user-configurable |
| 2 | Class is `ONGOING` / `TRANSPORT` / `SERVICE` and "Leave system & media controls alone" is on (default **on**) | `ALLOW` |
| 3 | Quiet Mode is **paused** (see 5.5) | `ALLOW` |
| 4 | Matches **Always allow**: app, sender name, sender digits, or word | `ALLOW` |
| 5 | Looks like an OTP (see 5.6) and "Always let codes through" is on (default **on**) | `ALLOW` |
| 6 | An active **Schedule** applies → use that schedule's policy | `ALLOW` / `SILENCE` |
| 7 | App is in **Muted apps** | `SILENCE` |
| 8 | **Quiet Mode is ON** | `SILENCE` |
| 9 | otherwise | `ALLOW` |

`SILENCE` = `cancelNotification(sbn.key)` immediately.

**Honest limitation, to be stated in the store listing and in-app:** a notification listener
cannot prevent a notification from being posted; it can only remove it right after. On some
devices a brief sound or a single vibration may occur before removal. The mitigation the app
offers is a one-tap link to the system's per-channel settings for repeat offenders, plus the
recommendation to keep the phone on vibrate. We will not claim "zero flash".

Decisions are written to `firewall_decisions` (`sbnKey`, `action`, `ruleId`, `at`) so the
Inbox can show *why* something was let through or silenced, and so the dashboard counts are
auditable rather than guessed.

### 5.4 Sender and number matching

Requirement: find a notification by a person's name **or** by a phone number, in any format
(`+880 1709-093872`, `01709093872`, `1709093872`).

- `senderDigits` stores only digits, and additionally a `senderTail` = last **9** digits.
- Search input is normalized the same way; if the query is ≥4 digits after stripping
  non-digits, it is matched against `senderDigits LIKE '%tail%'` **in addition to** the
  full-text query.
- Name matching is FTS prefix matching, diacritic- and case-insensitive.

### 5.5 Quiet Mode, pause and schedules

- **Quiet Mode** is a single persisted boolean in DataStore. It survives process death and
  reboot (A11 fixed).
- **Pause** chips: 5 min · 15 min · 1 hour · Until I turn it back on. A pause stores an
  `pausedUntilEpochMs`; the firewall checks the clock — no alarm needed for correctness. A
  single `WorkManager` one-shot only exists to refresh the UI/status notification when it
  expires.
- **Schedules** are `(name, startMinuteOfDay, endMinuteOfDay, daysOfWeekBitmask, policy,
  extraAllowedApps, enabled)`. Overnight windows (22:00 → 07:00) are supported by comparing
  modulo 1440. `policy` is `QUIET` (silence everything except Always-allow + this schedule's
  extras) or `OPEN` (let everything through, overriding Quiet Mode).
- Schedule state is computed **lazily at decision time** from the wall clock. Consequence:
  the app needs **no `SCHEDULE_EXACT_ALARM` permission**, which is both a policy and a
  battery win.

### 5.6 OTP handling (and the Android 15+ reality)

Detection: body contains a 4–8 digit standalone token **and** an OTP-ish word in the user's
language set (`otp`, `code`, `verification`, `verify`, `pin`, `otp`/`কোড`/`ওটিপি`).

**Constraint, non-negotiable:** on Android 15 and above the system redacts OTP content from
notification listeners that are not trusted companion-device apps. We therefore:

- treat a redacted body as normal content and do not attempt any workaround;
- still classify the notification as an OTP by title/app heuristics so it *breaks through*;
- never persist an extracted code in a dedicated column — the code stays inside `body` and is
  offered as a one-tap **Copy code** action computed at render time.

The old `otpCode` column (A6) is removed.

### 5.7 Installed apps and the "system app" question

Requirement: Chrome must **not** be treated as a system app just because it shipped with the
phone; the launcher and framework components must be.

Enumeration uses a `<queries>` element for `ACTION_MAIN` + `CATEGORY_LAUNCHER` — **not**
`QUERY_ALL_PACKAGES`, which requires a Play declaration form we do not want to file.

```
isLaunchable   = packageManager.getLaunchIntentForPackage(pkg) != null
isPreinstalled = (flags and FLAG_SYSTEM) != 0
wasUpdated     = (flags and FLAG_UPDATED_SYSTEM_APP) != 0
hasNotified    = pkg exists in our notifications table

showAsUserApp   = isLaunchable || wasUpdated || hasNotified
showAsComponent = !showAsUserApp && isPreinstalled
```

Chrome: preinstalled **and** launchable → user app. ✅
Launcher / `com.android.systemui` / carrier stubs: preinstalled, not launchable, never
notified → component. ✅
Any package that has actually sent the user a notification is **always** listed, whatever its
flags — relevance beats taxonomy.

The app picker shows a single switch, **"Show system components"**, default **off**, plus a
search field and A→Z ordering with "apps that notify you most" pinned to the top.

---

## 6. Data model

Room, `exportSchema = true`, real `Migration` objects, **never** `fallbackToDestructiveMigration`.

```
notifications
  id                INTEGER PK AUTOINCREMENT
  sbnKey            TEXT     NOT NULL              -- INDEX
  packageName       TEXT     NOT NULL              -- INDEX
  appLabel          TEXT     NOT NULL
  title             TEXT
  body              TEXT
  subText           TEXT
  senderName        TEXT                            -- INDEX
  senderDigits      TEXT                            -- INDEX (digits only)
  channelId         TEXT
  androidCategory   TEXT
  importance        INTEGER
  signalClass       TEXT     NOT NULL              -- ALERT|ONGOING|PROGRESS|TRANSPORT|SERVICE
  contentHash       TEXT     NOT NULL
  firstSeenAt       INTEGER  NOT NULL              -- INDEX DESC
  lastSeenAt        INTEGER  NOT NULL
  endedAt           INTEGER
  updateCount       INTEGER  NOT NULL DEFAULT 1
  wasRateLimited    INTEGER  NOT NULL DEFAULT 0
  isSeen            INTEGER  NOT NULL DEFAULT 0    -- INDEX
  isStarred         INTEGER  NOT NULL DEFAULT 0    -- INDEX
  removalReason     INTEGER
  UNIQUE(sbnKey, contentHash, firstSeenAt)

notifications_fts   -- FTS4, content=notifications
  title, body, appLabel, senderName

firewall_decisions
  id, sbnKey, action (ALLOW|SILENCE), ruleId, ruleLabel, at        -- INDEX(at)

allow_rules
  id, type (APP|SENDER|WORD), value, matchMode (EXACT|CONTAINS|DIGITS), enabled, createdAt

muted_apps
  packageName PK, mutedAt

schedules
  id, name, startMinute, endMinute, daysMask, policy, enabled, createdAt

schedule_apps
  scheduleId, packageName            -- PK(scheduleId, packageName), FK CASCADE
                                     -- replaces the CSV columns of A13

daily_stats                          -- materialized, one row per local day
  day (yyyymmdd) PK, captured, silenced, allowed, quietMinutes, distinctApps

app_cache
  packageName PK, label, isLaunchable, isPreinstalled, wasUpdated, iconUpdatedAt
```

**Settings** (DataStore Preferences, not Room): `quietModeEnabled`, `pausedUntilEpochMs`,
`leaveSystemAndMediaAlone`, `otpAlwaysBreaksThrough`, `retentionDays`, `showTransportInInbox`,
`appLockEnabled`, `onboardingCompleted`, `themeMode`, `dynamicColorEnabled`.

### Retention

- Default **90 days**; options 30 / 90 / 365 / Forever.
- Starred rows are **never** purged automatically.
- Hard cap **50,000** rows; beyond it the oldest unstarred rows are dropped.
- `body` is truncated at 4,000 characters on write.
- A daily `WorkManager` job (`RetentionWorker`, flex 6h, requires battery-not-low) does the
  purge and rebuilds `daily_stats`.

### Paging

The Inbox uses `PagingSource` from Room (`androidx.room:room-paging` + `paging-compose`).
Loading 50,000 rows into a `StateFlow<List<…>>` — which the prototype does — is not viable.

---

## 7. Statistics & the Home dashboard

The dashboard exists to make an invisible benefit visible. Range selector: **All · 30d · 7d**.

| Tile | Definition |
|---|---|
| Captured | count of `notifications` in range |
| Silenced | `firewall_decisions.action = SILENCE` in range |
| Let through | `action = ALLOW` in range |
| Apps | distinct `packageName` in range |
| Quiet days | days in range where Quiet Mode was on ≥ 4h |
| Current streak | consecutive quiet days ending today |
| Longest streak | max consecutive quiet days, all time |
| Busiest hour | hour-of-day with the most captures (local time) |
| Noisiest app | package with the most captures in range |

**Activity heatmap.** GitHub-style: 7 rows (Mon→Sun) × up to 26 week columns, cell intensity
= that day's silenced count bucketed into 5 levels by the range's 90th percentile. Tapping a
cell filters the Inbox to that day.

**Estimated focus reclaimed.** Shown only when `silenced ≥ 50`, and always rendered with its
assumption visible:

> **~3h 12m of attention reclaimed**
> Estimate: 8 seconds of refocus per interruption avoided. Tap to change or hide.

The constant is a single value in `StatsTuning.kt`, user-adjustable (4 / 8 / 15 seconds) and
hideable. We do not use the widely-quoted 23-minute figure; it does not apply per-notification
and using it would be dishonest.

---

## 8. Design system

Minimal, dense, calm. Dark-first, because this is an app about *reducing* stimulation.

### Colour

Tokens are defined once in `ui/theme/Tokens.kt` and consumed only through
`MaterialTheme.colorScheme` + a small `LocalAppColors` extension. **No hardcoded `Color(0x…)`
outside the theme package** — this is a lint-enforced rule.

| Role | Dark | Light |
|---|---|---|
| background | `#0F1012` | `#FBFBFC` |
| surface | `#17181B` | `#FFFFFF` |
| surfaceVariant | `#1F2125` | `#F1F2F4` |
| outline | `#2C2F34` | `#E2E4E8` |
| onSurface | `#E8EAED` | `#16181B` |
| onSurfaceVariant | `#9AA0A8` | `#5E646C` |
| primary (accent) | `#7B93FF` | `#3B5BDB` |
| positive (allowed) | `#4ADE80` | `#16A34A` |
| muted (silenced) | `#9AA0A8` | `#6B7280` |
| warning | `#FBBF24` | `#B45309` |
| danger | `#F87171` | `#DC2626` |

Heatmap ramp (5 steps, accent-derived): `#1F2125 → #2A3565 → #3F51A8 → #5A72DB → #7B93FF`.

Dynamic colour is **off by default** (brand consistency) with a toggle in Settings.

### Type

Single family (system default / Roboto). Scale — nothing else may be used:

| Token | Size / line / weight | Use |
|---|---|---|
| `displayNumber` | 32 / 36 / 600, tabular figures | stat tile values |
| `titleLarge` | 22 / 28 / 600 | screen titles |
| `titleMedium` | 16 / 22 / 600 | section headers, row titles |
| `bodyLarge` | 15 / 22 / 400 | notification body |
| `bodyMedium` | 14 / 20 / 400 | secondary text |
| `label` | 12 / 16 / 500 | chips, tile captions |
| `mono` | 13 / 18 / 500, monospace | OTP codes, numbers |

All numeric readouts use `FontFeatureSetting("tnum")` so counters do not jitter.

### Space, shape, motion

- 4dp grid. Allowed spacing: `4, 8, 12, 16, 20, 24, 32, 48`.
- Screen horizontal padding: `16dp`. List row vertical padding: `12dp`.
- Corner radius: `8` (chips), `12` (rows, tiles), `20` (sheets, dialogs), `full` (toggles).
- Elevation is expressed as surface tone, never as a shadow, except for the bottom bar.
- Motion: `150ms` for state, `250ms` `FastOutSlowIn` for entry; **no** decorative animation.
  Everything respects `Settings.Global.ANIMATOR_DURATION_SCALE = 0`.

### Component inventory (built once, in Phase 0, reused everywhere)

`StatTile` · `SegmentedRange` (All/30d/7d) · `ActivityHeatmap` · `NotificationRow` ·
`SessionRow` · `AppRow` · `SectionHeader` · `EmptyState` · `PermissionCard` ·
`HealthBanner` · `QuietSwitch` · `PauseChipRow` · `SearchField` · `FilterChipRow` ·
`ConfirmSheet` · `AppPickerSheet` · `TimeRangePicker`.

### Accessibility (hard requirements, verified in Phase 10)

- Every touch target ≥ 48×48dp.
- Layouts survive font scale **200%** and display size **largest** without clipping.
- Contrast ≥ 4.5:1 for text, ≥ 3:1 for icons and the heatmap ramp.
- Every icon-only control has a `contentDescription`; every list row has a merged semantics
  node reading "*App · title · time · unread*".
- Colour is never the only signal: silenced rows carry an icon and a label, not just grey.
- Full TalkBack pass and RTL pass are release gates.

---

## 9. Screens

### 9.1 Onboarding — 4 screens, skippable after step 2

1. **Promise.** "Your phone stops buzzing. Nothing gets lost." One illustration, one button.
2. **Turn on notification access.** Explains in one sentence what it does and that nothing
   leaves the phone. Button deep-links to `ACTION_NOTIFICATION_LISTENER_SETTINGS`. The screen
   polls `NotificationManagerCompat.getEnabledListenerPackages()` on resume and advances
   automatically. A "Show me how" expander carries per-OEM wording, because the Settings path
   differs on Samsung / Xiaomi / Oppo.
3. **Who always gets through?** Pre-selects Phone, Messages and Clock. The user may add apps
   now or skip — skipping is safe because capture is unconditional.
4. **Done.** "Quiet Mode is on. Everything is being saved." → Home.

Shown only when `onboardingCompleted == false`. Re-runnable from Settings → About.

### 9.2 Home

```
┌────────────────────────────────────────┐
│  Quiet Mode                     [ ●  ] │   ← QuietSwitch, 56dp tall, unmissable
│  On since 9:14 AM                      │
├────────────────────────────────────────┤
│  ⏸ 5 min   15 min   1 hour   Until off │   ← PauseChipRow
├────────────────────────────────────────┤
│  ✓ Connected · capturing               │   ← HealthBanner (green/amber/red)
├────────────────────────────────────────┤
│           All  ·  30d  ·  7d           │
│  ┌──────────┬──────────┬────────────┐  │
│  │ Silenced │ Captured │ Apps       │  │
│  │   1,284  │   1,613  │    41      │  │
│  ├──────────┼──────────┼────────────┤  │
│  │ Streak   │ Longest  │ Busiest    │  │
│  │   6d     │   13d    │  2 PM      │  │
│  └──────────┴──────────┴────────────┘  │
│  ▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦▦            │   ← ActivityHeatmap
│  ~3h 12m of attention reclaimed  (i)   │
├────────────────────────────────────────┤
│  Noisiest this week                    │
│  Instagram ████████████ 312            │
│  WhatsApp  ██████ 154                  │
│  → Mute Instagram                      │   ← inline action, no navigation
└────────────────────────────────────────┘
```

The `HealthBanner` is amber when the listener is bound but has received nothing for 24h, red
when notification access is revoked, with a one-tap fix in both cases.

### 9.3 Inbox

- Sticky search field. Typing searches title, body, app name, sender name and phone digits
  simultaneously, debounced 250ms.
- Filter chips: **All · Unseen · Seen · Starred**, plus an app chip opened from a bottom sheet.
- Rows grouped under sticky day headers: *Today · Yesterday · 24 Aug*.
- An `ALERT` row shows app icon, app name, title, one line of body, relative time, an unread
  dot, and a small badge if it was silenced.
- A session row (`ONGOING`/`PROGRESS`) shows the range and the update count instead of a
  timestamp — this is where the hotspot fix becomes visible to the user.
- Swipe right = star. Swipe left = delete (with undo snackbar). Long-press = multi-select
  (mark seen / star / delete / mute this app).
- Opening a row marks it seen and shows the detail sheet: full body, channel, category, the
  firewall decision and the rule that caused it, **Copy code** if a code is present, **Open
  app**, **Mute this app**, **Always allow this sender**.
- Empty states are specific, never a shrug: "Nothing captured yet — notification access is
  off" carries a fix button; "No results for *1709093872*" offers to clear filters.

### 9.4 Settings

Flat list of eight rows, each opening one screen: Permissions & health · Always allow ·
Schedules · Muted apps · Storage & retention · App lock · Privacy · About.

**Permissions & health** is the screen the user asked for: a live list of every permission
and system grant the app can use, its current state, one line of why it exists, and a button
that fixes it.

| Item | Required? | Fix action |
|---|---|---|
| Notification access | **Required** | `ACTION_NOTIFICATION_LISTENER_SETTINGS` |
| Post notifications (13+) | Optional — for digest & status | runtime request |
| Ignore battery optimisation | Recommended | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| OEM autostart (Xiaomi/Oppo/Vivo/Huawei/Samsung) | Recommended, detected | best-effort OEM intent + written steps |
| Listener connection | live | "Reconnect" → toggles the component and calls `requestRebind` |
| Last notification captured | live | — |

---

## 10. Reliability

The single most common one-star review for apps in this category is *"it stopped working
after a few days."* That is a solvable engineering problem and is treated as a feature.

- `RECEIVE_BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` receiver calls
  `NotificationListenerService.requestRebind(componentName)`.
- A periodic `ListenerHealthWorker` (every 6h) checks whether the service is connected; if
  not it disables and re-enables the component via `PackageManager.setComponentEnabledSetting`,
  which forces the system to rebind.
- `onListenerConnected()` backfills from `getActiveNotifications()`, so nothing posted during
  a gap is lost.
- Every ingest is wrapped so that a single malformed notification can never crash the service;
  failures increment a counter surfaced in Settings → About → Diagnostics.
- OEM aggressiveness is detected from `Build.MANUFACTURER` and surfaced once, non-nagging.

---

## 11. Privacy & security

| Decision | Rationale |
|---|---|
| **No `INTERNET` permission** | Makes exfiltration impossible, not merely promised. This is the product's strongest marketing claim and it must never be traded away. |
| No analytics, no crash reporting SDK | Follows from the above. Diagnostics are local and shown to the user. |
| `android:allowBackup="false"`, empty `dataExtractionRules` | Fixes A5 — the archive must not be copied to cloud backup. |
| Optional **App lock** (`androidx.biometric`), off by default | The inbox is a transcript of the user's private messages. |
| No OTP code column | Fixes A6 and respects the Android 15+ redaction model. |
| Export is share-sheet only (JSON/CSV to a user-chosen file) | The user can move their data out; the app never initiates a transfer. |
| Room DB in `filesDir`, no external storage | Standard sandboxing. |
| SQLCipher | **Deferred past 1.0.** Documented tradeoff: ~3 MB APK, Keystore-backed passphrase, complicates migrations. App lock covers the realistic threat model first. |

### Google Play compliance notes

- `BIND_NOTIFICATION_LISTENER_SERVICE` is a sensitive permission. Play requires that it be
  necessary for core functionality that is *promoted in the store listing*. Ours is: the
  listing leads with notification filtering and history, so the requirement is met — but the
  listing copy and the in-app prominent disclosure must both say it plainly.
- A **prominent disclosure** screen (onboarding step 2) must appear *before* the permission
  request, stating what is accessed and that it stays on the device.
- Data safety form: notification content collected? **Yes — processed on device only, not
  collected, not shared.** Answer it accurately; misdeclaration is the most common cause of
  removal.
- Never use notification content for ads, profiling, or resale. Not planned, and must remain
  so.
- Avoid `QUERY_ALL_PACKAGES` (see 5.7) so no permission-declaration form is required.

---

## 12. Technical stack

| Area | Choice | Why |
|---|---|---|
| Language | Kotlin 2.2.x, coroutines + Flow | — |
| UI | Jetpack Compose, Material 3 | already in place |
| minSdk | **26** (raised from 24) | notification channels & importance are core to the model; API 24–25 is a negligible share and costs real workarounds |
| targetSdk / compileSdk | 36 | current |
| DI | **Hilt** | annotation-based, so parallel agents never edit a shared container file |
| Persistence | Room 2.7 + FTS4 + `room-paging`, `exportSchema = true`, real migrations | fixes A4, A13 |
| Settings | DataStore Preferences | fixes A11 |
| Background | WorkManager | retention, health check, digest |
| Navigation | Navigation-Compose with type-safe routes (`kotlinx.serialization`) | — |
| Lists | Paging 3 | 50k rows |
| Biometrics | `androidx.biometric` | app lock |
| Testing | JUnit4, Turbine, Robolectric, Roborazzi, Compose UI test, Room in-memory | Roborazzi already configured |
| Release | R8 **on**, resource shrinking on, `exportSchema` checked in | fixes A16 |
| **Removed** | Retrofit, OkHttp, Moshi, Firebase BOM, secrets-gradle-plugin, `.env`, `GEMINI_API_KEY` | fixes A7 |

### Module and package layout

Single `:app` Gradle module — deliberately. Multi-module would force every parallel agent to
edit `settings.gradle.kts` and the version catalog, which is exactly the merge conflict this
plan is built to avoid. Boundaries are enforced by package, and by the ownership matrix in
`TASKS.md`.

```
com.quietinbox
├── QuietInboxApp.kt                 @HiltAndroidApp
├── MainActivity.kt
├── core/
│   ├── model/          domain models, SignalClass, FirewallAction, AllowRule…
│   ├── signal/         SignalClassifier, SignalTuning, ContentHash
│   ├── firewall/       FirewallEngine, RuleMatcher, ScheduleEvaluator
│   ├── apps/           InstalledAppsProvider, IconCache
│   ├── time/           Clock abstraction (tests must not read the wall clock)
│   └── util/
├── data/
│   ├── db/             entities, DAOs, database, migrations
│   ├── prefs/          SettingsDataStore
│   ├── repo/           NotificationRepository, RuleRepository, StatsRepository…
│   └── work/           RetentionWorker, ListenerHealthWorker, DigestWorker
├── service/
│   ├── QuietListenerService.kt
│   ├── BootReceiver.kt
│   └── ingest/         NotificationParser, IngestPipeline
├── feature/
│   ├── onboarding/  home/  inbox/  rules/  schedules/  settings/  permissions/
│   │   └── each: <Name>Screen.kt, <Name>ViewModel.kt, components/
├── ui/
│   ├── theme/          Tokens, Color, Type, Shape, Theme
│   └── components/     the shared component inventory
└── di/                 AppModule, DatabaseModule, (+ one module per feature)
```

---

## 13. Quality gates (release blockers)

1. `./gradlew assembleRelease lint test` is green; zero `lint` errors, zero `Deprecated` in
   new code.
2. Unit coverage ≥ 80% on `core/signal`, `core/firewall`, `data/repo`.
3. **Spam regression test:** a synthetic feed of 600 hotspot updates and 400 download progress
   ticks over 10 minutes produces **exactly 2 rows**. This test is the acceptance proof for
   the bug that motivated this rewrite.
4. Inbox scrolls at 60fps with 50,000 seeded rows on a mid-range device; no frame > 16ms in a
   Macrobenchmark scroll trace.
5. Cold start to first frame < 800ms on a Pixel 6a.
6. Listener survives: reboot · app update · force-stop-then-notification · 48h idle.
7. Font scale 200% and RTL screenshots pass for every screen (Roborazzi).
8. TalkBack walkthrough of Home, Inbox, Settings with no unlabeled control.
9. APK contains **no** `android.permission.INTERNET` — asserted by an automated test that
   parses the merged manifest.
10. Room schema JSON checked in; a migration test upgrades 1→N with data intact.

---

## 14. Deliberate rejections

| Rejected | Reason |
|---|---|
| Cloud sync / account | Contradicts the no-INTERNET moat, which is the main differentiator. |
| AI/LLM categorisation | Requires either a network call or a large on-device model. Keyword + channel + category heuristics cover the need at zero cost. |
| Tasker-style rule builder | The audience explicitly includes non-technical and older users. Buzzkill already owns the power-user niche. |
| Replacing the system notification shade | Not possible without an accessibility service; that path is being restricted (Android 17 Advanced Protection) and is a policy risk. |
| Per-notification-channel blocking UI | Channel IDs are opaque strings; users cannot reason about them. App-level + word-level covers 95% of intent. |
| Widgets, Wear, tablets-first layouts | Post-1.0. Phone portrait is the whole surface for v1.0. |
| Storing notification icons per row | Storage explosion; per-package cache instead. |
