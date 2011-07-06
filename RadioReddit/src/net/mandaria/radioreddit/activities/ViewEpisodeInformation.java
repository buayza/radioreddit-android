/*
 *	radio reddit for android: mobile app to listen to radioreddit.com
 *  Copyright (C) 2011 Bryan Denny
 *  
 *  This file is part of "radio reddit for android"
 *
 *  "radio reddit for android" is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  "radio reddit for android" is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with "radio reddit for android".  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mandaria.radioreddit.activities;

import net.mandaria.radioreddit.R;
import net.mandaria.radioreddit.RadioRedditApplication;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

public class ViewEpisodeInformation extends Activity
{
	private int sdkVersion = 0;

	TextView lbl_ShowTitle;
	TextView lbl_EpisodeTitle;
	TextView lbl_ShowHosts;
	TextView lbl_ShowRedditors;
	TextView lbl_EpisodeDescription;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		try
		{
			sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		}
		catch(NumberFormatException e)
		{

		}

		// Disable title on phones, enable action bar on tablets
		if(sdkVersion < 11)
			requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewepisodeinformation);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		lbl_ShowTitle = (TextView) findViewById(R.id.lbl_ShowTitle);
		lbl_EpisodeTitle = (TextView) findViewById(R.id.lbl_EpisodeTitle);
		lbl_ShowHosts = (TextView) findViewById(R.id.lbl_ShowHosts);
		lbl_ShowRedditors = (TextView) findViewById(R.id.lbl_ShowRedditors);
		lbl_EpisodeDescription = (TextView) findViewById(R.id.lbl_EpisodeDescription);

		RadioRedditApplication application = (RadioRedditApplication) getApplication();

		if(sdkVersion >= 11)
		{
			if(application.CurrentStream != null)
				getActionBar().setTitle(getString(R.string.current_station) + ": " + application.CurrentStream.Name);
		}

		if(application.CurrentEpisode != null)
		{
			lbl_ShowTitle.setText(application.CurrentEpisode.ShowTitle);
			lbl_EpisodeTitle.setText(application.CurrentEpisode.EpisodeTitle);
			lbl_ShowHosts.setText(application.CurrentEpisode.ShowHosts);
			lbl_ShowRedditors.setText(application.CurrentEpisode.ShowRedditors);
			lbl_EpisodeDescription.setText(Html.fromHtml(application.CurrentEpisode.EpisodeDescription));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				// app icon in Action Bar (Android 3.0) clicked; go home
				Intent intent = new Intent(this, RadioReddit.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
