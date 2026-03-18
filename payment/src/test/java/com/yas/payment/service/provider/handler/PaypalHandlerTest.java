package com.yas.payment.service.provider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.InitiatedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCreatePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import com.yas.payment.viewmodel.InitPaymentRequestVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaypalHandlerTest {

    private PaymentProviderService paymentProviderService;
    private PaypalService paypalService;
    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        paymentProviderService = org.mockito.Mockito.mock(PaymentProviderService.class);
        paypalService = org.mockito.Mockito.mock(PaypalService.class);
        paypalHandler = new PaypalHandler(paymentProviderService, paypalService);
    }

    @Test
    void getProviderId_shouldReturnPaypal() {
        assertThat(paypalHandler.getProviderId()).isEqualTo(PaymentMethod.PAYPAL.name());
    }

    @Test
    void initPayment_shouldMapRequestAndResponse() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .checkoutId("checkout-1")
            .totalPrice(new BigDecimal("99.50"))
            .build();
        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("clientId=abc");
        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class)))
            .thenReturn(PaypalCreatePaymentResponse.builder()
                .status("success")
                .paymentId("paypal-payment-id")
                .redirectUrl("https://paypal/approve")
                .build());

        InitiatedPayment result = paypalHandler.initPayment(requestVm);

        ArgumentCaptor<PaypalCreatePaymentRequest> requestCaptor =
            ArgumentCaptor.forClass(PaypalCreatePaymentRequest.class);
        verify(paypalService).createPayment(requestCaptor.capture());
        PaypalCreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.totalPrice()).isEqualByComparingTo(new BigDecimal("99.50"));
        assertThat(capturedRequest.checkoutId()).isEqualTo("checkout-1");
        assertThat(capturedRequest.paymentMethod()).isEqualTo(PaymentMethod.PAYPAL.name());
        assertThat(capturedRequest.paymentSettings()).isEqualTo("clientId=abc");

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getPaymentId()).isEqualTo("paypal-payment-id");
        assertThat(result.getRedirectUrl()).isEqualTo("https://paypal/approve");
    }

    @Test
    void capturePayment_shouldMapRequestAndEnumResponse() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("paypal-token")
            .build();
        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("clientId=abc");
        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class)))
            .thenReturn(PaypalCapturePaymentResponse.builder()
                .checkoutId("checkout-1")
                .amount(new BigDecimal("50.00"))
                .paymentFee(new BigDecimal("2.00"))
                .gatewayTransactionId("tx-1")
                .paymentMethod(PaymentMethod.PAYPAL.name())
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .failureMessage(null)
                .build());

        CapturedPayment result = paypalHandler.capturePayment(requestVm);

        ArgumentCaptor<PaypalCapturePaymentRequest> requestCaptor =
            ArgumentCaptor.forClass(PaypalCapturePaymentRequest.class);
        verify(paypalService).capturePayment(requestCaptor.capture());
        PaypalCapturePaymentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.token()).isEqualTo("paypal-token");
        assertThat(capturedRequest.paymentSettings()).isEqualTo("clientId=abc");

        assertThat(result.getCheckoutId()).isEqualTo("checkout-1");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getPaymentFee()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(result.getGatewayTransactionId()).isEqualTo("tx-1");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getFailureMessage()).isNull();
    }
}
