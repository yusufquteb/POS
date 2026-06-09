package com.pos.system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityCashDrawerActivity extends BaseActivity {

    private DBHelper   dbHelper;
    private RecyclerView rvDrawers;
    private RecyclerView rvTransactions;
    private ExtendedFloatingActionButton fabAdd;
    private View       tvEmpty;
    private MaterialCardView cardBalance;
    private TextView   tvBalanceLabel;
    private TextView   tvBalance;
    private MaterialButton btnDeposit;
    private MaterialButton btnWithdraw;

    private final List<HashMap<String, String>> drawersList = new ArrayList<>();
    private final List<HashMap<String, String>> txList      = new ArrayList<>();
    private DrawersAdapter  drawersAdapter;
    private TxAdapter       txAdapter;
    private HashMap<String, String> selectedDrawer = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cash_drawer);
        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        loadDrawers();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        if (tb != null) {
            setSupportActionBar(tb);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("الخزينة");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        rvDrawers      = findViewById(R.id.rv_drawers);
        rvTransactions = findViewById(R.id.rv_transactions);
        fabAdd         = findViewById(R.id.fab_add);
        tvEmpty        = findViewById(R.id.tv_empty);
        cardBalance    = findViewById(R.id.card_balance);
        tvBalanceLabel = findViewById(R.id.tv_balance_label);
        tvBalance      = findViewById(R.id.tv_balance);
        btnDeposit     = findViewById(R.id.btn_deposit);
        btnWithdraw    = findViewById(R.id.btn_withdraw);

        rvDrawers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        drawersAdapter = new DrawersAdapter();
        rvDrawers.setAdapter(drawersAdapter);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        txAdapter = new TxAdapter();
        rvTransactions.setAdapter(txAdapter);

        if (fabAdd != null)
            fabAdd.setOnClickListener(v -> showAddDrawerDialog());
        if (btnDeposit != null)
            btnDeposit.setOnClickListener(v -> showTransactionDialog("in"));
        if (btnWithdraw != null)
            btnWithdraw.setOnClickListener(v -> showTransactionDialog("out"));
    }

    private void loadDrawers() {
        executor.execute(() -> {
            List<HashMap<String, String>> drawers = dbHelper.getAllCashDrawers();
            runOnUiThread(() -> {
                drawersList.clear();
                drawersList.addAll(drawers);
                drawersAdapter.notifyDataSetChanged();
                if (tvEmpty != null) tvEmpty.setVisibility(drawersList.isEmpty() ? View.VISIBLE : View.GONE);
                if (!drawersList.isEmpty() && selectedDrawer == null) selectDrawer(drawersList.get(0));
            });
        });
    }

    private void selectDrawer(HashMap<String, String> drawer) {
        selectedDrawer = drawer;
        long drawerId = Long.parseLong(drawer.getOrDefault("id", "0"));
        if (tvBalanceLabel != null)
            tvBalanceLabel.setText(drawer.getOrDefault("name", "الخزينة"));
        if (tvBalance != null)
            tvBalance.setText(String.format(Locale.US, "%.2f %s",
                Double.parseDouble(drawer.getOrDefault("current_balance", "0")), getCurrency()));
        if (cardBalance != null) cardBalance.setVisibility(View.VISIBLE);
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
        TextInputEditText et = dv.findViewById(R.id.et_input);
        new MaterialAlertDialogBuilder(this)
            .setTitle("إضافة خزينة جديدة")
            .setView(dv)
            .setPositiveButton("حفظ", (d, w) -> {
                String name = et != null && et.getText() != null ? et.getText().toString().trim() : "";
                if (name.isEmpty()) { showToast("الاسم مطلوب"); return; }
                executor.execute(() -> {
                    long id = dbHelper.addCashDrawer(name);
                    runOnUiThread(() -> { if (id > 0) { showToast("تمت الإضافة"); loadDrawers(); } else showToast("خطأ"); });
                });
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showTransactionDialog(String type) {
        if (selectedDrawer == null) { showToast("يرجى اختيار خزينة أولاً"); return; }
        View dv = getLayoutInflater().inflate(R.layout.dialog_cash_transaction, null);
        TextInputEditText etAmount = dv.findViewById(R.id.et_amount);
        TextInputEditText etReason = dv.findViewById(R.id.et_reason);
        String title = "in".equals(type) ? "إيداع في الخزينة" : "سحب من الخزينة";
        new MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dv)
            .setPositiveButton("تأكيد", (d, w) -> {
                String amtStr = etAmount != null && etAmount.getText() != null ? etAmount.getText().toString().trim() : "0";
                String reason = etReason != null && etReason.getText() != null ? etReason.getText().toString().trim() : "";
                double amt = 0;
                try { amt = Double.parseDouble(amtStr); } catch (Exception e) { showToast("مبلغ غير صحيح"); return; }
                if (amt <= 0) { showToast("المبلغ يجب أن يكون أكبر من صفر"); return; }
                long drawerId = Long.parseLong(selectedDrawer.getOrDefault("id", "0"));
                double finalAmt = amt;
                executor.execute(() -> {
                    boolean ok = dbHelper.cashDrawerTransaction(drawerId, type, finalAmt, reason, "admin", "", "manual");
                    runOnUiThread(() -> {
                        if (ok) { showToast(("in".equals(type) ? "تم الإيداع" : "تم السحب") + " بنجاح"); loadDrawers(); }
                        else showToast("خطأ: رصيد غير كافٍ أو خطأ في العملية");
                    });
                });
            })
            .setNegativeButton("إلغاء", null)
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
            HashMap<String, String> d = drawersList.get(pos);
            h.tvName.setText(d.getOrDefault("name", "—"));
            h.tvBal.setText(String.format(Locale.US, "%.2f", Double.parseDouble(d.getOrDefault("current_balance", "0"))));
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
            HashMap<String, String> tx = txList.get(pos);
            String type = tx.getOrDefault("type", "in");
            h.tvType.setText("in".equals(type) ? "إيداع ↑" : "سحب ↓");
            h.tvType.setTextColor("in".equals(type) ? 0xFF4CAF50 : 0xFFF44336);
            h.tvAmount.setText(String.format(Locale.US, "%.2f %s",
                Double.parseDouble(tx.getOrDefault("amount","0")), getCurrency()));
            h.tvAmount.setTextColor("in".equals(type) ? 0xFF4CAF50 : 0xFFF44336);
            h.tvReason.setText(tx.getOrDefault("reason", "—"));
            h.tvBalance.setText("الرصيد: " + String.format(Locale.US, "%.2f",
                Double.parseDouble(tx.getOrDefault("balance_after","0"))));
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
