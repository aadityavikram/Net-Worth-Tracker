# Net Worth Tracker

An Android app to track your personal net worth across assets and liabilities. All data is stored locally on your device.

**Repository:** [github.com/aadityavikram/Net-Worth-Tracker](https://github.com/aadityavikram/Net-Worth-Tracker)

## Features

- **Dashboard** with total net worth, assets vs liabilities, and category breakdown
- **Track 8 categories:**
  - US Stocks (USD)
  - Mutual Funds (INR)
  - Indian Stocks (INR)
  - EPF (INR)
  - NPS (INR)
  - Gold (INR)
  - Real Estate (INR)
  - Home Loan / Liabilities (INR)
- **Add, edit, and delete** entries with name, amount, currency, and notes
- **Live USD/INR conversion** — fetches the current exchange rate on launch (cached for 1 hour, with manual refresh)
- **Compact Indian formatting** (Lakhs / Crores)

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room Database (local persistence)
- MVVM architecture
- Navigation Compose

## Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android SDK 35
- Min SDK 26 (Android 8.0)

## Getting Started

1. Open the project folder in Android Studio
2. Let Gradle sync complete
3. Run on an emulator or physical device (▶ Run)

### Build from command line

```bash
# Generate Gradle wrapper (first time only, if gradlew is missing)
gradle wrapper

# Build debug APK
./gradlew assembleDebug        # macOS/Linux
gradlew.bat assembleDebug      # Windows
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Usage

1. Tap **+** to add an asset or liability
2. Pick a category, enter name and amount
3. US Stocks default to USD; other categories default to INR
4. View your net worth on the home screen — liabilities are subtracted automatically

## Project Structure

```
app/src/main/java/com/networth/tracker/
├── data/           # Room entities, DAO, repository
├── ui/screens/     # Dashboard & Add/Edit screens
├── ui/theme/       # Material 3 theme
├── viewmodel/      # ViewModels
└── util/           # Formatting helpers
```

## License

MIT
