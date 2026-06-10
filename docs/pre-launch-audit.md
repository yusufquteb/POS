# POS System — Pre-Launch Audit Report
**Date:** 2026-06-10  
**Branch:** `claude/pre-launch-stabilization-199n6k`  
**Auditor:** Claude Code (automated + manual review)

---

## Summary

| Category | Count | Status |
|---|---|---|
| Critical Accounting Bugs | 4 | ✅ Fixed in this branch |
| Unreachable Screens Fixed | 1 | ✅ Fixed in this branch |
| Non-functional UI Buttons | 2 | ⚠️ Identified (see below) |
| Hardcoded Strings Fixed | 3 | ✅ Fixed in this branch |
| Dead Layout Files | 4 | ⚠️ Identified |
| Verified OK (navigation) | 22 MainActivity cards | ✅ |
| Verified OK (font system) | Cairo unified at theme level | ✅ |
| Verified OK (DB sections) | All 28 tables, 160 methods, labeled | ✅ |

---

## 1. Complete Navigation Map

### 1.1 Auth Flow
```
SplashActivity (launcher)
  └─ AuthActivity
       ├─ Guest login → MainActivity
       └─ Registered login → MainActivity
```

### 1.2 MainActivity — 22 Card Sections
| Card ID | Destination Activity | Status |
|---|---|---|
| card_pos | ActivityPOSActivity | ✅ Connected |
| card_products | ActivityProductsActivity | ✅ Connected |
| card_customers | ActivityCustomersActivity | ✅ Connected |
| card_invoices | ActivityInvoicesActivity | ✅ Connected |
| card_returns | ActivityReturnActivity | ✅ Connected |
| card_reports | ActivityReportsActivity | ✅ Connected |
| card_expenses | ActivityExpensesActivity | ✅ Connected |
| card_suppliers | ActivitySuppliersActivity | ✅ Connected |
| card_purchase_orders | ActivityPurchaseOrdersActivity | ✅ Connected |
| card_debts | ActivityDebtsActivity | ✅ Connected |
| card_price_quotes | ActivityPriceQuotesActivity | ✅ Connected |
| card_stock | ActivityStockCountActivity | ✅ Connected |
| card_analytics | ActivityAnalyticsActivity | ✅ Connected |
| card_expiry | ActivityExpiryDashboardActivity | ✅ Connected |
| card_cash_drawer | ActivityCashDrawerActivity | ✅ Connected |
| card_backup | ActivityBackupActivity | ✅ Connected |
| card_shifts | ActivityShiftsActivity | ✅ Connected |
| card_employees | ActivityEmployeesActivity | ✅ Connected |
| card_scanner | ActivityBarcodeScannerActivity | ✅ Connected |
| card_pin | ActivityPinLockActivity | ✅ Connected |
| card_settings | ActivitySettingsActivity | ✅ Connected |
| card_remove_data | ActivityRemoveDataActivity | ✅ Connected |

### 1.3 Settings Screen — Sub-navigation
| Card | Destination | Status |
|---|---|---|
| card_store | ActivityStoreSettingsActivity | ✅ |
| card_locations | ActivityLocationActivity | ✅ Fixed (was unreachable) |
| card_printer | ActivityPrinterSettingsActivity | ✅ |
| card_backup | ActivityBackupActivity | ✅ |
| card_pin_lock | ActivityPinLockActivity | ✅ |
| card_language | Language dialog | ✅ |
| card_about | About dialog | ✅ |
| card_premium | BillingManager dialog | ✅ |
| card_rate_app | ReviewManager / Play Store | ✅ |
| btn_logout | Confirm + AuthActivity.logout() | ✅ |
| btn_remove_data | ActivityRemoveDataActivity | ✅ |

### 1.4 AndroidManifest — Registered Activities (44 total)
- All 44 activities registered and `exported=false` (except SplashActivity).
- All activities audited for reachability — see section 5.

---

## 2. Complete Database Flow Map

### 2.1 Tables (28 total)
| Table | Used by |
|---|---|
| products | POS, Products, Stock, Invoices, Returns |
| customers | POS, Customers, Debts |
| suppliers | Suppliers, PurchaseOrders, Debts |
| invoices | POS, Invoices, Reports |
| invoice_items | POS, Invoices, Reports (COGS) |
| returns | Returns |
| return_items | Returns |
| expenses | Expenses, Reports |
| purchase_orders | PurchaseOrders |
| purchase_order_items | PurchaseOrders |
| customer_debt_payments | Debts, CustomerDetail |
| supplier_debt_payments | Debts, SupplierDetail |
| employees | Employees |
| shifts | Shifts |
| cash_drawer | CashDrawer |
| locations | LocationActivity |
| warehouses | LocationActivity |
| price_quotes | PriceQuotes |
| price_quote_items | PriceQuotes |
| stock_counts | StockCount |
| stock_count_items | StockCount |
| store_settings | StoreSettings |
| printer_settings | PrinterSettings |
| categories | Products |
| units | Products |
| barcodes | Products |
| app_settings | ThemeManager, LanguageManager |
| notifications | (reserved) |

### 2.2 Key Transactional Flows

#### Sale (POS → Invoice)
```
ActivityPOSActivity
  └─ DBHelper.createInvoice() / createInvoiceWithPartialPayment()
       ├─ INSERT invoices
       ├─ INSERT invoice_items (per product)
       ├─ UPDATE products.qty (decrement per item)         ← stock reduced
       ├─ [FIXED] addCustomerDebt() if remainingAmount > 0 ← debt recorded
       └─ updateCustomerStats()
```

#### Return
```
ActivityReturnActivity
  └─ DBHelper.createReturn()
       ├─ INSERT returns
       ├─ INSERT return_items
       ├─ UPDATE products.qty (restore per item)            ← stock restored
       └─ [FIXED] Reduce customer debt if invoice had credit
            ├─ UPDATE invoices.remaining_amount
            └─ UPDATE customers.debt (MAX(0, debt - reduction))
```

#### Purchase Order Receipt
```
ActivityPurchaseOrdersActivity
  └─ DBHelper.receivePurchaseOrder()
       ├─ UPDATE products.qty (increase per item)           ← stock increased
       ├─ UPDATE purchase_orders.status = 'received'
       └─ [FIXED] UPDATE suppliers.debt += po.total        ← supplier debt recorded
```

---

## 3. Critical Accounting Bugs (Fixed)

### Bug 1 — Credit Sales Don't Record Customer Debt
**File:** `DBHelper.java:3729` — `createInvoiceWithPartialPayment()`  
**Symptom:** Customer buys on credit (paid < total), but `customers.debt` is never updated.  
**Root cause:** Method inserts invoice with `remaining_amount > 0` but never calls `addCustomerDebt()`.  
**Fix:** After transaction commits, call `addCustomerDebt(customerId, remainingAmount, invoiceNumber)` when `remainingAmount > 0`.

### Bug 2 — Returns Don't Reduce Customer Debt
**File:** `DBHelper.java:2041` — `createReturn()`  
**Symptom:** When a credit-sale invoice has returned items, the customer's outstanding debt is not reduced.  
**Root cause:** Method restores stock but makes no debt adjustment.  
**Fix:** Inside the transaction, query original invoice's `remaining_amount`. If > 0, reduce `customers.debt` by `min(refund, remaining)` and update `invoices.remaining_amount`.

### Bug 3 — Purchase Order Receipt Doesn't Create Supplier Debt
**File:** `DBHelper.java:2621` — `receivePurchaseOrder()`  
**Symptom:** Receiving a PO increases stock but never adds debt to the supplier's balance.  
**Root cause:** Method updates `products.qty` and `po.status` but never calls `updateSupplierDebt()`.  
**Fix:** Inside transaction, query PO for `supplier_id` and `total`, then `UPDATE suppliers SET debt = debt + total`.

### Bug 4 — Report Profit Overstated (Missing COGS)
**File:** `DBHelper.java:1678` — `getReportSummary()`  
**Symptom:** `net_profit = total_sales - expenses`. Cost of goods sold (COGS) is ignored, so gross margin is reported as net profit — heavily overstated.  
**Root cause:** Expenses cursor is the only deduction. The correct formula is in `getFullProfitReport()` (line 1341) but not in the summary.  
**Fix:** Add COGS query (`SUM(invoice_items.qty * products.cost_price)`), store as `total_cogs`, and compute `net_profit = sales - cogs - expenses`.

---

## 4. Non-functional UI Features

### 4.1 Expiry Dashboard — "Mark All Reviewed" Button (No-op)
**File:** `ActivityExpiryDashboardActivity.java`  
**Button:** `btn_mark_all_reviewed`  
**Current behavior:** Click listener exists but calls a method with no DB write — no row is updated.  
**Expected:** `UPDATE products SET expiry_reviewed = 1 WHERE ...` or equivalent flag.  
**Priority:** Medium — misleads staff into thinking action was taken.

### 4.2 Price Quote → Invoice Conversion (Not Implemented)
**File:** `ActivityPriceQuotesActivity.java`  
**Button:** `btn_convert_to_invoice`  
**Current behavior:** Shows a toast "قريباً" (coming soon).  
**Expected:** Load quote items into POS cart and open ActivityPOSActivity with pre-filled items.  
**Priority:** Medium — advertised feature shown in UI but disabled.

---

## 5. Unreachable Activities (Fixed)

### 5.1 ActivityLocationActivity — Was Completely Isolated
**Before:** No Activity, button, or menu item navigated to `ActivityLocationActivity`.  
It was registered in `AndroidManifest.xml` with `parentActivityName=".ActivitySettingsActivity"` but no entry point existed.  
**Fix applied:** Added `card_locations` card in `activity_settings.xml`, wired click listener in `ActivitySettingsActivity.java`, and added string resources (`settings_locations`, `settings_locations_desc`).

---

## 6. Dead Code / Unused Files

### 6.1 Unused Layout Files
| File | Evidence of Disuse |
|---|---|
| `res/layout/ght.xml` | Not referenced in any Java/Kotlin or included by any layout |
| `res/layout/main.xml` | Not `setContentView`-d or `<include>`-d anywhere |
| `res/layout/layout_custom_dialog.xml` | No `LayoutInflater.inflate()` call references it |
| `res/layout/layout_base_dialog.xml` | No `LayoutInflater.inflate()` call references it |

**Recommendation:** Remove all four to reduce APK size and avoid confusion.

### 6.2 Hardcoded Strings Fixed
Three hardcoded Arabic strings in `activity_settings.xml` were replaced with proper string resources (`settings_remove_data`, `settings_locations`, `settings_locations_desc`).

---

## 7. Accounting Consistency Verification

| Flow | Invoice | Stock | Debt | Reports |
|---|---|---|---|---|
| Cash Sale | ✅ recorded | ✅ decremented | N/A | ✅ in sales |
| Credit Sale (partial pay) | ✅ recorded | ✅ decremented | ✅ Fixed | ✅ in sales |
| Return (cash refund) | ✅ recorded | ✅ restored | ✅ Fixed | ✅ as negative |
| Return (debt reduction) | ✅ recorded | ✅ restored | ✅ Fixed | ✅ as negative |
| PO Received | N/A | ✅ incremented | ✅ Fixed | N/A |
| Expense Recorded | N/A | N/A | N/A | ✅ in expenses |
| Profit Calculation | — | — | — | ✅ Fixed (COGS) |

---

## 8. Verified OK

| Area | Finding |
|---|---|
| MainActivity cards | All 22 cards connected to correct destinations |
| Font system | Cairo font unified at theme level (`res/font/`) — no stray `sans-serif-medium` in critical paths |
| DBHelper sections | All 28 tables and ~160 methods have labeled section dividers |
| Backup / Restore | `SafeBackupManager` fully implemented with JSON export/import |
| PIN Lock | `ActivityPinLockActivity` fully functional (set/verify/clear) |
| Stock Count | `ActivityStockCountActivity` correctly writes adjustments |
| Cash Drawer | `ActivityCashDrawerActivity` records open/close with float |
| AndroidManifest | All 44 activities registered; only SplashActivity is `exported=true` |
| Auth flow | Guest + registered login both reach MainActivity |
| Logout | `AuthActivity.logout()` clears session + returns to AuthActivity |

---

## 9. Pre-Launch Priority Checklist

### P0 — Critical (Data Integrity) ✅ Done
- [x] Fix: Credit sales not recording customer debt (`createInvoiceWithPartialPayment`)
- [x] Fix: Returns not adjusting customer debt (`createReturn`)
- [x] Fix: PO receipt not creating supplier debt (`receivePurchaseOrder`)
- [x] Fix: Profit report missing COGS (`getReportSummary`)
- [x] Fix: `ActivityLocationActivity` unreachable (Settings card added)

### P1 — High (Feature Completeness) ✅ Done
- [x] Fix: Expiry "Mark All Reviewed" — adds `expiry_reviewed` column (DB v10), marks products, hides from dashboard with count feedback
- [x] Fix: Price Quote → Invoice — tap a quote → "تحويل إلى فاتورة" → opens cart pre-filled; quote marked as converted

### P2 — Medium (Cleanup) ✅ Done
- [x] Removed 4 unused layout files (`ght.xml`, `main.xml`, `layout_custom_dialog.xml`, `layout_base_dialog.xml`)
- [x] Privacy URL now reads from `@string/privacy_policy_url` resource (defaults to Play Store URL); set the string to your real URL
- [x] Shift close: if |actual_cash − expected| > 1.0, warns with discrepancy amount before saving

### P3 — Low (Polish) ✅ Done
- [x] "Mark All Reviewed" shows confirmation count + snackbar feedback after marking
- [ ] Customer detail tabs: architectural refactor deferred (Activity→Fragment migration needed)
- [ ] Analytics tabs: architectural refactor deferred (Activity→Fragment migration needed)

---

*Report generated by automated audit pass on branch `claude/pre-launch-stabilization-199n6k`.*
