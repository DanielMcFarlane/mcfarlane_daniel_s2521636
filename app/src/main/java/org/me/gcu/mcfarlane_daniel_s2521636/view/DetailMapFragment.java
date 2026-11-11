//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Fragment responsible for displaying the Google Map, plotting situations, and showing detailed information cards.
//

package org.me.gcu.mcfarlane_daniel_s2521636.view;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.card.MaterialCardView;
import org.me.gcu.mcfarlane_daniel_s2521636.R;
import org.me.gcu.mcfarlane_daniel_s2521636.model.LocationPoint;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import org.me.gcu.mcfarlane_daniel_s2521636.viewmodel.TrafficViewModel;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailMapFragment extends Fragment implements OnMapReadyCallback {
    private TrafficViewModel viewModel;
    private TextView detailTitle;
    private TextView detailComment;
    private ImageView closeDetailButton;
    private MapView mapView;
    private GoogleMap googleMap;
    private CardView textCard;
    private CardView mapCard;
    private View searchAndListContainer;

    // Metadata Container and Fields
    private View metadataContainer;
    private TextView detailImpact, detailOpLanes, detailRestLanes, detailDuration;
    private TextView detailDirection, detailStartTime, detailEndTime, detailUrgent, detailLocations;

    private final List<Polyline> activeMarkerPolylines = new ArrayList<>();
    private Marker activeMarker = null;
    private List<Situation> lastPlottedSituations = null;
    private boolean wasSingleSituation = false;
    private String lastOpenedSituationId = null;
    private LatLng lastOpenedMarkerPosition = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textCard = view.findViewById(R.id.textCard);
        if (textCard == null) {
            textCard = requireActivity().findViewById(R.id.textCard);
            searchAndListContainer = requireActivity().findViewById(R.id.searchAndListContainer);
        }

        detailTitle = textCard.findViewById(R.id.detailTitle);
        detailComment = textCard.findViewById(R.id.detailComment);
        closeDetailButton = textCard.findViewById(R.id.closeDetailButton);

        metadataContainer = textCard.findViewById(R.id.metadataContainer);
        detailImpact = textCard.findViewById(R.id.detailImpact);
        detailOpLanes = textCard.findViewById(R.id.detailOpLanes);
        detailRestLanes = textCard.findViewById(R.id.detailRestLanes);
        detailDuration = textCard.findViewById(R.id.detailDuration);
        detailDirection = textCard.findViewById(R.id.detailDirection);
        detailStartTime = textCard.findViewById(R.id.detailStartTime);
        detailEndTime = textCard.findViewById(R.id.detailEndTime);
        detailUrgent = textCard.findViewById(R.id.detailUrgent);
        detailLocations = textCard.findViewById(R.id.detailLocations);

        mapCard = view.findViewById(R.id.mapCard);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        closeDetailButton.setOnClickListener(buttonView -> {
            boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            if (isLandscape) {
                viewModel.showAllFilteredOnMap();
            } else {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        viewModel.getSelectedSituation().observe(getViewLifecycleOwner(), situation -> {
            if (situation == null) return;
            if (lastOpenedSituationId != null && !lastOpenedSituationId.equals(situation.getSituationId())) {
                lastOpenedSituationId = null;
                lastOpenedMarkerPosition = null;
            }
            applySingleDetailLayout();
            populateSingleSituationDetails(situation);
            renderMapMarkers();
        });

        viewModel.getPlotAllFilteredOnMap().observe(getViewLifecycleOwner(), shouldPlotAll -> {
            if (Boolean.TRUE.equals(shouldPlotAll)) {
                applyLiveMapLayout();
                populateMultiSituationSummary(viewModel.getFilteredSituations().getValue());
                renderMapMarkers();
            }
        });

        viewModel.getFilteredSituations().observe(getViewLifecycleOwner(), situationList -> {
            if (Boolean.TRUE.equals(viewModel.getPlotAllFilteredOnMap().getValue())) {
                populateMultiSituationSummary(situationList);
                renderMapMarkers();
            }
        });
    }

    private void clearActivePolylines() {
        for (Polyline p : activeMarkerPolylines) {
            p.remove();
        }
        activeMarkerPolylines.clear();
    }

    private void drawSafePolylines(List<LatLng> coordinates, int color, boolean isActiveMarker) {
        if (coordinates == null || coordinates.size() < 2 || googleMap == null) return;

        List<LatLng> currentSegment = new ArrayList<>();
        currentSegment.add(coordinates.get(0));
        float[] results = new float[1];

        for (int i = 1; i < coordinates.size(); i++) {
            LatLng prev = coordinates.get(i - 1);
            LatLng curr = coordinates.get(i);

            Location.distanceBetween(prev.latitude, prev.longitude, curr.latitude, curr.longitude, results);

            // Break the polyline if the jump between points exceeds 15km
            if (results[0] > 15000) {
                if (currentSegment.size() >= 2) {
                    Polyline p = googleMap.addPolyline(new PolylineOptions().addAll(currentSegment).width(8f).color(color));
                    if (isActiveMarker) activeMarkerPolylines.add(p);
                }
                currentSegment.clear();
            }
            currentSegment.add(curr);
        }

        if (currentSegment.size() >= 2) {
            Polyline p = googleMap.addPolyline(new PolylineOptions().addAll(currentSegment).width(8f).color(color));
            if (isActiveMarker) activeMarkerPolylines.add(p);
        }
    }

    private void applySingleDetailLayout() {
        if (textCard == null) return;
        ViewGroup.LayoutParams params = textCard.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams) params;
            textParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            textParams.weight = 0f;
            textCard.setLayoutParams(textParams);
        }
    }

    private void applyLiveMapLayout() {
        if (textCard == null) return;
        ViewGroup.LayoutParams params = textCard.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams) params;
            textParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            textParams.weight = 0f;
            textCard.setLayoutParams(textParams);
        }
    }

    private void populateSingleSituationDetails(Situation situation) {
        if (textCard != null) textCard.setVisibility(View.VISIBLE);
        if (searchAndListContainer != null) searchAndListContainer.setVisibility(View.GONE);

        closeDetailButton.setVisibility(View.VISIBLE);
        metadataContainer.setVisibility(View.VISIBLE);
        detailComment.setVisibility(View.VISIBLE);

        detailTitle.setText(situation.getPrimaryRoadName() + "   " + situation.getDisplayTypeLabel());
        detailComment.setText(situation.getGeneralPublicComment());

        if (!situation.getDelaysType().isEmpty()) {
            String formattedDelay = situation.getDelaysType().replaceAll("([a-z])([A-Z]+)", "$1 $2");
            formattedDelay = formattedDelay.substring(0, 1).toUpperCase() + formattedDelay.substring(1);
            detailImpact.setText(Html.fromHtml("<b>Traffic Impact:</b> " + formattedDelay, Html.FROM_HTML_MODE_COMPACT));
            detailImpact.setVisibility(View.VISIBLE);
        } else {
            detailImpact.setVisibility(View.GONE);
        }

        if (situation.getOperationalLanes() >= 0) {
            detailOpLanes.setText(Html.fromHtml("<b>Operational Lanes:</b> " + situation.getOperationalLanes(), Html.FROM_HTML_MODE_COMPACT));
            detailOpLanes.setVisibility(View.VISIBLE);
        } else {
            detailOpLanes.setVisibility(View.GONE);
        }

        if (situation.getRestrictedLanes() >= 0) {
            detailRestLanes.setText(Html.fromHtml("<b>Restricted Lanes:</b> " + situation.getRestrictedLanes(), Html.FROM_HTML_MODE_COMPACT));
            detailRestLanes.setVisibility(View.VISIBLE);
        } else {
            detailRestLanes.setVisibility(View.GONE);
        }

        long durationDays = situation.getDurationInDays();
        if (durationDays > 0) {
            detailDuration.setText(Html.fromHtml("<b>Planned Duration:</b> " + durationDays + " days", Html.FROM_HTML_MODE_COMPACT));
            detailDuration.setVisibility(View.VISIBLE);
        } else {
            detailDuration.setVisibility(View.GONE);
        }

        String direction = situation.getDirection().isEmpty() ? "Both / Unspecified" : situation.getDirection();
        detailDirection.setText(Html.fromHtml("<b>Direction:</b> " + direction, Html.FROM_HTML_MODE_COMPACT));

        detailStartTime.setText(Html.fromHtml("<b>Start Time:</b> " + formatFriendlyDate(situation.getOverallStartTime()), Html.FROM_HTML_MODE_COMPACT));

        if (!situation.getOverallEndTime().isEmpty()) {
            detailEndTime.setText(Html.fromHtml("<b>End Time:</b> " + formatFriendlyDate(situation.getOverallEndTime()), Html.FROM_HTML_MODE_COMPACT));
            detailEndTime.setVisibility(View.VISIBLE);
        } else {
            detailEndTime.setVisibility(View.GONE);
        }

        if (situation.isUrgentRoadworks()) {
            detailUrgent.setText("Urgent Roadworks: Yes");
            detailUrgent.setVisibility(View.VISIBLE);
        } else {
            detailUrgent.setVisibility(View.GONE);
        }

        StringBuilder locationsBuilder = new StringBuilder();
        List<LocationPoint> sortedLocations = new ArrayList<>(situation.getLocations());
        sortedLocations.sort((p1, p2) -> p1.getPointRole().compareToIgnoreCase(p2.getPointRole()));

        for (LocationPoint locationPoint : sortedLocations) {
            String junctionDescriptions = locationPoint.getJunctionDescriptions().isEmpty() ? "Segment" : String.join(" / ", locationPoint.getJunctionDescriptions());
            String rawRole = locationPoint.getPointRole();
            String formattedRole = rawRole.isEmpty() ? "Point" : rawRole.substring(0, 1).toUpperCase() + rawRole.substring(1).toLowerCase();
            locationsBuilder.append("&#8226; <b>").append(formattedRole).append(":</b> ").append(junctionDescriptions).append("<br/>");
        }
        detailLocations.setText(Html.fromHtml(locationsBuilder.toString(), Html.FROM_HTML_MODE_COMPACT));
    }

    private String formatFriendlyDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "Immediate";
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(isoDateString);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.UK);
            return offsetDateTime.format(dateFormatter);
        } catch (Exception parsingException) {
            return isoDateString;
        }
    }

    private void populateMultiSituationSummary(List<Situation> situationList) {
        closeDetailButton.setVisibility(View.GONE);
        metadataContainer.setVisibility(View.GONE);

        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            detailComment.setVisibility(View.GONE);
            if (textCard != null) textCard.setVisibility(View.GONE);
            if (searchAndListContainer != null) searchAndListContainer.setVisibility(View.VISIBLE);
        } else {
            if (textCard != null) textCard.setVisibility(View.VISIBLE);
            detailComment.setVisibility(View.VISIBLE);
            detailComment.setText(getString(R.string.tap_marker_hint));
        }

        int listSize = situationList != null ? situationList.size() : 0;
        detailTitle.setText(getString(R.string.matching_results, listSize));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        int paddingEdge = (int) (32 * getResources().getDisplayMetrics().density);
        int paddingTop = (int) (76 * getResources().getDisplayMetrics().density);
        googleMap.setPadding(paddingEdge, paddingTop, paddingEdge, paddingEdge);

        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Nullable
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                Situation situation = (Situation) marker.getTag();
                if (situation == null) return null;

                View view = getLayoutInflater().inflate(R.layout.item_situation, null);
                boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
                int infoWindowWidth;

                if (isLandscape) {
                    infoWindowWidth = (int) (340 * getResources().getDisplayMetrics().density);
                } else {
                    infoWindowWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85f);
                }

                view.setLayoutParams(new ViewGroup.LayoutParams(infoWindowWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
                view.setMinimumWidth(infoWindowWidth);

                if (view instanceof MaterialCardView) {
                    MaterialCardView card = (MaterialCardView) view;
                    card.setCardBackgroundColor(Color.WHITE);
                    card.setCardElevation(8f);
                    card.setUseCompatPadding(false);
                    View innerLayout = card.getChildAt(0);
                    if (innerLayout != null) {
                        innerLayout.setLayoutParams(new FrameLayout.LayoutParams(infoWindowWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                }

                ImageView icon = view.findViewById(R.id.situationIcon);
                TextView roadAndType = view.findViewById(R.id.itemRoadAndType);
                TextView comment = view.findViewById(R.id.itemComment);
                TextView delayBadge = view.findViewById(R.id.itemDelayBadge);

                comment.setMaxLines(Integer.MAX_VALUE);
                comment.setEllipsize(null);
                roadAndType.setText(situation.getPrimaryRoadName() + "   " + situation.getDisplayTypeLabel());

                String commentText = situation.getGeneralPublicComment();
                if (commentText == null || commentText.trim().isEmpty()) {
                    commentText = getString(R.string.no_additional_details);
                }
                comment.setText(commentText);

                String badgeContent = situation.getShortStatus();
                double distanceMiles = situation.getMinimumDistanceMiles(viewModel.getUserLatitude(), viewModel.getUserLongitude());
                if (distanceMiles < 10000) {
                    badgeContent += "   " + getString(R.string.miles_away, distanceMiles);
                }

                delayBadge.setText(badgeContent);
                delayBadge.getBackground().setTint(situation.getSeverityColour());
                icon.setImageResource(situation.getIconResource());

                return view;
            }

            @Nullable
            @Override
            public View getInfoContents(@NonNull Marker marker) {
                return null;
            }
        });

        googleMap.setOnMarkerClickListener(marker -> {
            Situation situation = (Situation) marker.getTag();
            if (situation != null) {
                clearActivePolylines();
                activeMarker = marker;

                if (Boolean.TRUE.equals(viewModel.getPlotAllFilteredOnMap().getValue())) {
                    List<LatLng> polylineCoordinates = new ArrayList<>();
                    for (LocationPoint locationPoint : situation.getLocations()) {
                        if (locationPoint.getLatitude() != 0.0 && locationPoint.getLongitude() != 0.0) {
                            polylineCoordinates.add(new LatLng(locationPoint.getLatitude(), locationPoint.getLongitude()));
                        }
                    }
                    drawSafePolylines(polylineCoordinates, situation.getSeverityColour() != 0 ? situation.getSeverityColour() : Color.BLUE, true);
                }
            }
            return false;
        });

        googleMap.setOnInfoWindowCloseListener(marker -> {
            if (marker.equals(activeMarker)) {
                clearActivePolylines();
                activeMarker = null;
            }
        });

        googleMap.setOnInfoWindowClickListener(marker -> {
            Situation situation = (Situation) marker.getTag();
            if (situation != null) {
                lastOpenedSituationId = situation.getSituationId();
                lastOpenedMarkerPosition = marker.getPosition();
                viewModel.selectSituation(situation);
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).showDetailsPaneIfNeeded();
                }
            }
        });

        LatLng scotlandCentreCoordinates = new LatLng(56.4907, -4.2026);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(scotlandCentreCoordinates, 6.5f));
        renderMapMarkers();
    }

    private void renderMapMarkers() {
        if (googleMap == null || viewModel == null) return;
        googleMap.clear();
        clearActivePolylines();
        activeMarker = null;

        Situation singleSituation = viewModel.getSelectedSituation().getValue();
        List<Situation> situationsToPlot = new ArrayList<>();

        if (singleSituation != null && Boolean.FALSE.equals(viewModel.getPlotAllFilteredOnMap().getValue())) {
            situationsToPlot.add(singleSituation);
        } else if (viewModel.getFilteredSituations().getValue() != null) {
            situationsToPlot.addAll(viewModel.getFilteredSituations().getValue());
        }

        if (situationsToPlot.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int plottedPointsCount = 0;
        boolean isSingleSituation = (situationsToPlot.size() == 1);
        Marker exactMarkerToOpen = null;
        Marker fallbackMarkerToOpen = null;

        for (Situation situation : situationsToPlot) {
            List<LatLng> segmentCoordinates = new ArrayList<>();
            float markerHue = (situation.getStreamSource() == Situation.StreamSource.UNPLANNED_INCIDENT)
                    ? BitmapDescriptorFactory.HUE_RED
                    : (situation.getDurationInDays() > 30 ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_GREEN);

            for (LocationPoint locationPoint : situation.getLocations()) {
                if (locationPoint.getLatitude() == 0.0 && locationPoint.getLongitude() == 0.0) continue;

                LatLng coordinates = new LatLng(locationPoint.getLatitude(), locationPoint.getLongitude());
                segmentCoordinates.add(coordinates);
                boundsBuilder.include(coordinates);
                plottedPointsCount++;

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(coordinates)
                        .title(situation.getPrimaryRoadName() + " (" + locationPoint.getPointRole().toUpperCase() + ")")
                        .snippet(situation.getGeneralPublicComment())
                        .icon(BitmapDescriptorFactory.defaultMarker(markerHue)));

                if (marker != null) {
                    marker.setTag(situation);
                    if (lastOpenedSituationId != null && situation.getSituationId().equals(lastOpenedSituationId)) {
                        fallbackMarkerToOpen = marker;
                        if (lastOpenedMarkerPosition != null && coordinates.equals(lastOpenedMarkerPosition)) {
                            exactMarkerToOpen = marker;
                        }
                    }
                }
            }
            if (isSingleSituation) {
                drawSafePolylines(segmentCoordinates, situation.getSeverityColour() != 0 ? situation.getSeverityColour() : Color.BLUE, false);
            }
        }

        Marker markerToOpen = exactMarkerToOpen != null ? exactMarkerToOpen : fallbackMarkerToOpen;
        if (markerToOpen != null && Boolean.TRUE.equals(viewModel.getPlotAllFilteredOnMap().getValue())) {
            markerToOpen.showInfoWindow();
            activeMarker = markerToOpen;
            Situation situationToReselect = (Situation) markerToOpen.getTag();

            if (situationToReselect != null) {
                List<LatLng> polylineCoordinates = new ArrayList<>();
                for (LocationPoint locationPoint : situationToReselect.getLocations()) {
                    if (locationPoint.getLatitude() != 0.0 && locationPoint.getLongitude() != 0.0) {
                        polylineCoordinates.add(new LatLng(locationPoint.getLatitude(), locationPoint.getLongitude()));
                    }
                }
                drawSafePolylines(polylineCoordinates, situationToReselect.getSeverityColour() != 0 ? situationToReselect.getSeverityColour() : Color.BLUE, true);
            }
            lastOpenedSituationId = null;
            lastOpenedMarkerPosition = null;
        }

        if (plottedPointsCount > 0) {
            try {
                if (isSingleSituation) {
                    if (plottedPointsCount > 1) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
                    } else {
                        LatLng zoomTarget = null;
                        if (lastOpenedMarkerPosition != null && lastOpenedSituationId != null
                                && situationsToPlot.get(0).getSituationId().equals(lastOpenedSituationId)) {
                            zoomTarget = lastOpenedMarkerPosition;
                        } else {
                            LocationPoint firstPoint = situationsToPlot.get(0).getLocations().get(0);
                            zoomTarget = new LatLng(firstPoint.getLatitude(), firstPoint.getLongitude());
                        }
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomTarget, 13f));
                    }
                } else if (!wasSingleSituation && (lastPlottedSituations == null || lastPlottedSituations.size() != situationsToPlot.size())) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
                }
            } catch (Exception cameraAnimationException) {
            }
        }
        wasSingleSituation = isSingleSituation;
        lastPlottedSituations = new ArrayList<>(situationsToPlot);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) mapView.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}