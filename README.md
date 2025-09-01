# Photo Map App

This Android application allows users to view photos on a Google Map based on their embedded GPS location data. It provides a simple and intuitive way to visualize a collection of geolocated images, such as travel photos or event pictures.

## Features
- **Photo Selection**: Select multiple photos from your device's gallery.
- **GPS Data Extraction**: Automatically reads GPS (latitude and longitude) information from photo metadata (EXIF data).
- **Map Integration**: Displays selected photos as markers on a Google Map.
- **Interactive Markers**: Tapping a photo marker zooms in on its location and displays a larger, zoomable version of the photo.
- **Location Handling**:
    - Requests and uses the device's current location to center the map initially.
    - If a photo lacks GPS data, it prompts the user to use their current location as the photo's location.
- **Dynamic Permissions**: Asks for necessary location and photo access permissions at runtime.

## How It Works

1.  **Permissions**: When the app starts, it checks for and requests location and photo access permissions. These are essential for its core functionality.
2.  **Map Initialization**: The app loads a Google Map fragment. Once the map is ready, it attempts to get the user's current location to provide an initial view.
3.  **Photo Loading**: The user taps the **"Select Photo"** button to open the device's gallery. They can select one or more photos.
4.  **Data Processing**: The app processes each selected photo:
    - It reads the photo's **EXIF data** to find GPS coordinates.
    - If coordinates are found, a marker is placed on the map at that specific location.
    - If no GPS data is found, a dialog box asks the user if they want to use their current location for the photo.
5.  **Marker Display**: Each photo is represented by a resized thumbnail as a marker on the map, making it easy to see which photo corresponds to which location.
6.  **Marker Interaction**: Clicking a marker:
    - Animates the map camera to zoom in on the marker's location.
    - Opens an alert dialog to display a larger version of the photo, which can be panned and zoomed using the **PhotoView** library.

## Technical Stack
- **Platform**: Android
- **Language**: Kotlin
- **UI**: Android XML layouts
- **Mapping**: Google Maps SDK for Android
- **Location**: Google Play Services Location API (`FusedLocationProviderClient`)
- **Image Handling**: `ExifInterface` for reading EXIF data, `PhotoView` for interactive image display, and standard Android methods for bitmap manipulation.
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)

## Requirements
- Android Studio
- An Android device or emulator with Google Play Services installed.
- A valid Google Maps API Key configured in `app/build.gradle.kts` as `buildConfigField("String", "MAPS_API_KEY", "YOUR_API_KEY_HERE")`.