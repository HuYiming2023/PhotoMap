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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var googleMap: GoogleMap
    private val TAG = "PhotoMapApp"
    private val MARKER_ZOOM_LEVEL = 18f

    // Request permissions launcher to handle permission results
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchPhotoPicker()
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    // Photo picker launcher to handle photo selection results
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
                    // Handle single photo
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

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val selectPhotoButton = findViewById<Button>(R.id.select_photo_button)
        selectPhotoButton.setOnClickListener {
            checkAndRequestPermission()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setOnMarkerClickListener(this)

        // This makes the home screen map fully interactive
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        Log.d(TAG, "Map is ready")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // When a marker is clicked, zoom in to its exact location
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, MARKER_ZOOM_LEVEL))
        marker.showInfoWindow()
        return true
    }

    private fun checkAndRequestPermission() {
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
                Toast.makeText(this, "需要访问照片权限才能读取照片的 GPS 数据。", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun launchPhotoPicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        photoPickerLauncher.launch(intent)
    }

    private fun processPhotos(uris: List<Uri>) {
        googleMap.clear()

        // Use LatLngBounds.Builder to build a bounding box around all markers
        val builder = LatLngBounds.Builder()
        var markerCount = 0

        for (uri in uris) {
            var inputStream: InputStream? = null
            try {
                inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open input stream for URI: $uri")
                    Toast.makeText(this, "无法读取照片。", Toast.LENGTH_SHORT).show()
                    continue
                }

                val exifInterface = ExifInterface(inputStream)
                val latLong = exifInterface.latLong
                val photoName = getFileNameFromUri(uri)

                if (latLong != null) {
                    val photoLocation = LatLng(latLong[0], latLong[1])

                    // Add this photo's location to the bounds builder
                    builder.include(photoLocation)
                    markerCount++

                    // Read the image into a Bitmap and scale it
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false)

                    // Add a marker with the photo as the icon and name as the title
                    googleMap.addMarker(MarkerOptions()
                        .position(photoLocation)
                        .title(photoName)
                        .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap)))

                    Log.d(TAG, "Photo location: ${latLong[0]}, ${latLong[1]}")

                } else {
                    Toast.makeText(this, "“$photoName”没有找到 GPS 数据。", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "No GPS data found for URI: $uri")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error processing photo: ${e.message}")
                Toast.makeText(this, "处理照片时出错。", Toast.LENGTH_SHORT).show()
            } finally {
                try {
                    inputStream?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing stream: ${e.message}")
                }
            }
        }

        // After processing all photos, adjust the map zoom
        if (markerCount > 0) {
            val bounds = builder.build()
            val padding = 100 // Padding in pixels to ensure markers are not on the edge
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap.animateCamera(cameraUpdate)
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
}