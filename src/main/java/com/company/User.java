package com.company;

public class User {
    private long id;
    private String name;
    private String tag;
    private String photo;
    private String publicRsa;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPublicRsa() {
        return publicRsa;
    }

    public void setPublicRsa(String publicRsa) {
        this.publicRsa = publicRsa;
    }
}
