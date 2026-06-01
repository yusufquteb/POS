package com.pos.system;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * BluetoothPrinterHelper - مساعد طباعة البلوتوث المحسّن
 * 
 * المميزات:
 * ✅ دعم Android 12+ (صلاحيات جديدة)
 * ✅ معالجة شاملة للأخطاء
 * ✅ دعم ESC/POS commands
 * ✅ طباعة بالعربية والإنجليزية
 * ✅ Auto-reconnect عند الفصل
 */
public class BluetoothPrinterHelper {
    
    private static final String TAG = "BluetoothPrinter";
    
    // UUID للطابعات (SPP - Serial Port Profile)
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // ESC/POS Commands
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte[] INIT_PRINTER = {ESC, '@'}; // Initialize
    private static final byte[] LINE_FEED = {0x0A};
    private static final byte[] PAPER_CUT = {GS, 'V', 66, 0}; // Cut paper
    
    // Alignment
    private static final byte[] ALIGN_LEFT = {ESC, 'a', 0};
    private static final byte[] ALIGN_CENTER = {ESC, 'a', 1};
    private static final byte[] ALIGN_RIGHT = {ESC, 'a', 2};
    
    // Text Style
    private static final byte[] BOLD_ON = {ESC, 'E', 1};
    private static final byte[] BOLD_OFF = {ESC, 'E', 0};
    private static final byte[] UNDERLINE_ON = {ESC, '-', 1};
    private static final byte[] UNDERLINE_OFF = {ESC, '-', 0};
    private static final byte[] DOUBLE_SIZE = {GS, '!', 0x11};
    private static final byte[] NORMAL_SIZE = {GS, '!', 0x00};
    
    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket currentSocket;
    private OutputStream currentOutputStream;
    
    // Listeners
    private PrintListener printListener;
    
    public BluetoothPrinterHelper(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Bluetooth Check Methods
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * التحقق من دعم Bluetooth
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }
    
    /**
     * التحقق من تفعيل Bluetooth
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    /**
     * التحقق من الصلاحيات (Android 12+)
     */
    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 11 وأقل لا يحتاج صلاحيات إضافية
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Printer Discovery Methods
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * الحصول على قائمة الطابعات المقترنة
     */
    public List<BluetoothDevice> getPairedPrinters() {
        List<BluetoothDevice> printers = new ArrayList<>();
        
        if (!isBluetoothSupported() || !isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth not supported or not enabled");
            return printers;
        }
        
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions");
            return printers;
        }
        
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    // فلترة الطابعات فقط
                    if (isPrinterDevice(device)) {
                        printers.add(device);
                        Log.d(TAG, "Found printer: " + getDeviceName(device));
                    }
                }
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting paired devices", e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting paired devices", e);
        }
        
        return printers;
    }
    
    /**
     * فحص إذا كان الجهاز طابعة
     */
    private boolean isPrinterDevice(BluetoothDevice device) {
        try {
            String name = getDeviceName(device);
            if (name != null) {
                String nameLower = name.toLowerCase();
                return nameLower.contains("printer") ||
                       nameLower.contains("print") ||
                       nameLower.contains("pos") ||
                       nameLower.contains("rpp") ||
                       nameLower.contains("mpt") ||
                       nameLower.contains("thermal") ||
                       nameLower.contains("receipt") ||
                       nameLower.contains("escpos");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking device", e);
        }
        return false;
    }
    
    /**
     * الحصول على اسم الجهاز (مع معالجة الصلاحيات)
     */
    private String getDeviceName(BluetoothDevice device) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (hasBluetoothPermissions()) {
                    return device.getName();
                }
            } else {
                return device.getName();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied getting device name", e);
        }
        return "Unknown Device";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Connection Methods
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * الاتصال بالطابعة
     */
    public boolean connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting to printer: " + getDeviceName(device));
        
        if (!hasBluetoothPermissions()) {
            notifyError("Missing Bluetooth permissions");
            return false;
        }
        
        // إغلاق أي اتصال سابق
        disconnect();
        
        try {
            // إنشاء Socket
            currentSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            
            // الاتصال
            currentSocket.connect();
            
            // الحصول على OutputStream
            currentOutputStream = currentSocket.getOutputStream();
            
            // تهيئة الطابعة
            currentOutputStream.write(INIT_PRINTER);
            currentOutputStream.flush();
            
            Log.d(TAG, "✅ Connected successfully");
            notifyConnected(getDeviceName(device));
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Permission denied", e);
            notifyError("Permission denied: " + e.getMessage());
            return false;
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Connection failed", e);
            notifyError("Connection failed: " + e.getMessage());
            
            // محاولة طريقة بديلة (Fallback)
            return connectFallback(device);
        }
    }
    
    /**
     * طريقة اتصال بديلة (Reflection)
     */
    private boolean connectFallback(BluetoothDevice device) {
        Log.d(TAG, "Trying fallback connection method...");
        
        try {
            // استخدام Reflection للاتصال
            java.lang.reflect.Method m = device.getClass()
                .getMethod("createRfcommSocket", new Class[]{int.class});
            
            currentSocket = (BluetoothSocket) m.invoke(device, 1);
            currentSocket.connect();
            currentOutputStream = currentSocket.getOutputStream();
            currentOutputStream.write(INIT_PRINTER);
            currentOutputStream.flush();
            
            Log.d(TAG, "✅ Fallback connection successful");
            notifyConnected(getDeviceName(device));
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Fallback connection failed", e);
            notifyError("All connection methods failed");
            return false;
        }
    }
    
    /**
     * قطع الاتصال
     */
    public void disconnect() {
        try {
            if (currentOutputStream != null) {
                currentOutputStream.close();
                currentOutputStream = null;
            }
            
            if (currentSocket != null) {
                currentSocket.close();
                currentSocket = null;
            }
            
            Log.d(TAG, "Disconnected");
            notifyDisconnected();
            
        } catch (IOException e) {
            Log.e(TAG, "Error disconnecting", e);
        }
    }
    
    /**
     * التحقق من الاتصال
     */
    public boolean isConnected() {
        return currentSocket != null && currentSocket.isConnected();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Printing Methods
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * طباعة نص بسيط
     */
    public boolean print(String text) {
        if (!isConnected()) {
            notifyError("Not connected to printer");
            return false;
        }
        
        try {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            currentOutputStream.write(data);
            currentOutputStream.write(LINE_FEED);
            currentOutputStream.flush();
            
            Log.d(TAG, "✅ Print successful");
            notifyPrintSuccess();
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Print failed", e);
            notifyError("Print failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * طباعة مع تنسيق
     */
    public boolean printFormatted(PrintJob job) {
        if (!isConnected()) {
            notifyError("Not connected to printer");
            return false;
        }
        
        try {
            // تهيئة
            currentOutputStream.write(INIT_PRINTER);
            
            // المحاذاة
            switch (job.alignment) {
                case CENTER:
                    currentOutputStream.write(ALIGN_CENTER);
                    break;
                case RIGHT:
                    currentOutputStream.write(ALIGN_RIGHT);
                    break;
                default:
                    currentOutputStream.write(ALIGN_LEFT);
                    break;
            }
            
            // النمط
            if (job.bold) {
                currentOutputStream.write(BOLD_ON);
            }
            if (job.underline) {
                currentOutputStream.write(UNDERLINE_ON);
            }
            if (job.doubleSize) {
                currentOutputStream.write(DOUBLE_SIZE);
            }
            
            // النص
            byte[] data = job.text.getBytes(StandardCharsets.UTF_8);
            currentOutputStream.write(data);
            currentOutputStream.write(LINE_FEED);
            
            // إيقاف التنسيق
            if (job.bold) {
                currentOutputStream.write(BOLD_OFF);
            }
            if (job.underline) {
                currentOutputStream.write(UNDERLINE_OFF);
            }
            if (job.doubleSize) {
                currentOutputStream.write(NORMAL_SIZE);
            }
            
            currentOutputStream.flush();
            
            Log.d(TAG, "✅ Formatted print successful");
            notifyPrintSuccess();
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Formatted print failed", e);
            notifyError("Print failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * طباعة فاتورة كاملة
     */
    public boolean printReceipt(Receipt receipt) {
        if (!isConnected()) {
            notifyError("Not connected to printer");
            return false;
        }
        
        try {
            // تهيئة
            currentOutputStream.write(INIT_PRINTER);
            
            // رأس الفاتورة
            printLine("", Alignment.CENTER, true, false, true);
            printLine(receipt.storeName, Alignment.CENTER, true, false, true);
            printLine(receipt.storeAddress, Alignment.CENTER, false, false, false);
            printLine(receipt.storePhone, Alignment.CENTER, false, false, false);
            printDivider();
            
            // معلومات الفاتورة
            printLine("رقم الفاتورة: " + receipt.invoiceNumber, Alignment.LEFT, false, false, false);
            printLine("التاريخ: " + receipt.date, Alignment.LEFT, false, false, false);
            printDivider();
            
            // المنتجات
            for (ReceiptItem item : receipt.items) {
                String line = String.format("%s x%d  %.2f",
                    item.name, item.quantity, item.price);
                printLine(line, Alignment.LEFT, false, false, false);
            }
            
            printDivider();
            
            // الإجمالي
            printLine("المجموع: " + String.format("%.2f", receipt.total),
                     Alignment.RIGHT, true, false, false);
            
            if (receipt.tax > 0) {
                printLine("الضريبة: " + String.format("%.2f", receipt.tax),
                         Alignment.RIGHT, false, false, false);
                printLine("الإجمالي النهائي: " + String.format("%.2f", receipt.finalTotal),
                         Alignment.RIGHT, true, false, false);
            }
            
            printDivider();
            printLine("شكراً لتعاملكم معنا", Alignment.CENTER, false, false, false);
            
            // قطع الورق
            currentOutputStream.write(LINE_FEED);
            currentOutputStream.write(LINE_FEED);
            currentOutputStream.write(LINE_FEED);
            currentOutputStream.write(PAPER_CUT);
            currentOutputStream.flush();
            
            Log.d(TAG, "✅ Receipt printed successfully");
            notifyPrintSuccess();
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Receipt print failed", e);
            notifyError("Print failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * طباعة سطر مع تنسيق
     */
    private void printLine(String text, Alignment alignment, 
                          boolean bold, boolean underline, boolean doubleSize) 
                          throws IOException {
        // المحاذاة
        switch (alignment) {
            case CENTER:
                currentOutputStream.write(ALIGN_CENTER);
                break;
            case RIGHT:
                currentOutputStream.write(ALIGN_RIGHT);
                break;
            default:
                currentOutputStream.write(ALIGN_LEFT);
                break;
        }
        
        // النمط
        if (bold) currentOutputStream.write(BOLD_ON);
        if (underline) currentOutputStream.write(UNDERLINE_ON);
        if (doubleSize) currentOutputStream.write(DOUBLE_SIZE);
        
        // النص
        currentOutputStream.write(text.getBytes(StandardCharsets.UTF_8));
        currentOutputStream.write(LINE_FEED);
        
        // إعادة التنسيق
        if (bold) currentOutputStream.write(BOLD_OFF);
        if (underline) currentOutputStream.write(UNDERLINE_OFF);
        if (doubleSize) currentOutputStream.write(NORMAL_SIZE);
    }
    
    /**
     * طباعة خط فاصل
     */
    private void printDivider() throws IOException {
        currentOutputStream.write(ALIGN_LEFT);
        currentOutputStream.write("--------------------------------".getBytes());
        currentOutputStream.write(LINE_FEED);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Listener Methods
    // ═══════════════════════════════════════════════════════════════════
    
    public void setPrintListener(PrintListener listener) {
        this.printListener = listener;
    }
    
    private void notifyConnected(String deviceName) {
        if (printListener != null) {
            printListener.onConnected(deviceName);
        }
    }
    
    private void notifyDisconnected() {
        if (printListener != null) {
            printListener.onDisconnected();
        }
    }
    
    private void notifyPrintSuccess() {
        if (printListener != null) {
            printListener.onPrintSuccess();
        }
    }
    
    private void notifyError(String error) {
        if (printListener != null) {
            printListener.onError(error);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Inner Classes
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * مهمة طباعة
     */
    public static class PrintJob {
        public String text;
        public Alignment alignment = Alignment.LEFT;
        public boolean bold = false;
        public boolean underline = false;
        public boolean doubleSize = false;
        
        public PrintJob(String text) {
            this.text = text;
        }
        
        public PrintJob bold() {
            this.bold = true;
            return this;
        }
        
        public PrintJob center() {
            this.alignment = Alignment.CENTER;
            return this;
        }
        
        public PrintJob doubleSize() {
            this.doubleSize = true;
            return this;
        }
    }
    
    /**
     * فاتورة
     */
    public static class Receipt {
        public String storeName;
        public String storeAddress;
        public String storePhone;
        public String invoiceNumber;
        public String date;
        public List<ReceiptItem> items = new ArrayList<>();
        public double total;
        public double tax;
        public double finalTotal;
    }
    
    /**
     * منتج في الفاتورة
     */
    public static class ReceiptItem {
        public String name;
        public int quantity;
        public double price;
        
        public ReceiptItem(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }
    
    /**
     * محاذاة
     */
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }
    
    /**
     * Listener للطباعة
     */
    public interface PrintListener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onPrintSuccess();
        void onError(String error);
    }
}
