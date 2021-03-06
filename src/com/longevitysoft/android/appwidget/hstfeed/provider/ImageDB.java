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
package com.longevitysoft.android.appwidget.hstfeed.provider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

import com.longevitysoft.android.appwidget.hstfeed.service.HSTImage;
import com.longevitysoft.android.appwidget.hstfeed.util.HSTFeedUtil;

/**
 * @author fbeachler
 * 
 */
public class ImageDB extends SQLiteOpenHelper {

	public static final String TAG = "ImageDB";

	private static final String DB_NAME = "hstfeed_images.db";
	private static final int DB_VERSION = 1;

	private SQLiteDatabase db = null;
	private static ImageDB iDBInstance;
	private Context ctx = null;

	private ImageDB(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		db = getWritableDatabase();
		ctx = context;
	}

	public static ImageDB getInstance(Context context) {
		if (null == iDBInstance) {
			iDBInstance = new ImageDB(context);
		}
		return iDBInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		this.db = db;
		db.execSQL(ImageDBUtil.buildWidgetsTableSQL());
		db.execSQL(ImageDBUtil.buildImagesTableSQL());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != DB_VERSION) {
			db.execSQL("DROP TABLE IF EXISTS " + ImageDBUtil.TABLE_IMAGES);
			db.execSQL("DROP TABLE IF EXISTS " + ImageDBUtil.TABLE_WIDGETS);
			onCreate(db);
		}
	}

	/**
	 * Store the widget params in the DB.
	 * 
	 * @param appWidgetId
	 * @param type
	 * @param period
	 * @param ra
	 * @param dec
	 * @param area
	 * @param order
	 * @return
	 */
	public boolean createWidget(int appWidgetId, int type, int period,
			Float ra, Float dec, Float area, int order) {
		deleteWidget(appWidgetId);
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(ImageDBUtil.WIDGETS_ID, appWidgetId);
		values.put(ImageDBUtil.WIDGETS_TYPE, type);
		values.put(ImageDBUtil.WIDGETS_PERIOD, period);
		values.put(ImageDBUtil.WIDGETS_RA, ra);
		values.put(ImageDBUtil.WIDGETS_DEC, dec);
		values.put(ImageDBUtil.WIDGETS_AREA, area);
		values.put(ImageDBUtil.WIDGETS_ORDER, order);

		values.put(ImageDBUtil.WIDGETS_CURRENT, 1);
		values.put(ImageDBUtil.WIDGETS_LASTUPDATE, 0);
		values.put(ImageDBUtil.WIDGETS_UPDATES, 0);
		boolean ret = db.insert(ImageDBUtil.TABLE_WIDGETS, null, values) > -1;
		return ret;
	}

	/**
	 * Update period and order of widget.
	 * 
	 * @param appWidgetId
	 * @param period
	 * @param order
	 */
	public void updateWidget(int appWidgetId, int period, Float ra, Float dec,
			Float area, int order) {
		String whereClause = ImageDBUtil.WIDGETS_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		ContentValues values = new ContentValues();
		values.put(ImageDBUtil.WIDGETS_PERIOD, period);
		values.put(ImageDBUtil.WIDGETS_RA, ra);
		values.put(ImageDBUtil.WIDGETS_DEC, dec);
		values.put(ImageDBUtil.WIDGETS_AREA, area);
		values.put(ImageDBUtil.WIDGETS_ORDER, order);
		db.update(ImageDBUtil.TABLE_WIDGETS, values, whereClause, whereArgs);
	}

	/**
	 * Set the widget to return TRUE next time needsUpdate is called.
	 * 
	 * @param appWidgetId
	 */
	public void invalidateWidget(int appWidgetId) {
		String whereClause = ImageDBUtil.WIDGETS_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		ContentValues values = new ContentValues();
		values.put(ImageDBUtil.WIDGETS_LASTUPDATE, 0);
		db.update(ImageDBUtil.TABLE_WIDGETS, values, whereClause, whereArgs);
	}

	/**
	 * Delete widget from storage and all associated images.
	 * 
	 * @param appWidgetId
	 */
	public void deleteWidget(int appWidgetId) {
		String whereClause = null;
		String[] whereArgs = new String[0];
		whereArgs = new String[] { Integer.toString(appWidgetId) };
		whereClause = ImageDBUtil.WIDGETS_ID + " = ?";
		db.delete(ImageDBUtil.TABLE_WIDGETS, whereClause, whereArgs);
		whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		db.delete(ImageDBUtil.TABLE_IMAGES, whereClause, whereArgs);
	}

	/**
	 * @param appWidgetId
	 * @return True if widget is storing stale content.
	 */
	public boolean needsUpdate(int appWidgetId) {
		Bundle widget = getWidget(appWidgetId);

		Time now = new Time();
		now.setToNow();
		Time last = new Time();
		last.set(widget.getLong(ImageDBUtil.WIDGETS_LASTUPDATE));
		last.minute += widget.getInt(ImageDBUtil.WIDGETS_PERIOD);
		last.second -= 5;

		if (last.before(now)) {
			Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
					ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_WEIGHT },
					ImageDBUtil.IMAGES_WIDGETID + " = " + appWidgetId, null,
					null, null, ImageDBUtil.IMAGES_WEIGHT);
			int updates = widget.getInt(ImageDBUtil.WIDGETS_UPDATES);
			int current = widget.getInt(ImageDBUtil.WIDGETS_CURRENT);
			long lastupdate = widget.getLong(ImageDBUtil.WIDGETS_LASTUPDATE);
			if (null == c) {
				return true;
			}
			if (0 == c.getCount()) {
				c.close();
				return true;
			}
			int idx = 0;
			if (c.moveToFirst() && 1 < c.getCount()) {
				updates++;
				c.moveToLast();
				idx = c.getColumnIndex(ImageDBUtil.IMAGES_ID);
				if (c.getInt(idx) == current) {
					// current image is last image, loop to first
					if (c.moveToFirst()) {
						current = c.getInt(idx);
					}
				} else {
					// current image is not last image
					if (widget.getInt(ImageDBUtil.WIDGETS_ORDER) == 1) {
						// random ordering
						Random rand = new Random();
						int oldCurrent = current;
						c.moveToFirst();
						do {
							current = rand.nextInt(c.getCount());
							if (!c.move(current - 1)) {
								break;
							}
							current = c.getInt(idx);
						} while (current == oldCurrent);
					} else {
						// linear ordering
						c.moveToFirst();
						do {
							if (0 > idx || idx >= c.getColumnCount()) {
								continue;
							}
							if (c.getInt(idx) > current) {
								break;
							}
						} while (c.moveToNext());
						current = c.getInt(idx);
					}
				}
			}
			c.close();
			lastupdate = now.toMillis(false);
			ContentValues values = new ContentValues();
			values.put(ImageDBUtil.WIDGETS_CURRENT, current);
			values.put(ImageDBUtil.WIDGETS_LASTUPDATE, lastupdate);
			values.put(ImageDBUtil.WIDGETS_UPDATES, updates);
			db.update(ImageDBUtil.TABLE_WIDGETS, values, ImageDBUtil.WIDGETS_ID
					+ " = " + appWidgetId, null);
			return true;
		}
		return false;
	}

	/**
	 * Set the weight of the image currently displayed in widget.
	 * 
	 * @param appWidgetId
	 * @param current
	 */
	public void setWidgetCurrent(int appWidgetId, int current) {
		ContentValues values = new ContentValues();
		values.put(ImageDBUtil.WIDGETS_CURRENT, current);
		db.update(ImageDBUtil.TABLE_WIDGETS, values, ImageDBUtil.WIDGETS_ID
				+ " = " + appWidgetId, null);
	}

	/**
	 * @param appWidgetId
	 * @param name
	 * @param archvUri
	 * @param fullUri
	 * @param credits
	 * @param creditsUri
	 * @param caption
	 * @param captionUri
	 * @param weight
	 * @param bitmap
	 * @return
	 */
	public long setImage(int appWidgetId, String name, String archvUri,
			String fullUri, String credits, String creditsUri, String caption,
			String captionUri, int weight, Bitmap bitmap) {
		Cursor c;
		if (weight < 0) {
			c = db.query(ImageDBUtil.TABLE_IMAGES,
					new String[] { ImageDBUtil.IMAGES_WEIGHT },
					ImageDBUtil.IMAGES_WIDGETID + " = " + appWidgetId, null,
					null, null, ImageDBUtil.IMAGES_WEIGHT);
			if (null == c || c.getCount() < 1) {
				weight = 0;
			} else {
				c.moveToLast();
				weight = c.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_WEIGHT)) + 1;
			}
			if (c != null) {
				c.close();
			}
		}
		ContentValues values = new ContentValues();
		values.put(ImageDBUtil.IMAGES_WIDGETID, appWidgetId);
		values.put(ImageDBUtil.IMAGES_WEIGHT, weight);
		values.put(ImageDBUtil.IMAGES_NAME, name);
		values.put(ImageDBUtil.IMAGES_FULL_URI, fullUri);
		values.put(ImageDBUtil.IMAGES_ARCHIVE_URI, archvUri);
		values.put(ImageDBUtil.IMAGES_CREDITS, credits);
		values.put(ImageDBUtil.IMAGES_CREDITS_URI, creditsUri);
		values.put(ImageDBUtil.IMAGES_CAPTION, caption);
		values.put(ImageDBUtil.IMAGES_CAPTION_URI, captionUri);
		long rowId = db.insert(ImageDBUtil.TABLE_IMAGES, null, values);
		FileOutputStream fos;
		String outFP = HSTFeedUtil.buildImgFilePath(appWidgetId, rowId, ctx);
		File f = new File(outFP);
		try {
			fos = new FileOutputStream(f);
			if (null != bitmap) {
				bitmap.compress(CompressFormat.PNG, 100, fos);
				bitmap.recycle();
			}
			fos.flush();
			fos.close();
			values.clear();
			values.put(ImageDBUtil.IMAGES_FILEPATH, outFP);
			String whereClause = ImageDBUtil.IMAGES_ID + " = ?";
			String[] whereArgs = new String[] { Long.toString(rowId) };
			int numRows = db.update(ImageDBUtil.TABLE_IMAGES, values,
					whereClause, whereArgs);
			if (numRows < 1) {
				throw new IllegalStateException(
						"failed to update image DB with cached file path");
			}
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
		return rowId;
	}

	/**
	 * @param appWidgetId
	 * @param name
	 * @param archvUri
	 * @param fullUri
	 * @param credits
	 * @param creditsUri
	 * @param caption
	 * @param captionUri
	 * @param weight
	 * @param bitmap
	 * @return
	 */
	public long setImage(int appWidgetId, String name, String archvUri,
			String fullUri, String credits, String creditsUri, String caption,
			String captionUri, int weight, byte[] bitmap) {
		if (bitmap == null) {
			return -1;
		}
		Options opts = new Options();
		opts.inDither = true;
		opts.inJustDecodeBounds = false;
		opts.inInputShareable = true;
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bmp = BitmapFactory.decodeByteArray(bitmap, 0, bitmap.length,
				opts);
		return setImage(appWidgetId, name, archvUri, fullUri, credits,
				creditsUri, caption, captionUri, weight, bmp);
	}

	/**
	 * Get dimension of cached image. Call this before calling {@link
	 * this#getImageBitmap(int, int, int)} to calculate proper sample
	 * widgetSize.
	 * 
	 * @param appWidgetId
	 * @param imgId
	 * @return vector with elements representing bounds. Element 0 = width.
	 *         Element 1 = height. Or empty vector if bounds could not be
	 *         retrieved.
	 */
	public Vector<Integer> getImageBitmapBounds(int appWidgetId, int imgId) {
		Bitmap bitmap = null;
		Vector<Integer> bounds = new Vector<Integer>(2);
		String whereClause = ImageDBUtil.IMAGES_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(imgId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
				ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_FILEPATH,
				ImageDBUtil.IMAGES_WEIGHT }, whereClause, whereArgs, null,
				null, ImageDBUtil.IMAGES_WEIGHT);
		if (null == c) {
			return bounds;
		}
		if (c.moveToFirst()) {
			String bitmapFilepath = c.getString(c
					.getColumnIndex(ImageDBUtil.IMAGES_FILEPATH));
			Options opts = new Options();
			opts.inDither = true;
			opts.inJustDecodeBounds = true;
			opts.inPurgeable = true;
			opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
			bitmap = BitmapFactory.decodeFile(bitmapFilepath);
			if (null == bitmap) {
				return bounds;
			}
			bounds.add(bitmap.getWidth());
			bounds.add(bitmap.getHeight());
		}
		c.close();
		return bounds;
	}

	/**
	 * Get chached image as a {@link Bitmap}.
	 * 
	 * @param appWidgetId
	 * @param imgId
	 * @return
	 */
	public Bitmap getImageBitmap(int appWidgetId, int imgId, int bmpSampleSize) {
		Bitmap bitmap = null;
		String whereClause = ImageDBUtil.IMAGES_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(imgId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
				ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_FILEPATH,
				ImageDBUtil.IMAGES_WEIGHT }, whereClause, whereArgs, null,
				null, ImageDBUtil.IMAGES_WEIGHT);
		if (null == c) {
			return bitmap;
		}
		Options opts = new Options();
		opts.inDither = true;
		opts.inJustDecodeBounds = false;
		opts.inPurgeable = true;
		opts.inSampleSize = bmpSampleSize;
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		if (c.moveToFirst()) {
			String bitmapFilepath = c.getString(c
					.getColumnIndex(ImageDBUtil.IMAGES_FILEPATH));
			bitmap = BitmapFactory.decodeFile(bitmapFilepath);
		}
		c.close();
		return bitmap;
	}

	/**
	 * @param appWidgetId
	 * @param imgId
	 * @return
	 */
	public ByteArrayOutputStream getImageAsBAOS(int appWidgetId, int imgId) {
		ByteArrayOutputStream baos = null;
		String whereClause = ImageDBUtil.IMAGES_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(imgId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
				ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_FILEPATH,
				ImageDBUtil.IMAGES_WEIGHT }, whereClause, whereArgs, null,
				null, ImageDBUtil.IMAGES_WEIGHT);
		if (null == c) {
			return baos;
		}
		if (c.moveToFirst()) {
			String bitmapFilepath = c.getString(c
					.getColumnIndex(ImageDBUtil.IMAGES_FILEPATH));
			c.close();
			if (null != bitmapFilepath) {
				baos = new ByteArrayOutputStream();
				try {
					FileInputStream fis = new FileInputStream(bitmapFilepath);
					byte[] buff = new byte[1024];
					int off = 0;
					while (true) {
						int read = fis.read(buff, off, buff.length);
						if (read < 1) {
							break;
						}
						off += read;
						baos.write(buff, 0, read);
					}
					fis.close();
				} catch (Exception e) {
					Log.e(TAG, Log.getStackTraceString(e));
				}
			}
		}
		return baos;
	}

	/**
	 * @param appWidgetId
	 * @param imgId
	 * @return
	 */
	public Bundle getImageMeta(int appWidgetId, int imgId) {
		String whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ? AND "
				+ ImageDBUtil.IMAGES_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId),
				Integer.toString(imgId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
				ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_WIDGETID,
				ImageDBUtil.IMAGES_WEIGHT, ImageDBUtil.IMAGES_NAME,
				ImageDBUtil.IMAGES_ARCHIVE_URI, ImageDBUtil.IMAGES_FULL_URI,
				ImageDBUtil.IMAGES_CAPTION, ImageDBUtil.IMAGES_CREDITS,
				ImageDBUtil.IMAGES_CREDITS_URI, ImageDBUtil.IMAGES_FILEPATH },
				whereClause, whereArgs, null, null, ImageDBUtil.IMAGES_WEIGHT);
		Bundle bundle = new Bundle();
		if (null == c) {
			return bundle;
		}
		if (c.moveToFirst()) {
			bundle.putInt(ImageDBUtil.IMAGES_ID, Integer.valueOf(c.getInt(c
					.getColumnIndex(ImageDBUtil.IMAGES_ID))));
			bundle.putInt(ImageDBUtil.IMAGES_WIDGETID, Integer.valueOf(c
					.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_WIDGETID))));
			bundle.putInt(ImageDBUtil.IMAGES_WEIGHT, Integer.valueOf(c.getInt(c
					.getColumnIndex(ImageDBUtil.IMAGES_WEIGHT))));
			bundle.putString(ImageDBUtil.IMAGES_NAME,
					c.getString(c.getColumnIndex(ImageDBUtil.IMAGES_NAME)));
			bundle.putString(ImageDBUtil.IMAGES_CAPTION,
					c.getString(c.getColumnIndex(ImageDBUtil.IMAGES_CAPTION)));
			bundle.putString(ImageDBUtil.IMAGES_ARCHIVE_URI, c.getString(c
					.getColumnIndex(ImageDBUtil.IMAGES_ARCHIVE_URI)));
			bundle.putString(ImageDBUtil.IMAGES_FULL_URI,
					c.getString(c.getColumnIndex(ImageDBUtil.IMAGES_FULL_URI)));
			bundle.putString(ImageDBUtil.IMAGES_CREDITS,
					c.getString(c.getColumnIndex(ImageDBUtil.IMAGES_CREDITS)));
			bundle.putString(ImageDBUtil.IMAGES_CREDITS_URI, c.getString(c
					.getColumnIndex(ImageDBUtil.IMAGES_CREDITS_URI)));
			bundle.putString(ImageDBUtil.IMAGES_FILEPATH,
					c.getString(c.getColumnIndex(ImageDBUtil.IMAGES_FILEPATH)));
		}
		c.close();
		return bundle;
	}

	/**
	 * Delete all images from storage.
	 * 
	 * @param appWidgetId
	 */
	public void deleteAllImages(int appWidgetId) {
		String whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES,
				new String[] { ImageDBUtil.IMAGES_ID }, whereClause, whereArgs,
				null, null, ImageDBUtil.IMAGES_WEIGHT);
		if (null == c || 0 == c.getCount()) {
			return;
		}
		c.moveToFirst();
		do {
			int id = c.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_ID));
			// delete file
			try {
				File f = new File(HSTFeedUtil.buildImgFilePath(appWidgetId, id,
						ctx));
				f.delete();
			} catch (Exception e) {
				Log.w(TAG,
						"exception while attempting to delete image from cache, exception was"
								+ Log.getStackTraceString(e));
			}
		} while (c.moveToNext());
		db.delete(ImageDBUtil.TABLE_IMAGES, whereClause, whereArgs);
	}

	/**
	 * @param appWidgetId
	 * @param position
	 */
	public void deleteImage(int appWidgetId, int position) {
		String whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES,
				new String[] { ImageDBUtil.IMAGES_ID }, whereClause, whereArgs,
				null, null, ImageDBUtil.IMAGES_WEIGHT);
		if (null == c) {
			return;
		}
		if (c.moveToFirst()) {
			int i = 0;
			while (i < position) {
				if (!c.moveToNext())
					break;
				i++;
			}
			// delete from DB
			int id = c.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_ID));
			db.delete(ImageDBUtil.TABLE_IMAGES, ImageDBUtil.IMAGES_ID + " = "
					+ id, null);
			// delete file
			try {
				File f = new File(HSTFeedUtil.buildImgFilePath(appWidgetId, id,
						ctx));
				f.delete();
			} catch (Exception e) {
				Log.w(TAG,
						"exception while attempting to delete image from cache, exception was"
								+ Log.getStackTraceString(e));
			}
		}
		c.close();
	}

	/**
	 * @param appWidgetId
	 * @param position
	 * @param direction
	 */
	public void moveImage(int appWidgetId, int position, int direction) {
		if (position == 0 && direction < 0) {
			return;
		}
		String whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
				ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_WEIGHT },
				whereClause, whereArgs, null, null, ImageDBUtil.IMAGES_WEIGHT);
		if (null == c) {
			return;
		}
		if (position >= (c.getCount() - 1) && direction > 0) {
			c.close();
			return;
		}
		if (c.moveToFirst()) {
			int i = 0;
			while (i < position) {
				if (!c.moveToNext())
					break;
				i++;
			}
			int id = c.getInt(0);
			int weight = c.getInt(1);
			int newID = -1, newWeight = -1;
			if (direction < 0) {
				if (c.moveToPrevious()) {
					newID = c.getInt(0);
					newWeight = c.getInt(1);
				}
			} else if (direction > 0) {
				if (c.moveToNext()) {
					newID = c.getInt(0);
					newWeight = c.getInt(1);
				}
			} else {
				c.close();
				return;
			}

			ContentValues values = new ContentValues();
			values.put(ImageDBUtil.IMAGES_WEIGHT, newWeight);
			whereClause = ImageDBUtil.IMAGES_ID + " = ?";
			whereArgs = new String[] { Integer.toString(id) };
			db.update(ImageDBUtil.TABLE_IMAGES, values, whereClause, whereArgs);

			values = new ContentValues();
			values.put(ImageDBUtil.IMAGES_WEIGHT, weight);
			whereArgs = new String[] { Integer.toString(newID) };
			db.update(ImageDBUtil.TABLE_IMAGES, values, whereClause, whereArgs);
		}
		c.close();
	}

	/**
	 * @param appWidgetId
	 * @return
	 */
	public Bundle getWidget(int appWidgetId) {
		String whereClause = ImageDBUtil.WIDGETS_ID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		Cursor c = db.query(ImageDBUtil.TABLE_WIDGETS, new String[] {
				ImageDBUtil.WIDGETS_ID, ImageDBUtil.WIDGETS_CURRENT,
				ImageDBUtil.WIDGETS_LASTUPDATE, ImageDBUtil.WIDGETS_ORDER,
				ImageDBUtil.WIDGETS_PERIOD, ImageDBUtil.WIDGETS_RA,
				ImageDBUtil.WIDGETS_DEC, ImageDBUtil.WIDGETS_AREA,
				ImageDBUtil.WIDGETS_TYPE, ImageDBUtil.WIDGETS_IMG_LIST_COUNT,
				ImageDBUtil.WIDGETS_UPDATES }, whereClause, whereArgs, null,
				null, null);
		Bundle bundle = null;
		if (null == c) {
			return bundle;
		}
		if (c.moveToFirst()) {
			bundle = new Bundle();
			bundle.putInt(ImageDBUtil.WIDGETS_ID, Integer.valueOf(c.getInt(c
					.getColumnIndex(ImageDBUtil.WIDGETS_ID))));
			bundle.putInt(ImageDBUtil.WIDGETS_CURRENT, Integer.valueOf(c
					.getInt(c.getColumnIndex(ImageDBUtil.WIDGETS_CURRENT))));
			bundle.putLong(ImageDBUtil.WIDGETS_LASTUPDATE, Long.valueOf(c
					.getLong(c.getColumnIndex(ImageDBUtil.WIDGETS_LASTUPDATE))));
			bundle.putInt(ImageDBUtil.WIDGETS_ORDER, Integer.valueOf(c.getInt(c
					.getColumnIndex(ImageDBUtil.WIDGETS_ORDER))));
			bundle.putInt(ImageDBUtil.WIDGETS_PERIOD, Integer.valueOf(c
					.getInt(c.getColumnIndex(ImageDBUtil.WIDGETS_PERIOD))));
			bundle.putFloat(ImageDBUtil.WIDGETS_RA, Float.parseFloat(c
					.getString(c.getColumnIndex(ImageDBUtil.WIDGETS_RA))));
			bundle.putFloat(ImageDBUtil.WIDGETS_DEC, Float.parseFloat(c
					.getString(c.getColumnIndex(ImageDBUtil.WIDGETS_DEC))));
			bundle.putFloat(ImageDBUtil.WIDGETS_AREA, Float.parseFloat(c
					.getString(c.getColumnIndex(ImageDBUtil.WIDGETS_AREA))));
			bundle.putInt(ImageDBUtil.WIDGETS_IMG_LIST_COUNT, c.getInt(c
					.getColumnIndex(ImageDBUtil.WIDGETS_IMG_LIST_COUNT)));
			bundle.putInt(ImageDBUtil.WIDGETS_TYPE, Integer.valueOf(c.getInt(c
					.getColumnIndex(ImageDBUtil.WIDGETS_TYPE))));
			bundle.putInt(ImageDBUtil.WIDGETS_UPDATES, Integer.valueOf(c
					.getInt(c.getColumnIndex(ImageDBUtil.WIDGETS_UPDATES))));
		}
		c.close();
		return bundle;
	}

	/**
	 * @param appWidgetId
	 * @return
	 */
	public Integer getWidgetPreviousImage(int appWidgetId, int current) {
		String whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES, new String[] {
				ImageDBUtil.IMAGES_ID, ImageDBUtil.IMAGES_WEIGHT },
				whereClause, whereArgs, null, null, ImageDBUtil.IMAGES_WEIGHT);
		Integer ret = null;
		Integer prevId = null;
		if (c.moveToFirst()) {
			do {
				if (c.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_ID)) == current) {
					if (null == prevId) {
						// prev image at end of cursor
						c.moveToLast();
						ret = c.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_ID));
					} else {
						ret = prevId;
					}
					break; // got id, exit loop
				} else {
					prevId = c.getInt(c.getColumnIndex(ImageDBUtil.IMAGES_ID));
				}
			} while (c.moveToNext());
		}
		if (null != c) {
			c.close();
		}
		return ret;
	}

	/**
	 * @param appWidgetId
	 * @return
	 */
	public Bundle getWidgetImages(int appWidgetId, int bmpSampleSize) {
		String whereClause = ImageDBUtil.IMAGES_WIDGETID + " = ?";
		String[] whereArgs = new String[] { Integer.toString(appWidgetId) };
		Cursor c = db.query(ImageDBUtil.TABLE_IMAGES,
				new String[] { ImageDBUtil.IMAGES_FILEPATH }, whereClause,
				whereArgs, null, null, ImageDBUtil.IMAGES_WEIGHT);

		Bundle ret = new Bundle();
		Bitmap[] bmps = new Bitmap[0];
		if (c.moveToFirst()) {
			Options opts = new Options();
			opts.inDither = true;
			opts.inJustDecodeBounds = false;
			opts.inPurgeable = true;
			opts.inSampleSize = bmpSampleSize;
			opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
			bmps = new Bitmap[c.getCount()];
			for (int i = 0; i < c.getCount(); i++) {
				String bitmapFilepath = c.getString(c
						.getColumnIndex(ImageDBUtil.IMAGES_FILEPATH));
				try {
					bmps[i] = BitmapFactory.decodeFile(bitmapFilepath, opts);
				} catch (Exception e) {
					Log.d(TAG,
							"failed to load HST images from storage, exception was:\n"
									+ Log.getStackTraceString(e));
				}
				c.moveToNext();
			}
		}
		if (null != c) {
			c.close();
		}
		ret.putParcelableArray("images", bmps);
		return ret;
	}

}