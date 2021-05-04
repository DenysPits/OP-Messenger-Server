package com.company;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    private long id;
    private long fromId;
    private long toId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long time = System.currentTimeMillis();
    private String action;
    private String body;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFromId() {
        if (fromId < 0)
            throw new RuntimeException();
        return fromId;
    }

    public void setFromId(long fromId) {
        this.fromId = fromId;
    }

    public long getToId() {
        if (toId < 0)
            throw new RuntimeException();
        return toId;
    }

    public void setToId(long toId) {
        this.toId = toId;
    }

    public long getTime() {
        if (time < 0)
            throw new RuntimeException();
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
