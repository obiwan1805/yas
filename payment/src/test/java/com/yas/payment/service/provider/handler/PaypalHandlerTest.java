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
    void initPayment_shouldMapRequestAndResponse() {
        InitPaymentRequestVm requestVm = InitPaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .totalPrice(new BigDecimal("120.50"))
            .checkoutId("checkout-123")
            .build();

        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("paypal-settings");
        when(paypalService.createPayment(any(PaypalCreatePaymentRequest.class)))
            .thenReturn(PaypalCreatePaymentResponse.builder()
                .status("success")
                .paymentId("payment-id")
                .redirectUrl("https://paypal.test/redirect")
                .build());

        InitiatedPayment result = paypalHandler.initPayment(requestVm);

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getPaymentId()).isEqualTo("payment-id");
        assertThat(result.getRedirectUrl()).isEqualTo("https://paypal.test/redirect");

        ArgumentCaptor<PaypalCreatePaymentRequest> requestCaptor = ArgumentCaptor.forClass(
            PaypalCreatePaymentRequest.class
        );
        verify(paypalService).createPayment(requestCaptor.capture());
        verify(paymentProviderService).getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name());

        PaypalCreatePaymentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.totalPrice()).isEqualByComparingTo(new BigDecimal("120.50"));
        assertThat(capturedRequest.checkoutId()).isEqualTo("checkout-123");
        assertThat(capturedRequest.paymentMethod()).isEqualTo(PaymentMethod.PAYPAL.name());
        assertThat(capturedRequest.paymentSettings()).isEqualTo("paypal-settings");
    }

    @Test
    void capturePayment_shouldMapRequestAndResponse() {
        CapturePaymentRequestVm requestVm = CapturePaymentRequestVm.builder()
            .paymentMethod(PaymentMethod.PAYPAL.name())
            .token("token-xyz")
            .build();

        when(paymentProviderService.getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name()))
            .thenReturn("paypal-settings");
        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class)))
            .thenReturn(PaypalCapturePaymentResponse.builder()
                .checkoutId("checkout-456")
                .amount(new BigDecimal("120.50"))
                .paymentFee(new BigDecimal("1.20"))
                .gatewayTransactionId("gateway-789")
                .paymentMethod(PaymentMethod.PAYPAL.name())
                .paymentStatus(PaymentStatus.COMPLETED.name())
                .failureMessage(null)
                .build());

        CapturedPayment result = paypalHandler.capturePayment(requestVm);

        assertThat(result.getCheckoutId()).isEqualTo("checkout-456");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("120.50"));
        assertThat(result.getPaymentFee()).isEqualByComparingTo(new BigDecimal("1.20"));
        assertThat(result.getGatewayTransactionId()).isEqualTo("gateway-789");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getFailureMessage()).isNull();

        ArgumentCaptor<PaypalCapturePaymentRequest> requestCaptor = ArgumentCaptor.forClass(
            PaypalCapturePaymentRequest.class
        );
        verify(paypalService).capturePayment(requestCaptor.capture());
        verify(paymentProviderService).getAdditionalSettingsByPaymentProviderId(PaymentMethod.PAYPAL.name());

        PaypalCapturePaymentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.token()).isEqualTo("token-xyz");
        assertThat(capturedRequest.paymentSettings()).isEqualTo("paypal-settings");
    }
}