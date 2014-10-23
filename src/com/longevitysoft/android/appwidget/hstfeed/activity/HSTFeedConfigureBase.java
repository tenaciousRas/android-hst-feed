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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;

/**
 * @author fbeachler
 * 
 */
public abstract class HSTFeedConfigureBase extends BaseActivity {

	public static final int REQUEST_FEED = 1;
	public static final int REQUEST_LOCAL = 2;
	public static final int REQUEST_FEED_DOWNLOAD = 3;
	public static final int REQUEST_LOCAL_EDIT = 4;
	public static final int REQUEST_IMAGES = 5;

	public static final int ORDER_ORDERED = 0;
	public static final int ORDER_RANDOM = 1;

	public static final int TYPE_LOCAL = 0;

	public static final String INTENT_EXTRA_NAME_WIDGET_CONFIG_RESET = "configReset";
	public static final String INTENT_EXTRA_NAME_WIDGET_SIZE = "widgetSize";

	private EditText ra, dec, area;
	private Button cancel, go;
	private Float origRa, origDec, origArea;
	private Bundle widget;

	protected abstract void readSizeFromIntent(Intent intent);

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readSizeFromIntent(getIntent());
		appWidgetId = getIntent().getIntExtra(
				AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
		edit = getIntent().getBooleanExtra("edit", false);
		widget = getIntent().getBundleExtra("widget");
		if (!edit) {
			origRa = origDec = origArea = -1f;
		} else {
			origRa = widget.getFloat("ra", -1f);
			origDec = widget.getFloat("dec", -1f);
			origArea = widget.getFloat("area", -1f);
		}
		setContentView(R.layout.config_widget);
		ra = (EditText) findViewById(R.id.search_ra);
		dec = (EditText) findViewById(R.id.search_dec);
		area = (EditText) findViewById(R.id.search_area);
		cancel = (Button) findViewById(R.id.config_begin_cancel);
		go = (Button) findViewById(R.id.config_begin_go);
		final ImageDB db = ImageDB.getInstance(getBaseContext());
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
				Intent intent = new Intent(HSTFeedConfigureBase.this,
						HSTFeedConfigureImages.class);
				intent.putExtra("appWidgetId", appWidgetId);
				intent.putExtra(INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
				intent.putExtra("edit", edit);
				if (null == widget) {
					widget = new Bundle();
				}
				String txtVal = null;
				Float fVal = 0f;
				txtVal = ra.getText().toString().trim();
				if (txtVal == null || txtVal.length() < 1) {
					txtVal = "0";
				}
				fVal = Float.parseFloat(txtVal);
				widget.putFloat("ra", fVal);
				txtVal = dec.getText().toString().trim();
				if (txtVal == null || txtVal.length() < 1) {
					txtVal = "0";
				}
				fVal = Float.parseFloat(txtVal);
				widget.putFloat("dec", fVal);
				txtVal = area.getText().toString().trim();
				if (txtVal == null || txtVal.length() < 1) {
					txtVal = "0";
				}
				fVal = Float.parseFloat(txtVal);
				widget.putFloat("area", fVal);
				if (!edit) {
					// store widget
					db.createWidget(appWidgetId, widgetSize,
							HSTFeedConfigureBase.TYPE_LOCAL, 0,
							widget.getFloat("ra", 0f),
							widget.getFloat("dec", 0f),
							widget.getFloat("area", 0f), 0);
				} else {
					// update existing widget
					db.updateWidget(appWidgetId, 0, widget.getFloat("ra", 0f),
							widget.getFloat("dec", 0f),
							widget.getFloat("area", 0f), 0);
					if (!origRa.equals(widget.getFloat("ra"))
							|| !origDec.equals(widget.getFloat("dec"))
							|| !origArea.equals(widget.getFloat("area"))) {
						// delete cached images if ra, dec, or area change
						// FIXME could get ANR here
						db.deleteAllImages(appWidgetId);
						intent.putExtra(INTENT_EXTRA_NAME_WIDGET_CONFIG_RESET, true);
					}
				}
				// launch next activity
				intent.putExtra("widget", widget);
				startActivityForResult(intent, REQUEST_LOCAL);
			}
		});
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
		if (edit) {
			ra.setText(Float.toString(getIntent().getBundleExtra("widget")
					.getFloat("ra")));
			dec.setText(Float.toString(getIntent().getBundleExtra("widget")
					.getFloat("dec")));
			area.setText(Float.toString(getIntent().getBundleExtra("widget")
					.getFloat("area")));
		}
		go.requestFocus(); // first field being edit text causes keyboard to
							// hide UI
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
			if (!edit) {
				setConfigResult(resultCode);
			}
			Intent updateIntent = new Intent();
			updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);
			updateIntent.putExtra(INTENT_EXTRA_NAME_WIDGET_SIZE, widgetSize);
			getBaseContext().sendBroadcast(updateIntent);
			finish();
		}
	}

	/**
	 * @param resultCode
	 */
	private void setConfigResult(int resultCode) {
		final Intent intent = new Intent();
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_ENABLED);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(resultCode, intent);
	}

}