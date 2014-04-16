package com.d2js.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class MediaUtility {
	private Handler handler = null;
	private MediaItemData data = null;
	
	private MediaUtility(Handler handler, String date, int content) {
		this.handler = handler;
		this.data = new MediaItemData(date, content);
	}

	public void sendMessage(int what) {
		sendMessage(what, 0);
	}
	
	public void sendMessage(int what, int extra) {
		if (handler != null) {
			Message.obtain(handler, what, extra, 0, data).sendToTarget();
		}
	}
	
	protected void doDownload(String url, String path) {
		URLConnection conn = null;
		InputStream is = null;
		try {
			conn = new URL(url).openConnection();
			conn.connect();
			is = conn.getInputStream();
		} catch (Exception ex) {
			// 打开链接失败，通知上层
			sendMessage(Constants.MSG_HTTP_ERROR);
			return;
		}
		if (is == null) {
			// 没有下载流，通知上层
			sendMessage(Constants.MSG_HTTP_ERROR);
			return;
		}
		// 根据响应获取文件大小
		int fileSize = conn.getContentLength();
		File file = new File(path);
		if (file.length() == fileSize) {
			sendMessage(Constants.MSG_FILE_DOWNLOADED);
			try {
				is.close();
			} catch(IOException ioex) {
			}
			return;
		}
		if (file.getFreeSpace() < fileSize) {
			sendMessage(Constants.MSG_SPACE_ERROR);
			try {
				is.close();
			} catch(IOException ioex) {
			}
			return;
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException ex) {
			// 文件不能写，通知上层
			sendMessage(Constants.MSG_DISK_ERROR);
			try {
				is.close();
			} catch(IOException ioex) {
			}
			return;
		}
		// 创建写入文件内存流，通过此流向目标写文件
		byte buf[] = new byte[1024];
		int downloaded = 0;
		int numread = 0;
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		try {
			while ((numread = is.read(buf)) != -1) {
				bos.write(buf, 0, numread);
				downloaded += numread;
				// 通知上层
				sendMessage(Constants.MSG_FILE_PROGRESSED, downloaded*100/fileSize);
			}

			is.close();
			is = null;
			bos.close();
			bos = null;
			fos.close();
			fos = null;

			if(downloaded < fileSize) {
				sendMessage(Constants.MSG_HTTP_ERROR, downloaded);
			} else {
				sendMessage(Constants.MSG_FILE_DOWNLOADED);
			}
		} catch (IOException ex) {
			sendMessage(Constants.MSG_HTTP_ERROR);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ex) {
				}
			}
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException ex) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
					fos = null;
				} catch (IOException ex) {
				}
			}
		}
	}
	
	class DownloadTask extends AsyncTask<String, Integer, String> {
		private String path;

		protected void setTargetPath(String path) {
			this.path = path;
		}

		@Override
		protected String doInBackground(String... urls) {
			doDownload(urls[0], path);
			return null;
		}
	}

	public void downloadFile(String url, String path) {
		DownloadTask task = new DownloadTask();
		task.setTargetPath(path);
		task.execute(url);
	}
}
