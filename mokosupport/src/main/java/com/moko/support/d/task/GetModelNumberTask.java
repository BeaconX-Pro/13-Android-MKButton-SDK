package com.moko.support.d.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.d.entity.OrderCHAR;

public class GetModelNumberTask extends OrderTask {

    public byte[] data;

    public GetModelNumberTask(String address) {
        super(OrderCHAR.CHAR_MODEL_NUMBER, OrderTask.RESPONSE_TYPE_READ, address);
    }

    @Override
    public byte[] assemble() {
        return data;
    }
}
