package com.ferdyhaspin.adhan_prayer_android.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

/**
 * Created by ferdyhaspin on 13/03/20.
 * Copyright (c) 2020 All rights reserved.
 */
class LocationHelper : Fragment(),
    Constants, ConnectionCallbacks,
    OnConnectionFailedListener {
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var mCallback: LocationCallback? = null
    private var sLocationPermissionDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallback = if (context is LocationCallback) {
            context
        } else {
            throw IllegalArgumentException("activity must extend BaseActivity and implement LocationHelper.LocationCallback")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    fun checkLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (PermissionUtil.hasSelfPermission(
                requireActivity(),
                permissions
            )
        ) {
            initAppAfterCheckingLocation()
        } else {
            if (!sLocationPermissionDenied) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        Constants.REQUEST_LOCATION
                    )
                }
            }
        }
    }

    private fun initAppAfterCheckingLocation() {
        if (!::mGoogleApiClient.isInitialized) {
            buildGoogleApiClient()
        } else if (sLastLocation == null) {
            if (mGoogleApiClient.isConnected) {
                checkIfLocationServicesEnabled()
            }
        } else {
            Log.d(TAG, "${sLastLocation?.latitude}, ${sLastLocation?.longitude}")
            mCallback?.onLocationChanged(sLastLocation)
        }
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(requireContext())
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient.connect()
    }

    private fun checkLocationAndInit() {
        if (sLastLocation == null) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())
            val locationTask =
                fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sLastLocation = location
                    initAppAfterCheckingLocation()
                }
            }
            locationTask.addOnFailureListener { e: Exception ->
                e.printStackTrace()
                Log.e(TAG, "getLastLocation().addOnFailureListener()")
                mCallback?.onLocationSettingsFailed()
            }
        } else {
            initAppAfterCheckingLocation()
        }
    }

    private fun checkIfLocationServicesEnabled() {
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.numUpdates = 1
        mLocationRequest.smallestDisplacement = 170f
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(requireContext())
        val task =
            client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { checkLocationAndInit() }
        task.addOnFailureListener { e: Exception? ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(
                        requireActivity(),
                        Constants.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: SendIntentException) { // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) { //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        if (requestCode == Constants.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) { // All required changes were successfully made
                checkLocationAndInit()
                // The user was asked to change settings, but chose not to
            } else {
                mCallback?.onLocationSettingsFailed()
            }
        }
    }

    override fun onConnected(bundle: Bundle?) {
        checkIfLocationServicesEnabled()
    }

    override fun onConnectionSuspended(i: Int) {}
    override fun onConnectionFailed(connectionResult: ConnectionResult) {}
    private fun setLocationPermissionDenied() {
        sLocationPermissionDenied = true
    }
    //    public static boolean isLocationPermissionDenied() {
//        return sLocationPermissionDenied;
//    }
    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissions()
            } else {
                Log.i(TAG, "LOCATION permission was NOT granted.")
                setLocationPermissionDenied()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    interface LocationCallback {
        fun onLocationSettingsFailed()
        fun onLocationChanged(location: Location?)
    }

    companion object {
        private const val TAG = "LocationHelper"
        private var sLastLocation: Location? = null
        fun newInstance(): LocationHelper {
            return LocationHelper()
        }
    }
}