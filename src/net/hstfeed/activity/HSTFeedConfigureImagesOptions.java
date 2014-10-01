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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedConfigureImagesOptions extends BaseActivity implements
		OnItemClickListener {

	public static final String TAG = "HSTFeedConfigureImagesOptions";

	public static final int ACTION_DELETE = 0;
	public static final int ACTION_NEXT = 1;
	public static final int ACTION_PREV = 2;

	private ListView mList;
	private int mPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPosition = getIntent().getIntExtra("position", -1);
		if (mPosition < 0) {
			finish();
		}
		// setup UI options list
		setContentView(R.layout.simple_list_view);
		StringAdapter adapter = new StringAdapter(this);
		mList = (ListView) findViewById(R.id.mlist);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent();
		intent.putExtra("position", mPosition);

		int result = RESULT_OK;
		switch (position) {
		case 0:
			// delete
			intent.putExtra("action", ACTION_DELETE);
			break;
		case 1:
			// move ok
			intent.putExtra("action", ACTION_NEXT);
			break;
		case 2:
			// move prev
			intent.putExtra("action", ACTION_PREV);
			break;
		default:
			result = RESULT_CANCELED;
		}
		setResult(result, intent);
		finish();
	}

	private class StringAdapter extends BaseAdapter {
		String[] items = { getString(R.string.delete_image),
				getString(R.string.next_image),
				getString(R.string.prev_image) };

		LayoutInflater mInflate;

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