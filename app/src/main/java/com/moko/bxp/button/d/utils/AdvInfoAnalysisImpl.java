package com.moko.bxp.button.d.utils;

import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.SparseArray;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.d.entity.AdvInfo;
import com.moko.support.d.entity.DeviceInfo;
import com.moko.support.d.entity.OrderServices;
import com.moko.support.d.service.DeviceInfoAnalysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;


public class AdvInfoAnalysisImpl implements DeviceInfoAnalysis<AdvInfo> {
    private HashMap<String, AdvInfo> beaconXInfoHashMap;

    public AdvInfoAnalysisImpl() {
        this.beaconXInfoHashMap = new HashMap<>();
    }

    @Override
    public AdvInfo parseDeviceInfo(DeviceInfo deviceInfo) {
        ScanResult result = deviceInfo.scanResult;
        ScanRecord record = result.getScanRecord();
        Map<ParcelUuid, byte[]> map = record.getServiceData();
        SparseArray<byte[]> manufacturerData = record.getManufacturerSpecificData();
        if ((map == null || map.isEmpty()) && manufacturerData == null) return null;
        int battery = -1;
        int triggerStatus = -1;
        int triggerCount = -1;
        String deviceId = "";
//        String beaconTemp = "";
        int accX = 0;
        int accY = 0;
        int accZ = 0;
        int accShown = 0;
        int deviceInfoFrame = -1;
        int frameType = -1;
        int rangeData = -1;
        int verifyEnable = 0;
        int deviceType = 0;
        String uuid = "";
        int major = 0;
        int minor = 0;
        int rssi1m = 0;
        int rssi0m = 0;
        String namespaceId = "";
        String instanceId = "";
//        String dataStr = "";
        byte[] dataBytes = new byte[0];
        if (manufacturerData != null) {
            byte[] manufacturerBytes = record.getManufacturerSpecificData(0x004C);
            if (null == manufacturerBytes || manufacturerBytes.length != 23) return null;
            frameType = manufacturerBytes[0];
            String uuidRaw = MokoUtils.bytesToHexString(Arrays.copyOfRange(manufacturerBytes, 2, 18)).toLowerCase(Locale.ROOT);
            StringBuilder stringBuilder = new StringBuilder(uuidRaw);
            stringBuilder.insert(8, "-");
            stringBuilder.insert(13, "-");
            stringBuilder.insert(18, "-");
            stringBuilder.insert(23, "-");
            uuid = stringBuilder.toString();
            major = MokoUtils.toInt(Arrays.copyOfRange(manufacturerBytes, 18, 20));
            minor = MokoUtils.toInt(Arrays.copyOfRange(manufacturerBytes, 20, 22));
            rssi1m = manufacturerBytes[22];
            dataBytes = manufacturerBytes;

        }
        if (map != null) {
            final Iterator iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                final ParcelUuid parcelUuid = (ParcelUuid) iterator.next();
                if (parcelUuid.getUuid().equals(OrderServices.SERVICE_ADV_DEVICE.getUuid())) {
                    byte[] data = map.get(new ParcelUuid(OrderServices.SERVICE_ADV_DEVICE.getUuid()));
                    if (data == null || data.length < 21) continue;
                    deviceInfoFrame = data[0] & 0xFF;
                    accX = MokoUtils.toIntSigned(Arrays.copyOfRange(data, 4, 6));
                    accY = MokoUtils.toIntSigned(Arrays.copyOfRange(data, 6, 8));
                    accZ = MokoUtils.toIntSigned(Arrays.copyOfRange(data, 8, 10));
//                int tempInteger = data[10];
//                int tempDecimal = data[11] & 0xFF;
//                beaconTemp = String.format("%d.%d", tempInteger, tempDecimal);
                    rangeData = data[12];
                    battery = MokoUtils.toInt(Arrays.copyOfRange(data, 13, 15));
                }
                if (parcelUuid.getUuid().equals(OrderServices.SERVICE_ADV_TRIGGER.getUuid())) {
                    byte[] data = map.get(new ParcelUuid(OrderServices.SERVICE_ADV_TRIGGER.getUuid()));
                    if (data == null || data.length < 7) continue;
//                dataStr = MokoUtils.bytesToHexString(data);
                    dataBytes = data;
                    frameType = data[0] & 0xFF;
                    verifyEnable = (data[1] & 0x01) == 0x01 ? 1 : 0;
                    triggerStatus = (data[1] & 0x02) == 0x02 ? 1 : 0;
                    triggerCount = MokoUtils.toInt(Arrays.copyOfRange(data, 2, 4));
                    XLog.i("mac=" + deviceInfo.mac);
                    XLog.i("data=" + Arrays.toString(data));
                    deviceId = String.format("0x%s", MokoUtils.bytesToHexString(Arrays.copyOfRange(data, 4, data.length - 2)).toUpperCase());
                    deviceType = data[data.length - 2] & 0xFF;
                } else if (parcelUuid.getUuid().equals(OrderServices.SERVICE_ADV_IBEACON.getUuid())) {
                    byte[] data = map.get(new ParcelUuid(OrderServices.SERVICE_ADV_IBEACON.getUuid()));
                    if (data == null || data.length != 23) continue;
//                dataStr = MokoUtils.bytesToHexString(data);
                    dataBytes = data;
                    frameType = data[0] & 0xFF;
                    rssi1m = data[1];
                    String uuidRaw = MokoUtils.bytesToHexString(Arrays.copyOfRange(data, 3, 19)).toLowerCase(Locale.ROOT);
                    StringBuilder stringBuilder = new StringBuilder(uuidRaw);
                    stringBuilder.insert(8, "-");
                    stringBuilder.insert(13, "-");
                    stringBuilder.insert(18, "-");
                    stringBuilder.insert(23, "-");
                    uuid = stringBuilder.toString();
                    major = MokoUtils.toInt(Arrays.copyOfRange(data, 19, 21));
                    minor = MokoUtils.toInt(Arrays.copyOfRange(data, 21, 23));
                    XLog.i("mac=" + deviceInfo.mac);
                    XLog.i("data=" + Arrays.toString(data));
                } else if (parcelUuid.getUuid().equals(OrderServices.SERVICE_ADV_UID.getUuid())) {
                    byte[] data = map.get(new ParcelUuid(OrderServices.SERVICE_ADV_UID.getUuid()));
                    if (data == null || data.length != 20) continue;
//                dataStr = MokoUtils.bytesToHexString(data);
                    dataBytes = data;
                    frameType = data[0] & 0xFF;
                    rssi0m = data[1];
                    namespaceId = MokoUtils.bytesToHexString(Arrays.copyOfRange(data, 2, 12));
                    instanceId = MokoUtils.bytesToHexString(Arrays.copyOfRange(data, 12, 18));
                    XLog.i("mac=" + deviceInfo.mac);
                    XLog.i("data=" + Arrays.toString(data));
                }

            }
            if (deviceInfoFrame == -1 && frameType == -1) return null;
        }
        if (accX != 0 || accY != 0 || accZ != 0) {
            accShown = 1;
        }
        AdvInfo advInfo;
        if (beaconXInfoHashMap.containsKey(deviceInfo.mac)) {
            advInfo = beaconXInfoHashMap.get(deviceInfo.mac);
            if (!TextUtils.isEmpty(deviceInfo.name)) {
                advInfo.name = deviceInfo.name;
            }
            advInfo.rssi = deviceInfo.rssi;
            if (battery >= 0) {
                advInfo.battery = battery;
            }
            if (result.isConnectable()) advInfo.connectState = 1;
            advInfo.txPower = record.getTxPowerLevel();
            advInfo.rangingData = rangeData;
            advInfo.deviceId = deviceId;
            advInfo.verifyEnable = verifyEnable;
            advInfo.deviceType = deviceType;
            advInfo.scanRecord = deviceInfo.scanRecord;
            long currentTime = SystemClock.elapsedRealtime();
            long intervalTime = currentTime - advInfo.scanTime;
            advInfo.intervalTime = intervalTime;
            advInfo.scanTime = currentTime;
        } else {
            advInfo = new AdvInfo();
            advInfo.name = deviceInfo.name;
            advInfo.mac = deviceInfo.mac;
            advInfo.rssi = deviceInfo.rssi;
            if (battery < 0) {
                advInfo.battery = -1;
            } else {
                advInfo.battery = battery;
            }
            if (result.isConnectable()) {
                advInfo.connectState = 1;
            } else {
                advInfo.connectState = 0;
            }
            advInfo.txPower = record.getTxPowerLevel();
            advInfo.rangingData = rangeData;
            advInfo.deviceId = deviceId;
            advInfo.verifyEnable = verifyEnable;
            advInfo.deviceType = deviceType;
            advInfo.scanRecord = deviceInfo.scanRecord;
            advInfo.scanTime = SystemClock.elapsedRealtime();
            advInfo.advDataHashMap = new LinkedHashMap<>();
            beaconXInfoHashMap.put(deviceInfo.mac, advInfo);
        }
        if (frameType == 0x00) {
            AdvInfo.AdvData advData = new AdvInfo.AdvData();
            advData.dataBytes = dataBytes;
            advData.frameType = frameType;
            advData.rssi0m = rssi0m;
            advData.namespaceId = namespaceId;
            advData.instanceId = instanceId;
            advInfo.advDataHashMap.put(frameType, advData);
        } else if (frameType == 0x02 || frameType == 0x50) {
            AdvInfo.AdvData advData = new AdvInfo.AdvData();
            advData.dataBytes = dataBytes;
            advData.frameType = frameType;
            advData.uuid = uuid;
            advData.major = major;
            advData.minor = minor;
            advData.rssi1m = rssi1m;
            advInfo.advDataHashMap.put(0x50, advData);
        } else if (frameType == 0x20 || frameType == 0x21 || frameType == 0x22 || frameType == 0x23) {
            AdvInfo.AdvData triggerData = new AdvInfo.AdvData();
//            triggerData.dataStr = dataStr;
            triggerData.dataBytes = dataBytes;
            triggerData.frameType = frameType;
            triggerData.triggerStatus = triggerStatus;
            triggerData.triggerCount = triggerCount;
            advInfo.advDataHashMap.put(frameType, triggerData);
        }
        advInfo.deviceInfoFrame = deviceInfoFrame;
        if (deviceInfoFrame == 0) {
            advInfo.rangingData = rangeData;
            advInfo.accX = accX;
            advInfo.accY = accY;
            advInfo.accZ = accZ;
            advInfo.accShown = accShown;
        }
        return advInfo;
    }
}
