package com.bewil.besms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;

    private TextView tvElapsedTime;
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String LOGS_KEY = "serviceLogs";
    private LogReceiver logReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartService = findViewById(R.id.btnStartService);
        Button btnStopService = findViewById(R.id.btnStopService);
        Button btnClearLogs = findViewById(R.id.btnClearLogs);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);

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

        logReceiver = new LogReceiver(tvElapsedTime);
        IntentFilter filter = new IntentFilter("ServiceLogUpdate");
        registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        scheduleTask("MyWorker");
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
        if (!MyForegroundService.isRunning()) {
            Intent serviceIntent = new Intent(this, MyForegroundService.class);
            startForegroundService(serviceIntent);
        }
    }

    private void scheduleTask(String tag){
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(MyWorker.class, 20, TimeUnit.MINUTES)
            .addTag(tag)
            .setInitialDelay(20, TimeUnit.MINUTES)
            .build();

        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
    }

    private void cancelTask(String tag){
        WorkManager.getInstance(this).cancelAllWorkByTag(tag);
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(logReceiver);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Permission denied, handle accordingly (e.g., show a message)
                Toast.makeText(this, "Notification permission is required to run the service", Toast.LENGTH_LONG).show();
            }
        }
    }
}