package com.octoblu.gateblu;

public class Device {
    private String name;
    private String type;

    public Device(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getImageName() {
        String[] parts = getType().split(":");
        return parts[parts.length - 1];
    }
}
