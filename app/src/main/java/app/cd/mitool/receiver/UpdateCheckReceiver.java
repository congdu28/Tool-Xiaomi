package app.cd.mitool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.cd.mitool.util.UpdateHelper;

/**
 * Periodic broadcast receiver triggered by AlarmManager to check GitHub releases in background
 * and automatically notify the user when a new app update is published.
 */
public class UpdateCheckReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        UpdateHelper.checkForUpdate(context.getApplicationContext(), false);
    }
}
