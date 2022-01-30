package ru.serega6531.packmate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.serega6531.packmate.service.StreamService;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@ConditionalOnProperty(name = "old-streams-cleanup-enabled", havingValue = "true")
public class OldStreamsCleanupTask {

    private final StreamService service;
    private final int oldStreamsThreshold;

    public OldStreamsCleanupTask(StreamService service, @Value("${old-streams-threshold}") int oldStreamsThreshold) {
        this.service = service;
        this.oldStreamsThreshold = oldStreamsThreshold;
    }

    @Scheduled(fixedDelayString = "PT${cleanup-interval}M", initialDelayString = "PT1M")
    public void cleanup() {
        ZonedDateTime before = ZonedDateTime.now().minus(oldStreamsThreshold, ChronoUnit.MINUTES);
        log.info("Cleaning up old non-favorite streams (before {})", before);
        long deleted = service.cleanupOldStreams(before);
        log.info("Deleted {} rows", deleted);
    }

}
