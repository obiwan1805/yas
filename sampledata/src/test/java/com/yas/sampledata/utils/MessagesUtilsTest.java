package com.yas.sampledata.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagesUtilsTest {

    @Test
    void getMessage_whenMessageCodeMissing_shouldReturnCodeItself() {
        String message = MessagesUtils.getMessage("sampledata.missing.code");

        assertEquals("sampledata.missing.code", message);
    }
}
