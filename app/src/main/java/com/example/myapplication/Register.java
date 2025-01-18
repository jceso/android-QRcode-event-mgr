package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Register extends AppCompatActivity {
    EditText nameInput, emailInput, passwordInput, phoneInput;
    Button reg_btn;
    TextView logNow;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), QrScanner.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        nameInput = findViewById(R.id.name);
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        phoneInput = findViewById(R.id.phone);
        reg_btn = findViewById(R.id.register_btn);
        logNow = findViewById(R.id.log_now);
        logNow.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        reg_btn.setOnClickListener(v -> {
            String name, email, password, phone;
            name = String.valueOf(nameInput.getText());
            email = String.valueOf(emailInput.getText());
            password = String.valueOf(passwordInput.getText());
            phone = phoneInput.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Register.this, "Enter email", Toast.LENGTH_SHORT).show();
                return;
            } if (TextUtils.isEmpty(password)) {
                Toast.makeText(Register.this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            } if (TextUtils.isEmpty(phone)) {
                Toast.makeText(Register.this, "Enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            fAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {  // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = fAuth.getCurrentUser();
                            Toast.makeText(Register.this, "Account created!", Toast.LENGTH_SHORT).show();
                            DocumentReference dr = fStore.collection("Users").document(Objects.requireNonNull(user).getUid());
                            Map<String,Object> userInfo = new HashMap<>();

                            userInfo.put("UserName", name);
                            userInfo.put("UserEmail", email);
                            userInfo.put("PhoneNumber", phone);
                            userInfo.put("isAdmin", "0");   //On FireStore change to 1 in case of admin
                            dr.set(userInfo);

                            startActivity(new Intent(getApplicationContext(), QrScanner.class));
                            finish();
                        } else
                            Toast.makeText(Register.this, "Authentication failed!", Toast.LENGTH_SHORT).show();
                    });
        });
    }
}