package com.pos.system;

import com.pos.system.BaseActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.google.zxing.ResultPoint;
import java.util.List;
import com.pos.system.databinding.ActivityBarcodeScannerBinding;

/**
 * شاشة مسح الباركود - متوافقة مع Sketchware
 */
public class ActivityBarcodeScannerActivity extends BaseActivity {

    private ActivityBarcodeScannerBinding binding;

    
    private static final int CAMERA_PERMISSION_CODE = 200;
    private DecoratedBarcodeView barcodeView;
    private ImageView btn_close, btn_flash;
    private MaterialButton btn_manual_input;
    private boolean isFlashOn = false;
    private boolean isScanning = false;

    // ✅ Store callback as member variable to prevent garbage collection
    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            // ✅ Safety checks to prevent crash
            if (!isScanning || isFinishing() || isDestroyed()) {
                return;
            }
            
            if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                isScanning = false;
                barcodeView.pause();
                returnResult(result.getText());
            }
        }
        
        // ✅ CRITICAL: Must implement this method
        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // Optional: You can leave this empty or handle result points
            // This prevents the NullPointerException
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBarcodeScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        initViews();
        setupListeners();
        
        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission();
        }
    }

    private void initViews() {
        barcodeView = binding.barcodeScanner;
        btn_close = binding.btnClose;
        btn_flash = binding.btnFlash;
        btn_manual_input = binding.btnManualInput;
    }

    private void setupListeners() {
        // زر الإغلاق
        btn_close.setOnClickListener(v -> finish());
        
        // زر الفلاش
        btn_flash.setOnClickListener(v -> toggleFlash());
        
        // زر الإدخال اليدوي
        btn_manual_input.setOnClickListener(v -> showManualInputDialog());
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
               == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.CAMERA}, 
            CAMERA_PERMISSION_CODE);
    }

    private void startScanning() {
        isScanning = true;
        barcodeView.decodeContinuous(callback); // ✅ Use member variable
    }

    private void toggleFlash() {
        if (isFlashOn) {
            barcodeView.setTorchOff();
            btn_flash.setImageResource(R.drawable.ic_flash_off);
            isFlashOn = false;
        } else {
            barcodeView.setTorchOn();
            btn_flash.setImageResource(R.drawable.ic_flash_on);
            isFlashOn = true;
        }
    }

    private void showManualInputDialog() {
        // ✅ Pause scanning while dialog is open
        if (barcodeView != null) {
            barcodeView.pause();
            isScanning = false;
        }
        
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("مثال: 6281234567890");
        input.setPadding(50, 30, 50, 30);
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(60, 20, 60, 20);
        container.addView(input);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("📱 إدخال الباركود يدوياً")
            .setMessage("أدخل رقم الباركود (13 رقم عادةً)")
            .setView(container)
            .setPositiveButton("✓ موافق", (d, w) -> {
                String barcode = input.getText().toString().trim();
                if (!barcode.isEmpty()) {
                    returnResult(barcode);
                } else {
                    showToast("⚠ الرجاء إدخال الباركود");
                    // Resume scanning if input was cancelled
                    if (!isFinishing()) {
                        isScanning = true;
                        barcodeView.resume();
                    }
                }
            })
            .setNegativeButton("✗ إلغاء", (d, w) -> {
                // Resume scanning if dialog cancelled
                if (!isFinishing()) {
                    isScanning = true;
                    barcodeView.resume();
                }
            })
            .setOnCancelListener(d -> {
                // Resume scanning if dialog cancelled
                if (!isFinishing()) {
                    isScanning = true;
                    barcodeView.resume();
                }
            })
            .show();
    }

    private void returnResult(String barcode) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SCAN_RESULT", barcode);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                showToast("⚠ يجب السماح باستخدام الكاميرا لمسح الباركود");
                showManualInputDialog();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null && isScanning) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isScanning = false;
        if (barcodeView != null) {
            barcodeView.pause();
            if (isFlashOn) {
                barcodeView.setTorchOff();
            }
        }
    }

    // ✅ IMPORTANT: Add onDestroy to clean up
    @Override
    protected void onDestroy() {
        isScanning = false;
        if (barcodeView != null) {
            barcodeView.pause();
        }
        super.onDestroy();
    }
}
