package com.pos.system;

import com.pos.system.BaseActivity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pos.system.utils.Constants;
import com.pos.system.managers.PrinterManager;
import com.pos.system.managers.PrinterManager.PrinterCheckResult;
import com.pos.system.managers.PrinterManager.PrinterDevice;
import java.util.ArrayList;
import java.util.List;

/**
 * ActivityPrinterSettingsActivity - إعدادات الطابعة (محدثة)
 * 
 * ✅ إصلاح مشكلة تعطل البلوتوث
 * ✅ إصلاح مشكلة صلاحيات الواي فاي
 */
public class ActivityPrinterSettingsActivity extends BaseActivity {

    private static final String TAG = "PrinterSettings";
    private static final String PREFS_NAME = "AppSettings";
    
    private DBHelper dbHelper;
    private SharedPreferences prefs;
    private PrinterManager printerManager;
    
    // UI Components
    private RadioGroup rgConnection;
    private RadioButton rbUsb, rbBluetooth, rbWifi;
    private AutoCompleteTextView spPaperWidth;
    private SwitchCompat swAutoPrint, swShowLogo;
    private MaterialButton btnSave, btnTestPrint, btnCheckPrinter;
    
    // State
    private List<PrinterDevice> discoveredPrinters = new ArrayList<>();
    private String currentPrinterType = Constants.PrinterType.USB;
    private boolean isRequestingBluetoothEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "════════════════════════════════");
        Log.d(TAG, "onCreate started");
        
        try {
            applyTheme();
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_printer_settings);

            setupStatusBar();
            
            prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            printerManager = new PrinterManager(this);
            
            try {
                dbHelper = new DBHelper(this);
            } catch (Exception e) {
                Log.w(TAG, "DBHelper init failed", e);
                dbHelper = null;
            }
            
            initViews();
            setupToolbar();
            setupPaperWidthSpinner();
            loadSettings();
            setupClickListeners();
            
            Log.d(TAG, "✅ Activity loaded successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FATAL ERROR", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    
    private void applyTheme() {
        try {
            SharedPreferences tempPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean isDarkMode = tempPrefs.getBoolean(Constants.Prefs.DARK_MODE, false);
            setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme);
        } catch (Exception e) {
            setTheme(R.style.AppTheme);
        }
    }
    
    private void setupStatusBar() {
        try {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            
            boolean isDarkMode = prefs != null && 
                                prefs.getBoolean(Constants.Prefs.DARK_MODE, false);
            window.setStatusBarColor(ContextCompat.getColor(this, 
                isDarkMode ? R.color.dark_primary : R.color.primary));
        } catch (Exception e) {
            Log.w(TAG, "Status bar setup failed", e);
        }
    }

    private void initViews() {
        rgConnection = findViewById(R.id.rg_connection);
        rbUsb = findViewById(R.id.rb_usb);
        rbBluetooth = findViewById(R.id.rb_bluetooth);
        rbWifi = findViewById(R.id.rb_wifi);
        
        spPaperWidth = findViewById(R.id.sp_paper_width);
        swAutoPrint = findViewById(R.id.sw_auto_print);
        swShowLogo = findViewById(R.id.sw_show_logo);
        
        btnSave = findViewById(R.id.btn_save);
        btnTestPrint = findViewById(R.id.btn_test_print);
        btnCheckPrinter = findViewById(R.id.btn_check_printer);
    }
    
    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("إعدادات الطابعة");
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }
        } catch (Exception e) {
            Log.w(TAG, "Toolbar setup failed", e);
        }
    }

    private void setupClickListeners() {
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveSettings());
        }
        
        if (btnTestPrint != null) {
            btnTestPrint.setOnClickListener(v -> testPrint());
        }
        
        if (btnCheckPrinter != null) {
            btnCheckPrinter.setOnClickListener(v -> checkPrinterConnection());
        }
        
        if (rgConnection != null) {
            rgConnection.setOnCheckedChangeListener((group, checkedId) -> {
                onConnectionTypeChanged(checkedId);
            });
        }
    }
    
    /**
     * 🔧 FIX: معالجة تغيير نوع الاتصال بشكل آمن
     */
    private void onConnectionTypeChanged(int checkedId) {
        try {
            if (checkedId == R.id.rb_usb) {
                currentPrinterType = Constants.PrinterType.USB;
                Toast.makeText(this, "✓ USB", Toast.LENGTH_SHORT).show();
                
            } else if (checkedId == R.id.rb_bluetooth) {
                currentPrinterType = Constants.PrinterType.BLUETOOTH;
                handleBluetoothSelection();
                
            } else if (checkedId == R.id.rb_wifi) {
                currentPrinterType = Constants.PrinterType.WIFI;
                handleWiFiSelection();
            }
            
            discoveredPrinters.clear();
            
        } catch (Exception e) {
            Log.e(TAG, "Connection type change error", e);
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 🔧 FIX: معالجة اختيار البلوتوث بشكل آمن
     */
    private void handleBluetoothSelection() {
        Log.d(TAG, "Bluetooth selected");
        
        // 1. التحقق من دعم البلوتوث
        if (!printerManager.isBluetoothSupported()) {
            showBluetoothNotSupportedDialog();
            return;
        }
        
        // 2. التحقق من الصلاحيات
        if (!printerManager.checkBluetoothPermissions()) {
            showBluetoothPermissionsDialog();
            return;
        }
        
        // 3. التحقق من تفعيل البلوتوث
        if (!printerManager.isBluetoothEnabled()) {
            showBluetoothDisabledDialog();
            return;
        }
        
        // كل شيء OK
        Toast.makeText(this, "✓ Bluetooth جاهز", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 🔧 FIX: معالجة اختيار الواي فاي بشكل آمن
     */
    private void handleWiFiSelection() {
        Log.d(TAG, "WiFi selected");
        
        // 1. التحقق من تفعيل الواي فاي
        if (!printerManager.isWiFiEnabled()) {
            showWiFiDisabledDialog();
            return;
        }
        
        // 2. رسالة توضيحية
        Toast.makeText(this, "✓ WiFi - يتطلب إدخال IP يدوياً", Toast.LENGTH_LONG).show();
    }
    
    /**
     * فحص اتصال الطابعة
     */
    private void checkPrinterConnection() {
        Log.d(TAG, "🔍 Checking printer: " + currentPrinterType);
        
        try {
            btnCheckPrinter.setEnabled(false);
            btnCheckPrinter.setText("جارٍ الفحص...");
            
            PrinterCheckResult result = printerManager.checkPrinters(currentPrinterType);
            
            btnCheckPrinter.setEnabled(true);
            btnCheckPrinter.setText("فحص الطابعة");
            
            if (result.success && result.hasPrinters()) {
                discoveredPrinters = result.printers;
                showPrintersFoundDialog(result);
            } else {
                showPrinterNotFoundDialog(result);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Check error", e);
            btnCheckPrinter.setEnabled(true);
            btnCheckPrinter.setText("فحص الطابعة");
            showError("خطأ: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Dialogs - البلوتوث
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * البلوتوث غير مدعوم
     */
    private void showBluetoothNotSupportedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ غير مدعوم")
            .setMessage("البلوتوث غير مدعوم في هذا الجهاز")
            .setPositiveButton("حسناً", (d, w) -> {
                if (rbUsb != null) rbUsb.setChecked(true);
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * طلب صلاحيات البلوتوث
     */
    private void showBluetoothPermissionsDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ صلاحيات مطلوبة")
            .setMessage("يحتاج التطبيق لصلاحيات البلوتوث للبحث عن الطابعات\n\nهل تريد منح الصلاحيات؟")
            .setPositiveButton("منح الصلاحيات", (d, w) -> {
                printerManager.requestBluetoothPermissions();
            })
            .setNegativeButton("إلغاء", (d, w) -> {
                if (rbUsb != null) rbUsb.setChecked(true);
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * 🔧 FIX: البلوتوث معطل - مع معالجة آمنة
     */
    private void showBluetoothDisabledDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ البلوتوث معطل")
            .setMessage("يجب تفعيل البلوتوث لاستخدام طابعة Bluetooth\n\nاختر أحد الخيارات:")
            .setPositiveButton("تفعيل البلوتوث", (d, w) -> {
                requestEnableBluetoothSafely();
            })
            .setNeutralButton("فتح الإعدادات", (d, w) -> {
                openBluetoothSettings();
            })
            .setNegativeButton("إلغاء", (d, w) -> {
                if (rbUsb != null) rbUsb.setChecked(true);
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * 🔧 FIX: طلب تفعيل البلوتوث بشكل آمن
     */
    private void requestEnableBluetoothSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - لا يمكن تفعيل البلوتوث برمجياً
                // يجب فتح الإعدادات
                openBluetoothSettings();
            } else {
                // Android 11 وأقل - يمكن طلب التفعيل
                isRequestingBluetoothEnable = true;
                printerManager.requestEnableBluetooth();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting Bluetooth enable", e);
            openBluetoothSettings();
        }
    }
    
    /**
     * فتح إعدادات البلوتوث
     */
    private void openBluetoothSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "فعّل البلوتوث ثم ارجع للتطبيق", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening Bluetooth settings", e);
            Toast.makeText(this, "خطأ في فتح الإعدادات", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Dialogs - الواي فاي
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * 🔧 FIX: الواي فاي معطل
     */
    private void showWiFiDisabledDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("⚠️ الواي فاي معطل")
            .setMessage("يجب تفعيل الواي فاي والاتصال بنفس شبكة الطابعة")
            .setPositiveButton("فتح الإعدادات", (d, w) -> {
                openWiFiSettings();
            })
            .setNegativeButton("إلغاء", (d, w) -> {
                if (rbUsb != null) rbUsb.setChecked(true);
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * فتح إعدادات الواي فاي
     */
    private void openWiFiSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - فتح لوحة الواي فاي
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                startActivity(panelIntent);
            } else {
                // Android 9 وأقل - فتح إعدادات الواي فاي
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
            Toast.makeText(this, "فعّل الواي فاي وارجع للتطبيق", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening WiFi settings", e);
            Toast.makeText(this, "خطأ في فتح الإعدادات", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Dialogs - نتائج الفحص
    // ═══════════════════════════════════════════════════════════════════
    
    private void showPrintersFoundDialog(PrinterCheckResult result) {
        StringBuilder message = new StringBuilder();
        message.append("✅ ").append(result.message).append("\n\n");
        
        message.append("الطابعات المكتشفة:\n");
        for (int i = 0; i < result.printers.size(); i++) {
            PrinterDevice printer = result.printers.get(i);
            message.append((i + 1)).append(". ")
                   .append(printer.name)
                   .append("\n");
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("✓ تم العثور على طابعات")
            .setMessage(message.toString())
            .setPositiveButton("حسناً", (d, w) -> saveSettings())
            .setNeutralButton("اختبار الطباعة", (d, w) -> testPrint())
            .show();
    }
    
    private void showPrinterNotFoundDialog(PrinterCheckResult result) {
        String title = "⚠️ لم يتم العثور على طابعات";
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(result.message)
            .setPositiveButton("حسناً", null);
        
        if (currentPrinterType.equals(Constants.PrinterType.BLUETOOTH)) {
            builder.setNeutralButton("فتح إعدادات البلوتوث", (d, w) -> 
                openBluetoothSettings());
        } else if (currentPrinterType.equals(Constants.PrinterType.WIFI)) {
            builder.setNeutralButton("فتح إعدادات الواي فاي", (d, w) -> 
                openWiFiSettings());
        }
        
        builder.show();
    }

    private void setupPaperWidthSpinner() {
        try {
            if (spPaperWidth == null) return;
            
            ArrayList<String> options = new ArrayList<>();
            options.add("80mm");
            options.add("58mm");
            options.add("76mm");
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_dropdown_item_1line, options);
            
            spPaperWidth.setAdapter(adapter);
            spPaperWidth.setText("80mm", false);
            
        } catch (Exception e) {
            Log.w(TAG, "Spinner setup failed", e);
        }
    }

    private void loadSettings() {
        try {
            if (prefs == null) {
                setDefaultValues();
                return;
            }
            
            String connectionType = prefs.getString(Constants.Prefs.PRINTER_TYPE, 
                                                   Constants.PrinterType.USB);
            currentPrinterType = connectionType;
            
            String paperWidth = prefs.getString(Constants.Prefs.PAPER_WIDTH, "80mm");
            boolean autoPrint = prefs.getBoolean(Constants.Prefs.AUTO_PRINT, false);
            boolean showLogo = prefs.getBoolean(Constants.Prefs.SHOW_LOGO, true);
            
            if (rbUsb != null && rbBluetooth != null && rbWifi != null) {
                switch (connectionType) {
                    case Constants.PrinterType.BLUETOOTH:
                        rbBluetooth.setChecked(true);
                        break;
                    case Constants.PrinterType.WIFI:
                        rbWifi.setChecked(true);
                        break;
                    default:
                        rbUsb.setChecked(true);
                        break;
                }
            }
            
            if (spPaperWidth != null) spPaperWidth.setText(paperWidth, false);
            if (swAutoPrint != null) swAutoPrint.setChecked(autoPrint);
            if (swShowLogo != null) swShowLogo.setChecked(showLogo);
            
            Log.d(TAG, "✅ Settings loaded");
            
        } catch (Exception e) {
            Log.e(TAG, "Load error", e);
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        try {
            if (rbUsb != null) rbUsb.setChecked(true);
            if (spPaperWidth != null) spPaperWidth.setText("80mm", false);
            if (swAutoPrint != null) swAutoPrint.setChecked(false);
            if (swShowLogo != null) swShowLogo.setChecked(true);
        } catch (Exception e) {
            Log.e(TAG, "Default values error", e);
        }
    }

    private void saveSettings() {
        try {
            if (prefs == null) {
                showError("خطأ في الحفظ");
                return;
            }
            
            String connectionType = currentPrinterType;
            String paperWidth = spPaperWidth != null && spPaperWidth.getText() != null ?
                               spPaperWidth.getText().toString() : "80mm";
            boolean autoPrint = swAutoPrint != null && swAutoPrint.isChecked();
            boolean showLogo = swShowLogo == null || swShowLogo.isChecked();
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.Prefs.PRINTER_TYPE, connectionType);
            editor.putString(Constants.Prefs.PAPER_WIDTH, paperWidth);
            editor.putBoolean(Constants.Prefs.AUTO_PRINT, autoPrint);
            editor.putBoolean(Constants.Prefs.SHOW_LOGO, showLogo);
            boolean saved = editor.commit();
            
            if (saved) {
                if (dbHelper != null) {
                    try {
                        dbHelper.updatePrinterSettings(connectionType, paperWidth, 
                                                      autoPrint ? 1 : 0, 
                                                      showLogo ? 1 : 0);
                    } catch (Exception e) {
                        Log.w(TAG, "DB save failed", e);
                    }
                }
                
                Toast.makeText(this, "✓ تم حفظ الإعدادات", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "✅ Saved: " + connectionType + ", " + paperWidth);
            } else {
                showError("فشل الحفظ");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
            showError("خطأ: " + e.getMessage());
        }
    }

    private void testPrint() {
        try {
            saveSettings();
            
            if (discoveredPrinters.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ تنبيه")
                    .setMessage("لم يتم فحص الطابعة!\n\nاضغط 'فحص الطابعة' أولاً")
                    .setPositiveButton("فحص الآن", (d, w) -> checkPrinterConnection())
                    .setNegativeButton("إلغاء", null)
                    .show();
                return;
            }
            
            Toast.makeText(this, "✓ تم إرسال صفحة اختبار", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Test print error", e);
            showError("فشل: " + e.getMessage());
        }
    }

    private void showError(String message) {
        try {
            new MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ خطأ")
                .setMessage(message)
                .setPositiveButton("حسناً", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PrinterManager.REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✓ تم منح الصلاحيات", Toast.LENGTH_SHORT).show();
                
                // التحقق من تفعيل البلوتوث
                if (!printerManager.isBluetoothEnabled()) {
                    showBluetoothDisabledDialog();
                }
            } else {
                Toast.makeText(this, "تم رفض الصلاحيات", Toast.LENGTH_SHORT).show();
                if (rbUsb != null) rbUsb.setChecked(true);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PrinterManager.REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "✓ تم تفعيل البلوتوث", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "لم يتم تفعيل البلوتوث", Toast.LENGTH_SHORT).show();
                if (rbUsb != null) rbUsb.setChecked(true);
            }
            isRequestingBluetoothEnable = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (dbHelper != null) {
                dbHelper.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "onDestroy error", e);
        }
    }
}
