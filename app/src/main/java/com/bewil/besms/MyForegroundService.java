package com.bewil.besms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String LOGS_KEY = "serviceLogs";

    private Handler handler;
    private Runnable runnable;
    private long startTime;

    private static boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        startTime = System.currentTimeMillis();
        createNotificationChannel();

        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                String logEntry = currentTime + " - Elapsed: " + elapsedTime + " sec";

                saveLog(logEntry);
                updateNotification(logEntry);
                sendUpdateToActivity();

                handler.postDelayed(this, 5000); // Repeat every 5 sec
            }
        };
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, getNotification("Service Starting..."));
        updateNotification("Service Restarted");
        return START_REDELIVER_INTENT;
    }

    private void saveLog(String logEntry) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existingLogs = sharedPreferences.getString(LOGS_KEY, "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LOGS_KEY, logEntry + "\n" + existingLogs);
        editor.apply();
    }

    private void sendUpdateToActivity() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String logs = sharedPreferences.getString(LOGS_KEY, "");

        Intent intent = new Intent("ServiceLogUpdate");
        intent.setPackage(getPackageName());
        intent.putExtra("logs", logs);
        sendBroadcast(intent);
    }

    private void updateNotification(String logEntry) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, getNotification(logEntry));
    }

    private Notification getNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Persistent Foreground Service")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        isServiceRunning = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isRunning(){
        return isServiceRunning;
    }
}

