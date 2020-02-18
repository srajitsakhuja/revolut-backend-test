package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class).in(Singleton.class);
        bind(DSLContext.class).toProvider(DslContextProvider.class);
    }

    private static class DslContextProvider implements Provider<DSLContext> {

        static final String DB_URL = "jdbc:h2:./target/generated-test-sources/db/test-revolut-backend-test-h2Db";
        static final String DB_USER_NAME = "sa";
        static final String DB_PASSWORD = "";

        @Override
        public DSLContext get() {
            Connection connection = null;
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_PASSWORD);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return DSL.using(connection, SQLDialect.H2, new Settings().withExecuteWithOptimisticLocking(true));
        }
    }
}
