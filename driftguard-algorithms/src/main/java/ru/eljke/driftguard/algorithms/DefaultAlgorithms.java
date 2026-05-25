package ru.eljke.driftguard.algorithms;

import ru.eljke.driftguard.algorithms.adwin.AdwinDetector;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareDetector;
import ru.eljke.driftguard.algorithms.ks.KsDetector;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyDetector;
import ru.eljke.driftguard.algorithms.psi.PsiDetector;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.SimpleDetectorRegistry;

import java.util.List;

/**
 * Factory for the built-in algorithm set shipped with DriftGuard.
 */
public final class DefaultAlgorithms {
    private DefaultAlgorithms() {
    }

    public static List<DetectorAlgorithm<?, ?>> all() {
        return List.of(
                new PsiDetector(),
                new AdwinDetector(),
                new PageHinkleyDetector(),
                new KsDetector(),
                new ChiSquareDetector()
        );
    }

    /**
     * Creates a registry with all built-in algorithm implementations.
     */
    public static DetectorRegistry registry() {
        return new SimpleDetectorRegistry(all());
    }
}


