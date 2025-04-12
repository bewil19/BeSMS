package com.bewil.besms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ALL_PERMISSIONS = 1001;

    private TextView tvElapsedTime;
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String LOGS_KEY = "serviceLogs";
    private LogReceiver logReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("StartingBeSMS", BuildConfig.COMMIT_HASH);

        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        tvElapsedTime = findViewById(R.id.tvElapsedTime);

        findViewById(R.id.btnStartService).setOnClickListener(view -> startForegroundService());
        findViewById(R.id.btnStopService).setOnClickListener(v -> stopForegroundService());
        findViewById(R.id.btnClearLogs).setOnClickListener(view -> clearLogs());

        tvElapsedTime.setText(getStoredLogs());

        logReceiver = new LogReceiver(tvElapsedTime);
        IntentFilter filter = new IntentFilter("ServiceLogUpdate");
        registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        //scheduleTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startForegroundService();
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        List<String> permissionsToExplain = new ArrayList<>();

        String[] allPermissions = new String[]{
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.SEND_SMS
        };

        for (String permission : allPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    permissionsToExplain.add(permission);
                }
            }
        }

        if (!permissionsToExplain.isEmpty()) {
            showPermissionRationale(permissionsToRequest.toArray(new String[0]));
        } else if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_ALL_PERMISSIONS
            );
        }
    }

    private void showPermissionRationale(String[] permissionsToRequest) {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs SMS and phone permissions to function properly. Please grant them.")
                .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(
                        this,
                        permissionsToRequest,
                        REQUEST_ALL_PERMISSIONS
                ))
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void clearLogs() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LOGS_KEY, ""); // Set logs to an empty string
        editor.apply();

        tvElapsedTime.setText(getStoredLogs());
        
        sendTestText();
    }
    
    private void sendTestText(){
        SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> subscriptionInfoList;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        for(int i = 0; i < subscriptionInfoList.size(); i++){
            int id = subscriptionInfoList.get(i).getSubscriptionId();
            sendSMS("+447516617538", "Hello from SIM " + id, id);
            Random r = new Random();
            Integer delay = r.nextInt(120000 - 30000) + 30000;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getStoredLogs() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(LOGS_KEY, "Service Logs:\n");
    }

    private void startForegroundService() {
        if (!MyForegroundService.isRunning()) {
            Intent serviceIntent = new Intent(this, MyForegroundService.class);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
                // Android 14+
                startForegroundService(serviceIntent);
            } else {
                // Android 13 and below
                startService(serviceIntent);
            }
        }
    }

    public void sendSMS(String phoneNumber, String message, Integer simNumber){
        SmsManager smsManager = getSystemService(SmsManager.class).createForSubscriptionId(simNumber);
        //SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(simNumber);
        smsManager.sendTextMessage(phoneNumber, null, message, null, null, 0);
    }

    /*private void scheduleTask(){
        cancelTask();

        Log.d("scheduleTask", "All tasks has been scheduled.");

        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(MyWorker.class, 20, TimeUnit.MINUTES)
            .addTag("MyWorker")
            .setInitialDelay(20, TimeUnit.MINUTES)
            .build();

        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
    }

    private void cancelTask(){
        Log.d("cancelTask", "All tasks has been cancelled.");
        WorkManager.getInstance(this).cancelAllWorkByTag("MyWorker");
    }*/

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
        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // Check if the user denied the permission permanently (Don't ask again)
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        showSettingsDialog(permission);  // Show dialog to go to app settings
                    }
                }
            }
        }
    }

    private void showSettingsDialog(String permission) {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("This permission is required for the app to function. Please enable it in your device settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_ALL_PERMISSIONS);
    }
}