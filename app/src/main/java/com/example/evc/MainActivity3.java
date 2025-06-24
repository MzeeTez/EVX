package com.example.evc;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity3 extends AppCompatActivity {

    private String phoneNumber;
    private static final String TAG = "MainActivity3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main3);

        // Get phone number from intent extras
        phoneNumber = getIntent().getStringExtra("phone");

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.w(TAG, "No phone number received from intent");
            Toast.makeText(this, "Phone number missing", Toast.LENGTH_SHORT).show();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void backhome(View v) {
        Intent intent = new Intent(MainActivity3.this, MainActivity2.class);
        startActivityWithTransition(intent);
    }

    public void ROTP(View v) {
        Toast.makeText(this, "Correct OTP", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(MainActivity3.this, MainActivity4.class);
        intent.putExtra("phone", phoneNumber);
        startActivityWithTransition(intent);
    }

    private void startActivityWithTransition(Intent intent) {
        Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
        startActivity(intent, options);
    }
}
