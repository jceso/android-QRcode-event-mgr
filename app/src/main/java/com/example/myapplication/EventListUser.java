package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Set;

public class EventListUser extends AppCompatActivity {
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_list_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting
        FirebaseApp.initializeApp(EventListUser.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(EventListUser.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(EventListUser.this, name_btn);
        ImageButton backButton = findViewById(R.id.back);
        BasicButtons.handleBackButton(EventListUser.this, backButton);

        FloatingActionButton scanButton = findViewById(R.id.scanButton);
        FloatingActionButton adminButton = findViewById(R.id.adminButton);
        Button cartButton = findViewById(R.id.cartButton);

        scanButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), QrScanner.class));
            finish();
        });

        cartButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ShoppingCart.class));
            finish();
        });

        checkIfUserIsAdmin(adminButton);
        showEvents();
    }

    private void showEvents() {
        // Setting recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler);
        TextView empty = findViewById(R.id.empty_list);

        database.getReference().child("event").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Retrieve saved event keys from Local Storage
                ArrayList<Event> arrayList = new ArrayList<>();
                Set<String> savedEventKeys = EventFileManager.loadEvents(EventListUser.this);
                savedEventKeys.remove(null);
                Log.d("EventBooking", "Saved Event Keys: " + savedEventKeys);

                // Filter events based on saved keys
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    Log.d("EventListUser", "Event Key: " + dataSnapshot.getKey());
                    if (event != null && savedEventKeys.contains(dataSnapshot.getKey())) {
                        event.setKey(dataSnapshot.getKey());
                        arrayList.add(event);
                    }
                }
                if (arrayList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    empty.setVisibility(View.GONE);
                }

                EventAdapter adapter = new EventAdapter(EventListUser.this, arrayList, 2);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkIfUserIsAdmin(FloatingActionButton adminButton) {
        // Get current user UID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If the user is authenticated, get user info
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("Users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("User Info", "Document exists " + documentSnapshot);
                        String isAdmin = documentSnapshot.getString("isAdmin");
                        Log.d("User Info", "isAdmin: " + isAdmin);
                        if ("1".equals(isAdmin)) {
                            adminButton.setVisibility(View.VISIBLE);    // Make the admin button visible

                            adminButton.setOnClickListener(v -> {
                                startActivity(new Intent(getApplicationContext(), EventListAdmin.class));
                                finish();
                            });
                        }
                        else
                            adminButton.setVisibility(View.GONE);   // Hide the button
                    } else
                        Log.d("User Info", "No such document");
            });
        } else {
            adminButton.setVisibility(View.GONE);
        }
    }
}