package com.moko.support.d.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.d.entity.OrderCHAR;

public class GetFirmwareRevisionTask extends OrderTask {

    public byte[] data;

    public GetFirmwareRevisionTask(String address) {
        super(OrderCHAR.CHAR_FIRMWARE_REVISION, OrderTask.RESPONSE_TYPE_READ, address);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
