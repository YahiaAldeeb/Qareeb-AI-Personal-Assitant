# Qareeb AI Personal Assistant

## Project Structure
- `Qareeb App/` - Android Kotlin/Compose app
- `mobile_backend/` - Python FastAPI backend with Droidrun

## Android App (Qareeb App)
- **Build**: `.\gradlew assembleDebug` (run from Qareeb App directory)
- **SDK**: compileSdk=36, minSdk=26, targetSdk=36
- **Key deps**: Room, Retrofit, Porcupine (wake word), Vosk (STT), Lottie animations
- **Run in Android Studio**: Open Qareeb App folder, wait for Gradle sync, press Shift+F10

## Backend (mobile_backend)
- **Run**: `uvicorn app.main:app --host 0.0.0.0 --port 8000`
- **Deps**: FastAPI, Groq LLM, Droidrun, PostgreSQL (Supabase)
- **APIs**: Users, Tasks, Transactions, Sync, AI (transcription + automation)

## Emulator Networking (CRITICAL GOTCHA)
- Emulator accesses host via `10.0.2.2`, NOT `192.168.1.7`
- Files to update for emulator:
  - `Qareeb App/app/src/main/java/com/example/qareeb/NetworkModule.kt` (line 11)
  - `Qareeb App/app/src/main/java/com/example/qareeb/data/remote/RetrofitInstance.kt` (line 19)
- Change `192.168.1.7:8000` → `10.0.2.2:8000`

## Droidrun (UI Automation)
- **Setup**: Run `droidrun setup` to install Portal app on Android device
- **Verify**: Run `droidrun ping` to confirm connection
- **Portal app**: `com.droidrun.portal` - enables accessibility-based UI automation

## API Keys (in mobile_backend/.env)
- GROQ_API_KEY: For LLM transcription and intent classification
- DATABASE_URL: Supabase PostgreSQL connection string