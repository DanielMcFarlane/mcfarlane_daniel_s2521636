# Traffic Scotland Mobile App
**Student Name:** Daniel McFarlane  
**Student ID:** S2521636  
**Module:** Mobile Platform Development MHI326841  
**Institution:** Glasgow Caledonian University

---

## Overview
An Android application built to consume and display live traffic and travel data from the official Traffic Scotland DATEX II XML feeds. The app parses real-time data to provide road users with up-to-date information on unplanned events (incidents) and current roadworks across the Scottish trunk road and motorway network.

Targeted for Android 8.0 (Oreo / API Level 26) and above.

## Key Features
- **Live XML Parsing:** Downloads and parses Traffic Scotland's Unplanned Events and Current Roadworks feeds natively using `XmlPullParser`.
- **Situation Dashboard:** Displays a list of all current incidents and roadworks, detailing the event type and general public comments.
- **Detailed Inspection:** Tap any situation to view in-depth metadata including impact, validity periods, direction and coordinates.
- **Colour-Coded Delay Indicators:** Roadworks feature visual badges indicating short, medium and long delay durations.
- **Road Search:** Filter situations by specific road names (e.g. "M8").
- **Proximity Search:** Find incidents within a user-defined distance from a set location.
- **Interactive Map:** Plots filtered or selected situations on a live map using the Google Maps SDK, with custom icons indicating the event type.
- **Responsive Layouts:** Distinct, optimised UI configurations for both Portrait and Landscape orientations (`layout` and `layout-land`).
- **Automated Background Updates:** Uses background threading to automatically fetch the latest data on startup and at regular intervals, keeping the UI responsive.

## Architecture & Tech Stack
- **Language:** Java & XML
- **Architecture:** MVVM (Model-View-ViewModel) using multiple fragments:
    - **Model (`model`):** `Situation` and `LocationPoint`
    - **Data (`data`):** `TrafficRepository` and `TrafficParser`
    - **ViewModel (`viewmodel`):** `TrafficViewModel`
    - **View (`view`):** `MainActivity`, `SituationListFragment`, `DetailMapFragment` and `SituationAdapter`
- **Networking & Parsing:** `java.net.http.HttpClient` on background threads and `XmlPullParser` for XML extraction.
- **UI Components:** Google Material Design (Cards, Chips and BottomNavigationView).
- **Mapping:** Google Maps Android SDK (`com.google.android.gms:play-services-maps`).

## Getting Started
Download and install the APK from the releases section of the repo.

or
1. Clone this repository and open the project directly in Android Studio.
2. Add your API credentials to `local.properties` in the project root:
   ```properties
   MAPS_API_KEY=your_google_maps_api_key
   TRAFFIC_CLIENT_ID=your_client_id
   TRAFFIC_CLIENT_KEY=your_client_key
   ```
3. Sync Gradle and run the app on an Android emulator or physical device running Android 8.0 (API 26) or later.

## Testing
Instrumented tests are located in `app/src/androidTest/java/org/me/gcu/mcfarlane_daniel_s2521636/` and cover:
- `TrafficParserTest`
- `SituationTest`
- `LocationPointTest`
- `MainActivityTest`

Run the test suite directly from Android Studio or via Gradle using `./gradlew connectedAndroidTest`.

Further testing and evidence is provided in the submitted word document.

## Icon Attribution & Licences
Custom vector icons are used in `res/drawable/` to represent situation types and map controls (`ic_roadworks_short`, `ic_roadworks_medium`, `ic_roadworks_long`, `ic_traffic_queue`, `ic_road_closed`, `ic_lane_restriction` and `ic_map_layers`).

- **Source:** Google Material Icons
- **Licence:** Apache License 2.0

## Disclaimer
This project is an academic submission for the GCU Mobile Platform Development module. It relies on the Traffic Scotland DATEX II developer API for educational purposes only.