package com.pos.system;

import android.content.Context;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class InvoicePrinter {

    private static final String TAG = "InvoicePrinter";

    private final Context context;
    private final DBHelper dbHelper;

    public InvoicePrinter(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    public boolean printInvoice(long invoiceId, int customerId) {
        try {
            HashMap<String, String> storeSettings  = dbHelper.getStoreSettings();
            HashMap<String, Object> customer        = dbHelper.getCustomerById(customerId);
            HashMap<String, Object> invoice         = dbHelper.getInvoiceById(invoiceId);
            ArrayList<HashMap<String, Object>> items = dbHelper.getInvoiceItems(invoiceId);

            if (invoice == null) {
                Log.e(TAG, "Invoice not found: " + invoiceId);
                return false;
            }

            HashMap<String, String> printerSettings = dbHelper.getPrinterSettings();
            String paperWidth      = printerSettings.getOrDefault("paper_width", "80mm");
            String connectionType  = printerSettings.getOrDefault("connection_type", "bluetooth");

            String content = buildInvoiceContent(storeSettings, customer, invoice, items, paperWidth);

            if ("bluetooth".equals(connectionType)) {
                return printViaBluetooth(content, paperWidth);
            } else {
                return printViaUSB(content, paperWidth);
            }

        } catch (Exception e) {
            Log.e(TAG, "printInvoice error: " + e.getMessage(), e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Receipt builder
    // ─────────────────────────────────────────────────────────────────────────

    private String buildInvoiceContent(
        HashMap<String, String>         storeSettings,
        HashMap<String, Object>         customer,
        HashMap<String, Object>         invoice,
        ArrayList<HashMap<String, Object>> items,
        String                          paperWidth
    ) {
        int    width    = "80mm".equals(paperWidth) ? 48 : 32;
        String currency = storeSettings != null
            ? storeSettings.getOrDefault("currency", "ج.م") : "ج.م";

        StringBuilder sb = new StringBuilder();

        // 1. Logo
        if (storeSettings != null) {
            String logo = storeSettings.get("logo");
            if (logo != null && !logo.isEmpty())
                sb.append("[C]<img>").append(logo).append("</img>\n");
        }

        // 2. Store header
        String storeName    = safeStr(storeSettings, "name", "المتجر");
        String storeAddress = safeStr(storeSettings, "address", "");
        String storePhone   = safeStr(storeSettings, "phone", "");
        String taxNumber    = safeStr(storeSettings, "tax", "");

        sb.append(centerText(storeName, width)).append("\n");
        if (!storeAddress.isEmpty())
            sb.append(centerText(storeAddress, width)).append("\n");
        if (!storePhone.isEmpty())
            sb.append(centerText("هاتف: " + storePhone, width)).append("\n");
        if (!taxNumber.isEmpty())
            sb.append(centerText("الرقم الضريبي: " + taxNumber, width)).append("\n");

        sb.append(repeat("=", width)).append("\n");

        // 3. Invoice header
        String invoiceNumber = safeObjStr(invoice, "invoice_number",
            String.valueOf(invoice.get("id")));
        String invoiceDate   = formatInvoiceDate(safeObjStr(invoice, "created_at", ""));

        sb.append("رقم الفاتورة: ").append(invoiceNumber).append("\n");
        sb.append("التاريخ:       ").append(invoiceDate).append("\n");

        // 4. Customer info
        if (customer != null) {
            String custName  = safeObjStr(customer, "name", "");
            String custPhone = safeObjStr(customer, "phone", "");
            if (!custName.isEmpty())  sb.append("العميل: ").append(custName).append("\n");
            if (!custPhone.isEmpty()) sb.append("الهاتف: ").append(custPhone).append("\n");
        }

        sb.append(repeat("=", width)).append("\n");

        // 5. Items table
        sb.append(String.format("%-20s %5s %8s %10s\n", "الصنف", "الكمية", "السعر", "الإجمالي"));
        sb.append(repeat("-", width)).append("\n");

        for (HashMap<String, Object> item : items) {
            String name  = truncate(safeObjStr(item, "name", "---"), 20);
            int    qty   = safeInt(item.get("qty"), 1);
            double price = safeDouble(item.get("price"));
            double lineTotal = item.containsKey("total")
                ? safeDouble(item.get("total"))
                : price * qty;

            sb.append(String.format("%-20s %5d %8.2f %10.2f\n", name, qty, price, lineTotal));
        }

        sb.append(repeat("=", width)).append("\n");

        // 6. Totals
        double subtotal  = safeDouble(invoice.get("subtotal"));
        double discount  = safeDouble(invoice.get("discount"));
        double tax       = safeDouble(invoice.get("tax"));
        double total     = safeDouble(invoice.get("total"));

        // If subtotal not stored, derive it
        if (subtotal <= 0) subtotal = total + discount - tax;

        sb.append(rightAlign("المجموع الفرعي: " + formatMoney(subtotal, currency), width)).append("\n");

        if (discount > 0)
            sb.append(rightAlign("الخصم: -" + formatMoney(discount, currency), width)).append("\n");

        if (tax > 0)
            sb.append(rightAlign("الضريبة: +" + formatMoney(tax, currency), width)).append("\n");

        sb.append(repeat("=", width)).append("\n");
        sb.append(rightAlign("الإجمالي النهائي: " + formatMoney(total, currency), width)).append("\n");
        sb.append(repeat("=", width)).append("\n");

        // 7. Payment method
        String rawMethod  = safeObjStr(invoice, "payment_method", "نقدي");
        String methodLabel = paymentLabel(rawMethod);
        sb.append("طريقة الدفع: ").append(methodLabel).append("\n");

        // 8. Loyalty points (shown only when customer has a balance)
        if (customer != null) {
            String customerId = safeObjStr(customer, "id", "");
            if (!customerId.isEmpty()) {
                try {
                    int points = dbHelper.getCustomerLoyaltyPoints(customerId);
                    if (points > 0)
                        sb.append("نقاط الولاء: ").append(points).append(" نقطة\n");
                } catch (Exception ignored) {}
            }
        }

        sb.append(repeat("-", width)).append("\n");

        // 9. Thank-you message
        sb.append("\n");
        sb.append(centerText("شكراً لتعاملكم معنا", width)).append("\n");
        sb.append(centerText("نتمنى لكم يوماً سعيداً", width)).append("\n");

        // 10. QR code
        String qrData = "INV-" + invoiceNumber + "-" + total;
        sb.append("\n[C]<qrcode size='20'>").append(qrData).append("</qrcode>\n");

        // 11. Paper cut
        sb.append("\n\n\n[CUT]\n");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Bluetooth printing
    // ─────────────────────────────────────────────────────────────────────────

    private boolean printViaBluetooth(String content, String paperWidth) {
        try {
            android.bluetooth.BluetoothAdapter adapter =
                android.bluetooth.BluetoothAdapter.getDefaultAdapter();
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
            com.dantsu.escposprinter.EscPosPrinter printer =
                new com.dantsu.escposprinter.EscPosPrinter(
                    com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
                        .selectFirstPaired(),
                    203, paperWidthMm, 32
                );
            printer.printFormattedText(content);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Bluetooth print error: " + e.getMessage(), e);
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  USB printing — not yet supported (Phase 4)
    // ─────────────────────────────────────────────────────────────────────────

    private boolean printViaUSB(String content, String paperWidth) {
        Log.w(TAG, "USB printing not yet supported; use Bluetooth");
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String paymentLabel(String code) {
        if (code == null) return "نقدي";
        switch (code.toLowerCase(Locale.ROOT)) {
            case "vodafone":  return "فودافون كاش";
            case "instapay":  return "انستاباي";
            case "card":      return "بطاقة بنكية";
            case "cash":
            default:          return "نقدي";
        }
    }

    private String formatInvoiceDate(String createdAt) {
        if (createdAt == null || createdAt.isEmpty())
            return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(new Date());
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("yyyy/MM/dd HH:mm",     Locale.getDefault());
            Date parsed = in.parse(createdAt);
            return parsed != null ? out.format(parsed) : createdAt;
        } catch (Exception e) {
            return createdAt;
        }
    }

    private String formatMoney(double amount, String currency) {
        return String.format(Locale.getDefault(), "%.2f %s", amount, currency);
    }

    private String centerText(String text, int width) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() >= width) return text.substring(0, width);
        int pad = (width - text.length()) / 2;
        return repeat(" ", pad) + text;
    }

    private String rightAlign(String text, int width) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() >= width) return text;
        return repeat(" ", width - text.length()) + text;
    }

    private String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) : text;
    }

    private double safeDouble(Object o) {
        if (o instanceof Double)  return (double) o;
        if (o instanceof Float)   return ((Float) o).doubleValue();
        if (o instanceof Integer) return ((Integer) o).doubleValue();
        if (o instanceof Long)    return ((Long) o).doubleValue();
        if (o instanceof String)  {
            try { return Double.parseDouble((String) o); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    private int safeInt(Object o, int fallback) {
        if (o instanceof Integer) return (int) o;
        if (o instanceof Long)    return ((Long) o).intValue();
        if (o instanceof Double)  return ((Double) o).intValue();
        if (o instanceof String)  {
            try { return Integer.parseInt((String) o); } catch (Exception ignored) {}
        }
        return fallback;
    }

    /** Read a String value from a HashMap<String,Object>. */
    private String safeObjStr(HashMap<String, Object> map, String key, String fallback) {
        if (map == null) return fallback;
        Object v = map.get(key);
        if (v == null) return fallback;
        String s = v.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    /** Read a String value from a HashMap<String,String>. */
    private String safeStr(HashMap<String, String> map, String key, String fallback) {
        if (map == null) return fallback;
        String v = map.get(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
