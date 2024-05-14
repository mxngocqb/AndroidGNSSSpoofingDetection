/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myapplication;

import static com.example.myapplication.MeasurementProvider.TAG;

import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

//import com.google.android.apps.location.gps.gnsslogger.ResultFragment.UIResultComponent;
import com.example.pseudorange.GpsMathOperations;
import com.example.pseudorange.GpsNavigationMessageStore;
import com.example.pseudorange.PseudorangePositionVelocityFromRealTimeEvents;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * A class that handles real time position and velocity calculation, passing {@link
 * GnssMeasurementsEvent} instances to the {@link PseudorangePositionVelocityFromRealTimeEvents}
 * whenever a new raw measurement is received in order to compute a new position solution. The
 * computed position and velocity solutions
 */
public class RealTimePositionVelocityCalculator implements MeasurementListener {
    /**
     * Residual analysis where user disabled residual plots
     */
    public static final int RESIDUAL_MODE_DISABLED = -1;

    /**
     * Residual analysis where the user is not moving
     */
    public static final int RESIDUAL_MODE_STILL = 0;

    /**
     * Residual analysis where the user is moving
     */
    public static final int RESIDUAL_MODE_MOVING = 1;

    /**
     * Residual analysis where the user chose to enter a LLA input as their position
     */
    public static final int RESIDUAL_MODE_AT_INPUT_LOCATION = 2;

    private static final long EARTH_RADIUS_METERS = 6371000;
    private PseudorangePositionVelocityFromRealTimeEvents
            mPseudorangePositionVelocityFromRealTimeEvents;
    private HandlerThread mPositionVelocityCalculationHandlerThread;
    private Handler mMyPositionVelocityCalculationHandler;

    private int mCurrentColor = Color.rgb(0x4a, 0x5f, 0x70);
    private int mCurrentColorIndex = 0;
    private boolean mAllowShowingRawResults = false;
    //  private MapFragment mMapFragment;
    private MainActivity mMainActivity;
    //  private PlotFragment mPlotFragment;
    private int[] mRgbColorArray = {
            Color.rgb(0x4a, 0x5f, 0x70),
            Color.rgb(0x7f, 0x82, 0x5f),
            Color.rgb(0xbf, 0x90, 0x76),
            Color.rgb(0x82, 0x4e, 0x4e),
            Color.rgb(0x66, 0x77, 0x7d)
    };
    private int mResidualPlotStatus;
    private double[] mGroundTruth = null;
    private int mPositionSolutionCount = 0;

    private Context context;

    public RealTimePositionVelocityCalculator() {
        mPositionVelocityCalculationHandlerThread =
                new HandlerThread("Position From Realtime Pseudoranges");
        mPositionVelocityCalculationHandlerThread.start();
        mMyPositionVelocityCalculationHandler =
                new Handler(mPositionVelocityCalculationHandlerThread.getLooper());

        final Runnable r =
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mPseudorangePositionVelocityFromRealTimeEvents =
                                    new PseudorangePositionVelocityFromRealTimeEvents();
                        } catch (Exception e) {
                            Log.e(
                                    TAG, " Exception in constructing PseudorangePositionFromRealTimeEvents : ", e);
                        }
                    }
                };
        mMyPositionVelocityCalculationHandler.post(r);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
    }

    /**
     * Update the reference location in {@link PseudorangePositionVelocityFromRealTimeEvents} if the
     * received location is a network location visualize both GPS location computed by the device
     * and the one computed from the raw data.
     */
    @Override
    public void onLocationChanged(final Location location) {
        if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            final Runnable r =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
                                return;
                            }
                            try {
                                mPseudorangePositionVelocityFromRealTimeEvents.setReferencePosition(
                                        (int) (location.getLatitude() * 1E7),
                                        (int) (location.getLongitude() * 1E7),
                                        (int) (location.getAltitude() * 1E7));
                            } catch (Exception e) {
                                Log.e(TAG, " Exception setting reference location : ", e);
                            }
                        }
                    };
            mMyPositionVelocityCalculationHandler.post(r);
        } else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            if (mAllowShowingRawResults) {
                final Runnable r =
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
                                    return;
                                }
                                double[] posSolution =
                                        mPseudorangePositionVelocityFromRealTimeEvents.getPositionSolutionLatLngDeg();
                                double[] velSolution =
                                        mPseudorangePositionVelocityFromRealTimeEvents.getVelocitySolutionEnuMps();
                                double[] pvUncertainty =
                                        mPseudorangePositionVelocityFromRealTimeEvents
                                                .getPositionVelocityUncertaintyEnu();
                                String[] stateOfSatellite =
                                        mPseudorangePositionVelocityFromRealTimeEvents.getStateOfSatellite();

                                if (Double.isNaN(posSolution[0])) {
                                    logPositionFromRawDataEvent("No Position Calculated Yet");
                                    logPositionError("And no offset calculated yet...");
                                } else {
                                    if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
                                            && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
                                        updateGroundTruth(posSolution);
                                    }
                                    String formattedLatDegree = new DecimalFormat("##.##########").format(posSolution[0]);
                                    String formattedLngDegree = new DecimalFormat("##.##########").format(posSolution[1]);
                                    String formattedAltMeters = new DecimalFormat("##.#").format(posSolution[2]);
                                    logPositionFromRawDataEvent(
                                            "latDegrees = "
                                                    + formattedLatDegree
                                                    + " lngDegrees = "
                                                    + formattedLngDegree
                                                    + "altMeters = "
                                                    + formattedAltMeters);

                                    synchronized (mMainActivity) {
                                        mMainActivity.setShowPosition(formattedLatDegree + ", " + formattedLngDegree);
                                    }

                                    synchronized (mMainActivity) {
                                        mMainActivity.setShowStatus(stateOfSatellite);
                                    }

                                    String formattedVelocityEastMps =
                                            new DecimalFormat("##.###").format(velSolution[0]);
                                    String formattedVelocityNorthMps =
                                            new DecimalFormat("##.###").format(velSolution[1]);
                                    String formattedVelocityUpMps =
                                            new DecimalFormat("##.###").format(velSolution[2]);
                                    logVelocityFromRawDataEvent(
                                            "Velocity East = "
                                                    + formattedVelocityEastMps
                                                    + "mps"
                                                    + " Velocity North = "
                                                    + formattedVelocityNorthMps
                                                    + "mps"
                                                    + "Velocity Up = "
                                                    + formattedVelocityUpMps
                                                    + "mps");

                                    String formattedPosUncertaintyEastMeters =
                                            new DecimalFormat("##.###").format(pvUncertainty[0]);
                                    String formattedPosUncertaintyNorthMeters =
                                            new DecimalFormat("##.###").format(pvUncertainty[1]);
                                    String formattedPosUncertaintyUpMeters =
                                            new DecimalFormat("##.###").format(pvUncertainty[2]);
                                    logPositionUncertainty(
                                            "East = "
                                                    + formattedPosUncertaintyEastMeters
                                                    + "m North = "
                                                    + formattedPosUncertaintyNorthMeters
                                                    + "m Up = "
                                                    + formattedPosUncertaintyUpMeters
                                                    + "m");
                                    String formattedVelUncertaintyEastMeters =
                                            new DecimalFormat("##.###").format(pvUncertainty[3]);
                                    String formattedVelUncertaintyNorthMeters =
                                            new DecimalFormat("##.###").format(pvUncertainty[4]);
                                    String formattedVelUncertaintyUpMeters =
                                            new DecimalFormat("##.###").format(pvUncertainty[5]);
                                    logVelocityUncertainty(
                                            "East = "
                                                    + formattedVelUncertaintyEastMeters
                                                    + "mps North = "
                                                    + formattedVelUncertaintyNorthMeters
                                                    + "mps Up = "
                                                    + formattedVelUncertaintyUpMeters
                                                    + "mps");
                                    String formattedOffsetMeters =
                                            new DecimalFormat("##.######")
                                                    .format(
                                                            getDistanceMeters(
                                                                    location.getLatitude(),
                                                                    location.getLongitude(),
                                                                    posSolution[0],
                                                                    posSolution[1]));
                                    logPositionError("position offset = " + formattedOffsetMeters + " meters");
                                    String formattedSpeedOffsetMps =
                                            new DecimalFormat("##.###")
                                                    .format(
                                                            Math.abs(
                                                                    location.getSpeed()
                                                                            - Math.sqrt(
                                                                            Math.pow(velSolution[0], 2)
                                                                                    + Math.pow(velSolution[1], 2))));
                                    logVelocityError("speed offset = " + formattedSpeedOffsetMps + " mps");
                                }
                                logLocationEvent("onLocationChanged: " + location);
                            }
                        };
                mMyPositionVelocityCalculationHandler.post(r);
            }
        }
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mMainActivity = mainActivity;
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onGnssMeasurementsReceived(final GnssMeasurementsEvent event) {
        mAllowShowingRawResults = true;
        final Runnable r =
                new Runnable() {
                    @Override
                    public void run() {
                        if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
                            return;
                        }
                        try {
                            if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
                                    && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
                                mPseudorangePositionVelocityFromRealTimeEvents
                                        .setCorrectedResidualComputationTruthLocationLla(mGroundTruth);
                            }
                            mPseudorangePositionVelocityFromRealTimeEvents
                                    .computePositionVelocitySolutionsFromRawMeas(event);
                            mPseudorangePositionVelocityFromRealTimeEvents.getPseudorangeResidualsMeters();
                            GpsMathOperations.createAndFillArray(GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES, Double.NaN);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
        mMyPositionVelocityCalculationHandler.post(r);
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        /*
        if (event.getType() == GnssNavigationMessage.TYPE_GPS_L1CA) {
            mPseudorangePositionVelocityFromRealTimeEvents.parseHwNavigationMessageUpdates(event);
        }*/
        if (event.getType() == GnssNavigationMessage.TYPE_GPS_L1CA) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    mPseudorangePositionVelocityFromRealTimeEvents.parseHwNavigationMessageUpdates(event);
                }
            };
            long delayMillis = 10000;
            mMyPositionVelocityCalculationHandler.postDelayed(r, delayMillis);
        }
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
    }

    @Override
    public void onNmeaReceived(long l, String s) {
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
    }

    private void logEvent(String tag, String message, int color) {
        String composedTag = TAG + tag;
        Log.d(composedTag, message);
        logText(tag, message, color);
    }

    private void logText(String tag, String text, int color) {
//        mMainActivity.logData(text);
    }

    public void logLocationEvent(String event) {
        mCurrentColor = getNextColor();
        logEvent("Location", event, mCurrentColor);
    }

    private void logPositionFromRawDataEvent(String event) {
        logEvent("Calculated Position From Raw Data", event + "\n", mCurrentColor);
    }

    private void logVelocityFromRawDataEvent(String event) {
        logEvent("Calculated Velocity From Raw Data", event + "\n", mCurrentColor);
    }

    private void logPositionError(String event) {
        logEvent(
                "Offset between the reported position and Google's WLS position based on reported "
                        + "measurements",
                event + "\n",
                mCurrentColor);
    }

    private void logVelocityError(String event) {
        logEvent(
                "Offset between the reported velocity and "
                        + "Google's computed velocity based on reported measurements ",
                event + "\n",
                mCurrentColor);
    }

    private void logPositionUncertainty(String event) {
        logEvent("Uncertainty of the calculated position from Raw Data", event + "\n", mCurrentColor);
    }

    private void logVelocityUncertainty(String event) {
        logEvent("Uncertainty of the calculated velocity from Raw Data", event + "\n", mCurrentColor);
    }

    private synchronized int getNextColor() {
        ++mCurrentColorIndex;
        return mRgbColorArray[mCurrentColorIndex % mRgbColorArray.length];
    }

    /**
     * Return the distance (measured along the surface of the sphere) between 2 points
     */
    public double getDistanceMeters(
            double lat1Degree, double lng1Degree, double lat2Degree, double lng2Degree) {

        double deltaLatRadian = Math.toRadians(lat2Degree - lat1Degree);
        double deltaLngRadian = Math.toRadians(lng2Degree - lng1Degree);

        double a =
                Math.sin(deltaLatRadian / 2) * Math.sin(deltaLatRadian / 2)
                        + Math.cos(Math.toRadians(lat1Degree))
                        * Math.cos(Math.toRadians(lat2Degree))
                        * Math.sin(deltaLngRadian / 2)
                        * Math.sin(deltaLngRadian / 2);
        double angularDistanceRad = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * angularDistanceRad;
    }

    /**
     * Update the ground truth for pseudorange residual analysis based on the user activity.
     */
    private synchronized void updateGroundTruth(double[] posSolution) {

        // In case of switching between modes, last ground truth from previous mode will be used.
        if (mGroundTruth == null) {
            // If mGroundTruth has not been initialized, we set it to be the same as position solution
            mGroundTruth = new double[]{0.0, 0.0, 0.0};
            mGroundTruth[0] = posSolution[0];
            mGroundTruth[1] = posSolution[1];
            mGroundTruth[2] = posSolution[2];
        } else if (mResidualPlotStatus == RESIDUAL_MODE_STILL) {
            // If the user is standing still, we average our WLS position solution
            // Reference: https://en.wikipedia.org/wiki/Moving_average#Cumulative_moving_average
            mGroundTruth[0] =
                    (mGroundTruth[0] * mPositionSolutionCount + posSolution[0])
                            / (mPositionSolutionCount + 1);
            mGroundTruth[1] =
                    (mGroundTruth[1] * mPositionSolutionCount + posSolution[1])
                            / (mPositionSolutionCount + 1);
            mGroundTruth[2] =
                    (mGroundTruth[2] * mPositionSolutionCount + posSolution[2])
                            / (mPositionSolutionCount + 1);
            mPositionSolutionCount++;
        } else if (mResidualPlotStatus == RESIDUAL_MODE_MOVING) {
            // If the user is moving fast, we use single WLS position solution
            mGroundTruth[0] = posSolution[0];
            mGroundTruth[1] = posSolution[1];
            mGroundTruth[2] = posSolution[2];
            mPositionSolutionCount = 0;
        }
    }

    /**
     * Sets the ground truth mode in {@link PseudorangePositionVelocityFromRealTimeEvents} for
     * calculating corrected pseudorange residuals, also logs the change in ResultFragment
     */
    public void setResidualPlotMode(int residualPlotStatus, double[] fixedGroundTruth) {
        mResidualPlotStatus = residualPlotStatus;
        if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
            return;
        }
        switch (mResidualPlotStatus) {
            case RESIDUAL_MODE_MOVING:
                mPseudorangePositionVelocityFromRealTimeEvents
                        .setCorrectedResidualComputationTruthLocationLla(mGroundTruth);
                logEvent("Residual Plot", "Mode is set to moving", mCurrentColor);
                break;

            case RESIDUAL_MODE_STILL:
                mPseudorangePositionVelocityFromRealTimeEvents
                        .setCorrectedResidualComputationTruthLocationLla(mGroundTruth);
                logEvent("Residual Plot", "Mode is set to still", mCurrentColor);
                break;

            case RESIDUAL_MODE_AT_INPUT_LOCATION:
                mPseudorangePositionVelocityFromRealTimeEvents
                        .setCorrectedResidualComputationTruthLocationLla(fixedGroundTruth);
                logEvent("Residual Plot", "Mode is set to fixed ground truth", mCurrentColor);
                break;

            case RESIDUAL_MODE_DISABLED:
                mGroundTruth = null;
                mPseudorangePositionVelocityFromRealTimeEvents
                        .setCorrectedResidualComputationTruthLocationLla(mGroundTruth);
                logEvent("Residual Plot", "Mode is set to Disabled", mCurrentColor);
                break;

            default:
                mPseudorangePositionVelocityFromRealTimeEvents
                        .setCorrectedResidualComputationTruthLocationLla(null);
        }
    }

    @Override
    public void onTTFFReceived(long l) {
    }
}
