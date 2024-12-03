package com.example.myapplication;

import android.util.Log;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Event {
    private String key, title, place, description;
    private long date;

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

    public String getPlace() {
        return this.place;
    }
    public void setPlace(String place) {
        this.place = place;
    }

    public long getDate() {
        return this.date;
    }
    public void setDate(int d, int mth, int y, int h, int min) {
        LocalDateTime localDateTime = LocalDateTime.of(y, mth, d, h, min);
        this.date = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Log the long value
        Log.d("date", "Epoch milliseconds: " + this.date);

    }

    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }


}
