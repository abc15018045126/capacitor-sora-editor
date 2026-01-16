# Notes

A lightweight, offline-first note-taking application built with Capacitor and React.

## Features

- **Completely Offline**: All data is stored locally on your device; no network transmission occurs.
- **Privacy Guaranteed**: No trackers, no ads, and no user data collection.
- **Organization**: Support for **Note Groups** (folder-based) to keep your notes organized by category.
- **Recycle Bin**: Deleted notes are moved to a trash folder first, allowing for easy recovery.
- **Open Format**: Notes are saved as `.txt` files in the `Documents/QuickNotes` directory, making them easily accessible, backable, and syncable with any third-party tools.
- **Cross-Platform**: Built with modern web technologies, compatible with Android, iOS, and Web.

## Usage

1.  **Create**: Tap the `+` button at the bottom right.
2.  **Edit**: Tap a note in the list to enter editor mode. Changes are saved automatically in real-time.
3.  **Delete**: Use the "Move to Trash" option in the editor's menu. Notes in the **Recycle Bin** can be permanently deleted or restored.
4.  **Organize**: Open the sidebar (left swipe or top-left icon) to create and manage note groups.
5.  **Multi-Select**: **Long press** any note in the list to enter selection mode. Select multiple items to move them to a group or delete them in bulk.
6.  **Search**: Use the search bar on the main screen to quickly find notes by content or title.
7.  **Export/Backup**: Since notes are stored as plain text files in your local storage, you can access them directly via any file manager.

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

### Automated Release (GitHub Actions)

This repository is configured with a GitHub Actions workflow that automatically builds, signs, and publishes a new release whenever a tag is pushed.

To release a new version (e.g., `v1.0.1`):

1.  **Commit your changes:**
    ```bash
    git add .
    git commit -m "Update something"
    ```

2.  **Push to GitHub:**
    ```bash
    git push
    ```

3.  **Create and push a version tag:**
    ```bash
    git tag v1.0.1
    git push origin v1.0.1
    ```

Once the tag is pushed, you can monitor the progress in the **Actions** tab of your GitHub repository. When finished, the signed APKs (`arm64-v8a` and `universal`) will be automatically available in the **Releases** section.

## License

This project is open-source and licensed under the [GPL-3.0](LICENSE) License.
