package com.bewil.besms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.lsposed.lsparanoid.Obfuscate;

//@Obfuscate
public class BootAndPowerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_POWER_CONNECTED.equals(action) &&
                !MyForegroundService.isRunning()){

            Intent serviceIntent = new Intent(context, MyForegroundService.class);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
                // Android 14+
                context.startForegroundService(serviceIntent);
            } else {
                // Android 13 and below
                context.startService(serviceIntent);
            }
        }
    }
}
