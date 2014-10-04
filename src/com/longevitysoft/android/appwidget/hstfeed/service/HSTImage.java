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
package com.longevitysoft.android.appwidget.hstfeed.service;

import android.graphics.Bitmap;

public class HSTImage {
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