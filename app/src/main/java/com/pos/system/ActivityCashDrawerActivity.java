package com.pos.system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pos.system.databinding.ActivityCashDrawerBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityCashDrawerActivity extends BaseActivity {

    private ActivityCashDrawerBinding binding;
    private DBHelper   dbHelper;

    private final List<HashMap<String, String>> drawersList = new ArrayList<>();
    private final List<HashMap<String, String>> txList      = new ArrayList<>();
    private DrawersAdapter  drawersAdapter;
    private TxAdapter       txAdapter;
    private HashMap<String, String> selectedDrawer = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCashDrawerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.coordinatorRoot);
        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        loadDrawers();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.hub_cash_drawer);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        binding.rvDrawers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        drawersAdapter = new DrawersAdapter();
        binding.rvDrawers.setAdapter(drawersAdapter);
        binding.rvDrawers.setItemAnimator(null);

        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        txAdapter = new TxAdapter();
        binding.rvTransactions.setAdapter(txAdapter);
        binding.rvTransactions.setItemAnimator(null);

        binding.fabAdd.setOnClickListener(v -> showAddDrawerDialog());
        binding.btnEmptyAddDrawer.setOnClickListener(v -> showAddDrawerDialog());

        binding.rvDrawers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) binding.fabAdd.shrink();
                else if (dy < 0) binding.fabAdd.extend();
            }
        });

        binding.btnDeposit.setOnClickListener(v -> showTransactionDialog("in"));
        binding.btnWithdraw.setOnClickListener(v -> showTransactionDialog("out"));
    }

    private void loadDrawers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<HashMap<String, String>> drawers = dbHelper.getAllCashDrawers();
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                drawersList.clear();
                drawersList.addAll(drawers);
                drawersAdapter.notifyDataSetChanged();
                binding.tvEmpty.setVisibility(drawersList.isEmpty() ? View.VISIBLE : View.GONE);
                if (!drawersList.isEmpty() && selectedDrawer == null) selectDrawer(drawersList.get(0));
            });
        });
    }

    private void selectDrawer(HashMap<String, String> drawer) {
        selectedDrawer = drawer;
        long drawerId = 0;
        double balance = 0;
        try { drawerId = Long.parseLong(drawer.getOrDefault("id", "0")); } catch (Exception ignored) {}
        try { balance  = Double.parseDouble(drawer.getOrDefault("current_balance", "0")); } catch (Exception ignored) {}
        binding.tvBalanceLabel.setText(drawer.getOrDefault("name", getString(R.string.nav_cash_drawer)));
        binding.tvBalance.setText(String.format(Locale.US, "%.2f %s", balance, getCurrency()));
        binding.cardBalance.setVisibility(View.VISIBLE);
        loadTransactions(drawerId);
    }

    private void loadTransactions(long drawerId) {
        executor.execute(() -> {
            List<HashMap<String, String>> txs = dbHelper.getCashTransactions(drawerId, 50);
            runOnUiThread(() -> {
                txList.clear();
                txList.addAll(txs);
                txAdapter.notifyDataSetChanged();
            });
        });
    }

    private void showAddDrawerDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_simple_input, null);
        TextInputLayout tilInput = dv.findViewById(R.id.til_input);
        TextInputEditText et = dv.findViewById(R.id.et_input);
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_new_drawer_title)
            .setView(dv)
            .setPositiveButton(R.string.save, (d, w) -> {
                if (tilInput != null) tilInput.setError(null);
                String name = et != null && et.getText() != null ? et.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    if (tilInput != null) tilInput.setError(getString(R.string.name_required));
                    return;
                }
                executor.execute(() -> {
                    long id = dbHelper.addCashDrawer(name);
                    runOnUiThread(() -> { if (id > 0) { showToast(getString(R.string.drawer_added_successfully)); loadDrawers(); } else showSnackbar(getString(R.string.error_title), true); });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showTransactionDialog(String type) {
        if (selectedDrawer == null) { showToast(getString(R.string.select_drawer_first)); return; }
        View dv = getLayoutInflater().inflate(R.layout.dialog_cash_transaction, null);
        TextInputLayout tilAmount = dv.findViewById(R.id.til_amount);
        TextInputEditText etAmount = dv.findViewById(R.id.et_amount);
        TextInputEditText etReason = dv.findViewById(R.id.et_reason);
        String title = "in".equals(type) ? getString(R.string.deposit_to_drawer) : getString(R.string.withdraw_from_drawer);
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dv)
            .setPositiveButton(R.string.confirm, (d, w) -> {
                if (tilAmount != null) tilAmount.setError(null);
                String amtStr = etAmount != null && etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String reason = etReason != null && etReason.getText() != null ? etReason.getText().toString().trim() : "";
                double amt = 0;
                try { amt = Double.parseDouble(amtStr.isEmpty() ? "0" : amtStr); } catch (Exception e) {
                    if (tilAmount != null) tilAmount.setError(getString(R.string.invalid_amount));
                    return;
                }
                if (amt <= 0) { showToast(getString(R.string.amount_must_be_positive)); return; }
                long drawerId;
                try { drawerId = Long.parseLong(selectedDrawer.getOrDefault("id", "0")); }
                catch (Exception e) { showToast(getString(R.string.error_unknown)); return; }
                double finalAmt = amt;
                executor.execute(() -> {
                    boolean ok = dbHelper.cashDrawerTransaction(drawerId, type, finalAmt, reason, "admin", "", "manual");
                    runOnUiThread(() -> {
                        if (ok) { showToast("in".equals(type) ? getString(R.string.deposit_successful) : getString(R.string.withdraw_successful)); loadDrawers(); }
                        else showSnackbar(getString(R.string.insufficient_balance_or_error), true);
                    });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private String getCurrency() {
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            return s != null ? s.getOrDefault("currency", "ج.م") : "ج.م";
        } catch (Exception e) { return "ج.م"; }
    }


    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }

    // ──── Drawers Adapter ────────────────────────────────────────────
    private class DrawersAdapter extends RecyclerView.Adapter<DrawersAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_drawer_chip, parent, false));
        }
        @Override
        public void onBindViewHolder(VH h, int pos) {
            if (pos < 0 || pos >= drawersList.size()) return;
            HashMap<String, String> d = drawersList.get(pos);
            h.tvName.setText(d.getOrDefault("name", "—"));
            double bal = 0;
            try { bal = Double.parseDouble(d.getOrDefault("current_balance", "0")); } catch (Exception ignored) {}
            h.tvBal.setText(String.format(Locale.US, "%.2f", bal));
            boolean sel = selectedDrawer != null && d.getOrDefault("id","").equals(selectedDrawer.getOrDefault("id",""));
            h.card.setStrokeWidth(sel ? 4 : 0);
            h.itemView.setOnClickListener(v -> { selectDrawer(d); drawersAdapter.notifyDataSetChanged(); });
        }
        @Override public int getItemCount() { return drawersList.size(); }
        class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView tvName, tvBal;
            VH(View v) { super(v); card = (MaterialCardView) v; tvName = v.findViewById(R.id.tv_name); tvBal = v.findViewById(R.id.tv_balance); }
        }
    }

    // ──── Transactions Adapter ────────────────────────────────────────
    private class TxAdapter extends RecyclerView.Adapter<TxAdapter.VH> {
        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_cash_transaction, parent, false));
        }
        @Override
        public void onBindViewHolder(VH h, int pos) {
            if (pos < 0 || pos >= txList.size()) return;
            HashMap<String, String> tx = txList.get(pos);
            String type = tx.getOrDefault("type", "in");
            h.tvType.setText("in".equals(type) ? getString(R.string.transaction_type_deposit) : getString(R.string.transaction_type_withdraw));
            android.content.Context ctx = h.itemView.getContext();
            int colorIn  = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_success);
            int colorOut = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_error);
            h.tvType.setTextColor("in".equals(type) ? colorIn : colorOut);
            double amount = 0, balanceAfter = 0;
            try { amount       = Double.parseDouble(tx.getOrDefault("amount","0")); } catch (Exception ignored) {}
            try { balanceAfter = Double.parseDouble(tx.getOrDefault("balance_after","0")); } catch (Exception ignored) {}
            h.tvAmount.setText(String.format(Locale.US, "%.2f %s", amount, getCurrency()));
            h.tvAmount.setTextColor("in".equals(type) ? colorIn : colorOut);
            h.tvReason.setText(tx.getOrDefault("reason", "—"));
            h.tvBalance.setText(getString(R.string.balance_colon_format, String.format(Locale.US, "%.2f", balanceAfter)));
            h.tvDate.setText(tx.getOrDefault("created_at","").replace("T"," "));
        }
        @Override public int getItemCount() { return txList.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvType, tvAmount, tvReason, tvBalance, tvDate;
            VH(View v) {
                super(v);
                tvType    = v.findViewById(R.id.tv_type);
                tvAmount  = v.findViewById(R.id.tv_amount);
                tvReason  = v.findViewById(R.id.tv_reason);
                tvBalance = v.findViewById(R.id.tv_balance);
                tvDate    = v.findViewById(R.id.tv_date);
            }
        }
    }
}
