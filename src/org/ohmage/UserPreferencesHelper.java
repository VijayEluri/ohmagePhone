/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

public class UserPreferencesHelper {

	private static final boolean DEFAULT_SHOW_FEEDBACK = true;
	private static final boolean DEFAULT_SHOW_PROFILE = false;
	private static final boolean DEFAULT_SHOW_UPLOAD_QUEUE = false;
	private static final boolean DEFAULT_SHOW_MOBILITY = true;

	public static final String KEY_SHOW_FEEDBACK = "key_show_feedback";
	public static final String KEY_SHOW_PROFILE = "key_show_profile";
	public static final String KEY_SHOW_UPLOAD_QUEUE = "key_show_upload_queue";
	public static final String KEY_SHOW_MOBILITY = "key_show_mobility";
	private static final String KEY_BASELINE_END_TIME = "key_baseline_end_time";

	private final SharedPreferences mPreferences;

	public UserPreferencesHelper(Context activity) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
	}

	public boolean clearAll() {
		return mPreferences.edit().clear().commit();
	}

	public boolean showFeedback() {
		return mPreferences.getBoolean(KEY_SHOW_FEEDBACK, DEFAULT_SHOW_FEEDBACK);
	}

	public boolean showProfile() {
		return mPreferences.getBoolean(KEY_SHOW_PROFILE, DEFAULT_SHOW_PROFILE);
	}
	
	public boolean showUploadQueue() {
		return mPreferences.getBoolean(KEY_SHOW_UPLOAD_QUEUE, DEFAULT_SHOW_UPLOAD_QUEUE);
	}
	
	public boolean showMobility() {
		return mPreferences.getBoolean(KEY_SHOW_MOBILITY, DEFAULT_SHOW_MOBILITY);
	}

	/**
	 * Returns the baseline or a time 3 months ago if it is not set
	 * @param context
	 * @return
	 */
	public static long getBaseLineEndTime(Context context) {
		long base = PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY_BASELINE_END_TIME, -1);
		if(base == -1) {
			//If baseline is not set, we set it to 3 months ago
			Calendar cal = Calendar.getInstance();
			Utilities.clearTime(cal);
			cal.add(Calendar.MONTH, -3);
			base = cal.getTimeInMillis();
		}
		return base;
	}

	public static void clearBaseLineEndTime(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().remove(KEY_BASELINE_END_TIME).commit();
	}
}
