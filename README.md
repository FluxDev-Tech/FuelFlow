# FuelFlow Pro ⛽

**FuelFlow Pro** is an industrial-grade gas station automation and retail management dashboard app. Built entirely using modern **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database with SQLite**, this app balances physical fuel pump automation with robust offline-first transaction logging. 

---

## 📱 Application Preview & UI Access
1. **Interactive Sandbox Preview**: Ready to run on AI Studio's Streaming Android Emulator.
2. **Access Security Credentials (Demo Mode)**:
   * **Station Director / Admin**: `admin@fuelflow.com` (Access Code: `admin123`)
   * **Station Cashier / POS**: `cashier@fuelflow.com` (Access Code: `cashier123`)
   * **Pump Attendant**: `attendant@fuelflow.com` (Access Code: `attendant123`)

---

## 🛠️ Tech Stack & Key Components

* **Architecture**: MVVM (Model-View-ViewModel) + Repository pattern adhering strictly to **Unidirectional Data Flow (UDF)**.
* **UI Framework**: Modern **Jetpack Compose** utilizing the Material Design 3 (M3) declarative components system.
* **Local Persistence**: **Room Database** (SQLite) mapping transaction tables, active shifts, employee rosters, fuel tank levels, and customer loyalty points offline with zero network latency.
* **State Management**: Kotlin **Coroutines** + **StateFlow** + `collectAsStateWithLifecycle` for atomic states and reactive UI updates.
* **Simulated Sensor Inputs**: Multi-role dispatcher simulating dynamic flow nozzles and tanker refueling routines on the background thread.

---

## 🚀 Key Functional Features

* **Real-time Tank Monitoring**: Interactive line indicators of fuel capacities, presenting safe levels and flashing reactive indicators when gravity tank levels fall below `30%`.
* **POS Register (Sales)**: Direct calculation of fuel totals corresponding with specific octane grades (liters or dollar amount presets). Auto-deducts inventory on execution.
* **Flexible Dual Navigation**:
  * 📱 **M3 Bottom Navigation Bar**: Quick-access navigation buttons directly at the bottom screen for **Dashboard**, **POS Register**, **Fuel Tanks**, **Nozzle Pumps**, and **Settings**.
  * 🗂️ **Operational Side Drawer**: Collapsible left-hand control ledger containing precise shift logging, customer directories, CSV reports generator, and profile management tools.
* **Staff Security Ledger**: Credentials-guarded session authorizations with dynamic theme adaptations (custom Dark Theme or default System preferences).

---

## ⚙️ Installation & Workspace Setup

To compile, modify, or run **FuelFlow Pro** on your local machine, follow the steps below:

### Prerequisites
* **Android Studio**: Android Studio Jellyfish (2023.3.1) or Koala (2024.1.1) or newer is highly recommended.
* **Java Development Kit**: JDK 17 manual or enterprise setup.
* **Android SDK Build-Tools**: Version `34` or higher.
* **Gradle Build Environment**: Gradle 8.2+ configured with Kotlin DSL.

### 1. Cloned Repository Setup
Clone this repository to your local path:
```bash
git clone https://github.com/aistudio-community/fuelflow-pro.git
cd fuelflow-pro
```

### 2. Gradle Synchronization
Open the root directory in Android Studio. Android Studio will detect the Kotlin DSL configuration and run a Gradle Sync. The version catalog (`gradle/libs.versions.toml`) is used to resolve standard libraries:
* Compose Multitasking Libraries
* Room Compiler (via KSP)
* Coroutines & Extended Material Symbols

---

## 🖥️ Compile & How to Run

### Command Line (Gradle)
To assemble a local Debug APK directly from your terminal:
```bash
gradle assembleDebug
```
The output file will be generated recursively inside the app module build directories:
`/app/build/outputs/apk/debug/app-debug.apk`

### Working with Tests
Run local unit and Robolectric tests (JVM side) to assert transaction flows and SQLite room adapters without an emulator:
```bash
gradle :app:testDebugUnitTest
```

---

## 📦 Production Build & Deployment Tactics

To package a release-ready version of **FuelFlow Pro** for private deployment or Google Play Store distribution, adhere to these recommendations:

### 1. Generating a Signed App Bundle (AAB) or APK
For official Store deployment, always compile an **Android App Bundle (AAB)** rather than an APK. It automatically splits code and assets depending on target hardware layout, reducing install size by up to 50%:
```bash
gradle :app:bundleRelease
```

### 2. Release Keystore Security
Configure a secure release keystore. Create a secure password-protected `signingConfigs` segment inside `app/build.gradle.kts`. Under no circumstances should keystores or key aliases be hardcoded in plain-text. Inject passwords safely through secure environment vaults or CI configuration properties:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_KEYSTORE_PATH") ?: "release.keystore")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }
    
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. Native Optimizer (R8 & Proguard)
* Ensure `isMinifyEnabled = true` and `isShrinkResources = true` are enabled in your release build-type block.
* This activates Proguard/R8 optimization to prune dead modules, decrease string sizes, and obfuscate sensitive class configurations, making reverse engineering of fuel transaction payloads extremely difficult.

---

## 🌟 Visual Guidelines (Visual Polish & Usability)
* **Custom Adaptive Launcher Icon**: Handcrafted foreground layout and custom background (`ic_launcher_background`/`ic_launcher_foreground`) to give the home launcher a high-quality, professional appearance.
* **Cosmic Slate Theme**: Dark-mode primary color schemes featuring amber accents (`#FFC107` / `#090D16`), offering excellent visibility in high-noise or nocturnal work terminals to alleviate eye-fatigue.
