package com.pos.system.domain.model;

public class Location {
    public long id;
    public String name;
    public String address;
    public boolean isMain;
    public String createdAt;

    public Location() {}

    public Location(String name, String address, boolean isMain) {
        this.name = name;
        this.address = address;
        this.isMain = isMain;
    }
}
