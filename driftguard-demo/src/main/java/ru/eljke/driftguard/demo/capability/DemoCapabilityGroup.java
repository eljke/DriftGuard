package ru.eljke.driftguard.demo.capability;

import java.util.List;

public record DemoCapabilityGroup(
        String id,
        String title,
        String description,
        List<DemoCapability> capabilities
) {
    public DemoCapabilityGroup {
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
    }
}