package de.Keyle.MyPet.api.util;

import com.google.common.base.Objects;

public class KeyValue<T, U> {
    T key;
    U value;

    public KeyValue(T key, U value) {
        this.key = key;
        this.value = value;
    }

    public T getKey() {
        return key;
    }

    public U getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KeyValue<?, ?> keyValue = (KeyValue<?, ?>) o;
        return Objects.equal(key, keyValue.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }
}
