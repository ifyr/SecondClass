package com.d2js.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;

public class SystemUtility {

	public SystemUtility() {
	}

	public static int getNetworkStatus(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();
		if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
			return Constants.STATE_NETWORK_WIFI;
		}

		State gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState();
		if (gprs == State.CONNECTED || gprs == State.CONNECTING) {
			return Constants.STATE_NETWORK_MOBILE;
		}
		
		return Constants.STATE_NETWORK_NONE;
	}
}
