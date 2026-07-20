package com.pos.system;

import android.os.Bundle;
import com.pos.system.FeatureGate;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityPurchaseOrderBinding;

/**
 * ActivityPurchaseOrderActivity - صفحة أوامر الشراء / Purchase Orders
 *
 * ✅ قائمة كاملة بأوامر الشراء (رقم PO، المورد، الإجمالي، الحالة، التاريخ)
 * ✅ FAB لإنشاء أمر شراء جديد (اختيار مورد → إضافة بنود → ملاحظات → تأكيد)
 * ✅ نقر على أمر معلّق → خيار "تم الاستلام" → receivePurchaseOrder()
 * ✅ حالة فارغة عند عدم وجود أوامر
 *
 * @version 1.0
 */
public class ActivityPurchaseOrderActivity extends BaseActivity {

    private ActivityPurchaseOrderBinding binding;


    private RecyclerView rvOrders;
    private ExtendedFloatingActionButton fabAddOrder;
    private View tvEmpty;
    private OrdersAdapter adapter;
    private DBHelper dbHelper;
    private String currency = "ج.م";

    private final List<HashMap<String, String>> ordersList = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPurchaseOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets(binding.getRoot());

        if (!FeatureGate.isUnlocked(this)) {
            FeatureGate.requirePremium(this, getString(R.string.feature_purchase_orders), true);
            return;
        }

        dbHelper = new DBHelper(this);
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            if (s != null) currency = s.getOrDefault("currency", "ج.م");
        } catch (Exception ignored) {}
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFab();
        loadOrders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Initialisation
    // ─────────────────────────────────────────────────────────────────────────

    private void initViews() {
        rvOrders    = binding.rvOrders;
        fabAddOrder = binding.fabAddOrder;
        tvEmpty     = binding.tvEmpty;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        if (rvOrders == null) return;
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrdersAdapter();
        rvOrders.setAdapter(adapter);
    }

    private void setupFab() {
        if (fabAddOrder != null)
            fabAddOrder.setOnClickListener(v -> startCreatePoFlow());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Data loading
    // ─────────────────────────────────────────────────────────────────────────

    private void loadOrders() {
        ordersList.clear();
        try {
            List<HashMap<String, String>> result = dbHelper.getPurchaseOrders();
            if (result != null) ordersList.addAll(result);
        } catch (Exception ignored) {}
        updateUI();
    }

    private void updateUI() {
        boolean empty = ordersList.isEmpty();
        if (rvOrders != null) rvOrders.setVisibility(empty ? View.GONE  : View.VISIBLE);
        if (tvEmpty  != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (adapter  != null) adapter.notifyDataSetChanged();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Create PO flow  (3 dialogs: supplier → items → notes/confirm)
    // ─────────────────────────────────────────────────────────────────────────

    private void startCreatePoFlow() {
        // ── Step 1: اختيار المورد ──────────────────────────────────────────
        List<HashMap<String, String>> suppliers = new ArrayList<>();
        try {
            List<HashMap<String, String>> result = dbHelper.getAllSuppliers();
            if (result != null) suppliers.addAll(result);
        } catch (Exception ignored) {}

        if (suppliers.isEmpty()) {
            showToast(getString(R.string.no_suppliers_registered));
            return;
        }

        String[] supplierNames = new String[suppliers.size()];
        for (int i = 0; i < suppliers.size(); i++) {
            String name    = safeGet(suppliers.get(i), "name");
            String company = safeGet(suppliers.get(i), "company");
            supplierNames[i] = company.isEmpty() ? name : name + " (" + company + ")";
        }

        final int[] selectedSupplierIdx = {0};

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_supplier_title)
                .setSingleChoiceItems(supplierNames, 0,
                        (dialog, which) -> selectedSupplierIdx[0] = which)
                .setPositiveButton(R.string.next_btn, (dialog, which) -> {
                    HashMap<String, String> chosenSupplier = suppliers.get(selectedSupplierIdx[0]);
                    showAddItemsDialog(chosenSupplier, new ArrayList<>());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * الخطوة 2: إضافة بنود أمر الشراء
     * يمكن للمستخدم إضافة بنود متعددة قبل الانتقال للخطوة 3.
     */
    private void showAddItemsDialog(HashMap<String, String> supplier,
                                    List<HashMap<String, String>> existingItems) {

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_po_item, null, false);

        TextInputEditText etProductName = dialogView.findViewById(R.id.et_po_product_name);
        TextInputEditText etQty         = dialogView.findViewById(R.id.et_po_qty);
        TextInputEditText etCost        = dialogView.findViewById(R.id.et_po_cost);
        TextView tvItemsAdded           = dialogView.findViewById(R.id.tv_items_added);

        // عرض عدد البنود المضافة حتى الآن
        if (tvItemsAdded != null)
            tvItemsAdded.setText(getString(R.string.items_added_count_format, existingItems.size()));

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.add_item_title_format, safeGet(supplier, "name")))
                .setView(dialogView)
                // زر "إضافة بند آخر"
                .setNeutralButton(R.string.add_another_item_btn, (dialog, which) -> {
                    HashMap<String, String> item = buildItemFromInputs(
                            etProductName, etQty, etCost);
                    if (item != null) {
                        existingItems.add(item);
                        showAddItemsDialog(supplier, existingItems);
                    } else {
                        showAddItemsDialog(supplier, existingItems);   // أعد فتح
                    }
                })
                // زر "التالي: المراجعة"
                .setPositiveButton(R.string.review_btn, (dialog, which) -> {
                    HashMap<String, String> item = buildItemFromInputs(
                            etProductName, etQty, etCost);
                    if (item != null) existingItems.add(item);
                    if (existingItems.isEmpty()) {
                        showToast(getString(R.string.please_add_one_item));
                        showAddItemsDialog(supplier, existingItems);
                        return;
                    }
                    showConfirmPoDialog(supplier, existingItems);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * يحاول بناء بند من حقول الإدخال.
     * يعيد null إذا كانت الحقول فارغة (لم يُدخل شيئاً).
     */
    private HashMap<String, String> buildItemFromInputs(TextInputEditText etName,
                                                         TextInputEditText etQty,
                                                         TextInputEditText etCost) {
        String name    = getEditText(etName);
        String qtyStr  = getEditText(etQty);
        String costStr = getEditText(etCost);

        if (name.isEmpty() && qtyStr.isEmpty() && costStr.isEmpty()) return null;

        if (name.isEmpty()) {
            showToast(getString(R.string.please_enter_product_name));
            return null;
        }
        double qty  = 1;
        double cost = 0;
        try { qty  = Double.parseDouble(qtyStr);  } catch (Exception ignored) {}
        try { cost = Double.parseDouble(costStr); } catch (Exception ignored) {}

        double totalLine = qty * cost;

        HashMap<String, String> item = new HashMap<>();
        item.put("name",  name);
        item.put("qty",   String.valueOf(qty));
        item.put("cost",  String.valueOf(cost));
        item.put("total", String.valueOf(totalLine));
        return item;
    }

    /**
     * الخطوة 3: مراجعة الأمر وإضافة ملاحظات والتأكيد
     */
    private void showConfirmPoDialog(HashMap<String, String> supplier,
                                     List<HashMap<String, String>> items) {

        // احسب الإجمالي
        double total = 0;
        StringBuilder summary = new StringBuilder();
        for (HashMap<String, String> item : items) {
            double qty  = parseDouble(safeGet(item, "qty"));
            double cost = parseDouble(safeGet(item, "cost"));
            double line = parseDouble(safeGet(item, "total"));
            total += line;
            summary.append("• ").append(safeGet(item, "name"))
                   .append("  ×").append(formatQty(qty))
                   .append("  @").append(String.format(Locale.getDefault(), "%.2f", cost))
                   .append(" = ").append(String.format(Locale.getDefault(), "%.2f %s\n", line, currency));
        }
        summary.append("\n")
               .append(getString(R.string.po_total_label_format, total, currency));

        final double finalTotal = total;

        // حقل الملاحظات
        TextInputLayout tilNotes = new TextInputLayout(this);
        tilNotes.setHint(getString(R.string.notes_optional_hint));
        TextInputEditText etNotes = new TextInputEditText(this);
        etNotes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        tilNotes.addView(etNotes);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        tilNotes.setPadding(pad, pad, pad, 0);

        // احتوِ الملاحظات في LinearLayout للعرض تحت الملخص
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, 0, pad, pad);
        TextView tvSummary = new TextView(this);
        tvSummary.setText(summary.toString());
        tvSummary.setPadding(0, pad, 0, pad);
        tvSummary.setTextSize(14f);
        container.addView(tvSummary);
        container.addView(tilNotes);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_po_title)
                .setView(container)
                .setPositiveButton(R.string.create_btn, (dialog, which) -> {
                    String notes = getEditText(etNotes);
                    createPurchaseOrder(supplier, items, finalTotal, notes);
                })
                .setNegativeButton(R.string.back_btn, (dialog, which) ->
                        showAddItemsDialog(supplier, items))
                .show();
    }

    private void createPurchaseOrder(HashMap<String, String> supplier,
                                     List<HashMap<String, String>> items,
                                     double total,
                                     String notes) {
        try {
            String supplierName = safeGet(supplier, "name");
            String supplierId   = safeGet(supplier, "id");

            long poId = dbHelper.addPurchaseOrder(supplierName, supplierId, items, total, notes);
            View rootView = rvOrders != null ? rvOrders : findViewById(android.R.id.content);
            if (poId > 0) {
                loadOrders();
                if (rootView != null) {
                    Snackbar.make(rootView,
                            getString(R.string.po_created_success),
                            Snackbar.LENGTH_LONG).show();
                } else {
                    showToast(getString(R.string.po_created_success));
                }
            } else {
                showSnackbar(getString(R.string.po_creation_failed), true);
            }
        } catch (Exception e) {
            showToast(getString(R.string.error_with_message, e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Mark as received
    // ─────────────────────────────────────────────────────────────────────────

    private void showOrderOptions(HashMap<String, String> order) {
        String status   = safeGet(order, "status");
        String poNumber = safeGet(order, "po_number");
        boolean isPending = "pending".equalsIgnoreCase(status);

        if (!isPending) {
            // الأمر مستلم — عرض تفاصيل فقط
            showOrderDetails(order);
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.po_title_format, poNumber))
                .setMessage(getString(R.string.po_details_message_format,
                        safeGet(order, "supplier_name"),
                        parseDouble(safeGet(order, "total")), currency,
                        safeGet(order, "created_at")))
                .setPositiveButton(R.string.mark_received_btn, (dialog, which) ->
                        confirmReceivePo(order))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void confirmReceivePo(HashMap<String, String> order) {
        String poNumber = safeGet(order, "po_number");
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_receipt_title)
                .setMessage(getString(R.string.confirm_receive_po_message_format, poNumber))
                .setPositiveButton(R.string.yes_received_btn, (dialog, which) ->
                        receiveOrder(order))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void receiveOrder(HashMap<String, String> order) {
        try {
            long poId = parseId(safeGet(order, "id"));
            boolean success = dbHelper.receivePurchaseOrder(poId);
            View rootView = rvOrders != null ? rvOrders : findViewById(android.R.id.content);
            if (success) {
                try {
                    double orderTotal = parseDouble(safeGet(order, "total"));
                    String poNum = safeGet(order, "po_number");
                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                    dbHelper.addWalletTransaction("OUT", orderTotal, getString(R.string.purchase_goods_wallet_note_format, poNum), date);
                } catch (Exception ignored) {}
                loadOrders();
                if (rootView != null) {
                    Snackbar.make(rootView,
                            getString(R.string.po_received_success),
                            Snackbar.LENGTH_LONG).show();
                } else {
                    showToast(getString(R.string.po_received_success));
                }
            } else {
                showSnackbar(getString(R.string.po_update_failed), true);
            }
        } catch (Exception e) {
            showToast(getString(R.string.error_with_message, e.getMessage()));
        }
    }

    private void showOrderDetails(HashMap<String, String> order) {
        String details = getString(R.string.po_full_details_format,
                safeGet(order, "po_number"),
                safeGet(order, "supplier_name"),
                parseDouble(safeGet(order, "total")), currency,
                formatStatus(safeGet(order, "status")),
                safeGet(order, "created_at"));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.po_details_title)
                .setMessage(details)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String safeGet(HashMap<String, String> map, String key) {
        if (map == null) return "";
        String v = map.get(key);
        return v != null ? v : "";
    }

    private String getEditText(TextInputEditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private long parseId(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0; }
    }

    private String formatQty(double qty) {
        if (qty == (long) qty) return String.valueOf((long) qty);
        return String.format(Locale.getDefault(), "%.2f", qty);
    }

    private String formatStatus(String status) {
        if ("received".equalsIgnoreCase(status)) return getString(R.string.po_status_received);
        if ("pending".equalsIgnoreCase(status))  return getString(R.string.po_status_pending);
        return status;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RecyclerView Adapter
    // ─────────────────────────────────────────────────────────────────────────

    private class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_purchase_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (position < 0 || position >= ordersList.size()) return;
            HashMap<String, String> order = ordersList.get(position);

            String poNumber  = safeGet(order, "po_number");
            String supplier  = safeGet(order, "supplier_name");
            String status    = safeGet(order, "status");
            String date      = safeGet(order, "created_at");
            double total     = parseDouble(safeGet(order, "total"));
            boolean pending  = "pending".equalsIgnoreCase(status);

            if (holder.tvPoNumber   != null) holder.tvPoNumber.setText(poNumber);
            if (holder.tvSupplier   != null) holder.tvSupplier.setText(supplier);
            if (holder.tvTotal      != null)
                holder.tvTotal.setText(
                        String.format(Locale.getDefault(), "%.2f %s", total, currency));
            if (holder.tvDate       != null) holder.tvDate.setText(date);
            if (holder.tvStatus     != null) {
                holder.tvStatus.setText(formatStatus(status));
                // لون مختلف للحالة
                int colorRes = pending
                        ? com.google.android.material.R.attr.colorTertiary
                        : com.google.android.material.R.attr.colorPrimary;
                // تعيين لون بسيط عبر الثيم
                holder.tvStatus.setTextColor(
                        getThemeColor(pending
                                ? com.google.android.material.R.attr.colorTertiary
                                : com.google.android.material.R.attr.colorPrimary));
            }

            holder.itemView.setOnClickListener(v -> showOrderOptions(order));
        }

        @Override
        public int getItemCount() { return ordersList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvPoNumber, tvSupplier, tvTotal, tvDate, tvStatus;

            VH(View v) {
                super(v);
                tvPoNumber = v.findViewById(R.id.tv_po_number);
                tvSupplier = v.findViewById(R.id.tv_supplier_name);
                tvTotal    = v.findViewById(R.id.tv_po_total);
                tvDate     = v.findViewById(R.id.tv_po_date);
                tvStatus   = v.findViewById(R.id.tv_po_status);
            }
        }
    }

    /** استخراج لون من الثيم الحالي */
    private int getThemeColor(int attrRes) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }
}
