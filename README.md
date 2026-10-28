# Traffic Scotland Mobile App
**Student Name:** Daniel McFarlane  
**Student ID:** S2521636    
**Module:** Mobile Platform Development MHI326841   
**Institution:** Glasgow Caledonian University

---

## Overview
An Android application built to consume and display live traffic and travel data from the official Traffic Scotland DATEX II XML feeds. The app parses real-time data to provide road users with up-to-date information on unplanned events (incidents) and ongoing/ future roadworks across the Scottish road network.

Targeted for Android 8.0 (Oreo / API Level 26) and above.

## Key Features
- **Live XML Parsing:** Downloads and parses Traffic Scotland's Unplanned Events and Current Roadworks feeds natively using XmlPullParser.
- **Situation Dashboard:** Displays a curated list of all current incidents and roadworks, detailing the event type and general public comments.
- **Detailed Inspection:** Tap any situation to view in-depth metadata including impact, validity periods, direction and coordinates.
- **Color-Coded Delay Indicators:** Roadworks feature visual badges indicating the severity and duration of delays.
- **Road Search:** Filter situations by specific road names (e.g., "M8").
- **Proximity Search:** Find incidents within a user-defined distance from a set location.
- **Interactive Map:** Plots filtered or selected situations on a live map utilising the Google Maps SDK, with custom icons indicating the event type.
- **Responsive Layouts:** Distinct, optimised UI configurations for both Portrait and Landscape orientations.
- **Automated Background Updates:** Leverages background threading to automatically fetch the latest data on startup and at regular intervals, keeping the UI responsive.

## Architecture & Tech Stack
- **Language:** Java & XML
- **Architecture:** Single Activity with Multiple Fragments (`MainActivity`, `SituationListFragment`, `DetailMapFragment`).
- **Networking/Data:** `java.net.http.HttpClient` (async background threads) & `XmlPullParser` for strict XML extraction.
- **UI Components:** Google Material Design (Cards, Chips, BottomNavigationView).
- **Mapping:** Google Maps Android SDK (`com.google.android.gms:play-services-maps`).

## Disclaimer
This project is an academic submission for the GCU Mobile Platform Development module. It relies on the Traffic Scotland DATEX II developer API for educational purposes only.