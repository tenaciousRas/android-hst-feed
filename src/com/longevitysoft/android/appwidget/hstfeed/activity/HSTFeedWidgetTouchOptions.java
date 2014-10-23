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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.appwidget.HSTFeedBase;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
 * Display a list of options when the user clicks an instance of
 * {@link HSTFeedBase}.
 * 
 * @author fbeachler
 * 
 */
public class HSTFeedWidgetTouchOptions extends BaseActivity implements
		OnItemClickListener {

	private static final String TAG = "HSTFeedWidgetTouchOptions";

	private class StringAdapter extends BaseAdapter {

		protected int mode = 0; // 0 for partial items, 1 for full items

		protected String[] items_config_widget = {
				getString(R.string.configure_widget),
				getString(R.string.widget_info) };

		protected String[] items_config_all = {
				getString(R.string.view_fullsize),
				getString(R.string.next_image), getString(R.string.prev_image),
				getString(R.string.edit_images),
				getString(R.string.configure_widget),
				getString(R.string.widget_info) };

		private LayoutInflater mInflate;

		public StringAdapter(Context context) {
			mInflate = LayoutInflater.from(context);
		}

		/**
		 * @param mode
		 */
		public void setMode(int mode) {
			this.mode = mode;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getCount()
		 */
		public int getCount() {
			if (0 == mode) {
				return items_config_widget.length;
			} else {
				return items_config_all.length;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItem(int)
		 */
		public Object getItem(int position) {
			if (0 == mode) {
				return items_config_widget[position];
			} else {
				return items_config_all[position];
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.widget.Adapter#getView(int, android.view.View,
		 * android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				// if it's not recycled, initialize some attributes
				convertView = mInflate.inflate(
						android.R.layout.simple_list_item_1, parent, false);
				// styleHolder(holder);
			}
			TextView tx = (TextView) convertView
					.findViewById(android.R.id.text1);
			if (0 == mode) {
				tx.setText(items_config_widget[position]);
			} else {
				tx.setText(items_config_all[position]);
			}
			tx.setTag(convertView);

			return convertView;
		}
	}

	private Bundle widget, imageData;
	private ListView mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appWidgetId = getIntent().getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		widgetSize = getIntent().getIntExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE,
				HSTFeedService.SIZE_SMALL);
		widget = getIntent().getBundleExtra("widget");
		imageData = getIntent().getBundleExtra("imageData");
		// setup UI options list
		setContentView(R.layout.simple_list_view);
		StringAdapter adapter = new StringAdapter(this);
		if (null != imageData) {
			adapter.setMode(1);
		} else {
			adapter.setMode(0);
		}
		mList = (ListView) findViewById(R.id.mlist);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent configIntent = null;
		if (null == imageData) {
			// handle options displayed for empty widget
			switch (position) {
			case 0:
				// configure widget
				Log.d(TAG, "config widget");
				switch (widgetSize) {
				case HSTFeedService.SIZE_LARGE:
					configIntent = new Intent(this, HSTFeedConfigureLg.class);
					break;
				case HSTFeedService.SIZE_MEDIUM:
					configIntent = new Intent(this, HSTFeedConfigureMed.class);
					break;
				case HSTFeedService.SIZE_SMALL:
				default:
					configIntent = new Intent(this, HSTFeedConfigureSm.class);
					break;
				}
				configIntent.putExtra("appWidgetId", appWidgetId);
				configIntent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
				if (null == widget) {
					configIntent.putExtra("edit", false);
				} else {
					configIntent.putExtra("edit", true);
				}
				configIntent.putExtra("widget", widget);
				startActivity(configIntent);
				finish();
				break;
			case 1:
				// widget info
				Log.d(TAG, "widget info");
				configIntent = new Intent(this, HSTFeedWidgetInfo.class);
				startActivity(configIntent);
				finish();
				break;
			}
		} else {
			// handle options displayed for non-empty widget
			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			ImageDB db = ImageDB.getInstance(this);
			RemoteViews views = null;
			switch (position) {
			case 0:
				// view full size
				Log.d(TAG, "view fullsize");
				configIntent = new Intent(this, HSTFeedFullsizeDisplay.class);
				configIntent.putExtra("appWidgetId", appWidgetId);
				configIntent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
				configIntent.putExtra("widget", widget);
				configIntent.putExtra("imageData", imageData);
				startActivity(configIntent);
				finish();
				break;
			case 1:
				// next image
				Log.d(TAG, "next image");
				db.invalidateWidget(appWidgetId);
				db.needsUpdate(appWidgetId);
				Intent intent = new Intent(getBaseContext(),
						HSTFeedService.class);
				intent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
				intent.putExtra("appWidgetId", appWidgetId);
				startService(intent);
				finish();
				break;
			case 2:
				// prev image
				Log.d(TAG, "prev image");
				// set current to previous image
				db = ImageDB.getInstance(this);
				Integer prevId = db.getWidgetPreviousImage(appWidgetId,
						widget.getInt(ImageDBUtil.WIDGETS_CURRENT));
				if (null != prevId) {
					db.setWidgetCurrent(appWidgetId, prevId);
					db.invalidateWidget(appWidgetId);
					intent = new Intent(getBaseContext(), HSTFeedService.class);
					intent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
					intent.putExtra("appWidgetId", appWidgetId);
					startService(intent);
					manager.updateAppWidget(appWidgetId, views);
				} else {
					Log.d(TAG, "unable to set previous image, aborting");
				}
				finish();
				break;
			case 3:
				// configure images
				Log.d(TAG, "edit images");
				configIntent = new Intent(this, HSTFeedConfigureImages.class);
				configIntent.putExtra("appWidgetId", appWidgetId);
				configIntent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
				configIntent.putExtra("edit", true);
				configIntent.putExtra("widget", widget);
				startActivity(configIntent);
				finish();
				break;
			case 4:
				// configure widget
				Log.d(TAG, "config widget");
				switch (widgetSize) {
				case HSTFeedService.SIZE_LARGE:
					configIntent = new Intent(this, HSTFeedConfigureLg.class);
					break;
				case HSTFeedService.SIZE_MEDIUM:
					configIntent = new Intent(this, HSTFeedConfigureMed.class);
					break;
				case HSTFeedService.SIZE_SMALL:
				default:
					configIntent = new Intent(this, HSTFeedConfigureSm.class);
					break;
				}
				configIntent.putExtra("appWidgetId", appWidgetId);
				configIntent.putExtra(HSTFeedConfigureBase.INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
				configIntent.putExtra("edit", true);
				configIntent.putExtra("widget", widget);
				startActivity(configIntent);
				finish();
				break;
			case 5:
				// widget info
				Log.d(TAG, "widget info");
				configIntent = new Intent(this, HSTFeedWidgetInfo.class);
				startActivity(configIntent);
				finish();
			}
		}
	}
}