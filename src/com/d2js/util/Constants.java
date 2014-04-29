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

	public static final int MSG_PLAY_MEDIA = 21;
	public static final int MSG_AUDIO_FINISH = 22;
	public static final int MSG_AUDIO_ERROR = 23;
	public static final int MSG_VIDEO_FINISH = 24;
	public static final int MSG_VIDEO_ERROR = 25;
	public static final int MSG_FILE_DOWNLOADING = 26;
	public static final int MSG_FILE_PROGRESSED = 27;
	public static final int MSG_FILE_DOWNLOADED = 28;
	public static final int MSG_HTTP_ERROR = 29;
	public static final int MSG_DISK_ERROR = 30;
	public static final int MSG_SPACE_ERROR = 31;
	public static final int MSG_MEDIA_DOWNLOAD = 32;
	public static final int MSG_MEDIA_RECEIVED = 33;
	public static final int MSG_MEDIA_DELETE = 34;
	public static final int MSG_CONFIRM_YES = 35;
	public static final int MSG_CONFIRM_NO = 36;
	public static final int MSG_RELOAD_LIST = 37;

	public static final String DATE_FIRST_CONTENT = "140303";
	
	public static final String PREFKEY_COOKIE = "Saved_Cookies";
	public static final String PREFKEY_LIST = "Saved_List";
	
	public static final String JSONKEY_SINCEDATE = "since";
	public static final String JSONKEY_UNTILDATE = "until";
	public static final String JSONKEY_RETCODE = "code";
	
	public static final int MAX_MEDIA_COUNT = 5;
	public static final String PATH_LOCAL_MEDIAPATH = "/dierjiaoshi/media/";
	public static final String PATH_LOCAL_MEDIAURL = "http://127.0.0.1:8964/";
}
