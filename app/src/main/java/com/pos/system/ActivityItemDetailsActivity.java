package com.pos.system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityItemDetailsBinding;

public class ActivityItemDetailsActivity extends BaseActivity {

    private ActivityItemDetailsBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private String productId;
    private HashMap<String, String> product;
    private List<HashMap<String, String>> salesHistory = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        productId = getIntent().getStringExtra("product_id");

        dbHelper = new DBHelper(this);
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            if (s != null) currency = s.getOrDefault("currency", "ج.م");
        } catch (Exception ignored) {}

        setupToolbar();
        setupRecycler();
        setupButtons();
        loadData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        binding.recyclerHistory.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnEditPrice.setOnClickListener(v -> showEditPriceDialog());
        binding.btnAddQty.setOnClickListener(v -> showAddQtyDialog());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadData() {
        if (productId == null) { finish(); return; }
        product = dbHelper.getProductById(productId);
        if (product == null) { finish(); return; }

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(product.getOrDefault("name", ""));

        binding.tvProductName.setText(product.getOrDefault("name", ""));
        binding.tvBarcode.setText(getString(R.string.barcode_label_format, product.getOrDefault("barcode", "")));
        binding.tvSellPrice.setText(String.format(Locale.US, "%.2f", safeDouble(product, "price")));
        binding.tvBuyPrice.setText(String.format(Locale.US, "%.2f", safeDouble(product, "cost")));
        binding.tvQty.setText(product.getOrDefault("qty", "0"));
        binding.tvCategory.setText(getString(R.string.category_label_format, product.getOrDefault("category", "-")));

        // Load sales history for this product
        salesHistory.clear();
        try {
            List<HashMap<String, String>> items = dbHelper.getInvoiceItemsByProduct(productId);
            salesHistory.addAll(items);
        } catch (Exception ignored) {}
        adapter.notifyDataSetChanged();
    }

    private double safeDouble(HashMap<String, String> m, String key) {
        try { return Double.parseDouble(m.getOrDefault(key, "0")); } catch (Exception e) { return 0; }
    }

    private void showEditPriceDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextInputEditText etInput = v.findViewById(R.id.et_input);
        if (etInput != null) {
            etInput.setHint(R.string.new_sell_price_hint);
            etInput.setText(product != null ? product.getOrDefault("price", "0") : "0");
        }
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_sell_price_title)
            .setView(v)
            .setPositiveButton(R.string.save, (d, w) -> {
                try {
                    String s = etInput != null && etInput.getText() != null
                        ? etInput.getText().toString().trim() : "";
                    if (s.isEmpty()) return;
                    double newPrice = Double.parseDouble(s);
                    dbHelper.updateProductPrice(productId, newPrice);
                    loadData();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showAddQtyDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_quantity, null);
        TextInputEditText etQty = v.findViewById(R.id.et_quantity);
        if (etQty != null) { etQty.setHint(R.string.added_quantity_hint); etQty.setText("1"); }
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_quantity_title)
            .setView(v)
            .setPositiveButton(R.string.add_btn, (d, w) -> {
                try {
                    String s = etQty != null && etQty.getText() != null
                        ? etQty.getText().toString().trim() : "";
                    if (s.isEmpty()) return;
                    int qty = Integer.parseInt(s);
                    dbHelper.addProductQuantity(productId, qty);
                    loadData();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_product_message, product != null ? product.getOrDefault("name","") : ""))
            .setPositiveButton(R.string.delete, (d, w) -> {
                if (productId != null) {
                    dbHelper.deleteProduct(productId);
                    finish();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvRef, tvQty, tvPrice, tvDate;
            VH(View v) { super(v);
                tvRef  = v.findViewById(R.id.tv_ref);
                tvQty  = v.findViewById(R.id.tv_qty);
                tvPrice = v.findViewById(R.id.tv_price);
                tvDate  = v.findViewById(R.id.tv_date);
            }
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Reuse item_account_transaction or create a simple one
            View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_transaction, parent, false);
            return new VH(itemView);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= salesHistory.size()) return;
            HashMap<String, String> item = salesHistory.get(pos);
            if (h.tvRef  != null) h.tvRef.setText(getString(R.string.invoice_ref_format, item.getOrDefault("invoice_id","")));
            if (h.tvDate != null) h.tvDate.setText(item.getOrDefault("created_at",""));
            // Use tvNote for qty info
            TextView tvNote = h.itemView.findViewById(R.id.tv_note);
            if (tvNote != null) tvNote.setText(getString(R.string.quantity_label_format, item.getOrDefault("qty","0")));
            // Use tvAmount for total
            TextView tvAmount = h.itemView.findViewById(R.id.tv_amount);
            if (tvAmount != null) {
                double total = 0;
                try { total = Double.parseDouble(item.getOrDefault("total","0")); } catch (Exception ignored) {}
                tvAmount.setText(String.format(Locale.US, "%.2f %s", total, currency));
            }
        }
        @Override public int getItemCount() { return salesHistory.size(); }
    }
}
