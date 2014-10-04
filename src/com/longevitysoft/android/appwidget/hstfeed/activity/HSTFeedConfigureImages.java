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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.adapter.ImageAdapter;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedConfigureImages extends BaseActivity {

	public static final String TAG = "HSTFeedConfigureImages";

	private Bitmap[] bmps;
	private GridView grid;
	private Button cancel, ok;
	private ImageAdapter adapter;
	private TextView header;

	private int mode;
	@SuppressWarnings("unused")
	private Bundle widget;
	// private float ra, dec, area;
	private boolean edit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_images);
		appWidgetId = getIntent().getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		size = getIntent().getIntExtra("size", HSTFeedService.SIZE_SMALL);
		edit = getIntent().getBooleanExtra("edit", false);
		widget = getIntent().getBundleExtra("widget");
		grid = (GridView) findViewById(R.id.images_grid);
		cancel = (Button) findViewById(R.id.config_images_cancel);
		ok = (Button) findViewById(R.id.config_images_go);
		header = (TextView) findViewById(R.id.images_count);
		adapter = new ImageAdapter(this);
		grid.setAdapter(adapter);
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
		ImageDB db = ImageDB.getInstance(this);
		Log.d(TAG, "edit mode=" + edit);
		if (!edit) {
			// edit new images
			ok.setEnabled(true);
			// TODO load images from feed
		} else {
			// modify images
			Bundle bundle = db.getWidgetImages(appWidgetId);
			bmps = (Bitmap[]) bundle.getParcelableArray("images");
			putBitmapsInAdapter(bmps, adapter);
			if (bmps.length > 0)
				header.setText(String.format(
						getText(R.string.feed_images_count).toString(),
						bmps.length));
		}
	}

	/**
	 * Copy bitmaps in the array to the image adapter.
	 * 
	 * @param bmps
	 * @param adapter
	 */
	private void putBitmapsInAdapter(Bitmap[] bmps, ImageAdapter adapter) {
		adapter.clear();
		if (null == bmps) {
			return;
		}
		for (Bitmap bmp : bmps) {
			adapter.add(bmp);
		}
	}

	@Override
	protected void onResume() {
		grid.invalidate();
		grid.invalidateViews();
		// ok.setEnabled(adapter.getCount() > 0);
		if (adapter.getCount() > 0) {
			header.setText(String.format(getText(R.string.feed_images_count)
					.toString(), adapter.getCount()));
		}
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArray("images", adapter.toArray());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		bmps = (Bitmap[]) savedInstanceState.getParcelableArray("images");
		putBitmapsInAdapter(bmps, adapter);
		header.setText(String.format(getText(R.string.feed_images_count)
				.toString(), adapter.getCount()));
		super.onRestoreInstanceState(savedInstanceState);
	}

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

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int dim = 0;
		switch (size) {
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
				startActivityForResult(grab, HSTFeedConfigureBase.REQUEST_IMAGES);
				break;
			}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 10 && resultCode == RESULT_OK) {
			Intent intent = new Intent(getBaseContext(), HSTFeedService.class);
			intent.putExtra("size", HSTFeedService.SIZE_SMALL);
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
				if (adapter.getCount() > 0) {
					header.setText(String.format(
							getText(R.string.feed_images_count).toString(),
							adapter.getCount()));
				} else {
					header.setText(getText(R.string.feed_images_empty));
				}
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
		putBitmapsInAdapter(bitmaps, adapter);
	}

	/**
	 * @param position
	 */
	private void launchMenu(int position) {
		Intent intent = new Intent(this, HSTFeedConfigureImagesOptions.class);
		intent.putExtra("position", position);
		startActivityForResult(intent, 20);
	}
}