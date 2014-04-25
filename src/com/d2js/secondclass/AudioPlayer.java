package com.d2js.secondclass;

import com.d2js.util.Constants;
import com.d2js.util.MediaUtility;
import com.d2js.util.ProgressUtility;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Handler;
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

	private View view = null;
	private MediaPlayer mediaPlayer = null;
	private SeekBar seekBar = null;
	private Button btnPausePlay = null;
	private boolean isPlaying = true;
	private boolean isSeeking = false;
	private boolean seekComplete = true;
	private boolean isPaused = false;
	private int progress = 0;
	private String localmedia = null;

	public AudioPlayer() {
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
		
		seekBar.setProgress(0);

		this.isPlaying = true;
		this.isSeeking = false;
		this.seekComplete = true;

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

		mediaPlayer.setScreenOnWhilePlaying(true);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		this.progress = Main.instance.playingData.progress;
		this.localmedia = Main.instance.playingData.path;
		if (localmedia == null || localmedia.isEmpty() || Main.instance.playingData.download < 100) {
			MediaUtility mediaUtil = new MediaUtility(this.messageHander,
					Main.instance.playingData);
			localmedia = mediaUtil.getLocalMedia();
			mediaUtil.download();
		} else {
			begin();
		}

		toggleButton(true);
	}

	@SuppressLint("HandlerLeak")
	Handler messageHander = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_HTTP_ERROR:
			case Constants.MSG_DISK_ERROR:
			case Constants.MSG_MEDIA_RECEIVED:
			case Constants.MSG_FILE_DOWNLOADING:
			case Constants.MSG_FILE_DOWNLOADED:
				break;
			case Constants.MSG_FILE_PROGRESSED:
				if (msg.arg1 > 0) {
					begin();
				}
				break;
			default:
				super.handleMessage(msg);
				return;
			}
			Message.obtain(Main.instance.getHandler(),
					msg.what, msg.arg1, msg.arg2, msg.obj).sendToTarget();
		}
	};

	public void begin() {
		try {
			mediaPlayer.setDataSource(localmedia);
			mediaPlayer.prepare();
		} catch (Exception ex) {
			finish();
		}
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
			// ���õ�ǰ���ŵ�λ��
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
		// ���ý�������������Ϊ��Ƶ������󲥷�ʱ��
		seekBar.setMax(mp.getDuration());
		// ���ճ�ʼλ�ò���
		if (progress != 0) {
			mp.seekTo(progress);
		}
		// ��ʼ�̣߳����½������Ŀ̶�
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
		stop();
		Main.instance.getHandler().sendEmptyMessage(Constants.MSG_AUDIO_ERROR);
		return false;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		seekComplete = true;
	}

	protected void toggleButton(boolean playing) {
		btnPausePlay.setBackgroundResource(playing ? R.drawable.btn_pause
										: R.drawable.btn_play);
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
