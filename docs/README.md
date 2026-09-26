# Arn User Guide

![Arn logo](../src/main/resources/images/ArnLogo.png)

Arn keeps todos, deadlines, and events in a local task list. You can use the desktop interface or enter the same commands in a terminal.

## Getting started

Download `Arn.jar` from the [releases page](https://github.com/Aryann-Sharma/ARN/releases). Install a 64-bit Java 17 runtime for x64 Windows, Linux, or Intel macOS, then open a terminal in the directory where you want your task data:

```bash
java -jar Arn.jar
```

JavaFX is included in the JAR. Native ARM libraries are not included in this release.

Type in the input field and press **Enter** or select **Send**. The quick actions are:

- **Show tasks** runs `list`.
- **By date** runs `sort`.
- **Examples** shows sample commands.

The header shows how many tasks are saved. Quick actions preserve your unfinished input. If a command fails, its text stays in the input field so you can correct or retry it. Empty input does not change the task count. New messages scroll into view; resizing the window while reading older messages does not force you to the bottom.

## Command and date rules

Enter one command per line. Command names are case-sensitive and lowercase. Descriptions can contain spaces, Unicode text, and the `|` character. Leading and trailing whitespace is removed, and a description must not be empty.

Dates use exactly `YYYY-MM-DD` or `YYYY-MM-DD HHMM`, with zero-padded fields and 24-hour time. For example:

| Accepted | Rejected |
| --- | --- |
| `2026-10-02` | `2026-2-2` |
| `2026-10-02 1800` | `2026-10-02 18:00` |
| `2028-02-29` | `2026-02-29` |
| `2026-10-02 0000` | `2026-10-02 2400` |

Dates must exist on the calendar. Midnight entered as `0000` remains a timed task when saved and reopened. Dates do not include a time zone.

## Add tasks

A todo has a description and no date:

```text
todo Read a chapter
```

```text
added: [T][ ] Read a chapter
```

A deadline has one due date:

```text
deadline Submit report /by 2026-10-02 1800
```

```text
added: [D][ ] Submit report (by Oct 2 2026, 6:00PM)
```

An event has a start and an end:

```text
event Team lunch /from 2026-10-04 1200 /to 2026-10-04 1330
```

```text
added: [E][ ] Team lunch (from Oct 4 2026, 12:00PM to Oct 4 2026, 1:30PM)
```

Both event dates must include a time or both omit it. The end must be equal to or later than the start. A date-only event can be entered like this:

```text
event Workshop /from 2026-10-05 /to 2026-10-06
```

Keep a space around the date clauses, as shown above.

## List tasks

`list` displays every task in its saved order. The following list, search, and sort examples assume you have added only Read a chapter, Submit report, and Team lunch, and have left them incomplete:

```text
1. [T][ ] Read a chapter
2. [D][ ] Submit report (by Oct 2 2026, 6:00PM)
3. [E][ ] Team lunch (from Oct 4 2026, 12:00PM to Oct 4 2026, 1:30PM)
```

`[T]`, `[D]`, and `[E]` identify todos, deadlines, and events. `[ ]` means incomplete and `[X]` means complete.

## Find tasks

`find KEYWORD` searches for text anywhere in a description, ignoring letter case:

```text
find REPORT
```

```text
Here are the matching tasks in your list:
2. [D][ ] Submit report (by Oct 2 2026, 6:00PM)
```

Search results keep the task numbers from `list`. In this example, use `mark 2` to complete the matching task.

## View tasks by date

`sort` displays deadlines and events in chronological order. Events use their start date, and todos are excluded:

```text
2. [D][ ] Submit report (by Oct 2 2026, 6:00PM)
3. [E][ ] Team lunch (from Oct 4 2026, 12:00PM to Oct 4 2026, 1:30PM)
```

This view keeps the saved task numbers and does not change the order of `list`. Tasks with equal dates keep their relative order.

## Mark or remove tasks

Use the displayed number to update a task:

| Command | Effect |
| --- | --- |
| `mark 2` | Mark Submit report complete |
| `unmark 2` | Mark Submit report incomplete |
| `delete 2` | Remove Submit report |

Task numbers start at 1. Deleting a task shifts the numbers of later tasks, so use `list` to check the current numbering before another update.

Marking an already completed task, or unmarking an already incomplete task, leaves the save file unchanged.

## Console mode

```bash
java -jar Arn.jar --cli
```

The console accepts the same commands, one per line. It exits on `bye` or end of input, so a UTF-8 command file can also be redirected into it. Use a UTF-8 terminal for non-ASCII text.

```bash
java -jar Arn.jar --help
java -jar Arn.jar --version
```

`--help` prints the available options and `--version` prints the release version. An unknown option returns exit code 2. A startup failure caused by unreadable or malformed task data returns exit code 1.

Command errors are printed and the session continues. Exit code 0 means the session ended normally; it does not mean every command succeeded.

In the desktop application, `bye` displays a farewell. Close the window to exit.

## Saving and recovering data

Arn saves tasks in `data/arn.txt`, relative to the directory from which it is launched. Launching the JAR from a different directory uses a different task file. The file is created on the first successful change; opening the app or using `list`, `find`, `sort`, or `bye` does not create or rewrite it.

The save file is UTF-8 plain text with a version header. Older unversioned files are supported if they use UTF-8 and contain tasks that meet the current validation rules. A save writes a complete temporary copy before replacing the active file, using atomic replacement where the file system supports it. The `arn.txt.lock` file normally remains beside the save file after exit.

| Problem | What happens and how to recover |
| --- | --- |
| Invalid command or date | Arn shows an error and leaves your tasks unchanged. Correct the input and retry. |
| Save fails | The change is rolled back. Check the reported error and whether the data directory is writable, then retry. The desktop keeps the command in the input field; in the console, enter it again. |
| Another instance is saving | The change is rejected. Wait for that save to finish and retry; a subsequent conflict may require a restart. |
| The save file changed outside this session | Arn refuses to overwrite the newer data. Restart to load it before making changes. |
| Saved data is malformed or unreadable | Startup stops and leaves the file intact. Make a backup, inspect the reported problem, and correct the file or restore a known good copy before restarting. |

To back up or move your tasks, close Arn and copy `data/arn.txt`. Keep a backup before editing it manually. Task descriptions are not encrypted.

Some older releases used the operating system's default text encoding. If an older file is rejected as invalid UTF-8, keep the original backup, open a copy in an editor using its original encoding, and save it as UTF-8. Older files may also contain dates or event ranges that are no longer accepted; correct the reported task using the rules above before restarting.

See the [project README](../README.md) for build and test instructions and the [changelog](../CHANGELOG.md) for release changes.
