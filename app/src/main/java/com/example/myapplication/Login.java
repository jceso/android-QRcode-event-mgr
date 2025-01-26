package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class Login extends AppCompatActivity {
    EditText emailInput, passwordInput;
    Button log_btn;
    TextView regNow;
    FirebaseAuth fAuth;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null)
            checkLevelAccess(currentUser.getUid());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fAuth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        log_btn = findViewById(R.id.login_btn);
        regNow = findViewById(R.id.reg_now);
        regNow.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Register.class);
            startActivity(intent);
            finish();
        });

        log_btn.setOnClickListener(v -> {
            String email, password;
            email = String.valueOf(emailInput.getText());
            password = String.valueOf(passwordInput.getText());

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Login.this, "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Login.this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            fAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
                Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                checkLevelAccess(Objects.requireNonNull(authResult.getUser()).getUid());
            }).addOnFailureListener(e -> Toast.makeText(Login.this, "No user found with these credentials", Toast.LENGTH_SHORT).show());
        });
    }

    private void checkLevelAccess(String uid) {
        Log.d("Login","ID: " + uid);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("Users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                Log.d("TAG", "OnSuccess: " + documentSnapshot.getData());
                if (Objects.equals(documentSnapshot.getString("isAdmin"), "1")) {
                    //User is admin
                    startActivity(new Intent(getApplicationContext(), EventListAdmin.class));
                    finish();
                } else {
                    //Normal user
                    startActivity(new Intent(getApplicationContext(), QrScanner.class));
                    finish();
                }
        });
    }
}