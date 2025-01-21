package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class QrScanner extends AppCompatActivity {
    private TextView scannedValue;
    FirebaseUser user;

    private final ActivityResultLauncher<ScanOptions> scannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null)
                    Toast.makeText(QrScanner.this, "Cancelled", Toast.LENGTH_LONG).show();
                else {
                    scannedValue.setText(result.getContents());
                    // Pass the scanned value to EventBooking activity
                    Intent intent = new Intent(getApplicationContext(), EventBooking.class);
                    intent.putExtra("event_uid", result.getContents()); // Put the scanned QR code content in the intent
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

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(QrScanner.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(QrScanner.this, name_btn);
        ImageButton backButton = findViewById(R.id.back);
        BasicButtons.handleBackButton(QrScanner.this, backButton);

        scannedValue = findViewById(R.id.value);
        Button scan_btn = findViewById(R.id.scan_btn);
        Button list = findViewById(R.id.evt_list);
        user = FirebaseAuth.getInstance().getCurrentUser();

        scan_btn.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan QR Code");
            options.setOrientationLocked(true);
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            scannerLauncher.launch(options);
        });

        list.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), EventListAdmin.class));
            finish();
        });
    }
}