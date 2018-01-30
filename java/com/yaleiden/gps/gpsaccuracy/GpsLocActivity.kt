package com.yaleiden.gps.gpsaccuracy

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.gps_display.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.TableRow


/**
 * Created by Yale on 1/29/2018.
 */
class GpsLocActivity : AppCompatActivity() {

    private val DEBUG_TAG: String = "GpsLocActivity"
    private lateinit var mLocationCallback: LocationCallback
    private var mRequestingLocationUpdates: Boolean = false  // true if we have user permission
    private val MY_PERMISSIONS_REQUEST_FINE_LOCATION: Int = 300
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mfusedLocationProviderclient: FusedLocationProviderClient
    private lateinit var mFab: FloatingActionButton
    private var spinner: Spinner? = null
    private var textViewLat: TextView? = null
    private var textViewLng: TextView? = null
    private var textViewSpd: TextView? = null
    private var textViewAccuracy: TextView? = null
    private var tableRowLatLng: TableRow? = null
    private val speedArray: Array<String> = arrayOf("m/s", "mph", "knots")
    /*
        Conversions:
        1 m/s = 2.23694 mph
        1 m/s = 1.94384 knot
         */
    private val conversionArray: Array<Float> = arrayOf(1.0f, 2.23694f, 1.94384f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gps_display)


        mFab = findViewById(R.id.floatingActionButton)
        mFab.setOnClickListener(View.OnClickListener {
            fabClick()
            startLocationUpdates()
        }
        )

        spinner = findViewById(R.id.spinnerSpeed)

        textViewLat = findViewById(R.id.textViewLat)
        textViewLng = findViewById(R.id.textViewLng)
        textViewSpd = findViewById(R.id.textViewSpd)
        textViewAccuracy = findViewById(R.id.textViewAccuracy)
        tableRowLatLng = findViewById(R.id.tableRowLatLng)

        tableRowLatLng!!.setOnClickListener(View.OnClickListener { copyLatLng() })

        val speedAdapter = ArrayAdapter(this, R.layout.spinner_item_speed, speedArray)
        spinner!!.adapter = speedAdapter

        //Runtime permission check
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            mRequestingLocationUpdates = false

            // show an explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION)

            }
        }

        //Location callback
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                for (location in locationResult!!.locations) {
                    // Update UI with location data
                    onLocationUpdated(location)

                }
            }
        }
    }


    // called when floating action button is pressed
    fun fabClick() {

        if (mRequestingLocationUpdates) {

            startLocationUpdates()
            //TODO show fab active
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mRequestingLocationUpdates) {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        mfusedLocationProviderclient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        if (mRequestingLocationUpdates == true) {
            mfusedLocationProviderclient.removeLocationUpdates(mLocationCallback)
        }
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    // permission was granted
                    mRequestingLocationUpdates = true

                    mfusedLocationProviderclient = LocationServices.getFusedLocationProviderClient(this);

                    createLocationRequest();

                } else {

                    // permission denied. Notify user
                    // functionality that depends on this permission.

                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.

            else -> {
                // Ignore all other requests.
            }
        }
    }


    private fun onLocationUpdated(location: Location) {

        val accuracy: Float = location.accuracy
        val bearing: Float = location.bearing
        val lat: Double = location.latitude
        val lng: Double = location.longitude
        val provider: String = location.provider
        val speed: Float = location.speed  //meters per second
        //val speed_accuracy: Float = location.speedAccuracyMetersPerSecond
        val gps_time: Long = location.time
        val desription: Int = location.describeContents()

        val errorText: String = "%.1f".format(accuracy)

        textViewLat!!.text = "Lat: " + lat.toString()
        textViewLng!!.text = "Lng: " + lng.toString()
        //textViewSpd!!.text = speed.toString()
        textViewSpd!!.text = spinnerString(speed)
        textViewAccuracy!!.text = errorText
        /*
        Conversions:
        1 m/s = 2.23694 mph
        1 m/s = 1.94384 knot
         */


/*
        getAccuracy()
        //Get the estimated horizontal accuracy of this location, radial, in meters. float
        getBearing()
        //Get the bearing, in degrees. float
        getLatitude()
        //Get the latitude, in degrees.
        getLongitude()
        //Get the longitude, in degrees.
        getProvider()
        //Returns the name of the provider that generated this fix.
        getSpeed()
        //Get the speed if it is available, in meters/second over ground. float
        getSpeedAccuracyMetersPerSecond()
        //Get the estimated speed accuracy of this location, in meters per second. float
        getTime()
        //Return the UTC time of this fix, in milliseconds since January 1, 1970. long
        describeContents()
        //Describe the kinds of special objects contained in this Parcelable instance's marshaled representation. int

*/
        val currentPosition = LatLng(location.latitude, location.longitude)


    }

    /**
     * Returns String
     * @param Float speed value
     * @return String value of converted speed unit
     */
    private fun spinnerString(speed: Float): String {
        var selectedSpeed: String = spinnerSpeed.selectedItem.toString()
        var spd: Float = 0.0f
        var count: Int = 0
        for (sp in speedArray) {
            if (sp == selectedSpeed) {
                spd = speed * conversionArray[count]
            }
            count += 1
        }

        return "%.1f".format(spd)
    }


    @SuppressLint("MissingPermission")
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest.setInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        mfusedLocationProviderclient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */)
    }

    fun snackMessage(msg: String) {

        val snackbar = Snackbar
                .make(findViewById<CoordinatorLayout>(R.id.gpsCoordinatorLayout), msg, Snackbar.LENGTH_INDEFINITE)

        snackbar.show()
    }

    fun snackMessageDismiss(msg: String) {

        val snackbar = Snackbar
                .make(findViewById<CoordinatorLayout>(R.id.gpsCoordinatorLayout), msg, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("Dismiss", { snackbar.dismiss() }
        )
        snackbar.show()
    }

    fun copyText(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    fun copyLatLng() {
        val copyString = textViewLat!!.text.toString() + " " + textViewLng!!.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", copyString)
        clipboard.setPrimaryClip(clip)
        snackMessage("Coordinates copied to clipboard")
    }


}
