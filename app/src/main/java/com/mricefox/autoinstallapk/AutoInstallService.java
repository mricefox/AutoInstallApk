package com.mricefox.autoinstallapk;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Messenger;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * <p>Author:MrIcefox
 * <p>Email:extremetsa@gmail.com
 * <p>Description:
 * <p>Date:2018/8/6
 */

public class AutoInstallService extends AccessibilityService {
    public static final String ACTION_START_INSTALL = "com.mricefox.autoinstallapk.intent.action.START_INSTALL";
    public static final String ACTION_FINISH_INSTALL = "com.mricefox.autoinstallapk.intent.action.FINISH_INSTALL";

    private static final String TAG = "AutoInstallApk.Svr";

    private boolean enable = false;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_START_INSTALL:
                    enable = true;
                    break;
                case ACTION_FINISH_INSTALL:
                    enable = false;
                    break;
            }
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!enable) {
            return;
        }
        if ("com.google.android.packageinstaller".equals(event.getPackageName())) {
            List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByText("安装");
            for (AccessibilityNodeInfo node : nodes) {
                if ("android.widget.Button".equals(node.getClassName())) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return;
                }
            }

            nodes = event.getSource().findAccessibilityNodeInfosByText("应用未安装");
            if (!nodes.isEmpty()) {
                nodes = event.getSource().findAccessibilityNodeInfosByText("完成");
                for (AccessibilityNodeInfo node : nodes) {
                    if ("android.widget.Button".equals(node.getClassName())) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return;
                    }
                }
            }

            nodes = event.getSource().findAccessibilityNodeInfosByText("解析软件包时出现问题");
            if (!nodes.isEmpty()) {
                nodes = event.getSource().findAccessibilityNodeInfosByText("确定");
                for (AccessibilityNodeInfo node : nodes) {
                    if ("android.widget.Button".equals(node.getClassName())) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return;
                    }
                }
            }

        } else if ("com.miui.global.packageinstaller".equals(event.getPackageName())) {
            List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByText("完成");
            for (AccessibilityNodeInfo node : nodes) {
                if ("android.widget.Button".equals(node.getClassName())) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return;
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "onServiceConnected");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_START_INSTALL);
        filter.addAction(ACTION_FINISH_INSTALL);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        return super.onUnbind(intent);
    }
}
