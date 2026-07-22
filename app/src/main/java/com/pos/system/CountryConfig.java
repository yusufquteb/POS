package com.pos.system;

import com.pos.system.managers.LanguageManager;
import java.util.ArrayList;
import java.util.List;

/**
 * CountryConfig — إعدادات الدول العربية المدعومة
 * 11 دولة: EG SA AE KW QA BH OM JO MA DZ TN
 */
public class CountryConfig {

    public final String code;
    public final String nameAr;
    public final String nameEn;
    public final String currency;
    public final String currencyCode;
    public final double vatRate;
    public final int    decimalPlaces;
    public final String phonePrefix;
    public final int    flagRes;

    private CountryConfig(String code, String nameAr, String nameEn, String currency, String currencyCode,
                          double vatRate, int decimalPlaces, String phonePrefix, int flagRes) {
        this.code          = code;
        this.nameAr        = nameAr;
        this.nameEn        = nameEn;
        this.currency      = currency;
        this.currencyCode  = currencyCode;
        this.vatRate       = vatRate;
        this.decimalPlaces = decimalPlaces;
        this.phonePrefix   = phonePrefix;
        this.flagRes       = flagRes;
    }

    /** Country name in the currently active app language */
    public String displayName() {
        return LanguageManager.isArabic() ? nameAr : nameEn;
    }

    // ─────────────────────────────────────────────────────────────
    //  Static country list
    // ─────────────────────────────────────────────────────────────

    private static final List<CountryConfig> ALL = new ArrayList<>();

    static {
        ALL.add(new CountryConfig("EG", "مصر",         "Egypt",         "ج.م",  "EGP", 14.0, 2, "+20",  R.drawable.flag_eg));
        ALL.add(new CountryConfig("SA", "السعودية",     "Saudi Arabia",  "ر.س",  "SAR", 15.0, 2, "+966", R.drawable.flag_sa));
        ALL.add(new CountryConfig("AE", "الإمارات",     "UAE",           "د.إ",  "AED",  5.0, 2, "+971", R.drawable.flag_ae));
        ALL.add(new CountryConfig("KW", "الكويت",       "Kuwait",        "د.ك",  "KWD",  0.0, 3, "+965", R.drawable.flag_kw));
        ALL.add(new CountryConfig("QA", "قطر",          "Qatar",         "ر.ق",  "QAR",  0.0, 2, "+974", R.drawable.flag_qa));
        ALL.add(new CountryConfig("BH", "البحرين",      "Bahrain",       "د.ب",  "BHD", 10.0, 3, "+973", R.drawable.flag_bh)); // VAT 10% since 2019
        ALL.add(new CountryConfig("OM", "عُمان",        "Oman",          "ر.ع",  "OMR",  5.0, 3, "+968", R.drawable.flag_om)); // VAT 5% since 2021
        ALL.add(new CountryConfig("JO", "الأردن",       "Jordan",        "د.أ",  "JOD", 16.0, 3, "+962", R.drawable.flag_jo)); // GST 16%
        ALL.add(new CountryConfig("MA", "المغرب",       "Morocco",       "د.م",  "MAD", 20.0, 2, "+212", R.drawable.flag_ma)); // TVA 20%
        ALL.add(new CountryConfig("DZ", "الجزائر",      "Algeria",       "د.ج",  "DZD", 19.0, 2, "+213", R.drawable.flag_dz)); // TVA 19%
        ALL.add(new CountryConfig("TN", "تونس",         "Tunisia",       "د.ت",  "TND", 19.0, 3, "+216", R.drawable.flag_tn)); // TVA 19%
    }

    /** الحصول على إعدادات دولة بالكود */
    public static CountryConfig forCode(String code) {
        if (code == null) return forCode("EG");
        for (CountryConfig c : ALL) {
            if (c.code.equalsIgnoreCase(code)) return c;
        }
        return ALL.get(0); // default EG
    }

    /** قائمة جميع الدول */
    public static List<CountryConfig> all() {
        return new ArrayList<>(ALL);
    }

    /** قائمة أكواد الدول */
    public static List<String> codes() {
        List<String> list = new ArrayList<>();
        for (CountryConfig c : ALL) list.add(c.code);
        return list;
    }

    /** قائمة أسماء الدول باللغة الحالية للتطبيق */
    public static List<String> names() {
        List<String> list = new ArrayList<>();
        for (CountryConfig c : ALL) list.add(c.displayName());
        return list;
    }

    @Override
    public String toString() {
        return displayName() + " (" + currency + ")";
    }
}
