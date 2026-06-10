package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class ActivitySectionHubActivity extends BaseActivity {

    public static final String EXTRA_SECTION       = "hub_section";
    public static final String SECTION_CUSTOMERS   = "customers";
    public static final String SECTION_SUPPLIERS   = "suppliers";
    public static final String SECTION_INVENTORY   = "inventory";
    public static final String SECTION_TREASURY    = "treasury";
    public static final String SECTION_REPORTS     = "reports";

    static class HubItem {
        final int icon;
        final int label;
        final Class<?> destination;
        HubItem(int icon, int label, Class<?> destination) {
            this.icon = icon; this.label = label; this.destination = destination;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_hub);

        View root = findViewById(android.R.id.content);
        if (root != null) applyWindowInsets(root);

        String section = getIntent().getStringExtra(EXTRA_SECTION);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
            toolbar.setTitle(getTitleForSection(section));
        }

        List<HubItem> items = getItemsForSection(section);
        RecyclerView rv = findViewById(R.id.rv_hub_items);
        if (rv != null) {
            rv.setLayoutManager(new GridLayoutManager(this, 2));
            rv.setAdapter(new HubAdapter(items));
        }
    }

    private String getTitleForSection(String section) {
        if (section == null) return getString(R.string.app_name);
        switch (section) {
            case SECTION_CUSTOMERS: return getString(R.string.customers_title);
            case SECTION_SUPPLIERS: return getString(R.string.suppliers_title);
            case SECTION_INVENTORY: return getString(R.string.hub_inventory);
            case SECTION_TREASURY:  return getString(R.string.hub_treasury);
            case SECTION_REPORTS:   return getString(R.string.reports_title);
            default:                return getString(R.string.app_name);
        }
    }

    private List<HubItem> getItemsForSection(String section) {
        List<HubItem> items = new ArrayList<>();
        if (section == null) return items;
        switch (section) {
            case SECTION_CUSTOMERS:
                items.add(new HubItem(R.drawable.ic_people,         R.string.customers_title,           ActivityCustomersActivity.class));
                items.add(new HubItem(R.drawable.ic_invoices,       R.string.invoices_title,             ActivityInvoicesActivity.class));
                items.add(new HubItem(R.drawable.ic_receive,        R.string.return_sale,                ActivityReturnActivity.class));
                items.add(new HubItem(R.drawable.ic_money,          R.string.debt_title,                 ActivityDebtActivity.class));
                items.add(new HubItem(R.drawable.ic_time,           R.string.hub_installments,           ActivityInstallmentsActivity.class));
                items.add(new HubItem(R.drawable.ic_backup,         R.string.hub_checks,                 ActivityChecksActivity.class));
                items.add(new HubItem(R.drawable.ic_person,         R.string.hub_customer_remaining,     ActivityCustomerRemainingActivity.class));
                items.add(new HubItem(R.drawable.ic_invoices,       R.string.hub_price_quotes,           ActivityPriceQuotesActivity.class));
                break;
            case SECTION_SUPPLIERS:
                items.add(new HubItem(R.drawable.ic_local_shipping, R.string.suppliers_title,            ActivitySuppliersActivity.class));
                items.add(new HubItem(R.drawable.ic_local_shipping, R.string.purchase_orders_title,      ActivityPurchaseOrderActivity.class));
                items.add(new HubItem(R.drawable.ic_suppliers,      R.string.hub_supplier_remaining,     ActivitySupplierRemainingActivity.class));
                break;
            case SECTION_INVENTORY:
                items.add(new HubItem(R.drawable.ic_inventory,      R.string.products_title,             ActivityProductsActivity.class));
                items.add(new HubItem(R.drawable.ic_inventory,      R.string.hub_stock_count,            ActivityStockCountActivity.class));
                items.add(new HubItem(R.drawable.ic_time,           R.string.hub_expiry,                 ActivityExpiryDashboardActivity.class));
                break;
            case SECTION_TREASURY:
                items.add(new HubItem(R.drawable.ic_money,          R.string.hub_wallet,                 ActivityWalletActivity.class));
                items.add(new HubItem(R.drawable.ic_money,          R.string.hub_cash_drawer,            ActivityCashDrawerActivity.class));
                items.add(new HubItem(R.drawable.ic_money,          R.string.expenses_title,             ActivityExpensesActivity.class));
                items.add(new HubItem(R.drawable.ic_time,           R.string.shifts_title,               ActivityShiftActivity.class));
                break;
            case SECTION_REPORTS:
                items.add(new HubItem(R.drawable.ic_reports,        R.string.reports_title,              ActivityReportsActivity.class));
                items.add(new HubItem(R.drawable.ic_reports,        R.string.business_insights_title,    ActivityBusinessInsightsActivity.class));
                break;
        }
        return items;
    }

    private class HubAdapter extends RecyclerView.Adapter<HubAdapter.VH> {
        private final List<HubItem> items;
        HubAdapter(List<HubItem> items) { this.items = items; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hub_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            HubItem item = items.get(position);
            holder.icon.setImageResource(item.icon);
            holder.label.setText(item.label);
            holder.card.setOnClickListener(v ->
                startActivity(new Intent(ActivitySectionHubActivity.this, item.destination)));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final ImageView icon;
            final TextView label;
            VH(View v) {
                super(v);
                card  = (MaterialCardView) v;
                icon  = v.findViewById(R.id.iv_hub_icon);
                label = v.findViewById(R.id.tv_hub_label);
            }
        }
    }
}
