package com.d2js.util;

public class Constants {

	public static final int STATE_NETWORK_NONE = 0;
	public static final int STATE_NETWORK_MOBILE = 1;
	public static final int STATE_NETWORK_WIFI = 2;

	public static final int STATE_PLAYING_NONE = 0;
	public static final int STATE_PLAYING_AUDIO = 1;
	public static final int STATE_PLAYING_VIDEO = 2;
	
	public static final int STATE_POPUP_NONE = 0;
	public static final int STATE_POPUP_SETTING = 1;
	public static final int STATE_POPUP_CONFIRM = 2;
	public static final int STATE_POPUP_WAITING = 3;
	
	public static final int MSG_NET_NONE = 11;
	public static final int MSG_LOAD_ACTIVITY = 12;
	public static final int MSG_LOAD_LISTDATA = 14;

	public static final int MSG_PLAY_AUDIO = 21;
	public static final int MSG_PLAY_VIDEO = 22;
	public static final int MSG_AUDIO_FINISH = 23;
	public static final int MSG_AUDIO_ERROR = 24;
	public static final int MSG_VIDEO_FINISH = 25;
	public static final int MSG_VIDEO_ERROR = 26;
	public static final int MSG_FILE_PROGRESSED = 27;
	public static final int MSG_FILE_DOWNLOADED = 28;
	public static final int MSG_HTTP_ERROR = 29;
	public static final int MSG_DISK_ERROR = 30;
	public static final int MSG_SPACE_ERROR = 31;

	public static final String DATE_FIRST_CONTENT = "140303";
	
	public static final String PREFKEY_COOKIE = "Saved_Cookies";
	public static final String PREFKEY_LIST = "Saved_List";
	
	public static final String JSONKEY_SINCEDATE = "since";
	public static final String JSONKEY_UNTILDATE = "until";
	public static final String JSONKEY_RETCODE = "code";
}
