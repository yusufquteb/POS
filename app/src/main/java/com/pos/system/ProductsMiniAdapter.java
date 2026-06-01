package com.pos.system;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * ProductsMiniAdapter - Adapter for displaying products in a compact list
 * Used in ActivityAddProductActivity to show recently added products
 * 
 * @author POS System
 * @version 1.0
 * @since 2026-02-14
 */
public class ProductsMiniAdapter extends RecyclerView.Adapter<ProductsMiniAdapter.ViewHolder> {

    private Context context;
    private List<HashMap<String, String>> productsList;
    private OnProductClickListener listener;

    /**
     * Interface for handling product item clicks
     */
    public interface OnProductClickListener {
        void onProductClick(HashMap<String, String> product);
    }

    /**
     * Constructor
     * @param context Context
     * @param productsList List of products to display
     */
    public ProductsMiniAdapter(Context context, List<HashMap<String, String>> productsList) {
        this.context = context;
        this.productsList = productsList;
    }

    /**
     * Set click listener for product items
     * @param listener Click listener
     */
    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_product_mini, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HashMap<String, String> product = productsList.get(position);
        
        // Set product name
        String name = product.get("name");
        holder.tvProductName.setText(name != null ? name : "N/A");
        
        // Set barcode
        String barcode = product.get("barcode");
        holder.tvBarcode.setText(barcode != null ? barcode : "N/A");
        
        // Set price with currency formatting
        String price = product.get("price");
        if (price != null) {
            try {
                double priceValue = Double.parseDouble(price);
                holder.tvPrice.setText(String.format(Locale.getDefault(), "%.2f", priceValue));
            } catch (NumberFormatException e) {
                holder.tvPrice.setText(price);
            }
        } else {
            holder.tvPrice.setText("0.00");
        }
        
        // Set quantity
        String qty = product.get("qty");
        holder.tvQty.setText(qty != null ? qty : "0");
        
        // Handle click event
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productsList != null ? productsList.size() : 0;
    }

    /**
     * ViewHolder class for product items
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        TextView tvBarcode;
        TextView tvPrice;
        TextView tvQty;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvBarcode = itemView.findViewById(R.id.tv_barcode);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvQty = itemView.findViewById(R.id.tv_qty);
        }
    }
}
