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
package com.longevitysoft.android.appwidget.hstfeed.handler;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService.HSTFeedXML;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedXMLWorkerHandler extends Handler {

	public static interface HSTFeedXMLWorkerListener {
		public void onFeedParseStart(final int appWidgetId, final int widgetSize);

		public void onFeedXMLLoaded(final int appWidgetId,
				final int widgetSize, final int numImages);

		public void onFeedImageLoaded(final int appWidgetId,
				final int widgetSize, final String imgSrc);

		public void onFeedAllImagesLoaded(final int appWidgetId,
				final int widgetSize, final HSTFeedXML feed);

		public void onFeedParseComplete(final int appWidgetId,
				final int widgetSize);

	}

	public static final int MSG_WHAT_FEED_PARSE_STARTED = 0;
	public static final int MSG_WHAT_FEED_PARSE_COMPLETE = 1;
	public static final int MSG_WHAT_FEED_XML_LOADED = 2;
	public static final int MSG_WHAT_FEED_IMAGE_LOADED = 3;
	public static final int MSG_WHAT_FEED_IMAGES_ALL_LOADED = 4;

	private List<HSTFeedXMLWorkerListener> listeners;

	public HSTFeedXMLWorkerHandler(int appWidgetId) {
		init();
	}

	public HSTFeedXMLWorkerHandler(Callback callback) {
		super(callback);
		init();
	}

	public HSTFeedXMLWorkerHandler(Looper looper, Callback callback) {
		super(looper, callback);
		init();
	}

	public HSTFeedXMLWorkerHandler(Looper looper) {
		super(looper);
		init();
	}

	/**
	 * Init this class.
	 */
	public void init() {
		listeners = new ArrayList<HSTFeedXMLWorkerListener>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.Handler#handleMessage(android.os.Message)
	 */
	@Override
	public void handleMessage(Message msg) {
		int appWidgetId = msg.arg1;
		int widgetSize = msg.arg2;
		switch (msg.what) {
		case MSG_WHAT_FEED_PARSE_STARTED:
			if (null != listeners) {
				for (HSTFeedXMLWorkerListener listener : listeners) {
					listener.onFeedParseStart(appWidgetId, widgetSize);
				}
			}
			break;
		case MSG_WHAT_FEED_XML_LOADED:
			if (null != listeners) {
				for (HSTFeedXMLWorkerListener listener : listeners) {
					listener.onFeedXMLLoaded(appWidgetId, widgetSize, (Integer) msg.obj);
				}
			}
			break;
		case MSG_WHAT_FEED_IMAGE_LOADED:
			if (null != listeners) {
				for (HSTFeedXMLWorkerListener listener : listeners) {
					listener.onFeedImageLoaded(appWidgetId, widgetSize, (String) msg.obj);
				}
			}
			break;
		case MSG_WHAT_FEED_IMAGES_ALL_LOADED:
			if (null != listeners) {
				for (HSTFeedXMLWorkerListener listener : listeners) {
					listener.onFeedAllImagesLoaded(appWidgetId, widgetSize, 
							(HSTFeedXML) msg.obj);
				}
			}
			break;
		case MSG_WHAT_FEED_PARSE_COMPLETE:
			if (null != listeners) {
				for (HSTFeedXMLWorkerListener listener : listeners) {
					listener.onFeedParseComplete(appWidgetId, widgetSize);
				}
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Add a listener to handler.
	 * 
	 * @param listener
	 */
	public void addListener(HSTFeedXMLWorkerListener listener) {
		if (null == listeners) {
			return;
		}
		listeners.add(listener);
	}

	/**
	 * Add a listener from handler.
	 * 
	 * @param listener
	 */
	public void removeListener(HSTFeedXMLWorkerListener listener) {
		if (null == listeners) {
			return;
		}
		listeners.remove(listener);
	}
}
