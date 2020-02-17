package dao;

import java.math.BigDecimal;
import java.util.UUID;

public class Deposit {
    UUID accountId;
    BigDecimal amount;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
