package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.example.myapplication.DetectedActivitiesIntentReceiver;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.material.tabs.TabLayout;

import java.lang.reflect.InvocationTargetException;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_REQUEST_ID = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String TAG = "MainActivity";
    private MeasurementProvider mMeasurementProvider;
    private RealTimePositionVelocityCalculator mRealTimePositionVelocityCalculator;



    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        setupFragments();
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
            setupFragments();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
        }
    }

    private void setupFragments() {
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


}