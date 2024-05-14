package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST_ID = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String TAG = "MainActivity";
    private MeasurementProvider mMeasurementProvider;
    private RealTimePositionVelocityCalculator mRealTimePositionVelocityCalculator;
    TextView showPosition;
    TextView showStatus;
    private Timer watchdogTimer;
    private byte keepAliveClock = '0';

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        registerGnssMessages();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMeasurementProvider != null) {
            mMeasurementProvider.unregisterAll();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showPosition = (TextView) findViewById(R.id.text_view);
        showStatus = (TextView) findViewById(R.id.text_view2);
        requestPermission(this);
    }

    private boolean hasPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Permissions granted at install time.
            return true;
        }
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermission(final Activity activity) {
        if (hasPermissions(activity)) {
            registerGnssMessages();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
        }
    }

    private void registerGnssMessages() {
        mRealTimePositionVelocityCalculator = new RealTimePositionVelocityCalculator();
        mRealTimePositionVelocityCalculator.setMainActivity(this);
        mRealTimePositionVelocityCalculator.setResidualPlotMode(
                RealTimePositionVelocityCalculator.RESIDUAL_MODE_DISABLED, null /* fixedGroundTruth */);

        mMeasurementProvider =
                new MeasurementProvider(
                        getApplicationContext(),
                        mRealTimePositionVelocityCalculator);

        mMeasurementProvider.registerMeasurements();
        mMeasurementProvider.registerLocation();
        mMeasurementProvider.registerNavigation();
//        mMeasurementProvider.registerGnssStatus();
//        mMeasurementProvider.registerNmea();


        java.lang.reflect.Method method;
        LocationManager locationManager = mMeasurementProvider.getLocationManager();

        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
        } catch (NoSuchMethodException e) {

        }
    }

    public void setShowPosition(String message) {
        showPosition.setText(message);
    }

    private void watchdogTimerCreate() {
        if (watchdogTimer == null) {
            watchdogTimer = new Timer();
            watchdogTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        FileOutputStream bkltStream = new FileOutputStream(new File("/sys/class/gpio/gpio95/value"));
                        bkltStream.write(new byte[]{keepAliveClock});
                        bkltStream.flush();
                        bkltStream.close();
                        keepAliveClock = (keepAliveClock == '0' ? (byte) '1' : (byte) '0');
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 2000);
        }
    }

    public void setShowStatus(String[] stateOfSatellite) {
        StringBuilder status = new StringBuilder();
        for (int i = 0; i<stateOfSatellite.length; i++){
            if (stateOfSatellite[i] != null){
                status.append("satellite:").append(i+1).append(" status: ").append(stateOfSatellite[i]).append("\n");
            }
        }
        showStatus.setText(status);
    }
}