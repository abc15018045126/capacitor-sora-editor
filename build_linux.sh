#!/bin/bash

# Android Build Script for Linux/macOS
# Requirements: Android SDK, JDK 17+

echo "Checking environment..."

if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME is not set."
    exit 1
fi

echo "Installing Web Dependencies..."
npm install

echo "Building Web Assets..."
npm run build

echo "Syncing with Capacitor..."
npx cap sync android

echo "Building Android Release APK..."
cd android
./gradlew :app:assembleRelease

echo "Done! APK location: android/app/build/outputs/apk/release/app-release.apk"
