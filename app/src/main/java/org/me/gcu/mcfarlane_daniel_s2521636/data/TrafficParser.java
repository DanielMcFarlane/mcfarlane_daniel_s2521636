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

    private static final String TAG_SITUATION = "situation";
    private static final String TAG_SITUATION_RECORD = "situationrecord";
    private static final String TAG_GENERAL_COMMENT = "generalpubliccomment";
    private static final String TAG_TO = "to";
    private static final String TAG_FROM = "from";
    private static final String TAG_GROUP_LOCATIONS = "groupoflocations";
    private static final String TAG_CREATION_TIME = "situationrecordcreationtime";
    private static final String TAG_START_TIME = "overallstarttime";
    private static final String TAG_END_TIME = "overallendtime";
    private static final String TAG_OPERATIONAL_LANES = "numberofoperationallanes";
    private static final String TAG_RESTRICTED_LANES = "numberoflanesrestricted";
    private static final String TAG_DELAYS_TYPE = "delaystype";
    private static final String TAG_VALUE = "value";
    private static final String TAG_DIRECTION = "tpegdirection";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_ILC_DESCRIPTOR = "tpegilcpointdescriptortype";
    private static final String TAG_JUNCTION_DESCRIPTOR = "tpegjunctionpointdescriptortype";
    private static final String TAG_OTHER_DESCRIPTOR = "tpegotherpointdescriptortype";
    private static final String TAG_MANAGEMENT_TYPE = "roadorcarriagewayorlanemanagementtype";
    private static final String TAG_ABNORMAL_TRAFFIC = "abnormaltraffictype";
    private static final String TAG_ROAD_MAINTENANCE = "roadmaintenancetype";
    private static final String TAG_URGENT_ROADWORKS = "urgentroadworks";

    public List<Situation> parseXmlString(String xmlContent, Situation.StreamSource streamSource) {
        List<Situation> situations = new ArrayList<>();

        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return situations;
        }

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
                        if (TAG_SITUATION.equalsIgnoreCase(tagName)) {
                            currentSituation = new Situation();
                            currentSituation.setStreamSource(streamSource);
                            extractAttribute(xmlParser, "id", currentSituation);
                        } else if (TAG_SITUATION_RECORD.equalsIgnoreCase(tagName) && currentSituation != null) {
                            extractSituationRecordAttributes(xmlParser, currentSituation);
                        } else if (TAG_GENERAL_COMMENT.equalsIgnoreCase(tagName)) {
                            insideGeneralPublicComment = true;
                        } else if ((TAG_TO.equalsIgnoreCase(tagName) || TAG_FROM.equalsIgnoreCase(tagName)) && currentSituation != null) {
                            currentLocationPoint = new LocationPoint(tagName.toLowerCase());
                            insideToOrFrom = true;
                        } else if (TAG_GROUP_LOCATIONS.equalsIgnoreCase(tagName) && currentSituation != null) {
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
                            case TAG_CREATION_TIME:
                                currentSituation.setCreationTime(currentText);
                                break;
                            case TAG_START_TIME:
                                currentSituation.setOverallStartTime(currentText);
                                break;
                            case TAG_END_TIME:
                                currentSituation.setOverallEndTime(currentText);
                                break;
                            case TAG_OPERATIONAL_LANES:
                                currentSituation.setOperationalLanes(parseSafeInt(currentText));
                                break;
                            case TAG_RESTRICTED_LANES:
                                currentSituation.setRestrictedLanes(parseSafeInt(currentText));
                                break;
                            case TAG_DELAYS_TYPE:
                                currentSituation.setDelaysType(currentText);
                                break;
                            case TAG_VALUE:
                                if (insideGeneralPublicComment) {
                                    currentSituation.setGeneralPublicComment(currentText);
                                } else if (insideToOrFrom || currentLocationPoint != null) {
                                    lastDescriptorValue = currentText;
                                }
                                break;
                            case TAG_GENERAL_COMMENT:
                                insideGeneralPublicComment = false;
                                break;
                            case TAG_DIRECTION:
                                currentSituation.setDirection(currentText);
                                break;
                            case TAG_LATITUDE:
                                if (currentLocationPoint != null) {
                                    currentLocationPoint.setLatitude(parseSafeDouble(currentText));
                                }
                                break;
                            case TAG_LONGITUDE:
                                if (currentLocationPoint != null) {
                                    currentLocationPoint.setLongitude(parseSafeDouble(currentText));
                                }
                                break;
                            case TAG_ILC_DESCRIPTOR:
                                if (currentLocationPoint != null && !lastDescriptorValue.isEmpty()) {
                                    currentLocationPoint.addRoadName(lastDescriptorValue);
                                    lastDescriptorValue = "";
                                }
                                break;
                            case TAG_JUNCTION_DESCRIPTOR:
                                if (currentLocationPoint != null && !lastDescriptorValue.isEmpty()) {
                                    currentLocationPoint.addJunctionDescription(lastDescriptorValue);
                                    lastDescriptorValue = "";
                                }
                                break;
                            case TAG_OTHER_DESCRIPTOR:
                                if (currentLocationPoint != null && !lastDescriptorValue.isEmpty()) {
                                    if ("linkName".equalsIgnoreCase(currentText)) {
                                        currentLocationPoint.addRoadName(lastDescriptorValue);
                                    } else {
                                        currentLocationPoint.addJunctionDescription(lastDescriptorValue);
                                    }
                                    lastDescriptorValue = "";
                                }
                                break;
                            case TAG_TO:
                            case TAG_FROM:
                            case TAG_GROUP_LOCATIONS:
                                if (currentLocationPoint != null) {
                                    if (currentLocationPoint.getLatitude() != 0.0 && currentLocationPoint.getLongitude() != 0.0) {
                                        currentSituation.addLocationPoint(currentLocationPoint);
                                    }
                                    currentLocationPoint = null;
                                }
                                insideToOrFrom = false;
                                break;
                            case TAG_MANAGEMENT_TYPE:
                            case TAG_ABNORMAL_TRAFFIC:
                            case TAG_ROAD_MAINTENANCE:
                                currentSituation.setManagementOrTrafficType(currentText);
                                break;
                            case TAG_URGENT_ROADWORKS:
                                currentSituation.setUrgentRoadworks(Boolean.parseBoolean(currentText));
                                break;
                            case TAG_SITUATION:
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