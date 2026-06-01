package com.pos.system.managers;

import android.app.Activity;
import android.content.IntentSender;
import android.util.Log;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

/**
 * AppUpdateManager - تحديثات التطبيق عبر Google Play In-App Update
 * النسخة 1.0
 *
 * النوعان:
 * - FLEXIBLE: تحديث في الخلفية (للتحديثات غير الحرجة)
 * - IMMEDIATE: تحديث إلزامي فوري (للتحديثات الأمنية الحرجة)
 */
public class AppUpdateManager {

    private static final String TAG        = "AppUpdateManager";
    private static final int    UPDATE_RC  = 1001; // Request Code

    private com.google.android.play.core.appupdate.AppUpdateManager manager;
    private Activity activity;

    public AppUpdateManager(Activity activity) {
        this.activity = activity;
        try {
            manager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(activity);
        } catch (Exception e) {
            Log.e(TAG, "AppUpdateManager init: " + e.getMessage());
        }
    }

    /**
     * فحص وتحديث مرن (في الخلفية)
     */
    public void checkForFlexibleUpdate() {
        if (manager == null) return;
        manager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                startFlexibleUpdate(info);
            }
        }).addOnFailureListener(e -> Log.w(TAG, "Update check failed: " + e.getMessage()));
    }

    /**
     * تحديث فوري إلزامي (للتحديثات الحرجة)
     */
    public void checkForImmediateUpdate() {
        if (manager == null) return;
        manager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startImmediateUpdate(info);
            }
        }).addOnFailureListener(e -> Log.w(TAG, "Update check failed: " + e.getMessage()));
    }

    private void startFlexibleUpdate(AppUpdateInfo info) {
        try {
            manager.startUpdateFlowForResult(info,
                activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), UPDATE_RC);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "startFlexibleUpdate: " + e.getMessage());
        }
    }

    private void startImmediateUpdate(AppUpdateInfo info) {
        try {
            manager.startUpdateFlowForResult(info,
                activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE), UPDATE_RC);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "startImmediateUpdate: " + e.getMessage());
        }
    }

    /**
     * استئناف تحديث متوقف عند الرجوع للتطبيق
     */
    public void onResume() {
        if (manager == null) return;
        manager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (info.installStatus() ==
                    com.google.android.play.core.install.model.InstallStatus.DOWNLOADED) {
                manager.completeUpdate();
            }
        });
    }
}
