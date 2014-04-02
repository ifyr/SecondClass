package com.d2js.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * 实时检测耳机插拔
 * 用法：
	@Override
	protected void onResume() {
	    headsetPlugReceiver = new HeadsetPlugReceiver();
	    IntentFilter intentFilter = new IntentFilter();
	    intentFilter.addAction("android.intent.action.HEADSET_PLUG");
	    registerReceiver(headsetPlugReceiver, intentFilter);
	}
	@Override
	protected void onPause() {
		unregisterReceiver(headsetPlugReceiver);
	}
*/

public class HeadsetPlugReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.hasExtra("state")) {
			if (intent.getIntExtra("state", 0) == 0) {
				//耳机拔出
			} else if (intent.getIntExtra("state", 0) == 1) {
				//耳机插入
			}
		}
	}
}
