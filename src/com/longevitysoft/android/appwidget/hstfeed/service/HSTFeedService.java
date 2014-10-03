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
package com.longevitysoft.android.appwidget.hstfeed.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.longevitysoft.android.appwidget.hstfeed.Constants;
import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.activity.HSTFeedFullsizeDisplay;
import com.longevitysoft.android.appwidget.hstfeed.activity.HSTFeedWidgetTouchOptions;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;

/**
 * @author fbeachler
 * 
 */
public class HSTFeedService extends Service {

	public static final String TAG = "HSTFeedService";

	public static final String MAST_URL = "http://archive.stsci.edu/stpr/vo_search.php";

	public static final int SIZE_SMALL = 0;
	public static final int SIZE_MEDIUM = 1;
	public static final int SIZE_LARGE = 2;

	public static final int WHAT_REMOTE_VIEWS = 10;

	public static final String BUNDLE_NAME_REMOTE_VIEWS = "views.remote";

	public class LocalBinder extends Binder {
		public static final String TAG = HSTFeedService.TAG + "$LocalBinder";

		public HSTFeedService getService() {
			Log.i(TAG, "getService");
			return HSTFeedService.this;
		}
	}

	@SuppressWarnings("unused")
	private Handler activityHandler;
	private final IBinder binder = new LocalBinder();
	private AppWidgetManager manager;
	private DownloadHSTFeedXMLTask feedAsync;
	private boolean downloadInProgress;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent == null) {
			return;
		}
		int appWidgetId = intent.getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		if (AppWidgetManager.INVALID_APPWIDGET_ID == appWidgetId) {
			return;
		}
		manager = AppWidgetManager.getInstance(this);
		int size = intent.getIntExtra("size", SIZE_SMALL);
		RemoteViews update = buildRemoteViews(this, appWidgetId, size);
		manager.updateAppWidget(appWidgetId, update);
	}

	/**
	 * Set the activity that handles messages from this service.
	 * 
	 * @param activityHandler
	 */
	public void setActivityHandler(Handler activityHandler) {
		this.activityHandler = activityHandler;
	}

	/**
	 * @param context
	 * @param appWidgetId
	 * @return
	 */
	public RemoteViews buildRemoteViews(Context context, int appWidgetId,
			int size) {
		ImageDB db = ImageDB.getInstance(context);
		Bundle widget = db.getWidget(appWidgetId);
		if (null == widget) {
			return buildEmptyView(context, appWidgetId, size);
		}
		RemoteViews view = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout);
		int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
		float ra = widget.getFloat(ImageDBUtil.WIDGETS_RA);
		float dec = widget.getFloat(ImageDBUtil.WIDGETS_DEC);
		float area = widget.getFloat(ImageDBUtil.WIDGETS_AREA);
		// set text views
		view.setTextViewText(R.id.ra, Float.toString(ra) + Constants.SEMICOLON
				+ Constants.SPACE);
		view.setTextViewText(R.id.dec, Float.toString(dec)
				+ Constants.SEMICOLON + Constants.SPACE);
		view.setTextViewText(R.id.area,
				Float.toString(area) + context.getString(R.string.sym_degree));
		if (db.needsUpdate(appWidgetId)) {
			// set bitmap
			Bundle imgData = db.getImageMeta(appWidgetId, current);
			if (imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null
					&& !downloadInProgress) {
				// load feed+images in background
				feedAsync = new DownloadHSTFeedXMLTask();
				feedAsync.setManager(manager);
				downloadInProgress = true;
				feedAsync.execute(Integer.toString(appWidgetId),
						Integer.toString(size), MAST_URL, Float.toString(ra),
						Float.toString(dec), Float.toString(area));
				// set loading msg
				view.setViewVisibility(R.id.hst_img_loading, View.VISIBLE);
				view.setViewVisibility(R.id.label_credits, View.INVISIBLE);
				view.setViewVisibility(R.id.credits, View.INVISIBLE);
				view.setViewVisibility(R.id.name, View.INVISIBLE);
				view.setImageViewResource(R.id.hst_img,
						R.drawable.ic_feed_loading);
				view = buildImageViewClickIntent(view, appWidgetId, size,
						widget, null);
			} else {
				if (imgData.getString(ImageDBUtil.IMAGES_NAME) != null) {
					view.setTextViewText(R.id.name,
							imgData.getString(ImageDBUtil.IMAGES_NAME));
				}
				if (imgData.getString(ImageDBUtil.IMAGES_CREDITS) != null) {
					view.setTextViewText(R.id.credits,
							imgData.getString(ImageDBUtil.IMAGES_CREDITS));
				}
				Bitmap dbbm = db.getImageBitmap(appWidgetId, current);
				if (dbbm != null) {
					view.setViewVisibility(R.id.hst_img_loading, View.INVISIBLE);
					view.setViewVisibility(R.id.label_credits, View.VISIBLE);
					view.setViewVisibility(R.id.credits, View.VISIBLE);
					view.setViewVisibility(R.id.name, View.VISIBLE);
					view.setImageViewBitmap(R.id.hst_img, dbbm);
				}
				// set click intent
				view = buildImageViewClickIntent(view, appWidgetId, size,
						widget, imgData);
			}
		}
		return view;
	}

	private class DownloadHSTFeedXMLTask extends
			AsyncTask<String, Void, Integer> {

		private AppWidgetManager manager;
		private int appWidgetId = -1;
		private int size = SIZE_SMALL;

		@Override
		protected Integer doInBackground(String... params) {
			Log.d(TAG, "doInBackground started");
			// download feed
			appWidgetId = Integer.parseInt(params[0]);
			size = Integer.parseInt(params[1]);
			ImageDB db = ImageDB.getInstance(getBaseContext());
			String url = params[2] + "?";
			url += "pos=" + params[3] + Constants.COMMA + params[4] + "&size="
					+ params[5];
			Log.i(TAG, "download feed from url=" + url);
			List<HSTImage> feedImages = downloadFeedXML(url);
			int insertCnt = 0;
			// download images
			if (feedImages != null) {
				for (int i = 0; i < feedImages.size(); i++) {
					byte[] imgData = downloadImage(
							transformArchiveUrlToImageUrl(feedImages.get(i)
									.getArchiveUrl())).toByteArray();
					if (imgData.length > 0) {
						// store immediately
						db.setImage(appWidgetId, feedImages.get(i).getName(),
								feedImages.get(i).getArchiveUrl(), feedImages
										.get(i).getFullUrl(), feedImages.get(i)
										.getCredits(), feedImages.get(i)
										.getCreditsUrl(), feedImages.get(i)
										.getCaption(), feedImages.get(i)
										.getCaptionUrl(), insertCnt, imgData);
						insertCnt++;
					}
				}
			}
			Log.d(TAG, "doInBackground finished");
			return insertCnt;
		}

		@Override
		protected void onPostExecute(Integer insertCnt) {
			Log.d(TAG, "onPostExecute started");
			ImageDB db = ImageDB.getInstance(getBaseContext());
			Bundle widget = db.getWidget(appWidgetId);
			if (null == widget) {
				Log.w(TAG,
						"something is awry, could not retrieve widget that was just updated");
				return;
			}
			RemoteViews view = null;
			if (insertCnt != null && insertCnt > 0 && appWidgetId > -1) {
				int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
				view = new RemoteViews(getBaseContext().getPackageName(),
						R.layout.widget_layout);
				Bitmap dbbm = db.getImageBitmap(appWidgetId, current);
				Bundle imgData = db.getImageMeta(appWidgetId, current);
				if (imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null) {
					Log.w(TAG,
							"something is awry - could not get the cached file path for the file just downloaded");
					downloadInProgress = false;
					return;
				}
				if (dbbm != null) {
					view.setViewVisibility(R.id.hst_img_loading, View.INVISIBLE);
					view.setViewVisibility(R.id.label_credits, View.VISIBLE);
					view.setViewVisibility(R.id.credits, View.VISIBLE);
					view.setViewVisibility(R.id.name, View.VISIBLE);
					view.setImageViewBitmap(R.id.hst_img, dbbm);
					if (imgData.getString(ImageDBUtil.IMAGES_NAME) != null) {
						view.setTextViewText(R.id.name, HSTFeedFullsizeDisplay
								.toTitleCase(imgData
										.getString(ImageDBUtil.IMAGES_NAME)));
					}
					if (imgData.getString(ImageDBUtil.IMAGES_CREDITS) != null) {
						view.setTextViewText(R.id.credits,
								imgData.getString(ImageDBUtil.IMAGES_CREDITS));
					}
					// set click intent
					view = buildImageViewClickIntent(view, appWidgetId, size,
							widget, imgData);
				}
			} else if (insertCnt != null && insertCnt < 1) {
				view = new RemoteViews(getBaseContext().getPackageName(),
						R.layout.widget_layout_empty);
				view.setTextViewText(R.id.content, String.format(
						getString(R.string.no_images_found),
						widget.getFloat("ra"), widget.getFloat("dec"),
						widget.getFloat("area")));
				view = buildImageViewClickIntent(view, appWidgetId, size,
						widget, null);
			}
			if (null != view) {
				// update widget
				manager.updateAppWidget(appWidgetId, view);
			}
			downloadInProgress = false;
			Log.d(TAG, "onPostExecute finished");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			super.onCancelled();
			downloadInProgress = false;
		}

		/**
		 * @param manager
		 *            the manager to set
		 */
		public void setManager(AppWidgetManager manager) {
			this.manager = manager;
		}

		/**
		 * Download an image. Limits the image file size to 2MB, pretty much any
		 * compressed format like JPG larger than this will cause an out of
		 * memory exception by the AOSP BitmapFactory#decode and #compress
		 * methods.
		 * 
		 * @param url
		 * @return
		 */
		private ByteArrayOutputStream downloadImage(String url) {
			Log.i(TAG, "download image from url=" + url);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				HttpURLConnection con = (HttpURLConnection) (new URL(url))
						.openConnection();
				con.setRequestMethod(HttpGet.METHOD_NAME);
				con.setDoInput(true);
				con.setDoOutput(false);
				con.connect();
				if (con.getContentLength() > 10 * 1024 * 1024) {
					// TODO place oversized error image
					return baos;
				}
				InputStream is = con.getInputStream();
				byte[] b = new byte[1024];
				while (is.read(b) != -1) {
					baos.write(b);
				}
				con.disconnect();
			} catch (MalformedURLException e) {
				Log.e(TAG, Log.getStackTraceString(e));
			} catch (IOException e) {
				Log.e(TAG, Log.getStackTraceString(e));
			}
			return baos;
		}

		/**
		 * Download a MAST feed XML file. For example:
		 * http://archive.stsci.edu/stpr/vo_search.php?pos=149.0,69.0&size=5.0
		 * 
		 * @param url
		 * @return
		 */
		private List<HSTImage> downloadFeedXML(String url) {
			HttpURLConnection con;
			List<HSTImage> ret = new ArrayList<HSTImage>();
			try {
				// get XML feed
				con = (HttpURLConnection) (new URL(url)).openConnection();
				con.setDoInput(true);
				con.setDoOutput(false);
				con.setRequestMethod(HttpGet.METHOD_NAME);
				con.connect();
				ret = parseMASTFeed(con.getInputStream());
				con.disconnect();
			} catch (MalformedURLException e) {
				Log.e(TAG, Log.getStackTraceString(e));
			} catch (IOException e) {
				Log.e(TAG, Log.getStackTraceString(e));
			}
			return ret;
		}

		/**
		 * Parse a MAST HST PR feed. Example:
		 * http://archive.stsci.edu/stpr/vo_search.php?pos=129.0,69.0&size=25.0
		 * 
		 * @param is
		 * @return
		 */
		public List<HSTImage> parseMASTFeed(InputStream is) {
			List<HSTImage> ret = new ArrayList<HSTImage>();
			XmlPullParserFactory factory = null;
			XmlPullParser parser = null;
			HSTImage hstImg = null;
			String text = null;
			boolean hasJPGImg = false;
			int tdCnt = 0;
			try {
				factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				parser = factory.newPullParser();
				parser.setInput(is, null);
				int eventType = parser.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					String tagname = parser.getName();
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if (tagname.equalsIgnoreCase("tr")) {
							hstImg = new HSTImage();
						}
						if (tagname.equalsIgnoreCase("td")) {
							tdCnt++;
						}
						break;
					case XmlPullParser.TEXT:
						text = parser.getText().toLowerCase(Locale.US);
						break;
					case XmlPullParser.END_TAG:
						if (tagname.equalsIgnoreCase("tr")) {
							if (hasJPGImg) {
								ret.add(hstImg);
							}
							tdCnt = 0;
							hasJPGImg = false;
						} else if (tagname.equalsIgnoreCase("td")) {
							if (tdCnt == 4) {
								hstImg.setFullUrl(text);
								if (text.endsWith("jpeg")
										|| text.endsWith("jpg")
										|| text.endsWith("png")
										|| text.endsWith("gif")) {
									hasJPGImg = true;
								}
							} else if (tdCnt == 6) {
								hstImg.setName(text);
							} else if (tdCnt == 8) {
								hstImg.setCaption(text);
							} else if (tdCnt == 10) {
								hstImg.setArchiveUrl(text);
							} else if (tdCnt == 33) {
								hstImg.setCredits(text);
							}
						}
						break;
					default:
						break;
					}
					eventType = parser.next();
				}
			} catch (XmlPullParserException e) {
				Log.e(TAG, Log.getStackTraceString(e));
			} catch (IOException e) {
				Log.e(TAG, Log.getStackTraceString(e));
			}

			return ret;
		}

		/**
		 * Turn a HST archive web page URL with cutouts to a URL for large raw
		 * image cutout.
		 * 
		 * <pre>
		 * Some examples:
		 * http://hubblesite.org/newscenter/archive/releases/2008/02/image/b/format/web/
		 * 	http://imgsrc.hubblesite.org/hu/db/images/hs-2008-02-b-web.jpg
		 * http://hubblesite.org/newscenter/archive/releases/2010/05/image/a/format/large_web/
		 *  http://imgsrc.hubblesite.org/hu/db/images/hs-2010-05-a-large_web.jpg
		 * http://hubblesite.org/newscenter/archive/releases/2010/05/image/a/format/small_web/
		 *  http://imgsrc.hubblesite.org/hu/db/images/hs-2010-05-a-small_web.jpg
		 * </pre>
		 * 
		 * @param url
		 * @return
		 */
		private String transformArchiveUrlToImageUrl(final String url) {
			if (url == null) {
				return null;
			}
			Uri parsed = Uri.parse(url);
			StringBuilder transformed = new StringBuilder()
					.append("http://imgsrc.hubblesite.org/hu/db/images/");
			transformed.append("hs-");
			transformed.append(parsed.getPathSegments().get(3));
			transformed.append("-");
			transformed.append(parsed.getPathSegments().get(4));
			transformed.append("-");
			transformed.append(parsed.getPathSegments().get(6));
			transformed.append("-web.jpg");
			return transformed.toString();
		}
	}

	/**
	 * Modifies the input view with an onclick pendingintent.
	 * 
	 * @param view
	 * @param appWidgetId
	 * @param size
	 * @param widget
	 * @param imgData
	 * @return modifies the input view with an onclick, and returns the modified
	 *         view
	 */
	private RemoteViews buildImageViewClickIntent(final RemoteViews view,
			final int appWidgetId, final int size, final Bundle widget,
			final Bundle imgData) {
		if (null == view) {
			return null;
		}
		RemoteViews ret = view;
		Intent configIntent = new Intent(getBaseContext(),
				HSTFeedWidgetTouchOptions.class);
		configIntent.putExtra("size", size);
		configIntent.putExtra("appWidgetId", appWidgetId);
		configIntent.putExtra("widget", widget);
		configIntent.putExtra("imageData", imgData);
		PendingIntent pending = PendingIntent.getActivity(getBaseContext(), 0,
				configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		ret.setOnClickPendingIntent(R.id.widget_frame, pending);
		return ret;
	}

	/**
	 * @param context
	 * @return
	 */
	private RemoteViews buildEmptyView(Context context, int appWidgetId,
			int size) {
		RemoteViews view = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout_empty);
		view = buildImageViewClickIntent(view, appWidgetId, size, null, null);
		return view;
	}

}