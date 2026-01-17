# YCBT SDK Implementation Guide for FitnessAndroidApp

## Document Purpose
This document contains comprehensive analysis of the YCBT SDK based on the official demo app (`YCBleSdkDemo`) 
to help fix the ring battery display and data retrieval issues.

**Created:** 2026-01-09
**SDK Version:** ycbtsdk-release.aar (from android-sdk-master)

---

## CRITICAL ISSUE IDENTIFIED

### The Problem
The ring's battery percentage shows **0%** even after successful Bluetooth connection. 
The SDK's async callbacks for `getDeviceInfo()` and other data commands **never fire**.

### Root Cause
The YCBT SDK has an internal **auto-reconnect timer (isRepeat=true)** that disconnects and reconnects 
every ~15 seconds. This happens BEFORE the ring can respond to commands, causing:
1. Commands to be sent successfully ✓
2. Ring starts processing ✓
3. SDK disconnects before response arrives ✗
4. Callbacks never fire ✗

---

## KEY DIFFERENCES FROM DEMO APP

### 1. SDK Initialization

**Demo App (MyApplication.java line 37):**
```java
YCBTClient.initClient(this, true);  // Single param: true = enable reconnect
```

**Our App (BleManager.kt):**
```kotlin
YCBTClient.initClient(context, false, true)  // false for reconnect, true for debug
```

**FIX:** Try using `YCBTClient.initClient(context, true)` like the demo.

---

### 2. Connection State Handling

**Demo App uses three states:**
```java
Constants.BLEState.Disconnect   // Disconnected
Constants.BLEState.Connected    // BLE connected, but SDK not ready
Constants.BLEState.ReadWriteOK  // SDK READY for commands (state 1)
```

**Demo waits for ReadWriteOK before sending commands:**
```java
// In MyApplication.java - Global listener
if (code == Constants.BLEState.ReadWriteOK) {
    // NOW send commands
    ConnectEvent event = new ConnectEvent();
    event.state = 1;
    EventBus.getDefault().post(event);
}
```

**Our App:** Sends commands on state 7 (BLE fully connected) but state 1 (ReadWriteOK) arrives late or never.

**FIX:** Check `YCBTClient.connectState() == ReadWriteOK` before EVERY command.

---

### 3. Connection State Check Before Commands

**Demo App pattern (used EVERYWHERE):**
```java
if (YCBTClient.connectState() == ReadWriteOK) {
    YCBTClient.getDeviceInfo(callback);
    // or any other SDK command
} else {
    Toast.makeText(this, "请先连接设备...", Toast.LENGTH_LONG).show();
}
```

**Our App:** We send commands without checking connectState().

**FIX:** Always check `YCBTClient.connectState()` before sending commands.

---

### 4. Post-Connection Setup Commands

**Demo App calls these after connection (baseOrderSet in MainActivity.java):**
```java
// 1. Set language FIRST
YCBTClient.settingLanguage(0x01, callback);  // 0x01 = Chinese

// 2. Set phone time
Calendar calendar = Calendar.getInstance();
// ... format time ...
YCBTClient.getDeviceInfo(callback);  // Called in setPhoneTime()

// 3. Set heart rate monitoring interval
YCBTClient.settingHeartMonitor(0x01, 10, callback);  // Every 10 minutes
```

**Our App:** We skip language/time sync.

**FIX:** Call `settingLanguage()` first, then other commands.

---

## SDK METHODS REFERENCE

### Device Information

```java
// Get device info (battery, version)
YCBTClient.getDeviceInfo(new BleDataResponse() {
    @Override
    public void onDataResponse(int code, float ratio, HashMap resultMap) {
        if (code == 0 && resultMap != null) {
            // Parse using Gson (recommended by demo)
            String backVal = resultMap.toString();
            Gson gson = new Gson();
            BandBaseInfo info = gson.fromJson(backVal, BandBaseInfo.class);
            String battery = info.getData().getDeviceBatteryValue();  // String!
            String version = info.getData().getDeviceVersion();
        }
    }
});

// Read cached device info (synchronous)
String deviceType = (String) YCBTClient.readDeviceInfo(Constants.FunctionConstant.DEVICETYPE);
// Returns: 0x00 = Watch/Bracelet, 0x01 = Ring
```

### Response Data Structure (BandBaseInfo.java)

```java
{
    "dataType": int,
    "code": int,              // 0 = success
    "data": {
        "deviceBatteryValue": String,  // Battery as STRING (e.g., "85")
        "deviceVersion": String        // Firmware version
    }
}
```

---

### Real-Time Data

```java
// Enable real-time sport data
YCBTClient.appRealSportFromDevice(0x01, callback);  // 0x01 = enable, 0x00 = disable

// Register callback for real-time data
YCBTClient.appRegisterRealDataCallBack(new BleRealDataResponse() {
    @Override
    public void onRealDataResponse(int dataType, HashMap dataMap) {
        if (dataType == Constants.DATATYPE.Real_UploadSport) {
            int sportStep = (int) dataMap.get("sportStep");
            int sportDistance = (int) dataMap.get("sportDistance");
            int sportCalorie = (int) dataMap.get("sportCalorie");
        }
    }
});
```

---

### History Data

```java
// Health history data types (hex codes from demo):
0x0504  // Sleep history
0x051A  // Blood oxygen history
0x051C  // Temperature & humidity history
0x051E  // Body temperature history
0x0520  // Ambient light history
0x0529  // Fall/drop records
0x052D  // General history

// Constants for common types:
Constants.DATATYPE.Health_HistoryHeart   // Heart rate history
Constants.DATATYPE.Health_HistorySleep   // Sleep history
Constants.DATATYPE.Health_HistorySport   // Steps/sport history

// Example:
YCBTClient.healthHistoryData(Constants.DATATYPE.Health_HistoryHeart, new BleDataResponse() {
    @Override
    public void onDataResponse(int code, float ratio, HashMap hashMap) {
        if (hashMap != null) {
            // hashMap contains: heartStartTime, heartValue, etc.
        }
    }
});
```

---

### Useful Settings Methods

```java
// Set language (IMPORTANT - do this first after connection!)
// 0x00:English 0x01:Chinese 0x02:Russian 0x03:German 0x04:French
// 0x05:Japanese 0x06:Spanish 0x07:Italian 0x08:Portuguese
// 0x09:Korean 0x0A:Polish 0x0B:Malay 0x0C:Traditional Chinese 0xFF:Other
YCBTClient.settingLanguage(0x00, callback);

// Set heart rate monitoring
// param1: 0x01 = enable, 0x00 = disable
// param2: interval in minutes
YCBTClient.settingHeartMonitor(0x01, 10, callback);

// Send phone model (might be required for some features)
YCBTClient.appMobileModel("Phone Model Name", callback);

// Get all real-time data at once
YCBTClient.getAllRealDataFromDevice(callback);

// Check connection state
int state = YCBTClient.connectState();
// Returns: Constants.BLEState.ReadWriteOK if ready
```

---

## IMPLEMENTATION CHANGES TO MAKE

### Priority 1: Fix SDK Initialization

```kotlin
// Change from:
YCBTClient.initClient(context, false, true)

// To (like demo):
YCBTClient.initClient(context, true)
```

### Priority 2: Wait for ReadWriteOK

```kotlin
// In registerConnectionListener():
when (code) {
    Constants.BLEState.ReadWriteOK -> {  // This is state 1
        // NOW send commands
        onConnectionSuccess()
    }
}
```

### Priority 3: Check connectState() Before Commands

```kotlin
private fun readDeviceInfo() {
    if (YCBTClient.connectState() != Constants.BLEState.ReadWriteOK) {
        Log.w(TAG, "Not ready for commands")
        return
    }
    // Send command...
}
```

### Priority 4: Add Post-Connection Setup

```kotlin
private fun onConnectionSuccess() {
    // 1. Set language first (English = 0x00)
    YCBTClient.settingLanguage(0x00, callback)
    
    // 2. Then get device info
    YCBTClient.getDeviceInfo(callback)
    
    // 3. Set heart rate monitoring
    YCBTClient.settingHeartMonitor(0x01, 10, callback)
}
```

### Priority 5: Use Gson for Response Parsing

```kotlin
// Add dependency: implementation 'com.google.code.gson:gson:2.10'

// Create model class (already have BandBaseInfo in demo)
data class BandBaseInfo(
    val dataType: Int,
    val code: Int,
    val data: BandBaseInfoModel?
) {
    data class BandBaseInfoModel(
        val deviceBatteryValue: String?,
        val deviceVersion: String?
    )
}

// Parse response:
val gson = Gson()
val info = gson.fromJson(resultMap.toString(), BandBaseInfo::class.java)
val battery = info.data?.deviceBatteryValue?.toIntOrNull() ?: 0
```

---

## DATA TYPE CONSTANTS REFERENCE

From `Constants.DATATYPE`:

| Constant | Description | Notes |
|----------|-------------|-------|
| `Real_UploadSport` | Real-time steps data | Contains sportStep, sportDistance, sportCalorie |
| `Real_UploadECG` | Real-time ECG data | Raw ECG values |
| `Real_UploadPPG` | Real-time PPG data | Raw PPG byte array |
| `Real_UploadECGHrv` | Real-time HRV data | Float value |
| `Real_UploadECGRR` | Real-time RR interval | Float value |
| `Real_UploadBlood` | Real-time blood pressure | bloodDBP, bloodSBP, heartValue |
| `Health_HistoryHeart` | Heart rate history | |
| `Health_HistorySleep` | Sleep history | |
| `Health_HistorySport` | Steps/sport history | |
| `DeviceMeasurementResult` | Measurement results from device | |

---

## BLE STATE CONSTANTS REFERENCE

From `Constants.BLEState`:

| Constant | Value | Description |
|----------|-------|-------------|
| `Disconnect` | 2/3 | Device disconnected |
| `Connected` | 6 | BLE connected but SDK not ready |
| `ReadWriteOK` | 1 | **SDK ready for commands** |

**Note:** State 7 is "fully connected" (BLE level) but doesn't mean SDK is ready!

---

## DEMO APP FILES REFERENCE

| File | Purpose |
|------|---------|
| `MyApplication.java` | SDK initialization, global connection listener |
| `MainActivity.java` | Main connection flow, baseOrderSet(), data sync |
| `OldActivity.java` | Device info, battery display, history data |
| `TimeSetActivity.java` | Settings examples (language, monitoring, etc.) |
| `BandBaseInfo.java` | Model for device info response |
| `ConnectEvent.java` | EventBus event for connection state |

---

## TESTING CHECKLIST

After implementing changes:

1. [ ] App compiles without errors
2. [ ] SDK initializes with `initClient(context, true)`
3. [ ] Connection listener waits for ReadWriteOK (state 1)
4. [ ] `connectState()` is checked before commands
5. [ ] `settingLanguage()` is called first after connection
6. [ ] `getDeviceInfo()` callback fires
7. [ ] Battery percentage is parsed and displayed
8. [ ] No auto-reconnect loop visible in logs

---

## LOG MESSAGES TO LOOK FOR

**Success indicators:**
```
✓ FULLY CONNECTED (state 7)
ReadWriteOK received (state 1)
getDeviceInfo callback: code=0
Battery from SDK: XX%
```

**Problem indicators:**
```
开始连接 isConnecting==true--isRepeat==true  ← Auto-reconnect still happening
getDeviceInfo callback never fires
State 1 never received
```

---

## CONTACT SDK VENDOR IF NEEDED

If changes don't fix the issue, contact YCBT and ask:

1. How to disable `isRepeat` auto-reconnect behavior?
2. Is there a `stopReconnect()` or `disableAutoReconnect()` method?
3. What's the correct way to wait for SDK readiness?
4. Why does `initClient(context, false, true)` still show `isRepeat=true`?

---

## APPENDIX: Demo App Dependencies

From `demo/YCBleSdkDemo/app/build.gradle`:

```gradle
dependencies {
    implementation 'com.google.code.gson:gson:2.8.6'
    implementation 'org.greenrobot:eventbus:3.1.1'
    // ... SDK AARs in libs folder
}
```

SDK AAR files in `demo/YCBleSdkDemo/app/libs/`:
- `ycbtsdk-release.aar` (main SDK)
- `AliAgent-release-4.1.3.aar`
- `BmpConvert_V1.2.1-release.aar`
- `JL_Watch_V1.10.0-release.aar`
- `jl_bt_ota_V1.9.3-release.aar`
- `jl_rcsp_V0.5.2-release.aar`
