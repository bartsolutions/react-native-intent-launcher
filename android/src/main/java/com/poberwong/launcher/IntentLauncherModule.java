package com.poberwong.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.*;


/**
 * Created by poberwong on 16/6/30.
 */
public class IntentLauncherModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final int REQUEST_CODE = 12;
    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_CATEGORY = "category";
    private static final String TAG_EXTRA = "extra";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_CLASS_NAME = "className";
    Promise promise;

    public IntentLauncherModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "IntentLauncher";
    }

    @ReactMethod
    public void LaunchAutoStartSetting() {
        //Get manufacturer
        String manufacturer = Build.MANUFACTURER;
        Log.i("AUTO_START", " deviceBrand : " + manufacturer);
        Intent intent =new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName comp =null;
        if (!TextUtils.isEmpty(manufacturer)) {
            if ("honor".equals(manufacturer.toLowerCase()) || "huawei".equals(manufacturer.toLowerCase())) {
                if (Build.VERSION.SDK_INT >= 26) {
                    comp = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity");
                } else if (Build.VERSION.SDK_INT >= 23) {
                    comp = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
                } else {
                    comp = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.com.huawei.permissionmanager.ui.MainActivity");
                }
            } else if ("xiaomi".equals(manufacturer.toLowerCase())) {
                comp = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            } else if ("vivo".equals(manufacturer.toLowerCase())) {
                if (Build.VERSION.SDK_INT >= 26) {
                    comp = new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity");
                } else {
                    comp = new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.SoftwareManagerActivity");
                }
            } else if ("oppo".equals(manufacturer.toLowerCase())) {
                if (Build.VERSION.SDK_INT >= 26) {
                    comp = new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity");
                } else {
                    comp = new ComponentName("com.color.safecenter", "com.color.safecenter.permission.startup.StartupAppListActivity");
                }
            } else if ("samsung".equals(manufacturer.toLowerCase())) {
                comp = new ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm_cn.com.samsung.android.sm.ui.ram.AutoRunActivity");
            }
        }

        //其他机型直接跳设置界面，适配的几个厂商中有不同系列的手机，可能会跳不到指定界面，因此需要在异常时调到普通设置界面做保护
        Context context = getReactApplicationContext();
        try {
            if (comp == null) {
                intent = new Intent(Settings.ACTION_SETTINGS);
                context.startActivity(intent);
            } else {
                intent.setComponent(comp);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            //抛出异常就直接打开设置页面 
            e.printStackTrace();
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }

    }

    /**
     * 选用方案
     * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     * getReactApplicationContext().startActivity(intent);
     */
    @ReactMethod
    public void startActivity(ReadableMap params, final Promise promise) {
        this.promise = promise;
        Intent intent = new Intent();

        if (params.hasKey(ATTR_CLASS_NAME)) {
            ComponentName cn;
            if (params.hasKey(ATTR_PACKAGE_NAME)) {
                cn = new ComponentName(params.getString(ATTR_PACKAGE_NAME), params.getString(ATTR_CLASS_NAME));
            } else {
                cn = new ComponentName(getReactApplicationContext(), params.getString(ATTR_CLASS_NAME));
            }
            intent.setComponent(cn);
        }
        if (params.hasKey(ATTR_ACTION)) {
            intent.setAction(params.getString(ATTR_ACTION));
        }
        if (params.hasKey(ATTR_DATA)) {
            intent.setData(Uri.parse(params.getString(ATTR_DATA)));
        }
        if (params.hasKey(ATTR_TYPE)) {
            intent.setType(params.getString(ATTR_TYPE));
        }
        if (params.hasKey(TAG_EXTRA)) {
            intent.putExtras(Arguments.toBundle(params.getMap(TAG_EXTRA)));
        }
        if (params.hasKey(ATTR_FLAGS)) {
            intent.addFlags(params.getInt(ATTR_FLAGS));
        }
        if (params.hasKey(ATTR_CATEGORY)) {
            intent.addCategory(params.getString(ATTR_CATEGORY));
        }

        getReactApplicationContext().startActivityForResult(intent, REQUEST_CODE, null); // 暂时使用当前应用的任务栈
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode != REQUEST_CODE) {
            return;
        }
        WritableMap params = Arguments.createMap();
        if (intent != null) {
            params.putInt("resultCode", resultCode);

            Uri data = intent.getData();
            if (data != null) {
                params.putString("data", data.toString());
            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                params.putMap("extra", Arguments.fromBundle(extras));
            }
        }

        this.promise.resolve(params);
    }
}
