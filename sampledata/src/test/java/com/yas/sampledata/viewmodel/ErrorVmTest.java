package com.yas.sampledata.viewmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorVmTest {

    @Test
    void overloadedConstructor_shouldInitializeEmptyFieldErrors() {
        ErrorVm vm = new ErrorVm("400", "Bad Request", "invalid payload");

        assertEquals("400", vm.statusCode());
        assertEquals("Bad Request", vm.title());
        assertEquals("invalid payload", vm.detail());
        assertTrue(vm.fieldErrors().isEmpty());
    }
}
