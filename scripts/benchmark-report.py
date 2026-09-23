#!/usr/bin/env python3
"""Render JMH results as a markdown table: python3 benchmark-report.py results.json docs/benchmark.md"""

import json
import sys
from datetime import date

LABELS = {
    "fastSuiteCold": ("Fast suite (no Spring)", "cold: fresh JVM per run"),
    "fastSuiteHot": ("Fast suite (no Spring)", "hot: warmed JVM"),
    "bootedSuiteCold": ("Booted suite (Spring, H2)", "cold: fresh JVM, context built"),
    "bootedSuiteHot": ("Booted suite (Spring, H2)", "hot: warmed JVM, context cached"),
}


def main(source, target):
    with open(source, encoding="utf-8") as handle:
        results = json.load(handle)
    rows = []
    for result in results:
        name = result["benchmark"].rsplit(".", 1)[-1]
        suite, condition = LABELS.get(name, (name, ""))
        metric = result["primaryMetric"]
        runs = sum(len(iteration) for iteration in metric["rawData"])
        rows.append((suite, condition, metric["score"], metric["scoreError"], metric["scoreUnit"], runs))
    rows.sort()
    lines = [
        "# Benchmark",
        "",
        f"JMH 1.37 through the JUnit Platform Launcher, in-process, generated on {date.today().isoformat()}.",
        "Cold runs use SingleShotTime in fresh forks; hot runs use AverageTime after warmup.",
        "Gradle and JVM start-up are not part of these numbers, and neither are the ArchUnit rules:",
        "they analyze the packaged jar, which is not the code under test.",
        "",
        "| Suite | Condition | Time | Error (99.9% CI) | Runs |",
        "| --- | --- | --- | --- | --- |",
    ]
    for suite, condition, score, error, unit, runs in rows:
        lines.append(f"| {suite} | {condition} | {score:.1f} {unit} | {error:.1f} {unit} | {runs} |")
    lines.append("")
    with open(target, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    print(f"wrote {target} ({len(rows)} rows)")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
