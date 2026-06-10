package com.pos.system;

import com.pos.system.BaseActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import android.net.Uri;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityProductsBinding;

/**
 * ActivityProductsActivity - محسّنة
 *
 * ✅ الإصلاحات:
 * - استبدال startActivityForResult/onActivityResult بـ ActivityResultLauncher
 * - نقل النصوص المضمّنة إلى strings.xml
 * - استبدال e.printStackTrace() بـ Log.e()
 * - إصلاح try/catch فارغة في onBindViewHolder
 */
public class ActivityProductsActivity extends BaseActivity {

    private ActivityProductsBinding binding;


    private static final String TAG = "ProductsActivity";

    private RecyclerView                        recyclerProducts;
    private ProductsAdapter                     adapter;
    private DBHelper                            dbHelper;
    private ExtendedFloatingActionButton        fabAddProduct;
    private FloatingActionButton                fabImportCsv;
    private TextInputEditText                   etSearch;
    private View                                emptyState;
    private TextView                            tvProductsCount;

    private final List<HashMap<String, String>> productsList    = new ArrayList<>();
    private final List<HashMap<String, String>> allProductsList = new ArrayList<>();

    // ✅ SAF file picker for CSV import
    private final ActivityResultLauncher<String[]> csvPickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) importProductsFromCsv(uri);
            }
        );

    // ✅ ActivityResultLauncher بدلاً من startActivityForResult
    private final ActivityResultLauncher<Intent> addProductLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadProducts();
                }
            }
        );

    private final ActivityResultLauncher<Intent> editProductLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadProducts();
                }
            }
        );

    // ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFAB();
        setupSearch();
        loadProducts();
    }

    private void initViews() {
        recyclerProducts = binding.recyclerProducts;
        fabAddProduct    = binding.fabAddProduct;
        fabImportCsv     = binding.fabImportCsv;
        etSearch         = binding.etSearch;
        emptyState       = binding.emptyState;
        tvProductsCount  = binding.tvProductsCount;
    }

    private void setupToolbar() {
        View toolbar = binding.toolbar;
        if (toolbar != null) toolbar.setOnClickListener(v -> finish());
        if (binding.toolbar instanceof com.google.android.material.appbar.MaterialToolbar) {
            com.google.android.material.appbar.MaterialToolbar mt =
                (com.google.android.material.appbar.MaterialToolbar) binding.toolbar;
            mt.setNavigationOnClickListener(v -> finish());
            mt.inflateMenu(R.menu.menu_products);
            mt.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_edit_prices) {
                    startActivity(new Intent(this, ActivityEditPricesActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    private void setupRecyclerView() {
        recyclerProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductsAdapter();
        recyclerProducts.setAdapter(adapter);

        // سحب لليسار للحذف
        new ItemTouchHelper(new ItemSwipeHelper(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    deleteProduct(viewHolder.getAdapterPosition());
                }
            }
        }).attachToRecyclerView(recyclerProducts);
    }

    private void setupFAB() {
        fabAddProduct.setOnClickListener(v ->
            addProductLauncher.launch(new Intent(this, ActivityAddProductActivity.class))
        );
        if (binding.btnEmptyCta != null) {
            binding.btnEmptyCta.setOnClickListener(v ->
                addProductLauncher.launch(new Intent(this, ActivityAddProductActivity.class))
            );
        }
        if (fabImportCsv != null) {
            fabImportCsv.setOnClickListener(v -> showImportOptions());
        }
    }

    private void showImportOptions() {
        String[] opts = {
            getString(R.string.import_csv_pick),
            getString(R.string.import_csv_download_template)
        };
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.import_csv))
            .setItems(opts, (d, which) -> {
                if (which == 0) csvPickerLauncher.launch(new String[]{"text/csv", "text/plain", "*/*"});
                else            exportCsvTemplate();
            })
            .show();
    }

    private void exportCsvTemplate() {
        try {
            String header = "اسم المنتج,الكمية,سعر الشراء,سعر البيع,التصنيف,رقم الباركود,وصف المنتج\n";
            String example = "مثال منتج,10,50.00,75.00,إلكترونيات,1234567890,وصف اختياري\n";
            String fileName = "products_template.csv";
            java.io.File dir = new java.io.File(getExternalFilesDir(null), "exports");
            dir.mkdirs();
            java.io.File file = new java.io.File(dir, fileName);
            try (java.io.FileWriter fw = new java.io.FileWriter(file)) {
                fw.write('﻿'); // BOM for Excel Arabic support
                fw.write(header);
                fw.write(example);
            }
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);
            Intent share = new Intent(Intent.ACTION_VIEW);
            share.setDataAndType(uri, "text/csv");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.import_csv_download_template)));
        } catch (Exception e) {
            showSnackbar(getString(R.string.unknown_error), true);
        }
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterProducts(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    // ─────────────────────────────────────────────
    private void loadProducts() {
        productsList.clear();
        allProductsList.clear();
        try {
            allProductsList.addAll(dbHelper.getAllProductsList());
            productsList.addAll(allProductsList);
            updateUI();
        } catch (Exception e) {
            // ✅ Log.e بدل e.printStackTrace()
            Log.e(TAG, "loadProducts error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.operation_failed), true);
        }
    }

    private void filterProducts(String query) {
        productsList.clear();
        if (query.isEmpty()) {
            productsList.addAll(allProductsList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (HashMap<String, String> product : allProductsList) {
                String name     = product.get("name");
                String barcode  = product.get("barcode");
                String category = product.get("category");
                if ((name     != null && name.toLowerCase().contains(lowerQuery)) ||
                    (barcode  != null && barcode.toLowerCase().contains(lowerQuery)) ||
                    (category != null && category.toLowerCase().contains(lowerQuery))) {
                    productsList.add(product);
                }
            }
        }
        updateUI();
    }

    // ─────────────────────────────────────────────
    private void updateUI() {
        boolean isEmpty = productsList.isEmpty();
        recyclerProducts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (tvProductsCount != null) {
            tvProductsCount.setText(
                getString(R.string.products_count, productsList.size())
            );
        }
        if (!isEmpty) adapter.notifyDataSetChanged();
    }

    // ─────────────────────────────────────────────
    private void deleteProduct(int position) {
        if (position < 0 || position >= productsList.size()) return;

        HashMap<String, String> product = productsList.get(position);
        String barcode = product.get("barcode");
        String name    = product.get("name");

        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_product_confirm))
            .setMessage(getString(R.string.delete_product_message) + " \"" + name + "\"?")
            .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                if (dbHelper.deleteProduct(barcode)) {
                    allProductsList.remove(product);
                    productsList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateUI();
                    showSnackbar(getString(R.string.product_deleted), false);
                } else {
                    adapter.notifyItemChanged(position);
                    showSnackbar(getString(R.string.operation_failed), true);
                }
            })
            .setNegativeButton(getString(R.string.no),
                (dialog, which) -> adapter.notifyItemChanged(position))
            .show();
    }

    // ─────────────────────────────────────────────
    private void showProductDetails(HashMap<String, String> product) {
        // ✅ استخدام strings.xml بدل النصوص المضمّنة
        String details =
            getString(R.string.barcode)     + ": " + safeGet(product, "barcode")   + "\n" +
            getString(R.string.product_name)+ ": " + safeGet(product, "name")      + "\n" +
            getString(R.string.brand)       + ": " + safeGet(product, "brand")     + "\n" +
            getString(R.string.price)       + ": " + safeGet(product, "price")     + " " + getString(R.string.currency_egp) + "\n" +
            getString(R.string.cost)        + ": " + safeGet(product, "cost")      + " " + getString(R.string.currency_egp) + "\n" +
            getString(R.string.quantity)    + ": " + safeGet(product, "qty")       + "\n" +
            getString(R.string.category)    + ": " + safeGet(product, "category")  + "\n" +
            getString(R.string.location)    + ": " + safeGet(product, "location")  + "\n" +
            getString(R.string.supplier)    + ": " + safeGet(product, "supplier");

        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.product_details))
            .setMessage(details)
            .setPositiveButton("تفاصيل كاملة", (dialog, which) -> {
                Intent detailIntent = new Intent(this, ActivityItemDetailsActivity.class);
                detailIntent.putExtra("product_id", safeGet(product, "id"));
                startActivity(detailIntent);
            })
            .setNeutralButton(getString(R.string.edit), (dialog, which) -> editProduct(product))
            .setNegativeButton(getString(R.string.ok), null)
            .show();
    }

    private void editProduct(HashMap<String, String> product) {
        Intent intent = new Intent(this, ActivityAddProductActivity.class);
        intent.putExtra("barcode",   product.get("barcode"));
        intent.putExtra("name",      product.get("name"));
        intent.putExtra("brand",     product.get("brand"));
        intent.putExtra("price",     product.get("price"));
        intent.putExtra("cost",      product.get("cost"));
        intent.putExtra("qty",       product.get("qty"));
        intent.putExtra("category",  product.get("category"));
        intent.putExtra("location",  product.get("location"));
        intent.putExtra("supplier",  product.get("supplier"));
        intent.putExtra("edit_mode", true);
        editProductLauncher.launch(intent);
    }

    private void showProductOptions(HashMap<String, String> product) {
        String[] options = {
            getString(R.string.product_option_edit),
            getString(R.string.product_option_price_history)
        };
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(product.getOrDefault("name", ""))
            .setItems(options, (d, which) -> {
                if (which == 0) editProduct(product);
                else            showPriceHistory(product);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showPriceHistory(HashMap<String, String> product) {
        String id   = product.getOrDefault("id", "");
        String name = product.getOrDefault("name", "");
        java.util.List<HashMap<String, String>> history = dbHelper.getProductPriceHistory(id, 10);
        if (history.isEmpty()) {
            showSnackbar(getString(R.string.price_history_empty), false);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (HashMap<String, String> h : history) {
            String date   = h.getOrDefault("created_at", "");
            String price  = h.getOrDefault("price", "0");
            String qty    = h.getOrDefault("qty", "1");
            if (date.length() > 10) date = date.substring(0, 10);
            sb.append("• ").append(date).append("  —  ").append(price)
              .append(" × ").append(qty).append("\n");
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.price_history_title, name))
            .setMessage(sb.toString().trim())
            .setPositiveButton(R.string.ok, null)
            .show();
    }

    private String safeGet(HashMap<String, String> map, String key) {
        String v = map.get(key);
        return (v != null && !v.isEmpty()) ? v : "-";
    }

    // ─────────────────────────────────────────────
    // CSV Import (SAF)
    // Format A (legacy): barcode,name,cost,price,qty,category
    // Format B (template): اسم المنتج,الكمية,سعر الشراء,سعر البيع,التصنيف,رقم الباركود,وصف المنتج
    // ─────────────────────────────────────────────
    private void importProductsFromCsv(Uri uri) {
        showSnackbar(getString(R.string.importing), false);
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            int imported = 0, skipped = 0;
            try (InputStream is = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(
                     new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                boolean templateFormat = false; // true = name-first (Arabic template)
                while ((line = reader.readLine()) != null) {
                    line = line.trim().replaceAll("^﻿", ""); // strip BOM
                    if (line.isEmpty()) continue;
                    String[] cols = line.split(",", -1);
                    if (firstLine) {
                        firstLine = false;
                        // Detect format by first column header
                        String first = cols[0].trim();
                        templateFormat = first.contains("اسم") || first.contains("name");
                        boolean isHeader = templateFormat
                            || first.equalsIgnoreCase("barcode") || first.contains("باركود");
                        if (isHeader) continue; // skip header row
                    }
                    if (cols.length < 2) { skipped++; continue; }
                    String name, barcode, category, description;
                    double cost, price;
                    int    qty;
                    if (templateFormat) {
                        // Format B: name,qty,cost,price,category,barcode,description
                        name        = cols[0].trim();
                        qty         = cols.length > 1 ? csvParseInt(cols[1])    : 0;
                        cost        = cols.length > 2 ? csvParseDouble(cols[2]) : 0;
                        price       = cols.length > 3 ? csvParseDouble(cols[3]) : 0;
                        category    = cols.length > 4 ? cols[4].trim()          : "";
                        barcode     = cols.length > 5 ? cols[5].trim()          : "";
                        description = cols.length > 6 ? cols[6].trim()          : "";
                    } else {
                        // Format A: barcode,name,cost,price,qty,category
                        barcode     = cols[0].trim();
                        name        = cols[1].trim();
                        cost        = cols.length > 2 ? csvParseDouble(cols[2]) : 0;
                        price       = cols.length > 3 ? csvParseDouble(cols[3]) : 0;
                        qty         = cols.length > 4 ? csvParseInt(cols[4])    : 0;
                        category    = cols.length > 5 ? cols[5].trim()          : "";
                        description = "";
                    }
                    if (name.isEmpty()) { skipped++; continue; }
                    boolean ok = dbHelper.insertProduct(
                        barcode, name, "", "", cost, price, qty,
                        "", "", "", "", 5, category, description, "", "");
                    if (ok) imported++; else skipped++;
                }
            } catch (Exception e) {
                Log.e(TAG, "importProductsFromCsv error: " + e.getMessage(), e);
            }
            final int finalImported = imported, finalSkipped = skipped;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                loadProducts();
                new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.import_csv))
                    .setMessage(getString(R.string.import_results, finalImported, finalSkipped))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            });
        });
    }

    private double csvParseDouble(String s) {
        try { return s != null && !s.trim().isEmpty() ? Double.parseDouble(s.trim()) : 0; }
        catch (NumberFormatException e) { return 0; }
    }

    private int csvParseInt(String s) {
        try { return s != null && !s.trim().isEmpty() ? Integer.parseInt(s.trim()) : 0; }
        catch (NumberFormatException e) { return 0; }
    }

    // ─────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────
    private class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> product = productsList.get(position);

            holder.tvName.setText(safeGet(product, "name"));
            holder.tvBarcode.setText(safeGet(product, "barcode"));

            String brand = product.get("brand");
            if (brand != null && !brand.isEmpty()) {
                holder.tvBrand.setVisibility(View.VISIBLE);
                holder.tvBrand.setText(brand);
            } else {
                holder.tvBrand.setVisibility(View.GONE);
            }

            // ✅ إصلاح catch فارغة — استخدام Log
            try {
                double price = Double.parseDouble(safeGet(product, "price"));
                NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
                nf.setMaximumFractionDigits(2);
                holder.tvPrice.setText(nf.format(price) + " " + getString(R.string.currency_egp));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid price format: " + product.get("price"));
                holder.tvPrice.setText("0.00 " + getString(R.string.currency_egp));
            }

            try {
                int qty = Integer.parseInt(safeGet(product, "qty"));
                holder.tvQty.setText(getString(R.string.quantity) + ": " + qty);

                int reorderLevel = 5;
                try {
                    reorderLevel = Integer.parseInt(safeGet(product, "reorder_level"));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid reorder_level: " + product.get("reorder_level"));
                }

                if (qty == 0) {
                    holder.tvQty.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_error));
                } else if (qty <= reorderLevel) {
                    holder.tvQty.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_warning));
                } else {
                    holder.tvQty.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_success));
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid qty format: " + product.get("qty"));
                holder.tvQty.setText(getString(R.string.quantity) + ": 0");
            }

            holder.itemView.setOnClickListener(v -> showProductDetails(product));
            holder.itemView.setOnLongClickListener(v -> {
                showProductOptions(product);
                return true;
            });
        }

        @Override
        public int getItemCount() { return productsList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvBarcode, tvBrand, tvPrice, tvQty;
            ViewHolder(View view) {
                super(view);
                tvName    = view.findViewById(R.id.tv_name);
                tvBarcode = view.findViewById(R.id.tv_barcode);
                tvBrand   = view.findViewById(R.id.tv_brand);
                tvPrice   = view.findViewById(R.id.tv_price);
                tvQty     = view.findViewById(R.id.tv_qty);
            }
        }
    }

    // ─────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
