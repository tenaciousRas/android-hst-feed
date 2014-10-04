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
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
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
				getString(R.string.next_image),
				getString(R.string.edit_images),
				getString(R.string.configure_widget),
				getString(R.string.widget_info) };

		private LayoutInflater mInflate;

		public StringAdapter(Context context) {
			mInflate = LayoutInflater.from(context);
		}

		public void setMode(int mode) {
			this.mode = mode;
		}

		public int getCount() {
			if (0 == mode) {
				return items_config_widget.length;
			} else {
				return items_config_all.length;
			}
		}

		public Object getItem(int position) {
			if (0 == mode) {
				return items_config_widget[position];
			} else {
				return items_config_all[position];
			}
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) mInflate.inflate(
					android.R.layout.simple_list_item_1, null);
			if (0 == mode) {
				view.setText(items_config_widget[position]);
			} else {
				view.setText(items_config_all[position]);
			}
			view.setTag(view);

			return view;
		}
	}

	private Bundle widget, imageData;
	private ListView mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appWidgetId = getIntent().getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		size = getIntent().getIntExtra("size", HSTFeedService.SIZE_SMALL);
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
			switch (position) {
			case 0:
				// configure widget
				Log.d(TAG, "config widget");
				switch (size) {
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
				configIntent.putExtra("size", size);
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
			switch (position) {
			case 0:
				// view full size
				Log.d(TAG, "view fullsize");
				configIntent = new Intent(this, HSTFeedFullsizeDisplay.class);
				configIntent.putExtra("appWidgetId", appWidgetId);
				configIntent.putExtra("size", size);
				configIntent.putExtra("widget", widget);
				configIntent.putExtra("imageData", imageData);
				startActivity(configIntent);
				finish();
				break;
			case 1:
				// next image
				Log.d(TAG, "next image");
				ImageDB db = ImageDB.getInstance(this);
				db.invalidateWidget(appWidgetId);
				RemoteViews views = feedService.buildRemoteViews(this,
						appWidgetId, size);
				AppWidgetManager manager = AppWidgetManager.getInstance(this);
				manager.updateAppWidget(appWidgetId, views);
				db.invalidateWidget(appWidgetId);
				finish();
				break;
			case 2:
				// configure images
				Log.d(TAG, "edit images");
				configIntent = new Intent(this, HSTFeedConfigureImages.class);
				configIntent.putExtra("appWidgetId", appWidgetId);
				configIntent.putExtra("size", size);
				configIntent.putExtra("edit", true);
				configIntent.putExtra("widget", widget);
				startActivity(configIntent);
				finish();
				break;
			case 3:
				// configure widget
				Log.d(TAG, "config widget");
				switch (size) {
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
				configIntent.putExtra("size", size);
				configIntent.putExtra("edit", true);
				configIntent.putExtra("widget", widget);
				startActivity(configIntent);
				finish();
				break;
			case 4:
				// widget info
				Log.d(TAG, "widget info");
				configIntent = new Intent(this, HSTFeedWidgetInfo.class);
				startActivity(configIntent);
				finish();
			}
		}
	}

}