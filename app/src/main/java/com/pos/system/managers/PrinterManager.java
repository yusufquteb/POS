package com.pos.system.managers;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.pos.system.R;
import com.pos.system.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PrinterManager {

    private static final String TAG = "PrinterManager";

    public static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    public static final int REQUEST_USB_PERMISSION        = 1002;
    public static final int REQUEST_LOCATION_PERMISSION   = 1003;
    public static final int REQUEST_ENABLE_BLUETOOTH      = 1004;

    private final Context  context;
    private final Activity activity;

    private BluetoothAdapter bluetoothAdapter;
    private UsbManager       usbManager;
    private WifiManager      wifiManager;

    private PrinterConnectionListener connectionListener;

    public PrinterManager(Activity activity) {
        this.activity = activity;
        this.context  = activity.getApplicationContext();
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            usbManager  = (UsbManager)  context.getSystemService(Context.USB_SERVICE);
            wifiManager = (WifiManager) context.getApplicationContext()
                                               .getSystemService(Context.WIFI_SERVICE);
            Log.d(TAG, "✅ PrinterManager initialized");
        } catch (Exception e) {
            Log.e(TAG, "❌ Initialization error", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ✅ الدالة المُضافة (كانت مفقودة)
    // ═══════════════════════════════════════════════════════════════
    /** التحقق من أن الجهاز يدعم البلوتوث */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    // ═══════════════════════════════════════════════════════════════
    // USB
    // ═══════════════════════════════════════════════════════════════
    public PrinterCheckResult checkUSBPrinters() {
        if (usbManager == null)
            return new PrinterCheckResult(false, context.getString(R.string.usb_manager_unavailable), null);
        try {
            java.util.HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList == null || deviceList.isEmpty())
                return new PrinterCheckResult(false, context.getString(R.string.no_usb_devices_connected), null);

            List<PrinterDevice> printers = new ArrayList<>();
            for (UsbDevice device : deviceList.values()) {
                if (isPrinterDevice(device)) {
                    printers.add(new PrinterDevice(
                        device.getDeviceName(), "USB",
                        device.getProductName() != null ? device.getProductName() : "USB Printer",
                        true));
                }
            }
            if (printers.isEmpty())
                return new PrinterCheckResult(false, context.getString(R.string.no_usb_printers_connected_message), null);

            return new PrinterCheckResult(true, context.getString(R.string.usb_printers_found_format, printers.size()), printers);
        } catch (Exception e) {
            return new PrinterCheckResult(false, context.getString(R.string.usb_check_error_format, e.getMessage()), null);
        }
    }

    private boolean isPrinterDevice(UsbDevice device) {
        return device.getDeviceClass() == 7 ||
               (device.getInterfaceCount() > 0 && device.getInterface(0).getInterfaceClass() == 7);
    }

    // ═══════════════════════════════════════════════════════════════
    // Bluetooth
    // ═══════════════════════════════════════════════════════════════
    public boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                       == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                       == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                       == PackageManager.PERMISSION_GRANTED;
        }
    }

    public void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void requestEnableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    public PrinterCheckResult checkBluetoothPrinters() {
        if (bluetoothAdapter == null)
            return new PrinterCheckResult(false, context.getString(R.string.bluetooth_not_supported_device), null);
        if (!checkBluetoothPermissions())
            return new PrinterCheckResult(false, context.getString(R.string.please_grant_bluetooth_permission), null);
        if (!isBluetoothEnabled())
            return new PrinterCheckResult(false, context.getString(R.string.bluetooth_disabled_message), null);

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices == null || pairedDevices.isEmpty())
                return new PrinterCheckResult(false, context.getString(R.string.no_bluetooth_devices_paired_message), null);

            List<PrinterDevice> printers = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                if (isBluetoothPrinter(device)) {
                    printers.add(new PrinterDevice(device.getAddress(), "Bluetooth",
                        device.getName() != null ? device.getName() : "Bluetooth Printer", true));
                }
            }
            if (printers.isEmpty())
                return new PrinterCheckResult(false,
                    context.getString(R.string.no_bluetooth_printers_paired_format, pairedDevices.size()), null);

            return new PrinterCheckResult(true,
                context.getString(R.string.bluetooth_printers_found_format, printers.size()), printers);

        } catch (SecurityException e) {
            return new PrinterCheckResult(false, context.getString(R.string.permission_error_bluetooth), null);
        } catch (Exception e) {
            return new PrinterCheckResult(false, context.getString(R.string.bluetooth_check_error_format, e.getMessage()), null);
        }
    }

    private boolean isBluetoothPrinter(BluetoothDevice device) {
        try {
            String name = device.getName();
            if (name != null) {
                String n = name.toLowerCase();
                return n.contains("printer") || n.contains("print") || n.contains("pos")
                    || n.contains("rpp") || n.contains("mpt") || n.contains("thermal")
                    || n.contains("receipt");
            }
            int cls = device.getBluetoothClass().getDeviceClass();
            return cls == 0x1664 || cls == 0x0600;
        } catch (Exception e) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════
    // WiFi
    // ═══════════════════════════════════════════════════════════════
    public boolean isWiFiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public void requestEnableWiFi() {
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.startActivity(
                    new Intent(android.provider.Settings.Panel.ACTION_WIFI));
            } else {
                wifiManager.setWifiEnabled(true);
            }
        }
    }

    public PrinterCheckResult checkWiFiPrinters() {
        if (wifiManager == null)
            return new PrinterCheckResult(false, context.getString(R.string.wifi_manager_unavailable), null);
        if (!isWiFiEnabled())
            return new PrinterCheckResult(false, context.getString(R.string.wifi_disabled_message), null);

        return new PrinterCheckResult(false,
            context.getString(R.string.wifi_printer_check_requirements_message), null);
    }

    // ═══════════════════════════════════════════════════════════════
    // General
    // ═══════════════════════════════════════════════════════════════
    public PrinterCheckResult checkPrinters(String printerType) {
        switch (printerType) {
            case Constants.PrinterType.USB:       return checkUSBPrinters();
            case Constants.PrinterType.BLUETOOTH: return checkBluetoothPrinters();
            case Constants.PrinterType.WIFI:      return checkWiFiPrinters();
            default: return new PrinterCheckResult(false, context.getString(R.string.unknown_printer_type_format, printerType), null);
        }
    }

    public void openBluetoothSettings() {
        activity.startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    public void openWiFiSettings() {
        activity.startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }

    // ═══════════════════════════════════════════════════════════════
    // Inner Classes
    // ═══════════════════════════════════════════════════════════════
    public static class PrinterCheckResult {
        public final boolean success;
        public final String  message;
        public final List<PrinterDevice> printers;

        public PrinterCheckResult(boolean success, String message, List<PrinterDevice> printers) {
            this.success  = success;
            this.message  = message;
            this.printers = printers;
        }

        public boolean hasPrinters()   { return printers != null && !printers.isEmpty(); }
        public int     getPrinterCount(){ return printers != null ? printers.size() : 0; }
    }

    public static class PrinterDevice {
        public final String  id, type, name;
        public final boolean isConnected;

        public PrinterDevice(String id, String type, String name, boolean isConnected) {
            this.id = id; this.type = type; this.name = name; this.isConnected = isConnected;
        }

        @Override public String toString() { return name + " (" + type + ")"; }
    }

    public interface PrinterConnectionListener {
        void onPrinterConnected(PrinterDevice printer);
        void onPrinterDisconnected();
        void onPrinterError(String error);
    }

    public void setConnectionListener(PrinterConnectionListener listener) {
        this.connectionListener = listener;
    }
}
