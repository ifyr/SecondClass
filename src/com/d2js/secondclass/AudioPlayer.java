package com.d2js.secondclass;

import com.d2js.util.Constants;
import com.d2js.util.ProgressUtility;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class AudioPlayer implements OnCompletionListener, OnInfoListener,
		SeekBar.OnSeekBarChangeListener, OnSeekCompleteListener,
		OnErrorListener, OnPreparedListener, OnVideoSizeChangedListener {
	private static AudioPlayer instance = null;

	private View view = null;
	private MediaPlayer mediaPlayer = null;
	private SeekBar seekBar = null;
	private Button btnPausePlay = null;
	private boolean isPlaying = true;
	private boolean isSeeking = false;
	private boolean seekComplete = true;
	private boolean isPaused = false;
	private int progress = 0;

	public AudioPlayer() {
	}

	public static AudioPlayer SharedInstance() {
		if (instance == null) {
			instance = new AudioPlayer();
		}
		return instance;
	}

	public void createView(LinearLayout layout) {
		if (view == null) {
			LayoutInflater inflater = LayoutInflater.from(layout.getContext());
			view = inflater.inflate(R.layout.panel_audioplayer, null);

			seekBar = (SeekBar) view.findViewById(R.id.audio_seekbar);
			seekBar.setOnSeekBarChangeListener(this);

			btnPausePlay = (Button) view.findViewById(R.id.audio_control);
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
		}

		LinearLayout.LayoutParams match_wrap = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(view, match_wrap);
	}

	public void play() {
		TextView title = (TextView) view.findViewById(R.id.audio_name);
		title.setText(Main.instance.playingData.title);

		this.isPlaying = true;
		this.isSeeking = false;
		this.seekComplete = true;

		String path = Main.instance.playingData.path;
		if (path == null || path.isEmpty()) {
			path = Main.instance.playingData.media;
		}
		this.progress = Main.instance.playingData.progress;

		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
		}
		mediaPlayer.reset();
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		mediaPlayer.setOnInfoListener(this);

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

	public void finish() {
		stop();
		Message.obtain(Main.instance.getHandler(), Constants.MSG_AUDIO_FINISH,
				this.progress).sendToTarget();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int duration = seekBar.getMax();
		TextView played = (TextView) view.findViewById(R.id.audio_timerplayed);
		played.setText(ProgressUtility.milliSecondsToTimer(progress));

		TextView rest = (TextView) view.findViewById(R.id.audio_timerrest);
		rest.setText("-"
				+ ProgressUtility.milliSecondsToTimer(duration - progress));

		this.progress = progress;
		if (progress == duration) {
			Main.instance.playingData.progress = 0;
			finish();
		} else {
			Main.instance.playingData.progress = progress;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		this.isSeeking = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		progress = seekBar.getProgress();
		if (mediaPlayer != null) {
			// 设置当前播放的位置
			mediaPlayer.seekTo(progress);
		}
		this.isSeeking = false;
		this.seekComplete = false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (seekComplete) {
			progress = 0;
			finish();
		}
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
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
						sleep(100);
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
		Main.instance.getHandler().sendEmptyMessage(Constants.MSG_AUDIO_ERROR);
		return false;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		seekComplete = true;
	}

	@SuppressWarnings("deprecation")
	protected void toggleButton(boolean playing) {
		btnPausePlay
				.setBackgroundDrawable(view
						.getContext()
						.getResources()
						.getDrawable(
								(playing ? R.drawable.btn_pause
										: R.drawable.btn_play)));
	}

	public void stop() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.stop();
			}
			isPlaying = false;
		}
	}

	public void pause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPaused = true;
		}
	}
	
	public void resume() {
		if (mediaPlayer != null && isPaused) {
			mediaPlayer.start();
			isPaused = false;
		}
	}
}
