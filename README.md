# JumpScareLockScreen

A prank Android app that shows a fake lock screen and jump scares anyone who tries to unlock your phone.

## How it works
- Shows a convincing fake lock screen (time, date, lock icon)
- When someone swipes up or taps to unlock → **scary face + loud alarm sound + vibration**
- Automatically fires every time the screen turns on (even from sleep)
- Runs silently in the background using a foreground service

---

## Setup in Termux (no PC needed)

### 1. Install required packages in Termux
```sh
pkg update && pkg upgrade -y
pkg install git openjdk-17 -y
```

### 2. Clone this repo
```sh
git clone https://github.com/YOUR_USERNAME/JumpScareLockScreen.git
cd JumpScareLockScreen
```

### 3. Set JAVA_HOME
```sh
export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```
Add these lines to `~/.bashrc` so they persist:
```sh
echo 'export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
```

### 4. Download the Android SDK command-line tools
```sh
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
curl -o tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip tools.zip
mv cmdline-tools latest
export ANDROID_HOME=~/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

### 5. Accept licenses & install SDK components
```sh
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### 6. Build the APK
```sh
cd ~/JumpScareLockScreen
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 7. Install it
```sh
adb install app/build/outputs/apk/debug/app-debug.apk
```
Or copy it to your Downloads folder and install via file manager (enable "Install unknown apps" in Settings first).

---

## After installing
1. Open the app once manually to grant permissions
2. It will ask for "Display over other apps" — grant it
3. Done — lock your phone and hand it to someone 😈

## To uninstall / disable the prank
Go to Settings → Apps → "System" → Uninstall
(The app is named "System" so it looks innocent)
