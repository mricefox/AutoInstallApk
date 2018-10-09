package com.mricefox.autoinstallapk;

import android.graphics.drawable.Drawable;

import java.io.File;

/**
 * <p>Author:MrIcefox
 * <p>Email:extremetsa@gmail.com
 * <p>Description:
 * <p>Date:2018/10/8
 */

public class AppInfo {
    String packageName;
    String name;
    String installedVersionName, apkVersionName;
    int installedVersionCode, apkVersionCode;
    Drawable icon;
    long apkSize;
    File apkFile;
    boolean broken;

    boolean needUpgrade() {
        return installedVersionCode < apkVersionCode;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                ", installedVersionName='" + installedVersionName + '\'' +
                ", apkVersionName='" + apkVersionName + '\'' +
                ", installedVersionCode=" + installedVersionCode +
                ", apkVersionCode=" + apkVersionCode +
                ", icon=" + icon +
                ", apkSize=" + apkSize +
                ", apkFile=" + apkFile +
                '}';
    }
}
