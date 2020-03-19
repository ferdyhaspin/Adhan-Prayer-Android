package com.ferdyhaspin.adhan_prayer_android.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*

/**
 * Created by ferdyhaspin on 19/03/20.
 * Copyright (c) 2020  All rights reserved.
 */

class LocationProvider : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var mLocationProviderCallback: LocationProviderCallback

    private var isLocationPermissionDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mLocationProviderCallback = if (context is LocationProviderCallback) {
            context
        } else {
            throw IllegalArgumentException("activity implement LocationProvider.LocationProviderCallback")
        }
    }

    fun checkLocation() {
        val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationProviderCallback.onGPSisDisable()
            return
        }

        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (PermissionUtil.hasSelfPermission(requireActivity(), permissions)) {
            getLocationUpdates()
        } else {
            if (!isLocationPermissionDenied && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, Constants.REQUEST_LOCATION)
            }else{
                mLocationProviderCallback.onLocationError("Permission Denied")
            }
        }
    }

    private fun getLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest()
        locationRequest.interval = 50000
        locationRequest.fastestInterval = 50000
        locationRequest.smallestDisplacement = 170f //170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //according to your app

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult?) {
                if (result != null) {
                    mLocationProviderCallback.onLocationChanged(result.lastLocation)
                } else {
                    mLocationProviderCallback.onLocationError("Location result null")
                }
            }
        }
    }

    // Start location updates
    private fun startLocationUpdates() {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
    }

    // Stop location updates
    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Stop receiving location update when activity not visible/foreground
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // Start receiving location update when activity  visible/foreground
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationUpdates()
            } else {
                isLocationPermissionDenied = true
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    interface LocationProviderCallback {
        fun onLocationChanged(location: Location)
        fun onLocationError(msg: String)
        fun onGPSisDisable()
    }

    companion object {
        private var mLocationProvider: LocationProvider? = null

        fun newInstance(): LocationProvider {
            if (mLocationProvider == null) {
                mLocationProvider = LocationProvider()
            }
            return mLocationProvider as LocationProvider
        }
    }

}