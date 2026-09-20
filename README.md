# Domain First, Database Last

This is the companion code for a four-part series in
[Code That Makes Sense](https://codethatmakessense.substack.com). It is one webshop.
We build it database-first, then take it to domain-first one module at a time,
without a rewrite. The git history is the content. Every commit is one step, it
builds, and its tests pass. Read it in order.

## The plan

1. **Part 1, the legacy webshop** (done, tag `part-1`). We build the webshop the way most teams do, database first. The entities mirror the tables, the rules live in services, every feature adds a migration, and every test boots Spring and H2. At the end we expose it over HTTP, so you can try it.
2. **Part 2, a module built domain-first** (done, tag `part-2`). We add the next feature, returns, the other way round and inside the same application. Behavior comes first, the domain owns its ports, the use cases run on in-memory adapters, and the schema comes last.
3. **Part 3, the legacy module migrated** (in progress). We strangle the order module onto an aggregate, one method at a time, on the tables it already has. There is no rewrite and no dual write. The schema contracts afterwards.
4. **Part 4, the tests and the numbers** (not started). We retire the tests that boot the world, turn the architecture into a build rule, split the fast suite from the full one, and measure.

Each part ends with a tag, from `part-1` to `part-4`. `git checkout part-2` shows
the code as the second post leaves it. `git log --oneline part-1..part-2` lists
the steps that post walks through.

## Status

Part 3 is in progress. The latest step is "refactor(order): route pay through the aggregate". pay() goes through the aggregate.

The earlier steps of this part:

- refactor: lift Sku, Quantity, Money and OrderId into a shared kernel
- refactor(order): rename the JPA entities to rows
- feat(order): model the order aggregate from its behavior
- feat(order): let the domain own its ports
- refactor(order): move the status enum into the domain
- refactor(order): draw order ids from the sequence explicitly
- feat(order): persist the aggregate onto the legacy tables
- feat(order): reserve stock through the legacy stock module
- refactor(order): route place through the aggregate

## Run it

You need Java 25. Gradle comes with the wrapper. There is no database to install,
because H2 runs in memory and Flyway creates the schema on start.

```sh
./gradlew test       # everything
./gradlew bootRun    # the shop on port 8080
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

## Layout now

```text
com.codethatmakessense.shop
├── shared            Sku, Quantity, Money and OrderId
├── order             the legacy service, routed to the aggregate method by method
│   ├── domain        the Order aggregate, the value objects, and the ports
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
└── report            read-only reports

http/                 the request files for the HTTP Client
```

The domain and application packages import no framework. An ArchUnit test fails
the build if that changes.

## Your turn

Fork the repo and pick one.

- Rewrite `JpaReturnRequestRepository` with `JdbcClient`. The contract test tells you when you are done. The domain does not notice the change.
- Write a second decorator on the repository port. Timing is a good candidate: log the slow reads. The same contract test tells you when you are done.
