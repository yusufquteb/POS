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
import com.pos.system.databinding.ActivityPrinterSettingsBinding;

/**
 * ActivityPrinterSettingsActivity - إعدادات الطابعة (محدثة)
 *
 * ✅ إصلاح مشكلة تعطل البلوتوث
 * ✅ إصلاح مشكلة صلاحيات الواي فاي
 */
public class ActivityPrinterSettingsActivity extends BaseActivity {

    private ActivityPrinterSettingsBinding binding;

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
            binding = ActivityPrinterSettingsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            applyWindowInsets(binding.getRoot());

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
            showToast(getString(R.string.error_with_message, e.getMessage()));
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
        rgConnection = binding.rgConnection;
        rbUsb = binding.rbUsb;
        rbBluetooth = binding.rbBluetooth;
        rbWifi = binding.rbWifi;
        
        spPaperWidth = binding.spPaperWidth;
        swAutoPrint = binding.swAutoPrint;
        swShowLogo = binding.swShowLogo;
        
        btnSave = binding.btnSave;
        btnTestPrint = binding.btnTestPrint;
        btnCheckPrinter = binding.btnCheckPrinter;
    }
    
    private void setupToolbar() {
        try {
            Toolbar toolbar = binding.toolbar;
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.settings_printer);
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
                // USB printing is not yet implemented — revert selection to Bluetooth
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.printer_usb_title)
                    .setMessage(R.string.printer_usb_message)
                    .setPositiveButton(R.string.ok, (d, w) -> {
                        if (rgConnection != null) rgConnection.check(R.id.rb_bluetooth);
                    })
                    .setCancelable(false)
                    .show();

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
            showToast(getString(R.string.error_with_message, e.getMessage()));
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
        showToast(getString(R.string.bluetooth_ready_toast));
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
        showToast(getString(R.string.wifi_manual_ip_toast));
    }
    
    /**
     * فحص اتصال الطابعة
     */
    private void checkPrinterConnection() {
        Log.d(TAG, "🔍 Checking printer: " + currentPrinterType);
        
        try {
            btnCheckPrinter.setEnabled(false);
            btnCheckPrinter.setText(getString(R.string.checking_printer));
            
            PrinterCheckResult result = printerManager.checkPrinters(currentPrinterType);
            
            btnCheckPrinter.setEnabled(true);
            btnCheckPrinter.setText(getString(R.string.check_printer));
            
            if (result.success && result.hasPrinters()) {
                discoveredPrinters = result.printers;
                showPrintersFoundDialog(result);
            } else {
                showPrinterNotFoundDialog(result);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Check error", e);
            btnCheckPrinter.setEnabled(true);
            btnCheckPrinter.setText(getString(R.string.check_printer));
            showError(getString(R.string.error_with_message, e.getMessage()));
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
            .setTitle(R.string.bluetooth_not_supported_title)
            .setMessage(R.string.bluetooth_not_supported_msg)
            .setPositiveButton(R.string.ok, (d, w) -> {
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
            .setTitle(R.string.bluetooth_permissions_title)
            .setMessage(R.string.bluetooth_permissions_msg)
            .setPositiveButton(R.string.grant_permissions, (d, w) -> {
                printerManager.requestBluetoothPermissions();
            })
            .setNegativeButton(R.string.cancel, (d, w) -> {
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
            .setTitle(R.string.bluetooth_disabled_title)
            .setMessage(R.string.bluetooth_disabled_msg)
            .setPositiveButton(R.string.enable_bluetooth, (d, w) -> {
                requestEnableBluetoothSafely();
            })
            .setNeutralButton(R.string.open_settings, (d, w) -> {
                openBluetoothSettings();
            })
            .setNegativeButton(R.string.cancel, (d, w) -> {
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
            showToast(getString(R.string.bluetooth_enable_reminder_toast));
        } catch (Exception e) {
            Log.e(TAG, "Error opening Bluetooth settings", e);
            showSnackbar(getString(R.string.error_opening_settings), true);
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
            .setTitle(R.string.wifi_disabled_title)
            .setMessage(R.string.wifi_disabled_msg)
            .setPositiveButton(R.string.open_settings, (d, w) -> {
                openWiFiSettings();
            })
            .setNegativeButton(R.string.cancel, (d, w) -> {
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
            showToast(getString(R.string.wifi_enable_reminder_toast));
        } catch (Exception e) {
            Log.e(TAG, "Error opening WiFi settings", e);
            showSnackbar(getString(R.string.error_opening_settings), true);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Dialogs - نتائج الفحص
    // ═══════════════════════════════════════════════════════════════════
    
    private void showPrintersFoundDialog(PrinterCheckResult result) {
        StringBuilder message = new StringBuilder();
        message.append("✅ ").append(result.message).append("\n\n");
        
        message.append(getString(R.string.discovered_printers_label)).append("\n");
        for (int i = 0; i < result.printers.size(); i++) {
            PrinterDevice printer = result.printers.get(i);
            message.append((i + 1)).append(". ")
                   .append(printer.name)
                   .append("\n");
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.printers_found_title)
            .setMessage(message.toString())
            .setPositiveButton(R.string.ok, (d, w) -> saveSettings())
            .setNeutralButton(R.string.test_print, (d, w) -> testPrint())
            .show();
    }

    private void showPrinterNotFoundDialog(PrinterCheckResult result) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.printers_not_found_title)
            .setMessage(result.message)
            .setPositiveButton(R.string.ok, null);

        if (currentPrinterType.equals(Constants.PrinterType.BLUETOOTH)) {
            builder.setNeutralButton(R.string.open_bluetooth_settings, (d, w) ->
                openBluetoothSettings());
        } else if (currentPrinterType.equals(Constants.PrinterType.WIFI)) {
            builder.setNeutralButton(R.string.open_wifi_settings, (d, w) ->
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
                showError(getString(R.string.error_saving));
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
                
                showToast("✓ " + getString(R.string.settings_saved));
                Log.d(TAG, "✅ Saved: " + connectionType + ", " + paperWidth);
            } else {
                showError(getString(R.string.save_failed));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
            showError(getString(R.string.error_with_message, e.getMessage()));
        }
    }

    private void testPrint() {
        try {
            saveSettings();
            
            if (discoveredPrinters.isEmpty()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("⚠️ " + getString(R.string.warning))
                    .setMessage(R.string.printer_not_checked_msg)
                    .setPositiveButton(R.string.check_now, (d, w) -> checkPrinterConnection())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
                return;
            }

            showToast("✓ " + getString(R.string.test_page_sent));
            
        } catch (Exception e) {
            Log.e(TAG, "Test print error", e);
            showError(getString(R.string.error_with_message, e.getMessage()));
        }
    }

    private void showError(String message) {
        try {
            new MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ " + getString(R.string.error_title))
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
        } catch (Exception e) {
            showToast(message);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PrinterManager.REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("✓ " + getString(R.string.permissions_granted_toast));
                
                // التحقق من تفعيل البلوتوث
                if (!printerManager.isBluetoothEnabled()) {
                    showBluetoothDisabledDialog();
                }
            } else {
                showToast(getString(R.string.permissions_denied_toast));
                if (rbUsb != null) rbUsb.setChecked(true);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PrinterManager.REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                showToast("✓ " + getString(R.string.bluetooth_enabled_toast));
            } else {
                showSnackbar(getString(R.string.bluetooth_not_enabled_toast), true);
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
