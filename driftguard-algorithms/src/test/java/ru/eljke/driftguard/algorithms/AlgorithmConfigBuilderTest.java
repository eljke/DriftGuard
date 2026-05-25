package ru.eljke.driftguard.algorithms;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareConfig;
import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.psi.PsiConfig;
import ru.eljke.driftguard.core.domain.DriftDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlgorithmConfigBuilderTest {
    @Test
    void buildsBuiltInAlgorithmConfigs() {
        assertEquals(AdwinConfig.ALGORITHM, AdwinConfig.builder()
                .windowSize(64)
                .minSubWindowSize(8)
                .delta(0.002)
                .criticalMultiplier(2.0)
                .build()
                .algorithm());
        assertEquals(PsiConfig.ALGORITHM, PsiConfig.builder()
                .baselineWindowSize(40)
                .currentWindowSize(40)
                .buckets(10)
                .warningThreshold(0.1)
                .criticalThreshold(0.25)
                .epsilon(0.0001)
                .build()
                .algorithm());
        assertEquals(KsConfig.ALGORITHM, KsConfig.builder()
                .baselineWindowSize(40)
                .currentWindowSize(40)
                .warningPValue(0.05)
                .criticalPValue(0.01)
                .build()
                .algorithm());
        assertEquals(ChiSquareConfig.ALGORITHM, ChiSquareConfig.builder()
                .baselineWindowSize(40)
                .currentWindowSize(40)
                .buckets(8)
                .warningPValue(0.05)
                .criticalPValue(0.01)
                .minExpectedCount(1.0)
                .build()
                .algorithm());
        assertEquals(DriftDirection.DOWN, PageHinkleyConfig.builder()
                .warmupSamples(20)
                .delta(0.1)
                .warningThreshold(25.0)
                .criticalThreshold(50.0)
                .alpha(0.05)
                .direction(DriftDirection.DOWN)
                .build()
                .direction());
        assertEquals(DriftDirection.UP, PageHinkleyConfig.builder()
                .warmupSamples(20)
                .delta(0.1)
                .warningThreshold(25.0)
                .criticalThreshold(50.0)
                .alpha(0.05)
                .build()
                .direction());
    }
}
