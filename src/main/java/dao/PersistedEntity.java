package dao;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class PersistedEntity {
    UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public abstract Collection<?> obtainValues();
}
