package config;

import com.google.inject.Provider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DslContextProvider implements Provider<DSLContext> {
    private static final String DB_URL = "jdbc:h2:./target/generated-sources/db/revolut-backend-test-h2Db";
    private static final String DB_USER_NAME = "sa";
    private static final String DB_PASSWORD = "";

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
