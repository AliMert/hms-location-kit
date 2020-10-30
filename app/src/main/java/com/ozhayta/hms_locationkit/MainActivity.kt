package com.ozhayta.hms_locationkit

import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
        var DESTINATION_LATITUDE =  37.003312
        var DESTINATION_LONGITUDE = 35.323345

        // huawei istanbul
        // var DESTINATION_LATITUDE =  41.028720
        // var DESTINATION_LONGITUDE = 29.117530

    // var DESTINATION_LATITUDE =  36.995962
    // var DESTINATION_LONGITUDE = 35.324905
    }

    private var locationService: HMSLocationService?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationService = HMSLocationService(this)

        locationService?.setLocationCallback {
            it?.let { location -> locationReceived(location) }
                ?: Log.i(TAG, "location returned null")
        }


        et_latitude.setText(DESTINATION_LATITUDE.toString())
        et_longitude.setText(DESTINATION_LONGITUDE.toString())


        // request location
        get_location_btn.setOnClickListener {
            calculateDistanceButtonIsClicked()
        }

        set_mock_location_btn.setOnClickListener {
            alertDialogSetMockLocation()
        }
    }

    private fun calculateDistanceButtonIsClicked() {
        locationService?.requestLocationUpdatesWithCallback()
        val message  = "getting location...."
        log.text = message
        latitude.text = message
        longitude.text = message
        accuracy.text = message
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

    private fun alertDialogSetMockLocation() {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_layout, null)
        val latitudeEditText = dialogLayout.findViewById<EditText>(R.id.latitude_editText)
        val longitudeEditText = dialogLayout.findViewById<EditText>(R.id.longitude_editText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Please enter a mock location:")
            .setPositiveButton("ok", null)
            .setNegativeButton("disable Mock location") { dialog, which ->
                locationService?.setMockMode(false)
                calculateDistanceButtonIsClicked()
            }
            .setView(dialogLayout)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.red, this.theme))
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            val lat = latitudeEditText.text.toString().toDoubleOrNull()
            val lon = longitudeEditText.text.toString().toDoubleOrNull()

            if (latitudeEditText.text.isEmpty() || lat == null ) {
                latitudeEditText.error = "please enter correct lat"
                latitudeEditText.requestFocus()
                locationService?.setMockMode(false)
                calculateDistanceButtonIsClicked()
                return@setOnClickListener
            }
            if (longitudeEditText.text.isEmpty() || lon == null) {
                longitudeEditText.error = "please enter correct lon"
                longitudeEditText.requestFocus()
                locationService?.setMockMode(false)
                calculateDistanceButtonIsClicked()
                return@setOnClickListener
            }

            val mockLocation = Location("mockLocation")
            mockLocation.latitude = lat
            mockLocation.longitude = lon

            locationService?.setMockMode(true)
            locationService?.setMockLocation(mockLocation)

            calculateDistanceButtonIsClicked()
            dialog.dismiss()
        }
    }


    override fun onDestroy() {
        // don't need to receive callback
       locationService?.removeLocationUpdatesWithCallback()
        super.onDestroy()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}