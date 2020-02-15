package dao;

import package_.tables.records.UserRecord;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class User extends PersistedEntity {
    String firstName;
    String lastName;
    LocalDate dateOfBirth;
    String phoneNumber;
    boolean isBlocked;
    UUID guardianId;

    public User() { }

    public User(UserRecord record) {
        setId(record.getId());
        setFirstName(record.getFirstName());
        setLastName(record.getLastName());
        setDateOfBirth(record.getDateOfBirth());
        setPhoneNumber(record.getPhoneNumber());
        setGuardianId(record.getGuardianId());
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public UUID getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(UUID guardianId) {
        this.guardianId = guardianId;
    }

    @Override
    public String toString() {
        return String.format("id %s, firstName: %s, lastName: %s, dob:%s, phoneNumber: %s, isBlocked: %s, guardianId:%s",
                id, firstName, lastName, dateOfBirth, phoneNumber, isBlocked, guardianId);
    }

    @Override
    public Collection<?> obtainValues() {
        return List.of(getId(), getFirstName(), getLastName(), getDateOfBirth(), getPhoneNumber(), isBlocked(), getGuardianId());
    }
}
