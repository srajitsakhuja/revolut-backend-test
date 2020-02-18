package service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import dao.PersistedEntity;
import exception.PersistedEntityException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Results;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.TableImpl;

public abstract class PersistenceService<E extends PersistedEntity, T extends Record, I extends Object> {
    protected DSLContext dslContext;

    @Inject
    PersistenceService(DSLContext dslContext) throws SQLException {
        this.dslContext = dslContext;
    }

    protected void store(E entity) throws PersistedEntityException, SQLException {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        process(entity);
    }
    protected abstract T findById(UUID id) throws PersistedEntityException, SQLException;

    protected T findById(TableImpl<T> table, I id, TableField<T, I> field) throws PersistedEntityException, SQLException {
        T record;

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
    }
}
