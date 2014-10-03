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
package com.longevitysoft.android.appwidget.hstfeed.appwidget;

import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author fbeachler
 * 
 */
public class HSTFeed extends AppWidgetProvider {
	public static final String TAG = "HSTFeed";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(
					context, HSTFeed.class));
		}
		for (int appWidgetId : appWidgetIds) {
			Intent intent = new Intent(context, HSTFeedService.class);
			intent.putExtra("size", HSTFeedService.SIZE_SMALL);
			intent.putExtra("appWidgetId", appWidgetId);
			context.startService(intent);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.d(TAG, "onReceive action=" + intent.getAction());
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED
				.equals(intent.getAction())) {
			int appWidgetId = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			ImageDB db = ImageDB.getInstance(context);
			db.deleteWidget(appWidgetId);
			onDeleted(context, new int[] { appWidgetId });
		}
		if (AppWidgetManager.ACTION_APPWIDGET_ENABLED
				.equals(intent.getAction())
				|| AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent
						.getAction())) {
			int appWidgetId = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			Intent servIntent = new Intent(context, HSTFeedService.class);
			servIntent.putExtra("size", HSTFeedService.SIZE_SMALL);
			servIntent.putExtra("appWidgetId", appWidgetId);
			context.startService(servIntent);
		}
	}
}