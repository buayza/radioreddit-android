package net.mandaria.radioreddit.errors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import net.mandaria.radioreddit.R;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class CustomExceptionHandler implements UncaughtExceptionHandler 
{

	private static String TAG = "CarDashboard";
	
    private UncaughtExceptionHandler defaultUEH;

    private String url;
    private Context context;

    /* 
     * if any of the parameters is null, the respective functionality 
     * will not be used 
     */
    public CustomExceptionHandler (Context context) 
    {
    	this.context = context;
        this.url = "http://www.bryandenny.com/software/BugReport.aspx";
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) 
    {
    	try
    	{
	    	Log.i(TAG, "Exception caught");
	    	// Stacktrace
	        final Writer result = new StringWriter();
	        final PrintWriter printWriter = new PrintWriter(result);
	        e.printStackTrace(printWriter);
	        String stacktrace = result.toString();
	        printWriter.close();
	        Log.i(TAG, "StackTrace done");
	        // Application
			String appName = context.getString(R.string.app_name);
			Log.i(TAG, "get string");
			String version = "";
			try 
			{
				version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			} catch (NameNotFoundException ex) 
			{
	
			}
			
			String application = appName + " " + version;
			Log.i(TAG, "Application done");
			// Debug
			// TODO: can combine this with email feedback intent
	        String debug = "\n\n\n\n\n";
	        debug += context.getString(R.string.email_using_custom_rom) + "\n";
	        debug += "--------------------\n";
			debug += context.getString(R.string.email_do_not_edit_message) + "\n\n";
	        debug += "BRAND: " + Build.BRAND + "\n";
	        debug += "CPU_ABI: " + Build.CPU_ABI + "\n";
	        debug += "DEVICE: " + Build.DEVICE + "\n";
	        debug += "DISPLAY: " + Build.DISPLAY + "\n";
	        debug += "FINGERPRINT: " + Build.FINGERPRINT + "\n";
	        debug += "HOST: " + Build.HOST + "\n";
	        debug += "ID: " + Build.ID + "\n";
	        debug += "MANUFACTURER: " + Build.MANUFACTURER + "\n";
	        debug += "MODEL: " + Build.MODEL + "\n";
	        debug += "PRODUCT: " + Build.PRODUCT + "\n";
	        debug += "TAGS: " + Build.TAGS + "\n";
	        debug += "TIME: " + Build.TIME + "\n";
	        debug += "TYPE: " + Build.TYPE + "\n";
	        debug += "USER: " + Build.USER + "\n";
	        debug += "VERSION.CODENAME: " + Build.VERSION.CODENAME + "\n";
	        debug += "VERSION.INCREMENTAL: " + Build.VERSION.INCREMENTAL + "\n";
	        debug += "VERSION.RELEASE: " + Build.VERSION.RELEASE + "\n";
	        debug += "VERSION.SDK: " + Build.VERSION.SDK + "\n";
	        debug += "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + "\n";
	        debug += "Total Internal Memory: " + getTotalInternalMemorySize() + "\n";
	        debug += "Available Internal Memory: " + getAvailableInternalMemorySize() + "\n";
	        Log.i(TAG, "Debug done");
	        
	        if (url != null) 
	        {
	            sendToServer(stacktrace, debug, application);
	            Log.i(TAG, "Email Sent");
	        }
    	}
    	catch(Exception ex)
    	{
    		defaultUEH.uncaughtException(t, ex);
    	}

        defaultUEH.uncaughtException(t, e);
        Log.i(TAG, "Default UEH done");
    }
    
    public long getAvailableInternalMemorySize() 
    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }
    
    public long getTotalInternalMemorySize() 
    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    } 

    private void sendToServer(String stacktrace, String debug, String application) 
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("stacktrace", stacktrace));
        nvps.add(new BasicNameValuePair("debug", debug));
        nvps.add(new BasicNameValuePair("application", application));
        try 
        {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            httpClient.execute(httpPost);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
}

