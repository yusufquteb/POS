package com.pos.system;

import com.pos.system.BaseActivity;

import android.content.Intent;
import android.util.Log;
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
            showSnackbar("خطأ في تحميل الفاتورة", true);
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
            showSnackbar("لم يتم العثور على الفاتورة", true);
            finish();
            return;
        }
        
        // بيانات الفاتورة
        if (tvInvoiceNumber != null) {
            String invNum = safeObjStr(invoice, "invoice_number", String.valueOf(invoiceId));
            tvInvoiceNumber.setText("فاتورة #" + invNum);
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
        
        // الإجماليات — subtotal (قبل الخصم/الضريبة)، total (الإجمالي النهائي)
        double subtotal   = safeDouble(invoice.get("subtotal"));
        double discount   = safeDouble(invoice.get("discount"));
        double tax        = safeDouble(invoice.get("tax"));
        double finalTotal = safeDouble(invoice.get("total"));
        
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
        if (items == null) items = new ArrayList<>();
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
        try {
            HashMap<String, Object> invoice = dbHelper.getInvoiceById(invoiceId);
            if (invoice == null) { showSnackbar("⚠️ لم يتم العثور على الفاتورة", true); return; }
            int customerId = invoice.containsKey("customer_id") ? safeInt(invoice.get("customer_id")) : 0;
            boolean ok = new InvoicePrinter(this).printInvoice(invoiceId, customerId);
            showSnackbar(ok ? "✓ تمت الطباعة بنجاح"
                            : "⚠️ فشلت الطباعة — تحقق من إعدادات الطابعة", !ok);
        } catch (Exception e) {
            android.util.Log.e("InvDetails", "printInvoice: " + e.getMessage(), e);
            showSnackbar("❌ خطأ في الطباعة", true);
        }
    }
    
    private void sendViaWhatsApp() {
        try {
            HashMap<String, Object> invoice = dbHelper.getInvoiceById(invoiceId);
            if (invoice == null) return;
            
            int customerId = invoice.containsKey("customer_id") ? safeInt(invoice.get("customer_id")) : 0;
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
        HashMap<String, String> storeSettings = dbHelper.getStoreSettings();
        String storeName = storeSettings != null ? storeSettings.getOrDefault("name", "متجرنا") : "متجرنا";
        String currency  = storeSettings != null ? storeSettings.getOrDefault("currency", "ج.م")  : "ج.م";

        String invoiceNum = safeObjStr(invoice, "invoice_number", String.valueOf(invoiceId));
        String createdAt  = safeObjStr(invoice, "created_at", "");
        String payMethod  = paymentLabel(safeObjStr(invoice, "payment_method", "نقدي"));

        double subtotal   = safeDouble(invoice.get("subtotal"));
        double discount   = safeDouble(invoice.get("discount"));
        double tax        = safeDouble(invoice.get("tax"));
        double finalTotal = safeDouble(invoice.get("total"));

        StringBuilder sb = new StringBuilder();
        sb.append("🛒 *فاتورة من ").append(storeName).append("*\n\n");
        sb.append("📋 رقم الفاتورة: ").append(invoiceNum).append("\n");
        if (!createdAt.isEmpty())
            sb.append("📅 التاريخ: ").append(formatDate(createdAt)).append("\n");
        if (customer != null)
            sb.append("👤 العميل: ").append(safeObjStr(customer, "name", "")).append("\n");
        sb.append("\n");

        sb.append("*المنتجات:*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");

        for (HashMap<String, Object> item : items) {
            String name      = safeObjStr(item, "name", "---");
            int    qty       = safeInt(item.get("qty"));
            double price     = safeDouble(item.get("price"));
            double lineTotal = safeDouble(item.get("total"));
            if (lineTotal <= 0) lineTotal = price * qty;

            sb.append("• ").append(name).append("\n");
            sb.append("  ").append(qty).append(" × ")
              .append(String.format(Locale.getDefault(), "%.2f", price))
              .append(" = ")
              .append(String.format(Locale.getDefault(), "%.2f %s", lineTotal, currency))
              .append("\n\n");
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format(Locale.getDefault(), "المجموع الفرعي: %.2f %s\n", subtotal, currency));
        if (discount > 0)
            sb.append(String.format(Locale.getDefault(), "الخصم: -%.2f %s\n", discount, currency));
        if (tax > 0)
            sb.append(String.format(Locale.getDefault(), "الضريبة: +%.2f %s\n", tax, currency));
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format(Locale.getDefault(), "*الإجمالي: %.2f %s*\n", finalTotal, currency));
        sb.append("💳 طريقة الدفع: ").append(payMethod).append("\n\n");
        sb.append("شكراً لتعاملكم معنا 🙏\n");
        sb.append("نتمنى لكم يوماً سعيداً 😊");

        return sb.toString();
    }

    private String paymentLabel(String code) {
        if (code == null) return "نقدي";
        switch (code.toLowerCase(Locale.ROOT)) {
            case "vodafone":  return "فودافون كاش";
            case "instapay":  return "انستاباي";
            case "card":      return "بطاقة بنكية";
            default:          return "نقدي";
        }
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
            String name  = safeObjStr(item, "name", "---");
            int    qty   = safeInt(item.get("qty"));
            double price = safeDouble(item.get("price"));
            double lineTotal = safeDouble(item.get("total"));
            if (lineTotal <= 0) lineTotal = price * qty;
            holder.tvName.setText(name);
            holder.tvDetails.setText(
                String.format(Locale.getDefault(), "الكمية: %d × %.2f = %.2f", qty, price, lineTotal));
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
        if (dbHelper != null) dbHelper.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Safe type helpers
    // ─────────────────────────────────────────────────────────────────────────

    private double safeDouble(Object o) {
        if (o instanceof Double)  return (double) o;
        if (o instanceof Float)   return ((Float) o).doubleValue();
        if (o instanceof Integer) return ((Integer) o).doubleValue();
        if (o instanceof Long)    return ((Long) o).doubleValue();
        if (o instanceof String)  { try { return Double.parseDouble((String) o); } catch (Exception ignored) {} }
        return 0.0;
    }

    private int safeInt(Object o) {
        if (o instanceof Integer) return (int) o;
        if (o instanceof Long)    return ((Long) o).intValue();
        if (o instanceof Double)  return ((Double) o).intValue();
        if (o instanceof String)  { try { return Integer.parseInt((String) o); } catch (Exception ignored) {} }
        return 0;
    }

    private String safeObjStr(HashMap<String, Object> map, String key, String fallback) {
        if (map == null) return fallback;
        Object v = map.get(key);
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
