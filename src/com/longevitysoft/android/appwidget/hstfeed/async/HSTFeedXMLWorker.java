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

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;

import com.longevitysoft.android.appwidget.hstfeed.Constants;
import com.longevitysoft.android.appwidget.hstfeed.handler.HSTFeedXMLWorkerHandler;
import com.longevitysoft.android.appwidget.hstfeed.provider.ImageDB;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTFeedService.HSTFeedXML;
import com.longevitysoft.android.appwidget.hstfeed.service.HSTImage;

/**
 * Parse XML feed and download feed images in background.
 * 
 * @author fbeachler
 * 
 */
public class HSTFeedXMLWorker extends AsyncTask<String, Void, HSTFeedXML> {

	public static final String TAG = "HSTFeedXMLWorker";

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

	protected Message buildBaseHandlerMsg() {
		Message msg = workerHandler.obtainMessage();
		msg.arg1 = appWidgetId;
		msg.arg2 = widgetSize;
		return msg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	protected HSTFeedXML doInBackground(String... params) {
		Log.d(TAG, "doInBackground started");
		// download feed
		appWidgetId = Integer.parseInt(params[0]);
		widgetSize = Integer.parseInt(params[1]);
		if (null != workerHandler) {
			Message msg = buildBaseHandlerMsg();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_PARSE_STARTED;
			msg.sendToTarget();
		}
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
			Message msg = buildBaseHandlerMsg();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_XML_LOADED;
			msg.obj = feedImages.size();
			msg.sendToTarget();
		}
		int insertCnt = 0;
		// download images
		if (0 < feedImages.size()) {
			for (HSTImage feedImage : feedImages) {
				byte[] imgData = downloadImage(
						transformArchiveUrlToImageUrl(
								feedImage.getArchiveUrl(), ctx)).toByteArray();
				if (imgData.length > 0) {
					// store immediately
					db.setImage(appWidgetId, feedImage.getName(),
							feedImage.getArchiveUrl(), feedImage.getFullUrl(),
							feedImage.getCredits(), feedImage.getCreditsUrl(),
							feedImage.getCaption(), feedImage.getCaptionUrl(),
							insertCnt, imgData);
					// notify handler
					if (null != workerHandler) {
						Message msg = buildBaseHandlerMsg();
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
		// notify handler
		if (null != workerHandler) {
			Message msg = buildBaseHandlerMsg();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_IMAGES_ALL_LOADED;
			msg.obj = parsedFeed;
			msg.sendToTarget();
			msg = buildBaseHandlerMsg();
			msg.what = HSTFeedXMLWorkerHandler.MSG_WHAT_FEED_PARSE_COMPLETE;
			msg.sendToTarget();
		}
		ctx = null;
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
		ctx = null;
	}

	/**
	 * @param ctx
	 *            the ctx to set
	 */
	public void setCtx(Context ctx) {
		this.ctx = ctx;
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
			con.addRequestProperty("Accept", "Accept:*/*;q=0.8");
			con.addRequestProperty(
					"User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.101 Safari/537.36");
			con.setDoInput(true);
			con.setDoOutput(true);
			if (con.getContentLength() > 10 * 1024 * 1024) {
				// TODO place oversized error image
				return baos;
			}
			InputStream is = con.getInputStream();
			byte[] buf = new byte[1024];
			int rdCnt = 1;
			while (true) {
				// while (is.read(b) != -1) {
				rdCnt = is.read(buf, 0, 1024);
				if (0 > rdCnt) {
					break;
				}
				baos.write(buf, 0, rdCnt);
			}
			con.disconnect();
			baos.flush();
			baos.close();
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
	private String transformArchiveUrlToImageUrl(final String url, Context ctx) {
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
		int formFactor = 0; // small images
		DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
		if (null != metrics) {
			if (640 < metrics.widthPixels) {
				formFactor = 2;
			}
			if (480 < metrics.widthPixels) {
				formFactor = 1;
			}
		}
		switch (formFactor) {
		case 2:
			transformed.append("-large_web.jpg");
			break;
		case 1:
			transformed.append("-web.jpg");
			break;
		case 0:
		default:
			transformed.append("-small_web.jpg");
			break;
		}
		return transformed.toString();
	}
}