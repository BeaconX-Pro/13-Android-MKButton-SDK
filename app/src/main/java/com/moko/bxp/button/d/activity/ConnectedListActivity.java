package com.moko.bxp.button.d.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.bxp.button.d.AppConstants;
import com.moko.bxp.button.d.R;
import com.moko.bxp.button.d.adapter.ConnectedListAdapter;
import com.moko.bxp.button.d.databinding.DActivityConnectedListBinding;
import com.moko.bxp.button.d.utils.SPUtiles;
import com.moko.lib.bxpui.dialog.LoadingDialog;
import com.moko.lib.bxpui.utils.ToastUtils;
import com.moko.support.d.DMokoSupport;
import com.moko.support.d.OrderTaskAssembler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;


public class ConnectedListActivity extends BaseActivity implements BaseQuickAdapter.OnItemClickListener
        , BaseQuickAdapter.OnItemChildClickListener {

    private DActivityConnectedListBinding mBind;
    private ArrayList<String> mConnectedList;
    private ConnectedListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = DActivityConnectedListBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        mConnectedList = DMokoSupport.getInstance().getConnectedDeviceList();
        adapter = new ConnectedListAdapter();
        adapter.replaceData(mConnectedList);
        adapter.setOnItemChildClickListener(this);
        adapter.setOnItemClickListener(this);
        adapter.openLoadAnimation();
        mBind.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.shape_recycleview_divider));
        mBind.rvDevices.addItemDecoration(itemDecoration);
        mBind.rvDevices.setAdapter(adapter);
        mBind.tvBatchRemoteRemind.setOnClickListener(v -> {
            if (isWindowLocked()) return;
            if (mConnectedList.isEmpty()) {
                ToastUtils.showToast(this, "Cannot be empty!");
                return;
            }
            Intent deviceInfoIntent = new Intent(ConnectedListActivity.this, BatchRemoteReminderActivity.class);
            startLauncher.launch(deviceInfoIntent);
        });
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                // 设备断开，通知页面更新
                dismissLoadingProgressDialog();
                String address = event.getBluetoothDevice().getAddress();
                SPUtiles.removeValue(this, AppConstants.SP_KEY_DEVICE_TYPE + "_" + address);
                SPUtiles.removeValue(this, AppConstants.SP_KEY_SOFTWARE_TYPE + "_" + address);
                SPUtiles.removeValue(this, AppConstants.SP_KEY_BOARD_TYPE + "_" + address);
                ToastUtils.showToast(this, String.format("%s has disconnected", address));
                mConnectedList = DMokoSupport.getInstance().getConnectedDeviceList();
                adapter.replaceData(mConnectedList);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }


    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        final String address = (String) adapter.getItem(position);
        showLoadingProgressDialog();
        DMokoSupport.getInstance().disConnectBle(address);
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        final String address = (String) adapter.getItem(position);
        OrderTaskAssembler.setAddress(address);
        Intent deviceInfoIntent = new Intent(ConnectedListActivity.this, DeviceInfoActivity.class);
        deviceInfoIntent.putExtra(AppConstants.EXTRA_KEY_DEVICE_MAC, address);
        startLauncher.launch(deviceInfoIntent);
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        EventBus.getDefault().unregister(this);
        setResult(RESULT_OK);
        finish();
    }

    private final ActivityResultLauncher<Intent> startLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            mConnectedList = DMokoSupport.getInstance().getConnectedDeviceList();
            adapter.replaceData(mConnectedList);
        }
    });
}
