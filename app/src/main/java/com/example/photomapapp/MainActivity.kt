//package com.example.photomapapp
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.provider.OpenableColumns
//import android.util.Log
//import android.widget.Button
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.OnMapReadyCallback
//import com.google.android.gms.maps.SupportMapFragment
//import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.LatLngBounds
//import com.google.android.gms.maps.model.Marker
//import com.google.android.gms.maps.model.MarkerOptions
//import androidx.exifinterface.media.ExifInterface
//import com.github.chrisbanes.photoview.PhotoView
//import java.io.IOException
//import java.io.InputStream
//
//class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
//
//    private lateinit var googleMap: GoogleMap
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private var userCurrentLocation: LatLng? = null
//    private val TAG = "PhotoMapApp"
//    private val MARKER_ZOOM_LEVEL = 18f
//
//    // LocationRequest and LocationCallback for requesting new location updates
//    private lateinit var locationRequest: LocationRequest
//    private lateinit var locationCallback: LocationCallback
//
//    // Location permission launcher
//    private val requestLocationPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                enableMyLocation()
//                getUserLocation() // Get location after permission is granted
//            } else {
//                Toast.makeText(this, "Access to location is required to use the positioning feature.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    // Photo permission launcher
//    private val requestPhotoPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                launchPhotoPicker()
//            } else {
//                Toast.makeText(this, "Access to photos is required to read photo data.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    // Photo picker launcher
//    private val photoPickerLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                val uris = mutableListOf<Uri>()
//                result.data?.clipData?.let { clipData ->
//                    for (i in 0 until clipData.itemCount) {
//                        uris.add(clipData.getItemAt(i).uri)
//                    }
//                } ?: result.data?.data?.let { uri ->
//                    uris.add(uri)
//                }
//
//                if (uris.isNotEmpty()) {
//                    processPhotos(uris)
//                }
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        val mapFragment = supportFragmentManager
//            .findFragmentById(R.id.map_fragment) as SupportMapFragment
//        mapFragment.getMapAsync(this)
//
//        val selectPhotoButton = findViewById<Button>(R.id.select_photo_button)
//        selectPhotoButton.setOnClickListener {
//            checkAndRequestPhotoPermission()
//        }
//
//        // Initialize LocationRequest and LocationCallback
//        locationRequest = LocationRequest.create().apply {
//            interval = 10000 // 10 seconds
//            fastestInterval = 5000 // 5 seconds
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                locationResult.lastLocation?.let { location ->
//                    userCurrentLocation = LatLng(location.latitude, location.longitude)
//                    Log.d(TAG, "Got a fresh location update: ${userCurrentLocation?.latitude}, ${userCurrentLocation?.longitude}")
//                }
//            }
//        }
//    }
//
//    override fun onMapReady(map: GoogleMap) {
//        googleMap = map
//        googleMap.setOnMarkerClickListener(this)
//
//        googleMap.uiSettings.isZoomControlsEnabled = true
//        googleMap.uiSettings.isScrollGesturesEnabled = true
//        googleMap.uiSettings.isZoomGesturesEnabled = true
//        googleMap.uiSettings.isCompassEnabled = true
//
//        checkAndRequestLocationPermission()
//
//        Log.d(TAG, "Map is ready")
//    }
//
//    override fun onMarkerClick(marker: Marker): Boolean {
//        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, MARKER_ZOOM_LEVEL))
//
//        val photoUri = marker.tag as? Uri
//        photoUri?.let { uri ->
//            val builder = AlertDialog.Builder(this)
//            val inflater = this.layoutInflater
//            val dialogView = inflater.inflate(R.layout.dialog_photo, null)
//            val photoView = dialogView.findViewById<PhotoView>(R.id.dialog_photo_view)
//
//            try {
//                photoView.setImageURI(uri)
//                builder.setView(dialogView)
//                val dialog = builder.create()
//                dialog.show()
//                return true
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to display photo in dialog: ${e.message}")
//                Toast.makeText(this, "Unable to display photo.", Toast.LENGTH_SHORT).show()
//            }
//        }
//        return false
//    }
//
//    private fun checkAndRequestPhotoPermission() {
//        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            Manifest.permission.READ_MEDIA_IMAGES
//        } else {
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        }
//
//        when {
//            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
//                launchPhotoPicker()
//            }
//            shouldShowRequestPermissionRationale(permission) -> {
//                Toast.makeText(this, "Access to photos is required to read photo GPS data.", Toast.LENGTH_LONG).show()
//                requestPhotoPermissionLauncher.launch(permission)
//            }
//            else -> {
//                requestPhotoPermissionLauncher.launch(permission)
//            }
//        }
//    }
//
//    private fun checkAndRequestLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            enableMyLocation()
//            getUserLocation()
//        } else {
//            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//        }
//    }
//
//    private fun enableMyLocation() {
//        try {
//            googleMap.isMyLocationEnabled = true
//            googleMap.uiSettings.isMyLocationButtonEnabled = true
//        } catch (e: SecurityException) {
//            Log.e(TAG, "Location permission not granted.", e)
//        }
//    }
//
//    private fun getUserLocation() {
//        try {
//            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//                if (location != null) {
//                    userCurrentLocation = LatLng(location.latitude, location.longitude)
//                    Log.d(TAG, "Got last known user location: ${userCurrentLocation?.latitude}, ${userCurrentLocation?.longitude}")
//                } else {
//                    Log.e(TAG, "Last location is null. Requesting new location updates.")
//                    requestNewLocationUpdates()
//                }
//            }
//        } catch (e: SecurityException) {
//            Log.e(TAG, "Location permission not granted.", e)
//        }
//    }
//
//    // New method: Request real-time location updates
//    private fun requestNewLocationUpdates() {
//        try {
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
//        } catch (e: SecurityException) {
//            Log.e(TAG, "Location permission not granted for new updates.", e)
//        }
//    }
//
//    // Stop location updates when the Activity is paused to save battery
//    override fun onPause() {
//        super.onPause()
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//    }
//
//    private fun launchPhotoPicker() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//        photoPickerLauncher.launch(intent)
//    }
//
//    private fun processPhotos(uris: List<Uri>) {
//        googleMap.clear()
//        val builder = LatLngBounds.Builder()
//        var markerCount = 0
//
//        for (uri in uris) {
//            var inputStream: InputStream? = null
//            try {
//                inputStream = contentResolver.openInputStream(uri)
//                if (inputStream == null) {
//                    Log.e(TAG, "Could not open input stream for URI: $uri")
//                    continue
//                }
//
//                val exifInterface = ExifInterface(inputStream)
//                val latLong = exifInterface.latLong
//
//                if (latLong != null) {
//                    val photoLocation = LatLng(latLong[0], latLong[1])
//                    addMarkerToMap(uri, photoLocation)
//                    builder.include(photoLocation)
//                    markerCount++
//                } else {
//                    handlePhotoWithoutGps(uri, builder, markerCount)
//                }
//            } catch (e: IOException) {
//                Log.e(TAG, "Error processing photo: ${e.message}")
//                Toast.makeText(this, "Error processing photo.", Toast.LENGTH_SHORT).show()
//            } finally {
//                try {
//                    inputStream?.close()
//                } catch (e: IOException) {
//                    Log.e(TAG, "Error closing stream: ${e.message}")
//                }
//            }
//        }
//
//        // Final map adjustment after all photos have been processed
//        if (markerCount > 0) {
//            val bounds = builder.build()
//            val padding = 100
//            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
//            googleMap.animateCamera(cameraUpdate)
//        }
//    }
//
//    private fun handlePhotoWithoutGps(uri: Uri, builder: LatLngBounds.Builder, markerCount: Int) {
//        val photoName = getFileNameFromUri(uri)
//        val dialog = AlertDialog.Builder(this)
//            .setTitle("Photo has no GPS data")
//            .setMessage("\"$photoName\" has no GPS data. Would you like to use your current location for the photo's location?")
//            .setPositiveButton("Yes") { _, _ ->
//                // When the user clicks "Yes", re-request the location to ensure it's the latest data
//                try {
//                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//                        if (location != null) {
//                            val newLocation = LatLng(location.latitude, location.longitude)
//                            addMarkerToMap(uri, newLocation)
//                            builder.include(newLocation)
//                            // We don't need to update markerCount here as it's a local variable.
//                        } else {
//                            Toast.makeText(this, "Unable to get your current location, the photo could not be added.", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                } catch (e: SecurityException) {
//                    Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("No") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .create()
//        dialog.show()
//    }
//
//    private fun addMarkerToMap(uri: Uri, location: LatLng) {
//        val photoName = getFileNameFromUri(uri)
//        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
//        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)
//
//        val marker = googleMap.addMarker(MarkerOptions()
//            .position(location)
//            .title(photoName)
//            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))
//
//        marker?.tag = uri
//    }
//
//    private fun getFileNameFromUri(uri: Uri): String {
//        var result = "Unknown"
//        if (uri.scheme == "content") {
//            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
//                if (cursor.moveToFirst()) {
//                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                    if (nameIndex != -1) {
//                        result = cursor.getString(nameIndex)
//                    }
//                }
//            }
//        }
//        return result
//    }
//}

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
import android.widget.Button
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
import java.io.IOException
import java.io.InputStream

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

    // LocationRequest and LocationCallback for requesting new location updates
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // --- Permission Launchers and Photo Picker ---
    // Location permission launcher
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableMyLocation()
                getUserLocation() // Get location after permission is granted
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

        // Initialize LocationRequest and LocationCallback for location updates
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
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

        // Configure map UI settings
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isCompassEnabled = true

        checkAndRequestLocationPermission()

        Log.d(TAG, "Map is ready")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Animate camera to the clicked marker
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, MARKER_ZOOM_LEVEL))

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
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to display photo in dialog: ${e.message}")
                Toast.makeText(this, "Unable to display photo.", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    // Stop location updates when the Activity is paused to save battery
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // --- Permission Handling and Location Methods ---
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

    // New method: Request real-time location updates
    private fun requestNewLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted for new updates.", e)
        }
    }

    // --- Photo Processing Methods ---
    private fun launchPhotoPicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        photoPickerLauncher.launch(intent)
    }

    private fun processPhotos(uris: List<Uri>) {
        googleMap.clear()
        val builder = LatLngBounds.Builder()
        var markerCount = 0

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
                    builder.include(photoLocation)
                    markerCount++
                } else {
                    handlePhotoWithoutGps(uri, builder, markerCount)
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

        // Final map adjustment after all photos have been processed
        if (markerCount > 0) {
            val bounds = builder.build()
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)
        }
    }

    private fun handlePhotoWithoutGps(uri: Uri, builder: LatLngBounds.Builder, markerCount: Int) {
        val photoName = getFileNameFromUri(uri)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Photo has no GPS data")
            .setMessage("\"$photoName\" has no GPS data. Would you like to use your current location for the photo's location?")
            .setPositiveButton("Yes") { _, _ ->
                // When the user clicks "Yes", re-request the location to ensure it's the latest data
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val newLocation = LatLng(location.latitude, location.longitude)
                            addMarkerToMap(uri, newLocation)
                            builder.include(newLocation)
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
        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)

        val marker = googleMap.addMarker(MarkerOptions()
            .position(location)
            .title(photoName)
            .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))

        marker?.tag = uri
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
}