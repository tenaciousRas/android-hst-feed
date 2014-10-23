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

/**
 * @author fbeachler
 * 
 */
public class ImageDBUtil {

	public static final String TABLE_WIDGETS = "widgets";
	public static final String WIDGETS_ID = "id";
	public static final String WIDGETS_SIZE = "widget_size";
	public static final String WIDGETS_TYPE = "type";
	public static final String WIDGETS_PERIOD = "period";
	public static final String WIDGETS_RA = "ra";
	public static final String WIDGETS_DEC = "dec";
	public static final String WIDGETS_AREA = "area";
	public static final String WIDGETS_ORDER = "random";
	public static final String WIDGETS_IMG_LIST_COUNT = "img_cnt";
	public static final String WIDGETS_CURRENT = "img_curr";
	public static final String WIDGETS_LASTUPDATE = "last_updated";
	public static final String WIDGETS_UPDATES = "updates";

	public static final String TABLE_IMAGES = "images";
	public static final String IMAGES_ID = "id";
	public static final String IMAGES_WIDGETID = "wid";
	public static final String IMAGES_NAME = "name";
	public static final String IMAGES_ARCHIVE_URI = "archive_uri";
	public static final String IMAGES_FULL_URI = "full_uri";
	public static final String IMAGES_FILEPATH = "filepath";
	public static final String IMAGES_CAPTION = "caption";
	public static final String IMAGES_CAPTION_URI = "caption_uri";
	public static final String IMAGES_CREDITS = "credits";
	public static final String IMAGES_CREDITS_URI = "credits_uri";
	public static final String IMAGES_WEIGHT = "weight";

	public static final String buildWidgetsTableSQL() {
		String ret = "CREATE TABLE " + TABLE_WIDGETS + "(" + WIDGETS_ID
				+ " INTEGER PRIMARY KEY, " + WIDGETS_SIZE + " INTEGER, "
				+ WIDGETS_TYPE + " INTEGER, " + WIDGETS_PERIOD + " INTEGER, "
				+ WIDGETS_RA + " TEXT, " + WIDGETS_DEC + " TEXT, "
				+ WIDGETS_AREA + " TEXT, " + WIDGETS_ORDER + " INTEGER, "
				+ WIDGETS_CURRENT + " INTEGER, " + WIDGETS_IMG_LIST_COUNT
				+ " INTEGER, " + WIDGETS_LASTUPDATE + " INTEGER, "
				+ WIDGETS_UPDATES + " INTEGER);";
		return ret;
	}

	public static final String buildImagesTableSQL() {
		String ret = "CREATE TABLE " + TABLE_IMAGES + "(" + IMAGES_ID
				+ " INTEGER PRIMARY KEY, " + IMAGES_WIDGETID + " INTEGER, "
				+ IMAGES_WEIGHT + " INTEGER, " + IMAGES_NAME + " TEXT, "
				+ IMAGES_ARCHIVE_URI + " TEXT, " + IMAGES_FULL_URI + " TEXT, "
				+ IMAGES_CREDITS + " TEXT, " + IMAGES_CREDITS_URI + " TEXT, "
				+ IMAGES_CAPTION + " TEXT, " + IMAGES_CAPTION_URI + " TEXT, "
				+ IMAGES_FILEPATH + " TEXT " + ");";
		return ret;
	}
}
