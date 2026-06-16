package dev.traveler.core.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.follow.PathFollowSettings;
import dev.traveler.core.navigation.steering.PathSteeringSettings;
import dev.traveler.core.route.RouteSearchSettings;
import org.junit.jupiter.api.Test;

class TravelerSettingsTest {
    @Test
    void settingKeepsTypedKeyDefaultAndValidation() {
        Setting<Integer> setting = Setting.of(
                "route.horizontal-margin",
                Integer.class,
                24,
                value -> value > 0,
                "margin must be positive");

        assertEquals("route.horizontal-margin", setting.key());
        assertEquals(Integer.class, setting.type());
        assertEquals(24, setting.defaultValue());
        assertEquals(8, setting.validate(8));
        assertThrows(IllegalArgumentException.class, () -> setting.validate(0));
    }

    @Test
    void defaultTravelerSettingsBuildExistingRouteSettings() {
        TravelerSettings settings = TravelerSettings.standard();

        RouteSearchSettings route = settings.routeSearchSettings();

        assertEquals(24, route.horizontalMargin());
        assertEquals(8, route.verticalMargin());
        assertTrue(route.movementProfile().capabilities().canSwim());
        assertEquals(3.0, route.movementProfile().capabilities().maxSafeFallDistance());
    }

    @Test
    void settingsCanOverrideMovementAndSearchValuesImmutably() {
        TravelerSettings standard = TravelerSettings.standard();
        TravelerSettings custom = standard
                .with(TravelerSettings.ROUTE_HORIZONTAL_MARGIN, 12)
                .with(TravelerSettings.MOVEMENT_CAN_SWIM, false)
                .with(TravelerSettings.MOVEMENT_MAX_SAFE_FALL_DISTANCE, 10.0);

        assertEquals(24, standard.routeSearchSettings().horizontalMargin());
        assertTrue(standard.movementCapabilities().canSwim());

        assertEquals(12, custom.routeSearchSettings().horizontalMargin());
        assertFalse(custom.movementCapabilities().canSwim());
        assertEquals(10.0, custom.movementCapabilities().maxSafeFallDistance());
    }

    @Test
    void centralSettingsBuildNavigationValueObjects() {
        TravelerSettings settings = TravelerSettings.standard()
                .with(TravelerSettings.NAVIGATION_REACHED_DISTANCE, 0.5)
                .with(TravelerSettings.NAVIGATION_LOOK_AHEAD_DISTANCE, 3.0);

        PathFollowSettings follow = settings.pathFollowSettings();
        PathSteeringSettings steering = settings.pathSteeringSettings();

        assertEquals(0.5, follow.reachedDistance());
        assertEquals(3.0, follow.lookAheadDistance());
        assertEquals(3.0, steering.pathOffset());
        assertEquals(0.35, steering.lateralCorrectionDerivativeGain());
        assertEquals(0.75, steering.minimumPathOffset());
        assertEquals(1.0, steering.lateralErrorLookaheadReductionGain());
        assertEquals(1.25, steering.actionApproachPathOffset());
    }

    @Test
    void settingOverridesAreValidated() {
        TravelerSettings settings = TravelerSettings.standard();

        assertThrows(
                IllegalArgumentException.class,
                () -> settings.with(TravelerSettings.MOVEMENT_MAX_SAFE_FALL_DISTANCE, -1.0));
    }
}
