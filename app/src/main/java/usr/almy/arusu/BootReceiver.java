package usr.almy.arusu;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver { //он ждет включения телефона
	@SuppressLint("UnsafeProtectedBroadcastReceiver")
	@Override
	public void onReceive(Context context, Intent intent) { //дождался
		context.startService(new Intent(context, Overlay.class)); //запускает сервис
	}
}
