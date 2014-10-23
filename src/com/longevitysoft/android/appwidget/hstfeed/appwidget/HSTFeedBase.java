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
package com.longevitysoft.android.appwidget.hstfeed.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;

/**
 * @author fbeachler
 * 
 */
public abstract class HSTFeedBase extends AppWidgetProvider {
	public static final String TAG = "HSTFeed";

	/**
	 * @return widget size value as declared in {@link HSTFeedService}.
	 */
	public abstract int getWidgetSize();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.appwidget.AppWidgetProvider#onUpdate(android.content.Context,
	 * android.appwidget.AppWidgetManager, int[])
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(
					context, HSTFeedBase.class));
		}
		for (int appWidgetId : appWidgetIds) {
			Log.d(TAG, "onUpdate appWidgetId=" + appWidgetId + ", widgetSize="
					+ getWidgetSize());
			Intent intent = new Intent(context, HSTFeedService.class);
			intent.putExtra("widgetSize", getWidgetSize());
			intent.putExtra("appWidgetId", appWidgetId);
			context.startService(intent);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.appwidget.AppWidgetProvider#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		int appWidgetId = intent.getIntExtra(
				AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		Log.d(TAG, "onReceive action=" + intent.getAction() + ", appWidgetId="
				+ appWidgetId + ", widgetSize=" + getWidgetSize());
		if (AppWidgetManager.INVALID_APPWIDGET_ID != appWidgetId) {
			if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(intent
					.getAction())) {
				ImageDB db = ImageDB.getInstance(context);
				db.deleteWidget(appWidgetId);
				onDeleted(context, new int[] { appWidgetId });
			}
			if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(intent
					.getAction())
					|| AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent
							.getAction())) {
				ImageDB db = ImageDB.getInstance(context);
				Bundle widget = db.getWidget(appWidgetId);
				Intent servIntent = new Intent(context, HSTFeedService.class);
				servIntent.putExtra("appWidgetId", appWidgetId);
				if (null == widget) {
					// widget not saved, build empty view
					servIntent.putExtra("widgetSize",
							intent.getIntExtra("widgetSize", getWidgetSize()));
					context.startService(servIntent);
				} else {
					// only start service if provider same size as stored widget
					if (widget.getInt(ImageDBUtil.WIDGETS_SIZE) == getWidgetSize()) {
						// cycle images
						db.invalidateWidget(appWidgetId);
						db.needsUpdate(appWidgetId);
						// build remote view
						servIntent.putExtra("widgetSize", getWidgetSize());
						context.startService(servIntent);
					}
				}
			}
		}
	}
}