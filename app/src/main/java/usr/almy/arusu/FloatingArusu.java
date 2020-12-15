package usr.almy.arusu;

import android.app.Application;
import android.content.res.Resources;

public class FloatingArusu extends Application { //чтобы ресурсы можно было достать всегда
	protected static FloatingArusu instance; //чтобы оно было доступно везде (в пределах этого приложения)

	public static Resources getStaticResources() {
		return instance.getResources();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
	}
}