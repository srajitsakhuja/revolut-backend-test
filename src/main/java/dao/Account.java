package dao;

import package_.tables.records.AccountRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Account extends PersistedEntity{

    BigDecimal balance;
    boolean isBlocked;
    LocalDateTime creationTime;
    UUID userId;

    Account() {}

    public Account(AccountRecord record) {
        id = record.getId();
        balance = record.getBalance();
        isBlocked = record.getIsBlocked();
        creationTime = record.getCreationTime();
        userId = record.getUserId();
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public Collection<?> obtainValues() {
        return List.of(getId(), getBalance(), isBlocked(), getCreationTime(), getUserId());
    }
}
