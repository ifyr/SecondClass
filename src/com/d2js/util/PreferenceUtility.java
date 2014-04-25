package com.d2js.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtility {

	private static PreferenceUtility instance = null;
	private SharedPreferences sharedPerferences = null;

	private PreferenceUtility(Context context) {
		sharedPerferences = context.getApplicationContext()
				.getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE);
	}

	public static PreferenceUtility createInstance(Context context) {
		if (instance == null) {
			instance = new PreferenceUtility(context);
		}
		return instance;
	}

	public static PreferenceUtility SharedInstance() {
		return instance;
	}

	public String getString(String key, String defValue) {
		return sharedPerferences.getString(key, defValue);
	}

	public void putString(String key, String value) {
		sharedPerferences.edit().putString(key, value).commit();
	}

	public boolean getBoolean(String key, boolean defValue) {
		return sharedPerferences.getBoolean(key, defValue);
	}

	public void putBoolean(String key, boolean value) {
		sharedPerferences.edit().putBoolean(key, value).commit();
	}

	public int getInt(String key, int defValue) {
		return sharedPerferences.getInt(key, defValue);
	}

	public void putInt(String key, int value) {
		sharedPerferences.edit().putInt(key, value).commit();
	}
}
