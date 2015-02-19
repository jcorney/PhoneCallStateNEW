package com.boomgaarden_corney.android.phonecallstate;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class PhoneCallStateMainActivity extends Activity {

	private static String incomingNumber;
	private static int counterCall = 0;
	private static int counterIdle = 0;
	private static int counterOffHook = 0;
	private int simCardState;
	private int phoneType;

	private final static String DEBUG_TAG = "DEBUG_PHONECALLSTATE";
	private final static String SERVER_URL = "http://54.86.68.241/phonecallstate/test.php";

	private static TextView txtResults;

	private static String errorMsg;
	private String simCardStateStr;
	private String phoneTypeStr;
	private String countryANDNetworkCodeStr;
	private String subscriberIDStr;
	private String voiceMailStr;
	private String deviceIDStr;

	TelephonyManager mTelephonyManager;

	private static List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private static List<NameValuePair> paramsPhoneCallState = new ArrayList<NameValuePair>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phone_call_state_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);	
		mTelephonyManager =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

		setDeviceData();
		showDeviceData();
		sendDeviceData();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.phone_call_state_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private static String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("PHONECALLSTATE")) {
			writer.write(buildPostRequest(paramsPhoneCallState));
			paramsPhoneCallState = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();

		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Telephony
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		GetSimState();
		GetPhoneType();
		GetMccMnc();	
		GetSubscriberId();
		GetVoiceMailNumber();
		GetDevicID();

		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
		paramsDevice.add(new BasicNameValuePair(simCardStateStr, " "));
		paramsDevice.add(new BasicNameValuePair(phoneTypeStr, " "));
		paramsDevice.add(new BasicNameValuePair(countryANDNetworkCodeStr, " "));
		paramsDevice.add(new BasicNameValuePair(countryANDNetworkCodeStr, " "));
		paramsDevice.add(new BasicNameValuePair(voiceMailStr, " "));
		paramsDevice.add(new BasicNameValuePair(deviceIDStr, " "));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");
		txtResults.append(simCardStateStr + "\n");
		txtResults.append(phoneTypeStr + "\n");
		txtResults.append("MCC and MNC: " + countryANDNetworkCodeStr + "\n");
		txtResults.append("Mobile Subscriber ID: " + subscriberIDStr + "\n");
		txtResults.append("Voice Mail Number: " + voiceMailStr + "\n");
		txtResults.append("Device ID: " + deviceIDStr);
		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Telephony info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	public static class PhoneCallStateBroadcastReceiver extends
			BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
					TelephonyManager.EXTRA_STATE_RINGING)) {
				incomingNumber = intent
						.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
				counterCall = counterCall + 1;
				txtResults.append("Incoming Call From: " + incomingNumber
						+ "\nTotal Calls: " + counterCall + "\n");
				paramsPhoneCallState
						.add(new BasicNameValuePair("Incoming Call From: ",
								String.valueOf(incomingNumber)));
				paramsPhoneCallState.add(new BasicNameValuePair(
						"Total Incoming Calls:", String.valueOf(counterCall)));
				sendPhoneCallStateData(context);
			} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
					.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
				counterIdle = counterIdle + 1;
				txtResults.append("Total Idle: " + counterIdle + "\n");
				paramsPhoneCallState.add(new BasicNameValuePair(
						"Total Idle Events:", String.valueOf(counterIdle)));
				sendPhoneCallStateData(context);
			} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
					.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
				counterOffHook = counterOffHook + 1;
				txtResults.append("Total Off Hook: " + counterOffHook + "\n");
				paramsPhoneCallState.add(new BasicNameValuePair(
						"Total Off Hook Events:", String
								.valueOf(counterOffHook)));
				sendPhoneCallStateData(context);
			}

		}

		private void sendPhoneCallStateData(Context context) {
			ConnectivityManager connectMgr = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

			// Verify network connectivity is working; if not add note to
			// TextView
			// and Logcat file
			if (networkInfo != null && networkInfo.isConnected()) {
				// Send HTTP POST request to server which will include POST
				// parameters with Telephony info
				new SendHttpRequestTask().execute(SERVER_URL, "PHONECALLSTATE");
			} else {
				setErrorMsg("No Network Connectivity");
				showErrorMsg();
			}
		}

		private void setErrorMsg(String error) {
			errorMsg = error;
			paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
		}

		private void showErrorMsg() {
			Log.d(DEBUG_TAG, errorMsg);
			txtResults.append(errorMsg + "\n");
		}

		private String sendHttpRequest(String myURL, String postParameters)
				throws IOException {

			URL url = new URL(myURL);

			// Setup Connection
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000); /* in milliseconds */
			conn.setConnectTimeout(15000); /* in milliseconds */
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			// Setup POST query params and write to stream
			OutputStream ostream = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					ostream, "UTF-8"));

			if (postParameters.equals("DEVICE")) {
				writer.write(buildPostRequest(paramsDevice));
			} else if (postParameters.equals("PHONECALLSTATE")) {
				writer.write(buildPostRequest(paramsPhoneCallState));
				paramsPhoneCallState = new ArrayList<NameValuePair>();
			} else if (postParameters.equals("ERROR_MSG")) {
				writer.write(buildPostRequest(paramsErrorMsg));
				paramsErrorMsg = new ArrayList<NameValuePair>();
			}

			writer.flush();
			writer.close();
			ostream.close();

			// Connect and Log response
			conn.connect();
			int response = conn.getResponseCode();
			Log.d(DEBUG_TAG, "The response is: " + response);

			conn.disconnect();

			return String.valueOf(response);

		}

		private class SendHttpRequestTask extends
				AsyncTask<String, Void, String> {

			// @params come from SendHttpRequestTask.execute() call
			@Override
			protected String doInBackground(String... params) {
				// params comes from the execute() call: params[0] is the url,
				// params[1] is type POST
				// request to send - i.e. whether to send Device or
				// Telephony
				// parameters.
				try {
					return sendHttpRequest(params[0], params[1]);
				} catch (IOException e) {
					setErrorMsg("Unable to retrieve web page. URL may be invalid.");
					showErrorMsg();
					return errorMsg;
				}
			}
		}
		
		
	}
	private void GetSimState() {
		simCardState = mTelephonyManager.getSimState();
		switch (simCardState) {
		case 0:
			simCardStateStr = ("SIM Card State: "
					+ String.valueOf(simCardState) + " Unknown. Signifies that the SIM is in transition between states.");
			break;
			
		case 1:
			simCardStateStr = ("SIM Card State: "
					+ String.valueOf(simCardState) + " No SIM card is available in the device");
			break;
			
		case 2:
			simCardStateStr = ("SIM Card State: "
					+ String.valueOf(simCardState) + " Locked: requires the user's SIM PIN to unlock");
			break;
			
		case 3:
			simCardStateStr = ("SIM Card State: "
					+ String.valueOf(simCardState) + " Locked: requires the user's SIM PUK to unlock");
			break;

		case 4:
			simCardStateStr = ("SIM Card State: "
					+ String.valueOf(simCardState) + " Locked: requries a network PIN to unlock");
			break;
			
		case 5:
			simCardStateStr = ("SIM Card State: "
					+ String.valueOf(simCardState) + "  Ready");
			break;

		default:
			break;
		}
	}
	
	private void GetPhoneType() {
		phoneType = mTelephonyManager.getPhoneType();
		switch (phoneType) {
		case 0:
			phoneTypeStr = ("Phone Type: "
					+ String.valueOf(phoneType) + " No phone radio.");
			break;
			
		case 1:
			phoneTypeStr = ("Phone Type: "
					+ String.valueOf(phoneType) + " Phone radio is GSM.");
			break;
			
		case 2:
			phoneTypeStr = ("Phone Type: "
					+ String.valueOf(phoneType) + " Phone radio is CDMA.");
			break;
			
		case 3:
			phoneTypeStr = ("Phone Type: "
					+ String.valueOf(phoneType) + " Phone is via SIP.");
			break;

		default:
			break;
		}
	}
	
	private void GetMccMnc() {
		countryANDNetworkCodeStr = mTelephonyManager.getNetworkOperator();
		
	}
	
	private void GetSubscriberId() {
		subscriberIDStr = mTelephonyManager.getSubscriberId();
		
	}
	
	private void GetVoiceMailNumber() {
		voiceMailStr = mTelephonyManager.getVoiceMailNumber();
		
	}
	
	private void GetDevicID() {
		deviceIDStr = mTelephonyManager.getDeviceId();
		
	}
	
	

}