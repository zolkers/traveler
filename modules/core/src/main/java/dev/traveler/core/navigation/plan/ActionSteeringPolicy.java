package dev.traveler.core.navigation.plan;

import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.navigation.locomotion.LocomotionPlan;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.navigation.steering.SteeringPlan;

public interface ActionSteeringPolicy {
    SteeringPlan steeringFor(
            LocomotionPlan action,
            NavigationPath path,
            NavigationPoint position,
            int nextNodeIndex,
            NavigationPoint actionTarget,
            SteeringPlan pathSteering);
}
