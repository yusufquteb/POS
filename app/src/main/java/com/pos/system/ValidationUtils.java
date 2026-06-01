package com.pos.system.utils;

import android.util.Patterns;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * ValidationUtils - أدوات التحقق من صحة المدخلات
 * يوفر دوال شاملة للتحقق من صحة البيانات المختلفة
 * 
 * @author POS System
 * @version 1.0
 * @since 2026-02-13
 */
public class ValidationUtils {

    // أنماط التحقق
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^[0-9]{8,13}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");
    private static final Pattern PRICE_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final Pattern ARABIC_PATTERN = Pattern.compile("^[\\u0600-\\u06FF\\s]+$");
    
    // الحد الأدنى والأقصى للقيم
    public static final double MIN_PRICE = 0.0;
    public static final double MAX_PRICE = 1000000.0;
    public static final int MIN_QUANTITY = 0;
    public static final int MAX_QUANTITY = 100000;
    public static final int MIN_BARCODE_LENGTH = 8;
    public static final int MAX_BARCODE_LENGTH = 13;

    /**
     * فحص ما إذا كان EditText فارغاً
     * @param editText حقل الإدخال
     * @return true إذا كان فارغاً
     */
    public static boolean isEmpty(@Nullable EditText editText) {
        return SafeTextUtils.isEmpty(editText);
    }

    /**
     * فحص ما إذا كانت النص فارغ أو يحتوي على مسافات فقط
     * @param text النص
     * @return true إذا كان فارغاً
     */
    public static boolean isBlank(@Nullable String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * التحقق من صحة الباركود
     * @param barcode الباركود
     * @return true إذا كان صحيحاً
     */
    public static boolean isValidBarcode(@Nullable String barcode) {
        if (isBlank(barcode)) {
            return false;
        }
        barcode = barcode.trim();
        return barcode.length() >= MIN_BARCODE_LENGTH && 
               barcode.length() <= MAX_BARCODE_LENGTH &&
               BARCODE_PATTERN.matcher(barcode).matches();
    }

    /**
     * التحقق من صحة الباركود من EditText
     * @param editText حقل الإدخال
     * @return رسالة الخطأ أو null إذا كان صحيحاً
     */
    @Nullable
    public static String validateBarcode(@Nullable EditText editText) {
        String barcode = SafeTextUtils.getText(editText);
        
        if (barcode.isEmpty()) {
            return "الباركود مطلوب";
        }
        
        if (barcode.length() < MIN_BARCODE_LENGTH) {
            return "الباركود يجب أن يكون " + MIN_BARCODE_LENGTH + " أرقام على الأقل";
        }
        
        if (barcode.length() > MAX_BARCODE_LENGTH) {
            return "الباركود طويل جداً";
        }
        
        if (!BARCODE_PATTERN.matcher(barcode).matches()) {
            return "الباركود يجب أن يحتوي على أرقام فقط";
        }
        
        return null; // صحيح
    }

    /**
     * التحقق من صحة اسم المنتج
     * @param editText حقل الإدخال
     * @return رسالة الخطأ أو null إذا كان صحيحاً
     */
    @Nullable
    public static String validateProductName(@Nullable EditText editText) {
        String name = SafeTextUtils.getText(editText);
        
        if (name.isEmpty()) {
            return "اسم المنتج مطلوب";
        }
        
        if (name.length() < 2) {
            return "اسم المنتج قصير جداً";
        }
        
        if (name.length() > 100) {
            return "اسم المنتج طويل جداً";
        }
        
        return null;
    }

    /**
     * التحقق من صحة السعر
     * @param price السعر
     * @return true إذا كان صحيحاً
     */
    public static boolean isValidPrice(double price) {
        return price >= MIN_PRICE && price <= MAX_PRICE;
    }

    /**
     * التحقق من صحة السعر من EditText
     * @param editText حقل الإدخال
     * @return رسالة الخطأ أو null إذا كان صحيحاً
     */
    @Nullable
    public static String validatePrice(@Nullable EditText editText) {
        String priceText = SafeTextUtils.getText(editText);
        
        if (priceText.isEmpty()) {
            return "السعر مطلوب";
        }
        
        if (!PRICE_PATTERN.matcher(priceText).matches()) {
            return "السعر غير صحيح";
        }
        
        try {
            double price = Double.parseDouble(priceText);
            
            if (price < MIN_PRICE) {
                return "السعر لا يمكن أن يكون سالباً";
            }
            
            if (price > MAX_PRICE) {
                return "السعر كبير جداً";
            }
            
            return null;
        } catch (NumberFormatException e) {
            return "السعر غير صحيح";
        }
    }

    /**
     * التحقق من صحة الكمية
     * @param quantity الكمية
     * @return true إذا كانت صحيحة
     */
    public static boolean isValidQuantity(int quantity) {
        return quantity >= MIN_QUANTITY && quantity <= MAX_QUANTITY;
    }

    /**
     * التحقق من صحة الكمية من EditText
     * @param editText حقل الإدخال
     * @return رسالة الخطأ أو null إذا كانت صحيحة
     */
    @Nullable
    public static String validateQuantity(@Nullable EditText editText) {
        String qtyText = SafeTextUtils.getText(editText);
        
        if (qtyText.isEmpty()) {
            return "الكمية مطلوبة";
        }
        
        try {
            int quantity = Integer.parseInt(qtyText);
            
            if (quantity < MIN_QUANTITY) {
                return "الكمية لا يمكن أن تكون سالبة";
            }
            
            if (quantity > MAX_QUANTITY) {
                return "الكمية كبيرة جداً";
            }
            
            return null;
        } catch (NumberFormatException e) {
            return "الكمية غير صحيحة";
        }
    }

    /**
     * التحقق من صحة رقم الهاتف
     * @param phone رقم الهاتف
     * @return true إذا كان صحيحاً
     */
    public static boolean isValidPhone(@Nullable String phone) {
        if (isBlank(phone)) {
            return false;
        }
        phone = phone.trim().replaceAll("[\\s-()]", ""); // إزالة المسافات والشرطات
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * التحقق من صحة رقم الهاتف من EditText
     * @param editText حقل الإدخال
     * @param required هل الحقل مطلوب
     * @return رسالة الخطأ أو null إذا كان صحيحاً
     */
    @Nullable
    public static String validatePhone(@Nullable EditText editText, boolean required) {
        String phone = SafeTextUtils.getText(editText);
        
        if (phone.isEmpty()) {
            return required ? "رقم الهاتف مطلوب" : null;
        }
        
        phone = phone.replaceAll("[\\s-()]", "");
        
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "رقم الهاتف غير صحيح";
        }
        
        return null;
    }

    /**
     * التحقق من صحة البريد الإلكتروني
     * @param email البريد الإلكتروني
     * @return true إذا كان صحيحاً
     */
    public static boolean isValidEmail(@Nullable String email) {
        if (isBlank(email)) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /**
     * التحقق من صحة البريد الإلكتروني من EditText
     * @param editText حقل الإدخال
     * @param required هل الحقل مطلوب
     * @return رسالة الخطأ أو null إذا كان صحيحاً
     */
    @Nullable
    public static String validateEmail(@Nullable EditText editText, boolean required) {
        String email = SafeTextUtils.getText(editText);
        
        if (email.isEmpty()) {
            return required ? "البريد الإلكتروني مطلوب" : null;
        }
        
        if (!isValidEmail(email)) {
            return "البريد الإلكتروني غير صحيح";
        }
        
        return null;
    }

    /**
     * التحقق من أن النص يحتوي على أحرف عربية فقط
     * @param text النص
     * @return true إذا كان يحتوي على عربي فقط
     */
    public static boolean isArabic(@Nullable String text) {
        if (isBlank(text)) {
            return false;
        }
        return ARABIC_PATTERN.matcher(text.trim()).matches();
    }

    /**
     * التحقق من أن الطول ضمن النطاق
     * @param text النص
     * @param minLength الحد الأدنى
     * @param maxLength الحد الأقصى
     * @return true إذا كان ضمن النطاق
     */
    public static boolean isLengthValid(@Nullable String text, int minLength, int maxLength) {
        if (text == null) {
            return false;
        }
        int length = text.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * التحقق من طول النص من EditText
     * @param editText حقل الإدخال
     * @param minLength الحد الأدنى
     * @param maxLength الحد الأقصى
     * @param fieldName اسم الحقل
     * @return رسالة الخطأ أو null إذا كان صحيحاً
     */
    @Nullable
    public static String validateLength(@Nullable EditText editText, 
                                       int minLength, 
                                       int maxLength, 
                                       @NonNull String fieldName) {
        String text = SafeTextUtils.getText(editText);
        
        if (text.isEmpty() && minLength > 0) {
            return fieldName + " مطلوب";
        }
        
        if (text.length() < minLength) {
            return fieldName + " قصير جداً (الحد الأدنى " + minLength + " أحرف)";
        }
        
        if (text.length() > maxLength) {
            return fieldName + " طويل جداً (الحد الأقصى " + maxLength + " حرف)";
        }
        
        return null;
    }

    /**
     * التحقق من أن القيمة ضمن نطاق معين
     * @param value القيمة
     * @param min الحد الأدنى
     * @param max الحد الأقصى
     * @return true إذا كانت ضمن النطاق
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * تنظيف المدخلات من الأحرف الخطيرة (منع SQL Injection)
     * @param input المدخل
     * @return النص المنظف
     */
    @NonNull
    public static String sanitizeInput(@Nullable String input) {
        if (input == null) {
            return "";
        }
        // إزالة الأحرف الخطيرة
        return input.trim()
                   .replaceAll("[;'\"\\\\]", "")  // منع SQL injection
                   .replaceAll("[<>]", "");       // منع XSS
    }

    /**
     * فحص ما إذا كان النص يحتوي على أحرف خطيرة
     * @param text النص
     * @return true إذا كان يحتوي على أحرف خطيرة
     */
    public static boolean containsDangerousCharacters(@Nullable String text) {
        if (text == null) {
            return false;
        }
        return text.matches(".*[;'\"\\\\<>].*");
    }

    /**
     * التحقق من أن التاريخ بصيغة صحيحة (yyyy-MM-dd)
     * @param date التاريخ
     * @return true إذا كان صحيحاً
     */
    public static boolean isValidDate(@Nullable String date) {
        if (isBlank(date)) {
            return false;
        }
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /**
     * التحقق الشامل من نموذج المنتج
     * @param barcode الباركود
     * @param name الاسم
     * @param price السعر
     * @param quantity الكمية
     * @return رسالة الخطأ أو null إذا كان كل شيء صحيح
     */
    @Nullable
    public static String validateProductForm(@Nullable EditText barcode,
                                            @Nullable EditText name,
                                            @Nullable EditText price,
                                            @Nullable EditText quantity) {
        String error;
        
        error = validateBarcode(barcode);
        if (error != null) return error;
        
        error = validateProductName(name);
        if (error != null) return error;
        
        error = validatePrice(price);
        if (error != null) return error;
        
        error = validateQuantity(quantity);
        if (error != null) return error;
        
        return null; // كل شيء صحيح
    }

    /**
     * التحقق من أن المبلغ موجب
     * @param amount المبلغ
     * @return true إذا كان موجباً
     */
    public static boolean isPositive(double amount) {
        return amount > 0;
    }

    /**
     * التحقق من أن المبلغ صفر أو موجب
     * @param amount المبلغ
     * @return true إذا كان صفر أو موجب
     */
    public static boolean isNonNegative(double amount) {
        return amount >= 0;
    }
}
