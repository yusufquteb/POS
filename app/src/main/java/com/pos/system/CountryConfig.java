package com.pos.system;

import java.util.ArrayList;
import java.util.List;

/**
 * CountryConfig — إعدادات الدول العربية المدعومة
 * 11 دولة: EG SA AE KW QA BH OM JO MA DZ TN
 */
public class CountryConfig {

    public final String code;
    public final String nameAr;
    public final String currency;
    public final String currencyCode;
    public final double vatRate;
    public final int    decimalPlaces;
    public final String phonePrefix;

    private CountryConfig(String code, String nameAr, String currency, String currencyCode,
                          double vatRate, int decimalPlaces, String phonePrefix) {
        this.code          = code;
        this.nameAr        = nameAr;
        this.currency      = currency;
        this.currencyCode  = currencyCode;
        this.vatRate       = vatRate;
        this.decimalPlaces = decimalPlaces;
        this.phonePrefix   = phonePrefix;
    }

    // ─────────────────────────────────────────────────────────────
    //  Static country list
    // ─────────────────────────────────────────────────────────────

    private static final List<CountryConfig> ALL = new ArrayList<>();

    static {
        ALL.add(new CountryConfig("EG", "مصر",         "ج.م",  "EGP", 14.0, 2, "+20"));
        ALL.add(new CountryConfig("SA", "السعودية",     "ر.س",  "SAR", 15.0, 2, "+966"));
        ALL.add(new CountryConfig("AE", "الإمارات",     "د.إ",  "AED",  5.0, 2, "+971"));
        ALL.add(new CountryConfig("KW", "الكويت",       "د.ك",  "KWD",  0.0, 3, "+965"));
        ALL.add(new CountryConfig("QA", "قطر",          "ر.ق",  "QAR",  0.0, 2, "+974"));
        ALL.add(new CountryConfig("BH", "البحرين",      "د.ب",  "BHD", 10.0, 3, "+973")); // VAT 10% منذ 2019
        ALL.add(new CountryConfig("OM", "عُمان",        "ر.ع",  "OMR",  5.0, 3, "+968")); // VAT 5% منذ 2021
        ALL.add(new CountryConfig("JO", "الأردن",       "د.أ",  "JOD", 16.0, 3, "+962")); // GST 16%
        ALL.add(new CountryConfig("MA", "المغرب",       "د.م",  "MAD", 20.0, 2, "+212")); // TVA 20%
        ALL.add(new CountryConfig("DZ", "الجزائر",      "د.ج",  "DZD", 19.0, 2, "+213")); // TVA 19%
        ALL.add(new CountryConfig("TN", "تونس",         "د.ت",  "TND", 19.0, 3, "+216")); // TVA 19%
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

    /** قائمة أسماء الدول العربية */
    public static List<String> names() {
        List<String> list = new ArrayList<>();
        for (CountryConfig c : ALL) list.add(c.nameAr);
        return list;
    }

    @Override
    public String toString() {
        return nameAr + " (" + currency + ")";
    }
}
