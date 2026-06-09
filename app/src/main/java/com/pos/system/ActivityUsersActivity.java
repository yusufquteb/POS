package com.pos.system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.pos.system.managers.UserManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityUsersActivity extends BaseActivity {

    private DBHelper   dbHelper;
    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fabAdd;
    private View       tvEmpty;
    private View       progressBar;

    private final List<HashMap<String, String>> usersList = new ArrayList<>();
    private UsersAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        applyWindowInsets(findViewById(R.id.coordinator_root));
        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        loadUsers();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        if (tb != null) {
            setSupportActionBar(tb);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("إدارة المستخدمين");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        fabAdd       = findViewById(R.id.fab_add);
        tvEmpty      = findViewById(R.id.tv_empty);
        progressBar  = findViewById(R.id.progress_bar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter();
        recyclerView.setAdapter(adapter);

        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddUserDialog());
    }

    private void loadUsers() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<HashMap<String, String>> list = dbHelper.getAllUsers();
            runOnUiThread(() -> {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                usersList.clear();
                usersList.addAll(list);
                adapter.notifyDataSetChanged();
                if (tvEmpty != null) tvEmpty.setVisibility(usersList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showAddUserDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        TextInputEditText etName     = dv.findViewById(R.id.et_name);
        TextInputEditText etUsername = dv.findViewById(R.id.et_username);
        TextInputEditText etPin      = dv.findViewById(R.id.et_pin);
        android.widget.Spinner spRole = dv.findViewById(R.id.sp_role);

        String[] roles = {"كاشير", "مدير", "مدير النظام"};
        android.widget.ArrayAdapter<String> roleAdapter =
            new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spRole != null) spRole.setAdapter(roleAdapter);

        new MaterialAlertDialogBuilder(this)
            .setTitle("إضافة مستخدم جديد")
            .setView(dv)
            .setPositiveButton("حفظ", (d, w) -> {
                String name     = etName     != null && etName.getText()     != null ? etName.getText().toString().trim()     : "";
                String username = etUsername != null && etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                String pin      = etPin      != null && etPin.getText()      != null ? etPin.getText().toString().trim()      : "";
                if (name.isEmpty() || username.isEmpty() || pin.isEmpty()) {
                    showToast("جميع الحقول مطلوبة"); return;
                }
                if (pin.length() < 4) { showToast("الـ PIN يجب أن يكون 4 أرقام على الأقل"); return; }

                String[] roleVals = {UserManager.ROLE_CASHIER, UserManager.ROLE_MANAGER, UserManager.ROLE_ADMIN};
                String role = roleVals[spRole != null ? spRole.getSelectedItemPosition() : 0];

                executor.execute(() -> {
                    long id = dbHelper.addUser(name, username, pin, role);
                    runOnUiThread(() -> {
                        if (id > 0) { showToast("تمت إضافة المستخدم"); loadUsers(); }
                        else showToast("خطأ: اسم المستخدم موجود مسبقاً");
                    });
                });
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showUserOptions(HashMap<String, String> user) {
        String role = user.getOrDefault("role", UserManager.ROLE_CASHIER);
        boolean isAdmin = UserManager.ROLE_ADMIN.equals(role);
        String[] options = isAdmin
            ? new String[]{"تغيير PIN"}
            : new String[]{"تغيير PIN", "تعطيل المستخدم", "حذف"};

        new MaterialAlertDialogBuilder(this)
            .setTitle(user.getOrDefault("name","") + " - " + UserManager.getRoleDisplayName(role))
            .setItems(options, (d, w) -> {
                if (w == 0) showChangePinDialog(user);
                else if (w == 1 && !isAdmin) deactivateUser(user);
                else if (w == 2 && !isAdmin) confirmDeleteUser(user);
            })
            .setNegativeButton("إغلاق", null)
            .show();
    }

    private void showChangePinDialog(HashMap<String, String> user) {
        View dv = getLayoutInflater().inflate(R.layout.dialog_simple_input, null);
        TextInputEditText et = dv.findViewById(R.id.et_input);
        if (et != null) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
            .setTitle("تغيير PIN - " + user.getOrDefault("name",""))
            .setView(dv)
            .setPositiveButton("حفظ", (d, w) -> {
                String pin = et != null && et.getText() != null ? et.getText().toString().trim() : "";
                if (pin.length() < 4) { showToast("PIN يجب أن يكون 4 أرقام على الأقل"); return; }
                long uid = Long.parseLong(user.getOrDefault("id","0"));
                executor.execute(() -> {
                    boolean ok = dbHelper.updateUserPin(uid, pin);
                    runOnUiThread(() -> showToast(ok ? "تم تغيير PIN بنجاح" : "خطأ في التغيير"));
                });
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void deactivateUser(HashMap<String, String> user) {
        long uid = Long.parseLong(user.getOrDefault("id","0"));
        executor.execute(() -> {
            boolean ok = dbHelper.updateUser(uid, user.getOrDefault("name",""),
                user.getOrDefault("role","cashier"), 0);
            runOnUiThread(() -> { if (ok) { showToast("تم تعطيل المستخدم"); loadUsers(); } });
        });
    }

    private void confirmDeleteUser(HashMap<String, String> user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("حذف المستخدم")
            .setMessage("هل أنت متأكد من حذف المستخدم " + user.getOrDefault("name","") + "؟")
            .setPositiveButton("حذف", (d, w) -> {
                long uid = Long.parseLong(user.getOrDefault("id","0"));
                executor.execute(() -> {
                    boolean ok = dbHelper.deleteUser(uid);
                    runOnUiThread(() -> { if (ok) { showToast("تم الحذف"); loadUsers(); } else showToast("لا يمكن حذف المستخدم"); });
                });
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }


    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }

    // ──────────────────────────────────────────────────────────────────
    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(R.layout.item_user, parent, false));
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            HashMap<String, String> user = usersList.get(pos);
            h.tvName.setText(user.getOrDefault("name","—"));
            h.tvUsername.setText("@" + user.getOrDefault("username",""));
            String role = user.getOrDefault("role", UserManager.ROLE_CASHIER);
            h.tvRole.setText(UserManager.getRoleDisplayName(role));
            int roleColor;
            switch (role) {
                case UserManager.ROLE_ADMIN:   roleColor = 0xFFE91E63; break;
                case UserManager.ROLE_MANAGER: roleColor = 0xFF2196F3; break;
                default:                       roleColor = 0xFF4CAF50; break;
            }
            h.tvRole.setTextColor(roleColor);
            boolean active = "1".equals(user.getOrDefault("is_active","1"));
            h.tvStatus.setText(active ? "نشط" : "معطّل");
            h.tvStatus.setTextColor(active ? 0xFF4CAF50 : 0xFF9E9E9E);
            h.itemView.setOnClickListener(v -> showUserOptions(user));
        }

        @Override public int getItemCount() { return usersList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvUsername, tvRole, tvStatus;
            VH(View v) {
                super(v);
                tvName     = v.findViewById(R.id.tv_name);
                tvUsername = v.findViewById(R.id.tv_username);
                tvRole     = v.findViewById(R.id.tv_role);
                tvStatus   = v.findViewById(R.id.tv_status);
            }
        }
    }
}
