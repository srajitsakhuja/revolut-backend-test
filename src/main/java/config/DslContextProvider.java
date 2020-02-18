package config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.google.inject.Provider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

public class DslContextProvider implements Provider<DSLContext> {
    private static final String PROPERTIES_FILE = "application.properties";
    private static String DB_URL;
    private static String DB_USER_NAME;
    private static String DB_PASSWORD;

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
