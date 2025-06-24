package com.example.evc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class acceptor2 extends AppCompatActivity {

    private LinearLayout loadingBar;
    private int blockCount = 10;
    private int currentBlock = 0;
    private Handler handler = new Handler();
    private Runnable blockAdder;

    private String donorPhone = "";
    private String acceptorPhone = "";
    private String requestId = "";

    private boolean isRouteAccepted = false;
    private static final int TIMEOUT_MS = 60000; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acceptor2);

        loadingBar = findViewById(R.id.loading_bar);

        donorPhone = getIntent().getStringExtra("donorPhone");
        acceptorPhone = getIntent().getStringExtra("acceptorPhone");
        requestId = getIntent().getStringExtra("requestId");

        if (donorPhone == null || acceptorPhone == null || requestId == null) {
            Toast.makeText(this, "Missing intent data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startLoadingAnimation();
        waitForRouteAcceptance();
        startTimeoutCountdown();
    }

    private void startLoadingAnimation() {
        blockAdder = new Runnable() {
            @Override
            public void run() {
                if (currentBlock < blockCount) {
                    View block = new View(acceptor2.this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    params.setMargins(4, 0, 4, 0);
                    block.setLayoutParams(params);
                    block.setBackgroundColor(0xFFFFFFFF); // White blocks
                    loadingBar.addView(block);
                    currentBlock++;
                    handler.postDelayed(this, 300);
                } else {
                    loadingBar.removeAllViews();
                    currentBlock = 0;
                    handler.postDelayed(this, 300);
                }
            }
        };
        handler.post(blockAdder);
    }

    private void waitForRouteAcceptance() {
        String routeId = donorPhone + "_" + acceptorPhone;
        DatabaseReference routeRef = FirebaseDatabase.getInstance().getReference("Routes").child(routeId);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                routeRef.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists() && "accepted".equals(snapshot.child("status").getValue(String.class))) {
                        isRouteAccepted = true;
                        handler.removeCallbacks(blockAdder);
                        goToRouteActivity();
                    } else {
                        handler.postDelayed(this, 2000);
                    }
                }).addOnFailureListener(e -> handler.postDelayed(this, 2000));
            }
        }, 2000);
    }

    private void startTimeoutCountdown() {
        handler.postDelayed(() -> {
            if (!isRouteAccepted) {
                cancelRequestAndReturn();
            }
        }, TIMEOUT_MS);
    }

    private void cancelRequestAndReturn() {
        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("requests").child(requestId);
        reqRef.removeValue();

        Toast.makeText(this, "Request timed out. Going back...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(acceptor2.this, Acceptor1.class);
        intent.putExtra("phone", acceptorPhone);
        startActivity(intent);
        finish();
    }

    private void goToRouteActivity() {
        Intent intent = new Intent(acceptor2.this, RouteActivity.class);
        intent.putExtra("donorPhone", donorPhone);
        intent.putExtra("acceptorPhone", acceptorPhone);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(blockAdder);
    }
}
