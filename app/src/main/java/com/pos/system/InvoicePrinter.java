package com.pos.system;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InvoicePrinter {
    
    private static final String TAG = "InvoicePrinter";
    private Context context;
    private DBHelper dbHelper;
    
    public InvoicePrinter(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }
    
    /**
     * طباعة فاتورة
     */
    public boolean printInvoice(long invoiceId, int customerId) {
        try {
            // 1. جلب بيانات المتجر
            HashMap<String, String> storeSettings = dbHelper.getStoreSettings();
            
            // 2. جلب بيانات العميل
            HashMap<String, Object> customer = dbHelper.getCustomerById(customerId);
            
            // 3. جلب بيانات الفاتورة
            HashMap<String, Object> invoice = dbHelper.getInvoiceById(invoiceId);
            
            // 4. جلب عناصر الفاتورة
            ArrayList<HashMap<String, Object>> items = dbHelper.getInvoiceItems(invoiceId);
            
            // 5. جلب إعدادات الطباعة
            HashMap<String, String> printerSettings = dbHelper.getPrinterSettings();
            String paperWidth = printerSettings.get("paper_width");
            String connectionType = printerSettings.get("connection_type");
            
            // 6. بناء محتوى الفاتورة
            String invoiceContent = buildInvoiceContent(
                storeSettings, customer, invoice, items, paperWidth
            );
            
            // 7. الطباعة حسب نوع الاتصال
            if ("bluetooth".equals(connectionType)) {
                return printViaBluetooth(invoiceContent, paperWidth);
            } else {
                return printViaUSB(invoiceContent, paperWidth);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error printing invoice: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * بناء محتوى الفاتورة
     */
    private String buildInvoiceContent(
        HashMap<String, String> storeSettings,
        HashMap<String, Object> customer,
        HashMap<String, Object> invoice,
        ArrayList<HashMap<String, Object>> items,
        String paperWidth
    ) {
        StringBuilder sb = new StringBuilder();
        
        int width = "80mm".equals(paperWidth) ? 48 : 32; // عدد الأحرف في السطر
        
        // 1. الشعار (إذا وجد)
        String logoPath = storeSettings.get("logo");
        if (logoPath != null && !logoPath.isEmpty()) {
            sb.append("[C]<img>").append(logoPath).append("</img>\n");
        }
        
        // 2. معلومات المتجر
        sb.append(centerText(storeSettings.get("name"), width)).append("\n");
        sb.append(centerText(storeSettings.get("address"), width)).append("\n");
        sb.append(centerText("هاتف: " + storeSettings.get("phone"), width)).append("\n");
        
        String taxNumber = storeSettings.get("tax");
        if (taxNumber != null && !taxNumber.isEmpty()) {
            sb.append(centerText("الرقم الضريبي: " + taxNumber, width)).append("\n");
        }
        
        sb.append(repeat("=", width)).append("\n");
        
        // 3. بيانات الفاتورة
        sb.append("رقم الفاتورة: ").append(invoice.get("id")).append("\n");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateTime = sdf.format(new Date());
        sb.append("التاريخ: ").append(dateTime).append("\n");
        
        // 4. بيانات العميل
        if (customer != null) {
            sb.append("العميل: ").append(customer.get("name")).append("\n");
            sb.append("الهاتف: ").append(customer.get("phone")).append("\n");
        }
        
        sb.append(repeat("=", width)).append("\n");
        
        // 5. جدول الأصناف
        sb.append(String.format("%-20s %5s %8s %10s\n", "الصنف", "الكمية", "السعر", "الإجمالي"));
        sb.append(repeat("-", width)).append("\n");
        
        for (HashMap<String, Object> item : items) {
            String name = truncate(item.get("name").toString(), 20);
            int qty = (int) item.get("qty");
            double price = (double) item.get("price");
            double total = price * qty;
            
            sb.append(String.format("%-20s %5d %8.2f %10.2f\n", 
                name, qty, price, total));
        }
        
        sb.append(repeat("=", width)).append("\n");
        
        // 6. الإجماليات
        double subtotal = (double) invoice.get("total");
        double discount = (double) invoice.get("discount");
        double tax = invoice.containsKey("tax") ? (double) invoice.get("tax") : 0;
        double finalTotal = (double) invoice.get("total");
        
        sb.append(String.format("%30s %15.2f\n", "المجموع الفرعي:", subtotal));
        
        if (discount > 0) {
            sb.append(String.format("%30s -%15.2f\n", "الخصم:", discount));
        }
        
        if (tax > 0) {
            sb.append(String.format("%30s %15.2f\n", "الضريبة:", tax));
        }
        
        sb.append(repeat("=", width)).append("\n");
        sb.append(String.format("%30s %15.2f\n", "الإجمالي النهائي:", finalTotal));
        
        sb.append(repeat("=", width)).append("\n");
        
        // 7. رسالة الشكر
        sb.append("\n");
        sb.append(centerText("شكراً لتعاملكم معنا", width)).append("\n");
        sb.append(centerText("نتمنى لكم يوماً سعيداً", width)).append("\n");
        
        // 8. QR Code
        String qrData = generateQRData(invoice);
        sb.append("\n[C]<qrcode size='20'>").append(qrData).append("</qrcode>\n");
        
        // 9. قطع الورق
        sb.append("\n\n\n[CUT]\n");
        
        return sb.toString();
    }
    
    /**
     * مركز النص
     */
    private String centerText(String text, int width) {
        if (text == null || text.isEmpty()) return "";
        
        int padding = (width - text.length()) / 2;
        if (padding < 0) return text.substring(0, width);
        
        return repeat(" ", padding) + text;
    }
    
    /**
     * تكرار حرف
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * قص النص
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
    
    /**
     * توليد بيانات QR Code
     */
    private String generateQRData(HashMap<String, Object> invoice) {
        // يمكن تخصيص البيانات حسب الحاجة
        return "INV-" + invoice.get("id") + "-" + System.currentTimeMillis();
    }
    
    /**
     * الطباعة عبر Bluetooth
     */
    private boolean printViaBluetooth(String content, String paperWidth) {
        try {
            android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) {
                Log.e(TAG, "Bluetooth not available or disabled");
                return false;
            }
            HashMap<String, String> printerSettings = dbHelper.getPrinterSettings();
            String address = printerSettings.getOrDefault("printer_address", "");
            if (address.isEmpty()) {
                Log.e(TAG, "No printer address configured");
                return false;
            }
            int paperWidthMm = "58mm".equals(paperWidth) ? 58 : 80;
            com.dantsu.escposprinter.EscPosPrinter printer = new com.dantsu.escposprinter.EscPosPrinter(
                com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections.selectFirstPaired(),
                203, paperWidthMm, 32
            );
            printer.printFormattedText(content);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Bluetooth print error: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * الطباعة عبر USB
     */
    private boolean printViaUSB(String content, String paperWidth) {
        // سيتم التنفيذ في المرحلة التالية
        Log.d(TAG, "Printing via USB: " + paperWidth);
        return false;
    }
}