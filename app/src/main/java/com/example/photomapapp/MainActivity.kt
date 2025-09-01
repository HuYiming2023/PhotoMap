package com.example.photomapapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import androidx.exifinterface.media.ExifInterface
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException
import java.io.InputStream
import kotlin.collections.ArrayList

/**
 * Main Activity for the Photo Map application.
 * Handles photo selection, location retrieval, and displaying photos on a Google Map.
 */
class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // --- Variables and Constants ---
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userCurrentLocation: LatLng? = null
    private val TAG = "PhotoMapApp"
    private val MARKER_ZOOM_LEVEL = 18f
    private val allMarkers = mutableListOf<Marker>()

    // LocationRequest and LocationCallback for requesting new location updates
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // --- Permission Launchers and Photo Picker ---
    // Location permission launcher
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableMyLocation()
                getUserLocation()
            } else {
                Toast.makeText(this, "Access to location is required to use the positioning feature.", Toast.LENGTH_SHORT).show()
            }
        }

    // Photo permission launcher
    private val requestPhotoPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchPhotoPicker()
            } else {
                Toast.makeText(this, "Access to photos is required to read photo data.", Toast.LENGTH_SHORT).show()
            }
        }

    // Photo picker launcher
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uris = mutableListOf<Uri>()
                result.data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } ?: result.data?.data?.let { uri ->
                    uris.add(uri)
                }

                if (uris.isNotEmpty()) {
                    processPhotos(uris)
                }
            }
        }

    // --- Lifecycle Methods ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val selectPhotoButton = findViewById<Button>(R.id.select_photo_button)
        selectPhotoButton.setOnClickListener {
            checkAndRequestPhotoPermission()
        }

        val fabReturnToOverview = findViewById<FloatingActionButton>(R.id.fab_return_to_overview)
        fabReturnToOverview.setOnClickListener {
            updateMapZoomToShowAllPhotos()
            it.visibility = View.GONE
        }

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    userCurrentLocation = LatLng(location.latitude, location.longitude)
                    Log.d(TAG, "Got a fresh location update: ${userCurrentLocation?.latitude}, ${userCurrentLocation?.longitude}")
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMarkerClickListener(this)
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter())

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = true
        googleMap.uiSettings.isTiltGesturesEnabled = true

        checkAndRequestLocationPermission()

        Log.d(TAG, "Map is ready")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Animate camera to the clicked marker
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, MARKER_ZOOM_LEVEL))

        // Show the return to overview button
        findViewById<FloatingActionButton>(R.id.fab_return_to_overview).visibility = View.VISIBLE

        // Get photo URI from marker tag and show dialog
        val photoUri = marker.tag as? Uri
        photoUri?.let { uri ->
            val builder = AlertDialog.Builder(this)
            val inflater = this.layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_photo, null)
            val photoView = dialogView.findViewById<PhotoView>(R.id.dialog_photo_view)

            try {
                photoView.setImageURI(uri)
                builder.setView(dialogView)
                val dialog = builder.create()
                dialog.show()
                // Return false here so that the custom info window will be displayed
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to display photo in dialog: ${e.message}")
                Toast.makeText(this, "Unable to display photo.", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun checkAndRequestPhotoPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                launchPhotoPicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Access to photos is required to read photo GPS data.", Toast.LENGTH_LONG).show()
                requestPhotoPermissionLauncher.launch(permission)
            }
            else -> {
                requestPhotoPermissionLauncher.launch(permission)
            }
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
            getUserLocation()
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableMyLocation() {
        try {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted.", e)
        }
    }

    private fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userCurrentLocation = LatLng(location.latitude, location.longitude)
                    Log.d(TAG, "Got last known user location: ${userCurrentLocation?.latitude}, ${userCurrentLocation?.longitude}")
                } else {
                    Log.e(TAG, "Last location is null. Requesting new location updates.")
                    requestNewLocationUpdates()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted.", e)
        }
    }

    private fun requestNewLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted for new updates.", e)
        }
    }

    private fun launchPhotoPicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        photoPickerLauncher.launch(intent)
    }

    private fun processPhotos(uris: List<Uri>) {
        for (uri in uris) {
            var inputStream: InputStream? = null
            try {
                inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open input stream for URI: $uri")
                    continue
                }

                val exifInterface = ExifInterface(inputStream)
                val latLong = exifInterface.latLong

                if (latLong != null) {
                    val photoLocation = LatLng(latLong[0], latLong[1])
                    addMarkerToMap(uri, photoLocation)
                } else {
                    handlePhotoWithoutGps(uri)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error processing photo: ${e.message}")
                Toast.makeText(this, "Error processing photo.", Toast.LENGTH_SHORT).show()
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing stream: ${e.message}")
                }
            }
        }

        updateMapZoomToShowAllPhotos()
    }

    private fun handlePhotoWithoutGps(uri: Uri) {
        val photoName = getFileNameFromUri(uri)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Photo has no GPS data")
            .setMessage("\"$photoName\" has no GPS data. Would you like to use your current location for the photo's location?")
            .setPositiveButton("Yes") { _, _ ->
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val newLocation = LatLng(location.latitude, location.longitude)
                            addMarkerToMap(uri, newLocation)
                            updateMapZoomToShowAllPhotos()
                        } else {
                            Toast.makeText(this, "Unable to get your current location, the photo could not be added.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun addMarkerToMap(uri: Uri, location: LatLng) {
        val photoName = getFileNameFromUri(uri)
        var marker: Marker? = null
        try {
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)

            marker = googleMap.addMarker(MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))

            marker?.tag = uri
            marker?.let {
                it.title = photoName
                allMarkers.add(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create marker from URI: ${e.message}")
            Toast.makeText(this, "Could not create marker for a photo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result = "Unknown"
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        return result
    }

    private fun updateMapZoomToShowAllPhotos() {
        if (allMarkers.isEmpty()) {
            return
        }

        val builder = LatLngBounds.Builder()
        allMarkers.forEach { marker ->
            builder.include(marker.position)
        }

        val bounds = builder.build()
        val padding = 100
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        googleMap.animateCamera(cameraUpdate)
    }

    inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val window = LayoutInflater.from(this@MainActivity).inflate(R.layout.custom_marker_info_window, null)

        override fun getInfoWindow(marker: Marker): View? {
            renderWindowText(marker, window)
            return window
        }

        override fun getInfoContents(marker: Marker): View? {
            return null
        }

        private fun renderWindowText(marker: Marker, view: View) {
            val titleTextView = view.findViewById<TextView>(R.id.marker_title)
            val title = marker.title
            if (title != null) {
                titleTextView.text = title
            }
        }
    }
}