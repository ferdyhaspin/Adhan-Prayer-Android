package com.ferdyhaspin.adhan_prayer_android.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import org.jetbrains.annotations.NotNull;

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */

public class LocationHelper extends Fragment implements Constants, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static String TAG = "LocationHelper";

    private static Location sLastLocation;

    private GoogleApiClient mGoogleApiClient;

    private LocationCallback mCallback;
    private boolean sLocationPermissionDenied;

    public static LocationHelper newInstance() {
        return new LocationHelper();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof LocationCallback) {
            mCallback = (LocationCallback) context;
        } else {
            throw new IllegalArgumentException("activity must extend BaseActivity and implement LocationHelper.LocationCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

//    public Location getLocation() {
//        return sLastLocation;
//    }

    public void checkLocationPermissions() {
        if (PermissionUtil.hasSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            initAppAfterCheckingLocation();
        } else {
            if (!sLocationPermissionDenied) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                }
            }
        }
    }

    private void initAppAfterCheckingLocation() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        } else if (sLastLocation == null) {
            if (mGoogleApiClient.isConnected()) {
                // check for a location.
                checkIfLocationServicesEnabled();
            } //else if (mGoogleApiClient.isConnecting()) {
            //do nothing
            //}
        } else {
            Log.d(TAG, sLastLocation.getLatitude() + "," + sLastLocation.getLongitude());
            if (mCallback != null) {
                mCallback.onLocationChanged(sLastLocation);
            }
        }
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(requireContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private void checkLocationAndInit() {
        if (sLastLocation == null) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            Task<Location> locationTask = fusedLocationClient.getLastLocation();

            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    sLastLocation = location;
                    initAppAfterCheckingLocation();
                }
            });

            locationTask.addOnFailureListener(e -> {
                e.printStackTrace();
                Log.e(TAG, "getLastLocation().addOnFailureListener()");
                if (mCallback != null) {
                    mCallback.onLocationSettingsFailed();
                }
            });
        } else {
            initAppAfterCheckingLocation();
        }
    }

    private void checkIfLocationServicesEnabled() {
        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(requireContext());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> checkLocationAndInit());

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(),
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {// All required changes were successfully made
                checkLocationAndInit();
                // The user was asked to change settings, but chose not to
            } else {
                if (mCallback != null) {
                    mCallback.onLocationSettingsFailed();
                }
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkIfLocationServicesEnabled();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void setLocationPermissionDenied() {
        sLocationPermissionDenied = true;
    }

//    public static boolean isLocationPermissionDenied() {
//        return sLocationPermissionDenied;
//    }

    /**
     * Callback received when a permissions request has been completed.
     */
    //@Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions,
                                           @NotNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissions();
            } else {
                Log.i(TAG, "LOCATION permission was NOT granted.");
                setLocationPermissionDenied();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    // NOT SURE WHERE THIS CODE GOES, BUT ITS NEEDED SO THAT WE CAN BE NOTIFIED
    // OF SEVERE LOCATION CHANGES BY THE USER, SO THAT WE CAN RE-CALCULATE THE
    // SALAAT TIMES AND UPDATE THE ALARMS.
    // ALSO HAVE TO HANDLE CANCELING OF UPDATES IF THE USER REVOKES PERMISSION
    // THROUGH SETTINGS
    // IMPORTANT: THIS SHOULD NEEDS TO BE SET AGAIN IN THE BOOT RECEIVER.
  /*
  Intent passiveIntent = new Intent(context, PassiveLocationChangedReceiver.class);
  PendingIntent locationListenerPassivePendingIntent = PendingIntent.getActivity(getActivity(), 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
  locationUpdateRequester.requestPassiveLocationUpdates(getActivity(), locationListenerPassivePendingIntent);
   */
//    public void requestPassiveLocationUpdates(Context context, PendingIntent pendingIntent) {
//        long oneHourInMillis = 3600000;
//        float fiftyKinMeters = 50000.0f;
//
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        try {
//            if (locationManager != null) {
//                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
//                        oneHourInMillis, fiftyKinMeters, pendingIntent);
//            }
//        } catch (SecurityException se) {
//            //do nothing. We should always have permision in order to reach this screen.
//        }
//    }
//
//    public void removePassiveLocationUpdates(Context context, PendingIntent pendingIntent) {
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        try {
//            if (locationManager != null) {
//                locationManager.removeUpdates(pendingIntent);
//            }
//        } catch (SecurityException se) {
//            //do nothing. We should always have permision in order to reach this screen.
//        }
//    }

    public interface LocationCallback {
        void onLocationSettingsFailed();

        void onLocationChanged(Location location);
    }
}
