package service;

import dao.Account;
import exception.AccountException;
import exception.PersistedEntityException;
import exception.UserException;
import org.jooq.exception.DataAccessException;
import package_.tables.records.AccountRecord;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static package_.Tables.ACCOUNT;

public class AccountService extends PersistenceService<Account, AccountRecord, UUID> {
    private static final String CREATE_ACCOUNT_EXCEPTION_MESSAGE = "Account could not be created";
    private static final String FIND_ACCOUNT_EXCEPTION_MESSAGE = "Account could not be found";

    @Override
    protected void process(Account account) {
        account.setCreationTime(LocalDateTime.now());
    }

    @Override
    public Collection<AccountRecord> find() throws PersistedEntityException {
        List<AccountRecord> records;
        try {
            records = super.find(ACCOUNT).stream()
                    .map(record -> record.into(AccountRecord.class)).collect(Collectors.toList());
        } catch (PersistedEntityException e) {
            throw new PersistedEntityException(e.getMessage());
        }
        return records;
    }

    @Override
    public void store(Account account) throws AccountException {
        try {
            super.store(account);
            dslContext.insertInto(ACCOUNT).values(account.getId(),
                    account.getBalance(),
                    account.isBlocked(),
                    account.getCreationTime(),
                    account.getUserId()).execute();
        } catch (DataAccessException | PersistedEntityException exception) {
            throw new AccountException(CREATE_ACCOUNT_EXCEPTION_MESSAGE);
        }
    }

    @Override
    public AccountRecord findById(UUID id) throws UserException {
        AccountRecord accountRecord;
        try {
            accountRecord = findById(ACCOUNT, id, ACCOUNT.ID);
        } catch (PersistedEntityException exception) {
            throw new UserException(FIND_ACCOUNT_EXCEPTION_MESSAGE);
        }
        return accountRecord;
    }
}
