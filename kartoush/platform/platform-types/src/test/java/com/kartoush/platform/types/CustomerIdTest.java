package com.kartoush.platform.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kartoush.platform.ulid.InvalidUlidException;
import com.kartoush.platform.ulid.UlidGenerator;
import org.junit.jupiter.api.Test;

class CustomerIdTest {

    @Test
    void generatesUlid() {
        UlidGenerator generator = () -> "01ARZ3NDEKTSV4RRFFQ69G5FAV";

        CustomerId id = CustomerId.newId(generator);

        assertNotNull(id.value());
        assertEquals(26, id.value().length());
        assertEquals("01ARZ3NDEKTSV4RRFFQ69G5FAV", id.value());
    }

    @Test
    void ofNormalizesToUppercase() {
        CustomerId id = CustomerId.of("01arz3ndektsv4rrffq69g5fav");
        assertEquals("01ARZ3NDEKTSV4RRFFQ69G5FAV", id.value());
    }

    @Test
    void ofRejectsInvalidLength() {
        assertThrows(InvalidUlidException.class, () -> CustomerId.of("01ARZ3NDEKTSV4RRFFQ69G5FA"));
    }

    @Test
    void ofRejectsInvalidCharacters() {
        assertThrows(InvalidUlidException.class, () -> CustomerId.of("01ARZ3NDEKTSV4RRIFFQ69G5FAV"));
    }

    @Test
    void ofRejectsFirstCharOutOfRange() {
        assertThrows(InvalidUlidException.class, () -> CustomerId.of("Z1ARZ3NDEKTSV4RRFFQ69G5FAV"));
    }
}
