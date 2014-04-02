package com.d2js.util;

import java.io.FileOutputStream;
import java.io.PrintStream;

import android.content.Context;

/*
 * 未处理异常保存
 * 用法：
 * UncaughtExceptionHandler	ueHandler = new UncaughtExceptionHandler(this);
 * Thread.setDefaultUncaughtExceptionHandler(ueHandler);
 */
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
	private Context context;

	public UncaughtExceptionHandler(Context context) {
		this.context = context;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		// write to /data/data/<app_package>/files/error.log
		FileOutputStream fs = null;
		PrintStream ps = null;
		try {
			fs = context.openFileOutput("error.log", Context.MODE_PRIVATE);
			ps = new PrintStream(fs);
			ex.printStackTrace(ps);
		} catch (Exception e) {
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				if (fs != null) {
					fs.close();
				}
			} catch (Exception e) {
			}
		}

		// kill App Progress
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}