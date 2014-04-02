package com.d2js.weixin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.Time;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.d2js.util.UncaughtExceptionHandler;

@SuppressWarnings("deprecation")
public class Main extends Activity implements SensorEventListener {

	public static Main instance = null;

	private PopupWindow setting = null;
	private PopupWindow confirm = null;

	private AudioManager audioManager = null;
	private SensorManager sensorManager = null;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	private int audioMode = 0;
	private long backClick = 0;
	private boolean isVertical = false; // 垂直放置

	private SharedPreferences sharedPreferences;
	private UncaughtExceptionHandler ueHandler = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// 启动activity时不自动弹出软键盘
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		instance = this;

		ueHandler = new UncaughtExceptionHandler(this);
		Thread.setDefaultUncaughtExceptionHandler(ueHandler);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(32, "WakeLock");
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioMode = audioManager.getMode();

		sharedPreferences = this.getSharedPreferences("APP_SETTINGS",
				Context.MODE_PRIVATE);
		
		ImageView btnSetting = (ImageView)this.findViewById(R.id.header_setting);
		btnSetting.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Main.instance.showConfirmDialog("标题", "确认要关闭\n此对话框吗？");
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) { // 获取back键
			if (setting != null && setting.isShowing()) {
				setting.setFocusable(false);
				setting.dismiss();
			} else if (confirm != null && confirm.isShowing()) {
				confirm.setFocusable(false);
				confirm.dismiss();
			} else {
				Time now = new Time();
				now.setToNow();
				if (now.toMillis(true) > backClick + 3000) {
					Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
					backClick = now.toMillis(true);
				} else {
					this.finish();
				}
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU) { // 获取 Menu键
			if (setting != null && setting.isShowing()) {
				setting.setFocusable(false);
				setting.dismiss();
			} else {
				if (confirm != null && confirm.isShowing()) {
					confirm.setFocusable(false);
					confirm.dismiss();
				}
				showSettingDialog();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			showVideoPlayerDialog();
		}
		return false;
	}

	@Override
	protected void onResume() {
		// 注册传感器，第一个参数为监听器，第二个是传感器类型，第三个是延迟类型
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),// 距离感应器
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), // 方向传感器
				SensorManager.SENSOR_DELAY_NORMAL);

		super.onResume();
	}

	@Override
	protected void onPause() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);// 注销传感器监听
		}
		if (wakeLock.isHeld()) {
			wakeLock.setReferenceCounted(false);
			wakeLock.release();
		}

		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_PROXIMITY:
			onSensorProximity(event.values.clone());
			break;
		case Sensor.TYPE_ORIENTATION:
			onSensorOrientation(event.values.clone());
			break;
		default:
			break;
		}
	}

	// 方向判断
	private void onSensorOrientation(float[] values) {
		if (values != null) {
			// 当手机水平时，values[1]=0，手机顶部在上面时，values[1] >= -180
			isVertical = (values[1] < -90.0);
		}
	}

	// 距离传感器判断
	private void onSensorProximity(float[] values) {
		if (values != null) {
			// 贴近距离感应器的时候its[0]返回值为0.0，当手离开时返回1.0
			if (values[0] == 0.0) {// 贴近手机
				if (isVertical // 竖着举起
						&& !audioManager.isWiredHeadsetOn()) { // 没有带耳机
					if (audioMode == AudioManager.MODE_NORMAL) {
						audioManager.setMode(AudioManager.MODE_IN_CALL);
						audioMode = AudioManager.MODE_IN_CALL;
					}

					wakeLock.acquire();// 申请设备电源锁
				}
			} else {// 远离手机
				if (audioMode != AudioManager.MODE_NORMAL) {
					audioManager.setMode(AudioManager.MODE_NORMAL);
					audioMode = AudioManager.MODE_NORMAL;
					// Toast.makeText(this, "喇叭模式", Toast.LENGTH_LONG).show();
				}
				if (wakeLock.isHeld()) {
					wakeLock.setReferenceCounted(false);
					wakeLock.release();
				}
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	private void showVideoPlayerDialog() {
		Intent intent = new Intent (Main.instance, VideoPlayer.class);
		startActivity(intent);
	}

	private void showConfirmDialog(String title, String message) {
		if (confirm == null) {
			View layout = getLayoutInflater().inflate(R.layout.confirm_dialog,
					null);
			confirm = new PopupWindow(layout, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, true);
			confirm.update();
			confirm.setAnimationStyle(R.style.DialogTopBottom);
			confirm.getContentView().setFocusableInTouchMode(true);
			confirm.getContentView().setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Main.instance.dispatchKeyEvent(event);
					return true;
				}
			});

			Button btnConfirm = (Button) layout.findViewById(R.id.btnConfirm);
			btnConfirm.setClickable(true);
			btnConfirm.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirm.setFocusable(false);
					confirm.dismiss();
				}
			});

			Button btnCancel = (Button) layout.findViewById(R.id.btnCancel);
			btnCancel.setClickable(true);
			btnCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					confirm.setFocusable(false);
					confirm.dismiss();
				}
			});
		}

		TextView confirmTitle = (TextView) confirm.getContentView().findViewById(
				R.id.confirm_title);
		confirmTitle.setText(title);
		TextView confirmMessage = (TextView) confirm.getContentView()
				.findViewById(R.id.confirm_message);
		confirmMessage.setText(message);

		confirm.update();
		confirm.setFocusable(true);
		confirm.showAtLocation(this.findViewById(R.id.main), Gravity.CENTER, 0,
				0);
	}

	private void showSettingDialog() {
		if (setting == null) {
			View layout = getLayoutInflater().inflate(R.layout.setting_dialog,
					null);
			setting = new PopupWindow(layout, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, true);
			setting.setAnimationStyle(R.style.DialogRight);

			ToggleButton tbUseTraffic = (ToggleButton) setting.getContentView()
					.findViewById(R.id.toggleUseTraffic);
			tbUseTraffic.setChecked(sharedPreferences.getBoolean("UseTraffic",
					false));
			tbUseTraffic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton button, boolean value) {
					Main.instance.saveSettings("UseTraffic", value);
				}
			});

			ToggleButton tbAutoDownload = (ToggleButton) setting
					.getContentView().findViewById(R.id.toggleAutoDownload);
			tbAutoDownload.setChecked(sharedPreferences.getBoolean(
					"AutoDownload", true));
			tbAutoDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton button, boolean value) {
					Main.instance.saveSettings("AutoDownload", value);
				}
			});

			ToggleButton tbAutoDelete = (ToggleButton) setting.getContentView()
					.findViewById(R.id.toggleAutoDelete);
			tbAutoDelete.setChecked(sharedPreferences.getBoolean("AutoDelete",
					true));
			tbAutoDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton button, boolean value) {
					Main.instance.saveSettings("AutoDelete", value);
				}
			});

			setting.getContentView().setFocusableInTouchMode(true);
			setting.getContentView().setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Main.instance.dispatchKeyEvent(event);
					return true;
				}
			});

			Button back = (Button) layout.findViewById(R.id.btnBack);
			back.setClickable(true);
			back.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					setting.setFocusable(false);
					setting.dismiss();
				}
			});
		}
		setting.setFocusable(true);
		setting.update();
		setting.showAtLocation(this.findViewById(R.id.main), Gravity.RIGHT
				| Gravity.CENTER_VERTICAL, 0, 0);
	}

	public void saveSettings(String key, boolean value) {
		sharedPreferences.edit().putBoolean(key, value).apply();
	}
}
