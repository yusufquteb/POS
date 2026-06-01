package com.pos.system;

import com.pos.system.BaseActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * ActivityInvoiceDetailsActivity - صفحة تفاصيل الفاتورة المحسّنة
 * تاريخ الإنشاء: 2026-02-09
 * 
 * ✅ التحسينات:
 * - استخدام RecyclerView بشكل صحيح
 * - معالجة آمنة للبيانات
 * - إصلاح مشكلة Adapter
 */
public class ActivityInvoiceDetailsActivity extends BaseActivity {
    
    private DBHelper dbHelper;
    private long invoiceId;
    
    private TextView tvInvoiceNumber, tvDate;
    private TextView tvCustomerName, tvCustomerPhone;
    private TextView tvSubtotal, tvDiscount, tvTax, tvFinalTotal;
    private RecyclerView listItems;
    private View layoutDiscount, layoutTax;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_details);
        
        dbHelper = new DBHelper(this);
        invoiceId = getIntent().getLongExtra("invoice_id", 0);
        
        if (invoiceId == 0) {
            Toast.makeText(this, "خطأ في تحميل الفاتورة", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupToolbar();
        loadInvoiceData();
        setupButtons();
    }
    
    private void initViews() {
        tvInvoiceNumber = findViewById(R.id.tv_invoice_number);
        tvDate = findViewById(R.id.tv_date);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerPhone = findViewById(R.id.tv_customer_phone);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvTax = findViewById(R.id.tv_tax);
        tvFinalTotal = findViewById(R.id.tv_final_total);
        listItems = findViewById(R.id.recycler_items);
        layoutDiscount = findViewById(R.id.layout_discount);
        layoutTax = findViewById(R.id.layout_tax);
        
        // ✅ إعداد RecyclerView
        if (listItems != null) {
            listItems.setLayoutManager(new LinearLayoutManager(this));
            listItems.setHasFixedSize(true);
        }
    }
    
    private void setupToolbar() {
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> finish());
        }
    }
    
    private void loadInvoiceData() {
        HashMap<String, Object> invoice = dbHelper.getInvoiceById(invoiceId);
        
        if (invoice == null) {
            Toast.makeText(this, "لم يتم العثور على الفاتورة", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // بيانات الفاتورة
        if (tvInvoiceNumber != null) {
            tvInvoiceNumber.setText("فاتورة #" + invoiceId);
        }
        
        if (tvDate != null && invoice.containsKey("created_at")) {
            tvDate.setText(formatDate(invoice.get("created_at").toString()));
        }
        
        // بيانات العميل
        if (tvCustomerName != null && invoice.containsKey("customer_name")) {
            tvCustomerName.setText("الاسم: " + invoice.get("customer_name"));
        }
        
        if (tvCustomerPhone != null && invoice.containsKey("customer_phone")) {
            tvCustomerPhone.setText("الهاتف: " + invoice.get("customer_phone"));
        }
        
        // الإجماليات
        double subtotal = invoice.containsKey("total") ? (double) invoice.get("total") : 0;
        double discount = invoice.containsKey("discount") ? (double) invoice.get("discount") : 0;
        double tax = invoice.containsKey("tax") ? (double) invoice.get("tax") : 0;
        double finalTotal = invoice.containsKey("final_total") ? (double) invoice.get("final_total") : 0;
        
        if (tvSubtotal != null) {
            tvSubtotal.setText(String.format("%.2f ج.م", subtotal));
        }
        
        // إخفاء/إظهار الخصم
        if (discount > 0) {
            if (layoutDiscount != null) layoutDiscount.setVisibility(View.VISIBLE);
            if (tvDiscount != null) tvDiscount.setText(String.format("-%.2f ج.م", discount));
        } else {
            if (layoutDiscount != null) layoutDiscount.setVisibility(View.GONE);
        }
        
        // إخفاء/إظهار الضريبة
        if (tax > 0) {
            if (layoutTax != null) layoutTax.setVisibility(View.VISIBLE);
            if (tvTax != null) tvTax.setText(String.format("+%.2f ج.م", tax));
        } else {
            if (layoutTax != null) layoutTax.setVisibility(View.GONE);
        }
        
        if (tvFinalTotal != null) {
            tvFinalTotal.setText(String.format("%.2f ج.م", finalTotal));
        }
        
        // تحميل العناصر
        ArrayList<HashMap<String, Object>> items = dbHelper.getInvoiceItems(invoiceId);
        if (listItems != null) {
            ItemsAdapter adapter = new ItemsAdapter(items);
            listItems.setAdapter(adapter);
        }
    }
    
    private void setupButtons() {
        View btnPrint = findViewById(R.id.btn_print);
        if (btnPrint != null) {
            btnPrint.setOnClickListener(v -> printInvoice());
        }
        
        View btnWhatsapp = findViewById(R.id.btn_whatsapp);
        if (btnWhatsapp != null) {
            btnWhatsapp.setOnClickListener(v -> sendViaWhatsApp());
        }
    }
    
    private void printInvoice() {
        // سيتم التنفيذ لاحقاً
        showSnackbar("ℹ الطباعة قيد التطوير", false);
    }
    
    private void sendViaWhatsApp() {
        try {
            HashMap<String, Object> invoice = dbHelper.getInvoiceById(invoiceId);
            if (invoice == null) return;
            
            int customerId = invoice.containsKey("customer_id") ? (int) invoice.get("customer_id") : 0;
            HashMap<String, Object> customer = dbHelper.getCustomerById(customerId);
            
            if (customer == null || !customer.containsKey("phone")) {
                showSnackbar("❌ لا يوجد رقم هاتف للعميل", true);
                return;
            }
            
            String phone = customer.get("phone").toString();
            if (phone == null || phone.isEmpty() || phone.equals("00000000000")) {
                showSnackbar("❌ رقم هاتف غير صحيح", true);
                return;
            }
            
            ArrayList<HashMap<String, Object>> items = dbHelper.getInvoiceItems(invoiceId);
            String message = buildWhatsAppMessage(invoice, items, customer);
            
            String cleanPhone = phone.replaceAll("[^0-9+]", "");
            if (!cleanPhone.startsWith("+")) {
                cleanPhone = "+2" + cleanPhone;
            }
            
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + 
                         "&text=" + Uri.encode(message);
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            
        } catch (Exception e) {
            showSnackbar("❌ حدث خطأ في إرسال الفاتورة", true);
        }
    }
    
    private String buildWhatsAppMessage(HashMap<String, Object> invoice,
                                       ArrayList<HashMap<String, Object>> items,
                                       HashMap<String, Object> customer) {
        StringBuilder sb = new StringBuilder();
        
        HashMap<String, String> storeSettings = dbHelper.getStoreSettings();
        String storeName = storeSettings.containsKey("name") ? storeSettings.get("name") : "متجرنا";
        
        sb.append("🛒 *فاتورة من ").append(storeName).append("*\n\n");
        sb.append("📋 رقم الفاتورة: ").append(invoiceId).append("\n");
        sb.append("📅 التاريخ: ").append(formatDate(invoice.get("created_at").toString())).append("\n");
        sb.append("👤 العميل: ").append(customer.get("name")).append("\n\n");
        
        sb.append("*المنتجات:*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        
        for (HashMap<String, Object> item : items) {
            String name = item.get("product_name").toString();
            int qty = (int) item.get("quantity");
            double price = (double) item.get("price");
            double itemTotal = price * qty;
            
            sb.append("• ").append(name).append("\n");
            sb.append("  الكمية: ").append(qty);
            sb.append(" × ").append(String.format("%.2f", price));
            sb.append(" = ").append(String.format("%.2f ج.م", itemTotal)).append("\n\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        
        double subtotal = (double) invoice.get("total");
        double discount = invoice.containsKey("discount") ? (double) invoice.get("discount") : 0;
        double tax = invoice.containsKey("tax") ? (double) invoice.get("tax") : 0;
        double finalTotal = (double) invoice.get("final_total");
        
        sb.append("المجموع الفرعي: ").append(String.format("%.2f ج.م", subtotal)).append("\n");
        
        if (discount > 0) {
            sb.append("الخصم: -").append(String.format("%.2f ج.م", discount)).append("\n");
        }
        
        if (tax > 0) {
            sb.append("الضريبة: +").append(String.format("%.2f ج.م", tax)).append("\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("*الإجمالي: ").append(String.format("%.2f ج.م*", finalTotal)).append("\n\n");
        
        sb.append("شكراً لتعاملكم معنا 🙏\n");
        sb.append("نتمنى لكم يوماً سعيداً 😊");
        
        return sb.toString();
    }
    
    private String formatDate(String dateTime) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dateTime));
        } catch (Exception e) {
            return dateTime;
        }
    }
    
    private void showSnackbar(String msg, boolean error) {
        try {
            Snackbar s = Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG);
            s.setBackgroundTint(error ? 0xFFB3261E : 0xFF2E7D32);
            s.show();
        } catch (Exception e) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * ✅ RecyclerView Adapter محسّن للعناصر
     */
    private class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {
        private ArrayList<HashMap<String, Object>> items;
        
        ItemsAdapter(ArrayList<HashMap<String, Object>> items) {
            this.items = items != null ? items : new ArrayList<>();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, Object> item = items.get(position);
            
            String name = item.containsKey("product_name") ? item.get("product_name").toString() : "";
            int qty = item.containsKey("quantity") ? (int) item.get("quantity") : 0;
            double price = item.containsKey("price") ? (double) item.get("price") : 0;
            double total = price * qty;
            
            holder.tvName.setText(name);
            holder.tvDetails.setText(String.format("الكمية: %d × %.2f = %.2f ج.م", qty, price, total));
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvDetails;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(android.R.id.text1);
                tvDetails = itemView.findViewById(android.R.id.text2);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
