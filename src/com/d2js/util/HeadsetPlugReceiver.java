package com.d2js.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * ʵʱ���������
 * �÷���
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
				//�����γ�
			} else if (intent.getIntExtra("state", 0) == 1) {
				//��������
			}
		}
	}
}
