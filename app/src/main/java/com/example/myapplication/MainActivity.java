package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class MainActivity extends AppCompatActivity {
    QRGEncoder qrgEncoder;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText qrText = findViewById(R.id.qrText);
        ImageView qrImage = findViewById(R.id.qrImage);
        Button grtButton = findViewById(R.id.grtButton);
        Button shareButton = findViewById(R.id.shareButton);

        grtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(qrText.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Inserisci il testo da codificare", Toast.LENGTH_SHORT).show();
                } else {
                    WindowManager mgr = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = mgr.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);

                    int width = size.x;
                    int height = size.y;
                    int dimen = width < height ? width : height;
                    dimen = dimen*3/4;

                    qrgEncoder = new QRGEncoder(qrText.getText().toString(), null, QRGContents.Type.TEXT, dimen);

                    try {
                        bitmap = qrgEncoder.getBitmap();
                        qrImage.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e("TAG", e.toString());
                    }
                }
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, getUriFromBitmap());
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this code");
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        });
    }


    private Uri getUriFromBitmap() {
        try {
            // Create a file to store the QR code image
            File file = new File(
                this.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "qr_code_" + System.currentTimeMillis() + ".png"
            );

            // Write the bitmap to the file
            FileOutputStream outStream = new FileOutputStream(file);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream);
                outStream.close();
            }

            // Get the URI for the file using FileProvider
            return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        } catch (IOException e) {
            Log.e("TAG", "getUriFromBitmap: " + e.getMessage());
        }

        return null;
    }
}