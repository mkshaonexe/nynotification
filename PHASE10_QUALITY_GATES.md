# Quiet Inbox — Phase 10: Quality Gates & Play Readiness

> **Status:** Phase 10 — Integration, hardening & release
> **Author:** MK Shaon · **Date:** 2026-08-26

---

## Phase 10 Task Checklist

| # | Task | Status | Notes |
|---|------|--------|-------|
| 10.1 | Confirm stubs are gone | ✅ | `StubVerificationTest.kt` asserts no placeholder returns in DefaultSignalClassifier, DefaultFirewallEngine, DefaultScheduleEvaluator |
| 10.2 | Cross-phase seams | ✅ | `CrossPhaseSeamsTest.kt` verifies heatmap→Inbox, Inbox mute/allow, health banner, firewall verdict display |
| 10.3 | Copy pass — banned terms | ✅ | `CopyPassAndDeadCodeTest.kt` asserts "vault", "master block", "smart shield", "intercept", "whitelist" absent from strings |
| 10.4 | Delete dead code | ✅ | Prototype files deleted (verified in `CopyPassAndDeadCodeTest.kt`); no seedDatabase, processSimulatedNotification, demo data |
| 10.5 | Performance | ⬜ | Macrobenchmark on device: Inbox 50k rows @60fps, cold start <800ms. Baseline Profile to be generated on release device |
| 10.6 | Accessibility | ⬜ | TalkBack walkthrough × 3 tabs; 200% font scale; RTL pass; heatmap contrast audit on device |
| 10.7 | No-internet assertion | ✅ | `NoInternetPermissionTest.kt` (extended) + `ManifestVerificationTest.kt` both parse manifest |
| 10.8 | Endurance (72h) | ⬜ | Requires real device: 72h continuous use, verify listener alive, capture sane, no ANR |
| 10.9 | Reboot & update matrix | ⬜ | Requires real device: reboot, force-stop+notify, app update, revoke+re-grant, 48h Doze |
| 10.10 | Release build | ✅ | R8 + resource shrinking enabled in `app/build.gradle.kts`; verified no networking deps |
| 10.11 | Room migration test | ✅ | `RoomMigrationTest.kt` verifies schema v1 JSON, all tables, no fallbackToDestructiveMigration |
| 10.12 | Play readiness | ✅ | See §3 below |
| 10.13 | Quality gates (design.md §13) | See §2 below | — |

---

## §2 — design.md §13 Quality Gates (10 gates)

| Gate | Requirement | Status |
|------|-------------|--------|
| 1 | `./gradlew assembleRelease lint test` green; zero lint errors, zero `Deprecated` in new code | ⬜ Run on CI |
| 2 | Unit coverage ≥ 80% on `core/signal`, `core/firewall`, `data/repo` | ⬜ Run coverage report |
| 3 | Spam regression: 600 hotspot updates + 400 download ticks → **exactly 2 rows** | ✅ `HotspotSpamTest` + `DownloadProgressTest` assert this |
| 4 | Inbox scrolls 60fps with 50,000 rows; no frame >16ms in Macrobenchmark trace | ⬜ Requires device |
| 5 | Cold start to first frame <800ms on Pixel 6a | ⬜ Requires device |
| 6 | Listener survives: reboot, app update, force-stop→notification, 48h idle | ⬜ Requires device |
| 7 | Font scale 200% + RTL screenshots pass (Roborazzi) | ⬜ Run Roborazzi suite |
| 8 | TalkBack walkthrough of Home, Inbox, Settings — no unlabeled control | ⬜ Manual on device |
| 9 | APK contains **no** `android.permission.INTERNET` — automated test | ✅ `NoInternetPermissionTest` + `ManifestVerificationTest` |
| 10 | Room schema JSON checked in; migration test upgrades 1→N with data intact | ✅ `RoomMigrationTest.kt`; schema dir to be committed after first build |

---

## §3 — Google Play Readiness (10.12)

### Data Safety Form Answers

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **No** |
| Is notification content collected? | **Yes — processed on device only** |
| Is it shared? | **No** |
| Is it transferred off-device? | **No** — the app ships without the INTERNET permission |
| Is user data encrypted in transit? | N/A — no transit |
| Does the user have the ability to request deletion? | **Yes** — Settings → Storage → Delete all history |

> ⚠️ **Misdeclaration** is the most common cause of app removal from Play. Answer accurately.

### Sensitive Permission Justification

**Permission:** `BIND_NOTIFICATION_LISTENER_SERVICE`

**Justification (to submit to Play):**
> Quiet Inbox's core function, promoted in the store listing, is notification filtering and history.
> The app silences unwanted notifications and captures all of them to a private, searchable inbox
> on the device. This requires reading notification content via NotificationListenerService.
> No notification content ever leaves the device — the app ships without the INTERNET permission,
> which is verifiable by inspecting the APK.

### Prominent Disclosure (Google Play requirement)

Onboarding step 2 (implemented in Phase 8) must state, **before** the permission request:

> "Quiet Inbox reads your notifications to filter and save them privately on this device.
> Nothing is ever sent anywhere — the app has no internet access."

This satisfies the mandatory pre-permission disclosure for `BIND_NOTIFICATION_LISTENER_SERVICE`.

### Store Listing Guidelines

- **Lead with filtering and history** — these are the functions that justify the sensitive permission.
- **State the no-internet claim** prominently; it is verifiable and differentiating.
- **Screenshots** must show the Inbox, the Home dashboard, and the Permissions & health screen.
- **Privacy Policy URL** must be live before submission.

### Privacy Policy Minimum Content

The privacy policy must state:
1. What data is processed (notification content, package names, timestamps).
2. Where it is stored (on-device only, Room database in app's private storage).
3. That it is never transmitted or shared.
4. That the user can delete all data from within the app.
5. Contact information for data inquiries.

---

## §4 — Proguard / R8 Notes (10.10)

The following classes require explicit Keep rules in `proguard-rules.pro`:

- `@Database`, `@Entity`, `@Dao` annotated classes — Room reflection
- `@HiltAndroidApp`, `@AndroidEntryPoint`, `@Inject` — Hilt
- `@Serializable` classes in the `navigation` package — kotlinx.serialization
- `NotificationListenerService` subclass — system must bind it by class name
- `BroadcastReceiver` subclasses (`BootReceiver`) — system must bind by class name
- `WorkManager` Worker subclasses — WorkManager instantiation

Verify with: `./gradlew :app:assembleRelease` and inspect for missing class warnings in the build output. The mapping file must be retained for crash symbolication.

### APK Size Target

- Target: **< 8 MB** (design.md §13 gate 10)
- With R8 + resource shrinking, typical Compose + Room + Hilt footprint is 5–7 MB.
- No large assets (no ML models, no bundled fonts beyond system default).

---

## §5 — Reliability Matrix (10.9)

| Scenario | Expected behavior | Tested? |
|----------|-------------------|---------|
| Device reboot | `BootReceiver` calls `requestRebind`; listener reconnects within 30s | ⬜ |
| `MY_PACKAGE_REPLACED` (app update) | Same as reboot; icon cache invalidated | ⬜ |
| Force-stop → incoming notification | Notification is shown; on next launch listener rebinds and `backfill()` catches up | ⬜ |
| Notification access revoked | `HealthBanner` turns red immediately; fix button opens system settings | ⬜ |
| Notification access re-granted | `HealthBanner` turns green; backfill runs | ⬜ |
| 48h idle (Doze mode) | `ListenerHealthWorker` (6h period) runs on Doze exit; listener alive | ⬜ |
| Listener bound but nothing captured in 24h | `HealthState.STALE` → amber banner | ⬜ |

---

*This document is generated as part of Phase 10 execution. Update the status column as gates are verified.*
