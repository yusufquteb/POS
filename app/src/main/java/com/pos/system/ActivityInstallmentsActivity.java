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
import com.pos.system.databinding.ActivityInstallmentsBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityInstallmentsActivity extends BaseActivity {

    private ActivityInstallmentsBinding binding;
    private DBHelper   dbHelper;

    private final List<HashMap<String, String>> dataList = new ArrayList<>();
    private InstallmentsAdapter adapter;
    private int currentTab = 0;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInstallmentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.coordinatorRoot);
        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        loadData();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.hub_installments);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InstallmentsAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setItemAnimator(null);

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.installments_tab_contracts));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.installments_tab_overdue));
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.fabAdd.setOnClickListener(v -> showAddContractDialog());
        binding.btnEmptyAddInstallment.setOnClickListener(v -> showAddContractDialog());

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) binding.fabAdd.shrink();
                else if (dy < 0) binding.fabAdd.extend();
            }
        });
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<HashMap<String, String>> data = currentTab == 0
                ? dbHelper.getAllInstallmentContracts()
                : dbHelper.getOverdueInstallments();
            double total = dbHelper.getTotalInstallmentsReceivable();
            final List<HashMap<String, String>> fd = data;
            final double ft = total;
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                dataList.clear();
                dataList.addAll(fd);
                adapter.notifyDataSetChanged();
                binding.tvEmpty.setVisibility(dataList.isEmpty() ? View.VISIBLE : View.GONE);
                binding.tvSummary.setText(getString(R.string.total_receivable_label, ft, getCurrency()));
            });
        });
    }

    private String getCurrency() {
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            return s != null ? s.getOrDefault("currency", "ج.م") : "ج.م";
        } catch (Exception e) { return "ج.م"; }
    }

    private void showAddContractDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_add_installment, null);
        TextInputLayout tilCustomerName  = dv.findViewById(R.id.til_customer_name);
        TextInputLayout tilTotalAmount   = dv.findViewById(R.id.til_total_amount);
        TextInputEditText etCustomerName    = dv.findViewById(R.id.et_customer_name);
        TextInputEditText etTotalAmount     = dv.findViewById(R.id.et_total_amount);
        TextInputEditText etDownPayment     = dv.findViewById(R.id.et_down_payment);
        TextInputEditText etInstallmentCount = dv.findViewById(R.id.et_installment_count);
        TextInputEditText etStartDate       = dv.findViewById(R.id.et_start_date);
        TextInputEditText etNotes           = dv.findViewById(R.id.et_notes);

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_installment_contract_title)
            .setView(dv)
            .setPositiveButton(R.string.save, (d, w) -> {
                if (tilCustomerName != null) tilCustomerName.setError(null);
                if (tilTotalAmount != null) tilTotalAmount.setError(null);

                String name   = etCustomerName != null && etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
                String totStr = etTotalAmount  != null && etTotalAmount.getText()  != null ? etTotalAmount.getText().toString().trim()  : "";
                String dwnStr = etDownPayment  != null && etDownPayment.getText()  != null ? etDownPayment.getText().toString().trim()  : "0";
                String cntStr = etInstallmentCount != null && etInstallmentCount.getText() != null ? etInstallmentCount.getText().toString().trim() : "1";
                String start  = etStartDate    != null && etStartDate.getText()    != null ? etStartDate.getText().toString().trim()    : "";
                String notes  = etNotes        != null && etNotes.getText()        != null ? etNotes.getText().toString().trim()        : "";

                if (name.isEmpty()) {
                    if (tilCustomerName != null) tilCustomerName.setError(getString(R.string.customer_name_required));
                    return;
                }
                if (totStr.isEmpty()) {
                    if (tilTotalAmount != null) tilTotalAmount.setError(getString(R.string.total_amount_required));
                    return;
                }
                double tot = 0, dwn = 0;
                int cnt = 1;
                try {
                    tot = Double.parseDouble(totStr);
                    dwn = Double.parseDouble(dwnStr.isEmpty() ? "0" : dwnStr);
                    cnt = Integer.parseInt(cntStr.isEmpty() ? "1" : cntStr);
                } catch (Exception ex) {
                    if (tilTotalAmount != null) tilTotalAmount.setError(getString(R.string.invalid_data));
                    return;
                }
                if (tot <= 0) { showToast(getString(R.string.total_amount_must_be_positive)); return; }
                if (dwn >= tot) { showToast(getString(R.string.down_payment_exceeds_total)); return; }
                if (cnt <= 0 || cnt > 120) { showToast(getString(R.string.installment_count_range_error)); return; }
                double finalTot = tot, finalDwn = dwn;
                int finalCnt = cnt;
                executor.execute(() -> {
                    long id = dbHelper.createInstallmentContract(0, name, finalTot, finalDwn, finalCnt, start, notes);
                    runOnUiThread(() -> {
                        if (id > 0) { showToast(getString(R.string.contract_created_success)); loadData(); }
                        else showSnackbar(getString(R.string.save_failed), true);
                    });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showContractDetails(HashMap<String, String> contract) {
        long cid;
        try {
            cid = Long.parseLong(contract.getOrDefault("id", "0"));
        } catch (NumberFormatException e) {
            showSnackbar(getString(R.string.invalid_contract_data), true);
            return;
        }
        executor.execute(() -> {
            List<HashMap<String, String>> payments = dbHelper.getContractPayments(cid);
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.contract_details_customer, contract.getOrDefault("customer_name", "—"))).append("\n");
            sb.append(getString(R.string.contract_details_total, contract.getOrDefault("total_amount", "0"), getCurrency())).append("\n");
            sb.append(getString(R.string.contract_details_paid, contract.getOrDefault("paid_amount", "0"), getCurrency())).append("\n");
            sb.append(getString(R.string.contract_details_remaining, contract.getOrDefault("remaining_amount", "0"), getCurrency())).append("\n\n");
            sb.append(getString(R.string.installments_schedule_label)).append("\n");
            List<HashMap<String, String>> pending = new ArrayList<>();
            for (HashMap<String, String> p : payments) {
                String s = p.getOrDefault("status", "pending");
                sb.append(p.getOrDefault("installment_number", "")).append(". ")
                  .append(p.getOrDefault("due_date", "")).append(" - ")
                  .append(p.getOrDefault("amount", "0")).append(" ")
                  .append("paid".equals(s) ? "✓" : "late".equals(s) ? "⚠" : "⏳").append("\n");
                if ("pending".equals(s) || "late".equals(s)) pending.add(p);
            }
            runOnUiThread(() -> {
                if (!pending.isEmpty()) {
                    String[] opts = new String[pending.size()];
                    for (int i = 0; i < pending.size(); i++) {
                        HashMap<String, String> pp = pending.get(i);
                        opts[i] = getString(R.string.pay_installment_option,
                                  pp.getOrDefault("installment_number", ""),
                                  pp.getOrDefault("due_date", ""),
                                  pp.getOrDefault("amount", ""), getCurrency());
                    }
                    new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.contract_details_title)
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.pay_installment_btn, (d2, w2) ->
                            new MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.select_installment_title)
                                .setItems(opts, (d3, which) -> {
                                    long pid;
                                    try {
                                        pid = Long.parseLong(pending.get(which).getOrDefault("id", "0"));
                                    } catch (NumberFormatException e) {
                                        showSnackbar(getString(R.string.invalid_installment_data), true);
                                        return;
                                    }
                                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                                    executor.execute(() -> {
                                        boolean ok = dbHelper.payInstallment(pid, today);
                                        runOnUiThread(() -> { if (ok) { showToast(getString(R.string.payment_success)); loadData(); } else showSnackbar(getString(R.string.payment_error), true); });
                                    });
                                })
                                .show())
                        .setNegativeButton(R.string.close, null)
                        .show();
                } else {
                    new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.contract_details_title)
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.close, null)
                        .show();
                }
            });
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // ──────────────────────────────────────────────────────────────────
    private class InstallmentsAdapter extends RecyclerView.Adapter<InstallmentsAdapter.VH> {

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_installment, parent, false));
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            if (pos < 0 || pos >= dataList.size()) return;
            HashMap<String, String> item = dataList.get(pos);
            String currency = getCurrency();
            android.content.Context ctx = h.itemView.getContext();
            if (currentTab == 0) {
                h.tvTitle.setText(item.getOrDefault("customer_name", "—"));
                h.tvSubtitle.setText(ctx.getString(R.string.contract_number_label, item.getOrDefault("contract_number", "—")));
                h.tvAmount.setText(String.format(Locale.US, "%.2f %s",
                    parseD(item.getOrDefault("total_amount", "0")), currency));
                h.tvInfo.setText(ctx.getString(R.string.remaining_installments_label,
                    String.format(Locale.US, "%.2f", parseD(item.getOrDefault("remaining_amount", "0"))),
                    item.getOrDefault("installment_count", "0")));
                String status = item.getOrDefault("status", "active");
                h.tvStatus.setText(statusLabel(ctx, status));
                h.tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ctx,
                    "completed".equals(status) ? R.color.color_success : "overdue".equals(status) ? R.color.color_error : R.color.color_info));
                h.itemView.setOnClickListener(v -> showContractDetails(item));
            } else {
                h.tvTitle.setText(item.getOrDefault("customer_name", "—"));
                h.tvSubtitle.setText(ctx.getString(R.string.contract_installment_label,
                    item.getOrDefault("contract_number", "—"), item.getOrDefault("installment_number", "")));
                h.tvAmount.setText(String.format(Locale.US, "%.2f %s",
                    parseD(item.getOrDefault("amount", "0")), currency));
                h.tvInfo.setText(ctx.getString(R.string.due_date_label, item.getOrDefault("due_date", "—")));
                h.tvStatus.setText(R.string.status_overdue);
                h.tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.color_error));
                h.itemView.setOnClickListener(v -> {
                    long pid = Long.parseLong(item.getOrDefault("id", "0"));
                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                    new MaterialAlertDialogBuilder(ActivityInstallmentsActivity.this)
                        .setTitle(R.string.pay_installment_dialog_title)
                        .setMessage(ctx.getString(R.string.pay_installment_message,
                            item.getOrDefault("amount",""), currency, item.getOrDefault("customer_name","")))
                        .setPositiveButton(R.string.confirm_payment_btn, (d, w) ->
                            executor.execute(() -> {
                                boolean ok = dbHelper.payInstallment(pid, today);
                                runOnUiThread(() -> { if (ok) { showToast(getString(R.string.payment_done)); loadData(); } else showSnackbar(getString(R.string.error_generic), true); });
                            }))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                });
            }
        }

        @Override public int getItemCount() { return dataList.size(); }

        private double parseD(String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
        }

        private String statusLabel(android.content.Context ctx, String s) {
            switch (s) {
                case "completed": return ctx.getString(R.string.status_completed);
                case "overdue":   return ctx.getString(R.string.status_overdue);
                default:          return ctx.getString(R.string.status_active);
            }
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvAmount, tvInfo, tvStatus;
            VH(View v) {
                super(v);
                tvTitle    = v.findViewById(R.id.tv_title);
                tvSubtitle = v.findViewById(R.id.tv_subtitle);
                tvAmount   = v.findViewById(R.id.tv_amount);
                tvInfo     = v.findViewById(R.id.tv_info);
                tvStatus   = v.findViewById(R.id.tv_status);
            }
        }
    }
}
