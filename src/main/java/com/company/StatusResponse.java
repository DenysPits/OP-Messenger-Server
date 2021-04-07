package com.company;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class StatusResponse {
    @JsonUnwrapped
    private Status status;
    private long id;
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

    public Status getStatus() {
        return status;
    }

    public long getId() {
        return id;
    }

    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    public long getTime() {
        return time;
    }
}
