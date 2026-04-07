package com.prank.jumpscarealock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class WakeService extends Service {

    private static final String CHANNEL_ID = "wake_channel";
    private WakeReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register receiver programmatically (required for SCREEN_ON on newer Android)
        receiver = new WakeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(receiver, filter);

        startForeground(1, buildNotification());
    }

    private Notification buildNotification() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "Background", NotificationManager.IMPORTANCE_MIN);
        channel.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);

        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("System")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setNotificationSilent()
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
