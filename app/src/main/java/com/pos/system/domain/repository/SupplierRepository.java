package com.pos.system.domain.repository;

import com.pos.system.domain.model.Supplier;
import java.util.List;

public interface SupplierRepository {
    List<Supplier> getAllSuppliers();
    List<Supplier> searchSuppliers(String query);
    Supplier getSupplierById(long id);
    long insertSupplier(Supplier supplier);
    int updateSupplier(Supplier supplier);
    int deleteSupplier(long id);
    boolean phoneExists(String phone);
    int updateDebt(long supplierId, double debtDelta);
}
