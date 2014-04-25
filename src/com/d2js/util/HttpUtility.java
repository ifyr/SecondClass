package com.d2js.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;

public class HttpUtility {
	private HashMap<String, String> cookieContainer = null;

	public HttpUtility(String cookies) {
		cookieContainer = new HashMap<String, String>();
		parseCookie(cookies);
	}

	public void parseCookie(String cookies) {
		String[] cookieValues = cookies.split(";");
		for (int i = 0; i < cookieValues.length; i++) {
			String[] cookiePair = cookieValues[i].split("=");
			cookieContainer.put(cookiePair[0],
					cookiePair.length > 1 ? cookiePair[1] : "");
		}
	}

	public String generateCookie() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<String, String>> iter = cookieContainer.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append(";");
		}
		return sb.toString();
	}

	public void updateCookies(HttpResponse httpResponse) {
		Header[] headers = httpResponse.getHeaders("Set-Cookie");

		for (int i = 0; headers != null && i < headers.length; i++) {
			parseCookie(headers[i].getValue());
		}
	}

	public void writeCookies(HttpRequest httpRequest) {
		String cookies = generateCookie();
		if (cookies != null && !cookies.isEmpty()) {
			httpRequest.addHeader("cookie", cookies);
		}
	}
	
	protected String doDownload(String url) {
		// HttpGet对象
		HttpGet httpRequest = new HttpGet(url);
		// 写Cookie
		writeCookies(httpRequest);
		String strResult = "";
		try {
			// HttpClient对象
			HttpClient httpClient = new DefaultHttpClient();
			// 获得HttpResponse对象
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// 更新Cookie
				updateCookies(httpResponse);
				// 取得返回的数据
				strResult = EntityUtils.toString(httpResponse
						.getEntity());
			} else {
				strResult = "{\"code\":600,\"message\":\"Http status error\"}";
			}
		} catch (ClientProtocolException e) {
			strResult = "{\"code\":601,\"message\":\"Client protocal exception\"}";
		} catch (IOException e) {
			strResult = "{\"code\":602,\"message\":\"IO exception\"}";
		}
		return strResult;		
	}
	
	class DownloadTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... urls) {
			return doDownload(urls[0]);
		}
	}

	public String getList(String sinceDate) {
		String strResult = null;
		DownloadTask task = new DownloadTask();
		try {
			strResult = task.execute(
					"http://dierjiaoshi.duapp.com/wechat/appdata.php?s="
							+ sinceDate).get();
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
		} finally {
			task = null;
		}
		return strResult;
	}
}
