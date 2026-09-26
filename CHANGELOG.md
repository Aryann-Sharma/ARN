# Changelog

## 0.3.0 — 2026-09-26

### Commands and dates

- Reject invalid calendar dates and times instead of silently adjusting them.
- Preserve an explicitly entered midnight time across saving and reopening.
- Keep task numbers from the saved list in search results and date-sorted views.
- Handle empty input, repeated whitespace, invalid task numbers, and malformed date clauses consistently.
- Keep description searches independent of the system locale.

### Storage

- Save successful changes before reporting success and roll back a change if saving fails.
- Avoid writes for read-only commands and operations that leave tasks unchanged.
- Create the data file only after the first successful change.
- Stop startup on malformed or unreadable saves while preserving the original file.
- Detect saves changed by another session or editor and require a restart before overwriting them.
- Coordinate writers with a lock file and replace data using a complete temporary file.
- Preserve Unicode descriptions, pipe characters, and compatibility with older save files.

### Desktop and console

- Preserve unfinished input when using quick actions.
- Keep failed commands available for correction or retry.
- Preserve the task count after blank input and the reading position when resizing.
- Add visible keyboard focus, an accessible command label, and a wrapping date hint.
- Add `--cli`, `--help`, and `--version` to the packaged application.
- Handle console end of input cleanly and use UTF-8 for console input and output.
- Return a nonzero exit status for console startup failures and invalid launch options.

### Build and verification

- Update the Gradle wrapper, JavaFX, Shadow, and JUnit dependencies.
- Treat compiler warnings as build failures and produce reproducible archives.
- Add regression coverage for parsing, task models, persistence, command rollback, and JavaFX interactions.
- Exercise the fat JAR in separate processes, including persistence across launches, startup failures, bundled native libraries, and desktop rendering.
- Generate JaCoCo coverage reports and packaged GUI screenshots.
- Run CI on Windows, Linux, and Intel macOS and check dependency updates monthly.

The release artifact is `Arn.jar`, for x64 Windows, Linux, and Intel macOS with Java 17.
