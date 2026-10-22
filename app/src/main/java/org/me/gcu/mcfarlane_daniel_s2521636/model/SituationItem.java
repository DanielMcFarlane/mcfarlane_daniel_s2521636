//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Represents a simplified, serializable traffic situation item.
//

package org.me.gcu.mcfarlane_daniel_s2521636.model;

import java.io.Serializable;

public class SituationItem implements Serializable {

    private String recordId = "";
    private String recordType = "";
    private String description = "";
    private String startDate = "";
    private String delayType = "";
    private double latitude = 0.0;
    private double longitude = 0.0;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getDelayType() { return delayType; }
    public void setDelayType(String delayType) { this.delayType = delayType; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}