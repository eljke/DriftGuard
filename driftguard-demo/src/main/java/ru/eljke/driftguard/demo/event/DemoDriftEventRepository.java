package ru.eljke.driftguard.demo.event;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.util.Collection;
import java.util.List;

/**
 * English demo documentation.
 *
 * English demo documentation.
 * English demo documentation.
 * synthetic run, live playback or Kafka demo.</p>
 */
public interface DemoDriftEventRepository {
    void append(String source, String runId, DriftEvent event);

    default void appendAll(String source, String runId, Collection<DriftEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(event -> append(source, runId, event));
    }

    List<DemoStoredDriftEvent> recent(int limit);

    List<DriftEvent> recentEvents(int limit);

    void clear();
}


