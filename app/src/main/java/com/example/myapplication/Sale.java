package com.example.myapplication;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class Sale {
    private String eventUID, userUID;
    private int seat;
    private long time;

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
    public void getName(OnNameFetchedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query the Users collection by userUID to get the UserName
        db.collection("Users").document(this.userUID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String userName = document.getString("UserName");
                    listener.onNameFetched(userName);  // Callback to return the name
                } else
                    listener.onNameFetched(null);
            } else
                listener.onNameFetched(null);
        });
    }

    // Define a listener interface to get the result asynchronously
    public interface OnNameFetchedListener {
        void onNameFetched(String name);
    }

    public int getSeat() {
        return this.seat;
    }
    public void setSeat(int seat) {
        this.seat = seat;
    }

    public long getTime() {
        return this.time;
    }
    public void setTime(LocalDateTime localDateTime) {
        this.time = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
