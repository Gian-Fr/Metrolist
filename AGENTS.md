# Working with Metrolist as an AI agent

Metrolist is a 3rd party YouTube Music client written in Kotlin. It follows material 3 design guidelines closely.

## Environment setup

If this is your first time working with Metrolist, please follow instructions below

1. Initialize git submodules

```bash
git submodule update --init --recursive
```

2. Install protoc and protoc-gen-go in your package manager of choice.
3. Create your debug keystore

```bash
[ ! -f "app/persistent-debug.keystore" ] && keytool -genkeypair -v -keystore app/persistent-debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" || echo "Keystore already exists."
```

4. Generate the protobuf files

```bash
cd app
bash generate_proto.sh
cd ..
```

5. Build the app. Ensure you're in the root directory of the project when running this command.

```bash
./gradlew :app:assembleuniversalFossDebug
```

6. The built apk will be located in `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`.

## Rules for working on the project

1. Always create a new branch for your feature work. Follow these naming conventions:
   - Bug fixes: `fix/short-description`
   - New features: `feature/short-description`
   - Refactoring: `refactor/short-description`
   - Documentation: `docs/short-description`
   - Chores: `chore/short-description`
2. Branch descriptions should be concise yet descriptive enough to understand the purpose of the branch at a glance.
3. Always pull the latest changes from `main` before starting your work to minimize merge conflicts.
4. While working on your feature you should rebase your branch on top of the latest `main` at least once a day to ensure compatibility.
5. Commit names should be clear and follow the format: `type(scope): short description`. For example: `feat(ui): add dark mode support`. Including the scope is optional.

## AI-only guidelines

1. You are strictly prohibited from making ANY changes to the readme/markdown files, including this one. This is to ensure that the documentation remains accurate and consistent for all contributors.
2. You are NOT allowed to use the following commands:
   - `git push`
   - `git commit`
   - destructive `gh` commands
   - `rm` or other destructive commands that don't have a clear undo path
   - You should absolutely NOT use any commands that would modify the git history, do force pushes (except or rebases on your own branch), or delete branches without explicit instructions from a human.
3. Always follow the guidelines and instructions provided by human contributors.
4. Ensure the absolutely highest code quality in all contributions, including proper formatting, clear variable naming, and comprehensive comments where necessary.
5. Comments should be added only for complex logic or non-obvious code. Avoid redundant comments that simply restate what the code does.
6. Prioritize performance, battery efficiency, and maintainability in all code contributions. Always consider the impact of your changes on the overall user experience and app performance.
7. If you have any doubts ask a human contributor. This can be done using the askQuestions tool if you're running in GitHub Copilot. I don't know if other agents have these type of tools.

## Building and testing your changes

1. After making changes to the code, you should build the app to ensure that there are no compilation errors. Use the following command from the root directory of the project:

```bash
./gradlew :app:assembleuniversalFossDebug
```

2. If the build is not successful, review the error messages, fix the issues in your code, and try building again.
3. Once the build is successful, you can test your changes on an emulator or a physical device. Install the generated APK located at `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk` and ask a human for help testing the specific features you worked on.
