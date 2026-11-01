package org.me.gcu.mcfarlane_daniel_s2521636;

import org.junit.Test;
import static org.junit.Assert.*;
import org.me.gcu.mcfarlane_daniel_s2521636.data.TrafficParser;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import java.util.List;

public class TrafficParserTest {

    @Test
    public void parserExtractsSituationCorrectly() {
        // Define a minimal valid XML payload to simulate a DATEX II response
        String dummyXml = "<?xml version=\"1.0\"?><situation id=\"123\"><generalPublicComment><value>Test Incident</value></generalPublicComment></situation>";
        TrafficParser parser = new TrafficParser();

        // Invoke the parser to simulate processing an incoming unplanned incident feed
        List<Situation> situations = parser.parseXmlString(dummyXml, Situation.StreamSource.UNPLANNED_INCIDENT);

        // Validate that the parser successfully built the domain objects and mapped the core attributes
        assertEquals("The parser should return exactly 1 parsed situation", 1, situations.size());
        assertEquals("The situation ID should be extracted correctly", "123", situations.get(0).getSituationId());
        assertEquals("The public comment should be extracted correctly", "Test Incident", situations.get(0).getGeneralPublicComment());
    }
}