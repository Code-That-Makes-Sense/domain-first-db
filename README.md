# Domain First, Database Last

This is the companion code for a four-part series in
[Code That Makes Sense](https://codethatmakessense.substack.com). It is one webshop.
We build it database-first, then take it to domain-first one module at a time,
without a rewrite. The git history is the content. Every commit is one step, it
builds, and its tests pass. Read it in order.

## The plan

1. **Part 1, the legacy webshop** (done, tag `part-1`). We build the webshop the way most teams do, database first. The entities mirror the tables, the rules live in services, every feature adds a migration, and every test boots Spring and H2. At the end we expose it over HTTP, so you can try it.
2. **Part 2, a module built domain-first** (done, tag `part-2`). We add the next feature, returns, the other way round and inside the same application. Behavior comes first, the domain owns its ports, the use cases run on in-memory adapters, and the schema comes last.
3. **Part 3, the legacy module migrated** (done, tag `part-3`). We strangle the order module onto an aggregate, one method at a time, on the tables it already has. There is no rewrite and no dual write. The schema contracts afterwards.
4. **Part 4, the tests and the numbers** (in progress). We retire the tests that boot the world, turn the architecture into a build rule, split the fast suite from the full one, and measure.

Each part ends with a tag, from `part-1` to `part-4`. `git checkout part-2` shows
the code as the second post leaves it. `git log --oneline part-1..part-2` lists
the steps that post walks through.

## Status

Part 4 is in progress. The latest step is "ci: run both suites and the benchmark on GitHub Actions". GitHub Actions run the suites on every push and the benchmark on every part tag.

The earlier steps of this part:

- test: retire the rule tests that boot the world
- test: extend the architecture guard to every module
- chore: split the fast suite from the full suite
- docs: part-to-tag map and measured scorecard
- feat: benchmark the suites with JMH

## Run it

You need Java 25. Gradle comes with the wrapper. There is no database to install,
because H2 runs in memory and Flyway creates the schema on start.

```sh
./gradlew test       # everything
./gradlew fastTest   # the domain, the use cases, the in-memory adapters and the architecture rules
./gradlew bootRun    # the shop on port 8080
./gradlew jmh benchmarkReport   # both suites, hot and cold, written to docs/benchmark.md
```

With the app running, `http/shop.http` walks an order from stock receipt to
shipment, and `http/returns.http` walks a return to its refund. Run `shop.http`
first, because the return needs a shipped order.

The files use the JetBrains HTTP Client format. You can open them in IntelliJ with
the `dev` environment selected, or run them from a terminal with the
[HTTP Client CLI](https://www.jetbrains.com/help/idea/http-client-cli.html),
which you get with `brew install ijhttp`:

```sh
ijhttp --env-file http/http-client.env.json --env dev http/shop.http http/returns.http
```

CI runs both suites on every push and the benchmark on every `part-*` tag.

## Layout now

```text
com.codethatmakessense.shop
├── shared            Sku, Quantity, Money and OrderId
├── order             the HTTP controller and the Spring configuration
│   ├── domain        the Order aggregate, the value objects, and the ports
│   ├── application   OrderService: load, call the aggregate, save
│   └── adapter
│       ├── jpa       the rows and the JPA adapter
│       └── legacy    the StockReservations adapter over the legacy stock module
├── returns           the Spring configuration of the module
│   ├── domain        the ReturnRequest aggregate and the ports
│   ├── application   ReturnService
│   └── adapter
│       ├── jpa       the row and the JPA adapter
│       ├── legacy    reads the legacy order tables for the ShippedItems port
│       ├── cache     the caching decorator
│       └── web       the HTTP controller
├── stock             legacy, used only through the StockReservations port
├── report            read-only reports
└── bench             the JMH benchmark of the test suites

http/                 the request files for the HTTP Client
docs/                 the benchmark report
scripts/              the script that renders the benchmark report
```

The domain and application packages import no framework. An ArchUnit test fails
the build if that changes.

## Your turn

Fork the repo and pick one.

- Rewrite `JpaReturnRequestRepository` with `JdbcClient`. The contract test tells you when you are done. The domain does not notice the change.
- Write a second decorator on the repository port. Timing is a good candidate: log the slow reads. The same contract test tells you when you are done.
- Strangle the stock module the same way. `StockReservations` and its adapter already exist, so the seam is there. Model `StockItem` from its behavior, route `reserve`, `release` and `consume` one at a time, keep the tests green, and contract the schema last.

## The scorecard

Measured on 2026-10-04 on the author's laptop with Gradle 9.4 and `--no-daemon`,
one run each, with the classes already compiled. The wall times include the
Gradle and JVM start-up. The JUnit time is the number that moves.

| Measure                              | `part-1`       | `part-4`                     |
| ------------------------------------ | -------------- | ---------------------------- |
| Test classes that boot Spring or H2  | 5 of 5         | 12 of 20                     |
| Tests                                | 20, all booted | 71 total, 49 with no context |
| JUnit time, tests with no context    | none exist     | 0.41 s for 49 tests          |
| JUnit time, full suite               | 2.10 s         | 2.86 s                       |
| Wall time, `./gradlew fastTest`      | no such suite  | 3.7 s                        |
| Wall time, `./gradlew test`          | 6.0 s          | 6.8 s                        |
| Files touched by a rule change       | 9              | 3                            |
| Migrations needed by that change     | 1              | 0                            |

The rule change at `part-1` is the partial-shipments commit, the one at `part-4`
is the cancel fix. Both counts include this README.

The full suite did not get faster. It got bigger, because the code now has
two adapters per port, a contract test for each, and an HTTP layer. What
changed is where a rule lives and what it costs to check one. A domain rule
runs in milliseconds with nothing on the classpath, and a rule change no longer
touches the schema.
