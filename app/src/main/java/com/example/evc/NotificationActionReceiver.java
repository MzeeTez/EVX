package com.example.evc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.*;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String requestId = intent.getStringExtra("requestId");

        if (action == null || requestId == null) return;

        DatabaseReference requestRef = FirebaseDatabase.getInstance().getReference("requests").child(requestId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String donorPhone = snapshot.child("to").getValue(String.class);
                if (donorPhone == null) {
                    Toast.makeText(context, "Donor phone not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (action.equals("ACCEPT_ACTION")) {
                    // Step 1: Get donor's location
                    DatabaseReference donorRef = FirebaseDatabase.getInstance().getReference("users").child(donorPhone);
                    donorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot donorSnap) {
                            Double donorLat = donorSnap.child("latitude").getValue(Double.class);
                            Double donorLng = donorSnap.child("longitude").getValue(Double.class);

                            if (donorLat == null || donorLng == null) {
                                Toast.makeText(context, "Donor location not available", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Step 2: Update request status and location
                            requestRef.child("status").setValue("accepted");
                            requestRef.child("donorLat").setValue(donorLat);
                            requestRef.child("donorLng").setValue(donorLng);
                            requestRef.child("acceptedBy").setValue(donorPhone);

                            // Step 3: Notify Donor app (donar1) via broadcast
                            Intent statusIntent = new Intent("REQUEST_STATUS_UPDATED");
                            statusIntent.putExtra("status", "accepted");
                            statusIntent.putExtra("requestId", requestId);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(statusIntent);

                            Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(context, "Failed to read donor location", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else if (action.equals("REJECT_ACTION")) {
                    requestRef.child("status").setValue("rejected");
                    Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(context, "Request read failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
