package com.gramcall.ai.utils

import org.junit.Assert.*
import org.junit.Test

class PhoneUtilsTest {
    @Test fun normalizeStripsCountryCodeAndSpaces() {
        assertEquals("9876543210", PhoneUtils.normalize("+91 98765 43210"))
        assertEquals("9876543210", PhoneUtils.normalize("09876543210"))
        assertEquals("9876543210", PhoneUtils.normalize("98765-43210"))
    }

    @Test fun validatesIndianMobile() {
        assertTrue(PhoneUtils.isValid("9876543210"))
        assertFalse(PhoneUtils.isValid("5876543210"))
        assertFalse(PhoneUtils.isValid("98765"))
        assertFalse(PhoneUtils.isValid(""))
    }
}
