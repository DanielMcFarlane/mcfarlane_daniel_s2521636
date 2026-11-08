//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Manages UI-related data, handles filtering logic, and acts as a bridge between the repository and the views.
//

package org.me.gcu.mcfarlane_daniel_s2521636.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.me.gcu.mcfarlane_daniel_s2521636.data.TrafficRepository;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrafficViewModel extends ViewModel {

    public enum StreamFilter {
        ALL,
        INCIDENTS_ONLY,
        ROADWORKS_ONLY
    }

    public static class FeedStatus {
        public enum State { INITIALISING, REFRESHING, LIVE, OFFLINE }
        public final State state;
        public final String timestamp;

        public FeedStatus(State state, String timestamp) {
            this.state = state;
            this.timestamp = timestamp;
        }
    }

    // Default coordinates for GCU if device location is unavailable
    private static final double DEFAULT_GCU_LATITUDE = 55.8672;
    private static final double DEFAULT_GCU_LONGITUDE = -4.2502;

    private final TrafficRepository repository;
    private List<Situation> masterSituations = new ArrayList<>();

    private final MutableLiveData<List<Situation>> filteredSituations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Situation> selectedSituation = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> plotAllFilteredOnMap = new MutableLiveData<>(true);
    private final MutableLiveData<FeedStatus> feedStatus = new MutableLiveData<>();

    private StreamFilter currentStreamFilter = StreamFilter.ALL;
    private String currentRoadQuery = "";
    private double maxDistanceMiles = -1.0;
    private double userLatitude = DEFAULT_GCU_LATITUDE;
    private double userLongitude = DEFAULT_GCU_LONGITUDE;

    public TrafficViewModel(File cacheDirectory) {
        feedStatus.setValue(new FeedStatus(FeedStatus.State.INITIALISING, null));

        repository = new TrafficRepository(cacheDirectory);
        repository.startPeriodicUpdates((situations, isOffline, timestamp) -> {
            masterSituations = situations;
            FeedStatus.State state = isOffline ? FeedStatus.State.OFFLINE : FeedStatus.State.LIVE;
            feedStatus.postValue(new FeedStatus(state, timestamp));
            applyFiltersInternal();
        });
    }

    public LiveData<List<Situation>> getFilteredSituations() {
        return filteredSituations;
    }

    public LiveData<Situation> getSelectedSituation() {
        return selectedSituation;
    }

    public LiveData<Boolean> getPlotAllFilteredOnMap() {
        return plotAllFilteredOnMap;
    }

    public LiveData<FeedStatus> getFeedStatus() {
        return feedStatus;
    }

    public void selectSituation(Situation situation) {
        plotAllFilteredOnMap.setValue(false);
        selectedSituation.setValue(situation);
    }

    public void showAllFilteredOnMap() {
        selectedSituation.setValue(null);
        plotAllFilteredOnMap.setValue(true);
    }

    public void setStreamFilter(StreamFilter streamFilter) {
        this.currentStreamFilter = streamFilter;
        applyFiltersInternal();
    }

    public void setRoadSearchQuery(String searchQuery) {
        this.currentRoadQuery = searchQuery != null ? searchQuery.trim() : "";
        applyFiltersInternal();
    }

    public void setMaxDistanceMiles(double distanceMiles) {
        this.maxDistanceMiles = distanceMiles;
        applyFiltersInternal();
    }

    public void updateUserLocation(double latitude, double longitude) {
        this.userLatitude = latitude;
        this.userLongitude = longitude;
        applyFiltersInternal();
    }

    public double getUserLatitude() {
        return userLatitude;
    }

    public double getUserLongitude() {
        return userLongitude;
    }

    public void refreshDataNow() {
        feedStatus.setValue(new FeedStatus(FeedStatus.State.REFRESHING, null));
        repository.triggerManualRefresh((situations, isOffline, timestamp) -> {
            masterSituations = situations;
            FeedStatus.State state = isOffline ? FeedStatus.State.OFFLINE : FeedStatus.State.LIVE;
            feedStatus.postValue(new FeedStatus(state, timestamp));
            applyFiltersInternal();
        });
    }

    private void applyFiltersInternal() {
        List<Situation> matchingSituations = new ArrayList<>();
        for (Situation situation : masterSituations) {
            if (!matchesStreamType(situation)) continue;
            if (!situation.matchesRoadQuery(currentRoadQuery)) continue;

            if (maxDistanceMiles > 0) {
                double distanceToSituation = situation.getMinimumDistanceMiles(userLatitude, userLongitude);
                if (distanceToSituation > maxDistanceMiles) continue;
            }
            matchingSituations.add(situation);
        }

        // Sort situations by closeness (ascending distance from user location)
        matchingSituations.sort((s1, s2) -> {
            double distance1 = s1.getMinimumDistanceMiles(userLatitude, userLongitude);
            double distance2 = s2.getMinimumDistanceMiles(userLatitude, userLongitude);
            return Double.compare(distance1, distance2);
        });

        filteredSituations.postValue(matchingSituations);
    }

    private boolean matchesStreamType(Situation situation) {
        if (currentStreamFilter == StreamFilter.ALL) return true;
        return (currentStreamFilter == StreamFilter.INCIDENTS_ONLY && situation.getStreamSource() == Situation.StreamSource.UNPLANNED_INCIDENT)
                || (currentStreamFilter == StreamFilter.ROADWORKS_ONLY && situation.getStreamSource() == Situation.StreamSource.CURRENT_ROADWORKS);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}