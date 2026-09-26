# Arn

[![Build](https://github.com/Aryann-Sharma/ARN/actions/workflows/ci.yml/badge.svg)](https://github.com/Aryann-Sharma/ARN/actions/workflows/ci.yml)

Arn is a desktop task manager built with Java 17 and JavaFX. Type short commands to add todos, track deadlines, plan events, and find tasks. Tasks stay on your computer and remain available between sessions.

![Arn desktop interface](docs/Ui.png)

## Download and run

Download `Arn.jar` from the [releases page](https://github.com/Aryann-Sharma/ARN/releases), then run it from the directory where you want to keep your task data:

```bash
java -jar Arn.jar
```

The release requires a **64-bit Java 17 runtime** and supports **x64 Windows, Linux, and Intel macOS**. JavaFX and its native libraries are included in the fat JAR; Java itself is not included. The release does not bundle native ARM libraries.

Type a command and press **Enter** or select **Send**. Quick actions show your tasks, sort dated tasks, or display examples. Quick actions preserve anything you are typing, and failed commands remain in the input field for correction.

A console interface is also available:

```bash
java -jar Arn.jar --cli
java -jar Arn.jar --help
java -jar Arn.jar --version
```

Console input and output use UTF-8. Use a terminal configured for UTF-8 when entering non-ASCII text. The console exits on `bye` or end of input.

See the [user guide](docs/README.md) for setup, examples, and storage troubleshooting, or the [changelog](CHANGELOG.md) for release history.

## Commands

| Command | Example |
| --- | --- |
| Add a todo | `todo Read a chapter` |
| Add a deadline | `deadline Submit report /by 2026-10-02 1800` |
| Add an event | `event Team lunch /from 2026-10-04 1200 /to 2026-10-04 1330` |
| Show tasks | `list` |
| Mark complete or incomplete | `mark 1`, `unmark 1` |
| Delete a task | `delete 1` |
| Search descriptions | `find report` |
| Show dated tasks chronologically | `sort` |
| Display a farewell; exit in console mode | `bye` |

Commands are case-sensitive. Dates must be valid calendar dates in `YYYY-MM-DD` or `YYYY-MM-DD HHMM` format, with 24-hour time. Invalid dates such as `2026-02-30` are rejected. An event's start and end must both include a time or both omit it, and its end cannot precede its start.

`find` and `sort` display the original task numbers from `list`, so those numbers can be used directly with `mark`, `unmark`, and `delete`. They do not change the saved order.

## Local storage

Task data lives in `data/arn.txt`, relative to the working directory. The file is created on the first successful change. Opening the app, viewing tasks, and commands that leave tasks unchanged do not rewrite it.

Changes are written to a temporary file before replacing the save file, using atomic replacement where supported. A failed save restores the previous state and reports an error. The versioned UTF-8 format also accepts legacy save files.

If saved data is malformed or unreadable, startup stops and leaves the original data intact. If another instance or an external editor changes the file, Arn rejects further writes from the stale session; restart to load the latest data. A persistent `data/arn.txt.lock` file is normal and coordinates saves.

Task descriptions are stored as plain text. The local `data/` directory is excluded from Git.

## Development

Install **JDK 17**. The checked-in Gradle wrapper supplies the build tools.

On Windows:

```powershell
.\gradlew.bat run
.\gradlew.bat clean check shadowJar --warning-mode fail
```

On macOS or Linux:

```bash
./gradlew run
./gradlew clean check shadowJar --warning-mode fail
```

On Linux without a graphical session, run the checks under a virtual display:

```bash
xvfb-run --auto-servernum ./gradlew clean check shadowJar --warning-mode fail
```

The first build needs internet access to download dependencies. The application works offline after installation.

## Design and verification

The code is organized into a few small layers:

| Area | Main classes | Responsibility |
| --- | --- | --- |
| Entry points | `Launcher`, `Arn` | Select the interface, initialize storage, and save changes before reporting success |
| User interface | `MainWindow`, `DialogBox`, `Ui`, `Gui` | Collect input and display responses |
| Commands | `Parser` | Validate command syntax and update tasks |
| Task model | `TaskList`, `Task`, `Todo`, `Deadline`, `Event`, `TaskDate` | Manage tasks, dates, searches, and sorted views |
| Persistence | `TaskFileHandler`, `StorageException` | Read compatible save files and protect writes |

FXML, CSS, and images are under `src/main/resources/`. Tests are under `src/test/java/arn/`.

`check` runs unit and JavaFX tests plus `jarSmokeTest`. The packaged tests launch separate JVMs, exercise console persistence and startup failures, inspect bundled resources, and open the desktop using the release JAR. GUI checks produce screenshots at normal and minimum window sizes.

| Output | Location |
| --- | --- |
| Unit and JavaFX test report | `build/reports/tests/test/index.html` |
| Packaged application test report | `build/reports/tests/jarSmokeTest/index.html` |
| JaCoCo coverage report | `build/reports/jacoco/test/html/index.html` |
| Packaged GUI screenshots | `build/reports/ui-smoke/` |
| Runnable fat JAR | `build/libs/Arn.jar` |

CI runs the same checks on Windows, Linux, and Intel macOS, and retains test reports and the JAR as build artifacts. Compiler warnings fail the build. Dependency updates are checked monthly.

For contribution and release steps, see [CONTRIBUTING.md](CONTRIBUTING.md).
