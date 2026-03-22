package com.yas.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.paymentprovider.CreatePaymentVm;
import com.yas.payment.viewmodel.paymentprovider.PaymentProviderVm;
import com.yas.payment.viewmodel.paymentprovider.UpdatePaymentVm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

class PaymentProviderControllerTest {

    private PaymentProviderService paymentProviderService;
    private PaymentProviderController paymentProviderController;

    @BeforeEach
    void setUp() {
        paymentProviderService = org.mockito.Mockito.mock(PaymentProviderService.class);
        paymentProviderController = new PaymentProviderController(paymentProviderService);
    }

    @Test
    void create_shouldReturnCreatedResponse() {
        CreatePaymentVm requestVm = new CreatePaymentVm();
        requestVm.setId("PAYPAL");
        PaymentProviderVm responseVm = new PaymentProviderVm("PAYPAL", "Paypal", "/config", 1, 10L, "icon");
        when(paymentProviderService.create(requestVm)).thenReturn(responseVm);

        var result = paymentProviderController.create(requestVm);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(responseVm);
        verify(paymentProviderService).create(requestVm);
    }

    @Test
    void update_shouldReturnOkResponse() {
        UpdatePaymentVm requestVm = new UpdatePaymentVm();
        requestVm.setId("PAYPAL");
        PaymentProviderVm responseVm = new PaymentProviderVm("PAYPAL", "Paypal", "/config", 2, 11L, "icon2");
        when(paymentProviderService.update(requestVm)).thenReturn(responseVm);

        var result = paymentProviderController.update(requestVm);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responseVm);
        verify(paymentProviderService).update(requestVm);
    }

    @Test
    void getAll_shouldReturnEnabledProviders() {
        Pageable pageable = Pageable.ofSize(10);
        List<PaymentProviderVm> providers = List.of(
            new PaymentProviderVm("PAYPAL", "Paypal", "/config", 1, 10L, "icon")
        );
        when(paymentProviderService.getEnabledPaymentProviders(pageable)).thenReturn(providers);

        var result = paymentProviderController.getAll(pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(providers);
        verify(paymentProviderService).getEnabledPaymentProviders(pageable);
    }
}
