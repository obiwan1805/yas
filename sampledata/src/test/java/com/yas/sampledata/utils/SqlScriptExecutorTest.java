package com.yas.sampledata.utils;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlScriptExecutorTest {

    @Test
    void executeScriptsForSchema_shouldRunSqlScriptFromClasspath() throws Exception {
        SqlScriptExecutor executor = new SqlScriptExecutor();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:sampledata_sql_executor;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        executor.executeScriptsForSchema(dataSource, "PUBLIC", "classpath*:db/test/*.sql");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                 "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TEST_TABLE'"
             );
             ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
