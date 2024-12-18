package com.example.myapplication;

public class Sale {
    private String eventUID, userUID, time;

    public Sale() { }

    public String getEventUID() {
        return this.eventUID;
    }
    public void setEventUID(String eventUID) {
        this.eventUID = eventUID;
    }

    public String getUserUID() {
        return this.userUID;
    }
    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    public String getTime() {
        return this.time;
    }
    public void setTime(String time) {
        this.time = time;
    }
}
