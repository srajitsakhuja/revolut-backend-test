package service;

import dao.PersistedEntity;
import exception.PersistedEntityException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Results;
import org.jooq.SQLDialect;
import org.jooq.TableField;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class PersistenceService<E extends PersistedEntity, T extends Record, I extends Object> {
    static final String DB_URL = "jdbc:h2:./revolut-backend-test-h2Db";
    static final String DB_USER_NAME = "sa";
    static final String DB_PASSWORD = "";

    protected DSLContext dslContext;

    protected void configureDslContext() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_PASSWORD);
        dslContext = DSL.using(connection, SQLDialect.H2, new Settings().withExecuteWithOptimisticLocking(true));
    }

    protected void store(E entity) throws PersistedEntityException, SQLException {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }

        process(entity);
        configureDslContext();
    }
    protected abstract T findById(UUID id) throws PersistedEntityException, SQLException;

    protected T findById(TableImpl<T> table, I id, TableField<T, I> field) throws PersistedEntityException, SQLException {
        T record;

        configureDslContext();
        try {
            record = dslContext.selectFrom(table).where(field.eq(id)).fetchOne();
        } catch (DataAccessException e) {
            throw new PersistedEntityException("Record not found!");
        }

        if (record == null) {
            throw new PersistedEntityException("Record not found!");
        }

        return record;
    }

    protected abstract void process(E entity) throws PersistedEntityException, SQLException;

    protected abstract Collection<T> find() throws PersistedEntityException, SQLException;

    public Collection<Record> find(TableImpl<T> table) throws PersistedEntityException, SQLException {
        List<Record> records = new ArrayList<>();
        configureDslContext();
        try {
            Results results = dslContext.selectFrom(table).fetchMany();
            for (Result<Record> result :results) {
                records.addAll(result);
            }
        } catch (DataAccessException e) {
            throw new PersistedEntityException(e.getMessage());
        }

        return records;
    }

    protected void update(E entity) throws PersistedEntityException, SQLException {
        if (entity.getId() == null) {
            throw new PersistedEntityException("Resource does not exist!");
        }

        configureDslContext();
    }
}
