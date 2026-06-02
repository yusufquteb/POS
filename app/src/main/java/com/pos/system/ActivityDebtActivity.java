package com.pos.system;

import android.content.Intent;
import android.net.Uri;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * ActivityDebtActivity - صفحة إدارة ديون العملاء / Customer Debt Management
 *
 * ✅ إحصائيات: إجمالي الديون + عدد العملاء المدينين
 * ✅ بحث فوري بالاسم أو الهاتف
 * ✅ قائمة RecyclerView بالعملاء المدينين
 * ✅ نقر على عميل → حوار تسوية الدين
 * ✅ حالة فارغة عند عدم وجود ديون
 *
 * @version 2.0
 */
public class ActivityDebtActivity extends BaseActivity {

    private RecyclerView rvDebts;
    private TextView tvTotalDebt;
    private TextView tvDebtCount;
    private View tvEmpty;
    private TextInputEditText etSearch;
    private DebtAdapter adapter;
    private DBHelper dbHelper;

    /** القائمة الكاملة (غير مفلترة) */
    private final List<HashMap<String, String>> allDebtsList = new ArrayList<>();
    /** القائمة المعروضة بعد الفلتر */
    private final List<HashMap<String, String>> filteredList  = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt);

        applyWindowInsets(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        setupSearch();
        setupRecyclerView();
        loadDebts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialisation
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews() {
        rvDebts     = findViewById(R.id.rv_debts);
        tvTotalDebt = findViewById(R.id.tv_total_debt);
        tvDebtCount = findViewById(R.id.tv_debt_count);
        tvEmpty     = findViewById(R.id.tv_empty);
        etSearch    = findViewById(R.id.et_search);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString().trim());
            }
        });
    }

    private void setupRecyclerView() {
        if (rvDebts == null) return;
        rvDebts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DebtAdapter();
        rvDebts.setAdapter(adapter);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Data loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadDebts() {
        allDebtsList.clear();
        try {
            List<HashMap<String, String>> result = dbHelper.getCustomersWithDebt();
            if (result != null) allDebtsList.addAll(result);
        } catch (Exception ignored) {}

        updateStatsCard();

        // أعد تطبيق الفلتر الحالي بعد إعادة التحميل
        String currentQuery = (etSearch != null && etSearch.getText() != null)
                ? etSearch.getText().toString().trim() : "";
        filterList(currentQuery);
    }

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allDebtsList);
        } else {
            String lq = query.toLowerCase(Locale.getDefault());
            for (HashMap<String, String> item : allDebtsList) {
                String name  = safeGet(item, "name").toLowerCase(Locale.getDefault());
                String phone = safeGet(item, "phone").toLowerCase(Locale.getDefault());
                if (name.contains(lq) || phone.contains(lq)) {
                    filteredList.add(item);
                }
            }
        }
        updateListUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void updateStatsCard() {
        double total = 0;
        for (HashMap<String, String> item : allDebtsList) {
            try { total += Double.parseDouble(safeGet(item, "debt")); }
            catch (NumberFormatException ignored) {}
        }
        if (tvTotalDebt != null)
            tvTotalDebt.setText(String.format(Locale.getDefault(), "%.2f ر.س", total));
        if (tvDebtCount != null)
            tvDebtCount.setText(String.valueOf(allDebtsList.size()));
    }

    private void updateListUI() {
        boolean empty = filteredList.isEmpty();
        if (rvDebts != null) rvDebts.setVisibility(empty ? View.GONE  : View.VISIBLE);
        if (tvEmpty != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private String safeGet(HashMap<String, String> map, String key) {
        if (map == null) return "";
        String v = map.get(key);
        return v != null ? v : "";
    }

    private String getEditText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Settle-debt dialog
    // ─────────────────────────────────────────────────────────────────────────

    private void showSettleDebtDialog(HashMap<String, String> customer) {
        String customerId   = safeGet(customer, "id");
        String customerName = safeGet(customer, "name");

        // اجلب أحدث قيمة للدين مباشرة من قاعدة البيانات
        double currentDebt;
        try {
            currentDebt = dbHelper.getCustomerDebt(customerId);
        } catch (Exception e) {
            try { currentDebt = Double.parseDouble(safeGet(customer, "debt")); }
            catch (NumberFormatException nfe) { currentDebt = 0; }
        }
        final double finalDebt = currentDebt;

        // بناء واجهة الحوار
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_settle_debt, null, false);

        TextView tvDialogName      = dialogView.findViewById(R.id.tv_dialog_customer_name);
        TextView tvDialogDebt      = dialogView.findViewById(R.id.tv_dialog_current_debt);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_payment_amount);

        if (tvDialogName != null)
            tvDialogName.setText(customerName);
        if (tvDialogDebt != null)
            tvDialogDebt.setText(String.format(Locale.getDefault(),
                    "الدين الحالي / Current Debt:  %.2f ر.س", finalDebt));

        new MaterialAlertDialogBuilder(this)
                .setTitle("تسوية دين العميل\nSettle Customer Debt")
                .setView(dialogView)
                .setPositiveButton("تسوية الدين / Settle", (dialog, which) -> {
                    String amountStr = getEditText(etAmount);
                    if (amountStr.isEmpty()) {
                        showToast("يرجى إدخال مبلغ الدفع\nPlease enter payment amount");
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        showToast("المبلغ غير صحيح\nInvalid amount");
                        return;
                    }
                    if (amount <= 0) {
                        showToast("يجب أن يكون المبلغ أكبر من صفر\nAmount must be greater than zero");
                        return;
                    }
                    if (amount > finalDebt) {
                        showToast("المبلغ أكبر من الدين الحالي\nAmount exceeds current debt");
                        return;
                    }
                    performSettlement(customerId, amount);
                })
                .setNegativeButton("إلغاء / Cancel", null)
                .show();
    }

    private void performSettlement(String customerId, double amount) {
        try {
            boolean success = dbHelper.settleCustomerDebt(customerId, amount);
            View rootView = rvDebts != null ? rvDebts : findViewById(android.R.id.content);
            if (success) {
                loadDebts();   // أعد تحميل القائمة والإحصائيات
                if (rootView != null) {
                    Snackbar.make(rootView,
                            String.format(Locale.getDefault(),
                                    "✓ تمت التسوية (%.2f ر.س) / Debt settled", amount),
                            Snackbar.LENGTH_LONG).show();
                } else {
                    showToast("✓ تمت التسوية بنجاح");
                }
            } else {
                if (rootView != null) {
                    Snackbar.make(rootView,
                            "فشل في تسوية الدين / Failed to settle debt",
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    showToast("فشل في تسوية الدين");
                }
            }
        } catch (Exception e) {
            showToast("خطأ أثناء التسوية\nError during settlement");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RecyclerView Adapter  (inline item layout: item_debt)
    // ─────────────────────────────────────────────────────────────────────────

    private class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_debt, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            HashMap<String, String> item = filteredList.get(position);

            String name  = safeGet(item, "name");
            String phone = safeGet(item, "phone");

            if (holder.tvName  != null) holder.tvName.setText(name);
            if (holder.tvPhone != null)
                holder.tvPhone.setText(phone.isEmpty() ? "لا يوجد رقم / No phone" : phone);
            if (holder.tvDebt  != null) {
                try {
                    double debt = Double.parseDouble(safeGet(item, "debt"));
                    holder.tvDebt.setText(
                            String.format(Locale.getDefault(), "%.2f ر.س", debt));
                } catch (NumberFormatException e) {
                    holder.tvDebt.setText("0.00 ر.س");
                }
            }

            // نقر على الصف → حوار التسوية
            holder.itemView.setOnClickListener(v -> showSettleDebtDialog(item));

            // زر الاتصال (إن وُجد في التخطيط)
            if (holder.btnCall != null) {
                if (phone.isEmpty()) {
                    holder.btnCall.setVisibility(View.GONE);
                } else {
                    holder.btnCall.setVisibility(View.VISIBLE);
                    holder.btnCall.setOnClickListener(v -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_DIAL,
                                    Uri.parse("tel:" + phone)));
                        } catch (Exception ignored) {}
                    });
                }
            }
        }

        @Override
        public int getItemCount() { return filteredList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvDebt;
            View btnCall;

            VH(View v) {
                super(v);
                tvName  = v.findViewById(R.id.tv_customer_name);
                tvPhone = v.findViewById(R.id.tv_customer_phone);
                tvDebt  = v.findViewById(R.id.tv_customer_debt);
                btnCall = v.findViewById(R.id.btn_call);
            }
        }
    }
}
