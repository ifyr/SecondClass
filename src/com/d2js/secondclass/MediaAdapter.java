package com.d2js.secondclass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.d2js.util.Constants;
import com.d2js.util.MediaItemData;
import com.d2js.util.MediaList;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaAdapter extends BaseAdapter {
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd",
			Locale.CHINA);
	private ArrayList<ItemData> itemlist = null;
	private int count = 0;
	private LayoutInflater inflater = null;
	private String readDate = null;
	private Context context = null;

	public MediaAdapter(Context context) {
		this.context = context;
		this.inflater = LayoutInflater.from(context);
		this.itemlist = new ArrayList<ItemData>();

		this.readDate = MediaList.ReadDate();
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
				try {
					String itemdata = MediaList.ItemData(date);
					if (itemdata != null && !itemdata.isEmpty()) {
						JSONObject json = new JSONObject(
								MediaList.ItemData(date));
						if (json != null) {
							ItemData data = new ItemData(date);
							data.dateLocale = json.optString("date", null);
							for (int i = 0; i < 5; i++) {
								if (json.has("content" + i)) {
									JSONObject content = json
											.optJSONObject("content" + i);
									data.contents.add(new MediaItemData(date,
											i, content));
								}
							}
							itemlist.add(data);
						}
					}
				} catch (JSONException e) {
				}
				cal.add(Calendar.DAY_OF_MONTH, -1);
				date = dateFormat.format(cal.getTime());
			}
		}
		this.count = itemlist.size();
	}

	@Override
	public int getCount() {
		return this.count;
	}

	@Override
	public Object getItem(int position) {
		if (position > 0 && position < this.count) {
			return this.itemlist.get(position);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		if (position >= 0 && position < this.count) {
			ItemData item = this.itemlist.get(position);
			try {
				return Integer.parseInt(item.date);
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
		updateView(convertView, itemlist.get(position));
		return convertView;
	}

	@Override
	public void notifyDataSetChanged() {
		if (this.count != MediaList.Count()
				|| !MediaList.ReadDate().equals(readDate)) {
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
				try {
					JSONObject json = new JSONObject(MediaList.ItemData(date));
					if (json != null) {
						ItemData data = new ItemData(date);
						data.dateLocale = json.optString("date", null);
						for (int i = 0; i < Constants.MAX_MEDIA_COUNT; i++) {
							if (json.has("content" + i)) {
								JSONObject content = json
										.optJSONObject("content" + i);
								data.contents.add(new MediaItemData(date, i,
										content));
							}
						}
						itemlist.add(0, data);
					}
				} catch (JSONException ex) {
				}
				cal.add(Calendar.DAY_OF_MONTH, 1);
				date = dateFormat.format(cal.getTime());
			}
		}
		count = itemlist.size();
		super.notifyDataSetChanged();
	}

	private void updateView(View view, ItemData item) {
		TextView item_date = (TextView) view.findViewById(R.id.item_date);
		item_date.setText(item.dateLocale);

		LinearLayout layout_audio = (LinearLayout) view
				.findViewById(R.id.item_audio);
		LinearLayout layout_video = (LinearLayout) view
				.findViewById(R.id.item_video);

		int audio_count = 0, video_count = 0;

		for (int i = 0; i < item.contents.size(); i++) {
			MediaItemData data = item.contents.get(i);
			if (data.media == null || data.media.isEmpty()) {
				continue;
			}

			View itemview = null;
			LinearLayout layout = null;
			if (data.media.endsWith("mp3")) {
				layout = layout_audio;
				itemview = layout.getChildAt(audio_count);
				audio_count ++;
			} else {
				layout = layout_video;
				itemview = layout.getChildAt(video_count);
				video_count ++;
			}
			if (itemview == null) {
				itemview = inflater.inflate(R.layout.view_media, null);
				layout.addView(itemview,
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
			}
			updateItemView(itemview, data);
		}

		boolean needRequestLayout = false;
		if (layout_audio.getChildCount() > audio_count) {
			layout_audio.removeViewsInLayout(audio_count,
					layout_audio.getChildCount() - audio_count);
			layout_audio.requestLayout();
			needRequestLayout = true;
		}

		if (layout_video.getChildCount() > video_count) {
			layout_video.removeViewsInLayout(video_count,
					layout_video.getChildCount() - video_count);
			layout_video.requestLayout();
			needRequestLayout = true;
		}

		if(needRequestLayout) {
			view.requestLayout();
		}
	}

	private void updateItemView(View view, final MediaItemData data) {
		TextView titleView = (TextView) view.findViewById(R.id.item_mediatitle);
		if (data.subject == null || data.subject.isEmpty()) {
			titleView.setText(data.title);
		} else {
			titleView.setText("[" + data.subject + "]" + data.title);
		}

		TextView lengthView = (TextView) view
				.findViewById(R.id.item_medialength);
		lengthView.setText(data.length);

		LinearLayout audiostatus = (LinearLayout) view
				.findViewById(R.id.item_mediastatus);
		updateStatusView(audiostatus, data);

		ImageView imageView = (ImageView) view
				.findViewById(R.id.item_mediaaction);
		if (data.media.endsWith("mp3")) {
			imageView.setImageResource(R.drawable.audio);
		} else {
			imageView.setImageResource(R.drawable.video);
		}
		imageView.setClickable(true);
		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Message.obtain(Main.instance.getHandler(),
						Constants.MSG_PLAY_MEDIA, data).sendToTarget();
			}
		});
	}

	public void updateStatusView(LinearLayout layout, final MediaItemData data) {
		Log.v("MediaAdapter", "download:" + data.download + " media:"
				+ data.media);
		if (data.download < 0) {
			View status = layout.findViewById(R.layout.status_downloadable);
			if (status == null) {
				layout.removeAllViewsInLayout();

				status = inflater.inflate(R.layout.status_downloadable, null);
				layout.addView(status, LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
			}
			ImageView downloadable = (ImageView) status
					.findViewById(R.id.status_image_downloadable);
			downloadable.setClickable(true);
			downloadable.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Message.obtain(Main.instance.getHandler(),
							Constants.MSG_MEDIA_DOWNLOAD, data).sendToTarget();
				}
			});
		} else if (data.download > 100) {
			View status = layout.findViewById(R.layout.status_downloaded);
			if (status == null) {
				layout.removeAllViewsInLayout();

				status = inflater.inflate(R.layout.status_downloaded, null);
				layout.addView(status, LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
			}
			ImageView downloaded = (ImageView) status
					.findViewById(R.id.status_image_downloaded);
			downloaded.setClickable(true);
			downloaded.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Message.obtain(Main.instance.getHandler(),
							Constants.MSG_MEDIA_DELETE, data).sendToTarget();
				}
			});
		} else {
			View status = layout.findViewById(R.layout.status_downloading);
			if (status == null) {
				layout.removeAllViewsInLayout();

				status = inflater.inflate(R.layout.status_downloading, null);
				layout.addView(status, LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);

				ImageView downloading = (ImageView) status
						.findViewById(R.id.status_image_downloading);
				Animation circling = AnimationUtils.loadAnimation(context,
						R.anim.circling);
				downloading.startAnimation(circling);
			}
			String strProgress = null;
			if (data.progress >= 100) {
				strProgress = "下载完成";
			} else if (data.progress <= 0) {
				strProgress = "等待下载";
			} else {
				strProgress = "下载中…" + data.progress + "%";
			}
			TextView progress = (TextView) status
					.findViewById(R.id.status_text_downloading);
			progress.setText(strProgress);
		}
	}

	public void updateMediaItem(MediaItemData data) {
		for (int i = 0; i < this.itemlist.size(); i++) {
			ItemData item = itemlist.get(i);
			if (item.date.equals(data.date)) {
				for (int j = 0; j < item.contents.size(); j++) {
					MediaItemData content = item.contents.get(j);
					if (content.equals(data)) {
						item.contents.set(j, data);
						notifyDataSetChanged();
						return;
					}
				}
			}
		}
	}

	class ItemData {
		public String date = null;
		public String dateLocale = null;
		public ArrayList<MediaItemData> contents = null;

		public ItemData(String date) {
			this.date = date;
			this.dateLocale = null;
			this.contents = new ArrayList<MediaItemData>();
		}
	};
}
