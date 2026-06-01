package com.pos.system.utils;

import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SafeTextUtils - أدوات آمنة للتعامل مع النصوص
 * تحل مشكلة NullPointerException الشائعة مع EditText و TextView
 * 
 * @author POS System
 * @version 1.0
 * @since 2026-02-13
 */
public class SafeTextUtils {

    /**
     * الحصول على نص من EditText بشكل آمن
     * @param editText حقل الإدخال
     * @return النص أو سلسلة فارغة في حالة null
     */
    @NonNull
    public static String getText(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    /**
     * الحصول على نص من EditText بشكل آمن مع قيمة افتراضية
     * @param editText حقل الإدخال
     * @param defaultValue القيمة الافتراضية
     * @return النص أو القيمة الافتراضية
     */
    @NonNull
    public static String getText(@Nullable EditText editText, @NonNull String defaultValue) {
        String text = getText(editText);
        return text.isEmpty() ? defaultValue : text;
    }

    /**
     * الحصول على نص من TextView بشكل آمن
     * @param textView عنصر النص
     * @return النص أو سلسلة فارغة في حالة null
     */
    @NonNull
    public static String getText(@Nullable TextView textView) {
        if (textView == null || textView.getText() == null) {
            return "";
        }
        return textView.getText().toString().trim();
    }

    /**
     * الحصول على رقم صحيح من EditText بشكل آمن
     * @param editText حقل الإدخال
     * @param defaultValue القيمة الافتراضية في حالة الخطأ
     * @return الرقم أو القيمة الافتراضية
     */
    public static int getInt(@Nullable EditText editText, int defaultValue) {
        try {
            String text = getText(editText);
            if (text.isEmpty()) {
                return defaultValue;
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * الحصول على رقم عشري من EditText بشكل آمن
     * @param editText حقل الإدخال
     * @param defaultValue القيمة الافتراضية في حالة الخطأ
     * @return الرقم أو القيمة الافتراضية
     */
    public static double getDouble(@Nullable EditText editText, double defaultValue) {
        try {
            String text = getText(editText);
            if (text.isEmpty()) {
                return defaultValue;
            }
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * الحصول على رقم طويل من EditText بشكل آمن
     * @param editText حقل الإدخال
     * @param defaultValue القيمة الافتراضية في حالة الخطأ
     * @return الرقم أو القيمة الافتراضية
     */
    public static long getLong(@Nullable EditText editText, long defaultValue) {
        try {
            String text = getText(editText);
            if (text.isEmpty()) {
                return defaultValue;
            }
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * فحص ما إذا كان EditText فارغاً
     * @param editText حقل الإدخال
     * @return true إذا كان فارغاً
     */
    public static boolean isEmpty(@Nullable EditText editText) {
        return getText(editText).isEmpty();
    }

    /**
     * فحص ما إذا كان EditText ليس فارغاً
     * @param editText حقل الإدخال
     * @return true إذا كان غير فارغ
     */
    public static boolean isNotEmpty(@Nullable EditText editText) {
        return !isEmpty(editText);
    }

    /**
     * تعيين نص لـ EditText بشكل آمن
     * @param editText حقل الإدخال
     * @param text النص
     */
    public static void setText(@Nullable EditText editText, @Nullable String text) {
        if (editText != null) {
            editText.setText(text != null ? text : "");
        }
    }

    /**
     * تعيين نص لـ TextView بشكل آمن
     * @param textView عنصر النص
     * @param text النص
     */
    public static void setText(@Nullable TextView textView, @Nullable String text) {
        if (textView != null) {
            textView.setText(text != null ? text : "");
        }
    }

    /**
     * تعيين رقم صحيح لـ EditText بشكل آمن
     * @param editText حقل الإدخال
     * @param value الرقم
     */
    public static void setInt(@Nullable EditText editText, int value) {
        setText(editText, String.valueOf(value));
    }

    /**
     * تعيين رقم عشري لـ EditText بشكل آمن
     * @param editText حقل الإدخال
     * @param value الرقم
     */
    public static void setDouble(@Nullable EditText editText, double value) {
        setText(editText, String.format("%.2f", value));
    }

    /**
     * تنظيف النص من المسافات الزائدة والأحرف الخاصة
     * @param text النص
     * @return النص المنظف
     */
    @NonNull
    public static String sanitize(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                   .replaceAll("\\s+", " ")  // إزالة المسافات المتعددة
                   .replaceAll("[<>\"']", ""); // إزالة الأحرف الخطيرة
    }

    /**
     * اختصار النص إلى طول معين
     * @param text النص
     * @param maxLength الطول الأقصى
     * @return النص المختصر
     */
    @NonNull
    public static String truncate(@Nullable String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "";
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * تحويل أول حرف إلى كبير
     * @param text النص
     * @return النص بعد التحويل
     */
    @NonNull
    public static String capitalize(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    /**
     * فحص ما إذا كان النص يحتوي على أحرف فقط (بدون أرقام)
     * @param text النص
     * @return true إذا كان يحتوي على أحرف فقط
     */
    public static boolean isAlphabetic(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("[a-zA-Zأ-ي\\s]+");
    }

    /**
     * فحص ما إذا كان النص يحتوي على أرقام فقط
     * @param text النص
     * @return true إذا كان يحتوي على أرقام فقط
     */
    public static boolean isNumeric(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("\\d+");
    }

    /**
     * فحص ما إذا كان النص يحتوي على أرقام وأحرف
     * @param text النص
     * @return true إذا كان يحتوي على أرقام وأحرف
     */
    public static boolean isAlphanumeric(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("[a-zA-Z0-9أ-ي\\s]+");
    }

    /**
     * مقارنة نصين مع تجاهل حالة الأحرف
     * @param text1 النص الأول
     * @param text2 النص الثاني
     * @return true إذا كانا متساويين
     */
    public static boolean equalsIgnoreCase(@Nullable String text1, @Nullable String text2) {
        if (text1 == null && text2 == null) {
            return true;
        }
        if (text1 == null || text2 == null) {
            return false;
        }
        return text1.equalsIgnoreCase(text2);
    }

    /**
     * فحص ما إذا كان النص يحتوي على نص آخر (مع تجاهل حالة الأحرف)
     * @param text النص الرئيسي
     * @param search النص المراد البحث عنه
     * @return true إذا كان يحتوي عليه
     */
    public static boolean containsIgnoreCase(@Nullable String text, @Nullable String search) {
        if (text == null || search == null) {
            return false;
        }
        return text.toLowerCase().contains(search.toLowerCase());
    }
}
