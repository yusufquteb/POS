package com.pos.system.domain.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class InvoiceTest {

    @Test
    public void isCompleted_whenStatusCompleted_returnsTrue() {
        Invoice inv = new Invoice();
        inv.status = "completed";
        assertTrue(inv.isCompleted());
    }

    @Test
    public void isReturned_whenStatusReturned_returnsTrue() {
        Invoice inv = new Invoice();
        inv.status = "returned";
        assertTrue(inv.isReturned());
    }

    @Test
    public void isCompleted_whenStatusReturned_returnsFalse() {
        Invoice inv = new Invoice();
        inv.status = "returned";
        assertFalse(inv.isCompleted());
    }
}
