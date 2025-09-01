# Photo Map App

This Android application allows users to view photos on a Google Map based on their embedded GPS location data. It offers a simple and intuitive way to visualize a collection of geolocated images, such as travel photos or event pictures.

## Features
- **Photo Selection**: Select multiple photos from your device's gallery.
- **GPS Data Extraction**: Automatically reads GPS (latitude and longitude) information from photo metadata (EXIF data).
- **Map Integration**: Displays selected photos as markers on a Google Map.
- **Interactive Markers**: Tapping a photo marker animates the camera to zoom in on its location and displays a larger, zoomable version of the photo.
- **Cumulative Photo Display**: The app keeps track of all uploaded photos and automatically adjusts the map view to encompass all photo locations.
- **Location Handling**:
    - Requests and uses the device's current location to center the map initially.
    - If a photo lacks GPS data, it prompts the user to use their current location as the photo's location.
- **Dynamic Permissions**: Asks for necessary location and photo access permissions at runtime.
- **Photo Name Display**: When you tap a marker, a custom info window appears, showing the photo's filename for easy identification.
- **Return to Overview**: A floating action button allows you to instantly return to the "cumulative view" of all uploaded photos on the map.

---

## How It Works

1.  **Permissions**: When the app starts, it checks for and requests location and photo access permissions.
2.  **Map Initialization**: A Google Map fragment loads. Once ready, the app gets the user's current location for the initial view.
3.  **Photo Loading**: The user taps the **"Select Photo"** button to open the device's gallery and select photos.
4.  **Data Processing**: The app processes each photo, reading its **EXIF data** to find GPS coordinates. If found, a marker is placed on the map. If not, a dialog asks the user if they want to use their current location.
5.  **Marker Display**: Each photo is represented by a resized thumbnail as a marker on the map.
6.  **Marker Interaction**: Clicking a marker:
    - Animates the map camera to zoom in.
    - Opens a custom info window to display the photo's filename.
    - Displays a larger, zoomable version of the photo using the **PhotoView** library.
7.  **Map Controls**: The map supports standard gestures, including **pinch-to-zoom and mouse scroll wheel zoom**, for seamless navigation. A new "Return to Overview" button provides a one-tap solution to view all photo locations at once.

---

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
- A valid Google Maps API Key configured in `app/build.gradle.kts`.