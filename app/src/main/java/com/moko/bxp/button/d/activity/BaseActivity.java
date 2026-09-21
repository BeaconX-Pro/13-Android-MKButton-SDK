package com.moko.bxp.button.d.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.DisplayCutout;

import com.elvishew.xlog.XLog;

import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


public class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // targetSdk 35+ / Android 16：预测性返回不会再走 Activity.onBackPressed()，
        // 在此统一接管系统返回，并继续回调子类已有的 onBackPressed() 实现。
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BaseActivity.this.onBackPressed();
            }
        });
        if (savedInstanceState != null) {
            Intent intent = new Intent(this, GuideActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        getWindow().getDecorView().setOnApplyWindowInsetsListener((v, insets) -> {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                List<Rect> rects = cutout.getBoundingRects();
                if (rects.size() != 0) {
                    getWindow().getDecorView().setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                            cutout.getSafeInsetRight(), cutout.getSafeInsetBottom());
                }
            }
            return insets;
        });
    }

    /**
     * 覆盖 ComponentActivity 默认实现（会再次进 OnBackPressedDispatcher，可能死循环）。
     * 未重写的页面默认 finish；已重写 onBackPressed 的子类仍走各自逻辑。
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        XLog.i("onConfigurationChanged...");
        finish();
    }


    // 记录上次页面控件点击时间,屏蔽无效点击事件
    protected long mLastOnClickTime = 0;

    public boolean isWindowLocked() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastOnClickTime > 500) {
            mLastOnClickTime = current;
            return false;
        } else {
            return true;
        }
    }

    public boolean isWriteStoragePermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
