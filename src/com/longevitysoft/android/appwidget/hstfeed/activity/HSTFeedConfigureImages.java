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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.adapter.ImageAdapter;
import com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler.HSTFeedXMLWorkerListener;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService.HSTFeedXML;

/**
 * Activity to configure images in feed once they are downloaded.
 * 
 * @author fbeachler
 * 
 */
public class HSTFeedConfigureImages extends BaseActivity implements
		HSTFeedXMLWorkerListener {

	public static final String TAG = "HSTFeedConfigureImages";

	/**
	 * UI references.
	 */
	private Bitmap[] bmps;
	private GridView grid;
	private Button cancel, ok;
	private ImageAdapter adapter;
	private TextView header;
	private ProgressBar loadProg;

	/**
	 * Storage mode. Currently only local storage supported.
	 */
	private int mode; // storage mode

	/**
	 * Edit mode defines whether user is editing an existing widget, or creating
	 * a new one.
	 */
	private boolean edit; // edit mode

	/**
	 * Keep track of total image count from background loader, for progress bar.
	 */
	private int loaderNumImagesTotal;

	/**
	 * Number of image just downloaded in background, for progress bar.
	 */
	private int loaderNumImagesLoaded;

	/**
	 * Widget info.
	 */
	private Bundle widget;

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

	public static class ServiceHandler extends Handler {

		private WeakReference<HSTFeedConfigureImages> activity;

		/**
		 * @param activity
		 *            the activity to set
		 */
		public void setActivity(WeakReference<HSTFeedConfigureImages> activity) {
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

	protected ServiceConnection mServiceConnection = new ServiceConnection() {
		public void onServiceDisconnected(ComponentName arg0) {
			Log.i(TAG, "BaseActivity::onServiceDisconnected");
			feedService.removeXMLWorkerListener(HSTFeedConfigureImages.this);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#onCreate
	 * (android.os.Bundle)
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_images);
		appWidgetId = getIntent().getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		widgetSize = getIntent().getIntExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE,
				HSTFeedService.SIZE_SMALL);
		edit = getIntent().getBooleanExtra("edit", false);
		widget = getIntent().getBundleExtra("widget");
		loaderNumImagesLoaded = 0;
		loaderNumImagesTotal = 0;
		grid = (GridView) findViewById(R.id.images_grid);
		cancel = (Button) findViewById(R.id.config_images_cancel);
		ok = (Button) findViewById(R.id.config_images_go);
		header = (TextView) findViewById(R.id.images_count);
		loadProg = (ProgressBar) findViewById(R.id.loading_progress);
		adapter = new ImageAdapter(this);
		grid.setAdapter(adapter);
		initLayoutListeners();
		// init background feed loader
		sHandler = new ServiceHandler();
		sHandler.setActivity(new WeakReference<HSTFeedConfigureImages>(this));
		Log.d(TAG, "edit mode=" + edit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#onResume
	 * ()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		grid.invalidate();
		grid.invalidateViews();
		buildHeaderText();
		if (edit
				&& getIntent().hasExtra(
						HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_CONFIG_RESET)) {
			// edit cached images in existing widget
			ImageDB db = ImageDB.getInstance(this);
			loadImagesFromCache(db);
		} else {
			mServiceBindIntent = new Intent(this, HSTFeedService.class);
			bindService(mServiceBindIntent, mServiceConnection,
					Context.BIND_AUTO_CREATE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#onPause
	 * ()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (feedServiceBound) {
			try {
				feedService.removeXMLWorkerListener(this);
				unbindService(mServiceConnection);
			} catch (Exception e) {
				Log.d(TAG,
						"unable to unbind service connection, exception was:\n"
								+ Log.getStackTraceString(e));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != bmps) {
			for (Bitmap bmp : bmps) {
				bmp.recycle();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArray("images", adapter.toArray());
		super.onSaveInstanceState(outState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		bmps = (Bitmap[]) savedInstanceState.getParcelableArray("images");
		hydrateGridAdapter(bmps, adapter);
		header.setText(String.format(getText(R.string.feed_images_count)
				.toString(), adapter.getCount()));
		super.onRestoreInstanceState(savedInstanceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		switch (mode) {
		case HSTFeedConfigureBase.TYPE_LOCAL:
			inflater.inflate(R.menu.load_feed_images, menu);
			break;
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int dim = 0;
		switch (widgetSize) {
		case HSTFeedService.SIZE_SMALL:
			dim = 200;
			break;
		case HSTFeedService.SIZE_MEDIUM:
			dim = 350;
			break;
		case HSTFeedService.SIZE_LARGE:
			dim = 400;
			break;
		}
		if (mode == HSTFeedConfigureBase.TYPE_LOCAL) {
			switch (item.getItemId()) {
			case R.id.load_feed_images:
				Intent grab = new Intent(Intent.ACTION_GET_CONTENT, null);
				grab.setType("image/*");
				grab.putExtra("crop", "true");
				grab.putExtra("aspectX", 1);
				grab.putExtra("aspectY", 1);
				grab.putExtra("outputX", dim);
				grab.putExtra("outputY", dim);
				grab.putExtra("noFaceDetection", true);
				grab.putExtra("return-data", true);
				startActivityForResult(grab,
						HSTFeedConfigureBase.REQUEST_IMAGES);
				break;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 10 && resultCode == RESULT_OK) {
			Intent intent = new Intent(getBaseContext(), HSTFeedService.class);
			intent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, HSTFeedService.SIZE_SMALL);
			intent.putExtra("appWidgetId", appWidgetId);
			getBaseContext().startService(intent);
			Log.d(TAG, "initial feed update");
			setResult(RESULT_OK);
			finish();
		} else if (requestCode == 20 && resultCode == RESULT_OK) {
			// from configure image options
			int action = data.getIntExtra("action", -1);
			int position = data.getIntExtra("position", -1);
			if (position < 0) {
				return;
			}
			ImageDB db = ImageDB.getInstance(this);
			switch (action) {
			case HSTFeedConfigureImagesOptions.ACTION_DELETE:
				db.deleteImage(appWidgetId, position);
				adapter.deletePosition(position);
				buildHeaderText();
				break;
			case HSTFeedConfigureImagesOptions.ACTION_NEXT:
				db.moveImage(appWidgetId, position, 1);
				// update adapter
				moveItemInAdapter(bmps, adapter, position, 1);
				grid.invalidate();
				break;
			case HSTFeedConfigureImagesOptions.ACTION_PREV:
				db.moveImage(appWidgetId, position, -1);
				// update adapter
				moveItemInAdapter(bmps, adapter, position, -1);
				grid.invalidate();
				break;
			}
			grid.invalidate();
			grid.invalidateViews();
		}
	}

	/**
	 * Called when service connected.
	 */
	public void handleOnServiceConnected() {
		feedService.addXMLWorkerListener(this);
		if (!edit) {
			feedService.loadFeedInBackground(appWidgetId, widgetSize, widget);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * onFeedParseStart()
	 */
	@Override
	public void onFeedParseStart(final int appWidgetId, final int widgetSize) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				loadProg.setVisibility(View.VISIBLE);
				header.setText(getString(R.string.download_xml_feed));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * onFeedXMLLoaded(int)
	 */
	@Override
	public void onFeedXMLLoaded(final int appWidgetId, final int widgetSize,
			final int numImages) {
		loaderNumImagesTotal = numImages;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				header.setText(getString(R.string.download_image_list));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * onFeedImageLoaded(java.lang.String)
	 */
	@Override
	public void onFeedImageLoaded(final int appWidgetId, final int widgetSize,
			final String imgSrc) {
		loaderNumImagesLoaded++;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				header.setText(String.format(getText(R.string.download_images)
						.toString(), loaderNumImagesLoaded,
						loaderNumImagesTotal));
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * onFeedAllImagesLoaded
	 * (com.longevitysoft.android.appwidget.hstfeed.service.
	 * HSTFeedService.HSTFeedXML)
	 */
	@Override
	public void onFeedAllImagesLoaded(final int appWidgetId,
			final int widgetSize, final HSTFeedXML feed) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (0 < loaderNumImagesLoaded) {
					header.setText(getString(R.string.download_image_list));
					ImageDB db = ImageDB.getInstance(getBaseContext());
					loadImagesFromCache(db);
				} else {
					header.setText(String.format(
							getString(R.string.no_images_found),
							widget.getFloat(ImageDBUtil.WIDGETS_RA),
							widget.getFloat(ImageDBUtil.WIDGETS_DEC),
							widget.getFloat(ImageDBUtil.WIDGETS_AREA)));
				}
				loadProg.setVisibility(View.INVISIBLE);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * onFeedParseComplete()
	 */
	@Override
	public void onFeedParseComplete(final int appWidgetId, final int widgetSize) {
		ImageDB db = ImageDB.getInstance(getBaseContext());
		db.invalidateWidget(appWidgetId);
		db.needsUpdate(appWidgetId);
	}

	/**
	 * Init UI listeners.
	 */
	private void initLayoutListeners() {
		grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				launchMenu(position);
			}
		});
		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!edit) {
					ImageDB db = ImageDB
							.getInstance(HSTFeedConfigureImages.this);
					db.deleteWidget(appWidgetId);
				}
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		ok.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Intent updateIntent = new Intent();
				// updateIntent
				// .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				// updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				// appWidgetId);
				// getBaseContext().sendBroadcast(updateIntent);
				setResult(RESULT_OK);
				finish();
			}
		});
	}

	/**
	 * Load cached images from local storage.
	 * 
	 * @param db
	 *            local cache storage interface
	 */
	private void loadImagesFromCache(ImageDB db) {
		if (null == db) {
			Log.w(TAG, "unexpected error when loading images from cache.");
			return;
		}
		/**
		 * get widget images as bundle - legacy arch. use hardcoded sample size
		 * = 8 to reduce bitmap memory footprint
		 */
		Bundle bundle = db.getWidgetImages(appWidgetId, 4);
		bmps = (Bitmap[]) bundle.getParcelableArray("images");
		hydrateGridAdapter(bmps, adapter);
		if (bmps.length > 0) {
			header.setText(String.format(getText(R.string.feed_images_count)
					.toString(), bmps.length));
		}
	}

	/**
	 * Copy bitmaps array to image adapter.
	 * 
	 * @param bmps
	 * @param adapter
	 */
	private void hydrateGridAdapter(Bitmap[] bmps, ImageAdapter adapter) {
		adapter.clear();
		if (null == bmps) {
			return;
		}
		for (Bitmap bmp : bmps) {
			adapter.add(bmp);
		}
	}

	/**
	 * Moves the position of the bitmap in array and adapter.
	 * 
	 * @param bitmaps
	 * @param adapter
	 * @param position
	 * @param dir
	 */
	private void moveItemInAdapter(Bitmap[] bitmaps, ImageAdapter adapter,
			int position, int dir) {
		int imgCount = bitmaps.length;
		int newPos = position + dir;
		if (newPos > imgCount - 1 || newPos < 1) {
			return;
		}
		Bitmap swap = bitmaps[newPos].copy(bitmaps[newPos].getConfig(),
				bitmaps[newPos].isMutable());
		bitmaps[newPos].recycle();
		bitmaps[newPos] = bitmaps[position];
		bitmaps[position] = swap;
		hydrateGridAdapter(bitmaps, adapter);
	}

	/**
	 * Display the configure images options list.
	 * 
	 * @param position
	 *            position of image in grid
	 */
	private void launchMenu(int position) {
		Intent intent = new Intent(this, HSTFeedConfigureImagesOptions.class);
		intent.putExtra("position", position);
		startActivityForResult(intent, 20);
	}

	/**
	 * Sets header text based on items in adapter.
	 */
	private void buildHeaderText() {
		if (adapter.getCount() > 0) {
			header.setText(String.format(getText(R.string.feed_images_count)
					.toString(), adapter.getCount()));
		} else {
			header.setText(getText(R.string.feed_images_empty));
		}
	}
}