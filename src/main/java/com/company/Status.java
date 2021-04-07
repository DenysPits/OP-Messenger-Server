package com.company;

import com.fasterxml.jackson.annotation.*;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Status {
    SUCCESS("success", true),
    TAG_IS_TAKEN("tagIsTaken", false),
    USER_DOES_NOT_EXIST("userDoesNotExist", false),
    FAIL("fail", false);

    private String status;
    private boolean isSuccessful;

    public String getStatus() {
        return status;
    }

    @JsonIgnore
    public boolean isSuccessful() {
        return isSuccessful;
    }

    Status(String name, boolean isSuccessful) {
        this.status = name;
        this.isSuccessful = isSuccessful;
    }
}
