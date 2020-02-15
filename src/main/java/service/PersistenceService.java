package service;

import dao.PersistedEntity;
import exception.PersistedEntityException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

public abstract class PersistenceService<E extends PersistedEntity, T extends Record, I extends Object> {
    static final String DB_URL = "jdbc:h2:./revolut-backend-test-h2Db";
    static final String DB_USER_NAME = "sa";
    static final String DB_PASSWORD = "";

    protected DSLContext dslContext;

    protected void store(E entity) throws PersistedEntityException {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        try {
            process(entity);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_PASSWORD);
            dslContext = DSL.using(connection, SQLDialect.H2);
        } catch (SQLException | PersistedEntityException exception) {
            throw new PersistedEntityException(exception.getMessage());
        }
    }
    protected abstract T findById(UUID id) throws PersistedEntityException;

    protected T findById(TableImpl<T> table, I id, TableField<T, I> field) throws PersistedEntityException {
        T record;
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_PASSWORD);
            dslContext = DSL.using(connection, SQLDialect.H2);
            record = dslContext.selectFrom(table).where(field.eq(id)).fetchOne();

            if (record == null) {
                throw new PersistedEntityException("Record not found!");
            }
        } catch (SQLException e) {
            throw new PersistedEntityException(e.getMessage());
        }

        return record;
    }

    protected abstract void process(E entity) throws PersistedEntityException;
}
