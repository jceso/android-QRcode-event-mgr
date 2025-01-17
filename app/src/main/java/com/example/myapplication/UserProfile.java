package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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

        showDetails();
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
}