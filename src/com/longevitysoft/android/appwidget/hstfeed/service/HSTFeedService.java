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
package com.longevitysoft.android.appwidget.hstfeed.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.longevitysoft.android.appwidget.hstfeed.Constants;
import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.async.HSTFeedXMLWorker;
import com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler;
import com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler.HSTFeedXMLWorkerListener;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;
import com.longevitysoft.android.appwidget.hstfeed.util.HSTFeedUtil;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedService extends Service implements HSTFeedXMLWorkerListener {

	public static final String TAG = "HSTFeedService";

	public static final String MAST_FEED_URL = "http://archive.stsci.edu/stpr/vo_search.php";
	public static final String MAST_HST_URL = "http://imgsrc.hubblesite.org/hu/db/images/";
	public static final String QUERY_PARAM_NAME_POS = "pos";
	public static final String QUERY_PARAM_NAME_SIZE = "size";

	public static final int SIZE_SMALL = 0;
	public static final int SIZE_MEDIUM = 1;
	public static final int SIZE_LARGE = 2;

	public static final int WHAT_REMOTE_VIEWS = 10;

	public static final String BUNDLE_NAME_REMOTE_VIEWS = "views.remote";

	/**
	 * @author fbeachler
	 * 
	 */
	public static class HSTFeedXML {
		List<HSTImage> imageList;

		public HSTFeedXML() {
			imageList = new ArrayList<HSTImage>();
		}

		/**
		 * @return the imageList
		 */
		public List<HSTImage> getImageList() {
			return imageList;
		}

		/**
		 * @param imageList
		 *            the imageList to set
		 */
		public void setImageList(List<HSTImage> imageList) {
			if (null == imageList) {
				this.imageList = new ArrayList<HSTImage>();
			}
			this.imageList = imageList;
		}

	}

	public class LocalBinder extends Binder {
		public static final String TAG = HSTFeedService.TAG + "$LocalBinder";

		public HSTFeedService getService() {
			Log.d(TAG, "getService");
			return HSTFeedService.this;
		}
	}

	private final IBinder binder = new LocalBinder();
	private HSTFeedXMLWorker feedAsync;
	private HSTFeedXMLWorkerHandler xmlWorkerHandler;
	private AppWidgetManager manager;
	private Map<Integer, Boolean> downloadInProgress;

	@SuppressLint("UseSparseArrays")
	public HSTFeedService() {
		(new Thread(new Runnable() {

			@Override
			public void run() {
				Looper.prepare();
				xmlWorkerHandler = new HSTFeedXMLWorkerHandler(
						Looper.myLooper());
				xmlWorkerHandler.addListener(HSTFeedService.this);
				Looper.loop();
			}
		})).start();
		downloadInProgress = new HashMap<Integer, Boolean>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@SuppressLint("UseSparseArrays")
	@SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent == null) {
			return;
		}
		int appWidgetId = intent.getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		if (AppWidgetManager.INVALID_APPWIDGET_ID == appWidgetId) {
			Log.w(TAG,
					"widget called with an invalid widget ID, aborting service update.");
			return;
		}
		if (null == downloadInProgress) {
			downloadInProgress = new HashMap<Integer, Boolean>();
		}
		int widgetSize = intent.getIntExtra("widgetSize", SIZE_SMALL);
		ImageDB db = ImageDB.getInstance(getBaseContext());
		Bundle widget = db.getWidget(appWidgetId);
		Bundle imgData = null;
		if (null != widget) {
			int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
			imgData = db.getImageMeta(appWidgetId, current);
		}
		RemoteViews update = buildRemoteViews(this, appWidgetId, widgetSize,
				widget, imgData);
		if ((null == imgData || imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null)
				&& !dlInProg(appWidgetId)) {
			// load feed+images in background
			loadFeedInBackground(appWidgetId, widgetSize, widget);
		}
		manager = AppWidgetManager.getInstance(this);
		manager.updateAppWidget(appWidgetId, update);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedParseStart()
	 */
	@Override
	public void onFeedParseStart(final int appWidgetId, final int widgetSize) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedXMLLoaded(int)
	 */
	@Override
	public void onFeedXMLLoaded(final int appWidgetId, final int widgetSize,
			final int numImages) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedImageLoaded(java.lang.String)
	 */
	@Override
	public void onFeedImageLoaded(final int appWidgetId, final int widgetSize,
			final String imgSrc) {
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
	public void onFeedAllImagesLoaded(final int appWidgetId,
			final int widgetSize, final HSTFeedXML feed) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler
	 * .HSTFeedXMLWorkerListener#onFeedParseComplete()
	 */
	@Override
	public void onFeedParseComplete(final int appWidgetId, final int widgetSize) {
		downloadInProgress.put(appWidgetId, Boolean.valueOf(false));
		ImageDB db = ImageDB.getInstance(getBaseContext());
		Bundle widget = db.getWidget(appWidgetId);
		Bundle imgData = null;
		if (null != widget) {
			int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
			imgData = db.getImageMeta(appWidgetId, current);
		}
		RemoteViews update = buildRemoteViews(this, appWidgetId, widgetSize,
				widget, imgData);
		manager = AppWidgetManager.getInstance(this);
		manager.updateAppWidget(appWidgetId, update);
	}

	/**
	 * Add a listener for messages from {@link HSTFeedXMLWorkerHandler}.
	 * 
	 * @param xmlWorkerHandler
	 */
	public void addXMLWorkerListener(HSTFeedXMLWorkerListener xmlWorkerListener) {
		if (null == xmlWorkerListener) {
			return;
		}
		if (null != xmlWorkerHandler) {
			xmlWorkerHandler.addListener(xmlWorkerListener);
		}
	}

	/**
	 * @param xmlWorkerHandler
	 *            listener to remove
	 */
	public void removeXMLWorkerListener(
			HSTFeedXMLWorkerListener xmlWorkerListener) {
		if (null == xmlWorkerListener) {
			return;
		}
		if (null != xmlWorkerHandler) {
			xmlWorkerHandler.removeListener(xmlWorkerListener);
		}
	}

	/**
	 * Use {@link this#feedAsync} to load MAST xml feed and images in background
	 * thread.
	 * 
	 * @param appWidgetId
	 * @param widgetSize
	 * @param ra
	 * @param dec
	 * @param area
	 */
	public void loadFeedInBackground(final int appWidgetId,
			final int widgetSize, final Bundle widget) {
		if (null == widget) {
			Log.w(TAG,
					"oops!  given blank widget for load in bg call - aborting...");
			return;
		}
		float ra = widget.getFloat(ImageDBUtil.WIDGETS_RA);
		float dec = widget.getFloat(ImageDBUtil.WIDGETS_DEC);
		float area = widget.getFloat(ImageDBUtil.WIDGETS_AREA);
		feedAsync = new HSTFeedXMLWorker();
		feedAsync.setHSTFeedXMLWorkerHandler(xmlWorkerHandler);
		feedAsync.setCtx(getBaseContext());
		downloadInProgress.put(appWidgetId, Boolean.valueOf(true));
		feedAsync.execute(Integer.toString(appWidgetId),
				Integer.toString(widgetSize), MAST_FEED_URL,
				Float.toString(ra), Float.toString(dec), Float.toString(area));
	}

	/**
	 * @param context
	 * @param appWidgetId
	 * @return
	 */
	public RemoteViews buildRemoteViews(Context context, int appWidgetId,
			int widgetSize, Bundle widget, Bundle imgData) {
		ImageDB db = ImageDB.getInstance(context);
		if (null == widget) {
			return buildEmptyView(context, appWidgetId, widgetSize);
		}
		RemoteViews view = buildRemoteViewsLayout(context, widgetSize);
		int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
		float ra = widget.getFloat(ImageDBUtil.WIDGETS_RA);
		float dec = widget.getFloat(ImageDBUtil.WIDGETS_DEC);
		float area = widget.getFloat(ImageDBUtil.WIDGETS_AREA);
		// set text views based on widget widgetSize
		switch (widgetSize) {
		case SIZE_MEDIUM:
		case SIZE_SMALL:
			view.setTextViewText(R.id.ra, Float.toString(ra)
					+ Constants.SEMICOLON + Constants.SPACE);
			view.setTextViewText(R.id.dec, Float.toString(dec)
					+ Constants.SEMICOLON + Constants.SPACE);
			view.setTextViewText(
					R.id.area,
					Float.toString(area)
							+ context.getString(R.string.sym_degree));
			break;
		case SIZE_LARGE:
			view.setTextViewText(R.id.ra, getString(R.string.abbr_ra)
					+ Constants.SPACE + Float.toString(ra)
					+ Constants.SEMICOLON + Constants.SPACE);
			view.setTextViewText(R.id.dec, getString(R.string.dec)
					+ Constants.SPACE + Float.toString(dec)
					+ Constants.SEMICOLON + Constants.SPACE);
			view.setTextViewText(
					R.id.area,
					getString(R.string.area) + Constants.SPACE
							+ Float.toString(area)
							+ context.getString(R.string.sym_degree));
			break;
		}
		if (dlInProg(appWidgetId)
				|| imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null) {
			// set loading msg
			view.setViewVisibility(R.id.hst_img_loading, View.VISIBLE);
			view.setViewVisibility(R.id.label_credits, View.INVISIBLE);
			view.setViewVisibility(R.id.credits, View.INVISIBLE);
			view.setViewVisibility(R.id.name, View.INVISIBLE);
			view.setImageViewResource(R.id.hst_img, R.drawable.ic_feed_loading);
			view = HSTFeedUtil.buildWidgetClickIntent(view, getBaseContext(),
					appWidgetId, widgetSize, widget, null);
		}
		if (imgData.getString(ImageDBUtil.IMAGES_FILEPATH) != null) {
			if (imgData.getString(ImageDBUtil.IMAGES_NAME) != null) {
				view.setTextViewText(R.id.name,
						imgData.getString(ImageDBUtil.IMAGES_NAME));
			}
			if (imgData.getString(ImageDBUtil.IMAGES_CREDITS) != null) {
				view.setTextViewText(R.id.credits,
						imgData.getString(ImageDBUtil.IMAGES_CREDITS));
			}
			Vector<Integer> imgBnds = db.getImageBitmapBounds(appWidgetId,
					current);
			int sampleSize = 1;
			if (null != imgBnds && imgBnds.size() > 1) {
				sampleSize = HSTFeedUtil.calculateInSampleSize(imgBnds.get(0),
						imgBnds.get(1), widgetSize);
			}
			Bitmap dbbm = db.getImageBitmap(appWidgetId, current, sampleSize);
			if (dbbm != null) {
				view.setViewVisibility(R.id.hst_img_loading, View.INVISIBLE);
				view.setViewVisibility(R.id.label_credits, View.VISIBLE);
				view.setViewVisibility(R.id.credits, View.VISIBLE);
				view.setViewVisibility(R.id.name, View.VISIBLE);
				view.setImageViewBitmap(R.id.hst_img, dbbm);
			} else {
				// TODO set src as image missing from cache
			}
			// set click intent
			view = HSTFeedUtil.buildWidgetClickIntent(view, getBaseContext(),
					appWidgetId, widgetSize, widget, imgData);
		}
		return view;
	}

	/**
	 * @param context
	 * @param widgetSize
	 * @return
	 */
	private RemoteViews buildRemoteViewsLayout(Context context, int widgetSize) {
		RemoteViews view;
		switch (widgetSize) {
		case SIZE_LARGE:
			view = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout_lg);
			break;
		case SIZE_MEDIUM:
			view = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout_med);
			break;
		case SIZE_SMALL:
		default:
			view = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout_sm);
			break;
		}
		return view;
	}

	/**
	 * @param context
	 * @return
	 */
	private RemoteViews buildEmptyView(Context context, int appWidgetId,
			int widgetSize) {
		RemoteViews view = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout_empty);
		view = HSTFeedUtil.buildWidgetClickIntent(view, getBaseContext(),
				appWidgetId, widgetSize, null, null);
		return view;
	}

	protected boolean dlInProg(int appWidgetId) {
		return (downloadInProgress.containsKey(Integer.valueOf(appWidgetId)) && downloadInProgress
				.get(Integer.valueOf(appWidgetId)).booleanValue());
	}
}