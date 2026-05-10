package com.moko.support.d.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.d.entity.OrderCHAR;

public class GetManufacturerNameTask extends OrderTask {

    public byte[] data;

    public GetManufacturerNameTask(String address) {
        super(OrderCHAR.CHAR_MANUFACTURER_NAME, OrderTask.RESPONSE_TYPE_READ, address);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
