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
	 * �����������϶�view�����������׵Ķ��������������δ��� ,���������ACTION_MOVE��������ʱ�ͻᴥ��
	 * �ο�GestureDetector��onTouchEvent����Դ��
	 * 
	 * @param e1
	 *            The first down motion event that started the scrolling.
	 * @param e2
	 *            The move motion event that triggered the current onScroll.
	 * @param distanceX
	 *            The distance along the X axis(��) that has been scrolled since
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
	 * �������������ACTION_UPʱ�Żᴥ�� �ο�GestureDetector��onTouchEvent����Դ��
	 * 
	 * @param e1
	 *            ��1��ACTION_DOWN MotionEvent ����ֻ��һ��
	 * @param e2
	 *            ���һ��ACTION_MOVE MotionEvent
	 * @param velocityX
	 *            X���ϵ��ƶ��ٶȣ�����/��
	 * @param velocityY
	 *            Y���ϵ��ƶ��ٶȣ�����/��
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
	 * ���������ͬ��onSingleTapUp��������GestureDetectorȷ���û��ڵ�һ�δ�����Ļ��û�н����ŵڶ��δ�����Ļ��Ҳ���ǣ�����
	 * ��˫������ʱ�򴥷�
	 * */
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}
}
