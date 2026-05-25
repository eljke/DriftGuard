package ru.eljke.driftguard.demo.event;

import org.springframework.stereotype.Repository;
import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * English demo documentation.
 *
 * English demo documentation.
 * English demo documentation.
 * English demo documentation.
 */
@Repository
public class InMemoryDemoDriftEventRepository implements DemoDriftEventRepository {
    private static final int MAX_EVENTS = 2_000;

    private final List<DemoStoredDriftEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void append(String source, String runId, DriftEvent event) {
        if (event == null) {
            return;
        }
        events.add(new DemoStoredDriftEvent(source, runId, Instant.now(), event));
        trim();
    }

    @Override
    public List<DemoStoredDriftEvent> recent(int limit) {
        int safeLimit = Math.max(1, limit);
        return events.stream()
                .sorted(Comparator.comparing(DemoStoredDriftEvent::receivedAt).reversed())
                .limit(safeLimit)
                .toList();
    }

    @Override
    public List<DriftEvent> recentEvents(int limit) {
        return recent(limit).stream()
                .map(DemoStoredDriftEvent::event)
                .toList();
    }

    @Override
    public void clear() {
        events.clear();
    }

    private void trim() {
        int overflow = events.size() - MAX_EVENTS;
        if (overflow <= 0) {
            return;
        }
        for (int index = 0; index < overflow; index++) {
            if (events.isEmpty()) {
                return;
            }
            events.removeFirst();
        }
    }
}

