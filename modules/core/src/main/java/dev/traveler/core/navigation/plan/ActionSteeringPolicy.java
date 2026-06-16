package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;

public interface ActionSteeringPolicy {
    SteeringPlan steeringFor(
            LocomotionPlan action,
            NavigationPath path,
            WorldPoint position,
            int nextNodeIndex,
            WorldPoint actionTarget,
            SteeringPlan pathSteering);
}
