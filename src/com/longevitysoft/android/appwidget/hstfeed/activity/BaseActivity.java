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

import java.lang.ref.WeakReference;

import com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler.HSTFeedXMLWorkerListener;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService.HSTFeedXML;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Base activity for features and code common to multiple activities.
 * 
 * @author fbeachler
 * 
 */
public class BaseActivity extends Activity implements HSTFeedXMLWorkerListener {

	public static final String TAG = "BaseActivity";

	/**
	 * Handler for messages from service.
	 */
	protected ServiceHandler sHandler;

	/**
	 * Intent to bind HST Feed service.
	 */
	protected Intent mServiceBindIntent;

	/**
	 * HST Feed Service for downloading XML feed and images.
	 */
	protected HSTFeedService feedService;

	/**
	 * Flag if service is bound.
	 */
	protected boolean feedServiceBound = false;

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

	public static class ServiceHandler extends Handler {

		private WeakReference<BaseActivity> activity;

		/**
		 * @param activity
		 *            the activity to set
		 */
		public void setActivity(WeakReference<BaseActivity> activity) {
			this.activity = activity;
		}

		@Override
		public void handleMessage(Message msg) {
			// Log.v(TAG, new
			// StringBuilder().append("#handleMessage - msg.what=")
			// .append(msg.what).append(", msg.data=").append(msg.getData())
			// .append(", msg.arg1=").append(msg.arg1).toString());
			Bundle data = msg.getData();
			Thread t = null;
			switch (msg.what) {
			case HSTFeedService.WHAT_REMOTE_VIEWS:
				Log.d(TAG, "got remote views from service");
				final int appWidgetId = msg.arg1;
				final RemoteViews rv = data
						.getParcelable(HSTFeedService.BUNDLE_NAME_REMOTE_VIEWS);
				t = new Thread() {

					@Override
					public void run() {
						activity.get().runOnUiThread(new Runnable() {
							public void run() {
								AppWidgetManager manager = AppWidgetManager
										.getInstance(activity.get()
												.getBaseContext());
								manager.updateAppWidget(appWidgetId, rv);
							}
						});
					}

				};
				t.start();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	// private final BroadcastReceiver feedChangeReceiver = new
	// BroadcastReceiver() {
	//
	// public void onReceive(Context context, Intent intent) {
	// String action = intent.getAction();
	// Log.d(TAG, "feedChangeReceiver intent=" + intent.toString());
	// final Intent mIntent = intent;
	// }
	// };

	protected ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName arg0) {
			Log.i(TAG, "BaseActivity::onServiceDisconnected");
			feedService.removeXMLWorkerListener(BaseActivity.this);
			feedService = null;
			feedServiceBound = false;
		}

		public void onServiceConnected(ComponentName comp, IBinder binder) {
			Log.i(TAG, "BaseActivity::onServiceConnected");
			feedService = ((HSTFeedService.LocalBinder) binder).getService();
			feedServiceBound = true;
			handleOnServiceConnected();
		}
	};

	/**
	 * @return the mServiceBindIntent
	 */
	public Intent getBindIntent() {
		return mServiceBindIntent;
	}

	/**
	 * @return the feedService
	 */
	public HSTFeedService getfeedService() {
		return feedService;
	}

	/**
	 * @return the sHandler
	 */
	public Handler getServiceHandler() {
		return sHandler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "OnCreate");
		sHandler = new ServiceHandler();
		sHandler.setActivity(new WeakReference<BaseActivity>(this));
		Intent intent = getIntent();
		if (intent != null) {
			widgetSize = intent.getIntExtra("widgetSize",
					HSTFeedService.SIZE_SMALL);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		init();
	}

	@Override
	protected void onPause() {
		super.onPause();
		feedService.removeXMLWorkerListener(this);
		stopService(mServiceBindIntent);
		if (null != mServiceConnection) {
			try {
				unbindService(mServiceConnection);
			} catch (Exception e) {
				Log.w(TAG, e.getClass().getCanonicalName()
						+ " caught when unbinding service");
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void init() {
		Log.d(TAG, "init() feedService=" + feedService);
		if (!feedServiceBound) {
			mServiceBindIntent = new Intent(this, HSTFeedService.class);
			bindService(mServiceBindIntent, mServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedParseStart()
	 */
	@Override
	public void onFeedParseStart() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedXMLLoaded(int)
	 */
	@Override
	public void onFeedXMLLoaded(int numImages) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedImageLoaded(java.lang.String)
	 */
	@Override
	public void onFeedImageLoaded(String imgSrc) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .
	 * HSTFeedXMLWorkerListener#onFeedAllImagesLoaded(com.longevitysoft.android.
	 * appwidget.hstfeed.service.HSTFeedService.HSTFeedXML)
	 */
	@Override
	public void onFeedAllImagesLoaded(HSTFeedXML feed) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedParseComplete()
	 */
	@Override
	public void onFeedParseComplete() {
	}

	/**
	 * Called when service connected.
	 */
	public void handleOnServiceConnected() {
		feedService.addXMLWorkerListener(this);
	}
}