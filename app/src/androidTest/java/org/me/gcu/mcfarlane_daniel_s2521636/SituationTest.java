//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Tests the Situation model class to verify duration, distance, search matching, and severity colour logic.
//

package org.me.gcu.mcfarlane_daniel_s2521636;

import android.graphics.Color;
import org.junit.Test;
import static org.junit.Assert.*;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import org.me.gcu.mcfarlane_daniel_s2521636.model.LocationPoint;

public class SituationTest {
    @Test
    public void durationCalculation_isCorrect() {
        // Initialise a Situation with ISO 8601 timestamps spanning a 4-day period
        Situation situation = new Situation();
        situation.setOverallStartTime("2026-11-01T10:00:00Z");
        situation.setOverallEndTime("2026-11-05T10:00:00Z");

        assertEquals("Duration should be calculated as 4 days", 4, situation.getDurationInDays());
    }

    @Test
    public void missingDates_returnsZeroDuration() {
        // Initialise a Situation simulating missing date attributes from the XML feed
        Situation situation = new Situation();
        situation.setOverallStartTime("");
        situation.setOverallEndTime("");

        assertEquals("Missing dates should default to 0 days duration", 0, situation.getDurationInDays());
    }

    @Test
    public void matchesRoadQuery_findsExactAndPartialMatches() {
        // Populate a Situation with standard general public comment data
        Situation situation = new Situation();
        situation.setGeneralPublicComment("Roadworks on the M8 eastbound");

        assertTrue("Should match 'M8' exactly", situation.matchesRoadQuery("M8"));
        assertTrue("Should match 'roadworks' regardless of case", situation.matchesRoadQuery("roadworks"));
        assertFalse("Should not match unrelated road names like 'M9'", situation.matchesRoadQuery("M9"));
    }

    @Test
    public void getMinimumDistanceMiles_calculatesCorrectDistance() {
        // Configure a Situation with known geographical coordinates for Edinburgh
        Situation situation = new Situation();
        LocationPoint point = new LocationPoint("point");
        point.setLatitude(55.9533);
        point.setLongitude(-3.1883);
        situation.addLocationPoint(point);

        // Define static reference coordinates for Glasgow
        double userLat = 55.8642;
        double userLon = -4.2518;

        double distance = situation.getMinimumDistanceMiles(userLat, userLon);

        // Assert that the computed spherical distance falls within the expected real-world range
        assertTrue("Distance between Glasgow and Edinburgh should be between 40 and 50 miles", distance > 40.0 && distance < 50.0);
    }

    @Test
    public void getSeverityColour_returnsCorrectColourForDuration() {
        // Simulate a major ongoing roadworks event lasting beyond the 30-day threshold
        Situation situation = new Situation();
        situation.setStreamSource(Situation.StreamSource.CURRENT_ROADWORKS);
        situation.setOverallStartTime("2026-11-01T10:00:00Z");
        situation.setOverallEndTime("2026-12-15T10:00:00Z");

        assertEquals("Situations over 30 days should return the Red colour int", Color.parseColor("#C62828"), situation.getSeverityColour());
    }
}