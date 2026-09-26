# Changelog

## Unreleased

- Reschedule an event's start or end independently, retaining the omitted endpoint.
- Retain existing event times when rescheduling with date-only values, including when both endpoints are supplied; show the full resulting range in responses.
- Document retained times and validate the complete event before saving or adding undo history.
- Keep new responses visible by updating message layout before scrolling, including wrapped errors in long conversations.
- Edit task descriptions without changing their type, position, completion status, or dates.
- Reschedule deadlines and events with the existing date formats and validation rules.
- Undo up to 100 saved changes in the current session, including additions, deletions, marking, editing, and rescheduling.
- Preserve tasks and undo history when saving fails, and skip saves and history entries for unchanged values.
- Add desktop examples and tests for editing, rescheduling, undo, and persistence across launches.

## 0.3.1 — 2026-09-26

- Explain missing spaces around date markers, missing arguments, repeated markers, and event markers in the wrong order, with examples of valid commands.
- Distinguish date format errors from nonexistent calendar dates and invalid times. Identify whether a problem is in the deadline date or an event's start or end.
- Report available task numbers and give clearer guidance for unknown commands, letter case, and extra arguments.
- Include the save path and recovery advice in storage errors, and explain malformed records without discarding their validation details.
- Make desktop `bye` display a farewell for three seconds before closing. Disable command input and quick actions during the pause.
- Add regression tests for error explanations, recovery examples, and closing the packaged desktop app.

## 0.3.0 — 2026-09-26

### Commands and dates

- Validate calendar dates, times, and event ranges. Preserve an explicitly entered midnight time across saving and reopening.
- Keep task numbers from the saved list in search results and date-sorted views.
- Handle empty input, repeated whitespace, invalid task numbers, and malformed date clauses consistently. Keep description searches independent of the system locale.

### Storage

- Save successful changes before reporting success and roll back a change if saving fails.
- Create the data file only after the first successful change. Avoid writes for commands that leave tasks unchanged.
- Stop startup on malformed or unreadable saves while preserving the original file.
- Detect files changed by another session or editor and require a restart to load the newer data.
- Coordinate writers with a lock file and replace data using a complete temporary file.
- Preserve Unicode descriptions and pipe characters. Continue to read unversioned UTF-8 files that contain valid tasks.

### Desktop and console

- Preserve unfinished input when using quick actions and keep failed commands in the desktop input field for correction.
- Preserve the task count after blank input and the reading position when resizing.
- Add visible keyboard focus, an accessible command label, and a wrapping date hint.
- Add `--cli`, `--help`, and `--version` to the packaged application.
- Use UTF-8 in the console and exit cleanly at end of input. Return a nonzero exit status for console startup failures and invalid launch options.

### Build and verification

- Update the Gradle wrapper, JavaFX, Shadow, and JUnit dependencies.
- Treat compiler warnings as build failures and produce reproducible archives.
- Add regression coverage for parsing, task models, persistence, command rollback, and JavaFX interactions.
- Test the fat JAR in separate processes, including persistence across launches, startup failures, bundled native libraries, and desktop rendering.
- Generate JaCoCo coverage reports and packaged GUI screenshots.
- Run CI on Windows, Linux, and Intel macOS. Configure monthly dependency checks with Dependabot.

The release artifact is `Arn.jar`, for x64 Windows, Linux, and Intel macOS with Java 17.
