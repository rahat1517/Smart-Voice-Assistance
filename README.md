# RahatAssistant (Type-2 Voice Assistant Skeleton)

Wake word: **"hey rahat"** (SpeechRecognizer partial results, foreground service)

## Important
This zip does **not** include Gradle Wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`) because it cannot be generated here.
You have 2 easy options:

### Option A (Recommended): Android Studio
1) Open Android Studio
2) File → Open → select this folder
3) Let it sync (if it asks for Gradle wrapper, use **Gradle installed** or generate wrapper)

### Option B: Generate wrapper locally
If you have `gradle` installed:
```bash
gradle wrapper
```

Then you can run:
```bash
./gradlew assembleDebug
```
