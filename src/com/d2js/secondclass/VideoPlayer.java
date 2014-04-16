package com.d2js.secondclass;

import com.d2js.util.ProgressUtility;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
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
public class VideoPlayer extends Activity implements OnCompletionListener,
		OnErrorListener, OnInfoListener, OnPreparedListener,
		OnSeekCompleteListener, OnVideoSizeChangedListener {

	private SurfaceView surfaceView = null;
	private SurfaceHolder surfaceHolder = null;
	private MediaPlayer mediaPlayer = null;
	private SeekBar seekBar = null;
	private Button btnPausePlay = null;
	private boolean isPlaying = false; // 控制更新播放进度条
	private boolean isPaused = false; // 控制界面恢复
	private boolean isSeeking = false; // 拖动进度条时不根据播放更新进度条
	private boolean seekComplete = true; // 防止拖动进度条后被onComplete中止
	private int progress = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Window win = getWindow();
		win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_videoplayer);

		seekBar = (SeekBar) findViewById(R.id.video_seekbar);
		seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

		btnPausePlay = (Button) findViewById(R.id.video_control);
		btnPausePlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer == null) {
					return;
				}
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
					toggleButton(false);
				} else {
					mediaPlayer.start();
					toggleButton(true);
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

		this.isPlaying = true;
		this.isSeeking = false;
		this.seekComplete = true;
		this.isPaused = false;

		String path = Main.instance.playingData.path;
		if (path == null || path.isEmpty()) {
			path = Main.instance.playingData.media;
		}
		this.progress = Main.instance.playingData.progress;

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		mediaPlayer.setOnInfoListener(this);

		surfaceView = (SurfaceView) findViewById(R.id.video_surface);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(surfaceHolderCallback);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		try {
			mediaPlayer.setScreenOnWhilePlaying(true);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(path);
			mediaPlayer.prepare();
		} catch (Exception ex) {
			finish();
		}

		toggleButton(true);
	}

	@Override
	public void onResume() {
		if (mediaPlayer != null && isPaused) {
			mediaPlayer.start();
			isPaused = false;
		}

		super.onResume();
	}

	@Override
	public void onPause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPaused = true;
		}

		super.onPause();
	}

	@Override
	public void finish() {
		stop();
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}

		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		super.finish();
	}

	private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
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

	};

	private OnSeekBarChangeListener onSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			int duration = seekBar.getMax();
			TextView played = (TextView) findViewById(R.id.video_timerplayed);
			played.setText(ProgressUtility.milliSecondsToTimer(progress));

			TextView rest = (TextView) findViewById(R.id.video_timerrest);
			rest.setText("-"
					+ ProgressUtility.milliSecondsToTimer(duration - progress));

			if (fromUser) {
				int secProgress = seekBar.getSecondaryProgress();
				if (secProgress > progress) {
					VideoPlayer.this.mediaPlayer.seekTo(progress);
				} else {
					seekBar.setProgress(VideoPlayer.this.seekBar.getProgress());
				}
			}
			VideoPlayer.this.progress = progress;
			if (progress == duration) {
				Main.instance.playingData.progress = 0;
				finish();
			} else {
				Main.instance.playingData.progress = progress;
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			isSeeking = true;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// 当进度条停止修改的时候触发
			// 取得当前进度条的刻度
			progress = seekBar.getProgress();
			if (mediaPlayer != null) {
				// 设置当前播放的位置
				mediaPlayer.seekTo(progress);
			}
			isSeeking = false;
			seekComplete = false;
		}
	};

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (seekComplete) {
			progress = 0;
			finish();
		} else {
			seekComplete = true;
			mediaPlayer.start();
		}
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
		// 按照初始位置播放
		if (progress != 0) {
			mp.seekTo(progress);
		}
		// 开始线程，更新进度条的刻度
		new Thread() {
			@Override
			public void run() {
				try {
					isPlaying = true;
					while (isPlaying) {
						progress = mediaPlayer.getCurrentPosition();
						if (!isSeeking && seekComplete) {
							seekBar.setProgress(progress);
						}
						sleep(1000);
					}
				} catch (Exception e) {
				}
			}
		}.start();

		toggleButton(true);
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

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		seekComplete = true;
	}

	protected void toggleButton(boolean playing) {
		btnPausePlay.setBackgroundDrawable(getResources().getDrawable(
				(playing ? R.drawable.btn_pause : R.drawable.btn_play)));
	}

	public void stop() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.stop();
			}
			isPlaying = false;
		}
	}
}
