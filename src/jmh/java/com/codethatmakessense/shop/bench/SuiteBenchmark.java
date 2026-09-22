package com.codethatmakessense.shop.bench;

import static org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.util.concurrent.TimeUnit;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SuiteBenchmark {

    private static final String ROOT = "com.codethatmakessense.shop";

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Fork(5)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public TestExecutionSummary fastSuiteCold() {
        return run(TagFilter.excludeTags("boots-spring"));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(1)
    @Warmup(iterations = 3)
    @Measurement(iterations = 10)
    public TestExecutionSummary fastSuiteHot() {
        return run(TagFilter.excludeTags("boots-spring"));
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Fork(5)
    @Warmup(iterations = 0)
    @Measurement(iterations = 1)
    public TestExecutionSummary bootedSuiteCold() {
        return run(TagFilter.includeTags("boots-spring"));
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(1)
    @Warmup(iterations = 3)
    @Measurement(iterations = 10)
    public TestExecutionSummary bootedSuiteHot() {
        return run(TagFilter.includeTags("boots-spring"));
    }

    private static TestExecutionSummary run(org.junit.platform.launcher.PostDiscoveryFilter tags) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage(ROOT))
                .filters(PackageNameFilter.includePackageNames(ROOT),
                        excludeClassNamePatterns(".*ArchitectureTest"), tags)
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(request, listener);
        TestExecutionSummary summary = listener.getSummary();
        if (summary.getTotalFailureCount() > 0 || summary.getTestsFoundCount() == 0) {
            throw new IllegalStateException("The suite did not pass: " + summary.getTestsFoundCount()
                    + " found, " + summary.getTotalFailureCount() + " failed");
        }
        return summary;
    }
}
