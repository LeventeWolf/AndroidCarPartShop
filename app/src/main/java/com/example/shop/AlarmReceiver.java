package com.example.shop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.shop.Notification.NotificationHelper;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        new NotificationHelper(context).send("Itt az ideje vásárolni!:)");
    }
}
