package org.me.gcu.mcfarlane_daniel_s2521636;

import org.junit.Test;
import static org.junit.Assert.*;
import org.me.gcu.mcfarlane_daniel_s2521636.model.LocationPoint;

public class LocationPointTest {

    @Test
    public void addRoadName_preventsDuplicates() {
        // Initialise a LocationPoint instance to test data deduplication
        LocationPoint point = new LocationPoint("point");

        point.addRoadName("M8");
        point.addRoadName("M8 ");
        point.addRoadName("A9");

        assertEquals("Should only contain 2 unique road names", 2, point.getRoadNames().size());
        assertTrue(point.getRoadNames().contains("M8"));
        assertTrue(point.getRoadNames().contains("A9"));
    }

    @Test
    public void addJunctionDescription_ignoresNullAndEmptyStrings() {
        // Initialise a LocationPoint instance to validate input sanitisation
        LocationPoint point = new LocationPoint("point");

        point.addJunctionDescription(null);
        point.addJunctionDescription("");
        point.addJunctionDescription("   ");
        point.addJunctionDescription("Junction 15");

        assertEquals("Should only contain 1 valid description", 1, point.getJunctionDescriptions().size());
        assertTrue(point.getJunctionDescriptions().contains("Junction 15"));
    }
}