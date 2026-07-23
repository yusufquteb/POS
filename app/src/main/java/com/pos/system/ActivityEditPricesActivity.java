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
        String allLabel = getString(R.string.filter_all);
        String selectedCat = binding.spinnerCategory.getSelectedItem() != null
            ? binding.spinnerCategory.getSelectedItem().toString() : allLabel;
        int count = dbHelper.getProductCountByCategory(allLabel.equals(selectedCat) ? "" : selectedCat);
        binding.tvPreview.setText(getString(R.string.products_to_update_format, count));
    }

    private void setupApplyButton() {
        binding.btnApply.setOnClickListener(v -> applyChanges());
    }

    private void applyChanges() {
        String amtStr = "";
        if (binding.etAmount != null && binding.etAmount.getEditText() != null)
            amtStr = binding.etAmount.getEditText().getText().toString().trim();
        if (amtStr.isEmpty()) {
            Snackbar.make(binding.getRoot(), getString(R.string.please_enter_amount), Snackbar.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (NumberFormatException e) {
            Snackbar.make(binding.getRoot(), getString(R.string.invalid_amount), Snackbar.LENGTH_SHORT).show();
            return;
        }

        boolean isSell = binding.rgPriceType.getCheckedRadioButtonId() == R.id.rb_sell_price;
        boolean isIncrease = binding.rgDirection.getCheckedRadioButtonId() == R.id.rb_increase;
        boolean isPercent = binding.rgAmountType.getCheckedRadioButtonId() == R.id.rb_percent;
        String category = binding.spinnerCategory.getSelectedItem() != null
            ? binding.spinnerCategory.getSelectedItem().toString() : "";
        if (getString(R.string.filter_all).equals(category)) category = "";

        final boolean fSell = isSell, fInc = isIncrease, fPct = isPercent;
        final double fAmt = amount;
        final String fCat = category;

        String dir = getString(isIncrease ? R.string.price_direction_increase : R.string.price_direction_decrease);
        String pType = isPercent ? "%" : " " + currency;
        String priceType = getString(isSell ? R.string.price : R.string.cost);
        String catLabel = category.isEmpty() ? getString(R.string.all_products_label)
            : getString(R.string.category_label_format, category);

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_price_edit_title)
            .setMessage(getString(R.string.confirm_price_edit_message_format, dir, priceType, amount + pType, catLabel))
            .setPositiveButton(R.string.apply_btn, (d, w) -> {
                int updated;
                if (fSell) updated = dbHelper.bulkUpdateSellPrice(fInc, fCat, fAmt, fPct);
                else       updated = dbHelper.bulkUpdateBuyCost(fInc, fCat, fAmt, fPct);
                Snackbar.make(binding.getRoot(),
                    getString(R.string.products_updated_format, updated), Snackbar.LENGTH_LONG).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}
