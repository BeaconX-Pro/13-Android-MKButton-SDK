package com.moko.bxp.button.d.entity;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class AdvInfo implements Serializable {

    public String name;
    public int rssi;
    public String mac;
    public String scanRecord;
    public int battery;
    public long intervalTime;
    public long scanTime;
    public int txPower;
    public int rangingData;
    public int accX = 0;
    public int accY = 0;
    public int accZ = 0;
    public int accShown = 0;
    public int connectState;
    public int verifyEnable;
    public int deviceType;
    public boolean isOTA;
    public int deviceInfoFrame;
    public String beaconTemp;
    public String deviceId;
    public LinkedHashMap<Integer, AdvData> advDataHashMap;

    @Override
    public String toString() {
        return "AdvInfo{" +
                "name='" + name + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }

    public static class AdvData {
        public int frameType;
        public int triggerStatus;
        public int triggerCount;
        public int major;
        public int minor;
        public String uuid;
        public int rssi1m;
        public int rssi0m;
        public String instanceId;
        public String namespaceId;
        public byte[] dataBytes;
        public int motionStatus;
//        public String dataStr;

        @Override
        public String toString() {
            return "AdvData{" +
                    "frameType=" + frameType +
//                    ", data='" + dataStr + '\'' +
                    '}';
        }
    }
}
