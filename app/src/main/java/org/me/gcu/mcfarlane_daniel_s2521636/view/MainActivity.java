//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Main entry point of the app, handling UI layout transitions, navigation, and location permissions.
//

package org.me.gcu.mcfarlane_daniel_s2521636.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.me.gcu.mcfarlane_daniel_s2521636.R;
import org.me.gcu.mcfarlane_daniel_s2521636.viewmodel.TrafficViewModel;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String KEY_PORTRAIT_SHOWING_DETAILS = "portrait_showing_details";

    private TrafficViewModel viewModel;
    private View listContainer;
    private View detailContainer;
    private boolean isShowingDetailsInPortrait = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Let the app background stretch under the camera notch in landscape mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);

        listContainer = findViewById(R.id.listFragmentContainer);
        detailContainer = findViewById(R.id.detailFragmentContainer);

        // Adjust the layout so it doesn't overlap with system bars or screen notches
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets safeInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            View topHeader = findViewById(R.id.topHeader);
            View filterSection = findViewById(R.id.filterSection);
            View bottomNav = findViewById(R.id.bottomNav);
            View textCard = findViewById(R.id.textCard);

            int px16 = getResources().getDimensionPixelSize(R.dimen.padding_large);
            int px10 = getResources().getDimensionPixelSize(R.dimen.padding_medium);
            int px4 = getResources().getDimensionPixelSize(R.dimen.padding_small);

            int topOffset = Math.max(0, safeInsets.top - px4);

            if (isLandscape) {
                view.setPadding(0, 0, 0, 0);
                if (topHeader != null) {
                    topHeader.setPadding(Math.max(safeInsets.left, px16), safeInsets.top, px16, px16);
                }
                if (filterSection != null) {
                    filterSection.setPadding(Math.max(safeInsets.left, px16), px10, px10, px10);
                }
                if (listContainer != null) {
                    listContainer.setPadding(Math.max(safeInsets.left, px16), 0, 0, 0);
                }
                if (detailContainer != null) {
                    detailContainer.setPadding(0, 0, 0, 0);
                }
                if (bottomNav != null) {
                    bottomNav.setPadding(safeInsets.left, bottomNav.getPaddingTop(), safeInsets.right, safeInsets.bottom);
                }
                if (textCard != null) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) textCard.getLayoutParams();
                    params.leftMargin = safeInsets.left + px16;
                    params.bottomMargin = px16;
                    params.topMargin = px10;
                    params.rightMargin = px10;
                    textCard.setLayoutParams(params);
                }
            } else {
                view.setPadding(safeInsets.left, 0, safeInsets.right, 0);
                if (topHeader != null) {
                    topHeader.setPadding(topHeader.getPaddingLeft(), topOffset, topHeader.getPaddingRight(), topHeader.getPaddingBottom());
                }
                if (bottomNav != null) {
                    bottomNav.setPadding(bottomNav.getPaddingLeft(), bottomNav.getPaddingTop(), bottomNav.getPaddingRight(), safeInsets.bottom / 2);
                }
            }
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState != null) {
            isShowingDetailsInPortrait = savedInstanceState.getBoolean(KEY_PORTRAIT_SHOWING_DETAILS, false);
        }

        // Initialise ViewModel using a Factory to pass the cache directory instead of the Application context
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TrafficViewModel(getFilesDir());
            }
        }).get(TrafficViewModel.class);

        setupViews();
        updatePaneVisibility();
        requestCoarseLocationIfPermitted();

        viewModel.getPlotAllFilteredOnMap().observe(this, shouldPlotAll -> updatePaneVisibility());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
                if (isShowingDetailsInPortrait) {
                    if (bottomNav != null && bottomNav.getSelectedItemId() == R.id.nav_map) {
                        if (Boolean.FALSE.equals(viewModel.getPlotAllFilteredOnMap().getValue())) {
                            viewModel.showAllFilteredOnMap();
                        } else {
                            bottomNav.setSelectedItemId(R.id.nav_list);
                        }
                    } else {
                        isShowingDetailsInPortrait = false;
                        updatePaneVisibility();
                    }
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupViews() {
        TextView statusBanner = findViewById(R.id.statusBanner);
        EditText roadSearchInput = findViewById(R.id.roadSearchInput);
        EditText distanceInput = findViewById(R.id.distanceInput);
        View refreshButton = findViewById(R.id.refreshButton);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (statusBanner != null) {
            viewModel.getFeedStatus().observe(this, status -> {
                if (status == null) return;
                switch (status.state) {
                    case INITIALISING:
                        statusBanner.setText(getString(R.string.status_initialising));
                        break;
                    case REFRESHING:
                        statusBanner.setText(getString(R.string.status_refreshing));
                        break;
                    case OFFLINE:
                        statusBanner.setText(getString(R.string.status_offline, status.timestamp));
                        break;
                    case LIVE:
                        statusBanner.setText(getString(R.string.status_live, status.timestamp));
                        break;
                }
            });
        }

        if (roadSearchInput != null) {
            roadSearchInput.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence inputText, int start, int before, int count) {
                    viewModel.setRoadSearchQuery(inputText.toString());
                }
            });
        }

        if (distanceInput != null) {
            distanceInput.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence inputText, int start, int before, int count) {
                    try {
                        double parsedDistance = inputText.toString().trim().isEmpty() ? -1.0 : Double.parseDouble(inputText.toString().trim());
                        viewModel.setMaxDistanceMiles(parsedDistance);
                    } catch (NumberFormatException formatException) {
                        viewModel.setMaxDistanceMiles(-1.0);
                    }
                }
            });
        }

        if (refreshButton != null) {
            refreshButton.setOnClickListener(view -> viewModel.refreshDataNow());
        }

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_list) {
                    isShowingDetailsInPortrait = false;
                    updatePaneVisibility();
                    return true;
                }
                if (item.getItemId() == R.id.nav_map) {
                    viewModel.showAllFilteredOnMap();
                    isShowingDetailsInPortrait = true;
                    updatePaneVisibility();
                    return true;
                }
                return false;
            });
        }
    }

    public void showDetailsPaneIfNeeded() {
        isShowingDetailsInPortrait = true;
        updatePaneVisibility();
    }

    private void updatePaneVisibility() {
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        View filterSection = findViewById(R.id.filterSection);
        Boolean isPlotAll = viewModel.getPlotAllFilteredOnMap().getValue();
        boolean showSearch = isPlotAll != null ? isPlotAll : true;

        if (isLandscape) {
            if (listContainer != null) listContainer.setVisibility(View.VISIBLE);
            if (detailContainer != null) detailContainer.setVisibility(View.VISIBLE);
            if (filterSection != null) filterSection.setVisibility(View.VISIBLE);
            return;
        }

        if (listContainer != null) listContainer.setVisibility(isShowingDetailsInPortrait ? View.GONE : View.VISIBLE);
        if (detailContainer != null) detailContainer.setVisibility(isShowingDetailsInPortrait ? View.VISIBLE : View.GONE);

        if (isShowingDetailsInPortrait && !showSearch) {
            if (filterSection != null) filterSection.setVisibility(View.GONE);
        } else {
            if (filterSection != null) filterSection.setVisibility(View.VISIBLE);
        }
    }

    private void requestCoarseLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceCoarseLocation();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void fetchDeviceCoarseLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lastKnownLocation != null) {
                    viewModel.updateUserLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                }
            }
        } catch (SecurityException locationAccessException) {
            Log.e("MainActivity", "Location permission revoked or unavailable", locationAccessException);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchDeviceCoarseLocation();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PORTRAIT_SHOWING_DETAILS, isShowingDetailsInPortrait);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable editable) {}
    }
}