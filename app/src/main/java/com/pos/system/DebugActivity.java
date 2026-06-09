package com.pos.system;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pos.system.BaseActivity;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * DebugActivity - صفحة عرض وإدارة الأخطاء المحسّنة
 * 
 * الميزات الجديدة:
 * - عرض تقارير متعددة
 * - نسخ التقرير للحافظة
 * - إرسال عبر Email/WhatsApp/Telegram
 * - حفظ سجل الأخطاء
 * - واجهة محسّنة
 * 
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class DebugActivity extends BaseActivity {

    // المفاتيح
    public static final String EXTRA_ERROR = "extra_error";
    public static final String EXTRA_STACK = "extra_stack";
    public static final String EXTRA_TIMESTAMP = "extra_timestamp";

    // معلومات الدعم — يجب تحديثها قبل النشر على Google Play
    private static final String DEV_EMAIL = "support@smartpos.app";
    private static final String DEV_WHATSAPP = ""; // أضف رقم الواتساب بدون + قبل النشر
    private static final String DEV_TELEGRAM = "";
    
    // المتغيرات
    private String errorMessage;
    private String stackTrace;
    private long timestamp;
    private String fullReport;
    
    // Views
    private TextView tvErrorMessage;
    private TextView tvStackTrace;
    private TextView tvTimestamp;
    private TextView tvDeviceInfo;
    private MaterialCardView cardDetails;
    private NestedScrollView scrollDetails;
    private MaterialButton btnToggleDetails;
    private MaterialButton btnRestart;
    private MaterialButton btnCopy;
    private MaterialButton btnEmail;
    private MaterialButton btnWhatsApp;
    private MaterialButton btnTelegram;
    private MaterialButton btnViewHistory;
    
    private boolean detailsExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        
        // جلب البيانات من Intent
        extractDataFromIntent();
        
        // تهيئة الـ Views
        initializeViews();
        
        // عرض البيانات
        displayErrorInfo();
        
        // إعداد الأزرار
        setupButtons();
    }
    
    /**
     * استخراج البيانات من Intent
     */
    private void extractDataFromIntent() {
        Intent intent = getIntent();
        errorMessage = intent.getStringExtra(EXTRA_ERROR);
        stackTrace = intent.getStringExtra(EXTRA_STACK);
        timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis());
        
        if (errorMessage == null) errorMessage = "خطأ غير معروف";
        if (stackTrace == null) stackTrace = "لا توجد تفاصيل متاحة";
        
        fullReport = buildFullReport();
    }
    
    /**
     * تهيئة الـ Views
     */
    private void initializeViews() {
        tvErrorMessage = findViewById(R.id.tv_error_message);
        tvStackTrace = findViewById(R.id.tv_stack_trace);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        tvDeviceInfo = findViewById(R.id.tv_device_info);
        cardDetails = findViewById(R.id.card_details);
        scrollDetails = findViewById(R.id.scroll_details);
        btnToggleDetails = findViewById(R.id.btn_toggle_details);
        btnRestart = findViewById(R.id.btn_restart);
        btnCopy = findViewById(R.id.btn_copy);
        btnEmail = findViewById(R.id.btn_send_email);
        btnWhatsApp = findViewById(R.id.btn_send_whatsapp);
        btnTelegram = findViewById(R.id.btn_send_telegram);
        btnViewHistory = findViewById(R.id.btn_view_history);
    }
    
    /**
     * عرض معلومات الخطأ
     */
    private void displayErrorInfo() {
        if (tvErrorMessage != null) {
            tvErrorMessage.setText(errorMessage);
        }
        
        if (tvStackTrace != null) {
            tvStackTrace.setText(stackTrace);
        }
        
        if (tvTimestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            tvTimestamp.setText("⏱️ " + sdf.format(new Date(timestamp)));
        }
        
        if (tvDeviceInfo != null) {
            tvDeviceInfo.setText(getDeviceInfo());
        }
        
        // إخفاء التفاصيل افتراضياً
        if (scrollDetails != null) {
            scrollDetails.setVisibility(View.GONE);
        }
    }
    
    /**
     * بناء التقرير الكامل
     */
    private String buildFullReport() {
        StringBuilder report = new StringBuilder();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        report.append("═══════════════════════════════════════════\n");
        report.append("⚠️ تقرير خطأ - POS System\n");
        report.append("═══════════════════════════════════════════\n\n");
        
        report.append("📅 التاريخ: ").append(sdf.format(new Date(timestamp))).append("\n\n");
        
        report.append("🔴 الخطأ:\n");
        report.append(errorMessage).append("\n\n");
        
        report.append("📱 معلومات الجهاز:\n");
        report.append(getDeviceInfo()).append("\n\n");
        
        report.append("📋 التفاصيل الكاملة:\n");
        report.append(stackTrace).append("\n");
        
        return report.toString();
    }
    
    /**
     * الحصول على معلومات الجهاز
     */
    private String getDeviceInfo() {
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager()
                .getPackageInfo(getPackageName(), 0);
            
            return String.format(Locale.getDefault(),
                "Android: %s (API %d)\n" +
                "الشركة: %s\n" +
                "الموديل: %s\n" +
                "التطبيق: v%s (%d)",
                android.os.Build.VERSION.RELEASE,
                android.os.Build.VERSION.SDK_INT,
                android.os.Build.MANUFACTURER,
                android.os.Build.MODEL,
                pInfo.versionName,
                pInfo.versionCode
            );
        } catch (Exception e) {
            return "لا يمكن الحصول على المعلومات";
        }
    }
    
    /**
     * إعداد الأزرار
     */
    private void setupButtons() {
        // زر إعادة التشغيل
        if (btnRestart != null) {
            btnRestart.setOnClickListener(v -> restartApp());
        }
        
        // زر إظهار/إخفاء التفاصيل
        if (btnToggleDetails != null) {
            btnToggleDetails.setOnClickListener(v -> toggleDetails());
        }
        
        // زر النسخ
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> copyToClipboard());
        }
        
        // زر الإيميل
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> sendEmail());
        }
        
        // زر واتساب
        if (btnWhatsApp != null) {
            btnWhatsApp.setOnClickListener(v -> sendWhatsApp());
        }
        
        // زر تليجرام
        if (btnTelegram != null) {
            btnTelegram.setOnClickListener(v -> sendTelegram());
        }
        
        // زر عرض السجل
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> showErrorHistory());
        }
    }
    
    /**
     * إعادة تشغيل التطبيق
     */
    private void restartApp() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                           Intent.FLAG_ACTIVITY_NEW_TASK |
                           Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            
            // قتل العملية للتأكد من إعادة التشغيل الكامل
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            showToast("فشلت إعادة التشغيل");
        }
    }
    
    /**
     * إظهار/إخفاء التفاصيل
     */
    private void toggleDetails() {
        detailsExpanded = !detailsExpanded;
        
        if (scrollDetails != null) {
            scrollDetails.setVisibility(detailsExpanded ? View.VISIBLE : View.GONE);
        }
        
        if (btnToggleDetails != null) {
            btnToggleDetails.setText(detailsExpanded ? 
                "إخفاء التفاصيل ▲" : "عرض التفاصيل ▼");
        }
    }
    
    /**
     * نسخ التقرير للحافظة
     */
    private void copyToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) 
                getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Error Report", fullReport);
            clipboard.setPrimaryClip(clip);
            
            showToast("✓ تم النسخ للحافظة");
        } catch (Exception e) {
            showToast("فشل النسخ");
        }
    }
    
    /**
     * إرسال عبر الإيميل
     */
    private void sendEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{DEV_EMAIL});
            intent.putExtra(Intent.EXTRA_SUBJECT, "⚠️ تقرير خطأ - POS System");
            intent.putExtra(Intent.EXTRA_TEXT, fullReport);
            
            startActivity(Intent.createChooser(intent, "إرسال عبر البريد"));
        } catch (Exception e) {
            showToast("لا يوجد تطبيق بريد");
        }
    }
    
    /**
     * إرسال عبر واتساب
     */
    private void sendWhatsApp() {
        if (DEV_WHATSAPP == null || DEV_WHATSAPP.isEmpty()) {
            showToast("رقم الدعم غير محدد");
            return;
        }
        try {
            String shortReport = fullReport.length() > 5000 ?
                fullReport.substring(0, 5000) + "\n\n[...تم اقتصاص التقرير]" :
                fullReport;
            String msg = Uri.encode(shortReport);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + DEV_WHATSAPP + "?text=" + msg));
            startActivity(intent);
        } catch (Exception e) {
            showToast("لا يوجد واتساب");
        }
    }
    
    /**
     * إرسال عبر تليجرام
     */
    private void sendTelegram() {
        if (DEV_TELEGRAM == null || DEV_TELEGRAM.isEmpty()) {
            showToast("معرف التليجرام غير محدد");
            return;
        }
        try {
            String msg = Uri.encode(fullReport.length() > 4000 ?
                fullReport.substring(0, 4000) + "\n\n[...اقتصاص]" :
                fullReport);
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://t.me/" + DEV_TELEGRAM + "?text=" + msg));
            startActivity(intent);
        } catch (Exception e) {
            showToast("لا يوجد تليجرام");
        }
    }
    
    /**
     * عرض سجل الأخطاء السابقة
     */
    private void showErrorHistory() {
        try {
            SharedPreferences prefs = getSharedPreferences("error_reports", MODE_PRIVATE);
            
            StringBuilder history = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            
            for (int i = 0; i < 5; i++) {
                String error = prefs.getString("error_" + i, null);
                long time = prefs.getLong("error_" + i + "_time", 0);
                
                if (error != null) {
                    history.append("═══════════════════════════════\n");
                    history.append("#").append(i + 1).append(" - ");
                    history.append(time > 0 ? sdf.format(new Date(time)) : "غير معروف");
                    history.append("\n───────────────────────────────\n");
                    
                    // استخراج السطر الأول من الخطأ
                    String[] lines = error.split("\n");
                    int linesToShow = Math.min(5, lines.length);
                    for (int j = 0; j < linesToShow; j++) {
                        history.append(lines[j]).append("\n");
                    }
                    if (lines.length > linesToShow) {
                        history.append("...\n");
                    }
                    history.append("\n");
                }
            }
            
            if (history.length() == 0) {
                history.append("لا توجد أخطاء محفوظة");
            }
            
            new MaterialAlertDialogBuilder(this)
                .setTitle("📋 سجل الأخطاء")
                .setMessage(history.toString())
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton("مسح السجل", (d, w) -> clearHistory())
                .show();
                
        } catch (Exception e) {
            showToast("فشل عرض السجل");
        }
    }
    
    /**
     * مسح سجل الأخطاء
     */
    private void clearHistory() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ تأكيد")
            .setMessage("هل تريد مسح جميع الأخطاء المحفوظة؟")
            .setPositiveButton(R.string.yes, (d, w) -> {
                GlobalExceptionHandler.clearErrorReports(this);
                showToast("✓ تم المسح");
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    @Override
    public void onBackPressed() {
        // منع الرجوع - يجب إعادة التشغيل
        new MaterialAlertDialogBuilder(this)
            .setTitle("تنبيه")
            .setMessage("حدث خطأ في التطبيق. يجب إعادة التشغيل.")
            .setPositiveButton("إعادة التشغيل", (d, w) -> restartApp())
            .setCancelable(false)
            .show();
    }
}
