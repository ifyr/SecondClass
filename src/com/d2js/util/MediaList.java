package com.d2js.util;

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
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd",
			Locale.CHINA);
	private String readDate = Constants.DATE_FIRST_CONTENT;
	private String today = null;
	private HashMap<String, Object> data = new HashMap<String, Object>();
	private static MediaList instance = null;

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
		instance.updateToday();
	}

	public static String ReadDate() {
		return instance.readDate;
	}

	public static String Today() {
		return instance.today;
	}

	public static int Count() {
		return instance.data.size();
	}

	public static boolean NeedLoad() {
		return instance.needLoad();
	}

	public static void LoadSaved(String strSaved) {
		SharedInstance().loadSaved(strSaved);
	}

	public void updateToday() {
		today = dateFormat.format(new Date());
	}

	public static boolean UpdateList(String strList) {
		return instance.updateList(strList);
	}

	public static JSONObject ItemData(String date) {
		return instance.getItemData(date);
	}

	public static String MakeSaveString() {
		return instance.makeSaveString();
	}

	private void loadSaved(String strSaved) {
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
		// BUG FIX
		if (json.has(Constants.DATE_FIRST_CONTENT)) {
			// 如果没有第一天的内容，刷新全部内容
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
		String date = dateFormat.format(cal.getTime());
		while (date.compareTo(Constants.DATE_FIRST_CONTENT) >= 0) {
			JSONObject item = json.optJSONObject(date);
			if (item != null && item.length() > 0) {
				data.put(date, item);
				if (date.compareTo(readDate) > 0) {
					readDate = date;
				}
			}

			cal.add(Calendar.DAY_OF_MONTH, -1);
			date = dateFormat.format(cal.getTime());
		}
	}

	private boolean updateList(String strList) {
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
			if (!data.containsKey(date)) {
				JSONObject item = json.optJSONObject(date);
				if (item != null && item.length() != 0) {
					data.put(date, item);
					if (date.compareTo(readDate) > 0) {
						readDate = date;
					}
				}
			}

			cal.add(Calendar.DAY_OF_MONTH, 1);
			date = dateFormat.format(cal.getTime());
		}

		return true;
	}

	private boolean needLoad() {
		return today.compareTo(readDate) > 0;
	}

	private JSONObject getItemData(String date) {
		if (data.containsKey(date)) {
			return (JSONObject) data.get(date);
		}
		return null;
	}

	private String makeSaveString() {
		JSONObject json = new JSONObject();
		try {
			json.put(Constants.JSONKEY_UNTILDATE, readDate);

			Set<String> keys = data.keySet();
			Iterator<String> itor = keys.iterator();
			while (itor.hasNext()) {
				String key = itor.next();
				json.put(key, data.get(key));
			}
		} catch (JSONException e) {
		}

		return json.toString();
	}
}
