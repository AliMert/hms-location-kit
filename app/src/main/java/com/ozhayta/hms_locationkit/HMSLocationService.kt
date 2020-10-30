package com.ozhayta.hms_locationkit

import android.Manifest
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.huawei.hms.common.ApiException
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.*


class HMSLocationService {

    companion object {
        const val TAG = "HMSLocationServices"
        const val LOCATION_PERMISSION = 10001
        const val LOCATION_PERMISSION_2 = 10002
    }

    constructor(
        fragment: Fragment,
        isContinuous: Boolean? = null,
        interval: Long? = null
    ) {
        initializeParameters(isContinuous, interval, fragment = fragment)
    }

    constructor(
        activity: Activity,
        isContinuous: Boolean? = null,
        interval: Long? = null
    ) {
        initializeParameters(isContinuous, interval, activity)
    }


    private var isContinuous: Boolean? = null
    private var interval: Long? = null
    private lateinit var activity: Activity
    private lateinit var fragment: Fragment

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var settingsClient: SettingsClient? = null


    private fun initializeParameters(
        isContinuous: Boolean? = null,
        interval: Long? = null,
        activity: Activity? = null,
        fragment: Fragment? = null
    ) {

        activity?.let { this.activity = it }
        fragment?.let { this.fragment = it }
        isContinuous?.let { this.isContinuous = it }
        interval?.let {
            this.isContinuous = true
            this.interval = it
        }

        initialize()
    }

    private fun initialize() {
        checkLocationPermission()

        if (this::activity.isInitialized) {
            Log.i(TAG, "activity initialized")
            // create fusedLocationProviderClient
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
            // create settingsClient
            settingsClient = LocationServices.getSettingsClient(activity)
        } else if (this::fragment.isInitialized) {
            Log.i(TAG, "fragment initialized")
            // create fusedLocationProviderClient
            fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(fragment.requireContext())
            // create settingsClient
            settingsClient = LocationServices.getSettingsClient(fragment.requireContext())
        }

        mLocationRequest = LocationRequest()

        // set the priority of the request
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        if (isContinuous != true)
            mLocationRequest?.numUpdates = 1
        else
            mLocationRequest?.interval = interval ?: 10000
    }

    fun setLocationCallback(callBack: ((location: Location?) -> Any)) {

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.i(TAG, "onLocationResult location[Latitude,Longitude,Accuracy]:" +
                                location.latitude + "," + location.longitude + "," + location.accuracy
                    )
                    callBack(location)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                val flag = locationAvailability.isLocationAvailable
                Log.i(TAG, "onLocationAvailability isLocationAvailable:$flag")
                // use last known location if location is not available
                if (!flag)
                    getLastKnownLocation(callBack)
            }
        }

    }

    fun getLastKnownLocation(callBack: (location: Location?) -> Any) {
        // get last known location
        fusedLocationProviderClient?.lastLocation
            ?.addOnSuccessListener { location ->
                Log.i(TAG, "last known location: $location")
                callBack(location)
            }
            ?.addOnFailureListener {
                Log.e(TAG, "error on last known location: ${it.localizedMessage}")
                callBack(null)
            }
    }

    /**
     * remove the request with callback
     */
    fun removeLocationUpdatesWithCallback() {
        try {
            fusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
                ?.addOnSuccessListener { Log.i(TAG, "removeLocationUpdatesWithCallback onSuccess") }
                ?.addOnFailureListener { e ->
                    Log.e(
                        TAG,
                        "removeLocationUpdatesWithCallback onFailure:" + e.message
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "removeLocationUpdatesWithCallback exception:" + e.message)
        }
    }

    /**
     * function：Requests location updates with a callback on the specified Looper thread.
     * first：use SettingsClient object to call checkLocationSettings(LocationSettingsRequest locationSettingsRequest) method to check device settings.
     * second： use  FusedLocationProviderClient object to call requestLocationUpdates (LocationRequest request, LocationCallback callback, Looper looper) method.
     */
    fun requestLocationUpdatesWithCallback() {
        try {
            val builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(mLocationRequest)
            val locationSettingsRequest = builder.build()
            // check devices settings before request location updates.
            settingsClient?.checkLocationSettings(locationSettingsRequest)
                ?.addOnSuccessListener {
                    Log.i(TAG, "check location settings success")
                    // request location updates
                    fusedLocationProviderClient
                        ?.requestLocationUpdates(
                            mLocationRequest,
                            mLocationCallback,
                            Looper.getMainLooper()
                        )
                        ?.addOnSuccessListener {
                            Log.i(
                                TAG,
                                "requestLocationUpdatesWithCallback onSuccess"
                            )
                        }
                        ?.addOnFailureListener { e ->
                            Log.e(
                                TAG,
                                "requestLocationUpdatesWithCallback onFailure:" + e.message
                            )
                        }
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "checkLocationSetting onFailure:" + e.message)
                    when ((e as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val rae = e as ResolvableApiException
                            if (this::activity.isInitialized)
                                rae.startResolutionForResult(activity, 0)
                            else
                                rae.startResolutionForResult(fragment.requireActivity(), 0)
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.e(TAG, "PendingIntent unable to execute request.")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "requestLocationUpdatesWithCallback exception:" + e.message)
        }
    }

    fun setMockMode(mockFlag: Boolean) {
        fusedLocationProviderClient?.setMockMode(mockFlag)
            ?.addOnSuccessListener {
                Log.i(TAG, "setMockMode onSuccess")
            }
            ?.addOnFailureListener { e ->
                Log.e(
                    TAG,
                    "setMockMode onFailure:" + e.message
                )
            }
    }

    fun setMockLocation(mockLocation: Location) {

        fusedLocationProviderClient?.setMockLocation(mockLocation)
            ?.addOnSuccessListener { Log.i(TAG, "setMockLocation onSuccess") }
            ?.addOnFailureListener { e ->
                Log.e(TAG,"setMockLocation onFailure:" + e.message)
            }
    }

    fun checkLocationPermission() {
        if (this::activity.isInitialized)
            checkLocationPermissionActivity()
        else
            checkLocationPermissionFragment()
    }

    private fun checkLocationPermissionActivity() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk < 28 Q")
            if ((ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED)
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                ActivityCompat.requestPermissions(activity, strings, LOCATION_PERMISSION)
            }
        } else {
            if ((ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                        ) && (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                        ) && (ActivityCompat.checkSelfPermission(
                    activity,
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                ) !== PackageManager.PERMISSION_GRANTED)
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                )
                ActivityCompat.requestPermissions(activity, strings, LOCATION_PERMISSION_2)
            }
        }

    }

    private fun checkLocationPermissionFragment() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk < 28 Q")
            if ((fragment.requireContext().checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                        && fragment.requireContext().checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED)
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                fragment.requestPermissions(strings, LOCATION_PERMISSION)
            }
        } else {
            if ((fragment.requireContext().checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                        ) && (fragment.requireContext().checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
                        ) && (fragment.requireContext().checkSelfPermission(
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                ) !== PackageManager.PERMISSION_GRANTED)
            ) {
                val strings = arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    "android.permission.ACCESS_BACKGROUND_LOCATION"
                )
                fragment.requestPermissions(strings, LOCATION_PERMISSION_2)
            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "onRequestPermissionsResult: apply LOCATION PERMISSION successful")
            } else {
                Log.i(TAG, "onRequestPermissionsResult: apply LOCATION PERMISSSION  failed")
            }
        }
        if (requestCode == 2) {
            if (grantResults.size > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(
                    TAG,
                    "onRequestPermissionsResult: apply ACCESS_BACKGROUND_LOCATION successful"
                )
            } else {
                Log.i(TAG, "onRequestPermissionsResult: apply ACCESS_BACKGROUND_LOCATION  failed")
            }
        }
    }

}

