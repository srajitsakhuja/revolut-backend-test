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
import package_.tables.records.UserRecord;

import static java.time.LocalDate.now;
import static package_.tables.User.USER;

public class UserService extends PersistenceService<User, UserRecord, UUID> {
    @Inject
    public UserService(DSLContext dslContext) throws SQLException {
        super(dslContext);
    }

    @Override
    protected void process(User user) throws PersistedEntityException, SQLException {
        UUID guardianId = user.getGuardianId();
        if (isMinor(user.getDateOfBirth())) {
            if (guardianId == null | !isValidGuardian(guardianId) ) {
                throw new PersistedEntityException("User is a minor; must have a suitable guardian!");
            }
        } else {
            if (guardianId != null) {
                throw new PersistedEntityException("You're a big boy; don't need a guardian");
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
            dslContext.update(USER)
                    .set(USER.FIRST_NAME, user.getFirstName())
                    .set(USER.LAST_NAME, user.getLastName())
                    .set(USER.DATE_OF_BIRTH, user.getDateOfBirth())
                    .set(USER.PHONE_NUMBER, user.getPhoneNumber())
                    .set(USER.IS_BLOCKED, user.isBlocked())
                    .where(USER.ID.eq(user.getId()))
                    .execute();
        } catch (DataAccessException exception) {
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
