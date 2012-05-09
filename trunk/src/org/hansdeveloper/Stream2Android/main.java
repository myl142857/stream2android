package org.hansdeveloper.Stream2Android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.UPnP;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.control.QueryListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
//import android.content.pm.ActivityInfo;
//import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.VideoView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class main extends ListActivity implements OnCompletionListener, KeyListener, OnClickListener, OnErrorListener, OnPreparedListener {
    Map<String, MediaItem> mVideoItemList = new HashMap<String, MediaItem>();
	MediaRendererDevice upnpDev = null;
	VideoView mVideoView = null;
	private ViewFlipper mViewFlipper; 
	private MainListAdapter mMainListAdapter;
	ListView mListViewMain;
	Button mClearList;
	boolean pre2_3 = false;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        mListViewMain = super.getListView(); 
        mListViewMain.setOnItemClickListener(new OnItemClickListener() 
		{ 
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) 
			{
				upnpDev.mVideoURL = (String)parent.getAdapter().getItem(position);
				upnpDev.handlerUI.sendEmptyMessage(0);
			}
		});
        
		LayoutInflater mInflater = LayoutInflater.from(getApplicationContext());
		try
		{
			pre2_3 = false;

			testMediaMetadataRetriever();
		}
		catch(Exception exep)
		{
			Log.d("Stream2Android", "testMediaMetadataRetriever=" + exep); 
		}
        mMainListAdapter = new MainListAdapter(mInflater, mVideoItemList);

		setListAdapter(mMainListAdapter);

		mViewFlipper = (ViewFlipper)findViewById(R.id.viewstack);
		mClearList = (Button)findViewById(R.id.buttonClearList);
		mClearList.setOnClickListener(this);
		
		LinearLayout viewMediaPlayer = (LinearLayout) mInflater.inflate(R.layout.videoplayer_view, null);
		mViewFlipper.addView(viewMediaPlayer);
		
        //mVideoView = (VideoView)viewMediaPlayer.findViewById(R.id.video_view);
		mVideoView = (VideoView)findViewById(R.id.videoView1);
        
		mVideoView.setOnCompletionListener(this);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnPreparedListener(this);
        
		Log.d("Stream2Android", "getCacheDir=" + this.getCacheDir().getAbsolutePath()); 
        
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
		Log.d("Stream2Android", "getExternalStoragePublicDirectory=" + path.getAbsolutePath()); 
		
		String state = Environment.getExternalStorageState();
		
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		
		if (Environment.MEDIA_MOUNTED.equals(state)) 
		{
			mExternalStorageAvailable  = mExternalStorageWriteable  = true;
			// make file on storage
			Log.d("Stream2Android", "create file testfile.empty on path" + path);
			if (path.exists())
				Log.d("Stream2Android", "1 path exists path=" + path); 
			
			path.mkdirs();
			
			if (path.exists())
				Log.d("Stream2Android", "2 path exists path=" + path); 

			File file = new File(path, "testfile.empty");
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String filepath = file.getAbsolutePath();
			if (file.exists())
				Log.d("Stream2Android", "file exists file=" + filepath); 

		} 
		else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
		{
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} 
		else 
		{
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		Log.d("Stream2Android", "mExternalStorageAvailable=" + mExternalStorageAvailable + ", mExternalStorageWriteable=" + mExternalStorageWriteable); 
     
        String filename = "MediaRendererDevice/MediaRenderer.xml";

		copyAsset(filename);

		filename = "MediaRendererDevice/service/AVTransport.xml";
		copyAsset(filename);

		filename = "MediaRendererDevice/service/ConnectionManager.xml";
		copyAsset(filename);

		filename = "MediaRendererDevice/service/RenderingControl.xml";
		copyAsset(filename);

        filename = "MediaRendererDevice/icons/icon.png";
		copyAsset(filename);

        filename = "MediaRendererDevice/icons/icon.gif";
		copyAsset(filename);

		try {
			UPnP.setEnable(UPnP.USE_ONLY_IPV4_ADDR);
			upnpDev = new MediaRendererDevice(getFilesDir().getAbsolutePath() + "/MediaRenderer.xml");
			upnpDev.setFriendlyName("Android Renderer (" + android.os.Build.MODEL + ")");
		} catch (InvalidDescriptionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    void copyAsset(String filename)
    {
		InputStream isd = null;
		FileOutputStream fos;
	
		String sourcefilename = getFilesDir().getAbsolutePath() + "/" + filename;

		// create destination directory
		File f = new File(sourcefilename);
		
		
        // copy to destination
		try {
			isd = getAssets().open(filename);
			fos = openFileOutput(f.getName(), Context.MODE_PRIVATE);
			while (isd.available() > 0)
			{
				byte[] b = new byte[1024];
				int bytesread = isd.read(b);
				fos.write(b, 0, bytesread);
			}
			fos.close();
			isd.close();

			String destfilename = getFilesDir().getAbsolutePath() + "/" + f.getName();
			
			f = new File(destfilename);
			InputStream fileIn = new FileInputStream(f);

			Log.d("android_asset", f.getName() + " InputStream.read=" + fileIn.available());
			fileIn.close();
	        
	    } catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
    }
	public class MediaRendererDevice extends Device implements ActionListener, QueryListener
	{
		public long startMillis;
		public MediaRendererDevice() {
			super();

		}

		public MediaRendererDevice(String descriptionFileName)
				throws InvalidDescriptionException {
			super(descriptionFileName);
			// TODO Auto-generated constructor stub
			Log.d("Stream2Android", descriptionFileName);
			Log.d("Stream2Android", this.getDeviceType());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(0)).getControlURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(0)).getDescriptionURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(0)).getEventSubURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(0)).getSCPDURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(0)).getActionList().size() + "");
			Log.d("Stream2Android", ((Service)this.getServiceList().get(1)).getControlURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(1)).getDescriptionURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(1)).getEventSubURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(1)).getSCPDURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(1)).getActionList().size() + "");
			Log.d("Stream2Android", ((Service)this.getServiceList().get(2)).getControlURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(2)).getDescriptionURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(2)).getEventSubURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(2)).getSCPDURL());
			Log.d("Stream2Android", ((Service)this.getServiceList().get(2)).getActionList().size() + "");

			this.getService("urn:upnp-org:serviceId:AVTransport").getStateVariable("LastChange").setValue("");

			this.getService("urn:upnp-org:serviceId:AVTransport").getStateVariable("TransportState").setValue("STOPPED");
			this.getService("urn:upnp-org:serviceId:AVTransport").getStateVariable("TransportStatus").setValue("OK");
			this.getService("urn:upnp-org:serviceId:AVTransport").getStateVariable("TransportPlaySpeed").setValue("1");
			
			this.getService("urn:upnp-org:serviceId:AVTransport").getStateVariable("CurrentTrack").setValue("0");
			
			startMillis = 0;

			setActionListener(this);
			setQueryListener(this);
			
			start();
			
		}

		@Override
		public boolean queryControlReceived(StateVariable stateVar) {
			// TODO Auto-generated method stub
			Log.d("Stream2Android", "queryControlReceived");
			return false;
		}
		
		private long durationMillis = 0; 
		public String mVideoURL = "";
		public final static String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";
		// Some variables use the biggest value of an i4 to signify 'NOT_IMPLEMENTED'
		public final static String NOT_IMPLEMENTED_I4 = "2147483647";
		@Override
		public boolean actionControlReceived(Action action) {
			// TODO Auto-generated method stub
			
			
			
			Log.d("Stream2Android", "actionControlReceived from " + action.getRequestHostAddress() + ", " + action.getService().getServiceType() + "." + action.getName());
			if (action.getService().getServiceType().equals("urn:schemas-upnp-org:service:ConnectionManager:1"))
			{
				if(action.getName().equals("GetProtocolInfo"))
				{
					//action.getArgument("Source").setValue("http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_MP_SD_AC3;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mp4:*,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVSPML_MP3;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVSPML_BASE;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:*,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMABASE;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO_320;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO;DLNA.ORG_OP=01,http-get:*:audio/mp4:*");
//					action.getArgument("Source").setValue("http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN,http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_PAL;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_HD_NA_ISO;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG1;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_MP_SD_AAC_MULT5;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_MP_SD_AC3;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=AVC_TS_MP_HD_AC3_T;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVSPML_MP3;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVSPML_BASE;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_BASE;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_FULL;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVHIGH_FULL;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/3gpp:DLNA.ORG_PN=MPEG4_P2_3GPP_SP_L0B_AAC;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/3gpp:DLNA.ORG_PN=MPEG4_P2_3GPP_SP_L0B_AMR;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:audio/mpeg:DLNA.ORG_PN=MP3;DLNA.ORG_OP=01,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMABASE;DLNA.ORG_OP=01,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAFULL;DLNA.ORG_OP=01,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAPRO;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO_320;DLNA.ORG_OP=01,http-get:*:audio/3gpp:DLNA.ORG_PN=AAC_ISO_320;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_MULT5_ISO;DLNA.ORG_OP=01,http-get:*:audio/L16;rate=44100;channels=2:DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01,http-get:*:image/jpeg:*,http-get:*:video/avi:*,http-get:*:video/divx:*,http-get:*:video/x-matroska:*,http-get:*:video/mpeg:*,http-get:*:video/mp4:*,http-get:*:video/x-ms-wmv:*,http-get:*:video/x-msvideo:*,http-get:*:video/x-flv:*,http-get:*:video/x-tivo-mpeg:*,http-get:*:video/quicktime:*,http-get:*:audio/mp4:*,http-get:*:audio/x-wav:*,http-get:*:audio/x-flac:*,http-get:*:application/ogg:*");
					action.getArgument("Source").setValue("");
					action.getArgument("Sink").setValue("http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN,http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_PAL;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_HD_NA_ISO;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_TS_SD_NA_ISO;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mpeg:DLNA.ORG_PN=MPEG1;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_MP_SD_AAC_MULT5;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_MP_SD_AC3;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=AVC_TS_MP_HD_AC3_T;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVSPML_MP3;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVSPML_BASE;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_BASE;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_FULL;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVHIGH_FULL;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/3gpp:DLNA.ORG_PN=MPEG4_P2_3GPP_SP_L0B_AAC;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:video/3gpp:DLNA.ORG_PN=MPEG4_P2_3GPP_SP_L0B_AMR;DLNA.ORG_OP=01;DLNA.ORG_CI=0,http-get:*:audio/mpeg:DLNA.ORG_PN=MP3;DLNA.ORG_OP=01,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMABASE;DLNA.ORG_OP=01,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAFULL;DLNA.ORG_OP=01,http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAPRO;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO_320;DLNA.ORG_OP=01,http-get:*:audio/3gpp:DLNA.ORG_PN=AAC_ISO_320;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO;DLNA.ORG_OP=01,http-get:*:audio/mp4:DLNA.ORG_PN=AAC_MULT5_ISO;DLNA.ORG_OP=01,http-get:*:audio/L16;rate=44100;channels=2:DLNA.ORG_PN=LPCM;DLNA.ORG_OP=01,http-get:*:image/jpeg:*,http-get:*:video/avi:*,http-get:*:video/divx:*,http-get:*:video/x-matroska:*,http-get:*:video/mpeg:*,http-get:*:video/mp4:*,http-get:*:video/x-ms-wmv:*,http-get:*:video/x-msvideo:*,http-get:*:video/x-flv:*,http-get:*:video/x-tivo-mpeg:*,http-get:*:video/quicktime:*,http-get:*:audio/mp4:*,http-get:*:audio/x-wav:*,http-get:*:audio/x-flac:*,http-get:*:application/ogg:*");
//					action.getArgument("Sink").setValue("");
					return true;
				}
				else if(action.getName().equals("GetCurrentConnectionIDs"))
				{
				}
			}
			else if (action.getService().getServiceType().equals("urn:schemas-upnp-org:service:AVTransport:1"))
			{
				if(action.getName().equals("GetTransportInfo"))
				{
					int InstanceID = action.getArgumentIntegerValue("InstanceID");
				
					String  CurrentTransportState = action.getArgument("CurrentTransportState").getRelatedStateVariable().getValue();
					String  CurrentTransportStatus = action.getArgument("CurrentTransportStatus").getRelatedStateVariable().getValue();
					String  CurrentSpeed = action.getArgument("CurrentSpeed").getRelatedStateVariable().getValue();
				
					action.getArgument("CurrentTransportState").setValue(CurrentTransportState);
					action.getArgument("CurrentTransportStatus").setValue(CurrentTransportStatus);
					action.getArgument("CurrentSpeed").setValue(CurrentSpeed);

					Log.d("Stream2Android", "CurrentTransportState=" + CurrentTransportState); 
					Log.d("Stream2Android", "CurrentTransportStatus=" + CurrentTransportStatus); 
					Log.d("Stream2Android", "CurrentSpeed=" + CurrentSpeed); 

					return true;
				}
				else if (action.getName().equals("SetAVTransportURI"))
				{

					startMillis = 0;
					mVideoURL = "";
					
					int InstanceID = action.getArgumentIntegerValue("InstanceID");
					
					String CurrentURI = action.getArgumentValue("CurrentURI");
					action.getArgument("CurrentURI").getRelatedStateVariable().setValue(CurrentURI);
					
					Log.d("Stream2Android", "SetAVTransportURI CurrentURI=" + CurrentURI);

					String CurrentURIMetaData = action.getArgumentValue("CurrentURIMetaData");
					action.getArgument("CurrentURIMetaData").getRelatedStateVariable().setValue(CurrentURIMetaData);

					Log.d("Stream2Android", "SetAVTransportURI CurrentURIMetaData=" + CurrentURIMetaData);
					if (CurrentURIMetaData.equals(""))
					{
						setStateVariable(action, "CurrentTrackDuration", "");
						durationMillis = 0;
						return false;
					}
					
				    /** Handling XML */ 
				    SAXParserFactory spf = SAXParserFactory.newInstance();  

				    SAXParser sp = null;
					try {
						sp = spf.newSAXParser();
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  

				    XMLReader xr = null;
					try {
						xr = sp.getXMLReader();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  
				    
					DIDL_XMLHandler myXMLHandler = new DIDL_XMLHandler();  

				    xr.setContentHandler(myXMLHandler);  

				    try {
						xr.parse(new InputSource(new StringReader(CurrentURIMetaData)));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					if (myXMLHandler.VideoItemList.size() > 0)
					{
						MediaItem media = myXMLHandler.VideoItemList.get(myXMLHandler.VideoItemList.firstKey());
						Log.d("Stream2Android", "stream name=" + myXMLHandler.VideoItemList.firstKey());
						String ImageUrl = "";
						
						ListIterator<SortedMap<String, String>> li = media.getResList().listIterator();
						while (li.hasNext())
						{
							SortedMap<String, String> resdata = li.next();
							// connect to first resource
							String resurl = resdata.get("res");
							Log.d("Stream2Android", "resource url=" + resurl + ""); 
							// correct url for multi ip-clients
							URL u;
							boolean couldconnect = false;
							try {
								u = new URL(resurl);
								Log.d("Stream2Android", "stream urlhost=" + u.getHost() + ", remotehost=" 
										+ action.getRequestHostAddress().split("\\/")[0] + ", localhost=" 
										+ action.getLocalHostAddress()); 
				
								// if no connection can be made, replace host with remote host
								InetSocketAddress sad = new InetSocketAddress(u.getHost(), u.getPort());
								Socket so = new Socket();
								try {
									so.connect(sad, 1000);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									resurl = resurl.replace(u.getHost(), action.getRequestHostAddress().split("\\/")[0]);
									Log.d("Stream2Android", "could not connect to host " + u.getHost() 
											+ ", connect to host " 
											+ action.getRequestHostAddress().split("\\/")[0] + " instead"); 
								}
								finally
								{
									try {
										so.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								Log.e("Stream2Android", "could not connect to host " + e.getMessage());
							}
//							if (couldconnect)
							{
								if (resurl.endsWith(".jpg") 
										|| resurl.endsWith(".png"))
								{
									// store url for later retrieval
									ImageUrl = resurl;
									// if we do have the video-url, w're done
									if (mVideoURL != "")
										break;
									// else, continue to look for the video-url
								}
								else
								{
									// this is the video resource
									mVideoURL = resurl;
									Log.d("Stream2Android", "mVideoURL=" + mVideoURL);
									if (!mVideoURL.contains(".mp4") 
											&& !mVideoURL.contains(".mkv")
											&& !mVideoURL.contains(".wmv")
											&& !mVideoURL.contains(".asf"))
									{
										Log.e("Stream2Android", "unsupported format mVideoURL=" + mVideoURL);
										mVideoURL = "";
										// try next res if any
										continue;
									}
									// format is supported
									// do we already have this url in our list?
									if (mVideoItemList.containsKey(mVideoURL))
									{
										Log.d("Stream2Android", "get media from listadapter");
										media = mVideoItemList.get(mVideoURL);
										Log.d("Stream2Android", "got media from listadapter, name =" + media.getTitle());
									}
									else
									{
										// no, add to the list and update listview
										Log.d("Stream2Android", "save mediaitem to listadapter");
										media.setUrl(mVideoURL);
										mVideoItemList.put(mVideoURL, media);

										// update listview
										Log.d("Stream2Android", "update listview");
										handlerUI.sendEmptyMessage(2);
									}
									// update duration
									String duration = resdata.get("duration");
									if (duration != null)
									{
										Log.d("Stream2Android", "duration=" + durationMillis);
										durationMillis = fromHMS2Millis(duration);
										Log.d("Stream2Android", "duration=" + durationMillis + " ms");
										
										setStateVariable(action, "CurrentTrackDuration", "" + durationMillis);
									}
									break;
								}
							}
/*							else
							{
								Log.e("Stream2Android", "could not connect to host " + mVideoURL);
								mVideoURL = "";
								return false;
							}
*/						}
						String filename = "tempvideo";
						if (mVideoURL.contains(".mp4"))
						{
							filename += ".mp4";
						}
						else if (mVideoURL.contains(".wmv"))
						{
							filename += ".wmv";
						}
						else if (mVideoURL.contains(".asf"))
						{
							filename += ".wmv";
						}
						else if (mVideoURL.contains(".mkv"))
						{
							filename += ".mkv";
						}
						else
						{
							Log.e("Stream2Android", "unsupported format mVideoURL=" + mVideoURL);
							mVideoURL = "";
							return false;
						}

						if (media.image == null && ImageUrl == "")
						{
							// no imageurl from res, try to get url from albumarturi
							ListIterator<SortedMap<String, String>> la = media.getAlbumartList().listIterator();
							while (la.hasNext())
							{
								SortedMap<String, String> resdata = la.next();
								ImageUrl = resdata.get("albumArtURI");
								break;
							}
						}
						if (media.image == null && ImageUrl != "")
						{
							// no image but we do have an image-url
							// retreive image from this url
							Bitmap bmImg;
							URL myFileUrl = null;          
						  	try {
						  		myFileUrl = new URL(ImageUrl);
						  	} catch (MalformedURLException e) {
						       	// TODO Auto-generated catch block
						       	e.printStackTrace();
						       	// not a valid image url
								Log.e("Stream2Android", "not a valid image url=" + ImageUrl);
								return true;
						  	}
						  	try {
						       	HttpURLConnection conn = (HttpURLConnection)myFileUrl.openConnection();
						       	conn.setDoInput(true);
						       	conn.connect();
						       	InputStream is = conn.getInputStream();
						       
						       	bmImg = BitmapFactory.decodeStream(is);

						       	if (bmImg != null)
								{
									java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
									bmImg.compress(Bitmap.CompressFormat.JPEG, 90, os);
									media.image = os.toByteArray();
									Log.d("Stream2Android", "download image succeeded, size=" + bmImg.getWidth() + "x" + bmImg.getHeight()); 

									// display the thumb and return succes
									handlerUI.sendEmptyMessage(2);

									return true;
								}
								else
								{
									Log.e("Stream2Android", "download image failed"); 
								}
						       	
						  	} catch (IOException e) {
						       	// TODO Auto-generated catch block
						       		e.printStackTrace();
						  	}
						}
						
						// do we have an image?
						if (media.image == null)
						{
							// no, retreive image from video-stream
							// capture stream to disk 
							boolean couldConnect = false;
		
							HttpURLConnection conn = null;
							BufferedInputStream in = null;
							OutputStream raf = null;
							Socket s = null;
							String filepath = "";
						
							try {
								
								Log.d("Stream2Android", "connect to mVideoURL=" + mVideoURL);
								
								URL u = new URL(mVideoURL);
								InetSocketAddress sa = new InetSocketAddress(u.getHost(), u.getPort());
								s = new Socket();
								s.connect(sa, 1000);
								
								int byterate = Integer.parseInt(media.getResList().getFirst().get("bitrate"));
								
								int BUFFER_SIZE = byterate;
		
								conn = (HttpURLConnection)u.openConnection();
								//conn.setRequestProperty("Range", "bytes=0-" + BUFFER_SIZE);
								conn.connect();
								Log.d("Stream2Android", "connected to mVideoURL=" + mVideoURL);
								int responsecode = conn.getResponseCode();
								if (responsecode / 100 == 2)
								{
									Log.d("Stream2Android", "response OK from mVideoURL=" + mVideoURL);
									in = new BufferedInputStream(conn.getInputStream());
									
									File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
									String state = Environment.getExternalStorageState();
									
									boolean mExternalStorageAvailable = false;
									boolean mExternalStorageWriteable = false;
									
									if (Environment.MEDIA_MOUNTED.equals(state)) 
									{
										mExternalStorageAvailable  = mExternalStorageWriteable  = true;
									} 
									else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
									{
										mExternalStorageAvailable = true;
										mExternalStorageWriteable = false;
									} 
									else 
									{
										mExternalStorageAvailable = mExternalStorageWriteable = false;
									}
									
									Log.d("Stream2Android", "mExternalStorageAvailable=" + mExternalStorageAvailable + ", mExternalStorageWriteable=" + mExternalStorageWriteable); 
									path.mkdirs();
		
									File file = new File(path, filename);
									file.createNewFile();
									
									filepath = file.getAbsolutePath();
									Log.d("Stream2Android", "save 100KB from stream to file=" + filepath); 
									
									raf = new FileOutputStream(file);
									byte data[] = new byte[BUFFER_SIZE];
									int numRead;
									int totalread = 0;
									while((numRead = in.read(data, 0, BUFFER_SIZE)) != -1)
									{
										
										raf.write(data, 0, numRead);
										totalread += numRead;
										if (totalread >= BUFFER_SIZE)
											break;
									}
									conn.disconnect();
									couldConnect = true;
								}
								else
								{
									Log.e("Stream2Android", "error receiving stream: responsemessage=" + conn.getResponseMessage());
								}
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							finally { 
						        if (s != null) { 
						            try {
										s.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} 
						        } 
						  
						        if (conn != null) { 
						            conn.disconnect(); 
						        } 
						  
						        if (raf != null) { 
						            try { 
						                raf.close(); 
						            } catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} 
						        } 
						  
						        if (in != null) { 
						            try { 
						                in.close(); 
						            } catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} 
						        } 
						    }
							couldConnect = true;
							if (couldConnect
									&& filepath.length() > 0
									)
							{
								/*try
								{
									Log.d("Stream2Android", "createVideoThumbnail create from file=" + filepath); 
									
									InputStream fileIn = new FileInputStream(filepath);
		
									Log.d("Stream2Android", "createVideoThumbnail InputStream.available=" + fileIn.available());
									fileIn.close();
									
	//								Bitmap m = ThumbnailUtils.createVideoThumbnail(filepath, android.provider.MediaStore.Video.Thumbnails.MICRO_KIND);
									MediaMetadataRetriever m = 
										new MediaMetadataRetriever();
	
									m.setDataSource(filepath);
									
									Bitmap bm = null;
									if (pre2_3) 
									{
										m.setMode(android.media.MediaMetadataRetriever.MODE_CAPTURE_FRAME_ONLY);
										bm = m.captureFrame();
									}
									else
										bm = m.getFrameAtTime();
									
									if (bm != null)
									{
										java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
										bm.compress(Bitmap.CompressFormat.JPEG, 90, os);
										media.image = os.toByteArray();
										Log.d("Stream2Android", "captureFrame succeeded, size=" + bm.getWidth() + "x" + bm.getHeight()); 
	
										// display the thumb and return
										handlerUI.sendEmptyMessage(2);
	
										return true;
									}
									else
									{
										Log.e("Stream2Android", "captureFrame failed"); 
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								*/
							}
						}
						return true;
					}
					else
					{
						// no videoitem found
						Log.e("Stream2Android", "no videoitem found"); 
						mVideoURL = "";
						return false;
					}
				}
				else if (action.getName().equals("Play"))
				{
					int InstanceID = action.getArgumentIntegerValue("InstanceID");
					
					String Speed = action.getArgumentValue("Speed");
					action.getArgument("Speed").getRelatedStateVariable().setValue(Speed);

					Log.d("Stream2Android", "play url: " + mVideoURL); 

					if (mVideoURL.length() > 0)
					{
						// start video playback
						handlerUI.sendEmptyMessage(0);
						
						// send new transportstate
						setStateVariable(action, "TransportState", "PLAYING");
						
						// simulate playback progress
						startMillis = Calendar.getInstance().getTimeInMillis(); 
					}						
						
					return true;
				}
				else if (action.getName().equals("Stop"))
				{
					startMillis = 0;
					
					handlerUI.sendEmptyMessage(1);
					
					int InstanceID = action.getArgumentIntegerValue("InstanceID");

					setStateVariable(action, "TransportState", "STOPPED");
					return true;
				}
				else if (action.getName().equals("GetPositionInfo"))
				{
					int InstanceID = action.getArgumentIntegerValue("InstanceID");

					// Track: return current track
					String CurrentTrack = action.getArgument("Track").getRelatedStateVariable().getValue();
					action.getArgument("Track").setValue(CurrentTrack);
					
					// TrackDuration
					String TrackDuration = action.getArgument("TrackDuration").getRelatedStateVariable().getValue();
					action.getArgument("TrackDuration").setValue(TrackDuration);
				
					// TrackMetaData
					String TrackMetaData = action.getArgument("TrackMetaData").getRelatedStateVariable().getValue();
					action.getArgument("TrackMetaData").setValue(TrackMetaData);
					
					// TrackURI
					String TrackURI = action.getArgument("TrackURI").getRelatedStateVariable().getValue();
					action.getArgument("TrackURI").setValue(TrackURI);
					
					// RelTime =
					String RelTime = "";
					//if (startMillis > 0)
					try
					{
						if (mVideoView.isPlaying())
						{
							long millisecondsfromstart = mVideoView.getCurrentPosition();
							RelTime = DisplayProgress(millisecondsfromstart);
						}
					}
					catch(Exception ex)
					{
						
					}
						action.getArgument("RelTime").setValue(RelTime);
					
					Log.d("Stream2Android", "GetPositionInfo RelTime=" + RelTime);
					
					setStateVariable(action, "RelativeTimePosition", RelTime);
					
					// AbsTime = NOT_IMPLEMENTED
					action.getArgument("AbsTime").setValue(NOT_IMPLEMENTED);
					
					// RelCount = NOT_IMPLEMENTED_I4
					action.getArgument("RelCount").setValue(NOT_IMPLEMENTED_I4);
					
					// AbsCount = NOT_IMPLEMENTED_I4
					action.getArgument("AbsCount").setValue(NOT_IMPLEMENTED_I4);
					
					return true;
				}
			}
			return false;
		}
		private Handler handlerUI = new Handler() 
	    {
	    	@Override
	    	public void handleMessage(Message msg) {
	    		switch (msg.what)
	    		{
	    			case 0:
	    				String osVer = System.getProperty("os.version");
	    				String url = mVideoURL;
	    				
						if (mVideoURL.startsWith("http://") )
						{
							url = mVideoURL.replace("192.168.0.2:8201", "vlc.familie-debruijn.nl:8201");
//							if(osVer.endsWith("hi3716c"))
//								mVideoView.setVideoURI(Uri.parse(url));
//							else
//								mVideoView.setVideoURI(Uri.parse("ss" + url));
						}
						else if (mVideoURL.startsWith("rtsp://"))
						{
							url = mVideoURL.replace("192.168.0.2:554", "vlc.familie-debruijn.nl:554");
						}
						
				        mVideoView.setVideoPath(url);
						
	    				Log.d("Stream2Android", "startPlayback: " + url);	    				
						mVideoView.start();
						return;
	    			
	    			case 1:
	    				Log.d("Stream2Android", "stopPlayback: " + mVideoURL);	    				
	    				mVideoView.stopPlayback();
						mListViewMain.setVisibility(View.VISIBLE);
						mClearList.setVisibility(View.VISIBLE);
	    				return;
	    			
	    			case 2:
        				mListViewMain.invalidateViews();
	    		}
	    	}
	    };
	    public void setStateVariable(String ServiceId, String varname, String varvalue)
		{
	
			this.getService(ServiceId).getStateVariable(varname).setValue(varvalue);

			this.getService(ServiceId).getStateVariable("LastChange").setValue("<Event><InstanceID val=\"0\"><" 
					+ varname + " val=\"" 
					+ varvalue + 
					"\" /></InstanceID></Event>");
		}
		public void setStateVariable(Action action, String varname, String varvalue)
		{
			action.getService().getStateVariable(varname).setValue(varvalue);
	
			action.getService().getStateVariable("LastChange").setValue("<Event><InstanceID val=\"0\"><" 
					+ varname + " val=\"" 
					+ varvalue + 
					"\" /></InstanceID></Event>");
		}
		private long fromHMS2Millis(String mhsIn)
		{
			long millis = Long.parseLong(mhsIn.split("\\.")[1]);
			long seconds = 0;
			long minutes = 0;
			long hours = 0;
			
			String[] mhs = mhsIn.split("\\.")[0].split(":");
			if (mhs.length == 3)
			{
				seconds = Long.parseLong(mhs[2]);
				minutes = Long.parseLong(mhs[1]);
				hours = Long.parseLong(mhs[0]);
			}
			else if (mhs.length == 2)
			{
				seconds = Long.parseLong(mhs[1]);
				minutes = Long.parseLong(mhs[0]);
			}
			else
			{
				seconds = Long.parseLong(mhs[0]);
			}

			return millis + (seconds*1000) + (minutes*60*1000) + (hours*60*60*1000);
		}
		private String DisplayProgress(long frommili)
		{

			String s = "";
			
			long hours = frommili / 3600000;
			s = hours + ":";
				
			long minutes = (frommili - (hours * 3600000)) / 60000;
			if (minutes < 10)
				s += "0" + minutes + ":";
			else
				s += minutes + ":";
			
			long seconds = (frommili - (hours * 3600000) - minutes * 60000) / 1000;
			if (seconds < 10)
			{
				s += "0" + seconds;
			}
			else
				s += seconds;
			return s;
		}
	}
    public class DIDL_XMLHandler extends DefaultHandler 
    {  
        Boolean currentElement = false;  
        String currentValue = "";
        //String duration = "";
        //String protocolInfo = "";
        public String sTitle = "";

        public SortedMap<String, MediaItem> VideoItemList = new TreeMap<String, MediaItem>();
        public SortedMap<String, MediaItem> AudioItemList = new TreeMap<String, MediaItem>();
        public SortedMap<String, MediaItem> ImageItemList = new TreeMap<String, MediaItem>();
        
        public SortedMap<String, String> streamattributes = null;
        public SortedMap<String, String> albumartattributes = null;

        MediaItem currentItem = null;

        public DIDL_XMLHandler()
        {
        }
    	/** Called when tag starts*/ 
	    @Override 
	    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	    {  
	    	currentValue = "";
	    	if (qName.equalsIgnoreCase("item"))
	    	{
	    		currentItem = new MediaItem();
	    	}
	    	else if (qName.equalsIgnoreCase("dc:title")) 
	        {  
	        	currentElement = true;
	        }  
	    	else if (qName.equalsIgnoreCase("upnp:class")) 
	        {  
	        	currentElement = true;
	        }  
	         else if (qName.equalsIgnoreCase("upnp:albumArtURI"))
	         {
	        	 albumartattributes = new TreeMap<String, String>();
	        	 
	        	 albumartattributes.put("protocolInfo", attributes.getValue("dlna:profileID"));
	        	 
	        	 currentElement = true;
	        	 
	         }
	        else if (qName.equalsIgnoreCase("res")) 
	        {  
	        	streamattributes = new TreeMap<String, String>();
	        	
	        	for (int attributecount = 0; attributecount < attributes.getLength(); attributecount++)
	        	{
		        	streamattributes.put(attributes.getLocalName(attributecount), attributes.getValue(attributecount));
	        	}
				
	        	currentElement = true;
	        }  
    	}  
	     
		@Override 
		public void characters(char[] ch, int start, int length)  
			throws SAXException {  

			if (currentElement) {
				currentValue += new String(ch, start, length);  
			}  
		}  
	     /** Called when tag closing */ 
        @Override 
        public void endElement(String uri, String localName, String qName) throws SAXException {  
            /** set value */ 
            if (qName.equalsIgnoreCase("dc:title")) 
            {
            	currentItem.setTitle(currentValue);
            }  
            else if (qName.equalsIgnoreCase("upnp:class")) 
            {
            	currentItem.setUpnp_class(currentValue);
            }  
			 else if (qName.equalsIgnoreCase("upnp:albumArtURI"))
			 {
				 albumartattributes.put("albumArtURI", currentValue);
			     currentItem.getAlbumartList().add(albumartattributes);
			     
			 }
            else if (qName.equalsIgnoreCase("res")) 
            {
	        	streamattributes.put("res", currentValue);

	        	currentItem.getResList().add(streamattributes);
            }
            else if (qName.equalsIgnoreCase("item")) 
	        {
	        	if (currentItem.getUpnp_class().equalsIgnoreCase("object.item.videoItem"))
				{
					Log.d("Stream2Android", "adding object.item.videoItem '" + currentItem.getTitle() + "' rescount=" + currentItem.getResList().size());
					VideoItemList.put(currentItem.getTitle(), currentItem);
				}
				else if (currentItem.getUpnp_class().equalsIgnoreCase("object.item.audioItem"))
				{
					Log.d("Stream2Android", "adding object.item.audioItem '" + currentItem.getTitle() + "' rescount=" + currentItem.getResList().size());
					AudioItemList.put(sTitle, currentItem);
				}				
				else if (currentItem.getUpnp_class().equalsIgnoreCase("object.item.imageItem"))
				{
					Log.d("Stream2Android", "adding object.item.imageItem '" + currentItem.getTitle() + "' rescount=" + currentItem.getResList().size());
					ImageItemList.put(sTitle, currentItem);
				}				
            }  
            currentElement = false;
            currentValue = "";
        } 
    }
    public class MediaItem
    {
    	private String title = "";
    	private String upnp_class = "";
    	private LinkedList<SortedMap<String, String>> resList = new LinkedList<SortedMap<String, String>>();
    	private LinkedList<SortedMap<String, String>> albumartList = new LinkedList<SortedMap<String, String>>();

    	private byte[] image;
		private String url = "";

		public LinkedList<SortedMap<String, String>> getAlbumartList() {
			// TODO Auto-generated method stub
			return albumartList;
		}

    	/**
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * @param title the title to set
		 */
		public void setTitle(String title) {
			this.title = title;
		}

		/**
		 * @return the upnp_class
		 */
		public String getUpnp_class() {
			return upnp_class;
		}

		/**
		 * @param upnp_class the upnp_class to set
		 */
		public void setUpnp_class(String upnp_class) {
			this.upnp_class = upnp_class;
		}

		/**
		 * @return the resList
		 */
		public LinkedList<SortedMap<String, String>> getResList() {
			return resList;
		}

		public void setImage(byte[] image) {
			this.image = image;
		}

		public byte[] getImage() {
			return image;
		}

		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @param url the url to set
		 */
		public void setUrl(String url) {
			this.url = url;
		}
    }
	@Override
	public void onCompletion(MediaPlayer arg0) {
		// TODO Auto-generated method stub
		upnpDev.startMillis = 0;
		upnpDev.setStateVariable("urn:upnp-org:serviceId:AVTransport", "TransportState", "STOPPED");
		mListViewMain.setVisibility(View.VISIBLE);
		mClearList.setVisibility(View.VISIBLE);
	}
	@Override
	public void clearMetaKeyState(View arg0, Editable arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public int getInputType() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public boolean onKeyDown(View arg0, Editable arg1, int arg2, KeyEvent arg3) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onKeyOther(View arg0, Editable arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onKeyUp(View arg0, Editable arg1, int arg2, KeyEvent arg3) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		// clear video and update listview
		mVideoItemList.clear();
		upnpDev.handlerUI.sendEmptyMessage(2);
	}
	public void testMediaMetadataRetriever()
	{
/*		android.media.MediaMetadataRetriever  m = new android.media.MediaMetadataRetriever();

		//Get the methods
        Method[] methods = m.getClass().getDeclaredMethods();

        //Loop through the methods and print out their names
        for (Method method : methods) {
        	if (method.getName() == "captureFrame")
        		pre2_3 = true;
            Log.d("Stream2Android", "method=" + method.getName() + ", " + method.toGenericString());
        }
        
        File file = new File("/sdcard/Movies", "Mario.mp4");
        if (file.exists())
        {
            Log.d("Stream2Android", "Mario.mp4 exists=" + file.length());
	        // since 2.2
			Bitmap lm = ThumbnailUtils.createVideoThumbnail("/sdcard/Movies/Mario.mp4", android.provider.MediaStore.Video.Thumbnails.MICRO_KIND);
	
			if (lm != null)
			{
				MediaItem mi = new MediaItem();
				mi.title = "Test";
	
				java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
				lm.compress(Bitmap.CompressFormat.JPEG, 90, os);
				mi.image = os.toByteArray();
				
				SortedMap<String, String> sm = new TreeMap<String, String>();
				sm.put("duration", "0:30:00.000");
				sm.put("res", "video");
				
				mi.getResList().add(sm);
				
				mVideoItemList.put(mi.title, mi);
				upnpDev.handlerUI.sendEmptyMessage(2);
				return;
			}
			m.setDataSource("/sdcard/Movies/Mario.mp4");
	
			//m.setTime(21410); // SGT specific
			
			Bitmap bm = m.captureFrame(); // pre 2.3
			if (bm != null)
			{
				MediaItem mi = new MediaItem();
				mi.title = "Test";
	
				java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
				bm.compress(Bitmap.CompressFormat.JPEG, 90, os);
				mi.image = os.toByteArray();
				
				SortedMap<String, String> sm = new TreeMap<String, String>();
				sm.put("duration", "0:30:00.000");
				sm.put("res", "video");
				
				mi.getResList().add(sm);
				
				mVideoItemList.put(mi.title, mi);
				upnpDev.handlerUI.sendEmptyMessage(2);
			}
        }
        */
	}
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		upnpDev.startMillis = 0;
		upnpDev.setStateVariable("urn:upnp-org:serviceId:AVTransport", "TransportState", "STOPPED");
		mListViewMain.setVisibility(View.VISIBLE);
		mClearList.setVisibility(View.VISIBLE);

		return true;
	}
	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		mListViewMain.setVisibility(View.GONE);
		mClearList.setVisibility(View.GONE);
	}
}