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
package net.hstfeed.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.hstfeed.Constants;
import net.hstfeed.R;
import net.hstfeed.provider.ImageDB;
import net.hstfeed.provider.ImageDBUtil;

import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
import android.widget.RemoteViews;

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

	public static class HSTImage {
		private String name;
		private String archiveUrl;
		private String fullUrl;
		private String filePath;
		private String caption;
		private String captionUrl;
		private String credits;
		private String creditsUrl;
		private Bitmap img;

		/**
		 * @return the url
		 */
		public String getFullUrl() {
			return fullUrl;
		}

		/**
		 * @param url
		 *            the url to set
		 */
		public void setFullUrl(String fullUrl) {
			this.fullUrl = fullUrl;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the caption
		 */
		public String getCaption() {
			return caption;
		}

		/**
		 * @param caption
		 *            the caption to set
		 */
		public void setCaption(String caption) {
			this.caption = caption;
		}

		/**
		 * @return the captionUrl
		 */
		public String getCaptionUrl() {
			return captionUrl;
		}

		/**
		 * @param captionUrl
		 *            the captionUrl to set
		 */
		public void setCaptionUrl(String captionUrl) {
			this.captionUrl = captionUrl;
		}

		/**
		 * @return the archiveUrl
		 */
		public String getArchiveUrl() {
			return archiveUrl;
		}

		/**
		 * @param archiveUrl
		 *            the archiveUrl to set
		 */
		public void setArchiveUrl(String archiveUrl) {
			this.archiveUrl = archiveUrl;
		}

		/**
		 * @return the credits
		 */
		public String getCredits() {
			return credits;
		}

		/**
		 * @param credits
		 *            the credits to set
		 */
		public void setCredits(String credits) {
			this.credits = credits;
		}

		/**
		 * @return the filePath
		 */
		public String getFilePath() {
			return filePath;
		}

		/**
		 * @param filePath
		 *            the filePath to set
		 */
		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		/**
		 * @return the creditsUrl
		 */
		public String getCreditsUrl() {
			return creditsUrl;
		}

		/**
		 * @param creditsUrl
		 *            the creditsUrl to set
		 */
		public void setCreditsUrl(String creditsUrl) {
			this.creditsUrl = creditsUrl;
		}

		/**
		 * @return the img
		 */
		public Bitmap getImg() {
			return img;
		}

		/**
		 * @param img
		 *            the img to set
		 */
		public void setImg(Bitmap img) {
			this.img = img;
		}
	}

	public class LocalBinder extends Binder {
		public static final String TAG = HSTFeedService.TAG + "$LocalBinder";

		public HSTFeedService getService() {
			Log.i(TAG, "getService");
			return HSTFeedService.this;
		}
	}

	@SuppressWarnings("unused")
	private int size;
	@SuppressWarnings("unused")
	private Handler activityHandler;
	private final IBinder binder = new LocalBinder();
	private DownloadHSTFeedXMLTask feedAsync;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent == null) {
			return;
		}
		int appWidgetId = intent.getIntExtra("appWidgetId",
				AppWidgetManager.INVALID_APPWIDGET_ID);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		size = intent.getIntExtra("size", SIZE_SMALL);
		feedAsync = new DownloadHSTFeedXMLTask();
		feedAsync.setManager(manager);
		RemoteViews update = buildRemoteViews(this, appWidgetId);
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
	public RemoteViews buildRemoteViews(Context context, int appWidgetId) {
		RemoteViews view = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout);
		ImageDB db = ImageDB.getInstance(context);
		if (!db.isWidgetSaved(appWidgetId)) {
			return view;
		}
		if (db.needsUpdate(appWidgetId) && !feedAsync.isExecuting()) {
			Bundle widget = db.getWidget(appWidgetId);
			int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
			float ra = widget.getFloat(ImageDBUtil.WIDGETS_RA);
			float dec = widget.getFloat(ImageDBUtil.WIDGETS_DEC);
			float area = widget.getFloat(ImageDBUtil.WIDGETS_AREA);
			// set text views
			view.setTextViewText(R.id.ra, Float.toString(ra) + Constants.SEMICOLON + Constants.SPACE);
			view.setTextViewText(R.id.dec, Float.toString(dec)
					+ Constants.SEMICOLON + Constants.SPACE);
			view.setTextViewText(
					R.id.area,
					Float.toString(area)
							+ context.getString(R.string.sym_degree));
			// set bitmap
			Bundle imgData = db.getImageMeta(appWidgetId, current);
			if (imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null) {
				feedAsync.execute(Integer.toString(appWidgetId), MAST_URL,
						Float.toString(ra), Float.toString(dec),
						Float.toString(area));
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
					view.setImageViewBitmap(R.id.hst_img, dbbm);
				}
				// Uri uri = Uri.fromFile(new File(imgData
				// .getString(ImageDBUtil.IMAGES_FILEPATH)));
				// Uri uri =
				// Uri.parse(imgData.getString(ImageDBUtil.IMAGES_ARCHIVE_URI));
				// set bitmap
				// view.setImageViewUri(R.id.hst_img,
				// Uri.parse(Constants.BLANK));
				// view.setImageViewUri(R.id.hst_img, uri);
				// set click intent
				// Intent configIntent = new Intent(context,
				// HSTFeedImageTouchOptions.class);
				// configIntent.putExtra("wid", appWidgetId);
				// configIntent.putExtra("size", size);
				// PendingIntent pending = PendingIntent.getActivity(context, 0,
				// configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				// view.setOnClickPendingIntent(R.id.widget_frame, pending);
			}
		}
		return view;
	}

	private class DownloadHSTFeedXMLTask extends
			AsyncTask<String, Void, Integer> {

		private AppWidgetManager manager;
		private boolean executing;
		private int appWidgetId = -1;

		@Override
		protected Integer doInBackground(String... params) {
			executing = true;
			appWidgetId = Integer.parseInt(params[0]);
			ImageDB db = ImageDB.getInstance(getBaseContext());
			String url = params[1] + "?";
			url += "pos=" + params[2] + Constants.COMMA + params[3] + "&size="
					+ params[4];
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
						// db.setImage(appWidgetId, i, BitmapFactory
						// .decodeByteArray(imgData, 0, imgData.length));
						db.setImage(appWidgetId, feedImages.get(i).name,
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
			return insertCnt;
		}

		@Override
		protected void onPostExecute(Integer insertCnt) {
			if (insertCnt != null && insertCnt > 0 && appWidgetId > -1) {
				ImageDB db = ImageDB.getInstance(getBaseContext());
				Bundle widget = db.getWidget(appWidgetId);
				int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
				RemoteViews view = new RemoteViews(getBaseContext()
						.getPackageName(), R.layout.widget_layout);
				Bitmap dbbm = db.getImageBitmap(appWidgetId, current);
				Bundle imgData = db.getImageMeta(appWidgetId, current);
				if (imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null) {
					Log.w(TAG,
							"something is awry - could not get the cached file path for the file just downloaded");
				}
				if (dbbm != null) {
					view.setImageViewBitmap(R.id.hst_img, dbbm);
					if (imgData.getString(ImageDBUtil.IMAGES_NAME) != null) {
						view.setTextViewText(R.id.name,
								imgData.getString(ImageDBUtil.IMAGES_NAME));
					}
					if (imgData.getString(ImageDBUtil.IMAGES_CREDITS) != null) {
						view.setTextViewText(R.id.credits,
								imgData.getString(ImageDBUtil.IMAGES_CREDITS));
					}
					// set click intent
					// Intent configIntent = new Intent(getBaseContext(),
					// HSTFeedImageTouchOptions.class);
					// configIntent.putExtra("wid", appWidgetId);
					// configIntent.putExtra("size", size);
					// configIntent.putExtra("url", "foobar");
					// PendingIntent pending = PendingIntent.getActivity(
					// getBaseContext(), 0, configIntent,
					// PendingIntent.FLAG_UPDATE_CURRENT);
					// update.setOnClickPendingIntent(R.id.widget_frame,
					// pending);
					// update widget
					manager.updateAppWidget(appWidgetId, view);
				}
			}
			executing = false;
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
			executing = false;
		}

		/**
		 * @return the executing
		 */
		public boolean isExecuting() {
			return executing;
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
			transformed.append("-small_web.jpg");
			return transformed.toString();
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

}