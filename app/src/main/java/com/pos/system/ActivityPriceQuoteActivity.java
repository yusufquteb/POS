package com.pos.system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityCartBinding;

public class ActivityPriceQuoteActivity extends BaseActivity {

    private ActivityCartBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private List<CartItem> cartItems = new ArrayList<>();
    private CartAdapter adapter;
    private String selectedCustomerName = "";
    private long selectedCustomerId = 0;
    private double discount = 0;
    private double taxRate = 0;

    static class CartItem {
        String id, name, barcode;
        double price;
        int quantity;
        CartItem(String id, String name, String barcode, double price, int qty) {
            this.id = id; this.name = name; this.barcode = barcode;
            this.price = price; this.quantity = qty;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            if (s != null) {
                currency = s.getOrDefault("currency", "ج.م");
                try { taxRate = Double.parseDouble(s.getOrDefault("tax_rate","0")); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        setupToolbar();
        setupRecycler();
        setupAddProduct();
        setupCheckout();
        updateTotals();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.new_price_quote_title);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        binding.recyclerCart.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter();
        binding.recyclerCart.setAdapter(adapter);
    }

    private void setupAddProduct() {
        // Use the "Add Product" FAB to search and add products
        binding.btnAddProduct.setOnClickListener(v -> showProductSearchDialog());
    }

    private void showProductSearchDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextInputEditText etSearch = dialogView.findViewById(R.id.et_input);
        if (etSearch != null) etSearch.setHint(getString(R.string.product_name_or_barcode_hint));

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.search_for_product_title)
            .setView(dialogView)
            .setPositiveButton(R.string.action_search, (d, w) -> {
                String query = etSearch != null && etSearch.getText() != null
                    ? etSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) searchProduct(query);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void searchProduct(String q) {
        if (q.isEmpty()) return;
        List<HashMap<String, String>> results = dbHelper.searchProducts(q);
        if (results.isEmpty()) {
            Snackbar.make(binding.getRoot(), getString(R.string.product_not_found), Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (results.size() == 1) {
            addToCart(results.get(0));
            return;
        }
        // Show picker
        String[] names = new String[results.size()];
        for (int i = 0; i < results.size(); i++)
            names[i] = results.get(i).getOrDefault("name","") + " - " + results.get(i).getOrDefault("price","0");
        final List<HashMap<String, String>> finalResults = results;
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_product_title)
            .setItems(names, (d, which) -> addToCart(finalResults.get(which)))
            .show();
    }

    private void addToCart(HashMap<String, String> p) {
        String id = p.getOrDefault("id","0");
        String name = p.getOrDefault("name","");
        String barcode = p.getOrDefault("barcode","");
        double price = 0;
        try { price = Double.parseDouble(p.getOrDefault("price","0")); } catch (Exception ignored) {}

        for (CartItem item : cartItems) {
            if (item.id.equals(id)) {
                item.quantity++;
                updateTotals();
                adapter.notifyDataSetChanged();
                return;
            }
        }
        cartItems.add(new CartItem(id, name, barcode, price, 1));
        updateTotals();
        adapter.notifyDataSetChanged();
    }

    private void updateTotals() {
        double subtotal = 0;
        for (CartItem item : cartItems) subtotal += item.price * item.quantity;
        double tax = subtotal * taxRate / 100.0;
        double total = subtotal - discount + tax;

        boolean hasItems = !cartItems.isEmpty();

        // Show/hide cart recycler and empty view
        binding.recyclerCart.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        binding.emptyView.setVisibility(hasItems ? View.GONE : View.VISIBLE);

        // Show/hide totals card
        binding.cardTotals.setVisibility(hasItems ? View.VISIBLE : View.GONE);

        if (hasItems) {
            if (binding.tvSubtotal != null)
                binding.tvSubtotal.setText(String.format(Locale.US, "%.2f %s", subtotal, currency));
            if (binding.tvDiscount != null)
                binding.tvDiscount.setText(String.format(Locale.US, "-%.2f %s", discount, currency));
            if (binding.tvTax != null)
                binding.tvTax.setText(String.format(Locale.US, "%.2f %s", tax, currency));
            if (binding.tvTotal != null)
                binding.tvTotal.setText(String.format(Locale.US, "%.2f %s", total, currency));
        }

        binding.btnCheckout.setEnabled(hasItems);
    }

    private void setupCheckout() {
        binding.btnCheckout.setText(R.string.save_quote);
        binding.btnCheckout.setOnClickListener(v -> saveQuote());

        // Clear button clears cart
        binding.btnClear.setOnClickListener(v -> {
            cartItems.clear();
            adapter.notifyDataSetChanged();
            updateTotals();
        });
    }

    private void saveQuote() {
        if (cartItems.isEmpty()) {
            Snackbar.make(binding.getRoot(), getString(R.string.add_products_first), Snackbar.LENGTH_SHORT).show();
            return;
        }
        double subtotal = 0;
        for (CartItem item : cartItems) subtotal += item.price * item.quantity;
        double tax = subtotal * taxRate / 100.0;
        double total = subtotal - discount + tax;

        List<HashMap<String, String>> items = new ArrayList<>();
        for (CartItem ci : cartItems) {
            HashMap<String, String> m = new HashMap<>();
            m.put("id", ci.id); m.put("barcode", ci.barcode);
            m.put("name", ci.name);
            m.put("price", String.valueOf(ci.price));
            m.put("qty", String.valueOf(ci.quantity));
            m.put("total", String.valueOf(ci.price * ci.quantity));
            items.add(m);
        }

        long quoteId = dbHelper.addPriceQuote(selectedCustomerName, selectedCustomerId,
            items, subtotal, discount, tax, total, "");
        if (quoteId > 0) {
            Snackbar.make(binding.getRoot(), getString(R.string.price_quote_saved), Snackbar.LENGTH_SHORT).show();
            finish();
        } else {
            Snackbar.make(binding.getRoot(), getString(R.string.error_saving), Snackbar.LENGTH_SHORT).show();
        }
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvQty, tvTotal;
            View btnPlus, btnMinus, btnRemove;
            VH(View v) { super(v);
                tvName   = v.findViewById(R.id.tv_product_name);
                tvPrice  = v.findViewById(R.id.tv_price);
                tvQty    = v.findViewById(R.id.tv_quantity);
                tvTotal  = v.findViewById(R.id.tv_total);
                btnPlus  = v.findViewById(R.id.btn_increase);
                btnMinus = v.findViewById(R.id.btn_decrease);
                btnRemove = v.findViewById(R.id.btn_remove);
            }
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= cartItems.size()) return;
            CartItem item = cartItems.get(pos);
            if (h.tvName  != null) h.tvName.setText(item.name);
            if (h.tvPrice != null) h.tvPrice.setText(String.format(Locale.US, "%.2f %s", item.price, currency));
            if (h.tvQty   != null) h.tvQty.setText(String.valueOf(item.quantity));
            if (h.tvTotal != null) h.tvTotal.setText(String.format(Locale.US, "%.2f", item.price * item.quantity));
            if (h.btnPlus != null) h.btnPlus.setOnClickListener(v -> {
                item.quantity++;
                notifyItemChanged(h.getAdapterPosition());
                updateTotals();
            });
            if (h.btnMinus != null) h.btnMinus.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                    notifyItemChanged(h.getAdapterPosition());
                    updateTotals();
                }
            });
            if (h.btnRemove != null) h.btnRemove.setOnClickListener(v -> {
                int p = h.getAdapterPosition();
                if (p >= 0 && p < cartItems.size()) {
                    cartItems.remove(p);
                    notifyItemRemoved(p);
                    updateTotals();
                }
            });
        }
        @Override public int getItemCount() { return cartItems.size(); }
    }
}
