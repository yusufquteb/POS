package com.pos.system;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import com.pos.system.databinding.ActivitySuppliersBinding;

/**
 * ActivitySuppliersActivity - صفحة الموردين
 *
 * ✅ استايل موحد + رفع المحتوى + إصلاح التعديل
 *
 * @version 3.0
 */
public class ActivitySuppliersActivity extends BaseActivity {

    private ActivitySuppliersBinding binding;


    private static final String TAG = "SuppliersActivity";

    private DBHelper dbHelper;
    private RecyclerView recyclerView;
    private SupplierAdapter adapter;
    private ArrayList<HashMap<String, Object>> fullList = new ArrayList<>();
    private View emptyState;
    private TextInputEditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuppliersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets(binding.getRoot());

        dbHelper     = new DBHelper(this);
        recyclerView  = binding.recyclerView;
        emptyState    = binding.emptyState;
        etSearch      = binding.etSearch;

        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshData();

        Snackbar.make(recyclerView, getString(R.string.swipe_delete_edit), Snackbar.LENGTH_LONG).show();

        View fabAdd = binding.fabAdd;
        if (fabAdd != null) fabAdd.setOnClickListener(v -> showDataSheet(null));
        if (binding.btnEmptyCta != null) binding.btnEmptyCta.setOnClickListener(v -> {
            if (fabAdd != null) fabAdd.performClick();
        });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
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
        if (tvTitle != null) tvTitle.setText(isEdit ? R.string.edit_supplier_title : R.string.add_supplier_title);

        // ✅ تعبئة البيانات للتعديل
        if (isEdit) {
            setText(etName,    editData, "name");
            setText(etPhone,   editData, "phone");
            setText(etAddress, editData, "address");
            setText(etNotes,   editData, "notes");
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name    = getText(etName);
                String phone   = getText(etPhone);
                String address = getText(etAddress);
                String notes   = getText(etNotes);

                if (name.isEmpty()) { showToast(getString(R.string.name_required)); return; }

                try {
                    if (isEdit) {
                        long id = getLong(editData, "id");
                        dbHelper.updateSupplier(id, name, phone, address, notes);
                        showToast(getString(R.string.supplier_updated));
                    } else {
                        dbHelper.addSupplier(name, phone, address, notes);
                        showToast(getString(R.string.supplier_added));
                    }
                    dialog.dismiss();
                    refreshData();
                } catch (Exception e) {
                    showToast(getString(R.string.error_with_message, e.getMessage()));
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

    private String getText(TextInputEditText et) {
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
            java.util.List<HashMap<String, String>> list = dbHelper.getAllSuppliers();
            for (HashMap<String, String> row : list) {
                HashMap<String, Object> obj = new HashMap<>();
                obj.putAll(row);
                fullList.add(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "refreshData error", e);
        }
        updateAdapter(fullList);
    }

    private void filter(String query) {
        if (query.isEmpty()) { updateAdapter(fullList); return; }
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
        adapter = new SupplierAdapter(data);
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
            .setTitle(R.string.delete_supplier)
            .setMessage(getString(R.string.delete_supplier_confirm_format, item.getOrDefault("name", "")))
            .setPositiveButton(R.string.delete, (d, w) -> {
                try {
                    dbHelper.deleteSupplier(getLong(item, "id"));
                    refreshData();
                    showToast(getString(R.string.supplier_deleted));
                } catch (Exception e) { showSnackbar(getString(R.string.delete_error), true); }
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

    private class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.VH> {
        private final ArrayList<HashMap<String, Object>> data;
        SupplierAdapter(ArrayList<HashMap<String, Object>> data) { this.data = data; }
        HashMap<String, Object> getItem(int pos) { return data.get(pos); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supplier, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (position < 0 || position >= data.size()) return;
            HashMap<String, Object> item = data.get(position);
            if (holder.tvName    != null) holder.tvName.setText(str(item, "name"));
            if (holder.tvPhone   != null) holder.tvPhone.setText(str(item, "phone"));
            if (holder.tvCompany != null) {
                String company = str(item, "company");
                holder.tvCompany.setText(company);
                holder.tvCompany.setVisibility(company.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (holder.tvDebt != null) {
                double debt = 0;
                try { debt = Double.parseDouble(str(item, "debt")); } catch (Exception ignored) {}
                if (debt > 0) {
                    String currency = "ج.م";
                    try {
                        HashMap<String, String> s = dbHelper.getStoreSettings();
                        if (s != null) currency = s.getOrDefault("currency", "ج.م");
                    } catch (Exception ignored) {}
                    holder.tvDebt.setText(getString(R.string.amount_currency_format, debt, currency));
                    holder.tvDebt.setVisibility(View.VISIBLE);
                } else {
                    holder.tvDebt.setVisibility(View.GONE);
                }
            }
            holder.itemView.setOnClickListener(v -> {
                String[] options = {getString(R.string.option_view_account), getString(R.string.option_edit_data)};
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(ActivitySuppliersActivity.this)
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            long sid = 0;
                            try { sid = Long.parseLong(str(item,"id")); } catch (Exception ignored) {}
                            android.content.Intent i2 = new android.content.Intent(ActivitySuppliersActivity.this,
                                ActivitySupplierAccountsActivity.class);
                            i2.putExtra("supplier_id", sid);
                            i2.putExtra("supplier_name", str(item, "name"));
                            startActivity(i2);
                        } else {
                            showDataSheet(item);
                        }
                    }).show();
            });
            if (holder.btnCall != null) {
                holder.btnCall.setOnClickListener(v -> {
                    String phone = str(item, "phone");
                    if (!phone.isEmpty()) {
                        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); }
                        catch (Exception ignored) {}
                    }
                });
            }
        }

        private String str(HashMap<String, Object> map, String key) {
            Object val = map.get(key);
            return val != null ? val.toString() : "";
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvCompany, tvDebt;
            View btnCall;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tv_name);
                tvPhone   = v.findViewById(R.id.tv_phone);
                tvCompany = v.findViewById(R.id.tv_company);
                tvDebt    = v.findViewById(R.id.tv_supplier_debt);
                btnCall   = v.findViewById(R.id.btn_call);
            }
        }
    }
}
