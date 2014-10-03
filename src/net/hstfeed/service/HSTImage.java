package net.hstfeed.service;

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