package com.example.myapplication;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Random;

public class QrScanner extends AppCompatActivity {
    private TextView scannedValue;
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;

    private final ActivityResultLauncher<ScanOptions> scannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null)
                    Toast.makeText(QrScanner.this, "Cancelled", Toast.LENGTH_LONG).show();
                else {
                    scannedValue.setText(result.getContents());
                    // Pass the scanned value to EventBooking activity
                    Intent intent = new Intent(getApplicationContext(), EventBooking.class);
                    intent.putExtra("scannedQRCode", result.getContents()); // Put the scanned QR code content in the intent
                    startActivity(intent);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scanner);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button name_btn = findViewById(R.id.user);
        Button logout_btn = findViewById(R.id.logout_btn);
        scannedValue = findViewById(R.id.value);
        Button scan_btn = findViewById(R.id.scan_btn);
        Button list = findViewById(R.id.evt_list);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        } else {
            db = FirebaseFirestore.getInstance();
            db.collection("Users").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get the name from the FireStore document
                    String nameInitial = documentSnapshot.getString("UserName");
                    name_btn.setText(nameInitial.substring(0,1));
                } else
                    Log.d("User Info", "No such document");
            });
        }

        // Generate random RGB values ensuring brightness and moderate saturation, not vivid colors
        Random rand = new Random();
        int r = (int) (Math.min(0.5f + rand.nextFloat() * 0.5f, 0.9f) * 255);
        int g = (int) (Math.min(0.5f + rand.nextFloat() * 0.5f, 0.9f) * 255);
        int b = (int) (Math.min(0.5f + rand.nextFloat() * 0.5f, 0.9f) * 255);
        // Set random color for username button
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor((0xFF << 24) | (r << 16) | (g << 8) | b);
        drawable.setCornerRadius(100);
        name_btn.setBackground(drawable);

        logout_btn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(), Login.class));
            finish();
        });

        scan_btn.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan QR Code");
            options.setOrientationLocked(false);
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            scannerLauncher.launch(options);
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), EventListAdmin.class));
                finish();
            }
        });
    }
}