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
            getSupportActionBar().setTitle("الأقساط");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InstallmentsAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setItemAnimator(null);

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("العقود"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("المتأخرة"));
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
                binding.tvSummary.setText("إجمالي المستحق: " + String.format(Locale.US, "%.2f %s", ft, getCurrency()));
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
            .setTitle("إضافة عقد تقسيط")
            .setView(dv)
            .setPositiveButton("حفظ", (d, w) -> {
                if (tilCustomerName != null) tilCustomerName.setError(null);
                if (tilTotalAmount != null) tilTotalAmount.setError(null);

                String name   = etCustomerName != null && etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
                String totStr = etTotalAmount  != null && etTotalAmount.getText()  != null ? etTotalAmount.getText().toString().trim()  : "";
                String dwnStr = etDownPayment  != null && etDownPayment.getText()  != null ? etDownPayment.getText().toString().trim()  : "0";
                String cntStr = etInstallmentCount != null && etInstallmentCount.getText() != null ? etInstallmentCount.getText().toString().trim() : "1";
                String start  = etStartDate    != null && etStartDate.getText()    != null ? etStartDate.getText().toString().trim()    : "";
                String notes  = etNotes        != null && etNotes.getText()        != null ? etNotes.getText().toString().trim()        : "";

                if (name.isEmpty()) {
                    if (tilCustomerName != null) tilCustomerName.setError("اسم العميل مطلوب");
                    return;
                }
                if (totStr.isEmpty()) {
                    if (tilTotalAmount != null) tilTotalAmount.setError("المبلغ الإجمالي مطلوب");
                    return;
                }
                double tot = 0, dwn = 0;
                int cnt = 1;
                try {
                    tot = Double.parseDouble(totStr);
                    dwn = Double.parseDouble(dwnStr.isEmpty() ? "0" : dwnStr);
                    cnt = Integer.parseInt(cntStr.isEmpty() ? "1" : cntStr);
                } catch (Exception ex) {
                    if (tilTotalAmount != null) tilTotalAmount.setError("بيانات غير صحيحة");
                    return;
                }
                if (tot <= 0) { showToast("المبلغ الإجمالي يجب أن يكون أكبر من صفر"); return; }
                if (dwn >= tot) { showToast("الدفعة الأولى لا تصح أن تساوي أو تتجاوز الإجمالي"); return; }
                if (cnt <= 0 || cnt > 120) { showToast("عدد الأقساط يجب أن يكون بين 1 و 120"); return; }
                double finalTot = tot, finalDwn = dwn;
                int finalCnt = cnt;
                executor.execute(() -> {
                    long id = dbHelper.createInstallmentContract(0, name, finalTot, finalDwn, finalCnt, start, notes);
                    runOnUiThread(() -> {
                        if (id > 0) { showToast("تم إنشاء العقد بنجاح"); loadData(); }
                        else showSnackbar("حدث خطأ أثناء الحفظ", true);
                    });
                });
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showContractDetails(HashMap<String, String> contract) {
        long cid = Long.parseLong(contract.getOrDefault("id", "0"));
        executor.execute(() -> {
            List<HashMap<String, String>> payments = dbHelper.getContractPayments(cid);
            StringBuilder sb = new StringBuilder();
            sb.append("العميل: ").append(contract.getOrDefault("customer_name", "—")).append("\n");
            sb.append("الإجمالي: ").append(contract.getOrDefault("total_amount", "0")).append(" ").append(getCurrency()).append("\n");
            sb.append("المسدد: ").append(contract.getOrDefault("paid_amount", "0")).append(" ").append(getCurrency()).append("\n");
            sb.append("المتبقي: ").append(contract.getOrDefault("remaining_amount", "0")).append(" ").append(getCurrency()).append("\n\n");
            sb.append("جدول الأقساط:\n");
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
                        opts[i] = "دفع القسط " + pp.getOrDefault("installment_number", "") +
                                  "  (" + pp.getOrDefault("due_date", "") + ")  " +
                                  pp.getOrDefault("amount", "") + " " + getCurrency();
                    }
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("تفاصيل العقد")
                        .setMessage(sb.toString())
                        .setPositiveButton("دفع قسط", (d2, w2) ->
                            new MaterialAlertDialogBuilder(this)
                                .setTitle("اختر القسط")
                                .setItems(opts, (d3, which) -> {
                                    long pid = Long.parseLong(pending.get(which).getOrDefault("id", "0"));
                                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                                    executor.execute(() -> {
                                        boolean ok = dbHelper.payInstallment(pid, today);
                                        runOnUiThread(() -> { if (ok) { showToast("تم الدفع بنجاح"); loadData(); } else showToast("خطأ في الدفع"); });
                                    });
                                })
                                .show())
                        .setNegativeButton("إغلاق", null)
                        .show();
                } else {
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("تفاصيل العقد")
                        .setMessage(sb.toString())
                        .setPositiveButton("إغلاق", null)
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
            HashMap<String, String> item = dataList.get(pos);
            String currency = getCurrency();
            if (currentTab == 0) {
                h.tvTitle.setText(item.getOrDefault("customer_name", "—"));
                h.tvSubtitle.setText("رقم العقد: " + item.getOrDefault("contract_number", "—"));
                h.tvAmount.setText(String.format(Locale.US, "%.2f %s",
                    parseD(item.getOrDefault("total_amount", "0")), currency));
                h.tvInfo.setText("متبقي: " + String.format(Locale.US, "%.2f",
                    parseD(item.getOrDefault("remaining_amount", "0"))) +
                    " | أقساط: " + item.getOrDefault("installment_count", "0"));
                String status = item.getOrDefault("status", "active");
                h.tvStatus.setText(statusAr(status));
                h.tvStatus.setTextColor("completed".equals(status) ? 0xFF4CAF50 : "overdue".equals(status) ? 0xFFF44336 : 0xFF2196F3);
                h.itemView.setOnClickListener(v -> showContractDetails(item));
            } else {
                h.tvTitle.setText(item.getOrDefault("customer_name", "—"));
                h.tvSubtitle.setText("عقد: " + item.getOrDefault("contract_number", "—") +
                    " - قسط رقم: " + item.getOrDefault("installment_number", ""));
                h.tvAmount.setText(String.format(Locale.US, "%.2f %s",
                    parseD(item.getOrDefault("amount", "0")), currency));
                h.tvInfo.setText("استحقاق: " + item.getOrDefault("due_date", "—"));
                h.tvStatus.setText("متأخر");
                h.tvStatus.setTextColor(0xFFF44336);
                h.itemView.setOnClickListener(v -> {
                    long pid = Long.parseLong(item.getOrDefault("id", "0"));
                    String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
                    new MaterialAlertDialogBuilder(ActivityInstallmentsActivity.this)
                        .setTitle("دفع القسط")
                        .setMessage("دفع " + item.getOrDefault("amount","") + " " + currency +
                            "\nالعميل: " + item.getOrDefault("customer_name",""))
                        .setPositiveButton("تأكيد الدفع", (d, w) ->
                            executor.execute(() -> {
                                boolean ok = dbHelper.payInstallment(pid, today);
                                runOnUiThread(() -> { if (ok) { showToast("تم الدفع"); loadData(); } else showToast("خطأ"); });
                            }))
                        .setNegativeButton("إلغاء", null)
                        .show();
                });
            }
        }

        @Override public int getItemCount() { return dataList.size(); }

        private double parseD(String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
        }

        private String statusAr(String s) {
            switch (s) {
                case "completed": return "مكتمل";
                case "overdue":   return "متأخر";
                default:          return "نشط";
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
