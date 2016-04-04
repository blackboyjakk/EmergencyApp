package kmitl.ce.project.purmpon.emergencyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStartReceiver extends BroadcastReceiver {
    public AutoStartReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Intent myIntent = new Intent(context, SoundDetectService.class);
        context.startService(myIntent);

    }
}
