package service;

import com.google.inject.Inject;
import dao.Account;
import dao.Deposit;
import dao.PersistedEntity;
import dao.Transfer;
import exception.PersistedEntityException;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import package_.tables.records.AccountRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static package_.Tables.ACCOUNT;

public class AccountService extends PersistenceService<Account, AccountRecord, UUID> {
    private final Logger logger;

    @Inject
    public AccountService(DSLContext dslContext) throws SQLException {
        super(dslContext);
        logger = LoggerFactory.getLogger(this.getClass());
    }

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
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
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
            AccountRecord accountRecord = dslContext.fetchOne(ACCOUNT, ACCOUNT.ID.eq(account.getId()));
            if (accountRecord == null) {
                throw new PersistedEntityException("Record not found!");
            }
            accountRecord.setUserId(account.getUserId());
            accountRecord.store();
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
        }
    }

    public void transferFunds(Transfer transfer) {
        dslContext.transaction(configuration ->
        {
            try {
                AccountRecord fromAccount = dslContext.fetchOne(ACCOUNT, ACCOUNT.ID.eq(transfer.getFromAccountId()));
                BigDecimal fromAccountBalance = fromAccount.getBalance().subtract(transfer.getAmount());
                if (fromAccountBalance.compareTo(new BigDecimal(3000)) < 0) {
                    return;
                }
                AccountRecord toAccount = dslContext.fetchOne(ACCOUNT, ACCOUNT.ID.eq(transfer.getToAccountId()));
                BigDecimal toAccountBalance = toAccount.getBalance().add(transfer.getAmount());

                if (fromAccount.getIsBlocked() | toAccount.getIsBlocked()) {
                    return;
                }
                fromAccount.setBalance(fromAccountBalance);
                fromAccount.store();
                toAccount.setBalance(toAccountBalance);
                toAccount.store();
            } catch (DataAccessException exception) {
                throw new PersistedEntityException(exception.getMessage());
            }
        });
    }

    public void depositFunds(Deposit deposit) throws PersistedEntityException {
        try {
            AccountRecord account = dslContext.fetchOne(ACCOUNT, ACCOUNT.ID.eq(deposit.getAccountId()));
            if (account.getIsBlocked()) {
                throw new PersistedEntityException("Can not deposit funds to a blocked account!");
            }
            BigDecimal balance = account.getBalance().add(deposit.getAmount());
            account.setBalance(balance);
            account.store();
        } catch (DataAccessException e) {
            throw new PersistedEntityException(e.getMessage());
        }
    }
}
