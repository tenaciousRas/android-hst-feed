package com.longevitysoft.android.appwidget.hstfeed.async;

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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.longevitysoft.android.appwidget.hstfeed.Constants;
import com.longevitysoft.android.appwidget.hstfeed.R;
import com.longevitysoft.android.appwidget.hstfeed.activity.HSTFeedFullsizeDisplay;
import com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDBUtil;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService.HSTFeedXML;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTImage;
import com.longevitysoft.android.appwidget.hstfeed.util.HSTFeedUtil;

/**
 * Parse XML feed and download feed images in background.
 * 
 * @author fbeachler
 * 
 */
public class HSTFeedXMLWorker extends AsyncTask<String, Void, HSTFeedXML> {

	public static final String TAG = "HSTFeedXMLWorker";

	/**
	 * OS widget mgr.
	 */
	private AppWidgetManager manager;

	/**
	 * Context for this instance.
	 */
	private Context ctx;

	/**
	 * Handler for msgs from XML worker.
	 */
	private HSTFeedXMLWorkerHandler workerHandler;

	/**
	 * ID of widget, assigned by android os.
	 */
	private int appWidgetId = -1;

	/**
	 * widget size
	 */
	private int widgetSize = HSTFeedService.SIZE_SMALL;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected HSTFeedXML doInBackground(String... params) {
		Log.d(TAG, "doInBackground started");
		if (null != workerHandler) {
			Message msg = workerHandler.obtainMessage();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_PARSE_STARTED;
			msg.sendToTarget();
		}
		// download feed
		appWidgetId = Integer.parseInt(params[0]);
		widgetSize = Integer.parseInt(params[1]);
		ImageDB db = ImageDB.getInstance(ctx);
		String url = params[2] + Constants.QUESTION
				+ HSTFeedService.QUERY_PARAM_NAME_POS + Constants.EQUALS
				+ params[3] + Constants.COMMA + params[4] + Constants.AMPERSAND
				+ HSTFeedService.QUERY_PARAM_NAME_SIZE + Constants.EQUALS
				+ params[5];
		Log.i(TAG, "download feed from url=" + url);
		HSTFeedXML parsedFeed = downloadFeedXML(url);
		List<HSTImage> feedImages = new ArrayList<HSTImage>();
		if (null != parsedFeed && null != parsedFeed.getImageList()) {
			feedImages = parsedFeed.getImageList();
		}
		if (null != workerHandler) {
			Message msg = workerHandler.obtainMessage();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_XML_LOADED;
			msg.arg1 = feedImages.size();
			msg.sendToTarget();
		}
		int insertCnt = 0;
		// download images
		if (0 < feedImages.size()) {
			for (HSTImage feedImage : feedImages) {
				byte[] imgData = downloadImage(
						transformArchiveUrlToImageUrl(feedImage.getArchiveUrl()))
						.toByteArray();
				if (imgData.length > 0) {
					// store immediately
					db.setImage(appWidgetId, feedImage.getName(),
							feedImage.getArchiveUrl(), feedImage.getFullUrl(),
							feedImage.getCredits(), feedImage.getCreditsUrl(),
							feedImage.getCaption(), feedImage.getCaptionUrl(),
							insertCnt, imgData);
					// notify handler
					if (null != workerHandler) {
						Message msg = workerHandler.obtainMessage();
						msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_IMAGE_LOADED;
						msg.obj = feedImage.getArchiveUrl();
						msg.sendToTarget();
					}
					insertCnt++;
				}
			}
		}
		Log.d(TAG, "doInBackground finished");
		return parsedFeed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
	 */
	@Override
	protected void onPostExecute(HSTFeedXML parsedFeed) {
		Log.d(TAG, "onPostExecute started");
		ImageDB db = ImageDB.getInstance(ctx);
		Bundle widget = db.getWidget(appWidgetId);
		if (null == widget) {
			Log.w(TAG,
					"something is awry, could not retrieve widget that was just updated");
			return;
		}
		// notify handler
		if (null != workerHandler) {
			Message msg = workerHandler.obtainMessage();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_IMAGES_ALL_LOADED;
			msg.obj = parsedFeed;
			msg.sendToTarget();
		}
		// build views
		RemoteViews view = null;
		if (parsedFeed != null && null != parsedFeed.getImageList()
				&& parsedFeed.getImageList().size() > 0 && appWidgetId > -1) {
			int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
			view = new RemoteViews(ctx.getPackageName(),
					R.layout.widget_layout_sm);
			// calc optimal sample widgetSize
			int sampleSize = HSTFeedUtil.calcBitmapScaleFactor(widgetSize,
					db.getImageBitmapBounds(appWidgetId, current));
			// load bitmap and image meta data
			Bitmap dbbm = db.getImageBitmap(appWidgetId, current, sampleSize);
			Bundle imgData = db.getImageMeta(appWidgetId, current);
			if (imgData.getString(ImageDBUtil.IMAGES_FILEPATH) == null) {
				Log.w(TAG,
						"something is awry - could not get the cached file path for the file just downloaded");
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
				view = HSTFeedUtil.buildWidgetClickIntent(view, ctx,
						appWidgetId, widgetSize, widget, imgData);
			}
		} else if (parsedFeed != null
				&& (null != parsedFeed.getImageList() && parsedFeed
						.getImageList().size() < 1)) {
			view = new RemoteViews(ctx.getPackageName(),
					R.layout.widget_layout_empty);
			// set text content based on widget widgetSize
			view.setTextViewText(R.id.content, String.format(
					ctx.getString(R.string.no_images_found),
					widget.getFloat("ra"), widget.getFloat("dec"),
					widget.getFloat("area")));
			view = HSTFeedUtil.buildWidgetClickIntent(view, ctx, appWidgetId,
					widgetSize, widget, null);
		} else {
			view = new RemoteViews(ctx.getPackageName(),
					R.layout.widget_layout_empty);
			view.setTextViewText(R.id.content,
					ctx.getString(R.string.download_images_err_unexpected));
			view = HSTFeedUtil.buildWidgetClickIntent(view, ctx, appWidgetId,
					widgetSize, widget, null);
		}
		if (null != view) {
			// update widget
			if (null == manager) {
				manager = AppWidgetManager.getInstance(ctx);
			}
			manager.updateAppWidget(appWidgetId, view);
		}
		if (null != workerHandler) {
			Message msg = workerHandler.obtainMessage();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_PARSE_COMPLETE;
			msg.sendToTarget();
		}
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
	}

	/**
	 * @param ctx
	 *            the ctx to set
	 */
	public void setCtx(Context ctx) {
		this.ctx = ctx;
	}

	/**
	 * @param manager
	 *            the manager to set
	 */
	public void setManager(AppWidgetManager manager) {
		this.manager = manager;
	}

	/**
	 * @param workerHandler
	 *            the mHandler to set
	 */
	public void setHSTFeedXMLWorkerHandler(HSTFeedXMLWorkerHandler workerHandler) {
		this.workerHandler = workerHandler;
	}

	/**
	 * Download an image. Limits the image file widgetSize to 2MB, pretty much
	 * any compressed format like JPG larger than this will cause an out of
	 * memory exception by the AOSP BitmapFactory#decode and #compress methods.
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
	 * Download a MAST feed XML file. For example: http://archive.stsci.edu/stpr
	 * /vo_search.php?pos=149.0,69.0&widgetSize=5.0
	 * 
	 * @param url
	 * @return
	 */
	private HSTFeedXML downloadFeedXML(String url) {
		HttpURLConnection con;
		HSTFeedXML ret = new HSTFeedXML();
		;
		try {
			// get XML feed
			con = (HttpURLConnection) (new URL(url)).openConnection();
			con.setDoInput(true);
			con.setDoOutput(false);
			System.setProperty("http.keepAlive", "false");
			con.setRequestMethod(HttpGet.METHOD_NAME);
			con.connect();
			ret = parseMASTFeed(con.getInputStream());
			con.disconnect();
			con.getInputStream().close();
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
		return ret;
	}

	/**
	 * Parse a MAST HST PR feed. Example:
	 * http://archive.stsci.edu/stpr/vo_search
	 * .php?pos=129.0,69.0&widgetSize=25.0
	 * 
	 * @param is
	 * @return
	 */
	public HSTFeedXML parseMASTFeed(InputStream is) {
		HSTFeedXML ret = new HSTFeedXML();
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
							ret.getImageList().add(hstImg);
						}
						tdCnt = 0;
						hasJPGImg = false;
					} else if (tagname.equalsIgnoreCase("td")) {
						if (tdCnt == 4) {
							hstImg.setFullUrl(text);
							if (text.endsWith("jpeg") || text.endsWith("jpg")
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
	 * Turn a HST archive web page URL with cutouts to a URL for large raw image
	 * cutout.
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
				.append(HSTFeedService.MAST_HST_URL);
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
