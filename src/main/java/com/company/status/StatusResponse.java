package com.company.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class StatusResponse {
    @JsonUnwrapped
    private final Status status;
    private final long id;
    private long time;

    public StatusResponse(Status status, long id, long time) {
        this.status = status;
        this.id = id;
        this.time = time;
    }

    public StatusResponse(Status status, long id) {
        this.status = status;
        this.id = id;
    }

    @SuppressWarnings("unused")
    public Status getStatus() {
        return status;
    }

    @SuppressWarnings("unused")
    public long getId() {
        return id;
    }

    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    @SuppressWarnings("unused")
    public long getTime() {
        return time;
    }
}
