# Domain First, Database Last

This is the companion code for a four-part series in
[Code That Makes Sense](https://codethatmakessense.substack.com). It is one webshop.
We build it database-first, then take it to domain-first one module at a time,
without a rewrite. The git history is the content. Every commit is one step, it
builds, and its tests pass. Read it in order.

## The plan

1. **Part 1, the legacy webshop** (done, tag `part-1`). We build the webshop the way most teams do, database first. The entities mirror the tables, the rules live in services, every feature adds a migration, and every test boots Spring and H2. At the end we expose it over HTTP, so you can try it.
2. **Part 2, a module built domain-first** (in progress). We add the next feature, returns, the other way round and inside the same application. Behavior comes first, the domain owns its ports, the use cases run on in-memory adapters, and the schema comes last.
3. **Part 3, the legacy module migrated** (not started). We strangle the order module onto an aggregate, one method at a time, on the tables it already has. There is no rewrite and no dual write. The schema contracts afterwards.
4. **Part 4, the tests and the numbers** (not started). We retire the tests that boot the world, turn the architecture into a build rule, split the fast suite from the full one, and measure.

Each part ends with a tag, from `part-1` to `part-4`. `git checkout part-2` shows
the code as the second post leaves it. `git log --oneline part-1..part-2` lists
the steps that post walks through.

## Status

Part 2 is in progress. The latest step is "feat(returns): keep the returns-rate report a query". The returns rate per SKU is a query, not a domain object.

The earlier steps of this part:

- feat(returns): model a return request from its behavior
- feat(returns): let the domain own its ports
- feat(returns): run the use cases over in-memory adapters
- fix(returns): count earlier returns of the same item
- test(returns): guard the domain boundary with ArchUnit
- feat(returns): answer ShippedItems from the legacy order tables
- feat(returns): derive the schema from the model
- fix(returns): saving a request twice must update it
- refactor(returns): fold the three refusals into one guard
- feat(returns): wire the module into the application

## Run it

You need Java 25. Gradle comes with the wrapper. There is no database to install,
because H2 runs in memory and Flyway creates the schema on start.

```sh
./gradlew test       # everything
./gradlew bootRun    # the shop on port 8080
```

With the app running, `http/shop.http` walks an order from stock receipt to
shipment.

The files use the JetBrains HTTP Client format. You can open them in IntelliJ with
the `dev` environment selected, or run them from a terminal with the
[HTTP Client CLI](https://www.jetbrains.com/help/idea/http-client-cli.html),
which you get with `brew install ijhttp`:

```sh
ijhttp --env-file http/http-client.env.json --env dev http/shop.http
```

## Layout now

```text
com.codethatmakessense.shop
├── order             the legacy service and the JPA entities that mirror the tables
├── returns           the Spring configuration of the module
│   ├── domain        the ReturnRequest aggregate and the ports
│   ├── application   ReturnService
│   └── adapter
│       ├── jpa       the row and the JPA adapter
│       └── legacy    reads the legacy order tables for the ShippedItems port
├── stock             reserve, release and consume stock
└── report            read-only reports

http/                 the request files for the HTTP Client
```

The domain and application packages import no framework. An ArchUnit test fails
the build if that changes.

## Your turn

Fork the repo and pick one.

- Rewrite `JpaReturnRequestRepository` with `JdbcClient`. The contract test tells you when you are done. The domain does not notice the change.
