package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;

public class UserProfile extends AppCompatActivity {
    private FirebaseUser user;
    private FirebaseDatabase database;
    private ArrayList<String> salesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(UserProfile.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(UserProfile.this, name_btn);
        ImageButton backButton = findViewById(R.id.back);
        BasicButtons.handleBackButton(UserProfile.this, backButton);

        showDetails();
        btnSetting();
    }

    private void showDetails() {
        user = FirebaseAuth.getInstance().getCurrentUser();

        TextView username = findViewById(R.id.username);
        TextView email = findViewById(R.id.email);
        TextView phone = findViewById(R.id.phone);
        TextView isOrganizer = findViewById(R.id.organizer);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get infos from the FireStore document
                        username.setText(documentSnapshot.getString("UserName"));
                        email.setText(documentSnapshot.getString("UserEmail"));
                        phone.setText(documentSnapshot.getString("PhoneNumber"));
                        if (documentSnapshot.getString("isAdmin") != null && Objects.equals(documentSnapshot.getString("isAdmin"), "1"))
                            isOrganizer.setVisibility(View.VISIBLE);
                        else
                            isOrganizer.setVisibility(View.GONE);
                    } else
                        Log.d("User Info", "No such document");
                });
    }

    private void btnSetting() {
        Button edit_btn = findViewById(R.id.edit_btn);
        Button delete_btn = findViewById(R.id.delete_btn);

        // EDIT button
        edit_btn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), EditProfile.class));
            finish();
        });

        // DELETE button
        delete_btn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(UserProfile.this).inflate(R.layout.warning_dialog, null);
            Button delete = dialogView.findViewById(R.id.delete_btn);
            Button cancel = dialogView.findViewById(R.id.cancel_btn);

            AlertDialog alertDialog = new AlertDialog.Builder(UserProfile.this)
                    .setView(dialogView)
                    .create();
            alertDialog.show();

            // Cancel button
            cancel.setOnClickListener(v1 -> alertDialog.dismiss());

            // Delete confirmation button
            delete.setOnClickListener(v2 -> {
                salesList = new ArrayList<>();
                database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");

                if (user != null) {
                    String currentUserId = user.getUid();

                    // Searching events organized by user
                    database.getReference().child("event").orderByChild("organizer").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                            for (DataSnapshot event : eventSnapshot.getChildren()) {
                                String eventId = event.getKey(); // Event ID is the key of the event entry

                                // Search for sales related to this event
                                searchSalesForEvent(eventId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            });
        });
    }

    private void searchSalesForEvent(String eventId) {
        database.getReference().child("sales").orderByChild("eventId").equalTo(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot salesSnapshot) {
                for (DataSnapshot sale : salesSnapshot.getChildren()) {
                    // Add each sale to the salesList instead of removing it immediately
                    String saleId = sale.getKey();
                    salesList.add(saleId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Delete Error", "Failed to fetch sales: ", databaseError.toException());
            }
        });
    }
}