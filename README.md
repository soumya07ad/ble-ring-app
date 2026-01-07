# Smart Ring BLE Android App

A native Android application for connecting and managing YCBT smart rings via Bluetooth Low Energy (BLE).

## 📋 Project Information

**App Name:** Wellness (Smart Ring Fitness Tracker)  
**Package:** `com.fitness.app`  
**Min SDK:** 26 (Android 8.0)  
**Target SDK:** 34 (Android 14)  
**Language:** Kotlin  
**Architecture:** MVVM with Jetpack Compose

---

## 🛠️ Development Requirements

### Required Software
- **Android Studio:** Hedgehog (2023.1.1) or later
- **JDK:** 17 or later
- **Gradle:** 8.2+ (included in project)
- **Kotlin:** 1.9.0+

### Required Hardware for Testing
- **Android Device:** Android 8.0+ (API 26+)
- **Bluetooth:** BLE-capable device
- **Smart Ring:** YCBT-compatible smart ring (e.g., R9 model)

---

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/soumya07ad/ble-ring-app.git
cd ble-ring-app
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Click **File → Open**
3. Navigate to the cloned `ble-ring-app` folder
4. Click **OK**
5. Wait for Gradle sync to complete (may take 2-5 minutes on first open)

### 3. Verify SDK Setup

If Android Studio prompts for SDK installation:
- Accept the Android SDK installation
- Install any missing SDK components
- Sync project again

### 4. Build the Project

```bash
# From Android Studio Terminal or command line:
./gradlew clean build
```

Or use Android Studio's **Build → Rebuild Project**

### 5. Run on Device

1. **Enable Developer Options** on your Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Go back to Settings → System → Developer Options
   - Enable **USB Debugging**

2. **Connect device via USB**

3. **Click Run** (green play button) in Android Studio

4. **Select your device** from the deployment target list

---

## 📱 Required Permissions

The app requires the following runtime permissions:

### Android 12+ (API 31+)
- `BLUETOOTH_SCAN` - To scan for nearby smart rings
- `BLUETOOTH_CONNECT` - To connect to the ring
- `ACCESS_FINE_LOCATION` - Required for BLE scanning

### Android 8-11 (API 26-30)
- `BLUETOOTH` - Basic Bluetooth access
- `BLUETOOTH_ADMIN` - Bluetooth management
- `ACCESS_FINE_LOCATION` - Required for BLE scanning

**Note:** The app will request these permissions at runtime when you first tap "Scan for Devices"

---

## 🧪 Testing Instructions for Office Testers

### Basic Connection Test

1. **Launch the app**
2. **Navigate to "Pair Your Smart Ring" screen**
3. **Grant permissions** when prompted
4. **Tap "Scan for Devices"**
5. **Wait 6 seconds** for scan to complete
6. **Verify ring appears** in "Available Devices" list
7. **Tap "Pair Device"**
8. **Wait ~2 seconds** for connection
9. **Verify "Ring Connected" message** appears

### Expected Behavior

✅ **Successful Connection:**
- Ring appears in scan results with name (e.g., "R9") and MAC address
- Connection completes in 1-3 seconds
- "Ring Connected" success screen shows
- Green checkmark icon displayed

❌ **Common Issues:**

| Issue | Likely Cause | Solution |
|-------|--------------|----------|
| No devices found | Ring not in pairing mode | Wake ring or enable pairing mode |
| Connection timeout | Ring connected to another app | Close other apps, try again |
| Immediate disconnect | Ring already paired elsewhere | Unpair from other devices first |
| Permission denied | Location/Bluetooth not granted | Grant permissions in app settings |

---

## 📊 Logcat Filtering for Testing

### View BLE Logs

In Android Studio Logcat, filter by:

```
tag:BleManager
```

Or for all app logs:

```
package:com.fitness.app
```

### Key Log Messages

**Scan Started:**
```
I/BleManager: Device found: R9
I/BleManager:    MAC: D8:36:03:02:07:87
I/BleManager:    RSSI: -65 dBm
```

**Connection Attempt:**
```
I/BleManager: ═══════════════════════════════════
I/BleManager: CONNECTION ATTEMPT
I/BleManager: MAC Address: D8:36:03:02:07:87
I/BleManager: → Calling YCBTClient.connectBle(...)
```

**Connection Success:**
```
I/BleManager: ✓ FULLY CONNECTED (state 7) to D8:36:03:02:07:87
I/BleManager:    Services discovered, MTU negotiated
```

**Connection Failure:**
```
E/BleManager: ✗ Connection timeout after 15000ms
```

---

## 🔧 Known Limitations & BLE Behavior

### BLE Scan Delay
- **First scan may take 6-10 seconds** to find devices
- This is normal Android BLE behavior
- Subsequent scans are faster

### Single Connection Limitation
- **Ring supports only ONE active BLE connection**
- If ring is connected to another app/device, connection will fail
- Solution: Disconnect from other apps first

### Permission Dependency
- **Location permission is REQUIRED for BLE scanning** on Android
- This is an Android OS requirement, not an app bug
- Without location permission, scan will return 0 devices

### RSSI Accuracy
- **RSSI values may show 0 dBm** in some SDK versions
- This is a known YCBT SDK limitation
- Does not affect connection functionality

### Pairing Mode
- **Some rings require explicit pairing mode activation**
- Check ring manual for pairing mode instructions
- Usually: hold button for 3-5 seconds or double-tap

---

## 📁 Project Structure

```
FitnessAndroidApp/
├── src/main/
│   ├── kotlin/com/fitness/app/
│   │   ├── ble/                    # BLE connection logic
│   │   │   ├── BleManager.kt       # Core BLE operations
│   │   │   ├── BleViewModel.kt     # UI state management
│   │   │   ├── BleState.kt         # State models
│   │   │   └── preview/            # Compose previews
│   │   ├── SmartRingSetupScreen.kt # Main pairing UI
│   │   ├── MainActivity.kt         # App entry point
│   │   └── FitnessApplication.kt   # App initialization
│   ├── res/                        # Resources
│   └── AndroidManifest.xml         # App manifest
├── libs/
│   └── ycbtsdk-release.aar         # YCBT BLE SDK
├── build.gradle.kts                # App build config
└── .gitignore                      # Git ignore rules
```

---

## 🐛 Troubleshooting

### Gradle Sync Fails

**Error:** "Failed to resolve: ..."

**Solution:**
1. Check internet connection
2. File → Invalidate Caches → Invalidate and Restart
3. Delete `.gradle` folder and sync again

### App Crashes on Launch

**Check:**
1. Minimum SDK version (Android 8.0+)
2. Permissions granted
3. Logcat for crash stack trace

### Ring Not Appearing in Scan

**Verify:**
1. Ring is powered on
2. Ring is in pairing mode
3. Ring is not connected to another device
4. Location permission granted
5. Bluetooth is enabled
6. Ring is within 1 meter of phone

### Connection Fails Immediately

**Possible Causes:**
1. Ring already paired to another app → Unpair first
2. Ring not in pairing mode → Activate pairing mode
3. Weak signal → Move ring closer (< 10cm)

---

## 📞 Support

For issues or questions:
1. Check Logcat output (filter by `BleManager`)
2. Verify ring is in pairing mode
3. Try with official ring app to confirm hardware works
4. Share Logcat logs for debugging

---

## 📝 Testing Checklist

- [ ] App builds without errors
- [ ] App runs on Android 8.0+ device
- [ ] Permissions are requested and granted
- [ ] Scan finds the ring
- [ ] Connection succeeds
- [ ] Success screen displays
- [ ] Logcat shows clear BLE logs
- [ ] No crashes during normal use

---

## 🔐 Security Notes

- No API keys or secrets are committed to this repository
- `local.properties` is gitignored
- Keystore files are excluded
- BLE communication uses standard Android security

---

## 📄 License

Proprietary - Internal use only

---

## 🏷️ Version

**Current Version:** 1.0.0  
**Last Updated:** January 2026  
**Build:** Debug (for testing)

---

**Ready for Office Testing** ✅
