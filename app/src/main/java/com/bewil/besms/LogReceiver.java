package com.bewil.besms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

public class LogReceiver extends BroadcastReceiver {
    private final TextView tvElapsedTime;

    public LogReceiver(TextView textView){
        this.tvElapsedTime = textView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("ServiceLogUpdate".equals(intent.getAction())) {
            String logs = intent.getStringExtra("logs");
            tvElapsedTime.setText(logs);
        }
    }
}
