package com.pos.system.managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.pos.system.DBHelper;
import com.android.billingclient.api.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * BillingManager - إدارة المشتريات والاشتراكات عبر Google Play
 * تاريخ الإنشاء: 12 فبراير 2026
 * 
 * المميزات:
 * ✅ إدارة الاشتراكات الشهرية والسنوية
 * ✅ التحقق من حالة Premium
 * ✅ استعادة المشتريات
 * ✅ معالجة الأخطاء
 */
public class BillingManager implements PurchasesUpdatedListener {
    
    private static final String TAG = "BillingManager";
    
    // Product IDs
    public static final String PREMIUM_MONTHLY = "premium_monthly";
    public static final String PREMIUM_YEARLY = "premium_yearly";
    public static final String PREMIUM_LIFETIME = "premium_lifetime";
    
    private BillingClient billingClient;
    private Context context;
    private DBHelper dbHelper;
    private BillingListener billingListener;
    
    // حالة الاتصال
    private boolean isReady = false;
    
    /**
     * Interface للاستماع لأحداث الدفع
     */
    public interface BillingListener {
        void onPurchaseSuccess(Purchase purchase);
        void onPurchaseFailure(String error);
        void onBillingReady();
    }
    
    /**
     * Constructor
     */
    public BillingManager(Context context, DBHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
        initBillingClient();
    }
    
    /**
     * تهيئة Billing Client
     */
    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build();
        
        // الاتصال بـ Google Play
        connectToBillingService();
    }
    
    /**
     * الاتصال بخدمة الدفع
     */
    private void connectToBillingService() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isReady = true;
                    Log.d(TAG, "Billing setup finished successfully");
                    
                    // استعادة المشتريات السابقة
                    restorePurchases();
                    
                    if (billingListener != null) {
                        billingListener.onBillingReady();
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    isReady = false;
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                isReady = false;
                Log.d(TAG, "Billing service disconnected");
                // إعادة المحاولة بعد 3 ثواني
                retryConnection();
            }
        });
    }
    
    /**
     * إعادة محاولة الاتصال
     */
    private void retryConnection() {
        new android.os.Handler().postDelayed(() -> {
            if (!isReady) {
                connectToBillingService();
            }
        }, 3000);
    }
    
    /**
     * شراء اشتراك Premium
     */
    public void purchasePremium(Activity activity, String productId) {
        if (!isReady) {
            Log.e(TAG, "Billing client not ready");
            if (billingListener != null) {
                billingListener.onPurchaseFailure("خدمة الدفع غير جاهزة، يرجى المحاولة لاحقاً");
            }
            return;
        }
        
        // إنشاء قائمة المنتجات
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );
        
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();
        
        // الاستعلام عن تفاصيل المنتج
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && 
                productDetailsList != null && !productDetailsList.isEmpty()) {
                
                ProductDetails productDetails = productDetailsList.get(0);
                
                // الحصول على offer token
                List<ProductDetails.SubscriptionOfferDetails> offers = 
                    productDetails.getSubscriptionOfferDetails();
                
                if (offers != null && !offers.isEmpty()) {
                    String offerToken = offers.get(0).getOfferToken();
                    
                    // إنشاء BillingFlowParams
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            List.of(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        )
                        .build();
                    
                    // إطلاق Billing Flow
                    billingClient.launchBillingFlow(activity, flowParams);
                } else {
                    Log.e(TAG, "No subscription offers available");
                    if (billingListener != null) {
                        billingListener.onPurchaseFailure("لا توجد عروض اشتراك متاحة");
                    }
                }
            } else {
                Log.e(TAG, "Failed to query product details: " + billingResult.getDebugMessage());
                if (billingListener != null) {
                    billingListener.onPurchaseFailure("فشل في الحصول على معلومات المنتج");
                }
            }
        });
    }
    
    /**
     * شراء Premium مدى الحياة (In-App Purchase)
     */
    public void purchaseLifetime(Activity activity) {
        if (!isReady) {
            Log.e(TAG, "Billing client not ready");
            if (billingListener != null) {
                billingListener.onPurchaseFailure("خدمة الدفع غير جاهزة");
            }
            return;
        }
        
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        );
        
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();
        
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && 
                productDetailsList != null && !productDetailsList.isEmpty()) {
                
                ProductDetails productDetails = productDetailsList.get(0);
                
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        List.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build();
                
                billingClient.launchBillingFlow(activity, flowParams);
            } else {
                Log.e(TAG, "Failed to query lifetime product: " + billingResult.getDebugMessage());
                if (billingListener != null) {
                    billingListener.onPurchaseFailure("فشل في الحصول على معلومات المنتج");
                }
            }
        });
    }
    
    /**
     * معالجة تحديثات المشتريات
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, 
                                   List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && 
            purchases != null) {
            
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase");
            if (billingListener != null) {
                billingListener.onPurchaseFailure("تم إلغاء عملية الشراء");
            }
        } else {
            Log.e(TAG, "Purchase failed: " + billingResult.getDebugMessage());
            if (billingListener != null) {
                billingListener.onPurchaseFailure("فشل في إتمام عملية الشراء");
            }
        }
    }
    
    /**
     * معالجة المشتريات
     */
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            
            // التحقق من المشتريات غير المؤكدة
            if (!purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            }
            
            // حفظ حالة Premium في قاعدة البيانات
            String productId = purchase.getProducts().get(0);
            String purchaseToken = purchase.getPurchaseToken();
            
            // حساب تاريخ الانتهاء
            String expiryDate = calculateExpiryDate(productId);
            
            // تحديث قاعدة البيانات
            // DBHelper signature: (isPremium, subscriptionType, expiryDate, purchaseToken)
            boolean updated = dbHelper.updateSubscription(
                true,
                productId,
                expiryDate,
                purchaseToken
            );
            
            if (updated) {
                Log.d(TAG, "Premium subscription activated successfully");
                if (billingListener != null) {
                    billingListener.onPurchaseSuccess(purchase);
                }
            } else {
                Log.e(TAG, "Failed to update subscription in database");
            }
        }
    }
    
    /**
     * تأكيد المشتريات
     */
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();
        
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged");
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
            }
        });
    }
    
    /**
     * حساب تاريخ انتهاء الاشتراك
     */
    private String calculateExpiryDate(String productId) {
        Calendar calendar = Calendar.getInstance();
        
        switch (productId) {
            case PREMIUM_MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            case PREMIUM_YEARLY:
                calendar.add(Calendar.YEAR, 1);
                break;
            case PREMIUM_LIFETIME:
                calendar.add(Calendar.YEAR, 100); // 100 سنة = مدى الحياة
                break;
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }
    
    /**
     * استعادة المشتريات السابقة
     */
    public void restorePurchases() {
        if (!isReady) {
            Log.e(TAG, "Billing client not ready");
            return;
        }
        
        // استعادة الاشتراكات
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : purchases) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            handlePurchase(purchase);
                        }
                    }
                }
            }
        );
        
        // استعادة المشتريات لمرة واحدة (Lifetime)
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : purchases) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            handlePurchase(purchase);
                        }
                    }
                }
            }
        );
    }
    
    /**
     * التحقق من حالة Premium
     */
    public boolean isPremiumUser() {
        return dbHelper.isPremiumUser();
    }
    
    /**
     * الحصول على معلومات الاشتراكات المتاحة
     */
    public void queryAvailableProducts(final ProductsCallback callback) {
        if (!isReady) {
            callback.onFailure("خدمة الدفع غير جاهزة");
            return;
        }
        
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        );
        
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();
        
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                callback.onSuccess(productDetailsList);
            } else {
                callback.onFailure("فشل في الحصول على المنتجات");
            }
        });
    }
    
    /**
     * Interface لاستقبال معلومات المنتجات
     */
    public interface ProductsCallback {
        void onSuccess(List<ProductDetails> products);
        void onFailure(String error);
    }
    
    /**
     * تعيين Billing Listener
     */
    public void setBillingListener(BillingListener listener) {
        this.billingListener = listener;
    }
    
    /**
     * إغلاق الاتصال
     */
    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
    
    /**
     * التحقق من جاهزية Billing Client
     */
    public boolean isReady() {
        return isReady && billingClient != null && billingClient.isReady();
    }
}
