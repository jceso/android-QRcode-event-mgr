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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class UserProfile extends AppCompatActivity {

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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        TextView username = findViewById(R.id.username);
        TextView email = findViewById(R.id.email);
        TextView phone = findViewById(R.id.phone);
        TextView password = findViewById(R.id.password);
        TextView isOrganizer = findViewById(R.id.organizer);

        if (user == null) {
            // If NOT authenticated, navigate to Login
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        } else {
            // If user is authenticated, get user info
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
    }

    private void btnSetting() {
        Button edit_btn = findViewById(R.id.edit_btn);
        Button delete_btn = findViewById(R.id.delete_btn);

        edit_btn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), EditProfile.class));
            finish();
        });

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
                // Search and delete all occurrences of the user
            });
        });

    }
}