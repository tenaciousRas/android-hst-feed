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
 * credit to the original author Free Beachler and a link to HSTFeed
 * (http://www.hstfeed.net).  You may not use this work for commercial
 * purposes.  You also agree to use this work in a manner that does not conflict
 * with the official HSTFeed Android Application.  If you alter, transform, or build 
 * upon this work, you may distribute the resulting work only under the same 
 * or similar license to this one.
 *
 * You should have received a copy of the Creative Commons Non-Commercial
 * License along with HSTFeed.  If not, see 
 * <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package com.longevitysoft.android.appwidget.hstfeed.async;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

/**
 * Decode bitmap images in background.
 * 
 * @author fbeachler
 * @see http
 *      ://developer.android.com/training/displaying-bitmaps/process-bitmap.html
 */
public class BitmapCacheWorker extends AsyncTask<Integer, Void, Bitmap> {
	private final WeakReference<ImageView> imageViewReference;
	private int data = 0;

	public BitmapCacheWorker(ImageView imageView) {
		// weakreference to imageview for GC
		imageViewReference = new WeakReference<ImageView>(imageView);
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(Params[])
	 */
	@Override
	    protected Bitmap doInBackground(Integer... params) {
	        data = params[0];
	        return null;
//	        return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
	    }

	// Once complete, see if ImageView is still around and set bitmap.
	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (imageViewReference != null && bitmap != null) {
			final ImageView imageView = imageViewReference.get();
			if (imageView != null) {
				imageView.setImageBitmap(bitmap);
			}
		}
	}
}