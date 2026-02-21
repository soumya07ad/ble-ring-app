# MRD SDK Data Analysis & Native Protocol Reference

## 1. Overview
This document details the reverse-engineered BLE protocol used by the MRD SDK (specifically found in `c.java` and `b.java`). It provides the exact byte commands and parsing logic required to implement health metric retrieval natively in Kotlin, without relying on the obfuscated SDK.

---

## 2. Native Protocol Reference
All commands are sent to the characteristic **`f000efe1-0451-4000-0000-00000000b000`**.
Responses are received via notification on **`f000efe3-0451-4000-0000-00000000b000`**.

**Header:** All commands start with `0xFC`.
**Length:** Fixed 20 bytes.

### 2.1. Commands & Response Types

| Metric | Function | **Command Data (Hex)** | **Response Header** | **Parsing Method** |
| :--- | :--- | :--- | :--- | :--- |
| **Blood Pressure** | Get Last Data | `FC 11 00 ...` | `FC 11` | `parseBpLast` |
| | Get History | `FC 11 01 ...` | `FC 11` | `parseBpHistory` |
| | Start Test | `FC 11 03 ...` | `FC 11` | `parseBpTest` |
| **SpO2** | Get Last Data | `FC 12 00 ...` | `FC 12` | `parseBoLast` |
| | Get History | `FC 12 01 ...` | `FC 12` | `parseBoHistory` |
| | Start Test | `FC 12 03 ...` | `FC 12` | `parseBoTest` |
| **HRV** | Get History | `FC 5D 00 ...` | `5D` | `parseHrvHistory` |
| **Stress (Pressure)** | Get History | `FC 52 0A 01/02 ...` | `FC 52` | `parseStressHistory` |
| | Start Test | `FC 09 09 ...` (Legacy) | `FC 09` | `parseStressTest` |
| **Sleep** | Get Summary | `FC 23 01 ...` | `FC 23` | `parseSleepSummary` |
| | Get History | `FC 0C 01 ...` | `FC 0C` | `parseSleepHistory` |
| **Firmware** | Get Version | `FC 0F 05 ...` | `FC 0F` | `parseFirmware` |
| **Battery** | Get Battery | `FC 0F 06 ...` | `FC 0F` | `parseBattery` |
| **Steps** | Get Real-time | `FC 03 00 ...` | `FC 03` | `parseStepsRealTime` |
| | Get History Num | `FC 03 80 ...` | `FC 03 80` | `parseStepsHistoryNum` |
| | Get History Data | `FC 03 C0 ...` | `FC 03 C0` | `parseStepsHistoryData` |
| **Heart Rate** | Get Last | `FC 0A 00 ...` | `FC 0A` | `parseHrLast` |
| | Get History | `FC 0A 01 ...` | `FC 0A` | `parseHrHistory` |
| | Start Test | `FC 09 <val> ...` | `FC 09` | `parseHrTest` |
| **Temperature** | Get Last | `FC 40 00 ...` | `FC 40` | `parseTempLast` |
| | Get History | `FC 40 01 ...` | `FC 40` | `parseTempHistory` |
| | Start Test | `FC 40 03 ...` | `FC 40` | `parseTempTest` |
| **Sport** | Get Real-time | `FC 1B 00 ...` | `FC 1B` | `parseSportRealTime` |


---

## 3. Byte Parsing Logic

Input: `packet` (Byte Array, 20 bytes). Indices are 0-based.
Note: Java/Kotlin `byte` is signed. Use `& 0xFF` to get unsigned 0-255 integer values.

### 3.1. Blood Pressure (`0x11`)
**Packet Type:** `packet[1] == 0x11`

**A. Last Data (Subtype 0)**:
*   **Systolic (HP):** `packet[11] & 0xFF`
*   **Diastolic (LP):** `packet[12] & 0xFF`
*   **Pulse (HR):** `packet[13] & 0xFF`
*   **Date:** `packet[5..10]` (Year, Month, Day, Hour, Min, Sec - BCD encoded).

### 3.2. SpO2 (`0x12`)
**Packet Type:** `packet[1] == 0x12`

**A. Last Data**:
*   **SpO2 Integer:** `packet[12] & 0xFF`
*   **SpO2 Decimal:** `packet[13] & 0xFF`
*   **Value:** `Integer.Decimal` (e.g., 98.0)

### 3.3. HRV History (`0x5D`)
**Packet Type:** `packet[1] == 0x5D` (93)

**Parsing Logic:**
*   **HRV Value:** `packet[12] & 0xFF`
*   **Timestamp (BCD Encoded):**
    *   Year: `bcd(packet[6])`
    *   Month: `bcd(packet[7])`
    *   Day: `bcd(packet[8])`
    *   Hour: `bcd(packet[9])`
    *   Min: `bcd(packet[10])`
    *   Sec: `bcd(packet[11])`
    *   *Helper:* `bcd(x) = Integer.parseInt(Integer.toHexString(x & 0xFF))`

### 3.4. Sleep (`0x23` / `0x0C`)
**Packet Type:** `packet[1] == 0x23` (Summary) or `0x0C` (History)

**A. Summary (`0x23`):**
*   **Deep Sleep:** `(packet[12] << 8) | packet[13]` (minutes?)
*   **Light Sleep:** `(packet[14] << 8) | packet[15]`
*   **Awake:** `(packet[16] << 8) | packet[17]`

**B. History (`0x0C`):**
Complex structure involving multiple packets for start/end times and states. Refer to `b.java:W` method for details.

### 3.5. Firmware & Battery (`0x0F`)
**Packet Type:** `packet[1] == 0x0F` (15)

**A. Firmware Version (Subtype 5):**
*   **Condition:** `packet[1] == 0x0F` and `packet[2] == 0x05` (or context based)
*   **Version:** `packet[7]`.`packet[8]`.`packet[9]`  (e.g., `1.0.5`)
*   **Type:** If `packet[10] == 0x55` (85), Type is BCD of `packet[11]`, `packet[12]`.

**B. Battery (Subtype 6):**
*   **Condition:** `packet[1] == 0x0F` and `packet[2] == 0x06`
*   **Battery Level:** `packet[8] & 0xFF`
*   **State:** `packet[9] & 0xFF` (Charging status)

### 3.6. Steps (`0x03`)
**Packet Type:** `packet[1] == 0x03`

**A. Real-time / Last Data:**
*   **Condition:** `packet[0]` is NOT `0x80` or `0xC0` (standard header `FC`).
*   **Packet Structure:** Contains 3 measurements (Days?).
    *   **Day 1 (Yesterday?):** Steps: `byte3(packet[2], packet[3], packet[4])`
    *   **Day 2 (Today?):** Distance: `byte3(packet[5], packet[6], packet[7])`
    *   **Day 3 (Calories?):** Calories: `byte3(packet[8], packet[9], packet[10])`
    *   *Helper:* `byte3(b1, b2, b3) = (b1 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b3 & 0xFF)`

### 3.7. Heart Rate (`0x0A`)
**Packet Type:** `packet[1] == 0x0A` (10)

**A. Last Data (Subtype 0) / Test:**
*   **Heart Rate Value:** `packet[12] & 0xFF`
*   **Timestamp:** BCD Encoded at `packet[6..11]`.

### 3.8. Temperature (`0x40`)
**Packet Type:** `packet[1] == 0x40` (64)

**A. Last Data:**
*   **Raw Value:** `(packet[12] & 0xFF) * 256 + (packet[13] & 0xFF)`
*   **Temperature (Celcius):** `Raw / 10.0`

### 3.9. Sport Mode (`0x1B`)
**Packet Type:** `packet[1] == 0x1B` (27)

**A. Real-time:**
*   **Condition:** complex bit field analysis of `packet[1]` etc. (Refer to `b.java:i` method).

### 3.10. Stress (`0x52`)
**Packet Type:** `packet[1] == 0x52` (82)

**A. History (Type 10 / 0x0A):**
*   **Condition:** `packet[1] == 0x52` AND (`packet[2] == 0x01` (Count) OR `packet[2] == 0x02` (Data)).
*   **Metric Type:** `packet[3] == 0x0A` (Start byte of payload wrapper? No, `b.java` uses index 1 of DECODED payload).
*   **Correction:** `b.java:L` uses `object` which is `packet` shifted or processed.
*   **Raw Parsing:**
    *   Command: `FC 52 0A 02 ...`
    *   Response: `FC 52 02 <TotalPackets> <Index> <Len> <Data...>` (Standard History Format).
    *   **Data Values:** Each byte in the payload `packet[6..6+Len]` is a stress value (0-100?).

---

## 4. Implementation Helper
Use this `when` block structure in your BLE notification handler:

```kotlin
fun parseBleData(packet: ByteArray) {
    if (packet[0] != 0xFC.toByte()) return // Invalid header

    when (packet[1].toInt() and 0xFF) {
        0x11 -> parseBloodPressure(packet)
        0x12 -> parseSpO2(packet)
        0x5D -> parseHRV(packet)
        0x0F -> parseSystemInfo(packet) // Firmware & Battery checks packet[2]
        0x23 -> parseSleepSummary(packet)
        // ... handle other cases
    }
}
```
