# Arn

Arn is a Java desktop task manager for todos, deadlines and events. I built it with Java 17 and JavaFX, with a command interface and local file storage. It works offline and does not need an account.

![Arn desktop interface](https://raw.githubusercontent.com/Aryann-Sharma/ARN/master/docs/Ui.png)

## Using Arn

Download `Arn.jar` from [Releases](https://github.com/Aryann-Sharma/ARN/releases) and run:

```text
java -jar Arn.jar
```

Use an **x64 Java 17** installation on Windows, Linux or Intel macOS. The JAR includes JavaFX and its native libraries, but Java itself must be installed separately. Native ARM builds are not included.

There is also a console interface:

```text
java -jar Arn.jar --cli
```

| Action | Example |
| --- | --- |
| Add a todo | `todo Read a chapter` |
| Add a deadline | `deadline Submit report /by 2026-10-20 1800` |
| Add an event | `event Team lunch /from 2026-10-20 1200 /to 2026-10-20 1330` |
| List tasks | `list` |
| Mark or unmark a task | `mark 1`, `unmark 1` |
| Edit a description | `edit 1 Read chapter two` |
| Reschedule a task | `reschedule 2 /by 2026-10-21 1800` |
| Search or sort | `find report`, `sort` |
| Undo a change | `undo` |
| Delete or exit | `delete 1`, `bye` |

Commands are lowercase. Dates use `YYYY-MM-DD` or `YYYY-MM-DD HHMM`, with 24-hour time. Searches ignore case. The [user guide](https://github.com/Aryann-Sharma/ARN/blob/master/docs/README.md) has the full command syntax, partial rescheduling examples and recovery instructions.

## Main features

- JavaFX GUI and console interfaces over the same task model.
- Todos, deadlines and events, with date validation, editing and rescheduling.
- Search and chronological views that preserve the task numbers used by commands.
- Multi-step undo for successful changes in the current session.
- Local persistence with checks for failed or conflicting saves.

Undo history is capped at 100 changes and is cleared when the app restarts. Failed commands and unchanged values do not add entries. There is no redo command.

## Saving tasks

Tasks are stored in `data/arn.txt`, relative to the directory where you launch the app. I made saving part of each successful change so that a command does not report success before its data is written.

Each save writes a complete temporary file and replaces the old one with an atomic move where the file system supports it. If the move is unsupported, the app uses ordinary replacement. If saving fails, it restores the previous task state and reports the error.

Before saving, Arn checks whether another process has changed the file. A lock coordinates writes; conflicting changes require a restart to load the newer data. Invalid or unreadable files stop startup rather than being silently replaced.

Task descriptions are plain text. This is a local task manager, so it has no sync or encrypted storage.

## Build and test

Use an x64 JDK 17. The Gradle wrapper is included.

```powershell
# Windows
.\gradlew.bat run
.\gradlew.bat clean check shadowJar --warning-mode fail
```

```bash
# macOS or Linux
./gradlew run
./gradlew clean check shadowJar --warning-mode fail
```

On Linux without a graphical session, run the checks through a virtual display:

```bash
xvfb-run --auto-servernum ./gradlew clean check shadowJar --warning-mode fail
```

The first build downloads dependencies. The packaged app is written to `build/libs/Arn.jar`.

`check` runs JUnit tests and separate packaged-app tests. They cover commands, dates, undo, persistence failures and JavaFX interactions. The packaged tests start the JAR in separate Java processes to check CLI behaviour, saved data, startup errors and GUI rendering.

[GitHub Actions](https://github.com/Aryann-Sharma/ARN/blob/master/.github/workflows/ci.yml) runs the checks on Windows, Linux and Intel macOS. Compiler warnings fail the build. Test reports, GUI screenshots and the Linux-built JAR are uploaded as workflow artifacts. JaCoCo reports cover the main test task, not the separate JAR processes.

## Code layout

- `Arn` and `TaskSnapshot`: command handling, save rollback and undo.
- `Parser`: command syntax and validation.
- `TaskList`, `Task`, `Todo`, `Deadline`, `Event`, `TaskDate`: task state and dates.
- `TaskFileHandler`: persistence and conflict detection.
- `MainWindow`, `DialogBox`, `Gui`, `Ui`: interfaces.
- `src/test/java/arn/`: unit, GUI and packaged-app tests.

See [CONTRIBUTING](https://github.com/Aryann-Sharma/ARN/blob/master/CONTRIBUTING.md) for development and release steps, and the [changelog](https://github.com/Aryann-Sharma/ARN/blob/master/CHANGELOG.md) for version history.
