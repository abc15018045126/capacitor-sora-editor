@echo off
REM Android Build Script for Windows
REM Requirements: Android SDK, JDK 17+, Node.js

echo Checking environment...

if "%ANDROID_HOME%"=="" (
    echo Error: ANDROID_HOME is not set.
    exit /b 1
)

echo Installing Web Dependencies...
call npm install

echo Building Web Assets...
call npm run build

echo Syncing with Capacitor...
call npx cap sync android

echo Building Android Release APK...
cd android
call gradlew.bat :app:assembleRelease

echo Done! APK location: android\app\build\outputs\apk\release\app-release.apk
pause
