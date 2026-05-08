# TasKalender

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)

**TasKalender** is a comprehensive personal organizer Android application designed to help users seamlessly manage their daily lives. By combining finance tracking, task management, and note-taking into a single, intuitive interface, TasKalender ensures you stay on top of your productivity and personal goals.

## ✨ Features

*   💸 **Cash & Finance Management**
    *   Track income and expenses easily.
    *   Categorize transactions to understand your spending habits.
    *   View transaction histories by date.
*   ✅ **Task Management**
    *   Create and manage your daily tasks.
    *   Mark tasks as completed and stay organized.
*   📝 **Notes & Folders**
    *   Write, edit, and organize rich notes.
    *   Group notes into custom folders for better structure.
*   👤 **User Authentication & Profile**
    *   Secure user registration and login.
    *   Manage and edit user profiles.

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **UI:** Android Views (XML layouts, Fragments, Activities, Bottom Sheets)
*   **Build System:** Gradle (Kotlin DSL)

## 🚀 Getting Started

### Prerequisites

*   [Android Studio](https://developer.android.com/studio) (Latest version recommended)
*   Android SDK

### Installation

1.  **Clone the repository**
    ```bash
    git clone https://github.com/alexcsl/TasKalender.git
    ```
2.  **Open the project**
    *   Launch Android Studio.
    *   Select `Open` and navigate to the cloned `TasKalender` directory.
3.  **Sync Gradle**
    *   Wait for Android Studio to sync the Gradle files.
4.  **Add Firebase Configuration (If Applicable)**
    *   Create an Android project in your Firebase Console.
    *   Download the `google-services.json` file.
    *   Place the `google-services.json` file into the `app/` directory. *(Note: This file is ignored by Git for security).*
5.  **Run the App**
    *   Connect an Android device or start an Emulator.
    *   Click the `Run` button (green play icon) in Android Studio.

## 📁 Project Structure

*   `app/src/main/java/com/example/tectest/data`: Contains Data Models and Repositories.
*   `app/src/main/java/com/example/tectest/ui`: Contains UI components (Activities, Fragments, Adapters) organized by feature (cash, home, login, notes, profile, task).
*   `app/src/main/java/com/example/tectest/viewmodel`: Contains the ViewModels for handling business logic.
*   `app/src/main/res`: Contains Android resources (layouts, drawables, colors, strings).

## 🔒 Security Note

Please ensure that sensitive files like `.env` and `app/google-services.json` are **never** committed to version control. The repository has been configured with `.gitignore` to prevent accidental uploads of these files.

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
