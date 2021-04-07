package com.company;

public class StatusResponse {
    private String status;
    private long id;
    private long time;

    public StatusResponse(String status, long id, long time) {
        this.status = status;
        this.id = id;
        this.time = time;
    }

    public StatusResponse(String status, long id) {
        this.status = status;
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public long getId() {
        return id;
    }

    public long getTime() {
        return time;
    }
}
