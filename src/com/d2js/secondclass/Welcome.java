package com.d2js.secondclass;

import com.d2js.util.Constants;
import com.d2js.util.MediaList;
import com.d2js.util.PreferenceUtility;
import com.d2js.util.SystemUtility;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;

public class Welcome extends Activity {
	private View loadingView = null;
	private ImageView loadingImage = null;
	private boolean loading = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		// PreferenceUtility需要在Welcome中初始化
		PreferenceUtility.createInstance(this);
		loadingView = findViewById(R.id.welcome_loading);
		loadingImage = (ImageView) findViewById(R.id.welcome_loadingimage);
		loading = false;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!loading) {
			loading = true;
			loadingView.setVisibility(View.VISIBLE);
			Animation circling = AnimationUtils
					.loadAnimation(this, R.anim.circling);
			loadingImage.startAnimation(circling);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					readData();
				}
			}, 1000);
		}
	}

	private void readData() {
		boolean hasNew = false; 
		if (SystemUtility.getNetworkStatus(this) != Constants.STATE_NETWORK_NONE) {
			if(!MediaList.UpdateList()) {
				hasNew = true;
			}
		}
		if (!hasNew) {
			MediaList.LoadSaved();
			if (MediaList.NeedUpdate()) {
				if (SystemUtility.getNetworkStatus(this) == Constants.STATE_NETWORK_NONE) {
					Toast.makeText(this, "没有网络，无法加载新内容", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "加载内容失败，请检查网络", Toast.LENGTH_SHORT).show();
				}
			}
		} else {
			MediaList.ClearExpire();
			MediaList.Save();
		}

		loading = false;
		loadingImage.clearAnimation();
		loadingView.setVisibility(View.INVISIBLE);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(Welcome.this, Main.class);
				startActivity(intent);
				Welcome.this.finish();
			}
		}, 100);
	}
}
