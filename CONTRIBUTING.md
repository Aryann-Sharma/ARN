# Contributing

Keep changes focused and consistent with the command interface. A pull request should explain the problem, the resulting behavior, and how it was tested.

## Development setup

Install an x64 JDK 17 on Windows, Linux, or Intel macOS. The repository includes the Gradle wrapper, so no separate Gradle installation is needed. Dependencies are downloaded on the first build.

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

`Launcher` selects desktop or console mode. `Arn` coordinates command execution and persistence, buffering responses until a change has been saved. `Parser` validates commands and operates on `TaskList`; task classes and `TaskDate` contain model and date behavior. `TaskFileHandler` owns file compatibility, write locking, and conflict detection.

Keep command behavior shared between the interfaces. Put layout in FXML and appearance in CSS under `src/main/resources/`. Use UTF-8, four spaces for Java and Gradle files, and the repository's line-ending rules. Compilation treats lint warnings as errors.

## Testing

Add a regression test for a bug fix and update tests when behavior changes.

| Command | Checks |
| --- | --- |
| `./gradlew test` | Models, parsing, persistence, application transactions, and JavaFX interactions |
| `./gradlew jarSmokeTest` | Fat JAR resources, console commands, startup failures, and desktop startup in separate JVMs |
| `./gradlew check` | Both test tasks |
| `./gradlew jacocoTestReport` | Unit and JavaFX test coverage report |

`test` also generates the JaCoCo report. Read the HTML reports under `build/reports/tests/test/`, `build/reports/tests/jarSmokeTest/`, and `build/reports/jacoco/test/html/`. The packaged GUI test captures `desktop.png` and `desktop-small.png` under `build/reports/ui-smoke/`; inspect both when changing layout or styling.

Use temporary directories for test data. Include failure and recovery paths for persistence changes, and ensure malformed or conflicting data cannot be silently overwritten. For parser changes, cover invalid syntax alongside successful commands. GUI tests should exercise the actual FXML and verify user-visible behavior.

CI runs checks on Windows, Linux, and Intel macOS. Linux uses Xvfb. Test reports, coverage, and GUI screenshots are uploaded as build artifacts, and the Linux job uploads the runnable JAR.

## Before opening a pull request

- Run `./gradlew clean check shadowJar --warning-mode fail`.
- Update the user guide for command or storage behavior changes.
- Add a changelog entry for a release-facing change.
- Keep commit messages short and specific.
- Leave generated output, local task data, and IDE files out of Git.

## Releasing

1. Set the release version in `build.gradle` and finish the matching entry in [CHANGELOG.md](CHANGELOG.md). The packaged tests check the JAR against that version automatically.
2. Run the full checks above and inspect both packaged GUI screenshots.
3. Check that `java -jar build/libs/Arn.jar --version` reports the intended version, then try the desktop and `--cli` with a temporary data directory.
4. Commit the release changes and push them. Create a matching `vX.Y.Z` tag for that commit and verify the three CI jobs.
5. Publish a GitHub release for the tag, describe the changes and supported platforms, and attach `build/libs/Arn.jar`. Include a SHA-256 checksum so downloads can be checked.

The release artifact is the fat JAR named `Arn.jar`. It bundles JavaFX for x64 Windows, Linux, and Intel macOS and requires Java 17. Build from the tagged source and avoid publishing an artifact left over from an earlier build.
