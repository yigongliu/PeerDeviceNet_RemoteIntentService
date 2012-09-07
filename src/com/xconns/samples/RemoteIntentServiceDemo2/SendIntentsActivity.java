package com.xconns.samples.RemoteIntentServiceDemo2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.xconns.peerdevicenet.DeviceInfo;
import com.xconns.peerdevicenet.IRemoteIntentHandler;
import com.xconns.peerdevicenet.IRemoteIntentService;

public class SendIntentsActivity extends Activity {
	final static String TAG = "SendIntentsActivity";
	
	IRemoteIntentService mRmtIntentService = null;

	final static int PICK_VIDEO_REQUEST = 101;
	final static int PICK_IMAGE_REQUEST = 102;

	private String intent_action = null;
	private Spinner action_spin = null;
	private Spinner mime_spin = null;
	private String contentType = "Image";
	private String mime_type = null;
	private EditText url = null;
	private Button sendButton = null;
	private Button pickButton = null;
	private EditText peerName = null;
	private EditText peerAddr = null;
	private EditText peerPort = null;
	private EditText status = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//check for remote custom intents
		Intent i = getIntent();
		String actionStr = i.getAction();
		Uri uri = i.getData();
		if (actionStr.equals("com.xconns.samples.ACTION_REMOTE_CALL")) {
			Log.d(TAG,
					"recv remote intent1: " + actionStr + ", uri: "
							+ uri.toString());
			Intent i2 = new Intent(android.content.Intent.ACTION_CALL);
			i2.setData(uri);
			try {
				startActivity(i2);
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			finish();
		} else if (actionStr.equals("com.xconns.samples.ACTION_REMOTE_DIAL")) {
			Log.d(TAG,
					"recv remote intent2: " + actionStr + ", uri: "
							+ uri.toString());
			Intent i2 = new Intent(android.content.Intent.ACTION_DIAL);
			i2.setData(uri);
			try {
				startActivity(i2);
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			finish();
		}
		
		//bind to remote intent service
		Intent intent = new Intent("com.xconns.peerdevicenet.RemoteIntentService");
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		//build gui components
		action_spin = (Spinner) findViewById(R.id.action_spinner);
		ArrayAdapter<CharSequence> action_adapter = ArrayAdapter
				.createFromResource(this, R.array.intent_actions,
						android.R.layout.simple_spinner_item);
		action_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		action_spin.setAdapter(action_adapter);
		action_spin.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				String action = parent.getItemAtPosition(pos).toString();
				Toast.makeText(parent.getContext(),
						"intent action: " + intent_action, Toast.LENGTH_SHORT)
						.show();
				if (action.equals("ACTION_VIEW")) {
					intent_action = android.content.Intent.ACTION_VIEW;
					url.setText("http://www.google.com");
					mime_type = null;
				} else if (action.equals("ACTION_CALL")) {
					intent_action = android.content.Intent.ACTION_CALL;
					mime_type = null;
					url.setText("tel:2125551212");
				} else if (action.equals("ACTION_DIAL")) {
					intent_action = android.content.Intent.ACTION_DIAL;
					mime_type = null;
					url.setText("tel:2125551212");
				} else if (action.equals("ACTION_EDIT")) {
					intent_action = android.content.Intent.ACTION_EDIT;
					mime_type = null;
					url.setText("http://www.google.com");
				}
			}

			public void onNothingSelected(AdapterView parent) {
				intent_action = null;
			}
		});

		mime_spin = (Spinner) findViewById(R.id.mime_spinner);
		ArrayAdapter<CharSequence> mime_adapter = ArrayAdapter
				.createFromResource(this, R.array.mime_types,
						android.R.layout.simple_spinner_item);
		mime_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mime_spin.setAdapter(mime_adapter);
		mime_spin.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				contentType = parent.getItemAtPosition(pos).toString();
				Toast.makeText(parent.getContext(),
						"contentType: " + contentType, Toast.LENGTH_SHORT)
						.show();
			}

			public void onNothingSelected(AdapterView parent) {
				contentType = null;
			}
		});

		pickButton = (Button) findViewById(R.id.button_pick);
		pickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (contentType == null)
					return;
				if (contentType.equals("Video")) {
					Intent intent = new Intent(
							Intent.ACTION_PICK,
							android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(intent, PICK_VIDEO_REQUEST);
				} else if (contentType.equals("Image")) {
					Intent intent = new Intent(
							Intent.ACTION_PICK,
							android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(intent, PICK_IMAGE_REQUEST);
				}
			}
		});

		url = (EditText) findViewById(R.id.intent_url);
		url.setText("http://www.google.com");
		
		peerName = (EditText) findViewById(R.id.peer_name);
		peerAddr = (EditText) findViewById(R.id.peer_addr);
		peerPort = (EditText) findViewById(R.id.peer_port);
		String addr = getLocalWifiIp(this);
		peerAddr.setText(addr);
		peerPort.setText("8913");
		
		status = (EditText) findViewById(R.id.status);

		sendButton = (Button) findViewById(R.id.button_send);
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (intent_action == null || intent_action.length() == 0) {
					Toast.makeText(SendIntentsActivity.this,
							"please set intent action", Toast.LENGTH_SHORT)
							.show();
					return;
				}
				Log.d(TAG, "send button clicked");
				Intent intent = new Intent();

				if (intent_action.equals(android.content.Intent.ACTION_CALL)) {
					intent_action = "com.xconns.samples.ACTION_REMOTE_CALL";
				}
				if (intent_action.equals(android.content.Intent.ACTION_DIAL)) {
					intent_action = "com.xconns.samples.ACTION_REMOTE_DIAL";
				}
				intent.setAction(intent_action);

				String u = url.getText().toString();
				Log.d(TAG, "retrv uri=" + u);
				if (u != null && u.length() > 0) {
					Log.d(TAG, "parse uri=" + u);
					Uri uri = Uri.parse(u);
					Log.d(TAG, "parsed uri=" + uri);
					if (mime_type != null && mime_type.length() > 0) {
						intent.setDataAndType(uri, mime_type);
					} else {
						intent.setData(uri);
					}
				}
				
				String name = peerName.getText().toString();
				String addr = peerAddr.getText().toString();
				String port = peerPort.getText().toString();
				if (name == null || name.length() == 0 ||
						addr == null || addr.length() == 0 ||
						port == null || port.length() == 0) {
					Toast.makeText(SendIntentsActivity.this,
							"miss info for peer device",
							Toast.LENGTH_LONG).show();
					return;
				}
				DeviceInfo dev = new DeviceInfo(name, addr, port);

				try {
					intent.setPackage("com.xconns.samples.RemoteIntentServiceDemo2");
					mRmtIntentService.startRemoteActivity(dev, intent, mHandler);
				} catch (Exception e) {
					Toast.makeText(SendIntentsActivity.this,
							"startRemoteActivity failed: " + e.getMessage(),
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected void onNewIntent(Intent i) {
		// TODO Auto-generated method stub
		super.onNewIntent(i);
		String actionStr = i.getAction();
		Uri uri = i.getData();
		if (actionStr.equals("com.xconns.samples.ACTION_REMOTE_CALL")) {
			Log.d(TAG,
					"recv remote intent3: " + actionStr + ", uri: "
							+ uri.toString());
			Intent i2 = new Intent(android.content.Intent.ACTION_CALL);
			i2.setData(uri);
			try {
				startActivity(i2);
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}

		} else if (actionStr.equals("com.xconns.samples.ACTION_REMOTE_DIAL")) {
			Log.d(TAG,
					"recv remote intent4: " + actionStr + ", uri: "
							+ uri.toString());
			Intent i2 = new Intent(android.content.Intent.ACTION_DIAL);
			i2.setData(uri);
			try {
				startActivity(i2);
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// If the request went well (OK) and the request was
		// PICK_VIDEO_REQUEST
		Uri uri = null;
		if (resultCode == Activity.RESULT_OK) {
			uri = data.getData();
			url.setText(uri.toString());
			mime_type = data.getType();
			if (mime_type == null || mime_type.length() == 0) {
				if (requestCode == PICK_VIDEO_REQUEST) {
					mime_type = "video/*";
				} else if (requestCode == PICK_IMAGE_REQUEST) {
					mime_type = "image/*";
				}
			} else {
				Log.d(TAG, "picker returns type: " + mime_type);
			}
		}
	}
	
	@Override
	public void onDestroy() {
		// leave group
		if (mRmtIntentService != null) {
			// Detach our existing connection.
			unbindService(mConnection);
		}
		super.onDestroy();
	}
	

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mRmtIntentService = IRemoteIntentService.Stub.asInterface(service);
			Log.d(TAG, "SendIntentsActivity bind to RemoteIntentService");
		}

		public void onServiceDisconnected(ComponentName className) {
			mRmtIntentService = null;
		}
	};
	
	Handler mGuiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String m = (String) msg.obj;
			status.setText(m);
		}
	};
	
	void sendMsgToGui(String info) {
		Message m = mGuiHandler.obtainMessage();
		m.obj = info;
		mGuiHandler.sendMessage(m);
	}

	private IRemoteIntentHandler mHandler = new IRemoteIntentHandler.Stub() {

		@Override
		public void onError(String errInfo) throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onError");
			sendMsgToGui(errInfo);
		}

		@Override
		public void onResult(Intent result) throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onResult");
			sendMsgToGui("remote intent get result");
		}

		@Override
		public void onStart(int sessionId) throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onStart");
			sendMsgToGui("remote intent start");
		}

		@Override
		public void onProgress(int percent) throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onProgress");
			sendMsgToGui("remote intent progress: "+percent+"/100");
		}

		@Override
		public void onComplete() throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onComplete");
			sendMsgToGui("remote intent complete");
		}

		@Override
		public void onRejected() throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onRejected");
			sendMsgToGui("remote intent rejected");
		}

		@Override
		public void onCanceled() throws RemoteException {
			Log.d(TAG, "IRemoteIntentHandler onCanceled");
			sendMsgToGui("remote intent canceled");
		}
	};
	
	
	static String getLocalWifiIp(Context ctx) {
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiInfo != null && wifiInfo.isConnected()) {
			WifiManager mWifiManager = (WifiManager) ctx
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo winfo = mWifiManager.getConnectionInfo();
			if (winfo != null) {
				int myIp = winfo.getIpAddress();
				return getIpString(myIp);
			}
		}
		return null;
	}
	
	static String getIpString(int ip) {
		StringBuilder str = new StringBuilder();
		str.append(ip & 0XFF).append(".")
			.append(((ip >> 8) & 0xFF)).append(".")
			.append(((ip >> 16) & 0xFF)).append(".")
			.append(((ip >> 24) & 0xFF));
		Log.d(TAG, "got ip: " + str.toString());
		return str.toString();
	}
}