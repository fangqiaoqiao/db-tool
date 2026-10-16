package com.dbtools.model;

public class DatabaseView {

    private final String id;
    private final String label;

    public DatabaseView(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
