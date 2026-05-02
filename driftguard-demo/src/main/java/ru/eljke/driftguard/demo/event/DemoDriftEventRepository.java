package ru.eljke.driftguard.demo.event;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.util.Collection;
import java.util.List;

/**
 * Хранилище drift events для demo-приложения.
 *
 * <p>Это не часть core-библиотеки. Demo использует этот слой, чтобы UI мог
 * показывать последние события независимо от конкретного источника:
 * synthetic run, live playback или Kafka demo.</p>
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
