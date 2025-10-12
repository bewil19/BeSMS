package com.bewil.besms;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.json.JSONObject;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//@Obfuscate
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ALL_PERMISSIONS = 1000;
    public static final String apiUrl = "https://dev.benjamin-wilson.co.uk/api/";
    private long downloadId;

    private TextView tvElapsedTime;
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String LOGS_KEY = "serviceLogs";
    private LogReceiver logReceiver;
    private ActivityResultLauncher<Intent> appSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("StartingBeSMS", BuildConfig.COMMIT_HASH);

        setContentView(R.layout.activity_main);

        appSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> checkAndRequestPermissions()
        );

        checkAndRequestPermissions();
        checkForUpdate();

        tvElapsedTime = findViewById(R.id.tvElapsedTime);

        tvElapsedTime.setText(getStoredLogs());

        logReceiver = new LogReceiver(tvElapsedTime);
        IntentFilter filter = new IntentFilter("ServiceLogUpdate");
        registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        startForegroundService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startForegroundService();
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

    private void checkForUpdate(){
        new Thread(() -> {
            try {
                String result = UrlHelper.getPage(apiUrl + "version");

                JSONObject json = new JSONObject(result);
                String latestVersion = json.getString("versionName");
                String apkUrl = json.getString("apkUrl");
                if(!latestVersion.equals(BuildConfig.COMMIT_HASH)){
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(this)
                            .setTitle("Update Available")
                            .setMessage("A new version is available. Would you like to update?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                downloadAndInstall(apkUrl);
                            })
                            .setNegativeButton("No", null)
                            .setCancelable(true)
                            .show();
                    });
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void downloadAndInstall(String apkUrl){
        File file = new File(getExternalFilesDir(null), "update.apk");
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Downloading update");
        request.setDescription("Please wait...");
        request.setDestinationUri(Uri.fromFile(file));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadId = downloadManager.enqueue(request);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long completedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (completedDownloadId == downloadId) {
                    unregisterReceiver(this);

                    try {
                        Uri apkUri = FileProvider.getUriForFile(context, getPackageName() + ".provider", file);
                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
                        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(installIntent);
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED);
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
            clearLogs();
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

    private String getPermissionFriendlyName(String permission) {
        return switch (permission) {
            case Manifest.permission.CAMERA -> "Camera";
            case Manifest.permission.ACCESS_FINE_LOCATION -> "Location";
            case Manifest.permission.READ_CONTACTS -> "Contacts";
            default -> permission; // fallback to raw string
        };
    }

    private void showSettingsDialog(String permission) {
        String permissionName = getPermissionFriendlyName(permission);

        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage(permissionName + " permission is required for the app to function. Please enable it in your device settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        //startActivityForResult(intent, REQUEST_ALL_PERMISSIONS);
        appSettingsLauncher.launch(intent);
    }
}