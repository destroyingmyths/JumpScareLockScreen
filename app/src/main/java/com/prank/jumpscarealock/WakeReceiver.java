package com.prank.jumpscarealock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class WakeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case Intent.ACTION_SCREEN_ON:
            case Intent.ACTION_USER_PRESENT:
            case Intent.ACTION_BOOT_COMPLETED:
                launchScare(context);
                break;
        }
    }

    private void launchScare(Context context) {
        Intent i = new Intent(context, JumpScareActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                   Intent.FLAG_ACTIVITY_CLEAR_TOP |
                   Intent.FLAG_ACTIVITY_SINGLE_TOP |
                   Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(i);
    }
}
