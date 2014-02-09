package com.d2js.weixin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Appstart extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appstart);
		//requestWindowFeature(Window.FEATURE_NO_TITLE); //去掉标题栏
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//	WindowManager.LayoutParams.FLAG_FULLSCREEN); //全屏显示
		//Toast.makeText(getApplicationContext(), "提示信息！", Toast.LENGTH_LONG).show();
		//overridePendingTransition(R.anim.hyperspace_in, R.anim.hyperspace_out);

	new Handler().postDelayed(new Runnable() {
		@Override
		public void run() {
			Intent intent = new Intent (Appstart.this,Welcome.class);
			startActivity(intent);
			Appstart.this.finish();
		}
	}, 1000);
	}
}
