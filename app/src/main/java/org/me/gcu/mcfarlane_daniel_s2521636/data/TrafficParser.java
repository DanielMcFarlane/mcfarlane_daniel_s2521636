//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Parses the Traffic Scotland DATEX II XML feeds into a list of Situation objects.
//

package org.me.gcu.mcfarlane_daniel_s2521636.data;

import android.util.Log;
import org.me.gcu.mcfarlane_daniel_s2521636.model.LocationPoint;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class TrafficParser {

    public List<Situation> parseXmlString(String xmlContent, Situation.StreamSource streamSource) {
        List<Situation> situations = new ArrayList<>();
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return situations;
        }

        // Strip any invisible characters or BOM before the XML declaration to prevent parsing errors
        int declarationIndex = xmlContent.indexOf("<?");
        if (declarationIndex > 0) {
            xmlContent = xmlContent.substring(declarationIndex);
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xmlParser = factory.newPullParser();
            xmlParser.setInput(new StringReader(xmlContent));

            Situation currentSituation = null;
            LocationPoint currentLocationPoint = null;
            String currentText = "";

            boolean insideGeneralPublicComment = false;
            boolean insideToOrFrom = false;
            String lastDescriptorValue = "";

            int eventType = xmlParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xmlParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("situation".equalsIgnoreCase(tagName)) {
                            currentSituation = new Situation();
                            currentSituation.setStreamSource(streamSource);
                            extractAttribute(xmlParser, "id", currentSituation);
                        } else if ("situationRecord".equalsIgnoreCase(tagName) && currentSituation != null) {
                            extractSituationRecordAttributes(xmlParser, currentSituation);
                        } else if ("generalPublicComment".equalsIgnoreCase(tagName)) {
                            insideGeneralPublicComment = true;
                        } else if (("to".equalsIgnoreCase(tagName) || "from".equalsIgnoreCase(tagName)) && currentSituation != null) {
                            currentLocationPoint = new LocationPoint(tagName.toLowerCase());
                            insideToOrFrom = true;
                        } else if ("groupOfLocations".equalsIgnoreCase(tagName) && currentSituation != null) {
                            if (currentLocationPoint == null) {
                                currentLocationPoint = new LocationPoint("Point");
                            }
                        }
                        break;

                    case XmlPullParser.TEXT:
                        currentText = xmlParser.getText() != null ? xmlParser.getText().trim() : "";
                        break;

                    case XmlPullParser.END_TAG:
                        if (currentSituation == null) {
                            break;
                        }

                        switch (tagName.toLowerCase()) {
                            case "situationrecordcreationtime":
                                currentSituation.setCreationTime(currentText);
                                break;
                            case "overallstarttime":
                                currentSituation.setOverallStartTime(currentText);
                                break;
                            case "overallendtime":
                                currentSituation.setOverallEndTime(currentText);
                                break;
                            case "numberofoperationallanes":
                                currentSituation.setOperationalLanes(parseSafeInt(currentText));
                                break;
                            case "numberoflanesrestricted":
                                currentSituation.setRestrictedLanes(parseSafeInt(currentText));
                                break;
                            case "delaystype":
                                currentSituation.setDelaysType(currentText);
                                break;
                            case "value":
                                // The <value> tag is used generically, context flags determine if it represents a comment or a location descriptor
                                if (insideGeneralPublicComment) {
                                    currentSituation.setGeneralPublicComment(currentText);
                                } else if (insideToOrFrom || currentLocationPoint != null) {
                                    lastDescriptorValue = currentText;
                                }
                                break;
                            case "generalpubliccomment":
                                insideGeneralPublicComment = false;
                                break;
                            case "tpegdirection":
                                currentSituation.setDirection(currentText);
                                break;
                            case "latitude":
                                if (currentLocationPoint != null) {
                                    currentLocationPoint.setLatitude(parseSafeDouble(currentText));
                                }
                                break;
                            case "longitude":
                                if (currentLocationPoint != null) {
                                    currentLocationPoint.setLongitude(parseSafeDouble(currentText));
                                }
                                break;
                            case "tpegilcpointdescriptortype":
                                if (currentLocationPoint != null && !lastDescriptorValue.isEmpty()) {
                                    currentLocationPoint.addRoadName(lastDescriptorValue);
                                    lastDescriptorValue = "";
                                }
                                break;
                            case "tpegjunctionpointdescriptortype":
                                if (currentLocationPoint != null && !lastDescriptorValue.isEmpty()) {
                                    currentLocationPoint.addJunctionDescription(lastDescriptorValue);
                                    lastDescriptorValue = "";
                                }
                                break;
                            case "tpegotherpointdescriptortype":
                                if (currentLocationPoint != null && !lastDescriptorValue.isEmpty()) {
                                    if ("linkName".equalsIgnoreCase(currentText)) {
                                        currentLocationPoint.addRoadName(lastDescriptorValue);
                                    } else {
                                        currentLocationPoint.addJunctionDescription(lastDescriptorValue);
                                    }
                                    lastDescriptorValue = "";
                                }
                                break;
                            case "to":
                            case "from":
                            case "groupoflocations":
                                if (currentLocationPoint != null) {
                                    // Prevent saving empty coordinates if the location point was instantiated but not populated with valid lat/long
                                    if (currentLocationPoint.getLatitude() != 0.0 && currentLocationPoint.getLongitude() != 0.0) {
                                        currentSituation.addLocationPoint(currentLocationPoint);
                                    }
                                    currentLocationPoint = null;
                                }
                                insideToOrFrom = false;
                                break;
                            case "roadorcarriagewayorlanemanagementtype":
                            case "abnormaltraffictype":
                            case "roadmaintenancetype":
                                currentSituation.setManagementOrTrafficType(currentText);
                                break;
                            case "urgentroadworks":
                                currentSituation.setUrgentRoadworks(Boolean.parseBoolean(currentText));
                                break;
                            case "situation":
                                situations.add(currentSituation);
                                currentSituation = null;
                                break;
                        }
                        break;
                }
                eventType = xmlParser.next();
            }
        } catch (XmlPullParserException | IOException parsingException) {
            Log.e("TrafficParser", "Error parsing DATEX II XML", parsingException);
        }

        return situations;
    }

    private void extractAttribute(XmlPullParser xmlParser, String targetAttribute, Situation situation) {
        int attributeCount = xmlParser.getAttributeCount();
        for (int index = 0; index < attributeCount; index++) {
            if (targetAttribute.equalsIgnoreCase(xmlParser.getAttributeName(index))) {
                situation.setSituationId(xmlParser.getAttributeValue(index));
            }
        }
    }

    private void extractSituationRecordAttributes(XmlPullParser xmlParser, Situation situation) {
        int attributeCount = xmlParser.getAttributeCount();
        if (attributeCount > 0) {
            situation.setRecordXsiType(xmlParser.getAttributeValue(0));
        }
        for (int index = 0; index < attributeCount; index++) {
            String attributeName = xmlParser.getAttributeName(index);
            if ("id".equalsIgnoreCase(attributeName)) {
                situation.setRecordId(xmlParser.getAttributeValue(index));
            } else if (attributeName.toLowerCase().contains("type")) {
                situation.setRecordXsiType(xmlParser.getAttributeValue(index));
            }
        }
    }

    private int parseSafeInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException parsingException) {
            return -1;
        }
    }

    private double parseSafeDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException parsingException) {
            return 0.0;
        }
    }
}