package ro.pub.acs.wifiscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class WiFiReceiver extends BroadcastReceiver implements Alarm.Callback {

    protected final static long POLLING_INTERVAL = 10 * 1000 * 60; // every 10 minutes
    private WifiManager mWifiManager;
    private Alarm mAlarm;
    private boolean mRegistered = false;
    private Context mContext;
    private String mLocationProvider = LocationManager.NETWORK_PROVIDER;
    private LocationManager mLocationManager;

    public WiFiReceiver(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mAlarm = new Alarm("WiFiReceiver", this);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void startReceiver() {
        Log.v(MainActivity.TAG, "Setting alarm");
        mAlarm.set(POLLING_INTERVAL, true);
    }

    public void stopReceiver() {
        mAlarm.cancel();
        synchronized (this) {
            if (mRegistered) {
                mContext.getApplicationContext().unregisterReceiver(this);
                mRegistered = false;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(MainActivity.TAG, "Got scan results " + mWifiManager.getScanResults().size());

        Location lastKnownLocation = null;

        try {
            lastKnownLocation = mLocationManager.getLastKnownLocation(mLocationProvider);
        } catch (SecurityException e) {
            Log.e(MainActivity.TAG, "Could not get last known location");
        }

        String buf = "";

        for (ScanResult scanResult : mWifiManager.getScanResults()) {
            boolean connectAbility = WifiSecurity.getScanResultSecurity(scanResult) == WifiSecurity.OPEN;
            if (!connectAbility) {
                List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();

                if (networks != null) {
                    for (WifiConfiguration config : networks) {
                        if (config.SSID.equals("\"" + scanResult.SSID + "\"")) {
                            connectAbility = true;
                            break;
                        }
                    }
                }
            }
            Log.v(MainActivity.TAG, lastKnownLocation.toString() + " " + scanResult.SSID + " " + scanResult.BSSID + " "
                    + scanResult.level + " " + scanResult.capabilities + " " + connectAbility);

            buf += "" + System.currentTimeMillis() + "," + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude() +
                    "," + scanResult.SSID + "," + scanResult.BSSID + "," + scanResult.level + "," + scanResult.capabilities + "," +
                    connectAbility + System.lineSeparator();
        }

        synchronized (this) {
            if (mRegistered)
                context.unregisterReceiver(this);
            mRegistered = false;
        }

        if (isExternalStorageWritable()) {
            try {
                File f = getPrivateAlbumStorageDir(mContext, "logs");
                BufferedWriter output = new BufferedWriter(new FileWriter(f.getAbsolutePath() + "//file.txt", true));
                output.write(buf);
                output.close();
            } catch (Exception e) {
                Log.v(MainActivity.TAG, "Could not write " + e.getMessage());
            }
        }
    }

    public File getPrivateAlbumStorageDir(Context context, String albumName) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), albumName);
        file.mkdirs();
        return file;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public void onTriggered() {
        Log.v(MainActivity.TAG, "Alarm triggered");

        // Register for Wifi scan results
        synchronized (this) {
            Log.v(MainActivity.TAG, "Registering receiver");
            mContext.getApplicationContext().registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mRegistered = true;
        }

        // Start the scan
        mWifiManager.startScan();
        Log.v(MainActivity.TAG, "Started scanning");
    }

    private enum WifiSecurity {
        WEP, PSK, EAP, OPEN;

        public static WifiSecurity getScanResultSecurity(ScanResult scanResult) {
            final String cap = scanResult.capabilities;
            WifiSecurity[] values = WifiSecurity.values();
            for (int i = values.length - 1; i >= 0; i--) {
                if (cap.contains(values[i].name())) {
                    return values[i];
                }
            }
            return OPEN;
        }
    }
}
