package com.yas.delivery;

import com.yas.delivery.controller.DeliveryController;
import com.yas.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeliveryComponentsTest {

    @Test
    void shouldInstantiateDeliveryController() {
        DeliveryController controller = new DeliveryController();

        assertNotNull(controller);
    }

    @Test
    void shouldInstantiateDeliveryService() {
        DeliveryService service = new DeliveryService();

        assertNotNull(service);
    }
}
