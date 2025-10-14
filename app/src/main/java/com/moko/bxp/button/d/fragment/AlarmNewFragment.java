package com.moko.bxp.button.d.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.bxp.button.d.activity.DeviceInfoActivity;
import com.moko.bxp.button.d.databinding.FragmentDAlarmNewBinding;

public class AlarmNewFragment extends Fragment {

    private FragmentDAlarmNewBinding mBind;
    private DeviceInfoActivity activity;

    public AlarmNewFragment() {
    }

    public static AlarmNewFragment newInstance() {
        AlarmNewFragment fragment = new AlarmNewFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBind = FragmentDAlarmNewBinding.inflate(inflater, container, false);
        activity = (DeviceInfoActivity) getActivity();
        if (activity.mBoardType == 3) {
            mBind.clLongConnectionAlarmEvents.setVisibility(View.VISIBLE);
        } else {
            mBind.rlLongConnectionAlarmEvents.setVisibility(View.VISIBLE);
        }
        return mBind.getRoot();
    }


    public void setSinglePressModeSwitch(int onOff) {
        mBind.tvSinglePressModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setDoublePressModeSwitch(int onOff) {
        mBind.tvDoublePressModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setLongPressModeSwitch(int onOff) {
        mBind.tvLongPressModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setAbnormalInactivityModeSwitch(int onOff) {
        mBind.tvAbnormalInactivityModeSwitch.setText(onOff == 1 ? "ON" : "OFF");
    }

    public void setAbnormalInactivityModeVisibility(boolean hasAcc) {
        mBind.layoutAbnormalInactivityModeSwitch.setVisibility(hasAcc ? View.VISIBLE : View.GONE);
    }

    public void setEventCount(int count) {
        mBind.tvEventCount.setText(String.valueOf(count));
    }

    public void setSubEventCount(int count) {
        mBind.tvEventCountSub.setText(String.valueOf(count));
    }

    public void setMainEventCount(int count) {
        mBind.tvEventCountMain.setText(String.valueOf(count));
    }
}
