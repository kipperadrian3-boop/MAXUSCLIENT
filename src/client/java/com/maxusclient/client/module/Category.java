package com.maxusclient.client.module;
public enum Category {
    ESP("ESP"),
    PLACEHOLDER1("PlaceHolder1"),
    PLACEHOLDER2("Placeholder2"),
    PLACEHOLDER3("Placeholder3"),
    PLACEHOLDER4("Placeholder4");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
