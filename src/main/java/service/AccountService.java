package service;

import dao.Account;
import exception.PersistedEntityException;
import org.jooq.exception.DataAccessException;
import package_.tables.records.AccountRecord;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static package_.Tables.ACCOUNT;

public class AccountService extends PersistenceService<Account, AccountRecord, UUID> {
    @Override
    protected void process(Account account) {
        account.setCreationTime(LocalDateTime.now());
    }

    @Override
    public Collection<AccountRecord> find() throws PersistedEntityException, SQLException {
        return super.find(ACCOUNT).stream()
                .map(record -> record.into(AccountRecord.class))
                .collect(Collectors.toList());
    }

    @Override
    public void store(Account account) throws PersistedEntityException, SQLException {
        super.store(account);
        try {
            dslContext.insertInto(ACCOUNT).values(account.getId(),
                    account.getBalance(),
                    account.isBlocked(),
                    account.getCreationTime(),
                    account.getUserId()).execute();
        } catch (DataAccessException e) {
            throw new PersistedEntityException(e.getMessage());
        }
    }

    @Override
    public AccountRecord findById(UUID id) throws PersistedEntityException, SQLException {
        return findById(ACCOUNT, id, ACCOUNT.ID);
    }

    @Override
    public void update(Account account) throws PersistedEntityException, SQLException {
        super.update(account);
        try {
            dslContext.update(ACCOUNT)
                    .set(ACCOUNT.IS_BLOCKED, account.isBlocked())
                    .set(ACCOUNT.USER_ID, account.getUserId())
                    .where(ACCOUNT.ID.eq(account.getId()))
                    .execute();
        } catch (DataAccessException exception) {
            throw new PersistedEntityException(exception.getMessage());
        }
    }
}
