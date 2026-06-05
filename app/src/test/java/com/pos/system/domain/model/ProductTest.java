package com.pos.system.domain.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProductTest {

    @Test
    public void isLowStock_whenQtyBelowReorderLevel_returnsTrue() {
        Product p = new Product();
        p.qty = 2;
        p.reorderLevel = 5;
        assertTrue(p.isLowStock());
    }

    @Test
    public void isLowStock_whenQtyEqualsReorderLevel_returnsTrue() {
        Product p = new Product();
        p.qty = 5;
        p.reorderLevel = 5;
        assertTrue(p.isLowStock());
    }

    @Test
    public void isLowStock_whenQtyAboveReorderLevel_returnsFalse() {
        Product p = new Product();
        p.qty = 10;
        p.reorderLevel = 5;
        assertFalse(p.isLowStock());
    }

    @Test
    public void constructor_setsFields() {
        Product p = new Product("123", "Test", 9.99, 50);
        assertEquals("123", p.barcode);
        assertEquals("Test", p.name);
        assertEquals(9.99, p.price, 0.001);
        assertEquals(50, p.qty);
    }
}
