package com.moko.bxp.button.d.activity;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.bxp.button.d.AppConstants;
import com.moko.bxp.button.d.R;
import com.moko.bxp.button.d.adapter.ExportDataListAdapter;
import com.moko.bxp.button.d.databinding.DActivityExportDataLongConnectionBinding;
import com.moko.bxp.button.d.utils.ToastUtils;
import com.moko.bxp.button.d.utils.Utils;
import com.moko.lib.bxpui.dialog.LoadingMessageDialog;
import com.moko.support.d.DMokoSupport;
import com.moko.support.d.OrderTaskAssembler;
import com.moko.support.d.entity.ExportData;
import com.moko.support.d.entity.OrderCHAR;
import com.moko.support.d.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import androidx.recyclerview.widget.LinearLayoutManager;

public class ExportLongConnectionDataActivity extends BaseActivity {

    private static final String EXPORT_FILE = "Long_connection_event.txt";
    private static final String EXPORT_FILE_TITLE = "Long_connection_event";

    private static String PATH_LOGCAT;
    private DActivityExportDataLongConnectionBinding mBind;

    private StringBuilder storeString;
    private ArrayList<ExportData> exportDatas;
    private boolean mIsSync;
    private ExportDataListAdapter adapter;
    private SimpleDateFormat sdf;
    private TimeZone timeZone;
    private boolean mIsShown;
    private String exportTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = DActivityExportDataLongConnectionBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());

        PATH_LOGCAT = DMainActivity.PATH_LOGCAT + File.separator + EXPORT_FILE;
        exportTitle = EXPORT_FILE_TITLE;
        exportDatas = new ArrayList<>();
        storeString = new StringBuilder();
        adapter = new ExportDataListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(exportDatas);
        mBind.rvExportData.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvExportData.setAdapter(adapter);
        timeZone = TimeZone.getTimeZone("GMT");
        sdf = new SimpleDateFormat(AppConstants.PATTERN_YYYY_MM_DD_T_HH_MM_SS_Z, Locale.US);
        sdf.setTimeZone(timeZone);
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
            EventBus.getDefault().cancelEventDelivery(event);
            runOnUiThread(() -> {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_LONG_CONNECTION:
                        int header = value[0] & 0xFF;// 0xEB
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xEB) return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x02 && cmd == 0x07 && length == 0x09) {
                            if (!mIsShown) {
                                mIsShown = true;
                                mBind.tvExport.setEnabled(true);
                            }
                            ExportData exportData = new ExportData();
                            byte[] timeBytes = Arrays.copyOfRange(value, 4, 12);
                            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).put(timeBytes, 0, timeBytes.length);
                            byteBuffer.flip();
                            long time = byteBuffer.getLong();
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeZone(timeZone);
                            calendar.setTimeInMillis(time);

                            String timestampStr = sdf.format(calendar.getTime());
                            exportData.timestamp = timestampStr;
                            exportData.triggerMode = String.valueOf(value[12] & 0xFF);

                            exportDatas.add(0, exportData);
                            storeString.insert(0, String.format("%s  %s\n", timestampStr, exportData.triggerMode));
                        }
                        adapter.replaceData(exportDatas);
                        break;
                }
            });
        }
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissSyncProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            runOnUiThread(() -> {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                if (Objects.requireNonNull(orderCHAR) == OrderCHAR.CHAR_PARAMS) {
                    if (value.length > 4) {
                        int header = value[0] & 0xFF;// 0xEB
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header != 0xEB) return;
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) {
                            return;
                        }
                        int length = value[3] & 0xFF;
                        if (flag == 0x01 && length == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            if (configKeyEnum == ParamsKeyEnum.KEY_LONG_CONNECTION_CLEAR) {
                                if (result == 0) {
                                    ToastUtils.showToast(ExportLongConnectionDataActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                } else {
                                    ToastUtils.showToast(ExportLongConnectionDataActivity.this, "Success！");
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    public void showSyncingProgressDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Syncing..");
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void dismissSyncProgressDialog() {
        if (mLoadingMessageDialog != null) mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    private void back() {
        if (mIsSync) {
            DMokoSupport.getInstance().disableLongConnectionNotify();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    public void onSync(View view) {
        if (isWindowLocked()) return;
        if (!mIsSync) {
            mIsSync = true;
            exportDatas.clear();
            storeString = new StringBuilder();
            adapter.replaceData(exportDatas);
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
            mBind.ivSync.startAnimation(animation);
            mBind.tvSync.setText("Stop");
            DMokoSupport.getInstance().enableLongConnectionNotify();

        } else {
            mIsSync = false;
            mBind.ivSync.clearAnimation();
            mBind.tvSync.setText("Sync");
            DMokoSupport.getInstance().disableLongConnectionNotify();
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked()) return;
        storeString = new StringBuilder();
        mBind.tvExport.setEnabled(false);
        exportDatas.clear();
        adapter.replaceData(exportDatas);
        showSyncingProgressDialog();
        DMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setLongConnectionEventClear());
    }

    public void onExport(View view) {
        if (isWindowLocked()) return;
        showSyncingProgressDialog();
        writeTrackedFile("");
        mBind.tvExport.postDelayed(() -> {
            dismissSyncProgressDialog();
            final String log = storeString.toString();
            if (!TextUtils.isEmpty(log)) {
                writeTrackedFile(log);
                File file = getTrackedFile();
                // 发送邮件
                String address = "Development@mokotechnology.com";
                Utils.sendEmail(ExportLongConnectionDataActivity.this, address, exportTitle, exportTitle, "Choose Email Client", file);
            }
        }, 500);
    }

    public void onBack(View view) {
        back();
    }


    public static void writeTrackedFile(String thLog) {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(thLog);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getTrackedFile() {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
