package com.pos.system;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.pos.system.databinding.ActivityInvoicesBinding;

/**
 * ActivityInvoicesActivity - صفحة الفواتير
 * @version 3.1 - Direct Cursor query (cache-safe)
 */
public class ActivityInvoicesActivity extends BaseActivity {

    private ActivityInvoicesBinding binding;


    private DBHelper dbHelper;
    private InvoicesAdapter adapter;
    private List<HashMap<String, String>> invoicesList    = new ArrayList<>();
    private List<HashMap<String, String>> allInvoicesList = new ArrayList<>();

    private RecyclerView recyclerView;
    private TextInputEditText etSearch;
    private MaterialButton btnScanBarcode;

    private final ActivityResultLauncher<Intent> barcodeScanLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    String barcode = BarcodeHelper.extractBarcode(result.getData());
                    if (barcode != null && !barcode.isEmpty() && etSearch != null) {
                        etSearch.setText(barcode);
                        filterInvoices(barcode);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoicesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        setupSearch();
        loadInvoices();
    }

    private void initViews() {
        recyclerView   = binding.recyclerInvoices;
        etSearch       = binding.etSearch;
        btnScanBarcode = binding.btnScanBarcode;

        if (recyclerView != null) {
            adapter = new InvoicesAdapter(invoicesList);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        if (btnScanBarcode != null) {
            btnScanBarcode.setOnClickListener(v -> openBarcodeScanner());
        }
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    filterInvoices(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void openBarcodeScanner() {
        BarcodeHelper.launch(this, barcodeScanLauncher);
    }

    /**
     * تحميل الفواتير مباشرة من قاعدة البيانات بدون الاعتماد على DBHelper.getAllInvoices()
     * هذا يضمن عمل الكود مع أي نسخة من DBHelper
     */
    private void loadInvoices() {
        invoicesList.clear();
        allInvoicesList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                "SELECT id, invoice_number, customer_name, total, created_at, payment_method " +
                "FROM invoices ORDER BY created_at DESC", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    HashMap<String, String> row = new HashMap<>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String colName = cursor.getColumnName(i);
                        String val = cursor.isNull(i) ? "" : cursor.getString(i);
                        row.put(colName, val != null ? val : "");
                    }
                    allInvoicesList.add(row);
                } while (cursor.moveToNext());
            }
            invoicesList.addAll(allInvoicesList);
            if (adapter != null) adapter.notifyDataSetChanged();

        } catch (Exception e) {
            android.util.Log.e("Invoices", "loadInvoices error: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void filterInvoices(String query) {
        invoicesList.clear();
        if (query == null || query.isEmpty()) {
            invoicesList.addAll(allInvoicesList);
        } else {
            String lower = query.toLowerCase();
            for (HashMap<String, String> inv : allInvoicesList) {
                String num  = inv.getOrDefault("invoice_number", "").toLowerCase();
                String cust = inv.getOrDefault("customer_name",  "").toLowerCase();
                if (num.contains(lower) || cust.contains(lower)) {
                    invoicesList.add(inv);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void openInvoiceDetails(HashMap<String, String> invoice) {
        try {
            long id = Long.parseLong(invoice.getOrDefault("id", "0"));
            Intent intent = new Intent(this, ActivityInvoiceDetailsActivity.class);
            intent.putExtra("invoice_id", id);
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("Invoices", "openInvoiceDetails: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvoices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ═══ Adapter ═══════════════════════════════════════════════
    private class InvoicesAdapter extends RecyclerView.Adapter<InvoicesAdapter.ViewHolder> {
        private final List<HashMap<String, String>> data;
        InvoicesAdapter(List<HashMap<String, String>> data) { this.data = data; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> invoice = data.get(position);
            holder.bind(invoice);
            holder.itemView.setOnClickListener(v -> openInvoiceDetails(invoice));
        }

        @Override public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber, tvCustomer, tvTotal, tvDate;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNumber   = itemView.findViewById(R.id.tv_invoice_number);
                tvCustomer = itemView.findViewById(R.id.tv_customer_name);
                tvTotal    = itemView.findViewById(R.id.tv_total);
                tvDate     = itemView.findViewById(R.id.tv_date);
            }
            void bind(HashMap<String, String> inv) {
                if (tvNumber   != null) tvNumber.setText(inv.getOrDefault("invoice_number", "-"));
                if (tvCustomer != null) tvCustomer.setText(inv.getOrDefault("customer_name", "عميل عام"));
                if (tvTotal    != null) tvTotal.setText(
                    String.format("%.2f", safeDouble(inv.getOrDefault("total", "0"))));
                if (tvDate     != null) tvDate.setText(inv.getOrDefault("created_at", ""));
            }
            private double safeDouble(String s) {
                try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
            }
        }
    }
}
