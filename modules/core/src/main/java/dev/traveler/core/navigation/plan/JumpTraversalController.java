package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.locomotion.AgentMotionState;
import dev.traveler.core.common.geometry.HorizontalVector;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.util.Objects;
import java.util.Optional;

public final class JumpTraversalController {
    private static final double ZERO_LENGTH = 1.0E-6;

    private final MovementVectorSettings settings;

    public JumpTraversalController(MovementVectorSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public Optional<MovementVectorIntent> movementVectorFor(
            MovementAction segmentAction,
            MovementActionDecision actionDecision,
            WorldPoint position,
            WorldPoint segmentStart,
            WorldPoint actionTarget,
            AgentMotionState motionState) {
        MovementAction action = Objects.requireNonNull(segmentAction, "segmentAction");
        MovementActionDecision decision = Objects.requireNonNull(actionDecision, "actionDecision");
        WorldPoint currentPosition = Objects.requireNonNull(position, "position");
        WorldPoint start = Objects.requireNonNull(segmentStart, "segmentStart");
        WorldPoint target = Objects.requireNonNull(actionTarget, "actionTarget");
        AgentMotionState motion = Objects.requireNonNull(motionState, "motionState");
        if (action != MovementAction.JUMP && action != MovementAction.STEP_UP) {
            return Optional.empty();
        }
        HorizontalVector axis = jumpAxis(start, target);
        if (axis.isZero()) {
            return Optional.empty();
        }
        if (decision.retainActionTarget()) {
            return Optional.of(new MovementVectorIntent(axis, PlannedMovementMode.DIRECT, true));
        }
        if (motion.onGround()
                && lateralError(start, target, currentPosition) > settings.jumpActionLateralTolerance()) {
            return Optional.of(new MovementVectorIntent(
                    lateralCorrection(start, target, currentPosition),
                    PlannedMovementMode.SIDESTEP_RECENTER,
                    false));
        }
        return Optional.of(new MovementVectorIntent(axis, PlannedMovementMode.DIRECT, true));
    }

    private static HorizontalVector jumpAxis(WorldPoint segmentStart, WorldPoint actionTarget) {
        HorizontalVector vector = segmentStart.horizontalVectorTo(actionTarget);
        if (vector.length() <= ZERO_LENGTH) {
            return new HorizontalVector(0.0, 0.0);
        }
        return vector.normalized();
    }

    private static HorizontalVector lateralCorrection(
            WorldPoint segmentStart,
            WorldPoint actionTarget,
            WorldPoint position) {
        double deltaX = actionTarget.x() - segmentStart.x();
        double deltaZ = actionTarget.z() - segmentStart.z();
        double lengthSquared = deltaX * deltaX + deltaZ * deltaZ;
        if (lengthSquared <= ZERO_LENGTH) {
            return position.horizontalVectorTo(actionTarget);
        }
        double offsetX = position.x() - segmentStart.x();
        double offsetZ = position.z() - segmentStart.z();
        double ratio = Math.clamp((offsetX * deltaX + offsetZ * deltaZ) / lengthSquared, 0.0, 1.0);
        WorldPoint nearest = new WorldPoint(
                segmentStart.x() + deltaX * ratio,
                position.y(),
                segmentStart.z() + deltaZ * ratio);
        return position.horizontalVectorTo(nearest);
    }

    private static double lateralError(
            WorldPoint segmentStart,
            WorldPoint actionTarget,
            WorldPoint position) {
        return lateralCorrection(segmentStart, actionTarget, position).length();
    }
}
