package com.d2js.secondclass;

import com.d2js.util.ApiUtility;
import com.d2js.util.ProgressUtility;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

@SuppressWarnings("deprecation")
public class VideoPlayer extends Activity implements OnCompletionListener,
		OnErrorListener, OnSeekCompleteListener, OnSeekBarChangeListener {

	private AudioManager audioManager = null;
	private int originalVolume = 0;
	private int originalMaxVolume = 0;

	// 更新进度条的刻度线程
	private Thread updateProgressThread = null;

	private final Handler messageHandler = new Handler();
	private AudioBecomingNoisyReceiver audioBecomingNoisyReceiver = null;

	private VideoView videoView = null;
	private String mediaPath = null;
	private ProgressBar seekBar = null;
	private Button btnPausePlay = null;
	private boolean isPlaying = false; // 控制更新播放进度条
	private boolean isPaused = false; // 控制界面恢复
	private boolean isSeeking = false; // 拖动进度条时不根据播放更新进度条
	private int progress = 0;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setSystemUiVisibility(View view) {
		if (ApiUtility.HAS_SET_SYSTEM_UI_VISIBILITY) {
			view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.STATUS_BAR_HIDDEN | View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		final Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
		winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
		win.setAttributes(winParams);

		// We set the background in the theme to have the launching animation.
		// But for the performance (and battery), we remove the background here.
		win.setBackgroundDrawable(null);

		setContentView(R.layout.activity_videoplayer);
		View root = findViewById(R.id.videoplayer_window);
		setSystemUiVisibility(root);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		originalVolume = audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		originalMaxVolume = audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
				originalMaxVolume, 0);

		audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
		audioBecomingNoisyReceiver.register();

		seekBar = (ProgressBar) findViewById(R.id.video_seekbar);
		// seekBar.setOnSeekBarChangeListener(this);

		btnPausePlay = (Button) findViewById(R.id.video_control);
		btnPausePlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (videoView.isPlaying()) {
					pause();
				} else {
					resume();
				}
			}
		});

		Button btnBack = (Button) findViewById(R.id.btn_video_back);
		btnBack.setClickable(true);
		btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				VideoPlayer.this.finish();
			}
		});

		TextView title = (TextView) findViewById(R.id.video_name);
		title.setText(Main.instance.playingData.title);

		seekBar.setProgress(0);

		this.isPlaying = true;
		this.isSeeking = false;
		this.isPaused = false;

		videoView = (VideoView) findViewById(R.id.video_surface);
		videoView.setOnErrorListener(this);
		videoView.setOnCompletionListener(this);
		videoView.setKeepScreenOn(true);
		videoView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				showController();
				return true;
			}
		});

		mediaPath = Main.instance.playingData.path;
		if (mediaPath == null || mediaPath.isEmpty()
				|| Main.instance.playingData.download < 100) {
			mediaPath = Main.instance.playingData.media;
			videoView.setVideoURI(Uri.parse(mediaPath));
		} else {
			videoView.setVideoPath(mediaPath);
		}

		this.progress = Main.instance.playingData.progress;

		begin();
	}

	@Override
	public void onStart() {
		((AudioManager) getSystemService(AUDIO_SERVICE)).requestAudioFocus(
				null, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		super.onStart();
	}

	@Override
	public void onPause() {
		pause();

		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();

		resume();
	}

	@Override
	protected void onStop() {
		((AudioManager) getSystemService(AUDIO_SERVICE))
				.abandonAudioFocus(null);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		stop();
		audioBecomingNoisyReceiver.unregister();
		super.onDestroy();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		isSeeking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// 当进度条停止修改的时候触发
		progress = seekBar.getProgress();
		videoView.seekTo(progress);
		isSeeking = false;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		this.progress = progress;
		updatePlayingProgress();
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		progress = videoView.getCurrentPosition();
		Main.instance.playingData.progress = progress;
		updatePlayingProgress();
		if (progress == seekBar.getMax()) {
			Main.instance.playingData.progress = 0;
			finish();
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		this.progress = 0;
		finish();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		finish();
		return false;
	}

	public void updateView() {
		Display display = getWindowManager().getDefaultDisplay();
		int videoWidth = videoView.getWidth();
		int videoHeight = videoView.getHeight();

		float heightRatio = (float) videoHeight / (float) display.getHeight();
		float widthRatio = (float) videoWidth / (float) display.getWidth();
		if (heightRatio > widthRatio) {
			videoHeight = (int) Math.ceil((float) videoHeight / heightRatio);
			videoWidth = (int) Math.ceil((float) videoWidth / heightRatio);
		} else {
			videoHeight = (int) Math.ceil((float) videoHeight / widthRatio);
			videoWidth = (int) Math.ceil((float) videoWidth / widthRatio);
		}
		videoView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth,
				videoHeight));

		seekBar.setMax(videoView.getDuration());

		isPlaying = true;
		isPaused = false;
		isSeeking = false;

		updateProgressThread = new Thread() {
			@Override
			public void run() {
				try {
					while (isPlaying) {
						if (!isPaused && !isSeeking && videoView.isPlaying()) {
							progress = videoView.getCurrentPosition();
							updatePlayingProgress();
						}
						sleep(1000);
					}
				} catch (InterruptedException e) {
				}
			}
		};
		updateProgressThread.start();

		toggleButton(true);
		messageHandler.postDelayed(hideControllerThread, 4000);
	}

	private final Runnable playingChecker = new Runnable() {
		@Override
		public void run() {
			if (videoView.isPlaying()) {
				updateView();
			} else {
				messageHandler.postDelayed(playingChecker, 200);
			}
		}
	};

	public void updatePlayingProgress() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateProgress();
			}
		});
	}

	public void updateProgress() {
		Log.v("VideoPlayer", "[Stop Tracking Touch] progress: " + progress
				+ " SeekBar Progress: " + seekBar.getProgress()
				+ " VideoView Progress: " + videoView.getCurrentPosition());

		int duration = seekBar.getMax();
		TextView played = (TextView) findViewById(R.id.video_timerplayed);
		played.setText(ProgressUtility.milliSecondsToTimer(progress));

		TextView rest = (TextView) findViewById(R.id.video_timerrest);
		rest.setText("-"
				+ ProgressUtility.milliSecondsToTimer(duration - progress));

		this.seekBar.setProgress(progress);
	}

	public void begin() {
		try {
			videoView.start();
			messageHandler.postDelayed(playingChecker, 200);
		} catch (Exception ex) {
			finish();
		}
	}

	public void pause() {
		if (videoView != null) {
			videoView.pause();
			isPaused = true;
			toggleButton(false);
		}
	}

	public void resume() {
		if (videoView != null) {
			videoView.start();
			isPaused = false;
			toggleButton(true);
		}
	}

	public void stop() {
		if (videoView != null && isPlaying) {
			if (videoView.isPlaying()) {
				videoView.stopPlayback();
			}
			isPlaying = false;
			if (updateProgressThread != null) {
				try {
					updateProgressThread.join(1000);
				} catch (InterruptedException e) {
				}
				updateProgressThread = null;
			}
		}
	}

	@Override
	public void finish() {
		stop();

		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume,
				0);

		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		super.finish();
	}

	protected void toggleButton(boolean playing) {
		btnPausePlay.setBackgroundResource(playing ? R.drawable.btn_pause
				: R.drawable.btn_play);
	}

	private Runnable hideControllerThread = new Runnable() {
		public void run() {
			findViewById(R.id.video_toppanel).setVisibility(View.GONE);
			findViewById(R.id.video_bottompanel).setVisibility(View.GONE);
		}
	};

	private void showController() {
		if (findViewById(R.id.video_bottompanel).getVisibility() == View.VISIBLE) {
			messageHandler.removeCallbacks(hideControllerThread);
		} else {
			findViewById(R.id.video_toppanel).setVisibility(View.VISIBLE);
			findViewById(R.id.video_bottompanel).setVisibility(View.VISIBLE);
		}
		messageHandler.postDelayed(hideControllerThread, 4000);
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();
		showController();
	}

	// We want to pause when the headset is unplugged.
	private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
		public void register() {
			VideoPlayer.this.registerReceiver(this, new IntentFilter(
					AudioManager.ACTION_AUDIO_BECOMING_NOISY));
		}

		public void unregister() {
			VideoPlayer.this.unregisterReceiver(this);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (videoView.isPlaying()) {
				pause();
			}
		}
	}
}
