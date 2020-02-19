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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PersistenceService<E extends PersistedEntity, T extends Record, I extends Object> {
    protected DSLContext dslContext;
    private final Logger logger;

    @Inject
    PersistenceService(DSLContext dslContext) {
        logger = LoggerFactory.getLogger(PersistenceService.class);
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
        String exceptionMessage = "Resource does not exist!";
        try {
            record = dslContext.selectFrom(table).where(field.eq(id)).fetchOne();
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
        }

        if (record == null) {
            logger.error(exceptionMessage);
            throw new PersistedEntityException(exceptionMessage);
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
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
        }

        return records;
    }

    protected void update(E entity) throws PersistedEntityException, SQLException {
        String exceptionMessage;
        if (entity.getId() == null) {
            exceptionMessage = "Resource does not exist!";
            logger.error(exceptionMessage);
            throw new PersistedEntityException(exceptionMessage);
        }
    }
}
