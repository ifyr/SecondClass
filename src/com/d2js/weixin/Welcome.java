package com.d2js.weixin;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;

public class Welcome extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);

		new Handler().postDelayed(new Runnable(){
			@Override
			public void run(){
				Intent intent = new Intent (Welcome.this, Main.class);
				startActivity(intent);
				Welcome.this.finish();
			}
		}, 100);
	}
}
