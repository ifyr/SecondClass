package com.d2js.weixin;

import android.os.Bundle;
import android.app.Activity;
import android.view.MotionEvent;
//import android.view.WindowManager;

public class InfoXiaoheiHead extends Activity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info_xiaohei_head);
		//requestWindowFeature(Window.FEATURE_NO_TITLE); // ȥ��������
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		//		WindowManager.LayoutParams.FLAG_FULLSCREEN); // ȫ����ʾ
		//Toast.makeText(getApplicationContext(), "��ʾ��Ϣ", Toast.LENGTH_LONG).show();
		//overridePendingTransition(R.anim.hyperspace_in, R.anim.hyperspace_out);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		finish();
		return true;
	}
}
