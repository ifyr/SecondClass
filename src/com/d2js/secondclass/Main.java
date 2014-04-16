package com.d2js.secondclass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.format.Time;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.d2js.util.Constants;
import com.d2js.util.HttpUtility;
import com.d2js.util.MediaList;
import com.d2js.util.MediaItemData;
import com.d2js.util.PreferenceUtility;
import com.d2js.util.SystemUtility;
import com.d2js.util.UncaughtExceptionHandler;

@SuppressLint("Wakelock")
@SuppressWarnings("deprecation")
public class Main extends Activity implements SensorEventListener {

	public static Main instance = null;

	private static final int REQUESTCODE_VIDEOPLAYER = 0;

	public MediaItemData playingData = null;

	private PopupWindow setting = null;
	private PopupWindow confirm = null;
	private PopupWindow waiting = null;
	private ListView medialist = null;
	private MediaAdapter mediaAdapter = null;

	private AudioManager audioManager = null;
	private SensorManager sensorManager = null;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	private int audioMode = 0;
	private long lastBackTime = 0;
	private boolean isVertical = false; // ��ֱ����
	private boolean isLoadingData = false;
	private int statusPlaying = Constants.STATE_PLAYING_NONE;
	private int statusPopup = Constants.STATE_POPUP_NONE;

	private UncaughtExceptionHandler ueHandler = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// ����activityʱ���Զ����������
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		instance = this;

		ueHandler = new UncaughtExceptionHandler(this);
		// Thread.setDefaultUncaughtExceptionHandler(ueHandler);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(32, "WakeLock");
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioMode = audioManager.getMode();

		ImageView btnSetting = (ImageView) this
				.findViewById(R.id.header_setting);
		btnSetting.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Main.this.showConfirmDialog("����", "ȷ��Ҫ�ر�\n�˶Ի�����");
			}
		});

		medialist = (ListView) findViewById(R.id.main_list);

		View refreshView = findViewById(R.id.panel_refresh);
		refreshView.setClickable(true);
		refreshView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Main.this.onRefreshData();
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) { // Back��
			if (statusPopup == Constants.STATE_POPUP_NONE) {
				Time now = new Time();
				now.setToNow();
				if (now.toMillis(true) > lastBackTime + 3000) {
					Toast.makeText(this, "�ٰ�һ���˳�����", Toast.LENGTH_SHORT).show();
					lastBackTime = now.toMillis(true);
				} else {
					this.finish();
				}
			} else {
				hideSettingDialog();
				hideWaitingDialog();
				hideConfirmDialog();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU) { // Menu��
			if (statusPopup == Constants.STATE_POPUP_SETTING) {
				hideSettingDialog();
			} else {
				hideConfirmDialog();
				hideWaitingDialog();

				showSettingDialog();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_SEARCH) { // Search��
			if (statusPopup == Constants.STATE_POPUP_WAITING) {
				hideWaitingDialog();
			} else {
				hideConfirmDialog();
				hideSettingDialog();

				onRefreshData();
			}
			return true;
		}
		return false;
	}

	@Override
	public void onResume() {
		// ע�ᴫ��������һ������Ϊ���������ڶ����Ǵ��������ͣ����������ӳ�����
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),// �����Ӧ��
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), // ���򴫸���
				SensorManager.SENSOR_DELAY_NORMAL);
		if (statusPlaying == Constants.STATE_PLAYING_AUDIO) {
			resumeAudioPlayer();
		}

		super.onResume();
		messageHandler.sendEmptyMessageDelayed(Constants.MSG_LOAD_ACTIVITY,
				1000);
	}

	@Override
	public void onPause() {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);// ע������������
		}
		if (wakeLock.isHeld()) {
			wakeLock.setReferenceCounted(false);
			wakeLock.release();
		}
		if (statusPlaying == Constants.STATE_PLAYING_AUDIO) {
			pauseAudioPlayer();
		}
		messageHandler.removeCallbacksAndMessages(null);

		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		this.finish();
	}

	@Override
	public void onDestroy() {
		messageHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
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

	// �����ж�
	private void onSensorOrientation(float[] values) {
		if (values != null) {
			// ���ֻ�ˮƽʱ��values[1]=0���ֻ�����������ʱ��values[1] >= -180
			isVertical = (values[1] < -90.0);
		}
	}

	// ���봫�����ж�
	private void onSensorProximity(float[] values) {
		if (values != null) {
			// ���������Ӧ����ʱ��its[0]����ֵΪ0.0�������뿪ʱ����1.0
			if (values[0] == 0.0) {// �����ֻ�
				if (isVertical // ���ž���
						&& !audioManager.isWiredHeadsetOn()) { // û�д�����
					if (audioMode == AudioManager.MODE_NORMAL) {
						audioManager.setMode(AudioManager.MODE_IN_CALL);
						audioMode = AudioManager.MODE_IN_CALL;
					}

					wakeLock.acquire();// �����豸��Դ��
				}
			} else {// Զ���ֻ�
				if (audioMode != AudioManager.MODE_NORMAL) {
					audioManager.setMode(AudioManager.MODE_NORMAL);
					audioMode = AudioManager.MODE_NORMAL;
					// Toast.makeText(this, "����ģʽ", Toast.LENGTH_LONG).show();
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

	@Override
	// ��������غ��жϲ�ִ�в���
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == REQUESTCODE_VIDEOPLAYER) {
			if (resultCode == RESULT_OK) {
				int position = playingData.progress;
				if (position != 0) {
					// TODO ����״̬
				}
			}
			statusPlaying = Constants.STATE_PLAYING_NONE;
		}
	}

	protected void onRefreshData() {
		MediaList.UpdateToday();
		if (!MediaList.NeedLoad()) {
			Toast.makeText(this, "�����������ݣ�����ˢ��", Toast.LENGTH_SHORT).show();
			return;
		}

		if (SystemUtility.getNetworkStatus(this) == Constants.STATE_NETWORK_NONE) {
			Toast.makeText(this, "û�����磬�޷���ȡ��������", Toast.LENGTH_SHORT).show();
			return;
		}

		if (isLoadingData) {
			return;
		}
		isLoadingData = true;

		showWaitingDialog();
		messageHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				updateListData();
			}
		}, 500);
	}

	public void updateListData() {
		boolean loadSuccessed = true;
		while (loadSuccessed && MediaList.NeedLoad()) {
			loadSuccessed = MediaList.UpdateList(HttpUtility.SharedInstance()
					.getList(MediaList.ReadDate()));
		}
		messageHandler.sendEmptyMessage(Constants.MSG_LOAD_LISTDATA);
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_LOAD_ACTIVITY:
				onLoadActivity();
				break;
			case Constants.MSG_LOAD_LISTDATA:
				onLoadListData();
				break;
			case Constants.MSG_PLAY_AUDIO:
				showAudioPlayer((String) msg.obj, msg.arg1);
				break;
			case Constants.MSG_PLAY_VIDEO:
				showVideoPlayer((String) msg.obj, msg.arg1);
				break;
			case Constants.MSG_AUDIO_FINISH:
				closeAudioPlayer(true);
				break;
			case Constants.MSG_AUDIO_ERROR:
				Toast.makeText(Main.this, "������Ƶʧ��", Toast.LENGTH_SHORT).show();
				closeAudioPlayer(true);
				break;
			case Constants.MSG_FILE_PROGRESSED:
				onFileProgressed((MediaItemData) msg.obj, msg.arg1);
				break;
			case Constants.MSG_FILE_DOWNLOADED:
				onFileDownloaded((MediaItemData) msg.obj);
				break;
			case Constants.MSG_HTTP_ERROR:
				onFileAborted((MediaItemData) msg.obj);
				Toast.makeText(Main.this, "�ļ�����ʧ�ܣ���������", Toast.LENGTH_SHORT)
						.show();
				break;
			case Constants.MSG_DISK_ERROR:
				onFileAborted((MediaItemData) msg.obj);
				Toast.makeText(Main.this, "�Ҳ���SD�������ܻ����ļ�", Toast.LENGTH_SHORT)
						.show();
				break;
			case Constants.MSG_SPACE_ERROR:
				onFileAborted((MediaItemData) msg.obj);
				Toast.makeText(Main.this, "SD�����������ܻ����ļ�", Toast.LENGTH_SHORT)
						.show();
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};

	public Handler getHandler() {
		return messageHandler;
	}

	protected void onLoadActivity() {
		if (mediaAdapter == null) {
			mediaAdapter = new MediaAdapter(Main.this);
			medialist.setAdapter(mediaAdapter);
		}
	}

	protected void onLoadListData() {
		isLoadingData = false;
		hideWaitingDialog();

		if (MediaList.NeedLoad()) {
			Toast.makeText(Main.this, "��ȡ��������ʧ�ܣ���������", Toast.LENGTH_SHORT)
					.show();
		} else {
			Toast.makeText(Main.this, "�ɹ���ȡ��������", Toast.LENGTH_SHORT).show();
			mediaAdapter.notifyDataSetChanged();
			PreferenceUtility.sharedInstance().putString(
					Constants.PREFKEY_LIST, MediaList.MakeSaveString());
		}
	}

	protected void onFileProgressed(MediaItemData data, Integer progress) {
		//
	}

	protected void onFileDownloaded(MediaItemData data) {

	}

	protected void onFileAborted(MediaItemData data) {

	}

	private void showVideoPlayer(String date, int content) {
		MediaItemData toplay = new MediaItemData(date, content);
		if (toplay.media == null || toplay.media.isEmpty()) {
			return;
		}

		if (statusPlaying == Constants.STATE_PLAYING_AUDIO) {
			closeAudioPlayer(true);
		} else if (statusPlaying == Constants.STATE_PLAYING_VIDEO) {
			// �����ϲ����ڣ��������봦��
			return;
		}

		statusPlaying = Constants.STATE_PLAYING_VIDEO;
		playingData = toplay;

		Intent intent = new Intent(Main.instance, VideoPlayer.class);
		startActivityForResult(intent, REQUESTCODE_VIDEOPLAYER);
	}

	private void showAudioPlayer(String date, int content) {
		MediaItemData toplay = new MediaItemData(date, content);
		if (toplay.media == null || toplay.media.isEmpty()) {
			return;
		}

		if (statusPlaying == Constants.STATE_PLAYING_AUDIO) {
			// ���ڲ��ŵ����ݣ�������������
			if (playingData != null && playingData.match(date, content)) {
				return;
			}
			closeAudioPlayer(false);
		} else if (statusPlaying == Constants.STATE_PLAYING_VIDEO) {
			// �����ϲ����ڣ��������봦��
			return;
		}

		statusPlaying = Constants.STATE_PLAYING_AUDIO;
		playingData = toplay;

		AudioPlayer audioplayer = AudioPlayer.SharedInstance();
		LinearLayout footerlayout = (LinearLayout) findViewById(R.id.main_footer);

		View audioview = footerlayout.findViewById(R.id.audio_layout);
		if (audioview == null) {
			footerlayout.removeAllViews();
			audioplayer.createView(footerlayout);
			footerlayout.requestLayout();
		}
		audioplayer.play();
	}

	private void closeAudioPlayer(boolean hide) {
		AudioPlayer.SharedInstance().stop();
		// TODO ����״̬
		if (hide) {
			LinearLayout audiolayout = (LinearLayout) findViewById(R.id.main_footer);
			audiolayout.removeAllViewsInLayout();
			audiolayout.requestLayout();
		}

		playingData = null;
		statusPlaying = Constants.STATE_PLAYING_NONE;
	}

	private void pauseAudioPlayer() {
		AudioPlayer.SharedInstance().pause();
	}

	private void resumeAudioPlayer() {
		AudioPlayer.SharedInstance().resume();
	}

	private void showWaitingDialog() {
		if (waiting == null) {
			View layout = getLayoutInflater().inflate(R.layout.dialog_waiting,
					null);
			waiting = new PopupWindow(layout, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, true);

			waiting.getContentView().setFocusableInTouchMode(true);
			waiting.getContentView().setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Main.this.dispatchKeyEvent(event);
					return true;
				}
			});

			waiting.setOnDismissListener(new PopupWindow.OnDismissListener() {
				@Override
				public void onDismiss() {
					ImageView waitingImage = (ImageView) waiting
							.getContentView().findViewById(R.id.waiting_image);
					waitingImage.clearAnimation();
				}
			});
		}

		waiting.update();
		waiting.setFocusable(true);
		waiting.showAtLocation(this.findViewById(R.id.main), Gravity.CENTER, 0,
				0);

		Animation circling = AnimationUtils
				.loadAnimation(this, R.anim.circling);
		ImageView waitingImage = (ImageView) waiting.getContentView()
				.findViewById(R.id.waiting_image);
		waitingImage.startAnimation(circling);

		statusPopup = Constants.STATE_POPUP_WAITING;
	}

	private void hideWaitingDialog() {
		if (waiting != null && waiting.isShowing()) {
			waiting.setFocusable(false);
			waiting.dismiss();

			statusPopup = Constants.STATE_POPUP_NONE;
		}
	}

	private void showConfirmDialog(String title, String message) {
		if (confirm == null) {
			View layout = getLayoutInflater().inflate(R.layout.dialog_confirm,
					null);
			confirm = new PopupWindow(layout, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, true);
			// confirm.setAnimationStyle(R.style.AnimTopBottom);
			confirm.getContentView().setFocusableInTouchMode(true);
			confirm.getContentView().setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Main.this.dispatchKeyEvent(event);
					return true;
				}
			});

			Button btnConfirm = (Button) layout.findViewById(R.id.btnConfirm);
			btnConfirm.setClickable(true);
			btnConfirm.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hideConfirmDialog();
				}
			});

			Button btnCancel = (Button) layout.findViewById(R.id.btnCancel);
			btnCancel.setClickable(true);
			btnCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hideConfirmDialog();
				}
			});
		}

		TextView confirmTitle = (TextView) confirm.getContentView()
				.findViewById(R.id.confirm_title);
		confirmTitle.setText(title);
		TextView confirmMessage = (TextView) confirm.getContentView()
				.findViewById(R.id.waiting_message);
		confirmMessage.setText(message);

		confirm.update();
		confirm.setFocusable(true);
		confirm.showAtLocation(this.findViewById(R.id.main), Gravity.CENTER, 0,
				0);
		statusPopup = Constants.STATE_POPUP_CONFIRM;
	}

	private void hideConfirmDialog() {
		if (confirm != null && confirm.isShowing()) {
			confirm.setFocusable(false);
			confirm.dismiss();

			statusPopup = Constants.STATE_POPUP_NONE;
		}
	}

	private void showSettingDialog() {
		if (setting == null) {
			View layout = getLayoutInflater().inflate(R.layout.dialog_setting,
					null);
			setting = new PopupWindow(layout, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, true);
			// setting.setAnimationStyle(R.style.AnimRight);

			PreferenceUtility preferUtil = PreferenceUtility.sharedInstance();
			ToggleButton tbUseTraffic = (ToggleButton) setting.getContentView()
					.findViewById(R.id.toggleUseTraffic);
			tbUseTraffic.setChecked(preferUtil.getBoolean("UseTraffic", false));
			tbUseTraffic
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton button,
								boolean value) {
							PreferenceUtility.sharedInstance().putBoolean(
									"UseTraffic", value);
						}
					});

			ToggleButton tbAutoDownload = (ToggleButton) setting
					.getContentView().findViewById(R.id.toggleAutoDownload);
			tbAutoDownload.setChecked(preferUtil.getBoolean("AutoDownload",
					true));
			tbAutoDownload
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton button,
								boolean value) {
							PreferenceUtility.sharedInstance().putBoolean(
									"AutoDownload", value);
						}
					});

			ToggleButton tbAutoDelete = (ToggleButton) setting.getContentView()
					.findViewById(R.id.toggleAutoDelete);
			tbAutoDelete.setChecked(preferUtil.getBoolean("AutoDelete", true));
			tbAutoDownload
					.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton button,
								boolean value) {
							PreferenceUtility.sharedInstance().putBoolean(
									"AutoDelete", value);
						}
					});

			setting.getContentView().setFocusableInTouchMode(true);
			setting.getContentView().setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Main.this.dispatchKeyEvent(event);
					return true;
				}
			});

			Button back = (Button) layout.findViewById(R.id.btnBack);
			back.setClickable(true);
			back.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hideSettingDialog();
				}
			});
		}
		setting.update();
		setting.setFocusable(true);
		setting.showAtLocation(this.findViewById(R.id.main), Gravity.CENTER, 0,
				0);
		statusPopup = Constants.STATE_POPUP_SETTING;
	}

	private void hideSettingDialog() {
		if (setting != null && setting.isShowing()) {
			setting.setFocusable(false);
			setting.dismiss();

			statusPopup = Constants.STATE_POPUP_NONE;
		}
	}
}
