package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class UserProfile extends AppCompatActivity {
    private FirebaseUser user;
    private FirebaseDatabase database;
    private HashSet<String> salesList;

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

        /*
        // DELETE button
        delete_btn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(UserProfile.this).inflate(R.layout.warning_dialog, null);
            EditText pw = dialogView.findViewById(R.id.currPw);
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
                salesList = new HashSet<>();
                database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");
                String password = pw.getText().toString().trim();

                // Check if title, place, and description are empty
                if (password.isEmpty()) {
                    pw.setError("Password is required");
                } else if (user != null) {
                    pw.setError(null);
                    TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

                    // Searching events organized by user
                    database.getReference().child("event").orderByChild("organizer").equalTo(user.getUid()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                            for (DataSnapshot event : eventSnapshot.getChildren()) {
                                // Search for sales related to organized events
                                searchSalesForEvents(event.getKey(), taskCompletionSource);
                            }
                            // Search for sales bought from the current user
                            searchSalesForUser(user.getUid(), taskCompletionSource);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            taskCompletionSource.setException(error.toException());
                        }
                    });

                    taskCompletionSource.getTask().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                                deleteSales(salesList, password);
                            else
                                Log.e("Delete Error", "Failed to fetch sales: " + task.getException().getMessage());
                        }
                    });
                }
            });
        });
         */
    }

    private void searchSalesForEvents(String eventId, TaskCompletionSource<Void> taskCompletionSource) {
        database.getReference().child("sale").orderByChild("eventId").equalTo(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot salesSnapshot) {
                for (DataSnapshot sale : salesSnapshot.getChildren()) {
                    salesList.add(sale.getKey());   // Add events sales to the HashSet (no duplicates)
                }
                taskCompletionSource.trySetResult(null);    // Notify completion
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                taskCompletionSource.setException(error.toException());
            }
        });
    }

    private void searchSalesForUser(String currentUser, TaskCompletionSource<Void> taskCompletionSource) {
        database.getReference().child("sale").orderByChild("userUID").equalTo(currentUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot saleSnapshot) {
                for (DataSnapshot sale : saleSnapshot.getChildren()) {
                    salesList.add(sale.getKey());      // Add user sales to the HashSet (no duplicates)
                }
                taskCompletionSource.trySetResult(null);    // Notify completion
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                taskCompletionSource.setException(error.toException());
            }
        });
    }

    private void deleteSales(HashSet<String> salesList, String pw) {
        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), pw);

        user.reauthenticate(credential).addOnSuccessListener(unused -> {
            if (salesList.isEmpty()) {
                Log.d("Delete Sales", "No sales to delete.");
            } else {

                DatabaseReference salesRef = database.getReference().child("sales");
                for (String saleKey : salesList) {
                    salesRef.child(saleKey).removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful())
                            Log.d("Delete Sales", "Sale deleted successfully: " + saleKey);
                        else
                            Log.e("Delete Sales", "Failed to delete sale: " + saleKey, task.getException());
                    });
                }
            }
            deleteUserData();
        }).addOnFailureListener(e -> Log.e("Re-authentication Failed", "Re-authentication failed: ", e));
    }

    private void deleteUserData() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("Users").document(user.getUid()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("Delete User", "User document deleted from Firestore.");

                user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Delete User", "User deleted from Firebase Authentication.");
                        Toast.makeText(UserProfile.this, "User has been deleted successfully", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(UserProfile.this, Login.class));
                        finish();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("Delete User", "Failed to delete user from Firebase Authentication.", e);
                    Toast.makeText(UserProfile.this, "ERROR: " + e, Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(e -> Log.e("Delete User", "Failed to delete user document from Firestore.", e));
    }
}