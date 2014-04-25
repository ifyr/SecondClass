package com.d2js.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class MediaUtility {
	private Handler handler = null;
	private MediaItemData data = null;

	public MediaUtility(Handler handler, MediaItemData data) {
		this.handler = handler;
		this.data = data;
	}

	public void sendMessage(int what) {
		sendMessage(what, 0, 0);
	}
	
	public void sendMessage(int what, int extra) {
		sendMessage(what, extra, 0);
	}

	public void sendMessage(int what, int arg1, int arg2) {
		if (handler != null) {
			Message.obtain(handler, what, arg1, arg2, data).sendToTarget();
		}
	}

	protected void doDownload(String url) {
		String localPath = Environment.getExternalStorageDirectory().getPath()
				+ Constants.PATH_LOCAL_MEDIAPATH
				+ url.substring(url.lastIndexOf('/') + 1);
		URLConnection conn = null;
		try {
			conn = new URL(url).openConnection();
		} catch (Exception ex) {
			// 打开链接失败，通知上层
			sendMessage(Constants.MSG_HTTP_ERROR);
			return;
		}
		// 根据响应获取文件大小
		int fileSize = conn.getContentLength();
		File file = new File(localPath);
		if (file.length() == fileSize) {
			data.path = localPath;
			sendMessage(Constants.MSG_FILE_DOWNLOADED, fileSize);
			return;
		}
		if (!file.exists()) {
			file.mkdirs();
		}
		if (file.getFreeSpace() < fileSize) {
			sendMessage(Constants.MSG_SPACE_ERROR);
			return;
		}
		if(file.exists()) {
			file.delete();
		}

		InputStream is = null;
		try {
			is = new BufferedInputStream(conn.getInputStream());
		} catch (IOException e) {
			// 打开Stream失败，通知上层
			sendMessage(Constants.MSG_HTTP_ERROR);
			return;
		}
		sendMessage(Constants.MSG_FILE_DOWNLOADING, fileSize);

		// 创建写入文件内存流，通过此流向目标写文件
		byte buf[] = new byte[8192];
		int downloaded = 0;
		int numread = 0;

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			while ((numread = is.read(buf)) != -1) {
				bos.write(buf, 0, numread);
				bos.flush();

				downloaded += numread;
				// 通知上层
				int percent = downloaded * 100 / fileSize;
				data.tempdata = buf.clone();
				sendMessage(Constants.MSG_MEDIA_RECEIVED, downloaded, numread);
				if (percent != data.download) {
					data.download = percent;
					sendMessage(Constants.MSG_FILE_PROGRESSED);
				}
			}

			is.close();
			is = null;
			bos.close();
			bos = null;
			fos.close();
			fos = null;

			if (downloaded < fileSize) {
				sendMessage(Constants.MSG_HTTP_ERROR);
			} else {
				data.path = localPath;
				sendMessage(Constants.MSG_FILE_DOWNLOADED, fileSize);
			}
		} catch (FileNotFoundException ex) {
			// 文件不能写，通知上层
			sendMessage(Constants.MSG_DISK_ERROR);
		} catch (IOException ex) {
			// 下载失败，通知上层
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
		@Override
		protected String doInBackground(String... urls) {
			doDownload(urls[0]);
			return null;
		}
	}

	public void download() {
		if(data.media == null || data.media.isEmpty()) {
			return;
		}
		new DownloadTask().execute(data.media);
	}

	public String getLocalMedia() {
		return Constants.PATH_LOCAL_MEDIAURL
				+ data.media.substring(data.media.lastIndexOf('/') + 1);
	}
}
