package net.mandaria.radioreddit.tasks;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import net.mandaria.radioreddit.RadioRedditApplication;
import net.mandaria.radioreddit.apis.RadioRedditAPI;
import net.mandaria.radioreddit.objects.RadioSong;
import android.app.Application;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


public class GetCurrentSongInformationTask extends AsyncTask<Void, RadioSong, RadioSong> 
{
	private static String TAG = "RadioReddit";
	private Context _context;
	private Locale _locale;
	private RadioRedditApplication _application;
	private Exception ex;
	
	

    public GetCurrentSongInformationTask(RadioRedditApplication application, Context context, Locale locale) 
    {
    	_context = context;
    	_locale = locale;
    	_application = application;
    }

	@Override
	protected RadioSong doInBackground(Void... unused) 
	{
		RadioSong song = null;
		try 
		{
			song = RadioRedditAPI.GetCurrentSongInformation(_context, _application);
            
            //Log.e(TAG, "New ad refresh rate: " + adRefreshRate);
		}
		catch(Exception e)
		{
			ex = e;
			Log.e(TAG, "FAIL: get current song information: " + e);
		}
		
		return song;
	}

	@Override
	protected void onProgressUpdate(RadioSong... item) 
	{

	}

	@Override
	protected void onPostExecute(RadioSong result) 
	{
		// TODO: has the station changed since we started the async call?  If so, then don't update the current song
		if(result != null)
		{
			_application.CurrentSong = result;
			Log.e(TAG, "Post execute: " + result);
		}
		else
		{
			Log.e(TAG, "FAIL: Post execute: " + result);
		}
		
		if(ex != null)
			Log.e(TAG, "FAIL: EXCEPTION: Post execute: " + ex);
		
	}
}