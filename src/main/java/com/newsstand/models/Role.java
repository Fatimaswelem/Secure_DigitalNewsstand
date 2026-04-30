package com.newsstand.models;

public enum Role {
    ADMIN(1),
    REGULAR(2),
    BASIC(3),
    STANDARD(4),
    PREMIUM(5);

    private final int id;

    Role(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Role fromId(int id) {
        for (Role role : values()) {
            if (role.id == id) return role;
        }
        throw new IllegalArgumentException("Unknown role id: " + id);
    }
}