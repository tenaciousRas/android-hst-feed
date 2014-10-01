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

import net.hstfeed.R;
import net.hstfeed.adapter.ImageAdapter;
import net.hstfeed.provider.ImageDB;
import net.hstfeed.service.HSTFeedService;
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

/**
 * @author fbeachler
 * 
 */
public class HSTFeedConfigureImages extends BaseActivity {

	public static final String TAG = "HSTFeedConfigureImages";

	private GridView grid;
	private Button cancel, ok;
	private ImageAdapter adapter;
	private TextView header;

	private int mode, appWidgetId, size;
	private float ra, dec, area;
	private boolean edit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_images);
		size = getIntent().getIntExtra("size", HSTFeedService.SIZE_SMALL);
		edit = getIntent().getBooleanExtra("edit", false);
		ra = getIntent().getFloatExtra("ra", 0f);
		dec = getIntent().getFloatExtra("dec", 0f);
		area = getIntent().getFloatExtra("area", 0f);
		appWidgetId = getIntent().getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
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
				Intent updateIntent = new Intent(
						AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);
				if (updateIntent.resolveActivity(getPackageManager()) != null) {
					startActivity(updateIntent);
				}
				setResult(RESULT_OK);
				finish();
			}
		});

		ImageDB db = ImageDB.getInstance(this);
		Log.d(TAG, "edit mode=" + edit);
		if (!edit) {
			// create widget
			ok.setEnabled(true);
			db.createWidget(appWidgetId, HSTFeedConfigure.TYPE_LOCAL, 0, ra,
					dec, area, 0);
			// load images
		} else {
			// modify widget
			Bundle bundle = db.getWidgetImages(appWidgetId);
			Bitmap[] bmps = (Bitmap[]) bundle.getParcelableArray("images");
			adapter.clear();
			for (Bitmap image : bmps) {
				adapter.add(image);
			}
			if (adapter.getCount() > 0)
				header.setText(String.format(
						getText(R.string.feed_images_count).toString(),
						adapter.getCount()));
		}
	}

	@Override
	protected void onResume() {
		grid.invalidate();
		grid.invalidateViews();
		// ok.setEnabled(adapter.getCount() > 0);
		if (adapter.getCount() > 0)
			header.setText(String.format(getText(R.string.feed_images_count)
					.toString(), adapter.getCount()));
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putParcelableArray("images", adapter.toArray());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		Bitmap[] images = (Bitmap[]) savedInstanceState
				.getParcelableArray("images");
		adapter.clear();
		for (Bitmap image : images) {
			adapter.add(image);
		}
		header.setText(String.format(getText(R.string.feed_images_count)
				.toString(), adapter.getCount()));
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		menu.clear();
		switch (mode) {
		case HSTFeedConfigure.TYPE_LOCAL:
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
		if (mode == HSTFeedConfigure.TYPE_LOCAL) {
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

				startActivityForResult(grab, HSTFeedConfigure.REQUEST_IMAGES);
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
			int action = data.getIntExtra("action", -1);
			int position = data.getIntExtra("position", -1);
			if (position < 0) {
				return;
			}
			ImageDB db = ImageDB.getInstance(this);
			Bundle bundle;
			Bitmap[] bmps;
			switch (action) {
			case HSTFeedConfigureImagesOptions.ACTION_DELETE:
				adapter.deletePosition(position);
				db.deleteImage(appWidgetId, position);
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
				bundle = db.getWidgetImages(appWidgetId);
				bmps = (Bitmap[]) bundle.getParcelableArray("images");
				adapter.clear();
				for (Bitmap image : bmps) {
					adapter.add(image);
				}
				break;
			case HSTFeedConfigureImagesOptions.ACTION_PREV:
				db.moveImage(appWidgetId, position, -1);
				bundle = db.getWidgetImages(appWidgetId);
				bmps = (Bitmap[]) bundle.getParcelableArray("images");
				adapter.clear();
				for (Bitmap image : bmps) {
					adapter.add(image);
				}
				break;
			}
			grid.invalidate();
			grid.invalidateViews();
		} else {
			if (resultCode == RESULT_OK) {
				// FIXME bound to overflow memory - ~1MiB limit!
				Bitmap bitmap = (Bitmap) data.getParcelableExtra("data");
				String fullUri = data.getStringExtra("fullUri");
				String archvUri = data.getStringExtra("archvUri");
				String name = data.getStringExtra("name");
				String credits = data.getStringExtra("credits");
				String creditsUri = data.getStringExtra("creditsUri");
				String caption = data.getStringExtra("caption");
				String captionUri = data.getStringExtra("captionUri");
				ImageDB db = ImageDB.getInstance(this);
				db.setWidgetCurrent(appWidgetId, (int) db.setImage(appWidgetId,
						name, archvUri, fullUri, credits, creditsUri, caption,
						captionUri, -1, bitmap));
				adapter.add(bitmap);
				header.setText(String.format(
						getText(R.string.feed_images_count).toString(),
						adapter.getCount()));
			}
		}
	}

	private void launchMenu(int position) {
		Intent intent = new Intent(this, HSTFeedConfigureImagesOptions.class);
		intent.putExtra("position", position);
		startActivityForResult(intent, 20);
	}
}