# Contributing

Thanks for taking the time to improve Arn. Keep changes focused, easy to review, and consistent with the existing command-based interaction model.

## Development setup

Arn requires JDK 17. The Gradle wrapper handles the remaining build tooling.

```bash
./gradlew clean test
./gradlew run
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

## Before opening a pull request

- Add or update tests for changed behavior.
- Run `./gradlew clean check shadowJar`.
- Update the README or user guide when commands or setup steps change.
- Keep commit messages short and specific to the change.
- Avoid committing generated build output or local task data.

Pull requests should explain the problem, the approach taken, and how the change was tested.
