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
import com.pos.system.databinding.ActivityCustomerAccountsBinding;
import com.pos.system.managers.LoyaltyManager;

public class ActivityCustomerAccountsActivity extends BaseActivity {

    private ActivityCustomerAccountsBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private long customerId;
    private String customerName;
    private List<HashMap<String, String>> txList = new ArrayList<>();
    private TxAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        customerId   = getIntent().getLongExtra("customer_id", 0);
        customerName = getIntent().getStringExtra("customer_name");
        if (customerName == null) customerName = "عميل";

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
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(customerName);
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
        binding.tvCustomerName.setText(customerName);
        txList.clear();
        txList.addAll(dbHelper.getCustomerTransactionHistory(customerId));

        double totalDebt = 0, totalPaid = 0;
        for (HashMap<String, String> t : txList) {
            String txType = t.getOrDefault("tx_type", "");
            double amt = 0;
            try { amt = Double.parseDouble(t.getOrDefault("amount", "0")); } catch (Exception ignored) {}
            if ("invoice".equals(txType)) {
                double rem = 0;
                try { rem = Double.parseDouble(t.getOrDefault("remaining_amount", "0")); } catch (Exception ignored) {}
                totalDebt += rem;
                totalPaid += (amt - rem);
            } else if ("payment".equals(txType)) {
                String pmtType = t.getOrDefault("payment_method", "OUT");
                if ("IN".equals(pmtType)) totalPaid += amt;
                else totalDebt += amt;
            }
        }
        binding.tvTotalDebt.setText(String.format(Locale.US, "%.2f %s", totalDebt, currency));
        binding.tvTotalPaid.setText(String.format(Locale.US, "%.2f %s", totalPaid, currency));
        adapter.notifyDataSetChanged();
        loadLoyaltyInfo();
    }

    private void loadLoyaltyInfo() {
        try {
            LoyaltyManager lm = new LoyaltyManager(this);
            int balance = lm.getBalance(String.valueOf(customerId));
            LoyaltyManager.Tier tier = lm.getTier(String.valueOf(customerId));

            android.widget.TextView tvTier   = binding.tvLoyaltyTier;
            android.widget.TextView tvPoints = binding.tvLoyaltyPoints;

            if (tvTier != null) {
                tvTier.setText(tier.displayName());
                tvTier.setVisibility(android.view.View.VISIBLE);
                int tierColor;
                switch (tier) {
                    case GOLD:   tierColor = 0xFFFFD700; break;
                    case SILVER: tierColor = 0xFF9E9E9E; break;
                    default:     tierColor = 0xFFCD7F32; break;
                }
                if (tvTier.getBackground() instanceof android.graphics.drawable.GradientDrawable) {
                    ((android.graphics.drawable.GradientDrawable) tvTier.getBackground()).setColor(tierColor);
                }
            }
            if (tvPoints != null) {
                tvPoints.setText("نقاط الولاء: " + balance + " نقطة");
                tvPoints.setVisibility(android.view.View.VISIBLE);
            }
            lm.close();
        } catch (Exception e) {
            android.util.Log.e("CustomerAccounts", "loadLoyaltyInfo: " + e.getMessage());
        }
    }

    private void showAddPaymentDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextInputEditText etAmount = v.findViewById(R.id.et_input);
        if (etAmount != null) etAmount.setHint("المبلغ المدفوع");

        new MaterialAlertDialogBuilder(this)
            .setTitle("تسجيل دفعة لـ " + customerName)
            .setView(v)
            .setPositiveButton("تأكيد", (d, w) -> {
                try {
                    String s = etAmount != null && etAmount.getText() != null
                        ? etAmount.getText().toString().trim() : "";
                    if (s.isEmpty()) return;
                    double amount = Double.parseDouble(s);
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    dbHelper.addDirectPayment("customer", customerId, customerName, "IN", amount, "", date);
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
            boolean isInvoice = "invoice".equals(txType);
            double amt = 0, rem = 0;
            try { amt = Double.parseDouble(t.getOrDefault("amount", "0")); } catch (Exception ignored) {}
            try { rem = Double.parseDouble(t.getOrDefault("remaining_amount", "0")); } catch (Exception ignored) {}

            if (h.tvRef  != null) h.tvRef.setText(isInvoice
                ? "فاتورة: " + t.getOrDefault("ref", "") : "دفعة");
            if (h.tvNote != null) h.tvNote.setText(t.getOrDefault("note", ""));
            if (h.tvDate != null) h.tvDate.setText(t.getOrDefault("created_at", ""));
            if (h.tvAmount != null) h.tvAmount.setText(
                String.format(Locale.US, "%.2f %s", amt, currency));
            if (h.tvRemaining != null) {
                if (isInvoice && rem > 0) {
                    h.tvRemaining.setVisibility(View.VISIBLE);
                    h.tvRemaining.setText("متبقي: " + String.format(Locale.US, "%.2f", rem));
                } else {
                    h.tvRemaining.setVisibility(View.GONE);
                }
            }
            if (h.indicator != null) {
                int color = isInvoice ? 0xFFF44336 : 0xFF4CAF50;
                h.indicator.setBackgroundColor(color);
            }
        }

        @Override
        public int getItemCount() { return txList.size(); }
    }
}
