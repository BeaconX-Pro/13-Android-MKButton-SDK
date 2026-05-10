package com.moko.support.d.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.d.entity.OrderCHAR;

public class GetHardwareRevisionTask extends OrderTask {

    public byte[] data;

    public GetHardwareRevisionTask(String address) {
        super(OrderCHAR.CHAR_HARDWARE_REVISION, OrderTask.RESPONSE_TYPE_READ, address);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
