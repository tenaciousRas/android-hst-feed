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

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.longevitysoft.android.appwidget.hstfeed.R;

/**
 * Displays widget credits and info.
 * 
 * @author fbeachler
 * 
 */
public class HSTFeedWidgetInfo extends BaseActivity {

	@SuppressWarnings("unused")
	private static final String TAG = "HSTFeedWidgetInfo";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setup ui
		setContentView(R.layout.widget_info);
		TextView contentTxt = (TextView) findViewById(R.id.content);
		contentTxt.setText(Html
				.fromHtml(getString(R.string.widget_info_content)));
	}

}