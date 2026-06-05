package com.pos.system.domain.repository;

import com.pos.system.domain.model.Product;
import java.util.List;

public interface ProductRepository {
    List<Product> getAllProducts();
    List<Product> searchProducts(String query);
    List<Product> getLowStockProducts(int threshold);
    List<Product> getExpiringProducts(int daysAhead);
    Product getProductById(long id);
    Product getProductByBarcode(String barcode);
    long insertProduct(Product product);
    int updateProduct(Product product);
    int deleteProduct(long id);
    boolean barcodeExists(String barcode);
    int getTotalCount();
    double getTotalInventoryValue();
}
