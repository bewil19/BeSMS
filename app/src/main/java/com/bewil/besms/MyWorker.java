package com.bewil.besms;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MyWorker extends Worker {

    public MyWorker(Context context, WorkerParameters workerParameters){
        super(context, workerParameters);
    }

    @NonNull
    @Override
    public Result doWork(){
        Log.d("MyWorker", "Starting doWork...");
        if (!MyForegroundService.isRunning()) {
            Context context = getApplicationContext();
            Intent serviceIntent = new Intent(context, MyForegroundService.class);
            context.startForegroundService(serviceIntent);
        }
        return Result.success();
    }
}
