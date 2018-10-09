package com.mricefox.autoinstallapk;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
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
    private static final String TAG = "AutoInstallApk.Svr";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        Log.d(TAG, "onAccessibilityEvent pkg:" + event.getPackageName());
//        Log.d(TAG, "onAccessibilityEvent class:" + event.getClassName());
//        Log.d(TAG, "onAccessibilityEvent txt:" + event.getText());

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
        Log.w(TAG, "onCreate");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.w(TAG, "onServiceConnected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy");
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.w(TAG, "onUnbind");
        return super.onUnbind(intent);
    }
}
