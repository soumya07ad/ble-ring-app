# Testing Branch

This branch is for **office testing only**.

## Quick Start for Testers

1. **Clone this branch:**
   ```bash
   git clone -b testing https://github.com/soumya07ad/ble-ring-app.git
   ```

2. **Open in Android Studio**

3. **Run on device**

4. **Test BLE connection** with smart ring

## What to Test

✅ App builds without errors  
✅ Permissions are requested  
✅ Scan finds the ring  
✅ Connection succeeds  
✅ Success screen displays  

## Report Issues

If you encounter issues:
1. Copy Logcat output (filter by `BleManager`)
2. Note the exact steps to reproduce
3. Share device Android version
4. Confirm ring works with official app

---

**This branch may contain experimental fixes - use `main` for stable code**
