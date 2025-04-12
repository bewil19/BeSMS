package com.bewil.besms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

public class MyWorker extends Worker {

    private static final String CHANNEL_ID = "WorkerServiceChannel";
    private static final int NOTIFICATION_ID = 2;

    public MyWorker(Context context, WorkerParameters workerParameters){
        super(context, workerParameters);
        createNotificationChannel(context);
    }

    private void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Worker Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Worker Service")
                .setContentText("Running background task...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync(){
        return CallbackToFutureAdapter.getFuture(completer -> {
            Notification notification = createNotification();
            completer.set(new ForegroundInfo(NOTIFICATION_ID, notification));
            return "ForegroundInfoAsync";
        });
    }

    @NonNull
    @Override
    public Result doWork(){
        Log.d("MyWorker", "Starting doWork...");

        try {
            // Ensure worker runs in foreground before starting another foreground service
            ForegroundInfo foregroundInfo = getForegroundInfoAsync().get();
            setForegroundAsync(foregroundInfo);
        } catch (Exception e) {
            Log.e("MyWorker", "Failed to set foreground: " + e.getMessage());
            return Result.failure();
        }

        if (!MyForegroundService.isRunning()) {
            Context context = getApplicationContext();
            Intent serviceIntent = new Intent(context, MyForegroundService.class);
            context.startForegroundService(serviceIntent);
        }

        return Result.success();
    }
}
