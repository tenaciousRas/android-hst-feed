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
package com.longevitysoft.android.appwidget.hstfeed;

/**
 * @author fbeachler
 *
 */
public final class Constants {

	public static final String APP_PKG_NAME = "com.longevitysoft.android.appwidget.hstfeed";

	public static final String ENCODING_UTF8 = "UTF-8";

	public static final int STATE_NONE = -1;

	public static final String BLANK = "";
	public static final String NEWLINE = "\n";
	public static final String SLASH = "/";
	public static final String SPACE = " ";
	public static final String PIPE = "|";
	public static final String REGEX_PIPE = "\\|";
	public static final String COMMA = ",";
	public static final String SEMICOLON = ";";
	public static final String COLON = ":";
	public static final String VALUE_UNKNOWN = "?";
	public static final String AMPERSAND = "&";
	public static final String SQLITE_CONCAT = " || ";
	public static final String QUOTE = "\"";
	public static final String APOS = "'";
	public static final String EQUALS = "=";
	public static final String NOT_EQUALS = "!=";
	public static final String NULL = "NULL";
	public static final String DOT = ".";
	public static final String UNDERSCORE = "_";
	public static final String SLASHPOUND = "/#";
	public static final String OPEN_PAREN = "(";
	public static final String CLOSED_PAREN = ")";
	public static final String EQUALS_QUESTION = " = ?";
	public static final String IN = " IN ";
	public static final String EXT_PNG = ".png";
	public static final String EXT_JPG = ".jpg";
	public static final String EXT_GIF = ".gif";
	public static final String EXT_TIF = ".tif";
	public static final String EXT_TIFF = ".tiff";

	public static final String HEADER_NAME_KEEP_ALIVE = "Keep-Alive";
	public static final String HEADER_NAME_ACCEPT = "Accept";
	public static final String HEADER_NAME_CONTENT_TYPE = "Content-type";
	public static final String HEADER_VALUE_CONTENT_TYPE_TEXT_XML = "text/xml";
	public static final String HEADER_VALUE_CONTENT_TYPE_TEXT_JSON = "application/json";
	public static final String HEADER_NAME_CONTENT_ENCODING = "Content-Encoding";
	public static final String HEADER_NAME_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String HEADER_VALUE_ACCEPT_ENCODING_DEFLATE = "deflate";
	public static final String HEADER_VALUE_ACCEPT_ENCODING_GZIP = "gzip";
	public static final String HEADER_VALUE_ACCEPT_APPLICATION_XML = "application/xml";

	public static final String ANALYTICS_ID = "";
	public static final String TRACKER_CATEGORY_CLICKS = "clicks";
	public static final String TRACKER_ACTION_SEARCH_CANCEL = "search_cancel";
	public static final String TRACKER_ACTION_SEARCH_GO = "search_go";
	public static final String TRACKER_ACTION_SLIDE_LEFT = "slide_left";
	public static final String TRACKER_ACTION_SLIDE_RIGHT = "slide_right";
	public static final String TRACKER_VAR_NAME_ORIENTATION = "orientation";

}