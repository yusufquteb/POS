package com.pos.system;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import com.pos.system.databinding.ActivityCustomersBinding;

/**
 * ActivityCustomersActivity - صفحة العملاء
 *
 * ✅ الإصلاحات:
 * - استايل موحد مع صفحة Cart وSettings (Material 3)
 * - رفع المحتوى فوق NavigationBar
 * - تطبيق الثيم تلقائياً
 * - إصلاح نقل بيانات التعديل
 *
 * @version 3.0
 */
public class ActivityCustomersActivity extends BaseActivity {

    private ActivityCustomersBinding binding;


    private DBHelper dbHelper;
    private RecyclerView recyclerView;
    private CustomerAdapter adapter;
    private ArrayList<HashMap<String, Object>> fullList = new ArrayList<>();
    private View emptyState;
    private TextInputEditText etSearch;
    private String currency = "ج.م";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // رفع المحتوى فوق NavigationBar
        applyWindowInsets(binding.getRoot());

        dbHelper    = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}
        recyclerView = binding.recyclerView;
        emptyState   = binding.emptyState;
        etSearch     = binding.etSearch;

        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshData();

        Snackbar.make(recyclerView, getString(R.string.swipe_delete_edit), Snackbar.LENGTH_LONG).show();

        View fabAdd = binding.fabAdd;
        if (fabAdd != null) fabAdd.setOnClickListener(v -> {
            if (!FeatureGate.canAddCustomer(this)) {
                FeatureGate.showCustomerLimitDialog(this);
            } else {
                showDataSheet(null);
            }
        });
        if (binding.btnEmptyCta != null) binding.btnEmptyCta.setOnClickListener(v -> {
            if (fabAdd != null) fabAdd.performClick();
        });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        setupSwipeActions();
    }

    private void showDataSheet(HashMap<String, Object> editData) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_add_data_bottomdialog_fragment, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        TextInputEditText etName    = view.findViewById(R.id.et_name);
        TextInputEditText etPhone   = view.findViewById(R.id.et_phone);
        TextInputEditText etAddress = view.findViewById(R.id.et_address);
        TextInputEditText etNotes   = view.findViewById(R.id.et_notes);
        MaterialButton btnSave      = view.findViewById(R.id.btn_save);
        TextView tvTitle            = view.findViewById(R.id.tv_title);

        boolean isEdit = editData != null;

        if (tvTitle != null) tvTitle.setText(isEdit ? "تعديل عميل" : "إضافة عميل جديد");

        // ✅ إصلاح: تعبئة الحقول بالبيانات القديمة عند التعديل
        if (isEdit) {
            setText(etName,    editData, "name");
            setText(etPhone,   editData, "phone");
            setText(etAddress, editData, "address");
            setText(etNotes,   editData, "notes");
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name    = getEditText(etName);
                String phone   = getEditText(etPhone);
                String address = getEditText(etAddress);
                String notes   = getEditText(etNotes);

                if (name.isEmpty()) {
                    showToast("يرجى إدخال الاسم");
                    return;
                }

                try {
                    if (isEdit) {
                        long id = getLong(editData, "id");
                        dbHelper.updateCustomer(id, name, phone, address, notes);
                        showToast("✓ تم التعديل بنجاح");
                    } else {
                        dbHelper.addCustomer(name, phone, address, notes);
                        showToast("✓ تمت الإضافة بنجاح");
                    }
                    dialog.dismiss();
                    refreshData();
                } catch (Exception e) {
                    showToast("خطأ: " + e.getMessage());
                }
            });
        }

        dialog.show();
    }

    private void setText(TextInputEditText et, HashMap<String, Object> data, String key) {
        if (et == null || data == null) return;
        Object val = data.get(key);
        et.setText(val != null ? val.toString() : "");
    }

    private String getEditText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private long getLong(HashMap<String, Object> data, String key) {
        if (data == null) return 0;
        Object val = data.get(key);
        if (val == null) return 0;
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0; }
    }

    private void refreshData() {
        fullList.clear();
        try {
            java.util.List<HashMap<String, String>> list = dbHelper.getAllCustomers();
            for (HashMap<String, String> row : list) {
                HashMap<String, Object> obj = new HashMap<>();
                obj.putAll(row);
                fullList.add(obj);
            }
        } catch (Exception ignored) {}
        updateAdapter(fullList);
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            updateAdapter(fullList);
            return;
        }
        String lower = query.toLowerCase();
        ArrayList<HashMap<String, Object>> filtered = new ArrayList<>();
        for (HashMap<String, Object> row : fullList) {
            Object name  = row.get("name");
            Object phone = row.get("phone");
            if ((name  != null && name.toString().toLowerCase().contains(lower)) ||
                (phone != null && phone.toString().contains(lower))) {
                filtered.add(row);
            }
        }
        updateAdapter(filtered);
    }

    private void updateAdapter(ArrayList<HashMap<String, Object>> data) {
        boolean empty = data.isEmpty();
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyState   != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        adapter = new CustomerAdapter(data);
        if (recyclerView != null) recyclerView.setAdapter(adapter);
    }

    private void setupSwipeActions() {
        if (recyclerView == null) return;
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView rv,
                @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder tgt) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (adapter == null || pos < 0) return;
                HashMap<String, Object> item = adapter.getItem(pos);
                if (direction == ItemTouchHelper.LEFT) {
                    confirmDelete(item, pos);
                } else {
                    showDataSheet(item);
                    if (adapter != null) adapter.notifyItemChanged(pos);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void confirmDelete(HashMap<String, Object> item, int pos) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("حذف العميل")
            .setMessage("هل تريد حذف: " + item.getOrDefault("name", "") + "؟")
            .setPositiveButton(R.string.delete, (d, w) -> {
                try {
                    long id = getLong(item, "id");
                    dbHelper.deleteCustomer(id);
                    refreshData();
                    showToast("✓ تم الحذف");
                } catch (Exception e) {
                    showSnackbar("خطأ في الحذف", true);
                }
            })
            .setNegativeButton(R.string.cancel, (d, w) -> {
                if (adapter != null) adapter.notifyItemChanged(pos);
            })
            .show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ═══ Adapter ═══
    private class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.VH> {
        private final ArrayList<HashMap<String, Object>> data;
        CustomerAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }
        HashMap<String, Object> getItem(int pos) { return data.get(pos); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (position < 0 || position >= data.size()) return;
            HashMap<String, Object> item = data.get(position);
            if (holder.tvName  != null) holder.tvName.setText(str(item, "name"));
            if (holder.tvPhone != null) {
                String phone = str(item, "phone");
                holder.tvPhone.setText(phone.isEmpty() ? getString(R.string.customers_no_phone) : phone);
            }

            // إجمالي المشتريات
            if (holder.tvTotalSpent != null) {
                double spent = safeDouble(item.get("total_spent"));
                if (spent > 0) {
                    holder.tvTotalSpent.setText(String.format("إجمالي: %.0f %s", spent, currency));
                    holder.tvTotalSpent.setVisibility(android.view.View.VISIBLE);
                } else {
                    holder.tvTotalSpent.setVisibility(android.view.View.GONE);
                }
            }

            // شارة الديون
            if (holder.tvDebtBadge != null) {
                double debt = safeDouble(item.get("debt"));
                if (debt > 0.01) {
                    holder.tvDebtBadge.setText(String.format("دين: %.0f %s", debt, currency));
                    holder.tvDebtBadge.setVisibility(android.view.View.VISIBLE);
                } else {
                    holder.tvDebtBadge.setVisibility(android.view.View.GONE);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                String[] options = {"عرض الحساب", "تعديل البيانات"};
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(ActivityCustomersActivity.this)
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            long cid = 0;
                            try { cid = Long.parseLong(str(item,"id")); } catch (Exception ignored) {}
                            android.content.Intent i2 = new android.content.Intent(ActivityCustomersActivity.this,
                                ActivityCustomerAccountsActivity.class);
                            i2.putExtra("customer_id", cid);
                            i2.putExtra("customer_name", str(item, "name"));
                            startActivity(i2);
                        } else {
                            showDataSheet(item);
                        }
                    }).show();
            });

            // الاتصال
            if (holder.btnCall != null) {
                holder.btnCall.setOnClickListener(v -> {
                    String phone = str(item, "phone");
                    if (!phone.isEmpty()) {
                        try {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                        } catch (Exception ignored) {}
                    }
                });
            }
        }

        private String str(HashMap<String, Object> map, String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : "";
        }

        private double safeDouble(Object o) {
            if (o instanceof Double)  return (double) o;
            if (o instanceof Float)   return ((Float) o).doubleValue();
            if (o instanceof Integer) return ((Integer) o).doubleValue();
            if (o instanceof Long)    return ((Long) o).doubleValue();
            if (o instanceof String)  { try { return Double.parseDouble((String) o); } catch (Exception ignored) {} }
            return 0.0;
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvTotalSpent, tvDebtBadge;
            View btnCall;
            VH(View v) {
                super(v);
                tvName       = v.findViewById(R.id.tv_name);
                tvPhone      = v.findViewById(R.id.tv_phone);
                tvTotalSpent = v.findViewById(R.id.tv_total_spent);
                tvDebtBadge  = v.findViewById(R.id.tv_debt_badge);
                btnCall      = v.findViewById(R.id.btn_call);
            }
        }
    }
}
