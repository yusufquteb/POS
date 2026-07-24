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
import com.pos.system.databinding.ActivityWalletBinding;

public class ActivityWalletActivity extends BaseActivity {

    private ActivityWalletBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private List<HashMap<String, String>> transactions = new ArrayList<>();
    private WalletAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

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
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalletAdapter();
        binding.recyclerTransactions.setAdapter(adapter);
    }

    private void setupButtons() {
        binding.btnDeposit.setOnClickListener(v -> showTransactionDialog("IN"));
        binding.btnWithdraw.setOnClickListener(v -> showTransactionDialog("OUT"));
    }

    private void loadData() {
        double balance = dbHelper.getWalletBalance();
        binding.tvBalance.setText(String.format(Locale.US, "%.2f", balance));
        binding.tvCurrency.setText(currency);

        transactions.clear();
        transactions.addAll(dbHelper.getWalletTransactions(null, null));
        boolean empty = transactions.isEmpty();
        binding.recyclerTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void showTransactionDialog(String type) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_input);
        if (etAmount != null) etAmount.setHint(getString(R.string.amount_hint));

        String title = "IN".equals(type) ? getString(R.string.deposit_to_drawer) : getString(R.string.withdraw_from_drawer);
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm, (d, w) -> {
                try {
                    String amtStr = etAmount != null && etAmount.getText() != null
                        ? etAmount.getText().toString().trim() : "";
                    if (amtStr.isEmpty()) return;
                    double amount = Double.parseDouble(amtStr);
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    dbHelper.addWalletTransaction(type, amount, "", date);
                    loadData();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            View viewDot;
            TextView tvNote, tvDate, tvAmount;
            VH(View v) {
                super(v);
                viewDot  = v.findViewById(R.id.view_dot);
                tvNote   = v.findViewById(R.id.tv_note);
                tvDate   = v.findViewById(R.id.tv_date);
                tvAmount = v.findViewById(R.id.tv_amount);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= transactions.size()) return;
            HashMap<String, String> t = transactions.get(pos);
            String type = t.getOrDefault("type", "OUT");
            boolean isIn = "IN".equals(type);
            if (h.viewDot != null) {
                h.viewDot.setBackgroundResource(
                    isIn ? R.drawable.circle_indicator_green : R.drawable.circle_indicator_red);
            }
            if (h.tvNote   != null) h.tvNote.setText(t.getOrDefault("note", ""));
            if (h.tvDate   != null) h.tvDate.setText(t.getOrDefault("date", ""));
            if (h.tvAmount != null) {
                double amt = 0;
                try { amt = Double.parseDouble(t.getOrDefault("amount", "0")); } catch (Exception ignored) {}
                h.tvAmount.setText((isIn ? "+" : "-") +
                    String.format(Locale.US, "%.2f", amt) + " " + currency);
                h.tvAmount.setTextColor(androidx.core.content.ContextCompat.getColor(h.tvAmount.getContext(), isIn ? R.color.color_success : R.color.color_error));
            }
        }

        @Override
        public int getItemCount() { return transactions.size(); }
    }
}
