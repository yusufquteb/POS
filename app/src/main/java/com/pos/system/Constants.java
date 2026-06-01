package com.pos.system.utils;

/**
 * Constants - الثوابت المركزية للتطبيق
 * يحتوي على جميع الثوابت المستخدمة في التطبيق في مكان واحد
 * 
 * @author SmartPOS Team
 * @version 2.0
 * @since 2026-02-14
 * 
 * التحديثات:
 * - إضافة دعم الدول العربية مع ميزات خاصة لكل دولة
 * - إضافة ثوابت النسخ الاحتياطي التلقائي
 * - إضافة ثوابت الضريبة المخصصة
 * - تحسين التنظيم والوثائق
 */
public class Constants {

    // منع إنشاء instance من الكلاس
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ════════════════════════════════════════════════════════════
    // Database Constants
    // ════════════════════════════════════════════════════════════
    public static class Database {
        public static final String NAME = "SmartPOS.db";
        public static final int VERSION = 3;
        
        // أسماء الجداول
        public static final String TABLE_PRODUCTS = "products";
        public static final String TABLE_CUSTOMERS = "customers";
        public static final String TABLE_SUPPLIERS = "suppliers";
        public static final String TABLE_INVOICES = "invoices";
        public static final String TABLE_INVOICE_ITEMS = "invoice_items";
        public static final String TABLE_EXPENSES = "expenses";
        public static final String TABLE_LOCATIONS = "locations";
        public static final String TABLE_CATEGORIES = "categories";
        public static final String TABLE_STORE_SETTINGS = "store_settings";
        public static final String TABLE_PRINTER_SETTINGS = "printer_settings";
        public static final String TABLE_BACKUP_LOG = "backup_log";
    }

    // ════════════════════════════════════════════════════════════
    // Validation Constants
    // ════════════════════════════════════════════════════════════
    public static class Validation {
        // Barcode
        public static final int BARCODE_MIN_LENGTH = 8;
        public static final int BARCODE_MAX_LENGTH = 13;
        
        // Product Name
        public static final int PRODUCT_NAME_MIN_LENGTH = 2;
        public static final int PRODUCT_NAME_MAX_LENGTH = 100;
        
        // Price
        public static final double MIN_PRICE = 0.0;
        public static final double MAX_PRICE = 1000000.0;
        
        // Quantity
        public static final int MIN_QUANTITY = 0;
        public static final int MAX_QUANTITY = 100000;
        
        // Phone
        public static final int PHONE_MIN_LENGTH = 10;
        public static final int PHONE_MAX_LENGTH = 15;
        
        // Tax Rate
        public static final double MIN_TAX_RATE = 0.0;
        public static final double MAX_TAX_RATE = 100.0;
        
        // General
        public static final int MIN_NAME_LENGTH = 2;
        public static final int MAX_NAME_LENGTH = 100;
        public static final int MAX_NOTES_LENGTH = 500;
    }

    // ════════════════════════════════════════════════════════════
    // Default Values
    // ════════════════════════════════════════════════════════════
    public static class Defaults {
        public static final double TAX_RATE = 15.0; // 15%
        public static final double DISCOUNT_RATE = 0.0;
        public static final int REORDER_LEVEL = 5;
        public static final String CURRENCY_SYMBOL = "ر.س";
        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String TIME_FORMAT = "HH:mm:ss";
        public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
        public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy";
        public static final String DISPLAY_TIME_FORMAT = "hh:mm a";
        public static final String COUNTRY_CODE = "SA"; // السعودية افتراضياً
        public static final String LANGUAGE = "ar";
    }

    // ════════════════════════════════════════════════════════════
    // Country & Region Constants - الدول العربية
    // ════════════════════════════════════════════════════════════
    public static class Countries {
        // معلومات الدول
        public static final String SAUDI_ARABIA = "SA";
        public static final String UAE = "AE";
        public static final String EGYPT = "EG";
        public static final String MOROCCO = "MA";
        public static final String ALGERIA = "DZ";
        public static final String TUNISIA = "TN";
        public static final String SUDAN = "SD";
        public static final String LIBYA = "LY";
        public static final String KUWAIT = "KW";
        public static final String QATAR = "QA";
        public static final String OMAN = "OM";
        public static final String YEMEN = "YE";
        public static final String BAHRAIN = "BH";
        public static final String JORDAN = "JO";
        public static final String LEBANON = "LB";
        public static final String PALESTINE = "PS";
        public static final String IRAQ = "IQ";
        public static final String SYRIA = "SY";
        
        // أسماء الدول بالعربية
        public static final String[] COUNTRY_NAMES_AR = {
            "السعودية", "الإمارات", "مصر", "المغرب", "الجزائر", 
            "تونس", "السودان", "ليبيا", "الكويت", "قطر", 
            "عمان", "اليمن", "البحرين", "الأردن", "لبنان",
            "فلسطين", "العراق", "سوريا"
        };
        
        // أكواد الدول
        public static final String[] COUNTRY_CODES = {
            SAUDI_ARABIA, UAE, EGYPT, MOROCCO, ALGERIA,
            TUNISIA, SUDAN, LIBYA, KUWAIT, QATAR,
            OMAN, YEMEN, BAHRAIN, JORDAN, LEBANON,
            PALESTINE, IRAQ, SYRIA
        };
        
        // العملات
        public static final String CURRENCY_SAR = "ر.س"; // ريال سعودي
        public static final String CURRENCY_AED = "د.إ"; // درهم إماراتي
        public static final String CURRENCY_EGP = "ج.م"; // جنيه مصري
        public static final String CURRENCY_MAD = "د.م"; // درهم مغربي
        public static final String CURRENCY_DZD = "د.ج"; // دينار جزائري
        public static final String CURRENCY_TND = "د.ت"; // دينار تونسي
        public static final String CURRENCY_SDG = "ج.س"; // جنيه سوداني
        public static final String CURRENCY_LYD = "د.ل"; // دينار ليبي
        public static final String CURRENCY_KWD = "د.ك"; // دينار كويتي
        public static final String CURRENCY_QAR = "ر.ق"; // ريال قطري
        public static final String CURRENCY_OMR = "ر.ع"; // ريال عماني
        public static final String CURRENCY_YER = "ر.ي"; // ريال يمني
        public static final String CURRENCY_BHD = "د.ب"; // دينار بحريني
        public static final String CURRENCY_JOD = "د.أ"; // دينار أردني
        public static final String CURRENCY_LBP = "ل.ل"; // ليرة لبنانية
        public static final String CURRENCY_ILS = "₪"; // شيكل
        public static final String CURRENCY_IQD = "د.ع"; // دينار عراقي
        public static final String CURRENCY_SYP = "ل.س"; // ليرة سورية
        
        // معدلات الضرائب الافتراضية لكل دولة
        public static final double TAX_RATE_SA = 15.0;  // السعودية - ضريبة القيمة المضافة
        public static final double TAX_RATE_AE = 5.0;   // الإمارات - ضريبة القيمة المضافة
        public static final double TAX_RATE_EG = 14.0;  // مصر - ضريبة القيمة المضافة
        public static final double TAX_RATE_MA = 20.0;  // المغرب - ضريبة القيمة المضافة
        public static final double TAX_RATE_DZ = 19.0;  // الجزائر
        public static final double TAX_RATE_TN = 19.0;  // تونس
        public static final double TAX_RATE_SD = 17.0;  // السودان
        public static final double TAX_RATE_LY = 0.0;   // ليبيا
        public static final double TAX_RATE_KW = 0.0;   // الكويت
        public static final double TAX_RATE_QA = 0.0;   // قطر
        public static final double TAX_RATE_OM = 5.0;   // عمان
        public static final double TAX_RATE_YE = 5.0;   // اليمن
        public static final double TAX_RATE_BH = 10.0;  // البحرين
        public static final double TAX_RATE_JO = 16.0;  // الأردن
        public static final double TAX_RATE_LB = 11.0;  // لبنان
        public static final double TAX_RATE_PS = 16.0;  // فلسطين
        public static final double TAX_RATE_IQ = 0.0;   // العراق
        public static final double TAX_RATE_SY = 0.0;   // سوريا
    }

    // ════════════════════════════════════════════════════════════
    // Country-Specific Features - ميزات خاصة بالدول
    // ════════════════════════════════════════════════════════════
    public static class CountryFeatures {
        // السعودية - ZATCA (هيئة الزكاة والضريبة والجمارك)
        public static class SaudiArabia {
            public static final String FEATURE_NAME = "ZATCA E-Invoicing";
            public static final String FEATURE_NAME_AR = "الفوترة الإلكترونية ZATCA";
            public static final String DESCRIPTION = "التكامل مع نظام الفوترة الإلكترونية السعودي";
            public static final boolean REQUIRES_TRN = true; // يتطلب رقم تسجيل ضريبي
            public static final boolean REQUIRES_QR = true;  // يتطلب رمز QR
            public static final String QR_FORMAT = "ZATCA";
            public static final int PHASE = 2; // المرحلة الحالية
        }
        
        // الإمارات - VAT
        public static class UAE {
            public static final String FEATURE_NAME = "UAE VAT Compliance";
            public static final String FEATURE_NAME_AR = "التوافق مع ضريبة القيمة المضافة الإماراتية";
            public static final String DESCRIPTION = "توافق مع متطلبات الضريبة في الإمارات";
            public static final boolean REQUIRES_TRN = true;
            public static final boolean SUPPORTS_DIGITAL_INVOICE = true;
        }
        
        // مصر - نظام الفواتير الإلكترونية
        public static class Egypt {
            public static final String FEATURE_NAME = "Egypt E-Invoice System";
            public static final String FEATURE_NAME_AR = "منظومة الفاتورة الإلكترونية المصرية";
            public static final String DESCRIPTION = "التكامل مع منظومة الفاتورة الإلكترونية المصرية";
            public static final boolean REQUIRES_TAX_ID = true;
            public static final boolean REQUIRES_DIGITAL_SIGNATURE = true;
        }
        
        // المغرب - Facture électronique
        public static class Morocco {
            public static final String FEATURE_NAME = "Morocco E-Invoice";
            public static final String FEATURE_NAME_AR = "الفوترة الإلكترونية المغربية";
            public static final String DESCRIPTION = "دعم نظام الفواتير الإلكترونية المغربي";
            public static final boolean MULTI_CURRENCY = true; // درهم/يورو
        }
        
        // الجزائر
        public static class Algeria {
            public static final String FEATURE_NAME = "Algeria Tax System";
            public static final String FEATURE_NAME_AR = "النظام الضريبي الجزائري";
            public static final String DESCRIPTION = "توافق مع النظام الضريبي الجزائري";
            public static final boolean REQUIRES_NIF = true; // رقم التعريف الضريبي
        }
        
        // تونس
        public static class Tunisia {
            public static final String FEATURE_NAME = "Tunisia Fiscal System";
            public static final String FEATURE_NAME_AR = "النظام الجبائي التونسي";
            public static final String DESCRIPTION = "توافق مع النظام الجبائي التونسي";
        }
        
        // السودان
        public static class Sudan {
            public static final String FEATURE_NAME = "Sudan Tax System";
            public static final String FEATURE_NAME_AR = "النظام الضريبي السوداني";
            public static final String DESCRIPTION = "دعم النظام الضريبي السوداني";
            public static final boolean SUPPORTS_MULTI_PRICE = true; // أسعار متعددة
        }
        
        // ليبيا
        public static class Libya {
            public static final String FEATURE_NAME = "Libya Business System";
            public static final String FEATURE_NAME_AR = "النظام التجاري الليبي";
            public static final String DESCRIPTION = "دعم النظام التجاري الليبي";
        }
        
        // الكويت
        public static class Kuwait {
            public static final String FEATURE_NAME = "Kuwait Business System";
            public static final String FEATURE_NAME_AR = "النظام التجاري الكويتي";
            public static final String DESCRIPTION = "دعم النظام التجاري الكويتي";
            public static final boolean NO_VAT = true; // لا توجد ضريبة قيمة مضافة
        }
        
        // قطر
        public static class Qatar {
            public static final String FEATURE_NAME = "Qatar Business System";
            public static final String FEATURE_NAME_AR = "النظام التجاري القطري";
            public static final String DESCRIPTION = "دعم النظام التجاري القطري";
            public static final boolean NO_VAT = true;
        }
        
        // عمان
        public static class Oman {
            public static final String FEATURE_NAME = "Oman VAT System";
            public static final String FEATURE_NAME_AR = "نظام ضريبة القيمة المضافة العماني";
            public static final String DESCRIPTION = "توافق مع ضريبة القيمة المضافة في عمان";
            public static final boolean REQUIRES_TRN = true;
        }
        
        // اليمن
        public static class Yemen {
            public static final String FEATURE_NAME = "Yemen Tax System";
            public static final String FEATURE_NAME_AR = "النظام الضريبي اليمني";
            public static final String DESCRIPTION = "دعم النظام الضريبي اليمني";
        }
    }

    // ════════════════════════════════════════════════════════════
    // Backup Settings - إعدادات النسخ الاحتياطي
    // ════════════════════════════════════════════════════════════
    public static class Backup {
        // فترات النسخ الاحتياطي التلقائي
        public static final String INTERVAL_DAILY = "daily";
        public static final String INTERVAL_WEEKLY = "weekly";
        public static final String INTERVAL_MONTHLY = "monthly";
        public static final String INTERVAL_MANUAL = "manual";
        public static final String INTERVAL_OFF = "off";
        
        // الأسماء بالعربية
        public static final String[] INTERVAL_NAMES_AR = {
            "يومي", "أسبوعي", "شهري", "يدوي فقط", "متوقف"
        };
        
        // القيم
        public static final String[] INTERVAL_VALUES = {
            INTERVAL_DAILY, INTERVAL_WEEKLY, INTERVAL_MONTHLY, INTERVAL_MANUAL, INTERVAL_OFF
        };
        
        // عدد النسخ الاحتياطية المحفوظة
        public static final int MAX_BACKUPS = 10;
        public static final int DEFAULT_KEEP_BACKUPS = 5;
        
        // أنواع النسخ الاحتياطي
        public static final String TYPE_LOCAL = "local";
        public static final String TYPE_CLOUD = "cloud";
        public static final String TYPE_BOTH = "both";
        
        // مواقع التخزين
        public static final String LOCATION_INTERNAL = "internal";
        public static final String LOCATION_EXTERNAL = "external";
        public static final String LOCATION_GOOGLE_DRIVE = "google_drive";
        public static final String LOCATION_DROPBOX = "dropbox";
    }

    // ════════════════════════════════════════════════════════════
    // Request Codes
    // ════════════════════════════════════════════════════════════
    public static class RequestCode {
        public static final int BARCODE_SCAN = 1001;
        public static final int IMAGE_PICK = 1002;
        public static final int CAMERA_CAPTURE = 1003;
        public static final int LOCATION_PICK = 1004;
        public static final int FILE_PICK = 1005;
        public static final int BACKUP_CREATE = 1006;
        public static final int BACKUP_RESTORE = 1007;
        public static final int PERMISSION_REQUEST = 1008;
    }

    // ════════════════════════════════════════════════════════════
    // Intent Extras Keys
    // ════════════════════════════════════════════════════════════
    public static class IntentExtra {
        public static final String PRODUCT_ID = "product_id";
        public static final String PRODUCT_BARCODE = "product_barcode";
        public static final String CUSTOMER_ID = "customer_id";
        public static final String SUPPLIER_ID = "supplier_id";
        public static final String INVOICE_ID = "invoice_id";
        public static final String EXPENSE_ID = "expense_id";
        public static final String MODE = "mode";
        public static final String TITLE = "title";
        public static final String DATA = "data";
        public static final String RESULT = "result";
        public static final String COUNTRY_CODE = "country_code";
    }

    // ════════════════════════════════════════════════════════════
    // Mode Constants
    // ════════════════════════════════════════════════════════════
    public static class Mode {
        public static final String ADD = "add";
        public static final String EDIT = "edit";
        public static final String VIEW = "view";
    }

    // ════════════════════════════════════════════════════════════
    // Shared Preferences Keys
    // ════════════════════════════════════════════════════════════
    public static class Prefs {
        public static final String NAME = "pos_prefs";
        
        // Store Settings
        public static final String STORE_NAME = "store_name";
        public static final String STORE_PHONE = "store_phone";
        public static final String STORE_ADDRESS = "store_address";
        public static final String STORE_LOGO = "store_logo";
        public static final String TAX_RATE = "tax_rate";
        public static final String TAX_ENABLED = "tax_enabled";
        public static final String CUSTOM_TAX_RATE = "custom_tax_rate";
        public static final String CURRENCY = "currency";
        public static final String COUNTRY_CODE = "country_code";
        public static final String TAX_REGISTRATION_NUMBER = "tax_registration_number";
        
        // App Settings
        public static final String LANGUAGE = "language";
        public static final String THEME = "theme";
        public static final String DARK_MODE = "dark_mode";
        public static final String FIRST_LAUNCH = "first_launch";
        public static final String LAST_BACKUP = "last_backup";
        public static final String AUTO_BACKUP = "auto_backup";
        public static final String BACKUP_INTERVAL = "backup_interval";
        public static final String BACKUP_TIME = "backup_time";
        public static final String BACKUP_LOCATION = "backup_location";
        
        // Printer Settings
        public static final String PRINTER_TYPE = "printer_type";
        public static final String PRINTER_ADDRESS = "printer_address";
        public static final String PRINTER_NAME = "printer_name";
        public static final String PAPER_WIDTH = "paper_width";
        public static final String AUTO_PRINT = "auto_print";
        public static final String SHOW_LOGO = "show_logo";
        
        // Notifications
        public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
        public static final String LOW_STOCK_ALERT = "low_stock_alert";
        public static final String DEBT_ALERTS = "debt_alerts";
        
        // Country-Specific Features
        public static final String ZATCA_ENABLED = "zatca_enabled";
        public static final String E_INVOICE_ENABLED = "e_invoice_enabled";
    }

    // ════════════════════════════════════════════════════════════
    // Payment Methods
    // ════════════════════════════════════════════════════════════
    public static class PaymentMethod {
        public static final String CASH = "نقدي";
        public static final String CARD = "بطاقة";
        public static final String BANK_TRANSFER = "تحويل بنكي";
        public static final String WALLET = "محفظة إلكترونية";
        public static final String CREDIT = "آجل";
    }

    // ════════════════════════════════════════════════════════════
    // Product Units
    // ════════════════════════════════════════════════════════════
    public static class Unit {
        public static final String PIECE = "قطعة";
        public static final String KG = "كيلو";
        public static final String GRAM = "جرام";
        public static final String LITER = "لتر";
        public static final String METER = "متر";
        public static final String BOX = "علبة";
        public static final String PACK = "عبوة";
        public static final String DOZEN = "دستة";
    }

    // ════════════════════════════════════════════════════════════
    // Expense Types
    // ════════════════════════════════════════════════════════════
    public static class ExpenseType {
        public static final String RENT = "إيجار";
        public static final String UTILITIES = "فواتير";
        public static final String SALARIES = "رواتب";
        public static final String SUPPLIES = "مستلزمات";
        public static final String MAINTENANCE = "صيانة";
        public static final String TRANSPORTATION = "مواصلات";
        public static final String MARKETING = "تسويق";
        public static final String OTHER = "أخرى";
    }

    // ════════════════════════════════════════════════════════════
    // Report Types
    // ════════════════════════════════════════════════════════════
    public static class ReportType {
        public static final String DAILY = "daily";
        public static final String WEEKLY = "weekly";
        public static final String MONTHLY = "monthly";
        public static final String YEARLY = "yearly";
        public static final String CUSTOM = "custom";
    }

    // ════════════════════════════════════════════════════════════
    // File Paths
    // ════════════════════════════════════════════════════════════
    public static class FilePath {
        public static final String BACKUP_FOLDER = "SmartPOS/Backups";
        public static final String REPORTS_FOLDER = "SmartPOS/Reports";
        public static final String IMAGES_FOLDER = "SmartPOS/Images";
        public static final String TEMP_FOLDER = "SmartPOS/Temp";
        public static final String EXPORT_FOLDER = "SmartPOS/Exports";
    }

    // ════════════════════════════════════════════════════════════
    // File Extensions
    // ════════════════════════════════════════════════════════════
    public static class FileExtension {
        public static final String BACKUP = ".db";
        public static final String EXCEL = ".xlsx";
        public static final String PDF = ".pdf";
        public static final String IMAGE_PNG = ".png";
        public static final String IMAGE_JPG = ".jpg";
        public static final String JSON = ".json";
    }

    // ════════════════════════════════════════════════════════════
    // Printer Types
    // ════════════════════════════════════════════════════════════
    public static class PrinterType {
        public static final String BLUETOOTH = "bluetooth";
        public static final String USB = "usb";
        public static final String WIFI = "wifi";
        public static final String NONE = "none";
    }

    // ════════════════════════════════════════════════════════════
    // Theme Constants
    // ════════════════════════════════════════════════════════════
    public static class Theme {
        public static final String LIGHT = "light";
        public static final String DARK = "dark";
        public static final String SYSTEM = "system";
    }

    // ════════════════════════════════════════════════════════════
    // Language Constants
    // ════════════════════════════════════════════════════════════
    public static class Language {
        public static final String ARABIC = "ar";
        public static final String ENGLISH = "en";
    }

    // ════════════════════════════════════════════════════════════
    // Notification Constants
    // ════════════════════════════════════════════════════════════
    public static class Notification {
        public static final String CHANNEL_ID = "pos_channel";
        public static final String CHANNEL_NAME = "SmartPOS Notifications";
        public static final String CHANNEL_DESC = "إشعارات نظام نقاط البيع";
        public static final int LOW_STOCK_ID = 1001;
        public static final int BACKUP_ID = 1002;
        public static final int SALE_ID = 1003;
        public static final int DEBT_ID = 1004;
    }

    // ════════════════════════════════════════════════════════════
    // Sort Options
    // ════════════════════════════════════════════════════════════
    public static class SortBy {
        public static final String NAME_ASC = "name_asc";
        public static final String NAME_DESC = "name_desc";
        public static final String PRICE_ASC = "price_asc";
        public static final String PRICE_DESC = "price_desc";
        public static final String DATE_ASC = "date_asc";
        public static final String DATE_DESC = "date_desc";
        public static final String QUANTITY_ASC = "quantity_asc";
        public static final String QUANTITY_DESC = "quantity_desc";
    }

    // ════════════════════════════════════════════════════════════
    // Status Constants
    // ════════════════════════════════════════════════════════════
    public static class Status {
        public static final String ACTIVE = "active";
        public static final String INACTIVE = "inactive";
        public static final String PENDING = "pending";
        public static final String COMPLETED = "completed";
        public static final String CANCELLED = "cancelled";
    }

    // ════════════════════════════════════════════════════════════
    // API Constants
    // ════════════════════════════════════════════════════════════
    public static class API {
        public static final String BASE_URL = "https://api.smartpos.app/";
        public static final int TIMEOUT_SECONDS = 30;
        public static final String API_KEY = "your_api_key_here";
        
        // ZATCA API (Saudi Arabia)
        public static final String ZATCA_BASE_URL = "https://api.zatca.gov.sa/";
        public static final String ZATCA_SANDBOX_URL = "https://sandbox.zatca.gov.sa/";
    }

    // ════════════════════════════════════════════════════════════
    // Error Codes
    // ════════════════════════════════════════════════════════════
    public static class ErrorCode {
        public static final int SUCCESS = 0;
        public static final int GENERAL_ERROR = -1;
        public static final int DATABASE_ERROR = -2;
        public static final int NETWORK_ERROR = -3;
        public static final int VALIDATION_ERROR = -4;
        public static final int PERMISSION_ERROR = -5;
        public static final int FILE_ERROR = -6;
        public static final int PRINTER_ERROR = -7;
        public static final int BACKUP_ERROR = -8;
    }

    // ════════════════════════════════════════════════════════════
    // Limits & Constraints
    // ════════════════════════════════════════════════════════════
    public static class Limits {
        public static final int MAX_CART_ITEMS = 100;
        public static final int MAX_INVOICE_ITEMS = 100;
        public static final int MAX_SEARCH_RESULTS = 50;
        public static final int PAGE_SIZE = 20;
        public static final int MIN_SEARCH_CHARS = 2;
        public static final long DEBOUNCE_DELAY_MS = 300;
    }

    // ════════════════════════════════════════════════════════════
    // Animation Durations
    // ════════════════════════════════════════════════════════════
    public static class Animation {
        public static final int FAST = 150;
        public static final int NORMAL = 300;
        public static final int SLOW = 500;
    }

    // ════════════════════════════════════════════════════════════
    // Regex Patterns
    // ════════════════════════════════════════════════════════════
    public static class Pattern {
        public static final String BARCODE = "^[0-9]{8,13}$";
        public static final String PHONE = "^[0-9]{10,15}$";
        public static final String EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        public static final String PRICE = "^\\d+(\\.\\d{1,2})?$";
        public static final String NUMERIC = "^[0-9]+$";
        public static final String TAX_NUMBER = "^[0-9]{15}$"; // رقم السجل الضريبي
    }

    // ════════════════════════════════════════════════════════════
    // Helper Methods
    // ════════════════════════════════════════════════════════════
    
    /**
     * الحصول على العملة حسب كود الدولة
     */
    public static String getCurrencyForCountry(String countryCode) {
        switch (countryCode) {
            case Countries.SAUDI_ARABIA: return Countries.CURRENCY_SAR;
            case Countries.UAE: return Countries.CURRENCY_AED;
            case Countries.EGYPT: return Countries.CURRENCY_EGP;
            case Countries.MOROCCO: return Countries.CURRENCY_MAD;
            case Countries.ALGERIA: return Countries.CURRENCY_DZD;
            case Countries.TUNISIA: return Countries.CURRENCY_TND;
            case Countries.SUDAN: return Countries.CURRENCY_SDG;
            case Countries.LIBYA: return Countries.CURRENCY_LYD;
            case Countries.KUWAIT: return Countries.CURRENCY_KWD;
            case Countries.QATAR: return Countries.CURRENCY_QAR;
            case Countries.OMAN: return Countries.CURRENCY_OMR;
            case Countries.YEMEN: return Countries.CURRENCY_YER;
            case Countries.BAHRAIN: return Countries.CURRENCY_BHD;
            case Countries.JORDAN: return Countries.CURRENCY_JOD;
            case Countries.LEBANON: return Countries.CURRENCY_LBP;
            case Countries.PALESTINE: return Countries.CURRENCY_ILS;
            case Countries.IRAQ: return Countries.CURRENCY_IQD;
            case Countries.SYRIA: return Countries.CURRENCY_SYP;
            default: return Countries.CURRENCY_SAR;
        }
    }
    
    /**
     * الحصول على معدل الضريبة حسب كود الدولة
     */
    public static double getTaxRateForCountry(String countryCode) {
        switch (countryCode) {
            case Countries.SAUDI_ARABIA: return Countries.TAX_RATE_SA;
            case Countries.UAE: return Countries.TAX_RATE_AE;
            case Countries.EGYPT: return Countries.TAX_RATE_EG;
            case Countries.MOROCCO: return Countries.TAX_RATE_MA;
            case Countries.ALGERIA: return Countries.TAX_RATE_DZ;
            case Countries.TUNISIA: return Countries.TAX_RATE_TN;
            case Countries.SUDAN: return Countries.TAX_RATE_SD;
            case Countries.LIBYA: return Countries.TAX_RATE_LY;
            case Countries.KUWAIT: return Countries.TAX_RATE_KW;
            case Countries.QATAR: return Countries.TAX_RATE_QA;
            case Countries.OMAN: return Countries.TAX_RATE_OM;
            case Countries.YEMEN: return Countries.TAX_RATE_YE;
            case Countries.BAHRAIN: return Countries.TAX_RATE_BH;
            case Countries.JORDAN: return Countries.TAX_RATE_JO;
            case Countries.LEBANON: return Countries.TAX_RATE_LB;
            case Countries.PALESTINE: return Countries.TAX_RATE_PS;
            case Countries.IRAQ: return Countries.TAX_RATE_IQ;
            case Countries.SYRIA: return Countries.TAX_RATE_SY;
            default: return Countries.TAX_RATE_SA;
        }
    }
    
    /**
     * الحصول على اسم الدولة بالعربية
     */
    public static String getCountryName(String countryCode) {
        for (int i = 0; i < Countries.COUNTRY_CODES.length; i++) {
            if (Countries.COUNTRY_CODES[i].equals(countryCode)) {
                return Countries.COUNTRY_NAMES_AR[i];
            }
        }
        return "غير معروف";
    }
}
