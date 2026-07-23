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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivitySupplierRemainingBinding;

public class ActivitySupplierRemainingActivity extends BaseActivity {

    private ActivitySupplierRemainingBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private List<HashMap<String, String>> suppliers = new ArrayList<>();
    private DebtAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupplierRemainingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            if (s != null) currency = s.getOrDefault("currency", "ج.م");
        } catch (Exception ignored) {}

        setupToolbar();
        setupRecycler();
        loadData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        binding.recyclerCustomers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DebtAdapter();
        binding.recyclerCustomers.setAdapter(adapter);
    }

    private void loadData() {
        suppliers.clear();
        suppliers.addAll(dbHelper.getSuppliersWithDebt());
        double total = 0;
        for (HashMap<String, String> s : suppliers) {
            try { total += Double.parseDouble(s.getOrDefault("debt", "0")); } catch (Exception ignored) {}
        }
        binding.tvTotalRemaining.setText(String.format(Locale.US, "%.2f %s", total, currency));
        boolean empty = suppliers.isEmpty();
        binding.recyclerCustomers.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void showPayDialog(HashMap<String, String> supplier) {
        long id = 0;
        double debt = 0;
        String name = supplier.getOrDefault("name", "");
        try { id   = Long.parseLong(supplier.getOrDefault("id",   "0")); } catch (Exception ignored) {}
        try { debt = Double.parseDouble(supplier.getOrDefault("debt", "0")); } catch (Exception ignored) {}
        final long fId = id;
        final double fDebt = debt;

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextInputEditText etAmount = v.findViewById(R.id.et_input);
        if (etAmount != null) {
            etAmount.setHint(getString(R.string.amount_hint));
            etAmount.setText(String.format(Locale.US, "%.2f", fDebt));
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.pay_supplier_due_format, name,
                String.format(Locale.US, "%.2f", fDebt) + " " + currency))
            .setView(v)
            .setPositiveButton(R.string.confirm, (d, w) -> {
                try {
                    String s = etAmount != null && etAmount.getText() != null
                        ? etAmount.getText().toString().trim() : "";
                    if (s.isEmpty()) return;
                    double paid = Double.parseDouble(s);
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    dbHelper.addDirectPayment("supplier", fId, name, "OUT", paid, getString(R.string.debt_settlement_note), date);
                    loadData();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvDebt;
            MaterialButton btnPay;
            VH(View v) {
                super(v);
                tvName  = v.findViewById(R.id.tv_name);
                tvPhone = v.findViewById(R.id.tv_phone);
                tvDebt  = v.findViewById(R.id.tv_debt);
                btnPay  = v.findViewById(R.id.btn_pay);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_debt, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= suppliers.size()) return;
            HashMap<String, String> s = suppliers.get(pos);
            double debt = 0;
            try { debt = Double.parseDouble(s.getOrDefault("debt", "0")); } catch (Exception ignored) {}
            if (h.tvName  != null) h.tvName.setText(s.getOrDefault("name", ""));
            if (h.tvPhone != null) h.tvPhone.setText(s.getOrDefault("phone", ""));
            if (h.tvDebt  != null) h.tvDebt.setText(String.format(Locale.US, "%.2f %s", debt, currency));
            if (h.btnPay  != null) h.btnPay.setOnClickListener(v -> showPayDialog(s));
        }

        @Override
        public int getItemCount() { return suppliers.size(); }
    }
}
