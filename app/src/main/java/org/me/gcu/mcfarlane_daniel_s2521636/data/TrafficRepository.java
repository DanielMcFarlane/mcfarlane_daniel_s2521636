//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Manages data retrieval from the Traffic Scotland API, including background fetching and local caching.
//

package org.me.gcu.mcfarlane_daniel_s2521636.data;

import android.util.Log;
import org.me.gcu.mcfarlane_daniel_s2521636.BuildConfig;
import org.me.gcu.mcfarlane_daniel_s2521636.model.Situation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrafficRepository {

    private final String clientId;
    private final String clientKey;
    private final File cacheDirectory;

    private static final String UNPLANNED_EVENTS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String CURRENT_ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";
    private static final String CACHE_UNPLANNED_FILE = "cached_unplanned.xml";
    private static final String CACHE_ROADWORKS_FILE = "cached_roadworks.xml";

    private static final long AUTO_UPDATE_INTERVAL_MINUTES = 60;

    public interface RepositoryCallback {
        void onDataUpdated(List<Situation> situations, boolean isOfflineMode, String timestamp);
    }

    private final TrafficParser parser = new TrafficParser();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TrafficRepository(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        this.clientId = BuildConfig.TRAFFIC_CLIENT_ID;
        this.clientKey = BuildConfig.TRAFFIC_CLIENT_KEY;
    }

    public void startPeriodicUpdates(RepositoryCallback callback) {
        scheduler.scheduleAtFixedRate(
                () -> fetchBothStreams(callback),
                0,
                AUTO_UPDATE_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public void triggerManualRefresh(RepositoryCallback callback) {
        scheduler.execute(() -> fetchBothStreams(callback));
    }

    private void fetchBothStreams(RepositoryCallback callback) {
        boolean isOfflineMode = false;

        String unplannedXmlContent = downloadXmlStream(UNPLANNED_EVENTS_URL);
        if (unplannedXmlContent != null && !unplannedXmlContent.isEmpty()) {
            saveToInternalCache(CACHE_UNPLANNED_FILE, unplannedXmlContent);
        } else {
            unplannedXmlContent = readFromInternalCache(CACHE_UNPLANNED_FILE, null);
            isOfflineMode = true;
        }

        String roadworksXmlContent = downloadXmlStream(CURRENT_ROADWORKS_URL);
        if (roadworksXmlContent != null && !roadworksXmlContent.isEmpty()) {
            saveToInternalCache(CACHE_ROADWORKS_FILE, roadworksXmlContent);
        } else {
            roadworksXmlContent = readFromInternalCache(CACHE_ROADWORKS_FILE, null);
            isOfflineMode = true;
        }

        List<Situation> combinedSituations = new ArrayList<>();
        if (unplannedXmlContent != null && !unplannedXmlContent.isEmpty()) {
            combinedSituations.addAll(parser.parseXmlString(unplannedXmlContent, Situation.StreamSource.UNPLANNED_INCIDENT));
        }

        if (roadworksXmlContent != null && !roadworksXmlContent.isEmpty()) {
            combinedSituations.addAll(parser.parseXmlString(roadworksXmlContent, Situation.StreamSource.CURRENT_ROADWORKS));
        }

        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        callback.onDataUpdated(combinedSituations, isOfflineMode, timestamp);
    }

    private String downloadXmlStream(String endpointUrl) {
        if (clientId.equals("TRAFFIC_SCOTLAND_CLIENT_ID") || clientKey.equals("TRAFFIC_SCOTLAND_CLIENT_KEY")) {
            Log.w("TrafficRepository", "Placeholder credentials detected; cannot connect to live feed.");
            return null;
        }

        HttpURLConnection httpConnection = null;
        try {
            URL url = new URL(endpointUrl);
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setConnectTimeout(8000);
            httpConnection.setReadTimeout(8000);

            // DATEX II requires Base64 encoded Basic Authentication
            String credentials = clientId + ":" + clientKey;
            String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            httpConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);

            if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e("TrafficRepository", "HTTP Error: " + httpConnection.getResponseCode() + " for URL: " + endpointUrl);
                return null;
            }

            StringBuilder xmlContentBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), StandardCharsets.UTF_8))) {
                String xmlLine;
                while ((xmlLine = reader.readLine()) != null) {
                    xmlContentBuilder.append(xmlLine);
                }
            }
            return xmlContentBuilder.toString();
        } catch (Exception networkException) {
            Log.e("TrafficRepository", "Network error downloading " + endpointUrl, networkException);
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        return null;
    }

    private void saveToInternalCache(String fileName, String content) {
        File file = new File(cacheDirectory, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception cacheWriteException) {
            Log.e("TrafficRepository", "Failed to cache XML", cacheWriteException);
        }
    }

    private String readFromInternalCache(String fileName, String defaultFallback) {
        File file = new File(cacheDirectory, fileName);
        if (!file.exists()) {
            return defaultFallback;
        }

        try (FileInputStream inputStream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            StringBuilder cachedContentBuilder = new StringBuilder();
            String cachedLine;
            while ((cachedLine = reader.readLine()) != null) {
                cachedContentBuilder.append(cachedLine);
            }
            if (cachedContentBuilder.length() > 0) {
                return cachedContentBuilder.toString();
            }
        } catch (Exception cacheReadException) {
            Log.d("TrafficRepository", "Cache file miss or format issue, normal for first launch.", cacheReadException);
        }
        return defaultFallback;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}