# Notes

A modern, lightweight, offline-first note-taking application. This project combines a React-based frontend with a powerful native Android editor powered by Jetpack Compose.

## 🚀 Key Features

- **Jetpack Compose Native Editor**: High-performance editor supporting large files with smooth scrolling and real-time line numbers.
- **Deep Theme Customization**: 
  - Independent color settings for Editor, UI (Toolbars), TOC Panel, Search Panel, and Menus.
  - One-click "Sync All" to match all panels with the main UI theme.
- **JSON Configuration Engine**: All settings are persisted via JSON. Advanced users can manually edit the JSON config for pixel-perfect customization.
- **Smart TOC (Table of Contents)**: Intelligent chapter generation for long documents with auto-scrolling synchronization.
- **Privacy & Performance**: 
  - 100% Offline: No data leaves your device.
  - Storage: Notes are saved in `Documents/Notes` to survive app uninstalls and allow external backups.
- **Advanced Search & Replace**: Real-time matching with navigation.

## 🛠 Technology Stack

- **Frontend**: [React](https://reactjs.org/) + [Vite](https://vitejs.dev/)
- **Native Bridge**: [Capacitor](https://capacitorjs.com/)
- **Android UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Editor Core**: Custom integration with Jetpack Compose LazyColumn and PointerInput for high-performance interactions.

## 📦 Build Environment

- **JDK**: 17+
- **Node.js**: 18+
- **Android SDK**: API Level 34+
- **Gradle**: 8.x

## 🚀 How to Build

We provide automated scripts to help you build the project.

### Windows
Run `build_win.bat` in the root directory.

### Linux / macOS
```bash
chmod +x build_linux.sh
./build_linux.sh
```

### Manual Steps
```bash
# 1. Install dependencies
npm install

# 2. Build web assets
npm run build

# 3. Sync to Android project
npx cap sync android

# 4. Build Release APK
cd android
./gradlew :app:assembleRelease
```

## 📈 Version History

- **v1.2.2 (Latest)**
  - **Full Theme Customization**: UI, TOC, and Search panels now support independent coloring.
  - **JSON Persistence**: Settings are now saved automatically and can be manually edited.
  - **Reset Feature**: One-click to restore all default settings.
  - **Improved Scrollbar**: Now support dragging and auto-hide with better touch area.
- **v1.0.9**
  - Performance optimization for massive text files.
  - TOC auto-scroll synchronization.

## 📄 License

This project is open-source and licensed under the [GPL-3.0](LICENSE) License.
