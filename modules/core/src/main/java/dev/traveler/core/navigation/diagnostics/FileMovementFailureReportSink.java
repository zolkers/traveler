package dev.traveler.core.navigation.diagnostics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class FileMovementFailureReportSink implements MovementFailureReportSink {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC);
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final Path reportDirectory;

    public FileMovementFailureReportSink(Path reportDirectory) {
        this.reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory");
    }

    @Override
    public void write(MovementFailureReport report) {
        MovementFailureReport safeReport = Objects.requireNonNull(report, "report");
        try {
            Files.createDirectories(reportDirectory);
            Files.writeString(reportPath(safeReport), safeReport.renderText());
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private Path reportPath(MovementFailureReport report) {
        String name = FILE_STAMP.format(report.createdAt())
                + "-"
                + report.context().failure().kind().name().toLowerCase(java.util.Locale.ROOT)
                + "-"
                + SEQUENCE.incrementAndGet()
                + ".txt";
        return reportDirectory.resolve(name);
    }
}
