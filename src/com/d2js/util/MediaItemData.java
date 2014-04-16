package com.d2js.util;

import org.json.JSONObject;


public class MediaItemData {

	public String date = null;
	public int content = 0;
	public String title = null;
	public String path = null;
	public String media = null;
	public JSONObject item = null;
	public int progress = 0;
	
	@SuppressWarnings("unused")
	private MediaItemData() {
	}
	
	public MediaItemData(String date, int content) {
		JSONObject json = MediaList.ItemData(date);
		if (json == null || json.length() == 0) {
			return;
		}
		JSONObject item = json.optJSONObject("content" + content);
		if (item == null || item.length() == 0) {
			return;
		}
		this.date = date;
		this.content = content;
		this.title = item.optString("title", null);
		this.progress = item.optInt("progress", 0);
		this.path = item.optString("path", null);
		this.media = item.optString("media", null);
		this.item = item;
	}
	
	public MediaItemData(MediaItemData data) {
		this.date = data.date;
		this.content = data.content;
		this.title = data.title;
		this.progress = data.progress;
		this.path = data.path;
		this.media = data.media;
		this.item = data.item;
	}

	public boolean match(String date, int content) {
		return this.date.equals(date) && this.content == content;
	}
}
