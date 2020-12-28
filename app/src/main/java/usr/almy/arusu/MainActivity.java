package usr.almy.arusu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.loader.content.CursorLoader;

import java.util.Objects;

//основная активность
public class MainActivity extends Activity {
	//ее статичный экземпляр для обращения //это как бы говнокод, но пусть будет
	static MainActivity now;
	static String e_char;

	@SuppressLint({"ResourceType", "SetTextI18n"})
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) { //при создании
		super.onCreate(savedInstanceState); //надо
		now = this; //говнокод
		if (checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, 0);
		}
		startService(new Intent(this, Overlay.class)); //запуск сервиса
		//это подготовка
		LinearLayout main__ = new LinearLayout(this);
		ScrollView main_ = new ScrollView(this);
		main_.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		LinearLayout main = new LinearLayout(this);
		main.setPadding(4, 8, 4, 0);
		main.setOrientation(LinearLayout.VERTICAL);
		main.setGravity(Gravity.CENTER_HORIZONTAL);

		//сохраненные настройки
		SharedPreferences sp = getSharedPreferences("saved", Context.MODE_PRIVATE);

		//красивый шрифт
		Typeface dalsp = Typeface.createFromAsset(getAssets(), "dalsp.ttf");

		e_char = sp.getString("e_char", "");

		//вкл/выкл оверлей
		CheckBox so = new CheckBox(this);
		so.setTextSize(24);
		so.setTypeface(dalsp);
		so.setText(getString(R.string.overlay_show));
		so.setTextColor(0xffe3e3e3);
		so.setGravity(Gravity.CENTER);
		so.setChecked(true);
		so.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Overlay.now.setWindow(isChecked);
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean("so", isChecked);
			editor.apply();
		});
		so.setChecked(sp.getBoolean("so", true));

		//текст
		TextView tv = new TextView(this);
		tv.setTextSize(24);
		tv.setTypeface(dalsp);
		tv.setText(getString(R.string.size));
		tv.setTextColor(0xffe3e3e3);
		tv.setGravity(Gravity.CENTER);

		//управление размером
		SeekBar size = new SeekBar(this);
		size.setMax(220);
		size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			//надо
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

			}

			//надо
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			//когда изменили seekbar
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int progress = seekBar.getProgress();
				Overlay.now.setSizes((short) (128f * ((float) (progress + 20) / 50f)), (short) (160f * ((float) (progress + 20) / 50f)));
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt("size", progress);
				editor.apply(); //для сохранения настроек
			}
		});

		//вкл/выкл молнии
		CheckBox light = new CheckBox(this);
		light.setText(getString(R.string.lightning));
		light.setTypeface(dalsp);
		light.setOnCheckedChangeListener((buttonView, isChecked) -> Overlay.now.overlay.light = isChecked);
		light.setGravity(Gravity.CENTER);
		light.setTextSize(24);

		//настройка выключения блютуза
		CheckBox bto = new CheckBox(this);
		bto.setText(

				getString(R.string.bt_setting));
		bto.setOnCheckedChangeListener((buttonView, isChecked) ->

		{
			Overlay.reset_bluetooth = isChecked;
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean("bto", isChecked);
			editor.apply();
		});
		bto.setChecked(sp.getBoolean("bto", true));

		//настройка очистки памяти
		CheckBox cm = new CheckBox(this);
		cm.setText(

				getString(R.string.mc_setting));
		cm.setOnCheckedChangeListener((buttonView, isChecked) ->

		{
			Overlay.clear_memory = isChecked;
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean("cm", isChecked);
			editor.apply();
		});
		cm.setChecked(sp.getBoolean("cm", true));

		//настройка звука
		CheckBox ns = new CheckBox(this);
		ns.setText(

				getString(R.string.ns_setting));
		ns.setOnCheckedChangeListener((buttonView, isChecked) ->

		{
			Overlay.play_nyaa = isChecked;
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean("ns", isChecked);
			editor.apply();
		});
		ns.setChecked(sp.getBoolean("ns", true));

		//настройка движения по синусу
		CheckBox sm = new CheckBox(this);
		sm.setText(

				getString(R.string.sm_setting));
		sm.setChecked(sp.getBoolean("sm", true));
		sm.setOnCheckedChangeListener((buttonView, isChecked) -> {
			Overlay.now.overlay.sin_m = isChecked;
			SharedPreferences.Editor editor = sp.edit();
			editor.putBoolean("sm", isChecked);
			editor.apply();
		});

		//имена всех встроенных персонажей
		String[] names = new String[]{
				getString(R.string.Maria),
				getString(R.string.ITohka),
				getString(R.string.Tohka),
				getString(R.string.Yoshino),
				getString(R.string.Kotori),
				getString(R.string.Kurumi),
				getString(R.string.Origami),
				getString(R.string.IOrigami)
		};
		//их айдишники в ресурсах
		int[] ids = new int[]{
				R.drawable.maria, R.drawable.i_tohka,
				R.drawable.tohka, R.drawable.yoshino,
				R.drawable.kotori, R.drawable.kurumi,
				R.drawable.origami, R.drawable.i_origami
		};

		//для выбора персонажа
		Spinner character = new Spinner(this, Spinner.MODE_DIALOG);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		character.setAdapter(adapter);
		character.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				Overlay.now.overlay.character = (byte) pos;
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt("char", pos);
				editor.putString("e_char", "");
				editor.apply();
				Overlay.now.overlay.update();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				int spos = sp.getInt("char", 0);
				Overlay.now.overlay.character = (byte) ids[spos];
				SharedPreferences.Editor editor = sp.edit();
				editor.putString("e_char", "");
				editor.apply();
				Overlay.now.overlay.update();
			}
		});
		int spos = sp.getInt("char", 0);
		character.setSelection(spos);

		TextView opacity_t = new TextView(this);
		opacity_t.setText(getString(R.string.opacity));
		opacity_t.setTextSize(24);
		opacity_t.setTypeface(dalsp);
		opacity_t.setTextColor(0xffe3e3e3);
		opacity_t.setGravity(Gravity.CENTER);
		SeekBar opacity = new SeekBar(this);
		opacity.setMax(70);
		opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			//надо
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

			}

			//надо
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			//когда изменили seekbar
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int progress = seekBar.getProgress() + 30 - 1;
				SharedPreferences.Editor editor = sp.edit();
				editor.putInt("opacity", progress);
				editor.apply();
				Overlay.now.overlay.alpha = progress * 256 / 100;
			}
		});
		int opacity_ = sp.getInt("opacity", 70);
		opacity.setProgress(opacity_);

		//чтобы ставить свою картинку
		Button cc = new Button(this);
		cc.setOnClickListener(v ->
		{
			Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
			chooseFile.setType("image/png"); //нам нужны только png
			chooseFile = Intent.createChooser(chooseFile, "Choose picture");
			startActivityForResult(chooseFile, 1); //ждем выбора
		});
		cc.setText(

				getString(R.string.custom_char));

		//на всякий случай
		main__.setBackgroundColor(0xff2f2f2f);
		main_.setBackgroundColor(0xff2f2f2f);
		main.setBackgroundColor(0xff2f2f2f);

		//добавляем все, что создали выше
		main.addView(so);
		main.addView(sm);
		main.addView(tv);
		main.addView(size);
		main.addView(opacity_t);
		main.addView(opacity);
		main.addView(light);
		main.addView(character);
		main.addView(bto);
		main.addView(cm);
		main.addView(ns);
		main.addView(cc);

		//встраиваем это
		main_.addView(main);
		main__.addView(main_);

		//и отображаем
		setContentView(main__);
	}

	//дождались выбора изображения
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == -1) {
				//меняем изображение
				SharedPreferences sp = getSharedPreferences("saved", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sp.edit();
				editor.putString("e_char", getRealPathFromURI(data.getData()));
				editor.apply();
				Overlay.now.overlay.update();
			}
		}
	}

	//андроид возвращает uri, который нельзя давать в File, поэтому я нашел в инете метод для конвертации uri в нормальный путь файла
	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = {MediaStore.Images.Media.DATA};

		CursorLoader cursorLoader = new CursorLoader(
				this,
				contentUri, proj, null, null, null);
		Cursor cursor = cursorLoader.loadInBackground();

		int column_index =
				Objects.requireNonNull(cursor).getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}
}