package com.d2js.util;

import com.d2js.secondclass.Main;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class GestureListener extends SimpleOnGestureListener {

	public GestureListener() {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	/**
	 * 无论是用手拖动view，或者是以抛的动作滚动，都会多次触发 ,这个方法在ACTION_MOVE动作发生时就会触发
	 * 参看GestureDetector的onTouchEvent方法源码
	 * 
	 * @param e1
	 *            The first down motion event that started the scrolling.
	 * @param e2
	 *            The move motion event that triggered the current onScroll.
	 * @param distanceX
	 *            The distance along the X axis(轴) that has been scrolled since
	 *            the last call to onScroll. This is NOT the distance between e1
	 *            and e2.
	 * @param distanceY
	 *            The distance along the Y axis that has been scrolled since the
	 *            last call to onScroll. This is NOT the distance between e1 and
	 *            e2.
	 * */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	/**
	 * 这个方法发生在ACTION_UP时才会触发 参看GestureDetector的onTouchEvent方法源码
	 * 
	 * @param e1
	 *            第1个ACTION_DOWN MotionEvent 并且只有一个
	 * @param e2
	 *            最后一个ACTION_MOVE MotionEvent
	 * @param velocityX
	 *            X轴上的移动速度，像素/秒
	 * @param velocityY
	 *            Y轴上的移动速度，像素/秒
	 * */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (e1.getX() - e2.getX() > 120) {  
			Main.instance.getHandler().sendEmptyMessage(Constants.MSG_RELOAD_LIST);
            return true;  
        }
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	/**
	 * 这个方法不同于onSingleTapUp，他是在GestureDetector确信用户在第一次触摸屏幕后，没有紧跟着第二次触摸屏幕，也就是，不是
	 * “双击”的时候触发
	 * */
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}
}
