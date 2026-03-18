package com.yas.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.service.PaymentService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.CapturePaymentResponseVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentResponseVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentControllerTest {

    private PaymentService paymentService;
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentService = org.mockito.Mockito.mock(PaymentService.class);
        paymentController = new PaymentController(paymentService);
    }

    @Test
    void initPayment_shouldDelegateToService() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .totalPrice(new BigDecimal("10.00"))
            .checkoutId("checkout-1")
            .build();
        InitPaymentResponseVm responseVm = InitPaymentResponseVm.builder()
            .status("success")
            .paymentId("id-1")
            .redirectUrl("http://redirect")
            .build();
        when(paymentService.initPayment(requestVm)).thenReturn(responseVm);

        InitPaymentResponseVm result = paymentController.initPayment(requestVm);

        assertThat(result).isEqualTo(responseVm);
        verify(paymentService).initPayment(requestVm);
    }

    @Test
    void capturePayment_shouldDelegateToService() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("token-1")
            .build();
        CapturePaymentResponseVm responseVm = CapturePaymentResponseVm.builder()
            .orderId(10L)
            .checkoutId("checkout-1")
            .amount(new BigDecimal("10.00"))
            .paymentFee(new BigDecimal("1.00"))
            .gatewayTransactionId("txn-1")
            .paymentMethod(PaymentMethod.PAYPAL)
            .paymentStatus(PaymentStatus.COMPLETED)
            .failureMessage(null)
            .build();
        when(paymentService.capturePayment(requestVm)).thenReturn(responseVm);

        CapturePaymentResponseVm result = paymentController.capturePayment(requestVm);

        assertThat(result).isEqualTo(responseVm);
        verify(paymentService).capturePayment(requestVm);
    }

    @Test
    void cancelPayment_shouldReturnOkMessage() {
        var result = paymentController.cancelPayment();

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).isEqualTo("Payment cancelled");
    }
}
