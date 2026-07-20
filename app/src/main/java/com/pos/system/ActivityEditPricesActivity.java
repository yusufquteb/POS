package com.pos.system;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityEditPricesBinding;

public class ActivityEditPricesActivity extends BaseActivity {

    private ActivityEditPricesBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPricesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}
        setupToolbar();
        setupCategorySpinner();
        setupPreviewListener();
        setupApplyButton();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategorySpinner() {
        List<String> categories = dbHelper.getAllProductCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(adapter);
        binding.spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { updatePreview(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    }

    private void setupPreviewListener() {
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePreview(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        if (binding.etAmount.getEditText() != null)
            binding.etAmount.getEditText().addTextChangedListener(watcher);
    }

    private void updatePreview() {
        String selectedCat = binding.spinnerCategory.getSelectedItem() != null
            ? binding.spinnerCategory.getSelectedItem().toString() : "الكل";
        int count = dbHelper.getProductCountByCategory("الكل".equals(selectedCat) ? "" : selectedCat);
        binding.tvPreview.setText("سيتم تعديل " + count + " منتج");
    }

    private void setupApplyButton() {
        binding.btnApply.setOnClickListener(v -> applyChanges());
    }

    private void applyChanges() {
        String amtStr = "";
        if (binding.etAmount != null && binding.etAmount.getEditText() != null)
            amtStr = binding.etAmount.getEditText().getText().toString().trim();
        if (amtStr.isEmpty()) {
            Snackbar.make(binding.getRoot(), "أدخل المبلغ أو النسبة", Snackbar.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) {
            Snackbar.make(binding.getRoot(), "مبلغ غير صحيح", Snackbar.LENGTH_SHORT).show();
            return;
        }

        boolean isSell = binding.rgPriceType.getCheckedRadioButtonId() == R.id.rb_sell_price;
        boolean isIncrease = binding.rgDirection.getCheckedRadioButtonId() == R.id.rb_increase;
        boolean isPercent = binding.rgAmountType.getCheckedRadioButtonId() == R.id.rb_percent;
        String category = binding.spinnerCategory.getSelectedItem() != null
            ? binding.spinnerCategory.getSelectedItem().toString() : "";
        if ("الكل".equals(category)) category = "";

        final boolean fSell = isSell, fInc = isIncrease, fPct = isPercent;
        final double fAmt = amount;
        final String fCat = category;

        String dir = isIncrease ? "زيادة" : "تخفيض";
        String pType = isPercent ? "%" : " " + currency;
        String priceType = isSell ? "سعر البيع" : "سعر الشراء";
        String catLabel = category.isEmpty() ? "كل المنتجات" : "تصنيف: " + category;

        new MaterialAlertDialogBuilder(this)
            .setTitle("تأكيد التعديل")
            .setMessage(dir + " " + priceType + " بـ " + amount + pType + " لـ " + catLabel)
            .setPositiveButton("تطبيق", (d, w) -> {
                int updated;
                if (fSell) updated = dbHelper.bulkUpdateSellPrice(fInc, fCat, fAmt, fPct);
                else       updated = dbHelper.bulkUpdateBuyCost(fInc, fCat, fAmt, fPct);
                Snackbar.make(binding.getRoot(),
                    "تم تعديل " + updated + " منتج", Snackbar.LENGTH_LONG).show();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }
}
