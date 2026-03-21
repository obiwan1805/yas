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
import org.springframework.data.domain.PageRequest;
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
        requestVm.setName("Paypal");
        requestVm.setConfigureUrl("/config");

        PaymentProviderVm expected = new PaymentProviderVm(
            "PAYPAL",
            "Paypal",
            "/config",
            1,
            100L,
            "http://icon"
        );
        when(paymentProviderService.create(requestVm)).thenReturn(expected);

        var response = paymentProviderController.create(requestVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(paymentProviderService).create(requestVm);
    }

    @Test
    void update_shouldReturnOkResponse() {
        UpdatePaymentVm requestVm = new UpdatePaymentVm();
        requestVm.setId("COD");
        requestVm.setName("Cash");
        requestVm.setConfigureUrl("/config-cod");

        PaymentProviderVm expected = new PaymentProviderVm(
            "COD",
            "Cash",
            "/config-cod",
            2,
            101L,
            "http://icon-cod"
        );
        when(paymentProviderService.update(requestVm)).thenReturn(expected);

        var response = paymentProviderController.update(requestVm);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(paymentProviderService).update(requestVm);
    }

    @Test
    void getAll_shouldReturnEnabledProviders() {
        Pageable pageable = PageRequest.of(0, 20);
        List<PaymentProviderVm> providers = List.of(
            new PaymentProviderVm("PAYPAL", "Paypal", "/config", 1, 100L, "http://icon")
        );
        when(paymentProviderService.getEnabledPaymentProviders(pageable)).thenReturn(providers);

        var response = paymentProviderController.getAll(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(providers);
        verify(paymentProviderService).getEnabledPaymentProviders(pageable);
    }
}
