# Rich Chat Debug Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `/traveler debug status` print a richer chat diagnostic report with computed anomalies.

**Architecture:** Keep diagnostics in `modules/core` so Minecraft modules only expose the command. Add a compact report/anomaly layer beside existing debug snapshots, then reuse it from the common command feature.

**Tech Stack:** Java 21, JUnit, existing Traveler command/debug framework.

---

### Task 1: Core Debug Report

**Files:**
- Create: `modules/core/src/main/java/dev/traveler/core/debug/DebugAnomaly.java`
- Create: `modules/core/src/main/java/dev/traveler/core/debug/DebugReport.java`
- Modify: `modules/core/src/main/java/dev/traveler/core/debug/DebugTextFormatter.java`
- Test: `modules/core/src/test/java/dev/traveler/core/debug/NavigationDebugSnapshotTest.java`

- [ ] Write failing tests for multi-line report output and anomaly names.
- [ ] Run `./gradlew.bat :core:test --tests dev.traveler.core.debug.NavigationDebugSnapshotTest`.
- [ ] Implement immutable report/anomaly generation in `core`.
- [ ] Re-run the targeted core debug tests.

### Task 2: Command Integration

**Files:**
- Modify: `modules/mc/1_21_11/common/src/main/java/dev/traveler/mc/v1_21_11/common/command/DebugTravelerCommandFeature.java`
- Test: `modules/mc/1_21_11/common/src/test/java/dev/traveler/mc/v1_21_11/common/command/TravelerCommandModuleTest.java`

- [ ] Write failing command test expecting rich status text and anomalies.
- [ ] Run `./gradlew.bat :mc_1_21_11_common:test --tests dev.traveler.mc.v1_21_11.common.command.TravelerCommandModuleTest`.
- [ ] Wire the command to `DebugTextFormatter.detailedStatus(...)`.
- [ ] Re-run targeted command tests.

### Task 3: Verification

**Files:**
- No new files.

- [ ] Run `./gradlew.bat check`.
- [ ] Inspect `git diff --check` and `git status --short`.
- [ ] Commit with `feat(core): add rich chat debug diagnostics`.
