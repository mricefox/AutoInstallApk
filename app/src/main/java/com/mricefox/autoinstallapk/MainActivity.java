package com.mricefox.autoinstallapk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AutoInstallApk.Main";
    private static final int REQUEST_CODE_UNKNOWN_APP = 1 << 1;
    private static final int REQUEST_CODE_ACCESSIBILITY_SETTINGS = 1 << 2;
    private static final int REQUEST_CODE_INSTALL_APK = 1 << 3;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1 << 4;
    private static final int REQUEST_CODE_OPEN_DIRECTORY = 1 << 5;
    private static final String DEFAULT_APK_DIR = "/storage/emulated/0/Android/data/com.coolapk.market/files/Download";
    private static final String DEFAULT_DIR_URI = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2Fcom.coolapk.market%2Ffiles%2FDownload";
    private static final String SP_FILE_NAME = "PrefsFile";
    private static final String SP_KEY_BASE_DIR = "base_dir_uri";
    private static final String SP_KEY_DELETE_APK = "delete_apk";

    private Toolbar toolbar;
    private RecyclerView appListRecyclerView;
    private SwipeRefreshLayout refreshLayout;
    private FloatingActionButton floatingButton;
    private AppListAdapter appListAdapter;
    private List<AppInfo> appsInfo = new ArrayList<>();
    private AppInfo installingApk;
    private boolean batchInstall;
    private Uri baseDirectoryUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        refreshLayout = findViewById(R.id.refresh_layout);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initAppsInfo();
                refreshLayout.setRefreshing(false);
            }
        });

        floatingButton = findViewById(R.id.floating_button);
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                batchInstall = true;
                startInstall();
            }
        });

        appListRecyclerView = findViewById(R.id.not_installed_list);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        appListRecyclerView.setLayoutManager(linearLayoutManager);
        appListAdapter = new AppListAdapter();
        appListRecyclerView.setAdapter(appListAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                AppInfo appInfo = appsInfo.get(position);
                if (!appInfo.needUpgrade()) {
                    return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
                return 0;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                appListRecyclerView.getAdapter().notifyItemRemoved(position);
                AppInfo appInfo = appsInfo.remove(position);
                refreshUpdatableNumUI();
                if (appInfo.apkFile.exists()) {
                    appInfo.apkFile.delete();
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
            }
        });
        itemTouchHelper.attachToRecyclerView(appListRecyclerView);

        SharedPreferences setting = getSharedPreferences(SP_FILE_NAME, 0);
        String baseDirUri = setting.getString(SP_KEY_BASE_DIR, null);
        if (baseDirUri == null) {
            SharedPreferences.Editor editor = setting.edit();
            editor.putString(SP_KEY_BASE_DIR, DEFAULT_DIR_URI);
            editor.commit();
            baseDirUri = DEFAULT_DIR_URI;
        }
        baseDirectoryUri = Uri.parse(baseDirUri);

        requestStoragePermission();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        MenuItem item = menu.findItem(R.id.action_setting);
        CheckBox checkBox = (CheckBox) item.getActionView();
        checkBox.setText("安装完成后删除文件");
        checkBox.setTextColor(Color.WHITE);

        boolean deleteApk;
        SharedPreferences setting = getSharedPreferences(SP_FILE_NAME, 0);
        if (!setting.contains(SP_KEY_DELETE_APK)) {
            SharedPreferences.Editor editor = setting.edit();
            editor.putBoolean(SP_KEY_DELETE_APK, false);
            editor.commit();
            deleteApk = false;
        } else {
            deleteApk = setting.getBoolean(SP_KEY_DELETE_APK, false);
        }
        checkBox.setChecked(deleteApk);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                SharedPreferences setting = getSharedPreferences(SP_FILE_NAME, 0);
                SharedPreferences.Editor editor = setting.edit();
                editor.putBoolean(SP_KEY_DELETE_APK, checked);
                editor.commit();
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_picker:
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                    Toast.makeText(this, "8.0以下系统不支持选择文件夹", Toast.LENGTH_SHORT).show();
                    return true;
                }

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        DocumentsContract.buildDocumentUriUsingTree(baseDirectoryUri
                                , DocumentsContract.getTreeDocumentId(baseDirectoryUri)));
                startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
                return true;
            case R.id.action_refresh:
                refreshAppListManual();
                return true;
            case R.id.action_setting:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode);
        switch (requestCode) {
            case REQUEST_CODE_UNKNOWN_APP:
                if (!InstallUtils.canInstallNonMarketApps(this)) {
                    Toast.makeText(this, "未允许安装未知来源应用,告辞", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_CODE_ACCESSIBILITY_SETTINGS:
                if (!InstallUtils.accessibilityEnabled(this)) {
                    Toast.makeText(this, "未开启无障碍服务,告辞", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    requestInstallNonMarketApps();
                }
                break;
            case REQUEST_CODE_INSTALL_APK:
                int position = appsInfo.indexOf(installingApk);

                updateSingleAppInfo(installingApk);
                if (installingApk.apkVersionCode == installingApk.installedVersionCode) {
                    //install success
                    SharedPreferences setting = getSharedPreferences(SP_FILE_NAME, 0);
                    boolean deleteApk = setting.getBoolean(SP_KEY_DELETE_APK, false);
                    if (deleteApk) {
                        if (installingApk.apkFile.exists()) {
                            installingApk.apkFile.delete();
                        }
                        appsInfo.remove(installingApk);
                        refreshUpdatableNumUI();
                        appListAdapter.notifyItemRemoved(position);
                    } else {
                        appListRecyclerView.getAdapter().notifyItemChanged(position);
                    }
                } else {
                    //install fail
                    installingApk.broken = true;
                    appListRecyclerView.getAdapter().notifyItemChanged(position);
                }

                installingApk = null;

                if (batchInstall) {
                    startInstall();
                }
                break;
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION:
                break;
            case REQUEST_CODE_OPEN_DIRECTORY:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    if (!baseDirectoryUri.equals(uri)) {
                        baseDirectoryUri = data.getData();
                        SharedPreferences setting = getSharedPreferences(SP_FILE_NAME, 0);
                        SharedPreferences.Editor editor = setting.edit();
                        editor.putString(SP_KEY_BASE_DIR, baseDirectoryUri.toString());
                        editor.commit();
                        refreshAppListManual();
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    refreshAppListManual();
                    requestEnableAccessibility();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //no more ask, boo
                        Toast.makeText(this, "没有权限读取apk还肿么安装,告辞", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        requestStoragePermission();
                    }
                }
                break;
        }
    }

    private void requestStoragePermission() {
        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        } else {
            refreshAppListManual();
            requestEnableAccessibility();
        }
    }

    private void initAppsInfo() {
        File baseApkDirectory = new File(Environment.getExternalStorageDirectory()
                , baseDirectoryUri.getPathSegments().get(1).split(":")[1]);
        if (!baseApkDirectory.isDirectory()) {
            Toast.makeText(this, "Apk文件夹不存在", Toast.LENGTH_SHORT).show();
            appsInfo.clear();
            refreshUpdatableNumUI();
            appListAdapter.notifyDataSetChanged();
            return;
        }

        File[] files = baseApkDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.length() > 0 && pathname.getName().endsWith(".apk");
            }
        });
        if (files == null || files.length == 0) {
            Toast.makeText(this, "文件夹内没有apk", Toast.LENGTH_SHORT).show();
            appsInfo.clear();
            refreshUpdatableNumUI();
            appListAdapter.notifyDataSetChanged();
            return;
        }

        List<File> apks = Arrays.asList(files);
        Collections.sort(apks, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.lastModified() > o2.lastModified()) {
                    return -1;
                } else if (o1.lastModified() < o2.lastModified()) {
                    return 1;
                }
                return o1.compareTo(o2);
            }
        });
        appsInfo = getApkFileInfo(apks);
        getInstalledApksInfo(appsInfo);

        for (AppInfo info : appsInfo) {
            Log.d(TAG, "apksInfo:" + info);
        }
        refreshUpdatableNumUI();
        appListAdapter.notifyDataSetChanged();
    }

    private void requestEnableAccessibility() {
        //开启无障碍
        if (!InstallUtils.accessibilityEnabled(this)) {
            Toast.makeText(this, "请开启AutoInstallApk无障碍服务", Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_CODE_ACCESSIBILITY_SETTINGS);
        }
    }

    private void requestInstallNonMarketApps() {
        //允许安装未知来源应用
        if (!InstallUtils.canInstallNonMarketApps(this)) {
            Toast.makeText(this, "请允许安装未知来源应用", Toast.LENGTH_SHORT).show();

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            } else {
                intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            }

            startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP);
        }
    }

    private class AppViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView name, versionInfo, size;
        private ImageButton install;

        public AppViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
            versionInfo = itemView.findViewById(R.id.version_info);
            size = itemView.findViewById(R.id.apk_size);
            install = itemView.findViewById(R.id.install);
        }
    }

    private class AppListAdapter extends RecyclerView.Adapter<AppViewHolder> {

        @Override
        public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.app_list_item_layout, parent, false);
            AppViewHolder viewHolder = new AppViewHolder(v);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(final AppViewHolder holder, final int position) {
            final AppInfo appInfo = appsInfo.get(position);
            holder.icon.setImageDrawable(appInfo.icon);
            holder.name.setText(appInfo.name);
            holder.versionInfo.setText(String.format("%s > %s", appInfo.installedVersionName, appInfo.apkVersionName));
            holder.size.setText(displayFileSize(appInfo.apkSize));
            if (appInfo.needUpgrade()) {
                holder.install.setImageResource(R.drawable.ic_archive_blue_24dp);
            } else {
                holder.install.setImageResource(R.drawable.ic_delete_gray_24dp);
            }
            holder.install.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (appInfo.needUpgrade()) {
                        batchInstall = false;
                        installingApk = appInfo;
                        InstallUtils.installApk(appInfo.apkFile, MainActivity.this, REQUEST_CODE_INSTALL_APK);
                    } else {
                        if (appInfo.apkFile.exists()) {
                            appInfo.apkFile.delete();
                        }
                        appsInfo.remove(appInfo);
                        refreshUpdatableNumUI();
                        appListAdapter.notifyItemRemoved(position);
                    }
                }
            });
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return appsInfo.size();
        }
    }

    private void getInstalledApksInfo(List<AppInfo> outInfos) {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);

        for (PackageInfo pkgInfo : packageInfoList) {
            for (AppInfo appInfo : outInfos) {
                if (appInfo.packageName.equals(pkgInfo.packageName)) {
                    appInfo.name = pkgInfo.applicationInfo.loadLabel(pm).toString();
                    appInfo.installedVersionName = pkgInfo.versionName;
                    appInfo.installedVersionCode = pkgInfo.versionCode;
                    appInfo.icon = pkgInfo.applicationInfo.loadIcon(pm);
                }
            }
        }
    }

    private List<AppInfo> getApkFileInfo(List<File> apkFiles) {
        PackageManager pm = getPackageManager();
        List<AppInfo> appInfos = new ArrayList<>(apkFiles.size());

        for (File apk : apkFiles) {
            PackageInfo info = pm.getPackageArchiveInfo(apk.getPath(), 0);
            if (info == null) {
                continue;//skip illegal apk
            }
            AppInfo appInfo = new AppInfo();
            appInfo.packageName = info.packageName;
            //MiuiResources.getText NotFoundException, can not find res ?
            appInfo.name = null;
            appInfo.apkVersionName = info.versionName;
            appInfo.apkVersionCode = info.versionCode;
            appInfo.apkSize = apk.length();
            appInfo.apkFile = apk;
            appInfos.add(appInfo);
        }
        return appInfos;
    }

    private static String displayFileSize(long fileSize) {
        if (fileSize <= 0) {
            return "0";
        } else {
            final String[] fileUnit = new String[]{"B", "KB", "MB", "GB", "TB"};
            int group = (int) (Math.log10(fileSize) / Math.log10(1024));
            return new DecimalFormat("#,##0.##").format(fileSize / Math.pow(1024, group))
                    + " " + fileUnit[group];
        }
    }

    private void startInstall() {
        for (AppInfo appInfo : appsInfo) {
            if (appInfo.needUpgrade() && !appInfo.broken) {
                installingApk = appInfo;
                InstallUtils.installApk(appInfo.apkFile, MainActivity.this, REQUEST_CODE_INSTALL_APK);
                break;
            }
        }
    }

    private void refreshAppListManual() {
        refreshLayout.setRefreshing(true);
        initAppsInfo();
        refreshLayout.setRefreshing(false);
    }

    private void updateSingleAppInfo(AppInfo appInfo) {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);

        for (PackageInfo pkgInfo : packageInfoList) {
            if (appInfo.packageName.equals(pkgInfo.packageName)) {
                appInfo.name = pkgInfo.applicationInfo.loadLabel(pm).toString();
                appInfo.installedVersionName = pkgInfo.versionName;
                appInfo.installedVersionCode = pkgInfo.versionCode;
                appInfo.icon = pkgInfo.applicationInfo.loadIcon(pm);
                break;
            }
        }
    }

    private void refreshUpdatableNumUI() {
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        int num = 0;
        for (AppInfo info : appsInfo) {
            if (info.needUpgrade()) {
                ++num;
            }
        }
        getSupportActionBar().setTitle(String.format("可更新(%d)", num));
    }
}
