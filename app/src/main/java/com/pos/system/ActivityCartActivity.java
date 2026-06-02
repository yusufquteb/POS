package com.pos.system;

import android.content.Intent;
import com.pos.system.BarcodeHelper;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.pos.system.managers.ReviewManager;

/**
 * ActivityCartActivity - نقطة البيع (POS)
 * v4.0 - Google Play Ready
 * - طريقة الدفع (نقدي/بطاقة/آجل)
 * - ضريبة من إعدادات المتجر
 * - طباعة فعلية عبر Bluetooth
 * - دعم ثنائي اللغة بالكامل
 */
public class ActivityCartActivity extends BaseActivity {

    private static final String TAG = "CartActivity";

    private DBHelper dbHelper;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;

    // Totals
    private double subtotal    = 0, discountAmt = 0, discountPct = 0;
    private double taxRate     = 0, taxAmount   = 0, total       = 0;
    private String currency    = "ج.م";
    private boolean taxEnabled = false;

    // Customer & Payment
    private String selectedCustomerId   = null;
    private String selectedCustomerName = null;
    private String paymentMethod        = "cash";

    // Views
    private RecyclerView      recyclerCart;
    private View              emptyView, rowTax;
    private MaterialCardView  cardTotals;
    private TextView          tvSubtotal, tvDiscount, tvTax, tvTotal;
    private TextInputEditText etDiscount;
    private TextView          tvSelectedCustomer;
    private MaterialButton    btnSelectCustomer, btnAddProduct, btnCheckout, btnClear;
    private FloatingActionButton btnScanBarcode;
    private ChipGroup         chipGroupPayment;

    private final ActivityResultLauncher<Intent> barcodeLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    String bc = BarcodeHelper.extractBarcode(result.getData());
                    if (bc != null && !bc.isEmpty()) addProductByBarcode(bc);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        // CoordinatorLayout with fitsSystemWindows=true handles insets automatically

        dbHelper  = new DBHelper(this);
        cartItems = new ArrayList<>();

        loadStoreSettings();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    private void loadStoreSettings() {
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            currency   = s.getOrDefault("currency", "ج.م");
            taxEnabled = "true".equalsIgnoreCase(s.getOrDefault("tax_enabled", "false"));
            taxRate    = 0;
            if (taxEnabled) {
                try { taxRate = Double.parseDouble(s.getOrDefault("tax_rate", "0")); }
                catch (Exception ignored) {}
            }
        } catch (Exception e) { currency = "ج.م"; }
    }

    private void initViews() {
        recyclerCart       = findViewById(R.id.recycler_cart);
        emptyView          = findViewById(R.id.empty_view);
        cardTotals         = findViewById(R.id.card_totals);
        tvSubtotal         = findViewById(R.id.tv_subtotal);
        tvDiscount         = findViewById(R.id.tv_discount);
        tvTax              = findViewById(R.id.tv_tax);
        rowTax             = findViewById(R.id.row_tax);
        tvTotal            = findViewById(R.id.tv_total);
        etDiscount         = findViewById(R.id.et_discount);
        tvSelectedCustomer = findViewById(R.id.tv_selected_customer);
        btnSelectCustomer  = findViewById(R.id.btn_select_customer);
        btnAddProduct      = findViewById(R.id.btn_add_product);
        btnCheckout        = findViewById(R.id.btn_checkout);
        btnClear           = findViewById(R.id.btn_clear);
        btnScanBarcode     = findViewById(R.id.btn_scan_barcode);
        chipGroupPayment   = findViewById(R.id.chip_group_payment);
        if (rowTax != null) rowTax.setVisibility(taxEnabled ? View.VISIBLE : View.GONE);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) { setSupportActionBar(toolbar); toolbar.setNavigationOnClickListener(v -> finish()); }
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter();
        recyclerCart.setLayoutManager(new LinearLayoutManager(this));
        recyclerCart.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        if (btnAddProduct     != null) btnAddProduct.setOnClickListener(v -> showAddProductDialog());
        if (btnSelectCustomer != null) btnSelectCustomer.setOnClickListener(v -> showSelectCustomerDialog());
        if (btnScanBarcode    != null) btnScanBarcode.setOnClickListener(v -> openBarcodeScanner());
        if (btnCheckout       != null) btnCheckout.setOnClickListener(v -> checkout());
        if (btnClear          != null) btnClear.setOnClickListener(v -> confirmClearCart());
        if (etDiscount != null) {
            etDiscount.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                public void afterTextChanged(Editable s) {}
                public void onTextChanged(CharSequence s, int st, int b, int c) { applyDiscount(s.toString().trim()); }
            });
        }
        if (chipGroupPayment != null) {
            chipGroupPayment.setOnCheckedStateChangeListener((group, ids) -> {
                if (!ids.isEmpty()) {
                    Chip chip = group.findViewById(ids.get(0));
                    if (chip != null && chip.getTag() != null) paymentMethod = chip.getTag().toString();
                }
            });
        }
    }

    private void openBarcodeScanner() {
        BarcodeHelper.launch(this, barcodeLauncher);
    }

    private void addProductByBarcode(String barcode) {
        try {
            HashMap<String, String> p = dbHelper.getProductByBarcode(barcode);
            if (p != null) {
                int avail = parseInt(p.getOrDefault("qty", "0"));
                if (avail <= 0) { snack(getString(R.string.out_of_stock)); return; }
                showQuantityDialog(p);
            } else { snack(getString(R.string.product_not_found) + ": " + barcode); }
        } catch (Exception e) { showToast(getString(R.string.error_loading)); }
    }

    private void showAddProductDialog() {
        try {
            List<HashMap<String, String>> products = dbHelper.getAllProducts();
            if (products == null || products.isEmpty()) { snack(getString(R.string.no_products)); return; }
            String[] entries = new String[products.size()];
            for (int i = 0; i < products.size(); i++) {
                HashMap<String, String> p = products.get(i);
                entries[i] = p.getOrDefault("name", "") + "  (" +
                    getString(R.string.stock_available) + ": " + p.getOrDefault("qty", "0") +
                    " | " + formatCurrency(parseDouble(p.getOrDefault("price", "0"))) + ")";
            }
            new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.cart_add_product))
                .setItems(entries, (d, w) -> {
                    HashMap<String, String> p = products.get(w);
                    if (parseInt(p.getOrDefault("qty", "0")) <= 0) { snack(getString(R.string.out_of_stock)); return; }
                    showQuantityDialog(p);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        } catch (Exception e) { showToast(getString(R.string.error_loading)); }
    }

    private void showQuantityDialog(HashMap<String, String> product) {
        String name   = product.getOrDefault("name", "");
        double price  = parseDouble(product.getOrDefault("price", "0"));
        int    avail  = parseInt(product.getOrDefault("qty", "0"));

        TextInputEditText etQty = new TextInputEditText(this);
        etQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etQty.setHint(getString(R.string.quantity_hint));
        etQty.setText("1");
        int p = (int)(12 * getResources().getDisplayMetrics().density);
        etQty.setPadding(p, p, p, p);

        new MaterialAlertDialogBuilder(this)
            .setTitle(name)
            .setMessage(getString(R.string.product_price) + ": " + formatCurrency(price) +
                "\n" + getString(R.string.stock_available) + ": " + avail)
            .setView(etQty)
            .setPositiveButton(getString(R.string.add_to_cart), (d, w) -> {
                String qs = etQty.getText() != null ? etQty.getText().toString().trim() : "";
                if (qs.isEmpty()) { showToast(getString(R.string.invalid_quantity)); return; }
                try {
                    int qty = Integer.parseInt(qs);
                    if (qty <= 0) { showToast(getString(R.string.quantity_positive)); }
                    else if (qty > avail) { showToast(getString(R.string.quantity_exceeds) + " (" + avail + ")"); }
                    else { addToCart(product, qty); }
                } catch (NumberFormatException e) { showToast(getString(R.string.invalid_quantity)); }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void addToCart(HashMap<String, String> product, int qty) {
        String id   = product.getOrDefault("id", "");
        String name = product.getOrDefault("name", "");
        double pr   = parseDouble(product.getOrDefault("price", "0"));
        boolean found = false;
        for (CartItem item : cartItems) {
            if (item.id.equals(id)) { item.quantity += qty; found = true; break; }
        }
        if (!found) cartItems.add(new CartItem(id, name, pr, qty));
        updateUI();
        snack("✓ " + name + " – " + getString(R.string.added_to_cart));
    }

    private void showSelectCustomerDialog() {
        try {
            List<HashMap<String, String>> customers = dbHelper.getAllCustomers();
            String[] names = new String[customers.size() + 1];
            names[0] = "– " + getString(R.string.no_customer) + " –";
            for (int i = 0; i < customers.size(); i++) {
                String n = customers.get(i).getOrDefault("name", "");
                String ph = customers.get(i).getOrDefault("phone", "");
                names[i + 1] = n + (ph.isEmpty() ? "" : "  |  " + ph);
            }
            new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.select_customer))
                .setItems(names, (d, w) -> {
                    if (w == 0) {
                        selectedCustomerId = null; selectedCustomerName = null;
                        if (tvSelectedCustomer != null) tvSelectedCustomer.setText(getString(R.string.no_customer_selected));
                    } else {
                        HashMap<String, String> c = customers.get(w - 1);
                        selectedCustomerId = c.get("id"); selectedCustomerName = c.get("name");
                        if (tvSelectedCustomer != null) tvSelectedCustomer.setText(selectedCustomerName);
                    }
                }).show();
        } catch (Exception e) { showToast(getString(R.string.error_loading)); }
    }

    private void applyDiscount(String input) {
        try {
            discountPct = input.isEmpty() ? 0 : Double.parseDouble(input);
            if (discountPct < 0) discountPct = 0;
            if (discountPct > 100) discountPct = 100;
        } catch (NumberFormatException e) { discountPct = 0; }
        recalculateTotals();
    }

    private void recalculateTotals() {
        subtotal = 0;
        for (CartItem item : cartItems) subtotal += item.price * item.quantity;
        discountAmt = subtotal * (discountPct / 100.0);
        double afterDiscount = subtotal - discountAmt;
        taxAmount = taxEnabled ? afterDiscount * (taxRate / 100.0) : 0;
        total     = afterDiscount + taxAmount;
        if (tvSubtotal != null) tvSubtotal.setText(formatCurrency(subtotal));
        if (tvDiscount != null) tvDiscount.setText(formatCurrency(discountAmt));
        if (tvTax      != null) tvTax.setText(formatCurrency(taxAmount) + (taxEnabled ? " (" + taxRate + "%)" : ""));
        if (tvTotal    != null) tvTotal.setText(formatCurrency(total));
    }

    private void updateUI() {
        boolean empty = cartItems.isEmpty();
        if (recyclerCart != null) recyclerCart.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyView    != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (cardTotals   != null) cardTotals.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (btnCheckout  != null) btnCheckout.setEnabled(!empty);
        if (btnClear     != null) btnClear.setEnabled(!empty);
        if (cartAdapter  != null) cartAdapter.notifyDataSetChanged();
        recalculateTotals();
    }

    private void checkout() {
        if (cartItems.isEmpty()) { snack(getString(R.string.cart_empty)); return; }
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.subtotal)).append(": ").append(formatCurrency(subtotal)).append("\n");
        if (discountAmt > 0) msg.append(getString(R.string.discount_amount)).append(": ").append(formatCurrency(discountAmt)).append("\n");
        if (taxEnabled && taxAmount > 0) msg.append(getString(R.string.tax)).append(" (").append(taxRate).append("%): ").append(formatCurrency(taxAmount)).append("\n");
        msg.append("──────────────────\n");
        msg.append(getString(R.string.final_total)).append(": ").append(formatCurrency(total)).append("\n");
        msg.append(getString(R.string.payment_method)).append(": ").append(getPaymentLabel(paymentMethod)).append("\n");
        msg.append(getString(R.string.customer_label)).append(": ").append(selectedCustomerName != null ? selectedCustomerName : getString(R.string.no_customer));

        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cart_checkout))
            .setMessage(msg.toString())
            .setPositiveButton(getString(R.string.confirm_sale), (d, w) -> processCheckout())
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void processCheckout() {
        try {
            String invoiceNumber = "INV-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new java.util.Date());
            String custId   = selectedCustomerId   != null ? selectedCustomerId   : "0";
            String custName = selectedCustomerName != null ? selectedCustomerName : "";

            long invoiceId = dbHelper.createInvoiceWithDetails(
                invoiceNumber, custId, custName, cartItems, subtotal, discountAmt, taxAmount, total, paymentMethod);

            if (invoiceId > 0) {
                for (CartItem item : cartItems) dbHelper.decreaseProductQuantity(item.id, item.quantity);
                try { new ReviewManager(this).onInvoiceCreated(); } catch (Exception ignored) {}
                showCheckoutSuccess(invoiceNumber, invoiceId);
            } else {
                snack(getString(R.string.checkout_failed));
            }
        } catch (Exception e) {
            showToast(getString(R.string.checkout_error));
            android.util.Log.e(TAG, "processCheckout: " + e.getMessage(), e);
        }
    }

    private void showCheckoutSuccess(String invoiceNumber, long invoiceId) {
        String msg = getString(R.string.invoice_number) + ": " + invoiceNumber
            + "\n" + getString(R.string.final_total) + ": " + formatCurrency(total)
            + (selectedCustomerName != null ? "\n" + getString(R.string.customer_label) + ": " + selectedCustomerName : "");
        new MaterialAlertDialogBuilder(this)
            .setTitle("✅ " + getString(R.string.checkout_success))
            .setMessage(msg)
            .setPositiveButton(getString(R.string.print), (d, w) -> {
                printInvoice(invoiceId); clearCart(); finish();
            })
            .setNeutralButton(getString(R.string.view_invoice), (d, w) -> {
                clearCart();
                startActivity(new Intent(this, ActivityInvoicesActivity.class));
                finish();
            })
            .setNegativeButton(getString(R.string.close), (d, w) -> { clearCart(); finish(); })
            .setCancelable(false).show();
    }

    private void printInvoice(long invoiceId) {
        try {
            int custId = 0;
            if (selectedCustomerId != null) try { custId = Integer.parseInt(selectedCustomerId); } catch (Exception ignored) {}
            boolean ok = new InvoicePrinter(this).printInvoice(invoiceId, custId);
            if (!ok) showToast(getString(R.string.print_error));
        } catch (Exception e) {
            showToast(getString(R.string.print_error));
            android.util.Log.e(TAG, "printInvoice: " + e.getMessage(), e);
        }
    }

    private void confirmClearCart() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cart_clear))
            .setMessage(getString(R.string.confirm_clear_cart))
            .setPositiveButton(getString(R.string.yes), (d, w) -> clearCart())
            .setNegativeButton(getString(R.string.no), null)
            .show();
    }

    private void clearCart() {
        cartItems.clear(); discountPct = 0; discountAmt = 0;
        if (etDiscount != null) etDiscount.setText("");
        selectedCustomerId = null; selectedCustomerName = null;
        if (tvSelectedCustomer != null) tvSelectedCustomer.setText(getString(R.string.no_customer_selected));
        updateUI();
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%.2f %s", amount, currency);
    }
    private double parseDouble(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
    private int    parseInt(String s)    { try { return Integer.parseInt(s);  } catch (Exception e) { return 0; } }
    private void   snack(String msg)     {
        View root = findViewById(android.R.id.content);
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
        else showToast(msg);
    }
    private String getPaymentLabel(String m) {
        switch (m) {
            case "card":   return getString(R.string.payment_card);
            case "credit":   return getString(R.string.payment_credit);
            case "vodafone": return getString(R.string.payment_vodafone);
            case "instapay": return getString(R.string.payment_instapay);
            default:       return getString(R.string.payment_cash);
        }
    }

    // ─── CartItem ────────────────────────────────────────────────
    static class CartItem {
        String id, name; double price; int quantity;
        CartItem(String id, String name, double price, int qty) {
            this.id = id; this.name = name; this.price = price; this.quantity = qty;
        }
        double getTotal() { return price * quantity; }
    }

    // ─── Adapter ─────────────────────────────────────────────────
    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            CartItem item = cartItems.get(pos);
            if (h.tvName     != null) h.tvName.setText(item.name);
            if (h.tvPrice    != null) h.tvPrice.setText(formatCurrency(item.price));
            if (h.tvQuantity != null) h.tvQuantity.setText(String.valueOf(item.quantity));
            if (h.tvTotal    != null) h.tvTotal.setText(formatCurrency(item.getTotal()));

            if (h.btnDecrease != null) h.btnDecrease.setOnClickListener(v -> {
                int p = h.getAdapterPosition(); if (p < 0 || p >= cartItems.size()) return;
                if (cartItems.get(p).quantity > 1) { cartItems.get(p).quantity--; notifyItemChanged(p); recalculateTotals(); }
                else { cartItems.remove(p); notifyItemRemoved(p); notifyItemRangeChanged(p, cartItems.size()); updateUI(); }
            });
            if (h.btnIncrease != null) h.btnIncrease.setOnClickListener(v -> {
                int p = h.getAdapterPosition(); if (p < 0 || p >= cartItems.size()) return;
                cartItems.get(p).quantity++; notifyItemChanged(p); recalculateTotals();
            });
            if (h.btnRemove != null) h.btnRemove.setOnClickListener(v -> {
                int p = h.getAdapterPosition(); if (p < 0 || p >= cartItems.size()) return;
                String n = cartItems.get(p).name;
                cartItems.remove(p); notifyItemRemoved(p); notifyItemRangeChanged(p, cartItems.size());
                updateUI(); snack(n + " " + getString(R.string.removed_from_cart));
            });
        }
        @Override public int getItemCount() { return cartItems.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvQuantity, tvTotal;
            MaterialButton btnDecrease, btnIncrease;
            ImageView btnRemove;
            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_product_name);
                tvPrice = v.findViewById(R.id.tv_price);
                tvQuantity = v.findViewById(R.id.tv_quantity);
                tvTotal = v.findViewById(R.id.tv_total);
                btnDecrease = v.findViewById(R.id.btn_decrease);
                btnIncrease = v.findViewById(R.id.btn_increase);
                btnRemove = v.findViewById(R.id.btn_remove);
            }
        }
    }
}
