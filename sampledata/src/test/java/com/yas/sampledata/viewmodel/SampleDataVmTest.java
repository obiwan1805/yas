package com.yas.sampledata.viewmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SampleDataVmTest {

    @Test
    void message_shouldMatchInput() {
        SampleDataVm vm = new SampleDataVm("done");

        assertEquals("done", vm.message());
    }
}
