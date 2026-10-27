# BudgetEase

**A Kotlin Android personal-finance project for organising transactions, budgets, and spending categories.**

[Watch the app walkthrough](https://youtu.be/bE8zsW_5qEY) · [Explore the code](https://github.com/OleTab22/BudgetEase-Prototype)

BudgetEase is designed around a familiar personal-finance task: record income and expenses, organise spending, set category budgets, and understand where money goes. The repository combines an Android interface with local data, navigation, and dependency-injection components.

## App areas

| Area | Included screens and components |
| --- | --- |
| **Overview** | Dashboard and wallet screens |
| **Transactions** | Add-transaction, transaction-list, and transaction-detail interfaces |
| **Budgets** | Budget screen, budget-goal dialog, and progress-related components |
| **Categories** | Category management and category icon/colour selection |
| **Accounts** | Login, registration, password-recovery, onboarding, and profile screens |
| **Insights** | Statistics interface; reporting features are still an area for further development |

The table describes components present in the repository. Some flows and reporting features may be incomplete; this is an evolving prototype.

## Technologies

- **Language:** Kotlin
- **UI:** XML layouts, Material Components, view/data binding, and Compose dependencies
- **App architecture:** ViewModel, repository/data layers, Hilt dependency injection
- **Local storage:** Room, DataStore
- **Navigation and concurrency:** Jetpack Navigation, Kotlin coroutines, WorkManager
- **Additional libraries:** CameraX, Glide, MPAndroidChart
- **Testing tools:** JUnit, MockK, AndroidX test libraries

## Repository structure

| Path | Purpose |
| --- | --- |
| [`app/src/main/java/com/budgetease/ui/`](app/src/main/java/com/budgetease/ui/) | Screens organised by feature: authentication, budget, category, dashboard, statistics, transactions, wallet |
| [`app/src/main/java/com/budgetease/data/`](app/src/main/java/com/budgetease/data/) | Local data, models, preferences, and repository layer |
| [`app/src/main/java/com/budgetease/di/`](app/src/main/java/com/budgetease/di/) | Dependency-injection wiring |
| [`app/src/main/res/layout/`](app/src/main/res/layout/) | Android UI layout files |
| [`app/src/test/`](app/src/test/) | Unit-test source directory |
| [`.github/workflows/build.yml`](.github/workflows/build.yml) | Android build/test automation configuration |

## Run locally

**Requirements:** Android Studio, JDK 17, Android SDK 35, and an emulator or connected Android device.

1. Clone the project:

   ```bash
   git clone https://github.com/OleTab22/BudgetEase-Prototype.git
   cd BudgetEase-Prototype
   ```

2. Open the cloned folder in Android Studio and let Gradle synchronise.
3. Select an emulator or device, then run the `app` configuration.

Command-line examples:

```bash
./gradlew :app:assembleDebug
./gradlew test
```

On Windows, use `gradlew.bat`. The app targets Android SDK 35 and JVM 11 bytecode; JDK 17 is used to run the Android Gradle build tools.

## Testing and current status

The repository includes JUnit/MockK dependencies and test sources. A [GitHub Actions run on 27 October 2026](https://github.com/OleTab22/BudgetEase-Prototype/actions/runs/40485414281) successfully built the debug APK and completed the JVM unit-test task using JDK 17. The updated workflow is proposed in [PR #2](https://github.com/OleTab22/BudgetEase-Prototype/pull/2).

This confirms the build and test task complete in CI; it does not establish complete feature coverage or end-to-end correctness.

The [video walkthrough](https://youtu.be/bE8zsW_5qEY) is the best starting point for seeing the interface. Planned enhancements include fuller reporting, more complete editing flows, and further end-to-end verification.

## Author

Developed by **Olebogeng Tabane** as part of a practical Android development portfolio.
