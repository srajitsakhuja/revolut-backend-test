package service;

import dao.User;
import exception.PersistedEntityException;
import exception.UserException;
import org.jooq.exception.DataAccessException;
import package_.tables.records.UserRecord;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static package_.tables.User.USER;

public class UserService extends PersistenceService<User, UserRecord, UUID> {
    static final String CREATE_USER_EXCEPTION_MESSAGE = "User could not be created";
    static final String FIND_USER_EXCEPTION_MESSAGE = "User could not be found";

    @Override
    protected void process(User user) throws PersistedEntityException {
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
    public void store(User user) throws PersistedEntityException {
        try {
            super.store(user);
            dslContext.insertInto(USER).values(user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDateOfBirth(),
                    user.getPhoneNumber(),
                    user.isBlocked(),
                    user.getGuardianId()).execute();
        } catch (DataAccessException | PersistedEntityException exception) {
            throw new UserException(CREATE_USER_EXCEPTION_MESSAGE);
        }
    }

    @Override
    public UserRecord findById(UUID id) throws PersistedEntityException {
        UserRecord userRecord;
        try {
            userRecord = findById(USER, id, USER.ID);
        } catch (PersistedEntityException  exception) {
            throw new PersistedEntityException(FIND_USER_EXCEPTION_MESSAGE);
        }
        return userRecord;
    }

    private boolean isMinor(LocalDate dateOfBirth) {
        return dateOfBirth.isAfter(now().minusYears(18));
    }

    private boolean isValidGuardian(UUID guardianId) throws PersistedEntityException {
        UserRecord guardian = findById(guardianId);
        return !isMinor(guardian.getDateOfBirth()) && !guardian.getIsBlocked();
    }

    public Collection<UserRecord> find() throws PersistedEntityException {
        List<UserRecord> records;
        try {
            records = super.find(USER).stream()
                    .map(record -> record.into(UserRecord.class)).collect(Collectors.toList());
        } catch (PersistedEntityException e) {
            throw new PersistedEntityException(e.getMessage());
        }
        return records;
    }
}
