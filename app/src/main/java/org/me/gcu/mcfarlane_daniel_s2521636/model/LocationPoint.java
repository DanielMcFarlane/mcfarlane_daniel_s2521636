//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Represents a geographic coordinate and its associated road descriptions for a traffic situation.
//

package org.me.gcu.mcfarlane_daniel_s2521636.model;

import java.util.ArrayList;
import java.util.List;

public class LocationPoint {

    private String pointRole;
    private double latitude;
    private double longitude;
    private final List<String> roadNames = new ArrayList<>();
    private final List<String> junctionDescriptions = new ArrayList<>();

    public LocationPoint(String pointRole) {
        this.pointRole = pointRole;
    }

    public String getPointRole() { return pointRole; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public List<String> getRoadNames() { return roadNames; }
    public List<String> getJunctionDescriptions() { return junctionDescriptions; }

    public void addRoadName(String roadName) {
        // DATEX II XML feeds occasionally repeat the exact same descriptor nodes, this prevents duplicating them in the UI
        if (roadName != null && !roadName.trim().isEmpty() && !roadNames.contains(roadName.trim())) {
            roadNames.add(roadName.trim());
        }
    }

    public void addJunctionDescription(String junctionDescription) {
        if (junctionDescription != null && !junctionDescription.trim().isEmpty() && !junctionDescriptions.contains(junctionDescription.trim())) {
            junctionDescriptions.add(junctionDescription.trim());
        }
    }

    public String getFormattedSummary() {
        String formattedRoads = roadNames.isEmpty() ? "N/A" : String.join(", ", roadNames);
        String formattedJunctions = junctionDescriptions.isEmpty() ? "Segment" : String.join(" / ", junctionDescriptions);

        return String.format("%s: %s (%s) [%.5f, %.5f]",
                pointRole.toUpperCase(), formattedJunctions, formattedRoads, latitude, longitude);
    }
}