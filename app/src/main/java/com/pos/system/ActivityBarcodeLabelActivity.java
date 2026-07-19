package com.pos.system;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.util.*;

/**
 * Barcode Label Printing — select products and print labels on Bluetooth thermal printer.
 * Each label: scannable barcode + product name + price.
 */
public class ActivityBarcodeLabelActivity extends BaseActivity {

    private DBHelper dbHelper;
    private LabelAdapter adapter;
    private final List<LabelItem> allItems  = new ArrayList<>();
    private final List<LabelItem> filteredItems = new ArrayList<>();
    private String currency = "ج.م";
    private MaterialButton btnPrint;
    private TextView tvSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_label);
        applyWindowInsets(findViewById(R.id.root_layout));

        dbHelper = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); }
        catch (Exception ignored) {}

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupPrintButton();
        loadProducts();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) { setSupportActionBar(toolbar); toolbar.setNavigationOnClickListener(v -> finish()); }
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rv_products);
        adapter = new LabelAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupSearch() {
        TextInputEditText etSearch = findViewById(R.id.et_search);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupPrintButton() {
        btnPrint  = findViewById(R.id.btn_print_labels);
        tvSelected = findViewById(R.id.tv_selected_count);
        if (btnPrint != null) {
            btnPrint.setOnClickListener(v -> printSelectedLabels());
        }
    }

    private void loadProducts() {
        new Thread(() -> {
            try {
                List<HashMap<String, String>> products = dbHelper.getAllProducts();
                allItems.clear();
                for (HashMap<String, String> p : products) {
                    String barcode = p.getOrDefault("barcode", "");
                    String name    = p.getOrDefault("name", "");
                    String price   = p.getOrDefault("price", "0");
                    if (!barcode.isEmpty() && !name.isEmpty()) {
                        allItems.add(new LabelItem(barcode, name, price));
                    }
                }
                runOnUiThread(() -> {
                    filteredItems.clear();
                    filteredItems.addAll(allItems);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                runOnUiThread(() -> showToast(getString(R.string.error_loading)));
            }
        }).start();
    }

    private void filter(String query) {
        filteredItems.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            String q = query.trim().toLowerCase();
            for (LabelItem item : allItems) {
                if (item.name.toLowerCase().contains(q) || item.barcode.contains(q)) {
                    filteredItems.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = 0;
        for (LabelItem item : allItems) { if (item.selected) count++; }
        if (tvSelected != null) {
            tvSelected.setText(getString(R.string.print_selected, count));
        }
    }

    private void printSelectedLabels() {
        List<LabelItem> selected = new ArrayList<>();
        for (LabelItem item : allItems) { if (item.selected) selected.add(item); }
        if (selected.isEmpty()) {
            showToast(getString(R.string.no_products_selected));
            return;
        }
        showToast(getString(R.string.printing_labels));
        new Thread(() -> {
            boolean success = false;
            try {
                HashMap<String, String> printerSettings = dbHelper.getPrinterSettings();
                String paperWidth = printerSettings.getOrDefault("paper_width", "80mm");
                int paperMm = "58mm".equals(paperWidth) ? 58 : 80;
                int barWidth = paperMm == 58 ? 300 : 500;

                com.dantsu.escposprinter.EscPosPrinter printer =
                    new com.dantsu.escposprinter.EscPosPrinter(
                        com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
                            .selectFirstPaired(),
                        203, paperMm, 32);

                for (LabelItem item : selected) {
                    for (int copy = 0; copy < item.copies; copy++) {
                        Bitmap barcodeBitmap = generateBarcode(item.barcode, barWidth, 80);
                        String imgHex = barcodeBitmap != null
                            ? com.dantsu.escposprinter.textparser.PrinterTextParserImg
                                .bitmapToHexadecimalString(printer, barcodeBitmap)
                            : null;

                        StringBuilder label = new StringBuilder();
                        if (imgHex != null) {
                            label.append("[C]<img>").append(imgHex).append("</img>\n");
                        }
                        label.append("[C]").append(item.barcode).append("\n");
                        label.append("[C]<b>").append(item.name).append("</b>\n");
                        double priceVal = 0;
                        try { priceVal = Double.parseDouble(item.price); } catch (Exception ignored) {}
                        label.append("[C]").append(String.format(Locale.US, "%.2f %s", priceVal, currency)).append("\n");
                        label.append("[C]--------------------------------\n");
                        printer.printFormattedText(label.toString());
                    }
                }
                success = true;
            } catch (Exception e) {
                android.util.Log.e("BarcodeLabel", "print error: " + e.getMessage(), e);
            }
            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    showToast(getString(R.string.print_success));
                } else {
                    showToast(getString(R.string.printer_not_connected));
                }
            });
        }).start();
    }

    private Bitmap generateBarcode(String content, int width, int height) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (Exception e) {
            android.util.Log.e("BarcodeLabel", "generateBarcode error: " + e.getMessage(), e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Data model
    // ─────────────────────────────────────────────────────────────────────

    static class LabelItem {
        String barcode, name, price;
        boolean selected = false;
        int copies = 1;
        LabelItem(String barcode, String name, String price) {
            this.barcode = barcode; this.name = name; this.price = price;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────────

    class LabelAdapter extends RecyclerView.Adapter<LabelAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            CheckBox cbSelect;
            TextView tvName, tvBarcode, tvPrice;
            ImageButton btnMinus, btnPlus;
            TextView tvCopies;

            VH(View v) {
                super(v);
                cbSelect  = v.findViewById(R.id.cb_select);
                tvName    = v.findViewById(R.id.tv_product_name);
                tvBarcode = v.findViewById(R.id.tv_barcode);
                tvPrice   = v.findViewById(R.id.tv_price);
                btnMinus  = v.findViewById(R.id.btn_minus);
                btnPlus   = v.findViewById(R.id.btn_plus);
                tvCopies  = v.findViewById(R.id.tv_copies);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_barcode_label, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= filteredItems.size()) return;
            LabelItem item = filteredItems.get(pos);
            h.tvName.setText(item.name);
            h.tvBarcode.setText(item.barcode);
            double p = 0;
            try { p = Double.parseDouble(item.price); } catch (Exception ignored) {}
            h.tvPrice.setText(String.format(Locale.US, "%.2f %s", p, currency));
            h.cbSelect.setChecked(item.selected);
            h.tvCopies.setText(String.valueOf(item.copies));

            h.cbSelect.setOnCheckedChangeListener((btn, checked) -> {
                item.selected = checked;
                updateSelectedCount();
            });
            h.btnMinus.setOnClickListener(v -> {
                if (item.copies > 1) { item.copies--; h.tvCopies.setText(String.valueOf(item.copies)); }
            });
            h.btnPlus.setOnClickListener(v -> {
                if (item.copies < 99) { item.copies++; h.tvCopies.setText(String.valueOf(item.copies)); }
            });
        }

        @Override
        public int getItemCount() { return filteredItems.size(); }
    }
}
