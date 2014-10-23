/**
 * HSTFeed is an Android Application that displays a slideshow of
 * HST PR images from the MAST web service.
 * 
 * HSTFeed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This code is distributed under the Creative Commons Non-Commercial License.
 * You are free to share and remix the code, but you must include credit to the 
 * original author Free Beachler, Longevity Software LLC as described herein.  
 * All distributions of this code and application must include the Longevity Software LLC logo.  
 * Any remix and/or distribution of the application which is capable of displaying images must 
 * also display the Longevity Software LLC logo with attribution.  You may not use this work 
 * for commercial purposes.  You agree to use this work in a manner that does not conflict 
 * with the HSTFeed Android Application.  If you alter, transform, or build upon this work, 
 * you may distribute the resulting work only under the same or newer version of this license.
 * 
 * You should have received a copy of the Creative Commons Non-Commercial
 * License along with HSTFeed.  If not, see 
 * <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package com.longevitysoft.android.appwidget.hstfeed.activity;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
 * Base activity for features and code common to multiple activities.
 * 
 * @author fbeachler
 * 
 */
public abstract class BaseActivity extends Activity {

	public static final String TAG = "BaseActivity";

	/**
	 * App widget ID -- assigned by os.
	 */
	protected int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	/**
	 * Add or edit mode.
	 */
	protected boolean edit = false;

	/**
	 * Size of widget as declared constant in {@link HSTFeedService}.
	 */
	protected int widgetSize;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "OnCreate");
		Intent intent = getIntent();
		if (intent != null) {
			widgetSize = intent.getIntExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE,
					HSTFeedService.SIZE_SMALL);
		} else {
			widgetSize = HSTFeedService.SIZE_SMALL;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
	}

	/**
	 * 
	 */
	private void init() {
	}

	/**
	 * Called when service connected.
	 */
	public void handleOnServiceConnected() {
	}
}