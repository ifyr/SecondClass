package com.d2js.secondclass;

import com.d2js.util.Constants;
import com.d2js.util.HttpUtility;
import com.d2js.util.MediaList;
import com.d2js.util.PreferenceUtility;
import com.d2js.util.SystemUtility;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

public class Welcome extends Activity {
	private PreferenceUtility preferUtil = null;
	private View loadingView = null;
	private ImageView loadingImage = null;
	private boolean loading = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		// PreferenceUtility需要在Welcome中初始化
		preferUtil = PreferenceUtility.createInstance(this);
		loadingView = findViewById(R.id.welcome_loading);
		loadingImage = (ImageView) findViewById(R.id.welcome_loadingimage);
		loading = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		messageHandler.sendEmptyMessageDelayed(Constants.MSG_LOAD_ACTIVITY, 1000);
	}
	
	@Override
	public void onPause() {
		messageHandler.removeCallbacksAndMessages(null);
		super.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		this.finish();
	}
	
	@Override
	public void onDestroy() {
		messageHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}

	@SuppressLint("HandlerLeak")
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_LOAD_ACTIVITY:
				if (!loading) {
					Welcome.this.checkLoad();
				}
				break;
			case Constants.MSG_NET_NONE:
				onNetNone();
				break;
			case Constants.MSG_LOAD_LISTDATA:
				onLoadList((String) msg.obj);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};

	private void checkLoad() {
		if (loading) {
			return;
		}
		loading = true;

		loadingView.setVisibility(View.VISIBLE);
		Animation circling = AnimationUtils
				.loadAnimation(this, R.anim.circling);
		loadingImage.startAnimation(circling);

		MediaList.LoadSaved(preferUtil.getString(Constants.PREFKEY_LIST, ""));
		int networkState = SystemUtility.getNetworkStatus(this);
		if (networkState == Constants.STATE_NETWORK_NONE) {
			Message.obtain(messageHandler, Constants.MSG_NET_NONE, networkState, 0,
					null).sendToTarget();
			return;
		}
		if (MediaList.NeedLoad()) {
			readData();
		} else {
			showMain();
		}
	}

	private void readData() {
		String strList = HttpUtility.SharedInstance().getList(
				MediaList.ReadDate());
		Message.obtain(messageHandler, Constants.MSG_LOAD_LISTDATA, strList)
				.sendToTarget();
	}

	private void onLoadList(String strList) {
		boolean updated = MediaList.UpdateList(strList);
		if (updated) {
			preferUtil.putString(Constants.PREFKEY_LIST,
					MediaList.MakeSaveString());
			if (MediaList.NeedLoad()) {
				readData();
			} else {
				showMain();
			}
		} else {
			Toast.makeText(this, "加载内容失败，请检查网络", Toast.LENGTH_SHORT).show();
			showMain();
		}
	}

	private void onNetNone() {
		if (MediaList.NeedLoad()) {
			Toast.makeText(this, "没有网络，无法加载新内容", Toast.LENGTH_SHORT).show();
		}
		showMain();
	}

	private void showMain() {
		loading = false;
		loadingImage.clearAnimation();
		loadingView.setVisibility(View.INVISIBLE);

		messageHandler.postDelayed(runMain, 100);
	}
	
	private Runnable runMain = new Runnable() {
		@Override
		public void run() {
			Intent intent = new Intent(Welcome.this, Main.class);
			startActivity(intent);
			Welcome.this.finish();
		}
	};
}
