package ro.pub.acs.wifiscanner;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    WiFiReceiver mReceiver;
    public static final String TAG = "WiFiScanner";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mReceiver = new WiFiReceiver(this);

        initButton();
    }

    private void initButton() {
        setScanning(false);

        Button button = findViewById(R.id.button);
        button.setText(isScanning() ? "Stop scan" : "Start scan");

        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning()) {
                    Log.v(TAG, "Stop scanning");
                    mReceiver.stopReceiver();
                    setScanning(false);
                    ((Button) v).setText("Start scan");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                1001);
                    } else {
                        Log.v(TAG, "Start scanning");
                        mReceiver.startReceiver();
                        setScanning(true);
                        ((Button) v).setText("Stop scan");
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == 1001 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Start scanning");
            mReceiver.startReceiver();
            setScanning(true);

            Button button = findViewById(R.id.button);
            button.setText("Stop scan");
        }
    }

    private boolean isScanning() {
        SharedPreferences prefs = this.getSharedPreferences("scan_status", Context.MODE_PRIVATE);
        return prefs.getBoolean("scanning", false);
    }

    private void setScanning(boolean status) {
        SharedPreferences prefs = this.getSharedPreferences("scan_status", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("scanning", status);
        editor.commit();
    }
}
