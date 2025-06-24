package com.example.evc;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {

    private EditText editTextNumberDecimal;
    private FirebaseFirestore firestoreDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        editTextNumberDecimal = findViewById(R.id.editTextNumberDecimal);
        firestoreDb = FirebaseFirestore.getInstance();

        // Restore saved phone
        String savedPhone = getSharedPreferences("UserProfile", MODE_PRIVATE).getString("phone", "");
        if (!savedPhone.isEmpty()) {
            editTextNumberDecimal.setText(savedPhone);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void ackhome(View v) {
        showToast("Returning to Home");
        startActivityWithTransition(new Intent(MainActivity2.this, MainActivity.class));
    }

    public void sendotp(View v) {
        String number = editTextNumberDecimal.getText().toString().trim();

        if (number.isEmpty()) {
            showToast("Please enter a number");
            return;
        }

        if (!number.matches("\\d{10}")) {
            showToast("Enter a valid 10-digit number");
            return;
        }

        checkAndRegisterNumber(number);
    }

    private void checkAndRegisterNumber(String number) {
        showToast("Checking number...");

        DocumentReference docRef = firestoreDb.collection("UserNumbers").document(number);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Existing user
                    savePhoneLocally(number);
                    saveFcmToken(number);
                    showToast("Number found. Redirecting...");
                    Intent intent = new Intent(MainActivity2.this, MainActivity5.class);
                    intent.putExtra("phone", number);
                    startActivityWithTransition(intent);
                } else {
                    // New user
                    registerNewNumber(docRef, number);
                }
            } else {
                showToast("Error checking number: " + task.getException().getMessage());
            }
        });
    }

    private void registerNewNumber(DocumentReference docRef, String number) {
        Map<String, String> userData = new HashMap<>();
        userData.put("Number", number);

        docRef.set(userData)
                .addOnSuccessListener(unused -> {
                    savePhoneLocally(number);
                    saveFcmToken(number);
                    showToast("Number saved and registered");
                    Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
                    intent.putExtra("phone", number);
                    startActivityWithTransition(intent);
                })
                .addOnFailureListener(e ->
                        showToast("Error saving number: " + e.getMessage())
                );
    }

    private void savePhoneLocally(String number) {
        getSharedPreferences("UserProfile", MODE_PRIVATE)
                .edit()
                .putString("phone", number)
                .apply();
    }

    private void saveFcmToken(String number) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                FirebaseFirestore.getInstance()
                        .collection("UserNumbers")
                        .document(number)
                        .update("token", token)
                        .addOnSuccessListener(unused -> {
                            // Token saved
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to save FCM token", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity2.this, message, Toast.LENGTH_SHORT).show();
    }

    private void startActivityWithTransition(Intent intent) {
        Bundle options = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
        startActivity(intent, options);
    }
}
