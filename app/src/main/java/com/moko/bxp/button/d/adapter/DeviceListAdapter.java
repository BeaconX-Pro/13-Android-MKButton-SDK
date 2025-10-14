package com.moko.bxp.button.d.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.elvishew.xlog.XLog;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.bxp.button.d.R;
import com.moko.bxp.button.d.entity.AdvInfo;

public class DeviceListAdapter extends BaseQuickAdapter<AdvInfo, BaseViewHolder> {
    public DeviceListAdapter() {
        super(R.layout.d_list_item_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, AdvInfo item) {
        helper.setText(R.id.tv_name, TextUtils.isEmpty(item.name) ? "N/A" : item.name);
        helper.setText(R.id.tv_mac, "MAC:" + item.mac);
        helper.setText(R.id.tv_rssi, String.format("%ddBm", item.rssi));
        helper.setText(R.id.tv_interval_time, item.intervalTime == 0 ? "<->N/A" : String.format("<->%dms", item.intervalTime));
        helper.setText(R.id.tv_battery, item.battery < 0 ? "N/A" : (item.battery > 100 ? String.format("%dmV", item.battery) : String.format("%d%%", item.battery)));
        helper.addOnClickListener(R.id.tv_connect);
        helper.setGone(R.id.tv_connect, item.connectState > 0);
        helper.setGone(R.id.tv_device_id, !TextUtils.isEmpty(item.deviceId));
        helper.setText(R.id.tv_device_id, String.format("Device ID:%s", item.deviceId));
        helper.setVisible(R.id.tv_tx_power, item.txPower != Integer.MIN_VALUE);
        helper.setText(R.id.tv_tx_power, String.format("Tx power:%ddBm", item.txPower));
        LinearLayout parent = helper.getView(R.id.ll_adv_info);
        parent.removeAllViews();
        for (Integer integer : item.advDataHashMap.keySet()) {
            AdvInfo.AdvData advData = item.advDataHashMap.get(integer);
            XLog.i(advData.toString());
            if (advData.frameType == 0x02 || advData.frameType == 0x50) {
                helper.setVisible(R.id.tv_tx_power, false);
                View view = LayoutInflater.from(mContext).inflate(R.layout.d_adv_ibeacon, null);
                TextView tvUUID = view.findViewById(R.id.tv_uuid);
                TextView tvMajor = view.findViewById(R.id.tv_major);
                TextView tvMinor = view.findViewById(R.id.tv_minor);
                TextView tvRssi1m = view.findViewById(R.id.tv_rssi_1m);
                TextView tvProximityState = view.findViewById(R.id.tv_proximity_state);
                TextView tvTxPower = view.findViewById(R.id.tv_tx_power);
                RelativeLayout rlTxPower = view.findViewById(R.id.rl_tx_power);
                tvUUID.setText(advData.uuid);
                tvMajor.setText(String.valueOf(advData.major));
                tvMinor.setText(String.valueOf(advData.minor));
                tvRssi1m.setText(advData.rssi1m + "dBm");
                double distance = MokoUtils.getDistance(item.rssi, Math.abs(advData.rssi1m));
                String distanceDesc = "Unknown";
                if (distance <= 0.1) {
                    distanceDesc = "Immediate";
                } else if (distance > 0.1 && distance <= 1.0) {
                    distanceDesc = "Near";
                } else if (distance > 1.0) {
                    distanceDesc = "Far";
                }
                tvProximityState.setText(distanceDesc);
                rlTxPower.setVisibility(item.txPower == Integer.MIN_VALUE ? View.GONE : View.VISIBLE);
                tvTxPower.setText(item.txPower + "dBm");
                parent.addView(view);
            } else if (advData.frameType == 0x00) {
                helper.setVisible(R.id.tv_tx_power, false);
                View view = LayoutInflater.from(mContext).inflate(R.layout.d_adv_uid, null);
                TextView tvNamespaceId = view.findViewById(R.id.tv_namespace);
                TextView tvInstanceId = view.findViewById(R.id.tv_instance_id);
                TextView tvRssi0m = view.findViewById(R.id.tv_rssi_0m);
                tvNamespaceId.setText(advData.namespaceId);
                tvInstanceId.setText(advData.instanceId);
                tvRssi0m.setText((advData.rssi0m + "dBm"));
                parent.addView(view);
            } else if (advData.frameType == 0x20 || advData.frameType == 0x21 || advData.frameType == 0x22 || advData.frameType == 0x23) {
                helper.setVisible(R.id.tv_tx_power, false);
                String triggerTypeStr = "";
                switch (advData.frameType) {
                    case 0x20:
                        triggerTypeStr = "Single press alarm mode";
                        break;
                    case 0x21:
                        triggerTypeStr = "Double press alarm mode";
                        break;
                    case 0x22:
                        triggerTypeStr = "Long press alarm mode";
                        break;
                    case 0x23:
                        triggerTypeStr = "Abnormal inactivity alarm mode";
                        break;
                }
                View view = LayoutInflater.from(mContext).inflate(R.layout.d_adv_item_trigger, null);
                TextView tvTriggerType = view.findViewById(R.id.tv_trigger_type);
                TextView tvTriggerStatus = view.findViewById(R.id.tv_trigger_status);
                TextView tvTriggerCount = view.findViewById(R.id.tv_trigger_count);
                TextView tvMotionStatus = view.findViewById(R.id.tv_motion_status);
                RelativeLayout rlTriggerCount = view.findViewById(R.id.rl_trigger_count);
                RelativeLayout rlMotionStatus = view.findViewById(R.id.rl_motion_status);
                tvTriggerType.setText(triggerTypeStr);
                if (item.deviceType == 2) {
                    rlMotionStatus.setVisibility(View.VISIBLE);
                    if (advData.triggerStatus == 0) tvTriggerStatus.setText("Standby");
                    else if (advData.triggerStatus == 2) tvTriggerStatus.setText("Main-Triggered");
                    else if (advData.triggerStatus == 4) tvTriggerStatus.setText("Sub-Triggered");
                    tvMotionStatus.setText(advData.motionStatus == 0 ? "Stationary" : "Moving");
                } else {
                    rlMotionStatus.setVisibility(View.GONE);
                    tvTriggerStatus.setText(advData.triggerStatus == 0 ? "Standby" : "Triggered");
                }
                tvTriggerCount.setText(String.valueOf(advData.triggerCount));
                rlTriggerCount.setVisibility(advData.frameType == 0x23 ? View.GONE : View.VISIBLE);
                parent.addView(view);
            }
        }
        if (item.deviceInfoFrame == 0) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.d_adv_item_device, null);
            TextView tvRssi = view.findViewById(R.id.tv_rssi);
            RelativeLayout rlAcc = view.findViewById(R.id.rl_acc);
            TextView tvAcc = view.findViewById(R.id.tv_acc);
            tvRssi.setText(String.format("%ddBm", item.rangingData));
            rlAcc.setVisibility(item.accShown == 1 ? View.VISIBLE : View.GONE);
            tvAcc.setText(String.format("X:%smg Y:%smg Z:%smg", item.accX, item.accY, item.accZ));
            parent.addView(view);
        }
    }
}
