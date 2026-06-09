package com.pos.system;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import com.pos.system.BarcodeHelper;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityAddProductBinding;

/**
 * ActivityAddProductActivity - صفحة إضافة وتعديل المنتجات
 *
 * ✅ الإصلاحات:
 * - إصلاح عدم ظهور المنتج فوراً بعد الإضافة (notifyDataSetChanged)
 * - إصلاح عدم نقل بيانات المنتج عند التعديل
 * - إضافة فتح الباركود (Barcode Scanner)
 * - تطبيق الثيم والألوان من BaseActivity
 * - رفع المحتوى فوق NavigationBar
 * - دعم وضع التعديل (EDIT MODE)
 *
 * @version 3.0
 */
public class ActivityAddProductActivity extends BaseActivity {

    private ActivityAddProductBinding binding;


    private static final String TAG = "AddProduct";

    // Extra keys للتعديل
    public static final String EXTRA_EDIT_MODE    = "edit_mode";
    public static final String EXTRA_PRODUCT_ID   = "product_id";
    public static final String EXTRA_PRODUCT_DATA = "product_data";

    // ═══ Views ═══
    private TextInputEditText etBarcode, etName, etBrand, etUnit,
                              etCost, etPrice, etQty, etExpiry,
                              etReorderLevel, etNotes, etBatchNumber, etSupplierRef;
    private AutoCompleteTextView etSupplier, spinnerLocation, spinnerCategory;
    private TextView tvProfit, tvProfitPercentage;
    private ImageView imgProduct;
    private TextInputLayout layoutBarcode, layoutName, layoutPrice, layoutQty;
    private FloatingActionButton fabCamera;
    private MaterialButton btnSave, btnSaveAndNew, btnScan;
    private ProgressBar progressBar;
    private RecyclerView recyclerProducts;
    private ProductsMiniAdapter productsAdapter;
    private List<HashMap<String, String>> productsList = new ArrayList<>();

    // ═══ Data ═══
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private String selectedImagePath = "";
    private Uri photoUri;
    private boolean isEditMode = false;
    private String editProductId = null;

    // ═══ Activity Result Launchers ═══

    /** فتح الباركود سكانر */
    private final ActivityResultLauncher<Intent> barcodeScanLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    String barcode = BarcodeHelper.extractBarcode(result.getData());
                    if (barcode != null && !barcode.isEmpty()) {
                        etBarcode.setText(barcode);
                        if (!isEditMode) checkBarcodeExists(barcode);
                    }
                }
            }
        );

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) handleImageSelection(imageUri);
                }
            }
        );

    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && photoUri != null) {
                    handleImageSelection(photoUri);
                }
            }
        );

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) openCamera();
                else showToast(getString(R.string.camera_permission_required));
            }
        );

    // ═══════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // رفع المحتوى فوق NavigationBar
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        try { HashMap<String, String> s = dbHelper.getStoreSettings(); if (s != null) currency = s.getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}

        // التحقق من وضع التعديل
        checkEditMode();

        initViews();
        setupToolbar();
        setupDropdowns();
        setupListeners();
        setupRecyclerView();
        loadProducts();

        // إذا كان وضع تعديل، حمّل بيانات المنتج
        if (isEditMode && editProductId != null) {
            loadProductData(editProductId);
        }
    }

    /** التحقق إن كان الـ Intent يحمل بيانات تعديل */
    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent == null) return;
        isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false);
        editProductId = intent.getStringExtra(EXTRA_PRODUCT_ID);
        if (editProductId == null || editProductId.isEmpty()) {
            isEditMode = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ═══════════════════════════════════════════════════════════
    // Init
    // ═══════════════════════════════════════════════════════════

    private void initViews() {
        etBarcode       = binding.etBarcode;
        etName          = binding.etName;
        etBrand         = binding.etBrand;
        etUnit          = binding.etUnit;
        etCost          = binding.etCost;
        etPrice         = binding.etPrice;
        etQty           = binding.etQty;
        etExpiry        = binding.etExpiry;
        etReorderLevel  = binding.etReorderLevel;
        etBatchNumber   = binding.etBatchNumber;
        etSupplierRef   = binding.etSupplierReference;
        etNotes         = binding.etNotes;

        etSupplier      = binding.etSupplier;
        spinnerLocation = binding.spinnerLocation;
        spinnerCategory = binding.spinnerCategory;

        tvProfit            = binding.tvProfit;
        tvProfitPercentage  = binding.tvProfitPercentage;
        imgProduct          = binding.imgProduct;

        layoutBarcode = binding.layoutBarcode;
        layoutName    = binding.layoutName;
        layoutPrice   = binding.layoutPrice;
        layoutQty     = binding.layoutQty;

        btnSave         = binding.btnSave;
        btnSaveAndNew   = binding.btnSaveAndNew;
        // ✅ إصلاح: البحث عن زر السكان بـ id الصحيح في الـ XML
        btnScan         = binding.btnScanBarcode;

        fabCamera    = binding.fabCamera;
        progressBar  = binding.progressBar;
        recyclerProducts = binding.recyclerProducts;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar1;
        if (toolbar == null) toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode
                ? getString(R.string.edit_product)
                : getString(R.string.add_product));
        }
    }

    private void setupDropdowns() {
        ArrayAdapter<String> supplierAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, loadSuppliersFromDB());
        if (etSupplier != null) etSupplier.setAdapter(supplierAdapter);

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, loadLocationsFromDB());
        if (spinnerLocation != null) spinnerLocation.setAdapter(locationAdapter);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, loadCategoriesFromDB());
        if (spinnerCategory != null) spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupListeners() {
        // ✅ زر السكان - فتح الباركود سكانر
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> openBarcodeScanner());
        }

        // زر الحفظ
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                hideKeyboard();
                if (validateForm()) {
                    if (isEditMode) {
                        updateProduct();
                    } else if (!FeatureGate.canAddProduct(this)) {
                        FeatureGate.showProductLimitDialog(this);
                    } else {
                        saveProduct(false);
                    }
                }
            });
        }

        // زر حفظ وإضافة جديد
        if (btnSaveAndNew != null) {
            btnSaveAndNew.setOnClickListener(v -> {
                hideKeyboard();
                if (validateForm()) {
                    if (!FeatureGate.canAddProduct(this)) {
                        FeatureGate.showProductLimitDialog(this);
                    } else {
                        saveProduct(true);
                    }
                }
            });
        }

        // الكاميرا
        if (fabCamera != null) {
            fabCamera.setOnClickListener(v -> showImageSourceDialog());
        }

        // تاريخ الانتهاء
        if (etExpiry != null) {
            etExpiry.setOnClickListener(v -> showDatePicker());
        }

        // حساب الربح
        TextWatcher profitWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { calculateProfit(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        if (etCost != null)  etCost.addTextChangedListener(profitWatcher);
        if (etPrice != null) etPrice.addTextChangedListener(profitWatcher);
    }

    private void setupRecyclerView() {
        if (recyclerProducts == null) return;
        productsAdapter = new ProductsMiniAdapter(this, productsList);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(this));
        recyclerProducts.setAdapter(productsAdapter);
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ فتح الباركود سكانر - ZXing المدمج عبر BarcodeHelper
    // ═══════════════════════════════════════════════════════════
    private void openBarcodeScanner() {
        BarcodeHelper.launch(this, barcodeScanLauncher);
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ تحميل بيانات المنتج للتعديل
    // ═══════════════════════════════════════════════════════════
    private void loadProductData(String productId) {
        try {
            HashMap<String, String> product = dbHelper.getProductById(productId);
            if (product == null) {
                showSnackbar("لم يتم العثور على المنتج", true);
                finish();
                return;
            }

            // ✅ تعبئة الحقول ببيانات المنتج
            setText(etBarcode,      product.get("barcode"));
            setText(etName,         product.get("name"));
            setText(etBrand,        product.get("brand"));
            setText(etUnit,         product.get("unit"));
            setText(etCost,         product.get("cost"));
            setText(etPrice,        product.get("price"));
            setText(etQty,          product.get("qty"));
            setText(etExpiry,       product.get("expiry"));
            setText(etReorderLevel, product.get("reorder_level"));
            setText(etNotes,        product.get("notes"));
            setText(etBatchNumber,  product.get("batch_number"));
            setText(etSupplierRef,  product.get("supplier_reference"));
            setText(etSupplier,     product.get("supplier"));
            setText(spinnerLocation,product.get("location"));
            setText(spinnerCategory,product.get("category"));

            // تحميل الصورة
            String imagePath = product.get("image_path");
            if (imagePath != null && !imagePath.isEmpty()) {
                selectedImagePath = imagePath;
                File imageFile = new File(imagePath);
                if (imageFile.exists() && imgProduct != null) {
                    Bitmap bmp = BitmapFactory.decodeFile(imagePath);
                    if (bmp != null) imgProduct.setImageBitmap(bmp);
                }
            }

            calculateProfit();

        } catch (Exception e) {
            showSnackbar("خطأ في تحميل بيانات المنتج", true);
        }
    }

    private void setText(TextView view, String text) {
        if (view != null && text != null) view.setText(text);
    }

    // ═══════════════════════════════════════════════════════════
    // Validation
    // ═══════════════════════════════════════════════════════════

    private boolean validateForm() {
        clearErrors();
        boolean isValid = true;

        String barcode = getText(etBarcode);
        String name    = getText(etName);
        String priceStr = getText(etPrice);

        if (barcode.isEmpty()) {
            if (layoutBarcode != null) layoutBarcode.setError(getString(R.string.required_field));
            isValid = false;
        }

        if (name.isEmpty()) {
            if (layoutName != null) layoutName.setError(getString(R.string.required_field));
            isValid = false;
        }

        double price = 0;
        try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
        if (price <= 0) {
            if (layoutPrice != null) layoutPrice.setError(getString(R.string.invalid_price));
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        if (layoutBarcode != null) layoutBarcode.setError(null);
        if (layoutName    != null) layoutName.setError(null);
        if (layoutPrice   != null) layoutPrice.setError(null);
        if (layoutQty     != null) layoutQty.setError(null);
    }

    private void checkBarcodeExists(String barcode) {
        try {
            if (dbHelper.isProductExists(barcode)) {
                if (layoutBarcode != null) layoutBarcode.setError(getString(R.string.barcode_exists));
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ حفظ المنتج (مع تحديث القائمة فوراً)
    // ═══════════════════════════════════════════════════════════

    private void saveProduct(boolean addNew) {
        showProgress(true);
        try {
            boolean result = dbHelper.insertProduct(
                getText(etBarcode),
                getText(etName),
                getText(etBrand),
                getText(etUnit),
                getDouble(etCost),
                getDouble(etPrice),
                getInt(etQty),
                getText(spinnerLocation),
                getText(etSupplier),
                getText(etExpiry),
                selectedImagePath,
                getInt(etReorderLevel, 5),
                getText(spinnerCategory),
                getText(etNotes),
                getText(etBatchNumber),
                getText(etSupplierRef)
            );

            if (result) {
                showToast(getString(R.string.product_added));
                clearForm();
                // ✅ تحديث القائمة فوراً بدون الحاجة للخروج والدخول
                loadProducts();
                if (!addNew) {
                    setResult(RESULT_OK);
                    finish();
                }
            } else {
                showToast(getString(R.string.operation_failed));
            }
        } catch (Exception e) {
            showToast(getString(R.string.operation_failed));
        } finally {
            showProgress(false);
        }
    }

    // ✅ تحديث المنتج عند التعديل
    private void updateProduct() {
        showProgress(true);
        try {
            boolean result = dbHelper.updateProduct(
                editProductId,
                getText(etBarcode),
                getText(etName),
                getText(etBrand),
                getText(etUnit),
                getDouble(etCost),
                getDouble(etPrice),
                getInt(etQty),
                getText(spinnerLocation),
                getText(etSupplier),
                getText(etExpiry),
                selectedImagePath,
                getInt(etReorderLevel, 5),
                getText(spinnerCategory),
                getText(etNotes),
                getText(etBatchNumber),
                getText(etSupplierRef)
            );

            if (result) {
                showToast(getString(R.string.product_updated));
                setResult(RESULT_OK);
                finish();
            } else {
                showToast(getString(R.string.operation_failed));
            }
        } catch (Exception e) {
            showToast(getString(R.string.operation_failed));
        } finally {
            showProgress(false);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Image Handling
    // ═══════════════════════════════════════════════════════════

    private void showImageSourceDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_image_source)
            .setItems(new String[]{
                getString(R.string.camera),
                getString(R.string.gallery)
            }, (dialog, which) -> {
                if (which == 0) checkCameraPermission();
                else openGallery();
            })
            .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(intent);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new java.util.Date());
            File storageDir = new File(getExternalFilesDir(null), "images");
            if (!storageDir.exists()) storageDir.mkdirs();
            return File.createTempFile("PRODUCT_" + timeStamp, ".jpg", storageDir);
        } catch (Exception e) {
            return null;
        }
    }

    private void handleImageSelection(Uri imageUri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(
                getContentResolver().openInputStream(imageUri));
            if (bitmap != null && imgProduct != null) {
                imgProduct.setImageBitmap(bitmap);
                selectedImagePath = saveImageToInternalStorage(bitmap);
            }
        } catch (Exception e) {
            showSnackbar("خطأ في تحميل الصورة", true);
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new java.util.Date());
            File directory = new File(getFilesDir(), "images");
            if (!directory.exists()) directory.mkdirs();
            File file = new File(directory, "product_" + timeStamp + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private void calculateProfit() {
        double cost  = getDouble(etCost);
        double price = getDouble(etPrice);
        if (cost > 0 && price > 0) {
            double profit    = price - cost;
            double profitPct = (profit / cost) * 100;
            if (tvProfit != null)
                tvProfit.setText(String.format(Locale.getDefault(), "%.2f %s", profit, currency));
            if (tvProfitPercentage != null)
                tvProfitPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", profitPct));
        } else {
            if (tvProfit != null)           tvProfit.setText("0.00 " + currency);
            if (tvProfitPercentage != null) tvProfitPercentage.setText("0.0%");
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this,
            (view, year, month, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);
                if (etExpiry != null) etExpiry.setText(date);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void clearForm() {
        if (etBarcode != null)      etBarcode.setText("");
        if (etName != null)         etName.setText("");
        if (etBrand != null)        etBrand.setText("");
        if (etUnit != null)         etUnit.setText("");
        if (etCost != null)         etCost.setText("");
        if (etPrice != null)        etPrice.setText("");
        if (etQty != null)          etQty.setText("");
        if (etExpiry != null)       etExpiry.setText("");
        if (etReorderLevel != null) etReorderLevel.setText("");
        if (etBatchNumber != null)  etBatchNumber.setText("");
        if (etSupplierRef != null)  etSupplierRef.setText("");
        if (etNotes != null)        etNotes.setText("");
        if (etSupplier != null)     etSupplier.setText("");
        if (spinnerLocation != null) spinnerLocation.setText("");
        if (spinnerCategory != null) spinnerCategory.setText("");
        if (imgProduct != null)     imgProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        selectedImagePath = "";
        clearErrors();
        if (etBarcode != null) etBarcode.requestFocus();
    }

    private void showProgress(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnSave != null) btnSave.setEnabled(!show);
    }

    private String getText(TextView view) {
        if (view == null) return "";
        String text = view.getText() == null ? "" : view.getText().toString().trim();
        return text;
    }

    private double getDouble(TextView view) {
        try { return Double.parseDouble(getText(view)); } catch (Exception e) { return 0.0; }
    }

    private int getInt(TextView view) {
        try { return Integer.parseInt(getText(view)); } catch (Exception e) { return 0; }
    }

    private int getInt(TextView view, int defaultVal) {
        try {
            String t = getText(view);
            return t.isEmpty() ? defaultVal : Integer.parseInt(t);
        } catch (Exception e) { return defaultVal; }
    }

    // ═══════════════════════════════════════════════════════════
    // Database Helpers
    // ═══════════════════════════════════════════════════════════

    private List<String> loadSuppliersFromDB() {
        List<String> list = new ArrayList<>();
        try {
            for (HashMap<String, String> row : dbHelper.getAllSuppliers()) {
                String name = row.get("name");
                if (name != null && !name.isEmpty()) list.add(name);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private List<String> loadLocationsFromDB() {
        List<String> list = new ArrayList<>();
        try {
            for (HashMap<String, String> row : dbHelper.getAllLocations()) {
                String name = row.get("name");
                if (name != null && !name.isEmpty()) list.add(name);
            }
        } catch (Exception ignored) {}
        return list;
    }

    private List<String> loadCategoriesFromDB() {
        List<String> list = new ArrayList<>();
        try {
            for (HashMap<String, String> row : dbHelper.getAllCategories()) {
                String name = row.get("name");
                if (name != null && !name.isEmpty()) list.add(name);
            }
        } catch (Exception ignored) {}
        return list;
    }

    /**
     * ✅ إصلاح: تحميل المنتجات وتحديث القائمة فوراً
     */
    private void loadProducts() {
        if (productsList == null) return;
        productsList.clear();
        try {
            List<HashMap<String, String>> list = dbHelper.getAllProducts();
            productsList.addAll(list);
        } catch (Exception ignored) {}
        if (productsAdapter != null) {
            productsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // ═══════════════════════════════════════════════════════════
    // ProductsMiniAdapter - عرض مصغر للمنتجات
    // ═══════════════════════════════════════════════════════════
    private class ProductsMiniAdapter extends RecyclerView.Adapter<ProductsMiniAdapter.VH> {

        private final android.content.Context ctx;
        private final List<HashMap<String, String>> data;

        ProductsMiniAdapter(android.content.Context ctx, List<HashMap<String, String>> data) {
            this.ctx  = ctx;
            this.data = data;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(ctx)
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            HashMap<String, String> p = data.get(position);
            if (holder.text1 != null) holder.text1.setText(p.getOrDefault("name", ""));
            if (holder.text2 != null) holder.text2.setText(
                "الكمية: " + p.getOrDefault("qty", "0") +
                " | السعر: " + p.getOrDefault("price", "0"));

            // ✅ الضغط على منتج لتعديله
            holder.itemView.setOnClickListener(v -> {
                String productId = p.get("id");
                if (productId != null) {
                    Intent intent = new Intent(ctx, ActivityAddProductActivity.class);
                    intent.putExtra(EXTRA_EDIT_MODE, true);
                    intent.putExtra(EXTRA_PRODUCT_ID, productId);
                    startActivityForResult(intent, 100);
                }
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView text1, text2;
            VH(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // ✅ تحديث القائمة بعد التعديل
            loadProducts();
        }
    }
}
