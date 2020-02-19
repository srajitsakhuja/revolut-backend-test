package service;

import com.google.inject.Inject;
import dao.Account;
import dao.Deposit;
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
            dslContext.update(ACCOUNT)
                    .set(ACCOUNT.IS_BLOCKED, account.isBlocked())
                    .set(ACCOUNT.USER_ID, account.getUserId())
                    .where(ACCOUNT.ID.eq(account.getId()))
                    .execute();
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
        }
    }

    public void transferFunds(Transfer transfer) throws SQLException, PersistedEntityException {
        AccountRecord fromAccount = findById(transfer.getFromAccountId());
        AccountRecord toAccount = findById(transfer.getToAccountId());

        BigDecimal fromAccountBalance = fromAccount.getBalance().subtract(transfer.getAmount());
        BigDecimal toAccountBalance = toAccount.getBalance().add(transfer.getAmount());

        if (fromAccountBalance.compareTo(new BigDecimal(3000)) < 0) {
            throw new PersistedEntityException("Insufficient Balance");
        }

        dslContext.transaction(configuration ->
        {
            try {
                dslContext.update(ACCOUNT)
                        .set(ACCOUNT.BALANCE, fromAccountBalance)
                        .where(ACCOUNT.ID.eq(fromAccount.getId()))
                        .execute();

                dslContext.update(ACCOUNT)
                        .set(ACCOUNT.BALANCE, toAccountBalance)
                        .where(ACCOUNT.ID.eq(toAccount.getId()))
                        .execute();
            } catch (DataAccessException e) {
                throw new PersistedEntityException(e.getMessage());
            }
        });

    }

    public void depositFunds(Deposit deposit) throws SQLException, PersistedEntityException {
        AccountRecord account = findById(deposit.getAccountId());

        try {
            dslContext.update(ACCOUNT)
                    .set(ACCOUNT.BALANCE, account.getBalance().add(deposit.getAmount()))
                    .where(ACCOUNT.ID.eq(deposit.getAccountId()))
                    .execute();
        } catch (DataAccessException e) {
            throw new PersistedEntityException(e.getMessage());
        }
    }
}
