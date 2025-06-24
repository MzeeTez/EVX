package com.example.evc;

import android.os.StrictMode;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FCMSender {

    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE";

    public static void sendNotification(String token, String title, String message, String clickAction) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            URL url = new URL(FCM_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Authorization", "key=" + SERVER_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject json = new JSONObject();
            json.put("to", token.trim());

            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", message);
            notification.put("click_action", clickAction); // Optional if handling deep link

            json.put("notification", notification);

            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(json.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = conn.getResponseCode();
            Log.d("FCMSender", "FCM Response Code: " + responseCode);

            conn.disconnect();

        } catch (Exception e) {
            Log.e("FCMSender", "Failed to send FCM notification", e);
        }
    }
}
