# Net Worth Tracker

An Android app to track your personal net worth across assets and liabilities. All data is stored locally on your device, with optional Google Drive backup.

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
- **Bank accounts** (savings, current, salary, credit card, overdraft) alongside assets and liabilities
- **Add, edit, and delete** entries with name, amount, currency, and notes
- **Asset transactions** and return metrics on holdings
- **Live USD/INR conversion** — fetches the current exchange rate on launch (cached for 1 hour, with manual refresh)
- **Compact Indian formatting** (Lakhs / Crores)
- **Investment Calculator** — SIP or lumpsum projections with annual step-up, year-wise breakdown (calendar years), and JSON download/load of input + output
- **Loan EMI Calculator** — EMI schedule with optional prepayments (reduce tenure or EMI), year-wise breakdown from the loan start year, and JSON download/load of input + output
- **PIN lock** for app access
- **Local JSON backup & restore** to `Documents/NetWorthTracker/`
- **Google Drive backup & restore** to a visible `My Drive/NetWorthTracker/` folder

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room Database (local persistence)
- MVVM architecture
- Navigation Compose
- Google Identity Authorization + Drive REST API

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

## Google Drive setup

Drive backup needs a Google Cloud OAuth client for this app:

1. Open [Google Cloud Console](https://console.cloud.google.com/) and create (or select) a project
2. Enable **Google Drive API**
3. Configure the **OAuth consent screen** (External is fine for personal use). Add yourself as a test user while the app is in Testing
4. Create credentials → **OAuth client ID** → Application type **Android**
   - Package name: `com.networth.tracker`
   - SHA-1: from your debug keystore, e.g.

```bash
keytool -list -v -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android -keypass android
```

5. Rebuild and run the app, open **Backup**, then **Connect Google Drive**

Backups are stored in a visible **My Drive → NetWorthTracker** folder. Restore pulls the latest `net_worth_backup_*.json` from that folder.

If you previously connected with the old private app-data permission, tap **Disconnect**, then **Connect Google Drive** again so the new Drive folder permission is granted.

## Usage

1. Tap **+** to add an asset, liability, or bank account
2. Pick a category, enter name and amount
3. US Stocks default to USD; other categories default to INR
4. View your net worth on the home screen — liabilities are subtracted automatically
5. Open the menu for **Investment Calculator** or **Loan EMI Calculator**
6. After calculating, use **Download JSON** to save the scenario (inputs + results), or **Load JSON** to restore one
7. Use the **Backup** tab for local JSON and Google Drive backup/restore

### Calculator scenario JSON

Both calculators export a JSON file with `type`, `version`, `input`, and `output`:

| Calculator | `type` | Suggested filename |
|------------|--------|--------------------|
| Investment | `investment_projection` | `investment_projection_yyyyMMdd_HHmmss.json` |
| Loan EMI | `loan_emi` | `loan_emi_yyyyMMdd_HHmmss.json` |

- **Download JSON** is available after a successful calculation
- **Load JSON** restores the inputs and recalculates results in the app
- Year-wise tables show calendar years (current year for Investment; loan start year for Loan EMI)

## Project Structure

```
app/src/main/java/com/networth/tracker/
├── data/           # Room entities, DAO, repository, calculators, backup & scenario JSON
├── ui/screens/     # Dashboard, Add/Edit, Backup, calculators
├── ui/theme/       # Material 3 theme
├── viewmodel/      # ViewModels
└── util/           # Formatting helpers
```

## License

MIT
