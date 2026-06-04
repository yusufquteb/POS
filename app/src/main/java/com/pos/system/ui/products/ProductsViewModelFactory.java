package com.pos.system.ui.products;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.pos.system.domain.repository.ProductRepository;

public class ProductsViewModelFactory implements ViewModelProvider.Factory {

    private final ProductRepository repository;

    public ProductsViewModelFactory(ProductRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProductsViewModel.class)) {
            return (T) new ProductsViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}
