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
 * credit to the original author Free Beachler and a link to HSTFeed
 * (http://www.hstfeed.net).  You may not use this work for commercial
 * purposes.  You also agree to use this work in a manner that does not conflict
 * with the official HSTFeed Android Application.  If you alter, transform, or build 
 * upon this work, you may distribute the resulting work only under the same 
 * or similar license to this one.
 *
 * You should have received a copy of the Creative Commons Non-Commercial
 * License along with HSTFeed.  If not, see 
 * <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package com.longevitysoft.android.appwidget.hstfeed.util;

import java.io.File;
import java.util.Vector;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.longevitysoft.android.appwidget.hstfeed.Constants;
import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.activity.HSTFeedWidgetTouchOptions;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedUtil {

	public static final String TAG = "HSTFeedUtil";

	public static final String CACHE_SUBDIR = "/imgcache";

	/**
	 * Modifies the input view with an onclick pendingintent.
	 * 
	 * @param view
	 * @param appWidgetId
	 * @param widgetSize
	 * @param widget
	 * @param imgData
	 * @return modifies the input view with an onclick, and returns the modified
	 *         view
	 */
	public static RemoteViews buildWidgetClickIntent(final RemoteViews view,
			Context ctx, final int appWidgetId, final int size,
			final Bundle widget, final Bundle imgData) {
		if (null == view) {
			return null;
		}
		RemoteViews ret = view;
		Intent configIntent = new Intent(ctx, HSTFeedWidgetTouchOptions.class);
		configIntent.putExtra("widgetSize", size);
		configIntent.putExtra("appWidgetId", appWidgetId);
		configIntent.putExtra("widget", widget);
		configIntent.putExtra("imageData", imgData);
		PendingIntent pending = PendingIntent.getActivity(ctx, 0, configIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		ret.setOnClickPendingIntent(R.id.widget_frame, pending);
		Log.v(TAG, "pending intent built for widget id=" + appWidgetId);
		return ret;
	}

	/**
	 * Calculate the optimal bitmap scale factor given widget widgetSize and
	 * bitmap boundaries.
	 * 
	 * @return
	 */
	public static int calcBitmapScaleFactor(int widgetSize,
			Vector<Integer> imgBounds) {
		int ret = 2;
		if (null == imgBounds || imgBounds.isEmpty()) {
			return ret;
		}
		int width = imgBounds.get(0);
		int height = imgBounds.get(1);
		switch (widgetSize) {
		case HSTFeedService.SIZE_LARGE:
			if (width > 1024) {
				ret = 2;
			}
			break;
		case HSTFeedService.SIZE_MEDIUM:
			if (width > 1024) {
				ret = 4;
			} else if (width < 512) {
				ret = 1;
			} else {
				ret = 2;
			}
			break;
		case HSTFeedService.SIZE_SMALL:
			if (width < 150) {
				ret = 1;
			} else {
				ret = 4;
			}
		default:
			break;
		}
		return ret;
	}

	/**
	 * @param appWidgetId
	 * @param imgId
	 * @return
	 */
	public static String buildImgFilePath(int appWidgetId, long imgId,
			Context ctx) {
		File f = new File(ctx.getFilesDir() + CACHE_SUBDIR);
		f.mkdir();
		f = new File(ctx.getFilesDir() + CACHE_SUBDIR, "img_" + appWidgetId
				+ Constants.UNDERSCORE + Long.toString(imgId) + ".png");
		return f.getAbsolutePath();
	}

}
