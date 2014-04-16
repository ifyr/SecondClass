package com.d2js.secondclass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.json.JSONObject;

import com.d2js.util.Constants;
import com.d2js.util.MediaList;

import android.content.Context;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaAdapter extends BaseAdapter {
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd",
			Locale.CHINA);
	private ArrayList<String> itemDates = null;
	private int count = 0;
	private LayoutInflater inflater = null;
	private String readDate = null;

	public MediaAdapter(Context context) {
		inflater = LayoutInflater.from(context);
		itemDates = new ArrayList<String>();

		readDate = MediaList.ReadDate();
		if (!Constants.DATE_FIRST_CONTENT.equals(readDate)) {
			Calendar cal = dateFormat.getCalendar();
			Date dateRead = null;
			try {
				dateRead = dateFormat.parse(readDate);
			} catch (ParseException e) {
				dateRead = new Date();
			}
			cal.setTime(dateRead);
			String date = dateFormat.format(cal.getTime());
			while (date.compareTo(Constants.DATE_FIRST_CONTENT) >= 0) {
				JSONObject json = MediaList.ItemData(date);
				if (json != null) {
					itemDates.add(date);
				}

				cal.add(Calendar.DAY_OF_MONTH, -1);
				date = dateFormat.format(cal.getTime());
			}
		}
		count = itemDates.size();
	}

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public Object getItem(int position) {
		if (position > 0 && position < count) {
			return itemDates.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		if (position > 0 && position < count) {
			String date = itemDates.get(position);
			try {
				return Integer.parseInt(date);
			} catch (NumberFormatException ex) {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.panel_item, null);
		}
		updateView(convertView, itemDates.get(position));
		return convertView;
	}

	@Override
	public void notifyDataSetChanged() {
		if (this.count == MediaList.Count()) {
			return;
		}

		if (!MediaList.ReadDate().equals(readDate)) {
			Calendar cal = dateFormat.getCalendar();
			Date read = null;
			try {
				read = dateFormat.parse(readDate);
			} catch (ParseException e) {
				read = new Date();
			}
			cal.setTime(read);
			String date = dateFormat.format(cal.getTime());
			readDate = MediaList.ReadDate();
			while (date.compareTo(readDate) <= 0) {
				JSONObject json = MediaList.ItemData(date);
				if (json != null) {
					itemDates.add(0, date);
				}

				cal.add(Calendar.DAY_OF_MONTH, 1);
				date = dateFormat.format(cal.getTime());
			}
		}
		count = itemDates.size();

		super.notifyDataSetChanged();
	}

	private void updateView(View view, String date) {
		JSONObject json = MediaList.ItemData(date);
		if (json == null) {
			return;
		}

		TextView item_date = (TextView) view.findViewById(R.id.item_date);
		item_date.setText(json.optString("date", date));

		LinearLayout.LayoutParams match_wrap = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);

		LinearLayout layout_audio = (LinearLayout) view
				.findViewById(R.id.item_audio);
		LinearLayout layout_video = (LinearLayout) view
				.findViewById(R.id.item_video);

		int audio_count = 0, video_count = 0;

		for (int i = 0; i < 5; i++) {
			String contentKey = "content" + i;
			JSONObject content = json.optJSONObject(contentKey);
			if (content == null || content.length() == 0) {
				continue;
			}
			String media = content.optString("media");
			if (media == null || media.isEmpty()) {
				continue;
			}
			if (media.endsWith("mp3")) {
				View audioview = layout_audio.getChildAt(audio_count);
				if (audioview == null) {
					audioview = inflater.inflate(R.layout.view_audio, null);
					layout_audio.addView(audioview, match_wrap);
				}
				updateAudioView(audioview, content, date, i);
				audio_count++;
			} else {
				View videoview = layout_video.getChildAt(video_count);
				if (videoview == null) {
					videoview = inflater.inflate(R.layout.view_video, null);
					layout_video.addView(videoview, match_wrap);
				}
				updateVideoView(videoview, content, date, i);
				video_count++;
			}
		}

		if (layout_audio.getChildCount() > audio_count) {
			layout_audio.removeViewsInLayout(audio_count,
					layout_audio.getChildCount() - audio_count);
			layout_audio.requestLayout();
		}

		if (layout_video.getChildCount() > video_count) {
			layout_video.removeViewsInLayout(video_count,
					layout_video.getChildCount() - video_count);
			layout_video.requestLayout();
		}
		view.requestLayout();
	}

	private void updateAudioView(View view, JSONObject json, String date,
			int content) {
		String title = json.optString("title");
		String subject = json.optString("subject");
		TextView titleView = (TextView) view.findViewById(R.id.item_audiotitle);
		if (subject == null || subject.isEmpty()) {
			titleView.setText(title);
		} else {
			titleView.setText("[" + subject + "]" + title);
		}

		String length = json.optString("length", "60Ãë");
		TextView lengthView = (TextView) view
				.findViewById(R.id.item_audiolength);
		lengthView.setText(length);

		final String item_date = date;
		final int item_content = content;
		ImageView imageView = (ImageView) view
				.findViewById(R.id.item_audioaction);
		imageView.setClickable(true);
		imageView.setOnClickListener(new View.OnClickListener() {
			String date = item_date;
			int content = item_content;

			@Override
			public void onClick(View v) {
				Message.obtain(Main.instance.getHandler(),
						Constants.MSG_PLAY_AUDIO, content, 0, date)
						.sendToTarget();
			}
		});
	}

	private void updateVideoView(View view, JSONObject json, String date,
			int content) {
		String title = json.optString("title");
		String subject = json.optString("subject");
		TextView titleView = (TextView) view.findViewById(R.id.item_videotitle);
		if (subject == null || subject.isEmpty()) {
			titleView.setText(title);
		} else {
			titleView.setText("[" + subject + "]" + title);
		}

		String length = json.optString("length", "Î´Öª³¤¶È");
		TextView lengthView = (TextView) view
				.findViewById(R.id.item_videolength);
		lengthView.setText(length);

		final String item_date = date;
		final int item_content = content;
		ImageView imageView = (ImageView) view
				.findViewById(R.id.item_videoaction);
		imageView.setClickable(true);
		imageView.setOnClickListener(new View.OnClickListener() {
			String date = item_date;
			int content = item_content;

			@Override
			public void onClick(View v) {
				Message.obtain(Main.instance.getHandler(),
						Constants.MSG_PLAY_VIDEO, content, 0, date)
						.sendToTarget();
			}
		});
	}
}
