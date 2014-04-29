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
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class VideoPlayer extends Activity implements OnBufferingUpdateListener, OnCompletionListener,
		OnErrorListener, OnInfoListener, OnPreparedListener, 
		OnSeekCompleteListener, OnVideoSizeChangedListener,
		OnSeekBarChangeListener, SurfaceHolder.Callback {

	private AudioManager audioManager = null;
	private int originalVolume = 0;
	private int originalMaxVolume = 0;
	
	private AudioBecomingNoisyReceiver audioBecomingNoisyReceiver = null;

	private SurfaceView surfaceView = null;
	private SurfaceHolder surfaceHolder = null;
	private MediaPlayer mediaPlayer = null;
	private String mediaPath = null;
	private SeekBar seekBar = null;
	private Button btnPausePlay = null;
	private boolean isPlaying = false; // 控制更新播放进度条
	private boolean isPaused = false; // 控制界面恢复
	private boolean isSeeking = false; // 拖动进度条时不根据播放更新进度条
	private boolean seekComplete = true; // 防止拖动进度条后被onComplete中止
	private int progress = 0;
	private Handler hideHandler = null;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void setSystemUiVisibility(View view) {
		if (ApiUtility.HAS_SET_SYSTEM_UI_VISIBILITY) {
			view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	                | View.STATUS_BAR_HIDDEN
	                | View.SYSTEM_UI_FLAG_FULLSCREEN
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
		originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		originalMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMaxVolume, 0);
		
		hideHandler = new Handler();
		
		audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
	    audioBecomingNoisyReceiver.register();

		seekBar = (SeekBar) findViewById(R.id.video_seekbar);
		seekBar.setOnSeekBarChangeListener(this);

		btnPausePlay = (Button) findViewById(R.id.video_control);
		btnPausePlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer == null) {
					return;
				}
				if (mediaPlayer.isPlaying()) {
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
		this.seekComplete = true;
		this.isPaused = false;

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		mediaPlayer.setOnInfoListener(this);

		surfaceView = (SurfaceView) findViewById(R.id.video_surface);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mediaPlayer.setScreenOnWhilePlaying(true);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		this.mediaPath = Main.instance.playingData.path;
		if (mediaPath == null || mediaPath.isEmpty()
				|| Main.instance.playingData.download < 100) {
			mediaPath = Main.instance.playingData.media;
		}

		this.progress = Main.instance.playingData.progress;

		begin();
	}

	@Override
    public void onStart() {
        ((AudioManager) getSystemService(AUDIO_SERVICE))
                .requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        super.onStart();
    }

	@Override
	public void onPause() {
		super.onPause();

		this.finish();
	}
	
	@Override
	public void onResume() {
		super.onResume();
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
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}

        audioBecomingNoisyReceiver.unregister();
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mediaPlayer.setDisplay(surfaceHolder);// 若无此句，将只有声音而无图像
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int duration = seekBar.getMax();
		TextView played = (TextView) findViewById(R.id.video_timerplayed);
		played.setText(ProgressUtility.milliSecondsToTimer(progress));

		TextView rest = (TextView) findViewById(R.id.video_timerrest);
		rest.setText("-"
				+ ProgressUtility.milliSecondsToTimer(duration - progress));

		Log.v("VideoPlayer", "[Progress Changed] progress: " + progress
				+ " SeekBar Progress: " + seekBar.getProgress()
				+ " SeekBar SecondProgress: " + seekBar.getSecondaryProgress()
				+ " MediaPlayer Progress: " + mediaPlayer.getCurrentPosition()
				+ " fromUser: " + fromUser);
		if (this.mediaPath.equals(Main.instance.playingData.path)) {
			if (fromUser) {
				this.mediaPlayer.seekTo(progress);
			} else {
				this.seekBar.setProgress(progress);
			}
			this.progress = progress;
			Main.instance.playingData.progress = progress;
		} else {
			if (fromUser) {
				int secProgress = seekBar.getSecondaryProgress();
				if (secProgress > progress) {
					this.progress = progress;
					Main.instance.playingData.progress = progress;
					this.mediaPlayer.seekTo(progress);
				} else {
					seekBar.setProgress(this.seekBar.getProgress());
				}
			} else {
				this.progress = progress;
				Main.instance.playingData.progress = progress;
				this.seekBar.setProgress(progress);
			}
		}
		if (progress == duration) {
			Main.instance.playingData.progress = 0;
			finish();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		this.isSeeking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// 当进度条停止修改的时候触发
		Log.v("VideoPlayer", "[Stop Tracking Touch] progress: " + progress
				+ " SeekBar Progress: " + seekBar.getProgress()
				+ " SeekBar SecondProgress: " + seekBar.getSecondaryProgress()
				+ " MediaPlayer Progress: " + mediaPlayer.getCurrentPosition());
		// 取得当前进度条的刻度
		this.progress = seekBar.getProgress();
		if (mediaPlayer != null) {
			if (this.mediaPath.equals(Main.instance.playingData.path)) {
				// 设置当前播放的位置
				mediaPlayer.seekTo(progress);
				this.seekComplete = false;
			} else {
				this.seekBar.setProgress(this.progress);
			}
		}
		this.isSeeking = false;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		seekComplete = true;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (seekComplete) {
			this.progress = 0;
			finish();
		} else {
			seekComplete = true;
			mediaPlayer.start();
		}
	}
	
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int progress) {
		seekBar.setSecondaryProgress(progress);
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		Display display = getWindowManager().getDefaultDisplay();
		int videoWidth = mp.getVideoWidth();
		int videoHeight = mp.getVideoHeight();

		float heightRatio = (float) videoHeight / (float) display.getHeight();
		float widthRatio = (float) videoWidth / (float) display.getWidth();
		if (heightRatio > widthRatio) {
			videoHeight = (int) Math.ceil((float) videoHeight / heightRatio);
			videoWidth = (int) Math.ceil((float) videoWidth / heightRatio);
		} else {
			videoHeight = (int) Math.ceil((float) videoHeight / widthRatio);
			videoWidth = (int) Math.ceil((float) videoWidth / widthRatio);
		}
		surfaceView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth,
				videoHeight));

		mp.start();
		// 设置进度条的最大进度为视频流的最大播放时长
		seekBar.setMax(mp.getDuration());
		seekBar.setSecondaryProgress(0);
		isPaused = false;
		// 按照初始位置播放
		if (progress != 0) {
			Log.v("VideoPlayer", "[Prepared] progress: " + progress);
			mp.seekTo(progress);
		} else {
			mp.seekTo(0);
		}
		// 开始线程，更新进度条的刻度
		new Thread() {
			@Override
			public void run() {
				try {
					isPlaying = true;
					while (isPlaying) {
						if (!isPaused) {
							Log.v("VideoPlayer",
									"[Runtime] progress: " + progress
											+ " SeekBar Progress: "
											+ seekBar.getProgress()
											+ " SeekBar SecondProgress: "
											+ seekBar.getSecondaryProgress()
											+ " MediaPlayer Progress: "
											+ mediaPlayer.getCurrentPosition());
							if (!isSeeking && seekComplete) {
								progress = mediaPlayer.getCurrentPosition();
								seekBar.setProgress(progress);
							}
						}
						sleep(1000);
					}
				} catch (Exception e) {
				}
			}
		}.start();

		toggleButton(true);
		delayHideControllers();
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int whatInfo, int extra) {
		return false;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		finish();
		return false;
	}

	public void begin() {
		try {
			mediaPlayer.setDataSource(mediaPath);
			mediaPlayer.prepare();

			toggleButton(true);
		} catch (Exception ex) {
			finish();
		}
	}
	
	public void pause() {
		mediaPlayer.pause();
		isPaused = true;
		toggleButton(false);
	}
	
	public void resume() {
		mediaPlayer.start();
		isPaused = false;
		toggleButton(true);
	}

	public void stop() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.stop();
			}
			isPlaying = false;
		}
	}

	public void finish() {
		stop();
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}

		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);

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
			hideControllers();
		}
	};

	public void delayHideControllers() {
		hideHandler.postDelayed(hideControllerThread, 4000);
	}

	public void hideControllers() {
		findViewById(R.id.video_toppanel).setVisibility(View.GONE);
		findViewById(R.id.video_bottompanel).setVisibility(View.GONE);
	}

	public void showControllers() {
		findViewById(R.id.video_toppanel).setVisibility(View.VISIBLE);
		findViewById(R.id.video_bottompanel).setVisibility(View.VISIBLE);
	}

	@Override
	public void onUserInteraction() {
		super.onUserInteraction();

		if (findViewById(R.id.video_bottompanel).getVisibility() == View.VISIBLE) {
			hideHandler.removeCallbacks(hideControllerThread);
		} else {
			showControllers();
		}
		delayHideControllers();
	}

    // We want to pause when the headset is unplugged.
    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
        public void register() {
            VideoPlayer.this.registerReceiver(this,
                    new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        public void unregister() {
        	VideoPlayer.this.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer.isPlaying()) {
            	pause();
            }
        }
    }
}
