# Arn

Arn is a lightweight desktop task assistant built with Java 17 and JavaFX. It uses a conversational command interface to capture todos, track deadlines, schedule events, and search or sort saved tasks.

Tasks are stored locally, so they remain available between sessions without requiring an account or internet connection.

## Features

- Create todos, deadlines, and scheduled events
- Mark tasks complete or incomplete
- Search tasks by description
- View dated tasks in chronological order
- Persist tasks automatically in a local save file
- Use quick actions for common commands
- See live task-count, empty-state, and error feedback
- Navigate a responsive JavaFX chat interface with keyboard-friendly input

## Requirements

- Java Development Kit (JDK) 17
- No separate Gradle installation is needed; the repository includes the Gradle wrapper

## Run the application

On Windows:

```powershell
.\gradlew.bat run
```

On macOS or Linux:

```bash
./gradlew run
```

Enter a command in the composer and press **Enter** or select **Send**. The **Show tasks**, **Upcoming**, and **Examples** quick actions provide shortcuts for common workflows.

## Command reference

| Command | Purpose | Example |
| --- | --- | --- |
| `todo DESCRIPTION` | Add a task without a date | `todo Read a chapter` |
| `deadline DESCRIPTION /by DATE` | Add a task with a due date | `deadline Submit report /by 2026-10-02 1800` |
| `event DESCRIPTION /from START /to END` | Add a scheduled event | `event Team lunch /from 2026-10-04 1200 /to 2026-10-04 1330` |
| `list` | Show all saved tasks | `list` |
| `mark NUMBER` | Mark a task complete | `mark 1` |
| `unmark NUMBER` | Mark a task incomplete | `unmark 1` |
| `delete NUMBER` | Delete a task | `delete 1` |
| `find KEYWORD` | Find tasks by description | `find report` |
| `sort` | Show deadlines and events by date | `sort` |
| `bye` | End the conversation | `bye` |

Dates use `YYYY-MM-DD` or `YYYY-MM-DD HHMM` in 24-hour time.

For detailed examples, see the [user guide](docs/README.md).

## Project structure

```text
src/main/java/arn/
├── Arn.java              Application setup and command-response bridge
├── MainWindow.java       JavaFX window controller
├── DialogBox.java        Styled conversation messages
├── Parser.java           Command routing and validation
├── TaskList.java         Task collection operations
├── TaskFileHandler.java  Local persistence
└── Task.java             Base model for Todo, Deadline, and Event

src/main/resources/
├── view/                 FXML layouts
├── styles/               JavaFX stylesheet
└── images/               Arn and user artwork
```

The application follows a small layered design: JavaFX controllers collect input, `Parser` interprets commands, `TaskList` manages the domain objects, and `TaskFileHandler` saves changes to `data/arn.txt`.

## Run the tests

On Windows:

```powershell
.\gradlew.bat clean test
```

On macOS or Linux:

```bash
./gradlew clean test
```

The suite covers task models, parsing, collection behavior, and a JavaFX smoke test that loads the real FXML and CSS and exercises the main input flow.

## Build a runnable JAR

```bash
./gradlew shadowJar
```

The packaged application is written to `build/libs/Arn.jar`.

## Data and privacy

Arn writes task data only to `data/arn.txt` in the application directory. This runtime file is excluded from Git. Avoid storing sensitive information in task descriptions if the project directory is shared or backed up to a public location.

## Artwork

The Arn logo and user avatar were generated with ChatGPT.
