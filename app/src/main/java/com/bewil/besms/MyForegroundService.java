package com.bewil.besms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lsposed.lsparanoid.Obfuscate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//@Obfuscate
public class MyForegroundService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "ServicePrefs";
    private static final String LOGS_KEY = "serviceLogs";

    private Handler handler;
    private Runnable runnable;

    private static boolean isServiceRunning = false;
    private final ArrayList<String[]> arrayList = new ArrayList<>();
    private String expireDate;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        createNotificationChannel();

        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                //String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                Random r = new Random();
                int delay = r.nextInt(120000 - 30000) + 30000;

                if(arrayList.isEmpty()){ //|| !currentDate().equals(expireDate)){

                    ExecutorService executor = Executors.newSingleThreadExecutor();

                    executor.execute(() -> {
                        getSMS();

                        handler.post(() -> {
                            if (arrayList.isEmpty()){ //|| !currentDate().equals(expireDate)) {
                                int delay2 = r.nextInt((2 * 60 * 60 * 1000) - (60 * 60 * 1000)) + (60 * 60 * 1000);
                                int hours = delay2 / (60 * 60 * 1000);
                                int minutes = (delay2 % (60 * 60 * 1000)) / (60 * 1000);
                                saveLog("Wait for " + hours + " hour(s) and " + minutes + " minute(s)");
                                updateNotification("Wait for " + hours + " hour(s) and " + minutes + " minute(s)");
                                sendUpdateToActivity();
                                handler.postDelayed(runnable, delay2);
                            } else {
                                saveLog("Wait for 5 seconds");
                                updateNotification("Wait for 5 seconds");
                                sendUpdateToActivity();
                                handler.postDelayed(runnable, 5000);
                            }
                        });
                    });
                } else {
                    String[] sms = arrayList.get(0);
                    String phoneNo = sms[0];
                    String message = sms[1];
                    int simNo = Integer.parseInt(sms[2]);
                    SmsHelper.sendSMS(getApplicationContext(), phoneNo, message, simNo);
                    saveLog("Sent SMS (" + arrayList.size() + ")...");
                    updateNotification("Sent SMS (" + arrayList.size() + ")...");
                    sendUpdateToActivity();
                    saveLog("Wait for " + delay);
                    sendUpdateToActivity();
                    arrayList.remove(0);
                    handler.postDelayed(this, delay);
                }
            }
        };
        handler.post(runnable);
    }

    private void getSMS(){
        arrayList.clear();

        SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
	    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
		    return;
	    }

	    List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if(subscriptionInfoList == null || subscriptionInfoList.isEmpty()){
            return;
        }
        for(int i = 0; i < subscriptionInfoList.size(); i++){
            int id = subscriptionInfoList.get(i).getSubscriptionId();
            String phoneNumber = subscriptionManager.getPhoneNumber(id);
            phoneNumber = phoneNumber.replace("+", "");
            if(phoneNumber.isEmpty()){
                continue;
            }
            saveLog("Getting SMS for " + phoneNumber + " ...");
            sendUpdateToActivity();

            getSMSFor(phoneNumber, id);
        }

        saveLog("Found " + arrayList.size() + " SMS to send.");
        sendUpdateToActivity();
    }

    private void getSMSFor(String simNumber, int simID){
        String result = UrlHelper.getPage(MainActivity.apiUrl + "getsms/" + simNumber);
        try{
            JSONObject jsonObject = new JSONObject(result);
            JSONObject jResult = jsonObject.getJSONObject("result");
            JSONObject meta = jResult.getJSONObject("meta");
            String phoneId = meta.getString("id");
            JSONObject sms = jResult.getJSONObject("sms");
            JSONObject phoneObj = sms.getJSONObject(phoneId);
            JSONArray phoneSms = phoneObj.getJSONArray("sms");
            int i = 0;
            while(i < phoneSms.length()){
                JSONObject row = phoneSms.getJSONObject(i);
                String phoneNumber = row.getString("ddi_label");
                String message = row.getString("sms");
                expireDate = row.getString("date");
                try{
                    String[] myStringArray = {phoneNumber, message, String.valueOf(simID)};
                    arrayList.add(myStringArray);
                    i++;
                } catch (Exception ignored) {
                }
            }
        } catch (JSONException ignored) {
        }
    }

    private String currentDate(){
	    return new SimpleDateFormat("y-MM-dd", Locale.getDefault()).format(new Date());
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
        editor.putString(LOGS_KEY, currentDate() + logEntry + "\n" + existingLogs);
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
        manager.notify(NOTIFICATION_ID, getNotification(currentDate() + logEntry));
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

