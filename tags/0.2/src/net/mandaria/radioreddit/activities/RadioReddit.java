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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.flurry.android.FlurryAgent;

import net.mandaria.radioreddit.R;
import net.mandaria.radioreddit.RadioRedditApplication;
import net.mandaria.radioreddit.data.DatabaseService;
import net.mandaria.radioreddit.media.PlaybackService;
import net.mandaria.radioreddit.media.StreamProxy;
import net.mandaria.radioreddit.objects.RadioStreams;
import net.mandaria.radioreddit.objects.RedditAccount;
import net.mandaria.radioreddit.tasks.GetRadioStreamsTask;
import net.mandaria.radioreddit.tasks.VoteRedditTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RadioReddit extends Activity
{
	TextView lbl_station;
	TextView lbl_SongVote;
	TextView lbl_SongTitle;
	TextView lbl_SongArtist;
	TextView lbl_SongPlaylist;
	ImageView btn_SongInfo;
	TextView lbl_Buffering;
	TextView lbl_Connecting;
	LinearLayout div_header;
	LinearLayout div_station;

	ProgressBar progress_LoadingSong;
	ImageView img_Logo;

	Button btn_play;
	Button btn_downvote;
	Button btn_upvote;
	StreamProxy proxy;

	private String LOG_TAG = "RadioReddit";
	private int sdkVersion = 0;

	private Handler mHandler = new Handler();
	private long mLastStreamsInformationUpdateMillis = 0;
	private boolean isStreamCacheLoaded = false;

	private PlaybackService player;
	private ServiceConnection conn;
	private BroadcastReceiver changeReceiver = new PlaybackChangeReceiver();
	private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiver();
	private BroadcastReceiver closeReceiver = new PlaybackCloseReceiver();
	
	@Override
	public void onStart()
	{
	   super.onStart();
	   FlurryAgent.onStartSession(this, getString(R.string.flurrykey));
	}
	
	@Override
	public void onStop()
	{
	   super.onStop();
	   FlurryAgent.onEndSession(this);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		RadioRedditApplication application = (RadioRedditApplication) getApplication();

		new GetRadioStreamsTask(application, RadioReddit.this, Locale.getDefault()).execute();

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
		setContentView(R.layout.main);

		div_header = (LinearLayout) findViewById(R.id.div_header);
		if(sdkVersion >= 11)
		{
			div_header.setVisibility(View.GONE);
		}

		div_station = (LinearLayout) findViewById(R.id.div_station);
		div_station.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ChooseStation();
			}
		});

		lbl_SongVote = (TextView) findViewById(R.id.lbl_SongVote);
		lbl_SongTitle = (TextView) findViewById(R.id.lbl_SongTitle);
		lbl_SongArtist = (TextView) findViewById(R.id.lbl_SongArtist);
		lbl_SongPlaylist = (TextView) findViewById(R.id.lbl_SongPlaylist);
		btn_SongInfo = (ImageView) findViewById(R.id.btn_SongInfo);
		btn_SongInfo.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				FlurryAgent.onEvent("radio reddit - View Episode Info");
				ViewEpisodeInfo();
			}
		});

		lbl_Buffering = (TextView) findViewById(R.id.lbl_Buffering);
		lbl_Connecting = (TextView) findViewById(R.id.lbl_Connecting);
		img_Logo = (ImageView) findViewById(R.id.img_Logo);
		progress_LoadingSong = (ProgressBar) findViewById(R.id.progress_LoadingSong);

		btn_upvote = (Button) findViewById(R.id.btn_upvote);
		btn_upvote.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				RadioRedditApplication application = (RadioRedditApplication) getApplication();
				// TODO: when do we allow them to vote? only when song info is available?
				if(application.CurrentSong != null || application.CurrentEpisode != null)
				{
					// TODO: consolidate ifs better
					if(application.CurrentStream.Type.equals("music"))
					{
						if(application.CurrentSong.Name.equals(""))
						{
							// TODO: show dialog warning
							final AlertDialog.Builder builder = new AlertDialog.Builder(RadioReddit.this);
						    builder.setMessage(getString(R.string.warning_SubmitSong) + "\n\n" + getString(R.string.warning_SubmitNote))
						    	.setTitle(getString(R.string.submit_title))
						    	.setIcon(android.R.drawable.ic_dialog_alert)
						    	.setCancelable(true)
						        .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener()
								{
									
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										new VoteRedditTask((RadioRedditApplication)getApplication(), RadioReddit.this, true, "", "").execute();
										setUpOrDownVote("true");
										
									}
								})
						        .setNegativeButton(getString(R.string.no), null);
						    
						    final AlertDialog alert = builder.create();
						    alert.show();
						}
						else
						{
							new VoteRedditTask(application, RadioReddit.this, true, "", "").execute();
							setUpOrDownVote("true");
						}
					}
					else if(application.CurrentStream.Type.equals("talk"))
					{
						if(application.CurrentEpisode.Name.equals(""))
						{
							final AlertDialog.Builder builder = new AlertDialog.Builder(RadioReddit.this);
						    builder.setMessage(getString(R.string.warning_SubmitEpisode) + "\n\n" + getString(R.string.warning_SubmitNote))
						    	.setTitle(getString(R.string.submit_title))
						    	.setIcon(android.R.drawable.ic_dialog_alert)
						    	.setCancelable(true)
						        .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener()
								{
									
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										new VoteRedditTask((RadioRedditApplication)getApplication(), RadioReddit.this, true, "", "").execute();
										setUpOrDownVote("true");
										
									}
								})
						        .setNegativeButton(getString(R.string.no), null);
						    
						    final AlertDialog alert = builder.create();
						    alert.show();
						}	
						else
						{
							new VoteRedditTask(application, RadioReddit.this, true, "", "").execute();
							setUpOrDownVote("true");
						}
					}
				}
			}
		});
		
		btn_downvote = (Button) findViewById(R.id.btn_downvote);
		btn_downvote.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				RadioRedditApplication application = (RadioRedditApplication) getApplication();
				// TODO: when do we allow them to vote? only when song info is available?
				if(application.CurrentSong != null || application.CurrentEpisode != null)
				{
					// TODO: consolidate ifs better
					// TODO: consolidate with btn_upvote function
					if(application.CurrentStream.Type.equals("music"))
					{
						if(application.CurrentSong.Name.equals(""))
						{
							// TODO: show dialog warning
							final AlertDialog.Builder builder = new AlertDialog.Builder(RadioReddit.this);
						    builder.setMessage(getString(R.string.warning_SubmitSong) + "\n\n" + getString(R.string.warning_SubmitNote))
						    	.setTitle(getString(R.string.submit_title))
						    	.setIcon(android.R.drawable.ic_dialog_alert)
						    	.setCancelable(true)
						        .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener()
								{
									
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										new VoteRedditTask((RadioRedditApplication)getApplication(), RadioReddit.this, false, "", "").execute();
										setUpOrDownVote("false");
										
									}
								})
						        .setNegativeButton(getString(R.string.no), null);
						    
						    final AlertDialog alert = builder.create();
						    alert.show();
						}
						else
						{
							new VoteRedditTask(application, RadioReddit.this, false, "", "").execute();
							setUpOrDownVote("false");
						}
					}
					else if(application.CurrentStream.Type.equals("talk"))
					{
						if(application.CurrentEpisode.Name.equals(""))
						{
							final AlertDialog.Builder builder = new AlertDialog.Builder(RadioReddit.this);
						    builder.setMessage(getString(R.string.warning_SubmitEpisode) + "\n\n" + getString(R.string.warning_SubmitNote))
						    	.setTitle(getString(R.string.submit_title))
						    	.setIcon(android.R.drawable.ic_dialog_alert)
						    	.setCancelable(true)
						        .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener()
								{
									
									@Override
									public void onClick(DialogInterface dialog, int which)
									{
										new VoteRedditTask((RadioRedditApplication)getApplication(), RadioReddit.this, false, "", "").execute();
										setUpOrDownVote("false");
										
									}
								})
						        .setNegativeButton(getString(R.string.no), null);
						    
						    final AlertDialog alert = builder.create();
						    alert.show();
						}	
						else
						{
							new VoteRedditTask(application, RadioReddit.this, false, "", "").execute();
							setUpOrDownVote("false");
						}
					}
				}
			}
		});
		
		btn_play = (Button) findViewById(R.id.btn_play);
		btn_play.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// TODO: Might need to re-write this function due to PlayerService changes
				if(player == null)
				{
					attachToPlaybackService();
				}
				else
				{
					if(!player.isPreparing()) // We can't do anything while media player is preparing....
					{
						if(player.isPlaying())
						{
							FlurryAgent.onEvent("radio reddit - Stop Button");
							player.stop();

							hideSongInformation();

							showPlayButton();

							lbl_Buffering.setVisibility(View.GONE);
							progress_LoadingSong.setVisibility(View.GONE);
						}
						else
						{
							FlurryAgent.onEvent("radio reddit - Play Button");
							playStream();
						}
					}
					else
					{
						// But we can tell the media player we actually don't want to start playing, we changed our mind
						if(!player.isAborting())
						{
							FlurryAgent.onEvent("radio reddit - Stop Button - Abort");
							// Mediaplayer is preparing, we want to not stream
							player.abort();

							hideSongInformation();

							showPlayButton();

							lbl_Buffering.setVisibility(View.GONE);
							progress_LoadingSong.setVisibility(View.GONE);
						}
						else
						{
							FlurryAgent.onEvent("radio reddit - Play Button - Stop Abort");
							// Mediaplayer is preparing, we want to stream (even though we previously aborted)
							player.stopAbort();

							hideSongInformation();

							showStopButton();

							progress_LoadingSong.setVisibility(View.VISIBLE);
						}
					}
				}
			}
		});

		startUpdateTimer();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		RadioRedditApplication application = (RadioRedditApplication) getApplication();

		MenuItem chooseStation = (MenuItem) menu.findItem(R.id.chooseStation);
		MenuItem viewEpisodeInfo = (MenuItem) menu.findItem(R.id.viewEpisodeInfo);
		MenuItem login = (MenuItem) menu.findItem(R.id.login);
		MenuItem logout = (MenuItem) menu.findItem(R.id.logout);
		
		RedditAccount account = Settings.getRedditAccount(RadioReddit.this);
		
		if(account != null)
		{
			login.setVisible(false);
			logout.setVisible(true);
			
			logout.setTitle(getString(R.string.logout) + " (" + account.Username + ")");
		}
		else
		{
			login.setVisible(true);
			logout.setVisible(false);
		}

		// Connecting to radio reddit
		if(application.RadioStreams == null || application.RadioStreams.size() == 0)
		{
			chooseStation.setEnabled(false);
		}
		else
		{
			chooseStation.setEnabled(true);
		}

		if(application.CurrentEpisode != null)
			viewEpisodeInfo.setVisible(true);
		else
			viewEpisodeInfo.setVisible(false);

		if(sdkVersion >= 11)
		{
			// TODO: maybe check if there is enough room to write the full text "Current station: main"
			//getResources().getString(R.string.currentStation) + ": " +
			if(application.CurrentStream != null)
				getActionBar().setTitle( application.CurrentStream.Name);
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		FlurryAgent.onEvent("radio reddit - Menu Button");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.login) 
		{
			FlurryAgent.onEvent("radio reddit - Menu Button - Login");
			startActivity(new Intent(this, Login.class));
			return true;
		}
		else if (item.getItemId() == R.id.logout) 
		{
			FlurryAgent.onEvent("radio reddit - Menu Button - Logout");
			Logout();
			return true;
		} 
		else if (item.getItemId() == R.id.chooseStation) 
		{
			FlurryAgent.onEvent("radio reddit - Menu Button - Choose Station");
			ChooseStation();
			return true;
		} 
		else if (item.getItemId() == R.id.viewEpisodeInfo) 
		{
			FlurryAgent.onEvent("radio reddit - Menu Button - View Episode Info");
			ViewEpisodeInfo();
			return true;
		} 
		else if (item.getItemId() == R.id.settings) 
		{
			FlurryAgent.onEvent("radio reddit - Menu Button - Settings");
			startActivity(new Intent(this, Settings.class));
			return true;
		} 
		else if (item.getItemId() == R.id.email_feedback) {
			FlurryAgent.onEvent("radio reddit - Menu Button - Email Feedback");
			SendEmail();
			return true;
		} 
		else if (item.getItemId() == R.id.exit) 
		{
			FlurryAgent.onEvent("radio reddit - Menu Button - Exit App");
			ExitApp();
			return true;
		}
		return false;
	}
	
	private void Logout()
	{		
		RedditAccount emptyAccount = new RedditAccount();
		emptyAccount.Username = emptyAccount.Cookie = emptyAccount.Modhash = "";
		
		Settings.setRedditAccount(RadioReddit.this, emptyAccount);
		
		Toast.makeText(RadioReddit.this,  getString(R.string.youHaveBeenLoggedOut), Toast.LENGTH_LONG).show();
	}

	private void ViewEpisodeInfo()
	{
		Intent i = new Intent(RadioReddit.this, ViewEpisodeInformation.class);
		startActivity(i);
	}

	private void ChooseStation()
	{
		RadioRedditApplication application = (RadioRedditApplication) getApplication();

//		if(player != null && !player.isPreparing()) // check if a current listen is in progress to prevent state exception
//		{
			if(application.RadioStreams != null && application.RadioStreams.size() > 0)
			{
				Intent i = new Intent(RadioReddit.this, SelectStation.class);
				startActivityForResult(i, 1);
			}
			else
			{
				// Try to get streams again
				new GetRadioStreamsTask(application, RadioReddit.this, Locale.getDefault()).execute();
			}
//		}
//		else
//		{
//			Toast.makeText(RadioReddit.this, getString(R.string.pleaseWaitToChangeStation), Toast.LENGTH_LONG).show();
//		}
	}

	private void ExitApp()
	{
		// kill the service, then exit to home launcher
		if(player != null)
			player.stopSelf();

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void SendEmail()
	{
		// Setup an intent to send email
		Intent sendIntent;
		sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("application/octet-stream");
		// Address
		sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.email_address) });
		// Subject
		String appName = getString(R.string.app_name);
		String version = "";
		try
		{
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch(NameNotFoundException e)
		{

		}

		sendIntent.putExtra(Intent.EXTRA_SUBJECT, appName + " " + version);
		// Body
		String body = "\n\n\n\n\n";
		body += getString(R.string.email_using_custom_rom) + "\n";
		body += "--------------------\n";
		body += getString(R.string.email_do_not_edit_message) + "\n\n";
		body += "BOARD: " + Build.BOARD + "\n";
		body += "BRAND: " + Build.BRAND + "\n";
		body += "CPU_ABI: " + Build.CPU_ABI + "\n";
		body += "DEVICE: " + Build.DEVICE + "\n";
		body += "DISPLAY: " + Build.DISPLAY + "\n";
		body += "FINGERPRINT: " + Build.FINGERPRINT + "\n";
		body += "HOST: " + Build.HOST + "\n";
		body += "ID: " + Build.ID + "\n";
		body += "MANUFACTURER: " + Build.MANUFACTURER + "\n";
		body += "MODEL: " + Build.MODEL + "\n";
		body += "PRODUCT: " + Build.PRODUCT + "\n";
		body += "TAGS: " + Build.TAGS + "\n";
		body += "TIME: " + Build.TIME + "\n";
		body += "TYPE: " + Build.TYPE + "\n";
		body += "USER: " + Build.USER + "\n";
		body += "VERSION.CODENAME: " + Build.VERSION.CODENAME + "\n";
		body += "VERSION.INCREMENTAL: " + Build.VERSION.INCREMENTAL + "\n";
		body += "VERSION.RELEASE: " + Build.VERSION.RELEASE + "\n";
		body += "VERSION.SDK: " + Build.VERSION.SDK + "\n";
		body += "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + "\n";

		sendIntent.putExtra(Intent.EXTRA_TEXT, body);
		startActivity(Intent.createChooser(sendIntent, "Send Mail"));
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if(player == null)
		{
			attachToPlaybackService();
		}

		RadioRedditApplication application = (RadioRedditApplication) getApplication();
		lbl_station = (TextView) findViewById(R.id.lbl_station);

		if(application.CurrentStream != null)
			lbl_station.setText(application.CurrentStream.Name);

		startUpdateTimer();

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		stopUpdateTimer();
	}

	private void startUpdateTimer()
	{
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 0);
	}

	private void stopUpdateTimer()
	{
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	@Override
	public void onAttachedToWindow()
	{
		super.onAttachedToWindow();

	}

	public void attachToPlaybackService()
	{
		Intent serviceIntent = new Intent(getApplicationContext(), PlaybackService.class);
		conn = new ServiceConnection()
		{
			@Override
			public void onServiceConnected(ComponentName name, IBinder service)
			{
				player = ((PlaybackService.ListenBinder) service).getService();
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				// Log.w(LOG_TAG, "DISCONNECT");
				player = null;
			}
		};

		// Explicitly start the service. Don't use BIND_AUTO_CREATE, since it causes an implicit service stop when the last binder is removed.
		getApplicationContext().startService(serviceIntent);
		getApplicationContext().bindService(serviceIntent, conn, 0);

		registerReceiver(changeReceiver, new IntentFilter(PlaybackService.SERVICE_CHANGE_NAME));
		registerReceiver(updateReceiver, new IntentFilter(PlaybackService.SERVICE_UPDATE_NAME));
		registerReceiver(closeReceiver, new IntentFilter(PlaybackService.SERVICE_CLOSE_NAME));
	}

	@Override
	public void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		// Toast.makeText(this, "detached from window", Toast.LENGTH_LONG).show();
		// Log.d(LOG_TAG, "detached from window");
		unregisterReceiver(changeReceiver);
		unregisterReceiver(updateReceiver);
		unregisterReceiver(closeReceiver);
		getApplicationContext().unbindService(conn);
	}

	private void showStopButton()
	{
		Resources res = getResources();

		Drawable stop = res.getDrawable(R.drawable.stop_button);
		btn_play.setBackgroundDrawable(stop);
	}

	private void showPlayButton()
	{
		Resources res = getResources();
		Drawable play = res.getDrawable(R.drawable.play_button);
		btn_play.setBackgroundDrawable(play);
	}

	private class PlaybackChangeReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String title = intent.getStringExtra(PlaybackService.EXTRA_TITLE);
			// infoText.setText(title);
			// Toast.makeText(RadioReddit.this, "PlaybackChange - onReceive", Toast.LENGTH_LONG).show();

			if(player != null && (player.isPlaying() || player.isPreparing()))
			{
				showStopButton();
				
				RadioRedditApplication application = (RadioRedditApplication) getApplication();
				
				if(application.CurrentSong == null && application.CurrentEpisode == null)
				{
					hideSongInformation();
	
					// show progress bar while waiting to load song information
					progress_LoadingSong.setVisibility(View.VISIBLE);
				}
			}
			else
				showPlayButton();
		}
	}

	private class PlaybackUpdateReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			RadioRedditApplication application = (RadioRedditApplication) getApplication();
			// Toast.makeText(RadioReddit.this, "PlaybackUpdate - onReceive", Toast.LENGTH_LONG).show();
			int buffered = intent.getIntExtra(PlaybackService.EXTRA_BUFFERED, 0);
			int duration = intent.getIntExtra(PlaybackService.EXTRA_DURATION, 1);
			int position = intent.getIntExtra(PlaybackService.EXTRA_POSITION, 0);
			int downloaded = intent.getIntExtra(PlaybackService.EXTRA_DOWNLOADED, 1);

			// Log.e("RADIO REDDIT BUFFERED", String.valueOf(buffered));
			// Log.e("RADIO REDDIT DURATION", String.valueOf(duration));
			// Log.e("RADIO REDDIT POSITION", String.valueOf(position));
			// Log.e("RADIO REDDIT DOWNLOADED", String.valueOf(downloaded));

			if(player != null && player.isBuffering())
			{
				FlurryAgent.onEvent("radio reddit - Is Buffering");
				hideSongInformation();
				lbl_Buffering.setVisibility(View.VISIBLE);
				progress_LoadingSong.setVisibility(View.VISIBLE);

			}
			else
			{
				FlurryAgent.onEvent("radio reddit - Is Not Buffering");
				lbl_Buffering.setVisibility(View.GONE);
				if(application.CurrentSong != null || application.CurrentEpisode != null)
				{
					progress_LoadingSong.setVisibility(View.GONE);
				}
			}
		}
	}

	private class PlaybackCloseReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{

		}
	}

	protected void listen(String url)
	{
		if(player != null)
		{
			try
			{
				player.listen(url, true);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				Toast.makeText(this, getString(R.string.error_OnListen) + ": " + ex.toString(), Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == Activity.RESULT_OK)
		{
			boolean changedStream = data.getBooleanExtra("changed_stream", false);

			if(changedStream || (player != null && !player.isPlaying())) // check if already playing
			{
				RadioRedditApplication application = (RadioRedditApplication) getApplication();
				application.CurrentSong = null; // clear the current song
				application.CurrentEpisode = null; // clear the current episode
				
				playStream();
			}
		}
	}

	private void playStream()
	{
		RadioRedditApplication application = (RadioRedditApplication) getApplication();

		if(application.CurrentStream != null)
		{
			Map params = new HashMap();
			params.put("station", application.CurrentStream.Name);
			FlurryAgent.onEvent("radio reddit - Play Stream", params);
			listen(application.CurrentStream.Relay);

			lbl_station.setText(application.CurrentStream.Name);

			hideSongInformation();

			// show progress bar while waiting to load song information
			progress_LoadingSong.setVisibility(View.VISIBLE);

			showStopButton();
		}
		else
		{
			Toast.makeText(this, getString(R.string.error_RadioRedditServerIsDownNotification), Toast.LENGTH_LONG).show();

			// Try to get streams again
			new GetRadioStreamsTask(application, RadioReddit.this, Locale.getDefault()).execute();
		}

	}

	private Runnable mUpdateTimeTask = new Runnable()
	{
		@Override
		public void run()
		{
			RadioRedditApplication application = (RadioRedditApplication) getApplication();
			
			// Attempt to load songs from cache
			if(application.RadioStreams == null && isStreamCacheLoaded == false)
			{
				DatabaseService service = new DatabaseService();
				application.RadioStreams = service.GetCachedStreams(RadioReddit.this);
				
				if(application.CurrentStream == null && application.RadioStreams != null && application.RadioStreams.size() > 0)
				{
					application.CurrentStream = RadioStreams.getMainStream(application.RadioStreams);
					
					lbl_station.setText(application.CurrentStream.Name);
				}	
				
				isStreamCacheLoaded = true;
			}

			// Update stream information every 30 seconds
			if((SystemClock.elapsedRealtime() - mLastStreamsInformationUpdateMillis) > 30000)
			{
				new GetRadioStreamsTask(application, RadioReddit.this, Locale.getDefault()).execute();
				mLastStreamsInformationUpdateMillis = SystemClock.elapsedRealtime();
			}

			// Connecting to radio reddit
			if(application.RadioStreams == null || application.RadioStreams.size() == 0)
			{		
				progress_LoadingSong.setVisibility(View.VISIBLE);
				lbl_Connecting.setVisibility(View.VISIBLE);
				div_station.setVisibility(View.GONE);
				btn_play.setVisibility(View.GONE);
				btn_upvote.setVisibility(View.GONE);
				btn_downvote.setVisibility(View.GONE);
				img_Logo.setImageResource(R.drawable.logo_darkeyes);
			}
			else if(application.isRadioRedditDown == true)
			{
				img_Logo.setImageResource(R.drawable.logo_darkeyes);
				lbl_SongTitle.setVisibility(View.VISIBLE);
				lbl_SongTitle.setText(getString(R.string.error_RadioRedditIsDown) + "\n\n" + application.radioRedditIsDownErrorMessage);
			}
			else
			{
				if(player == null || (player != null && !player.isPlaying() && !player.isPreparing()))//if(player != null && !player.isPlaying() && !player.isPreparing())
				{
					progress_LoadingSong.setVisibility(View.GONE);
				}
				lbl_Connecting.setVisibility(View.GONE);
				div_station.setVisibility(View.VISIBLE);
				btn_play.setVisibility(View.VISIBLE);
				btn_upvote.setVisibility(View.VISIBLE);
				btn_downvote.setVisibility(View.VISIBLE);
				img_Logo.setImageResource(R.drawable.logo);
			}

			if(sdkVersion >= 11)
				invalidateOptionsMenu(); // force update of menu to enable "Choose Station" when connected (mainly for Android 3.0)

			if(player != null && player.isPlaying())
			{
				// Update current song information
				if(!player.isBuffering())
				{
					if(application.CurrentSong != null)
					{
						showSongInformation();

						progress_LoadingSong.setVisibility(View.GONE);
					}
					else if(application.CurrentEpisode != null)
					{
						showEpisodeInformation();

						progress_LoadingSong.setVisibility(View.GONE);
					}
				}
			}
			else
			{
				if(application.isRadioRedditDown == false)
					hideSongInformation();
				if(player == null) // if(player != null && !player.isPreparing())
				{
					showPlayButton();
				}
				else if(player != null && player.isPreparing() && !player.isAborting())
					progress_LoadingSong.setVisibility(View.VISIBLE); // show please wait spinner when "re-connecting" to stream from network change
			}

			mHandler.postDelayed(this, 1000); // update every 1 second
		}

	};
	
	// when getting vote info, sets if user previously voted
	private void setUpOrDownVote(String vote)
	{
		if(!vote.equals("null"))
		{
			if(vote.equals("true"))
			{
				lbl_SongVote.setTextColor(getResources().getColor(R.color.UpVote));
				btn_upvote.setBackgroundResource(R.drawable.didupvote_button);
				btn_downvote.setBackgroundResource(R.drawable.willdownvote_button);
			}
			else
			{
				lbl_SongVote.setTextColor(getResources().getColor(R.color.DownVote));
				btn_upvote.setBackgroundResource(R.drawable.willupvote_button);
				btn_downvote.setBackgroundResource(R.drawable.diddownvote_button);
			}
		}
		else
		{
			lbl_SongVote.setTextColor(getResources().getColor(R.color.NoVote));
			btn_upvote.setBackgroundResource(R.drawable.willupvote_button);
			btn_downvote.setBackgroundResource(R.drawable.willdownvote_button);
		}
	}

	private void showEpisodeInformation()
	{
		RadioRedditApplication application = (RadioRedditApplication) getApplication();
		lbl_SongVote.setVisibility(View.VISIBLE);
		lbl_SongTitle.setVisibility(View.VISIBLE);
		lbl_SongArtist.setVisibility(View.VISIBLE);
		lbl_SongPlaylist.setVisibility(View.GONE);
		lbl_SongVote.setText(application.CurrentEpisode.Score);
		lbl_SongTitle.setText(application.CurrentEpisode.EpisodeTitle);
		lbl_SongArtist.setText(application.CurrentEpisode.ShowTitle);
		btn_SongInfo.setVisibility(View.VISIBLE);
		
		setUpOrDownVote(application.CurrentEpisode.Likes);
		
		btn_upvote.setEnabled(true);
		btn_downvote.setEnabled(true);
		
		// lbl_SongVote.setText(getString(R.string.vote_to_submit_song));
		// lbl_SongTitle.setText(getString(R.string.dummy_song_title));
		// lbl_SongArtist.setText(getString(R.string.dummy_song_artist));
		// lbl_SongPlaylist.setText(getString(R.string.dummy_song_playlist));

	}

	private void showSongInformation()
	{
		RadioRedditApplication application = (RadioRedditApplication) getApplication();
		lbl_SongVote.setVisibility(View.VISIBLE);
		lbl_SongTitle.setVisibility(View.VISIBLE);
		lbl_SongArtist.setVisibility(View.VISIBLE);
		lbl_SongPlaylist.setVisibility(View.VISIBLE);
		
		String score = application.CurrentSong.Score;
		if(application.CurrentSong.CumulativeScore != null)
			score += " (" + application.CurrentSong.CumulativeScore + ")";
		
		lbl_SongVote.setText(score);
		lbl_SongTitle.setText(application.CurrentSong.Title);
		lbl_SongArtist.setText(application.CurrentSong.Artist + " (" + application.CurrentSong.Redditor + ")");
		lbl_SongPlaylist.setText(getString(R.string.playlist) + ": " + application.CurrentSong.Playlist);
		btn_SongInfo.setVisibility(View.GONE);
		
		setUpOrDownVote(application.CurrentSong.Likes);
		
		btn_upvote.setEnabled(true);
		btn_downvote.setEnabled(true);
		
		// lbl_SongVote.setText(getString(R.string.vote_to_submit_song));
		// lbl_SongTitle.setText(getString(R.string.dummy_song_title));
		// lbl_SongArtist.setText(getString(R.string.dummy_song_artist));
		// lbl_SongPlaylist.setText(getString(R.string.dummy_song_playlist));

	}

	private void hideSongInformation()
	{
		// remove song information
		lbl_SongVote.setVisibility(View.GONE);
		lbl_SongTitle.setVisibility(View.GONE);
		lbl_SongArtist.setVisibility(View.GONE);
		lbl_SongPlaylist.setVisibility(View.GONE);
		lbl_SongVote.setText("");
		lbl_SongTitle.setText("");
		lbl_SongArtist.setText("");
		lbl_SongPlaylist.setText("");
		btn_SongInfo.setVisibility(View.GONE);
		btn_upvote.setBackgroundResource(R.drawable.willupvote_button);
		btn_downvote.setBackgroundResource(R.drawable.willdownvote_button);
		btn_upvote.setEnabled(false);
		btn_downvote.setEnabled(false);
	}

}