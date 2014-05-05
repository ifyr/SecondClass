package com.d2js.util;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

public class MediaList {
	private static MediaList instance = null;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd",
			Locale.CHINA);
	private String readDate = Constants.DATE_FIRST_CONTENT;
	private String today = null;
	private HashMap<String, String> datalist = new HashMap<String, String>();
	private HttpUtility httpUtil = null;

	private MediaList() {
		updateToday();
	}

	public static MediaList SharedInstance() {
		if (instance == null) {
			instance = new MediaList();
		}
		return instance;
	}

	public static void UpdateToday() {
		SharedInstance().updateToday();
	}

	public static String ReadDate() {
		return SharedInstance().readDate;
	}

	public static String Today() {
		return SharedInstance().today;
	}

	public static int Count() {
		return SharedInstance().datalist.size();
	}

	public static boolean NeedUpdate() {
		return SharedInstance().needUpdate();
	}

	public static void LoadSaved() {
		SharedInstance().loadSaved();
	}

	public static boolean UpdateList() {
		return SharedInstance().updateList();
	}

	public static String ItemData(String date) {
		return SharedInstance().getItemData(date);
	}

	public static void Save() {
		SharedInstance().save();
	}

	public static void Clear() {
		SharedInstance().clear();
	}

	public static void ClearExpire() {
		SharedInstance().clearExpire();
	}

	public static void UpdateMediaItem(MediaItemData item) {
		SharedInstance().updateMediaItem(item);
	}

	private void updateToday() {
		today = dateFormat.format(new Date());
	}

	private void loadSaved() {
		String strSaved = PreferenceUtility.SharedInstance().getString(
				Constants.PREFKEY_LIST, null);
		if (strSaved == null || strSaved.isEmpty()) {
			return;
		}

		JSONObject json = null;
		try {
			json = new JSONObject(strSaved);
		} catch (JSONException ex) {
			return;
		}
		if (json == null || json.length() == 0) {
			return;
		}

		String until_date = json.optString(Constants.JSONKEY_UNTILDATE);
		if (until_date == null || until_date.isEmpty()) {
			return;
		}
		readDate = until_date;

		Date date_until = null;
		try {
			date_until = dateFormat.parse(until_date);
		} catch (ParseException ex) {
			return;
		}

		Calendar cal = dateFormat.getCalendar();
		cal.setTime(date_until);
		cal.add(Calendar.DAY_OF_MONTH, -15);
		String date_since = dateFormat.format(cal.getTime());

		cal.setTime(date_until);
		String date = dateFormat.format(cal.getTime());
		while (date.compareTo(date_since) >= 0) {
			JSONObject item = json.optJSONObject(date);
			if (item != null && item.length() > 0) {
				datalist.put(date, item.toString());
				if (date.compareTo(readDate) > 0) {
					readDate = date;
				}
			}

			cal.add(Calendar.DAY_OF_MONTH, -1);
			date = dateFormat.format(cal.getTime());
		}
	}

	private boolean updateList() {
		if (httpUtil == null) {
			String cookies = PreferenceUtility.SharedInstance().getString(
					Constants.PREFKEY_COOKIE, "");
			httpUtil = new HttpUtility(cookies);
		}

		Calendar cal_read = dateFormat.getCalendar();
		cal_read.setTime(new Date());
		String date_read = dateFormat.format(cal_read.getTime());

		String strList = httpUtil.getList(date_read);
		if (strList == null || strList.isEmpty()) {
			return false;
		}

		JSONObject json = null;
		try {
			json = new JSONObject(strList);
		} catch (JSONException ex) {
			return false;
		}
		if (json == null || json.length() == 0) {
			return false;
		}

		int code = json.optInt(Constants.JSONKEY_RETCODE, 0);
		if (code != 200) {
			return false;
		}
		String since_date = json.optString(Constants.JSONKEY_SINCEDATE);
		String until_date = json.optString(Constants.JSONKEY_UNTILDATE);
		if (until_date == null || since_date == null || until_date.isEmpty()
				|| since_date.isEmpty()) {
			return false;
		}
		Date date_since = null;
		try {
			date_since = dateFormat.parse(since_date);
		} catch (ParseException ex) {
			return false;
		}

		Calendar cal = dateFormat.getCalendar();
		cal.setTime(date_since);
		String date = dateFormat.format(cal.getTime());
		while (date.compareTo(until_date) <= 0) {
			JSONObject item = json.optJSONObject(date);
			if (item != null && item.length() != 0) {
				datalist.put(date, item.toString());
				if (date.compareTo(readDate) > 0) {
					readDate = date;
				}
			}

			cal.add(Calendar.DAY_OF_MONTH, 1);
			date = dateFormat.format(cal.getTime());
		}

		return true;
	}

	private boolean needUpdate() {
		return today.compareTo(readDate) > 0;
	}

	private String getItemData(String date) {
		if (datalist.containsKey(date)) {
			return datalist.get(date);
		}
		return null;
	}

	private void save() {
		JSONObject json = new JSONObject();
		try {
			json.put(Constants.JSONKEY_UNTILDATE, readDate);
		} catch (JSONException e) {
			return;
		}

		Set<String> keys = datalist.keySet();
		Iterator<String> itor = keys.iterator();
		while (itor.hasNext()) {
			String key = itor.next();
			try {
				json.put(key, new JSONObject(datalist.get(key)));
			} catch (JSONException e) {
			}
		}

		PreferenceUtility.SharedInstance().putString(Constants.PREFKEY_LIST,
				json.toString());
	}

	private void updateMediaItem(MediaItemData data) {
		try {
			JSONObject json = new JSONObject(datalist.get(data.date));
			if (json.has("content" + data.content)) {
				JSONObject item = new JSONObject();
				item.put("media", data.media);
				item.put("title", data.title);
				item.put("subject", data.subject);
				item.put("length", data.length);
				item.put("progress", data.progress);
				item.put("path", data.path);
				item.put("download", data.download);

				json.put("content" + data.content, item);
				datalist.put(data.date, json.toString());
				save();
			}
		} catch (JSONException e) {
		}
	}

	private void clear() {
		readDate = Constants.DATE_FIRST_CONTENT;
		datalist.clear();
	}

	private void clearExpire() {
		String strSaved = PreferenceUtility.SharedInstance().getString(
				Constants.PREFKEY_LIST, null);
		if (strSaved == null || strSaved.isEmpty()) {
			return;
		}

		JSONObject json = null;
		try {
			json = new JSONObject(strSaved);
		} catch (JSONException ex) {
			return;
		}
		if (json == null || json.length() == 0) {
			return;
		}

		String until_date = json.optString(Constants.JSONKEY_UNTILDATE);
		if (until_date == null || until_date.isEmpty()) {
			return;
		}

		Date date_until = null;
		try {
			date_until = dateFormat.parse(until_date);
		} catch (ParseException ex) {
			return;
		}

		Calendar cal = dateFormat.getCalendar();
		cal.setTime(date_until);
		cal.add(Calendar.DAY_OF_MONTH, -15);
		String date_since = dateFormat.format(cal.getTime());

		cal.setTime(date_until);
		String date = dateFormat.format(cal.getTime());
		while (date.compareTo(date_since) >= 0) {
			JSONObject item = json.optJSONObject(date);
			if (item == null || item.length() == 0) {
				continue;
			}
			for (int i = 0; i < 5; i++) {
				String key = "content" + i;
				if (!item.has(key)) {
					continue;
				}
				JSONObject content = item.optJSONObject(key);
				if (content == null || !content.has("path")) {
					continue;
				}
				String path = content.optString("path", null);
				if (path == null || path.isEmpty()) {
					continue;
				}

				String newpath = null;
				if (datalist.containsKey(date)) {
					String newItemStr = datalist.get(date);
					try {
						JSONObject newItem = new JSONObject(newItemStr);
						if (newItem.has(key)) {
							JSONObject newcontent = newItem.optJSONObject(key);
							if (newcontent != null && newcontent.has("path")) {
								newpath = newcontent.optString("path", null);
							}
						}
					} catch (JSONException e) {
					}
				}
				if (!path.equals(newpath)) {
					File file = new File(path);
					if (file.exists()) {
						file.delete();
					}
				}
			}

			cal.add(Calendar.DAY_OF_MONTH, -1);
			date = dateFormat.format(cal.getTime());
		}
	}
}
