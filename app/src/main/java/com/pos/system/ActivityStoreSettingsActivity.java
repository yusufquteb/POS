package com.pos.system;

import com.pos.system.BaseActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import com.pos.system.databinding.ActivityStoreSettingsBinding;

/**
 * ActivityStoreSettingsActivity - محسّنة
 *
 * ✅ الإصلاحات:
 * - استبدال startActivityForResult/onActivityResult بـ ActivityResultLauncher
 * - نقل النصوص المضمّنة إلى strings.xml
 * - تنظيف الدوال المكررة (compressImage / loadImage)
 * - إصلاح مشكلة الصورة الفاتحة مع ARGB_8888
 */
public class ActivityStoreSettingsActivity extends BaseActivity {

    private ActivityStoreSettingsBinding binding;


    private static final String TAG = "StoreSettingsActivity";

    private DBHelper          dbHelper;
    private TextInputEditText etName, etPhone, etAddress, etTaxNumber;
    private android.widget.AutoCompleteTextView spCountry;
    private ImageView         imgLogo;
    private String            selectedLogoPath = "";
    private String            selectedCountryCode = "EG";
    private Uri               photoUri;

    // ✅ ActivityResultLauncher للكاميرا
    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && photoUri != null) {
                    try {
                        Bitmap bitmap = loadBitmapFromUri(photoUri);
                        if (bitmap != null) handleBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Camera result error: " + e.getMessage(), e);
                        showSnackbar(getString(R.string.unknown_error), true);
                    }
                }
            }
        );

    // ✅ ActivityResultLauncher للمعرض
    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    try {
                        Uri imageUri = result.getData().getData();
                        Bitmap bitmap = loadBitmapFromUri(imageUri);
                        if (bitmap != null) handleBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Gallery result error: " + e.getMessage(), e);
                        showSnackbar(getString(R.string.unknown_error), true);
                    }
                }
            }
        );

    // ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoreSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        try {
            dbHelper = new DBHelper(this);
            initViews();
            loadSettings();
            setupClickListeners();
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.unknown_error), true);
        }
    }

    // ─────────────────────────────────────────────
    private void initViews() {
        etName      = binding.etStoreName;
        etPhone     = binding.etStorePhone;
        etAddress   = binding.etStoreAddress;
        etTaxNumber = binding.etTaxNumber;
        imgLogo     = binding.imgStoreLogo;
        spCountry   = binding.spCountry;
        setupCountryDropdown();
    }

    private void setupCountryDropdown() {
        if (spCountry == null) return;
        List<CountryConfig> countries = CountryConfig.all();
        List<String> labels = new java.util.ArrayList<>();
        for (CountryConfig c : countries) labels.add(c.displayName() + " — " + c.currency);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, labels);
        spCountry.setAdapter(adapter);
        spCountry.setOnItemClickListener((parent, view, position, id) -> {
            selectedCountryCode = countries.get(position).code;
            CountryConfig cfg = CountryConfig.forCode(selectedCountryCode);
            // Auto-fill tax rate from country
            if (etTaxNumber != null && cfg.vatRate > 0)
                etTaxNumber.setText(String.valueOf((int) cfg.vatRate));
            else if (etTaxNumber != null && cfg.vatRate == 0)
                etTaxNumber.setText("0");
        });
    }

    private void setupClickListeners() {
        View toolbar = binding.toolbar;
        if (toolbar != null) toolbar.setOnClickListener(v -> finish());

        View layoutLogo = binding.layoutSelectLogo;
        if (layoutLogo != null) layoutLogo.setOnClickListener(v -> showImagePickerDialog());

        View btnSave = binding.btnSaveSettings;
        if (btnSave != null) btnSave.setOnClickListener(v -> saveSettings());
    }

    // ─────────────────────────────────────────────
    private void loadSettings() {
        try {
            HashMap<String, String> settings = dbHelper.getStoreSettings();
            if (settings == null || settings.isEmpty()) return;

            setTextSafe(etName,      settings.get("name"));
            setTextSafe(etPhone,     settings.get("phone"));
            setTextSafe(etAddress,   settings.get("address"));
            String taxRate = settings.get("tax_rate");
            if (taxRate == null || taxRate.isEmpty()) taxRate = settings.get("tax");
            setTextSafe(etTaxNumber, taxRate);

            // Country picker
            String storedCode = settings.get("country_code");
            if (storedCode != null && !storedCode.isEmpty()) selectedCountryCode = storedCode;
            CountryConfig cfg = CountryConfig.forCode(selectedCountryCode);
            if (spCountry != null) spCountry.setText(cfg.displayName() + " — " + cfg.currency, false);

            String logo = settings.get("logo");
            if (logo != null && !logo.isEmpty()) {
                selectedLogoPath = logo;
                loadLogoFromPath(logo);
            }

            // QR toggle
            com.google.android.material.switchmaterial.SwitchMaterial swQr = binding.swQrOnInvoice;
            if (swQr != null) {
                swQr.setChecked("true".equalsIgnoreCase(settings.getOrDefault("qr_on_invoice", "false")));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadSettings error: " + e.getMessage(), e);
        }
    }

    private void setTextSafe(TextInputEditText et, String value) {
        if (et != null && value != null) et.setText(value);
    }

    // ─────────────────────────────────────────────
    /** تحميل صورة من مسار مع ARGB_8888 لتجنب الصورة الفاتحة */
    private void loadLogoFromPath(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            if (imgLogo != null && bmp != null) imgLogo.setImageBitmap(bmp);
        } catch (Exception e) {
            Log.e(TAG, "loadLogoFromPath error: " + e.getMessage(), e);
        }
    }

    /** تحميل Bitmap من Uri */
    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
        } catch (Exception e) {
            Log.e(TAG, "loadBitmapFromUri error: " + e.getMessage(), e);
            return null;
        }
    }

    /** معالجة الصورة المختارة — حفظها وعرضها */
    private void handleBitmap(Bitmap bitmap) {
        selectedLogoPath = saveLogoToInternalStorage(bitmap);
        if (!selectedLogoPath.isEmpty() && imgLogo != null) {
            imgLogo.setImageBitmap(bitmap);
            showSnackbar(getString(R.string.saved_successfully), false);
        } else {
            showSnackbar(getString(R.string.operation_failed), true);
        }
    }

    // ─────────────────────────────────────────────
    private void showImagePickerDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.choose_image_source))
            .setItems(new String[]{
                getString(R.string.camera),
                getString(R.string.gallery)
            }, (dialog, which) -> {
                if (which == 0) openCamera();
                else openGallery();
            })
            .show();
    }

    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) == null) {
                showSnackbar(getString(R.string.unknown_error), true);
                return;
            }
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(cameraIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "openCamera error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.unknown_error), true);
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "openGallery error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.unknown_error), true);
        }
    }

    private File createImageFile() {
        try {
            File dir = new File(getFilesDir(), "store_logo");
            if (!dir.exists()) dir.mkdirs();
            return File.createTempFile("LOGO_" + System.currentTimeMillis(), ".jpg", dir);
        } catch (Exception e) {
            Log.e(TAG, "createImageFile error: " + e.getMessage(), e);
            return null;
        }
    }

    // ─────────────────────────────────────────────
    private void saveSettings() {
        try {
            String name    = getTextSafe(etName);
            String phone   = getTextSafe(etPhone);
            String address = getTextSafe(etAddress);
            String tax     = getTextSafe(etTaxNumber);

            if (name.isEmpty()) {
                showSnackbar(getString(R.string.name_required), true);
                if (etName != null) etName.requestFocus();
                return;
            }

            boolean success = dbHelper.updateStoreSettings(
                name, phone, tax, selectedLogoPath, address);
            // Save country + currency
            CountryConfig cfg = CountryConfig.forCode(selectedCountryCode);
            dbHelper.saveStoreSetting("country_code", selectedCountryCode);
            dbHelper.saveStoreSetting("currency",     cfg.currency);
            dbHelper.saveStoreSetting("tax_rate",     tax);
            // Save QR toggle
            com.google.android.material.switchmaterial.SwitchMaterial swQr = binding.swQrOnInvoice;
            if (swQr != null) {
                dbHelper.saveStoreSetting("qr_on_invoice", swQr.isChecked() ? "true" : "false");
            }

            showSnackbar(
                success ? getString(R.string.store_updated) : getString(R.string.operation_failed),
                !success
            );
        } catch (Exception e) {
            Log.e(TAG, "saveSettings error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.unknown_error), true);
        }
    }

    private String getTextSafe(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    // ─────────────────────────────────────────────
    /** حفظ الصورة بجودة 95 لتجنب الصورة الفاتحة */
    private String saveLogoToInternalStorage(Bitmap bitmap) {
        try {
            File directory = new File(getFilesDir(), "store_logo");
            if (!directory.exists()) directory.mkdirs();

            File file = new File(directory, "logo_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "saveLogoToInternalStorage error: " + e.getMessage(), e);
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            try { dbHelper.close(); } catch (Exception ignored) {}
        }
    }
}
