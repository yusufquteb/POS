package com.pos.system;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * ActivityDebtActivity - إدارة ديون العملاء والموردين
 *
 * تبويب 1: ديون العملاء — ما يدين به العملاء للمتجر
 * تبويب 2: ديون الموردين — ما يدين به المتجر للموردين
 */
public class ActivityDebtActivity extends BaseActivity {

    private static final int TAB_CUSTOMERS  = 0;
    private static final int TAB_SUPPLIERS  = 1;

    private TabLayout     tabLayout;
    private RecyclerView  rvDebts;
    private RecyclerView  rvSupplierDebts;
    private TextView      tvTotalDebt;
    private TextView      tvDebtCount;
    private TextView      tvStatsLabel;
    private TextView      tvCountLabel;
    private TextView      tvEmptyTitle;
    private TextView      tvEmptySubtitle;
    private ImageView     ivEmptyIcon;
    private View          tvEmpty;
    private TextInputEditText etSearch;

    private String currency = "ج.م";
    private DBHelper dbHelper;
    private int currentTab = TAB_CUSTOMERS;

    private CustomerDebtAdapter customerAdapter;
    private SupplierDebtAdapter supplierAdapter;

    private final List<HashMap<String, String>> allCustomerDebts  = new ArrayList<>();
    private final List<HashMap<String, String>> filteredCustomers = new ArrayList<>();
    private final List<HashMap<String, String>> allSupplierDebts  = new ArrayList<>();
    private final List<HashMap<String, String>> filteredSuppliers = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt);
        applyWindowInsets(findViewById(android.R.id.content));

        if (!FeatureGate.isUnlocked(this)) {
            FeatureGate.requirePremium(this, "إدارة الديون", true);
            return;
        }

        dbHelper = new DBHelper(this);
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            if (s != null) currency = s.getOrDefault("currency", "ج.م");
        } catch (Exception ignored) {}

        initViews();
        setupToolbar();
        setupTabs();
        setupSearch();
        setupRecyclerViews();
        loadAllData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ─────────────────────────────────────────────────────────────
    //  Init
    // ─────────────────────────────────────────────────────────────

    private void initViews() {
        tabLayout        = findViewById(R.id.tab_layout);
        rvDebts          = findViewById(R.id.rv_debts);
        rvSupplierDebts  = findViewById(R.id.rv_supplier_debts);
        tvTotalDebt      = findViewById(R.id.tv_total_debt);
        tvDebtCount      = findViewById(R.id.tv_debt_count);
        tvStatsLabel     = findViewById(R.id.tv_stats_label);
        tvCountLabel     = findViewById(R.id.tv_count_label);
        tvEmpty          = findViewById(R.id.tv_empty);
        tvEmptyTitle     = findViewById(R.id.tv_empty_title);
        tvEmptySubtitle  = findViewById(R.id.tv_empty_subtitle);
        ivEmptyIcon      = findViewById(R.id.iv_empty_icon);
        etSearch         = findViewById(R.id.et_search);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupTabs() {
        if (tabLayout == null) return;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                clearSearch();
                switchTab(currentTab);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCurrentTab(s.toString().trim());
            }
        });
    }

    private void setupRecyclerViews() {
        if (rvDebts != null) {
            rvDebts.setLayoutManager(new LinearLayoutManager(this));
            customerAdapter = new CustomerDebtAdapter();
            rvDebts.setAdapter(customerAdapter);
        }
        if (rvSupplierDebts != null) {
            rvSupplierDebts.setLayoutManager(new LinearLayoutManager(this));
            supplierAdapter = new SupplierDebtAdapter();
            rvSupplierDebts.setAdapter(supplierAdapter);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Data
    // ─────────────────────────────────────────────────────────────

    private void loadAllData() {
        allCustomerDebts.clear();
        allSupplierDebts.clear();
        try {
            List<HashMap<String, String>> c = dbHelper.getCustomersWithDebt();
            if (c != null) allCustomerDebts.addAll(c);
        } catch (Exception ignored) {}
        try {
            List<HashMap<String, String>> s = dbHelper.getSuppliersWithDebt();
            if (s != null) allSupplierDebts.addAll(s);
        } catch (Exception ignored) {}
        switchTab(currentTab);
    }

    private void switchTab(int tab) {
        boolean isCustomers = (tab == TAB_CUSTOMERS);

        if (rvDebts         != null) rvDebts.setVisibility(isCustomers ? View.VISIBLE : View.GONE);
        if (rvSupplierDebts != null) rvSupplierDebts.setVisibility(isCustomers ? View.GONE : View.VISIBLE);

        if (tvStatsLabel != null)
            tvStatsLabel.setText(isCustomers ? "إجمالي ديون العملاء" : "إجمالي ديون الموردين");
        if (tvCountLabel != null)
            tvCountLabel.setText(isCustomers ? "عميل مدين" : "مورد مدين");
        if (ivEmptyIcon != null)
            ivEmptyIcon.setImageResource(isCustomers ? R.drawable.ic_customers : R.drawable.ic_local_shipping);

        double total = isCustomers ? calcTotal(allCustomerDebts) : calcTotal(allSupplierDebts);
        if (tvTotalDebt != null)
            tvTotalDebt.setText(String.format(Locale.getDefault(), "%.2f %s", total, currency));
        if (tvDebtCount != null)
            tvDebtCount.setText(String.valueOf(isCustomers ? allCustomerDebts.size() : allSupplierDebts.size()));

        filterCurrentTab("");
    }

    private void filterCurrentTab(String query) {
        if (currentTab == TAB_CUSTOMERS) {
            filteredCustomers.clear();
            for (HashMap<String, String> item : allCustomerDebts) {
                if (matches(item, query)) filteredCustomers.add(item);
            }
            boolean empty = filteredCustomers.isEmpty();
            if (rvDebts    != null) rvDebts.setVisibility(empty ? View.GONE  : View.VISIBLE);
            if (tvEmpty    != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty && tvEmptySubtitle != null)
                tvEmptySubtitle.setText("جميع العملاء سدّدوا ديونهم");
            if (customerAdapter != null) customerAdapter.notifyDataSetChanged();
        } else {
            filteredSuppliers.clear();
            for (HashMap<String, String> item : allSupplierDebts) {
                if (matches(item, query)) filteredSuppliers.add(item);
            }
            boolean empty = filteredSuppliers.isEmpty();
            if (rvSupplierDebts != null) rvSupplierDebts.setVisibility(empty ? View.GONE  : View.VISIBLE);
            if (tvEmpty         != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty && tvEmptySubtitle != null)
                tvEmptySubtitle.setText("لا توجد ديون على الموردين");
            if (supplierAdapter != null) supplierAdapter.notifyDataSetChanged();
        }
    }

    private boolean matches(HashMap<String, String> item, String query) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase(Locale.getDefault());
        return safeGet(item, "name").toLowerCase(Locale.getDefault()).contains(q)
            || safeGet(item, "phone").contains(q);
    }

    private double calcTotal(List<HashMap<String, String>> list) {
        double t = 0;
        for (HashMap<String, String> item : list) {
            try { t += Double.parseDouble(safeGet(item, "debt")); }
            catch (NumberFormatException ignored) {}
        }
        return t;
    }

    private void clearSearch() {
        if (etSearch != null) etSearch.setText("");
    }

    // ─────────────────────────────────────────────────────────────
    //  Customer Debt Dialog
    // ─────────────────────────────────────────────────────────────

    private void showCustomerDebtDialog(HashMap<String, String> customer) {
        String customerId   = safeGet(customer, "id");
        String customerName = safeGet(customer, "name");
        String phone        = safeGet(customer, "phone");
        double currentDebt  = dbHelper.getCustomerDebt(customerId);

        String[] options = {"سداد دين", "تسجيل دين جديد", "عرض سجل الحركات", "اتصال"};
        if (phone.isEmpty()) options = new String[]{"سداد دين", "تسجيل دين جديد", "عرض سجل الحركات"};

        final String[] finalOptions = options;
        new MaterialAlertDialogBuilder(this)
            .setTitle(customerName)
            .setMessage(String.format(Locale.getDefault(),
                "الرصيد الحالي: %.2f %s", currentDebt, currency))
            .setItems(finalOptions, (d, which) -> {
                if (which == 0) showSettleDialog(customerId, customerName, currentDebt, true);
                else if (which == 1) showAddDebtDialog(customerId, customerName, true);
                else if (which == 2) showPaymentHistory(customerId, customerName, true);
                else if (which == 3 && !phone.isEmpty()) dialPhone(phone);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // ─────────────────────────────────────────────────────────────
    //  Supplier Debt Dialog
    // ─────────────────────────────────────────────────────────────

    private void showSupplierDebtDialog(HashMap<String, String> supplier) {
        String supplierId   = safeGet(supplier, "id");
        String supplierName = safeGet(supplier, "name");
        String phone        = safeGet(supplier, "phone");
        double currentDebt  = dbHelper.getSupplierDebt(supplierId);

        String[] options = {"سداد دين للمورد", "تسجيل مشتريات بالآجل", "عرض سجل الحركات", "اتصال"};
        if (phone.isEmpty()) options = new String[]{"سداد دين للمورد", "تسجيل مشتريات بالآجل", "عرض سجل الحركات"};

        final String[] finalOptions = options;
        new MaterialAlertDialogBuilder(this)
            .setTitle(supplierName)
            .setMessage(String.format(Locale.getDefault(),
                "المستحق للمورد: %.2f %s", currentDebt, currency))
            .setItems(finalOptions, (d, which) -> {
                if (which == 0) showSettleDialog(supplierId, supplierName, currentDebt, false);
                else if (which == 1) showAddDebtDialog(supplierId, supplierName, false);
                else if (which == 2) showPaymentHistory(supplierId, supplierName, false);
                else if (which == 3 && !phone.isEmpty()) dialPhone(phone);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // ─────────────────────────────────────────────────────────────
    //  Shared Dialogs
    // ─────────────────────────────────────────────────────────────

    private void showSettleDialog(String id, String name, double currentDebt, boolean isCustomer) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settle_debt, null, false);
        TextView tvName  = view.findViewById(R.id.tv_dialog_customer_name);
        TextView tvDebt  = view.findViewById(R.id.tv_dialog_current_debt);
        TextInputEditText etAmount = view.findViewById(R.id.et_payment_amount);
        TextInputEditText etNote   = view.findViewById(R.id.et_payment_note);

        if (tvName != null) tvName.setText(name);
        if (tvDebt != null) tvDebt.setText(String.format(Locale.getDefault(),
            isCustomer ? "الدين الحالي: %.2f %s" : "المستحق للمورد: %.2f %s",
            currentDebt, currency));

        new MaterialAlertDialogBuilder(this)
            .setTitle(isCustomer ? "سداد دين العميل" : "سداد دين للمورد")
            .setView(view)
            .setPositiveButton("تأكيد السداد", (d, w) -> {
                double amount = parseAmount(etAmount);
                if (amount <= 0) { showToast("يرجى إدخال مبلغ صحيح"); return; }
                if (amount > currentDebt) { showToast("المبلغ أكبر من الدين الحالي"); return; }
                String note = etNote != null && etNote.getText() != null
                    ? etNote.getText().toString().trim() : "";

                boolean ok = isCustomer
                    ? dbHelper.settleCustomerDebt(id, amount, note)
                    : dbHelper.settleSupplierDebt(id, amount, note);

                if (ok) {
                    showSnackbar(String.format(Locale.getDefault(),
                        "✓ تم السداد %.2f %s", amount, currency));
                    loadAllData();
                } else {
                    showSnackbar("فشل في تسجيل السداد", true);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showAddDebtDialog(String id, String name, boolean isCustomer) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settle_debt, null, false);
        TextView tvName = view.findViewById(R.id.tv_dialog_customer_name);
        TextView tvDebt = view.findViewById(R.id.tv_dialog_current_debt);
        TextInputEditText etAmount = view.findViewById(R.id.et_payment_amount);
        TextInputEditText etNote   = view.findViewById(R.id.et_payment_note);

        if (tvName != null) tvName.setText(name);
        double curr = isCustomer ? dbHelper.getCustomerDebt(id) : dbHelper.getSupplierDebt(id);
        if (tvDebt != null) tvDebt.setText(String.format(Locale.getDefault(),
            "الرصيد الحالي: %.2f %s", curr, currency));

        new MaterialAlertDialogBuilder(this)
            .setTitle(isCustomer ? "تسجيل دين جديد على العميل" : "تسجيل مشتريات بالآجل من المورد")
            .setView(view)
            .setPositiveButton("تسجيل", (d, w) -> {
                double amount = parseAmount(etAmount);
                if (amount <= 0) { showToast("يرجى إدخال مبلغ صحيح"); return; }
                String note = etNote != null && etNote.getText() != null
                    ? etNote.getText().toString().trim() : "";

                boolean ok = isCustomer
                    ? dbHelper.addCustomerDebt(id, amount, note)
                    : dbHelper.addSupplierDebt(id, amount, note);

                if (ok) {
                    showSnackbar(String.format(Locale.getDefault(),
                        "✓ تم تسجيل الدين %.2f %s", amount, currency));
                    loadAllData();
                } else {
                    showSnackbar("فشل في تسجيل الدين", true);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showPaymentHistory(String id, String name, boolean isCustomer) {
        List<HashMap<String, String>> payments = isCustomer
            ? dbHelper.getCustomerDebtPayments(id)
            : dbHelper.getSupplierDebtPayments(id);

        if (payments.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("سجل الحركات — " + name)
                .setMessage("لا توجد حركات مسجلة بعد.")
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (HashMap<String, String> p : payments) {
            String type   = safeGet(p, "type");
            String amount = safeGet(p, "amount");
            String note   = safeGet(p, "note");
            String date   = safeGet(p, "created_at");
            if (date.length() > 16) date = date.substring(0, 16);

            boolean isPayment = "payment".equals(type);
            sb.append(isPayment ? "✓ سداد  " : "✗ دين   ");
            try {
                sb.append(String.format(Locale.getDefault(), "%.2f %s",
                    Double.parseDouble(amount), currency));
            } catch (Exception e) { sb.append(amount).append(" ").append(currency); }
            sb.append("  |  ").append(date);
            if (!note.isEmpty()) sb.append("\n   ملاحظة: ").append(note);
            sb.append("\n\n");
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("سجل الحركات — " + name)
            .setMessage(sb.toString().trim())
            .setPositiveButton(R.string.close, null)
            .show();
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    private double parseAmount(TextInputEditText et) {
        if (et == null || et.getText() == null) return 0;
        try { return Double.parseDouble(et.getText().toString().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void dialPhone(String phone) {
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); }
        catch (Exception ignored) {}
    }

    private String safeGet(HashMap<String, String> map, String key) {
        if (map == null) return "";
        String v = map.get(key);
        return v != null ? v : "";
    }

    // ─────────────────────────────────────────────────────────────
    //  Customer Adapter
    // ─────────────────────────────────────────────────────────────

    private class CustomerDebtAdapter extends RecyclerView.Adapter<CustomerDebtAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_debt, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> item = filteredCustomers.get(pos);
            String name  = safeGet(item, "name");
            String phone = safeGet(item, "phone");

            if (h.tvName  != null) h.tvName.setText(name);
            if (h.tvPhone != null) h.tvPhone.setText(phone.isEmpty() ? "لا يوجد رقم" : phone);
            if (h.tvDebt  != null) {
                try {
                    h.tvDebt.setText(String.format(Locale.getDefault(),
                        "%.2f %s", Double.parseDouble(safeGet(item, "debt")), currency));
                } catch (NumberFormatException e) { h.tvDebt.setText("0.00 " + currency); }
            }
            h.itemView.setOnClickListener(v -> showCustomerDebtDialog(item));
            if (h.btnCall != null) {
                h.btnCall.setVisibility(phone.isEmpty() ? View.GONE : View.VISIBLE);
                h.btnCall.setOnClickListener(v -> dialPhone(phone));
            }
        }

        @Override public int getItemCount() { return filteredCustomers.size(); }

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

    // ─────────────────────────────────────────────────────────────
    //  Supplier Debt Adapter  (reuses item_debt layout)
    // ─────────────────────────────────────────────────────────────

    private class SupplierDebtAdapter extends RecyclerView.Adapter<SupplierDebtAdapter.VH> {

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_debt, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> item = filteredSuppliers.get(pos);
            String name    = safeGet(item, "name");
            String company = safeGet(item, "company");
            String phone   = safeGet(item, "phone");

            if (h.tvName  != null) h.tvName.setText(name);
            if (h.tvPhone != null) {
                String sub = company.isEmpty() ? phone : (company + (phone.isEmpty() ? "" : " · " + phone));
                h.tvPhone.setText(sub.isEmpty() ? "مورد" : sub);
            }
            if (h.tvDebt != null) {
                try {
                    h.tvDebt.setText(String.format(Locale.getDefault(),
                        "%.2f %s", Double.parseDouble(safeGet(item, "debt")), currency));
                } catch (NumberFormatException e) { h.tvDebt.setText("0.00 " + currency); }
            }
            h.itemView.setOnClickListener(v -> showSupplierDebtDialog(item));
            if (h.btnCall != null) {
                h.btnCall.setVisibility(phone.isEmpty() ? View.GONE : View.VISIBLE);
                h.btnCall.setOnClickListener(v -> dialPhone(phone));
            }
        }

        @Override public int getItemCount() { return filteredSuppliers.size(); }

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
