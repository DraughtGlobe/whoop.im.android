package im.whoop.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WhoopBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent service = new Intent(context, WhoopService.class);
    context.startService(service);
  }
}