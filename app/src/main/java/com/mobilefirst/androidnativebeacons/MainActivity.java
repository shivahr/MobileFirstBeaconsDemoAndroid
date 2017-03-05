package com.mobilefirst.androidnativebeacons;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.worklight.wlclient.api.WLBeacon;
import com.worklight.wlclient.api.WLBeaconTrigger;
import com.worklight.wlclient.api.WLBeaconTriggerAssociation;
import com.worklight.wlclient.api.WLBeaconsAndTriggersJSONStoreManager;
import com.worklight.wlclient.api.WLBeaconsMonitoringApplication;
import com.worklight.wlclient.api.WLBeaconsMonitoringApplication.WLAlertHandler;
import com.worklight.wlclient.api.WLBeaconsMonitoringApplication.WLRangingStatusHandler;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import org.altbeacon.beacon.Beacon;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	private TextView textView = null;

	private WLBeaconsMonitoringApplication wlBeaconsApplication;

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		textView = (TextView) findViewById(R.id.textView);

		WLClient.createInstance(this);

		wlBeaconsApplication = ((WLBeaconsMonitoringApplication) this.getApplication());
		wlBeaconsApplication.setAlertHandler(new WLAlertHandler(this));
		wlBeaconsApplication.setRangingStatusHandler(new RangingStatusHandler()); // optional
	}

	private WLBeaconsAndTriggersJSONStoreManager getJSONStoreManager() {
		return WLBeaconsAndTriggersJSONStoreManager.getInstance(getApplicationContext());
	}

	public void loadBeaconsAndTriggers(View v) {
		updateTextView("Loading beacons and triggers from server...");
		String adapterName = "MobileFirstBeaconsAdapter";
		String procedureName = "getBeaconsTriggersAndAssociations";
		getJSONStoreManager().loadBeaconsAndTriggers(adapterName, procedureName, new WLResponseListener() {
			@Override
			public void onSuccess(WLResponse arg0) {
				showBeaconsAndTriggers(null);
			}

			@Override
			public void onFailure(WLFailResponse response) {
				String responseText = "WLBeaconsAndTriggersJSONStoreManager.loadBeaconsAndTriggers() failed:\n"
						+ response.toString();
				Log.d(TAG, responseText);
				updateTextView(responseText);
			}
		});
	}

	public void showBeaconsAndTriggers(View v) {
		StringBuilder stringBuilder = new StringBuilder("Beacons:\n");
		List<WLBeacon> beacons = getJSONStoreManager().getBeaconsFromJsonStore();
		for (int i = 0; i < beacons.size(); i++) {
			WLBeacon beacon = beacons.get(i);
			stringBuilder.append((i + 1) + ") " + beacon.toString() + "\n\n");
		}
		stringBuilder.append("\nBeaconTriggers:\n");
		List<WLBeaconTrigger> beaconTriggers = getJSONStoreManager().getBeaconTriggersFromJsonStore();
		for (int i = 0; i < beaconTriggers.size(); i++) {
			WLBeaconTrigger beaconTrigger = beaconTriggers.get(i);
			stringBuilder.append((i + 1) + ") " + beaconTrigger.toString() + "\n\n");
		}
		stringBuilder.append("\nBeaconTriggerAssociations:\n");
		List<WLBeaconTriggerAssociation> beaconTriggerAssociations = getJSONStoreManager()
				.getBeaconTriggerAssociationsFromJsonStore();
		for (int i = 0; i < beaconTriggerAssociations.size(); i++) {
			WLBeaconTriggerAssociation beaconTriggerAssociation = beaconTriggerAssociations.get(i);
			stringBuilder.append((i + 1) + ") " + beaconTriggerAssociation.toString() + "\n\n");
		}
		updateTextView(stringBuilder.toString());
	}

	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

	private void coarseLocationCheck() {
		if (Build.VERSION.SDK_INT >= 23) {
			// Android M Permission check
			if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("This app needs location access");
				builder.setMessage("Please grant location access so this app can detect beacons in the background.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@TargetApi(23)
					@Override
					public void onDismiss(DialogInterface dialog) {
						requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
								PERMISSION_REQUEST_COARSE_LOCATION);
					}

				});
				builder.show();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_COARSE_LOCATION: {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG, "coarse location permission granted");
				} else {
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle("Functionality limited");
					builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
						}
					});
					builder.show();
				}
				return;
			}
		}
	}

	public void startMonitoring(View v) {
		coarseLocationCheck();
		updateTextView("Starting monitoring...");
		try {
			wlBeaconsApplication.startMonitoring();
			updateTextView("Beacon monitoring started.");
		} catch (RemoteException e) {
			updateTextView(e.toString());
		}
	}

	public void stopMonitoring(View v) {
		updateTextView("Stopping monitoring...");
		try {
			wlBeaconsApplication.stopMonitoring();
			// Optionally reset monitoring/ranging state
			wlBeaconsApplication.resetMonitoringRangingState();
			updateTextView("Beacon monitoring stopped.");
		} catch (RemoteException e) {
			updateTextView(e.toString());
		}
	}

	private void updateTextView(final String str) {
		runOnUiThread(new Runnable() {
			public void run() {
				String timeStamp = simpleDateFormat.format(new Date());
				textView.setText(timeStamp + "\n" + str);
			}
		});
	}

	private class RangingStatusHandler implements WLRangingStatusHandler {
		@Override
		public void notifyRangedBeacons(Collection<Beacon> beacons) {
			StringBuilder beaconDetails = new StringBuilder();
			for (Beacon beacon : beacons) {
				beaconDetails.append("Beacon " + beacon.toString() + " is about " + beacon.getDistance()
						+ " meters away, with Rssi: " + beacon.getRssi() + "\n");
			}
			updateTextView(beaconDetails.toString());
		}
	}
}
