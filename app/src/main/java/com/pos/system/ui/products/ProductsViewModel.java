package com.pos.system.ui.products;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.pos.system.R;
import com.pos.system.domain.model.Product;
import com.pos.system.domain.repository.ProductRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductsViewModel extends AndroidViewModel {

    private final ProductRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Long> insertedId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public ProductsViewModel(@NonNull Application application, ProductRepository repository) {
        super(application);
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
                    error.setValue(getApplication().getString(R.string.error_load_products));
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
                mainHandler.post(() -> error.setValue(getApplication().getString(R.string.error_search_products)));
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
                        error.setValue(getApplication().getString(R.string.error_add_product));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue(getApplication().getString(R.string.error_add_product)));
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
                    else error.setValue(getApplication().getString(R.string.error_update_product));
                });
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue(getApplication().getString(R.string.error_update_product)));
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
                    else error.setValue(getApplication().getString(R.string.error_delete_product));
                });
            } catch (Exception e) {
                mainHandler.post(() -> error.setValue(getApplication().getString(R.string.error_delete_product)));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
