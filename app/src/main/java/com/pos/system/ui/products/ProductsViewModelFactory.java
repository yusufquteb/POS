package com.pos.system.ui.products;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.pos.system.domain.repository.ProductRepository;

public class ProductsViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;
    private final ProductRepository repository;

    public ProductsViewModelFactory(Application application, ProductRepository repository) {
        this.application = application;
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProductsViewModel.class)) {
            return (T) new ProductsViewModel(application, repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}
