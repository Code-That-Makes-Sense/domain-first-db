# Benchmark

JMH 1.37 through the JUnit Platform Launcher, in-process, generated on 2026-10-04.
Cold runs use SingleShotTime in fresh forks; hot runs use AverageTime after warmup.
Gradle and JVM start-up are not part of these numbers, and neither are the ArchUnit rules:
they analyze the packaged jar, which is not the code under test.

| Suite | Condition | Time | Error (99.9% CI) | Runs |
| --- | --- | --- | --- | --- |
| Booted suite (Spring, H2) | cold: fresh JVM, context built | 3104.9 ms/op | 243.0 ms/op | 5 |
| Booted suite (Spring, H2) | hot: warmed JVM, context cached | 55.7 ms/op | 1.6 ms/op | 10 |
| Fast suite (no Spring) | cold: fresh JVM per run | 563.7 ms/op | 32.4 ms/op | 5 |
| Fast suite (no Spring) | hot: warmed JVM | 29.0 ms/op | 0.1 ms/op | 10 |
