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
import net.hstfeed.service.HSTFeedService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedConfigure extends BaseActivity {

	public static final int REQUEST_FEED = 1;
	public static final int REQUEST_LOCAL = 2;
	public static final int REQUEST_FEED_DOWNLOAD = 3;
	public static final int REQUEST_LOCAL_EDIT = 4;
	public static final int REQUEST_IMAGES = 5;

	public static final int ORDER_ORDERED = 0;
	public static final int ORDER_RANDOM = 1;

	public static final int TYPE_LOCAL = 0;

	EditText ra, dec, area;
	Button cancel, go;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_widget);

		size = getIntent().getIntExtra("size", HSTFeedService.SIZE_SMALL);
		appWidgetId = getIntent().getIntExtra(
				AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		ra = (EditText) findViewById(R.id.search_ra);
		dec = (EditText) findViewById(R.id.search_dec);
		area = (EditText) findViewById(R.id.search_area);
		cancel = (Button) findViewById(R.id.config_begin_cancel);
		go = (Button) findViewById(R.id.config_begin_go);

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setConfigResult(RESULT_CANCELED);
				finish();
			}
		});
		go.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HSTFeedConfigure.this,
						HSTFeedConfigureImages.class);
				intent.putExtra("appWidgetId", appWidgetId);
				intent.putExtra("size", size);
				String txtVal = null;
				Float fVal = 0f;
				txtVal = ra.getText().toString().trim();
				if (txtVal == null || txtVal.length() < 1) {
					txtVal = "0";
				}
				fVal = Float.parseFloat(txtVal);
				intent.putExtra("ra", fVal);
				txtVal = dec.getText().toString().trim();
				if (txtVal == null || txtVal.length() < 1) {
					txtVal = "0";
				}
				fVal = Float.parseFloat(txtVal);
				intent.putExtra("dec", fVal);
				txtVal = area.getText().toString().trim();
				if (txtVal == null || txtVal.length() < 1) {
					txtVal = "0";
				}
				fVal = Float.parseFloat(txtVal);
				intent.putExtra("area", fVal);
				startActivityForResult(intent, REQUEST_LOCAL);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			setConfigResult(resultCode);
			finish();
		}
	}

	/**
	 * @param resultCode
	 */
	private void setConfigResult(int resultCode) {
		final Intent intent = new Intent();
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(resultCode, intent);
	}
}