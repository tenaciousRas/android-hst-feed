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
import net.hstfeed.provider.ImageDB;
import net.hstfeed.service.HSTFeedService;
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

/**
 * @author fbeachler
 * 
 */
public class HSTFeedImageTouchOptions extends BaseActivity implements
		OnItemClickListener {

	private static final String TAG = "HSTFeedImageTouchOptions";

	private int appWidgetId, size;
	private Float ra, dec, area;
	private ListView mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appWidgetId = getIntent().getIntExtra("wid",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		ra = getIntent().getFloatExtra("ra", 0f);
		dec = getIntent().getFloatExtra("dec", 0f);
		area = getIntent().getFloatExtra("area", 0f);
		size = getIntent().getIntExtra("size", HSTFeedService.SIZE_SMALL);
		// setup UI options list
		setContentView(R.layout.simple_list_view);
		StringAdapter adapter = new StringAdapter(this);
		mList = (ListView) findViewById(R.id.mlist);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		switch (position) {
		case 0:
			// next image
			Log.d(TAG, "Next image");
			ImageDB db = ImageDB.getInstance(this);
			db.invalidateWidget(appWidgetId);
			RemoteViews views = feedService.buildRemoteViews(this, appWidgetId);
			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(appWidgetId, views);
			db.invalidateWidget(appWidgetId);
			finish();
			break;
		case 1:
			// configure images
			Log.d(TAG, "Edit images");
			Intent configIntent = new Intent(this, HSTFeedConfigureImages.class);
			configIntent.putExtra("appWidgetId", appWidgetId);
			configIntent.putExtra("size", size);
			configIntent.putExtra("edit", true);
			configIntent.putExtra("ra", ra);
			configIntent.putExtra("dec", dec);
			configIntent.putExtra("area", area);
			startActivity(configIntent);
			finish();
			break;
		}
	}

	private class StringAdapter extends BaseAdapter {
		protected String[] items = { getString(R.string.next_image),
				getString(R.string.edit_images) };

		private LayoutInflater mInflate;

		public StringAdapter(Context context) {
			mInflate = LayoutInflater.from(context);
		}

		public int getCount() {
			return items.length;
		}

		public Object getItem(int position) {
			return items[position];
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) mInflate.inflate(
					android.R.layout.simple_list_item_1, null);
			view.setText(items[position]);
			view.setTag(view);

			return view;
		}
	}

}