package com.company.status;

import com.fasterxml.jackson.annotation.*;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Status {
    SUCCESS("success"),
    TAG_IS_TAKEN("tagIsTaken"),
    USER_DOES_NOT_EXIST("userDoesNotExist"),
    FAIL("fail");

    private final String status;

    @SuppressWarnings("unused")
    public String getStatus() {
        return status;
    }

    Status(String name) {
        this.status = name;
    }
}
