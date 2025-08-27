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
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
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
//import java.io.IOException
//import java.io.InputStream
//
//class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
//
//    private lateinit var googleMap: GoogleMap
//    private val TAG = "PhotoMapApp"
//    private val MARKER_ZOOM_LEVEL = 18f
//
//    // Request permissions launcher to handle permission results
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                launchPhotoPicker()
//            } else {
//                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//    // Photo picker launcher to handle photo selection results
//    private val photoPickerLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                // Handle multiple photos
//                val uris = mutableListOf<Uri>()
//                result.data?.clipData?.let { clipData ->
//                    for (i in 0 until clipData.itemCount) {
//                        uris.add(clipData.getItemAt(i).uri)
//                    }
//                } ?: result.data?.data?.let { uri ->
//                    // Handle single photo
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
//        val mapFragment = supportFragmentManager
//            .findFragmentById(R.id.map_fragment) as SupportMapFragment
//        mapFragment.getMapAsync(this)
//
//        val selectPhotoButton = findViewById<Button>(R.id.select_photo_button)
//        selectPhotoButton.setOnClickListener {
//            checkAndRequestPermission()
//        }
//    }
//
//    override fun onMapReady(map: GoogleMap) {
//        googleMap = map
//        googleMap.setOnMarkerClickListener(this)
//
//        // This makes the home screen map fully interactive
//        googleMap.uiSettings.isZoomControlsEnabled = true
//        googleMap.uiSettings.isScrollGesturesEnabled = true
//        googleMap.uiSettings.isZoomGesturesEnabled = true
//
//        Log.d(TAG, "Map is ready")
//    }
//
//    override fun onMarkerClick(marker: Marker): Boolean {
//        // When a marker is clicked, zoom in to its exact location
//        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, MARKER_ZOOM_LEVEL))
//        marker.showInfoWindow()
//        return true
//    }
//
//    private fun checkAndRequestPermission() {
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
//                Toast.makeText(this, "需要访问照片权限才能读取照片的 GPS 数据。", Toast.LENGTH_LONG).show()
//                requestPermissionLauncher.launch(permission)
//            }
//            else -> {
//                requestPermissionLauncher.launch(permission)
//            }
//        }
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
//
//        // Use LatLngBounds.Builder to build a bounding box around all markers
//        val builder = LatLngBounds.Builder()
//        var markerCount = 0
//
//        for (uri in uris) {
//            var inputStream: InputStream? = null
//            try {
//                inputStream = contentResolver.openInputStream(uri)
//                if (inputStream == null) {
//                    Log.e(TAG, "Could not open input stream for URI: $uri")
//                    Toast.makeText(this, "无法读取照片。", Toast.LENGTH_SHORT).show()
//                    continue
//                }
//
//                val exifInterface = ExifInterface(inputStream)
//                val latLong = exifInterface.latLong
//                val photoName = getFileNameFromUri(uri)
//
//                if (latLong != null) {
//                    val photoLocation = LatLng(latLong[0], latLong[1])
//
//                    // Add this photo's location to the bounds builder
//                    builder.include(photoLocation)
//                    markerCount++
//
//                    // Read the image into a Bitmap and scale it
//                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
//                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)
//
//                    // Add a marker with the photo as the icon and name as the title
//                    googleMap.addMarker(MarkerOptions()
//                        .position(photoLocation)
//                        .title(photoName)
//                        .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))
//
//                    Log.d(TAG, "Photo location: ${latLong[0]}, ${latLong[1]}")
//
//                } else {
//                    Toast.makeText(this, "“$photoName”没有找到 GPS 数据。", Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, "No GPS data found for URI: $uri")
//                }
//            } catch (e: IOException) {
//                Log.e(TAG, "Error processing photo: ${e.message}")
//                Toast.makeText(this, "处理照片时出错。", Toast.LENGTH_SHORT).show()
//            } finally {
//                try {
//                    inputStream?.close()
//                } catch (e: IOException) {
//                    Log.e(TAG, "Error closing stream: ${e.message}")
//                }
//            }
//        }
//
//        // After processing all photos, adjust the map zoom
//        if (markerCount > 0) {
//            val bounds = builder.build()
//            val padding = 100 // Padding in pixels to ensure markers are not on the edge
//            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
//            googleMap.animateCamera(cameraUpdate)
//        }
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val TAG = "PhotoMapApp"
    private val MARKER_ZOOM_LEVEL = 18f
    private val photoMarkers = mutableListOf<PhotoMarker>()
    private val FILE_NAME = "photo_markers.json"

    // Request permissions for photos and location
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[getPhotoPermission()] == true && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    // Both permissions granted
                    launchPhotoPicker()
                    enableMyLocation()
                }
                permissions[getPhotoPermission()] == true -> {
                    // Only photo permission granted
                    launchPhotoPicker()
                }
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    // Only location permission granted
                    enableMyLocation()
                }
                else -> {
                    Toast.makeText(this, "Permissions were denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Photo picker launcher
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Handle multiple photos
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val selectPhotoButton = findViewById<Button>(R.id.select_photo_button)
        selectPhotoButton.setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMarkerClickListener(this)

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isCompassEnabled = true

        checkAndRequestPermissions()
        loadMarkersFromFile()
        displayMarkersOnMap()
        adjustCameraToMarkers()

        Log.d(TAG, "Map is ready")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, MARKER_ZOOM_LEVEL))
        marker.showInfoWindow()
        return true
    }

    private fun checkAndRequestPermissions() {
        val photoPermission = getPhotoPermission()
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

        val permissions = arrayOf(photoPermission, locationPermission)
        permissionLauncher.launch(permissions)
    }

    private fun getPhotoPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
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
                val photoName = getFileNameFromUri(uri)

                if (latLong != null) {
                    val photoLocation = LatLng(latLong[0], latLong[1])
                    addMarkerToList(photoLocation, photoName, uri)
                } else {
                    promptForLocation(uri, photoName)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error processing photo: ${e.message}")
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing stream: ${e.message}")
                }
            }
        }
        saveMarkersToFile()
        displayMarkersOnMap()
        adjustCameraToMarkers()
    }

    private fun promptForLocation(uri: Uri, photoName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("GPS数据缺失")
        builder.setMessage("照片“$photoName”没有找到GPS数据。你想使用当前位置吗？")
        builder.setPositiveButton("是") { _, _ ->
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val photoLocation = LatLng(location.latitude, location.longitude)
                        addMarkerToList(photoLocation, photoName, uri)
                        saveMarkersToFile()
                        displayMarkersOnMap()
                        adjustCameraToMarkers()
                    } else {
                        Toast.makeText(this, "无法获取当前位置。", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "未授予位置权限。", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("否") { _, _ ->
            Toast.makeText(this, "照片已被忽略。", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun addMarkerToList(location: LatLng, name: String, uri: Uri) {
        val marker = PhotoMarker(location.latitude, location.longitude, name, uri.toString())
        photoMarkers.add(marker)
        Log.d(TAG, "Added marker: $marker")
    }

    private fun displayMarkersOnMap() {
        googleMap.clear()
        for (marker in photoMarkers) {
            val location = LatLng(marker.latitude, marker.longitude)
            val uri = Uri.parse(marker.uri)

            var bitmap: Bitmap? = null
            try {
                bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            } catch (e: IOException) {
                Log.e(TAG, "Error decoding bitmap: ${e.message}")
            }

            bitmap?.let {
                val resizedBitmap = Bitmap.createScaledBitmap(it, 120, 120, false)
                googleMap.addMarker(MarkerOptions()
                    .position(location)
                    .title(marker.name)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))
            }
        }
    }

    private fun adjustCameraToMarkers() {
        if (photoMarkers.isEmpty()) {
            return
        }
        val builder = LatLngBounds.Builder()
        for (marker in photoMarkers) {
            builder.include(LatLng(marker.latitude, marker.longitude))
        }
        val bounds = builder.build()
        val padding = 150
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        googleMap.animateCamera(cameraUpdate)
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

    private fun saveMarkersToFile() {
        val jsonString = Gson().toJson(photoMarkers)
        try {
            openFileOutput(FILE_NAME, MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving markers: ${e.message}")
        }
    }

    private fun loadMarkersFromFile() {
        val file = File(filesDir, FILE_NAME)
        if (file.exists()) {
            try {
                val jsonString = openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
                val type = object : TypeToken<MutableList<PhotoMarker>>() {}.type
                photoMarkers.addAll(Gson().fromJson(jsonString, type))
            } catch (e: IOException) {
                Log.e(TAG, "Error loading markers: ${e.message}")
            }
        }
    }
}

// Data class to save marker information
data class PhotoMarker(val latitude: Double, val longitude: Double, val name: String, val uri: String)