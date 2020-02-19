package service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import dao.User;
import exception.PersistedEntityException;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import package_.tables.records.UserRecord;

import static java.time.LocalDate.now;
import static package_.tables.User.USER;

public class UserService extends PersistenceService<User, UserRecord, UUID> {
    private final Logger logger;

    @Inject
    public UserService(DSLContext dslContext) throws SQLException {
        super(dslContext);
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    protected void process(User user) throws PersistedEntityException, SQLException {
        UUID guardianId = user.getGuardianId();
        String exceptionMessage;
        if (isMinor(user.getDateOfBirth())) {
            if (guardianId == null | !isValidGuardian(guardianId) ) {
                exceptionMessage = "User is a minor; must have a suitable guardian!";
                logger.error(exceptionMessage);
                throw new PersistedEntityException(exceptionMessage);
            }
        } else {
            if (guardianId != null) {
                exceptionMessage = "You're a big boy; don't need a guardian";
                logger.error(exceptionMessage);
                throw new PersistedEntityException(exceptionMessage);
            }
        }
    }

    @Override
    public void store(User user) throws PersistedEntityException, SQLException {
        super.store(user);
        try {
            dslContext.insertInto(USER).values(user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDateOfBirth(),
                    user.getPhoneNumber(),
                    user.isBlocked(),
                    user.getGuardianId()).execute();
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
        }
    }

    @Override
    public UserRecord findById(UUID id) throws PersistedEntityException, SQLException {
        return findById(USER, id, USER.ID);
    }

    @Override
    public Collection<UserRecord> find() throws PersistedEntityException, SQLException {
        return super.find(USER).stream()
                    .map(record -> record.into(UserRecord.class)).collect(Collectors.toList());
    }

    @Override
    public void update(User user) throws PersistedEntityException, SQLException {
        super.update(user);

        try {
            UserRecord userRecord = dslContext.fetchOne(USER, USER.ID.eq(user.getId()));
            if (userRecord == null) {
                throw new PersistedEntityException("Record not found!");
            }
            userRecord.setFirstName(user.getFirstName() != null ? user.getFirstName() : userRecord.getFirstName());
            userRecord.setLastName(user.getLastName() != null ? user.getLastName() : userRecord.getLastName());
            userRecord.setDateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth() : userRecord.getDateOfBirth());
            userRecord.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : userRecord.getPhoneNumber());
            userRecord.store();
        } catch (DataAccessException exception) {
            logger.error(exception.getMessage());
            throw new PersistedEntityException(exception.getMessage());
        }
    }

    private boolean isMinor(LocalDate dateOfBirth) {
        return dateOfBirth.isAfter(now().minusYears(18));
    }

    private boolean isValidGuardian(UUID guardianId) throws PersistedEntityException, SQLException {
        UserRecord guardian = findById(guardianId);
        return !isMinor(guardian.getDateOfBirth()) && !guardian.getIsBlocked();
    }
}
