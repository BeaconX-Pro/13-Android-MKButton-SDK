package com.moko.support.d.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.d.entity.OrderCHAR;


public class GetSoftwareRevisionTask extends OrderTask {

    public byte[] data;

    public GetSoftwareRevisionTask(String address) {
        super(OrderCHAR.CHAR_SOFTWARE_REVISION, OrderTask.RESPONSE_TYPE_READ, address);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
