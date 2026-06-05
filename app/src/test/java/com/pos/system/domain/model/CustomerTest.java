package com.pos.system.domain.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class CustomerTest {

    @Test
    public void hasDebt_whenDebtPositive_returnsTrue() {
        Customer c = new Customer("Ahmed", "0500000000");
        c.debt = 150.0;
        assertTrue(c.hasDebt());
    }

    @Test
    public void hasDebt_whenDebtZero_returnsFalse() {
        Customer c = new Customer("Ahmed", "0500000000");
        c.debt = 0.0;
        assertFalse(c.hasDebt());
    }

    @Test
    public void constructor_setsNameAndPhone() {
        Customer c = new Customer("Sara", "0551234567");
        assertEquals("Sara", c.name);
        assertEquals("0551234567", c.phone);
    }
}
