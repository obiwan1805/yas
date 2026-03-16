package com.yas.sampledata.service;

import com.yas.sampledata.viewmodel.SampleDataVm;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SampleDataServiceTest {

    @Test
    void createSampleData_shouldReturnSuccessMessageWhenScriptExecutionFails() throws SQLException {
        DataSource productDataSource = mock(DataSource.class);
        DataSource mediaDataSource = mock(DataSource.class);
        when(productDataSource.getConnection()).thenThrow(new SQLException("Cannot connect product"));
        when(mediaDataSource.getConnection()).thenThrow(new SQLException("Cannot connect media"));

        SampleDataService service = new SampleDataService(productDataSource, mediaDataSource);

        SampleDataVm vm = service.createSampleData();

        assertEquals("Insert Sample Data successfully!", vm.message());
        verify(productDataSource, atLeastOnce()).getConnection();
        verify(mediaDataSource, atLeastOnce()).getConnection();
    }
}
