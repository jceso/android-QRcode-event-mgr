package com.example.myapplication;

import android.util.Log;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class Event {
    private String key, title, place, description, organizer;
    private long date;

    private float price;
    private int max_seats, num_subs;

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

    public float getPrice() {
        return this.price;
    }
    public void setPrice(float price) {
        this.price = price;
    }

    public int getSeats() {
        return this.max_seats;
    }
    public void setSeats(int max_seats) {
        this.max_seats = max_seats;
    }

    public int getNum_subs() {
        return num_subs;
    }
    public void setNum_subs(int num_subs) {
        this.num_subs = num_subs;
    }
}
