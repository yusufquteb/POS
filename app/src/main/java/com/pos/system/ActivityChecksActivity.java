package com.pos.system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pos.system.databinding.ActivityChecksBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityChecksActivity extends BaseActivity {

    private static final String TAG = "ActivityChecksActivity";

    private ActivityChecksBinding binding;
    private DBHelper dbHelper;

    private List<HashMap<String, String>> checksList = new ArrayList<>();
    private ChecksAdapter adapter;
    private boolean isCustomerTab = true;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChecksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.coordinatorRoot);

        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar("الشيكات");
        loadData();
    }

    private void initViews() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChecksAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setItemAnimator(null);

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("شيكات العملاء"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("شيكات الموردين"));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                isCustomerTab = tab.getPosition() == 0;
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.fabAdd.setOnClickListener(v -> showAddCheckDialog());
        binding.btnEmptyAddCheck.setOnClickListener(v -> showAddCheckDialog());

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) binding.fabAdd.shrink();
                else if (dy < 0) binding.fabAdd.extend();
            }
        });
    }

    private void setupToolbar(String title) {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<HashMap<String, String>> data = isCustomerTab ?
                dbHelper.getAllCustomerChecks() : dbHelper.getAllSupplierChecks();
            double total = 0;
            for (HashMap<String, String> item : data) {
                if ("pending".equals(item.getOrDefault("status", ""))) {
                    try { total += Double.parseDouble(item.getOrDefault("amount", "0")); } catch (Exception ignored) {}
                }
            }
            final double finalTotal = total;
            final List<HashMap<String, String>> finalData = data;
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                checksList.clear();
                checksList.addAll(finalData);
                adapter.notifyDataSetChanged();
                binding.tvEmpty.setVisibility(checksList.isEmpty() ? View.VISIBLE : View.GONE);
                String currency = getCurrency();
                binding.tvTotalAmount.setText("إجمالي المعلق: " + String.format("%.2f %s", finalTotal, currency));
            });
        });
    }

    private String getCurrency() {
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            return s != null ? s.getOrDefault("currency", "ج.م") : "ج.م";
        } catch (Exception e) { return "ج.م"; }
    }

    private void showAddCheckDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_check, null);
        TextInputLayout tilName = dialogView.findViewById(R.id.til_name);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.til_amount);
        TextInputEditText etName = dialogView.findViewById(R.id.et_name);
        TextInputEditText etCheckNumber = dialogView.findViewById(R.id.et_check_number);
        TextInputEditText etBankName = dialogView.findViewById(R.id.et_bank_name);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etIssueDate = dialogView.findViewById(R.id.et_issue_date);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.et_due_date);
        TextInputEditText etNotes = dialogView.findViewById(R.id.et_notes);

        new MaterialAlertDialogBuilder(this)
            .setTitle(isCustomerTab ? "إضافة شيك عميل" : "إضافة شيك مورد")
            .setView(dialogView)
            .setPositiveButton(R.string.save, (d, w) -> {
                if (tilName != null) tilName.setError(null);
                if (tilAmount != null) tilAmount.setError(null);

                String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
                String checkNum = etCheckNumber != null && etCheckNumber.getText() != null ? etCheckNumber.getText().toString().trim() : "";
                String bank = etBankName != null && etBankName.getText() != null ? etBankName.getText().toString().trim() : "";
                String amountStr = etAmount != null && etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String issueDate = etIssueDate != null && etIssueDate.getText() != null ? etIssueDate.getText().toString().trim() : "";
                String dueDate = etDueDate != null && etDueDate.getText() != null ? etDueDate.getText().toString().trim() : "";
                String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    if (tilName != null) tilName.setError("الاسم مطلوب");
                    return;
                }
                if (amountStr.isEmpty()) {
                    if (tilAmount != null) tilAmount.setError("المبلغ مطلوب");
                    return;
                }
                double amount = 0;
                try { amount = Double.parseDouble(amountStr); } catch (Exception ex) {
                    if (tilAmount != null) tilAmount.setError("مبلغ غير صحيح");
                    return;
                }

                double finalAmount = amount;
                executor.execute(() -> {
                    long result;
                    if (isCustomerTab) {
                        result = dbHelper.addCustomerCheck(0, name, checkNum, bank, finalAmount, issueDate, dueDate, notes);
                    } else {
                        result = dbHelper.addSupplierCheck(0, name, checkNum, bank, finalAmount, issueDate, dueDate, notes);
                    }
                    if (result > 0) {
                        runOnUiThread(() -> { showToast(getString(R.string.saved_successfully)); loadData(); });
                    } else {
                        runOnUiThread(() -> showSnackbar("حدث خطأ", true));
                    }
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showCheckOptionsDialog(HashMap<String, String> check) {
        long id;
        try { id = Long.parseLong(check.getOrDefault("id", "0")); }
        catch (NumberFormatException e) { showToast(getString(R.string.error_unknown)); return; }
        String status = check.getOrDefault("status", "pending");
        String[] options;
        if ("pending".equals(status)) {
            options = new String[]{"تم التحصيل ✓", "مرتد ✗", "إلغاء", "حذف"};
        } else {
            options = new String[]{"حذف"};
        }
        new MaterialAlertDialogBuilder(this)
            .setTitle("خيارات الشيك")
            .setItems(options, (d, w) -> {
                executor.execute(() -> {
                    boolean success = false;
                    if ("pending".equals(status)) {
                        if (w == 0) success = isCustomerTab ? dbHelper.updateCustomerCheckStatus(id, "collected") : dbHelper.updateSupplierCheckStatus(id, "paid");
                        else if (w == 1) success = isCustomerTab ? dbHelper.updateCustomerCheckStatus(id, "bounced") : dbHelper.updateSupplierCheckStatus(id, "bounced");
                        else if (w == 2) success = isCustomerTab ? dbHelper.updateCustomerCheckStatus(id, "cancelled") : dbHelper.updateSupplierCheckStatus(id, "cancelled");
                        else if (w == 3) success = isCustomerTab ? dbHelper.deleteCustomerCheck(id) : (dbHelper.getWritableDatabase().delete("supplier_checks", "id=?", new String[]{String.valueOf(id)}) > 0);
                    } else {
                        success = isCustomerTab ? dbHelper.deleteCustomerCheck(id) : (dbHelper.getWritableDatabase().delete("supplier_checks", "id=?", new String[]{String.valueOf(id)}) > 0);
                    }
                    boolean finalSuccess = success;
                    runOnUiThread(() -> { if (finalSuccess) { showToast("تم بنجاح"); loadData(); } else showSnackbar("حدث خطأ", true); });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ── Adapter ──────────────────────────────────────────────────────
    private class ChecksAdapter extends RecyclerView.Adapter<ChecksAdapter.VH> {

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_check, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            if (position < 0 || position >= checksList.size()) return;
            HashMap<String, String> c = checksList.get(position);
            h.tvName.setText(c.getOrDefault(isCustomerTab ? "customer_name" : "supplier_name", "—"));
            h.tvCheckNumber.setText("شيك رقم: " + c.getOrDefault("check_number", "—"));
            h.tvBankName.setText("البنك: " + c.getOrDefault("bank_name", "—"));
            double amount = 0;
            try { amount = Double.parseDouble(c.getOrDefault("amount", "0")); } catch (Exception ignored) {}
            h.tvAmount.setText(String.format("%.2f %s", amount, getCurrency()));
            h.tvDueDate.setText("تاريخ الاستحقاق: " + c.getOrDefault("due_date", "—"));
            String status = c.getOrDefault("status", "pending");
            h.tvStatus.setText(getStatusAr(status));
            int color;
            android.content.Context ctx = h.itemView.getContext();
            switch (status) {
                case "collected": case "paid": color = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_success); break;
                case "bounced": color = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_error); break;
                case "cancelled": color = androidx.core.content.ContextCompat.getColor(ctx, R.color.gray_400); break;
                default: color = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_warning); break;
            }
            h.tvStatus.setTextColor(color);
            h.itemView.setOnClickListener(v -> showCheckOptionsDialog(c));
        }

        @Override public int getItemCount() { return checksList.size(); }

        private String getStatusAr(String status) {
            switch (status) {
                case "collected": return "محصّل";
                case "paid": return "مدفوع";
                case "bounced": return "مرتد";
                case "cancelled": return "ملغي";
                default: return "معلق";
            }
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvCheckNumber, tvBankName, tvAmount, tvDueDate, tvStatus;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvCheckNumber = v.findViewById(R.id.tv_check_number);
                tvBankName = v.findViewById(R.id.tv_bank_name);
                tvAmount = v.findViewById(R.id.tv_amount);
                tvDueDate = v.findViewById(R.id.tv_due_date);
                tvStatus = v.findViewById(R.id.tv_status);
            }
        }
    }
}
