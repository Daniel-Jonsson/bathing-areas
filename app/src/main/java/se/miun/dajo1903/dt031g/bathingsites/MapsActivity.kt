package se.miun.dajo1903.dt031g.bathingsites

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.miun.dajo1903.dt031g.bathingsites.databinding.ActivityMapsBinding

/**
 * The MapsActivity is used to display a google map with all nearby bathing sites based on the
 * preference selected by the user. It also displays information about the bathing site when a user
 * clicks on the marker, such as name and distance.
 *
 * Custom marker snippet was learnt from: https://stackoverflow.com/questions/13904651/android-google-maps-v2-how-to-add-marker-with-multiline-snippet
 *
 * @author Daniel Jönsson
 * @see AppCompatActivity
 * @see OnMapReadyCallback
 * @see GoogleMap.InfoWindowAdapter
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    /* Instance field variables */

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var marker: Marker? = null
    private lateinit var currentPosition: Location
    private lateinit var bathingSites: List<BathingSite>
    private lateinit var appDatabase: AppDatabase
    private lateinit var dbHelper: DatabaseHelperImpl
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var markedSites: MutableMap<Marker, BathingSite> = mutableMapOf()
    private var circle: Circle? = null
    private val locationPermissionRequestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){ permission ->
        when {
            permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                setupLocationListener()
            }
            permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                makePermissionSnackbar()
            } else -> {
            makePermissionSnackbar()
        }
        }
    }

    /**
     * The onCreate function is called when the activity lifecycle has just started. It is used to
     * get the map and sets up a location request to be able to update the users location if
     * the user traveled at least 50 meters since last time. It also sets up the database and fetches
     * all the bathing sites.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationRequest = LocationRequest.Builder(5).setMinUpdateDistanceMeters(50F).setPriority(Priority.PRIORITY_HIGH_ACCURACY).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                if (p0.lastLocation != currentPosition){
                    p0.lastLocation?.let { changeLocation(it) }
                }
                super.onLocationResult(p0)
            }
        }
        appDatabase = DatabaseBuilder.getInstance(this)
        dbHelper = DatabaseHelperImpl(appDatabase)
        lifecycleScope.launch(Dispatchers.IO) {
            bathingSites = dbHelper.getBathingSites()
        }
        setupLocationListener()
    }

    /**
     * Hook called when the map is ready.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setInfoWindowAdapter(this)
        setMarkerOnClickListener(googleMap)
    }

    /**
     * Hook called when the activity is paused but still visible in the background. In that case the
     * location updates should be stopped.
     */
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    /**
     * Hook called when the activity resumes from a paused state, in that case the location updates
     * should start again.
     */
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    /**
     * Sets up a location listener, first asks for permission to access the users location if the
     * user has not yet already granted the permission.
     */
    private fun setupLocationListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                currentPosition = it
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            locationPermissionRequestPermission.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    /**
     * Changes the current location to the new location provided as parameter. Also changes the bounds
     * of the camera to fit the circle to be drawn using SphericalUtil. The function also calls a
     * method for updating the circle and getting the nearby bathing sites of the new location.
     */
    private fun changeLocation(location: Location) {
        currentPosition = location
        marker?.remove()
        val radius = getPreferredDistance()
        val latLngBounds = LatLngBounds.Builder()
            .include(SphericalUtil.computeOffset(LatLng(currentPosition.latitude, currentPosition.longitude), radius, 0.0))
            .include(SphericalUtil.computeOffset(LatLng(currentPosition.latitude, currentPosition.longitude), radius, 90.0))
            .include(SphericalUtil.computeOffset(LatLng(currentPosition.latitude, currentPosition.longitude), radius, 180.0))
            .include(SphericalUtil.computeOffset(LatLng(currentPosition.latitude, currentPosition.longitude), radius, 270.0))
            .build()

        val latLng = LatLng(currentPosition.latitude, currentPosition.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("Current position")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        marker = mMap.addMarker(markerOptions)!!
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 25))
        updateCircle(latLng)
        getNearbyBathingSites()
    }

    /**
     * function to filter the bathing sites to only get the bathingsites within the preferred distance
     * of the circle.
     */
    private fun getNearbyBathingSites() {
        var siteLatLng: LatLng
        var distance: Double
        var bathingSites: List<BathingSite>
        lifecycleScope.launch(Dispatchers.IO) {
            bathingSites = this@MapsActivity.bathingSites.toMutableList().filter { bathingSite ->
                siteLatLng = LatLng(bathingSite.latitude.toDouble(), bathingSite.longitude.toDouble())
                distance = calulateDistance(siteLatLng)
                distance <= getPreferredDistance()
            }
            markSites(bathingSites)
        }
    }

    /**
     * Removes markers which is not in the preffered distance anymore and adds sites which is inside
     * the distance and is not yet showing.
     */
    private suspend fun markSites(bathingSites: List<BathingSite>) {
        withContext(Dispatchers.Main) {
            val existingMarkers = markedSites.keys.toMutableList()
            existingMarkers.forEach { marker ->
                val bathingSite = markedSites[marker]
                if (bathingSite !in bathingSites) {
                    marker.remove()
                    markedSites.remove(marker, bathingSite)
                }
            }

            bathingSites.forEach { site ->
                if (site !in markedSites.values) {
                    val lat = site.latitude.toDouble()
                    val long = site.longitude.toDouble()
                    val latLng = LatLng(lat, long)
                    val markerOptions = MarkerOptions()
                        .position(latLng)
                        .title(site.name)
                        .visible(true)
                    val newMarker = mMap.addMarker(markerOptions)
                    markedSites[newMarker!!] = site
                }
            }
        }
    }

    /**
     * Updates the circle, removes the old circle and draws a new one.
     */
    private fun updateCircle(latLng: LatLng) {
            circle?.remove()
            val circleOptions = CircleOptions()
            circleOptions.center(latLng)
            circleOptions.radius(getPreferredDistance())
            circleOptions.strokeColor(getColor(R.color.primary))
            circleOptions.fillColor(getColor(R.color.accent))
            circle = mMap.addCircle(circleOptions)
    }

    /**
     * Sets up a click listener on each marker and display the information about the bathing sites which
     * the marker is pointing to.
     */
    private fun setMarkerOnClickListener(googleMap: GoogleMap) {
        googleMap.setOnMarkerClickListener(GoogleMap.OnMarkerClickListener { marker ->
            val bathingSite = markedSites[marker]
            if (bathingSite != null) showSiteDialog(bathingSite, marker) else return@OnMarkerClickListener false
            return@OnMarkerClickListener false
        })
    }

    /**
     * A way to compute the distance between current position and the bathing site. Returns distance
     * in meters. Utilizing the SphericalUtil to calculate the distance in a straight line between
     * two points.
     */
    private fun calulateDistance(siteLatLng: LatLng) : Double {
        val currentLocationLatLng = LatLng(currentPosition.latitude, currentPosition.longitude)
        return SphericalUtil.computeDistanceBetween(currentLocationLatLng, siteLatLng)
    }

    /**
     * Called when a user clicks on a marker, accepts two parameters which is the marker clicked
     * along with its associated bathing site.
     */
    private fun showSiteDialog(bathingSite: BathingSite, marker: Marker) {
        val bathingSiteLatLng = LatLng(bathingSite.latitude.toDouble(), bathingSite.longitude.toDouble())
        val distanceFromCurrentPos = calulateDistance(bathingSiteLatLng)/1000
        val roundedDistance = String.format("%.2f", distanceFromCurrentPos)
        val msg = """
            Address: ${bathingSite.address}
            Description: ${bathingSite.desc}
            Lat: ${bathingSite.latitude}
            Long: ${bathingSite.longitude}
            Water temp: ${bathingSite.waterTemp}
            Date for water temp: ${bathingSite.dateForTemp}
            Rating: ${bathingSite.grade}
            Distance: $roundedDistance${"km"}
        """.trimIndent()
        marker.snippet = msg
    }

    /**
     * Stops location updates.
     */
    private fun stopLocationUpdates() {
        locationCallback.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }

    /**
     * Resumes location updates.
     */
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationCallback.let { fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()) }
        }
    }

    /**
     * Gets the current preferred search distance (in km) and multiplies it by 1000 to represent it
     * in meters.
     */
    private fun getPreferredDistance() : Double {
        return (SettingsActivity.getSearchDistance(this) * 1000)
    }

    /**
     * Used to customize the marker content and to display multiple lines of snippet.
     */
    override fun getInfoContents(p0: Marker): View {
        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        val title = TextView(this)
        title.setTextColor(Color.BLACK)
        title.gravity = Gravity.CENTER
        title.typeface = Typeface.DEFAULT_BOLD
        title.textSize = 20F
        title.text = p0.title
        infoLayout.addView(title)
        if (p0.snippet != null) {
            val snippet = TextView(this)
            snippet.setTextColor(Color.GRAY)
            snippet.textSize = 16F
            snippet.text = p0.snippet
            infoLayout.addView(snippet)
        }
        return infoLayout
    }

    /**
     * Window to show marker info.
     */
    override fun getInfoWindow(p0: Marker): View? {
        return null
    }

    /**
     * Snackbar to notify user that the precise location is needed for this activity. Uses a callback
     * to close the activity once the snackbar dismisses.
     */
    private fun makePermissionSnackbar() {
        Snackbar.make(binding.root, getString(R.string.permission_message), Snackbar.LENGTH_LONG).addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                finish()
            }
        }).show()
    }
}