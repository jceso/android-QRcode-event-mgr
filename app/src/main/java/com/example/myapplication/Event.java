package com.example.myapplication;

public class Event {
    private String key, title, subtitle, description;

    public Event(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public Event(String key, String title, String subtitle, String description) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
    }

    public Event() { }

    public String getKey() {
        return this.key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubtitle() {
        return this.subtitle;
    }
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}
