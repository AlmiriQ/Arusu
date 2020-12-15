package usr.almy.arusu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.IBinder;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import static android.graphics.Bitmap.createBitmap;
import static java.lang.Math.sin;

//поверхность для персонажа и эффектов
class CharView extends SurfaceView implements Runnable {
	//поток отрисовки
	final Thread gfx;
	//поток обновлений
	final Thread upd;
	//амплитуда колебаний, ширина и высота
	volatile public short amplitude = 5, w = 128, h = 160;
	//холдер нужен для того, чтобы потом рисовать
	volatile SurfaceHolder sh;
	//контекст //нужен
	Context ctx;
	//изображение персонажа
	Bitmap image;
	//стандартная рисовалка для сброса эффектов
	Paint default_paint = new Paint();
	//надо
	Paint p = default_paint;
	//айди персонажа
	volatile byte character;
	//флаг молний
	volatile boolean light = false;
	//айдишники
	int[] ids = new int[]{
			R.drawable.maria, R.drawable.i_tohka,
			R.drawable.tohka, R.drawable.yoshino,
			R.drawable.kotori, R.drawable.kurumi,
			R.drawable.origami, R.drawable.i_origami
	};
	int t; //y координата
	volatile boolean sin_m; //параметр движения по синусу
	volatile boolean loaded; //флаг загрузки
	Canvas canvas; //холст

	//конструктор
	@SuppressLint("UseCompatLoadingForDrawables")
	public CharView(Context context) {
		super(context); //надо
		gfx = new Thread(this); //граф. поток
		upd = new UpdateThread(this); //поток обновлений
		sh = getHolder(); //холдер //не знаю почему, но если пытаться получить его после инициализации, то кидает ошибку, поэтому буду получать его здесь
		ctx = context; //контекст
		//настройки
		SharedPreferences sp = context.getSharedPreferences("saved", Context.MODE_PRIVATE); //настройки
		//номер персонажа
		character = (byte) sp.getInt("char", 0);
		//получаем изображение (Bitmap)
		image = getImage();
		//параметр движения по синусу
		sin_m = sp.getBoolean("sm", true);

		//делаем фон прозрачным
		getHolder().setFormat(PixelFormat.TRANSPARENT);
		setBackgroundColor(Color.TRANSPARENT);

		//говорим что все загрузилось
		loaded = true;

		//запускаем потоки
		upd.start();
		gfx.start();
	}

	//для изменения размеров изображения
	public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = createBitmap(
				bm, 0, 0, width, height, matrix, false);
		bm.recycle();
		return resizedBitmap;
	}

	//обновляет картинку персонажа
	void update() {
		if (MainActivity.e_char.equals("")) //если стандартный персонаж
			image = getImage();
		else { //и если кастомный
			SharedPreferences sp = FloatingArusu.instance.getSharedPreferences("saved", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();
			editor.putString("e_char", MainActivity.e_char);
			editor.apply();
			MainActivity.e_char = "";
			update();
		}
	}

	//обновляет размеры
	void update(short w, short h) {
		this.w = w;
		this.h = h;
		amplitude = (short) (h / 32);
		image = getImage();
	}

	//для получения изображения персонажа
	private Bitmap getImage() {
		SharedPreferences sp = ctx.getSharedPreferences("saved", Context.MODE_PRIVATE);
		if (sp.getString("e_char", "").equals(""))
			return getResizedBitmap(
					BitmapFactory.decodeResource(ctx.getResources(), ids[sp.getInt("char", 0)]),
					w, h - amplitude * 2
			);
		else
			return getResizedBitmap(
					BitmapFactory.decodeFile(sp.getString("e_char", "")),
					w, h - amplitude * 2
			);
	}

	//поток отрисовки
	@Override
	public void run() {
		//noinspection StatementWithEmptyBody
		while (!sh.getSurface().isValid()) ; //ждем пока поверхность рисования не будет готова
		while (!loaded) ; //ждем пока все загрузится
		//noinspection InfiniteLoopStatement
		while (true) { //цикл отрисовки
			canvas = sh.lockCanvas(); //получаем холст
			if (canvas == null)
				continue; //чтобы не вызвать NullPointerException

			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //стираем все

			canvas.drawBitmap(
					image, //изображение перонажа
					0, //x смещение
					t, //y смещение
					p //рисовалка (в ней все эффекты)
			); //рисуем изображение

			sh.unlockCanvasAndPost(canvas); //выкладываем результат на экран
		}
	}

	//класс потока обновлений
	private static class UpdateThread extends Thread {
		//отсюда берутся значения
		private final CharView parent;
		//для молний
		ColorMatrix cm = new ColorMatrix();
		Paint[] paints = new Paint[100];
		byte idx = 0;

		{
			for (byte i = 0; i < 100; i++) {
				int a = getRandom(), b = getRandom(), c = getRandom();
				float[] cmdt = new float[]{
						1 + a / 160f, b / 120f, c / 180f, 0, 0,
						(a + b) / 160f, 1, (b + c) / 160f, 0, 0,
						(a + c) / 120f, (a - b) / 80f, 1, 0, 0,
						0, 0, 0, 1, 0
				};
				cm.set(cmdt);
				ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
				paints[i] = new Paint();
				paints[i].setColorFilter(filter);
			}
		}

		//конструктор
		UpdateThread(CharView parent) {
			this.parent = parent;
		}

		private int getRandom() {
			return new Random().nextInt(30 - -10 + 1) + -10;
		}

		//выполнение потока
		@Override
		public void run() {
			super.run();
			//время окончания цикла //end time
			long et = System.currentTimeMillis();
			//разница времени (начало цикла)(n+1) - (конец цикла)(n) //delta time
			long dt;
			//общее время //time
			long t = 0l;
			//noinspection InfiniteLoopStatement
			while (true) { //вечный цикл
				if (parent.sin_m) { //для движения по синусу
					//находим dt
					dt = System.currentTimeMillis() - et;
					//прибавляем разницу времени к общему времени
					t += dt;
					//вычисляем координату по y через формулу
					// y = sin(t/240) * A + 3 + A
					//здесь t - общее время, A - амплитуда колебаний, а 3 это число сдвига для того, чтобы изображение точно не вышло за пределы окна
					parent.t = (int) (sin(t / 240d) * parent.amplitude + 3 + parent.amplitude);
				}
				if (parent.light) { //для молний
					parent.p = paints[idx++];
					idx %= 100;
				} else parent.p = parent.default_paint; //чтобы сбросить эффект
				et = System.currentTimeMillis();
			}
		}
	}
}

//собственно сервис
public class Overlay extends Service {
	final static SoundPool sp = new SoundPool.Builder().setAudioAttributes(
			new AudioAttributes.Builder() //билдер
					.setUsage(AudioAttributes.USAGE_GAME) //game лучше всех работает
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH) //поставил так, не знаю что лучше
					.build()
	).build(); //эта штука будет играть звук
	private static final int track; //надо
	@SuppressLint("StaticFieldLeak")
	public static Overlay now; //костыль
	volatile static boolean reset_bluetooth, clear_memory, play_nyaa; //параметры
	static Timer timer; //таймер, который будет работать каждые 15 секунд

	static {
		AssetFileDescriptor afd = FloatingArusu.getStaticResources().openRawResourceFd(R.raw.nyaa);
		track = sp.load(afd, 0);
	} //получаем звук

	final CharView overlay = new CharView(FloatingArusu.instance); //наш view с персонажем
	LinearLayout main; //основа окна
	volatile int LAYOUT_FLAG; //системный флаг окна
	volatile short half_of_width; //половина высоты
	WindowManager.LayoutParams params; //параметры окна
	volatile boolean showing_overlay = true; //флаг показа оверлея
	Stack<Pair<Short, Short>> updates = new Stack<>(); //сюда пихаются все обновления, пока окно скрыто
	private WindowManager wm; //менеджер окон (для того, чтобы попросить андроид показать окно)

	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
		} else {
			LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
		}
		params = new WindowManager.LayoutParams(
				128,
				160,
				LAYOUT_FLAG,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT
		);
	} //здесь получается флаг окна, зависящий от версии Android и задаются начальные координаты

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	} //требуется джавой

	public void setWindow(boolean v) {
		if (v)
			wm.addView(main, params);
		else
			wm.removeView(main);
		showing_overlay = v;
		if (v)
			checkUpdates();
	} // скрыть/показать окно

	public void checkUpdates() {
		if (updates.empty())
			return;
		if (!showing_overlay)
			return;
		Pair<Short, Short> p = updates.pop();
		updates.clear();
		setSizes(p.first, p.second);
	} //проверка обновлений

	//вызывается при создании сервиса
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onCreate() {
		super.onCreate();
		if (now != null)
			return; //чтобы не создавать его второй раз

		now = this; //чтобы не создался другой сервис
		wm = (WindowManager) getSystemService(WINDOW_SERVICE); //получаем системный менеджер окон

		main = new LinearLayout(this); //основа окна
		params.gravity = 51;
		params.x = 100;
		params.y = 100;

		//загружаем настройки
		SharedPreferences shpr = FloatingArusu.instance.getSharedPreferences("saved", Context.MODE_PRIVATE);
		play_nyaa = shpr.getBoolean("ns", true);
		reset_bluetooth = shpr.getBoolean("bto", true);
		clear_memory = shpr.getBoolean("cm", true);
		showing_overlay = shpr.getBoolean("so", true);

		//при нажатии на персонажа
		overlay.setOnTouchListener(new View.OnTouchListener() {
			volatile boolean ud = false; //чтобы знать нужно ли играть звук
			volatile int rsx, rsy, sx, sy; //координаты: относительно экрана и относительно окна

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE) { //если движение
					int x = (int) event.getRawX(); //
					int y = (int) event.getRawY(); // получаем координаты
					if (Math.sqrt((rsx - x) * (rsx - x) + (rsy - y) * (rsy - y)) < params.width >> 3)
						return true; //проверка на то, насколько сильно сдвинули
					params.x = x - sx; //это если достаточно сдвинули
					params.y = y - sy; //т. е. будет двигаться окно
					wm.updateViewLayout(main, params);  //обновляем окно
					ud = false; //звук играть не надо
				} else if (event.getAction() == MotionEvent.ACTION_DOWN) { //движение началось
					ud = true; //звук играть надо, но при ACTION_MOVE этот флаг может измениться
					rsx = (int) event.getRawX();
					rsy = (int) event.getRawY();
					sx = (int) event.getX();
					sy = (int) event.getY();
				} else if ((event.getAction() == MotionEvent.ACTION_UP) && ud) { //если движение окончено и не было сдвига окна
					if (play_nyaa) //если играть звук
						sp.play(track, 1, 1, 1, 0, 1); //то играет звук

					//отключение Bluetooth, если надо
					if (reset_bluetooth)
						try {
							BluetoothManager b_manager = (BluetoothManager) getBaseContext().getSystemService(BLUETOOTH_SERVICE);
							b_manager.getAdapter().disable();
						} catch (Exception e) {
							MainActivity.now.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 0);
						}

					//чистим память, если надо
					if (clear_memory)
						Runtime.getRuntime().gc();
				}
				return true;
			}
		});

		//добавляем персонажа в окно
		main.addView(overlay, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
		));

		//показываем окно
		wm.addView(main, params);

		//ставим сохраненные размеры
		SharedPreferences sp = getSharedPreferences("saved", Context.MODE_PRIVATE);
		Overlay.now.setSizes((short) (128f * ((float) (sp.getInt("size", 20) + 20) / 50f)), (short) (160f * ((float) (sp.getInt("size", 20) + 20) / 50f)));

		//очистка памяти каждые 15 секунд
		timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				System.gc(); //она в принципе не обязательна, пусть андроид сам решает
			}
		};
		timer.schedule(timerTask, 15000, 15000); //каждые 15 секунд будет срабатывать
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		startService(new Intent(this, Overlay.class)); //попытка перезапуска
	}

	public void setSizes(short w, short h) {
		params.width = w;
		params.height = h;
		half_of_width = (short) (w / 2);

		overlay.update(w, h);

		if (showing_overlay)
			wm.updateViewLayout(main, params);
		else
			updates.push(new Pair<>(w, h));
	} //чтобы менять размеры
}