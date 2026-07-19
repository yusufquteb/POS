package com.pos.system;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pos.system.databinding.ActivityStockCountBinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityStockCountActivity extends BaseActivity {

    private ActivityStockCountBinding binding;
    private DBHelper   dbHelper;

    private final List<HashMap<String, String>> allProducts  = new ArrayList<>();
    private final List<HashMap<String, String>> filteredList = new ArrayList<>();
    private final List<HashMap<String, String>> countedItems = new ArrayList<>();
    private CountAdapter adapter;
    private HashMap<String, String> activeSession = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockCountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.coordinatorRoot);
        dbHelper = new DBHelper(this);
        initViews();
        setupToolbar();
        checkActiveSession();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("جرد المخزون");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override public boolean onSupportNavigateUp() { onBackPressed(); return true; }

    private void initViews() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CountAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setItemAnimator(null);

        binding.fabStart.setOnClickListener(v -> startNewSession());

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0) binding.fabStart.shrink();
                else if (dy < 0) binding.fabStart.extend();
            }
        });

        binding.btnComplete.setOnClickListener(v -> confirmComplete());
        binding.btnCancel.setOnClickListener(v -> confirmCancel());
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterProducts(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkActiveSession() {
        executor.execute(() -> {
            activeSession = dbHelper.getActiveStockCountSession();
            runOnUiThread(() -> {
                if (activeSession != null) {
                    showSessionUI(true);
                    loadProductsForCount();
                } else {
                    showSessionUI(false);
                    loadPastSessions();
                }
            });
        });
    }

    private void showSessionUI(boolean hasSession) {
        binding.tvSessionStatus.setText(hasSession
            ? "جلسة جرد مفتوحة: " + (activeSession != null ? activeSession.getOrDefault("session_number","") : "")
            : "لا توجد جلسة جرد مفتوحة");
        binding.fabStart.setVisibility(hasSession ? View.GONE : View.VISIBLE);
        binding.btnComplete.setVisibility(hasSession ? View.VISIBLE : View.GONE);
        binding.btnCancel.setVisibility(hasSession ? View.VISIBLE : View.GONE);
        binding.tilSearch.setVisibility(hasSession ? View.VISIBLE : View.GONE);
    }

    private void loadProductsForCount() {
        executor.execute(() -> {
            List<HashMap<String, String>> products = dbHelper.getAllProducts();
            runOnUiThread(() -> {
                allProducts.clear();
                allProducts.addAll(products);
                filterProducts("");
            });
        });
    }

    private void filterProducts(String query) {
        filteredList.clear();
        for (HashMap<String, String> p : allProducts) {
            String name = p.getOrDefault("name","").toLowerCase();
            String barcode = p.getOrDefault("barcode","").toLowerCase();
            if (query.isEmpty() || name.contains(query.toLowerCase()) || barcode.contains(query.toLowerCase())) {
                filteredList.add(p);
            }
        }
        binding.tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void loadPastSessions() {
        executor.execute(() -> {
            List<HashMap<String, String>> sessions = dbHelper.getAllStockCountSessions();
            runOnUiThread(() -> {
                filteredList.clear();
                filteredList.addAll(sessions);
                binding.tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void startNewSession() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("بدء جلسة جرد جديدة")
            .setMessage("سيتم بدء جلسة جرد جديدة. يمكنك إدخال الكميات الفعلية لكل منتج ثم إتمام الجرد لتحديث المخزون.")
            .setPositiveButton("بدء", (d, w) ->
                executor.execute(() -> {
                    long id = dbHelper.createStockCountSession("admin");
                    runOnUiThread(() -> {
                        if (id > 0) { showToast("تم بدء جلسة الجرد"); checkActiveSession(); }
                        else showSnackbar("خطأ في بدء الجلسة", true);
                    });
                }))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void confirmComplete() {
        if (activeSession == null) return;
        int counted = countedItems.size();
        int total = allProducts.size();
        new MaterialAlertDialogBuilder(this)
            .setTitle("إتمام الجرد")
            .setMessage("تم إدخال " + counted + " منتج من أصل " + total + ".\n\n" +
                "المنتجات غير المُعدّة لن تتغير كمياتها.\n\n" +
                "هل تريد إتمام الجرد وتحديث المخزون؟")
            .setPositiveButton("إتمام", (d, w) -> {
                long sessionId = Long.parseLong(activeSession.getOrDefault("id","0"));
                executor.execute(() -> {
                    // Save all counted items first
                    for (HashMap<String, String> item : countedItems) {
                        int productId = Integer.parseInt(item.getOrDefault("product_id","0"));
                        String name = item.getOrDefault("name","");
                        String barcode = item.getOrDefault("barcode","");
                        int sysQty = Integer.parseInt(item.getOrDefault("system_qty","0"));
                        int countedQty = Integer.parseInt(item.getOrDefault("counted_qty","0"));
                        dbHelper.addStockCountItem(sessionId, productId, name, barcode, sysQty, countedQty, "");
                    }
                    boolean ok = dbHelper.completeStockCount(sessionId);
                    runOnUiThread(() -> {
                        if (ok) { showToast("تم إتمام الجرد وتحديث المخزون بنجاح"); activeSession = null; countedItems.clear(); checkActiveSession(); }
                        else showSnackbar("خطأ في إتمام الجرد", true);
                    });
                });
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("إلغاء الجرد")
            .setMessage("هل تريد إلغاء جلسة الجرد الحالية؟ لن تتغير أي كميات.")
            .setPositiveButton("إلغاء الجرد", (d, w) -> {
                long sessionId = Long.parseLong(activeSession.getOrDefault("id","0"));
                executor.execute(() -> {
                    boolean ok = dbHelper.cancelStockCount(sessionId);
                    runOnUiThread(() -> {
                        if (ok) { showToast("تم إلغاء الجرد"); activeSession = null; countedItems.clear(); checkActiveSession(); }
                        else showSnackbar("خطأ في الإلغاء", true);
                    });
                });
            })
            .setNegativeButton("تراجع", null)
            .show();
    }


    @Override protected void onDestroy() { super.onDestroy(); executor.shutdown(); }

    // ──────────────────────────────────────────────────────────────────
    private class CountAdapter extends RecyclerView.Adapter<CountAdapter.VH> {

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            return new VH(getLayoutInflater().inflate(
                activeSession != null ? R.layout.item_stock_count : R.layout.item_stock_session, parent, false));
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            HashMap<String, String> item = filteredList.get(pos);
            if (activeSession != null) {
                // Show product for counting
                h.tvName.setText(item.getOrDefault("name","—"));
                h.tvBarcode.setText(item.getOrDefault("barcode",""));
                int sysQty = Integer.parseInt(item.getOrDefault("qty","0"));
                h.tvSystemQty.setText("المخزون الحالي: " + sysQty);

                // Find if already counted
                HashMap<String, String> existing = null;
                for (HashMap<String, String> ci : countedItems) {
                    if (ci.getOrDefault("product_id","").equals(item.getOrDefault("id",""))) {
                        existing = ci;
                        break;
                    }
                }

                if (h.etCounted != null) {
                    String pid = item.getOrDefault("id","");
                    h.etCounted.setTag(pid);
                    if (existing != null) {
                        h.etCounted.setText(existing.getOrDefault("counted_qty",""));
                    } else {
                        h.etCounted.setText("");
                    }
                    h.etCounted.setOnFocusChangeListener((v, hasFocus) -> {
                        if (!hasFocus) {
                            String val = h.etCounted.getText() != null ? h.etCounted.getText().toString().trim() : "";
                            if (!val.isEmpty()) {
                                int counted;
                                try {
                                    counted = Integer.parseInt(val);
                                } catch (NumberFormatException e) {
                                    showToast(getString(R.string.error_unknown));
                                    h.etCounted.setText("");
                                    return;
                                }
                                HashMap<String, String> ci = new HashMap<>(item);
                                ci.put("product_id", pid);
                                ci.put("system_qty", String.valueOf(sysQty));
                                ci.put("counted_qty", String.valueOf(counted));
                                // Remove existing and add updated
                                countedItems.removeIf(c -> c.getOrDefault("product_id","").equals(pid));
                                countedItems.add(ci);
                                int diff = counted - sysQty;
                                if (h.tvDiff != null) {
                                    h.tvDiff.setText("الفرق: " + (diff >= 0 ? "+" : "") + diff);
                                    h.tvDiff.setTextColor(androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), diff == 0 ? R.color.gray_400 : diff > 0 ? R.color.color_success : R.color.color_error));
                                }
                            }
                        }
                    });
                }

                // Show difference if counted
                if (h.tvDiff != null && existing != null) {
                    int cq = Integer.parseInt(existing.getOrDefault("counted_qty","0"));
                    int diff = cq - sysQty;
                    h.tvDiff.setText("الفرق: " + (diff >= 0 ? "+" : "") + diff);
                    h.tvDiff.setTextColor(androidx.core.content.ContextCompat.getColor(h.itemView.getContext(), diff == 0 ? R.color.gray_400 : diff > 0 ? R.color.color_success : R.color.color_error));
                    h.tvDiff.setVisibility(View.VISIBLE);
                } else if (h.tvDiff != null) {
                    h.tvDiff.setVisibility(View.INVISIBLE);
                }

            } else {
                // Show past sessions
                if (h.tvName != null) h.tvName.setText(item.getOrDefault("session_number","—"));
                if (h.tvBarcode != null) h.tvBarcode.setText("بدأ: " + item.getOrDefault("started_at",""));
                String status = item.getOrDefault("status","");
                if (h.tvSystemQty != null) {
                    h.tvSystemQty.setText("الحالة: " + ("completed".equals(status) ? "مكتمل" : "cancelled".equals(status) ? "ملغي" : "مفتوح"));
                }
            }
        }

        @Override public int getItemCount() { return filteredList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvBarcode, tvSystemQty, tvDiff;
            EditText etCounted;
            VH(View v) {
                super(v);
                tvName      = v.findViewById(R.id.tv_name);
                tvBarcode   = v.findViewById(R.id.tv_barcode);
                tvSystemQty = v.findViewById(R.id.tv_system_qty);
                tvDiff      = v.findViewById(R.id.tv_diff);
                etCounted   = v.findViewById(R.id.et_counted);
            }
        }
    }
}
