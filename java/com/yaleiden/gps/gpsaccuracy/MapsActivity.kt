package com.yaleiden.gps.gpsaccuracy

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.model.*
import android.view.Menu
import android.view.MenuItem
import android.widget.Spinner
import android.widget.TextView


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, MapTypeDialogFragment.MapDialogListener {

    private val DEBUG_TAG: String = "MapsActivity"
    private var pointCount: Int = 0  //counter
    private var sampleSize: Int = 10  //number of Location values to collect
    private lateinit var mLocationCallback: LocationCallback
    private var mRequestingLocationUpdates: Boolean = false  // true if we have user permission
    private var isSamplingActive: Boolean = false
    private var isMapClean: Boolean = true  // prevents trying to remove lateinit markerArray, mCircle
    private val MY_PERMISSIONS_REQUEST_FINE_LOCATION: Int = 300
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
    private lateinit var mMap: GoogleMap
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mfusedLocationProviderclient: FusedLocationProviderClient
    private lateinit var locationArray: Array<Location>
    private lateinit var markerArray: Array<Marker>  //Markers stored in array so they can be removed later
    private val floydsisland: LatLng = LatLng(30.86, -82.26)  //Starting map location
    private lateinit var floydMarker: Marker
    private lateinit var mCircle: Circle  // can be removed from map in cleanUpMap()
    private lateinit var mProgressBar: ProgressBar  // 0 to sampleSize
    private var mapTypeSelected = 0
    private var spinner: Spinner? = null
    private var mFab: FloatingActionButton? = null
    private var textViewMv: TextView? = null
    private var textViewRv: TextView? = null
    private val blank_text: String = "- - -"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFab = findViewById(R.id.fab)
        mFab!!.setOnClickListener(View.OnClickListener {

            fabClick()
        }
        )

        spinner = findViewById(R.id.spinner)
        textViewMv = findViewById(R.id.textViewMv)
        textViewRv = findViewById(R.id.textViewRv)

        //onClickListeners to copy values to clipboard
        textViewMv!!.setOnClickListener { copyLatLng() }
        textViewRv!!.setOnClickListener { copyLatLng() }

        mProgressBar = findViewById(R.id.progressBar)
        mProgressBar.visibility = View.INVISIBLE

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.maps_activity_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.menu_map_type -> {

                showMapTypeDialog()
            }
            R.id.menu_gps -> {
                val intent = Intent(this, GpsLocActivity::class.java)
                //intent.putExtra("key", value)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // called when floating action button is pressed
    fun fabClick() {

        if (mRequestingLocationUpdates && !isSamplingActive) {
            sampleSize = Integer.valueOf(spinner!!.selectedItem.toString())
            isSamplingActive = true
            cleanUpMap()
            pointCount = 0
            startLocationUpdates()
            //TODO show fab active
        }
        if (!mRequestingLocationUpdates) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }
        if (mRequestingLocationUpdates && isSamplingActive){
            snackMessage("Positions are being sampled")
        }
        else {
            //
        }
    }

    override fun onResume() {
        super.onResume()
        if (mRequestingLocationUpdates && isSamplingActive) {
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

                    //createLocationRequest();
                    mLocationRequest = LocationRequest()
                    mLocationRequest.setInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker and move camera position
        floydMarker = mMap.addMarker(MarkerOptions().position(floydsisland).title("Floyd's Island"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(floydsisland, 18.0f))
    }


    private fun onLocationUpdated(location: Location) {
        /*
        if (BuildConfig.DEBUG) {
            Log.d(DEBUG_TAG, "1 pointCount = " + pointCount)
        }
        */

        if (pointCount == sampleSize) {
            //sampling is done: do analysis

            onLocationAnalysis(locationArray)
            stopLocationUpdates()

            mProgressBar.visibility = View.INVISIBLE


        } else {

            if (pointCount == 0) {

                mProgressBar.visibility = View.VISIBLE
                mProgressBar.max = sampleSize

                locationArray = Array(sampleSize) { location }

                val floydMarker: Marker = mMap.addMarker(MarkerOptions().position(floydsisland))
                markerArray = Array(sampleSize + 1) { floydMarker }

            }

            val currentPosition = LatLng(location.latitude, location.longitude)
            val currentLabel: String = "Point "+(pointCount + 1).toString()

            var mMarker: Marker = mMap.addMarker(MarkerOptions().position(currentPosition).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).title(currentLabel))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 19.0f))


            locationArray.set(pointCount, location)
            markerArray.set(pointCount, mMarker)
            pointCount = pointCount + 1
            mProgressBar.progress = pointCount


        }
    }

    private fun onLocationAnalysis(locations: Array<Location>) {

        if (BuildConfig.DEBUG) {
            Log.d(DEBUG_TAG, "onLocationAnalysis ")
        }

        //sampling is done: do analysis

        val bounds: Array<Double> = GeoAnalysis(locations).getBounds()
        val southwest: LatLng = LatLng(bounds[0], bounds[1])
        val northeast: LatLng = LatLng(bounds[2], bounds[3])
        val latlngBounds: LatLngBounds = LatLngBounds(southwest, northeast)

        //move camera to the new bounds
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latlngBounds, 32))


        val avgPosition = LatLng(bounds[4], bounds[5])

        //show average values on screen

        textViewMv!!.text = "%.8f".format(bounds[4])
        textViewRv!!.text = "%.8f".format(bounds[5])

        //Add avgMarker to markerArray so we can remove all markers on cleanUpMap()
        val avgMarker: Marker = mMap.addMarker(MarkerOptions().position(avgPosition).title("Average").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)))
        markerArray.set(sampleSize, avgMarker)


        mCircle = mMap.addCircle(makeCircle(bounds[4], bounds[5], bounds[6]))


        val errorText: Double = java.math.BigDecimal(bounds[6]).setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toDouble()
        snackMessageDismiss("Average error " + errorText + " meters")

        if (BuildConfig.DEBUG) {
            val sdText: Double = java.math.BigDecimal(bounds[7]).setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toDouble()
            Log.d(DEBUG_TAG, "Standard dev " + sdText)
        }

        isSamplingActive = false
        isMapClean = false
    }

    /**
     * Returns CircleOptions
     * @param lat Double
     * @param lng Double
     * @param rad Double (radius in meters)
     */
    private fun makeCircle(lat: Double, lng: Double, rad: Double): CircleOptions {
        // Add a circle of x meters around LatLng
        val circleOptions: CircleOptions = CircleOptions()
                .center(LatLng(lat, lng))
                .radius(rad)
                .strokeColor(Color.RED)
        //.fillColor(Color.BLUE)

        return circleOptions

    }

    fun snackMessage(msg: String) {

        val snackbar = Snackbar
                .make(findViewById<CoordinatorLayout>(R.id.dummy_layout_for_snackbar), msg, Snackbar.LENGTH_LONG)

        snackbar.show()
    }

    fun snackMessageDismiss(msg: String) {

        val snackbar = Snackbar
                .make(findViewById<CoordinatorLayout>(R.id.dummy_layout_for_snackbar), msg, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("Dismiss", { snackbar.dismiss() }
        )
        snackbar.show()
    }

    //Remove markers from sampled locations, error circle and lat/lng
    private fun cleanUpMap() {

        if (!isMapClean) {

            textViewMv!!.text = blank_text
            textViewRv!!.text = blank_text

            for (marker in markerArray) {
                marker.remove()
            }
            mCircle.remove()
        }
    }


    private fun showMapTypeDialog() {

        if (BuildConfig.DEBUG) {

            Log.d(DEBUG_TAG, "showMapTypeDialog()")
        }

        val mapTypeDialogFragment = MapTypeDialogFragment.newInstance(mapTypeSelected)
        mapTypeDialogFragment.show(this@MapsActivity.supportFragmentManager, "name")

    }

    /**
     * callback from MapTypeDialogFragment interface
     * @param value Int the type of map selected
     */
    override fun onChangeMapType(value: Int) {

        if (BuildConfig.DEBUG) {

            Log.d(DEBUG_TAG, "onChangeMapType " + value)
        }

        val map_types = arrayOf("Normal", "Satelite", "Terrain", "Hybrid")
        if (value == 0) {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            mapTypeSelected = 0
        }
        if (value == 1) {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            mapTypeSelected = 1
        }
        if (value == 2) {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            mapTypeSelected = 2
        }
        if (value == 3) {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            mapTypeSelected = 3
        }
    }

    fun copyLatLng() {
        val copyString = textViewMv!!.text.toString() + " " + textViewRv!!.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", copyString)
        clipboard.setPrimaryClip(clip)
        snackMessage("Coordinates copied to clipboard")
    }
}

