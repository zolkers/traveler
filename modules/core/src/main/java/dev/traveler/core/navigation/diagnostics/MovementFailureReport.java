package dev.traveler.core.navigation.diagnostics;

import dev.traveler.core.navigation.NavigationControlFrame;
import dev.traveler.core.navigation.follow.NavigationPath;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.behavior.decision.MovementAction;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MovementFailureReport(
        Instant createdAt,
        MovementFailureReportContext context,
        List<BlockScanSample> blockScan) {
    public MovementFailureReport {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(context, "context");
        blockScan = List.copyOf(Objects.requireNonNull(blockScan, "blockScan"));
    }

    public String renderText() {
        StringBuilder output = new StringBuilder();
        appendHeader(output);
        appendMove(output);
        appendBlockScan(output);
        return output.toString();
    }

    private void appendHeader(StringBuilder output) {
        output.append("traveler movement failure report\n");
        output.append("createdAt=").append(createdAt).append('\n');
        output.append("failure=").append(context.failure().kind()).append('\n');
        output.append("failure.message=").append(context.failure().message()).append('\n');
        output.append("session.message=").append(context.session().message()).append('\n');
    }

    private void appendMove(StringBuilder output) {
        NavigationControlFrame frame = context.frame();
        NavigationPath path = context.session().path();
        int nextNodeIndex = Math.clamp(
                frame.state().progress().nextNodeIndex(),
                1,
                path.nodeCount() - 1);
        MovementAction action = path.actionBeforeNode(nextNodeIndex);
        WorldPoint from = path.nodeAt(nextNodeIndex - 1);
        WorldPoint to = path.nodeAt(nextNodeIndex);
        output.append("possibleFailedMove=action=")
                .append(action)
                .append(" phase=")
                .append(frame.plan().phase())
                .append(" from=")
                .append(from)
                .append(" to=")
                .append(to)
                .append(" actionTarget=")
                .append(path.actionTargetBeforeNode(nextNodeIndex))
                .append(" intent=")
                .append(frame.intent())
                .append('\n');
        output.append("expected.position=").append(frame.movementTarget().point()).append('\n');
        output.append("expected.camera=").append(frame.cameraAngles()).append('\n');
        output.append("got.position=").append(context.input().position()).append('\n');
        output.append("got.camera=").append(context.input().cameraAngles()).append('\n');
        output.append("got.motion=").append(context.input().motionState()).append('\n');
    }

    private void appendBlockScan(StringBuilder output) {
        output.append("blockScan.size=").append(blockScan.size()).append('\n');
        output.append("blocks:\n");
        for (BlockScanSample sample : blockScan) {
            output.append("block=")
                    .append(sample.position().x())
                    .append(',')
                    .append(sample.position().y())
                    .append(',')
                    .append(sample.position().z())
                    .append(' ')
                    .append(sample.block())
                    .append(" passability=")
                    .append(sample.passability())
                    .append(" fluid=")
                    .append(sample.fluid())
                    .append(" behavior=")
                    .append(sample.behavior())
                    .append('\n');
        }
    }
}
