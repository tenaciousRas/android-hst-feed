/**
 * HSTFeed is an Android Application that displays a slideshow of
 * HST PR images from the MAST web service.
 * 
 * HSTFeed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * This code is distributed under the Creative Commons Non-Commercial License.
 * You are free to share and remix the code, but you must include
 * credit to the original author Free Beachler and a hyperlink to 
 * Longevity Software LLC (http://www.longevitysoft.com).  
 * You may not use this work for commercial purposes.  You agree to use this work 
 * in a manner that does not conflict with the HSTFeed Android Application.  If 
 * you alter, transform, or build upon this work, you may distribute the resulting 
 * work only under the same or newer version of this license.
 * 
 * You should have received a copy of the Creative Commons Non-Commercial
 * License along with HSTFeed.  If not, see 
 * <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package net.hstfeed.activity;

import java.lang.ref.WeakReference;

import net.hstfeed.service.HSTFeedService;
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
 * @author fbeachler
 * 
 */
public class BaseActivity extends Activity {

	public static final String TAG = "BaseActivity";

	protected ServiceHandler sHandler;
	protected Intent mServiceBindIntent;
	protected HSTFeedService feedService;
	protected boolean feedServiceBound = false;
	protected int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	protected boolean edit = false;
	protected int size;

	public static class ServiceHandler extends Handler {

		@SuppressWarnings("unused")
		private WeakReference<HSTFeedService> service;
		private WeakReference<BaseActivity> activity;

		/**
		 * @param service
		 *            the service to set
		 */
		public void setService(WeakReference<HSTFeedService> service) {
			this.service = service;
		}

		/**
		 * @param activity
		 *            the activity to set
		 */
		public void setActivity(WeakReference<BaseActivity> activity) {
			this.activity = activity;
		}

		public void initRefs(WeakReference<HSTFeedService> service,
				WeakReference<BaseActivity> activity) {
			this.service = service;
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
			feedService.setActivityHandler(null);
			feedService = null;
			sHandler.setService(null);
			feedServiceBound = false;
		}

		public void onServiceConnected(ComponentName comp, IBinder binder) {
			Log.i(TAG, "BaseActivity::onServiceConnected");
			feedService = ((HSTFeedService.LocalBinder) binder).getService();
			feedService.setActivityHandler(sHandler);
			sHandler.setService(new WeakReference<HSTFeedService>(feedService));
			feedServiceBound = true;
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
			size = intent.getIntExtra("size", 0);
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
		if (feedServiceBound && null != mServiceConnection) {
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
		Log.d(TAG, "init() feedService= " + feedService);
		if (!feedServiceBound) {
			mServiceBindIntent = new Intent(this, HSTFeedService.class);
			bindService(mServiceBindIntent, mServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
	}

}