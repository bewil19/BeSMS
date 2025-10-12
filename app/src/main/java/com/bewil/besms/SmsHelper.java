package com.bewil.besms;

import android.content.Context;
import android.telephony.SmsManager;

import org.lsposed.lsparanoid.Obfuscate;

//@Obfuscate
public class SmsHelper {

    public static void sendSMS(Context context, String phoneNumber, String message, Integer simNumber){
        SmsManager smsManager = context.getSystemService(SmsManager.class).createForSubscriptionId(simNumber);
        smsManager.sendTextMessage(phoneNumber, null, message, null, null, 0);
    }
}
