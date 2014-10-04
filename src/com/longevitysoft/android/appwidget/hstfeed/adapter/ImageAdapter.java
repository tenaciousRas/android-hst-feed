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
package com.longevitysoft.android.appwidget.hstfeed.adapter;

import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {

	private Vector<Bitmap> views;
	private Context context;
	private int[] padding;

	public ImageAdapter(Context context) {
		super();
		initClass(context);
	}

	public ImageAdapter(Context context, Bitmap[] contents) {
		super();
		initClass(context);
		for (Bitmap bm : contents) {
			views.add(bm);
		}
	}

	protected void initClass(Context context) {
		views = new Vector<Bitmap>();
		this.context = context;
		padding = new int[4];
		// padding[0] = padding[2] = context.getResources().getInteger(
		// com.longevitysoft.android.appwidget.hstfeed.R.dimen.widget_vertical_margin);
		// padding[1] = padding[3] = context.getResources().getInteger(
		// com.longevitysoft.android.appwidget.hstfeed.R.dimen.widget_horizontal_margin);
		padding[0] = padding[2] = 2;
		padding[1] = padding[3] = 4;
	}

	public void add(Bitmap image) {
		views.add(image);
	}

	public void clear() {
		views.clear();
	}

	public void deletePosition(int position) {
		views.remove(position);
	}

	public int getCount() {
		return views.size();
	}

	public Object getItem(int position) {
		return views.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView view = null;
		if (convertView == null) {
			view = new ImageView(context);
			view.setLayoutParams(new GridView.LayoutParams(85, 85));
			view.setScaleType(ImageView.ScaleType.FIT_CENTER);
			view.setPadding(padding[0], padding[1], padding[2], padding[3]);
		} else {
			view = (ImageView) convertView;
		}
		view.setImageBitmap(views.get(position));
		return view;
	}

	public Bitmap[] toArray() {
		Bitmap[] array = new Bitmap[views.size()];
		for (int i = 0; i < views.size(); i++) {
			array[i] = views.get(i);
		}
		return array;
	}
}