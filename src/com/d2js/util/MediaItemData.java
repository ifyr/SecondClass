package com.d2js.util;

import org.json.JSONException;
import org.json.JSONObject;


public class MediaItemData {

	public String date = null;
	public int content = 0;
	public int progress = 0;
	public int download = -1;
	public String path = null;
	public String media = "";
	public String title = null;
	public String length = null;
	public String subject = null;
	public byte[] tempdata = null; // 下载时缓存，平时为null
	
	@SuppressWarnings("unused")
	private MediaItemData() {
	}
	
	public MediaItemData(String date, int content) {
		JSONObject json;
		try {
			json = new JSONObject(MediaList.ItemData(date));
		} catch (JSONException e) {
			return;
		}
		if (json == null || json.length() == 0) {
			return;
		}
		JSONObject item = json.optJSONObject("content" + content);
		if (item == null || item.length() == 0) {
			return;
		}
		this.date = date;
		this.content = content;
		this.progress = item.optInt("progress", 0);
		this.path = item.optString("path", null);
		this.download = (this.path == null || this.path.isEmpty())?-1:200;
		this.media = item.optString("media", "");
		this.title = item.optString("title", null);
		this.length = item.optString("length", null);
		this.subject = item.optString("subject", null);
	}
	
	public MediaItemData(String date, int content, JSONObject json) {
		if (json== null || json.length() == 0) {
			return;
		}
		this.date = date;
		this.content = content;
		this.progress = json.optInt("progress", 0);
		this.path = json.optString("path", null);
		this.download = (this.path == null || this.path.isEmpty())?-1:200;
		this.media = json.optString("media", "");
		this.title = json.optString("title", null);
		this.length = json.optString("length", null);
		this.subject = json.optString("subject", null);
	}
	
	public MediaItemData(MediaItemData data) {
		this.date = data.date;
		this.content = data.content;
		this.progress = data.progress;
		this.path = data.path;
		this.download = data.download;
		this.media = data.media;
		this.title = data.title;
		this.length = data.length;
		this.subject = data.subject;
	}
	
	public boolean equals(MediaItemData data) {
		return this.media.equals(data.media);
	}
}
