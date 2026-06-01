package com.pos.system;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * BarcodeHelper - مساعد الباركود الموحَّد باستخدام ZXing المدمج
 * =====================================================================
 * يُستخدَم من: ActivityAddProductActivity, ActivityCartActivity,
 *              ActivityProductsActivity, ActivityInvoicesActivity
 *
 * الاستخدام:
 *   BarcodeHelper.launch(activity, launcher);
 *
 * في كل Activity:
 *   1) أعلن: ActivityResultLauncher<Intent> scanLauncher = BarcodeHelper.registerLauncher(this, result -> {...});
 *   2) عند الضغط: BarcodeHelper.launch(this, scanLauncher);
 *
 * الباركود يُعاد دائماً في:
 *   result.getData().getStringExtra("SCAN_RESULT")
 */
public class BarcodeHelper {

    /** مفاتيح الـ intent الموحَّدة */
    public static final String EXTRA_RESULT   = "SCAN_RESULT";  // المفتاح الرئيسي
    public static final String EXTRA_RESULT_2 = "RESULT";        // fallback
    public static final String EXTRA_RESULT_3 = "result";        // fallback

    /** واجهة نتيجة المسح */
    public interface OnScanResult {
        void onResult(String barcode);
    }

    /**
     * تسجيل launcher للـ barcode scanner
     * يجب استدعاؤه في onCreate() قبل setContentView()
     */
    public static ActivityResultLauncher<Intent> registerLauncher(
            AppCompatActivity activity, OnScanResult callback) {

        return activity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String barcode = extractBarcode(result.getData());
                    if (barcode != null && !barcode.isEmpty()) {
                        callback.onResult(barcode);
                    }
                }
            }
        );
    }

    /**
     * فتح شاشة المسح - يستخدم ActivityBarcodeScannerActivity المدمج مع ZXing
     */
    public static void launch(Activity activity, ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(activity, ActivityBarcodeScannerActivity.class);
        launcher.launch(intent);
    }

    /**
     * استخراج نص الباركود من Intent (يتحقق من كل المفاتيح الممكنة)
     */
    public static String extractBarcode(Intent data) {
        if (data == null) return null;
        String code = data.getStringExtra(EXTRA_RESULT);
        if (code == null) code = data.getStringExtra(EXTRA_RESULT_2);
        if (code == null) code = data.getStringExtra(EXTRA_RESULT_3);
        if (code == null) code = data.getStringExtra("barcode");
        return (code != null) ? code.trim() : null;
    }
}
