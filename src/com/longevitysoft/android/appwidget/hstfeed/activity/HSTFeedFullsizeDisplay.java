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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.appwidget.AppWidgetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedFullsizeDisplay extends BaseActivity {

	private static final String TAG = "HSTFeedFullsizeDisplay";

	private int appWidgetId, size, imageId;
	private Bundle widget, imageData;
	private Float ra, dec, area;
	private ImageView fullImg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widget = getIntent().getBundleExtra("widget");
		appWidgetId = widget.getInt(ImageDBUtil.WIDGETS_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		size = getIntent().getIntExtra("size", HSTFeedService.SIZE_SMALL);
		imageData = getIntent().getBundleExtra("imageData");
		imageId = imageData.getInt(ImageDBUtil.IMAGES_ID);
		ra = widget.getFloat("ra", 0f);
		dec = widget.getFloat("dec", 0f);
		area = widget.getFloat("area", 0f);
		// setup ui
		setContentView(R.layout.fullscrn_img_layout);
		fullImg = (ImageView) findViewById(R.id.hst_img);
		TextView contentTxt = (TextView) findViewById(R.id.name);
		if (imageData.getString(ImageDBUtil.IMAGES_NAME) != null) {
			contentTxt.setVisibility(View.VISIBLE);
			contentTxt.setText(HSTFeedFullsizeDisplay.toTitleCase(imageData
					.getString(ImageDBUtil.IMAGES_NAME)));
		}
		contentTxt = (TextView) findViewById(R.id.caption);
		if (imageData.getString(ImageDBUtil.IMAGES_CAPTION) != null) {
			contentTxt.setVisibility(View.VISIBLE);
			contentTxt.setText(HSTFeedFullsizeDisplay
					.capitalizeFirstLetterInEverySentence(imageData
							.getString(ImageDBUtil.IMAGES_CAPTION)));
		}
		contentTxt = (TextView) findViewById(R.id.label_credits);
		contentTxt.setVisibility(View.VISIBLE);
		contentTxt = (TextView) findViewById(R.id.credits);
		if (imageData.getString(ImageDBUtil.IMAGES_CREDITS) != null) {
			contentTxt.setText(imageData.getString(ImageDBUtil.IMAGES_CREDITS));
		}
		contentTxt = (TextView) findViewById(R.id.ra);
		contentTxt.setText(Float.toString(ra));
		contentTxt = (TextView) findViewById(R.id.dec);
		contentTxt.setText(Float.toString(dec));
		contentTxt = (TextView) findViewById(R.id.area);
		contentTxt.setText(Float.toString(area));
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
		ImageDB db = ImageDB.getInstance(getBaseContext());
		Bitmap dbbm = db.getImageBitmap(appWidgetId, imageId);
		DisplayMetrics metrics = getBaseContext().getResources()
				.getDisplayMetrics();
		if (null != metrics && null != dbbm) {
			int scW = metrics.widthPixels;
			float imgRatio = ((float) dbbm.getHeight() / (float) dbbm
					.getWidth());
			fullImg.getLayoutParams().height = (int) (scW * imgRatio);
			fullImg.getLayoutParams().width = scW;
		} else {
			fullImg.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
			fullImg.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
		}
		fullImg.setImageBitmap(dbbm);
	}

	/**
	 * @param givenString
	 * @return
	 */
	public static String toTitleCase(String givenString) {
		String[] arr = givenString.split(" ");
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			sb.append(Character.toUpperCase(arr[i].charAt(0)))
					.append(arr[i].substring(1)).append(" ");
		}
		return sb.toString().trim();
	}

	/**
	 * @param content
	 * @return
	 */
	public static String capitalizeFirstLetterInEverySentence(String content) {
		Pattern capitalize = Pattern.compile("([\\?!\\.]\\s*)([a-z])");
		Matcher m = capitalize.matcher(content);
		while (m.find()) {
			content = m.replaceFirst(m.group(1)
					+ m.group(2).toUpperCase(Locale.US));
			m = capitalize.matcher(content);
		}
		// Capitalize the first letter of the string.
		content = String.format("%s%s",
				Character.toUpperCase(content.charAt(0)), content.substring(1));
		return content;
	}
}