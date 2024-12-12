package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
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
        Button grtButton = findViewById(R.id.grtButton);
        ImageView qrImage = findViewById(R.id.qrImage);


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


    }

}