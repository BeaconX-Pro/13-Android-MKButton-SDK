# MOKO BXP-B-D Android SDK

Native Android SDK and demo app for BXP-B-D button beacon devices. Supports BLE scanning and advertisement parsing (trigger frames, UID, iBeacon, device info), connection with optional password verification, single/double/long-press alarm modes, abnormal inactivity detection, accelerometer data, alarm event storage/export, remote LED/buzzer reminders, power-saving configuration, and Nordic DFU firmware updates.

Cross-platform reference (same protocol): [Flutter-MKButton-SDK](https://github.com/BeaconX-Pro/Flutter-MKButton-SDK.git).

---

## Requirements

| Item | Description |
|------|-------------|
| Android Studio | 3.6+ (8.x recommended) |
| minSdk | 28 |
| compileSdk | 35 |
| Device | Physical device required (emulators do not support BLE) |

---

## Project Structure

```
BXP_B-D/
├── app/                          # Demo app (scan, connect, configure, DFU, full UI)
│   ├── activity/
│   │   ├── DMainActivity.java    # Scan page; device-type detection on connect
│   │   ├── DeviceInfoActivity.java   # Alarm / Device / Setting tabs
│   │   ├── DfuActivity.java
│   │   └── AlarmModeConfig* / AlarmEvent* / ...
│   ├── fragment/
│   │   ├── AlarmFragment.java        # V1 alarm UI
│   │   ├── AlarmNewFragment.java     # V2+ alarm UI
│   │   ├── DeviceFragment.java
│   │   └── SettingFragment.java
│   ├── utils/
│   │   └── AdvInfoAnalysisImpl.java  # Advertisement parser (demo impl)
│   └── service/
│       └── DfuServiceBtn.java
└── mokosupport/                  # BLE SDK module (primary integration dependency)
    ├── DMokoSupport.java         # Connect, send commands, Notify control, event callbacks
    ├── MokoBleScanner.java       # Scanning
    ├── OrderTaskAssembler.java   # Read/write task assembly (API entry)
    └── entity/
        ├── OrderCHAR.java        # GATT characteristic mapping
        ├── ParamsKeyEnum.java    # Protocol parameter keys
        └── OrderServices.java    # Advertisement service UUIDs
```

Communication has three stages: **scan → connect → command exchange**. The SDK reports connection status and command results via **EventBus** (you can switch to another bus in `DMokoSupport`).

---

## Integrating the SDK

### 1. Add the module

Copy `mokosupport` into your project root and add to `settings.gradle`:

```gradle
include ':app', ':mokosupport'
```

In the app module `build.gradle`:

```gradle
dependencies {
    implementation project(path: ':mokosupport')
}
```

### 2. Initialize

Initialize in `Application.onCreate()` or your first Activity:

```java
DMokoSupport.getInstance().init(getApplicationContext());
```

### 3. Permissions

`mokosupport` declares base BLE permissions in its `AndroidManifest.xml`. On Android 6.0+, scanning requires **runtime location permission**; on Android 12+, also request `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.

```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            REQUEST_CODE_LOCATION);
}
```

### 4. Register EventBus

Connection status, command results, and Notify data are delivered via EventBus. Register in your Activity/Fragment:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EventBus.getDefault().register(this);
}

@Override
protected void onDestroy() {
    EventBus.getDefault().unregister(this);
    super.onDestroy();
}
```

---

## Device Type Detection (`mFirmwareType` / `mSoftwareType` / `mBoardType`)

After a successful connection, `DMainActivity` reads device identity before opening the configuration UI. Three integers drive which pages, protocol paths, and UI layouts the demo uses. Pass them to downstream Activities via Intent extras (`EXTRA_KEY_DEVICE_TYPE`, `EXTRA_KEY_SOFTWARE_TYPE`, `EXTRA_KEY_BOARD_TYPE`).

### Detection flow

```
Connect
  └─ getVerifyPasswordEnable()
       ├─ password required → show dialog → setPassword() → getFirmwareType()
       └─ no password       → getFirmwareType()
            └─ KEY_FIRMWARE_TYPE  → mFirmwareType
                 └─ getSoftwareVersion(mFirmwareType)
                      ├─ V1 (mFirmwareType == 0): GATT CHAR_SOFTWARE_REVISION
                      └─ V2+ (mFirmwareType >= 1): protocol KEY_SOFTWARE_REVISION
                           ├─ version contains "CR" → mSoftwareType = 1
                           ├─ mFirmwareType == 2 && version contains "-D"
                           │    └─ getBoardType() → mBoardType
                           └─ else → open DeviceInfoActivity
```

---

## 1. Scanning for Devices

### Core classes

| Class | Description |
|-------|-------------|
| `MokoBleScanner` | Start/stop scanning |
| `MokoScanDeviceCallback` | Scan started, per-device callback, scan stopped |
| `DeviceInfoAnalysis` | Advertisement parser interface; demo impl: `AdvInfoAnalysisImpl` |
| `AdvInfo` | Parsed scan result (battery, trigger status, frame data, OTA flag) |

The demo scans without hardware filters; filter recognized frames in the parser callback. `DMainActivity` also supports name/MAC/RSSI filter via `ScanFilterDialog`.

### Code example

```java
MokoBleScanner scanner = new MokoBleScanner(context);
AdvInfoAnalysisImpl parser = new AdvInfoAnalysisImpl();

scanner.startScanDevice(new MokoScanDeviceCallback() {
    @Override
    public void onStartScan() {
        // Clear list, refresh UI
    }

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        AdvInfo info = parser.parseDeviceInfo(deviceInfo);
        if (info == null) return;
        // info.mac / info.name / info.rssi / info.battery
        // info.verifyEnable / info.triggerStatus / info.triggerCount
        // info.advDataHashMap — parsed broadcast frames
        // info.isOTA — true when adv name is MK_OTA
    }

    @Override
    public void onStopScan() {
        // Stop animation, etc.
    }
});

scanner.stopScanDevice();
```

### Advertisement frame types (`AdvInfoAnalysisImpl`)

| Source | UUID / ID | Frame types |
|--------|-----------|-------------|
| Device info | `0000ea00-...` | Battery, accelerometer, ranging data |
| Trigger | `0000fee0-...` | Single/double/long press (`0x20`/`0x21`/`0x22`/`0x23`), device ID, motion status |
| Eddystone UID | `0000feaa-...` | UID (`0x00`) |
| BeaconX iBeacon | `0000feab-...` | iBeacon (`0x02` / `0x50`) |
| Apple iBeacon | Manufacturer `0x004C` (23 bytes) | iBeacon |
| OTA mode | Name `MK_OTA` | DFU entry |

Trigger frame (`0000fee0`) fields:

- `verifyEnable` — password verification flag from advertisement
- `triggerStatus` — current trigger state
- `triggerCount` — trigger event count
- `deviceType` / `motionStatus` — device and motion flags

---

## 2. Connecting to a Device

### Connect

Only the device **MAC address** is required (from scan result `info.mac`):

```java
scanner.stopScanDevice();
DMokoSupport.getInstance().connDevice(mac);
```

OTA devices (`info.isOTA == true`) skip password and type detection; the demo opens `DfuActivity` directly.

### Connection status (EventBus)

```java
@Subscribe(threadMode = ThreadMode.MAIN)
public void onConnectStatusEvent(ConnectStatusEvent event) {
    String action = event.getAction();
    if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
        // GATT disconnected
    }
    if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
        // Service discovery done; start password / firmware-type detection
        DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.getVerifyPasswordEnable());
    }
}
```

### Password verification

After `ACTION_DISCOVER_SUCCESS`, read verify-password enable via `KEY_VERIFY_PASSWORD_ENABLE` on `CHAR_PASSWORD`:

| Result | Meaning |
|--------|---------|
| `1` | Password required — show dialog, then `setPassword(password)` |
| `0` | No password — proceed to `getFirmwareType()` |

Password write result (`KEY_PASSWORD`, write ACK `0xAA`):

```java
// On password success
DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.getFirmwareType());
// On password failure (not 0xAA) — disconnect
```

Change password in connected state via `OrderTaskAssembler.setModifyPassword(oldPwd, newPwd)` (`KEY_MODIFY_PASSWORD`).

### Manual disconnect

```java
DMokoSupport.getInstance().disConnectBle();
```

---

## 3. Reading and Writing Parameters

### Task queue

All reads/writes are wrapped as `OrderTask`, created by `OrderTaskAssembler`, and sent via `sendOrder` **in queue order**. Default timeout per task is 3 seconds.

```java
DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.getSensorType());

List<OrderTask> tasks = new ArrayList<>();
tasks.add(OrderTaskAssembler.getSlotAdvEnable());
tasks.add(OrderTaskAssembler.getFrameType());
tasks.add(OrderTaskAssembler.getSlotParams());
DMokoSupport.getInstance().sendOrder(tasks.toArray(new OrderTask[]{}));
```

Many `OrderTaskAssembler` methods accept `firmwareType` to choose GATT vs protocol path — always pass the `mFirmwareType` detected at connect time. See `OrderTaskAssembler.java` for the full list of `getXxx` / `setXxx` methods.

### Protocol frame format

Parameter channel (`CHAR_PARAMS` / `CHAR_PASSWORD`) frame layout:

```
EB [cmd] [len] [data...]
```

| Field | Description |
|-------|-------------|
| `0xEB` | Frame header |
| `cmd` | 1 byte, maps to `ParamsKeyEnum` |
| `len` | 1-byte payload length |
| `data` | Payload; write ACK uses response from device |

GATT characteristics (non-protocol) are accessed via dedicated tasks — e.g. `getSlotType()`, `getBattery()` map to `OrderCHAR` directly. Branch on `OrderCHAR` in `ACTION_ORDER_RESULT`.

### Command results (EventBus)

```java
@Subscribe(threadMode = ThreadMode.MAIN)
public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
    String action = event.getAction();
    OrderTaskResponse response = event.getResponse();

    if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
        // Timeout; check response.orderCHAR for which task
    }
    if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
        // All queued tasks finished
    }
    if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
        OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
        byte[] value = response.responseValue;
        // Parse value ...
    }
    if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
        // Device-initiated Notify (trigger, acc, click event, disconnect)
    }
}
```

### Example 1: Batch read device info (V2+)

```java
int firmwareType = mFirmwareType; // from connect-time detection
List<OrderTask> tasks = new ArrayList<>();
tasks.add(OrderTaskAssembler.getManufacturer(firmwareType));
tasks.add(OrderTaskAssembler.getDeviceModel(firmwareType));
tasks.add(OrderTaskAssembler.getProductDate(firmwareType));
tasks.add(OrderTaskAssembler.getHardwareVersion(firmwareType));
tasks.add(OrderTaskAssembler.getFirmwareVersion(firmwareType));
tasks.add(OrderTaskAssembler.getSoftwareVersion(firmwareType));
tasks.add(OrderTaskAssembler.getBatteryPercent());
DMokoSupport.getInstance().sendOrder(tasks.toArray(new OrderTask[]{}));
```

### Example 2: Read alarm mode parameters

```java
List<OrderTask> tasks = new ArrayList<>();
tasks.add(OrderTaskAssembler.getSlotAdvEnable());
tasks.add(OrderTaskAssembler.getFrameType());
tasks.add(OrderTaskAssembler.getSlotParams());
tasks.add(OrderTaskAssembler.getSlotTriggerParams());
tasks.add(OrderTaskAssembler.getSlotAdvBeforeTriggerEnable());
DMokoSupport.getInstance().sendOrder(tasks.toArray(new OrderTask[]{}));
```

### Example 3: Dismiss alarm / factory reset / power off

```java
DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setDismissAlarm());
DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setReset());
DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setClose());
```

---

## 4. Real-Time Sensor Notify

Trigger events, accelerometer samples, and click-event counts are pushed via Notify. Enable/disable in `DMokoSupport`:

```java
DMokoSupport.getInstance().enableSingleTriggerNotify();
DMokoSupport.getInstance().disableSingleTriggerNotify();

DMokoSupport.getInstance().enableDoubleTriggerNotify();
DMokoSupport.getInstance().disableDoubleTriggerNotify();

DMokoSupport.getInstance().enableLongTriggerNotify();
DMokoSupport.getInstance().disableLongTriggerNotify();

DMokoSupport.getInstance().enableAccNotify();
DMokoSupport.getInstance().disableAccNotify();

DMokoSupport.getInstance().enableClickEventNotify();
DMokoSupport.getInstance().disableClickEventNotify();

// Dual-button board (mBoardType == 3)
DMokoSupport.getInstance().enableClickSubEventNotify();
DMokoSupport.getInstance().disableClickSubEventNotify();

DMokoSupport.getInstance().enableLongConnectionNotify();
DMokoSupport.getInstance().disableLongConnectionNotify();
```

Receive data in `ACTION_CURRENT_DATA` and branch on `OrderCHAR`:

| Notify characteristic | Data |
|----------------------|------|
| `CHAR_SINGLE_TRIGGER` | Single-press trigger event |
| `CHAR_DOUBLE_TRIGGER` | Double-press trigger event |
| `CHAR_LONG_TRIGGER` | Long-press trigger event |
| `CHAR_ACC` | Real-time accelerometer samples |
| `CHAR_CLICK_EVENT` | Click event count (main button on dual-button board) |
| `CHAR_CLICK_SUB_EVENT` | Sub-button click event count (`mBoardType == 3`) |
| `CHAR_LONG_CONNECTION` | Long-connection alarm data |

Demo pages: `AccDataActivity`, `ExportDataActivity`, `ExportLongConnectionDataActivity`.

---

## 5. Disconnect Notifications

Handle two kinds of disconnect events separately.

### 5.1 BLE link disconnect (`ConnectStatusEvent`)

Triggered when the device powers off, goes out of range, connection fails, or you call `disConnectBle()`:

```java
if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
    // Close config UI, return to scan page, restart startScanDevice
}
```

**During DFU**, suppress disconnect dialogs (demo uses `isUpgrading` / `isUpgradeDisconnected` flags in `DeviceInfoActivity`).

### 5.2 Device-initiated disconnect Notify (`CHAR_DISCONNECT`)

The device may push a byte before disconnecting; receive it in `ACTION_CURRENT_DATA`:

```java
if (orderCHAR == OrderCHAR.CHAR_DISCONNECT) {
    int type = value[4] & 0xFF;
    // 2 = password changed successfully (reconnect required)
    // 3 = factory reset successful
    // 4 = normal disconnect notification
}
```

`ACTION_DISCONNECTED` usually follows. See `DeviceInfoActivity` for dialog handling.

---

## 6. DFU Firmware Update

The demo uses the **Nordic Android DFU Library** (transitive dependency via `MKBXPUILib`). UI entry: connect to an OTA advertisement (`MK_OTA`), or upgrade from **Settings** in `DeviceInfoActivity`.

Register the service in `AndroidManifest.xml`:

```xml
<service android:name="com.moko.bxp.button.d.service.DfuServiceBtn" />
```

`DfuServiceBtn` extends `DfuBaseService`.

### Flow

1. Scan OTA device (`info.isOTA == true`) and connect, or select firmware from Settings while connected
2. User selects a **`.zip`** firmware package
3. Disconnect normal GATT session if needed, then start DFU with MAC
4. Show progress via `DfuProgressListener`
5. Return to scan page and reconnect

### Code example

```java
DfuServiceListenerHelper.registerProgressListener(context, mDfuProgressListener);

DfuServiceInitiator starter = new DfuServiceInitiator(deviceMac)
        .setKeepBond(false)
        .setDisableNotification(true);
starter.setZip(null, firmwareFilePath);
starter.start(context, DfuServiceBtn.class);

private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
    @Override
    public void onProgressChanged(String address, int percent, float speed,
            float avgSpeed, int currentPart, int partsTotal) {
        // Progress: percent%
    }

    @Override
    public void onDfuCompleted(String deviceAddress) {
        // Success — prompt user to scan and reconnect
    }

    @Override
    public void onError(String deviceAddress, int error, int errorType, String message) {
        // Upgrade failed
    }
};

@Override
protected void onDestroy() {
    DfuServiceListenerHelper.unregisterProgressListener(context, mDfuProgressListener);
    super.onDestroy();
}
```

Notes:

- Firmware must be a valid non-empty **ZIP** file
- Call `disConnectBle()` before upgrading when already connected in normal mode
- Abort DFU if connection retries exceed 3 times (see `DfuActivity` / `DeviceInfoActivity`)

---

## 7. Typical Flow

```
Scan page (DMainActivity)
  ├─ MokoBleScanner.startScanDevice
  ├─ AdvInfoAnalysisImpl → device list (trigger / UID / iBeacon / device info)
  ├─ connDevice(mac)
  ├─ getVerifyPasswordEnable → [optional] setPassword
  ├─ getFirmwareType → getSoftwareVersion → [V3 -D] getBoardType
  │    └─ mFirmwareType / mSoftwareType / mBoardType
  └─ DeviceInfoActivity
       ├─ Alarm tab — single/double/long press, abnormal inactivity, alarm events
       │    ├─ V1: AlarmFragment
       │    └─ V2+: AlarmNewFragment (dual-button layout when mBoardType == 3)
       ├─ Device tab — name, device ID, connectable, password, scan response
       ├─ Setting tab — power saving, quick switch, DFU, reset, power off
       ├─ AlarmModeConfig* — per press-type slot configuration (-D / -CR variants)
       ├─ AlarmEvent* / AlarmEventB03 — stored alarm events & export
       ├─ AccDataActivity — real-time accelerometer
       ├─ RemoteReminder* — remote LED / buzzer alarm
       ├─ PowerSavingConfigActivity — power-saving parameters
       ├─ ACTION_CURRENT_DATA → trigger / acc / click-event Notify / disconnect
       ├─ ACTION_DISCONNECTED → link lost
       └─ Settings → DFU → DfuActivity → back to scan and reconnect
```

---

## 8. Core Classes Quick Reference

| Stage | Class | Role |
|-------|-------|------|
| Scan | `MokoBleScanner` | Scan control |
| Scan | `MokoScanDeviceCallback` | Scan callbacks |
| Scan | `AdvInfoAnalysisImpl` | Parse advertisements |
| Connect | `DMokoSupport` | Connect, send commands, Notify control, Bluetooth on/off |
| Type | `DMainActivity` | `mFirmwareType` / `mSoftwareType` / `mBoardType` detection |
| Comm | `OrderTaskAssembler` | Build read/write tasks |
| Comm | `ParamsKeyEnum` | Protocol parameter keys |
| Comm | `OrderCHAR` | GATT characteristic mapping |
| Event | `ConnectStatusEvent` | Connected / disconnected |
| Event | `OrderTaskResponseEvent` | Command results, Notify data |

---

## 9. Notes

1. **Permissions**: Android 6.0+ requires runtime location for scanning; Android 12+ needs `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`.
2. **EventBus**: The SDK posts events internally. To use LiveData/RxJava instead, change `orderFinish` / `orderTimeout` / `orderResult` / `orderNotify` in `DMokoSupport`.
3. **Logging**: The SDK uses `XLog` with file output and storage permission. To disable file logging, keep only `XLog.init(config)` in `BaseApplication`.
4. **Firmware variants**: V1 (`mFirmwareType == 0`) uses standard GATT Device Information characteristics; V2+ uses `ParamsKeyEnum` protocol keys. Always detect `mFirmwareType` at connect time and pass it to `OrderTaskAssembler` methods that accept `firmwareType`.
5. **Software / board variants**: Use `mSoftwareType` to pick `-D` vs `-CR` Activity classes; use `mBoardType` for dual-button (B03) UI and long-connection event handling.
6. **Demo references**: Scan/connect/type detection — `DMainActivity`; device info & tabs — `DeviceInfoActivity`, `AlarmFragment` / `AlarmNewFragment`, `DeviceFragment`, `SettingFragment`; alarm config — `AlarmModeConfigActivity` / `AlarmModeConfigCRActivity`; sensors — `AccDataActivity`, `ExportDataActivity`, `ExportLongConnectionDataActivity`; DFU — `DfuActivity`, `DeviceInfoActivity`.

---

## Changelog

| Date | Version | Notes |
|------|---------|-------|
| 2020.01.18 | mokosupport 1.0 | Initial release |
| 2021.03.11 | mokosupport 2.0 | Restructure SDK; support Android API 29; androidx; optimize docs |
| 2021.11.30 | mokosupport 3.0 | Change SDK package name; support light sensor data |
| — | mokosupport 4.0 | compileSdk 35, minSdk 28; |
