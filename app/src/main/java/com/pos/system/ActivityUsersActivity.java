package com.pos.system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pos.system.databinding.ActivityUsersBinding;
import com.pos.system.managers.UserManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityUsersActivity extends BaseActivity {

    private ActivityUsersBinding binding;
    private DBHelper   dbHelper;

    private final List<HashMap<String, String>> usersList = new ArrayList<>();
    private UsersAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.coordinatorRoot);
        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        loadUsers();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.users_management_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UsersAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setItemAnimator(null);

        binding.fabAdd.setOnClickListener(v -> showAddUserDialog());
        binding.btnEmptyAddUser.setOnClickListener(v -> showAddUserDialog());

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) binding.fabAdd.shrink();
                else if (dy < 0) binding.fabAdd.extend();
            }
        });
    }

    private void loadUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            List<HashMap<String, String>> list = dbHelper.getAllUsers();
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                usersList.clear();
                usersList.addAll(list);
                adapter.notifyDataSetChanged();
                binding.tvEmpty.setVisibility(usersList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showAddUserDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        TextInputLayout tilName     = dv.findViewById(R.id.til_name);
        TextInputLayout tilUsername = dv.findViewById(R.id.til_username);
        TextInputLayout tilPin      = dv.findViewById(R.id.til_pin);
        TextInputEditText etName     = dv.findViewById(R.id.et_name);
        TextInputEditText etUsername = dv.findViewById(R.id.et_username);
        TextInputEditText etPin      = dv.findViewById(R.id.et_pin);
        android.widget.Spinner spRole = dv.findViewById(R.id.sp_role);

        String[] roles = {getString(R.string.role_cashier), getString(R.string.role_manager), getString(R.string.role_admin)};
        android.widget.ArrayAdapter<String> roleAdapter =
            new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spRole != null) spRole.setAdapter(roleAdapter);

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_user_title)
            .setView(dv)
            .setPositiveButton(R.string.save, (d, w) -> {
                if (tilName != null) tilName.setError(null);
                if (tilUsername != null) tilUsername.setError(null);
                if (tilPin != null) tilPin.setError(null);

                String name     = etName     != null && etName.getText()     != null ? etName.getText().toString().trim()     : "";
                String username = etUsername != null && etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                String pin      = etPin      != null && etPin.getText()      != null ? etPin.getText().toString().trim()      : "";

                if (name.isEmpty()) {
                    if (tilName != null) tilName.setError(getString(R.string.name_required));
                    return;
                }
                if (username.isEmpty()) {
                    if (tilUsername != null) tilUsername.setError(getString(R.string.username_required));
                    return;
                }
                if (pin.isEmpty()) {
                    if (tilPin != null) tilPin.setError(getString(R.string.user_pin_required));
                    return;
                }
                if (pin.length() < 4) {
                    if (tilPin != null) tilPin.setError(getString(R.string.pin_min_length_error));
                    return;
                }

                String[] roleVals = {UserManager.ROLE_CASHIER, UserManager.ROLE_MANAGER, UserManager.ROLE_ADMIN};
                String role = roleVals[spRole != null ? spRole.getSelectedItemPosition() : 0];

                executor.execute(() -> {
                    long id = dbHelper.addUser(name, username, pin, role);
                    runOnUiThread(() -> {
                        if (id > 0) { showToast(getString(R.string.user_added_success)); loadUsers(); }
                        else showSnackbar(getString(R.string.error_with_message, getString(R.string.username_already_exists)), true);
                    });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showUserOptions(HashMap<String, String> user) {
        String role = user.getOrDefault("role", UserManager.ROLE_CASHIER);
        boolean isAdmin = UserManager.ROLE_ADMIN.equals(role);
        String[] options = isAdmin
            ? new String[]{getString(R.string.change_pin_option)}
            : new String[]{getString(R.string.change_pin_option), getString(R.string.deactivate_user_option), getString(R.string.delete)};

        new MaterialAlertDialogBuilder(this)
            .setTitle(user.getOrDefault("name","") + " - " + UserManager.getRoleDisplayName(this, role))
            .setItems(options, (d, w) -> {
                if (w == 0) showChangePinDialog(user);
                else if (w == 1 && !isAdmin) deactivateUser(user);
                else if (w == 2 && !isAdmin) confirmDeleteUser(user);
            })
            .setNegativeButton(R.string.close, null)
            .show();
    }

    private void showChangePinDialog(HashMap<String, String> user) {
        View dv = getLayoutInflater().inflate(R.layout.dialog_simple_input, null);
        TextInputEditText et = dv.findViewById(R.id.et_input);
        if (et != null) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_pin_title_format, user.getOrDefault("name","")))
            .setView(dv)
            .setPositiveButton(R.string.save, (d, w) -> {
                String pin = et != null && et.getText() != null ? et.getText().toString().trim() : "";
                if (pin.length() < 4) { showToast(getString(R.string.pin_min_length_error)); return; }
                long uid;
                try { uid = Long.parseLong(user.getOrDefault("id","0")); }
                catch (NumberFormatException e) { showToast(getString(R.string.error_unknown)); return; }
                executor.execute(() -> {
                    boolean ok = dbHelper.updateUserPin(uid, pin);
                    runOnUiThread(() -> showToast(ok ? getString(R.string.pin_changed_success) : getString(R.string.pin_change_error)));
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deactivateUser(HashMap<String, String> user) {
        long uid;
        try { uid = Long.parseLong(user.getOrDefault("id","0")); }
        catch (NumberFormatException e) { showToast(getString(R.string.error_unknown)); return; }
        executor.execute(() -> {
            boolean ok = dbHelper.updateUser(uid, user.getOrDefault("name",""),
                user.getOrDefault("role","cashier"), 0);
            runOnUiThread(() -> { if (ok) { showToast(getString(R.string.user_deactivated_success)); loadUsers(); } });
        });
    }

    private void confirmDeleteUser(HashMap<String, String> user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_user_title)
            .setMessage(getString(R.string.confirm_delete_user_message, user.getOrDefault("name","")))
            .setPositiveButton(R.string.delete, (d, w) -> {
                long uid;
                try { uid = Long.parseLong(user.getOrDefault("id","0")); }
                catch (NumberFormatException e) { showToast(getString(R.string.error_unknown)); return; }
                executor.execute(() -> {
                    boolean ok = dbHelper.deleteUser(uid);
                    runOnUiThread(() -> { if (ok) { showToast(getString(R.string.deleted_successfully)); loadUsers(); } else showToast(getString(R.string.cannot_delete_user)); });
                });
            })
            .setNegativeButton(R.string.cancel, null)
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
            if (pos < 0 || pos >= usersList.size()) return;
            HashMap<String, String> user = usersList.get(pos);
            h.tvName.setText(user.getOrDefault("name","—"));
            h.tvUsername.setText("@" + user.getOrDefault("username",""));
            String role = user.getOrDefault("role", UserManager.ROLE_CASHIER);
            android.content.Context ctx = h.itemView.getContext();
            h.tvRole.setText(UserManager.getRoleDisplayName(ctx, role));
            int roleColor;
            switch (role) {
                case UserManager.ROLE_ADMIN:   roleColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_error); break;
                case UserManager.ROLE_MANAGER: roleColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_info); break;
                default:                       roleColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.color_success); break;
            }
            h.tvRole.setTextColor(roleColor);
            boolean active = "1".equals(user.getOrDefault("is_active","1"));
            h.tvStatus.setText(active ? R.string.status_active : R.string.user_status_disabled);
            h.tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, active ? R.color.color_success : R.color.gray_400));
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
