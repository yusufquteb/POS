package com.pos.system;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import android.util.Log;


public class QRCodeGenerator {
    
    /**
     * توليد QR Code للفاتورة
     */
    public static Bitmap generateInvoiceQR(long invoiceId, String storeName, double total) {
        try {
            // بناء محتوى QR Code
            String content = String.format(
                "INVOICE:%d\nSTORE:%s\nTOTAL:%.2f\nDATE:%s",
                invoiceId,
                storeName,
                total,
                System.currentTimeMillis()
            );
            
            // توليد QR Code
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(bitMatrix);
            
        } catch (Exception e) {
            Log.e("QRCode", "Error generating QR Code: " + e.getMessage());
            return null;
        }
    }
}