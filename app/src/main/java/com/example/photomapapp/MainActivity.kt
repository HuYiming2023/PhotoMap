package com.example.photomapapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val TAG = "PhotoMapApp"

    // Request permissions launcher to handle permission results
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchPhotoPicker()
            } else {
                Toast.makeText(this, "Permission denied to read photos.", Toast.LENGTH_SHORT).show()
            }
        }

    // Photo picker launcher to handle photo selection results
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    processPhoto(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Find the button and set the click listener
        val selectPhotoButton = findViewById<Button>(R.id.select_photo_button)
        selectPhotoButton.setOnClickListener {
            checkAndRequestPermission()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d(TAG, "Map is ready")
        // Optional: Set a default camera position to show something on the map initially
        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
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
                Toast.makeText(this, "This app needs photo access to read GPS data from images.", Toast.LENGTH_LONG).show()
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
        photoPickerLauncher.launch(intent)
    }

    private fun processPhoto(uri: Uri) {
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                Toast.makeText(this, "Could not read photo.", Toast.LENGTH_SHORT).show()
                return
            }

            val exifInterface = ExifInterface(inputStream)
            val latLong = exifInterface.latLong

            if (latLong != null) {
                val latitude = latLong[0]
                val longitude = latLong[1]

                val photoLocation = LatLng(latitude, longitude)

                // Add a marker on the map at the photo's location
                googleMap.addMarker(MarkerOptions().position(photoLocation).title("Photo Location"))
                // Move the camera to the photo's location and zoom in
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(photoLocation, 15f))

                Toast.makeText(this, "Found photo location: $latitude, $longitude", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Photo location: $latitude, $longitude")

            } else {
                Toast.makeText(this, "No GPS data found in this photo.", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "No GPS data found for URI: $uri")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Error processing photo: ${e.message}")
            Toast.makeText(this, "Error processing photo.", Toast.LENGTH_SHORT).show()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing input stream: ${e.message}")
            }
        }
    }
}