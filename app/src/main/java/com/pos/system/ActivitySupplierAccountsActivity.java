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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivitySupplierAccountsBinding;

public class ActivitySupplierAccountsActivity extends BaseActivity {

    private ActivitySupplierAccountsBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private long supplierId;
    private String supplierName;
    private List<HashMap<String, String>> txList = new ArrayList<>();
    private TxAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupplierAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        supplierId   = getIntent().getLongExtra("supplier_id", 0);
        supplierName = getIntent().getStringExtra("supplier_name");
        if (supplierName == null) supplierName = "مورد";

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
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(supplierName);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TxAdapter();
        binding.recyclerTransactions.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnAddPayment.setOnClickListener(v -> showAddPaymentDialog());
    }

    private void loadData() {
        binding.tvCustomerName.setText(supplierName);
        txList.clear();
        txList.addAll(dbHelper.getSupplierTransactionHistory(supplierId));

        double totalDebt = 0, totalPaid = 0;
        for (HashMap<String, String> t : txList) {
            String txType = t.getOrDefault("tx_type", "");
            double amt = 0;
            try { amt = Double.parseDouble(t.getOrDefault("amount", "0")); } catch (Exception ignored) {}
            if ("purchase".equals(txType)) totalDebt += amt;
            else if ("payment".equals(txType) && "OUT".equals(t.getOrDefault("payment_method", "")))
                totalPaid += amt;
        }
        binding.tvTotalDebt.setText(String.format(Locale.US, "%.2f %s", totalDebt, currency));
        binding.tvTotalPaid.setText(String.format(Locale.US, "%.2f %s", totalPaid, currency));
        adapter.notifyDataSetChanged();
    }

    private void showAddPaymentDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextInputEditText etAmount = v.findViewById(R.id.et_input);
        if (etAmount != null) etAmount.setHint("المبلغ المدفوع");

        new MaterialAlertDialogBuilder(this)
            .setTitle("تسجيل دفعة للمورد " + supplierName)
            .setView(v)
            .setPositiveButton("تأكيد", (d, w) -> {
                try {
                    String s = etAmount != null && etAmount.getText() != null
                        ? etAmount.getText().toString().trim() : "";
                    if (s.isEmpty()) return;
                    double amount = Double.parseDouble(s);
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    dbHelper.addDirectPayment("supplier", supplierId, supplierName, "OUT", amount, "", date);
                    loadData();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private class TxAdapter extends RecyclerView.Adapter<TxAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            View indicator;
            TextView tvRef, tvNote, tvDate, tvAmount, tvRemaining;
            VH(View v) {
                super(v);
                indicator   = v.findViewById(R.id.view_indicator);
                tvRef       = v.findViewById(R.id.tv_ref);
                tvNote      = v.findViewById(R.id.tv_note);
                tvDate      = v.findViewById(R.id.tv_date);
                tvAmount    = v.findViewById(R.id.tv_amount);
                tvRemaining = v.findViewById(R.id.tv_remaining);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= txList.size()) return;
            HashMap<String, String> t = txList.get(pos);
            String txType = t.getOrDefault("tx_type", "");
            double amt = 0;
            try { amt = Double.parseDouble(t.getOrDefault("amount", "0")); } catch (Exception ignored) {}
            if (h.tvRef    != null) h.tvRef.setText("purchase".equals(txType)
                ? "طلب شراء: " + t.getOrDefault("ref", "") : "دفعة");
            if (h.tvNote   != null) h.tvNote.setText(t.getOrDefault("note", ""));
            if (h.tvDate   != null) h.tvDate.setText(t.getOrDefault("created_at", ""));
            if (h.tvAmount != null) h.tvAmount.setText(
                String.format(Locale.US, "%.2f %s", amt, currency));
            if (h.tvRemaining != null) h.tvRemaining.setVisibility(View.GONE);
            if (h.indicator != null)
                h.indicator.setBackgroundColor("payment".equals(txType) ? 0xFF4CAF50 : 0xFFF44336);
        }

        @Override
        public int getItemCount() { return txList.size(); }
    }
}
