package org.fox.ttrss.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fox.ttrss.GlobalState;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class HeadlinesImageGetter implements ImageGetter {
	private final String TAG = this.getClass().getSimpleName();
	
	private Context c;
    private TextView container;

    /***
     * Construct the URLImageParser which will execute AsyncTask and refresh the container
     * @param t
     * @param c
     */
    public HeadlinesImageGetter(TextView t, Context c) {
        this.c = c;
        this.container = t;
    }

    public Drawable getDrawable(String source) {
        Drawable test = GlobalState.getInstance().m_drawableCache.get(source);
        
        if (test != null) {
        	Log.d(TAG, "cache hit for " + source);
        	
        	return test;
        } else {
        	Log.d(TAG, "cache miss for " + source);
        	
            URLDrawable urlDrawable = new URLDrawable();
    		
		    GlobalState.getInstance().m_drawableCache.put(source, urlDrawable);

            // get the actual source
		    ImageGetterAsyncTask asyncTask = 
		        new ImageGetterAsyncTask( urlDrawable);
		
		    asyncTask.execute(source);
		    
		    // return reference to URLDrawable where I will change with actual image from
		    // the src tag
		    return urlDrawable;
        }
    }

    public class ImageGetterAsyncTask extends AsyncTask<String, Void, Drawable>  {
        URLDrawable urlDrawable;

        public ImageGetterAsyncTask(URLDrawable d) {
            this.urlDrawable = d;
        }

        @Override
        protected Drawable doInBackground(String... params) {
            String source = params[0];
            return fetchDrawable(source);
        }

        @Override
        protected void onPostExecute(Drawable result) {
        	if (result != null) {
	            // set the correct bound according to the result from HTTP call
	            urlDrawable.setBounds(0, 0, 0 + result.getIntrinsicWidth(), 0 
	                    + result.getIntrinsicHeight()); 
	
	            // change the reference of the current drawable to the result
	            // from the HTTP call
	            urlDrawable.drawable = result;
	
	            // redraw the image by invalidating the container 
	            HeadlinesImageGetter.this.container.invalidate();

	            // For ICS
	            HeadlinesImageGetter.this.container.setHeight((HeadlinesImageGetter.this.container.getHeight() 
	            		+ result.getIntrinsicHeight()));

	            // Pre ICS
	            HeadlinesImageGetter.this.container.setEllipsize(null);
        	}
        }

        /***
         * Get the Drawable from URL
         * @param urlString
         * @return
         */
        public Drawable fetchDrawable(String urlString) {
            try {
            	Log.d(TAG, "Requesting urlString: " + urlString);
            	
                InputStream is = fetch(urlString);
                Drawable drawable = Drawable.createFromStream(is, "src");
                drawable.setBounds(0, 0, 0 + drawable.getIntrinsicWidth(), 0 
                        + drawable.getIntrinsicHeight()); 
                return drawable;
            } catch (Exception e) {
                return null;
            } 
        }

        private InputStream fetch(String urlString) throws MalformedURLException, IOException {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(urlString);
            HttpResponse response = httpClient.execute(request);
            return response.getEntity().getContent();
        }
    }
}
