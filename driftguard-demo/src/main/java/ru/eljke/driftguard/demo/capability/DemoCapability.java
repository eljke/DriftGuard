package ru.eljke.driftguard.demo.capability;

import java.util.List;

public record DemoCapability(
        String id,
        String title,
        String description,
        String category,
        DemoCapabilityStatus status,
        List<String> apiEndpoints,
        List<String> uiSurfaces
) {
    public DemoCapability {
        apiEndpoints = List.copyOf(apiEndpoints == null ? List.of() : apiEndpoints);
        uiSurfaces = List.copyOf(uiSurfaces == null ? List.of() : uiSurfaces);
    }
}