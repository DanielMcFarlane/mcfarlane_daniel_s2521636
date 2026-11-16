//
// Name                  Daniel McFarlane
// Student ID            S2521636
// Programme of Study    Mobile Platform Development
//
// Tests the MainActivity UI components, navigation and landscape layout configuration.
//

package org.me.gcu.mcfarlane_daniel_s2521636;

import android.content.pm.ActivityInfo;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.me.gcu.mcfarlane_daniel_s2521636.view.MainActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    // Initialise the ActivityScenarioRule to launch and manage the lifecycle of MainActivity prior to executing each test
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    // Automatically grants the location permission when the test launches so the system dialog never appears and stalls the test
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_COARSE_LOCATION);

    @Test
    public void bottomNavigationIsDisplayed() {
        // Assert that the primary BottomNavigationView component successfully renders and is visible within the user's view hierarchy
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()));
    }

    @Test
    public void searchInputIsDisplayed() {
        // Assert that the road search EditText filtering component is properly instantiated and displayed on the screen
        onView(withId(R.id.roadSearchInput)).check(matches(isDisplayed()));
    }

    @Test
    public void landscapeLayoutShowsBothPanes() {
        // Rotate the emulator screen to landscape orientation
        activityRule.getScenario().onActivity(activity ->
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        );

        // Validate that the dedicated landscape XML has loaded by checking that the portrait-only bottomNav is completely removed
        onView(withId(R.id.bottomNav)).check(doesNotExist());

        // Assert that both the list and detail fragment containers are visible simultaneously on the screen
        onView(withId(R.id.listFragmentContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.detailFragmentContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void mapComponentIsDisplayedWhenNavigated() {
        // Programmatically click the 'Live Map' button in the bottom navigation menu
        onView(withId(R.id.nav_map)).perform(click());

        // Assert that the Google MapView component inside the detail fragment successfully renders to the user
        onView(withId(R.id.mapView)).check(matches(isDisplayed()));
    }

    @Test
    public void mapSatelliteToggle_executesWithoutCrashing() {
        // Navigate to the map view
        onView(withId(R.id.nav_map)).perform(click());

        // Assert the toggle card is displayed, then perform a click to ensure it updates state without crashing
        onView(withId(R.id.mapTypeToggleCard)).check(matches(isDisplayed())).perform(click());
    }
}