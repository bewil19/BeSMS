package com.bewil.besms;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;

    private Button btnStartService, btnStopService, btnClearLogs;
    private TextView tvElapsedTime;
    private BroadcastReceiver logReceiver;
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String LOGS_KEY = "serviceLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
        btnClearLogs = findViewById(R.id.btnClearLogs);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);

        // ✅ Request notification permission (Android 13+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }

        btnStartService.setOnClickListener(v -> startForegroundService());
        btnStopService.setOnClickListener(v -> stopForegroundService());
        btnClearLogs.setOnClickListener(view -> clearLogs());

        tvElapsedTime.setText(getStoredLogs());

        logReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("ServiceLogUpdate".equals(intent.getAction())) {
                    String logs = intent.getStringExtra("logs");
                    tvElapsedTime.setText(logs);
                }
            }
        };

        registerReceiver(logReceiver, new IntentFilter("ServiceLogUpdate"), Context.RECEIVER_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void clearLogs() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LOGS_KEY, ""); // Set logs to an empty string
        editor.apply();

        tvElapsedTime.setText(getStoredLogs());
    }

    private String getStoredLogs() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(LOGS_KEY, "Service Logs:\n");
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        startService(serviceIntent);
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(logReceiver);
    }

    // ✅ Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        }
    }
}