package com.company;

public class StatusIdResponse {
    private String status;
    private long id;

    public StatusIdResponse(String status, long id) {
        this.status = status;
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public long getId() {
        return id;
    }
}
