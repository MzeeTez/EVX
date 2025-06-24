package com.example.evc;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity4 extends AppCompatActivity {

    private EditText etName, etCarModel, etCarID;
    private String phoneNumber;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main4);

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etName = findViewById(R.id.editTextNumberDecimal8);
        etCarModel = findViewById(R.id.editTextNumberDecimal7);
        etCarID = findViewById(R.id.editTextNumberDecimal6);

        // Get phone number from previous activity
        phoneNumber = getIntent().getStringExtra("phone");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not found", Toast.LENGTH_LONG).show();
            finish(); // Stop activity if phone number is missing
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();
    }

    public void ackhome(View v) {
        Toast.makeText(this, "Returning to Home", Toast.LENGTH_SHORT).show();
        startActivityWithTransition(new Intent(MainActivity4.this, MainActivity.class));
    }

    public void sendData(View v) {
        String name = etName.getText().toString().trim();
        String carModel = etCarModel.getText().toString().trim();
        String carID = etCarID.getText().toString().trim();

        // Validate input
        if (name.isEmpty() || carModel.isEmpty() || carID.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data to save
        Map<String, String> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("carModel", carModel);
        userData.put("carID", carID);
        userData.put("phone", phoneNumber);

        // Save to Firestore
        firestore.collection("UserNumbers").document(phoneNumber)
                .set(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(MainActivity4.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    clearInputs();

                    // Proceed to next screen
                    Intent intent = new Intent(MainActivity4.this, MainActivity5.class);
                    intent.putExtra("phone", phoneNumber);
                    startActivityWithTransition(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity4.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void clearInputs() {
        etName.setText("");
        etCarModel.setText("");
        etCarID.setText("");
    }

    private void startActivityWithTransition(Intent intent) {
        Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
        startActivity(intent, options);
    }
}
