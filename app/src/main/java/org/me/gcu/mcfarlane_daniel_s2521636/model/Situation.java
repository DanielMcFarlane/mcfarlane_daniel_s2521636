//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Represents a specific traffic incident or roadwork situation and its metadata.
//

package org.me.gcu.mcfarlane_daniel_s2521636.model;

import android.graphics.Color;
import org.me.gcu.mcfarlane_daniel_s2521636.R;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Situation {

    public enum StreamSource {
        UNPLANNED_INCIDENT,
        CURRENT_ROADWORKS
    }

    private String situationId = "";
    private String recordId = "";
    private String recordXsiType = "";
    private StreamSource streamSource;

    private String creationTime = "";
    private String overallStartTime = "";
    private String overallEndTime = "";
    private String generalPublicComment = "";
    private String direction = "";
    private String managementOrTrafficType = "";
    private String delaysType = "";
    private int operationalLanes = -1;
    private int restrictedLanes = -1;
    private boolean urgentRoadworks = false;

    private final List<LocationPoint> locations = new ArrayList<>();

    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getRecordXsiType() { return recordXsiType; }
    public void setRecordXsiType(String recordXsiType) { this.recordXsiType = recordXsiType; }

    public StreamSource getStreamSource() { return streamSource; }
    public void setStreamSource(StreamSource streamSource) { this.streamSource = streamSource; }

    public String getCreationTime() { return creationTime; }
    public void setCreationTime(String creationTime) { this.creationTime = creationTime; }

    public String getOverallStartTime() { return overallStartTime; }
    public void setOverallStartTime(String overallStartTime) { this.overallStartTime = overallStartTime; }

    public String getOverallEndTime() { return overallEndTime; }
    public void setOverallEndTime(String overallEndTime) { this.overallEndTime = overallEndTime; }

    public String getGeneralPublicComment() { return generalPublicComment; }
    public void setGeneralPublicComment(String generalPublicComment) { this.generalPublicComment = generalPublicComment; }

    public String getDirection() { return formatCamelCase(direction); }
    public void setDirection(String direction) { this.direction = direction; }

    public String getManagementOrTrafficType() { return managementOrTrafficType; }
    public void setManagementOrTrafficType(String managementOrTrafficType) { this.managementOrTrafficType = managementOrTrafficType; }

    public String getDelaysType() { return delaysType; }
    public void setDelaysType(String delaysType) { this.delaysType = delaysType; }

    public int getOperationalLanes() { return operationalLanes; }
    public void setOperationalLanes(int operationalLanes) { this.operationalLanes = operationalLanes; }

    public int getRestrictedLanes() { return restrictedLanes; }
    public void setRestrictedLanes(int restrictedLanes) { this.restrictedLanes = restrictedLanes; }

    public boolean isUrgentRoadworks() { return urgentRoadworks; }
    public void setUrgentRoadworks(boolean urgentRoadworks) { this.urgentRoadworks = urgentRoadworks; }

    public List<LocationPoint> getLocations() { return locations; }
    public void addLocationPoint(LocationPoint point) { this.locations.add(point); }

    public String getPrimaryRoadName() {
        for (LocationPoint locationPoint : locations) {
            if (!locationPoint.getRoadNames().isEmpty()) {
                return locationPoint.getRoadNames().get(0);
            }
        }

        // Fallback attempt to extract UK road numbers directly from the text description
        if (generalPublicComment != null && !generalPublicComment.isEmpty()) {
            String[] tokens = generalPublicComment.split("\\s+");
            for (String token : tokens) {
                if (token.matches("^[AMB]\\d+.*")) {
                    return token.replaceAll("[^A-Za-z0-9()]", "");
                }
            }
        }
        return "Trunk Road";
    }

    public String getDisplayTypeLabel() {
        String subtype = managementOrTrafficType.isEmpty() ? recordXsiType : managementOrTrafficType;
        String prefix = (streamSource == StreamSource.UNPLANNED_INCIDENT) ? "Incident" : "Roadworks";
        return prefix + " (" + formatCamelCase(subtype) + ")";
    }

    public long getDurationInDays() {
        if (overallStartTime.isEmpty() || overallEndTime.isEmpty()) {
            return 0;
        }
        try {
            OffsetDateTime start = OffsetDateTime.parse(overallStartTime);
            OffsetDateTime end = OffsetDateTime.parse(overallEndTime);
            return Math.max(0, Duration.between(start, end).toDays());
        } catch (Exception parsingException) {
            return 0;
        }
    }

    public int getSeverityColour() {
        if (streamSource == StreamSource.UNPLANNED_INCIDENT) {
            return "roadClosed".equalsIgnoreCase(managementOrTrafficType) ? Color.parseColor("#D32F2F") : Color.parseColor("#E65100");
        }
        long durationDays = getDurationInDays();
        if ("longDelays".equalsIgnoreCase(delaysType) || durationDays > 30) {
            return Color.parseColor("#C62828");
        }
        if ("delays".equalsIgnoreCase(delaysType) || durationDays > 3) {
            return Color.parseColor("#EF6C00");
        }
        return Color.parseColor("#2E7D32");
    }

    public int getIconResource() {
        if (streamSource == StreamSource.UNPLANNED_INCIDENT) {
            if ("roadClosed".equalsIgnoreCase(managementOrTrafficType)) {
                return R.drawable.ic_road_closed;
            }
            if ("queuingTraffic".equalsIgnoreCase(managementOrTrafficType) || "AbnormalTraffic".equalsIgnoreCase(recordXsiType)) {
                return R.drawable.ic_traffic_queue;
            }
            return R.drawable.ic_lane_restriction;
        }
        long durationDays = getDurationInDays();
        if ("longDelays".equalsIgnoreCase(delaysType) || durationDays > 30) {
            return R.drawable.ic_roadworks_long;
        }
        if ("delays".equalsIgnoreCase(delaysType) || durationDays > 3) {
            return R.drawable.ic_roadworks_medium;
        }
        return R.drawable.ic_roadworks_short;
    }

    public String getImpactAndDurationSummary() {
        List<String> summaryParts = new ArrayList<>();
        if (!delaysType.isEmpty()) summaryParts.add("Delays: " + formatCamelCase(delaysType));
        if (operationalLanes >= 0) summaryParts.add("Operational Lanes: " + operationalLanes);
        if (restrictedLanes >= 0) summaryParts.add("Restricted Lanes: " + restrictedLanes);

        long durationDays = getDurationInDays();
        if (durationDays > 0) summaryParts.add("Planned Duration: " + durationDays + " days");

        return summaryParts.isEmpty() ? "Standard monitoring active" : String.join(" | ", summaryParts);
    }

    public String getShortStatus() {
        if (streamSource == StreamSource.UNPLANNED_INCIDENT) {
            if ("roadClosed".equalsIgnoreCase(managementOrTrafficType)) return "Road Closed";
            if (restrictedLanes > 0) return restrictedLanes + " Restricted Lanes";
            return "Incident";
        }

        if (!delaysType.isEmpty()) {
            return formatCamelCase(delaysType);
        }

        return "Roadworks";
    }

    public boolean matchesRoadQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String lowerCaseQuery = query.trim().toLowerCase();

        if (generalPublicComment.toLowerCase().contains(lowerCaseQuery) || getPrimaryRoadName().toLowerCase().contains(lowerCaseQuery)) {
            return true;
        }

        for (LocationPoint locationPoint : locations) {
            for (String road : locationPoint.getRoadNames()) {
                if (road.toLowerCase().contains(lowerCaseQuery)) return true;
            }
            for (String description : locationPoint.getJunctionDescriptions()) {
                if (description.toLowerCase().contains(lowerCaseQuery)) return true;
            }
        }
        return false;
    }

    public double getMinimumDistanceMiles(double userLatitude, double userLongitude) {
        if (locations.isEmpty()) {
            return Double.MAX_VALUE;
        }
        double minimumDistance = Double.MAX_VALUE;
        for (LocationPoint locationPoint : locations) {
            if (locationPoint.getLatitude() != 0.0 || locationPoint.getLongitude() != 0.0) {
                double distance = calculateHaversineMiles(userLatitude, userLongitude, locationPoint.getLatitude(), locationPoint.getLongitude());
                if (distance < minimumDistance) {
                    minimumDistance = distance;
                }
            }
        }
        return minimumDistance;
    }

    private double calculateHaversineMiles(double userLatitude, double userLongitude, double pointLatitude, double pointLongitude) {
        final double earthRadiusMiles = 3958.8; // Mean Earth radius in miles

        double deltaLatitude = Math.toRadians(pointLatitude - userLatitude);
        double deltaLongitude = Math.toRadians(pointLongitude - userLongitude);

        double halfChordLengthSquare = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(Math.toRadians(userLatitude)) * Math.cos(Math.toRadians(pointLatitude))
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);

        double angularDistance = 2 * Math.atan2(Math.sqrt(halfChordLengthSquare), Math.sqrt(1 - halfChordLengthSquare));

        return earthRadiusMiles * angularDistance;
    }

    private String formatCamelCase(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return rawText;
        }
        String formattedText = rawText.replaceAll("([a-z])([A-Z]+)", "$1 $2");
        return formattedText.substring(0, 1).toUpperCase() + formattedText.substring(1);
    }
}