package com.yas.sampledata.controller;

import com.yas.sampledata.service.SampleDataService;
import com.yas.sampledata.viewmodel.SampleDataVm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SampleDataControllerTest {

    @Test
    void createSampleData_shouldDelegateToService() {
        SampleDataService sampleDataService = mock(SampleDataService.class);
        when(sampleDataService.createSampleData()).thenReturn(new SampleDataVm("ok"));
        SampleDataController controller = new SampleDataController(sampleDataService);

        SampleDataVm response = controller.createSampleData(new SampleDataVm("request"));

        assertEquals("ok", response.message());
    }
}
