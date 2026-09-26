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

## Testing

Add a regression test for a bug fix and update tests when behavior changes.

| Command | Checks |
| --- | --- |
| `./gradlew test` | Task models, parsing, storage, failed-save recovery, and JavaFX interactions |
| `./gradlew jarSmokeTest` | Fat JAR resources, console commands, startup failures, and desktop startup in separate JVMs |
| `./gradlew check` | Both test tasks |
| `./gradlew jacocoTestReport` | Unit and JavaFX test coverage report |

`test` also generates the JaCoCo report. This report covers the tests run in that task; it excludes code executed by the separate processes in `jarSmokeTest`. Read the HTML reports under `build/reports/tests/test/`, `build/reports/tests/jarSmokeTest/`, and `build/reports/jacoco/test/html/`.

The desktop test saves `desktop.png`, `desktop-small.png`, and `desktop-farewell.png` under `build/reports/ui-smoke/`. Check the normal window, minimum window, and farewell screenshots when changing layout or styling. The test also checks that `bye` leaves the farewell visible for three seconds before closing the app.

Use temporary directories for test data. For storage changes, test failed saves and recovery as well as successful writes. For parser changes, include invalid input and successful commands. GUI tests should load the actual FXML and check behavior visible to the user.

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
4. Merge the reviewed changes into `master`. Create and push an annotated `vX.Y.Z` tag for the release commit, then wait for all three CI jobs for that tag to pass.
5. Download `Arn.jar` from the tagged run's `arn-application` artifact. Publish a GitHub release for the tag with the JAR, release notes, and an `Arn.jar.sha256` checksum file.
6. Download the published JAR, check its checksum, and confirm its version.

The release artifact is the fat JAR named `Arn.jar`. It bundles JavaFX for x64 Windows, Linux, and Intel macOS and requires Java 17. Use the artifact from the tagged CI run so the download matches the tested source.
