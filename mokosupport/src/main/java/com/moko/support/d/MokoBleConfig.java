package com.moko.support.d;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoBleManager;
import com.moko.ble.lib.callback.MokoResponseCallback;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.d.entity.OrderCHAR;
import com.moko.support.d.entity.OrderServices;

import androidx.annotation.NonNull;

final class MokoBleConfig extends MokoBleManager {

    private MokoResponseCallback mMokoResponseCallback;
    private BluetoothGattCharacteristic paramsCharacteristic;
    private BluetoothGattCharacteristic disconnectCharacteristic;
    private BluetoothGattCharacteristic singleTriggerCharacteristic;
    private BluetoothGattCharacteristic doubleTriggerCharacteristic;
    private BluetoothGattCharacteristic longTriggerCharacteristic;
    private BluetoothGattCharacteristic longConnectionCharacteristic;
    private BluetoothGattCharacteristic clickSubEventCharacteristic;
    private BluetoothGattCharacteristic accCharacteristic;
    private BluetoothGattCharacteristic passwordCharacteristic;
    private BluetoothGattCharacteristic clickEventCharacteristic;
    private BluetoothGatt gatt;

    public MokoBleConfig(@NonNull Context context, MokoResponseCallback callback) {
        super(context);
        mMokoResponseCallback = callback;
    }

    @Override
    public boolean checkServiceCharacteristicSupported(BluetoothGatt gatt) {
        final BluetoothGattService service = gatt.getService(OrderServices.SERVICE_CUSTOM.getUuid());
        final BluetoothGattService otaService = gatt.getService(OrderServices.SERVICE_OTA.getUuid());
        if (service != null) {
            this.gatt = gatt;
            paramsCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_PARAMS.getUuid());
            disconnectCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_DISCONNECT.getUuid());
            singleTriggerCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_SINGLE_TRIGGER.getUuid());
            doubleTriggerCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_DOUBLE_TRIGGER.getUuid());
            longTriggerCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_LONG_TRIGGER.getUuid());
            longConnectionCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_LONG_CONNECTION.getUuid());
            clickSubEventCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_CLICK_SUB_EVENT.getUuid());
            accCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_ACC.getUuid());
            passwordCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_PASSWORD.getUuid());
            clickEventCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_CLICK_EVENT.getUuid());
            return passwordCharacteristic != null
                    && disconnectCharacteristic != null
                    && paramsCharacteristic != null;
        }
        if (otaService != null) {
            disconnectCharacteristic = null;
            this.gatt = gatt;
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        if (disconnectCharacteristic == null) {
            requestMtu(247).done(bluetoothDevice -> mMokoResponseCallback.onServicesDiscovered(gatt)).enqueue();
            return;
        }
        requestMtu(247).with(((device, mtu) -> {
        })).then((device -> {
            enableDisconnectNotify();
            enablePasswordNotify();
            enableParamsNotify();
        })).enqueue();
    }

    @Override
    public void write(BluetoothGattCharacteristic characteristic, byte[] value) {
    }

    @Override
    public void read(BluetoothGattCharacteristic characteristic, byte[] value) {
        mMokoResponseCallback.onCharacteristicRead(characteristic, value);
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        mMokoResponseCallback.onDeviceDisconnected(device, reason);
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        mMokoResponseCallback.onDeviceDisconnected(device, reason);
    }


    public void enableParamsNotify() {
        setNotificationCallback(paramsCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(paramsCharacteristic, value);
        });
        enableNotifications(paramsCharacteristic).done(device -> mMokoResponseCallback.onServicesDiscovered(gatt)).enqueue();
    }

    public void disableParamsNotify() {
        disableNotifications(paramsCharacteristic).enqueue();
    }

    public void enableDisconnectNotify() {
        setNotificationCallback(disconnectCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(disconnectCharacteristic, value);
        });
        enableNotifications(disconnectCharacteristic).enqueue();
    }

    public void disableDisconnectNotify() {
        disableNotifications(disconnectCharacteristic).enqueue();
    }

    public void enablePasswordNotify() {
        setNotificationCallback(passwordCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(passwordCharacteristic, value);
        });
        enableNotifications(passwordCharacteristic).enqueue();
    }

    public void disablePasswordNotify() {
        disableNotifications(passwordCharacteristic).enqueue();
    }

    public void enableSingleTriggerNotify() {
        setNotificationCallback(singleTriggerCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(singleTriggerCharacteristic, value);
        });
        enableNotifications(singleTriggerCharacteristic).enqueue();
    }

    public void disableSingleTriggerNotify() {
        disableNotifications(singleTriggerCharacteristic).enqueue();
    }

    public void enableDoubleTriggerNotify() {
        setNotificationCallback(doubleTriggerCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(doubleTriggerCharacteristic, value);
        });
        enableNotifications(doubleTriggerCharacteristic).enqueue();
    }

    public void disableDoubleTriggerNotify() {
        disableNotifications(doubleTriggerCharacteristic).enqueue();
    }

    public void enableLongTriggerNotify() {
        setNotificationCallback(longTriggerCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(longTriggerCharacteristic, value);
        });
        enableNotifications(longTriggerCharacteristic).enqueue();
    }

    public void disableLongTriggerNotify() {
        disableNotifications(longTriggerCharacteristic).enqueue();
    }

    public void enableAccNotify() {
        setNotificationCallback(accCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(accCharacteristic, value);
        });
        enableNotifications(accCharacteristic).enqueue();
    }

    public void disableAccNotify() {
        disableNotifications(accCharacteristic).enqueue();
    }

    public void enableClickEventNotify() {
        setNotificationCallback(clickEventCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(clickEventCharacteristic, value);
        });
        enableNotifications(clickEventCharacteristic).enqueue();
    }

    public void disableClickEventNotify() {
        disableNotifications(clickEventCharacteristic).enqueue();
    }
    public void enableClickSubEventNotify() {
        setNotificationCallback(clickSubEventCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(clickSubEventCharacteristic, value);
        });
        enableNotifications(clickSubEventCharacteristic).enqueue();
    }

    public void disableClickSubEventNotify() {
        disableNotifications(clickSubEventCharacteristic).enqueue();
    }

    public void enableLongConnectionNotify() {
        setNotificationCallback(longConnectionCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(longConnectionCharacteristic, value);
        });
        enableNotifications(longConnectionCharacteristic).enqueue();
    }

    public void disableLongConnectionNotify() {
        disableNotifications(longConnectionCharacteristic).enqueue();
    }
}