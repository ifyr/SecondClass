package com.d2js.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

public class HttpdUtility {
	public static final int SOCKET_READ_TIMEOUT = 5000;
	public static final int SOCKET_READ_BUFSIZE = 8192;

	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private Thread serveThread = null;
	private MediaItemData servingData = null;
	private int servingLength = 0;
	private DataOutputStream response = null;
	private boolean hasSendHeader = false;
	private byte[] mediaData = null;
	private int mediaLength = 0;
	private int sendLength = 0;

	/**
	 * Constructs an HTTP server on given port.
	 */
	public HttpdUtility() {
	}

	private void safeClose(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
			}
		}
	}

	private void safeClose(Socket closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
			}
		}
	}

	private void safeClose(ServerSocket closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Start the server.
	 * 
	 * @throws IOException
	 *             if the socket is in use.
	 */
	public void start() throws IOException {
		if (serverSocket == null) {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(8964));

			serveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					do {
						try {
							clientSocket = serverSocket.accept();
							clientSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
							BufferedReader bufreader = new BufferedReader(
									new InputStreamReader(
											clientSocket.getInputStream()));
							response = new DataOutputStream(
									clientSocket.getOutputStream());
							Status status = readInput(bufreader.readLine());
							if (!Status.OK.equals(status)) {
								sendErrorStatus(status);
								continue;
							}
							while (!clientSocket.isClosed()
									&& !serverSocket.isClosed()
									&& servingData != null) {
								updateMediaData(null, 0);
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
							}
						} catch (IOException e) {
						} finally {
							safeClose(response);
							response = null;
							safeClose(clientSocket);
							clientSocket = null;
						}
					} while (!serverSocket.isClosed());
				}
			});
			serveThread.setDaemon(true);
			serveThread.setName("HTTPD Listener");
			serveThread.start();
		}
	}

	public Status readInput(String header) {
		try {
			StringTokenizer st = new StringTokenizer(header);
			if (!st.hasMoreTokens()) {
				return Status.BAD_REQUEST;
			}
			if (!"GET".equalsIgnoreCase(st.nextToken())) {
				return Status.METHOD_NOT_ALLOWED;
			}
			if (!st.hasMoreTokens()) {
				return Status.BAD_REQUEST;
			}
			if (servingData == null || !servingData.equals(st.nextToken())) {
				return Status.NOT_FOUND;
			}
			return Status.OK;
		} catch (Exception e) {
			return Status.INTERNAL_ERROR;
		}
	}

	/**
	 * Stop the server.
	 */
	public void stop() {
		try {
			servingData = null;
			safeClose(clientSocket);
			clientSocket = null;
			safeClose(serverSocket);
			serverSocket = null;
			if (serveThread != null) {
				serveThread.join();
				serveThread = null;
			}
		} catch (Exception e) {
		}
	}

	public final boolean wasStarted() {
		return serverSocket != null && serveThread != null;
	}

	public final boolean isAlive() {
		return wasStarted() && !serverSocket.isClosed()
				&& serveThread.isAlive();
	}

	public final boolean canServeNew() {
		return clientSocket == null || clientSocket.isClosed();
	}

	public boolean startServe(MediaItemData data, int length) {
		if (data.media == null || data.media.isEmpty()) {
			return false;
		}
		if (!wasStarted()) {
			try {
				start();
			} catch (IOException e) {
				stop();
				return false;
			}
		}
		if (isAlive() && canServeNew()) {
			hasSendHeader = false;
			mediaLength = 0;
			sendLength = 0;
			mediaData = null;
			servingData = data;
			servingLength = length;
			return true;
		}
		return false;
	}

	public void endServe() {
		servingData = null;
		safeClose(response);
		response = null;
		safeClose(clientSocket);
		clientSocket = null;
	}

	@SuppressLint("HandlerLeak")
	Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.MSG_FILE_DOWNLOADED:
				if (!hasSendHeader) {
					servingLength = msg.arg1;
					sendFile();
				}
				break;
			case Constants.MSG_HTTP_ERROR:
			case Constants.MSG_DISK_ERROR:
			case Constants.MSG_SPACE_ERROR:
				endServe();
				break;
			case Constants.MSG_FILE_DOWNLOADING:
				break;
			case Constants.MSG_MEDIA_RECEIVED:
				MediaItemData data = (MediaItemData)msg.obj;
				updateMediaData(data.tempdata, msg.arg2);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};

	public Handler getHandler() {
		return messageHandler;
	}

	protected synchronized void updateMediaData(byte[] data, int size) {
		if (mediaData == null && servingLength != 0) {
			mediaData = new byte[servingLength];
		}
		if (data != null && size != 0) {
			System.arraycopy(data, 0, mediaData, mediaLength, size);
			mediaLength += size;
		}
		if (response != null && mediaLength > sendLength) {
			sendBuffer();
		}
	}

	protected void sendBuffer() {
		if (!hasSendHeader) {
			sendHeader();
		}
		try {
			response.write(mediaData, sendLength, mediaLength - sendLength);
			sendLength = mediaLength;
			response.flush();

			if (sendLength == servingLength) {
				endServe();
			}
		} catch (IOException e) {
		}
	}

	private void sendFile() {
		String filepath = Environment.getExternalStorageDirectory().getPath()
				+ Constants.PATH_LOCAL_MEDIAPATH + this.servingData;
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(filepath);
			byte[] data = new byte[servingLength];
			fin.read(data);
			updateMediaData(data, servingLength);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			safeClose(fin);
		}
	}

	private void sendHeader() {
		SimpleDateFormat gmtFrmt = new SimpleDateFormat(
				"E, d MMM yyyy HH:mm:ss 'GMT'", Locale.CHINA);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		try {
			response.writeChars("HTTP/1.1 " + Status.OK.getDescription()
					+ " \r\n");
			response.writeChars("Content-Type: "
					+ Mimes.lookup(servingData.media) + "\r\n");
			response.writeChars("Last-Modified: " + gmtFrmt.format(new Date())
					+ "\r\n");
			response.writeChars("Connection: keep-alive\r\n");
			response.writeChars("Content-Length: " + servingLength + "\r\n");
			response.writeChars("\r\n");
			response.flush();
		} catch (IOException ioe) {
		}
		hasSendHeader = true;
	}

	private void sendErrorStatus(Status status) {
		SimpleDateFormat gmtFrmt = new SimpleDateFormat(
				"E, d MMM yyyy HH:mm:ss 'GMT'", Locale.CHINA);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		String desc = status.getDescription();
		try {
			response.writeChars("HTTP/1.1 " + desc + " \r\n");
			response.writeChars("Content-Type: " + Mimes.TXT.getMime() + "\r\n");
			response.writeChars("Last-Modified: " + gmtFrmt.format(new Date())
					+ "\r\n");
			response.writeChars("Connection: keep-alive\r\n");
			response.writeChars("Content-Length: " + desc.length() + "\r\n");
			response.writeChars("\r\n");
			response.writeChars(desc);
			response.flush();
		} catch (IOException ioe) {
		}
	}

	public boolean check(MediaItemData data) {
		return servingData.equals(data);
	}

	public enum Mimes {
		HTM("htm", "text/html"), TXT("txt", "text/plain"), MP3("mp3",
				"audio/mpeg"), MP4("mp4", "video/mp4");

		private final String ext;
		private final String mime;

		Mimes(String ext, String mime) {
			this.ext = ext;
			this.mime = mime;
		}

		public String getMime() {
			return this.mime;
		}

		static public String lookup(String url) {
			if (url == null) {
				return TXT.mime;
			}
			String ext = url.substring(url.lastIndexOf('.') + 1);
			for (Mimes m : Mimes.values()) {
				if (m.ext.equalsIgnoreCase(ext)) {
					return m.mime;
				}
			}
			return TXT.mime;
		}
	}

	public enum Status {
		OK(200, "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"), NO_CONTENT(
				204, "No Content"), PARTIAL_CONTENT(206, "Partial Content"), REDIRECT(
				301, "Moved Permanently"), NOT_MODIFIED(304, "Not Modified"), BAD_REQUEST(
				400, "Bad Request"), UNAUTHORIZED(401, "Unauthorized"), FORBIDDEN(
				403, "Forbidden"), NOT_FOUND(404, "Not Found"), METHOD_NOT_ALLOWED(
				405, "Method Not Allowed"), RANGE_NOT_SATISFIABLE(416,
				"Requested Range Not Satisfiable"), INTERNAL_ERROR(500,
				"Internal Server Error");

		private final int status;
		private final String description;

		Status(int status, String description) {
			this.status = status;
			this.description = description;
		}

		public int getStatus() {
			return this.status;
		}

		public String getDescription() {
			return "" + this.status + " " + description;
		}
	}
}
