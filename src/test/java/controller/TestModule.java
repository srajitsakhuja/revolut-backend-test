package controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import config.ObjectMapperProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

public class TestModule extends AbstractModule {
    private static final String PROPERTIES_FILE = "test.properties";
    @Override
    protected void configure() {
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class).in(Singleton.class);
        bind(DSLContext.class).toProvider(DslContextProvider.class);
    }

    private static class DslContextProvider implements Provider<DSLContext> {
        static String DB_URL;
        static String DB_USER_NAME;
        static String DB_PASSWORD;

        @Override
        public DSLContext get() {
            readProperties();
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_PASSWORD);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return DSL.using(connection, SQLDialect.H2, new Settings().withExecuteWithOptimisticLocking(true));
        }

        private void readProperties() {
            try {
                Properties properties = new Properties();
                properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
                DB_URL = properties.getProperty("DB_URL");
                DB_USER_NAME = properties.getProperty("DB_USER_NAME");
                DB_PASSWORD = properties.getProperty("DB_PASSWORD");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
