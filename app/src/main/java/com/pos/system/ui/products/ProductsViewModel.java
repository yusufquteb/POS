package com.pos.system.ui.products;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.pos.system.domain.model.Product;
import com.pos.system.domain.repository.ProductRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductsViewModel extends ViewModel {

    private final ProductRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Long> insertedId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public ProductsViewModel(ProductRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Long> getInsertedId() { return insertedId; }
    public LiveData<Boolean> getOperationSuccess() { return operationSuccess; }

    public void loadAllProducts() {
        loading.setValue(true);
        executor.execute(() -> {
            try {
                List<Product> result = repository.getAllProducts();
                mainHandler.post(() -> {
                    products.setValue(result);
                    loading.setValue(false);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    error.setValue("فشل تحميل المنتجات");
                    loading.setValue(false);
                });
            }
        });
    }

    public void searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadAllProducts();
            return;
        }
        executor.execute(() -> {
            try {
                List<Product> result = repository.searchProducts("%" + query.trim() + "%");
                mainHandler.post(() -> products.setValue(result));
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue("فشل البحث"));
            }
        });
    }

    public void insertProduct(Product product) {
        executor.execute(() -> {
            try {
                long id = repository.insertProduct(product);
                mainHandler.post(() -> {
                    if (id > 0) {
                        insertedId.setValue(id);
                        loadAllProducts();
                    } else {
                        error.setValue("فشل إضافة المنتج");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue("فشل إضافة المنتج: " + e.getMessage()));
            }
        });
    }

    public void updateProduct(Product product) {
        executor.execute(() -> {
            try {
                int rows = repository.updateProduct(product);
                mainHandler.post(() -> {
                    operationSuccess.setValue(rows > 0);
                    if (rows > 0) loadAllProducts();
                    else error.setValue("فشل تحديث المنتج");
                });
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue("فشل تحديث المنتج: " + e.getMessage()));
            }
        });
    }

    public void deleteProduct(long id) {
        executor.execute(() -> {
            try {
                int rows = repository.deleteProduct(id);
                mainHandler.post(() -> {
                    operationSuccess.setValue(rows > 0);
                    if (rows > 0) loadAllProducts();
                    else error.setValue("فشل حذف المنتج");
                });
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue("فشل حذف المنتج"));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
