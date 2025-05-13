# BudgetEase - Personal Finance Manager

![BudgetEase App Icon Placeholder](app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png)

BudgetEase is a comprehensive Android application designed to empower users to manage their personal finances effectively. It provides tools for tracking income and expenses, setting budget goals, managing categories, and visualizing spending habits.

## Table of Contents

- [Purpose of the App](#purpose-of-the-app)
- [Key Features](#key-features)
- [Design Considerations](#design-considerations)
  - [User Interface (UI) & User Experience (UX)](#user-interface-ui--user-experience-ux)
  - [Architecture](#architecture)
  - [Key Libraries & Technologies](#key-libraries--technologies)
- [GitHub & Version Control](#github--version-control)
- [GitHub Actions & CI/CD](#github-actions--cicd)
- [Screenshots](#screenshots)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Building and Running](#building-and-running)
- [Contributing](#contributing)
- [License](#license)

## Purpose of the App

In today's fast-paced world, keeping track of personal finances can be challenging. BudgetEase aims to simplify this process by providing an intuitive and feature-rich platform for:

*   **Tracking Transactions:** Easily record income and expenses.
*   **Budgeting:** Set monthly budget goals for different spending categories.
*   **Financial Awareness:** Gain insights into spending patterns through visual reports and summaries.
*   **Goal Achievement:** Monitor progress towards financial goals and make informed decisions.
*   **Receipt Management:** Attach digital copies of receipts to transactions for better record-keeping.

The app is designed for individuals who want to take control of their financial well-being, understand where their money goes, and plan for a more secure financial future.

## Key Features

*   **Dashboard:** At-a-glance overview of current balance, total income, total expenses for the month, and remaining budget. Displays recent transactions.
*   **Transaction Management:**
    *   Add, view, and (soon) edit transactions.
    *   Categorize transactions (e.g., Food, Transport, Salary).
    *   Specify transaction type (Income/Expense).
    *   Attach receipt images via camera or gallery.
    *   Date and time picker for accurate logging.
*   **Budget Goal Setting:**
    *   Create monthly budget goals for specific categories (e.g., "Groceries: R1500").
    *   Track spending progress against set goals with visual progress bars.
*   **Category Management:**
    *   Add custom spending/income categories.
    *   Assign unique icons and colors to categories.
    *   Delete user-created categories.
*   **Reporting & Statistics (Planned/In-Progress):**
    *   Pie charts for expense breakdown by category.
    *   Bar charts for income vs. expense comparison.
    *   Line charts for savings/spending trends over time.
*   **User Authentication:** Secure login and registration system.
*   **Profile Management:** View user details and manage app settings.
*   **Theme Customization:** Light, Dark, and System default theme options.
*   **Onboarding:** Introductory flow for new users (if `OnboardingActivity` is used).

## Design Considerations

### User Interface (UI) & User Experience (UX)

*   **Material Design 3:** The app leverages Material Design 3 components and principles for a modern, clean, and intuitive user interface. This includes using `MaterialToolbar`, `BottomNavigationView`, `FloatingActionButton`, `MaterialCardView`, `MaterialButton`, Chips, and Material Dialogs.
*   **Responsive Layouts:** Layouts are designed using `ConstraintLayout` and adaptive techniques (`match_parent`, `wrap_content`, `0dp` for match_constraint, `LinearLayout` with weights) to ensure a good user experience across various phone screen sizes and orientations.
*   **Intuitive Navigation:** Utilizes Android Jetpack Navigation Component for a clear and predictable navigation flow via a `BottomNavigationView` for top-level destinations and standard toolbar navigation for detail screens.
*   **Accessibility:** Considerations include clear visual hierarchy, sufficient touch target sizes, and descriptive content descriptions for UI elements. Color contrast is managed using theme attributes.
*   **Visual Feedback:** Provides visual feedback for user actions (e.g., ripple effects, Snackbar messages for success/error states, progress indicators).
*   **Dark Mode:** Supports dark, light, and system-default themes for user comfort and preference.

### Architecture

BudgetEase follows modern Android architecture best practices:

*   **MVVM (Model-View-ViewModel):** Separates UI logic (Views/Fragments) from business logic and data handling (ViewModels). This promotes testability and maintainability.
*   **Repository Pattern:** Abstracts data sources (local database, potentially remote APIs in the future) from ViewModels. Repositories are responsible for fetching and managing data.
*   **Single Source of Truth:** Data flows unidirectionally, typically from the database (via Room) through the Repository and ViewModel to the UI.
*   **Dependency Injection (Hilt):** Hilt is used for managing dependencies throughout the app, simplifying boilerplate and improving testability.
*   **Android Jetpack Components:**
    *   **Navigation Component:** For handling in-app navigation.
    *   **Room Persistence Library:** For local data storage (users, categories, transactions, budget goals).
    *   **ViewModel:** To store and manage UI-related data in a lifecycle-conscious way.
    *   **LiveData / Kotlin Flows:** Used for observing data changes and updating the UI reactively. `StateFlow` is prominently used for UI state management.
    *   **DataStore:** For storing simple key-value preferences like the selected theme.
    *   **CameraX:** For camera functionalities (attaching receipts).
*   **Coroutines:** For managing asynchronous operations and background tasks, ensuring the UI remains responsive.

### Key Libraries & Technologies

*   **Kotlin:** The primary programming language.
*   **Android Jetpack:**
    *   Navigation Component
    *   Room
    *   ViewModel
    *   LiveData & Kotlin Flow
    *   Hilt (for Dependency Injection)
    *   DataStore
    *   CameraX
*   **Material Components for Android:** For UI elements and themes.
*   **Glide:** For efficient image loading and caching (e.g., receipt previews).
*   **MPAndroidChart (Potentially, based on StatisticsFragment):** For displaying charts and graphs.
*   **Coroutines:** For asynchronous programming.

## GitHub & Version Control

GitHub was utilized as the central platform for version control and collaboration (even if developing solo, it's a best practice).

*   **Repository:** The project is hosted in a GitHub repository.
*   **Commits:** Atomic commits were made to track changes logically. Commit messages (ideally) followed conventional commit standards for clarity.
*   **Branching (Recommended Strategy):**
    *   `main` (or `master`): Stable, production-ready code.
    *   `develop`: Integration branch for ongoing development and features.
    *   `feature/<feature-name>`: Branches for developing new features (e.g., `feature/budget-goal-progress`).
    *   `fix/<issue-number>`: Branches for bug fixes.
    *   `release/<version>`: Branches for preparing releases.
*   **Pull Requests (PRs):** For larger features or when collaborating, PRs would be used to review code before merging into `develop` or `main`.
*   **Issue Tracking:** GitHub Issues could be used to track bugs, feature requests, and tasks.


## GitHub Actions & CI/CD

GitHub Actions can be leveraged for Continuous Integration and Continuous Delivery (CI/CD) to automate the build, test, and deployment pipeline.

A typical workflow file (e.g., `.github/workflows/android_ci.yml`) might include:

1.  **Trigger:** On push to `develop` or `main`, or on pull request creation.
2.  **Setup:**
    *   Checkout the code.
    *   Set up JDK.
    *   Set up Android SDK.
    *   Cache Gradle dependencies.
3.  **Build:**
    *   Run `./gradlew assembleDebug` or `./gradlew assembleRelease` to build the APK.
4.  **Test:**
    *   Run `./gradlew test` for unit tests.
    *   Run `./gradlew connectedCheck` for instrumented tests (requires an emulator or device).
5.  **Linting & Static Analysis:**
    *   Run `./gradlew lint` to check for code quality issues.
6.  **Artifacts:**
    *   Upload the generated APK as a build artifact.
7.  **Notifications (Optional):** Notify on success or failure (e.g., via Slack or email).
8.  **Deployment (Optional for Releases):**
    *   Automate deployment to Firebase App Distribution or Google Play Store on tagged releases or merges to the `main` branch.

## Screenshots

## Getting Started

### Prerequisites

*   Android Studio (latest stable version recommended - e.g., Hedgehog, Iguana)
*   Android SDK (latest version)
*   JDK 17 (as specified in `build.gradle.kts` or project settings)
*   An Android Emulator or a physical Android device (API Level 26+ recommended, based on typical CameraX support)

### Building and Running

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/OleTab22/BudgetEase-Prototype.git
    cd BudgetEase
    ```
   
2.  **Open in Android Studio:**
    *   Open Android Studio.
    *   Click on "Open" (or "Open an Existing Project").
    *   Navigate to the cloned `BudgetEase` directory and select it.

3.  **Sync Gradle:**
    *   Android Studio should automatically start a Gradle sync. If not, click "Sync Project with Gradle Files" (elephant icon in the toolbar).

4.  **Build the project:**
    *   Go to `Build > Make Project` or click the "Make Project" button (hammer icon).

5.  **Run the app:**
    *   Select a run configuration (usually `app`).
    *   Choose an available emulator or connect a physical device.
    *   Click the "Run 'app'" button (green play icon).
      
## Link to video
https://youtu.be/bE8zsW_5qEY?si=LTa91WjOZUNjy9iS

## Contributing

Contributions are welcome! If you'd like to contribute to BudgetEase, please follow these steps:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-amazing-feature`).
3.  Make your changes.
4.  Commit your changes (`git commit -m 'Add some amazing feature'`).
5.  Push to the branch (`git push origin feature/your-amazing-feature`).
6.  Open a Pull Request.

Please ensure your code adheres to the existing coding style and includes tests where appropriate.

## License

This project is licensed under the [MIT License](LICENSE.md) - see the `LICENSE.md` file for details (if you choose to add one).

*(User: You'll need to create a `LICENSE.md` file if you want to include a specific license. MIT is a common choice for open-source projects.)*

---

Readme generated with assistance from an AI Pair Programmer.
User: Please customize image paths, GitHub URLs, and add a `LICENSE.md` file if desired.
