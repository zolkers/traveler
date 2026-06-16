package dev.traveler.core.settings;

import dev.traveler.core.navigation.camera.CameraAimSettings;
import dev.traveler.core.navigation.camera.CameraTargetSettings;
import dev.traveler.core.navigation.control.ControlProjectionSettings;
import dev.traveler.core.navigation.follow.PathFollowSettings;
import dev.traveler.core.navigation.locomotion.LocomotionSequencerSettings;
import dev.traveler.core.navigation.plan.MovementVectorSettings;
import dev.traveler.core.navigation.recovery.MovementHealthSettings;
import dev.traveler.core.navigation.steering.PathSteeringSettings;
import dev.traveler.core.route.longdistance.LongDistanceRouteSettings;
import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.world.movement.EntityDimensions;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.movement.MovementProfile;
import dev.traveler.core.world.movement.TraversalCost;
import dev.traveler.core.world.movement.TraversalRules;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TravelerSettings {
    public static final Setting<Integer> ROUTE_HORIZONTAL_MARGIN =
            positiveInteger("route.horizontal-margin", 24);
    public static final Setting<Integer> ROUTE_VERTICAL_MARGIN =
            positiveInteger("route.vertical-margin", 8);

    public static final Setting<Double> LONG_DISTANCE_DIRECT_HORIZONTAL_DISTANCE =
            positiveDouble("long-distance.direct-horizontal-distance", 96.0);
    public static final Setting<Double> LONG_DISTANCE_SEGMENT_HORIZONTAL_DISTANCE =
            positiveDouble("long-distance.segment-horizontal-distance", 102.0);
    public static final Setting<Integer> LONG_DISTANCE_MAX_SEGMENT_AXIS_DELTA =
            positiveInteger("long-distance.max-segment-axis-delta", 72);
    public static final Setting<Double> LONG_DISTANCE_REPLAN_DISTANCE =
            positiveDouble("long-distance.replan-distance", 24.0);
    public static final Setting<Integer> LONG_DISTANCE_FRONTIER_VERTICAL_SEARCH_RADIUS =
            nonNegativeInteger("long-distance.frontier-vertical-search-radius", 16);
    public static final Setting<Integer> LONG_DISTANCE_FRONTIER_LATERAL_STEP =
            positiveInteger("long-distance.frontier-lateral-step", 1);
    public static final Setting<Integer> LONG_DISTANCE_FRONTIER_LATERAL_SAMPLES =
            nonNegativeInteger("long-distance.frontier-lateral-samples", 8);
    public static final Setting<Integer> LONG_DISTANCE_FRONTIER_CAPTURE_HORIZONTAL_MARGIN =
            nonNegativeInteger("long-distance.frontier-capture-horizontal-margin", 8);
    public static final Setting<Integer> LONG_DISTANCE_FRONTIER_CAPTURE_VERTICAL_MARGIN =
            nonNegativeInteger("long-distance.frontier-capture-vertical-margin", 16);
    public static final Setting<Integer> LONG_DISTANCE_VISIBILITY_EDGE_SAFETY_BLOCKS =
            nonNegativeInteger("long-distance.visibility-edge-safety-blocks", 24);
    public static final Setting<Integer> LONG_DISTANCE_TARGET_SNAPSHOT_BLOCK_BUDGET =
            positiveInteger("long-distance.target-snapshot-block-budget", 160_000);
    public static final Setting<Integer> LONG_DISTANCE_FRONTIER_FALLBACK_SURFACE_GOAL_LIMIT =
            positiveInteger("long-distance.frontier-fallback-surface-goal-limit", 16);
    public static final Setting<Double> LONG_DISTANCE_MINIMUM_LOOKAHEAD_REPLAN_DISTANCE =
            positiveDouble("long-distance.minimum-lookahead-replan-distance", 48.0);
    public static final Setting<Double> LONG_DISTANCE_LOOKAHEAD_REPLAN_DISTANCE_RATIO =
            positiveDouble("long-distance.lookahead-replan-distance-ratio", 0.5);
    public static final Setting<Double> LONG_DISTANCE_MAXIMUM_LOOKAHEAD_REPLAN_DISTANCE =
            positiveDouble("long-distance.maximum-lookahead-replan-distance", 96.0);
    public static final Setting<Double> LONG_DISTANCE_LOOKAHEAD_REPLAN_SEGMENT_CAP_RATIO =
            positiveDouble("long-distance.lookahead-replan-segment-cap-ratio", 0.75);

    public static final Setting<Double> ENTITY_WIDTH =
            positiveDouble("entity.width", 0.6);
    public static final Setting<Double> ENTITY_HEIGHT =
            positiveDouble("entity.height", 1.8);

    public static final Setting<Boolean> MOVEMENT_CAN_WALK =
            Setting.of("movement.can-walk", Boolean.class, true);
    public static final Setting<Boolean> MOVEMENT_CAN_SWIM =
            Setting.of("movement.can-swim", Boolean.class, true);
    public static final Setting<Boolean> MOVEMENT_CAN_FLY =
            Setting.of("movement.can-fly", Boolean.class, false);
    public static final Setting<Boolean> MOVEMENT_CAN_CROUCH =
            Setting.of("movement.can-crouch", Boolean.class, false);
    public static final Setting<Double> MOVEMENT_MAX_STEP_UP =
            nonNegativeDouble("movement.max-step-up", 0.6);
    public static final Setting<Double> MOVEMENT_MAX_JUMP_HEIGHT =
            nonNegativeDouble("movement.max-jump-height", 1.25);
    public static final Setting<Double> MOVEMENT_MAX_SAFE_FALL_DISTANCE =
            nonNegativeDouble("movement.max-safe-fall-distance", 3.0);

    public static final Setting<Boolean> TRAVERSAL_ALLOW_DIAGONAL =
            Setting.of("traversal.allow-diagonal", Boolean.class, true);
    public static final Setting<Boolean> TRAVERSAL_ALLOW_DIAGONAL_JUMP =
            Setting.of("traversal.allow-diagonal-jump", Boolean.class, false);
    public static final Setting<Boolean> TRAVERSAL_ALLOW_VERTICAL =
            Setting.of("traversal.allow-vertical", Boolean.class, true);
    public static final Setting<Double> TRAVERSAL_DEFAULT_COST =
            positiveDouble("traversal.default-cost", 1.0);

    public static final Setting<Double> CLIMB_FACE_INSET =
            boundedDouble("climb.face-inset", 0.3, 0.0, 0.5);
    public static final Setting<Double> CLIMB_UP_JUMP_THRESHOLD =
            nonNegativeDouble("climb.up-jump-threshold", 0.25);

    public static final Setting<Double> NAVIGATION_REACHED_DISTANCE =
            positiveDouble("navigation.reached-distance", 0.45);
    public static final Setting<Double> NAVIGATION_LOOK_AHEAD_DISTANCE =
            positiveDouble("navigation.look-ahead-distance", 2.0);
    public static final Setting<Double> NAVIGATION_ARRIVAL_DISTANCE =
            positiveDouble("navigation.arrival-distance", 2.5);
    public static final Setting<Double> NAVIGATION_MINIMUM_SPEED_SCALE =
            boundedDouble("navigation.minimum-speed-scale", 0.35, 0.0, 1.0);

    public static final Setting<Double> MOVEMENT_HEALTH_MINIMUM_PROGRESS_DISTANCE =
            nonNegativeDouble("movement-health.minimum-progress-distance", 0.05);
    public static final Setting<Double> MOVEMENT_HEALTH_STUCK_AFTER_SECONDS =
            positiveDouble("movement-health.stuck-after-seconds", 1.5);
    public static final Setting<Double> MOVEMENT_HEALTH_RECOVERY_COOLDOWN_SECONDS =
            nonNegativeDouble("movement-health.recovery-cooldown-seconds", 1.0);
    public static final Setting<Double> MOVEMENT_HEALTH_PATH_DIVERGENCE_DISTANCE =
            nonNegativeDouble("movement-health.path-divergence-distance", 1.2);
    public static final Setting<Double> MOVEMENT_HEALTH_PATH_DIVERGENCE_AFTER_SECONDS =
            positiveDouble("movement-health.path-divergence-after-seconds", 0.75);
    public static final Setting<Double> MOVEMENT_HEALTH_ACTION_SETUP_TIMEOUT_SECONDS =
            positiveDouble("movement-health.action-setup-timeout-seconds", 2.0);
    public static final Setting<Double> MOVEMENT_HEALTH_JUMP_GRACE_SECONDS =
            nonNegativeDouble("movement-health.jump-grace-seconds", 0.35);

    public static final Setting<Double> STEERING_PREDICTION_SECONDS =
            nonNegativeDouble("steering.prediction-seconds", 0.25);
    public static final Setting<Double> STEERING_CORRIDOR_RADIUS =
            nonNegativeDouble("steering.corridor-radius", 0.35);
    public static final Setting<Double> STEERING_LATERAL_CORRECTION_GAIN =
            nonNegativeDouble("steering.lateral-correction-gain", 1.0);
    public static final Setting<Double> STEERING_MAX_CORRECTION_DISTANCE =
            nonNegativeDouble("steering.max-correction-distance", 0.75);
    public static final Setting<Double> STEERING_CLEARANCE_WARNING_LATERAL_ERROR =
            nonNegativeDouble("steering.clearance-warning-lateral-error", 0.65);

    public static final Setting<Double> MOVEMENT_VECTOR_PRESS_THRESHOLD =
            boundedDouble("movement-vector.press-threshold", 0.32, 0.0, 1.0);
    public static final Setting<Double> MOVEMENT_VECTOR_CENTERING_CORRECTION_THRESHOLD =
            nonNegativeDouble("movement-vector.centering-correction-threshold", 0.5);
    public static final Setting<Double> MOVEMENT_VECTOR_TURN_STRAFE_THRESHOLD =
            boundedDouble("movement-vector.turn-strafe-threshold", 0.32, 0.0, 1.0);
    public static final Setting<Double> MOVEMENT_VECTOR_FORWARD_ARC_MINIMUM_FORWARD =
            boundedDouble("movement-vector.forward-arc-minimum-forward", 0.35, 0.0, 1.0);
    public static final Setting<Double> MOVEMENT_VECTOR_BACKPEDAL_MAXIMUM_DISTANCE =
            nonNegativeDouble("movement-vector.backpedal-maximum-distance", 0.8);
    public static final Setting<Double> MOVEMENT_VECTOR_SPECIAL_ACTION_LATERAL_TOLERANCE =
            nonNegativeDouble("movement-vector.special-action-lateral-tolerance", 1.0);
    public static final Setting<Double> MOVEMENT_VECTOR_JUMP_ACTION_LATERAL_TOLERANCE =
            nonNegativeDouble("movement-vector.jump-action-lateral-tolerance", 0.25);

    public static final Setting<Double> CONTROL_PRESS_THRESHOLD =
            boundedDouble("control.press-threshold", 0.32, 0.0, 1.0);
    public static final Setting<Double> CONTROL_RELEASE_THRESHOLD =
            boundedDouble("control.release-threshold", 0.18, 0.0, 1.0);

    public static final Setting<Double> CAMERA_AIM_MAX_YAW_DEGREES_PER_SECOND =
            positiveDouble("camera.aim.max-yaw-degrees-per-second", 540.0);
    public static final Setting<Double> CAMERA_AIM_MAX_PITCH_DEGREES_PER_SECOND =
            positiveDouble("camera.aim.max-pitch-degrees-per-second", 240.0);
    public static final Setting<Double> CAMERA_AIM_RESPONSE =
            positiveDouble("camera.aim.response", 18.0);
    public static final Setting<Double> CAMERA_AIM_DEADZONE_DEGREES =
            nonNegativeDouble("camera.aim.deadzone-degrees", 0.05);

    public static final Setting<Double> CAMERA_TARGET_LOOK_AHEAD_DISTANCE =
            positiveDouble("camera.target.look-ahead-distance", 3.5);
    public static final Setting<Double> CAMERA_TARGET_MINIMUM_HORIZONTAL_DISTANCE =
            positiveDouble("camera.target.minimum-horizontal-distance", 0.35);
    public static final Setting<Double> CAMERA_TARGET_NEUTRAL_PITCH_DEGREES =
            boundedDouble("camera.target.neutral-pitch-degrees", 0.0, -90.0, 90.0);
    public static final Setting<Double> CAMERA_TARGET_VERTICAL_AIM_SCALE =
            boundedDouble("camera.target.vertical-aim-scale", 0.65, 0.0, 1.0);
    public static final Setting<Double> CAMERA_TARGET_MAX_PITCH_DEGREES =
            boundedDouble("camera.target.max-pitch-degrees", 18.0, 0.0, 90.0);

    public static final Setting<Integer> LOCOMOTION_REQUIRED_STABLE_GROUND_FRAMES =
            positiveInteger("locomotion.required-stable-ground-frames", 2);
    public static final Setting<Double> LOCOMOTION_VERTICAL_VELOCITY_TOLERANCE =
            nonNegativeDouble("locomotion.vertical-velocity-tolerance", 0.08);
    public static final Setting<Integer> LOCOMOTION_ACTION_HOLD_FRAMES =
            nonNegativeInteger("locomotion.action-hold-frames", 4);

    private final Map<Setting<?>, Object> overrides;

    private TravelerSettings(Map<Setting<?>, Object> overrides) {
        this.overrides = Map.copyOf(Objects.requireNonNull(overrides, "overrides"));
    }

    public static TravelerSettings standard() {
        return new TravelerSettings(Map.of());
    }

    public <T> T get(Setting<T> setting) {
        Setting<T> safeSetting = Objects.requireNonNull(setting, "setting");
        Object value = overrides.get(safeSetting);
        if (value == null) {
            return safeSetting.defaultValue();
        }
        return safeSetting.type().cast(value);
    }

    public <T> TravelerSettings with(Setting<T> setting, T value) {
        Setting<T> safeSetting = Objects.requireNonNull(setting, "setting");
        T safeValue = safeSetting.validate(value);
        Map<Setting<?>, Object> next = new LinkedHashMap<>(overrides);
        next.put(safeSetting, safeValue);
        return new TravelerSettings(next);
    }

    public RouteSearchSettings routeSearchSettings() {
        return new RouteSearchSettings(
                get(ROUTE_HORIZONTAL_MARGIN),
                get(ROUTE_VERTICAL_MARGIN),
                movementProfile());
    }

    public LongDistanceRouteSettings longDistanceRouteSettings() {
        return new LongDistanceRouteSettings(
                get(LONG_DISTANCE_DIRECT_HORIZONTAL_DISTANCE),
                get(LONG_DISTANCE_SEGMENT_HORIZONTAL_DISTANCE),
                get(LONG_DISTANCE_MAX_SEGMENT_AXIS_DELTA),
                get(LONG_DISTANCE_REPLAN_DISTANCE),
                get(LONG_DISTANCE_FRONTIER_VERTICAL_SEARCH_RADIUS),
                get(LONG_DISTANCE_FRONTIER_LATERAL_STEP),
                get(LONG_DISTANCE_FRONTIER_LATERAL_SAMPLES),
                get(LONG_DISTANCE_FRONTIER_CAPTURE_HORIZONTAL_MARGIN),
                get(LONG_DISTANCE_FRONTIER_CAPTURE_VERTICAL_MARGIN),
                get(LONG_DISTANCE_VISIBILITY_EDGE_SAFETY_BLOCKS),
                get(LONG_DISTANCE_TARGET_SNAPSHOT_BLOCK_BUDGET),
                get(LONG_DISTANCE_FRONTIER_FALLBACK_SURFACE_GOAL_LIMIT),
                get(LONG_DISTANCE_MINIMUM_LOOKAHEAD_REPLAN_DISTANCE),
                get(LONG_DISTANCE_LOOKAHEAD_REPLAN_DISTANCE_RATIO),
                get(LONG_DISTANCE_MAXIMUM_LOOKAHEAD_REPLAN_DISTANCE),
                get(LONG_DISTANCE_LOOKAHEAD_REPLAN_SEGMENT_CAP_RATIO));
    }

    public MovementProfile movementProfile() {
        return new MovementProfile(
                entityDimensions(),
                movementCapabilities(),
                traversalRules());
    }

    public EntityDimensions entityDimensions() {
        return new EntityDimensions(get(ENTITY_WIDTH), get(ENTITY_HEIGHT));
    }

    public MovementCapabilities movementCapabilities() {
        return new MovementCapabilities(
                get(MOVEMENT_CAN_WALK),
                get(MOVEMENT_CAN_SWIM),
                get(MOVEMENT_CAN_FLY),
                get(MOVEMENT_CAN_CROUCH),
                get(MOVEMENT_MAX_STEP_UP),
                get(MOVEMENT_MAX_JUMP_HEIGHT),
                get(MOVEMENT_MAX_SAFE_FALL_DISTANCE));
    }

    public TraversalRules traversalRules() {
        return new TraversalRules(
                get(TRAVERSAL_ALLOW_DIAGONAL),
                get(TRAVERSAL_ALLOW_DIAGONAL_JUMP),
                get(TRAVERSAL_ALLOW_VERTICAL),
                new TraversalCost(get(TRAVERSAL_DEFAULT_COST)));
    }

    public PathFollowSettings pathFollowSettings() {
        return new PathFollowSettings(
                get(NAVIGATION_REACHED_DISTANCE),
                get(NAVIGATION_LOOK_AHEAD_DISTANCE),
                get(NAVIGATION_ARRIVAL_DISTANCE),
                get(NAVIGATION_MINIMUM_SPEED_SCALE));
    }

    public PathSteeringSettings pathSteeringSettings() {
        return new PathSteeringSettings(
                get(NAVIGATION_LOOK_AHEAD_DISTANCE),
                get(STEERING_PREDICTION_SECONDS),
                get(STEERING_CORRIDOR_RADIUS),
                get(STEERING_LATERAL_CORRECTION_GAIN),
                get(STEERING_MAX_CORRECTION_DISTANCE),
                get(STEERING_CLEARANCE_WARNING_LATERAL_ERROR));
    }

    public MovementHealthSettings movementHealthSettings() {
        return new MovementHealthSettings(
                get(MOVEMENT_HEALTH_MINIMUM_PROGRESS_DISTANCE),
                get(MOVEMENT_HEALTH_STUCK_AFTER_SECONDS),
                get(MOVEMENT_HEALTH_RECOVERY_COOLDOWN_SECONDS),
                get(MOVEMENT_HEALTH_PATH_DIVERGENCE_DISTANCE),
                get(MOVEMENT_HEALTH_PATH_DIVERGENCE_AFTER_SECONDS),
                get(MOVEMENT_HEALTH_ACTION_SETUP_TIMEOUT_SECONDS),
                get(MOVEMENT_HEALTH_JUMP_GRACE_SECONDS));
    }

    public MovementVectorSettings movementVectorSettings() {
        return new MovementVectorSettings(
                get(MOVEMENT_VECTOR_PRESS_THRESHOLD),
                get(MOVEMENT_VECTOR_CENTERING_CORRECTION_THRESHOLD),
                get(MOVEMENT_VECTOR_TURN_STRAFE_THRESHOLD),
                get(MOVEMENT_VECTOR_FORWARD_ARC_MINIMUM_FORWARD),
                get(MOVEMENT_VECTOR_BACKPEDAL_MAXIMUM_DISTANCE),
                get(MOVEMENT_VECTOR_SPECIAL_ACTION_LATERAL_TOLERANCE),
                get(MOVEMENT_VECTOR_JUMP_ACTION_LATERAL_TOLERANCE));
    }

    public ControlProjectionSettings controlProjectionSettings() {
        return new ControlProjectionSettings(
                get(CONTROL_PRESS_THRESHOLD),
                get(CONTROL_RELEASE_THRESHOLD));
    }

    public CameraAimSettings cameraAimSettings() {
        return new CameraAimSettings(
                get(CAMERA_AIM_MAX_YAW_DEGREES_PER_SECOND),
                get(CAMERA_AIM_MAX_PITCH_DEGREES_PER_SECOND),
                get(CAMERA_AIM_RESPONSE),
                get(CAMERA_AIM_DEADZONE_DEGREES));
    }

    public CameraTargetSettings cameraTargetSettings() {
        return new CameraTargetSettings(
                get(CAMERA_TARGET_LOOK_AHEAD_DISTANCE),
                get(CAMERA_TARGET_MINIMUM_HORIZONTAL_DISTANCE),
                get(CAMERA_TARGET_NEUTRAL_PITCH_DEGREES),
                get(CAMERA_TARGET_VERTICAL_AIM_SCALE),
                get(CAMERA_TARGET_MAX_PITCH_DEGREES));
    }

    public LocomotionSequencerSettings locomotionSequencerSettings() {
        return new LocomotionSequencerSettings(
                get(LOCOMOTION_REQUIRED_STABLE_GROUND_FRAMES),
                get(LOCOMOTION_VERTICAL_VELOCITY_TOLERANCE),
                get(LOCOMOTION_ACTION_HOLD_FRAMES));
    }

    private static Setting<Integer> positiveInteger(String key, int defaultValue) {
        return Setting.of(
                key,
                Integer.class,
                defaultValue,
                value -> value > 0,
                key + " must be positive.");
    }

    private static Setting<Integer> nonNegativeInteger(String key, int defaultValue) {
        return Setting.of(
                key,
                Integer.class,
                defaultValue,
                value -> value >= 0,
                key + " must be non-negative.");
    }

    private static Setting<Double> positiveDouble(String key, double defaultValue) {
        return Setting.of(
                key,
                Double.class,
                defaultValue,
                TravelerSettings::isPositive,
                key + " must be positive and finite.");
    }

    private static Setting<Double> nonNegativeDouble(String key, double defaultValue) {
        return Setting.of(
                key,
                Double.class,
                defaultValue,
                TravelerSettings::isNonNegative,
                key + " must be non-negative and finite.");
    }

    private static Setting<Double> boundedDouble(String key, double defaultValue, double minimum, double maximum) {
        return Setting.of(
                key,
                Double.class,
                defaultValue,
                value -> Double.isFinite(value) && value >= minimum && value <= maximum,
                key + " must be between " + minimum + " and " + maximum + ".");
    }

    private static boolean isPositive(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    private static boolean isNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }
}
