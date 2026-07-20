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
            confirmDelete(getString(R.string.delete_invoices_title), getString(R.string.delete_invoices_msg),
                () -> { dbHelper.getWritableDatabase().delete("invoices", null, null);
                        dbHelper.getWritableDatabase().delete("invoice_items", null, null); }));
        binding.btnDeleteExpenses.setOnClickListener(v ->
            confirmDelete(getString(R.string.delete_expenses_title), getString(R.string.delete_expenses_msg),
                () -> dbHelper.getWritableDatabase().delete("expenses", null, null)));
        binding.btnDeleteCustomers.setOnClickListener(v ->
            confirmDelete(getString(R.string.delete_customers_title), getString(R.string.delete_customers_msg),
                () -> dbHelper.getWritableDatabase().delete("customers", null, null)));
        binding.btnDeleteSuppliers.setOnClickListener(v ->
            confirmDelete(getString(R.string.delete_suppliers_title), getString(R.string.delete_suppliers_msg),
                () -> dbHelper.getWritableDatabase().delete("suppliers", null, null)));
        binding.btnDeleteProducts.setOnClickListener(v ->
            confirmDelete(getString(R.string.delete_products_title), getString(R.string.delete_products_msg),
                () -> dbHelper.getWritableDatabase().delete("products", null, null)));
        binding.btnDeleteAll.setOnClickListener(v ->
            confirmDelete(getString(R.string.delete_all_data_title), getString(R.string.delete_all_data_msg),
                () -> deleteAllData()));
    }

    private void confirmDelete(String title, String message, Runnable action) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.confirm_delete_action, (d, w) -> {
                try {
                    action.run();
                    Snackbar.make(binding.getRoot(), getString(R.string.deleted_successfully), Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Snackbar.make(binding.getRoot(), getString(R.string.error_with_message, e.getMessage()), Snackbar.LENGTH_LONG).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
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
