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

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.adapter.ImageAdapter;
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
public class HSTFeedConfigureImages extends BaseActivity {

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
		widgetSize = getIntent().getIntExtra("widgetSize",
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
		feedService.removeXMLWorkerListener(this);
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
			intent.putExtra("widgetSize", HSTFeedService.SIZE_SMALL);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * onFeedParseStart()
	 */
	@Override
	public void onFeedParseStart() {
		super.onFeedParseStart();
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
	public void onFeedXMLLoaded(final int numImages) {
		super.onFeedXMLLoaded(numImages);
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
	public void onFeedImageLoaded(final String imgSrc) {
		super.onFeedImageLoaded(imgSrc);
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
	public void onFeedAllImagesLoaded(final HSTFeedXML feed) {
		super.onFeedAllImagesLoaded(feed);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				header.setText(getString(R.string.download_image_list));
				ImageDB db = ImageDB.getInstance(getBaseContext());
				loadImagesFromCache(db);
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
	public void onFeedParseComplete() {
		super.onFeedParseComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.longevitysoft.android.appwidget.hstfeed.activity.BaseActivity#
	 * handleOnServiceConnected()
	 */
	@Override
	public void handleOnServiceConnected() {
		feedService.addXMLWorkerListener(this);
		ok.setEnabled(true);
		if (edit) {
			// edit cached images in existing widget
			ImageDB db = ImageDB.getInstance(this);
			loadImagesFromCache(db);
		} else {
			feedService.loadFeedInBackground(appWidgetId, widgetSize,
					widget.getFloat(ImageDBUtil.WIDGETS_RA),
					widget.getFloat(ImageDBUtil.WIDGETS_DEC),
					widget.getFloat(ImageDBUtil.WIDGETS_AREA));
		}
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
				Intent updateIntent = new Intent();
				updateIntent
						.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);
				getBaseContext().sendBroadcast(updateIntent);
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
		Bundle bundle = db.getWidgetImages(appWidgetId, 4); // hardcoded
		// sample
		// widgetSize
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