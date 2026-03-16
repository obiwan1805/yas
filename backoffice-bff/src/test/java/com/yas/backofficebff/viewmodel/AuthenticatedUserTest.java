package com.yas.backofficebff.viewmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticatedUserTest {

    @Test
    void username_shouldMatchConstructorValue() {
        AuthenticatedUser user = new AuthenticatedUser("backoffice-admin");

        assertEquals("backoffice-admin", user.username());
    }
}
