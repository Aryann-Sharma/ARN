# Contributing

Keep changes small enough to review and consistent with the existing commands. In a pull request, explain what changed, why it was needed, and how you tested it.

## Development setup

Install x64 JDK 17 on Windows, Linux, or Intel macOS, then open a terminal in the repository root. The Gradle wrapper downloads the build tools and dependencies on the first run; no separate Gradle installation is needed.

```bash
./gradlew run
./gradlew clean check shadowJar --warning-mode fail
```

On Windows, use `.\gradlew.bat` in place of `./gradlew`. On Linux without a graphical session, use:

```bash
xvfb-run --auto-servernum ./gradlew clean check shadowJar --warning-mode fail
```

Tests load real JavaFX controls, so they require a display or a virtual display.

## Code organization

`Launcher` selects desktop or console mode. `Arn` runs commands and waits for changes to be saved before displaying a success response. `Parser` checks command syntax and updates `TaskList`. The task classes store descriptions, completion status, and dates; `TaskDate` handles date parsing and formatting. `TaskFileHandler` reads and writes the save file and checks for changes made by other sessions.

Keep command behavior shared between the interfaces. Put layout in FXML and appearance in CSS under `src/main/resources/`. Use UTF-8, four spaces for Java and Gradle files, and the repository's line-ending rules. Compilation treats lint warnings as errors.

Editing replaces a task in place because descriptions and dates are immutable. `TaskSnapshot` records task references, order, and completion flags for rollback and undo. `Arn` adds an undo entry only after a changed task list is saved, and removes an entry only after an undo is saved. The last 100 changes are retained for the current session. If new mutable task fields are introduced, include them in snapshots and change detection.

## Testing

Add a regression test for a bug fix and update tests when behavior changes.

| Command | Checks |
| --- | --- |
| `./gradlew test` | Task models, parsing, storage, failed-save recovery, and JavaFX interactions |
| `./gradlew jarSmokeTest` | Fat JAR resources, console commands, startup failures, and desktop startup in separate JVMs |
| `./gradlew check` | Both test tasks |
| `./gradlew jacocoTestReport` | Unit and JavaFX test coverage report |

`test` also generates the JaCoCo report. This report covers the tests run in that task; it excludes code executed by the separate processes in `jarSmokeTest`. Read the HTML reports under `build/reports/tests/test/`, `build/reports/tests/jarSmokeTest/`, and `build/reports/jacoco/test/html/`.

The desktop test saves `desktop-welcome.png`, `desktop.png`, `desktop-small.png`, and `desktop-farewell.png` under `build/reports/ui-smoke/`. Check the welcome screen, normal window, minimum window, and farewell screenshots when changing layout or styling. Keep `docs/Ui.png` up to date using the welcome screenshot. The test also checks that `bye` leaves the farewell visible for three seconds before closing the app.

Use `images/ArnTaskbar.png` for the compact robot icon in the header, replies, and window icons. Keep its light background and small outer margin so it remains visible against different surfaces. The desktop supplies several icon sizes for the operating system to choose from; the taskbar controls the final displayed size.

Use temporary directories for test data. For storage changes, test failed saves and recovery as well as successful writes. For parser changes, include invalid input and successful commands. GUI tests should load the actual FXML and check behavior visible to the user.

For editing and undo, check all task types, task order, completion status, explicit midnight, unchanged values, and multiple consecutive undos. A failed change or undo must preserve both the current tasks and the available undo history. Include restart checks to distinguish persisted tasks from session-only history.

Event rescheduling treats a missing marker as an unchanged endpoint and a date-only value as retaining that endpoint's existing time. Resolve those values before checking event order and date precision. Test single-endpoint updates, retained midnight, explicit time replacements, failed saves, and undo. Creation and deadline rescheduling keep their existing date rules.

CI runs checks on Windows, Linux, and Intel macOS. Linux uses Xvfb. Test reports, coverage, and GUI screenshots are uploaded as build artifacts, and the Linux job uploads the runnable JAR.

## Before opening a pull request

- Run `./gradlew clean check shadowJar --warning-mode fail`.
- Update the user guide for command or storage behavior changes.
- Add a changelog entry when a change affects users.
- Keep commit messages short and specific.
- Leave generated output, local task data, and IDE files out of Git.

## Releasing

1. Set the release version in `build.gradle` and finish the matching entry in [CHANGELOG.md](CHANGELOG.md). The packaged tests check the JAR against that version automatically.
2. Run the full checks above and inspect the packaged GUI screenshots.
3. Check that `java -jar build/libs/Arn.jar --version` reports the intended version. Launch the desktop and `--cli` from a temporary working directory so they use test data.
4. After the pull request checks pass, merge the reviewed changes into `master` and rerun the full checks on the merged commit. Create and push an annotated `vX.Y.Z` tag for that tested commit, then wait for all three CI jobs for the tag to pass.
5. Download `Arn.jar` from the tagged run's `arn-application` artifact. Publish a GitHub release for the tag with the JAR, release notes, and an `Arn.jar.sha256` checksum file.
6. Download the published JAR, check its checksum, and confirm its version.

The release artifact is the fat JAR named `Arn.jar`. It bundles JavaFX for x64 Windows, Linux, and Intel macOS and requires Java 17. Use the artifact from the tagged CI run so the download matches the tested source.
