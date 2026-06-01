package com.pos.system;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * ActivityExpensesActivity - صفحة المصروفات
 *
 * ✅ استايل موحد + رفع المحتوى + تطبيق الثيم
 *
 * @version 3.0
 */
public class ActivityExpensesActivity extends BaseActivity {

    private RecyclerView recyclerExpenses;
    private ExpensesAdapter adapter;
    private DBHelper dbHelper;
    private ExtendedFloatingActionButton fabAddExpense;
    private TextView tvTotalExpenses, tvFilterDate;
    private View emptyState;

    private List<HashMap<String, String>> expensesList = new ArrayList<>();
    private String filterStartDate = "", filterEndDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses);

        applyWindowInsets(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFAB();
        loadExpenses();
    }

    private void initViews() {
        recyclerExpenses = findViewById(R.id.recycler_expenses);
        fabAddExpense    = findViewById(R.id.fab_add_expense);
        tvTotalExpenses  = findViewById(R.id.tv_total_expenses);
        tvFilterDate     = findViewById(R.id.tv_filter_date);
        emptyState       = findViewById(R.id.empty_state);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        View btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void setupRecyclerView() {
        if (recyclerExpenses == null) return;
        recyclerExpenses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExpensesAdapter();
        recyclerExpenses.setAdapter(adapter);

        // سحب لليسار للحذف
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv,
                @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder tgt) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                deleteExpense(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(recyclerExpenses);
    }

    private void setupFAB() {
        if (fabAddExpense != null) fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
    }

    private void loadExpenses() {
        expensesList.clear();
        try { expensesList.addAll(dbHelper.getAllExpenses()); }
        catch (Exception ignored) {}
        updateUI();
        calculateTotal();
    }

    private void updateUI() {
        boolean empty = expensesList.isEmpty();
        if (recyclerExpenses != null) recyclerExpenses.setVisibility(empty ? View.GONE  : View.VISIBLE);
        if (emptyState       != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty && adapter != null) adapter.notifyDataSetChanged();
    }

    private void calculateTotal() {
        double total = 0;
        for (HashMap<String, String> e : expensesList) {
            try {
                String amountStr = e.get("amount");
                if (amountStr != null && !amountStr.isEmpty())
                    total += Double.parseDouble(amountStr);
            } catch (NumberFormatException ignored) {}
        }
        if (tvTotalExpenses != null)
            tvTotalExpenses.setText(String.format(Locale.getDefault(), "%.2f ر.س", total));
    }

    private void showAddExpenseDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        TextInputEditText etCategory    = dialogView.findViewById(R.id.et_category);
        TextInputEditText etAmount      = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_description);
        TextInputEditText etDate        = dialogView.findViewById(R.id.et_date);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (etDate != null) etDate.setText(sdf.format(new Date()));

        if (etDate != null) {
            etDate.setOnClickListener(v -> {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(this, (view, year, month, day) -> {
                    cal.set(year, month, day);
                    etDate.setText(sdf.format(cal.getTime()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("إضافة مصروف جديد")
            .setView(dialogView)
            .setPositiveButton("إضافة", (dialog, which) -> {
                String category    = getText(etCategory);
                String amountStr   = getText(etAmount);
                String description = getText(etDescription);
                String date        = getText(etDate);

                if (category.isEmpty()) { showToast("يرجى إدخال التصنيف"); return; }
                if (amountStr.isEmpty()) { showToast("يرجى إدخال المبلغ"); return; }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) { showToast("المبلغ يجب أن يكون أكبر من صفر"); return; }

                    long result = dbHelper.addExpense(category, amount, description, date, "نقدي", "");
                    if (result > 0) {
                        showToast("✓ تمت إضافة المصروف");
                        loadExpenses();
                    } else {
                        showToast("فشل في إضافة المصروف");
                    }
                } catch (NumberFormatException e) {
                    showToast("المبلغ غير صحيح");
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private String getText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private void deleteExpense(int position) {
        if (position < 0 || position >= expensesList.size()) return;
        HashMap<String, String> expense = expensesList.get(position);

        new MaterialAlertDialogBuilder(this)
            .setTitle("حذف المصروف")
            .setMessage("هل أنت متأكد من حذف هذا المصروف؟")
            .setPositiveButton("نعم", (dialog, which) -> {
                try {
                    long id = Long.parseLong(expense.getOrDefault("id", "0"));
                    boolean deleted = dbHelper.deleteExpense(id);
                    if (deleted) {
                        expensesList.remove(position);
                        if (adapter != null) adapter.notifyItemRemoved(position);
                        calculateTotal();
                        updateUI();
                        showToast("✓ تم الحذف");
                    } else {
                        showToast("فشل في الحذف");
                    }
                } catch (Exception e) {
                    showToast("خطأ في الحذف");
                    if (adapter != null) adapter.notifyItemChanged(position);
                }
            })
            .setNegativeButton("لا", (dialog, which) -> {
                if (adapter != null) adapter.notifyItemChanged(position);
            })
            .show();
    }

    private void showFilterDialog() {
        String[] options = {"اليوم", "هذا الأسبوع", "هذا الشهر", "آخر 3 أشهر", "هذا العام", "إلغاء التصفية"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("تصفية المصروفات")
            .setItems(options, (dialog, which) -> {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                switch (which) {
                    case 0:
                        filterStartDate = sdf.format(calendar.getTime());
                        filterEndDate   = filterStartDate;
                        break;
                    case 1:
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                        filterStartDate = sdf.format(calendar.getTime());
                        calendar.add(Calendar.DAY_OF_WEEK, 6);
                        filterEndDate   = sdf.format(calendar.getTime());
                        break;
                    case 2:
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        filterStartDate = sdf.format(calendar.getTime());
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                        filterEndDate   = sdf.format(calendar.getTime());
                        break;
                    case 3:
                        calendar.add(Calendar.MONTH, -3);
                        filterStartDate = sdf.format(calendar.getTime());
                        filterEndDate   = sdf.format(new Date());
                        break;
                    case 4:
                        calendar.set(Calendar.DAY_OF_YEAR, 1);
                        filterStartDate = sdf.format(calendar.getTime());
                        calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
                        filterEndDate   = sdf.format(calendar.getTime());
                        break;
                    case 5:
                        filterStartDate = "";
                        filterEndDate   = "";
                        loadExpenses();
                        if (tvFilterDate != null) tvFilterDate.setVisibility(View.GONE);
                        return;
                }
                applyFilter();
            })
            .show();
    }

    private void applyFilter() {
        if (filterStartDate.isEmpty() || filterEndDate.isEmpty()) return;
        expensesList.clear();
        try {
            for (HashMap<String, String> expense : dbHelper.getAllExpenses()) {
                String date = expense.getOrDefault("date", "");
                if (date.compareTo(filterStartDate) >= 0 && date.compareTo(filterEndDate) <= 0) {
                    expensesList.add(expense);
                }
            }
        } catch (Exception ignored) {}
        updateUI();
        calculateTotal();
        if (tvFilterDate != null) {
            tvFilterDate.setText("من " + filterStartDate + " إلى " + filterEndDate);
            tvFilterDate.setVisibility(View.VISIBLE);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    private class ExpensesAdapter extends RecyclerView.Adapter<ExpensesAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            HashMap<String, String> expense = expensesList.get(position);
            if (holder.tvCategory != null)    holder.tvCategory.setText(expense.getOrDefault("category", ""));
            if (holder.tvDescription != null) holder.tvDescription.setText(expense.getOrDefault("description", ""));
            if (holder.tvDate != null)        holder.tvDate.setText(expense.getOrDefault("date", ""));
            if (holder.tvAmount != null) {
                try {
                    double amount = Double.parseDouble(expense.getOrDefault("amount", "0"));
                    holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f ر.س", amount));
                } catch (Exception e) { holder.tvAmount.setText("0.00 ر.س"); }
            }
            holder.itemView.setOnClickListener(v -> showExpenseDetails(expense));
        }

        @Override public int getItemCount() { return expensesList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvCategory, tvAmount, tvDescription, tvDate;
            VH(View v) {
                super(v);
                tvCategory    = v.findViewById(R.id.tv_category);
                tvAmount      = v.findViewById(R.id.tv_amount);
                tvDescription = v.findViewById(R.id.tv_description);
                tvDate        = v.findViewById(R.id.tv_date);
            }
        }
    }

    private void showExpenseDetails(HashMap<String, String> expense) {
        String details = "التصنيف: " + expense.getOrDefault("category", "") + "\n" +
                         "المبلغ: "   + expense.getOrDefault("amount", "0") + " ر.س\n" +
                         "الوصف: "    + expense.getOrDefault("description", "") + "\n" +
                         "التاريخ: "  + expense.getOrDefault("date", "");
        new MaterialAlertDialogBuilder(this)
            .setTitle("تفاصيل المصروف")
            .setMessage(details)
            .setPositiveButton("حسناً", null)
            .show();
    }
}
