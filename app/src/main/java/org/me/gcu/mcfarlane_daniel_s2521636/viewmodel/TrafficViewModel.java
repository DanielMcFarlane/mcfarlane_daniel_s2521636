//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Manages UI-related data, handles filtering logic, and acts as a bridge between the repository and the views.
//

package org.me.gcu.mcfarlane_daniel_s2521636.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import org.me.gcu.mcfarlane_daniel_s2521636.data.TrafficRepository;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import java.util.ArrayList;
import java.util.List;

public class TrafficViewModel extends AndroidViewModel {

    public enum StreamFilter {
        ALL,
        INCIDENTS_ONLY,
        ROADWORKS_ONLY
    }

    private final TrafficRepository repository;
    private List<Situation> masterSituations = new ArrayList<>();
    private final MutableLiveData<List<Situation>> filteredSituations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Situation> selectedSituation = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> plotAllFilteredOnMap = new MutableLiveData<>(true);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Initialising feed...");

    private StreamFilter currentStreamFilter = StreamFilter.ALL;
    private String currentRoadQuery = "";
    private double maxDistanceMiles = -1.0;

    // Default coordinates for Glasgow city centre if device location is unavailable
    private double userLatitude = 55.8669;
    private double userLongitude = -4.2504;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        repository.startPeriodicUpdates((situations, status) -> {
            masterSituations = situations;
            statusMessage.postValue(status);
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

    public LiveData<String> getStatusMessage() {
        return statusMessage;
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
        statusMessage.setValue("Refreshing Traffic Scotland feeds...");
        repository.triggerManualRefresh((situations, status) -> {
            masterSituations = situations;
            statusMessage.postValue(status);
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