package com.yas.payment.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesUtilsTest {

    @Test
    void getMessage_shouldFormatDefinedMessage() {
        String message = MessagesUtils.getMessage(Constants.ErrorCode.PAYMENT_PROVIDER_NOT_FOUND, "PAYPAL");

        assertThat(message).isEqualTo("Payment provider PAYPAL is not found");
    }

    @Test
    void getMessage_shouldReturnErrorCodeWhenMessageIsMissing() {
        String message = MessagesUtils.getMessage("UNKNOWN_ERROR_CODE");

        assertThat(message).isEqualTo("UNKNOWN_ERROR_CODE");
    }

    @Test
    void constants_shouldExposeExpectedValues() {
        assertThat(Constants.ErrorCode.PAYMENT_PROVIDER_NOT_FOUND).isEqualTo("PAYMENT_PROVIDER_NOT_FOUND");
        assertThat(Constants.Message.SUCCESS_MESSAGE).isEqualTo("SUCCESS");
    }
}
