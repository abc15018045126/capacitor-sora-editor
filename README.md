# Notes

A lightweight, offline-first note-taking application built with Capacitor and React.

## Features

- **Completely Offline**: All data is stored locally on your device; no network transmission occurs.
- **Privacy Guaranteed**: No trackers, no ads, and no user data collection.
- **Organization**: Support for **Note Groups** (folder-based) to keep your notes organized by category.
- **Recycle Bin**: Deleted notes are moved to a trash folder first, allowing for easy recovery.
- **Open Format**: Notes are saved in the `Documents/Notes` directory, making them easily accessible, backable, and syncable with any third-party tools. Data survives app uninstalls.
- **Cross-Platform**: Built with modern web technologies, compatible with Android, iOS, and Web.

## Version History

- **v1.0.9 (Latest)**
  - **Performance Optimization**: Deeply optimized for large files (millions of characters). Line numbers and editor remain smooth.
  - **Improved TOC**: Chapter list now automatically scrolls to the active chapter when opened.
  - **Better Jump Logic**: More accurate positioning when jumping to a chapter in large files.
  - **Undo Feature**: Dedicated undo button in the editor menu.
- **v1.0.8**
  - support for all file extension (not just .txt).
  - Storage moved to public `Documents/Notes` to survive uninstalls.
  - Request full file access for Android 11+.
  - Added line numbers toggle and auto-save toggle.
- **v1.0.7**
  - Added Recycle Bin and Trash recovery.
  - Added Group management (rename/delete).
  - Added Multi-select mode for batch operations.
  - Added Editor settings: Custom font size and background colors (White, Beige, Green, Blue, Black).

## Usage

1.  **Create**: Tap the `+` button at the bottom right.
2.  **Edit**: Tap a note in the list to enter editor mode. Changes are saved automatically in real-time.
3.  **Delete**: Use the "Move to Trash" option in the editor's menu. Notes in the **Recycle Bin** can be permanently deleted or restored.
4.  **Organize**: Open the sidebar (left swipe or top-left icon) to create and manage note groups.
5.  **Multi-Select**: **Long press** any note in the list to enter selection mode. Select multiple items to move them to a group or delete them in bulk.
6.  **Search**: Use the search bar on the main screen to quickly find notes by content or title.
7.  **Export/Backup**: Since notes are stored as files in your local storage, you can access them directly via any file manager at `Documents/Notes`.

## Development & Deployment

The project uses [Capacitor](https://capacitorjs.com/) to wrap the React application into a native Android app.

### Local Development

```bash
# Install dependencies
npm install

# Build web assets and sync to Android project
npm run build
npx cap sync android

# Run on a connected Android device
npx cap run android
```

## License

This project is open-source and licensed under the [GPL-3.0](LICENSE) License.
