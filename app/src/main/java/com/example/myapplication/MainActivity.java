package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
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

    TextView show_log;
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
        show_log = (TextView) findViewById(R.id.text_view);
        TableLayout tableLayout = findViewById(R.id.table_view);
        requestPermission(this);
        populateTable(tableLayout);
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
        mMeasurementProvider.registerGnssStatus();
        mMeasurementProvider.registerNmea();


        java.lang.reflect.Method method;
        LocationManager locationManager = mMeasurementProvider.getLocationManager();

        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
        } catch (NoSuchMethodException e) {

        }
    }

    public void logData(String message) {
        show_log.setText(message);
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

    private void populateTable(TableLayout tableLayout) {
        // Add header row
        TableRow headerRow = new TableRow(this);
        TextView header1 = new TextView(this);
        TextView header2 = new TextView(this);
        header1.setText("Satellite");
        header2.setText("Status");
        header1.setPadding(16, 0, 70, 0);
        header2.setPadding(16, 0, 70, 0);
        header1.setTextSize(16);
        header2.setTextSize(16);
        headerRow.addView(header1);
        headerRow.addView(header2);
        tableLayout.addView(headerRow);

        // Add data rows
        for (int i = 1; i <= 32; i++) {
            TableRow tableRow = new TableRow(this);

            TextView satelliteView = new TextView(this);
            TextView statusView = new TextView(this);

            satelliteView.setText(String.valueOf(i));
            statusView.setText("Unknown");  // Replace with actual status if available

            satelliteView.setPadding(16, 0, 70, 0);
            statusView.setPadding(16, 0, 70, 0);
            satelliteView.setTextSize(14);
            statusView.setTextSize(14);

            tableRow.addView(satelliteView);
            tableRow.addView(statusView);

            tableLayout.addView(tableRow);
        }
    }
}