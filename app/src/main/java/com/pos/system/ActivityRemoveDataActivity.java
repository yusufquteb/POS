package com.pos.system;

import android.os.Bundle;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.pos.system.databinding.ActivityRemoveDataBinding;

public class ActivityRemoveDataActivity extends BaseActivity {

    private ActivityRemoveDataBinding binding;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRemoveDataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        setupToolbar();
        setupButtons();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtons() {
        binding.btnDeleteInvoices.setOnClickListener(v ->
            confirmDelete("حذف جميع الفواتير", "هل تريد حذف جميع الفواتير؟ لا يمكن التراجع.",
                () -> { dbHelper.getWritableDatabase().delete("invoices", null, null);
                        dbHelper.getWritableDatabase().delete("invoice_items", null, null); }));
        binding.btnDeleteExpenses.setOnClickListener(v ->
            confirmDelete("حذف المصروفات", "هل تريد حذف جميع المصروفات؟",
                () -> dbHelper.getWritableDatabase().delete("expenses", null, null)));
        binding.btnDeleteCustomers.setOnClickListener(v ->
            confirmDelete("حذف العملاء", "هل تريد حذف جميع العملاء؟",
                () -> dbHelper.getWritableDatabase().delete("customers", null, null)));
        binding.btnDeleteSuppliers.setOnClickListener(v ->
            confirmDelete("حذف الموردين", "هل تريد حذف جميع الموردين؟",
                () -> dbHelper.getWritableDatabase().delete("suppliers", null, null)));
        binding.btnDeleteProducts.setOnClickListener(v ->
            confirmDelete("حذف المنتجات", "هل تريد حذف جميع المنتجات؟",
                () -> dbHelper.getWritableDatabase().delete("products", null, null)));
        binding.btnDeleteAll.setOnClickListener(v ->
            confirmDelete("حذف كل البيانات", "سيتم حذف جميع البيانات بالكامل. هل أنت متأكد؟",
                () -> deleteAllData()));
    }

    private void confirmDelete(String title, String message, Runnable action) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("تأكيد الحذف", (d, w) -> {
                try {
                    action.run();
                    Snackbar.make(binding.getRoot(), "تم الحذف", Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Snackbar.make(binding.getRoot(), "خطأ: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void deleteAllData() {
        String[] tables = {"invoice_items","invoices","purchase_order_items","purchase_orders",
            "expenses","direct_payments","wallet_transactions","price_quote_items","price_quotes",
            "customers","suppliers","products"};
        for (String t : tables) {
            try { dbHelper.getWritableDatabase().delete(t, null, null); } catch (Exception ignored) {}
        }
    }
}
