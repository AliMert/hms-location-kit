package com.ozhayta.hms_locationkit

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
        var DESTINATION_LATITUDE =  37.003312
        var DESTINATION_LONGITUDE = 35.323345

    // var DESTINATION_LATITUDE =  36.995962
    // var DESTINATION_LONGITUDE = 35.324905
    }

    private var locationServices: HMSLocationServices?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationServices = HMSLocationServices(this, false)

        locationServices?.setLocationCallback {
            it?.let { location -> locationReceived(location) }
                ?: Log.i(TAG, "location returned null")
        }

        // request location
        get_location_btn.setOnClickListener {
            locationServices?.requestLocationUpdatesWithCallback()
            val message  = "getting location...."
            log.text = message
            latitude.text = message
            longitude.text = message
            accuracy.text = message
        }


        et_latitude.setText(DESTINATION_LATITUDE.toString())
        et_longitude.setText(DESTINATION_LONGITUDE.toString())
    }

    private fun calculateDistance(location: Location){
        val startPoint = Location("currentLocation")
        startPoint.latitude = location.latitude
        startPoint.longitude = location.longitude


        val endPoint = Location("destinationLocation")


        val lat = et_latitude.text.toString().toDoubleOrNull()
        val lon = et_longitude.text.toString().toDoubleOrNull()
            if (lat == null) {
                log.text = "Enter a correct latitude !!"
                return
            }
            if (lon == null) {
                log.text = "Enter a correct longitude !!"
                return
            }

        endPoint.latitude =  lat
        endPoint.longitude = lon

        val distance: Float = startPoint.distanceTo(endPoint)
        Log.i(TAG, "distance: $distance")
        val message =
            if (distance <= 500)
                "distance: is in 500m range. distance: ${distance.toInt()}meters"
            else
                "distance: is NOT in 500m range !! distance: ${distance.toInt()}meters"

        Log.i(TAG, message)
        log.text = message
    }

    private fun locationReceived(location: Location) {

        Log.i("$TAG success", location.toString())
        log.text = "location is set !!"
        latitude.text = location.latitude.toString()
        longitude.text = location.longitude.toString()
        accuracy.text = location.accuracy.toString()
         calculateDistance(location)
    }




    override fun onDestroy() {
        // don't need to receive callback
       locationServices?.removeLocationUpdatesWithCallback()
        super.onDestroy()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationServices?.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

}